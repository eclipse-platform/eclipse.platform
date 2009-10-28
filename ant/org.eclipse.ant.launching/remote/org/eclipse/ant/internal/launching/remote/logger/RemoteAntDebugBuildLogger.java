/*******************************************************************************
 * Copyright (c) 2003, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.launching.remote.logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Task;
import org.eclipse.ant.internal.launching.debug.AntDebugState;
import org.eclipse.ant.internal.launching.debug.IDebugBuildLogger;
import org.eclipse.ant.internal.launching.debug.model.DebugMessageIds;

/**
 * Parts adapted from org.eclipse.jdt.internal.junit.runner.RemoteTestRunner
 * A build logger that reports via a socket connection.
 * See DebugMessageIds and MessageIds for more information about the protocol.
 */
public class RemoteAntDebugBuildLogger extends RemoteAntBuildLogger implements IDebugBuildLogger {
	
	private ServerSocket fServerSocket;
	private static final int fgServerSocketTimeout= 5000;
	private Socket fRequestSocket;
	
	private PrintWriter fRequestWriter;
	
	private BufferedReader fRequestReader;
	
    private boolean fBuildStartedSuspend= true;
	
	private Task fStepOverTaskInterrupted;
	
	private List fBreakpoints= null;
	
	/**
	 * Request port to connect to.
	 * Used for debug connections
	 */
	private int fRequestPort= -1;
	private AntDebugState fDebugState;

	/**
	 * Reader thread that processes requests from the debug client.
	 */
	private class ReaderThread extends Thread {
		public ReaderThread() {
			super("ReaderThread"); //$NON-NLS-1$
			setDaemon(true);
		}

		public void run(){
			try { 
				String message= null; 
				while (fRequestReader != null) { 
					if ((message= fRequestReader.readLine()) != null) {
						
						if (message.startsWith(DebugMessageIds.STEP_INTO)){
							synchronized(RemoteAntDebugBuildLogger.this) {
								fDebugState.setStepIntoSuspend(true);
								fDebugState.setStepIntoTask(fDebugState.getCurrentTask());
								RemoteAntDebugBuildLogger.this.notifyAll();
							}
						} if (message.startsWith(DebugMessageIds.STEP_OVER)){
							synchronized(RemoteAntDebugBuildLogger.this) {
								fDebugState.stepOver();
							}
						} else if (message.startsWith(DebugMessageIds.SUSPEND)) {
							synchronized(RemoteAntDebugBuildLogger.this) {
								fDebugState.setStepIntoTask(null);
								fDebugState.setStepOverTask(null);
								fStepOverTaskInterrupted= null;
								fDebugState.setClientSuspend(true);
							}
						} else if (message.startsWith(DebugMessageIds.RESUME)) {
							synchronized(RemoteAntDebugBuildLogger.this) {
								fDebugState.setStepIntoTask(null);
								fDebugState.setStepOverTask(null);
								fStepOverTaskInterrupted= null;
								RemoteAntDebugBuildLogger.this.notifyAll();
							}
						} else if (message.startsWith(DebugMessageIds.TERMINATE)) {
						    sendRequestResponse(DebugMessageIds.TERMINATED);
							shutDown();
						} else if (message.startsWith(DebugMessageIds.STACK)) {
							marshallStack();
						} else if (message.startsWith(DebugMessageIds.ADD_BREAKPOINT)) {
							addBreakpoint(message);
						} else if (message.startsWith(DebugMessageIds.REMOVE_BREAKPOINT)) {
							removeBreakpoint(message);
						}  else if (message.startsWith(DebugMessageIds.PROPERTIES)) {
							marshallProperties();
						}
					}
				} 
			} catch (Exception e) {
				RemoteAntDebugBuildLogger.this.shutDown();
			}
		}
	}
	
	private void requestConnect() {
		if (fDebugMode) {
			System.out.println("RemoteAntDebugBuildLogger: trying to connect" + fHost + ":" + fRequestPort); //$NON-NLS-1$ //$NON-NLS-2$
		}
		
		try{
			fServerSocket.setSoTimeout(fgServerSocketTimeout);
			fRequestSocket= fServerSocket.accept();
			fRequestWriter= new PrintWriter(fRequestSocket.getOutputStream(), true);
			fRequestReader = new BufferedReader(new InputStreamReader(fRequestSocket.getInputStream()));
			
			ReaderThread readerThread= new ReaderThread();
			readerThread.setDaemon(true);
			readerThread.start();
			return;
		} catch(SocketTimeoutException e){
		} catch(IOException e) {
		}
		shutDown();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.antsupport.logger.RemoteAntBuildLogger#shutDown()
	 */
	protected void shutDown() {
		if (fRequestWriter != null) {
			fRequestWriter.close();
			fRequestWriter= null;
		}
		
		if (fRequestReader != null) {
			try {
				fRequestReader.close();
			} catch (IOException e) {
			}
			fRequestReader= null;
		}
		
		if (fRequestSocket != null) {
			try {
				fRequestSocket.close();	
			} catch(IOException e) {
			}
		}
		fRequestSocket= null;
		
		super.shutDown();
	}
	
	/* (non-Javadoc)
	 * @see org.apache.tools.ant.BuildListener#buildStarted(org.apache.tools.ant.BuildEvent)
	 */
	public synchronized void buildStarted(BuildEvent event) {
		fDebugState= new AntDebugState(this);
		super.buildStarted(event);
		marshalMessage(-1, DebugMessageIds.BUILD_STARTED);
		if (fRequestPort != -1) {
			try {
				fServerSocket= new ServerSocket(fRequestPort);
			} catch (IOException ioe) {
				shutDown();
			}
			requestConnect();
		} else {
			shutDown();
		}
        fDebugState.buildStarted();
		fDebugState.setShouldSuspend(true);
		waitIfSuspended();
	}

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.BuildListener#taskStarted(org.apache.tools.ant.BuildEvent)
	 */
	public void taskStarted(BuildEvent event) {
        super.taskStarted(event);
		fDebugState.taskStarted(event);
	}
	
	/* (non-Javadoc)
	 * @see org.apache.tools.ant.BuildListener#taskFinished(org.apache.tools.ant.BuildEvent)
	 */
	public synchronized void taskFinished(BuildEvent event) {
		super.taskFinished(event);
		fDebugState.taskFinished();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ant.internal.ui.antsupport.logger.util.IDebugBuildLogger#waitIfSuspended()
	 */
	public synchronized void waitIfSuspended() {
		String detail= null;
		boolean shouldSuspend= true;
		RemoteAntBreakpoint breakpoint= breakpointAtLineNumber(fDebugState.getBreakpointLocation());
		if (breakpoint != null) {
			detail= breakpoint.toMarshallString();
			fDebugState.setShouldSuspend(false);
			if (fDebugState.getStepOverTask() != null) {
				fStepOverTaskInterrupted= fDebugState.getStepOverTask();
				fDebugState.setStepOverTask(null);
			}
		} else if (fDebugState.getCurrentTask() != null) {
	        if (fDebugState.isStepIntoSuspend()) {
	            detail= DebugMessageIds.STEP;
				fDebugState.setStepIntoSuspend(false);
	        } else if ((fDebugState.getLastTaskFinished() != null && fDebugState.getLastTaskFinished() == fDebugState.getStepOverTask()) || fDebugState.shouldSuspend()) {
	        	//suspend as a step over has finished
	        	detail= DebugMessageIds.STEP;
				fDebugState.setStepOverTask(null);
				fDebugState.setShouldSuspend(false);
	        } else if (fDebugState.getLastTaskFinished() != null && fDebugState.getLastTaskFinished() == fDebugState.getStepIntoTask()) {
	        	//suspend as a task that was stepped into has finally completed
	        	 detail= DebugMessageIds.STEP;
	        	 fDebugState.setStepIntoTask(null);
	        } else if (fDebugState.getLastTaskFinished() != null && fDebugState.getLastTaskFinished() == fStepOverTaskInterrupted) {
	        	//suspend as a task that was stepped over but hit a breakpoint has finally completed
	        	 detail= DebugMessageIds.STEP;
	        	 fStepOverTaskInterrupted= null;
	        } else if (fDebugState.isClientSuspend()) {
	            detail= DebugMessageIds.CLIENT_REQUEST;
				fDebugState.setClientSuspend(false);
	        } else {
	            shouldSuspend= false;
	        }
	    } else if (fDebugState.shouldSuspend() && fBuildStartedSuspend) {
            fBuildStartedSuspend= false;
			fDebugState.setShouldSuspend(false);
	    } else {
			shouldSuspend= false;
	    }
		
		if (shouldSuspend) {
			if (detail != null) {
				StringBuffer message= new StringBuffer(DebugMessageIds.SUSPENDED);
				message.append(detail);
				sendRequestResponse(message.toString());
			}
			 try {
				 wait();
                 shouldSuspend= false;
			 } catch (InterruptedException e) {
			 }
		}
	}

	private RemoteAntBreakpoint breakpointAtLineNumber(Location location) {
		if (fBreakpoints == null || location == null || location == Location.UNKNOWN_LOCATION) {
			return null;
		}
		String fileName= fDebugState.getFileName(location);
		int lineNumber= fDebugState.getLineNumber(location);
		for (int i = 0; i < fBreakpoints.size(); i++) {
			RemoteAntBreakpoint breakpoint = (RemoteAntBreakpoint) fBreakpoints.get(i);
			if (breakpoint.isAt(fileName, lineNumber)) {
				return breakpoint;
			}
		}
		return null;
	}

	private void sendRequestResponse(String message) {
		if (fRequestWriter == null) {
			return;
		}
		
		fRequestWriter.println(message);
	}
	
	protected void marshallStack() {
	    StringBuffer stackRepresentation= new StringBuffer();
	    fDebugState.marshalStack(stackRepresentation);
	    sendRequestResponse(stackRepresentation.toString());
	}
	
	protected void marshallProperties() {
	    StringBuffer propertiesRepresentation= new StringBuffer();
		fDebugState.marshallProperties(propertiesRepresentation, true);
	    sendRequestResponse(propertiesRepresentation.toString());
	}
	
	protected void addBreakpoint(String breakpointRepresentation) {
		if (fBreakpoints == null) {
			fBreakpoints= new ArrayList();
		}
		RemoteAntBreakpoint newBreakpoint= new RemoteAntBreakpoint(breakpointRepresentation);
		if (!fBreakpoints.contains(newBreakpoint)) {
			fBreakpoints.add(newBreakpoint);	
		}
	}
	
	protected void removeBreakpoint(String breakpointRepresentation) {
		if (fBreakpoints == null) {
			return;
		} 
		RemoteAntBreakpoint equivalentBreakpoint= new RemoteAntBreakpoint(breakpointRepresentation);
		for (Iterator iter = fBreakpoints.iterator(); iter.hasNext(); ) {
			RemoteAntBreakpoint breakpoint = (RemoteAntBreakpoint) iter.next();
			if (breakpoint.equals(equivalentBreakpoint)) {
				iter.remove();
				return;
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.BuildListener#targetStarted(org.apache.tools.ant.BuildEvent)
	 */
	public void targetStarted(BuildEvent event) {
		fDebugState.targetStarted(event);
		if (!fSentProcessId) {
			establishConnection();
		}
		waitIfSuspended();
		super.targetStarted(event);
	}
    
    /* (non-Javadoc)
     * @see org.apache.tools.ant.BuildListener#targetFinished(org.apache.tools.ant.BuildEvent)
     */
    public void targetFinished(BuildEvent event) {
        super.targetFinished(event);
		fDebugState.setTargetExecuting(null);
    }   
    
    /* (non-Javadoc)
     * @see org.eclipse.ant.internal.ui.antsupport.logger.RemoteAntBuildLogger#configure(java.util.Map)
     */
    public void configure(Map userProperties) {
       super.configure(userProperties);
       String requestPortProperty= (String) userProperties.remove("eclipse.connect.request_port"); //$NON-NLS-1$
        if (requestPortProperty != null) {
            fRequestPort= Integer.parseInt(requestPortProperty);
        }
    }
}
