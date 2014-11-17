/*******************************************************************************
 * Copyright (c) 2008, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.net;

import java.util.Date;

import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;

public class Policy {
	
	// general debug flag
	public static boolean DEBUG = false;

	public static boolean DEBUG_SYSTEM_PROVIDERS = false;

	static final DebugOptionsListener DEBUG_OPTIONS_LISTENER = new DebugOptionsListener() {
		public void optionsChanged(DebugOptions options) {
			DEBUG = options.getBooleanOption(Activator.ID + "/debug", false); //$NON-NLS-1$
			DEBUG_SYSTEM_PROVIDERS = DEBUG && options.getBooleanOption(Activator.ID + "/systemproviders", false); //$NON-NLS-1$
		}
	};

	/**
	 * Print a debug message to the console. Pre-pend the message with the
	 * current date and the name of the current thread.
	 * 
	 * @param message
	 */
	public static void debug(String message) {
		StringBuffer buffer = new StringBuffer();
		buffer.append(new Date(System.currentTimeMillis()));
		buffer.append(" - ["); //$NON-NLS-1$
		buffer.append(Thread.currentThread().getName());
		buffer.append("] "); //$NON-NLS-1$
		buffer.append(message);
		System.out.println(buffer.toString());
	}
}
