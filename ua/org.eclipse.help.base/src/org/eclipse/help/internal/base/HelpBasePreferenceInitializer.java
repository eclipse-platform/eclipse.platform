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

import java.util.Locale;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

/**
 */
public class HelpBasePreferenceInitializer extends
		AbstractPreferenceInitializer {

	@Override
	public void initializeDefaultPreferences() {
		IEclipsePreferences prefs = DefaultScope.INSTANCE.getNode(HelpBasePlugin.PLUGIN_ID);

		String os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH); //$NON-NLS-1$

		if (os.contains("windows")) { //$NON-NLS-1$
			prefs
					.put("custom_browser_path", //$NON-NLS-1$
							"\"C:\\Program Files\\Internet Explorer\\IEXPLORE.EXE\" %1"); //$NON-NLS-1$
		} else if (os.contains("linux")) { //$NON-NLS-1$
			prefs.put("custom_browser_path", //$NON-NLS-1$
					"konqueror %1"); //$NON-NLS-1$
		} else {
			prefs.put("custom_browser_path", "mozilla %1"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		try {
			prefs.flush();
		} catch (BackingStoreException e) {
		}
	}

}
