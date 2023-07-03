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
 *     George Suaridze <suag@1c.ru> (1C-Soft LLC) - Bug 559885
 *******************************************************************************/
package org.eclipse.ui.internal.cheatsheets;

import org.eclipse.osgi.util.NLS;

public final class Messages extends NLS {

	private static final String BUNDLE_NAME = "org.eclipse.ui.internal.cheatsheets.Messages";//$NON-NLS-1$

	private Messages() {
		// Do not instantiate
	}

	public static String ERROR_READING_STATE_FILE;
	public static String ERROR_WRITING_STATE_FILE;
	public static String CHEAT_SHEET_SELECTION_DIALOG_TITLE;
	public static String CHEAT_SHEET_SELECTION_DIALOG_MSG;
	public static String CHEAT_SHEET_UNSUPPORTED_LINK_ACTIVATION_MESSAGE;
	public static String COLLAPSE_ALL_BUT_CURRENT_TOOLTIP;
	public static String CATEGORY_OTHER;
	public static String RESTORE_ALL_TOOLTIP;
	public static String CHEAT_SHEET_OTHER_MENU;
	public static String PERFORM_TASK_TOOLTIP;
	public static String SKIP_TASK_TOOLTIP;
	public static String COMPLETE_TASK_TOOLTIP;
	public static String RESTART_TASK_TOOLTIP;
	public static String ERROR_FINDING_PLUGIN_FOR_ACTION;
	public static String ERROR_DATA_MISSING;
	public static String ERROR_DATA_MISSING_LOG;
	public static String ERROR_CONDITIONAL_DATA_MISSING_LOG;
	public static String ERROR_LOADING_CLASS_FOR_ACTION;
	public static String ERROR_CREATING_CLASS_FOR_ACTION;
	public static String START_CHEATSHEET_TOOLTIP;
	public static String RESTART_CHEATSHEET_TOOLTIP;
	public static String ADVANCE_TASK_TOOLTIP;
	public static String RETURN_TO_INTRO_TOOLTIP;
	public static String HELP_BUTTON_TOOLTIP;
	public static String ERROR_RUNNING_ACTION;
	public static String ERROR_INVALID_CHEATSHEET_ID;
	public static String ERROR_CHEATSHEET_DOESNOT_EXIST;
	public static String ERROR_APPLYING_STATE_DATA;
	public static String CHEATSHEET_STATE_RESTORE_FAIL_TITLE;
	public static String CHEATSHEET_STATE_RESET_CONFIRM;
	public static String CHEATSHEET_FROM_URL_WITH_EXEC;
	public static String CHEATSHEET_FROM_URL_WITH_EXEC_TITLE;
	public static String ERROR_APPLYING_STATE_DATA_LOG;
	public static String INITIAL_VIEW_DIRECTIONS;
	public static String ERROR_LOADING_CHEATSHEET_CONTENT;
	public static String ERROR_PAGE_MESSAGE;
	public static String ERROR_LOADING_CLASS;
	public static String ERROR_CREATING_CLASS;
	public static String CHEAT_SHEET_OTHER_CATEGORY;
	public static String LAUNCH_SHEET_ERROR;
	public static String CHEAT_SHEET_ERROR_OPENING;
	public static String ERROR_OPENING_PERSPECTIVE;
	public static String ERROR_SAVING_STATEFILE_URL;
	public static String CHEAT_SHEET_INTRO_TITLE;
	public static String ERROR_TITLE;
	public static String ERROR_CREATING_DOCUMENT_BUILDER;
	public static String ERROR_DOCUMENT_BUILDER_NOT_INIT;
	public static String ERROR_OPENING_FILE;
	public static String ERROR_OPENING_FILE_IN_PARSER;
	public static String ERROR_SAX_PARSING;
	public static String ERROR_SAX_PARSING_WITH_LOCATION;
	public static String ERROR_PARSING_CHEATSHEET_CONTENTS;
	public static String ERROR_PARSING_CHEATSHEET_ELEMENT;
	public static String ERROR_PARSING_NO_INTRO;
	public static String ERROR_PARSING_MORE_THAN_ONE_INTRO;
	public static String ERROR_PARSING_NO_ITEM;
	public static String ERROR_PARSING_PARAM_INVALIDRANGE;
	public static String ERROR_PARSING_PARAM_INVALIDNUMBER;
	public static String ERROR_PARSING_NO_DESCRIPTION;
	public static String ERROR_PARSING_MULTIPLE_DESCRIPTION;
	public static String ERROR_PARSING_NO_SUBITEM;
	public static String ERROR_PARSING_NO_ACTION;
	public static String ERROR_PARSING_NO_TITLE;
	public static String ERROR_PARSING_NO_CLASS;
	public static String ERROR_PARSING_NO_PLUGINID;
	public static String ERROR_PARSING_NO_CONDITION;
	public static String ERROR_PARSING_NO_VALUES;
	public static String ERROR_PARSING_NO_LABEL;
	public static String ERROR_PARSING_NO_SERIALIZATION;
	public static String ERROR_PARSING_INCOMPATIBLE_CHILDREN;
	public static String ERROR_PARSING_DUPLICATE_CHILD;
	public static String ERROR_PARSING_REQUIRED_CONFIRM;
	public static String ERROR_COMMAND_ID_NOT_FOUND;
	public static String ERROR_COMMAND_ERROR_STATUS;
	public static String ERROR_COMMAND_SERVICE_UNAVAILABLE;
	public static String WARNING_PARSING_UNKNOWN_ATTRIBUTE;
	public static String WARNING_PARSING_UNKNOWN_ELEMENT;
	public static String WARNING_PARSING_DESCRIPTION_UNKNOWN_ELEMENT;
	public static String WARNING_PARSING_ON_COMPLETION_UNKNOWN_ELEMENT;
	public static String EXCEPTION_RUNNING_ACTION;
	public static String ACTION_FAILED;
	public static String ERROR_MULTIPLE_ERRORS;
	public static String ERROR_PARSING_ROOT_NODE_TYPE;
	public static String COMPLETED_TASK;
	public static String ERROR_PARSING_DUPLICATE_TASK_ID;
	public static String ERROR_PARSING_NO_VALUE;
	public static String ERROR_PARSING_NO_NAME;
	public static String ERROR_PARSING_NO_ID;
	public static String ERROR_PARSING_MULTIPLE_ROOT;
	public static String ERROR_PARSING_NO_ROOT;
	public static String ERROR_PARSING_INVALID_ID;
	public static String ERROR_PARSING_CYCLE_DETECTED;
	public static String ERROR_PARSING_CYCLE_CONTAINS;
	public static String SELECTION_DIALOG_FILEPICKER_TITLE;
	public static String SELECTION_DIALOG_FILEPICKER_BROWSE;
	public static String SELECTION_DIALOG_OPEN_REGISTERED;
	public static String SELECTION_DIALOG_OPEN_FROM_FILE;
	public static String SELECTION_DIALOG_OPEN_FROM_URL;
	public static String COMPOSITE_PAGE_REVIEW_TASK;
	public static String COMPOSITE_PAGE_GOTO_TASK;
	public static String COMPOSITE_PAGE_START_TASK;
	public static String COMPOSITE_PAGE_SKIP_TASK;
	public static String COMPOSITE_PAGE_SKIP_TASK_GROUP;
	public static String COMPOSITE_MENU_SKIP;
	public static String COMPOSITE_MENU_START;
	public static String COMPOSITE_MENU_REVIEW;
	public static String COMPOSITE_MENU_RESET;
	public static String COMPOSITE_PAGE_BLOCKED;
	public static String COMPOSITE_PAGE_TASK_NOT_COMPLETE;
	public static String EXPLORER_PULLDOWN_MENU;
	public static String COMPOSITE_RESTART_DIALOG_TITLE;
	public static String COMPOSITE_RESTART_CONFIRM_MESSAGE;
	public static String RESTART_ALL_MENU;
	public static String RESTART_MENU;
	public static String ERROR_EDITABLE_TASK_WITH_CHILDREN;
	public static String ERROR_PARSING_TASK_NO_NAME;
	public static String ERROR_PARSING_CCS_NO_NAME;
	public static String ERROR_PARSING_TASK_NO_KIND;
	public static String ERROR_PARSING_TASK_INVALID_KIND;
	public static String ERROR_PARSING_CHILDLESS_TASK_GROUP;
	public static String THIS_TASK_SKIPPED;
	public static String PARENT_SKIPPED;
	public static String PARENT_COMPLETED;
	public static String PARENT_BLOCKED;
	public static String COMPOSITE_RESET_TASK_DIALOG_TITLE;
	public static String COMPOSITE_RESET_TASK_DIALOG_MESSAGE;
	public static String COMPOSITE_PAGE_END_REVIEW;
	public static String CHEATSHEET_TASK_NO_ID;

	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	public static String CheatSheetCategoryBasedSelectionDialog_showAll;
}
