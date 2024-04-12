/*******************************************************************************
 * Copyright (c) 2003, 2016 IBM Corporation and others.
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
package org.eclipse.ant.internal.launching.remote.logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.util.StringUtils;
import org.eclipse.ant.internal.launching.debug.AntDebugState;
import org.eclipse.ant.internal.launching.remote.AntSecurityException;
import org.eclipse.ant.internal.launching.remote.IAntCoreConstants;
import org.eclipse.ant.internal.launching.remote.InternalAntRunner;
import org.eclipse.ant.internal.launching.remote.RemoteAntMessages;

/**
 * Parts adapted from org.eclipse.jdt.internal.junit.runner.RemoteTestRunner A build logger that reports via a socket connection. See MessageIds for
 * more information about the protocol.
 */
public class RemoteAntBuildLogger extends DefaultLogger {

	/** Time of the start of the build */
	private final long fStartTime = System.currentTimeMillis();

	/**
	 * The client socket.
	 */
	private Socket fEventSocket;
	/**
	 * Print writer for sending messages
	 */
	private PrintWriter fWriter;
	/**
	 * Host to connect to, default is the localhost
	 */
	protected String fHost = IAntCoreConstants.EMPTY_STRING;
	/**
	 * Port to connect to.
	 */
	private int fEventPort = -1;

	private String fProcessId = null;

	/**
	 * Is the debug mode enabled?
	 */
	protected boolean fDebugMode = false;

	protected boolean fSentProcessId = false;

	private List<BuildEvent> fEventQueue;

	private String fLastFileName = null;
	private String fLastTaskName = null;

	@Override
	protected void printMessage(String message, PrintStream stream, int priority) {
		marshalMessage(priority, message);
	}

	/**
	 * Connect to the remote Ant build listener.
	 */
	protected void connect() {
		if (fDebugMode) {
			System.out.println("RemoteAntBuildLogger: trying to connect" + fHost + ":" + fEventPort); //$NON-NLS-1$ //$NON-NLS-2$
		}

		for (int i = 1; i < 5; i++) {
			try {
				fEventSocket = new Socket(fHost, fEventPort);
				fWriter = new PrintWriter(fEventSocket.getOutputStream(), true);
				return;
			}
			catch (IOException e) {
				// do nothing
			}
			try {
				Thread.sleep(500);
			}
			catch (InterruptedException e) {
				// do nothing
			}
		}
		shutDown();
	}

	/**
	 * Shutdown the connection to the remote build listener.
	 */
	protected void shutDown() {
		if (fEventQueue != null) {
			fEventQueue.clear();
		}
		if (fWriter != null) {
			fWriter.close();
			fWriter = null;
		}

		try {
			if (fEventSocket != null) {
				fEventSocket.close();
				fEventSocket = null;
			}
		}
		catch (IOException e) {
			// do nothing
		}
	}

	private void sendMessage(String msg) {
		if (fWriter == null) {
			return;
		}

		fWriter.println(msg);
	}

	@Override
	public void buildFinished(BuildEvent event) {
		if (!fSentProcessId) {
			establishConnection();
		}
		handleException(event);
		printMessage(getTimeString(System.currentTimeMillis() - fStartTime), out, Project.MSG_INFO);
		shutDown();
	}

	protected void handleException(BuildEvent event) {
		Throwable exception = event.getException();
		if (exception == null || exception instanceof AntSecurityException) {
			return;
		}

		StringBuilder message = new StringBuilder();
		message.append(System.lineSeparator());
		message.append(RemoteAntMessages.getString("RemoteAntBuildLogger.1")); //$NON-NLS-1$
		message.append(System.lineSeparator());
		if (Project.MSG_VERBOSE <= this.msgOutputLevel || !(exception instanceof BuildException)) {
			message.append(StringUtils.getStackTrace(exception));
		} else {
			if (exception instanceof BuildException) {
				message.append(exception.toString()).append(System.lineSeparator());
			} else {
				message.append(exception.getMessage()).append(System.lineSeparator());
			}
		}
		message.append(System.lineSeparator());
		printMessage(message.toString(), out, Project.MSG_ERR);
	}

	private String getTimeString(long milliseconds) {
		long seconds = milliseconds / 1000;
		long minutes = seconds / 60;
		seconds = seconds % 60;

		StringBuilder result = new StringBuilder(RemoteAntMessages.getString("RemoteAntBuildLogger.Total_time")); //$NON-NLS-1$
		if (minutes > 0) {
			result.append(minutes);
			if (minutes > 1) {
				result.append(RemoteAntMessages.getString("RemoteAntBuildLogger._minutes_2")); //$NON-NLS-1$
			} else {
				result.append(RemoteAntMessages.getString("RemoteAntBuildLogger._minute_3")); //$NON-NLS-1$
			}
		}
		if (seconds > 0) {
			if (minutes > 0) {
				result.append(' ');
			}
			result.append(seconds);

			if (seconds > 1) {
				result.append(RemoteAntMessages.getString("RemoteAntBuildLogger._seconds_4")); //$NON-NLS-1$
			} else {
				result.append(RemoteAntMessages.getString("RemoteAntBuildLogger._second_5")); //$NON-NLS-1$
			}
		}
		if (seconds == 0 && minutes == 0) {
			result.append(milliseconds);
			result.append(RemoteAntMessages.getString("RemoteAntBuildLogger._milliseconds_6")); //$NON-NLS-1$
		}
		return result.toString();
	}

	@Override
	public void targetStarted(BuildEvent event) {
		if (!fSentProcessId) {
			establishConnection();
		}

		if (Project.MSG_INFO <= msgOutputLevel) {
			marshalTargetMessage(event);
		}
	}

	protected void establishConnection() {
		if (fEventPort != -1) {
			connect();
		} else {
			shutDown();
			return;
		}

		fSentProcessId = true;
		StringBuilder message = new StringBuilder(MessageIds.PROCESS_ID);
		message.append(fProcessId);
		sendMessage(message.toString());
		if (fEventQueue != null) {
			for (BuildEvent buildEvent : fEventQueue) {
				processEvent(buildEvent);
			}
			fEventQueue = null;
		}
	}

	@SuppressWarnings("unused")
	@Override
	public void messageLogged(BuildEvent event) {
		if (event.getPriority() > msgOutputLevel && event.getPriority() != InternalAntRunner.MSG_PROJECT_HELP) {
			return;
		}

		if (!fSentProcessId) {
			if (event.getPriority() == InternalAntRunner.MSG_PROJECT_HELP) {
				if (Project.MSG_INFO > msgOutputLevel) {
					return;
				}
				// no buildstarted or project started for project help option
				establishConnection();
				return;
			}
			if (fEventQueue == null) {
				fEventQueue = new ArrayList<>(10);
			}
			fEventQueue.add(event);
			return;
		}

		processEvent(event);
	}

	private void processEvent(BuildEvent event) {
		if (event.getTask() != null && !emacsMode) {
			try {
				marshalTaskMessage(event);
			}
			catch (IOException e) {
				// do nothing
			}
		} else {
			marshalMessage(event);
		}
	}

	private void marshalMessage(BuildEvent event) {
		String eventMessage = event.getMessage();
		if (eventMessage.length() == 0) {
			return;
		}
		marshalMessage(event.getPriority(), eventMessage);
	}

	protected void marshalMessage(int priority, String message) {
		try {
			BufferedReader r = new BufferedReader(new StringReader(message));
			String line = r.readLine();
			StringBuilder messageLine;
			while (line != null) {
				messageLine = new StringBuilder();
				if (priority != -1) {
					messageLine.append(priority);
					messageLine.append(',');
				}
				messageLine.append(line);
				sendMessage(messageLine.toString());
				line = r.readLine();
			}
		}
		catch (IOException e) {
			// do nothing
		}
	}

	private void marshalTaskMessage(BuildEvent event) throws IOException {
		String eventMessage = event.getMessage();
		if (eventMessage.length() == 0) {
			return;
		}
		BufferedReader r = new BufferedReader(new StringReader(eventMessage));
		String line = r.readLine();
		StringBuilder message;
		String taskName = event.getTask().getTaskName();
		if (taskName != null && taskName.equals(fLastTaskName)) {
			taskName = IAntCoreConstants.EMPTY_STRING;
		} else {
			fLastTaskName = taskName;
		}
		Location location = event.getTask().getLocation();
		String fileName = null;
		int lineNumber = -1;
		try {
			fileName = location.getFileName();
			lineNumber = location.getLineNumber();
		}
		catch (NoSuchMethodError e) {
			// older Ant
			fileName = location.toString();
		}
		if (location.equals(Location.UNKNOWN_LOCATION)) {
			fileName = location.toString();
			lineNumber = -1;
		}
		int priority = event.getPriority();
		while (line != null) {
			message = new StringBuilder(MessageIds.TASK);
			message.append(priority);
			message.append(',');
			message.append(taskName);
			message.append(',');
			message.append(line.length());
			message.append(',');
			message.append(line);
			message.append(',');
			if (!fileName.equals(fLastFileName)) {
				message.append(fileName.length());
				message.append(',');
				message.append(fileName);
			}
			message.append(',');
			message.append(lineNumber);
			sendMessage(message.toString());
			fLastFileName = fileName;
			line = r.readLine();
		}
	}

	private void marshalTargetMessage(BuildEvent event) {
		Target target = event.getTarget();
		Location location = AntDebugState.getLocation(target);

		StringBuilder message = new StringBuilder();
		message.append(MessageIds.TARGET);
		message.append(',');
		message.append(target.getName());
		message.append(':');
		message.append(',');
		if (location != null && location != Location.UNKNOWN_LOCATION) {
			// if a target has a valid location then we are on an Ant that is
			// new enough to have the accessor methods on Location
			String fileName = location.getFileName();
			message.append(fileName.length());
			message.append(',');
			message.append(fileName);
			message.append(',');
			message.append(location.getLineNumber());
		}
		sendMessage(message.toString());
	}

	@Override
	public void buildStarted(BuildEvent event) {
		establishConnection();
		super.buildStarted(event);
	}

	public void configure(Map<String, String> userProperties) {
		String portProperty = userProperties.remove("eclipse.connect.port"); //$NON-NLS-1$

		if (portProperty != null) {
			fEventPort = Integer.parseInt(portProperty);
		}

		fProcessId = userProperties.remove("org.eclipse.ant.core.ANT_PROCESS_ID"); //$NON-NLS-1$
	}
}