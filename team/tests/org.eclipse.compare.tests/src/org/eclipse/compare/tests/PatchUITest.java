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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.internal.CompareUIPlugin;
import org.eclipse.compare.internal.patch.*;
import org.eclipse.core.internal.resources.WorkspaceRoot;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.PlatformUI;
import org.junit.*;
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
	public void testApplyClipboardPatch() throws CoreException {
		// Clipboard support on Mac OS is not reliable when tests are run
		// through an SSH session, see bug 272870 for details
		if (Platform.getOS().equals(Platform.OS_MACOSX)) {
			return;
		}

		copyIntoClipboard("patch_context0.txt");
		copyIntoWorkspace("context.txt");

		openPatchWizard();
		assertTrue(wizard.getPageCount() == 3);
		IWizardPage patchWizardPage = wizard.getPages()[0];

		assertTrue(patchWizardPage.canFlipToNextPage());

		callMethod(wizardDialog, "nextPressed");

		processQueuedEvents();
		assertTrue(wizard.canFinish());
		wizard.performFinish();
		wizardDialog.close();

		InputStream expected = PatchUtils.asInputStream("exp_context.txt");
		InputStream actual = testProject.getFile("context.txt").getContents();
		compareStreams(expected, actual);
	}
@Test
	public void testApplyWorkspacePatch() throws CoreException {
		copyIntoWorkspace("patch_addition.txt");

		openPatchWizard();
		assertTrue(wizard.getPageCount() == 3);
		IWizardPage patchWizardPage = wizard.getPages()[0];

		getButton(patchWizardPage, "fUseClipboardButton").setSelection(false);
		getButton(patchWizardPage, "fUsePatchFileButton").setSelection(false);
		getButton(patchWizardPage, "fUseURLButton").setSelection(false);
		getButton(patchWizardPage, "fUseWorkspaceButton").setSelection(true);

		TreeViewer tree = getTreeViewer(patchWizardPage, "fTreeViewer");
		treeSelect(tree, TEST_PROJECT + "/patch_addition.txt");

		processQueuedEvents();
		assertTrue(patchWizardPage.canFlipToNextPage());
		callMethod(wizardDialog, "nextPressed");

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
		assertTrue(wizard.getPageCount() == 3);
		IWizardPage patchWizardPage = wizard.getPages()[0];

		assertTrue(patchWizardPage.canFlipToNextPage());

		callMethod(wizardDialog, "nextPressed");

		processQueuedEvents();
		assertTrue(wizard.canFinish());
		wizard.performFinish();
		wizardDialog.close();

		InputStream expectedIS = PatchUtils.asInputStream("exp_addition.txt");
		InputStream actualIS = testProject.getFile("exp_addition.txt").getContents();

		String expected = PatchUtils.asString(expectedIS).replaceAll("\r?\n", "\r\n");
		String actual = PatchUtils.asString(actualIS);

		assertEquals(expected, actual);

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

	private void copyIntoClipboard(String name) {
		Clipboard clipboard = new Clipboard(getShell().getDisplay());
		InputStream patchIS = PatchUtils.asInputStream(name);
		String patch = null;
		try {
			patch = PatchUtils.asString(patchIS);
		} catch (IOException e) {
			fail(e.getMessage());
		}
		TextTransfer textTransfer = TextTransfer.getInstance();
		Transfer[] transfers = new Transfer[] { textTransfer };
		Object[] data = new Object[] { patch };
		clipboard.setContents(data, transfers);
		clipboard.dispose();
	}

	private void copyIntoWorkspace(String name) {
		IFile file = testProject.getFile(name);
		InputStream is = PatchUtils.asInputStream(name);
		try {
			file.create(is, true, null);
		} catch (CoreException e) {
			fail(e.getMessage());
		}
	}

	private void compareStreams(InputStream expectedIS, InputStream actualIS) {
		String expected = null;
		String actual = null;
		try {
			expected = PatchUtils.asString(expectedIS);
			actual = PatchUtils.asString(actualIS);
		} catch (IOException e) {
			fail(e.getMessage());
		}
		assertEquals(expected, actual);
	}

	private void treeSelect(TreeViewer tree, String path) {
		WorkspaceRoot root = (WorkspaceRoot) tree.getInput();
		IFile file = root.getFile(path);
		TreePath treePath = new TreePath(new Object[] { file });
		TreeSelection sel = new TreeSelection(treePath);
		tree.setSelection(sel);
	}

	private Button getButton(Object object, String name) {
		return (Button) getField(object, name);
	}

	private TreeViewer getTreeViewer(Object object, String name) {
		return (TreeViewer) getField(object, name);
	}

	private Object getField(Object object, String name) {
		Object ret = null;
		try {
			ret = ReflectionUtils.getField(object, name);
		} catch (IllegalArgumentException e) {
			fail(e.getMessage());
		} catch (SecurityException e) {
			fail(e.getMessage());
		} catch (IllegalAccessException e) {
			fail(e.getMessage());
		} catch (NoSuchFieldException e) {
			fail(e.getMessage());
		}
		return ret;
	}

	private Object callMethod(Object object, String name, Object... args) {
		Object ret = null;
		try {
			ret = ReflectionUtils.callMethod(object, name, args);
		} catch (IllegalArgumentException e) {
			fail(e.getMessage());
		} catch (IllegalAccessException e) {
			fail(e.getMessage());
		} catch (InvocationTargetException e) {
			fail(e.getMessage());
		} catch (NoSuchMethodException e) {
			fail(e.getMessage());
		}
		return ret;
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
