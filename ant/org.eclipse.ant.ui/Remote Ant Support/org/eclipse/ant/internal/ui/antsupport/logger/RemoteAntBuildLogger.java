/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.antsupport.logger;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.Socket;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.eclipse.ant.internal.ui.antsupport.AntSecurityException;
import org.eclipse.ant.internal.ui.antsupport.InternalAntMessages;
import org.eclipse.ant.internal.ui.antsupport.InternalAntRunner;

/**
 * Parts adapted from org.eclipse.jdt.internal.junit.runner.RemoteTestRunner
 * A build logger that reports via a socket connection.
 * See MessageIds for more information about the protocol.
 */
public class RemoteAntBuildLogger extends DefaultLogger {

	/** Time of the start of the build */
    private long startTime = System.currentTimeMillis();

	/**
	 * The client socket.
	 */
	private Socket fClientSocket;
	/**
	 * Print writer for sending messages
	 */
	private PrintWriter fWriter;
	/**
	 * Host to connect to, default is the localhost
	 */
	private String fHost= ""; //$NON-NLS-1$
	/**
	 * Port to connect to.
	 */
	private int fPort= -1;
	/**
	 * Is the debug mode enabled?
	 */
	private boolean fDebugMode= false;	
	
	private boolean fSentProcessId= false;
	
	private List fEventQueue;
	
	
	/* (non-Javadoc)
	 * @see org.apache.tools.ant.DefaultLogger#printMessage(java.lang.String, java.io.PrintStream, int)
	 */
	protected void printMessage(String message, PrintStream stream, int priority) {
		marshalMessage(priority, message);
	}
	
	/**
	 * Connect to the remote Ant build listener.
	 */
	private void connect() {
		if (fDebugMode) {
			System.out.println("RemoteAntBuildLogger: trying to connect" + fHost + ":" + fPort); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		for (int i= 1; i < 5; i++) {
			try{
				fClientSocket= new Socket(fHost, fPort);
				fWriter= new PrintWriter(fClientSocket.getOutputStream(), true);
				return;
			} catch(IOException e){
			}
			try {
				Thread.sleep(500);
			} catch(InterruptedException e) {
			}
		}
		shutDown();
	}

	/**
	 * Shutdown the connection to the remote build listener.
	 */
	private void shutDown() {
		if (fEventQueue != null) {
			fEventQueue.clear();
		}
		if (fWriter != null) {
			fWriter.close();
			fWriter= null;
		}
		
		try {
			if (fClientSocket != null) {
				fClientSocket.close();
				fClientSocket= null;
			}
		} catch(IOException e) {
		}
	}

	private void sendMessage(String msg) {
		if (fWriter == null) {
			return;
		}
		
		fWriter.println(msg);
	}
	
	/* (non-Javadoc)
	 * @see org.apache.tools.ant.BuildListener#buildFinished(org.apache.tools.ant.BuildEvent)
	 */
	public void buildFinished(BuildEvent event) {
		handleException(event);
        printMessage( getTimeString(System.currentTimeMillis() - startTime), out, Project.MSG_INFO); 
		shutDown();
	}
	
	protected void handleException(BuildEvent event) {
		Throwable exception = event.getException();
		if (exception == null
		|| exception instanceof AntSecurityException) {
			return;
		}
		printMessage(MessageFormat.format(InternalAntMessages.getString("RemoteAntBuildLogger.BUILD_FAILED__{0}_1"), new String[] { exception.toString()}), //$NON-NLS-1$
					out, Project.MSG_ERR);	
	}
	
	private String getTimeString(long milliseconds) {
		long seconds = milliseconds / 1000;
		long minutes = seconds / 60;
		seconds= seconds % 60;

		StringBuffer result= new StringBuffer(InternalAntMessages.getString("RemoteAntBuildLogger.Total_time")); //$NON-NLS-1$
		if (minutes > 0) {
			result.append(minutes);
			if (minutes > 1) {
				result.append(InternalAntMessages.getString("RemoteAntBuildLogger._minutes_2")); //$NON-NLS-1$
			} else {
				result.append(InternalAntMessages.getString("RemoteAntBuildLogger._minute_3")); //$NON-NLS-1$
			}
		}
		if (seconds > 0) {
			if (minutes > 0) {
				result.append(' ');
			}
			result.append(seconds);
	
			if (seconds > 1) {
				result.append(InternalAntMessages.getString("RemoteAntBuildLogger._seconds_4")); //$NON-NLS-1$
			} else {
				result.append(InternalAntMessages.getString("RemoteAntBuildLogger._second_5")); //$NON-NLS-1$
			} 
		}
		if (seconds == 0 && minutes == 0) {
			result.append(milliseconds);
			result.append(InternalAntMessages.getString("RemoteAntBuildLogger._milliseconds_6"));		 //$NON-NLS-1$
		}
		return result.toString();
	}
			

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.BuildListener#targetStarted(org.apache.tools.ant.BuildEvent)
	 */
	public void targetStarted(BuildEvent event) {
		if (!fSentProcessId) {
			establishConnection(event);
		}

		if (Project.MSG_INFO <= msgOutputLevel) {
			String msg= event.getTarget().getName() + ":"; //$NON-NLS-1$
			printMessage(msg, out, Project.MSG_INFO);
        }
	}

	private void establishConnection(BuildEvent event) {
		String portProperty= event.getProject().getProperty("eclipse.connect.port"); //$NON-NLS-1$
		if (portProperty != null) {
			fPort= Integer.parseInt(portProperty);
			connect();
		}
		
		fSentProcessId= true;
		StringBuffer message= new StringBuffer(MessageIds.PROCESS_ID);
		message.append(event.getProject().getProperty("org.eclipse.ant.ui.ATTR_ANT_PROCESS_ID")); //$NON-NLS-1$
		sendMessage(message.toString());
		if (fEventQueue != null) {
			for (Iterator iter = fEventQueue.iterator(); iter.hasNext();) {
				processEvent((BuildEvent)iter.next());
			}
			fEventQueue= null;
		}
	}

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.BuildListener#messageLogged(org.apache.tools.ant.BuildEvent)
	 */
	public void messageLogged(BuildEvent event) {
		if (event.getPriority() > msgOutputLevel && event.getPriority() != InternalAntRunner.MSG_PROJECT_HELP) {
			return;
		}
		
		if (!fSentProcessId) {
			if (event.getPriority() == InternalAntRunner.MSG_PROJECT_HELP) {
				if (Project.MSG_INFO > msgOutputLevel) {
					return;
				}
				//no buildstarted or project started for project help option
				establishConnection(event);
				return;
			}
			if (fEventQueue == null){
				fEventQueue= new ArrayList(10);
			}
			fEventQueue.add(event);
			return;
		}
		
		processEvent(event);
	}

	private void processEvent(BuildEvent event) {
		if (event.getTask() != null & !emacsMode) {
			try {
				marshalTaskMessage(event);
			} catch (IOException e) {
			}
		} else {
			marshalMessage(event);
		}
	}
	
	private void marshalMessage(BuildEvent event) {
		String eventMessage= event.getMessage().trim();
		if (eventMessage.length() == 0) {
			return;
		}
		marshalMessage(event.getPriority(), eventMessage);
	}

	private void marshalMessage(int priority, String message) {
		try {
			BufferedReader r = new BufferedReader(new StringReader(message));
			String line = r.readLine();
			StringBuffer messageLine;
			while (line != null) {
				messageLine= new StringBuffer();
				messageLine.append(priority);
				messageLine.append(',');
				messageLine.append(line);
				sendMessage(messageLine.toString());
				line = r.readLine();
			}
		} catch (IOException e) {
		}
	}

	private void marshalTaskMessage(BuildEvent event) throws IOException {
		String eventMessage= event.getMessage().trim();
		if (eventMessage.length() == 0) {
			return;
		}
		BufferedReader r = new BufferedReader(new StringReader(eventMessage));
		String line = r.readLine();
		StringBuffer message;
		String taskName= event.getTask().getTaskName();
		StringBuffer labelBuff= new StringBuffer();
		labelBuff.append('[');
		labelBuff.append(taskName);
		labelBuff.append("] "); //$NON-NLS-1$
		String label= labelBuff.toString();
		Location location= event.getTask().getLocation();
		int priority= event.getPriority();
		while (line != null) {
			message= new StringBuffer(MessageIds.TASK);
			message.append(priority);
			message.append(',');
			message.append(taskName);
			message.append(',');
			line= (label + line).trim();
			message.append(line.length());
			message.append(',');
			message.append(line);
			message.append(',');
			message.append(location);
			sendMessage(message.toString());
			line = r.readLine();
		}
	}

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.BuildListener#buildStarted(org.apache.tools.ant.BuildEvent)
	 */
	public void buildStarted(BuildEvent event) {
		establishConnection(event);
		super.buildStarted(event);
	}
}