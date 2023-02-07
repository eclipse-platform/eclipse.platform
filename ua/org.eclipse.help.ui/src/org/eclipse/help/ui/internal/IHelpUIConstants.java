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
 *******************************************************************************/
package org.eclipse.help.ui.internal;
/**
 * Interface for holding Help UI plug-in constants
 */
public interface IHelpUIConstants {
	// Help UI pluging id with a "." for convenience.
	public static final String HELP_UI_PLUGIN_ID = HelpUIPlugin.PLUGIN_ID + "."; //$NON-NLS-1$
	// F1 ids
	public static final String F1_SHELL = HELP_UI_PLUGIN_ID + "f1Shell"; //$NON-NLS-1$
	public static final String PREF_PAGE_HELP = HELP_UI_PLUGIN_ID
			+ "prefPageHelp"; //$NON-NLS-1$
	public static final String PREF_PAGE_HELP_CONTENT = HELP_UI_PLUGIN_ID
			+ "prefPageHelpContent"; //$NON-NLS-1$
	public static final String PREF_PAGE_CUSTOM_BROWSER_PATH = HELP_UI_PLUGIN_ID
			+ "prefPageCustomBrowserPath"; //$NON-NLS-1$

	public static final String IMAGE_FILE_F1TOPIC = "obj16/topic_small.png"; //$NON-NLS-1$
	public static final String IMAGE_COMMAND_F1TOPIC = "obj16/command_small.png"; //$NON-NLS-1$

	// Help view images
	public static final String IMAGE_CONTAINER = "obj16/container_obj.png"; //$NON-NLS-1$
	public static final String IMAGE_TOC_CLOSED = "obj16/toc_closed.png"; //$NON-NLS-1$
	public static final String IMAGE_TOC_OPEN = "obj16/toc_open.png"; //$NON-NLS-1$
	public static final String IMAGE_BOOKMARKS = "obj16/bookmarks_view.png"; //$NON-NLS-1$
	public static final String IMAGE_BOOKMARK = "obj16/bookmark_obj.png"; //$NON-NLS-1$
	public static final String IMAGE_HELP_SEARCH = "etool16/helpsearch_co.png"; //$NON-NLS-1$
	public static final String IMAGE_HELP_PRINT = "etool16/print_topic.png"; //$NON-NLS-1$
	public static final String IMAGE_RELATED_TOPICS = "etool16/reltopics_co.png"; //$NON-NLS-1$
	public static final String IMAGE_ALL_TOPICS = "etool16/alltopics_co.png"; //$NON-NLS-1$
	public static final String IMAGE_INDEX = "etool16/index_co.png"; //$NON-NLS-1$
	public static final String IMAGE_HELP = "etool16/help.png"; //$NON-NLS-1$
	public static final String IMAGE_DHELP = "view16/help_view.png"; //$NON-NLS-1$
	public static final String IMAGE_NW = "elcl16/openseparate_co.png"; //$NON-NLS-1$
	public static final String IMAGE_SHOW_CATEGORIES = "elcl16/showcat_co.png"; //$NON-NLS-1$
	public static final String IMAGE_SHOW_DESC = "elcl16/showdesc_co.png"; //$NON-NLS-1$
	public static final String IMAGE_ADD_BOOKMARK = "elcl16/addbkmrk_co.png"; //$NON-NLS-1$
	public static final String IMAGE_HIGHLIGHT = "elcl16/highlight.png"; //$NON-NLS-1$
	public static final String IMAGE_MAGNIFY = "elcl16/magnify_font.png"; //$NON-NLS-1$
	public static final String IMAGE_REDUCE = "elcl16/reduce_font.png"; //$NON-NLS-1$
	public static final String IMAGE_D_MAGNIFY = "dlcl16/magnify_font.png"; //$NON-NLS-1$
	public static final String IMAGE_D_REDUCE = "dlcl16/reduce_font.png"; //$NON-NLS-1$
	public static final String IMAGE_COLLAPSE_ALL = "elcl16/collapseall.png"; //$NON-NLS-1$
	public static final String IMAGE_CLOSE = "elcl16/close.png"; //$NON-NLS-1$
	public static final String IMAGE_CLOSE_HOT = "elcl16/close_hot.png"; //$NON-NLS-1$
	public static final String IMAGE_SYNC_TOC = "elcl16/synch_toc_nav.png"; //$NON-NLS-1$
	public static final String IMAGE_SHOW_ALL = "elcl16/show_all.png"; //$NON-NLS-1$
	public static final String IMAGE_DOC_OVR = "ovr16/doc_co.png"; //$NON-NLS-1$
	public static final String IMAGE_SCOPE_SET = "obj16/scopeset_obj.png"; //$NON-NLS-1$
	public static final String IMAGE_SEARCH_WIZ = "wizban/newsearch_wiz.png"; //$NON-NLS-1$
	// Help view constants
	public static final String HV_SEARCH = "search"; //$NON-NLS-1$
	public static final String HV_FSEARCH = "fsearch"; //$NON-NLS-1$
	public static final String HV_SEARCH_RESULT = "search-result"; //$NON-NLS-1$
	public static final String HV_FSEARCH_RESULT = "fsearch-result"; //$NON-NLS-1$

	public static final String HV_TOPIC_TREE = "topic-tree"; //$NON-NLS-1$
	public static final String HV_BOOKMARKS_TREE = "bookmarks-tree"; //$NON-NLS-1$
	public static final String HV_BOOKMARKS_HEADER = "bookmarks-header"; //$NON-NLS-1$
	public static final String HV_SEE_ALSO = "see-also"; //$NON-NLS-1$
	public static final String HV_BROWSER = "browser"; //$NON-NLS-1$
	public static final String HV_CONTEXT_HELP = "context-help"; //$NON-NLS-1$
	public static final String HV_FSEARCH_PAGE = "fsearch-page"; //$NON-NLS-1$
	public static final String HV_ALL_TOPICS_PAGE = "all-topics-page"; //$NON-NLS-1$
	public static final String HV_BOOKMARKS_PAGE = "bookmarks-page"; //$NON-NLS-1$
	public static final String HV_BROWSER_PAGE = "browser-page"; //$NON-NLS-1$
	public static final String HV_RELATED_TOPICS = "related-topics"; //$NON-NLS-1$
	public static final String HV_CONTEXT_HELP_PAGE = "context-help-page"; //$NON-NLS-1$
	public static final String HV_MISSING_CONTENT = "missing-content"; //$NON-NLS-1$

	public static final String HV_INDEX = "index"; //$NON-NLS-1$
	public static final String HV_INDEX_TYPEIN = "index-typein"; //$NON-NLS-1$
	public static final String HV_INDEX_PAGE = "index-page"; //$NON-NLS-1$
	public static final String HV_SCOPE_SELECT = "scope-select"; //$NON-NLS-1$

	static final String ENGINE_EXP_ID = "org.eclipse.help.ui.searchEngine"; //$NON-NLS-1$
	static final String TAG_ENGINE = "engine"; //$NON-NLS-1$
	static final String ATT_ID = "id"; //$NON-NLS-1$

	static final String ATT_LABEL ="label"; //$NON-NLS-1$
	static final String ATT_ICON = "icon";//$NON-NLS-1$
	static final String ATT_CLASS = "class";//$NON-NLS-1$
	static final String ATT_ENABLED = "enabled"; //$NON-NLS-1$
	static final String ATT_PAGE_CLASS = "pageClass";//$NON-NLS-1$
	static final String ATT_CATEGORY = "category";//$NON-NLS-1$
	static final String TAG_DESC = "description"; //$NON-NLS-1$
	static final String ATT_SCOPE_FACTORY = "scopeFactory";//$NON-NLS-1$
	static final String ATT_ENGINE_TYPE_ID = "engineTypeId"; //$NON-NLS-1$
	static final String ATT_NAME = "name";//$NON-NLS-1$
	static final String ATT_VALUE = "value";//$NON-NLS-1$
	static final int ADD = 1;
	static final int REMOVE = 2;
	static final int CHANGE = 3;
	static final String INTERNAL_HELP_ID = "org.eclipse.help.ui.localSearch";//$NON-NLS-1$
}
