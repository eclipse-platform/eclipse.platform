/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.tests.ui.debug;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.ant.internal.launching.debug.model.AntDebugTarget;
import org.eclipse.ant.internal.launching.debug.model.AntThread;
import org.eclipse.ant.internal.ui.launchConfigurations.IAntLaunchConfigurationConstants;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.ILineBreakpoint;

/**
 * Tests Ant breakpoints.
 */
public class BreakpointTests extends AbstractAntDebugTest {
	
	public BreakpointTests(String name) {
		super(name);
	}

	public void testDeferredBreakpoints() throws Exception {
		deferredBreakpoints(false);		
	}

	public void testDeferredBreakpointsSepVM() throws Exception {
		deferredBreakpoints(true);		
	}

	private void deferredBreakpoints(boolean sepVM) throws Exception, CoreException, DebugException {
		String fileName = "breakpoints";
		IFile file= getIFile(fileName + ".xml");
		List bps = new ArrayList();
		bps.add(createLineBreakpoint(5, file));
		bps.add(createLineBreakpoint(14, file));
		AntThread thread= null;
		try {
			thread= launchToBreakpoint(fileName, true, sepVM);
			assertNotNull("Breakpoint not hit within timeout period", thread);
			while (!bps.isEmpty()) {
				IBreakpoint hit = getBreakpoint(thread);
				assertNotNull("suspended, but not by breakpoint", hit);
				assertTrue("hit un-registered breakpoint", bps.contains(hit));
				assertTrue("suspended, but not by line breakpoint", hit instanceof ILineBreakpoint);
				ILineBreakpoint breakpoint= (ILineBreakpoint) hit;
				int lineNumber = breakpoint.getLineNumber();
				int stackLine = thread.getTopStackFrame().getLineNumber();
				assertEquals("line numbers of breakpoint and stack frame do not match", lineNumber, stackLine);
				bps.remove(breakpoint);
				breakpoint.delete();
				if (!bps.isEmpty()) {
                    if (sepVM) {
                        waitForTarget();
                    }
					thread = resume(thread);
				}
			}
		} finally {
			terminateAndRemove(thread);
			removeAllBreakpoints();
		}
	}

	public void testDisabledBreakpoint() throws Exception {
        disabledBreakpoint(false);				
	}

	public void testDisabledBreakpointSepVM() throws Exception {
        disabledBreakpoint(true);				
	}

	private void disabledBreakpoint(boolean separateVM) throws Exception, CoreException {
		String fileName = "breakpoints";
		ILineBreakpoint bp = createLineBreakpoint(5, fileName + ".xml");
		bp.setEnabled(false);
		AntDebugTarget debugTarget = null;
		try {
			debugTarget= launchAndTerminate(fileName, separateVM);
		} finally {
			terminateAndRemove(debugTarget);
			removeAllBreakpoints();
		}
	}

	public void testEnableDisableBreakpoint() throws Exception {
        enableDisableBreapoint(false);				
	}

	public void testEnableDisableBreakpointSepVM() throws Exception {
        enableDisableBreapoint(true);				
	}

	private void enableDisableBreapoint(boolean sepVM) throws Exception, CoreException {
		
		String fileName = "breakpoints";
		ILineBreakpoint bp = createLineBreakpoint(5, fileName + ".xml");
		bp.setEnabled(true);
		AntThread thread = null;
		try {
			if (sepVM) {
				fileName+= "SepVM";
			}
            ILaunchConfiguration config= getLaunchConfiguration(fileName);
            ILaunchConfigurationWorkingCopy copy= config.getWorkingCopy();
            copy.setAttribute(IAntLaunchConfigurationConstants.ATTR_ANT_TARGETS, "entry1,entry2");
			thread= launchToLineBreakpoint(copy, bp);
			bp.setEnabled(false);
            if (sepVM) {
                waitForTarget();
            }
			resumeAndExit(thread);
		} finally {
			terminateAndRemove(thread);
			removeAllBreakpoints();
		}
	}

    private synchronized void waitForTarget() {
        try {
            //wait for the target to get updated for the new breakpoint state 
            wait(1000);
        } catch (InterruptedException ie) {
        }
    }
	
	public void testSkipLineBreakpoint() throws Exception {
        skipLineBreakpoint(false);			    
	}
	
	public void testSkipLineBreakpointSepVM() throws Exception {
        skipLineBreakpoint(true);			    
	}

	private void skipLineBreakpoint(boolean sepVM) throws Exception {
		String fileName = "breakpoints";
		IFile file= getIFile(fileName + ".xml");
		ILineBreakpoint bp = createLineBreakpoint(5, file);
		createLineBreakpoint(15, file);
		AntThread thread = null;
		try {
			if (sepVM) {
				fileName+= "SepVM";
			}
		    thread= launchToLineBreakpoint(fileName, bp);
		    getBreakpointManager().setEnabled(false);
		    resumeAndExit(thread);
		} finally {
			terminateAndRemove(thread);
			removeAllBreakpoints();
			getBreakpointManager().setEnabled(true);
		}
	}
	
	public void testBreakpoint() throws Exception {
		breakpoints(false, "default", 5, 15);
	}
	
	public void testBreakpointSepVM() throws Exception {
		breakpoints(true, "default", 5, 15);
	}

	public void testTargetBreakpoint() throws Exception {
		breakpoints(false, "entry2", 4, 24);
	}
    
    public void testTaskOutOfTargetBreakpoint() throws Exception {
        breakpoints(false, "entry2", 36, 5);
    }
    
    public void testTaskOutOfTargetBreakpointSepVm() throws Exception {
        breakpoints(true, "entry2", 36, 5);
    }
	
	public void testTargetBreakpointSepVM() throws Exception {
		breakpoints(true, "entry2", 4, 24);
	}
	
	private void breakpoints(boolean sepVM, String defaultTargetName, int firstLineNumber, int secondLineNumber) throws CoreException {
		String fileName = "breakpoints";
		IFile file= getIFile(fileName + ".xml");
		ILineBreakpoint bp = createLineBreakpoint(firstLineNumber, file);
		AntThread thread = null;
		try {
			if (sepVM) {
				fileName+= "SepVM";
			}
			ILaunchConfiguration config= getLaunchConfiguration(fileName);
			ILaunchConfigurationWorkingCopy copy= config.getWorkingCopy();
			copy.setAttribute(IAntLaunchConfigurationConstants.ATTR_ANT_TARGETS, defaultTargetName);
            if (!sepVM) {
                Thread.sleep(3000); //TODO bug 121207: wait for previous launch to fully terminate
            }
		    thread= launchToLineBreakpoint(copy, bp);
			bp= createLineBreakpoint(secondLineNumber, file);
		    resumeToLineBreakpoint(thread, bp);
		} catch (InterruptedException e) {
           
        } finally {
			terminateAndRemove(thread);
			removeAllBreakpoints();
		}
	}
}
