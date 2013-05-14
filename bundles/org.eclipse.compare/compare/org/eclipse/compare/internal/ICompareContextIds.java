/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal;

import org.eclipse.compare.CompareUI;

/**
 * Help context ids for the Compare UI.
 * <p>
 * This interface contains constants only; it is not intended to be implemented
 * or extended.
 * </p>
 * 
 */
public interface ICompareContextIds {
	
	public static final String PREFIX= CompareUI.PLUGIN_ID + '.';
	
	// Dialogs
	public static final String EDITION_DIALOG= PREFIX + "edition_dialog_context"; //$NON-NLS-1$

	public static final String COMPARE_EDITOR= PREFIX + "compare_editor_context"; //$NON-NLS-1$
	public static final String PATCH_INPUT_WIZARD_PAGE= PREFIX + "patch_input_wizard_page_context";	//$NON-NLS-1$
	public static final String PATCH_PREVIEW_WIZARD_PAGE= PREFIX + "patch_preview_wizard_page_context";	//$NON-NLS-1$
	public static final String ADD_FROM_HISTORY_DIALOG= PREFIX + "add_from_history_dialog_context"; //$NON-NLS-1$
	public static final String COMPARE_DIALOG= PREFIX + "compare_dialog_context"; //$NON-NLS-1$
	public static final String COMPARE_WITH_EDITION_DIALOG= PREFIX + "compare_with_edition_dialog_context"; //$NON-NLS-1$
	public static final String REPLACE_WITH_EDITION_DIALOG= PREFIX + "replace_with_edition_dialog_context"; //$NON-NLS-1$
	
	// Viewer
	public static final String TEXT_MERGE_VIEW= PREFIX + "text_merge_view_context"; //$NON-NLS-1$
	public static final String IMAGE_COMPARE_VIEW= PREFIX + "image_compare_view_context"; //$NON-NLS-1$
	public static final String BINARY_COMPARE_VIEW= PREFIX + "binary_compare_view_context"; //$NON-NLS-1$
	public static final String DIFF_VIEW= PREFIX + "diff_view_context"; //$NON-NLS-1$
	
	// Actions
	public static final String GLOBAL_NEXT_DIFF_ACTION= PREFIX + "global_next_diff_action_context"; //$NON-NLS-1$
	public static final String GLOBAL_PREVIOUS_DIFF_ACTION= PREFIX + "global_previous_diff_action_context"; //$NON-NLS-1$
	public static final String NEXT_DIFF_ACTION= PREFIX + "next_diff_action_context"; //$NON-NLS-1$
	public static final String PREVIOUS_DIFF_ACTION= PREFIX + "previous_diff_action_context"; //$NON-NLS-1$
	public static final String IGNORE_WHITESPACE_ACTION= PREFIX + "ignore_whitespace_action_context"; //$NON-NLS-1$

	// Preference page
	public static final String COMPARE_PREFERENCE_PAGE= PREFIX + "compare_preference_page_context"; //$NON-NLS-1$
}
