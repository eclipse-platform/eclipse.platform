/**********************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.core.internal.content;

import java.util.Date;
import org.eclipse.osgi.util.NLS;

// Runtime plugin message catalog
public class ContentMessages extends NLS {

	/**
	 * The unique identifier constant of this plug-in.
	 */
	public static final String OWNER_NAME = "org.eclipse.core.contenttype"; //$NON-NLS-1$

	private static final String BUNDLE_NAME = "org.eclipse.core.internal.content.messages"; //$NON-NLS-1$

	// Content type manager
	public static String content_badInitializationData;
	public static String content_errorReadingContents;
	public static String content_errorLoadingSettings;
	public static String content_errorSavingSettings;
	public static String content_invalidContentDescriber;
	public static String content_invalidProperty;
	public static String content_missingIdentifier;
	public static String content_missingName;
	public static String content_parserConfiguration;

	static {
		// load message values from bundle file
		reloadMessages();
	}

	public static void reloadMessages() {
		NLS.initializeMessages(BUNDLE_NAME, ContentMessages.class);
	}

	/**
	 * Print a debug message to the console.
	 * Pre-pend the message with the current date and the name of the current thread.
	 */
	public static void message(String message) {
		StringBuilder buffer = new StringBuilder();
		buffer.append(new Date(System.currentTimeMillis()));
		buffer.append(" - ["); //$NON-NLS-1$
		buffer.append(Thread.currentThread().getName());
		buffer.append("] "); //$NON-NLS-1$
		buffer.append(message);
		System.out.println(buffer.toString());
	}
}