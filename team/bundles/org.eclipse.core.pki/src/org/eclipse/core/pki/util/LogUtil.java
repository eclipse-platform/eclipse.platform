/*******************************************************************************
 * Copyright (c) 2023 Eclipse Platform, Security Group and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Eclipse Platform - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.pki.util;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ServiceCaller;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;


/**
 * A logging utility class
 */
public class LogUtil {

	public static void logInfo(String message) {
		StackWalker stackWalker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
		final var pluginName = stackWalker.getCallerClass();
		final ServiceCaller<ILog> log = new ServiceCaller<>(pluginName, ILog.class);
		log.call(logger -> logger.info(message));
	}
	public static void logError(String message, Throwable t) {
		StackWalker stackWalker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
		final var pluginName = stackWalker.getCallerClass();
		final ServiceCaller<ILog> log = new ServiceCaller<>(pluginName, ILog.class);
		log.call(logger -> logger.error(message));
	}

	public static void logWarning(String message) {
		StackWalker stackWalker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
		final var pluginName = stackWalker.getCallerClass();
		final ServiceCaller<ILog> log = new ServiceCaller<>(pluginName, ILog.class);
		IStatus status = new Status(IStatus.WARNING, pluginName.getPackageName(), message);
		log.call(logger -> logger.warn(message));
	}
	public static void logDebug(String message) {
		StackWalker stackWalker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
		final var pluginName = stackWalker.getCallerClass();
		final ServiceCaller<DebugPlugin> log = new ServiceCaller<>(pluginName, DebugPlugin.class);
		IStatus status = new Status(IStatus.WARNING, pluginName.getPackageName(), message);
		log.call(logger -> DebugPlugin.logDebugMessage(message));
	}
}
