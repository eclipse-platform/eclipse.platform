/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.team.internal.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;

/**
 * Collects exceptions and can be configured to ignore duplicates exceptions. Exceptions can be logged
 * and a MultiStatus containing all collected exceptions can be returned.
 *
 * @see org.eclipse.core.runtime.MultiStatus
 * @see org.eclipse.core.runtime.IStatus
 *
 * @since 3.0
 */
public class ExceptionCollector {

	private List<IStatus> statuses = new ArrayList<>();
	private String message;
	private String pluginId;
	private int severity;
	private ILog log;

	/**
	 * Creates a collector and initializes the parameters for the top-level exception
	 * that would be returned from <code>getStatus</code> is exceptions are collected.
	 *
	 * @param message a human-readable message, localized to the current locale
	 * @param pluginId the unique identifier of the relevant plug-in
	 * @param severity the severity; one of <code>OK</code>,
	 *   <code>ERROR</code>, <code>INFO</code>, or <code>WARNING</code>
	 * @param log the log to output the exceptions to, or <code>null</code> if
	 *   exceptions should not be logged.
	 */
	public ExceptionCollector(String message, String pluginId, int severity, ILog log) {
		this.message = message;
		this.pluginId = pluginId;
		this.severity = severity;
		this.log = log;
	}

	/**
	 * Clears the exceptions collected.
	 */
	public void clear() {
		statuses.clear();
	}

	/**
	 * Returns a status that represents the exceptions collected. If the collector
	 * is empty <code>IStatus.OK</code> is returned. Otherwise a MultiStatus containing
	 * all collected exceptions is returned.
	 * @return a multistatus containing the exceptions collected or IStatus.OK if
	 * the collector is empty.
	 */
	public IStatus getStatus() {
		if(statuses.isEmpty()) {
			return Status.OK_STATUS;
		} else {
			MultiStatus multiStatus = new MultiStatus(pluginId, severity, message, null);
			Iterator it = statuses.iterator();
			while (it.hasNext()) {
				IStatus status = (IStatus) it.next();
				multiStatus.merge(status);
			}
			return multiStatus;
		}
	}

	/**
	 * Add this exception to the collector. If a log was specified in the constructor
	 * then the exception will be output to the log. You can retreive exceptions
	 * using <code>getStatus</code>.
	 *
	 * @param exception the exception to collect
	 */
	public void handleException(CoreException exception) {
		// log the exception if we have a log
		if(log != null) {
			log.log(new Status(severity, pluginId, 0, message, exception));
		}
		// Record each status individually to flatten the resulting multi-status
		IStatus exceptionStatus = exception.getStatus();
		// Wrap the exception so the stack trace is not lost.
		IStatus status = new Status(exceptionStatus.getSeverity(), exceptionStatus.getPlugin(), exceptionStatus.getCode(), exceptionStatus.getMessage(), exception);
		recordStatus(status);
		IStatus[] children = status.getChildren();
		for (IStatus status2 : children) {
			recordStatus(status2);
		}
	}

	private void recordStatus(IStatus status) {
		statuses.add(status);
	}
}
