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
	private static final String PREFIX = "ANSI_support_"; //$NON-NLS-1$

	public static final String PREF_ANSI_CONSOLE_ENABLED = PREFIX + "enabled"; //$NON-NLS-1$
	public static final String PREF_WINDOWS_MAPPING = PREFIX + "use_windows_color_mapping"; //$NON-NLS-1$
	public static final String PREF_SHOW_ESCAPES = PREFIX + "show_escapes"; //$NON-NLS-1$
	public static final String PREF_COLOR_PALETTE = PREFIX + "color_palette"; //$NON-NLS-1$
	public static final String PREF_KEEP_STDERR_COLOR = PREFIX + "keep_stderr_color"; //$NON-NLS-1$
	public static final String PREF_PUT_RTF_IN_CLIPBOARD = PREFIX + "put_rtf_to_clipboard"; //$NON-NLS-1$

	private AnsiConsolePreferenceConstants() {
		// Utility class, should not be instantiated
	}
}
