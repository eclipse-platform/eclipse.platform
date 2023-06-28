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
 *     vogella GmbH - Bug 287303 - [patch] Add Word Wrap action to Console View
 *******************************************************************************/
package org.eclipse.ui.internal.console;

import org.eclipse.ui.console.IConsoleConstants;

/**
 * Help context ids for the console plugin.
 * <p>
 * This interface contains constants only; it is not intended to be implemented
 * or extended.
 * </p>
 *
 */
public interface IConsoleHelpContextIds {

	String PREFIX = IConsoleConstants.PLUGIN_ID + "."; //$NON-NLS-1$

	// Actions
	String CLEAR_CONSOLE_ACTION = PREFIX + "clear_console_action_context"; //$NON-NLS-1$
	String CONSOLE_SCROLL_LOCK_ACTION = PREFIX + "console_scroll_lock_action_context"; //$NON-NLS-1$
	String CONSOLE_WORD_WRAP_ACTION = PREFIX + "console_word_wrap_action_context"; //$NON-NLS-1$
	String CONSOLE_SELECT_ALL_ACTION = PREFIX + "console_select_all_action_context"; //$NON-NLS-1$
	String CONSOLE_COPY_ACTION = PREFIX + "copy_to_clipboard_action_context"; //$NON-NLS-1$
	String CONSOLE_CUT_ACTION = PREFIX + "console_cut_action_context"; //$NON-NLS-1$
	String CONSOLE_PASTE_ACTION = PREFIX + "console_paste_action_context"; //$NON-NLS-1$
	String CONSOLE_FIND_REPLACE_ACTION = PREFIX + "console_find_replace_action_context"; //$NON-NLS-1$
	String CONSOLE_FIND_NEXT_ACTION = PREFIX + "console_find_next_action_context"; //$NON-NLS-1$
	String CONSOLE_FIND_PREVIOUS_ACTION = PREFIX + "console_find_previous_action_context"; //$NON-NLS-1$
	String CONSOLE_OPEN_LINK_ACTION = PREFIX + "console_open_link_action_context"; //$NON-NLS-1$
	String CONSOLE_OPEN_CONSOLE_ACTION = PREFIX + "console_open_console_action_context"; //$NON-NLS-1$
	String CONSOLE_DISPLAY_CONSOLE_ACTION = PREFIX + "console_display_console_action"; //$NON-NLS-1$
	String CONSOLE_PIN_CONSOLE_ACITON = PREFIX + "console_pin_console_action"; //$NON-NLS-1$

	// Views
	String CONSOLE_VIEW = PREFIX + "console_view_context"; //$NON-NLS-1$

	// Preference pages
	String CONSOLE_PREFERENCE_PAGE = PREFIX + "console_preference_page_context"; //$NON-NLS-1$
	String CONSOLE_ANSI_PREFERENCE_PAGE = PREFIX + "console_ansi_preference_page_context"; //$NON-NLS-1$
}

