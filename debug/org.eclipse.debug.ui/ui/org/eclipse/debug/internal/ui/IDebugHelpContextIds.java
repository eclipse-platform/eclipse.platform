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
 *     QNX Software Systems - Mikhail Khodjaiants - Registers View (Bug 53640)
 *     Wind River Systems - Pawel Piech - Added Modules view (bug 211158)
 *     Axel Richard (Obeo) - Bug 41353 - Launch configurations prototypes
 *******************************************************************************/
package org.eclipse.debug.internal.ui;


import org.eclipse.debug.ui.IDebugUIConstants;

/**
 * Help context ids for the debug ui.
 * <p>
 * This interface contains constants only; it is not intended to be implemented
 * or extended.
 * </p>
 *
 */
public interface IDebugHelpContextIds {

	String PREFIX = IDebugUIConstants.PLUGIN_ID + "."; //$NON-NLS-1$

	// Actions
	String CHANGE_VALUE_ACTION = PREFIX + "change_value_action_context"; //$NON-NLS-1$
	String OPEN_BREAKPOINT_ACTION = PREFIX + "open_breakpoint_action_context"; //$NON-NLS-1$
	String RELAUNCH_HISTORY_ACTION = PREFIX + "relaunch_history_action_context"; //$NON-NLS-1$
	String SHOW_BREAKPOINTS_FOR_MODEL_ACTION = PREFIX + "show_breakpoints_for_model_action_context"; //$NON-NLS-1$
	String COPY_BREAKPOINTS_ACTION = PREFIX + "copy_breakpoints_action_context"; //$NON-NLS-1$
	String PASTE_BREAKPOINTS_ACTION = PREFIX + "paste_breakpoints_action_context"; //$NON-NLS-1$
	String SHOW_TYPES_ACTION = PREFIX + "show_types_action_context"; //$NON-NLS-1$
	String VARIABLES_CONTENT_PROVIDERS_ACTION = PREFIX + "variables_content_providers_action_context"; //$NON-NLS-1$
	String VARIABLES_SELECT_LOGICAL_STRUCTURE = PREFIX + "variables_select_logical_structure"; //$NON-NLS-1$
	String VARIABLES_SELECT_DETAIL_PANE = PREFIX + "variables_select_detail_pane_action_context"; //$NON-NLS-1$
	String VARIABLES_AUTO_EXPAND = PREFIX + "variables_auto_expand_action_context"; //$NON-NLS-1$
	String SELECT_WORKING_SET_ACTION = PREFIX + "select_working_set_context"; //$NON-NLS-1$
	String CLEAR_WORKING_SET_ACTION = PREFIX + "clear_working_set_context"; //$NON-NLS-1$
	String EDIT_LAUNCH_CONFIGURATION_ACTION = PREFIX + "edit_launch_configuration_action_context"; //$NON-NLS-1$
	String OPEN_LAUNCH_CONFIGURATION_ACTION = PREFIX + "open_launch_configuration_action_context"; //$NON-NLS-1$
	String LINK_BREAKPOINTS_WITH_DEBUG_ACTION= PREFIX + "link_breakpoints_with_debug_context"; //$NON-NLS-1$
	String EDIT_SOURCELOOKUP_ACTION = PREFIX + "edit_source_lookup_path_context";//$NON-NLS-1$
	String LOOKUP_SOURCE_ACTION = PREFIX + "lookup_source_context";//$NON-NLS-1$
	String SKIP_ALL_BREAKPOINT_ACTION = PREFIX + "skip_all_breakpoints_context"; //$NON-NLS-1$
	String AUTO_MANAGE_VIEWS_ACTION = PREFIX + "auto_manage_views_context"; //$NON-NLS-1$
	String FIND_ELEMENT_ACTION = PREFIX + "find_element_context"; //$NON-NLS-1$
	String DETAIL_PANE = PREFIX + "detail_pane_context"; //$NON-NLS-1$
	String DETAIL_PANE_ASSIGN_VALUE_ACTION = PREFIX + "detail_pane_assign_value_action_context"; //$NON-NLS-1$
	String DETAIL_PANE_CONTENT_ASSIST_ACTION = PREFIX + "detail_pane_content_assist_action_context"; //$NON-NLS-1$
	String DETAIL_PANE_CUT_ACTION = PREFIX + "detail_pane_cut_action_context"; //$NON-NLS-1$
	String DETAIL_PANE_COPY_ACTION = PREFIX + "detail_pane_copy_action_context"; //$NON-NLS-1$
	String DETAIL_PANE_PASTE_ACTION = PREFIX + "detail_pane_paste_action_context"; //$NON-NLS-1$
	String DETAIL_PANE_SELECT_ALL_ACTION = PREFIX + "detail_pane_select_all_action_context"; //$NON-NLS-1$
	String DETAIL_PANE_FIND_REPLACE_ACTION = PREFIX + "detail_pane_find_replace_action_context"; //$NON-NLS-1$
	String DETAIL_PANE_WORD_WRAP_ACTION = PREFIX + "detail_pane_word_wrap_action_context"; //$NON-NLS-1$
	String DETAIL_PANE_MAX_LENGTH_ACTION = PREFIX + "detail_pane_max_length_action_context"; //$NON-NLS-1$
	String CONSOLE_TERMINATE_ACTION = PREFIX + "console_terminate_action_context"; //$NON-NLS-1$
	String CONSOLE_REMOVE_ALL_TERMINATED = PREFIX + "console_remove_all_terminated_context"; //$NON-NLS-1$
	String CONSOLE_REMOVE_LAUNCH = PREFIX + "console_remove_launch_context"; //$NON-NLS-1$;
	String CONSOLE_SHOW_PREFERENCES = PREFIX + "console_show_preferences_action_context"; //$NON-NLS-1$
	String SHOW_COLUMNS_ACTION = PREFIX + "show_columns_context"; //$NON-NLS-1$;
	String CONFIGURE_COLUMNS_ACTION = PREFIX + "configure_columns_context"; //$NON-NLS-1$;
	String MEMORY_VIEW_PANE_ORIENTATION_ACTION = PREFIX + "memory_view_pane_orientation_action_context"; //$NON-NLS-1$
	String SHOW_WHEN_STDOUT_CHANGES_ACTION = PREFIX + "show_stdout_action_context"; //$NON-NLS-1$
	String SHOW_WHEN_STDERR_CHANGES_ACTION = PREFIX + "show_stderr_action_context"; //$NON-NLS-1$
	String HORIZONTAL_DETAIL_PANE_LAYOUT_ACTION = PREFIX + "horizontal_detail_pane_layout_action_context"; //$NON-NLS-1$
	String VERTICAL_DETAIL_PANE_LAYOUT_ACTION = PREFIX + "vertical_detail_pane_layout_action_context"; //$NON-NLS-1$
	String DETAIL_PANE_HIDDEN_LAYOUT_ACTION = PREFIX + "detail_pane_hidden_layout_action_context"; //$NON-NLS-1$
	String DEBUG_VIEW_MODE_AUTO_ACTION = PREFIX + "debug_view_mode_auto_action_context"; //$NON-NLS-1$
	String DEBUG_VIEW_MODE_FULL_ACTION = PREFIX + "debug_view_mode_full_action_context"; //$NON-NLS-1$
	String DEBUG_VIEW_MODE_COMPACT_ACTION = PREFIX + "debug_view_mode_compact_action_context"; //$NON-NLS-1$
	String DEBUG_VIEW_DROP_DOWN_AUTOEXPAND_ACTION = PREFIX + "debug_view_drop_down_autoexpand_action_context"; //$NON-NLS-1$
	String DEBUG_TOOLBAR_VIEW_ACTION = PREFIX + "debug_toolbar_view_action_context"; //$NON-NLS-1$
	String DEBUG_TOOLBAR_WINDOW_ACTION = PREFIX + "debug_toolbar_window_action_context"; //$NON-NLS-1$
	String DEBUG_TOOLBAR_BOTH_ACTION = PREFIX + "debug_toolbar_both_action_context"; //$NON-NLS-1$

	// Views
	String DEBUG_VIEW = PREFIX + "debug_view_context"; //$NON-NLS-1$
	String VARIABLE_VIEW = PREFIX + "variable_view_context"; //$NON-NLS-1$
	String BREAKPOINT_VIEW = PREFIX + "breakpoint_view_context"; //$NON-NLS-1$
	String EXPRESSION_VIEW = PREFIX + "expression_view_context"; //$NON-NLS-1$
	String LAUNCH_CONFIGURATION_VIEW = PREFIX + "launch_configuration_view_context"; //$NON-NLS-1$
	String REGISTERS_VIEW = PREFIX + "registers_view_context"; //$NON-NLS-1$
	String PROCESS_CONSOLE = PREFIX + "process_console_context";  //$NON-NLS-1$
	String MODULES_VIEW = PREFIX + "modules_view_context"; //$NON-NLS-1$

	// Preference pages
	String DEBUG_PREFERENCE_PAGE = PREFIX + "debug_preference_page_context"; //$NON-NLS-1$
	String CONSOLE_PREFERENCE_PAGE = PREFIX + "console_preference_page_context"; //$NON-NLS-1$
	String SIMPLE_VARIABLE_PREFERENCE_PAGE = PREFIX + "simple_variable_preference_page_context"; //$NON-NLS-1$
	String PERSPECTIVE_PREFERENCE_PAGE = PREFIX + "perspective_preference_page_context"; //$NON-NLS-1$
	String LAUNCHING_PREFERENCE_PAGE = PREFIX + "launching_preference_page_context"; //$NON-NLS-1$
	String LAUNCH_CONFIGURATION_PREFERENCE_PAGE = PREFIX + "launch_configuration_preference_page_context"; //$NON-NLS-1$
	String VIEW_MANAGEMENT_PREFERENCE_PAGE = PREFIX + "view_management_preference_page_context"; //$NON-NLS-1$
	String LAUNCH_DELEGATES_PREFERENCE_PAGE = PREFIX + "launch_delegate_preference_page_context"; //$NON-NLS-1$

	// Dialogs
	String LAUNCH_CONFIGURATION_DIALOG = PREFIX + "launch_configuration_dialog"; //$NON-NLS-1$
	String LAUNCH_CONFIGURATION_PROPERTIES_DIALOG = PREFIX + "launch_configuration_properties_dialog"; //$NON-NLS-1$
	String SINGLE_LAUNCH_CONFIGURATION_DIALOG = PREFIX + "single_launch_configuration_dialog"; //$NON-NLS-1$
	String VARIABLE_SELECTION_DIALOG = PREFIX + "variable_selection_dialog_context"; //$NON-NLS-1$
	String EDIT_SOURCELOOKUP_DIALOG = PREFIX + "edit_source_lookup_path_dialog";//$NON-NLS-1$
	String SOURCELOOKUP_TAB = PREFIX + "launch_configuration_dialog_source_tab";//$NON-NLS-1$
	String ADD_SOURCE_CONTAINER_DIALOG = PREFIX + "add_source_container_dialog";//$NON-NLS-1$
	String ADD_PROJECT_CONTAINER_DIALOG = PREFIX + "project_source_container_dialog";//$NON-NLS-1$
	String ADD_FOLDER_CONTAINER_DIALOG = PREFIX + "folder_source_container_dialog";//$NON-NLS-1$
	String ADD_ARCHIVE_CONTAINER_DIALOG = PREFIX + "archive_source_container_dialog";//$NON-NLS-1$
	String MULTIPLE_SOURCE_DIALOG = PREFIX + "multiple_source_selection_dialog";//$NON-NLS-1$
	String ADD_WATCH_EXPRESSION_DIALOG= PREFIX + "add_watch_expression_dialog_context"; //$NON-NLS-1$
	String EDIT_WATCH_EXPRESSION_DIALOG = PREFIX + "edit_watch_expression_dialog_context"; //$NON-NLS-1$
	String FIND_ELEMENT_DIALOG = PREFIX + "find_element_dialog_context"; //$NON-NLS-1$
	String CONFIGURE_COLUMNS_DIALOG = PREFIX + "configure_columns_dialog_context"; //$NON-NLS-1$;
	String GROUP_BREAKPOINTS_DIALOG = PREFIX + "group_breakpoints_dialog_context"; //$NON-NLS-1$
	String ORGANIZE_FAVORITES_DIALOG = PREFIX + "organize_favorites_dialog_context"; //$NON-NLS-1$
	String SELECT_DEFAULT_WORKINGSET_DIALOG = PREFIX + "select_breakpoint_workingset_dialog"; //$NON-NLS-1$
	String DELETE_ASSOCIATED_LAUNCH_CONFIGS_DIALOG = PREFIX + "delete_associated_launch_configs_dialog"; //$NON-NLS-1$
	String SELECT_LAUNCH_MODES_DIALOG = PREFIX + "select_launch_modes_dialog"; //$NON-NLS-1$
	String SELECT_LAUNCHERS_DIALOG = PREFIX + "select_launchers_dialog"; //$NON-NLS-1$
	String SELECT_RESOURCES_TO_SAVE_DIALOG = PREFIX + "select_resources_to_save_dialog"; //$NON-NLS-1$
	String SELECT_LAUNCH_METHOD_DIALOG = PREFIX + "select_launch_method_dialog"; //$NON-NLS-1$
	String SELECT_LAUNCH_CONFIGURATION_DIALOG = PREFIX + "select_launch_configuration_dialog"; //$NON-NLS-1$
	String SELECT_DIRECTORY_SOURCE_CONTAINER_DIALOG = PREFIX + "select_directory_source_container_dialog"; //$NON-NLS-1$
	String SELECT_CONFIGURATION_TYPE_DIALOG = PREFIX + "select_configuration_type_dialog";  //$NON-NLS-1$
	String SELECT_FAVORITES_DIALOG = PREFIX + "select_favorites_dialog"; //$NON-NLS-1$
	String SELECT_NATIVE_ENVIRONMENT_DIALOG = PREFIX + "select_native_environment_dialog"; //$NON-NLS-1$
	String SELECT_LAUNCH_CONFIGURATION_MIGRATION_DIALOG = PREFIX + "select_launch_configuration_migration_dialog"; //$NON-NLS-1$

	// Property pages
	String PROCESS_PROPERTY_PAGE = PREFIX + "process_property_page_context"; //$NON-NLS-1$
	String PROCESS_PAGE_RUN_AT = PREFIX + "process_page_run_at_time_widget"; //$NON-NLS-1$
	String PROCESS_PAGE_TERMINATE_AT = PREFIX + "process_page_terminate_at_time_widget"; //$NON-NLS-1$
	String RUN_DEBUG_RESOURCE_PROPERTY_PAGE = PREFIX + "run_debug_resource_property_page"; //$NON-NLS-1$

	// Launch configuration dialog pages
	String LAUNCH_CONFIGURATION_DIALOG_COMMON_TAB = PREFIX + "launch_configuration_dialog_common_tab"; //$NON-NLS-1$
	String LAUNCH_CONFIGURATION_DIALOG_PERSPECTIVE_TAB = PREFIX + "launch_configuration_dialog_perspective_tab"; //$NON-NLS-1$
	String LAUNCH_CONFIGURATION_DIALOG_REFRESH_TAB = PREFIX + "launch_configuration_dialog_refresh_tab"; //$NON-NLS-1$
	String LAUNCH_CONFIGURATION_DIALOG_ENVIRONMENT_TAB = PREFIX +  "launch_configuration_dialog_environment_tab"; //$NON-NLS-1$
	String LAUNCH_CONFIGURATION_DIALOG_PROTOTYPE_TAB = PREFIX + "launch_configuration_dialog_prototype_tab"; //$NON-NLS-1$

	// Working set page
	String WORKING_SET_PAGE = PREFIX + "working_set_page_context"; //$NON-NLS-1$

	//Wizards
	String IMPORT_BREAKPOINTS_WIZARD_PAGE = PREFIX + "import_breakpoints_wizard_page_context"; //$NON-NLS-1$
	String EXPORT_BREAKPOINTS_WIZARD_PAGE = PREFIX + "export_breakpoints_wizard_page_context"; //$NON-NLS-1$
	String IMPORT_LAUNCH_CONFIGURATIONS_PAGE = PREFIX + "import_launch_configurations_context"; //$NON-NLS-1$
	String EXPORT_LAUNCH_CONFIGURATIONS_PAGE = PREFIX + "export_launch_configurations_context"; //$NON-NLS-1$

	//Editor
	String NO_SOURCE_EDITOR = PREFIX + "debugger_editor_no_source_common";//$NON-NLS-1$
}

