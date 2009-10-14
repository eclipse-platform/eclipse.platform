/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.tests.ui.debug;

import org.eclipse.ant.internal.ui.debug.model.AntLineBreakpoint;
import org.eclipse.ant.internal.ui.debug.model.AntThread;
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

/**
 * Tests run to line debug functionality
 */
public class RunToLineTests extends AbstractAntDebugTest {
	
	public RunToLineTests(String name) {
		super(name);
	}

	private Object fLock = new Object();
	private IEditorPart fEditor = null;
	
	class MyListener implements IPerspectiveListener2 {

		/* (non-Javadoc)
		 * @see org.eclipse.ui.IPerspectiveListener2#perspectiveChanged(org.eclipse.ui.IWorkbenchPage, org.eclipse.ui.IPerspectiveDescriptor, org.eclipse.ui.IWorkbenchPartReference, java.lang.String)
		 */
		public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective, IWorkbenchPartReference partRef, String changeId) {
            if (partRef.getTitle().equals("breakpoints.xml") && changeId == IWorkbenchPage.CHANGE_EDITOR_OPEN) {
                synchronized (fLock) {
                    fEditor = (IEditorPart) partRef.getPart(true);
                    fLock.notifyAll();
                }
            }
		}

		/* (non-Javadoc)
		 * @see org.eclipse.ui.IPerspectiveListener#perspectiveActivated(org.eclipse.ui.IWorkbenchPage, org.eclipse.ui.IPerspectiveDescriptor)
		 */
		public void perspectiveActivated(IWorkbenchPage page, IPerspectiveDescriptor perspective) {
		}

		/* (non-Javadoc)
		 * @see org.eclipse.ui.IPerspectiveListener#perspectiveChanged(org.eclipse.ui.IWorkbenchPage, org.eclipse.ui.IPerspectiveDescriptor, java.lang.String)
		 */
		public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective, String changeId) {			
		}
	    
	}

	/**
	 * Test a run to line, with no extra breakpoints.
	 * 
	 * @throws Exception
	 */
	public void testRunToLine() throws Exception {
	    runToLine(14, 14, true, false);
	}
	
	/**
	 * Test a run to line, with no extra breakpoints in seperate VM.
	 * 
	 * @throws Exception
	 */
	public void testRunToLineSepVM() throws Exception {
	    runToLine(14, 14, true, true);
	}
	
	/**
	 * Test a run to line, with an extra breakpoint, and preference to skip
	 * 
	 * @throws Exception
	 */
	public void testRunToLineSkipBreakpoint() throws Exception {
	    createLineBreakpoint(6, "breakpoints.xml");
	    runToLine(14, 14, true, false);
	}	
	
	/**
	 * Test a run to line, with an extra breakpoint, and preference to skip in a 
	 * seperate VM
	 * 
	 * @throws Exception
	 */
	public void testRunToLineSkipBreakpointSepVM() throws Exception {
	    createLineBreakpoint(6, "breakpoints.xml");
	    runToLine(14, 14, true, true);
	}	

	/**
	 * Test a run to line, with an extra breakpoint, and preference to *not* skip
	 * 
	 * @throws Exception
	 */
	public void testRunToLineHitBreakpoint() throws Exception {
	    createLineBreakpoint(6, "breakpoints.xml");
	    runToLine(14, 6, false, false);
	}
	
	/**
	 * Test a run to line, with an extra breakpoint, and preference to *not* skip
	 * 
	 * @throws Exception
	 */
	public void testRunToLineHitBreakpointSepVM() throws Exception {
	    createLineBreakpoint(6, "breakpoints.xml");
	    runToLine(14, 6, false, true);
	}

	/**
	 * Runs to the given line number in the 'breakpoints.xml' buildfile, after stopping at the
	 * Starts from line 5 in the buildfile.
	 * 
	 * @param lineNumber line number to run to, ONE BASED
	 * @param expectedLineNumber the line number to be on after run-to-line (may differ from
	 *  the target line number if the option to skip breakpoints is off).
	 * @param skipBreakpoints preference value for "skip breakpoints during run to line"
	 * @throws Exception
	 */
	public void runToLine(final int lineNumber, int expectedLineNumber, boolean skipBreakpoints, boolean sepVM) throws Exception {
		String fileName = "breakpoints";
		AntLineBreakpoint breakpoint = createLineBreakpoint(5, fileName + ".xml");
		
		boolean restore = DebugUITools.getPreferenceStore().getBoolean(IDebugUIConstants.PREF_SKIP_BREAKPOINTS_DURING_RUN_TO_LINE);
		DebugUITools.getPreferenceStore().setValue(IDebugUIConstants.PREF_SKIP_BREAKPOINTS_DURING_RUN_TO_LINE, skipBreakpoints);
		AntThread thread= null;
		final IPerspectiveListener2 listener = new MyListener();
		try {
		    // close all editors
		    Runnable closeAll = new Runnable() {
                public void run() {
                    IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                    activeWorkbenchWindow.getActivePage().closeAllEditors(false);
                    activeWorkbenchWindow.addPerspectiveListener(listener);
                }
            };
            Display display = DebugUIPlugin.getStandardDisplay();
            display.syncExec(closeAll);
            
			if (sepVM) {
				fileName+= "SepVM";
			}
			thread= launchToLineBreakpoint(fileName, breakpoint);
			// wait for editor to open
			synchronized (fLock) {
			    if (fEditor == null) {
			        fLock.wait(20000);
			    }
            }
			
			assertNotNull("Editor did not open", fEditor);
			
			final Exception[] exs = new Exception[1];
			final IThread suspendee = thread;
			Runnable r = new Runnable() {
                public void run() {
                    ITextEditor editor = (ITextEditor) fEditor;
                    IRunToLineTarget adapter = (IRunToLineTarget) editor.getAdapter(IRunToLineTarget.class);
                    assertNotNull("no run to line adapter", adapter);
                    IDocumentProvider documentProvider = editor.getDocumentProvider();
                    try {
                        // position cursor to line
                        documentProvider.connect(this);
                        IDocument document = documentProvider.getDocument(editor.getEditorInput());
                        int lineOffset = document.getLineOffset(lineNumber - 1); // document is 0 based!
                        documentProvider.disconnect(this);
                        editor.selectAndReveal(lineOffset, 0);
                        // run to line
                        adapter.runToLine(editor, editor.getSelectionProvider().getSelection(), suspendee);
                    } catch (CoreException e) {
                        exs[0] = e;
                    } catch (BadLocationException e) {
                        exs[0] = e;
                    }
                }
            };
            DebugElementEventWaiter waiter = new DebugElementEventWaiter(DebugEvent.SUSPEND, thread);
            DebugUIPlugin.getStandardDisplay().syncExec(r);
            waiter.waitForEvent();
            IStackFrame topStackFrame = thread.getTopStackFrame();
            assertEquals("wrong line", expectedLineNumber, topStackFrame.getLineNumber());
		} finally {
			terminateAndRemove(thread);
			removeAllBreakpoints();
			DebugUITools.getPreferenceStore().setValue(IDebugUIConstants.PREF_SKIP_BREAKPOINTS_DURING_RUN_TO_LINE, restore);
		    Runnable cleanup = new Runnable() {
                public void run() {
                    IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
                    activeWorkbenchWindow.removePerspectiveListener(listener);
                }
            };
            Display display = DebugUIPlugin.getStandardDisplay();
            display.asyncExec(cleanup);			
		}		
	}
}