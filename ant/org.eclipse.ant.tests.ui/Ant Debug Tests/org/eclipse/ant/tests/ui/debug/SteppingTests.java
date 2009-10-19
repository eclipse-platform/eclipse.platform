/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.tests.ui.debug;

import org.eclipse.ant.internal.launching.debug.model.AntStackFrame;
import org.eclipse.ant.internal.launching.debug.model.AntThread;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.core.model.IStackFrame;

public class SteppingTests extends AbstractAntDebugTest {

    public SteppingTests(String name) {
        super(name);
    }
    
    /**
     * bug 84400
     */
    public void testStepBackFromAntCall() throws Exception {
		antCallStack(false, 12, DebugEvent.STEP_OVER, "default: echo", 7);
    }
    
    /**
     * bug 84400
     */
    public void testStepBackFromAntCallSepVM() throws Exception {
		antCallStack(true, 12, DebugEvent.STEP_OVER, "default: echo", 7);
    }
	
	 /**
     * bug 88218, 85769
     */
    public void testStepIntoAntCall() throws Exception {
        AntThread thread= null;
        try {
            thread= antCallStack(false, 5, DebugEvent.STEP_INTO, "pre-call: echo", 16, false);
            IStackFrame[] frames= thread.getStackFrames();
            assertFrame("call", 10, (AntStackFrame)frames[1]);
        } finally {
            terminateAndRemove(thread);
            removeAllBreakpoints();
        }
    }
    
    /**
     * bug 88218, 85769
     */
    public void testStepIntoAntCallSepVM() throws Exception {
        AntThread thread= null;
        try {
            thread= antCallStack(true, 5, DebugEvent.STEP_INTO, "pre-call: echo", 16, false);
            IStackFrame[] frames= thread.getStackFrames();
            assertFrame("call", 10, (AntStackFrame)frames[1]);
        } finally {
            terminateAndRemove(thread);
            removeAllBreakpoints();
        }
    }
	
	 public void testStepOverAntCall() throws Exception {
		 antCallStack(false, 5, DebugEvent.STEP_OVER, "default: echo", 7);
	 }
	 
	 public void testStepOverAntCallSepVM() throws Exception {
		 antCallStack(true, 5, DebugEvent.STEP_OVER, "default: echo", 7);
	 }
     
     /**
      * bug 96022
      */
     public void testStepOverAntCallPastOtherAntCalls() throws Exception {
         debugStack(false, 7, DebugEvent.STEP_OVER, "default: echo", 9, "96022", true);
     }
     /**
      * bug 96022
      */
     public void testStepOverAntCallPastOtherAntCallsSepVm() throws Exception {
        debugStack(true, 7, DebugEvent.STEP_OVER, "default: echo", 9, "96022", true);
     }
	 
	 public void testStepOverAntCallHitBreakpoint() throws Exception {
		 String fileName = "debugAntCall";
		 createLineBreakpoint(12, fileName + ".xml");
		 antCallStack(false, 5, DebugEvent.BREAKPOINT, "call: sleep", 12);
	 }
	 
	 public void testStepOverAntCallHitBreakpointSepVM() throws Exception {
		 String fileName = "debugAntCall";
		 createLineBreakpoint(12, fileName + ".xml");
		 antCallStack(true, 5, DebugEvent.BREAKPOINT, "call: sleep", 12);
	 }
    
	private AntThread antCallStack(boolean sepVM, int lineNumber, int kind, String frameName, int frameLineNumber) throws CoreException {
        return antCallStack(sepVM, lineNumber, kind, frameName, frameLineNumber, true);
	}
    
	private AntThread antCallStack(boolean sepVM, int lineNumber, int kind, String frameName, int frameLineNumber, boolean terminate) throws CoreException {
        String fileName = "debugAntCall";
        return debugStack(sepVM, lineNumber, kind, frameName, frameLineNumber, fileName, terminate);
    }

	private AntThread debugStack(boolean sepVM, int lineNumber, int kind, String frameName, int frameLineNumber, String fileName, boolean terminate) throws CoreException, DebugException {
		ILineBreakpoint bp = createLineBreakpoint(lineNumber, fileName + ".xml");
		AntThread thread= null;
		try {
			if (sepVM) {
				fileName+="SepVM";
			}
			ILaunchConfiguration config= getLaunchConfiguration(fileName);
			thread= launchToLineBreakpoint(config, bp); 
			debugFrame(kind, frameName, frameLineNumber, thread);
			return thread;
        } finally {
			if (terminate) {
				terminateAndRemove(thread);
				removeAllBreakpoints();
			}
		}
	}

	private void debugFrame(int kind, String frameName, int frameLineNumber, AntThread thread) throws DebugException {
		AntStackFrame frame = (AntStackFrame)thread.getTopStackFrame();
		assertNotNull(frame);
		switch (kind) {
			case DebugEvent.BREAKPOINT: 
				stepOverToHitBreakpoint(frame);
				break;
			case DebugEvent.STEP_OVER:
				stepOver(frame);
				break;
			case DebugEvent.STEP_INTO:
				stepInto(frame);
				break;
		}
		assertFrame(thread, frameName, frameLineNumber);
	}
	
    private AntStackFrame assertFrame(AntThread thread, String frameName, int lineNumber) throws DebugException {
        AntStackFrame frame = (AntStackFrame)thread.getTopStackFrame();
        return assertFrame(frameName, lineNumber, frame);
    }

    private AntStackFrame assertFrame(String frameName, int lineNumber, AntStackFrame frame) {
        String actualFrameName= frame.getName();
        int actualLineNumber= frame.getLineNumber();
        assertTrue("Name of stack frame incorrect. Expected " + frameName + " was: " + actualFrameName, frameName.equals(actualFrameName));
        assertTrue("Line number of stack frame incorrect. Expected " + lineNumber + " was: " + actualLineNumber, lineNumber == actualLineNumber);
        return frame;
    }
    
    /**
     * bug 85309
     * @throws CoreException
     */
    public void testStepOutOfMacrodef() throws CoreException {
    	String fileName = "macrodef";
		debugStack(false, 8 , DebugEvent.STEP_OVER, "type: eclipseMacro", 16, fileName, true);
    }
    
    /**
     * bug 85309
     * @throws CoreException
     */
    public void testStepOutOfMacrodefSepVM() throws CoreException {
    	String fileName = "macrodef";
		debugStack(true, 8 , DebugEvent.STEP_OVER, "type: eclipseMacro", 16, fileName, true);
    }
    
    /**
     * bug 94769
     * @throws CoreException
     */
    public void testStepIntoMacrodef() throws CoreException {
    	testMacroDef(false);
    }
    
 /**
     * bug 94769
     * @throws CoreException
     */
    public void testStepIntoMacrodefSepVM() throws CoreException {
    	testMacroDef(true);
    }

    private void testMacroDef(boolean sepVM) throws CoreException, DebugException {
    	AntThread thread= null;
    	try {
    		String fileName = "macrodef";
    		thread= debugStack(sepVM, 16 , DebugEvent.STEP_INTO, "type: sequential", 0, fileName, false);
    		debugFrame(DebugEvent.STEP_INTO, "type: echo", 8, thread);
    		debugFrame(DebugEvent.STEP_OVER, "type: eclipseMacro", 17, thread);
    	} finally {
			terminateAndRemove(thread);
			removeAllBreakpoints();
		}
	}
    
//    public void testStepBackFromAnt() throws Exception {
//        antCallStack(false);
//    }
//    
//    public void testStepBackFromAntSepVM() throws Exception {
//        antCallStack(false);
//    }
//
//    
//    public void testStepIntoAnt() throws Exception {
//        antCallStack(false);
//    }
//    
//    public void testStepIntoAntSepVM() throws Exception {
//        antCallStack(false);
//    }
//
//    public void testStepOverAnt() throws Exception {
//        antCallStack(false);
//    }
//    
//    public void testStepOverAntSepVM() throws Exception {
//        antCallStack(false);
//    }
}
