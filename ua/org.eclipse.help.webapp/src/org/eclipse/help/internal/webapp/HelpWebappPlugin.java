/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *     George Suaridze <suag@1c.ru> (1C-Soft LLC) - Bug 560168
 *******************************************************************************/
package org.eclipse.help.internal.webapp;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

/**
 * Welp web application plug-in.
 */
public class HelpWebappPlugin extends Plugin {
	public static final String PLUGIN_ID = "org.eclipse.help.webapp"; //$NON-NLS-1$

	// debug options
	public static boolean DEBUG = false;

	public static boolean DEBUG_WORKINGSETS = false;

	protected static HelpWebappPlugin plugin;

	/**
	 * @return the singleton instance of the help webapp plugin
	 */
	public static HelpWebappPlugin getDefault() {
		return plugin;
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		// Setup debugging options
		DEBUG = isDebugging();
		if (DEBUG) {
			DEBUG_WORKINGSETS = "true".equalsIgnoreCase(Platform.getDebugOption("org.eclipse.help.webapp/debug/workingsets")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

}
