/*******************************************************************************
 *  Copyright (c) 2008, 2011 IBM Corporation and others.
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

package org.eclipse.ui.internal.intro.impl;

import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.osgi.service.prefs.BackingStoreException;

public class FontSelection {

	public static final String VAR_FONT_STYLE = "fontStyle";  //$NON-NLS-1$
	public static final String FONT_ABSOLUTE = "absolute";  //$NON-NLS-1$
	public static final String FONT_RELATIVE = "relative";  //$NON-NLS-1$
	private static final String SCALE_FACTOR = "scaleFactor"; //$NON-NLS-1$
	public static final String ATT_SCALABLE = "scalable"; //$NON-NLS-1$

	private static final int MIN_HEIGHT = 10;
	private static final int MAX_HEIGHT = 16;

	/*
	 * Returns the height in points of the default SWT font
	 */
	private static int getDefaultFontHeight() {
		Font defaultFont = JFaceResources.getDefaultFont();
		FontData[] fontData = defaultFont.getFontData();
		int height = MIN_HEIGHT;
		for (int i=0; i< fontData.length; i++) {
			FontData data = fontData[i];
			height = Math.max(height, data.getHeight());
		}
		return Math.min(height, MAX_HEIGHT);
	}

	public static String generatePageFontStyle() {
		int defaultFontHeight = getDefaultFontHeight();
		int scale = getScalePercentage();
		String result = getFontSizeDeclaration("", defaultFontHeight, 100, scale); //$NON-NLS-1$
		result += getFontSizeDeclaration("h1", defaultFontHeight, 200, scale); //$NON-NLS-1$
		result += getFontSizeDeclaration("h2", defaultFontHeight, 150, scale); //$NON-NLS-1$
		result += getFontSizeDeclaration("h3", defaultFontHeight, 120, scale); //$NON-NLS-1$
		result += getFontSizeDeclaration("h4", defaultFontHeight, 100, scale); //$NON-NLS-1$
		result += getFontSizeDeclaration("h5", defaultFontHeight, 80, scale); //$NON-NLS-1$
		result += getFontSizeDeclaration("h6", defaultFontHeight, 70, scale); //$NON-NLS-1$
		return result;
	}

	public static final int getScalePercentage() {
		int scale = Platform.getPreferencesService().getInt(IntroPlugin.PLUGIN_ID,  (SCALE_FACTOR), 0, null);
		return scale;
	}

	private static String getFontSizeDeclaration(String element, int baseSize, int percentage, int scale) {
		if (scale > 75) scale = 75;
		int newSize = (int) ((baseSize * percentage *1.25) / (100 - scale));
		return " body " + element  + "{  font-size : " + newSize  + "px; } ";  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
	}

	public static void setScalePercentage(int i) {
		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(IntroPlugin.PLUGIN_ID);
		prefs.putInt(SCALE_FACTOR, i);
		try {
			prefs.flush();
		} catch (BackingStoreException e) {
		}
	}

	// Set the scale factor to it's default
	public static void resetScalePercentage() {
		IEclipsePreferences iprefs = InstanceScope.INSTANCE.getNode(IntroPlugin.PLUGIN_ID);
		IEclipsePreferences dprefs = DefaultScope.INSTANCE.getNode(IntroPlugin.PLUGIN_ID);
		String defaultScale = dprefs.get(SCALE_FACTOR, "0"); //$NON-NLS-1$
		iprefs.put(SCALE_FACTOR, defaultScale);
	}

	public static String getFontStyle() {
		IProduct product = Platform.getProduct();
		if (product != null) {
			String pid = product.getId();
			String style = Platform.getPreferencesService().getString
				(IntroPlugin.PLUGIN_ID,  pid + "_" +FontSelection.VAR_FONT_STYLE, "", null); //$NON-NLS-1$ //$NON-NLS-2$
			if (style.length() > 0) {
				return style;
			}
			style = Platform.getPreferencesService().getString
				(IntroPlugin.PLUGIN_ID,  (FontSelection.VAR_FONT_STYLE), "", null); //$NON-NLS-1$
			if (style.length() > 0) {
				return style;
			}
		}
		// Use default for font style if not specified
		return FontSelection.FONT_RELATIVE;
	}
}
