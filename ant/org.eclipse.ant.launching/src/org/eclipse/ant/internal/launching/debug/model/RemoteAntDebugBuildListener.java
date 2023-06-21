/*******************************************************************************
 * Copyright (c) 2003, 2013 IBM Corporation and others.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import org.eclipse.ant.internal.launching.AntLaunching;
import org.eclipse.ant.internal.launching.debug.IAntDebugController;
import org.eclipse.ant.internal.launching.launchConfigurations.RemoteAntBuildListener;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.ILineBreakpoint;
import org.eclipse.debug.core.model.IProcess;

public class RemoteAntDebugBuildListener extends RemoteAntBuildListener implements IAntDebugController {

	// sockets to communicate with the remote Ant debug build logger
	private Socket fRequestSocket;
	private PrintWriter fRequestWriter;
	private volatile BufferedReader fResponseReader;

	private int fRequestPort = -1;
	private Thread fReaderThread;

	private AntDebugTarget fTarget;

	/**
	 * Reader thread that processes request responses from the remote Ant debug build logger
	 */
	private class ReaderThread extends Thread {
		public ReaderThread() {
			super("Ant Request Response Reader Thread"); //$NON-NLS-1$
			setDaemon(true);
		}

		@Override
		public void run() {
			try {
				String message = null;
				while (fResponseReader != null) {
					BufferedReader reader;
					synchronized (RemoteAntDebugBuildListener.this) {
						reader = fResponseReader;
					}
					if (reader != null && (message = reader.readLine()) != null) {
						synchronized (RemoteAntDebugBuildListener.this) {
							if (fResponseReader != null) {
								receiveMessage(message);
							}
						}
					}
				}
			}
			catch (IOException ie) { // the other end has shutdown
				RemoteAntDebugBuildListener.this.shutDown();
			}
			catch (Exception e) {
				AntLaunching.log("Internal error processing remote response", e); //$NON-NLS-1$
				RemoteAntDebugBuildListener.this.shutDown();
			}
		}
	}

	/**
	 * Constructor
	 *
	 * @param launch
	 *            the backing launch to listen to
	 * @param encoding
	 *            the encoding to use for communications
	 */
	public RemoteAntDebugBuildListener(ILaunch launch, String encoding) {
		super(launch, encoding);
		// fDebug= true;
	}

	@Override
	protected void receiveMessage(String message) {
		if (message.startsWith(DebugMessageIds.BUILD_STARTED)) {
			buildStarted();
		} else if (message.startsWith(DebugMessageIds.SUSPENDED)) {
			handleSuspendMessage(message);
		} else if (message.startsWith(DebugMessageIds.TERMINATED)) {
			try {
				fTarget.terminate();
			}
			catch (DebugException e) {
				// do nothing
			}
		} else if (message.startsWith(DebugMessageIds.STACK)) {
			AntThread thread = (AntThread) fTarget.getThreads()[0];
			thread.buildStack(message);
		} else if (message.startsWith(DebugMessageIds.PROPERTIES)) {
			AntThread thread = (AntThread) fTarget.getThreads()[0];
			thread.newProperties(message);
		} else {
			super.receiveMessage(message);
		}
	}

	private void handleSuspendMessage(String message) {
		if (message.endsWith(DebugMessageIds.CLIENT_REQUEST)) {
			fTarget.suspended(DebugEvent.CLIENT_REQUEST);
		} else if (message.endsWith(DebugMessageIds.STEP)) {
			fTarget.suspended(DebugEvent.STEP_END);
		} else if (message.contains(DebugMessageIds.BREAKPOINT)) {
			fTarget.breakpointHit(message);
		}
	}

	private void buildStarted() {
		IProcess process = getProcess();
		while (process == null) {
			try {
				synchronized (this) {
					wait(400);
				}
				process = getProcess();
			}
			catch (InterruptedException ie) {
				// do nothing
			}
		}
		fTarget = new AntDebugTarget(fLaunch, process, this);
		fLaunch.addDebugTarget(fTarget);

		if (!connectRequest()) {
			RemoteAntDebugBuildListener.this.shutDown();
			return;
		}

		fTarget.buildStarted();
	}

	private boolean connectRequest() {
		Exception exception = null;
		for (int i = 1; i < 20; i++) {
			try {
				fRequestSocket = new Socket("localhost", fRequestPort); //$NON-NLS-1$
				fRequestWriter = new PrintWriter(fRequestSocket.getOutputStream(), true);
				fResponseReader = new BufferedReader(new InputStreamReader(fRequestSocket.getInputStream(), getEncoding()));

				fReaderThread = new ReaderThread();
				fReaderThread.start();
				return true;
			}
			catch (UnknownHostException e) {
				exception = e;
				break;
			}
			catch (IOException e) {
				exception = e;
			}
			try {
				Thread.sleep(500);
			}
			catch (InterruptedException e) {
				// do nothing
			}
		}
		AntLaunching.log("Internal error attempting to connect to debug target", exception); //$NON-NLS-1$
		return false;
	}

	/**
	 * Start listening to an Ant build. Start a server connection that the RemoteAntDebugBuildLogger can connect to.
	 *
	 * @param eventPort
	 *            The port number to create the server connection on
	 * @param requestPort
	 *            The port number to use for sending requests to the remote logger
	 */
	public synchronized void startListening(int eventPort, int requestPort) {
		super.startListening(eventPort);
		fRequestPort = requestPort;
	}

	/**
	 * Sends a request to the Ant build
	 *
	 * @param request
	 *            debug command
	 */
	protected void sendRequest(String request) {
		if (fRequestWriter == null) {
			return;
		}
		synchronized (fRequestWriter) {
			fRequestWriter.println(request);
		}
	}

	@Override
	protected synchronized void shutDown() {
		if (fTarget != null) {
			try {
				fTarget.terminate();
				fTarget = null;
			}
			catch (DebugException e) {
				// do nothing
			}
		}
		fLaunch = null;
		if (DebugPlugin.getDefault() != null) {
			DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(this);
		}
		try {
			if (fReaderThread != null) {
				// interrupt reader thread so that we don't block on close
				// on a lock held by the BufferedReader
				// see bug: 38955
				fReaderThread.interrupt();
			}
			if (fResponseReader != null) {
				fResponseReader.close();
				fResponseReader = null;
			}
		}
		catch (IOException e) {
			// do nothing
		}
		if (fRequestWriter != null) {
			fRequestWriter.close();
			fRequestWriter = null;
		}
		try {
			if (fRequestSocket != null) {
				fRequestSocket.close();
				fRequestSocket = null;
			}
		}
		catch (IOException e) {
			// do nothing
		}
		super.shutDown();
	}

	@Override
	public void resume() {
		sendRequest(DebugMessageIds.RESUME);
	}

	@Override
	public void terminate() {
		// do nothing
	}

	@Override
	public void suspend() {
		sendRequest(DebugMessageIds.SUSPEND);
	}

	@Override
	public void stepInto() {
		sendRequest(DebugMessageIds.STEP_INTO);
	}

	@Override
	public void stepOver() {
		sendRequest(DebugMessageIds.STEP_OVER);
	}

	@Override
	public void handleBreakpoint(IBreakpoint breakpoint, boolean add) {
		if (fTarget == null || !fTarget.supportsBreakpoint(breakpoint)) {
			return;
		}
		StringBuilder message = new StringBuilder();
		if (add) {
			try {
				if (!breakpoint.isEnabled()) {
					return;
				}
			}
			catch (CoreException e) {
				AntLaunching.log(e);
				return;
			}
			message.append(DebugMessageIds.ADD_BREAKPOINT);
		} else {
			message.append(DebugMessageIds.REMOVE_BREAKPOINT);
		}
		message.append(DebugMessageIds.MESSAGE_DELIMITER);
		message.append(breakpoint.getMarker().getResource().getLocation().toOSString());
		message.append(DebugMessageIds.MESSAGE_DELIMITER);
		try {
			message.append(((ILineBreakpoint) breakpoint).getLineNumber());
			sendRequest(message.toString());
		}
		catch (CoreException ce) {
			AntLaunching.log(ce);
		}
	}

	@Override
	public void getProperties() {
		sendRequest(DebugMessageIds.PROPERTIES);
	}

	@Override
	public void getStackFrames() {
		sendRequest(DebugMessageIds.STACK);
	}

	@Override
	public StringBuffer unescapeString(StringBuffer property) {
		if (property.indexOf("\\r") == -1 && property.indexOf("\\n") == -1) { //$NON-NLS-1$ //$NON-NLS-2$
			return property;
		}
		for (int i = 0; i < property.length(); i++) {
			if ('\\' == property.charAt(i)) {
				String newString = ""; //$NON-NLS-1$
				if ('r' == property.charAt(i + 1)) {
					if (i - 1 > -1 && '\\' == property.charAt(i - 1)) {
						newString = "r"; //$NON-NLS-1$
					} else {
						newString += '\r';
					}
				} else if ('n' == property.charAt(i + 1)) {
					if (i - 1 > -1 && '\\' == property.charAt(i - 1)) {
						newString = "n"; //$NON-NLS-1$
					} else {
						newString += '\n';
					}

				}
				if (newString.length() > 0) {
					property.replace(i, i + 2, newString);
				}
			}
		}

		return property;
	}
}
