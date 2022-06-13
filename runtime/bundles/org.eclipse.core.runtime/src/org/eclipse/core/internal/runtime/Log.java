/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.core.internal.runtime;

import java.util.HashSet;
import java.util.Set;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.log.*;
import org.osgi.framework.Bundle;
import org.osgi.service.log.LogEntry;

/**
 *
 */
public class Log implements ILog, SynchronousLogListener, LogFilter {
	final Bundle bundle;
	private final Logger logger;
	private final Set<ILogListener> logListeners = new HashSet<>(5);

	public Log(Bundle plugin, Logger logger) {
		if (plugin == null)
			throw new IllegalArgumentException("Logging bundle must not be null."); //$NON-NLS-1$
		this.bundle = plugin;
		this.logger = logger;
	}

	/**
	 * Adds the given log listener to this log.  Subsequently the log listener will
	 * receive notification of all log events passing through this log.
	 *
	 * @see Platform#addLogListener(ILogListener)
	 */
	@Override
	public void addLogListener(ILogListener listener) {
		synchronized (logListeners) {
			logListeners.add(listener);
		}
	}

	/**
	 * Returns the plug-in with which this log is associated.
	 */
	@Override
	public Bundle getBundle() {
		return bundle;
	}

	/**
	 * Logs the given status.  The status is distributed to the log listeners
	 * installed on this log and then to the log listeners installed on the platform.
	 *
	 * @see Plugin#getLog()
	 */
	@Override
	public void log(final IStatus status) {
		// Log to the logger
		logger.log(PlatformLogWriter.getLog(status), PlatformLogWriter.getLevel(status), status.getMessage(), status.getException());
	}

	/**
	 * Removes the given log listener to this log.  Subsequently the log listener will
	 * no longer receive notification of log events passing through this log.
	 *
	 * @see Platform#removeLogListener(ILogListener)
	 */
	@Override
	public void removeLogListener(ILogListener listener) {
		synchronized (logListeners) {
			logListeners.remove(listener);
		}
	}

	@Override
	public void logged(LogEntry entry) {
		logToListeners(PlatformLogWriter.convertToStatus(entry));
	}

	private void logToListeners(final IStatus status) {
		// create array to avoid concurrent access
		ILogListener[] listeners;
		synchronized (logListeners) {
			listeners = logListeners.toArray(new ILogListener[logListeners.size()]);
		}
		for (final ILogListener listener : listeners) {
			ISafeRunnable code = new ISafeRunnable() {
				@Override
				public void run() throws Exception {
					listener.logging(status, bundle.getSymbolicName());
				}

				@Override
				public void handleException(Throwable e) {
					//Ignore
				}
			};
			SafeRunner.run(code);
		}
	}

	@Override
	public boolean isLoggable(Bundle loggingBundle, String loggerName, int logLevel) {
		return PlatformLogWriter.EQUINOX_LOGGER_NAME.equals(loggerName) && bundle.getBundleId() == loggingBundle.getBundleId();
	}
}
