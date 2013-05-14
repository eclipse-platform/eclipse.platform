/*******************************************************************************
 * Copyright (c) 2009, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.tests.ccvs.ui;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;

import junit.framework.Test;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSStatus;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.core.client.Session;
import org.eclipse.team.internal.ccvs.core.client.listeners.DiffListener;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.wizards.GenerateDiffFileWizard;
import org.eclipse.team.tests.ccvs.core.EclipseTest;
import org.eclipse.team.tests.ccvs.core.TeamCVSTestPlugin;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class CreatePatchTest extends EclipseTest {

	public static final String PATCHDATA = "patchdata";

	private final static int INITIAL_WIDTH = 300;
	private final static int INITIAL_HEIGHT = 350;

	private GenerateDiffFileWizard wizard = null;
	private WizardDialog wizardDialog = null;

	private IProject testProject = null;

	public CreatePatchTest() {
		super();
	}

	public CreatePatchTest(String name) {
		super(name);
	}

	public static Test suite() {
		return suite(CreatePatchTest.class);
	}

	protected void setUp() throws Exception {
		super.setUp();
		testProject = createProject("ApplyPatchTest", new String[] {});
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		testProject.delete(true, null);
	}

	//TODO Temporary switched off, see Bug 400540
	public void _testCreateWorkspacePatch() throws Exception {
		copyIntoWorkspace("exp_addition.txt", "addition.txt");

		openGenerateDiffFileWizard(new IResource[] { testProject });
		assertTrue(wizard.getPageCount() == 2);

		IWizardPage locationPage = wizard.getPages()[0];

		getButton(locationPage, "cpRadio").setSelection(false);
		getButton(locationPage, "fsRadio").setSelection(false);
		getButton(locationPage, "wsRadio").setSelection(true);
		getButton(locationPage, "wsRadio").notifyListeners(SWT.Selection,
				createSelectionEvent());
		getText(locationPage, "wsPathText").setText(
				"/" + testProject.getName() + "/patch_addition.txt");

		waitForSelection();

		processQueuedEvents();
		assertTrue(locationPage.canFlipToNextPage());
		callMethod(wizardDialog, "nextPressed");

		IWizardPage optionsPage = wizard.getPages()[1];

		getButton(optionsPage, "unifiedDiffOption").setSelection(true);
		getButton(optionsPage, "contextDiffOption").setSelection(false);
		getButton(optionsPage, "regularDiffOption").setSelection(false);
		getButton(optionsPage, "unified_workspaceRelativeOption").setSelection(
				true);
		getButton(optionsPage, "unified_projectRelativeOption").setSelection(
				false);
		getButton(optionsPage, "unified_selectionRelativeOption").setSelection(
				false);

		processQueuedEvents();
		assertTrue(wizard.canFinish());
		wizard.performFinish();
		wizardDialog.close();

		// Ignore lines prefixed with following values to avoid timestamps
		// mismatches
		String prefixesToIgnore[] = new String[] { "#P ApplyPatchTest-",
				"--- /dev/null", "+++ addition.txt" };

		InputStream expectedIS = asInputStream("patch_addition.txt");
		String expected = filterStream(expectedIS, prefixesToIgnore);

		IFile patchFile = testProject.getFile("patch_addition.txt");
		String actual = readProjectFile(patchFile, prefixesToIgnore);

		assertEquals(expected, actual);
	}

	private void openGenerateDiffFileWizard(IResource resources[]) {
		String title = CVSUIMessages.GenerateCVSDiff_title;
		wizard = new GenerateDiffFileWizard(getActivePart(), resources, true);
		wizard.setWindowTitle(title);
		wizardDialog = new WizardDialog(getShell(), wizard);
		wizardDialog.setMinimumPageSize(INITIAL_WIDTH, INITIAL_HEIGHT);
		wizardDialog.setBlockOnOpen(false);
		wizardDialog.open();
	}

	private void copyIntoWorkspace(String source, String target) {
		IFile file = testProject.getFile(target);
		InputStream is = asInputStream(source);
		try {
			if (file.exists()) {
				file.setContents(is, true, true, null);
			} else {
				file.create(is, true, null);
			}
		} catch (CoreException e) {
			fail(e.getMessage());
		}
	}

	private void waitForSelection() {
		IWizardPage locationPage = wizard.getPages()[0];
		int toSleep = 100;
		int totalWaited = 0;
		IResource[] sel = null;
		while (sel == null || sel.length == 0) {
			sel = (IResource[]) callMethod(locationPage, "getSelectedResources");
			processQueuedEvents();
			try {
				Thread.sleep(toSleep);
			} catch (InterruptedException e) {
				fail(e.getMessage());
			}
			totalWaited += toSleep;
			assertTrue(totalWaited < 2500);
		}
	}

	private String readProjectFile(final IFile file,
			final String[] prefixesToIgnore) {
		final String[] ret = new String[1];
		int toSleep = 100;
		int totalWaited = 0;
		while (!file.exists()) {
			processQueuedEvents();
			try {
				Thread.sleep(toSleep);
			} catch (InterruptedException e) {
				fail(e.getMessage());
			}
			totalWaited += toSleep;
			assertTrue(totalWaited < 2500);
		}
		try {
			ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
				public void run(IProgressMonitor monitor) throws CoreException {
					ret[0] = filterStream(file.getContents(), prefixesToIgnore);
				}
			}, file, IResource.NONE, null);
		} catch (CoreException e) {
			fail(e.getMessage());
		}
		return ret[0];
	}

	private String filterStream(InputStream stream, String prefixesToIgnore[]) {
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(stream));
		String line = null;
		StringBuffer buffer = new StringBuffer();
		try {
			while ((line = reader.readLine()) != null) {
				boolean ignore = false;
				for (int i = 0; i < prefixesToIgnore.length && !ignore; i++) {
					ignore = line.startsWith(prefixesToIgnore[i]);
				}
				if (!ignore) {
					buffer.append(line + "\n");
				}
			}
		} catch (IOException e) {
			fail(e.getMessage());
		}
		String ret = buffer.toString();
		try {
			reader.close();
		} catch (IOException e) {
			fail(e.getMessage());
		}
		return ret;
	}

	private InputStream asInputStream(String name) {
		IPath path = new Path(PATCHDATA).append(name);
		try {
			URL base = TeamCVSTestPlugin.getDefault().getBundle().getEntry("/");
			URL url = new URL(base, path.toString());
			return url.openStream();
		} catch (IOException e) {
			fail("Failed while reading " + name);
			return null;
		}
	}

	private Text getText(Object object, String name) {
		return (Text) ReflectionUtils.getField(object, name);
	}

	private Button getButton(Object object, String name) {
		return (Button) ReflectionUtils.getField(object, name);
	}

	private Object callMethod(Object object, String name) {
		return ReflectionUtils.callMethod(object, name, new Object[] {});
	}

	private Shell getShell() {
		return getActivePart().getSite().getShell();
	}

	private IWorkbenchPart getActivePart() {
		IWorkbenchWindow window = PlatformUI.getWorkbench()
				.getActiveWorkbenchWindow();
		assertNotNull(window);
		IWorkbenchPage page = window.getActivePage();
		if (page == null) {
			window.setActivePage(window.getPages()[0]);
			page = window.getActivePage();
		}
		assertNotNull(page);
		return page.getActivePart();
	}

	private void processQueuedEvents() {
		while (Display.getCurrent().readAndDispatch()) {
		}
	}

	private Event createSelectionEvent() {
		Event event = new Event();
		event.type = SWT.Selection;
		return event;
	}

	public void testBug319661() throws FileNotFoundException, CoreException {

		Session session = new Session(getRepository(),
				(ICVSFolder) getCVSResource(testProject)) {
			// Override the session so it always returns response with an error.
			private BufferedReader serverResp = new BufferedReader(
					new InputStreamReader(
							asInputStream("server_response_with_error.txt")));


			public String readLine() throws CVSException {
				try {
					return serverResp.readLine();
				} catch (IOException e) {
					throw new CVSException(new Status(IStatus.ERROR, null,
							null, e));
				}
			}

			public void close() {
				try {
					super.close();
					serverResp.close();
				} catch (IOException e) {
					fail(e.getMessage());
				}
			}
		};

		PrintStream stream = new PrintStream(new FileOutputStream(testProject
				.getFile("/patch_with_error.txt").getLocation().toFile()));

		try {
			session.open(getMonitor());

			DiffListener diffListener = new DiffListener(stream);

			IStatus status = Command.DIFF.execute(session,
					Command.NO_GLOBAL_OPTIONS, new Command.LocalOption[0],
					new String[0], diffListener, getMonitor());

			assertNotNull(
					"Diff command did not report error when some changes were excluded",
					status);
			assertEquals("Diff command did not report server error",
					CVSStatus.SERVER_ERROR, status.getCode());

			IStatus children[] = status.getChildren();
			assertTrue("Diff command did not report any server errors",
					children.length > 0);
			
			boolean errorLineOccurred = false;
			for (int i = 0; i < children.length; i++) {
				if (children[i].getCode() == CVSStatus.ERROR_LINE) {
					errorLineOccurred = true;
					break;
				}
			}

			assertTrue("Diff command did not report error line",
					errorLineOccurred);

		} finally {
			session.close();
			stream.close();
		}
	}
	
}
