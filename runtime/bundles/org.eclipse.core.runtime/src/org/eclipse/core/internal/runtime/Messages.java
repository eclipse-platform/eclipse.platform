/*******************************************************************************
 *  Copyright (c) 2005, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *  IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.runtime;

import org.eclipse.osgi.util.NLS;

// Runtime plugin message catalog
public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.core.internal.runtime.messages"; //$NON-NLS-1$

	// authorization
	public static String auth_alreadySpecified;
	public static String auth_notAvailable;

	// line separator platforms
	public static String line_separator_platform_unix;
	public static String line_separator_platform_windows;

	// metadata
	public static String meta_appNotInit;
	public static String meta_appStopped;
	public static String meta_exceptionParsingLog;

	// parsing/resolve

	// plugins

	// Preferences
	public static String preferences_saveProblems;

	// Compatibility - parsing/resolve

	// Compatibility - plugins
	public static String plugin_unableToGetActivator;

	static {
		// load message values from bundle file
		reloadMessages();
	}

	public static void reloadMessages() {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}
}