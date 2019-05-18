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
package org.eclipse.team.internal.ccvs.ui;

/**
 * Here's how to reference the help context in code:
 * 
 * WorkbenchHelp.setHelp(actionOrControl, IHelpContextIds.NAME_DEFIED_BELOW);
 */
public interface IHelpContextIds {

	public static final String PREFIX = CVSUIPlugin.ID + "."; //$NON-NLS-1$

	// Dialogs
	public static final String TAG_CONFIGURATION_OVERVIEW = PREFIX + "tag_configuration_overview"; //$NON-NLS-1$
	public static final String TAG_CONFIGURATION_REFRESHLIST = PREFIX + "tag_configuration_refreshlist"; //$NON-NLS-1$
	public static final String TAG_CONFIGURATION_REFRESHACTION = PREFIX + "tag_configuration_refreshaction"; //$NON-NLS-1$
	public static final String USER_VALIDATION_DIALOG = PREFIX + "user_validation_dialog_context"; //$NON-NLS-1$
	public static final String RELEASE_COMMENT_DIALOG = PREFIX + "release_comment_dialog_context"; //$NON-NLS-1$
	public static final String BRANCH_DIALOG = PREFIX + "branch_dialog_context"; //$NON-NLS-1$
	public static final String ADD_TO_VERSION_CONTROL_DIALOG = PREFIX + "add_to_version_control_dialog_context"; //$NON-NLS-1$
	public static final String EDITORS_DIALOG = PREFIX + "editors_dialog_context"; //$NON-NLS-1$
	public static final String HISTORY_FILTER_DIALOG = PREFIX + "history_filter_dialog_context"; //$NON-NLS-1$
	public static final String DATE_TAG_DIALOG = PREFIX + "date_tag_dialog_context"; //$NON-NLS-1$
	public static final String KEYBOARD_INTERACTIVE_DIALOG = PREFIX + "keyboard_interactive_dialog_context"; //$NON-NLS-1$
	public static final String COMMIT_SET_DIALOG = PREFIX + "commit_set_dialog_context"; //$NON-NLS-1$
	public static final String TAG_UNCOMMITED_PROMPT = PREFIX + "tag_uncommmited_dialog_context"; //$NON-NLS-1$
	public static final String ALTERNATIVE_REPOSITORY_DIALOG = PREFIX + "alternative_repository_dialog_context"; //$NON-NLS-1$
	public static final String REPOSITORY_FILTER_DIALOG = PREFIX + "repository_filter_dialog_context"; //$NON-NLS-1$
	
	// Different uses of the TagSelectionDialog
	public static final String REPLACE_TAG_SELECTION_DIALOG = PREFIX + "replace_tag_selection_dialog_context"; //$NON-NLS-1$
	public static final String COMPARE_TAG_SELECTION_DIALOG = PREFIX + "compare_tag_selection_dialog_context"; //$NON-NLS-1$
	public static final String TAG_REMOTE_WITH_EXISTING_DIALOG = PREFIX + "tag_remote_with_existing_dialog_context"; //$NON-NLS-1$

	// Different uses of the TagAsVersionDialog
	public static final String TAG_AS_VERSION_DIALOG = PREFIX + "tag_as_version_dialog_context"; //$NON-NLS-1$
	
	// Different uses of the OutgoingChangesDialog
	public static final String REPLACE_OUTGOING_CHANGES_DIALOG = PREFIX + "replace_outgoing_changes_dialog_context"; //$NON-NLS-1$
	public static final String TAG_OUTGOING_CHANGES_DIALOG = PREFIX + "tag_outgoing_changes_dialog_context"; //$NON-NLS-1$

	// Wizard Pages
	public static final String SHARING_AUTOCONNECT_PAGE = PREFIX + "sharing_autoconnect_page_context"; //$NON-NLS-1$
	public static final String SHARING_SELECT_REPOSITORY_PAGE = PREFIX + "sharing_select_repository_page_context"; //$NON-NLS-1$
	public static final String SHARING_NEW_REPOSITORY_PAGE = PREFIX + "sharing_new_repository_page_context"; //$NON-NLS-1$
	public static final String SHARING_MODULE_PAGE = PREFIX + "sharing_module_page_context"; //$NON-NLS-1$
	public static final String SHARING_TAG_SELETION_PAGE = PREFIX + "sharing_tag_selection_page_context"; //$NON-NLS-1$
	public static final String SHARING_SYNC_PAGE = PREFIX + "sharing_sync_page_context"; //$NON-NLS-1$

	public static final String PATCH_SELECTION_PAGE = PREFIX + "patch_selection_page_context"; //$NON-NLS-1$
	public static final String PATCH_OPTIONS_PAGE = PREFIX + "patch_options_page_context"; //$NON-NLS-1$
	
	public static final String KEYWORD_SUBSTITUTION_PAGE = PREFIX + "keyword_substitution_page_context"; //$NON-NLS-1$
	
	public static final String MERGE_WIZARD_PAGE = PREFIX + "merge_wizard_page_context"; //$NON-NLS-1$
	public static final String MERGE_START_PAGE = PREFIX + "merge_start_page_context"; //$NON-NLS-1$
	public static final String MERGE_END_PAGE = PREFIX + "merge_end_page_context"; //$NON-NLS-1$
	
	public static final String CHECKOUT_MODULE_SELECTION_PAGE = PREFIX + "checkout_module_selection_context"; //$NON-NLS-1$
	public static final String CHECKOUT_CONFIGURATION_PAGE = PREFIX + "checkout_configuration_context"; //$NON-NLS-1$
	public static final String CHECKOUT_LOCATION_SELECTION_PAGE = PREFIX + "checkout_location_selection_context"; //$NON-NLS-1$
	public static final String CHECKOUT_PROJECT_SELECTION_PAGE = PREFIX + "checkout_into_resource_selection_page_context"; //$NON-NLS-1$
	public static final String CHECKOUT_TAG_SELETION_PAGE = PREFIX + "checkout_tag_selection_page_context"; //$NON-NLS-1$
	
	public static final String UPDATE_TAG_SELETION_PAGE = PREFIX + "update_tag_selection_page_context"; //$NON-NLS-1$
	
	public static final String RESTORE_FROM_REPOSITORY_FILE_SELECTION_PAGE = PREFIX + "restore_from_repository_file_selection_page_context"; //$NON-NLS-1$
	public static final String REFRESH_REMOTE_PROJECT_SELECTION_PAGE = PREFIX + "refresh_remote_project_selection_page_context"; //$NON-NLS-1$

	public static final String COMMIT_FILE_TYPES_PAGE = PREFIX + "commit_file_types_page_context"; //$NON-NLS-1$
	public static final String COMMIT_COMMENT_PAGE = PREFIX + "commit_comment_page_context"; //$NON-NLS-1$
	
	public static final String CVS_SCM_URL_IMPORT_PAGE = PREFIX + "cvs_scm_url_import_page"; //$NON-NLS-1$

	// Preference Pages
	public static final String PREF_DEBUG_PROTOCOL = PREFIX + "debug_protocol_pref"; //$NON-NLS-1$
	public static final String PREF_PRUNE = PREFIX + "prune_empty_directories_pref"; //$NON-NLS-1$
	public static final String PREF_QUIET = PREFIX + "quietness_level_pref"; //$NON-NLS-1$
	public static final String PREF_COMPRESSION = PREFIX + "compression_level_pref"; //$NON-NLS-1$
	public static final String PREF_KEYWORDMODE = PREFIX + "default_keywordmode_pref"; //$NON-NLS-1$
	public static final String PREF_LINEEND = PREFIX + "line_end_pref"; //$NON-NLS-1$
	public static final String PREF_COMMS_TIMEOUT = PREFIX + "comms_timeout_pref"; //$NON-NLS-1$
	public static final String PREF_REPLACE_DELETE_UNMANAGED = PREFIX + "replace_deletion_of_unmanaged_pref"; //$NON-NLS-1$
	public static final String PREF_SAVE_DIRTY_EDITORS = PREFIX + "save_dirty_editors_pref"; //$NON-NLS-1$
	public static final String PREF_CHANGE_PERSPECTIVE_ON_SHOW_ANNOTATIONS = PREFIX + "change_perspective_on_show_annotations"; //$NON-NLS-1$
	public static final String PREF_ALLOW_EMPTY_COMMIT_COMMENTS = PREFIX + "allow_empty_commit_comment"; //$NON-NLS-1$
	public static final String PREF_DEFAULT_PERSPECTIVE_FOR_SHOW_ANNOTATIONS = PREFIX + "show_annotations_perspective"; //$NON-NLS-1$
	public static final String PREF_INCLUDE_CHANGE_SETS_IN_COMMIT = PREFIX + "include_change_sets"; //$NON-NLS-1$
	
	public static final String PREF_TREAT_NEW_FILE_AS_BINARY = PREFIX + "treat_new_files_as_binary_pref"; //$NON-NLS-1$
	public static final String PREF_DETERMINE_SERVER_VERSION = PREFIX + "determine_server_version"; //$NON-NLS-1$
	public static final String PREF_CONFIRM_MOVE_TAG = PREFIX + "confirm_move_tag"; //$NON-NLS-1$
	public static final String PREF_AUTOREFRESH_TAG = PREFIX + "auto_refresh_tag"; //$NON-NLS-1$

	public static final String GENERAL_PREFERENCE_PAGE = PREFIX + "general_preference_page_context"; //$NON-NLS-1$
	public static final String CONSOLE_PREFERENCE_PAGE = PREFIX + "console_preference_page_context"; //$NON-NLS-1$
	public static final String EXT_PREFERENCE_PAGE = PREFIX + "ext_preference_page_context"; //$NON-NLS-1$
	public static final String SSH2_PREFERENCE_PAGE = PREFIX + "ssh2_preference_page_context"; //$NON-NLS-1$
	public static final String EXT_PREFERENCE_RSH = PREFIX + "ext_preference_rsh_context"; //$NON-NLS-1$
	public static final String EXT_PREFERENCE_PARAM = PREFIX + "ext_preference_param_context"; //$NON-NLS-1$
	public static final String EXT_PREFERENCE_SERVER = PREFIX + "ext_preference_server_context"; //$NON-NLS-1$
	public static final String DECORATORS_PREFERENCE_PAGE = PREFIX + "decorators_preference_page_context"; //$NON-NLS-1$
	public static final String WATCH_EDIT_PREFERENCE_PAGE = PREFIX + "watch_edit_preference_page_context"; //$NON-NLS-1$
	public static final String PASSWORD_MANAGEMENT_PAGE = PREFIX + "password_management_preference_page_context"; //$NON-NLS-1$
	public static final String COMPARE_PREFERENCE_PAGE = PREFIX + "cvs_compare_preference_page_context"; //$NON-NLS-1$
	public static final String PROXY_PREFERENCE_PAGE = PREFIX + "proxy_preference_page_context"; //$NON-NLS-1$
	public static final String COMMENT_TEMPLATE_PREFERENCE_PAGE = PREFIX + "comment_template_preference_page_context"; //$NON-NLS-1$
	public static final String UPDATE_MERGE_PREFERENCE_PAGE = PREFIX + "update_merge_preference_page_context"; //$NON-NLS-1$

	// Views
	public static final String CONSOLE_VIEW = PREFIX + "console_view_context"; //$NON-NLS-1$
	public static final String REPOSITORIES_VIEW = PREFIX + "repositories_view_context"; //$NON-NLS-1$
	public static final String RESOURCE_HISTORY_VIEW = PREFIX + "resource_history_view_context"; //$NON-NLS-1$
	public static final String COMPARE_REVISIONS_VIEW = PREFIX + "compare_revision_view_context"; //$NON-NLS-1$

	public static final String CVS_EDITORS_VIEW = PREFIX + "cvs_editors_view_context"; //$NON-NLS-1$
	public static final String ANNOTATE_VIEW = PREFIX + "annotate_view_context"; //$NON-NLS-1$

	// Viewers
	public static final String CATCHUP_RELEASE_VIEWER = PREFIX + "catchup_release_viewer_context"; //$NON-NLS-1$

	// Add to .cvsignore dialog
	public static final String ADD_TO_CVSIGNORE = PREFIX + "add_to_cvsignore_context"; //$NON-NLS-1$

	// Actions
	public static final String GET_ANNOTATE_ACTION = PREFIX + "get_annotate_action_context"; //$NON-NLS-1$
	public static final String GET_FILE_REVISION_ACTION = PREFIX + "get_file_revision_action_context"; //$NON-NLS-1$
	public static final String GET_FILE_CONTENTS_ACTION = PREFIX + "get_file_contents_action_context"; //$NON-NLS-1$
	public static final String TAG_WITH_EXISTING_ACTION = PREFIX + "tag_with_existing_action_context"; //$NON-NLS-1$
	public static final String NEW_REPOSITORY_LOCATION_ACTION = PREFIX + "new_repository_location_action_context"; //$NON-NLS-1$
	public static final String NEW_DEV_ECLIPSE_REPOSITORY_LOCATION_ACTION = PREFIX + "new_dev_eclipse repository_location_action_context"; //$NON-NLS-1$
	public static final String SHOW_COMMENT_IN_HISTORY_ACTION = PREFIX + "show_comment_in_history_action_context"; //$NON-NLS-1$
	public static final String SHOW_TAGS_IN_HISTORY_ACTION = PREFIX + "show_tag_in_history_action_context"; //$NON-NLS-1$
	public static final String SELECT_WORKING_SET_ACTION = PREFIX + "select_working_set_action_context"; //$NON-NLS-1$
	public static final String DESELECT_WORKING_SET_ACTION = PREFIX + "deselect_working_set_action_context"; //$NON-NLS-1$
	public static final String EDIT_WORKING_SET_ACTION = PREFIX + "edit_working_set_action_context"; //$NON-NLS-1$
	public static final String REMOVE_REPOSITORY_LOCATION_ACTION = PREFIX + "remove_root_action_context"; //$NON-NLS-1$
	public static final String SHOW_IN_RESOURCE_HISTORY = PREFIX + "show_in_history_action_context"; //$NON-NLS-1$
	public static final String SELECT_NEW_RESOURCES_ACTION = PREFIX + "select_new_action_context"; //$NON-NLS-1$
	public static final String CONFIRM_MERGE_ACTION = PREFIX + "confirm_merge_action_context"; //$NON-NLS-1$;
	public static final String DISCONNECT_ACTION = PREFIX + "disconnect_action_context"; //$NON-NLS-1$;
	
	// Sync view menu actions
	public static final String SYNC_COMMIT_ACTION = PREFIX + "sync_commit_action_context"; //$NON-NLS-1$
	public static final String SYNC_FORCED_COMMIT_ACTION = PREFIX + "sync_forced_commit_action_context"; //$NON-NLS-1$
	public static final String SYNC_UPDATE_ACTION = PREFIX + "sync_update_action_context"; //$NON-NLS-1$
	public static final String SYNC_FORCED_UPDATE_ACTION = PREFIX + "sync_forced_update_action_context"; //$NON-NLS-1$
	public static final String SYNC_ADD_ACTION = PREFIX + "sync_add_action_context"; //$NON-NLS-1$
	public static final String SYNC_IGNORE_ACTION = PREFIX + "sync_ignore_action_context"; //$NON-NLS-1$
	public static final String MERGE_UPDATE_ACTION = PREFIX + "merge_update_action_context"; //$NON-NLS-1$
	public static final String MERGE_FORCED_UPDATE_ACTION = PREFIX + "merge_forced_update_action_context"; //$NON-NLS-1$
	public static final String MERGE_UPDATE_WITH_JOIN_ACTION = PREFIX + "merge_update_with_joinaction_context"; //$NON-NLS-1$
	
	// properties pages
	public static final String REPOSITORY_LOCATION_PROPERTY_PAGE = PREFIX + "repository_location_property_page_context"; //$NON-NLS-1$
	public static final String REPOSITORY_ENCODING_PROPERTY_PAGE = PREFIX + "repository_encoding_property_page_context"; //$NON-NLS-1$
	public static final String PROJECT_PROPERTY_PAGE = PREFIX + "project_property_page_context"; //$NON-NLS-1$
	public static final String FOLDER_PROPERTY_PAGE = PREFIX + "folder_property_page_context"; //$NON-NLS-1$
	public static final String FILE_PROPERTY_PAGE = PREFIX + "file_property_page_context"; //$NON-NLS-1$

}
