/*******************************************************************************
 *  Copyright (c) 2004, 2020 IBM Corporation and others.
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
 *     Patrick Chuong (Texas Instruments) - Improve usability of the breakpoint view (Bug 238956)
 *******************************************************************************/
package org.eclipse.debug.internal.ui;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.debug.internal.ui.preferences.IDebugPreferenceConstants;
import org.eclipse.debug.internal.ui.views.memory.MemoryViewUtil;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

public class DebugUIPreferenceInitializer extends AbstractPreferenceInitializer {

	public DebugUIPreferenceInitializer() {
		super();
	}

	@Override
	public void initializeDefaultPreferences() {
		IPreferenceStore prefs = DebugUIPlugin.getDefault().getPreferenceStore();
//		Debug PreferencePage
		prefs.setDefault(IDebugUIConstants.PREF_BUILD_BEFORE_LAUNCH, true);
		prefs.setDefault(IInternalDebugUIConstants.PREF_SAVE_DIRTY_EDITORS_BEFORE_LAUNCH, MessageDialogWithToggle.PROMPT);
		prefs.setDefault(IDebugUIConstants.PREF_SHOW_DEBUG_PERSPECTIVE_DEFAULT, IDebugUIConstants.ID_DEBUG_PERSPECTIVE);
		prefs.setDefault(IDebugUIConstants.PREF_SHOW_RUN_PERSPECTIVE_DEFAULT, IDebugUIConstants.PERSPECTIVE_NONE);
		prefs.setDefault(IDebugUIConstants.PREF_AUTO_REMOVE_OLD_LAUNCHES, true);
		prefs.setDefault(IDebugUIConstants.PREF_ACTIVATE_WORKBENCH, true);
		prefs.setDefault(IInternalDebugUIConstants.PREF_ACTIVATE_DEBUG_VIEW, true);
		prefs.setDefault(IInternalDebugUIConstants.PREF_SWITCH_TO_PERSPECTIVE, MessageDialogWithToggle.NEVER);
		prefs.setDefault(IInternalDebugUIConstants.PREF_SWITCH_PERSPECTIVE_ON_SUSPEND, MessageDialogWithToggle.PROMPT);
		prefs.setDefault(IInternalDebugUIConstants.PREF_WAIT_FOR_BUILD, MessageDialogWithToggle.ALWAYS);
		prefs.setDefault(IDebugUIConstants.PREF_REUSE_EDITOR, true);
		prefs.setDefault(IDebugUIConstants.PREF_SKIP_BREAKPOINTS_DURING_RUN_TO_LINE, false);
		prefs.setDefault(IInternalDebugUIConstants.PREF_RELAUNCH_IN_DEBUG_MODE, MessageDialogWithToggle.NEVER);
		prefs.setDefault(IInternalDebugUIConstants.PREF_CONTINUE_WITH_COMPILE_ERROR, MessageDialogWithToggle.PROMPT);
		prefs.setDefault(IDebugPreferenceConstants.PREF_PROMPT_REMOVE_ALL_BREAKPOINTS, true);
		prefs.setDefault(IDebugPreferenceConstants.PREF_PROMPT_REMOVE_ALL_TRIGGER_BREAKPOINTS, true);
		prefs.setDefault(IDebugPreferenceConstants.PREF_PROMPT_REMOVE_BREAKPOINTS_FROM_CONTAINER, true);
		prefs.setDefault(IDebugPreferenceConstants.PREF_PROMPT_REMOVE_ALL_EXPRESSIONS, true);
		prefs.setDefault(IDebugPreferenceConstants.PREF_PROMPT_DISABLE_ALL_BREAKPOINTS, true);

		/**
		 * Context launching preferences. Appear on the the Launching preference page
		 *
		 * @since 3.3.0
		 */
		prefs.setDefault(IInternalDebugUIConstants.PREF_USE_CONTEXTUAL_LAUNCH, true);
		prefs.setDefault(IInternalDebugUIConstants.PREF_LAUNCH_PARENT_PROJECT, false);
		prefs.setDefault(IInternalDebugUIConstants.PREF_LAUNCH_LAST_IF_NOT_LAUNCHABLE, true);

		prefs.setDefault(IInternalDebugUIConstants.PREF_TERMINATE_AND_RELAUNCH_LAUNCH_ACTION, false);
		prefs.setDefault(IInternalDebugUIConstants.PREF_BREAKPOINT_SORTING_ORDER, IInternalDebugUIConstants.BREAKPOINT_SORTING_ORDER_NAME);

		//View Management preference page
		prefs.setDefault(IDebugUIConstants.PREF_MANAGE_VIEW_PERSPECTIVES, IDebugUIConstants.PREF_MANAGE_VIEW_PERSPECTIVES_DEFAULT);
		prefs.setDefault(IInternalDebugUIConstants.PREF_TRACK_VIEWS, true);

		//ConsolePreferencePage
		prefs.setDefault(IDebugPreferenceConstants.CONSOLE_WRAP, false);
		prefs.setDefault(IDebugPreferenceConstants.CONSOLE_WIDTH, 80);
		prefs.setDefault(IDebugPreferenceConstants.CONSOLE_OPEN_ON_OUT, true);
		prefs.setDefault(IDebugPreferenceConstants.CONSOLE_OPEN_ON_ERR, true);
		prefs.setDefault(IDebugPreferenceConstants.CONSOLE_LIMIT_CONSOLE_OUTPUT, true);
		prefs.setDefault(IDebugPreferenceConstants.CONSOLE_LOW_WATER_MARK, 80000);
		prefs.setDefault(IDebugPreferenceConstants.CONSOLE_HIGH_WATER_MARK, 100000);
		prefs.setDefault(IDebugPreferenceConstants.CONSOLE_TAB_WIDTH, 8);
		prefs.setDefault(IDebugPreferenceConstants.CONSOLE_INTERPRET_CONTROL_CHARACTERS, false);
		prefs.setDefault(IDebugPreferenceConstants.CONSOLE_INTERPRET_CR_AS_CONTROL_CHARACTER, true);

		// console colors
		setThemeBasedPreferences(prefs, false);

		PreferenceConverter.setDefault(prefs, IDebugUIConstants.PREF_CHANGED_VALUE_BACKGROUND, new RGB(255, 255, 0));
		PreferenceConverter.setDefault(prefs, IDebugUIConstants.PREF_MEMORY_HISTORY_UNKNOWN_COLOR, new RGB(114, 119, 129));
		PreferenceConverter.setDefault(prefs, IDebugUIConstants.PREF_MEMORY_HISTORY_KNOWN_COLOR, new RGB(0, 0, 0));

		//Launching Preference Page
		prefs.setDefault(IDebugUIConstants.PREF_MAX_HISTORY_SIZE, 10);
		prefs.setDefault(IInternalDebugUIConstants.PREF_REMOVE_FROM_LAUNCH_HISTORY, true);

		//VariableViewsPreferencePage
		prefs.setDefault(IDebugPreferenceConstants.VARIABLES_DETAIL_PANE_ORIENTATION, IDebugPreferenceConstants.VARIABLES_DETAIL_PANE_UNDERNEATH);
		PreferenceConverter.setDefault(prefs, IDebugUIConstants.PREF_CHANGED_DEBUG_ELEMENT_COLOR, new RGB(255, 0, 0));
		prefs.setDefault(IDebugPreferenceConstants.PREF_DETAIL_PANE_WORD_WRAP, false);
		prefs.setDefault(IDebugUIConstants.PREF_MAX_DETAIL_LENGTH, 10000);

		//Registers View
		prefs.setDefault(IDebugPreferenceConstants.REGISTERS_DETAIL_PANE_ORIENTATION, IDebugPreferenceConstants.VARIABLES_DETAIL_PANE_UNDERNEATH);

		// Breakpoints view default settings
		prefs.setDefault(IDebugPreferenceConstants.BREAKPOINTS_DETAIL_PANE_ORIENTATION, IDebugPreferenceConstants.VARIABLES_DETAIL_PANE_UNDERNEATH);

		// Expression View
		prefs.setDefault(IDebugPreferenceConstants.EXPRESSIONS_DETAIL_PANE_ORIENTATION, IDebugPreferenceConstants.VARIABLES_DETAIL_PANE_UNDERNEATH);

		// Module View
		prefs.setDefault(IDebugPreferenceConstants.MODULES_DETAIL_PANE_ORIENTATION, IDebugPreferenceConstants.VARIABLES_DETAIL_PANE_UNDERNEATH);

		// Variable/Expression view default settings
		prefs.setDefault(IDebugUIConstants.ID_VARIABLE_VIEW + '+' + "org.eclipse.debug.ui.ShowDetailPaneAction", true); //$NON-NLS-1$
		prefs.setDefault(IDebugUIConstants.ID_EXPRESSION_VIEW + '+' + "org.eclipse.debug.ui.ShowDetailPaneAction", true); //$NON-NLS-1$
		prefs.setDefault(IDebugUIConstants.ID_VARIABLE_VIEW + '+' + "org.eclipse.debug.ui.ShowTypeNamesAction", false); //$NON-NLS-1$
		prefs.setDefault(IDebugUIConstants.ID_EXPRESSION_VIEW + '+' + "org.eclipse.debug.ui.ShowTypeNamesAction", false);		 //$NON-NLS-1$

		// set default for column size preference
		prefs.setDefault(IDebugPreferenceConstants.PREF_COLUMN_SIZE,
				IDebugPreferenceConstants.PREF_COLUMN_SIZE_DEFAULT);

		// set default for row size preference
		prefs.setDefault(IDebugPreferenceConstants.PREF_ROW_SIZE,
				IDebugPreferenceConstants.PREF_ROW_SIZE_DEFAULT);

		// set default padded string
		prefs.setDefault(IDebugUIConstants.PREF_PADDED_STR,
				IDebugPreferenceConstants.PREF_PADDED_STR_DEFAULT);

		// set default code page for ascii and ebcdic
		prefs.setDefault(IDebugUIConstants.PREF_DEFAULT_ASCII_CODE_PAGE,
				IDebugPreferenceConstants.DEFAULT_ASCII_CP);
		prefs.setDefault(IDebugUIConstants.PREF_DEFAULT_EBCDIC_CODE_PAGE,
				IDebugPreferenceConstants.DEFAULT_EBCDIC_CP);

		if (MemoryViewUtil.isLinuxGTK()) {
			prefs.setDefault(IDebugPreferenceConstants.PREF_DYNAMIC_LOAD_MEM, false);
		}
		else {
			prefs.setDefault(IDebugPreferenceConstants.PREF_DYNAMIC_LOAD_MEM, true);
		}

		prefs.setDefault(IDebugPreferenceConstants.PREF_TABLE_RENDERING_PAGE_SIZE, IDebugPreferenceConstants.DEFAULT_PAGE_SIZE);
		prefs.setDefault(IDebugPreferenceConstants.PREF_RESET_MEMORY_BLOCK, IDebugPreferenceConstants.RESET_VISIBLE);
		prefs.setDefault(IDebugPreferenceConstants.PREF_TABLE_RENDERING_PRE_BUFFER_SIZE,IDebugPreferenceConstants.DEFAULT_PAGE_SIZE);
		prefs.setDefault(IDebugPreferenceConstants.PREF_TABLE_RENDERING_POST_BUFFER_SIZE,IDebugPreferenceConstants.DEFAULT_PAGE_SIZE);

		/**
		 * new launch configuration filtering options
		 * @since 3.2
		 */
		prefs.setDefault(IInternalDebugUIConstants.PREF_FILTER_LAUNCH_CLOSED, true);
		prefs.setDefault(IInternalDebugUIConstants.PREF_FILTER_LAUNCH_DELETED, true);
		prefs.setDefault(IInternalDebugUIConstants.PREF_FILTER_WORKING_SETS, false);
		prefs.setDefault(IInternalDebugUIConstants.PREF_FILTER_LAUNCH_TYPES, false);

		/**
		 * Debug view mode default
		 *
		 * @since 3.5
		 */
		prefs.setDefault(IDebugPreferenceConstants.DEBUG_VIEW_MODE, IDebugPreferenceConstants.DEBUG_VIEW_MODE_AUTO);
	}

	/**
	 * Returns the RGB for the given key in the given color registry.
	 *
	 * @param registry the color registry
	 * @param key the key for the constant in the registry
	 * @param defaultRGB the default RGB if no entry is found
	 * @return RGB the RGB
	 * @since 3.4
	 */
	private static RGB findRGB(ColorRegistry registry, String key, RGB defaultRGB) {
		if (registry == null) {
			return defaultRGB;
		}

		RGB rgb= registry.getRGB(key);
		if (rgb != null) {
			return rgb;
		}

		return defaultRGB;
	}

	/**
	 * Sets the default value and fires a property
	 * change event if necessary.
	 *
	 * @param store	the preference store
	 * @param key the preference key
	 * @param newValue the new value
	 * @param fireEvent <code>false</code> if no event should be fired
	 * @since 3.4
	 */
	private static void setDefault(IPreferenceStore store, String key, RGB newValue, boolean fireEvent) {
		if (!fireEvent) {
			PreferenceConverter.setDefault(store, key, newValue);
		} else {
			RGB oldValue= null;
			if (store.isDefault(key)) {
				oldValue= PreferenceConverter.getDefaultColor(store, key);
			}

			PreferenceConverter.setDefault(store, key, newValue);

			if (oldValue != null && !oldValue.equals(newValue)) {
				store.firePropertyChangeEvent(key, oldValue, newValue);
			}
			}
		}

	public static void setThemeBasedPreferences(final IPreferenceStore store, final boolean fireEvent) {
		Display display= PlatformUI.getWorkbench().getDisplay();
		Runnable runnable = () -> {
			ColorRegistry registry = null;
			if (PlatformUI.isWorkbenchRunning()) {
				registry = PlatformUI.getWorkbench().getThemeManager().getCurrentTheme().getColorRegistry();
			}
			setDefault(store, IDebugPreferenceConstants.CONSOLE_BAKGROUND_COLOR, findRGB(registry, IInternalDebugUIConstants.THEME_CONSOLE_COLOR_BACKGROUND, new RGB(255, 255, 255)), fireEvent);
			setDefault(store, IDebugPreferenceConstants.CONSOLE_SYS_OUT_COLOR, findRGB(registry, IInternalDebugUIConstants.THEME_CONSOLE_COLOR_STD_OUT, new RGB(0, 0, 0)), fireEvent);
			setDefault(store, IDebugPreferenceConstants.CONSOLE_SYS_IN_COLOR, findRGB(registry, IInternalDebugUIConstants.THEME_CONSOLE_COLOR_STD_IN, new RGB(0, 200, 125)), fireEvent);
			setDefault(store, IDebugPreferenceConstants.CONSOLE_SYS_ERR_COLOR, findRGB(registry, IInternalDebugUIConstants.THEME_CONSOLE_COLOR_STD_ERR, new RGB(255, 0, 0)), fireEvent);
		};
		if (display.getThread() == Thread.currentThread()) {
			runnable.run();
		} else {
			display.asyncExec(runnable);
		}
	}
}
