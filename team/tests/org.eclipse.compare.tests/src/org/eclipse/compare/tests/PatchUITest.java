/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.internal.CompareUIPlugin;
import org.eclipse.compare.internal.patch.PatchMessages;
import org.eclipse.compare.internal.patch.PatchWizard;
import org.eclipse.compare.internal.patch.PatchWizardDialog;
import org.eclipse.core.internal.resources.WorkspaceRoot;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

public class PatchUITest {

	private static final String TEST_PROJECT = "ApplyPatchTest";

	private IWorkspaceRoot workspaceRoot = null;
	private IProject testProject = null;

	private PatchWizardDialog wizardDialog = null;
	private PatchWizard wizard = null;

	@Before
	public void setUp() throws Exception {
		workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		testProject = workspaceRoot.getProject(TEST_PROJECT);
		testProject.create(null);
		testProject.open(null);
	}

	@After
	public void tearDown() throws Exception {
		testProject.delete(true, null);
	}

	@Test
	public void testApplyClipboardPatch() throws Exception {
		// Clipboard support on Mac OS is not reliable when tests are run
		// through an SSH session, see bug 272870 for details
		if (Platform.getOS().equals(Platform.OS_MACOSX)) {
			return;
		}

		copyIntoClipboard("patch_context0.txt");
		copyIntoWorkspace("context.txt");

		openPatchWizard();
		assertThat(wizard.getPageCount()).isEqualTo(3);
		IWizardPage patchWizardPage = wizard.getPages()[0];

		assertTrue(patchWizardPage.canFlipToNextPage());

		ReflectionUtils.callMethod(wizardDialog, "nextPressed");

		processQueuedEvents();
		assertTrue(wizard.canFinish());
		wizard.performFinish();
		wizardDialog.close();

		InputStream expected = PatchUtils.asInputStream("exp_context.txt");
		InputStream actual = testProject.getFile("context.txt").getContents();
		compareStreams(expected, actual);
	}

	@Test
	public void testApplyWorkspacePatch() throws Exception {
		copyIntoWorkspace("patch_addition.txt");

		openPatchWizard();
		assertThat(wizard.getPageCount()).isEqualTo(3);
		IWizardPage patchWizardPage = wizard.getPages()[0];

		getButton(patchWizardPage, "fUseClipboardButton").setSelection(false);
		getButton(patchWizardPage, "fUsePatchFileButton").setSelection(false);
		getButton(patchWizardPage, "fUseURLButton").setSelection(false);
		getButton(patchWizardPage, "fUseWorkspaceButton").setSelection(true);

		TreeViewer tree = getTreeViewer(patchWizardPage, "fTreeViewer");
		treeSelect(tree, TEST_PROJECT + "/patch_addition.txt");

		processQueuedEvents();
		assertTrue(patchWizardPage.canFlipToNextPage());
		ReflectionUtils.callMethod(wizardDialog, "nextPressed");

		assertTrue(wizard.canFinish());
		wizard.performFinish();
		wizardDialog.close();

		InputStream expected = PatchUtils.asInputStream("exp_addition.txt");
		InputStream actual = testProject.getFile("exp_addition.txt")
				.getContents();
		compareStreams(expected, actual);
	}
@Test
	public void testApplyClipboardPatch_AdditionWithWindowsLD() throws Exception {
		// Clipboard support on Mac OS is not reliable when tests are run
		// through an SSH session, see bug 272870 for details
		if (Platform.getOS().equals(Platform.OS_MACOSX)) {
			return;
		}

		Preferences workspacePreferences = Platform.getPreferencesService().getRootNode().node(InstanceScope.SCOPE);
		final String previous = getStoredValue(workspacePreferences);
		// set new text file line delimiter to "\r\n" (Windows)
		saveValue(workspacePreferences, "\r\n");

		copyIntoClipboard("patch_addition.txt");

		openPatchWizard();
		assertThat(wizard.getPageCount()).isEqualTo(3);
		IWizardPage patchWizardPage = wizard.getPages()[0];

		assertTrue(patchWizardPage.canFlipToNextPage());

		ReflectionUtils.callMethod(wizardDialog, "nextPressed");

		processQueuedEvents();
		assertTrue(wizard.canFinish());
		wizard.performFinish();
		wizardDialog.close();

		InputStream expectedIS = PatchUtils.asInputStream("exp_addition.txt");
		InputStream actualIS = testProject.getFile("exp_addition.txt").getContents();

		String expected = PatchUtils.asString(expectedIS).replaceAll("\r?\n", "\r\n");
		String actual = PatchUtils.asString(actualIS);
		assertThat(actual).isEqualTo(expected);

		// restore previously saved value for LD
		saveValue(workspacePreferences, previous);
	}

	/**
	 * Returns the value that is currently stored for the line delimiter.
	 *
	 * @param node
	 *            preferences node from which the value should be read
	 * @return the currently stored line delimiter
	 */
	private String getStoredValue(Preferences node) {
		try {
			// be careful looking up for our node so not to create any nodes as side effect
			if (node.nodeExists(Platform.PI_RUNTIME))
				return node.node(Platform.PI_RUNTIME).get(Platform.PREF_LINE_SEPARATOR, null);
		} catch (BackingStoreException e) {
			// ignore
		}
		return null;
	}

	private void saveValue(Preferences preferences, String val) throws BackingStoreException{
		Preferences node = preferences.node(Platform.PI_RUNTIME);
		if (val == null) {
			node.remove(Platform.PREF_LINE_SEPARATOR);
		} else {
			node.put(Platform.PREF_LINE_SEPARATOR, val);
		}
		node.flush();
	}

	private void openPatchWizard() {
		ImageDescriptor patchWizardImage = CompareUIPlugin.getImageDescriptor("wizban/applypatch_wizban.png");
		String patchWizardTitle = PatchMessages.PatchWizard_title;

		IStorage patch = null;
		IResource target = null;
		CompareConfiguration configuration = new CompareConfiguration();

		wizard = new PatchWizard(patch, target, configuration);
		if (patchWizardImage != null)
			wizard.setDefaultPageImageDescriptor(patchWizardImage);
		if (patchWizardTitle != null)
			wizard.setWindowTitle(patchWizardTitle);
		wizard.setNeedsProgressMonitor(true);

		wizardDialog = new PatchWizardDialog(getShell(), wizard);
		wizardDialog.setBlockOnOpen(false);
		wizardDialog.open();
	}

	private void copyIntoClipboard(String name) throws IOException {
		Clipboard clipboard = new Clipboard(getShell().getDisplay());
		try (InputStream patchIS = PatchUtils.asInputStream(name)) {
			String patch = PatchUtils.asString(patchIS);
			TextTransfer textTransfer = TextTransfer.getInstance();
			Transfer[] transfers = new Transfer[] { textTransfer };
			Object[] data = new Object[] { patch };
			clipboard.setContents(data, transfers);
			clipboard.dispose();
		}
	}

	private void copyIntoWorkspace(String name) throws IOException, CoreException {
		IFile file = testProject.getFile(name);
		try (InputStream is = PatchUtils.asInputStream(name)) {
			file.create(is, true, null);
		}
	}

	private void compareStreams(InputStream expectedIS, InputStream actualIS) throws IOException {
		String expected = PatchUtils.asString(expectedIS);
		String actual = PatchUtils.asString(actualIS);
		assertThat(actual).isEqualTo(expected);
	}

	private void treeSelect(TreeViewer tree, String path) {
		WorkspaceRoot root = (WorkspaceRoot) tree.getInput();
		IFile file = root.getFile(path);
		TreePath treePath = new TreePath(new Object[] { file });
		TreeSelection sel = new TreeSelection(treePath);
		tree.setSelection(sel);
	}

	private Button getButton(Object object, String name)
			throws IllegalArgumentException, IllegalAccessException, SecurityException, NoSuchFieldException {
		return (Button) ReflectionUtils.getField(object, name);
	}

	private TreeViewer getTreeViewer(Object object, String name)
			throws IllegalArgumentException, IllegalAccessException, SecurityException, NoSuchFieldException {
		return (TreeViewer) ReflectionUtils.getField(object, name);
	}

	private Shell getShell() {
		return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
	}

	private void processQueuedEvents() {
		while (Display.getCurrent().readAndDispatch()) {
			// Process all the events in the queue
		}
	}

}
