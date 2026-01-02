/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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

import static org.eclipse.ant.tests.ui.debug.AntDebugTestUtil.createLineBreakpoint;
import static org.eclipse.ant.tests.ui.debug.AntDebugTestUtil.getBreakpoint;
import static org.eclipse.ant.tests.ui.debug.AntDebugTestUtil.getBreakpointManager;
import static org.eclipse.ant.tests.ui.debug.AntDebugTestUtil.launchAndTerminate;
import static org.eclipse.ant.tests.ui.debug.AntDebugTestUtil.launchToBreakpoint;
import static org.eclipse.ant.tests.ui.debug.AntDebugTestUtil.launchToLineBreakpoint;
import static org.eclipse.ant.tests.ui.debug.AntDebugTestUtil.removeAllBreakpoints;
import static org.eclipse.ant.tests.ui.debug.AntDebugTestUtil.resume;
import static org.eclipse.ant.tests.ui.debug.AntDebugTestUtil.resumeAndExit;
import static org.eclipse.ant.tests.ui.debug.AntDebugTestUtil.resumeToLineBreakpoint;
import static org.eclipse.ant.tests.ui.debug.AntDebugTestUtil.terminateAndRemove;
import static org.eclipse.ant.tests.ui.testplugin.AntUITestUtil.getIFile;
import static org.eclipse.ant.tests.ui.testplugin.AntUITestUtil.getLaunchConfiguration;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.ant.internal.launching.debug.model.AntDebugTarget;
import org.eclipse.ant.internal.launching.debug.model.AntLineBreakpoint;
import org.eclipse.ant.internal.launching.debug.model.AntThread;
import org.eclipse.ant.launching.IAntLaunchConstants;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.junit.jupiter.api.Test;

/**
 * Tests Ant breakpoints.
 */
@AntUIDebugTest
public class BreakpointTests {

	@Test
	public void testDeferredBreakpoints() throws Exception {
		deferredBreakpoints(false);
	}

	@Test
	public void testDeferredBreakpointsSepVM() throws Exception {
		deferredBreakpoints(true);
	}

	private void deferredBreakpoints(boolean sepVM) throws Exception, CoreException, DebugException {
		String fileName = "breakpoints"; //$NON-NLS-1$
		IFile file = getIFile(fileName + ".xml"); //$NON-NLS-1$
		List<AntLineBreakpoint> bps = new ArrayList<>();
		bps.add(createLineBreakpoint(5, file));
		bps.add(createLineBreakpoint(14, file));
		AntThread thread = null;
		try {
			thread = launchToBreakpoint(fileName, true, sepVM);
			assertNotNull(thread, "Breakpoint not hit within timeout period"); //$NON-NLS-1$
			while (!bps.isEmpty()) {
				IBreakpoint hit = getBreakpoint(thread);
				assertNotNull(hit, "suspended, but not by breakpoint"); //$NON-NLS-1$
				assertTrue(bps.contains(hit), "hit un-registered breakpoint"); //$NON-NLS-1$
				assertTrue(hit instanceof ILineBreakpoint, "suspended, but not by line breakpoint"); //$NON-NLS-1$
				ILineBreakpoint breakpoint = (ILineBreakpoint) hit;
				int lineNumber = breakpoint.getLineNumber();
				int stackLine = thread.getTopStackFrame().getLineNumber();
				assertEquals(lineNumber, stackLine, "line numbers of breakpoint and stack frame do not match"); //$NON-NLS-1$
				bps.remove(breakpoint);
				breakpoint.delete();
				if (!bps.isEmpty()) {
					if (sepVM) {
						waitForTarget();
					}
					thread = resume(thread);
				}
			}
		}
		finally {
			terminateAndRemove(thread);
			removeAllBreakpoints();
		}
	}

	@Test
	public void testDisabledBreakpoint() throws Exception {
		disabledBreakpoint(false);
	}

	@Test
	public void testDisabledBreakpointSepVM() throws Exception {
		disabledBreakpoint(true);
	}

	private void disabledBreakpoint(boolean separateVM) throws Exception, CoreException {
		String fileName = "breakpoints"; //$NON-NLS-1$
		ILineBreakpoint bp = createLineBreakpoint(5, fileName + ".xml"); //$NON-NLS-1$
		bp.setEnabled(false);
		AntDebugTarget debugTarget = null;
		try {
			debugTarget = launchAndTerminate(fileName, separateVM);
		}
		finally {
			terminateAndRemove(debugTarget);
			removeAllBreakpoints();
		}
	}

	@Test
	public void testEnableDisableBreakpoint() throws Exception {
		enableDisableBreapoint(false);
	}

	@Test
	public void testEnableDisableBreakpointSepVM() throws Exception {
		enableDisableBreapoint(true);
	}

	private void enableDisableBreapoint(boolean sepVM) throws Exception, CoreException {

		String fileName = "breakpoints"; //$NON-NLS-1$
		ILineBreakpoint bp = createLineBreakpoint(5, fileName + ".xml"); //$NON-NLS-1$
		bp.setEnabled(true);
		AntThread thread = null;
		try {
			if (sepVM) {
				fileName += "SepVM"; //$NON-NLS-1$
			}
			ILaunchConfiguration config = getLaunchConfiguration(fileName);
			ILaunchConfigurationWorkingCopy copy = config.getWorkingCopy();
			copy.setAttribute(IAntLaunchConstants.ATTR_ANT_TARGETS, "entry1,entry2"); //$NON-NLS-1$
			thread = launchToLineBreakpoint(copy, bp);
			bp.setEnabled(false);
			if (sepVM) {
				waitForTarget();
			}
			resumeAndExit(thread);
		}
		finally {
			terminateAndRemove(thread);
			removeAllBreakpoints();
		}
	}

	private synchronized void waitForTarget() throws InterruptedException {
		// wait for the target to get updated for the new breakpoint state
		wait(1000);
	}

	@Test
	public void testSkipLineBreakpoint() throws Exception {
		skipLineBreakpoint(false);
	}

	@Test
	public void testSkipLineBreakpointSepVM() throws Exception {
		skipLineBreakpoint(true);
	}

	private void skipLineBreakpoint(boolean sepVM) throws Exception {
		String fileName = "breakpoints"; //$NON-NLS-1$
		IFile file = getIFile(fileName + ".xml"); //$NON-NLS-1$
		ILineBreakpoint bp = createLineBreakpoint(5, file);
		createLineBreakpoint(15, file);
		AntThread thread = null;
		try {
			if (sepVM) {
				fileName += "SepVM"; //$NON-NLS-1$
			}
			thread = launchToLineBreakpoint(fileName, bp);
			getBreakpointManager().setEnabled(false);
			resumeAndExit(thread);
		}
		finally {
			terminateAndRemove(thread);
			removeAllBreakpoints();
			getBreakpointManager().setEnabled(true);
		}
	}

	@Test
	public void testBreakpoint() throws Exception {
		breakpoints(false, "default", 5, 15); //$NON-NLS-1$
	}

	@Test
	public void testBreakpointSepVM() throws Exception {
		breakpoints(true, "default", 5, 15); //$NON-NLS-1$
	}

	@Test
	public void testTargetBreakpoint() throws Exception {
		breakpoints(false, "entry2", 4, 24); //$NON-NLS-1$
	}

	@Test
	public void testTaskOutOfTargetBreakpoint() throws Exception {
		breakpoints(false, "entry2", 36, 5); //$NON-NLS-1$
	}

	@Test
	public void testTaskOutOfTargetBreakpointSepVm() throws Exception {
		breakpoints(true, "entry2", 36, 5); //$NON-NLS-1$
	}

	@Test
	public void testTargetBreakpointSepVM() throws Exception {
		breakpoints(true, "entry2", 4, 24); //$NON-NLS-1$
	}

	private void breakpoints(boolean sepVM, String defaultTargetName, int firstLineNumber, int secondLineNumber) throws CoreException, InterruptedException {
		String fileName = "breakpoints"; //$NON-NLS-1$
		IFile file = getIFile(fileName + ".xml"); //$NON-NLS-1$
		ILineBreakpoint bp = createLineBreakpoint(firstLineNumber, file);
		AntThread thread = null;
		try {
			if (sepVM) {
				fileName += "SepVM"; //$NON-NLS-1$
			}
			ILaunchConfiguration config = getLaunchConfiguration(fileName);
			ILaunchConfigurationWorkingCopy copy = config.getWorkingCopy();
			copy.setAttribute(IAntLaunchConstants.ATTR_ANT_TARGETS, defaultTargetName);
			if (!sepVM) {
				Thread.sleep(3000); // TODO bug 121207: wait for previous launch to fully terminate
			}
			thread = launchToLineBreakpoint(copy, bp);
			bp = createLineBreakpoint(secondLineNumber, file);
			resumeToLineBreakpoint(thread, bp);
		}
		finally {
			terminateAndRemove(thread);
			removeAllBreakpoints();
		}
	}
}
