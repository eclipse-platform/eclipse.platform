/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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

import org.eclipse.osgi.util.NLS;

public final class Messages extends NLS {

	private static final String BUNDLE_NAME = "org.eclipse.help.ui.internal.Messages";//$NON-NLS-1$

	private Messages() {
		// Do not instantiate
	}

	public static String Help_Error;
	public static String Help_Question;
	public static String Help_Info;
	public static String browserTitle;
	public static String select_browser;
	public static String SystemBrowser_noProgramForURL;
	public static String LaunchURL_exception;
	public static String ReusableHelpPart_searchPage_name;
	public static String ReusableHelpPart_allTopicsPage_name;
	public static String ReusableHelpPart_bookmarksPage_name;
	public static String ReusableHelpPart_contextHelpPage_name;
	public static String ReusableHelpPart_back_label;
	public static String ReusableHelpPart_back_tooltip;
	public static String ReusableHelpPart_forward_label;
	public static String ReusableHelpPart_forward_tooltip;
	public static String ReusableHelpPart_openInfoCenterAction_label;
	public static String ReusableHelpPart_openAction_label;
	public static String ReusableHelpPart_openInHelpContentsAction_label;
	public static String ReusableHelpPart_closeAction_tooltip;
	public static String ReusableHelpPart_copyAction_label;
	public static String ReusableHelpPart_bookmarkAction_label;
	public static String ReusableHelpPart_status;
	public static String ReusableHelpPart_indexPage_name;
	public static String ReusableHelpPart_missingContent;
	public static String HelpView_defaultText;
	public static String expression;
	public static String expression_label;
	public static String limit_to;
	public static String ScopeSelect_scope;
	public static String SeeAlsoPart_search;
	public static String SeeAlsoPart_allTopics;
	public static String SeeAlsoPart_goto;
	public static String SeeAlsoPart_contextHelp;
	public static String SeeAlsoPart_bookmarks;
	public static String SeeAlsoPart_index;
	public static String ContextHelpPart_about;
	public static String ContextHelpPart_aboutP;
	public static String ContextHelpPart_noDescription;
	public static String ContextHelpPart_query_view;
	public static String ContextHelpPart_seeAlso;
	public static String ContextHelpPart_more;
	public static String ContextHelpPart_searchFor;
	public static String SearchResultsPart_label;
	public static String SearchResultsPart_noHits;
	public static String SearchResultsPart_progress;
	public static String SearchResultsPart_cancel;
	public static String SearchResultsPart_moreResults;
	public static String SearchPart_dynamicJob;
	public static String SearchPart_learnMore;
	public static String SearchPart_title;
	public static String SearchPart_stop;
	public static String SearchPart_go;
	public static String SearchPart_collapse;
	public static String SearchPart_expand;
	public static String SearchPart_potential_hit;
	public static String WorkingSetContent;
	public static String WorkingSetCriteria;
	public static String UncategorizedCriteria;
	public static String selectAll;
	public static String selectWorkingSet;
	public static String FederatedSearchPart_advanced;
	public static String FederatedSearchPart_changeScopeSet;
	public static String ScopeSet_default;
	public static String ScopeSetDialog_wtitle;
	public static String ScopeSetDialog_new;
	public static String ScopeSetDialog_edit;
	public static String ScopeSetDialog_remove;
	public static String ScopeSetDialog_rename;
	public static String ScopeSetDialog_defaultName;
	public static String RenameDialog_wtitle;
	public static String NewDialog_wtitle;
	public static String RenameDialog_label;
	public static String RenameDialog_validationError;
	public static String RenameDialog_emptyName;
	public static String EngineResultSection_progress2;
	public static String EngineResultSection_canceling;
	public static String EngineResultSection_progressError;
	public static String EngineResultSection_previous;
	public static String EngineResultSection_next;
	public static String EngineResultSection_sectionTitle_error;
	public static String EngineResultSection_sectionTitle_hit;
	public static String EngineResultSection_progressTooltip;
	public static String EngineResultSection_searchInProgress;
	public static String EngineResultSection_sectionTitle_hits;
	public static String EngineResultSection_sectionTitle_hitsRange;
	public static String EngineTypeWizardPage_title;
	public static String EngineTypeWizardPage_desc;
	public static String EngineTypeWizardPage_label;
	public static String EngineDescriptorManager_errorLoading;
	public static String EngineDescriptorManager_errorSaving;
	public static String SearchResultsPart_showCategoriesAction_tooltip;
	public static String SearchResultsPart_showDescriptionAction_tooltip;
	public static String ScopePreferenceDialog_wtitle;
	public static String ScopePreferenceDialog_new;
	public static String ScopePreferenceDialog_delete;
	public static String NewEngineWizard_wtitle;
	public static String BrowserPart_showExternalTooltip;
	public static String HelpPreferencePage_message;
	public static String HelpPreferencePage_infopop;
	public static String BrowserPart_bookmarkTooltip;
	public static String BrowserPart_magnifyTooltip;
	public static String BrowserPart_reduceTooltip;
	public static String BrowserPart_highlightTooltip;
	public static String BrowserPart_printTooltip;
	public static String BrowserPart_syncTocTooltip;
	public static String HelpPreferencePage_wlabel;
	public static String HelpPreferencePage_view;
	public static String HelpPreferencePage_helpBrowser;
	public static String HelpPreferencePage_externalBrowser;
	public static String HelpPreferencePage_dlabel;
	public static String HelpPreferencePage_tray;
	public static String HelpPreferencePage_search;
	public static String HelpPreferencePage_searchLocation;
	public static String HelpPreferencePage_openContents;
	public static String InfoCenterPage_url;
	public static String InfoCenterPage_invalidURL;
	public static String InfoCenterPage_tocError;
	public static String WebSearchPage_label;
	public static String WebSearchPage_info;
	public static String RootScopePage_masterButton;
	public static String RootScopePage_name;
	public static String RootScopePage_desc;
	public static String AllTopicsPart_collapseAll_tooltip;
	public static String AllTopicsPart_showAll_tooltip;
	public static String BookmarksPart_savedTopics;
	public static String BookmarksPart_delete;
	public static String BookmarksPart_deleteAll;
	public static String AskShowAll_dialogTitle;
	public static String AskShowAll_message;
	public static String AskShowAll_toggleMessage;

	public static String DefaultHelpUI_wtitle;
	public static String DefaultHelpUI_noPerspMessage;
	public static String ContextHelpDialog_showInDynamicHelp;

	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	public static String HelpPreferencePage_openModeGroup;
	public static String HelpPreferencePage_contextHelpGroup;
	public static String HelpPreferencePage_olabel;
	public static String HelpPreferencePage_openInPlace;
	public static String HelpPreferencePage_openInEditor;
	public static String HelpPreferencePage_openInBrowser;

	public static String ReusableHelpPart_internalBrowserTitle;
	public static String ReusableHelpPart_internalWebBrowserError;
	public static String ScopeSet_errorLoading;
	public static String ScopeSet_errorSaving;
	public static String ScopeSet_selectAll;
	public static String ScopeSet_selectWorkingSet;
	public static String IndexInstructions;
	public static String IndexButton;

	public static String NoWorkbenchForExecuteCommand_msg;

	public static String SearchEmbeddedHelpOnly;
	public static String SearchEmbeddedHelpFirst;
	public static String SearchEmbeddedHelpLast;

	// Remote Help Preferences
	public static String AddICDialog_10;

	public static String AddICDialog_17;

	public static String AddICDialog_19;

	public static String AddICDialog_2;

	public static String AddICDialog_4;

	public static String AddICDialog_5;

	public static String AddICDialog_7;

	public static String AddICDialog_8;

	public static String AddICDialog_9;

	public static String EditICDialog_10;

	public static String EditICDialog_11;

	public static String EditICDialog_12;

	public static String EditICDialog_13;

	public static String EditICDialog_14;

	public static String EditICDialog_15;

	public static String EditICDialog_7;


	public static String HelpContentBlock_3;
	public static String HelpContentBlock_4;

	public static String HelpContentPage_title;
	public static String HelpContentBlock_addICTitle;
	public static String HelpContentBlock_editICTitle;
	public static String HelpContentBlock_removeICTitle;
	public static String HelpContentBlock_viewICTitle;
	public static String HelpContentBlock_testConnectionTitle;
	public static String HelpContentBlock_rmvTitle;
	public static String HelpContentBlock_rmvLabel;
	public static String HelpContentBlock_upTitle;
	public static String HelpContentBlock_downTitle;

	public static String RemoteICLabelProvider_4;

	public static String RemoteICLabelProvider_5;

	public static String RemoteICViewer_Enabled;
	public static String RemoteICViewer_Name;
	public static String RemoteICViewer_URL;

	public static String TestConnectionDialog_12;

	public static String TestConnectionDialog_13;

	public static String TestConnectionDialog_4;

	public static String TestConnectionDialog_6;
	public static String TestConnectionDialog_URL_With_Param;

	public static String ViewICPropsDialog_10;

	public static String ViewICPropsDialog_11;

	public static String ViewICPropsDialog_12;

	public static String ViewICPropsDialog_13;

	public static String ViewICPropsDialog_14;

	public static String ViewICPropsDialog_19;

	public static String ViewICPropsDialog_20;

	public static String ViewICPropsDialog_21;

	public static String ViewICPropsDialog_23;

	public static String ViewICPropsDialog_24;

	public static String ViewICPropsDialog_6;

	public static String ViewICPropsDialog_7;

	public static String ViewICPropsDialog_8;
	public static String ViewICPropsDialog_URL;
	public static String remoteHelpUnavailable;

	public static String See;
	public static String SeeAlso;
	public static String SeeList;

	public static String AlternateQueries;
}
