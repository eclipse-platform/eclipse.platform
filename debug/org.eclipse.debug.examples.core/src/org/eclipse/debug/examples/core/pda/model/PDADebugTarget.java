/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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
 *     Bjorn Freeman-Benson - initial API and implementation
 *     Pawel Piech (Wind River) - ported PDA Virtual Machine to Java (Bug 261400)
 *******************************************************************************/
package org.eclipse.debug.examples.core.pda.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.IBreakpointManagerListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.examples.core.pda.DebugCorePlugin;
import org.eclipse.debug.examples.core.pda.breakpoints.PDALineBreakpoint;
import org.eclipse.debug.examples.core.pda.breakpoints.PDARunToLineBreakpoint;
import org.eclipse.debug.examples.core.pda.protocol.PDACommand;
import org.eclipse.debug.examples.core.pda.protocol.PDACommandResult;
import org.eclipse.debug.examples.core.pda.protocol.PDAEvent;
import org.eclipse.debug.examples.core.pda.protocol.PDAEventStopCommand;
import org.eclipse.debug.examples.core.pda.protocol.PDAExitedEvent;
import org.eclipse.debug.examples.core.pda.protocol.PDARestartCommand;
import org.eclipse.debug.examples.core.pda.protocol.PDAStartedEvent;
import org.eclipse.debug.examples.core.pda.protocol.PDATerminateCommand;
import org.eclipse.debug.examples.core.pda.protocol.PDAVMResumeCommand;
import org.eclipse.debug.examples.core.pda.protocol.PDAVMResumedEvent;
import org.eclipse.debug.examples.core.pda.protocol.PDAVMStartedEvent;
import org.eclipse.debug.examples.core.pda.protocol.PDAVMSuspendCommand;
import org.eclipse.debug.examples.core.pda.protocol.PDAVMSuspendedEvent;
import org.eclipse.debug.examples.core.pda.protocol.PDAVMTerminatedEvent;


/**
 * PDA Debug Target
 */
public class PDADebugTarget extends PDADebugElement implements IDebugTarget, IBreakpointManagerListener, IPDAEventListener {

	// associated system process (VM)
	private IProcess fProcess;

	// containing launch object
	private ILaunch fLaunch;

	// sockets to communicate with VM
	private Socket fRequestSocket;
	private PrintWriter fRequestWriter;
	private BufferedReader fRequestReader;
	private Socket fEventSocket;
	private BufferedReader fEventReader;

	// suspended state
	private boolean fVMSuspended = false;

	// terminated state
	private boolean fTerminated = false;

	// threads
	private Map<Integer, PDAThread> fThreads = Collections.synchronizedMap(new LinkedHashMap<>());

	// event dispatch job
	private EventDispatchJob fEventDispatch;

	// event listeners
	private List<IPDAEventListener> fEventListeners = Collections.synchronizedList(new ArrayList<>());

	/**
	 * Listens to events from the PDA VM and fires corresponding
	 * debug events.
	 */
	class EventDispatchJob extends Job {

		public EventDispatchJob() {
			super("PDA Event Dispatch"); //$NON-NLS-1$
			setSystem(true);
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			String message = ""; //$NON-NLS-1$
			while (!isTerminated() && message != null) {
				try {
					message = fEventReader.readLine();
					if (message != null) {
						PDAEvent event = null;
						try {
							event = PDAEvent.parseEvent(message);
						}
						catch (IllegalArgumentException e) {
							DebugCorePlugin.getDefault().getLog().log(
								new Status (IStatus.ERROR, "org.eclipse.debug.examples.core", "Error parsing PDA event", e)); //$NON-NLS-1$ //$NON-NLS-2$
							continue;
						}
						for (Object listener : fEventListeners.toArray()) {
							((IPDAEventListener)listener).handleEvent(event);
						}
					}
				} catch (IOException e) {
					vmTerminated();
				}
			}
			return Status.OK_STATUS;
		}

	}

	/**
	 * Registers the given event listener. The listener will be notified of
	 * events in the program being interpretted. Has no effect if the listener
	 * is already registered.
	 *
	 * @param listener event listener
	 */
	public void addEventListener(IPDAEventListener listener) {
		synchronized(fEventListeners) {
			if (!fEventListeners.contains(listener)) {
				fEventListeners.add(listener);
			}
		}
	}

	/**
	 * Deregisters the given event listener. Has no effect if the listener is
	 * not currently registered.
	 *
	 * @param listener event listener
	 */
	public void removeEventListener(IPDAEventListener listener) {
		fEventListeners.remove(listener);
	}

	/**
	 * Constructs a new debug target in the given launch for the
	 * associated PDA VM process.
	 *
	 * @param launch containing launch
	 * @param process PDA VM
	 * @param requestPort port to send requests to the VM
	 * @param eventPort port to read events from
	 * @exception CoreException if unable to connect to host
	 */
	public PDADebugTarget(ILaunch launch, IProcess process, int requestPort, int eventPort) throws CoreException {
		super(null);
		fLaunch = launch;
		fProcess = process;
		addEventListener(this);
		try {
			// give interpreter a chance to start
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			fRequestSocket = new Socket("localhost", requestPort); //$NON-NLS-1$
			fRequestWriter = new PrintWriter(fRequestSocket.getOutputStream());
			fRequestReader = new BufferedReader(new InputStreamReader(fRequestSocket.getInputStream()));
			// give interpreter a chance to open next socket
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}
			fEventSocket = new Socket("localhost", eventPort); //$NON-NLS-1$
			fEventReader = new BufferedReader(new InputStreamReader(fEventSocket.getInputStream()));
		} catch (UnknownHostException e) {
			requestFailed("Unable to connect to PDA VM", e); //$NON-NLS-1$
		} catch (IOException e) {
			requestFailed("Unable to connect to PDA VM", e); //$NON-NLS-1$
		}
		fEventDispatch = new EventDispatchJob();
		fEventDispatch.schedule();
		IBreakpointManager breakpointManager = getBreakpointManager();
		breakpointManager.addBreakpointListener(this);
		breakpointManager.addBreakpointManagerListener(this);
		// initialize error hanlding to suspend on 'unimplemented instructions'
		// and 'no such label' errors
		sendCommand(new PDAEventStopCommand(PDAEventStopCommand.UNIMPINSTR, true));
		sendCommand(new PDAEventStopCommand(PDAEventStopCommand.NOSUCHLABEL, true));
	}

	@Override
	public IProcess getProcess() {
		return fProcess;
	}
	@Override
	public IThread[] getThreads() throws DebugException {
		synchronized (fThreads) {
			return fThreads.values().toArray(new IThread[fThreads.size()]);
		}
	}
	@Override
	public boolean hasThreads() throws DebugException {
		return fThreads.size() > 0;
	}
	@Override
	public String getName() throws DebugException {
		return "PDA"; //$NON-NLS-1$
	}
	@Override
	public boolean supportsBreakpoint(IBreakpoint breakpoint) {
		if (!isTerminated() && breakpoint.getModelIdentifier().equals(getModelIdentifier())) {
			try {
				String program = getLaunch().getLaunchConfiguration().getAttribute(DebugCorePlugin.ATTR_PDA_PROGRAM, (String)null);
				if (program != null) {
					IResource resource = null;
					if (breakpoint instanceof PDARunToLineBreakpoint) {
						PDARunToLineBreakpoint rtl = (PDARunToLineBreakpoint) breakpoint;
						resource = rtl.getSourceFile();
					} else {
						IMarker marker = breakpoint.getMarker();
						if (marker != null) {
							resource = marker.getResource();
						}
					}
					if (resource != null) {
						IPath p = IPath.fromOSString(program);
						return resource.getFullPath().equals(p);
					}
				}
			} catch (CoreException e) {
			}
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
		return getProcess().canTerminate();
	}

	@Override
	public synchronized boolean isTerminated() {
		return fTerminated || getProcess().isTerminated();
	}

	@Override
	public void terminate() throws DebugException {
//#ifdef ex2
//#     // TODO: Exercise 2 - send termination request to interpreter
//#else
		sendCommand(new PDATerminateCommand());
//#endif
	}

	@Override
	public boolean canResume() {
		return !isTerminated() && isSuspended();
	}

	@Override
	public boolean canSuspend() {
		return !isTerminated() && !isSuspended();
	}

	@Override
	public synchronized boolean isSuspended() {
		return !isTerminated() && fVMSuspended;
	}

	@Override
	public void resume() throws DebugException {
		sendCommand(new PDAVMResumeCommand());
	}

	@Override
	public void suspend() throws DebugException {
		sendCommand(new PDAVMSuspendCommand());
	}

	@Override
	public void breakpointAdded(IBreakpoint breakpoint) {
		if (supportsBreakpoint(breakpoint)) {
			try {
				if ((breakpoint.isEnabled() && getBreakpointManager().isEnabled()) || !breakpoint.isRegistered()) {
					PDALineBreakpoint pdaBreakpoint = (PDALineBreakpoint)breakpoint;
					pdaBreakpoint.install(this);
				}
			} catch (CoreException e) {
			}
		}
	}

	@Override
	public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
		if (supportsBreakpoint(breakpoint)) {
			try {
				PDALineBreakpoint pdaBreakpoint = (PDALineBreakpoint)breakpoint;
				pdaBreakpoint.remove(this);
			} catch (CoreException e) {
			}
		}
	}

	@Override
	public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
		if (supportsBreakpoint(breakpoint)) {
			try {
				if (breakpoint.isEnabled() && getBreakpointManager().isEnabled()) {
					breakpointAdded(breakpoint);
				} else {
					breakpointRemoved(breakpoint, null);
				}
			} catch (CoreException e) {
			}
		}
	}

	@Override
	public boolean canDisconnect() {
		return false;
	}

	@Override
	public void disconnect() throws DebugException {
	}

	@Override
	public boolean isDisconnected() {
		return false;
	}

	@Override
	public boolean supportsStorageRetrieval() {
		return true;
	}

	@Override
	public IMemoryBlock getMemoryBlock(long startAddress, long length) throws DebugException {
		return new PDAMemoryBlock(this, startAddress, length);
	}

	/**
	 * Notification we have connected to the VM and it has started.
	 * Resume the VM.
	 * @param event
	 */
	private void vmStarted(PDAVMStartedEvent event) {
		fireCreationEvent();
		installDeferredBreakpoints();
		try {
			resume();
		} catch (DebugException e) {
		}
	}

	/**
	 * Install breakpoints that are already registered with the breakpoint
	 * manager.
	 */
	private void installDeferredBreakpoints() {
		for (IBreakpoint breakpoint : getBreakpointManager().getBreakpoints(getModelIdentifier())) {
			breakpointAdded(breakpoint);
		}
	}

	/**
	 * Called when this debug target terminates.
	 */
	private void vmTerminated() {
		setTerminated(true);
		fThreads.clear();
		IBreakpointManager breakpointManager = getBreakpointManager();
		breakpointManager.removeBreakpointListener(this);
		breakpointManager.removeBreakpointManagerListener(this);
		fireTerminateEvent();
		removeEventListener(this);
	}

	private void vmResumed(PDAVMResumedEvent event) {
		setVMSuspended(false);
		fireResumeEvent(calcDetail(event.fReason));
	}

	private void vmSuspended(PDAVMSuspendedEvent event) {
		setVMSuspended(true);
		fireSuspendEvent(calcDetail(event.fReason));
	}

	private int calcDetail(String reason) {
		if (reason.equals("breakpoint") || reason.equals("watch")) { //$NON-NLS-1$ //$NON-NLS-2$
			return DebugEvent.BREAKPOINT;
		} else if (reason.equals("step")) { //$NON-NLS-1$
			return DebugEvent.STEP_OVER;
		} else if (reason.equals("drop")) { //$NON-NLS-1$
			return DebugEvent.STEP_RETURN;
		} else if (reason.equals("client")) { //$NON-NLS-1$
			return DebugEvent.CLIENT_REQUEST;
		} else if (reason.equals("event")) { //$NON-NLS-1$
			return DebugEvent.BREAKPOINT;
		} else {
			return DebugEvent.UNSPECIFIED;
		}
	}

	private void started(PDAStartedEvent event) {
		PDAThread newThread = new PDAThread(this, event.fThreadId);
		fThreads.put(Integer.valueOf(event.fThreadId), newThread);
		newThread.start();
	}

	private void exited(PDAExitedEvent event) {
		PDAThread thread = fThreads.remove(Integer.valueOf(event.fThreadId));
		if (thread != null) {
			thread.exit();
		}
	}

	private synchronized void setVMSuspended(boolean suspended) {
		fVMSuspended = suspended;
	}

	private synchronized void setTerminated(boolean terminated) {
		fTerminated = terminated;
	}

	private String sendRequest(String request) throws DebugException {
		synchronized (fRequestSocket) {
			fRequestWriter.println(request);
			fRequestWriter.flush();
			try {
				// wait for reply
				String retVal = fRequestReader.readLine();
				if (retVal == null) {
					requestFailed("Request failed: " + request + ".  Debugger connection closed.", null);				     //$NON-NLS-1$ //$NON-NLS-2$
				}
				return retVal;
			} catch (IOException e) {
				requestFailed("Request failed: " + request, e); //$NON-NLS-1$
			}
		}
		// Should never reach this satement.
		return null;
	}

	@Override
	public PDACommandResult sendCommand(PDACommand command) throws DebugException {
		String response = sendRequest(command.getRequest());
		return command.createResult(response);
	}

	/**
	 * When the breakpoint manager disables, remove all registered breakpoints
	 * requests from the VM. When it enables, reinstall them.
	 */
	@Override
	public void breakpointManagerEnablementChanged(boolean enabled) {
		for (IBreakpoint breakpoint : getBreakpointManager().getBreakpoints(getModelIdentifier())) {
			if (enabled) {
				breakpointAdded(breakpoint);
			} else {
				breakpointRemoved(breakpoint, null);
			}
		}
	}

	@Override
	public void handleEvent(PDAEvent event) {
		if (event instanceof PDAStartedEvent) {
			started((PDAStartedEvent)event);
		} else if (event instanceof PDAExitedEvent) {
			exited((PDAExitedEvent)event);
		} else if (event instanceof PDAVMStartedEvent) {
			vmStarted((PDAVMStartedEvent)event);
		} else if (event instanceof PDAVMTerminatedEvent) {
			vmTerminated();
		} else if (event instanceof PDAVMSuspendedEvent) {
			vmSuspended((PDAVMSuspendedEvent)event);
		} else if (event instanceof PDAVMResumedEvent) {
			vmResumed((PDAVMResumedEvent)event);
		}
	}

	/**
	 * Returns this debug target's single thread, or <code>null</code>
	 * if terminated.
	 *
	 * @param threadId ID of the thread to return, or <code>0</code>
	 * to return the first available thread
	 * @return this debug target's single thread, or <code>null</code>
	 * if terminated
	 */
	public PDAThread getThread(int threadId) {
		if (threadId > 0) {
			return fThreads.get(Integer.valueOf(threadId));
		} else {
			synchronized(fThreads) {
				if (fThreads.size() > 0) {
					return fThreads.values().iterator().next();
				}
			}
		}
		return null;
	}

	/**
	 * Restarts the current debug session
	 *
	 * @throws DebugException
	 */
	public void restart() throws DebugException {
		sendCommand(new PDARestartCommand());
	}

}
