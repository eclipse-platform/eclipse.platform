/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
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

package org.eclipse.help.ui.internal.util;

import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.environment.Constants;
import org.eclipse.swt.widgets.Display;

public class FontUtils {

	private static final int TRAY_FONT_HEIGHT_LIMIT = 17;

	/*
	 * Determine whether the font is suitable for use in a tray dialog
	 * @return true if the font is so large that the tray would look bad.
	 */
	static public boolean isFontTooLargeForTray() {
		try {
			int height = Display.getDefault().getSystemFont().getFontData()[0].getHeight();
			return height > TRAY_FONT_HEIGHT_LIMIT;
		} catch (RuntimeException e) {
			return true;
		}
	}

	/*
	 * Get a sequence of JavaScript which will scale the embedded browser contents
	 * @param percent The percentage scaling relative to the default size
	 * @return Javascript to perform the scaling or null if we cannot create scaling script for this OS/browser
	 */
	static public String getRescaleScript(int percent) {
		String scaleString = percent/100 + "." + (percent % 100) / 10; //$NON-NLS-1$
		String os = Platform.getOS();
		if (Constants.WS_WIN32.equalsIgnoreCase(os) ||
			Constants.OS_MACOSX.equalsIgnoreCase(os)) {
			return "document.body.style.zoom = " + scaleString; //$NON-NLS-1$
		}
		return null;  // No rescale in Mozilla browsers
	}

	/*
	 * Function to determine whether the browser in the help view supports a zoom command
	 */
	static public boolean canRescaleHelpView() {
		String os = Platform.getOS();
		if (Constants.WS_WIN32.equalsIgnoreCase(os) ||
			Constants.OS_MACOSX.equalsIgnoreCase(os)) {
			return true;
		}
		// No rescale in Mozilla browsers, see Bug 227198
		return false;
	}

}
