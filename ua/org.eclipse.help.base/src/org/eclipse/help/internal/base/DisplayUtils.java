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
 *******************************************************************************/
package org.eclipse.help.internal.base;

import java.lang.reflect.*;

import org.eclipse.core.runtime.*;
import org.osgi.framework.*;

/**
 * Utility class to control SWT Display and event loop run in
 * org.eclipse.help.ui plug-in
 */
public class DisplayUtils {
	private static final String HELP_UI_PLUGIN_ID = "org.eclipse.help.ui"; //$NON-NLS-1$
	private static final String LOOP_CLASS_NAME = "org.eclipse.help.ui.internal.HelpUIEventLoop"; //$NON-NLS-1$

	static void runUI() {
		invoke("run"); //$NON-NLS-1$
	}
	static void wakeupUI() {
		invoke("wakeup"); //$NON-NLS-1$
	}

	static void waitForDisplay() {
		invoke("waitFor"); //$NON-NLS-1$
	}

	private static void invoke(String method) {
		try {
			Bundle bundle = Platform.getBundle(HELP_UI_PLUGIN_ID);
			if (bundle == null) {
				return;
			}
			Class<?> c = bundle.loadClass(LOOP_CLASS_NAME);
			Method m = c.getMethod(method);
			m.invoke(null);
		} catch (Exception e) {
		}
	}
}
