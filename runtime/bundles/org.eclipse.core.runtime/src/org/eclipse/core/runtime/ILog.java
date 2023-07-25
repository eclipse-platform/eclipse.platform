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
 *     Christoph Läubrich - add methods for logging without a IStatus object
 *******************************************************************************/
package org.eclipse.core.runtime;

import org.eclipse.core.internal.runtime.InternalPlatform;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

/**
 * A log to which status events can be written.  Logs appear on individual
 * plug-ins and on the platform itself.  Clients can register log listeners which
 * will receive notification of all log events as they come in.
 * <p>
 * XXX Need to create a new log interface on common plugin. That interface should be a super interface of this ILog.
 * getBundle() would stay here. In the super interface we would have getName()
 * </p>
 *
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ILog {
	/**
	 * Adds the given log listener to this log.  Subsequently the log listener will
	 * receive notification of all log events passing through this log.
	 * This method has no effect if the identical listener is already registered on this log.
	 *
	 * @param listener the listener to add to this log
	 * @see Platform#addLogListener(ILogListener)
	 */
	public void addLogListener(ILogListener listener);

	/**
	 * Returns the plug-in with which this log is associated.
	 *
	 * @return the plug-in with which this log is associated
	 * @since 3.0
	 */
	public Bundle getBundle();

	/**
	 * Logs the given status.  The status is distributed to the log listeners
	 * installed on this log and then to the log listeners installed on the platform.
	 *
	 * @param status the status to log
	 */
	public void log(IStatus status);

	/**
	 * Removes the given log listener to this log.  Subsequently the log listener will
	 * no longer receive notification of log events passing through this log.
	 * This method has no effect if the identical listener is not registered on this log.
	 *
	 * @param listener the listener to remove
	 * @see Platform#removeLogListener(ILogListener)
	 */
	public void removeLogListener(ILogListener listener);

	/**
	 * Logs a status with {@link IStatus#INFO} using this logger
	 * {@link Bundle#getSymbolicName()} as pluginId
	 *
	 * @param message the message to log
	 * @since 3.17
	 */
	default void info(String message) {
		log(new Status(IStatus.INFO, getBundle().getSymbolicName(), message));
	}

	/**
	 * Logs a status with {@link IStatus#INFO} using this logger
	 * {@link Bundle#getSymbolicName()} as pluginId
	 *
	 * @param message   the message to log
	 * @param throwable an optional throwable to associate with this status
	 * @since 3.17
	 */
	default void info(String message, Throwable throwable) {
		log(new Status(IStatus.INFO, getBundle().getSymbolicName(), message, throwable));
	}

	/**
	 * Logs a status with {@link IStatus#WARNING} using this logger
	 * {@link Bundle#getSymbolicName()} as pluginId
	 *
	 * @param message the message to log
	 * @since 3.17
	 */
	default void warn(String message) {
		log(new Status(IStatus.WARNING, getBundle().getSymbolicName(), message));
	}

	/**
	 * Logs a status with {@link IStatus#WARNING} using this logger
	 * {@link Bundle#getSymbolicName()} as pluginId
	 *
	 * @param message   the message to log
	 * @param throwable an optional throwable to associate with this status
	 * @since 3.17
	 */
	default void warn(String message, Throwable throwable) {
		log(new Status(IStatus.WARNING, getBundle().getSymbolicName(), message, throwable));
	}

	/**
	 * Logs a status with {@link IStatus#ERROR} using this logger
	 * {@link Bundle#getSymbolicName()} as pluginId
	 *
	 * @param message the message to log
	 * @since 3.17
	 */
	default void error(String message) {
		log(new Status(IStatus.ERROR, getBundle().getSymbolicName(), message));
	}

	/**
	 * Logs a status with {@link IStatus#ERROR} using this logger
	 * {@link Bundle#getSymbolicName()} as pluginId
	 *
	 * @param message   the message to log
	 * @param throwable an optional throwable to associate with this status
	 * @since 3.17
	 */
	default void error(String message, Throwable throwable) {
		log(new Status(IStatus.ERROR, getBundle().getSymbolicName(), message, throwable));
	}

	/**
	 * Returns the log for the given bundle. If no such log exists, one is created.
	 *
	 * @param bundle the bundle whose log is returned
	 * @return the log for the given bundle
	 * @since 3.29
	 */
	public static ILog of(Bundle bundle) {
		return InternalPlatform.getDefault().getLog(bundle);
	}

	/**
	 * Returns the log for the bundle of the given class. If no such log exists, one
	 * is created.
	 *
	 * @param clazz the class in a bundle whose log is returned
	 * @return the log for the bundle to which the clazz belongs
	 *
	 * @since 3.29
	 */
	public static ILog of(Class<?> clazz) {
		Bundle bundle = FrameworkUtil.getBundle(clazz);
		return InternalPlatform.getDefault().getLog(bundle);
	}

	/**
	 * Returns the log for the bundle of the calling class. If no such log exists,
	 * one is created.
	 *
	 * @return the log for the bundle to which the caller belongs
	 *
	 * @since 3.29
	 */
	public static ILog get() {
		try {
			return of(InternalPlatform.STACK_WALKER.getCallerClass());
		} catch (IllegalCallerException e) {
			return of(ILog.class);
		}
	}
}
