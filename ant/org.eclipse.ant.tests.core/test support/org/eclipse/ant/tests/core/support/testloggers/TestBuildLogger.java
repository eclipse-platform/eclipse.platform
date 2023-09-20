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
package org.eclipse.ant.tests.core.support.testloggers;

import java.io.PrintStream;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildLogger;
import org.apache.tools.ant.Project;
import org.eclipse.ant.core.AntSecurityException;
import org.eclipse.ant.tests.core.testplugin.AntTestChecker;
import org.eclipse.core.runtime.OperationCanceledException;

public class TestBuildLogger implements BuildLogger {

	private int fMessageOutputLevel = Project.MSG_INFO;
	private PrintStream fErr = null;
	private PrintStream fOut = null;
	private boolean fSetProperties = true;

	/**
	 * An exception that has already been logged.
	 */
	private Throwable fHandledException = null;

	public TestBuildLogger() {
	}

	@Override
	public void setMessageOutputLevel(int level) {
		fMessageOutputLevel = level;
	}

	public int getMessageOutputLevel() {
		return fMessageOutputLevel;
	}

	@Override
	public void setEmacsMode(boolean emacsMode) {
		// do nothing
	}

	@Override
	public void buildStarted(BuildEvent event) {
		AntTestChecker.getDefault().buildStarted(event.getProject().getName());
	}

	@Override
	public void buildFinished(BuildEvent event) {
		handleException(event);
		fHandledException = null;
		AntTestChecker.getDefault().buildFinished();
	}

	@Override
	public void targetStarted(BuildEvent event) {
		AntTestChecker.getDefault().targetStarted(event.getTarget().getName());
		if (fSetProperties) {
			fSetProperties = false;
			AntTestChecker.getDefault().setUserProperties(event.getProject().getProperties());
		}
	}

	@Override
	public void targetFinished(BuildEvent event) {
		handleException(event);
		AntTestChecker.getDefault().targetFinished();
	}

	@Override
	public void taskStarted(BuildEvent event) {
		AntTestChecker.getDefault().taskStarted(event.getTask().getTaskName());
	}

	@Override
	public void taskFinished(BuildEvent event) {
		handleException(event);
		AntTestChecker.getDefault().targetFinished();
	}

	@Override
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

	@Override
	public void setErrorPrintStream(PrintStream err) {
		// this build logger logs to "null" unless
		// the user has explicitly set a logfile to use
		if (err == System.err) {
			fErr = null;
		} else {
			fErr = err;
		}
	}

	@Override
	public void setOutputPrintStream(PrintStream output) {
		// this build logger logs to "null" unless
		// the user has explicitly set a logfile to use
		if (output == System.out) {
			fOut = null;
		} else {
			fOut = output;
		}
	}

	protected void logMessage(String message, int priority) {
		if (priority > getMessageOutputLevel()) {
			return;
		}

		if (priority == Project.MSG_ERR) {
			if (getErrorPrintStream() != null && getErrorPrintStream() != System.err) {
				// user has designated to log to a logfile
				getErrorPrintStream().println(message);
			}
		} else {
			if (getOutputPrintStream() != null && getOutputPrintStream() != System.out) {
				// user has designated to log to a logfile
				getOutputPrintStream().println(message);
			}
		}
	}

	protected void handleException(BuildEvent event) {
		Throwable exception = event.getException();
		if (exception == null || exception == fHandledException || exception instanceof OperationCanceledException
				|| exception instanceof AntSecurityException) {
			return;
		}
		fHandledException = exception;
	}
}
