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
 *     Philippe Ombredanne - bug 84808
 *     William Mitsuda (wmitsuda@gmail.com) - Bug 153879 [Wizards] configurable size of cvs commit comment history
 *     Brock Janiczak <brockj@tpg.com.au> - Bug 161536 Warn user when committing resources with problem markers
 *******************************************************************************/

package org.eclipse.team.internal.ccvs.ui;

public interface ICVSUIConstants {
	public final String PREFIX = CVSUIPlugin.ID + "."; //$NON-NLS-1$

	// image path
	public final String ICON_PATH = "$nl$/icons/full/"; //$NON-NLS-1$
	
	// images
	public final String IMG_CVS_CONSOLE = "eview16/console_view.gif"; //$NON-NLS-1$
	public final String IMG_CVS_PERSPECTIVE = "eview16/cvs_persp.gif"; //$NON-NLS-1$
	public final String IMG_COMPARE_VIEW = "eview16/compare_view.gif"; //$NON-NLS-1$
	
	// overlays
	public final String IMG_MERGEABLE_CONFLICT = "ovr16/confauto_ov.gif"; //$NON-NLS-1$
	public final String IMG_QUESTIONABLE = "ovr16/question_ov.gif"; //$NON-NLS-1$
	public final String IMG_MERGED = "ovr16/merged_ov.gif"; //$NON-NLS-1$
	public final String IMG_EDITED = "ovr16/edited_ov.gif"; //$NON-NLS-1$
	public final String IMG_NO_REMOTEDIR = "ovr16/no_remotedir_ov.gif"; //$NON-NLS-1$
	
	// objects
	public final String IMG_REPOSITORY = "obj16/repository_rep.gif"; //$NON-NLS-1$
	public final String IMG_TAG = "obj16/tag.gif"; //$NON-NLS-1$
	public final String IMG_BRANCHES_CATEGORY = "obj16/branches_rep.gif"; //$NON-NLS-1$
	public final String IMG_VERSIONS_CATEGORY = "obj16/versions_rep.gif"; //$NON-NLS-1$
	public final String IMG_DATES_CATEGORY = "obj16/dates.gif"; //$NON-NLS-1$
	
	public final String IMG_MODULE = "obj16/module_rep.gif"; //$NON-NLS-1$
	public final String IMG_PROJECT_VERSION = "obj16/prjversions_rep.gif"; //$NON-NLS-1$
	public final String IMG_DATE = "obj16/date.gif"; //$NON-NLS-1$
	public final String IMG_CHANGELOG = "obj16/changelog_obj.gif"; //$NON-NLS-1$
	
	public final String IMG_LOCALREVISION_TABLE = "obj16/local_entry_tbl.gif"; //$NON-NLS-1$
	public final String IMG_REMOTEREVISION_TABLE = "obj16/remote_entry_tbl.gif"; //$NON-NLS-1$
	
	// toolbar
	public final String IMG_REFRESH = "elcl16/refresh.gif"; //$NON-NLS-1$
	public final String IMG_CLEAR = "elcl16/clear_co.gif"; //$NON-NLS-1$
	public final String IMG_COLLAPSE_ALL = "elcl16/collapseall.gif"; //$NON-NLS-1$
	public final String IMG_LINK_WITH_EDITOR = "elcl16/synced.gif"; //$NON-NLS-1$
	public final String IMG_REMOVE_CONSOLE = "elcl16/console_rem.gif"; //$NON-NLS-1$
	public final String IMG_REMOTEMODE = "elcl16/remote_history_mode.gif"; //$NON-NLS-1$
	public final String IMG_LOCALMODE = "elcl16/local_history_mode.gif";  //$NON-NLS-1$
	public final String IMG_LOCALREMOTE_MODE = "elcl16/all_history_mode.gif";  //$NON-NLS-1$
	
	// toolbar (disabled)
	public final String IMG_REFRESH_DISABLED = "dlcl16/refresh.gif"; //$NON-NLS-1$
	public final String IMG_CLEAR_DISABLED = "dlcl16/clear_co.gif"; //$NON-NLS-1$
	public final String IMG_REMOVE_CONSOLE_DISABLED = "dlcl16/console_rem.gif"; //$NON-NLS-1$
	public final String IMG_REMOTEMODE_DISABLED = "dlcl16/remote_history_mode.gif"; //$NON-NLS-1$
	public final String IMG_LOCALMODE_DISABLED = "dlcl16/local_history_mode.gif";  //$NON-NLS-1$
	public final String IMG_LOCALREMOTE_MODE_DISABLED = "dlcl16/all_history_mode.gif";  //$NON-NLS-1$
		
	// toolbar (enabled)
	public final String IMG_REFRESH_ENABLED = "elcl16/refresh.gif"; //$NON-NLS-1$
	public final String IMG_CLEAR_ENABLED = "elcl16/clear_co.gif"; //$NON-NLS-1$
	public final String IMG_COLLAPSE_ALL_ENABLED = "elcl16/collapseall.gif"; //$NON-NLS-1$
	public final String IMG_LINK_WITH_EDITOR_ENABLED = "elcl16/synced.gif"; //$NON-NLS-1$
	
	//history page toolbar
	public final String IMG_FILTER_HISTORY = "elcl16/filter_history.png"; //$NON-NLS-1$
	
	// wizards
	public final String IMG_NEWLOCATION = "etool16/newlocation_wiz.gif"; //$NON-NLS-1$
	public final String IMG_CVSLOGO = "etool16/newconnect_wiz.gif"; //$NON-NLS-1$
	
	// preferences
	public final String PREF_REVISION_MODE = "pref_revision_mode"; //$NON-NLS-1$
	public final String PREF_GROUPBYDATE_MODE = "pref_groupbydate_mode"; //$NON-NLS-1$
	public final String PREF_SHOW_COMMENTS = "pref_show_comments"; //$NON-NLS-1$
	public final String PREF_WRAP_COMMENTS = "pref_wrap_comments"; //$NON-NLS-1$
	public final String PREF_SHOW_TAGS = "pref_show_tags"; //$NON-NLS-1$
	public final String PREF_SHOW_SEARCH = "pref_show_search"; //$NON-NLS-1$
	public final String PREF_HISTORY_VIEW_EDITOR_LINKING = "pref_history_view_linking"; //$NON-NLS-1$
	public final String PREF_PRUNE_EMPTY_DIRECTORIES = "pref_prune_empty_directories";	 //$NON-NLS-1$
	public final String PREF_TIMEOUT = "pref_timeout";	 //$NON-NLS-1$
	public final String PREF_QUIETNESS = "pref_quietness"; //$NON-NLS-1$
	public final String PREF_CVS_RSH = "pref_cvs_rsh"; //$NON-NLS-1$
	public final String PREF_CVS_RSH_PARAMETERS = "pref_cvs_rsh_parameters"; //$NON-NLS-1$
	public final String PREF_CVS_SERVER = "pref_cvs_server"; //$NON-NLS-1$
	public final String PREF_CONSIDER_CONTENTS = "pref_consider_contents"; //$NON-NLS-1$
	/**
	 * Preference to save the pattern for the regex filter used in the Synchronize View.
	 */
	public final String PREF_SYNCVIEW_REGEX_FILTER_PATTERN = "pref_syncview_regex_filter_pattern"; //$NON-NLS-1$
	public final String PREF_REPLACE_UNMANAGED = "pref_replace_unmanaged"; //$NON-NLS-1$
	public final String PREF_COMPRESSION_LEVEL = "pref_compression_level"; //$NON-NLS-1$
	public final String PREF_TEXT_KSUBST = "pref_text_ksubst"; //$NON-NLS-1$
	public final String PREF_USE_PLATFORM_LINEEND = "pref_lineend"; //$NON-NLS-1$
	public final String PREF_PROMPT_ON_MIXED_TAGS = "pref_prompt_on_mixed_tags"; //$NON-NLS-1$
	public final String PREF_PROMPT_ON_SAVING_IN_SYNC = "pref_prompt_on_saving_in_sync"; //$NON-NLS-1$
	public final String PREF_SAVE_DIRTY_EDITORS = "pref_save_dirty_editors"; //$NON-NLS-1$
	public final String PREF_PROMPT_ON_CHANGE_GRANULARITY = "pref_prompt_on_change_granularity"; //$NON-NLS-1$
	public final String PREF_REPOSITORIES_ARE_BINARY = "pref_repositories_are_binary"; //$NON-NLS-1$
	public final String PREF_DETERMINE_SERVER_VERSION = "pref_determine_server_version"; //$NON-NLS-1$
	public final String PREF_CONFIRM_MOVE_TAG = "pref_confirm_move_tag"; //$NON-NLS-1$
	public final String PREF_DEBUG_PROTOCOL = "pref_debug_protocol"; //$NON-NLS-1$
	public final String PREF_WARN_REMEMBERING_MERGES = "pref_remember_merges"; //$NON-NLS-1$
	public final String PREF_FIRST_STARTUP = "pref_first_startup"; //$NON-NLS-1$
	public final String PREF_EXT_CONNECTION_METHOD_PROXY = "pref_ext_connection_method_proxy"; //$NON-NLS-1$
	public final String PREF_SHOW_COMPARE_REVISION_IN_DIALOG = "pref_show_compare_revision_in_dialog"; //$NON-NLS-1$
	public final String PREF_SHOW_AUTHOR_IN_EDITOR = "pref_show_author_in_editor"; //$NON-NLS-1$
	public final String PREF_COMMIT_SET_DEFAULT_ENABLEMENT = "pref_enable_commit_sets"; //$NON-NLS-1$
	public final String PREF_AUTO_REFRESH_TAGS_IN_TAG_SELECTION_DIALOG = "pref_auto_refresh_tags_in_tag_selection_dialog"; //$NON-NLS-1$
	public final String PREF_COMMIT_FILES_DISPLAY_THRESHOLD = "pref_commit_files_display_threshold"; //$NON-NLS-1$
	public final String PREF_COMMIT_COMMENTS_MAX_HISTORY = "pref_commit_comments_max_history"; //$NON-NLS-1$
	public final String PREF_AUTO_SHARE_ON_IMPORT = "pref_auto_share_on_import"; //$NON-NLS-1$
	public final String PREF_ENABLE_WATCH_ON_EDIT = "pref_enable_watch_on_edit"; //$NON-NLS-1$ 
	public final String PREF_USE_PROJECT_NAME_ON_CHECKOUT = "pref_use_project_name_on_checkout"; //$NON-NLS-1$
	public final String PREF_INCLUDE_CHANGE_SETS_IN_COMMIT = "pref_include_change_sets"; //$NON-NLS-1$
	public final String PREF_ANNOTATE_PROMPTFORBINARY = "pref_annotate_promptforbinary"; //$NON-NLS-1$
	public final String PREF_ALLOW_COMMIT_WITH_WARNINGS = "pref_commit_with_warning"; //$NON-NLS-1$
	public final String PREF_ALLOW_COMMIT_WITH_ERRORS = "pref_commit_with_errors"; //$NON-NLS-1$
	
	
	// console preferences
	public final String PREF_CONSOLE_COMMAND_COLOR = "pref_console_command_color"; //$NON-NLS-1$
	public final String PREF_CONSOLE_MESSAGE_COLOR = "pref_console_message_color"; //$NON-NLS-1$
	public final String PREF_CONSOLE_ERROR_COLOR = "pref_console_error_color"; //$NON-NLS-1$
	public final String PREF_CONSOLE_FONT = "pref_console_font"; //$NON-NLS-1$
	public final String PREF_CONSOLE_SHOW_ON_MESSAGE = "pref_console_show_on_message"; //$NON-NLS-1$	
	public final String PREF_CONSOLE_LIMIT_OUTPUT = "pref_console_limit_output"; //$NON-NLS-1$
	public final String PREF_CONSOLE_HIGH_WATER_MARK = "pref_console_high_water_mark"; //$NON-NLS-1$
	public final String PREF_CONSOLE_WRAP = "pref_console_wrap"; //$NON-NLS-1$
	public final String PREF_CONSOLE_WIDTH = "pref_console_width"; //$NON-NLS-1$
		
	// decorator preferences
	public final String PREF_FILETEXT_DECORATION = "pref_filetext_decoration"; //$NON-NLS-1$
	public final String PREF_FOLDERTEXT_DECORATION = "pref_foldertext_decoration"; //$NON-NLS-1$
	public final String PREF_PROJECTTEXT_DECORATION = "pref_projecttext_decoration"; //$NON-NLS-1$
	
	public final String PREF_SHOW_DIRTY_DECORATION = "pref_show_overlaydirty"; //$NON-NLS-1$
	public final String PREF_SHOW_ADDED_DECORATION = "pref_show_added"; //$NON-NLS-1$
	public final String PREF_SHOW_HASREMOTE_DECORATION = "pref_show_hasremote"; //$NON-NLS-1$
	public final String PREF_SHOW_NEWRESOURCE_DECORATION = "pref_show_newresource"; //$NON-NLS-1$
	
	public final String PREF_DIRTY_FLAG = "pref_dirty_flag"; //$NON-NLS-1$
	public final String PREF_ADDED_FLAG = "pref_added_flag"; //$NON-NLS-1$
	
	public final String PREF_CALCULATE_DIRTY = "pref_calculate_dirty";	 //$NON-NLS-1$
	public final String PREF_USE_FONT_DECORATORS= "pref_use_font_decorators";	//$NON-NLS-1$

	// watch/edit preferences
	public final String PREF_CHECKOUT_READ_ONLY = "pref_checkout_read_only"; //$NON-NLS-1$
	public final String PREF_EDIT_ACTION = "pref_edit_action"; //$NON-NLS-1$
	public final String PREF_EDIT_PROMPT_EDIT = "edit"; //$NON-NLS-1$
	public final String PREF_EDIT_PROMPT_HIGHJACK = "highjack"; //$NON-NLS-1$
	public final String PREF_EDIT_IN_BACKGROUND = "editInBackground"; //$NON-NLS-1$
	public final String PREF_EDIT_PROMPT = "pref_edit_prompt"; //$NON-NLS-1$
	public final String PREF_EDIT_PROMPT_NEVER = "never"; //$NON-NLS-1$
	public final String PREF_EDIT_PROMPT_ALWAYS = "always";	 //$NON-NLS-1$
	public final String PREF_EDIT_PROMPT_IF_EDITORS = "only";	 //$NON-NLS-1$
	
	// update preferences
	public final String PREF_UPDATE_PROMPT = "pref_update_prompt"; //$NON-NLS-1$
	public final String PREF_UPDATE_PROMPT_NEVER = "never";    //$NON-NLS-1$
	public final String PREF_UPDATE_PROMPT_AUTO = "auto"; //$NON-NLS-1$
	public final String PREF_UPDATE_PROMPT_IF_OUTDATED = "only";  //$NON-NLS-1$
	
	// Repositories view preferences
	public final String PREF_GROUP_VERSIONS_BY_PROJECT = "pref_group_versions_by_project"; //$NON-NLS-1$
	
	// Perspective changing preferences
	public final String PREF_CHANGE_PERSPECTIVE_ON_NEW_REPOSITORY_LOCATION = "pref_change_perspective_on_new_location"; //$NON-NLS-1$
	
	public final String PREF_ALLOW_EMPTY_COMMIT_COMMENTS= "pref_allow_empty_commit_comment"; //$NON-NLS-1$
	
	public final String PREF_UPDATE_HANDLING = "pref_team_update_handling"; //$NON-NLS-1$
	public final String PREF_UPDATE_HANDLING_PREVIEW = "previewUpdate"; //$NON-NLS-1$
	public final String PREF_UPDATE_HANDLING_PERFORM = "performUpdate"; //$NON-NLS-1$
	public final String PREF_UPDATE_HANDLING_TRADITIONAL = "traditionalUpdate"; //$NON-NLS-1$
	public final String PREF_UPDATE_PREVIEW = "pref_update_preview"; //$NON-NLS-1$
	public final String PREF_UPDATE_PREVIEW_IN_DIALOG = "dialog"; //$NON-NLS-1$
	public final String PREF_UPDATE_PREVIEW_IN_SYNCVIEW = "syncView"; //$NON-NLS-1$
	
	public final String PREF_ENABLE_MODEL_SYNC = "enableModelSync"; //$NON-NLS-1$
	public final String PREF_OPEN_COMPARE_EDITOR_FOR_SINGLE_FILE = "openCompareEditorForSingleFile"; //$NON-NLS-1$
		
	// Wizard banners
	public final String IMG_WIZBAN_SHARE = "wizban/newconnect_wizban.png";	 //$NON-NLS-1$
	public final String IMG_WIZBAN_MERGE = "wizban/mergestream_wizban.png";	 //$NON-NLS-1$
	public final String IMG_WIZBAN_DIFF = "wizban/createpatch_wizban.png";   //$NON-NLS-1$
	public final String IMG_WIZBAN_KEYWORD = "wizban/keywordsub_wizban.png"; //$NON-NLS-1$
	public final String IMG_WIZBAN_NEW_LOCATION = "wizban/newlocation_wizban.png"; //$NON-NLS-1$
	public final String IMG_WIZBAN_CHECKOUT = "wizban/newconnect_wizban.png";	 //$NON-NLS-1$
	public final String IMG_WIZBAN_IMPORT = "wizban/import_wiz.png";	 //$NON-NLS-1$
	
	// Properties
	public final String PROP_NAME = "cvs.name"; //$NON-NLS-1$
	public final String PROP_REVISION = "cvs.revision"; //$NON-NLS-1$
	public final String PROP_AUTHOR = "cvs.author"; //$NON-NLS-1$
	public final String PROP_COMMENT = "cvs.comment"; //$NON-NLS-1$
	public final String PROP_DATE = "cvs.date"; //$NON-NLS-1$
	public final String PROP_DIRTY = "cvs.dirty"; //$NON-NLS-1$
	public final String PROP_MODIFIED = "cvs.modified"; //$NON-NLS-1$
	public final String PROP_KEYWORD = "cvs.date"; //$NON-NLS-1$
	public final String PROP_TAG = "cvs.tag"; //$NON-NLS-1$
	public final String PROP_PERMISSIONS = "cvs.permissions"; //$NON-NLS-1$
	public final String PROP_HOST = "cvs.host"; //$NON-NLS-1$
	public final String PROP_USER = "cvs.user"; //$NON-NLS-1$
	public final String PROP_METHOD = "cvs.method"; //$NON-NLS-1$
	public final String PROP_PORT = "cvs.port"; //$NON-NLS-1$
	public final String PROP_ROOT = "cvs.root"; //$NON-NLS-1$
	
	// preference options
	public final int OPTION_NEVER = 1;
	public final int OPTION_PROMPT = 2;
	public final int OPTION_AUTOMATIC = 3;	
	
	public final String OPTION_NO_PERSPECTIVE= "none"; //$NON-NLS-1$
	
	// Command Ids
	public final String CMD_COMMIT = "org.eclipse.team.cvs.ui.commit"; //$NON-NLS-1$
	public final String CMD_COMMIT_ALL = "org.eclipse.team.cvs.ui.commitAll"; //$NON-NLS-1$
	public final String CMD_SYNCHRONIZE = "org.eclipse.team.ui.synchronizeLast"; //$NON-NLS-1$
	public final String CMD_UPDATE = "org.eclipse.team.cvs.ui.update"; //$NON-NLS-1$
	public final String CMD_UPDATE_ALL = "org.eclipse.team.cvs.ui.updateAll"; //$NON-NLS-1$
	public final String CMD_CREATEPATCH = "org.eclipse.team.cvs.ui.GenerateDiff"; //$NON-NLS-1$
	public final String CMD_TAGASVERSION = "org.eclipse.team.cvs.ui.tag"; //$NON-NLS-1$
	public final String CMD_BRANCH = "org.eclipse.team.cvs.ui.branch"; //$NON-NLS-1$
	public final String CMD_MERGE = "org.eclipse.team.cvs.ui.merge"; //$NON-NLS-1$
	public final String CMD_UPDATESWITCH = "org.eclipse.team.cvs.ui.updateSwitch"; //$NON-NLS-1$
	public final String CMD_SETFILETYPE = "org.eclipse.team.cvs.ui.setKeywordSubstitution"; //$NON-NLS-1$
	public final String CMD_ANNOTATE = "org.eclipse.team.cvs.ui.showAnnotation"; //$NON-NLS-1$
	public final String CMD_HISTORY = "org.eclipse.team.cvs.ui.showHistory"; //$NON-NLS-1$
	public final String CMD_ADD = "org.eclipse.team.cvs.ui.add"; //$NON-NLS-1$
	public final String CMD_IGNORE = "org.eclipse.team.cvs.ui.ignore"; //$NON-NLS-1$

}

