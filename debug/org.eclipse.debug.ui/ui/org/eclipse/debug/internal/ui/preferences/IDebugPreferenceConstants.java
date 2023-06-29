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
 *     QNX Software Systems - Mikhail Khodjaiants - Registers View (Bug 53640)
 *     Wind River Systems - Pawel Piech - Added Modules view (bug 211158)
 *     Patrick Chuong (Texas Instruments) - Improve usability of the breakpoint view (Bug 238956)
 *******************************************************************************/
package org.eclipse.debug.internal.ui.preferences;


import org.eclipse.debug.ui.IDebugUIConstants;

/**
 * Constants defining the keys to be used for accessing preferences
 * inside the debug ui plugin's preference bundle.
 *
 * In descriptions (of the keys) below describe the preference
 * stored at the given key. The type indicates type of the stored preferences
 *
 * The preference store is loaded by the plugin (DebugUIPlugin).
 * @see DebugUIPlugin.initializeDefaultPreferences(IPreferenceStore) - for initialization of the store
 */
public interface IDebugPreferenceConstants {

	/**
	 * The symbolic names for colors for displaying the content in the Console
	 * @see org.eclipse.jface.resource.ColorRegistry
	 */
	String CONSOLE_SYS_ERR_COLOR= "org.eclipse.debug.ui.errorColor"; //$NON-NLS-1$
	String CONSOLE_SYS_OUT_COLOR= "org.eclipse.debug.ui.outColor"; //$NON-NLS-1$
	String CONSOLE_SYS_IN_COLOR= "org.eclipse.debug.ui.inColor"; //$NON-NLS-1$
	String CONSOLE_BAKGROUND_COLOR= "org.eclipse.debug.ui.consoleBackground"; //$NON-NLS-1$

	/**
	 * @deprecated use IDebugUIConstants.PREF_MEMORY_HISTORY_UNKNOWN_COLOR instead
	 */
	@Deprecated String MEMORY_VIEW_UNBUFFERED_LINE_COLOR = IDebugUIConstants.PLUGIN_ID + ".MemoryViewLineColor"; //$NON-NLS-1$

	/**
	 * @deprecated use IDebugUIConstants.PREF_MEMORY_HISTORY_KNOWN_COLOR instead
	 */
	@Deprecated String MEMORY_VIEW_BUFFERED_LINE_COLOR = IDebugUIConstants.PLUGIN_ID + ".MemoryViewBufferedLineColor"; //$NON-NLS-1$

	/**
	 * (boolean) Whether or not the text in the console will wrap
	 */
	String CONSOLE_WRAP= "Console.wrap"; //$NON-NLS-1$

	/**
	 * (int) The maximum console character width, if wrapping.
	 */
	String CONSOLE_WIDTH = "Console.width"; //$NON-NLS-1$

	/**
	 * (boolean) Whether or not the console view will enable auto scroll lock
	 */
	String CONSOLE_AUTO_SCROLL_LOCK = "DEBUG.autoScrollLock"; //$NON-NLS-1$

	/**
	 * (boolean) Whether or not the console view is shown
	 * when there is program output.
	 */
	String CONSOLE_OPEN_ON_OUT= "DEBUG.consoleOpenOnOut"; //$NON-NLS-1$
	/**
	 * (boolean) Whether or not the console view is shown
	 * when there is program error.
	 */
	String CONSOLE_OPEN_ON_ERR= "DEBUG.consoleOpenOnErr"; //$NON-NLS-1$

	/**
	 * Console buffer high and low water marks
	 */
	String CONSOLE_LIMIT_CONSOLE_OUTPUT = "Console.limitConsoleOutput"; //$NON-NLS-1$
	String CONSOLE_LOW_WATER_MARK = "Console.lowWaterMark"; //$NON-NLS-1$
	String CONSOLE_HIGH_WATER_MARK = "Console.highWaterMark"; //$NON-NLS-1$

	/**
	 * Integer preference specifying the number of spaces composing a
	 * tab in the console.
	 *
	 * @since 3.0
	 */
	String CONSOLE_TAB_WIDTH= "Console.console_tab_width"; //$NON-NLS-1$

	/**
	 * (boolean) If <code>true</code> console will interpret ASCII control
	 * characters like <code>\b</code> received from stdout or stderr (or any other
	 * connected output stream).
	 * <p>
	 * If <code>false</code> control characters are appended to console like any
	 * other character. Since they are usually not printable they may be invisible
	 * or result in some Unicode default representation.
	 * </p>
	 */
	String CONSOLE_INTERPRET_CONTROL_CHARACTERS = "Console.interpret_control_characters"; //$NON-NLS-1$

	/**
	 * (boolean) Only used if {@link #CONSOLE_INTERPRET_CONTROL_CHARACTERS} is
	 * <code>true</code>.
	 * <p>
	 * If <code>true</code> carriage returns are handled with there usual control
	 * character interpretation. (move output cursor to begin of line)
	 * </p>
	 * <p>
	 * If <code>false</code> carriage returns are not handled special and may result
	 * in line breaks since they are usually legal line delimiter.
	 * </p>
	 */
	String CONSOLE_INTERPRET_CR_AS_CONTROL_CHARACTER = "Console.interpret_cr_as_control_characters"; //$NON-NLS-1$

	/**
	 * The orientation of the detail view in the VariablesView
	 */
	String VARIABLES_DETAIL_PANE_ORIENTATION = "Variables.detail.orientation"; //$NON-NLS-1$
	String EXPRESSIONS_DETAIL_PANE_ORIENTATION = "Expressions.detail.orientation"; //$NON-NLS-1$
	String REGISTERS_DETAIL_PANE_ORIENTATION = "Registers.detail.orientation"; //$NON-NLS-1$
	String MODULES_DETAIL_PANE_ORIENTATION = "Modules.detail.orientation"; //$NON-NLS-1$
	String BREAKPOINTS_DETAIL_PANE_ORIENTATION = "Breakpoints.detail.orientation"; //$NON-NLS-1$
	String VARIABLES_DETAIL_PANE_RIGHT = "Variables.detail.orientation.right"; //$NON-NLS-1$
	String VARIABLES_DETAIL_PANE_UNDERNEATH = "Variables.detail.orientation.underneath"; //$NON-NLS-1$
	String VARIABLES_DETAIL_PANE_HIDDEN = "Variables.detail.orientation.hidden"; //$NON-NLS-1$
	String VARIABLES_DETAIL_PANE_AUTO = "Variables.detail.orientation.auto"; //$NON-NLS-1$

	/**
	 * Memento for the last selected launch config in the
	 * launch config dialog.
	 *
	 * @deprecated no longer supported
	 */
	@Deprecated String PREF_LAST_LAUNCH_CONFIGURATION_SELECTION = IDebugUIConstants.PLUGIN_ID + ".lastLaunchConfigSelection"; //$NON-NLS-1$

	/**
	 * The maximum size of the launch history list
	 */
	int MAX_LAUNCH_HISTORY_SIZE= 40;

	/**
	 * Boolean preference controlling whether the text in the detail panes is
	 * wrapped. When <code>true</code> the text in the detail panes will be
	 * wrapped in new variable view.
	 *
	 * @since 2.1
	 */
	String PREF_DETAIL_PANE_WORD_WRAP = IDebugUIConstants.PLUGIN_ID + ".detail_pane_word_wrap"; //$NON-NLS-1$

	/**
	 * Column size preference for the Memory View
	 *
	 * @since 3.0
	 */
	String PREF_COLUMN_SIZE = "org.eclipse.debug.ui.memory.columnSize"; //$NON-NLS-1$

	/**
	 * Default column size for the Memory View
	 *
	 * @since 3.0
	 */
	int PREF_COLUMN_SIZE_DEFAULT = 4;


	/**
	 * Row size preference for Memory View
	 *
	 * @since 3.2
	 */
	String PREF_ROW_SIZE = "org.eclipse.debug.ui.memory.rowSize"; //$NON-NLS-1$

	/**
	 * Default row size for the Memory View
	 *
	 * @since 3.2
	 */
	int PREF_ROW_SIZE_DEFAULT = 16;

	/**
	 * Stores the boolean preference of whether to prompt when disabling all
	 * breakpoints.
	 *
	 * @since 3.15
	 */
	String PREF_PROMPT_DISABLE_ALL_BREAKPOINTS = IDebugUIConstants.PLUGIN_ID + ".disable_all_breakpoints_prompt"; //$NON-NLS-1$

	/**
	 * Stores the boolean preference of whether to prompt when removing all
	 * breakpoints.
	 *
	 * @since 3.3
	 */
	String PREF_PROMPT_REMOVE_ALL_BREAKPOINTS = IDebugUIConstants.PLUGIN_ID + ".remove_all_breakpoints_prompt"; //$NON-NLS-1$

	/**
	 * Stores the boolean preference of whether to prompt when removing all
	 * breakpoints.
	 *
	 * @since 3.11
	 */
	String PREF_PROMPT_REMOVE_ALL_TRIGGER_BREAKPOINTS = IDebugUIConstants.PLUGIN_ID + ".remove_all_trigger_breakpoints_prompt"; //$NON-NLS-1$

	/**
	 * stores the boolean preference of whether or not to prompt when removing all of the breakpoints
	 * from a breakpoints container.
	 * @since 3.3
	 */
	String PREF_PROMPT_REMOVE_BREAKPOINTS_FROM_CONTAINER = IDebugUIConstants.PLUGIN_ID + ".remove_breakpoints_from_container_prompt"; //$NON-NLS-1$

	/**
	 * Stores the boolean preference of whether to prompt when removing all expressions.
	 * @since 3.5
	 */
	String PREF_PROMPT_REMOVE_ALL_EXPRESSIONS = IDebugUIConstants.PLUGIN_ID + ".remove_all_expressions_prompt"; //$NON-NLS-1$

	/**
	 * Default padded string for renderings
	 *
	 * @since 3.1
	 */
	String PREF_PADDED_STR_DEFAULT = "??"; //$NON-NLS-1$

	/**
	 * Default ASCII code page if ASCII code page preference is not set.
	 * @since 3.1
	 */
	String DEFAULT_ASCII_CP = "CP1252"; //$NON-NLS-1$


	/**
	 * Default EBCDIC code page if EBCDIC code page preference is not set.
	 * @since 3.1
	 */
	String DEFAULT_EBCDIC_CP = "CP037"; //$NON-NLS-1$

	/**
	 * Preference to determine if table rendering should dynamically load
	 * memory as the user scrolls
	 *
	 * @since 3.1
	 */
	String PREF_DYNAMIC_LOAD_MEM = "org.eclpise.debug.ui.memory.dynamicLoad"; //$NON-NLS-1$


	/**
	 * Size of buffer in a table rendering when dynamic loading mode is off.
	 *
	 * @since 3.1
	 */
	String PREF_TABLE_RENDERING_PAGE_SIZE = "org.eclispe.debug.ui.memory.pageSize"; //$NON-NLS-1$

	/**
	 * Default page size when dynamic loading mode is off.  This preference is stored
	 * in number of lines.
	 *
	 * @since 3.1
	 */
	int DEFAULT_PAGE_SIZE = 20;
	/**
	 * Preference for defining behavior when resetting a memory monitor.
	 * Possible values:
	 * - RESET_AL - reset all renderings regardless if they are visible or not
	 * - RESET_VISIBLE - reset visible renderings
	 *
	 * @since 3.2
	 */
	String PREF_RESET_MEMORY_BLOCK = IDebugUIConstants.PLUGIN_ID + ".reset_memory_block"; //$NON-NLS-1$

	/**
	 * Constant to indicate that the memory view will reset all memory renderings when the reset
	 * memory monitor action is invoked.
	 *
	 * @since 3.2
	 */
	String RESET_ALL = IDebugUIConstants.PLUGIN_ID + "resetMemoryBlock.all"; //$NON-NLS-1$


	/**
	 * Constant to indicate that the memory view will reset visible memory renderings when
	 * the reset memory monitor action is invoked
	 *
	 * @since 3.2
	 */
	String RESET_VISIBLE = IDebugUIConstants.PLUGIN_ID + "resetMemoryBlock.visible"; //$NON-NLS-1$

	/**
	 * Preference identifier for the row size in a table rendering.
	 * This preference is expected to be saved by an </code>IPersistableDebugElement</code>.
	 * Memory Blocks can optionally provide and save this preference to customize
	 * the initial format of a table rendering.
	 *
	 * The value of this property is an Integer.  The value can be one of the
	 * following values:  1, 2, 4, 8, 16.  This value must be greater than
	 * <code>PREF_COL_SIZE_BY_MODEL</code> and must also be divisible by <code>PREF_COL_SIZE_BY_MODEL</code>.
	 *
	 * @since 3.2
	 */
	String PREF_ROW_SIZE_BY_MODEL = "org.eclipse.debug.ui.AbstractTableRendering.rowSize"; //$NON-NLS-1$
	/**
	 * Preference identifier for the column size in a table rendering.
	 * This preference is expected to be saved by an <code>IPersistableDebugElement</code>.
	 * Memory Blocks can optionally provide and save this preference to customize
	 * the initial format of a table rendering.
	 *
	 * The value of this property is an Integer.  The value can be one of the
	 * following values:  1, 2, 4, 8, 16.  This value must be smaller than
	 * <code>PREF_ROW_SIZE_BY_MODEL</code>.  <code>PREF_ROW_SIZE_BY_MODEL</code> must be divisible by <code>PREF_COL_SIZE_BY_MODEL</code>.
	 *
	 * @since 3.2
	 */
	String PREF_COL_SIZE_BY_MODEL = "org.eclipse.debug.ui.AbstractTableRendering.colSize"; //$NON-NLS-1$

	/**
	 * Number of lines to preload before the visible region in the table rendering
	 *
	 * @since 3.3
	 */
	String PREF_TABLE_RENDERING_PRE_BUFFER_SIZE = "org.eclispe.debug.ui.memory.preBufferSize"; //$NON-NLS-1$

	/**
	 * Number of lines to preload after the visible region in the table rendering
	 *
	 * @since 3.3
	 */
	String PREF_TABLE_RENDERING_POST_BUFFER_SIZE = "org.eclispe.debug.ui.memory.postBufferSize"; //$NON-NLS-1$

	/**
	 * The layout mode in Debug view.
	 *
	 * @since 3.5
	 */
	String DEBUG_VIEW_MODE = "org.eclispe.debug.ui.Debug_view.mode"; //$NON-NLS-1$
	String DEBUG_VIEW_MODE_AUTO = "Debug_view.mode.auto"; //$NON-NLS-1$
	String DEBUG_VIEW_MODE_COMPACT = "Debug_view.mode.compact"; //$NON-NLS-1$
	String DEBUG_VIEW_MODE_FULL = "Debug_view.mode.full"; //$NON-NLS-1$

	/**
	 * Preference whether to auto-expand in the breadcrumb drop-down viewers.
	 *
	 * @since 3.5
	 */
	String DEBUG_VIEW_BREADCRUMB_AUTO_EXPAND_DROP_DOWN = "org.eclispe.debug.ui.Debug_view.Breadcrumb.dropDownAutoexpand"; //$NON-NLS-1$

	/**
	 * Perspectives in which the debug toolbar is hidden.
	 *
	 * @since 3.8
	 */
	String DEBUG_VIEW_TOOLBAR_HIDDEN_PERSPECTIVES = "org.eclispe.debug.ui.Debug_view.debug_toolbar_hidden_perspectives"; //$NON-NLS-1$
}



