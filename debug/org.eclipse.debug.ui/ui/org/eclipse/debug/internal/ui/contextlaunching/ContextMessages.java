/*******************************************************************************
 *  Copyright (c) 2007, 2012 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.contextlaunching;

import org.eclipse.osgi.util.NLS;

/**
 * NLS'd messages for context launching artifacts
 * @since 3.3
 * CONTEXTLAUNCHING
 */
public class ContextMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.debug.internal.ui.contextlaunching.ContextMessages"; //$NON-NLS-1$
	public static String ContextRunner_0;
	public static String ContextRunner_1;
	public static String ContextRunner_13;
	public static String ContextRunner_14;
	public static String ContextRunner_15;
	public static String ContextRunner_3;
	public static String ContextRunner_7;
	public static String LaunchingResourceManager_0;
	public static String LaunchingResourceManager_1;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, ContextMessages.class);
	}

	private ContextMessages() {
	}
}
