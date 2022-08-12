/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
package org.eclipse.team.internal.ui;

public interface IPreferenceIds {
	public static final String PREFIX = TeamUIPlugin.ID + "."; //$NON-NLS-1$

	/*
	 * Default model provider layout to use with SubscriberSynchronizePage. The user can configure but this
	 * is used to select the initial layout.
	 */
	public static final String SYNCVIEW_DEFAULT_LAYOUT = PREFIX + "default_layout"; //$NON-NLS-1$

	/*
	 * Values used to indicate which layout to use as the default.
	 */
	public static final String FLAT_LAYOUT = PREFIX + "flay_layout"; //$NON-NLS-1$
	public static final String COMPRESSED_LAYOUT = PREFIX + "compressed_layout"; //$NON-NLS-1$
	public static final String TREE_LAYOUT = PREFIX + "tree_layout"; //$NON-NLS-1$

	/*
	 * Previous preference which is kept so it can be converted to the new preference (SYNCVIEW_DEFAULT_LAYOUT)
	 * @deprecated
	 */
	public static final String SYNCVIEW_COMPRESS_FOLDERS = PREFIX + "compress_folders"; //$NON-NLS-1$

	/*
	 * Preference to enable displaying synchronization state in the elements label. This
	 * preference is used automatically with {@link StructuredViewerAdvisor}.
	 */
	public static final String SYNCVIEW_VIEW_SYNCINFO_IN_LABEL = PREFIX + "view_syncinfo_in_label"; //$NON-NLS-1$

	/*
	 * Preference to enable the presence of the author field in a compare editor
	 */
	public static final String SHOW_AUTHOR_IN_COMPARE_EDITOR = PREFIX + "show_author_in_compare_editor"; //$NON-NLS-1$

	/*
	 * Preference to enable the presence of the author field in a compare editor
	 */
	public static final String MAKE_FILE_WRITTABLE_IF_CONTEXT_MISSING = PREFIX + "validate_edit_with_no_context"; //$NON-NLS-1$
	public static final String REUSE_OPEN_COMPARE_EDITOR= PREFIX + "reuse_open_compare_editors"; //$NON-NLS-1$

	/*
	 * Preference to enable the import of a project set to be run in the background
	 */
	public static final String RUN_IMPORT_IN_BACKGROUND= PREFIX + "run_import_in_background_"; //$NON-NLS-1$

	/*
	 * Preference to enable synchronizing with a patch via Apply Patch action
	 */
	public static final String APPLY_PATCH_IN_SYNCHRONIZE_VIEW = PREFIX + "apply_patch_in_sychronize_view"; //$NON-NLS-1$

	/*
	 * Preference to manage the perspective used to synchronize.
	 */
	public static final String SYNCVIEW_DEFAULT_PERSPECTIVE = PREFIX + "syncview_default_perspective"; //$NON-NLS-1$
	public static final String SYNCVIEW_DEFAULT_PERSPECTIVE_NONE = PREFIX + "sync_view_perspective_none"; //$NON-NLS-1$
	public static final String SYNCHRONIZING_COMPLETE_PERSPECTIVE = PREFIX + "sychronizing_default_perspective_to_show"; //$NON-NLS-1$

	/*
	 * Preference to save the last participant selected via the global synchronize action.
	 */
	public static final String SYNCHRONIZING_DEFAULT_PARTICIPANT = PREFIX + "sychronizing_default_participant"; //$NON-NLS-1$
	public static final String SYNCHRONIZING_DEFAULT_PARTICIPANT_SEC_ID = PREFIX + "sychronizing_default_participant_sec_id"; //$NON-NLS-1$

	/*
	 * Preference for disabling various prompts
	 */
	public static final String SYNCVIEW_REMOVE_FROM_VIEW_NO_PROMPT = PREFIX + "remove_from_view_without_prompt"; //$NON-NLS-1$

	/*
	 * Preferences for the Local History Page
	 */
	public static final String PREF_GROUPBYDATE_MODE = PREFIX + "group_bydate_mode"; //$NON-NLS-1$

}
