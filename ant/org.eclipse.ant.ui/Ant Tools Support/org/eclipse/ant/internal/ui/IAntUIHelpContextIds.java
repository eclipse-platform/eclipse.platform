/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.ant.internal.ui;

/**
 * Help context ids for the Ant UI
 * <p>
 * This interface contains constants only; it is not intended to be implemented or extended.
 * </p>
 */
public interface IAntUIHelpContextIds {
	public static final String PREFIX = "org.eclipse.ant.ui."; //$NON-NLS-1$

	// Actions
	public static final String REMOVE_ALL_ACTION = PREFIX + "remove_all_action_context"; //$NON-NLS-1$
	public static final String TARGET_MOVE_UP_ACTION = PREFIX + "target_move_up_action_context"; //$NON-NLS-1$
	public static final String TARGET_MOVE_DOWN_ACTION = PREFIX + "target_move_down_action_context"; //$NON-NLS-1$
	public static final String SEARCH_FOR_BUILDFILES_ACTION = PREFIX + "search_for_buildfiles_action_context"; //$NON-NLS-1$

	public static final String EDIT_LAUNCH_CONFIGURATION_ACTION = PREFIX + "edit_launch_configuration_action_context"; //$NON-NLS-1$
	public static final String RUN_TARGET_ACTION = PREFIX + "run_target_action_context"; //$NON-NLS-1$
	public static final String REMOVE_PROJECT_ACTION = PREFIX + "remove_project_action_context"; //$NON-NLS-1$
	public static final String ADD_BUILDFILE_ACTION = PREFIX + "add_buildfile_action_context"; //$NON-NLS-1$
	public static final String REFRESH_BUILDFILE_ACTION = PREFIX + "refresh_buildfile_action_context"; //$NON-NLS-1$

	// Dialogs
	public static final String ADD_TYPE_DIALOG = PREFIX + "add_type_dialog_context"; //$NON-NLS-1$
	public static final String EDIT_TYPE_DIALOG = PREFIX + "edit_type_dialog_context"; //$NON-NLS-1$
	public static final String ADD_TASK_DIALOG = PREFIX + "add_task_dialog_context"; //$NON-NLS-1$
	public static final String EDIT_TASK_DIALOG = PREFIX + "edit_task_dialog_context"; //$NON-NLS-1$
	public static final String ADD_PROPERTY_DIALOG = PREFIX + "add_property_dialog_context"; //$NON-NLS-1$
	public static final String EDIT_PROPERTY_DIALOG = PREFIX + "edit_property_dialog_context"; //$NON-NLS-1$
	public static final String SEARCH_FOR_BUILDFILES_DIALOG = PREFIX + "search_for_buildfiles_dialog_context"; //$NON-NLS-1$
	public static final String STATUS_DIALOG = PREFIX + "status_dialog_context"; //$NON-NLS-1$
	public static final String MESSAGE_WITH_TOGGLE_DIALOG = PREFIX + "message_with_toggle_dialog_context"; //$NON-NLS-1$
	public static final String TARGET_ORDER_DIALOG = PREFIX + "target_order_dialog_context"; //$NON-NLS-1$

	// Preference Pages
	public static final String ANT_PREFERENCE_PAGE = PREFIX + "ant_preference_page_context"; //$NON-NLS-1$
	public static final String ANT_RUNTIME_PREFERENCE_PAGE = PREFIX + "ant_runtime_preference_page_context"; //$NON-NLS-1$
	public static final String ANT_EDITOR_PREFERENCE_PAGE = PREFIX + "ant_editor_preference_page_context"; //$NON-NLS-1$
	public static final String ANT_EDITOR_CONTENTASSIST_PREFERENCE_PAGE = PREFIX + "ant_editor_contentassist_preference_page_context"; //$NON-NLS-1$
	public static final String ANT_EDITOR_FORMATTER_PREFERENCE_PAGE = PREFIX + "ant_editor_formatter_preference_page_context"; //$NON-NLS-1$
	public static final String ANT_EDITOR_TEMPLATE_PREFERENCE_PAGE = PREFIX + "ant_editor_templates_preference_page_context"; //$NON-NLS-1$
	public static final String ANT_TYPES_PAGE = PREFIX + "ant_types_page_context"; //$NON-NLS-1$
	public static final String ANT_CLASSPATH_PAGE = PREFIX + "ant_classpath_page_context"; //$NON-NLS-1$
	public static final String ANT_JRE_PAGE = PREFIX + "ant_jre_page_context"; //$NON-NLS-1$

	public static final String ANT_PROPERTIES_PAGE = PREFIX + "ant_properties_page_context"; //$NON-NLS-1$
	public static final String ANT_TASKS_PAGE = PREFIX + "ant_tasks_page_context"; //$NON-NLS-1$

	// Views
	public static final String ANT_VIEW = PREFIX + "ant_view_context"; //$NON-NLS-1$

	// Editor
	public static final String ANT_EDITOR = PREFIX + "ant_editor_context"; //$NON-NLS-1$

	// Launch configuration dialog tabs
	public static final String ANT_MAIN_TAB = PREFIX + "ant_main_tab_context"; //$NON-NLS-1$
	public static final String ANT_REFRESH_TAB = PREFIX + "ant_refresh_tab_context"; //$NON-NLS-1$
	public static final String ANT_ENVIRONMENT_TAB = PREFIX + "ant_environment_tab_context"; //$NON-NLS-1$
	public static final String ANT_COMMON_TAB = PREFIX + "ant_common_tab_context"; //$NON-NLS-1$
	public static final String ANT_TARGETS_TAB = PREFIX + "ant_targets_tab_context"; //$NON-NLS-1$
	public static final String ANT_PROPERTIES_TAB = PREFIX + "ant_properties_tab_context"; //$NON-NLS-1$
	public static final String ANT_CLASSPATH_TAB = PREFIX + "ant_classpath_tab_context"; //$NON-NLS-1$
	public static final String ANT_JRE_TAB = PREFIX + "ant_jre_tab_context"; //$NON-NLS-1$
	public static final String ANT_BUILD_TAB = PREFIX + "ant_build_tab_context"; //$NON-NLS-1$
	public static final String ANT_BUILDER_TAB = PREFIX + "ant_builder_tab_context"; //$NON-NLS-1$
	public static final String ANT_BUILD_OPTIONS_TAB = PREFIX + "ant_build_options_context"; //$NON-NLS-1$
}
