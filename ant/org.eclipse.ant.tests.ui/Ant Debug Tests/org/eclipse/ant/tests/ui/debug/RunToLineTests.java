/*******************************************************************************
 *  Copyright (c) 2005, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.tests.ui.debug;

import static org.eclipse.ant.tests.ui.debug.AntDebugTestUtil.createLineBreakpoint;
import static org.eclipse.ant.tests.ui.debug.AntDebugTestUtil.launchToLineBreakpoint;
import static org.eclipse.ant.tests.ui.debug.AntDebugTestUtil.removeAllBreakpoints;
import static org.eclipse.ant.tests.ui.debug.AntDebugTestUtil.terminateAndRemove;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.ant.internal.launching.debug.model.AntLineBreakpoint;
import org.eclipse.ant.internal.launching.debug.model.AntThread;
import org.eclipse.ant.tests.ui.testplugin.TestAgainException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.actions.IRunToLineTarget;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IPerspectiveListener2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.junit.jupiter.api.Test;

/**
 * Tests run to line debug functionality
 */
@SuppressWarnings("restriction")
@AntUIDebugTest
public class RunToLineTests {

	private final Object fLock = new Object();
	private IEditorPart fEditor = null;

	class MyListener implements IPerspectiveListener2 {

		@Override
		public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective,
				IWorkbenchPartReference partRef, String changeId) {
			if (partRef.getTitle().equals("breakpoints.xml") && changeId == IWorkbenchPage.CHANGE_EDITOR_OPEN) { //$NON-NLS-1$
				synchronized (fLock) {
					fEditor = (IEditorPart) partRef.getPart(true);
					fLock.notifyAll();
				}
			}
		}

		@Override
		public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
			// do nothing
		}

		@Override
		public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective, String changeId) {
			// do nothing
		}

	}

	/**
	 * Test a run to line, with no extra breakpoints.
	 */
	@Test
	public void testRunToLine() throws Exception {
		runToLine(14, 14, true, false);
	}

	/**
	 * Test a run to line, with no extra breakpoints in separate VM.
	 */
	@Test
	public void testRunToLineSepVM() throws Exception {
		runToLine(14, 14, true, true);
	}

	/**
	 * Test a run to line, with an extra breakpoint, and preference to skip
	 */
	@Test
	public void testRunToLineSkipBreakpoint() throws Exception {
		createLineBreakpoint(6, "breakpoints.xml"); //$NON-NLS-1$
		runToLine(14, 14, true, false);
	}

	/**
	 * Test a run to line, with an extra breakpoint, and preference to skip in a
	 * separate VM
	 */
	@Test
	public void testRunToLineSkipBreakpointSepVM() throws Exception {
		createLineBreakpoint(6, "breakpoints.xml"); //$NON-NLS-1$
		runToLine(14, 14, true, true);
	}

	/**
	 * Test a run to line, with an extra breakpoint, and preference to *not* skip
	 */
	@Test
	public void testRunToLineHitBreakpoint() throws Exception {
		createLineBreakpoint(6, "breakpoints.xml"); //$NON-NLS-1$
		runToLine(14, 6, false, false);
	}

	/**
	 * Test a run to line, with an extra breakpoint, and preference to *not* skip
	 */
	@Test
	public void testRunToLineHitBreakpointSepVM() throws Exception {
		createLineBreakpoint(6, "breakpoints.xml"); //$NON-NLS-1$
		runToLine(14, 6, false, true);
	}

	/**
	 * Runs to the given line number in the 'breakpoints.xml' buildfile, after
	 * stopping at the Starts from line 5 in the buildfile.
	 *
	 * @param lineNumber         line number to run to, ONE BASED
	 * @param expectedLineNumber the line number to be on after run-to-line (may
	 *                           differ from the target line number if the option to
	 *                           skip breakpoints is off).
	 * @param skipBreakpoints    preference value for "skip breakpoints during run
	 *                           to line"
	 */
	public void runToLine(final int lineNumber, int expectedLineNumber, boolean skipBreakpoints, boolean sepVM)
			throws Exception {
		String fileName = "breakpoints"; //$NON-NLS-1$
		AntLineBreakpoint breakpoint = createLineBreakpoint(5, fileName + ".xml"); //$NON-NLS-1$

		boolean restore = DebugUITools.getPreferenceStore()
				.getBoolean(IDebugUIConstants.PREF_SKIP_BREAKPOINTS_DURING_RUN_TO_LINE);
		DebugUITools.getPreferenceStore().setValue(IDebugUIConstants.PREF_SKIP_BREAKPOINTS_DURING_RUN_TO_LINE,
				skipBreakpoints);
		AntThread thread = null;
		final IPerspectiveListener2 listener = new MyListener();
		try {
			// close all editors
			Runnable closeAll = () -> {
				IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				activeWorkbenchWindow.getActivePage().closeAllEditors(false);
				activeWorkbenchWindow.addPerspectiveListener(listener);
			};
			Display display = DebugUIPlugin.getStandardDisplay();
			display.syncExec(closeAll);

			if (sepVM) {
				fileName += "SepVM"; //$NON-NLS-1$
			}
			thread = launchToLineBreakpoint(fileName, breakpoint);
			// wait for editor to open
			synchronized (fLock) {
				if (fEditor == null) {
					fLock.wait(20000);
				}
			}

			assertNotNull(fEditor, "Editor did not open"); //$NON-NLS-1$

			final Exception[] exs = new Exception[1];
			final IThread suspendee = thread;
			Runnable r = () -> {
				ITextEditor editor = (ITextEditor) fEditor;
				IRunToLineTarget adapter = editor.getAdapter(IRunToLineTarget.class);
				assertNotNull(adapter, "no run to line adapter"); //$NON-NLS-1$
				IDocumentProvider documentProvider = editor.getDocumentProvider();
				assertNotNull(documentProvider, "The document provider should not be null for: " + editor.getTitle()); //$NON-NLS-1$
				try {
					// position cursor to line
					documentProvider.connect(this);
					IDocument document = documentProvider.getDocument(editor.getEditorInput());
					assertNotNull(document, "The document should be available for: " + editor.getTitle()); //$NON-NLS-1$
					int lineOffset = document.getLineOffset(lineNumber - 1); // document is 0 based!
					documentProvider.disconnect(this);
					editor.selectAndReveal(lineOffset, 0);
					// run to line
					adapter.runToLine(editor, editor.getSelectionProvider().getSelection(), suspendee);
				} catch (CoreException | BadLocationException e) {
					exs[0] = e;
				}
			};
			DebugElementEventWaiter waiter = new DebugElementEventWaiter(DebugEvent.SUSPEND, thread);
			DebugUIPlugin.getStandardDisplay().syncExec(r);
			Object event = waiter.waitForEvent();
			if (event == null) {
				throw new TestAgainException("Retest - no suspend event was recieved"); //$NON-NLS-1$
			}
			IStackFrame topStackFrame = thread.getTopStackFrame();
			assertNotNull(topStackFrame, "There must be a top stack frame"); //$NON-NLS-1$
			assertEquals(expectedLineNumber, topStackFrame.getLineNumber(), "wrong line"); //$NON-NLS-1$
		} finally {
			terminateAndRemove(thread);
			removeAllBreakpoints();
			DebugUITools.getPreferenceStore().setValue(IDebugUIConstants.PREF_SKIP_BREAKPOINTS_DURING_RUN_TO_LINE,
					restore);
			Runnable cleanup = () -> {
				IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				activeWorkbenchWindow.removePerspectiveListener(listener);
			};
			Display display = DebugUIPlugin.getStandardDisplay();
			display.asyncExec(cleanup);
		}
	}
}