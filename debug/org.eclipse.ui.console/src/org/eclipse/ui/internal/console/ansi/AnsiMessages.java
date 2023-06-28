/*******************************************************************************
 * Copyright (c) 2012-2022 Mihai Nita and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.ui.internal.console.ansi;

import org.eclipse.osgi.util.NLS;

public class AnsiMessages extends NLS {
	private static final String BUNDLE_NAME = AnsiMessages.class.getPackageName() + ".messages"; //$NON-NLS-1$
	public static String PreferencePage_Title;
	public static String PreferencePage_PluginEnabled;
	public static String PreferencePage_Option_WindowsMapping;
	public static String PreferencePage_Option_ShowEscapes;
	public static String PreferencePage_Option_HonorStderr;
	public static String PreferencePage_Option_RtfInClipboard;
	public static String PreferencePage_Option_ColorPaletteSectionTitle;
	public static String PreferencePage_Option_ColorPaletteVGA;
	public static String PreferencePage_Option_ColorPaletteWinXP;
	public static String PreferencePage_Option_ColorPaletteWin10;
	public static String PreferencePage_Option_ColorPaletteMacOS;
	public static String PreferencePage_Option_ColorPalettePutty;
	public static String PreferencePage_Option_ColorPaletteXterm;
	public static String PreferencePage_Option_ColorPaletteMirc;
	public static String PreferencePage_Option_ColorPaletteUbuntu;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, AnsiMessages.class);
	}

	private AnsiMessages() {
	}
}
