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
package org.eclipse.ui.internal.console.ansi.preferences;

public class AnsiConsolePreferenceConstants {
	public static final String PREF_ANSI_CONSOLE_ENABLED = "booleanEnabled"; //$NON-NLS-1$
	public static final String PREF_WINDOWS_MAPPING = "booleanWindowsMapping"; //$NON-NLS-1$
	public static final String PREF_SHOW_ESCAPES = "booleanShowEscapes"; //$NON-NLS-1$
	public static final String PREF_COLOR_PALETTE = "choiceColorPalette"; //$NON-NLS-1$
	public static final String PREF_KEEP_STDERR_COLOR = "booleanKeepStderrColor"; //$NON-NLS-1$
	public static final String PREF_PUT_RTF_IN_CLIPBOARD = "booleanPutRtfInClipboard"; //$NON-NLS-1$

	private AnsiConsolePreferenceConstants() {
		// Utility class, should not be instantiated
	}
}
