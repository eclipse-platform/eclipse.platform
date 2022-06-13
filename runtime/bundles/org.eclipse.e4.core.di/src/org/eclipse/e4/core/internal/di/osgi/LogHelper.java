/*******************************************************************************
 * Copyright (c) 2010, 2015 IBM Corporation and others.
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
package org.eclipse.e4.core.internal.di.osgi;

import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.framework.log.FrameworkLogEntry;
import org.osgi.framework.FrameworkUtil;
import org.osgi.util.tracker.ServiceTracker;

public class LogHelper {

	private static final ServiceTracker<FrameworkLog, FrameworkLog> logTracker = openLogTracker();

	private static ServiceTracker<FrameworkLog, FrameworkLog> openLogTracker() {
		try {
			ServiceTracker<FrameworkLog, FrameworkLog> st = new ServiceTracker<>(
					FrameworkUtil.getBundle(LogHelper.class).getBundleContext(), FrameworkLog.class, null);
			st.open();
			return st;
		} catch (Throwable t) {
			return null;
		}
	}

	static final private String plugin_name = "org.eclipse.e4.core.di"; //$NON-NLS-1$

	static public void logError(String msg, Throwable e) {
		log(msg, FrameworkLogEntry.ERROR, e);
	}

	static public void logWarning(String msg, Throwable e) {
		log(msg, FrameworkLogEntry.WARNING, e);
	}

	static public void log(String msg, int severity, Throwable e) {
		FrameworkLog log = logTracker == null ? null : logTracker.getService();
		if (log != null) {
			FrameworkLogEntry logEntry = new FrameworkLogEntry(plugin_name, severity, 0, msg, 0, e, null);
			log.log(logEntry);
		}
	}
}
