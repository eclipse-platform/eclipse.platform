/*******************************************************************************
 * Copyright (c) 2004, 2016 IBM Corporation and others.
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
package org.eclipse.ant.internal.launching.debug.model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.ant.internal.launching.debug.IAntDebugConstants;
import org.eclipse.ant.internal.launching.debug.IAntDebugController;
import org.eclipse.core.externaltools.internal.IExternalToolConstants;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.IBreakpointManagerListener;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IThread;

/**
 * Ant Debug Target
 */
public class AntDebugTarget extends AntDebugElement implements IDebugTarget, IDebugEventSetListener, IBreakpointManagerListener {

	// associated system process (Ant Build)
	private final IProcess fProcess;

	// containing launch object
	private final ILaunch fLaunch;

	// Build file name
	private String fName;

	// suspend state
	private boolean fSuspended = false;

	// terminated state
	private boolean fTerminated = false;

	// threads
	private final AntThread fThread;
	private IThread[] fThreads;

	private final IAntDebugController fController;

	private List<IBreakpoint> fRunToLineBreakpoints;

	/**
	 * Constructs a new debug target in the given launch for the associated Ant build process.
	 *
	 * @param launch
	 *            containing launch
	 * @param process
	 *            Ant build process
	 * @param controller
	 *            the controller to communicate to the Ant build
	 */
	public AntDebugTarget(ILaunch launch, IProcess process, IAntDebugController controller) {
		super(null);
		fLaunch = launch;
		fProcess = process;

		fController = controller;

		fThread = new AntThread(this);
		fThreads = new IThread[] { fThread };

		DebugPlugin.getDefault().getBreakpointManager().addBreakpointManagerListener(this);
		DebugPlugin.getDefault().getBreakpointManager().addBreakpointListener(this);
		DebugPlugin.getDefault().addDebugEventListener(this);
	}

	@Override
	public IProcess getProcess() {
		return fProcess;
	}

	@Override
	public IThread[] getThreads() {
		return fThreads;
	}

	@Override
	public boolean hasThreads() throws DebugException {
		return !fTerminated && fThreads.length > 0;
	}

	@Override
	public String getName() throws DebugException {
		if (fName == null) {
			try {
				fName = getLaunch().getLaunchConfiguration().getAttribute(IExternalToolConstants.ATTR_LOCATION, DebugModelMessages.AntDebugTarget_0);
				fName = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(fName);
			}
			catch (CoreException e) {
				fName = DebugModelMessages.AntDebugTarget_0;
			}
		}
		return fName;
	}

	@Override
	public boolean supportsBreakpoint(IBreakpoint breakpoint) {
		if (breakpoint.getModelIdentifier().equals(IAntDebugConstants.ID_ANT_DEBUG_MODEL)) {
			// need to consider all breakpoints as no way to tell which set
			// of build files will be executed (ant task)
			return true;
		}
		return false;
	}

	@Override
	public IDebugTarget getDebugTarget() {
		return this;
	}

	@Override
	public ILaunch getLaunch() {
		return fLaunch;
	}

	@Override
	public boolean canTerminate() {
		return !fTerminated;
	}

	@Override
	public boolean isTerminated() {
		return fTerminated;
	}

	@Override
	public void terminate() throws DebugException {
		terminated();
	}

	@Override
	public boolean canResume() {
		return !fTerminated && fSuspended;
	}

	@Override
	public boolean canSuspend() {
		return !fTerminated && !fSuspended;
	}

	@Override
	public boolean isSuspended() {
		return fSuspended;
	}

	@Override
	public void resume() throws DebugException {
		fSuspended = false;
		fController.resume();
		if (fThread.isSuspended()) {
			fThread.resumedByTarget();
		}
		fireResumeEvent(DebugEvent.CLIENT_REQUEST);
	}

	/**
	 * Notification the target has suspended for the given reason
	 *
	 * @param detail
	 *            reason for the suspend
	 */
	public void suspended(int detail) {
		fSuspended = true;
		fThread.setStepping(false);
		fThread.fireSuspendEvent(detail);
	}

	@Override
	public void suspend() throws DebugException {
		fController.suspend();
	}

	@Override
	public void breakpointAdded(IBreakpoint breakpoint) {
		if (!fTerminated) {
			fController.handleBreakpoint(breakpoint, true);
			if (breakpoint instanceof AntLineBreakpoint) {
				if (((AntLineBreakpoint) breakpoint).isRunToLine()) {
					if (fRunToLineBreakpoints == null) {
						fRunToLineBreakpoints = new ArrayList<>();
					}
					fRunToLineBreakpoints.add(breakpoint);
				}
			}
		}
	}

	@Override
	public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
		if (!fTerminated) {
			fController.handleBreakpoint(breakpoint, false);
			if (fRunToLineBreakpoints != null) {
				if (fRunToLineBreakpoints.remove(breakpoint) && fRunToLineBreakpoints.isEmpty()) {
					fRunToLineBreakpoints = null;
				}
			}
		}
	}

	@Override
	public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
		if (supportsBreakpoint(breakpoint)) {
			try {
				if (breakpoint.isEnabled() && DebugPlugin.getDefault().getBreakpointManager().isEnabled()) {
					breakpointAdded(breakpoint);
				} else {
					breakpointRemoved(breakpoint, null);
				}
			}
			catch (CoreException e) {
				// do nothing
			}
		}
	}

	@Override
	public boolean canDisconnect() {
		return false;
	}

	@Override
	public void disconnect() throws DebugException {
		// do nothing
	}

	@Override
	public boolean isDisconnected() {
		return false;
	}

	@Override
	public boolean supportsStorageRetrieval() {
		return false;
	}

	@Override
	public IMemoryBlock getMemoryBlock(long startAddress, long length) throws DebugException {
		return null;
	}

	/**
	 * Notification we have connected to the Ant build logger and it has started. Resume the build.
	 */
	public void buildStarted() {
		fireCreationEvent();
		installDeferredBreakpoints();
		try {
			resume();
		}
		catch (DebugException e) {
			// do nothing
		}
	}

	/**
	 * Install breakpoints that are already registered with the breakpoint manager if the breakpoint manager is enabled and the breakpoint is enabled.
	 */
	private void installDeferredBreakpoints() {
		IBreakpointManager manager = DebugPlugin.getDefault().getBreakpointManager();
		if (!manager.isEnabled()) {
			return;
		}
		for (IBreakpoint breakpoint : manager.getBreakpoints(IAntDebugConstants.ID_ANT_DEBUG_MODEL)) {
			try {
				if (breakpoint.isEnabled()) {
					breakpointAdded(breakpoint);
				}
			}
			catch (CoreException e) {
				// do nothing
			}
		}
	}

	/**
	 * Called when this debug target terminates.
	 */
	public synchronized void terminated() {
		if (!fTerminated) {
			fThreads = new IThread[0];
			fTerminated = true;
			fSuspended = false;
			fController.terminate();
			if (DebugPlugin.getDefault() != null) {
				DebugPlugin.getDefault().getBreakpointManager().removeBreakpointListener(this);
				DebugPlugin.getDefault().removeDebugEventListener(this);
				DebugPlugin.getDefault().getBreakpointManager().removeBreakpointManagerListener(this);
			}
			if (!getProcess().isTerminated()) {
				try {
					fProcess.terminate();
				}
				catch (DebugException e) {
					// do nothing
				}
			}
			if (DebugPlugin.getDefault() != null) {
				fireTerminateEvent();
			}
		}
	}

	/**
	 * Single step the Ant build.
	 */
	public void stepOver() {
		fSuspended = false;
		fController.stepOver();
		fireResumeEvent(DebugEvent.CLIENT_REQUEST);
	}

	/**
	 * Step-into the Ant build.
	 */
	public void stepInto() {
		fSuspended = false;
		fController.stepInto();
		fireResumeEvent(DebugEvent.CLIENT_REQUEST);
	}

	/**
	 * Notification a breakpoint was encountered. Determine which breakpoint was hit and fire a suspend event.
	 *
	 * @param event
	 *            debug event
	 */
	public void breakpointHit(String event) {
		// determine which breakpoint was hit, and set the thread's breakpoint
		String[] datum = event.split(DebugMessageIds.MESSAGE_DELIMITER);
		String fileName = datum[1];
		int lineNumber = Integer.parseInt(datum[2]);
		boolean found = false;
		for (IBreakpoint breakpoint : DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(IAntDebugConstants.ID_ANT_DEBUG_MODEL)) {
			ILineBreakpoint lineBreakpoint = (ILineBreakpoint) breakpoint;
			if (setThreadBreakpoint(lineBreakpoint, lineNumber, fileName)) {
				found = true;
				break;
			}
		}
		if (!found && fRunToLineBreakpoints != null) {
			Iterator<IBreakpoint> iter = fRunToLineBreakpoints.iterator();
			while (iter.hasNext()) {
				ILineBreakpoint lineBreakpoint = (ILineBreakpoint) iter.next();
				if (setThreadBreakpoint(lineBreakpoint, lineNumber, fileName)) {
					break;
				}
			}
		}
		suspended(DebugEvent.BREAKPOINT);
	}

	private boolean setThreadBreakpoint(ILineBreakpoint lineBreakpoint, int lineNumber, String fileName) {
		try {
			if (lineBreakpoint.getLineNumber() == lineNumber
					&& fileName.equals(lineBreakpoint.getMarker().getResource().getLocation().toOSString())) {
				fThread.setBreakpoints(new IBreakpoint[] { lineBreakpoint });
				return true;
			}
		}
		catch (CoreException e) {
			// do nothing
		}
		return false;
	}

	public void breakpointHit(IBreakpoint breakpoint) {
		fThread.setBreakpoints(new IBreakpoint[] { breakpoint });
		suspended(DebugEvent.BREAKPOINT);
	}

	public void getStackFrames() {
		if (isSuspended()) {
			fController.getStackFrames();
		}
	}

	public void getProperties() {
		if (!fTerminated) {
			fController.getProperties();
		}
	}

	@Override
	public void handleDebugEvents(DebugEvent[] events) {
		for (DebugEvent event : events) {
			if (event.getKind() == DebugEvent.TERMINATE && event.getSource().equals(fProcess)) {
				terminated();
			}
		}
	}

	/**
	 * When the breakpoint manager disables, remove all registered breakpoints requests from the VM. When it enables, reinstall them.
	 *
	 * @see org.eclipse.debug.core.IBreakpointManagerListener#breakpointManagerEnablementChanged(boolean)
	 */
	@Override
	public void breakpointManagerEnablementChanged(boolean enabled) {
		for (IBreakpoint breakpoint : DebugPlugin.getDefault().getBreakpointManager().getBreakpoints(IAntDebugConstants.ID_ANT_DEBUG_MODEL)) {
			if (enabled) {
				breakpointAdded(breakpoint);
			} else {
				breakpointRemoved(breakpoint, null);
			}
		}
	}

	public IAntDebugController getAntDebugController() {
		return fController;
	}
}
