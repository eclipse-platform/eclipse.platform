/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
 *     QNX Software Systems - Mikhail Khodjaiants - Bug 114664
 *     Wind River Systems - Pawel Piech - Added Modules view (bug 211158)
 *     Lucas Bullen (Red Hat Inc.) - Bug 518652
 *     Axel Richard (Obeo) - Bug 41353 - Launch configurations prototypes
 *******************************************************************************/
package org.eclipse.debug.internal.ui;

import org.eclipse.debug.internal.ui.views.memory.MemoryBlocksTreeViewPane;
import org.eclipse.debug.internal.ui.views.memory.RenderingViewPane;
import org.eclipse.debug.ui.IDebugUIConstants;


public interface IInternalDebugUIConstants {

	String DIALOGSTORE_LASTEXTJAR= "org.eclipse.debug.ui.lastextjar"; //$NON-NLS-1$

	//Folders
	String ID_NAVIGATOR_FOLDER_VIEW= "org.eclipse.debug.internal.ui.NavigatorFolderView"; //$NON-NLS-1$
	String ID_TOOLS_FOLDER_VIEW= "org.eclipse.debug.internal.ui.ToolsFolderView"; //$NON-NLS-1$
	String ID_CONSOLE_FOLDER_VIEW= "org.eclipse.debug.internal.ui.ConsoleFolderView"; //$NON-NLS-1$
	String ID_OUTLINE_FOLDER_VIEW= "org.eclipse.debug.internal.ui.OutlineFolderView"; //$NON-NLS-1$

	// tool images
	String IMG_LCL_COLLAPSE_ALL = "IMG_LCL_COLLAPSE_ALL"; //$NON-NLS-1$
	String IMG_LCL_TERMINATE = "IMG_LCL_TERMINATE"; //$NON-NLS-1$
	String IMG_LCL_SHOW_LOGICAL_STRUCTURE = "IMG_LCL_SHOW_LOGICAL_STRUCTURE"; //$NON-NLS-1$
	String IMG_LCL_RUN_TO_LINE= "IMG_LCL_RUN_TO_LINE"; //$NON-NLS-1$
	String IMG_SRC_LOOKUP_MENU = "IMG_SRC_LOOKUP_MENU"; //$NON-NLS-1$

	/**
	 * @deprecated Use IDebugUIConstants#IMG_LCL_ADD
	 */
	@Deprecated String IMG_LCL_MONITOR_EXPRESSION= IDebugUIConstants.IMG_LCL_ADD;
	String IMG_LCL_REMOVE_MEMORY= "IMG_LCL_REMOVE_MEMORY"; //$NON-NLS-1$
	String IMG_LCL_RESET_MEMORY= "IMG_LCL_RESET_MEMORY";//$NON-NLS-1$
	String IMG_LCL_COPY_VIEW_TO_CLIPBOARD = "IMG_LCL_COPY_VIEW_TO_CLIPBOARD"; //$NON-NLS-1$
	String IMG_LCL_PRINT_TOP_VIEW_TAB = "IMG_LCL_PRINT_TOP_VIEW_TAB"; //$NON-NLS-1$

	// disabled local tool images
	String IMG_DLCL_LOCK= "IMG_DLCL_LOCK"; //$NON-NLS-1$
	String IMG_DLCL_DETAIL_PANE= "IMG_DLCL_DETAIL_PANE"; //$NON-NLS-1$
	String IMG_DLCL_CHANGE_VARIABLE_VALUE= "IMG_DLCL_CHANGE_VARIABLE_VALUE"; //$NON-NLS-1$
	String IMG_DLCL_TYPE_NAMES= "IMG_DLCL_TYPE_NAMES"; //$NON-NLS-1$
	String IMG_DLCL_SHOW_LOGICAL_STRUCTURE= "IMG_DLCL_SHOW_LOGICAL_STRUCTURE"; //$NON-NLS-1$
	String IMG_DLCL_DETAIL_PANE_UNDER= "IMG_DLCL_DETAIL_PANE_UNDER"; //$NON-NLS-1$
	String IMG_DLCL_DETAIL_PANE_RIGHT= "IMG_DLCL_DETAIL_PANE_RIGHT"; //$NON-NLS-1$
	String IMG_DLCL_DETAIL_PANE_HIDE= "IMG_DLCL_DETAIL_PANE_HIDE"; //$NON-NLS-1$
	String IMG_DLCL_DETAIL_PANE_AUTO= "IMG_DLCL_DETAIL_PANE_AUTO"; //$NON-NLS-1$
	String IMG_DLCL_COLLAPSE_ALL = "IMG_DLCL_COLLAPSE_ALL"; //$NON-NLS-1$
	String IMG_DLCL_TERMINATE = "IMG_DLCL_TERMINATE"; //$NON-NLS-1$
	String IMG_DLCL_REMOVE_ALL = "IMG_DLCL_REMOVE_ALL"; //$NON-NLS-1$
	String IMG_DLCL_REMOVE = "IMG_DLCL_REMOVE"; //$NON-NLS-1$
	String IMG_DLCL_RUN_TO_LINE= "IMG_DLCL_RUN_TO_LINE"; //$NON-NLS-1$
	String IMG_SRC_LOOKUP_MENU_DLCL = "IMG_SRC_LOOKUP_MENU_DISABLED"; //$NON-NLS-1$
	String IMG_DLCL_MONITOR_EXPRESSION= "IMG_DLCL_MONITOR_EXPRESSION"; //$NON-NLS-1$
	String IMG_DLCL_REMOVE_MEMORY= "IMG_DLCL_REMOVE_MEMORY"; //$NON-NLS-1$
	String IMG_DLCL_RESET_MEMORY= "IMG_DLCL_RESET_MEMORY"; //$NON-NLS-1$
	String IMG_DLCL_COPY_VIEW_TO_CLIPBOARD= "IMG_DLCL_COPY_VIEW_TO_CLIPBOARD"; //$NON-NLS-1$
	String IMG_DLCL_PRINT_TOP_VIEW_TAB= "IMG_DLCL_PRINT_TOP_VIEW_TAB"; //$NON-NLS-1$
	String IMG_DLCL_NEW_CONFIG = "IMG_DLCL_NEW_CONFIG"; //$NON-NLS-1$
	String IMG_DLCL_DUPLICATE_CONFIG = "IMG_DLCL_DUPLICATE_CONFIG"; //$NON-NLS-1$
	String IMG_DLCL_DELETE_CONFIG = "IMG_DLCL_DELETE_CONFIG"; //$NON-NLS-1$
	String IMG_DLCL_FILTER_CONFIGS = "IMG_DLCL_FILTER_CONFIGS"; //$NON-NLS-1$
	String IMG_DLCL_NEW_PROTO = "IMG_DLCL_NEW_PROTO"; //$NON-NLS-1$
	String IMG_DLCL_LINK_PROTO = "IMG_DLCL_LINK_PROTO"; //$NON-NLS-1$
	String IMG_DLCL_UNLINK_PROTO = "IMG_DLCL_UNLINK_PROTO"; //$NON-NLS-1$
	String IMG_DLCL_RESET_PROTO = "IMG_DLCL_RESET_PROTO"; //$NON-NLS-1$
	String IMG_DLCL_SUSPEND = "IMG_DLCL_SUSPEND"; //$NON-NLS-1$
	String IMG_DLCL_RESUME = "IMG_DLCL_RESUME"; //$NON-NLS-1$
	String IMG_DLCL_STEP_RETURN = "IMG_DLCL_STEP_RETURN"; //$NON-NLS-1$
	String IMG_DLCL_STEP_OVER = "IMG_DLCL_STEP_OVER"; //$NON-NLS-1$
	String IMG_DLCL_STEP_INTO = "IMG_DLCL_STEP_INTO"; //$NON-NLS-1$
	String IMG_DLCL_TERMINATE_AND_REMOVE = "IMG_DLCL_TERMINATE_AND_REMOVE"; //$NON-NLS-1$
	String IMG_DLCL_TERMINATE_ALL = "IMG_DLCL_TERMINATE_ALL"; //$NON-NLS-1$
	String IMG_DLCL_TERMINATE_AND_RELAUNCH = "IMG_DLCL_TERMINATE_AND_RELAUNCH"; //$NON-NLS-1$
	String IMG_DLCL_TOGGLE_STEP_FILTERS = "IMG_DLCL_TOGGLE_STEP_FILTERS"; //$NON-NLS-1$
	String IMG_DLCL_NEXT_THREAD = "IMG_DLCL_NEXT_THREAD"; //$NON-NLS-1$
	String IMG_DLCL_PREVIOUS_THREAD = "IMG_DLCL_PREVIOUS_THREAD"; //$NON-NLS-1$
	String IMG_DLCL_RESTART = "IMG_DLCL_RESTART"; //$NON-NLS-1$
	String IMG_DLCL_EXPORT_CONFIG = "IMG_DLCL_EXPORT_CONFIG"; //$NON-NLS-1$

	//TODO: Move this IDebugUIConstants. Created too late in 3.2 cycle to add API.
	//The enabled icon is already API.
	String IMG_DLCL_DISCONNECT= "IMG_DLCL_DISCONNECT"; //$NON-NLS-1$

	// enabled local tool images
	String IMG_ELCL_LOCK= "IMG_ELCL_LOCK"; //$NON-NLS-1$
	String IMG_ELCL_DETAIL_PANE= "IMG_ELCL_DETAIL_PANE"; //$NON-NLS-1$
	String IMG_ELCL_CHANGE_VARIABLE_VALUE= "IMG_ELCL_CHANGE_VARIABLE_VALUE"; //$NON-NLS-1$
	String IMG_ELCL_TYPE_NAMES= "IMG_ELCL_TYPE_NAMES"; //$NON-NLS-1$
	String IMG_ELCL_SHOW_LOGICAL_STRUCTURE= "IMG_ELCL_SHOW_LOGICAL_STRUCTURE"; //$NON-NLS-1$
	String IMG_ELCL_DETAIL_PANE_UNDER= "IMG_ELCL_DETAIL_PANE_UNDER"; //$NON-NLS-1$
	String IMG_ELCL_DETAIL_PANE_RIGHT= "IMG_ELCL_DETAIL_PANE_RIGHT"; //$NON-NLS-1$
	String IMG_ELCL_DETAIL_PANE_HIDE= "IMG_ELCL_DETAIL_PANE_HIDE"; //$NON-NLS-1$
	String IMG_ELCL_DETAIL_PANE_AUTO= "IMG_ELCL_DETAIL_PANE_AUTO"; //$NON-NLS-1$
	String IMG_ELCL_COLLAPSE_ALL = "IMG_ELCL_COLLAPSE_ALL"; //$NON-NLS-1$
	String IMG_ELCL_TERMINATE = "IMG_ELCL_TERMINATE"; //$NON-NLS-1$
	String IMG_SRC_LOOKUP_MENU_ELCL = "IMG_SRC_LOOKUP_MENU_ENABLED"; //$NON-NLS-1$
	String IMG_ELCL_MONITOR_EXPRESSION= "IMG_ELCL_MONITOR_EXPRESSION";  //$NON-NLS-1$
	String IMG_ELCL_REMOVE_MEMORY= "IMG_ELCL_REMOVE_MEMORY";  //$NON-NLS-1$
	String IMG_ELCL_RESET_MEMORY= "IMG_ELCL_RESET_MEMORY";  //$NON-NLS-1$
	String IMG_ELCL_COPY_VIEW_TO_CLIPBOARD= "IMG_ELCL_COPY_VIEW_TO_CLIPBOARD";  //$NON-NLS-1$
	String IMG_ELCL_PRINT_TOP_VIEW_TAB= "IMG_ELCL_PRINT_TOP_VIEW_TAB";  //$NON-NLS-1$
	String IMG_ELCL_REMOVE_ALL = "IMG_ELCL_REMOVE_ALL"; //$NON-NLS-1$
	String IMG_ELCL_REMOVE = "IMG_ELCL_REMOVE"; //$NON-NLS-1$
	String IMG_ELCL_HIERARCHICAL = "IMG_ELCL_HIERARCHICAL"; //$NON-NLS-1$
	String IMG_ELCL_HELP = "IMG_ELCL_HELP"; //$NON-NLS-1$
	String IMG_ELCL_NEW_CONFIG = "IMG_ELCL_NEW_CONFIG"; //$NON-NLS-1$
	String IMG_ELCL_DELETE_CONFIG = "IMG_ELCL_DELETE_CONFIG"; //$NON-NLS-1$
	String IMG_ELCL_FILTER_CONFIGS = "IMG_ELCL_FILTER_CONFIGS"; //$NON-NLS-1$
	String IMG_ELCL_DUPLICATE_CONFIG = "IMG_ELCL_DUPLICATE_CONFIG"; //$NON-NLS-1$
	String IMG_ELCL_NEW_PROTO = "IMG_ELCL_NEW_PROTO"; //$NON-NLS-1$
	String IMG_ELCL_LINK_PROTO = "IMG_ELCL_LINK_PROTO"; //$NON-NLS-1$
	String IMG_ELCL_UNLINK_PROTO = "IMG_ELCL_UNLINK_PROTO"; //$NON-NLS-1$
	String IMG_ELCL_RESET_PROTO = "IMG_ELCL_RESET_PROTO"; //$NON-NLS-1$
	String IMG_ELCL_SUSPEND = "IMG_ELCL_SUSPEND"; //$NON-NLS-1$
	String IMG_ELCL_RESUME = "IMG_ELCL_RESUME"; //$NON-NLS-1$
	String IMG_ELCL_STEP_RETURN = "IMG_ELCL_STEP_RETURN"; //$NON-NLS-1$
	String IMG_ELCL_STEP_OVER = "IMG_ELCL_STEP_OVER"; //$NON-NLS-1$
	String IMG_ELCL_STEP_INTO = "IMG_ELCL_STEP_INTO"; //$NON-NLS-1$
	String IMG_ELCL_DROP_TO_FRAME = "IMG_ELCL_DROP_TO_FRAME"; //$NON-NLS-1$
	String IMG_ELCL_TERMINATE_AND_REMOVE = "IMG_ELCL_TERMINATE_AND_REMOVE"; //$NON-NLS-1$
	String IMG_ELCL_TERMINATE_ALL = "IMG_ELCL_TERMINATE_ALL"; //$NON-NLS-1$
	String IMG_ELCL_TERMINATE_AND_RELAUNCH = "IMG_ELCL_TERMINATE_AND_RELAUNCH"; //$NON-NLS-1$
	String IMG_ELCL_TOGGLE_STEP_FILTERS = "IMG_ELCL_TOGGLE_STEP_FILTERS"; //$NON-NLS-1$
	String IMG_ELCL_STANDARD_OUT = "IMG_ELCL_STANDARD_OUT"; //$NON-NLS-1$
	String IMG_ELCL_STANDARD_ERR = "IMG_ELCL_STANDARD_ERR"; //$NON-NLS-1$
	String IMG_ELCL_NEXT_THREAD = "IMG_ELCL_NEXT_THREAD"; //$NON-NLS-1$
	String IMG_ELCL_PREVIOUS_THREAD = "IMG_ELCL_PREVIOUS_THREAD"; //$NON-NLS-1$
	String IMG_ELCL_RESTART = "IMG_ELCL_RESTART"; //$NON-NLS-1$
	String IMG_ELCL_DEBUG_VIEW_COMPACT_LAYOUT = "IMG_ELCL_DEBUG_VIEW_BREADCRUMB_LAYOUT"; //$NON-NLS-1$
	String IMG_ELCL_EXPORT_CONFIG = "IMG_ELCL_EXPORT_CONFIG"; //$NON-NLS-1$

	String IMG_OBJS_COMMON_TAB = "IMG_OBJS_COMMON_TAB"; //$NON-NLS-1$
	String IMG_OBJS_REFRESH_TAB = "IMG_OBJS_REFRESH_TAB"; //$NON-NLS-1$
	String IMG_OBJS_PERSPECTIVE_TAB = "IMG_OBJS_PERSPECTIVE_TAB"; //$NON-NLS-1$
	String IMG_OBJS_ARRAY_PARTITION = "IMG_OBJS_ARRAY_PARTITION"; //$NON-NLS-1$
	String IMG_SRC_LOOKUP_TAB = "IMG_SRC_LOOKUP_TAB"; //$NON-NLS-1$
	String IMG_OBJECT_MEMORY_CHANGED="IMG_OBJECT_MEMORY_CHANGED";  //$NON-NLS-1$
	String IMG_OBJECT_MEMORY="IMG_OBJECT_MEMORY";  //$NON-NLS-1$
	String IMG_OBJS_BREAKPOINT_TYPE="IMG_OBJ_BREAKPOINT_TYPE"; //$NON-NLS-1$
	String IMG_OBJS_LAUNCH_GROUP = "IMG_OBJ_LAUNCH_GROUP"; //$NON-NLS-1$
	String IMG_OBJS_CHECK = "IMG_OBJS_CHECK"; //$NON-NLS-1$
	String IMG_OBJS_UNCHECK = "IMG_OBJS_UNCHECK"; //$NON-NLS-1$
	String IMG_OBJS_PROTO_TAB = "IMG_OBJS_PROTO_TAB"; //$NON-NLS-1$

	// wizard images
	String IMG_WIZBAN_IMPORT_BREAKPOINTS = "IMG_WIZBAN_IMPORT_BREAKPOINTS"; //$NON-NLS-1$
	String IMG_WIZBAN_EXPORT_BREAKPOINTS = "IMG_WIZBAN_EXPORT_BREAKPOINTS"; //$NON-NLS-1$
	String IMG_WIZBAN_IMPORT_CONFIGS = "IMG_WIZBAN_IMPORT_CONFIGS"; //$NON-NLS-1$
	String IMG_WIZBAN_EXPORT_CONFIGS = "IMG_WIZBAN_EXPORT_CONFIGS"; //$NON-NLS-1$
	String IMG_ADD_SRC_LOC_WIZ = "IMG_ADD_SRC_LOCATION"; //$NON-NLS-1$
	String IMG_EDIT_SRC_LOC_WIZ = "IMG_EDIT_SRC_LOCATION"; //$NON-NLS-1$
	String IMG_ADD_SRC_DIR_WIZ = "IMG_ADD_SRC_DIRECTORY"; //$NON-NLS-1$
	String IMG_EDIT_SRC_DIR_WIZ = "IMG_EDIT_SRC_DIRECTORY"; //$NON-NLS-1$

	// internal preferences
	/**
	 * XML for perspective settings - see PerspectiveManager.
	 * @since 3.0
	 */
	String PREF_LAUNCH_PERSPECTIVES = IDebugUIConstants.PLUGIN_ID + ".PREF_LAUNCH_PERSPECTIVES"; //$NON-NLS-1$

	/**
	 * Preference for enabling/disabling launch configuration filtering based on project accessibility status
	 *
	 * @since 3.2
	 */
	String PREF_FILTER_LAUNCH_CLOSED = IDebugUIConstants.PLUGIN_ID + ".PREF_FILTER_LAUNCH_CLOSED"; //$NON-NLS-1$

	/**
	 * Preference for enabling/disabling launch configuration filtering based on project context
	 *
	 * @since 3.2
	 */
	String PREF_FILTER_LAUNCH_DELETED = IDebugUIConstants.PLUGIN_ID + ".PREF_FILTER_LAUNCH_DELETED"; //$NON-NLS-1$

	/**
	 * Preference for enabling/disabling filtering based on selected items from the launch configuration type table
	 * @since 3.2
	 */
	String PREF_FILTER_LAUNCH_TYPES = IDebugUIConstants.PLUGIN_ID + ".PREF_FILTER_LAUNCH_TYPES"; //$NON-NLS-1$

	/**
	 * Preference that saves which launch configuration types have been checked on the Launch Configurations pref page
	 * @since 3.2
	 */
	String PREF_FILTER_TYPE_LIST = IDebugUIConstants.PLUGIN_ID + ".PREF_FILTER_TYPE_LIST"; //$NON-NLS-1$

	/**
	 * Preference for filtering launch configurations based on the currently active working sets
	 * @since 3.2
	 */
	String PREF_FILTER_WORKING_SETS = IDebugUIConstants.PLUGIN_ID + ".PREF_FILTER_WORKING_SETS"; //$NON-NLS-1$

	/**
	 * Transparent overlay image identifier.
	 */
	String IMG_OVR_TRANSPARENT = "IMG_OVR_TRANSPARENT";  //$NON-NLS-1$

	/**
	 * Logical Structure overlay image identifier.
	 */
	String IMG_OVR_SHOW_LOGICAL_STRUCTURE = "IMG_OVR_SHOW_LOGICAL_STRUCTURE"; //$NON-NLS-1$

	/**
	 * Editor Id for the "Source Not Found" editor
	 */
	String ID_SOURCE_NOT_FOUND_EDITOR = "org.eclipse.debug.ui.NoSourceFoundEditor"; //$NON-NLS-1$

	/**
	 * Boolean preference indicating if contextual launch options should be visible
	 * to the user rather than the "run as" menu.
	 *
	 * @since 3.3.0
	 * CONTEXTLAUNCHING
	 */
	String PREF_USE_CONTEXTUAL_LAUNCH = IDebugUIConstants.PLUGIN_ID + ".UseContextualLaunch"; //$NON-NLS-1$

	/**
	 * Boolean preference indicating that if the selected resource is not launchable, then we should
	 * launch the last configuration that was launched.
	 *
	 * @since 3.3.0
	 * CONTEXTLAUNCHING
	 */
	String PREF_LAUNCH_LAST_IF_NOT_LAUNCHABLE = IDebugUIConstants.PLUGIN_ID + ".LaunchLastIfNotLaunchable"; //$NON-NLS-1$

	/**
	 * Boolean preference indicating if we should always consider the parent project when
	 * a selected context is not runnable
	 *
	 *  @since 3.3.0
	 *  CONTEXTLAUNCHING
	 */
	String PREF_LAUNCH_PARENT_PROJECT = IDebugUIConstants.PLUGIN_ID + ".LaunchParentProject"; //$NON-NLS-1$

	/**
	 * Boolean preference indicating if the user should be prompted prior to removing a launch configuration
	 * from the launch history drop down
	 *
	 * @since 3.4
	 */
	String PREF_REMOVE_FROM_LAUNCH_HISTORY = IDebugUIConstants.PLUGIN_ID + ".RemoveFromLaunchHistory"; //$NON-NLS-1$

	/**
	 * String preference controlling whether editors are saved before launching.
	 * Valid values are either "always", "never", or "prompt".
	 * If "always" or "never", launching will save editors (or not) automatically.
	 * If "prompt", the user will be prompted each time.
	 *
	 * @since 3.0
	 */
	String PREF_SAVE_DIRTY_EDITORS_BEFORE_LAUNCH = IDebugUIConstants.PLUGIN_ID + ".save_dirty_editors_before_launch"; //$NON-NLS-1$

	/**
	 * Preference specifying that all launches should be DEBUG_MODE if breakpoints exist in the workspace
	 * @since 3.0
	 */
	String PREF_RELAUNCH_IN_DEBUG_MODE = IDebugUIConstants.PLUGIN_ID + ".relaunch_in_debug_mode"; //$NON-NLS-1$

	/**
	 * Preference specifying that launches should continue when compile errors exist.
	 * @since 3.0
	 */
	String PREF_CONTINUE_WITH_COMPILE_ERROR = IDebugUIConstants.PLUGIN_ID + ".cancel_launch_with_compile_errors"; //$NON-NLS-1$

	/**
	 * Boolean preference controlling whether the debugger will force activate the debug
	 * view when a breakpoint is hit.
	 *
	 * @since 3.0
	 */
	String PREF_ACTIVATE_DEBUG_VIEW= IDebugUIConstants.PLUGIN_ID + ".activate_debug_view"; //$NON-NLS-1$

	/**
	 * String preference controlling whether the debugger switching to
	 * the associated perspective when launching.
	 * Valid values are either "always", "never", or "prompt".
	 * If "always" or "never", launching will switch perspectives (or not) automatically.
	 * If "prompt", the user will be prompted each time.
	 *
	 * @since 3.0
	 */
	String PREF_SWITCH_TO_PERSPECTIVE= IDebugUIConstants.PLUGIN_ID + ".switch_to_perspective"; //$NON-NLS-1$

	/**
	 * String preference controlling whether the debugger switching to
	 * the associated perspective when a launch suspends.
	 * Valid values are either "always", "never", or "prompt".
	 * If "always" or "never", suspension will switch perspectives (or not) automatically.
	 * If "prompt", the user will be prompted each time.
	 *
	 * @since 3.0
	 */
	String PREF_SWITCH_PERSPECTIVE_ON_SUSPEND= IDebugUIConstants.PLUGIN_ID + ".switch_perspective_on_suspend"; //$NON-NLS-1$

	/**
	 * String preference controlling whether the debugger waits for a currently
	 * pending (running or waiting) background build to complete before launching.
	 * Valid values are either "always", "never", or "prompt".
	 * If "always" or "never", launching will wait for builds to finish (or not) automatically.
	 * If "prompt", the user will be prompted each time.
	 *
	 * @since 3.0
	 */
	String PREF_WAIT_FOR_BUILD= IDebugUIConstants.PLUGIN_ID + ".wait_for_build"; //$NON-NLS-1$

	/**
	 * Font for Memory View
	 *
	 * @since 3.0
	 */
	String FONT_NAME = IDebugUIConstants.PLUGIN_ID + ".MemoryViewTableFont"; //$NON-NLS-1$

	int ADD_UNIT_PER_LINE = 16;	// number of addressable unit per line
	int CHAR_PER_BYTE = 2;		// number of characters to represent one byte

	/**
	 * Memory view's rendering view pane identifier (value <code>"org.eclipse.debug.ui.MemoryView.RenderingViewPane"</code>).
	 * @since 3.1
	 */
	String ID_RENDERING_VIEW_PANE = RenderingViewPane.RENDERING_VIEW_PANE_ID;

	/**
	 * Memory view's memory block tree viewer's identifier. (value <code>"org.eclipse.debug.ui.MemoryView.MemoryBlocksTreeViewPane"</code>).
	 * @since 3.1
	 */
	String ID_MEMORY_BLOCK_TREE_VIEWER = MemoryBlocksTreeViewPane.PANE_ID;

	/**
	 * Memory view image identifier
	 *
	 * @since 3.0
	 * */
	String IMG_CVIEW_MEMORY_VIEW= "IMG_CVIEW_MEMORY_VIEW";  //$NON-NLS-1$


	/**
	 * Modules view image identifier
	 *
	 * @since 3.4
	 * */
	String IMG_CVIEW_MODULES_VIEW= "IMG_CVIEW_MODULES_VIEW";  //$NON-NLS-1$

	/**
	 * Boolean preference controlling whether the debug view tracks
	 * views as the user manually opens/closes them for the purpose
	 * of not automatically opening/closing such views.
	 *
	 * @since 3.0
	 */
	String PREF_TRACK_VIEWS= IDebugUIConstants.PLUGIN_ID + ".track_views"; //$NON-NLS-1$

	/**
	 * Comma separated list of view ids closed by the user, that have view context bindings.
	 * @since 3.2
	 */
	String PREF_USER_VIEW_BINDINGS = IDebugUIConstants.PLUGIN_ID + ".user_view_bindings"; //$NON-NLS-1$

	/**
	 * Preference storing memento for the default breakpoint working set in
	 * the breakpoints view.
	 *
	 * @since 3.1
	 */
	String MEMENTO_BREAKPOINT_WORKING_SET_NAME = DebugUIPlugin.getUniqueIdentifier() + ".MEMENTO_BREAKPOINT_WORKING_SET_NAME"; //$NON-NLS-1$

	/**
	 * Breakpoint working set identifier.
	 *
	 * @since 3.1
	 * @deprecated There is an API equivalent constant that should be used, see
	 * {@link IDebugUIConstants#BREAKPOINT_WORKINGSET_ID}
	 */
	@Deprecated String ID_BREAKPOINT_WORKINGSET = "org.eclipse.debug.ui.breakpointWorkingSet"; //$NON-NLS-1$


	/**
	 * Address at the beginning of a page in a table rendering.
	 * @since 3.1
	 */
	String PROPERTY_PAGE_START_ADDRESS = "pageStart"; //$NON-NLS-1$


	/**
	 * This constant is used as a "quick-fix" for the issue of breakpoint to working set
	 * persistence when the state of a project changes.
	 *
	 * @since 3.2
	 */
	String WORKING_SET_NAME = "workingset_name"; //$NON-NLS-1$

	/**
	 * This constant is used as a "quick-fix" for the issue of breakpoint to working set
	 * persistence when the state of a project changes.
	 *
	 * @since 3.2
	 */
	String WORKING_SET_ID = "workingset_id";  //$NON-NLS-1$

	/**
	 * Annotation type identifier for annotation of the an instruction
	 * pointer with a dynamic image. Value is <code>org.eclipse.debug.ui.dynamicIP</code>,
	 * identifying a <code>org.eclipse.ui.editors.markerAnnotationSpecification</code>
	 * extension.
	 *
	 * @since 3.2
	 */
	String ANNOTATION_TYPE_DYNAMIC_INSTRUCTION_POINTER = "org.eclipse.debug.ui.dynamicIP"; //$NON-NLS-1$

	/**
	 * Identifier of the external tool builder launch category. Defined here since
	 * external tools is actually a dependent plug-in.
	 *
	 * @since 3.4
	 */
	String ID_EXTERNAL_TOOL_BUILDER_LAUNCH_CATEGORY = "org.eclipse.ui.externaltools.builder";  //$NON-NLS-1$

	//themes

	/**
	 * Theme color definition for process console background color.
	 *
	 * @since 3.4
	 */
	String THEME_CONSOLE_COLOR_BACKGROUND= "org.eclipse.debug.ui.console.background"; //$NON-NLS-1$

	/**
	 * Theme color definition for process console standard out.
	 *
	 * @since 3.4
	 */
	String THEME_CONSOLE_COLOR_STD_OUT= "org.eclipse.debug.ui.console.stream.out"; //$NON-NLS-1$

	/**
	 * Theme color definition for process console standard in.
	 *
	 * @since 3.4
	 */
	String THEME_CONSOLE_COLOR_STD_IN= "org.eclipse.debug.ui.console.stream.in"; //$NON-NLS-1$

	/**
	 * Theme color definition for process console standard err.
	 *
	 * @since 3.4
	 */
	String THEME_CONSOLE_COLOR_STD_ERR = "org.eclipse.debug.ui.console.stream.err"; //$NON-NLS-1$


	/**
	 * Boolean preference indicating if launch action should launch or terminate
	 * and relaunch by default
	 *
	 * @since 3.11
	 */
	String PREF_TERMINATE_AND_RELAUNCH_LAUNCH_ACTION = IDebugUIConstants.PLUGIN_ID + ".RelaunchAndTerminateLaunchAction"; //$NON-NLS-1$

	/**
	 * Int preference indicating the sorting order of breakpoints
	 *
	 * @since 3.12
	 */
	String PREF_BREAKPOINT_SORTING_ORDER = IDebugUIConstants.PLUGIN_ID + ".BreakpointSortingOrder"; //$NON-NLS-1$

	/**
	 * Int indicating the breakpoints will be sorted by name
	 *
	 * @since 3.12
	 */
	int BREAKPOINT_SORTING_ORDER_NAME = 0;

	/**
	 * Int indicating the breakpoints will be sorted by Creation Time
	 *
	 * @since 3.12
	 */
	int BREAKPOINT_SORTING_ORDER_CREATION_TIME = 1;

}
