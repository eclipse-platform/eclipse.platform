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

import org.eclipse.ant.internal.launching.debug.model.AntDebugTarget;
import org.eclipse.ant.internal.launching.debug.model.AntLineBreakpoint;
import org.eclipse.ant.internal.launching.debug.model.AntStackFrame;
import org.eclipse.ant.internal.launching.debug.model.AntThread;
import org.eclipse.ant.tests.ui.AbstractAntUIBuildTest;
import org.eclipse.ant.tests.ui.testplugin.DebugElementKindEventWaiter;
import org.eclipse.ant.tests.ui.testplugin.DebugEventWaiter;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.preferences.IDebugPreferenceConstants;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.IHyperlink;
import org.eclipse.ui.internal.console.ConsoleHyperlinkPosition;

/**
 * Tests for launch configurations
 */
@SuppressWarnings("restriction")
public abstract class AbstractAntDebugTest extends AbstractAntUIBuildTest {

	public static final int DEFAULT_TIMEOUT = 20000;

	/**
	 * The last relevant event set - for example, that caused a thread to suspend
	 */
	protected DebugEvent[] fEventSet;

	public AbstractAntDebugTest(String name) {
		super(name);
		// set error dialog to non-blocking to avoid hanging the UI during test
		ErrorDialog.AUTOMATED_MODE = true;
		SafeRunnable.setIgnoreErrors(true);
	}

	/**
	 * Sets the last relevant event set
	 * 
	 * @param set
	 *            event set
	 */
	protected void setEventSet(DebugEvent[] set) {
		fEventSet = set;
	}

	/**
	 * Returns the last relevant event set
	 * 
	 * @return event set
	 */
	protected DebugEvent[] getEventSet() {
		return fEventSet;
	}

	/**
	 * Returns the breakpoint manager
	 * 
	 * @return breakpoint manager
	 */
	protected IBreakpointManager getBreakpointManager() {
		return DebugPlugin.getDefault().getBreakpointManager();
	}

	/**
	 * Returns the source folder with the given name in the given project.
	 * 
	 * @param project
	 * @param name
	 *            source folder name
	 * @return package fragment root
	 */
	protected IPackageFragmentRoot getPackageFragmentRoot(IJavaProject project, String name) {
		IProject p = project.getProject();
		return project.getPackageFragmentRoot(p.getFolder(name));
	}

	@Override
	protected IHyperlink getHyperlink(int offset, IDocument doc) {
		if (offset >= 0 && doc != null) {
			Position[] positions = null;
			try {
				positions = doc.getPositions(ConsoleHyperlinkPosition.HYPER_LINK_CATEGORY);
			}
			catch (BadPositionCategoryException ex) {
				// no links have been added
				return null;
			}
			for (Position position : positions) {
				if (offset >= position.getOffset() && offset <= (position.getOffset() + position.getLength())) {
					return ((ConsoleHyperlinkPosition) position).getHyperLink();
				}
			}
		}
		return null;
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
	 * @exception Exception
	 *                if the event is never received.
	 */
	@Override
	protected Object launchAndWait(ILaunchConfiguration configuration, DebugEventWaiter waiter) throws CoreException {
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
	 * @exception Exception
	 *                if the event is never received.
	 */
	protected Object launchAndWait(ILaunchConfiguration configuration, DebugEventWaiter waiter, boolean register) throws CoreException {
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
		setEventSet(waiter.getEventSet());
		if (suspendee == null) {
			throw new TestAgainException("Retest - Program did not suspend, launch terminated"); //$NON-NLS-1$
		}
		return suspendee;
	}

	/**
	 * Launches the build file with the given name, and waits for a suspend event in that program. Returns the thread in which the suspend event
	 * occurred.
	 * 
	 * @param buildFileName
	 *            the build file to launch
	 * @return thread in which the first suspend event occurred
	 */
	protected AntThread launchAndSuspend(String buildFileName) throws Exception {
		ILaunchConfiguration config = getLaunchConfiguration(buildFileName);
		assertNotNull("Could not locate launch configuration for " + buildFileName, config); //$NON-NLS-1$
		return launchAndSuspend(config);
	}

	/**
	 * Launches the given configuration in debug mode, and waits for a suspend event in that program. Returns the thread in which the suspend event
	 * occurred.
	 * 
	 * @param config
	 *            the configuration to launch
	 * @return thread in which the first suspend event occurred
	 */
	protected AntThread launchAndSuspend(ILaunchConfiguration config) throws Exception {
		DebugEventWaiter waiter = new DebugElementKindEventWaiter(DebugEvent.SUSPEND, AntThread.class);
		waiter.setTimeout(DEFAULT_TIMEOUT);
		Object suspendee = launchAndWait(config, waiter);
		return (AntThread) suspendee;
	}

	/**
	 * Launches the build file with the given name, and waits for a breakpoint-caused suspend event in that program. Returns the thread in which the
	 * suspend event occurred.
	 * 
	 * @param buildFileName
	 *            the build file to launch
	 * @return thread in which the first suspend event occurred
	 */
	protected AntThread launchToBreakpoint(String buildFileName) throws Exception {
		return launchToBreakpoint(buildFileName, true, false);
	}

	/**
	 * Launches the build file with the given name in a separate VM, and waits for a breakpoint-caused suspend event in that program. Returns the
	 * thread in which the suspend event occurred.
	 * 
	 * @param buildFileName
	 *            the build file to launch
	 * @return thread in which the first suspend event occurred
	 */
	protected AntThread launchToBreakpointSepVM(String buildFileName) throws Exception {
		return launchToBreakpoint(buildFileName, true, true);
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
	protected AntThread launchToBreakpoint(String buildFileName, boolean register, boolean sepVM) throws Exception {
		if (sepVM) {
			buildFileName += "SepVM"; //$NON-NLS-1$
		}
		ILaunchConfiguration config = getLaunchConfiguration(buildFileName);
		assertNotNull("Could not locate launch configuration for " + buildFileName, config); //$NON-NLS-1$
		return launchToBreakpoint(config, register);
	}

	/**
	 * Launches the given configuration in debug mode, and waits for a breakpoint-caused suspend event in that program. Returns the thread in which
	 * the suspend event occurred.
	 * 
	 * @param config
	 *            the configuration to launch
	 * @return thread in which the first suspend event occurred
	 */
	protected AntThread launchToBreakpoint(ILaunchConfiguration config) throws CoreException {
		return launchToBreakpoint(config, true);
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
	protected AntThread launchToBreakpoint(ILaunchConfiguration config, boolean register) throws CoreException {
		DebugEventWaiter waiter = new DebugElementKindEventDetailWaiter(DebugEvent.SUSPEND, AntThread.class, DebugEvent.BREAKPOINT);
		waiter.setTimeout(DEFAULT_TIMEOUT);

		Object suspendee = launchAndWait(config, waiter, register);
		assertTrue("suspendee was not an AntThread", suspendee instanceof AntThread); //$NON-NLS-1$
		return (AntThread) suspendee;
	}

	/**
	 * Launches the build file with the given name, and waits for a terminate event in that program. Returns the debug target in which the suspend
	 * event occurred.
	 * 
	 * @param buildFileName
	 *            the build file to execute
	 * @return debug target in which the terminate event occurred
	 */
	protected AntDebugTarget launchAndTerminate(String buildFileName) throws Exception {
		return launchAndTerminate(buildFileName, false);
	}

	protected AntDebugTarget launchAndTerminate(String buildFileName, boolean sepVM) throws Exception {
		if (sepVM) {
			buildFileName += "SepVM"; //$NON-NLS-1$
		}
		ILaunchConfiguration config = getLaunchConfiguration(buildFileName);
		assertNotNull("Could not locate launch configuration for " + buildFileName, config); //$NON-NLS-1$
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
	protected AntDebugTarget debugLaunchAndTerminate(ILaunchConfiguration config, int timeout) throws Exception {
		DebugEventWaiter waiter = new DebugElementKindEventWaiter(DebugEvent.TERMINATE, AntDebugTarget.class);
		waiter.setTimeout(timeout);

		Object terminatee = launchAndWait(config, waiter);
		assertNotNull("Program did not terminate.", terminatee); //$NON-NLS-1$
		assertTrue("terminatee is not an AntDebugTarget", terminatee instanceof AntDebugTarget); //$NON-NLS-1$
		AntDebugTarget debugTarget = (AntDebugTarget) terminatee;
		assertTrue("debug target is not terminated", debugTarget.isTerminated() || debugTarget.isDisconnected()); //$NON-NLS-1$
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
	 * @throws CoreException
	 */
	protected AntThread launchToLineBreakpoint(String buildFileName, ILineBreakpoint bp) throws CoreException {
		ILaunchConfiguration config = getLaunchConfiguration(buildFileName);
		assertNotNull("Could not locate launch configuration for " + buildFileName, config); //$NON-NLS-1$
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
	 * @throws CoreException
	 */
	protected AntThread launchToLineBreakpoint(ILaunchConfiguration config, ILineBreakpoint bp) throws CoreException {
		DebugEventWaiter waiter = new DebugElementKindEventDetailWaiter(DebugEvent.SUSPEND, AntThread.class, DebugEvent.BREAKPOINT);
		waiter.setTimeout(DEFAULT_TIMEOUT);

		Object suspendee = launchAndWait(config, waiter);
		assertTrue("suspendee was not an AntThread", suspendee instanceof AntThread); //$NON-NLS-1$
		AntThread thread = (AntThread) suspendee;
		IBreakpoint hit = getBreakpoint(thread);
		assertNotNull("suspended, but not by breakpoint", hit); //$NON-NLS-1$
		assertTrue("hit un-registered breakpoint", bp.equals(hit)); //$NON-NLS-1$
		assertTrue("suspended, but not by line breakpoint", hit instanceof ILineBreakpoint); //$NON-NLS-1$
		ILineBreakpoint breakpoint = (ILineBreakpoint) hit;
		int lineNumber = breakpoint.getLineNumber();
		int stackLine = thread.getTopStackFrame().getLineNumber();
		assertTrue("line numbers of breakpoint and stack frame do not match", lineNumber == stackLine); //$NON-NLS-1$

		return thread;
	}

	/**
	 * Resumes the given thread, and waits for another breakpoint-caused suspend event. Returns the thread in which the suspend event occurs.
	 * 
	 * @param thread
	 *            thread to resume
	 * @return thread in which the first suspend event occurs
	 */
	protected AntThread resume(AntThread thread) throws Exception {
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
	protected AntThread resume(AntThread thread, int timeout) throws Exception {
		DebugEventWaiter waiter = new DebugElementKindEventDetailWaiter(DebugEvent.SUSPEND, AntThread.class, DebugEvent.BREAKPOINT);
		waiter.setTimeout(timeout);

		thread.resume();

		Object suspendee = waiter.waitForEvent();
		setEventSet(waiter.getEventSet());
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
	 * @throws CoreException
	 */
	protected AntThread resumeToLineBreakpoint(AntThread resumeThread, ILineBreakpoint bp) throws CoreException {
		DebugEventWaiter waiter = new DebugElementKindEventDetailWaiter(DebugEvent.SUSPEND, AntThread.class, DebugEvent.BREAKPOINT);
		waiter.setTimeout(DEFAULT_TIMEOUT);

		resumeThread.resume();

		Object suspendee = waiter.waitForEvent();
		setEventSet(waiter.getEventSet());
		if (suspendee == null) {
			throw new TestAgainException("Retest - Program did not suspend"); //$NON-NLS-1$
		}
		assertTrue("suspendee was not an AntThread", suspendee instanceof AntThread); //$NON-NLS-1$
		AntThread thread = (AntThread) suspendee;
		IBreakpoint hit = getBreakpoint(thread);
		assertNotNull("suspended, but not by breakpoint", hit); //$NON-NLS-1$
		assertTrue("hit un-registered breakpoint", bp.equals(hit)); //$NON-NLS-1$
		assertTrue("suspended, but not by line breakpoint", hit instanceof ILineBreakpoint); //$NON-NLS-1$
		ILineBreakpoint breakpoint = (ILineBreakpoint) hit;
		int lineNumber = breakpoint.getLineNumber();
		int stackLine = thread.getTopStackFrame().getLineNumber();
		assertTrue("line numbers of breakpoint and stack frame do not match", lineNumber == stackLine); //$NON-NLS-1$

		return (AntThread) suspendee;
	}

	/**
	 * Resumes the given thread, and waits for the debug target to terminate (i.e. finish/exit the program).
	 * 
	 * @param thread
	 *            thread to resume
	 */
	protected void exit(AntThread thread) throws Exception {
		DebugEventWaiter waiter = new DebugElementKindEventWaiter(DebugEvent.TERMINATE, IProcess.class);
		waiter.setTimeout(DEFAULT_TIMEOUT);

		thread.resume();

		Object suspendee = waiter.waitForEvent();
		setEventSet(waiter.getEventSet());
		assertNotNull("Program did not terminate.", suspendee); //$NON-NLS-1$
	}

	/**
	 * Resumes the given thread, and waits for the associated debug target to terminate.
	 * 
	 * @param thread
	 *            thread to resume
	 * @return the terminated debug target
	 */
	protected AntDebugTarget resumeAndExit(AntThread thread) throws Exception {
		DebugEventWaiter waiter = new DebugElementEventWaiter(DebugEvent.TERMINATE, thread.getDebugTarget());
		waiter.setTimeout(DEFAULT_TIMEOUT);

		thread.resume();

		Object suspendee = waiter.waitForEvent();
		setEventSet(waiter.getEventSet());
		if (suspendee == null) {
			throw new TestAgainException("Retest - The program did not terminate"); //$NON-NLS-1$
		}
		AntDebugTarget target = (AntDebugTarget) suspendee;
		assertTrue("program should have exited", target.isTerminated() || target.isDisconnected()); //$NON-NLS-1$
		return target;
	}

	protected IResource getBreakpointResource(String typeName) throws Exception {
		IJavaElement element = getJavaProject().findElement(new Path(typeName + ".java")); //$NON-NLS-1$
		IResource resource = element.getCorrespondingResource();
		if (resource == null) {
			resource = getJavaProject().getProject();
		}
		return resource;
	}

	/**
	 * Creates and returns a line breakpoint at the given line number in the given build file
	 * 
	 * @param lineNumber
	 *            line number
	 * @param file
	 *            the build file
	 * @throws CoreException
	 */
	protected AntLineBreakpoint createLineBreakpoint(int lineNumber, IFile file) throws CoreException {
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
	protected AntLineBreakpoint createLineBreakpoint(int lineNumber, String buildFileName) throws CoreException {
		return new AntLineBreakpoint(getIFile(buildFileName), lineNumber);
	}

	/**
	 * Terminates the given thread and removes its launch
	 */
	protected void terminateAndRemove(AntThread thread) throws CoreException {
		if (thread != null) {
			terminateAndRemove((AntDebugTarget) thread.getDebugTarget());
		}
	}

	/**
	 * Terminates the given debug target and removes its launch.
	 * 
	 * NOTE: all breakpoints are removed, all threads are resumed, and then the target is terminated. This avoids defunct processes on linux.
	 */
	protected void terminateAndRemove(AntDebugTarget debugTarget) throws CoreException {
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
		DebugEventWaiter waiter = new DebugElementEventWaiter(DebugEvent.MODEL_SPECIFIC, this);
		DebugPlugin.getDefault().fireDebugEventSet(new DebugEvent[] { new DebugEvent(this, DebugEvent.MODEL_SPECIFIC) });
		Object event = waiter.waitForEvent();
		if (event == null) {
			throw new TestAgainException("Retest - The model specific event was never recieved"); //$NON-NLS-1$
		}
	}

	/**
	 * Deletes all existing breakpoints
	 */
	protected void removeAllBreakpoints() throws CoreException {
		IBreakpoint[] bps = getBreakpointManager().getBreakpoints();
		getBreakpointManager().removeBreakpoints(bps, true);
	}

	/**
	 * Returns the first breakpoint the given thread is suspended at, or <code>null</code> if none.
	 * 
	 * @return the first breakpoint the given thread is suspended at, or <code>null</code> if none
	 */
	protected IBreakpoint getBreakpoint(IThread thread) {
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
	 * @throws DebugException
	 */
	protected AntThread stepOver(AntStackFrame frame) throws DebugException {
		org.eclipse.ant.tests.ui.testplugin.DebugEventWaiter waiter = new DebugElementKindEventDetailWaiter(DebugEvent.SUSPEND, AntThread.class, DebugEvent.STEP_END);
		waiter.setTimeout(DEFAULT_TIMEOUT);

		frame.stepOver();

		Object suspendee = waiter.waitForEvent();
		setEventSet(waiter.getEventSet());
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
	 * @throws DebugException
	 */
	protected AntThread stepOverToHitBreakpoint(AntStackFrame frame) throws DebugException {
		org.eclipse.ant.tests.ui.testplugin.DebugEventWaiter waiter = new DebugElementKindEventDetailWaiter(DebugEvent.SUSPEND, AntThread.class, DebugEvent.BREAKPOINT);
		waiter.setTimeout(DEFAULT_TIMEOUT);

		frame.stepOver();

		Object suspendee = waiter.waitForEvent();
		setEventSet(waiter.getEventSet());
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
	protected AntThread stepInto(AntStackFrame frame) throws DebugException {
		DebugEventWaiter waiter = new DebugElementKindEventDetailWaiter(DebugEvent.SUSPEND, AntThread.class, DebugEvent.STEP_END);
		waiter.setTimeout(DEFAULT_TIMEOUT);

		frame.stepInto();

		Object suspendee = waiter.waitForEvent();
		setEventSet(waiter.getEventSet());
		if (suspendee == null) {
			throw new TestAgainException("Retest - Program did not suspend"); //$NON-NLS-1$
		}
		return (AntThread) suspendee;
	}

	/**
	 * Performs a step return in the given stack frame and returns when complete.
	 * 
	 * @param frame
	 *            stack frame to step return from
	 */
	protected AntThread stepReturn(AntStackFrame frame) throws DebugException {
		DebugEventWaiter waiter = new DebugElementKindEventDetailWaiter(DebugEvent.SUSPEND, AntThread.class, DebugEvent.STEP_END);
		waiter.setTimeout(DEFAULT_TIMEOUT);

		frame.stepReturn();

		Object suspendee = waiter.waitForEvent();
		setEventSet(waiter.getEventSet());
		if (suspendee == null) {
			throw new TestAgainException("Retest - Program did not suspend"); //$NON-NLS-1$
		}
		return (AntThread) suspendee;
	}

	/**
	 * Performs a step into with filters in the given stack frame and returns when complete.
	 * 
	 * @param frame
	 *            stack frame to step in
	 */
	protected AntThread stepIntoWithFilters(AntStackFrame frame) throws DebugException {
		DebugEventWaiter waiter = new DebugElementKindEventWaiter(DebugEvent.SUSPEND, AntThread.class);
		waiter.setTimeout(DEFAULT_TIMEOUT);

		// turn filters on
		try {
			DebugUITools.setUseStepFilters(true);
			frame.stepInto();
		}
		finally {
			// turn filters off
			DebugUITools.setUseStepFilters(false);
		}

		Object suspendee = waiter.waitForEvent();
		setEventSet(waiter.getEventSet());
		if (suspendee == null) {
			throw new TestAgainException("Retest - Program did not suspend"); //$NON-NLS-1$
		}
		return (AntThread) suspendee;
	}

	/**
	 * Performs a step return with filters in the given stack frame and returns when complete.
	 * 
	 * @param frame
	 *            stack frame to step in
	 */
	protected AntThread stepReturnWithFilters(AntStackFrame frame) throws DebugException {
		DebugEventWaiter waiter = new DebugElementKindEventWaiter(DebugEvent.SUSPEND, AntThread.class);
		waiter.setTimeout(DEFAULT_TIMEOUT);

		// turn filters on
		try {
			DebugUITools.setUseStepFilters(true);
			frame.stepReturn();
		}
		finally {
			// turn filters off
			DebugUITools.setUseStepFilters(false);
		}

		Object suspendee = waiter.waitForEvent();
		setEventSet(waiter.getEventSet());
		if (suspendee == null) {
			throw new TestAgainException("Retest - Program did not suspend"); //$NON-NLS-1$
		}
		return (AntThread) suspendee;
	}

	/**
	 * Performs a step over with filters in the given stack frame and returns when complete.
	 * 
	 * @param frame
	 *            stack frame to step in
	 */
	protected AntThread stepOverWithFilters(AntStackFrame frame) throws DebugException {
		DebugEventWaiter waiter = new DebugElementKindEventWaiter(DebugEvent.SUSPEND, AntThread.class);
		waiter.setTimeout(DEFAULT_TIMEOUT);

		// turn filters on
		try {
			DebugUITools.setUseStepFilters(true);
			frame.stepOver();
		}
		finally {
			// turn filters off
			DebugUITools.setUseStepFilters(false);
		}

		Object suspendee = waiter.waitForEvent();
		setEventSet(waiter.getEventSet());
		if (suspendee == null) {
			throw new TestAgainException("Retest - Program did not suspend"); //$NON-NLS-1$
		}
		return (AntThread) suspendee;
	}

	/**
	 * Returns the compilation unit with the given name.
	 * 
	 * @param project
	 *            the project containing the CU
	 * @param root
	 *            the name of the source folder in the project
	 * @param pkg
	 *            the name of the package (empty string for default package)
	 * @param name
	 *            the name of the CU (ex. Something.java)
	 * @return compilation unit
	 */
	protected ICompilationUnit getCompilationUnit(IJavaProject project, String root, String pkg, String name) {
		IProject p = project.getProject();
		IResource r = p.getFolder(root);
		return project.getPackageFragmentRoot(r).getPackageFragment(pkg).getCompilationUnit(name);
	}

	/**
	 * Sets the current set of Debug / Other preferences to use during each test
	 * 
	 * @since 3.5
	 */
	protected void setPreferences() {
		IPreferenceStore debugUIPreferences = DebugUIPlugin.getDefault().getPreferenceStore();
		String property = System.getProperty("debug.workbenchActivation"); //$NON-NLS-1$
		boolean activate = property != null && property.equals("on"); //$NON-NLS-1$
		debugUIPreferences.setValue(IDebugPreferenceConstants.CONSOLE_OPEN_ON_ERR, activate);
		debugUIPreferences.setValue(IDebugPreferenceConstants.CONSOLE_OPEN_ON_OUT, activate);
		debugUIPreferences.setValue(IInternalDebugUIConstants.PREF_ACTIVATE_DEBUG_VIEW, activate);
		debugUIPreferences.setValue(IDebugUIConstants.PREF_ACTIVATE_WORKBENCH, activate);
	}

	@Override
	protected void tearDown() throws Exception {
		if (fEventSet != null) {
			fEventSet = null;
		}
		// reset the options
		IPreferenceStore debugUIPreferences = DebugUIPlugin.getDefault().getPreferenceStore();
		debugUIPreferences.setToDefault(IDebugPreferenceConstants.CONSOLE_OPEN_ON_ERR);
		debugUIPreferences.setToDefault(IDebugPreferenceConstants.CONSOLE_OPEN_ON_OUT);
		debugUIPreferences.setToDefault(IInternalDebugUIConstants.PREF_ACTIVATE_DEBUG_VIEW);
		debugUIPreferences.setToDefault(IDebugUIConstants.PREF_ACTIVATE_WORKBENCH);
		super.tearDown();
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		setPreferences();
		DebugUIPlugin.getStandardDisplay().syncExec(() -> {
			IWorkbench workbench = PlatformUI.getWorkbench();
			IPerspectiveDescriptor descriptor = workbench.getPerspectiveRegistry().findPerspectiveWithId(IDebugUIConstants.ID_DEBUG_PERSPECTIVE);
			workbench.getActiveWorkbenchWindow().getActivePage().setPerspective(descriptor);
		});
	}
}
