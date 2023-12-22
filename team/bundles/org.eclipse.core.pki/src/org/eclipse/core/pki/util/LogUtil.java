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
import org.eclipse.core.runtime.ServiceCaller;


/**
 * A logging utility class
 */
public class LogUtil {

	public static void logInfo(String message) {
		StackWalker stackWalker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
		final var pluginName = stackWalker.getClass();
		ServiceCaller.callOnce(pluginName, ILog.class, logger -> logger.info(message));
	}
	public static void logError(String message, Throwable t) {
		StackWalker stackWalker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);
		final var pluginName = stackWalker.getClass();
		ServiceCaller.callOnce(pluginName, ILog.class, logger -> logger.error(message));
	}
}
