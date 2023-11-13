/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
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
package org.eclipse.ant.tests.ui.debug;

import static org.junit.Assert.assertTrue;

import org.eclipse.ant.internal.launching.debug.model.AntThread;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.core.model.IStackFrame;
import org.junit.Test;

public class StackTests extends AbstractAntDebugTest {

	@Test
	public void testStackForAntCall() throws Exception {
		antCallStack(false);
	}

	@Test
	public void testStackForAntCallVM() throws Exception {
		antCallStack(true);
	}

	private void antCallStack(boolean sepVM) throws CoreException {
		String fileName = "85769"; //$NON-NLS-1$
		IFile file = getIFile(fileName + ".xml"); //$NON-NLS-1$
		ILineBreakpoint bp = createLineBreakpoint(18, file);
		AntThread thread = null;
		try {
			if (sepVM) {
				fileName += "SepVM"; //$NON-NLS-1$
			}
			thread = launchToLineBreakpoint(fileName, bp);

			IStackFrame[] frames = thread.getStackFrames();

			assertTrue(frames.length == 3);
			IStackFrame frame = frames[0];
			frame.getName().equals(""); //$NON-NLS-1$
		}
		finally {
			terminateAndRemove(thread);
			removeAllBreakpoints();
		}
	}
}
