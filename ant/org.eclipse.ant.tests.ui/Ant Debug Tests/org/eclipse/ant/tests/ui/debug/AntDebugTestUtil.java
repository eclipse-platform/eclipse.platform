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

import static org.eclipse.ant.tests.ui.testplugin.AntUITestUtil.getIFile;
import static org.eclipse.ant.tests.ui.testplugin.AntUITestUtil.getLaunchConfiguration;
import static org.eclipse.ant.tests.ui.testplugin.AntUITestUtil.getLaunchManager;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.eclipse.ant.internal.launching.debug.model.AntDebugTarget;
import org.eclipse.ant.internal.launching.debug.model.AntLineBreakpoint;
import org.eclipse.ant.internal.launching.debug.model.AntStackFrame;
import org.eclipse.ant.internal.launching.debug.model.AntThread;
import org.eclipse.ant.tests.ui.testplugin.DebugElementKindEventWaiter;
import org.eclipse.ant.tests.ui.testplugin.DebugEventWaiter;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.core.model.IThread;

public final class AntDebugTestUtil {

	public static final int DEFAULT_TIMEOUT = 20000;

	private AntDebugTestUtil() {
	}

	/**
	 * Returns the breakpoint manager
	 *
	 * @return breakpoint manager
	 */
	public static IBreakpointManager getBreakpointManager() {
		return DebugPlugin.getDefault().getBreakpointManager();
	}

	/**
	 * Launches the given configuration and waits for an event. Returns the source of the event. If the event is not received, the launch is
	 * terminated and an exception is thrown.
	 *
	 * @param configuration
	 *            the configuration to launch
	 * @param waiter
	 *            the event waiter to use
	 * @return Object the source of the event
	 */
	private static Object launchAndWait(ILaunchConfiguration configuration, DebugEventWaiter waiter)
			throws CoreException {
		return launchAndWait(configuration, waiter, true);
	}

	/**
	 * Launches the given configuration and waits for an event. Returns the source of the event. If the event is not received, the launch is
	 * terminated and an exception is thrown.
	 *
	 * @param configuration
	 *            the configuration to launch
	 * @param waiter
	 *            the event waiter to use
	 * @param register
	 *            whether to register the launch
	 * @return Object the source of the event
	 */
	public static Object launchAndWait(ILaunchConfiguration configuration, DebugEventWaiter waiter, boolean register)
			throws CoreException {
		ILaunch launch = configuration.launch(ILaunchManager.DEBUG_MODE, null, false, register);
		Object suspendee = waiter.waitForEvent();
		if (suspendee == null) {
			try {
				launch.terminate();
			}
			catch (CoreException e) {
				e.printStackTrace();
				fail("Program did not suspend, and unable to terminate launch."); //$NON-NLS-1$
			}
		}
		if (suspendee == null) {
			throw new TestAgainException("Retest - Program did not suspend, launch terminated"); //$NON-NLS-1$
		}
		return suspendee;
	}

	/**
	 * Launches the build file with the given name, and waits for a breakpoint-caused suspend event in that program. Returns the thread in which the
	 * suspend event occurred.
	 *
	 * @param buildFileName
	 *            the build file to launch
	 * @param register
	 *            whether to register the launch
	 * @return thread in which the first suspend event occurred
	 */
	public static AntThread launchToBreakpoint(String buildFileName, boolean register, boolean sepVM) throws Exception {
		if (sepVM) {
			buildFileName += "SepVM"; //$NON-NLS-1$
		}
		ILaunchConfiguration config = getLaunchConfiguration(buildFileName);
		assertNotNull(config, "Could not locate launch configuration for " + buildFileName); //$NON-NLS-1$
		return launchToBreakpoint(config, register);
	}

	/**
	 * Launches the given configuration in debug mode, and waits for a breakpoint-caused suspend event in that program. Returns the thread in which
	 * the suspend event occurred.
	 *
	 * @param config
	 *            the configuration to launch
	 * @param register
	 *            whether to register the launch
	 * @return thread in which the first suspend event occurred
	 */
	public static AntThread launchToBreakpoint(ILaunchConfiguration config, boolean register) throws CoreException {
		DebugEventWaiter waiter = new DebugElementKindEventDetailWaiter(DebugEvent.SUSPEND, AntThread.class, DebugEvent.BREAKPOINT);
		waiter.setTimeout(DEFAULT_TIMEOUT);

		Object suspendee = launchAndWait(config, waiter, register);
		assertTrue(suspendee instanceof AntThread, "suspendee was not an AntThread"); //$NON-NLS-1$
		return (AntThread) suspendee;
	}

	public static AntDebugTarget launchAndTerminate(String buildFileName, boolean sepVM) throws Exception {
		if (sepVM) {
			buildFileName += "SepVM"; //$NON-NLS-1$
		}
		ILaunchConfiguration config = getLaunchConfiguration(buildFileName);
		assertNotNull(config, "Could not locate launch configuration for " + buildFileName); //$NON-NLS-1$
		return debugLaunchAndTerminate(config, DEFAULT_TIMEOUT);
	}

	/**
	 * Launches the given configuration in debug mode, and waits for a terminate event in that program. Returns the debug target in which the
	 * terminate event occurred.
	 *
	 * @param config
	 *            the configuration to launch
	 * @param timeout
	 *            the number of milliseconds to wait for a terminate event
	 * @return thread in which the first suspend event occurred
	 */
	public static AntDebugTarget debugLaunchAndTerminate(ILaunchConfiguration config, int timeout) throws Exception {
		DebugEventWaiter waiter = new DebugElementKindEventWaiter(DebugEvent.TERMINATE, AntDebugTarget.class);
		waiter.setTimeout(timeout);

		Object terminatee = launchAndWait(config, waiter);
		assertNotNull(terminatee, "Program did not terminate."); //$NON-NLS-1$
		assertTrue(terminatee instanceof AntDebugTarget, "terminatee is not an AntDebugTarget"); //$NON-NLS-1$
		AntDebugTarget debugTarget = (AntDebugTarget) terminatee;
		assertTrue(debugTarget.isTerminated() || debugTarget.isDisconnected(), "debug target is not terminated"); //$NON-NLS-1$
		return debugTarget;
	}

	/**
	 * Launches the build file with the given name, and waits for a line breakpoint suspend event in that program. Returns the thread in which the
	 * suspend event occurred.
	 *
	 * @param buildFileName
	 *            the build file to execute
	 * @param bp
	 *            the breakpoint that should cause a suspend event
	 * @return thread in which the first suspend event occurred
	 */
	public static AntThread launchToLineBreakpoint(String buildFileName, ILineBreakpoint bp) throws CoreException {
		ILaunchConfiguration config = getLaunchConfiguration(buildFileName);
		assertNotNull(config, "Could not locate launch configuration for " + buildFileName); //$NON-NLS-1$
		return launchToLineBreakpoint(config, bp);
	}

	/**
	 * Launches the given configuration in debug mode, and waits for a line breakpoint suspend event in that program. Returns the thread in which the
	 * suspend event occurred.
	 *
	 * @param config
	 *            the configuration to launch
	 * @param bp
	 *            the breakpoint that should cause a suspend event
	 * @return thread in which the first suspend event occurred
	 */
	public static AntThread launchToLineBreakpoint(ILaunchConfiguration config, ILineBreakpoint bp)
			throws CoreException {
		DebugEventWaiter waiter = new DebugElementKindEventDetailWaiter(DebugEvent.SUSPEND, AntThread.class, DebugEvent.BREAKPOINT);
		waiter.setTimeout(DEFAULT_TIMEOUT);

		Object suspendee = launchAndWait(config, waiter);
		assertTrue(suspendee instanceof AntThread, "suspendee was not an AntThread"); //$NON-NLS-1$
		AntThread thread = (AntThread) suspendee;
		IBreakpoint hit = getBreakpoint(thread);
		assertNotNull(hit, "suspended, but not by breakpoint"); //$NON-NLS-1$
		assertTrue(bp.equals(hit), "hit un-registered breakpoint"); //$NON-NLS-1$
		assertTrue(hit instanceof ILineBreakpoint, "suspended, but not by line breakpoint"); //$NON-NLS-1$
		ILineBreakpoint breakpoint = (ILineBreakpoint) hit;
		int lineNumber = breakpoint.getLineNumber();
		int stackLine = thread.getTopStackFrame().getLineNumber();
		assertEquals(lineNumber, stackLine, "line numbers of breakpoint and stack frame do not match"); //$NON-NLS-1$

		return thread;
	}

	/**
	 * Resumes the given thread, and waits for another breakpoint-caused suspend event. Returns the thread in which the suspend event occurs.
	 *
	 * @param thread
	 *            thread to resume
	 * @return thread in which the first suspend event occurs
	 */
	public static AntThread resume(AntThread thread) throws Exception {
		return resume(thread, DEFAULT_TIMEOUT);
	}

	/**
	 * Resumes the given thread, and waits for another breakpoint-caused suspend event. Returns the thread in which the suspend event occurs.
	 *
	 * @param thread
	 *            thread to resume
	 * @param timeout
	 *            timeout in ms
	 * @return thread in which the first suspend event occurs
	 */
	public static AntThread resume(AntThread thread, int timeout) throws Exception {
		DebugEventWaiter waiter = new DebugElementKindEventDetailWaiter(DebugEvent.SUSPEND, AntThread.class, DebugEvent.BREAKPOINT);
		waiter.setTimeout(timeout);

		thread.resume();

		Object suspendee = waiter.waitForEvent();
		if (suspendee == null) {
			throw new TestAgainException("Retest - Program did not suspend"); //$NON-NLS-1$
		}
		return (AntThread) suspendee;
	}

	/**
	 * Resumes the given thread, and waits for a suspend event caused by the specified line breakpoint. Returns the thread in which the suspend event
	 * occurs.
	 *
	 * @param resumeThread
	 *            thread to resume
	 * @return thread in which the first suspend event occurs
	 */
	public static AntThread resumeToLineBreakpoint(AntThread resumeThread, ILineBreakpoint bp) throws CoreException {
		DebugEventWaiter waiter = new DebugElementKindEventDetailWaiter(DebugEvent.SUSPEND, AntThread.class, DebugEvent.BREAKPOINT);
		waiter.setTimeout(DEFAULT_TIMEOUT);

		resumeThread.resume();

		Object suspendee = waiter.waitForEvent();
		if (suspendee == null) {
			throw new TestAgainException("Retest - Program did not suspend"); //$NON-NLS-1$
		}
		assertTrue(suspendee instanceof AntThread, "suspendee was not an AntThread"); //$NON-NLS-1$
		AntThread thread = (AntThread) suspendee;
		IBreakpoint hit = getBreakpoint(thread);
		assertNotNull(hit, "suspended, but not by breakpoint"); //$NON-NLS-1$
		assertTrue(bp.equals(hit), "hit un-registered breakpoint"); //$NON-NLS-1$
		assertTrue(hit instanceof ILineBreakpoint, "suspended, but not by line breakpoint"); //$NON-NLS-1$
		ILineBreakpoint breakpoint = (ILineBreakpoint) hit;
		int lineNumber = breakpoint.getLineNumber();
		int stackLine = thread.getTopStackFrame().getLineNumber();
		assertEquals(lineNumber, stackLine, "line numbers of breakpoint and stack frame do not match"); //$NON-NLS-1$

		return (AntThread) suspendee;
	}

	/**
	 * Resumes the given thread, and waits for the associated debug target to terminate.
	 *
	 * @param thread
	 *            thread to resume
	 * @return the terminated debug target
	 */
	public static AntDebugTarget resumeAndExit(AntThread thread) throws Exception {
		DebugEventWaiter waiter = new DebugElementEventWaiter(DebugEvent.TERMINATE, thread.getDebugTarget());
		waiter.setTimeout(DEFAULT_TIMEOUT);

		thread.resume();

		Object suspendee = waiter.waitForEvent();
		if (suspendee == null) {
			throw new TestAgainException("Retest - The program did not terminate"); //$NON-NLS-1$
		}
		AntDebugTarget target = (AntDebugTarget) suspendee;
		assertTrue(target.isTerminated() || target.isDisconnected(), "program should have exited"); //$NON-NLS-1$
		return target;
	}

	/**
	 * Creates and returns a line breakpoint at the given line number in the given build file
	 *
	 * @param lineNumber
	 *            line number
	 * @param file
	 *            the build file
	 */
	public static AntLineBreakpoint createLineBreakpoint(int lineNumber, IFile file) throws CoreException {
		return new AntLineBreakpoint(file, lineNumber);
	}

	/**
	 * Creates and returns a line breakpoint at the given line number in the given build file.
	 *
	 * @param lineNumber
	 *            line number
	 * @param buildFileName
	 *            the build file name
	 */
	public static AntLineBreakpoint createLineBreakpoint(int lineNumber, String buildFileName) throws CoreException {
		return new AntLineBreakpoint(getIFile(buildFileName), lineNumber);
	}

	/**
	 * Terminates the given thread and removes its launch
	 */
	public static void terminateAndRemove(AntThread thread) throws CoreException {
		if (thread != null) {
			terminateAndRemove((AntDebugTarget) thread.getDebugTarget());
		}
	}

	/**
	 * Terminates the given debug target and removes its launch.
	 *
	 * NOTE: all breakpoints are removed, all threads are resumed, and then the target is terminated. This avoids defunct processes on linux.
	 */
	public static void terminateAndRemove(AntDebugTarget debugTarget) throws CoreException {
		if (debugTarget != null && !(debugTarget.isTerminated() || debugTarget.isDisconnected())) {
			DebugEventWaiter waiter = new DebugElementEventWaiter(DebugEvent.TERMINATE, debugTarget);
			removeAllBreakpoints();
			for (IThread thread : debugTarget.getThreads()) {
				try {
					if (thread.isSuspended()) {
						thread.resume();
					}
				}
				catch (CoreException e) {
					// do nothing
				}
			}
			debugTarget.terminate();
			Object event = waiter.waitForEvent();
			if (event == null) {
				throw new TestAgainException("Retest - Program did not terminate"); //$NON-NLS-1$
			}
			getLaunchManager().removeLaunch(debugTarget.getLaunch());
		}

		// ensure event queue is flushed
		Object context = new Object();
		DebugEventWaiter waiter = new DebugElementEventWaiter(DebugEvent.MODEL_SPECIFIC, context);
		DebugPlugin.getDefault()
				.fireDebugEventSet(new DebugEvent[] { new DebugEvent(context, DebugEvent.MODEL_SPECIFIC) });
		Object event = waiter.waitForEvent();
		if (event == null) {
			throw new TestAgainException("Retest - The model specific event was never recieved"); //$NON-NLS-1$
		}
	}

	/**
	 * Deletes all existing breakpoints
	 */
	public static void removeAllBreakpoints() throws CoreException {
		IBreakpoint[] bps = getBreakpointManager().getBreakpoints();
		getBreakpointManager().removeBreakpoints(bps, true);
	}

	/**
	 * Returns the first breakpoint the given thread is suspended at, or <code>null</code> if none.
	 *
	 * @return the first breakpoint the given thread is suspended at, or <code>null</code> if none
	 */
	public static IBreakpoint getBreakpoint(IThread thread) {
		IBreakpoint[] bps = thread.getBreakpoints();
		if (bps.length > 0) {
			return bps[0];
		}
		return null;
	}

	/**
	 * Performs a step over in the given stack frame and returns when complete.
	 *
	 * @param frame
	 *            stack frame to step in
	 */
	public static AntThread stepOver(AntStackFrame frame) throws DebugException {
		org.eclipse.ant.tests.ui.testplugin.DebugEventWaiter waiter = new DebugElementKindEventDetailWaiter(DebugEvent.SUSPEND, AntThread.class, DebugEvent.STEP_END);
		waiter.setTimeout(DEFAULT_TIMEOUT);

		frame.stepOver();

		Object suspendee = waiter.waitForEvent();
		if (suspendee == null) {
			throw new TestAgainException("Retest - Program did not suspend"); //$NON-NLS-1$
		}
		return (AntThread) suspendee;
	}

	/**
	 * Performs a step over in the given stack frame and expects to hit a breakpoint as part of the step over
	 *
	 * @param frame
	 *            stack frame to step in
	 */
	public static AntThread stepOverToHitBreakpoint(AntStackFrame frame) throws DebugException {
		org.eclipse.ant.tests.ui.testplugin.DebugEventWaiter waiter = new DebugElementKindEventDetailWaiter(DebugEvent.SUSPEND, AntThread.class, DebugEvent.BREAKPOINT);
		waiter.setTimeout(DEFAULT_TIMEOUT);

		frame.stepOver();

		Object suspendee = waiter.waitForEvent();
		if (suspendee == null) {
			throw new TestAgainException("Retest - Program did not suspend"); //$NON-NLS-1$
		}
		return (AntThread) suspendee;
	}

	/**
	 * Performs a step into in the given stack frame and returns when complete.
	 *
	 * @param frame
	 *            stack frame to step in
	 */
	public static AntThread stepInto(AntStackFrame frame) throws DebugException {
		DebugEventWaiter waiter = new DebugElementKindEventDetailWaiter(DebugEvent.SUSPEND, AntThread.class, DebugEvent.STEP_END);
		waiter.setTimeout(DEFAULT_TIMEOUT);

		frame.stepInto();

		Object suspendee = waiter.waitForEvent();
		if (suspendee == null) {
			throw new TestAgainException("Retest - Program did not suspend"); //$NON-NLS-1$
		}
		return (AntThread) suspendee;
	}



}
