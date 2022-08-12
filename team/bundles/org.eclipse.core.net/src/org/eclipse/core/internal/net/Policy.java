/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
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
package org.eclipse.core.internal.net;

import java.util.Date;

import org.eclipse.osgi.service.debug.DebugOptionsListener;

public class Policy {

	// general debug flag
	public static boolean DEBUG = false;

	public static boolean DEBUG_SYSTEM_PROVIDERS = false;

	static final DebugOptionsListener DEBUG_OPTIONS_LISTENER = options -> {
		DEBUG = options.getBooleanOption(Activator.ID + "/debug", false); //$NON-NLS-1$
		DEBUG_SYSTEM_PROVIDERS = DEBUG && options.getBooleanOption(Activator.ID + "/systemproviders", false); //$NON-NLS-1$
	};

	/**
	 * Print a debug message to the console. Pre-pend the message with the
	 * current date and the name of the current thread.
	 *
	 * @param message
	 */
	public static void debug(String message) {
		StringBuilder buffer = new StringBuilder();
		buffer.append(new Date(System.currentTimeMillis()));
		buffer.append(" - ["); //$NON-NLS-1$
		buffer.append(Thread.currentThread().getName());
		buffer.append("] "); //$NON-NLS-1$
		buffer.append(message);
		System.out.println(buffer.toString());
	}
}
