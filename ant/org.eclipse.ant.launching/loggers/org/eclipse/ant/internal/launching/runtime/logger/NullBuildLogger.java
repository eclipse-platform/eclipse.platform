/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
package org.eclipse.ant.internal.launching.runtime.logger;

import java.io.PrintStream;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.util.StringUtils;
import org.eclipse.ant.core.AntSecurityException;
import org.eclipse.ant.internal.core.AbstractEclipseBuildLogger;
import org.eclipse.core.runtime.OperationCanceledException;

public class NullBuildLogger extends AbstractEclipseBuildLogger implements BuildLogger {

	protected int fMessageOutputLevel = Project.MSG_INFO;
	private PrintStream fErr = null;
	private PrintStream fOut = null;
	protected boolean fEmacsMode = false;

	/**
	 * An exception that has already been logged.
	 */
	protected Throwable fHandledException = null;

	@Override
	public void setMessageOutputLevel(int level) {
		fMessageOutputLevel = level;
	}

	@Override
	public int getMessageOutputLevel() {
		return fMessageOutputLevel;
	}

	@Override
	public void setEmacsMode(boolean emacsMode) {
		fEmacsMode = emacsMode;
	}

	@Override
	public void buildStarted(BuildEvent event) {
		// do nothing
	}

	@Override
	public void buildFinished(BuildEvent event) {
		String message = handleException(event);
		if (message != null) {
			logMessage(message, getMessageOutputLevel());
		}
		fHandledException = null;
	}

	@Override
	public void targetStarted(BuildEvent event) {
		// do nothing
	}

	@Override
	public void targetFinished(BuildEvent event) {
		// do nothing
	}

	@Override
	public void taskStarted(BuildEvent event) {
		// do nothing
	}

	@Override
	public void taskFinished(BuildEvent event) {
		// do nothing
	}

	@Override
	public void messageLogged(BuildEvent event) {
		logMessage(event.getMessage(), event.getPriority());
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

	protected String handleException(BuildEvent event) {
		Throwable exception = event.getException();
		if (exception == null || exception == fHandledException || exception instanceof OperationCanceledException
				|| exception instanceof AntSecurityException) {
			return null;
		}
		fHandledException = exception;
		StringBuilder message = new StringBuilder();
		message.append(System.lineSeparator());
		message.append(RuntimeMessages.NullBuildLogger_1);
		message.append(System.lineSeparator());
		if (Project.MSG_VERBOSE <= fMessageOutputLevel || !(exception instanceof BuildException)) {
			message.append(StringUtils.getStackTrace(exception));
		} else {
			if (exception instanceof BuildException) {
				message.append(exception.toString()).append(System.lineSeparator());
			} else {
				message.append(exception.getMessage()).append(System.lineSeparator());
			}
		}

		return message.toString();
	}
}
