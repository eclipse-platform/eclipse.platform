/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.tests.ui.debug;

import org.eclipse.ant.internal.launching.debug.model.AntThread;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.core.model.IStackFrame;

public class StackTests extends AbstractAntDebugTest {

    public StackTests(String name) {
        super(name);
    }

    public void testStackForAntCall() throws Exception {
        antCallStack(false);
    }
    
    public void testStackForAntCallVM() throws Exception {
        antCallStack(true);
    }
    
    private void antCallStack(boolean sepVM) throws CoreException {
        String fileName = "85769";
        IFile file= getIFile(fileName + ".xml");
        ILineBreakpoint bp = createLineBreakpoint(18, file);
        AntThread thread = null;
        try {
            if (sepVM) {
                fileName+= "SepVM";
            }
            thread= launchToLineBreakpoint(fileName, bp);
            
            
            IStackFrame[] frames= thread.getStackFrames();
           
            assertTrue(frames.length == 3);
            IStackFrame frame = frames[0];
            frame.getName().equals("");
        } finally {
            terminateAndRemove(thread);
            removeAllBreakpoints();
        }
    }
}
