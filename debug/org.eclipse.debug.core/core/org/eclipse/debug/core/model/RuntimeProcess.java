/*******************************************************************************
 * Copyright (c) 2000, 2021 IBM Corporation and others.
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
package org.eclipse.debug.core.model;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.internal.core.DebugCoreMessages;
import org.eclipse.debug.internal.core.NullStreamsProxy;
import org.eclipse.debug.internal.core.StreamsProxy;

/**
 * Standard implementation of an <code>IProcess</code> that wrappers a system
 * process (<code>java.lang.Process</code>).
 * <p>
 * Clients may subclass this class. Clients that need to replace the implementation
 * of a streams proxy associated with an <code>IProcess</code> should subclass this
 * class. Generally clients should not instantiate this class directly, but should
 * instead call <code>DebugPlugin.newProcess(...)</code>, which can delegate to an
 * <code>IProcessFactory</code> if one is referenced by the associated launch configuration.
 * </p>
 * @see org.eclipse.debug.core.model.IProcess
 * @see org.eclipse.debug.core.IProcessFactory
 * @since 3.0
 */
public class RuntimeProcess extends PlatformObject implements IProcess {

	private static final int TERMINATION_TIMEOUT = 5000; // ms

	/**
	 * The launch this process is contained in
	 */
	private ILaunch fLaunch;

	/**
	 * The system process represented by this <code>IProcess</code>
	 */
	private Process fProcess;

	/**
	 * This process's exit value.
	 *
	 * synchronized by this
	 */
	private int fExitValue;

	/**
	 * The monitor which listens for this runtime process' system process
	 * to terminate.
	 */
	private final ProcessMonitorThread fMonitor;

	/**
	 * The streams proxy for this process
	 */
	private final IStreamsProxy fStreamsProxy;

	/**
	 * The name of the process
	 */
	private final String fName;

	/**
	 * Whether this process has been terminated
	 */
	private boolean fTerminated;

	/**
	 * Table of client defined attributes
	 */
	private final Map<String, String> fAttributes = new ConcurrentHashMap<>();

	/**
	 * Whether output from the process should be captured or swallowed
	 */
	private final boolean fCaptureOutput;

	/**
	 * Whether the descendants of this process should be terminated too
	 */
	private final boolean fTerminateDescendants;

	private final String fThreadNameSuffix;

	/**
	 * Constructs a RuntimeProcess on the given system process
	 * with the given name, adding this process to the given
	 * launch.
	 *
	 * @param launch the parent launch of this process
	 * @param process underlying system process
	 * @param name the label used for this process
	 * @param attributes map of attributes used to initialize the attributes
	 *   of this process, or <code>null</code> if none
	 */
	public RuntimeProcess(ILaunch launch, Process process, String name, Map<String, String> attributes) {
		setLaunch(launch);
		initializeAttributes(attributes);
		fProcess = process;
		try {
			setAttribute(IProcess.ATTR_PROCESS_ID, Long.toString(process.pid()));
		} catch (UnsupportedOperationException e) {
			// ignore, pid() is not implemented in this JVM
		}
		fName = name;
		fTerminated = true;
		try {
			fExitValue = process.exitValue();
		} catch (IllegalThreadStateException e) {
			fTerminated = false;
		}

		String captureOutput = launch.getAttribute(DebugPlugin.ATTR_CAPTURE_OUTPUT);
		fCaptureOutput = !("false".equals(captureOutput)); //$NON-NLS-1$

		boolean terminateDescendants = true;
		try {
			ILaunchConfiguration launchConfiguration = launch.getLaunchConfiguration();
			if (launchConfiguration != null) {
				terminateDescendants = launchConfiguration.getAttribute(DebugPlugin.ATTR_TERMINATE_DESCENDANTS, true);
			}
		} catch (CoreException e) {
			DebugPlugin.log(e);
		}
		fTerminateDescendants = terminateDescendants;
		fThreadNameSuffix = getPidInfo(process, launch);

		fStreamsProxy = createStreamsProxy();
		fMonitor = new ProcessMonitorThread(fThreadNameSuffix);
		// Process must be added to launch (to register for notifications)
		// before starting the monitor thread as otherwise the process may
		// terminate and generate notifications before they can properly be
		// processed.
		// See https://github.com/eclipse-platform/eclipse.platform/issues/598
		launch.addProcess(this);
		fMonitor.start();
		fireCreationEvent();
	}

	private static String getPidInfo(Process process, ILaunch launch) {
		String pid;
		ILaunchConfiguration lc = launch == null ? null : launch.getLaunchConfiguration();
		String name = lc == null ? "" : " " + lc.getName(); //$NON-NLS-1$ //$NON-NLS-2$
		try {
			pid = " for PID " + process.pid(); //$NON-NLS-1$
		} catch (Exception e) {
			pid = ""; //$NON-NLS-1$
		}
		return pid + name;
	}
	/**
	 * Initialize the attributes of this process to those in the given map.
	 *
	 * @param attributes attribute map or <code>null</code> if none
	 */
	private void initializeAttributes(Map<String, String> attributes) {
		if (attributes != null) {
			attributes.forEach(this::setAttribute);
		}
	}

	/**
	 * @see ITerminate#canTerminate()
	 */
	@Override
	public synchronized boolean canTerminate() {
		return !fTerminated;
	}

	/**
	 * @see IProcess#getLabel()
	 */
	@Override
	public String getLabel() {
		return fName;
	}

	/**
	 * Sets the launch this process is contained in
	 *
	 * @param launch the launch this process is contained in
	 */
	protected void setLaunch(ILaunch launch) {
		fLaunch = launch;
	}

	/**
	 * @see IProcess#getLaunch()
	 */
	@Override
	public ILaunch getLaunch() {
		return fLaunch;
	}

	/**
	 * Returns the underlying system process associated with this process.
	 *
	 * @return system process
	 */
	protected Process getSystemProcess() {
		return fProcess;
	}

	/**
	 * @see ITerminate#isTerminated()
	 */
	@Override
	public synchronized boolean isTerminated() {
		return fTerminated;
	}

	/**
	 * @see ITerminate#terminate()
	 */
	@Override
	public void terminate() throws DebugException {
		if (!isTerminated()) {
			try {
				Process process = getSystemProcess();
				if (process == null) {
					return;
				}

				List<ProcessHandle> descendants = Collections.emptyList();
				if (fTerminateDescendants) {
					try { // List of descendants of process is only a snapshot!
						descendants = process.descendants().collect(Collectors.toList());
					} catch (UnsupportedOperationException e) {
						// JVM may not support toHandle() -> assume no
						// descendants
					}
				}

				process.destroy();
				descendants.forEach(ProcessHandle::destroy);

				// await termination of process and descendants
				try { // (in total don't wait longer than TERMINATION_TIMEOUT)
					long waitStart = System.currentTimeMillis();
					if (process.waitFor(TERMINATION_TIMEOUT, TimeUnit.MILLISECONDS)) {
						synchronized (this) {
							fExitValue = process.exitValue();
						}
						if (waitFor(descendants, waitStart)) {
							return;
						}
					}
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			} finally {
				if (fStreamsProxy instanceof StreamsProxy) {
					((StreamsProxy) fStreamsProxy).kill();
				}
			}

			// clean-up
			fMonitor.killThread();
			IStatus status = new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugException.TARGET_REQUEST_FAILED, DebugCoreMessages.RuntimeProcess_terminate_failed, null);
			throw new DebugException(status);
		}
	}

	/**
	 * Awaits the termination of the processes of the given ProcessHandles.
	 * <p>
	 * If all of the specified processes terminate before {@code waitStart} +
	 * {@link #TERMINATION_TIMEOUT} this methods returns {@code true}. If any
	 * process has not terminated until the so specified timeout this methods
	 * aborts waiting and returns {@code false}.
	 * </p>
	 *
	 * @param descendants the list of handles to the processes to await
	 * @param waitStart the time when await of the process termination started
	 * @return true if each process has terminated (before timeout), else false
	 * @throws InterruptedException if the current thread was interrupted while
	 *             waiting
	 */
	private boolean waitFor(List<ProcessHandle> descendants, long waitStart) throws InterruptedException {
		try {
			for (ProcessHandle handle : descendants) {
				long remainingTime = TERMINATION_TIMEOUT - (System.currentTimeMillis() - waitStart);
				// await termination of this descendant
				handle.onExit().get(remainingTime, TimeUnit.MILLISECONDS);
			}
			return true;
		} catch (ExecutionException e) { // should not happen
			throw new IllegalStateException(e.getCause());
		} catch (TimeoutException e) {
			return false; // any sub-processes timed out
		}
	}

	/**
	 * Notification that the system process associated with this process
	 * has terminated.
	 */
	protected void terminated() {
		setAttribute(DebugPlugin.ATTR_TERMINATE_TIMESTAMP, Long.toString(System.currentTimeMillis()));

		if (fStreamsProxy instanceof StreamsProxy) {
			((StreamsProxy)fStreamsProxy).close();
		}


		// Avoid calling IProcess.exitValue() inside a sync section (Bug 311813).
		int exitValue = -1;
		boolean running = false;
		try {
			exitValue = fProcess.exitValue();
		} catch (IllegalThreadStateException ie) {
			running = true;
		}

		synchronized (this) {
			fTerminated= true;
			if (!running) {
				fExitValue = exitValue;
			}
			fProcess= null;
		}
		fireTerminateEvent();
	}

	/**
	 * @see IProcess#getStreamsProxy()
	 */
	@Override
	public IStreamsProxy getStreamsProxy() {
		if (!fCaptureOutput) {
			return null;
		}
		return fStreamsProxy;
	}

	/**
	 * Creates and returns the streams proxy associated with this process.
	 *
	 * @return streams proxy
	 */
	protected IStreamsProxy createStreamsProxy() {
		if (!fCaptureOutput) {
			return new NullStreamsProxy(getSystemProcess());
		}
		String encoding = getLaunch().getAttribute(DebugPlugin.ATTR_CONSOLE_ENCODING);
		Charset charset = null;
		if (encoding != null) {
			try {
				charset = Charset.forName(encoding);
			} catch (UnsupportedCharsetException | IllegalCharsetNameException e) {
				DebugPlugin.log(e);
			}
		}
		if (charset == null) {
			charset = Platform.getSystemCharset();
		}
		return new StreamsProxy(getSystemProcess(), charset, fThreadNameSuffix);
	}

	/**
	 * Fires a creation event.
	 */
	protected void fireCreationEvent() {
		fireEvent(new DebugEvent(this, DebugEvent.CREATE));
	}

	/**
	 * Fires the given debug event.
	 *
	 * @param event debug event to fire
	 */
	protected void fireEvent(DebugEvent event) {
		DebugPlugin manager= DebugPlugin.getDefault();
		if (manager != null) {
			manager.fireDebugEventSet(new DebugEvent[]{event});
		}
	}

	/**
	 * Fires a terminate event.
	 */
	protected void fireTerminateEvent() {
		fireEvent(new DebugEvent(this, DebugEvent.TERMINATE));
	}

	/**
	 * Fires a change event.
	 */
	protected void fireChangeEvent() {
		fireEvent(new DebugEvent(this, DebugEvent.CHANGE));
	}

	/**
	 * @see IProcess#setAttribute(String, String)
	 */
	@Override
	public void setAttribute(String key, String value) {
		Objects.requireNonNull(key);
		if (value == null) {
			// ConcurrentHashMap does not allow null values
			if (fAttributes.remove(key) != null) {
				fireChangeEvent();
			}
		} else {
			String origVal = fAttributes.put(key, value);
			if (!Objects.equals(origVal, value)) {
				fireChangeEvent();
			}
		}
	}

	/**
	 * @see IProcess#getAttribute(String)
	 */
	@Override
	public String getAttribute(String key) {
		return fAttributes.get(key);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter.equals(IProcess.class)) {
			return (T) this;
		}
		if (adapter.equals(IDebugTarget.class)) {
			ILaunch launch = getLaunch();
			IDebugTarget[] targets = launch.getDebugTargets();
			for (IDebugTarget target : targets) {
				if (this.equals(target.getProcess())) {
					return (T) target;
				}
			}
			return null;
		}
		if (adapter.equals(ILaunch.class)) {
			return (T) getLaunch();
		}
		//CONTEXTLAUNCHING
		if(adapter.equals(ILaunchConfiguration.class)) {
			return (T) getLaunch().getLaunchConfiguration();
		}
		return super.getAdapter(adapter);
	}

	/**
	 * @see IProcess#getExitValue()
	 */
	@Override
	public synchronized int getExitValue() throws DebugException {
		if (isTerminated()) {
			return fExitValue;
		}
		throw new DebugException(new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugException.TARGET_REQUEST_FAILED, DebugCoreMessages.RuntimeProcess_Exit_value_not_available_until_process_terminates__1, null));
	}

	/**
	 * Monitors a system process, waiting for it to terminate, and
	 * then notifies the associated runtime process.
	 */
	private class ProcessMonitorThread extends Thread {

		/**
		 * Whether the thread has been told to exit.
		 */
		private volatile boolean fExit;

		/**
		 * @see Thread#run()
		 */
		@Override
		public void run() {
			Process fOSProcess = RuntimeProcess.this.getSystemProcess();
			if (!fExit && fOSProcess != null) {
				try {
					fOSProcess.waitFor();
				} catch (InterruptedException ie) {
					// clear interrupted state
					Thread.interrupted();
				} finally {
					RuntimeProcess.this.terminated();
				}
			}
		}

		/**
		 * Creates a new process monitor and starts monitoring the process for
		 * termination.
		 *
		 * @param suffix Thread name suffix
		 */
		private ProcessMonitorThread(String suffix) {
			super(DebugCoreMessages.ProcessMonitorJob_0 + suffix);
			setDaemon(true);
		}

		/**
		 * Kills the monitoring thread.
		 *
		 * This method is to be useful for dealing with the error
		 * case of an underlying process which has not informed this
		 * monitor of its termination.
		 */
		private void killThread() {
			fExit = true;
			this.interrupt(); // ignored if monitor thread is not yet running
		}
	}
}
