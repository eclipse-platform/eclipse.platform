/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.tests.core.support.testloggers;


import java.io.PrintStream;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildLogger;
import org.apache.tools.ant.Project;
import org.eclipse.ant.core.AntSecurityException;
import org.eclipse.ant.tests.core.testplugin.AntTestChecker;
import org.eclipse.core.runtime.OperationCanceledException;

public class TestBuildLogger implements BuildLogger {

	private int fMessageOutputLevel= Project.MSG_INFO;
	private PrintStream fErr= null;
	private PrintStream fOut= null;
    private boolean fSetProperties= true;
	
	/**
	 * An exception that has already been logged.
	 */
	private Throwable fHandledException= null;
	
	
	public TestBuildLogger() {
	}
	
	/**
	 * @see org.apache.tools.ant.BuildLogger#setMessageOutputLevel(int)
	 */
	public void setMessageOutputLevel(int level) {
		fMessageOutputLevel= level;
	}
	
	protected int getMessageOutputLevel() {
		return fMessageOutputLevel;
	}

	/**
	 * @see org.apache.tools.ant.BuildLogger#setEmacsMode(boolean)
	 */
	public void setEmacsMode(boolean emacsMode) {
	}

	/**
	 * @see org.apache.tools.ant.BuildListener#buildStarted(org.apache.tools.ant.BuildEvent)
	 */
	public void buildStarted(BuildEvent event) {
		AntTestChecker.getDefault().buildStarted(event.getProject().getName());
	}

	/**
	 * @see org.apache.tools.ant.BuildListener#buildFinished(org.apache.tools.ant.BuildEvent)
	 */
	public void buildFinished(BuildEvent event) {
		handleException(event);
		fHandledException= null;
		AntTestChecker.getDefault().buildFinished();
	}

	/**
	 * @see org.apache.tools.ant.BuildListener#targetStarted(org.apache.tools.ant.BuildEvent)
	 */
	public void targetStarted(BuildEvent event) {
		AntTestChecker.getDefault().targetStarted(event.getTarget().getName());
        if (fSetProperties) {
            fSetProperties= false;
            AntTestChecker.getDefault().setUserProperties(event.getProject().getProperties());
        }
	}

	/**
	 * @see org.apache.tools.ant.BuildListener#targetFinished(org.apache.tools.ant.BuildEvent)
	 */
	public void targetFinished(BuildEvent event) {
		handleException(event);
		AntTestChecker.getDefault().targetFinished();
	}

	/**
	 * @see org.apache.tools.ant.BuildListener#taskStarted(org.apache.tools.ant.BuildEvent)
	 */
	public void taskStarted(BuildEvent event) {
		AntTestChecker.getDefault().taskStarted(event.getTask().getTaskName());
	}

	/**
	 * @see org.apache.tools.ant.BuildListener#taskFinished(org.apache.tools.ant.BuildEvent)
	 */
	public void taskFinished(BuildEvent event) {
		handleException(event);
		AntTestChecker.getDefault().targetFinished();
	}

	/**
	 * @see BuildListener#messageLogged(BuildEvent)
	 */
	public void messageLogged(BuildEvent event) {
		if (event.getPriority() > getMessageOutputLevel()) {
			return;
		}
		logMessage(event.getMessage(), event.getPriority());
		AntTestChecker.getDefault().messageLogged(event.getMessage());
	}

	protected PrintStream getErrorPrintStream() {
		return fErr;
	}
	
	protected PrintStream getOutputPrintStream() {
		return fOut;
	}
	
	/**
	 * @see org.apache.tools.ant.BuildLogger#setErrorPrintStream(java.io.PrintStream)
	 */
	public void setErrorPrintStream(PrintStream err) {
		//this build logger logs to "null" unless
		//the user has explicitly set a logfile to use
		if (err == System.err) {
			fErr= null;
		} else {
			fErr= err;
		}
	}

	/**
	 * @see org.apache.tools.ant.BuildLogger#setOutputPrintStream(java.io.PrintStream)
	 */
	public void setOutputPrintStream(PrintStream output) {
		//this build logger logs to "null" unless
		//the user has explicitly set a logfile to use
		if (output == System.out) {
			fOut= null;
		} else {
			fOut= output;
		}
	}
	
	protected void logMessage(String message, int priority) {
		if (priority > getMessageOutputLevel()) {
			return;
		}
		
		if (priority == Project.MSG_ERR) {
			if (getErrorPrintStream() != null && getErrorPrintStream() != System.err) {
				//user has designated to log to a logfile
				getErrorPrintStream().println(message);
			}
		} else {
			if (getOutputPrintStream() != null && getOutputPrintStream() != System.out) {
				//user has designated to log to a logfile
				getOutputPrintStream().println(message);
			} 
		}
	}
	
	protected void handleException(BuildEvent event) {
		Throwable exception = event.getException();
		if (exception == null || exception == fHandledException
		|| exception instanceof OperationCanceledException
		|| exception instanceof AntSecurityException) {
			return;
		}
		fHandledException= exception;
	}
}
