/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
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
package org.eclipse.debug.internal.core;

import java.util.Hashtable;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.eclipse.osgi.service.debug.DebugTrace;
import org.osgi.framework.BundleContext;

/**
 * Access to debug options.
 *
 * @since 3.3
 */
public class DebugOptions implements DebugOptionsListener {

	// debug option flags
	public static boolean DEBUG = false;
	public static boolean DEBUG_COMMANDS = false;
	public static boolean DEBUG_EVENTS = false;

	static final String DEBUG_FLAG = "org.eclipse.debug.core/debug"; //$NON-NLS-1$
	static final String DEBUG_FLAG_COMMANDS = "org.eclipse.debug.core/debug/commands"; //$NON-NLS-1$
	static final String DEBUG_FLAG_EVENTS = "org.eclipse.debug.core/debug/events"; //$NON-NLS-1$

	/**
	 * The {@link DebugTrace} object to print to OSGi tracing
	 * @since 3.8
	 */
	private static DebugTrace fgDebugTrace;

	/**
	 * Constructor
	 * @param context the bundle context
	 */
	public DebugOptions(BundleContext context) {
		Hashtable<String, String> props = new Hashtable<>(2);
		props.put(org.eclipse.osgi.service.debug.DebugOptions.LISTENER_SYMBOLICNAME, DebugPlugin.getUniqueIdentifier());
		context.registerService(DebugOptionsListener.class.getName(), this, props);
	}

	@Override
	public void optionsChanged(org.eclipse.osgi.service.debug.DebugOptions options) {
		fgDebugTrace = options.newDebugTrace(DebugPlugin.getUniqueIdentifier());
		DEBUG = options.getBooleanOption(DEBUG_FLAG, false);
		DEBUG_COMMANDS = DEBUG && options.getBooleanOption(DEBUG_FLAG_COMMANDS, false);
		DEBUG_EVENTS = DEBUG && options.getBooleanOption(DEBUG_FLAG_EVENTS, false);
	}

	/**
	 * Prints the given message to System.out and to the OSGi tracing (if started)
	 * @param option the option or <code>null</code>
	 * @param message the message to print or <code>null</code>
	 * @param throwable the {@link Throwable} or <code>null</code>
	 * @since 3.8
	 */
	public static void trace(String option, String message, Throwable throwable) {
		System.out.println(message);
		if(fgDebugTrace != null) {
			fgDebugTrace.trace(option, message, throwable);
		}
	}

	/**
	 * Prints the given message to System.out and to the OSGi tracing (if enabled)
	 *
	 * @param message the message or <code>null</code>
	 * @since 3.8
	 */
	public static void trace(String message) {
		trace(null, message, null);
	}
}
