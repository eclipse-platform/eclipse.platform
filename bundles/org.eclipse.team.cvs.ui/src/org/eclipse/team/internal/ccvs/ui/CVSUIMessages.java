/*******************************************************************************
 * Copyright (c) 2005, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *     Maik Schreiber - bug 102461
 *     Philippe Ombredanne - bug 84808
 *     Olexiy Buyanskyy <olexiyb@gmail.com> - Bug 76386 - [History View] CVS Resource History shows revisions from all branches
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui;

import org.eclipse.osgi.util.NLS;

public class CVSUIMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.team.internal.ccvs.ui.messages";//$NON-NLS-1$

	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, CVSUIMessages.class);
	}
	
	public static String AddAction_confirmAddingResourcesMessage;
	public static String AddAction_confirmAddingResourcesTitle;
	public static String AnnotatePreferencePage_AnnotatePrefPageBinaryFileMessage;
	public static String AnnotatePreferencePage_AnnotatePrefPageMessage;
	public static String AnnotatePreferencePage_AnnotatePrefPageTitle;
	public static String ChangeSetContentProvider_0;
	public static String CheckoutAsMainPage_WorkingSetMultiple;
	public static String CheckoutAsMainPage_WorkingSetSingle;
	public static String CommitAction_0;
	public static String CommitAction_3;
	public static String CommitAction_1;
	public static String CommitAction_2;
	public static String CommitCommentArea_10;
	public static String CommitCommentArea_7;
	public static String CommitCommentArea_8;
	public static String CommitCommentArea_9;
	public static String CommitWizard_10;
	public static String CommitWizard_11;
	public static String CommitWizard_12;
	public static String CommitWizard_13;
	public static String CommitWizard_8;
	public static String CommitWizard_9;
	public static String CreatePatchAction_0;
	public static String CreatePatchAction_1;
	public static String CVSHistoryPage_CollapseAllAction;
	public static String CVSHistoryPage_CollapseAllTooltip;
	public static String CVSHistoryPage_CompareModeTooltip;
	public static String CVSHistoryPage_EnterSearchTerm;
	public static String CVSHistoryPage_FilterOn;
	public static String CVSHistoryPage_FilterOnMessage;
	public static String CVSHistoryPage_FilterDescription;
	public static String CVSHistoryPage_ShowSearchField;
	public static String CVSHistoryPage_ValidateChangeMessage;
	public static String CVSHistoryPage_ValidateChangeTitle;
	public static String CVSHistoryPage_SortTagsAscendingAction;
	public static String CVSHistoryPage_SortTagsDescendingAction;
	public static String CVSPreferencesPage_46;
	public static String CVSPreferencesPage_47;
	public static String CVSPreferencesPage_48;
	public static String CVSPreferencesPage_49;
	public static String CVSPreferencesPage_50;
	public static String CVSPreferencesPage_51;
	public static String CVSPreferencesPage_52;
	public static String CVSPreferencesPage_53;
	public static String CVSPreferencesPage_54;
	public static String CVSPreferencesPage_55;
	public static String DiffOperation_ThePatchDoesNotContainAllTheChanges;
	public static String DiffOperation_ThePatchMayNotContainAllTheChanges;
	public static String DiffOperation_CreatePatchConflictMessage;
	public static String DiffOperation_CreatePatchConflictTitle;
	public static String DiffOperation_ErrorsOccurredWhileCreatingThePatch;
	public static String DiffOperation_ErrorAddingFileToDiff;
	public static String GenerateDiffFileWizard_11;
	public static String GenerateDiffFileWizard_12;
	public static String RemoveRootAction_RepositoryRemovalDialogMessageMultiple;
	public static String RemoveRootAction_RepositoryRemovalDialogMessageSingle;
	public static String RemoveRootAction_RepositoryRemovalDialogTitle;
	public static String RepositoriesSortingActionGroup_host;
	public static String RepositoriesSortingActionGroup_label;
	public static String RepositoriesSortingActionGroup_location;
	public static String RepositoriesSortingActionGroup_sortBy;
	public static String RepositoriesSortingActionGroup_descending;
	public static String SyncAction_1;
	public static String UserValidationDialog_5;
	public static String UserValidationDialog_6;
	public static String simpleInternal;
	public static String internal;
	public static String WorkbenchUserAuthenticator_0;
	public static String WorkspaceModelParticipant_0;
	public static String WorkspaceSubscriberContext_1;
	public static String WorkspaceSubscriberContext_2;
	public static String WorkspaceSubscriberContext_3;
	public static String WorkspaceSubscriberContext_4;
	public static String yes;
	public static String no;
	public static String information;
	public static String cvs;
	public static String notAvailable;
	public static String buildError;
	public static String ok;

	public static String nameAndRevision;
	public static String nameRevisionAndAuthor;
	public static String currentRevision;

	public static String AddAction_addFailed;
	public static String AddAction_adding;
	public static String AddAction_addIgnoredTitle;
	public static String AddAction_addIgnoredQuestion;

	public static String AddToVersionControlDialog_title;
	public static String AddToVersionControlDialog_thereIsAnUnaddedResource;
	public static String AddToVersionControlDialog_thereAreUnaddedResources;

	public static String BranchWizard_title;
	public static String BranchWizardPage_pageDescription;
	public static String BranchWizardPage_pageDescriptionVersion;
	public static String BranchWizardPage_specifyVersion;
	public static String BranchWizardPage_branchName;
	public static String BranchWizardPage_versionName;
	public static String BranchWizardPage_startWorking;
	public static String BranchWizardPage_versionPrefix;
	public static String BranchWizard_versionNameWarning;
	public static String BranchWizard_branchNameWarning;
	public static String BranchWizard_branchAndVersionMustBeDifferent;
	public static String BranchWizardPage_existingVersionsAndBranches;

	public static String ConsolePreferencePage_consoleColorSettings;
	public static String ConsolePreferencePage_commandColor;
	public static String ConsolePreferencePage_messageColor;
	public static String ConsolePreferencePage_errorColor;
	public static String CVSAction_errorTitle;
	public static String CVSAction_warningTitle;
	public static String CVSAction_multipleProblemsMessage;
	public static String CVSAction_mixingTagsTitle;
	public static String CVSAction_mixingTags;

	public static String ShowAnnotationAction_noSyncInfo;
	public static String ShowAnnotationOperation_taskName;
	public static String ShowAnnotationOperation_0;
	public static String ShowAnnotationOperation_4;

	public static String CVSCompareEditorInput_branchLabel;
	public static String CVSCompareEditorInput_headLabel;
	public static String CVSCompareEditorInput_comparing;
	public static String CVSCompareEditorInput_different;
	public static String CVSCompareEditorInput_inBranch;
	public static String CVSCompareEditorInput_inHead;
	public static String CVSCompareEditorInput_0;
	public static String CVSCompareEditorInput_1;
	public static String CVSCompareEditorInput_repository;
	public static String CVSCompareEditorInput_titleAncestor;
	public static String CVSCompareEditorInput_titleNoAncestor;
	public static String CVSCompareEditorInput_titleNoAncestorDifferent;
	public static String CVSCompareRevisionsInput_truncate;

	public static String CVSDecoratorPreferencesPage_0;
	public static String CVSDecoratorPreferencesPage_1;
	public static String CVSDecoratorPreferencesPage_2;
	public static String CVSDecoratorPreferencesPage_3;
	public static String CVSDecoratorPreferencesPage_4;
	public static String CVSDecoratorPreferencesPage_5;
	public static String CVSDecoratorPreferencesPage_6;
	public static String CVSDecoratorPreferencesPage_7;
	public static String CVSDecoratorPreferencesPage_8;
	public static String CVSDecoratorPreferencesPage_9;
	public static String CVSDecoratorPreferencesPage_10;
	public static String CVSDecoratorPreferencesPage_11;
	public static String CVSDecoratorPreferencesPage_12;
	public static String CVSDecoratorPreferencesPage_13;
	public static String CVSDecoratorPreferencesPage_14;
	public static String CVSDecoratorPreferencesPage_15;
	public static String CVSDecoratorPreferencesPage_16;
	public static String CVSDecoratorPreferencesPage_17;
	public static String CVSDecoratorPreferencesPage_18;
	public static String CVSDecoratorPreferencesPage_19;
	public static String CVSDecoratorPreferencesPage_20;
	public static String CVSDecoratorPreferencesPage_21;
	public static String CVSDecoratorPreferencesPage_22;
	public static String CVSDecoratorPreferencesPage_23;
	public static String CVSDecoratorPreferencesPage_24;
	public static String CVSDecoratorPreferencesPage_25;
	public static String CVSDecoratorPreferencesPage_26;
	public static String CVSDecoratorPreferencesPage_27;
	public static String CVSDecoratorPreferencesPage_28;
	public static String CVSDecoratorPreferencesPage_29;
	public static String CVSDecoratorPreferencesPage_30;
	public static String CVSDecoratorPreferencesPage_31;
	public static String CVSDecoratorPreferencesPage_32;
	public static String CVSDecoratorPreferencesPage_33;
	public static String CVSDecoratorPreferencesPage_34;
	public static String CVSDecoratorPreferencesPage_35;
	public static String CVSDecoratorPreferencesPage_36;
	public static String CVSDecoratorPreferencesPage_37;
	public static String CVSDecoratorPreferencesPage_38;  // repository label
	public static String CVSDecoratorPreferencesPage_39;
	public static String CVSDecoratorConfiguration_0;
	public static String CVSDecoratorConfiguration_1;
	public static String CVSDecoratorConfiguration_2;
	public static String CVSDecoratorConfiguration_3;
	public static String CVSDecoratorConfiguration_4;
	
	public static String CVSFilePropertiesPage_ignored;
	public static String CVSFilePropertiesPage_notManaged;
	public static String CVSFilePropertiesPage_isAdded;
	public static String CVSFilePropertiesPage_baseRevision;
	public static String CVSFilePropertiesPage_baseTimestamp;
	public static String CVSFilePropertiesPage_modified;
	public static String CVSFilePropertiesPage_keywordMode;
	public static String CVSFilePropertiesPage_tag;
	public static String CVSFilePropertiesPage_none;
	public static String CVSFilePropertiesPage_version;
	public static String CVSFilePropertiesPage_branch;
	public static String CVSFilePropertiesPage_date;
	public static String CVSFilePropertiesPage_error;
	public static String CVSFolderPropertiesPage_ignored;
	public static String CVSFolderPropertiesPage_notManaged;
	public static String CVSFolderPropertiesPage_notCVSFolder;
	public static String CVSFolderPropertiesPage_root;
	public static String CVSFolderPropertiesPage_repository;
	public static String CVSFolderPropertiesPage_static;
	public static String CVSFolderPropertiesPage_disconnect;
	public static String CVSFolderPropertiesPage_disconnectTitle;
	public static String CVSFolderPropertiesPage_disconnectQuestion;

	public static String CVSPropertiesPage_connectionType;
	public static String CVSPropertiesPage_user;
	public static String CVSPropertiesPage_password;
	public static String CVSPropertiesPage_host;
	public static String CVSPropertiesPage_port;
	public static String CVSPropertiesPage_path;
	public static String CVSPropertiesPage_module;
	public static String CVSPropertiesPage_defaultPort;
	public static String CVSPropertiesPage_tag;
	public static String CVSPreferencesPage_0;
	public static String CVSPreferencesPage_1;
	public static String CVSPreferencesPage_2;
	public static String CVSPreferencesPage_3;
	public static String CVSPreferencesPage_4;
	public static String CVSPreferencesPage_5;
	public static String CVSPreferencesPage_10;
	public static String CVSPreferencesPage_11;
	public static String CVSPreferencesPage_12;
	public static String CVSPreferencesPage_13;
	public static String CVSPreferencesPage_14;
	public static String CVSPreferencesPage_15;
	public static String CVSPreferencesPage_16;
	public static String CVSPreferencesPage_17;
	public static String CVSPreferencesPage_18;
	public static String CVSPreferencesPage_19;
	public static String CVSPreferencesPage_23;
	public static String CVSPreferencesPage_24;
	public static String CVSPreferencesPage_25;
	public static String CVSPreferencesPage_26;
	public static String CVSPreferencesPage_27;
	public static String CVSPreferencesPage_28;
	public static String CVSPreferencesPage_29;
	public static String CVSPreferencesPage_20;
	public static String CVSPreferencesPage_21;
	public static String CVSPreferencesPage_22;
	public static String CVSPreferencesPage_30;
	public static String CVSPreferencesPage_31;
	public static String CVSPreferencesPage_32;
	public static String CVSPreferencesPage_33;
	public static String CVSPreferencesPage_34;
	public static String CVSPreferencesPage_35;
	public static String CVSPreferencesPage_36;
	public static String CVSPreferencesPage_37;
	public static String CVSPreferencesPage_38;
	public static String CVSPreferencesPage_39;
	public static String CVSPreferencesPage_40;
	public static String CVSPreferencesPage_41;
	public static String CVSPreferencesPage_44;
	public static String CVSPreferencesPage_45;
	public static String CVSPropertiesPage_virtualModule;


	public static String CVSRemoteFilePropertySource_name;
	public static String CVSRemoteFilePropertySource_revision;
	public static String CVSRemoteFilePropertySource_date;
	public static String CVSRemoteFilePropertySource_author;
	public static String CVSRemoteFilePropertySource_comment;
	public static String CVSRemoteFolderPropertySource_name;
	public static String CVSRemoteFolderPropertySource_tag;
	public static String CVSRemoteFolderPropertySource_none;

	public static String CVSRepositoryLocationPropertySource_default;
	public static String CVSRepositoryLocationPropertySource_host;
	public static String CVSRepositoryLocationPropertySource_user;
	public static String CVSRepositoryLocationPropertySource_port;
	public static String CVSRepositoryLocationPropertySource_root;
	public static String CVSRepositoryLocationPropertySource_method;

	public static String CVSParticipant_0;
	public static String CVSParticipant_1;
	public static String CVSParticipant_2;
	public static String CVSUIPlugin_refreshTitle;
	public static String CVSUIPlugin_refreshQuestion;
	public static String CVSUIPlugin_refreshMultipleQuestion;

	public static String CVSAction_disabledTitle;
	public static String CVSAction_disabledMessage;
	public static String CVSAction_refreshTitle;
	public static String CVSAction_refreshQuestion;
	public static String CVSAction_refreshMultipleQuestion;

	public static String CommitAction_commitFailed;
	public static String CommitWizardCommitPage_0;
	public static String CommitWizardCommitPage_2;
	public static String CommitWizardCommitPage_3;
	public static String CommitWizardCommitPage_4;
	public static String CommitWizardCommitPage_1;
	public static String CommitWizardCommitPage_5;
	public static String CommitWizardFileTypePage_0;
	public static String CommitWizardFileTypePage_2;
	public static String CommitWizardFileTypePage_3;
	
	public static String ConfigureRepositoryLocationsWizard_title;
	public static String ConfigureRepositoryLocationsWizard_message;
	public static String ConfigureRepositoryLocationsWizard_createLocation;
	public static String ConfigureRepositoryLocationsWizard_createLocationTooltip;
	public static String ConfigureRepositoryLocationsWizard_column0;
	public static String ConfigureRepositoryLocationsWizard_column1;
	public static String ConfigureRepositoryLocationsWizard_showOnlyCompatible;
	public static String ConfigureRepositoryLocationsWizardDialog_finish;
	

	public static String CommitSyncAction_questionRelease;
	public static String CommitSyncAction_titleRelease;
	public static String CommitSyncAction_releaseAll;
	public static String CommitSyncAction_releasePart;
	public static String CommitSyncAction_cancelRelease;
	public static String CompareWithRevisionAction_compare;

	public static String CompareWithTagAction_message;
	public static String CompareEditorInput_fileProgress;

	public static String ConfigurationWizardAutoconnectPage_description;
	public static String ConfigurationWizardAutoconnectPage_user;
	public static String ConfigurationWizardAutoconnectPage_host;
	public static String ConfigurationWizardAutoconnectPage_port;
	public static String ConfigurationWizardAutoconnectPage_default;
	public static String ConfigurationWizardAutoconnectPage_connectionType;
	public static String ConfigurationWizardAutoconnectPage_repositoryPath;
	public static String ConfigurationWizardAutoconnectPage_module;
	public static String ConfigurationWizardAutoconnectPage_validate;
	public static String ConfigurationWizardAutoconnectPage_noSyncInfo;
	public static String ConfigurationWizardAutoconnectPage_noCVSDirectory;

	public static String RepositorySelectionPage_description;
	public static String RepositorySelectionPage_useExisting;
	public static String RepositorySelectionPage_useNew;

	public static String ConfigurationWizardMainPage_connection;
	public static String ConfigurationWizardMainPage_userName;
	public static String ConfigurationWizardMainPage_password;
	public static String ConfigurationWizardMainPage_host;
	public static String ConfigurationWizardMainPage_0;
	public static String ConfigurationWizardMainPage_1;
	public static String ConfigurationWizardMainPage_2;
	public static String ConfigurationWizardMainPage_3;
	public static String ConfigurationWizardMainPage_4;
	public static String ConfigurationWizardMainPage_5;
	public static String ConfigurationWizardMainPage_6;
	public static String ConfigurationWizardMainPage_7;
	public static String ConfigurationWizardMainPage_8;
	public static String ConfigurationWizardMainPage_9;
	public static String ConfigurationWizardMainPage_10;
	public static String ConfigurationWizardMainPage_useDefaultPort;
	public static String ConfigurationWizardMainPage_usePort;
	public static String ConfigurationWizardMainPage_repositoryPath;
	public static String ConfigurationWizardMainPage_invalidUserName;
	public static String ConfigurationWizardMainPage_invalidHostName;
	public static String ConfigurationWizardMainPage_invalidPort;
	public static String ConfigurationWizardMainPage_invalidPathWithSpaces;
	public static String ConfigurationWizardMainPage_invalidPathWithSlashes;
	public static String ConfigurationWizardMainPage_invalidPathWithTrailingSlash;
	public static String ConfigurationWizardMainPage_useNTFormat;

	public static String Console_resultServerError;
	public static String Console_resultException;
	public static String Console_resultAborted;
	public static String Console_resultOk;
	public static String Console_resultTimeFormat;
	public static String Console_couldNotFormatTime;
	public static String Console_preExecutionDelimiter;
	public static String Console_postExecutionDelimiter;
	public static String Console_info;
	public static String Console_warning;
	public static String Console_error;

	public static String AddToBranchAction_enterTag;
	public static String AddToBranchAction_enterTagLong;

	public static String GenerateCVSDiff_title;
	public static String GenerateCVSDiff_pageTitle;
	public static String GenerateCVSDiff_pageDescription;
	public static String GenerateCVSDiff_overwriteTitle;
	public static String GenerateCVSDiff_overwriteMsg;
	public static String GenerateCVSDiff_noDiffsFoundMsg;
	public static String GenerateCVSDiff_noDiffsFoundTitle;
	public static String GenerateCVSDiff_1;
	public static String GenerateCVSDiff_2;
	public static String HistoryFilterDialog_title;
	public static String HistoryFilterDialog_showMatching;
	public static String HistoryFilterDialog_matchingAny;
	public static String HistoryFilterDialog_matchingAll;
	public static String HistoryFilterDialog_branchName;
	public static String HistoryFilterDialog_author;
	public static String HistoryFilterDialog_comment;
	public static String HistoryFilterDialog_fromDate;
	public static String HistoryFilterDialog_toDate;

	public static String HistoryView_getContentsAction;
	public static String HistoryView_getRevisionAction;
	public static String HistoryView_tagWithExistingAction;
	public static String HistoryView_copy;
	public static String HistoryView_revision;
	public static String HistoryView_branches;
	public static String HistoryView_tags;
	public static String HistoryView_date;
	public static String HistoryView_author;
	public static String HistoryView_comment;
	public static String HistoryView_refreshLabel;
	public static String HistoryView_refresh;
	public static String HistoryView_selectAll;
	public static String HistoryView_overwriteTitle;
	public static String HistoryView_overwriteMsg;
	public static String HistoryView_fetchHistoryJob;

	public static String IgnoreAction_ignore;

	public static String MergeWizard_title;
	public static String MergeWizard_0;
	public static String MergeWizard_1;
	public static String MergeWizardPage_0;
	public static String MergeWizardPage_1;
	public static String MergeWizardPage_2;
	public static String MergeWizardPage_3;
	public static String MergeWizardPage_4;
	public static String MergeWizardPage_5;
	public static String MergeWizardPage_6;
	public static String MergeWizardPage_7;
	public static String MergeWizardPage_8;
	public static String MergeWizardPage_9;
	public static String MergeWizardPage_10;
	public static String MergeWizardPage_11;
	public static String MergeWizardPage_12;
	public static String MergeWizardPage_13;
	public static String MergeWizardEndPage_branches;

	public static String ModuleSelectionPage_moduleIsProject;
	public static String ModuleSelectionPage_specifyModule;

	public static String ModeWizardSelectionPage_10;
	public static String ModeWizardSelectionPage_11;
	public static String ModeWizardSelectionPage_12;
	public static String ModeWizardSelectionPage_13;
	public static String ModeWizardSelectionPage_14;
	public static String ModeWizardSelectionPage_15;
	public static String ModeWizardSelectionPage_17;
	public static String ModeWizardSelectionPage_18;
	public static String ModeWizardSelectionPage_19;
	public static String ModeWizardSelectionPage_20;
	public static String ModeWizardSelectionPage_21;
	public static String ModeWizardSelectionPage_22;
	public static String ModeWizardSelectionPage_23;
	public static String ModeWizardSelectionPage_24;
	public static String ModeWizardSelectionPage_25;

	public static String MoveTagAction_title;
	public static String MoveTagAction_message;

	public static String NewLocationWizard_title;
	public static String NewLocationWizard_heading;
	public static String NewLocationWizard_description;
	public static String NewLocationWizard_validationFailedText;
	public static String NewLocationWizard_validationFailedTitle;
	public static String NewLocationWizard_exception;
	
	public static String AlternativeLocationWizard_heading;
	public static String AlternativeLocationWizard_validationFailedText;
	public static String AlternativeLocationWizard_validationFailedTitle;
	public static String AlternativeLocationWizard_exception;
	
	public static String AlternativeConfigurationWizardMainPage_0;

	public static String OpenLogEntryAction_deletedTitle;
	public static String OpenLogEntryAction_deleted;

	public static String ReleaseCommentDialog_title;
	public static String ReleaseCommentDialog_unaddedResources;
	public static String ReleaseCommentDialog_selectAll;
	public static String ReleaseCommentDialog_deselectAll;
	public static String RemoteFolderElement_nameAndTag;
	public static String RemoteFolderElement_fetchingRemoteChildren;

	public static String ReplaceWithTagAction_message;
	public static String ReplaceWithTagAction_replace;
	public static String ReplaceWithRemoteAction_problemMessage;

	public static String ReplaceWithAction_confirmOverwrite;
	public static String ReplaceWithAction_localChanges;
	public static String ReplaceWithAction_calculatingDirtyResources;

	public static String ReplaceWithLatestAction_multipleTags;
	public static String ReplaceWithLatestAction_multipleVersions;
	public static String ReplaceWithLatestAction_multipleBranches;
	public static String ReplaceWithLatestAction_singleVersion;
	public static String ReplaceWithLatestAction_singleBranch;
	public static String ReplaceWithLatestAction_singleHEAD;

	public static String RepositoryManager_committing;
	public static String RepositoryManager_rename;
	public static String RepositoryManager_save;
	public static String RepositoryManager_ioException;
	public static String RepositoryManager_parsingProblem;
	public static String RepositoryManager_fetchingRemoteFolders;

	public static String RepositoriesView_refresh;
	public static String RepositoriesView_refreshTooltip;
	public static String RepositoriesView_new;
	public static String RepositoriesView_newSubmenu;
	public static String RepositoriesView_newAnonCVS;
	public static String RepositoriesView_collapseAll;
	public static String RepositoriesView_collapseAllTooltip;
	public static String RepositoriesView_NItemsSelected;
	public static String RepositoriesView_OneItemSelected;
	public static String RepositoriesView_ResourceInRepository;
	public static String RepositoriesView_CannotGetRevision;
	public static String RepositoriesView_NoFilter;
	public static String RepositoriesView_FilterOn;
	public static String RepositoriesView_FilterRepositoriesTooltip;
	public static String RemoteViewPart_workingSetToolTip;
	
	public static String RepositoryFilterDialog_title;
	public static String RepositoryFilterDialog_showModules;

	public static String ResourcePropertiesPage_status;
	public static String ResourcePropertiesPage_notManaged;
	public static String ResourcePropertiesPage_versioned;
	public static String ResourcePropertiesPage_notVersioned;
	//ResourcePropertiesPage.baseRevision=Base Revision
	//ResourcePropertiesPage.none=none
	public static String ResourcePropertiesPage_error;

	public static String SharingWizard_autoConnectTitle;
	public static String SharingWizard_autoConnectTitleDescription;
	public static String SharingWizard_selectTagTitle;
	public static String SharingWizard_selectTag;
	public static String SharingWizard_importTitle;
	public static String SharingWizard_importTitleDescription;
	public static String SharingWizard_title;
	public static String SharingWizard_enterInformation;
	public static String SharingWizard_enterInformationDescription;
	public static String SharingWizard_enterModuleName;
	public static String SharingWizard_enterModuleNameDescription;
	public static String SharingWizard_validationFailedText;
	public static String SharingWizard_validationFailedTitle;

	public static String ShowHistoryAction_showHistory;
	public static String SyncAction_noChangesTitle;
	public static String SyncAction_noChangesMessage;

	public static String TagAction_tagErrorTitle;
	public static String TagAction_tagWarningTitle;
	public static String TagAction_tagProblemsMessage;
	public static String TagAction_tagProblemsMessageMultiple;
	public static String TagAction_tagResources;
	public static String TagRefreshButtonArea_0;
	public static String TagRefreshButtonArea_1;
	public static String TagRefreshButtonArea_2;
	public static String TagRefreshButtonArea_3;
	public static String TagRefreshButtonArea_4;
	public static String TagRefreshButtonArea_5;
	public static String TagRefreshButtonArea_6;
	public static String TagRefreshButtonArea_7;
	public static String TagAction_enterTag;
	public static String TagAction_moveTag;
	public static String TagRootElement_0;
	public static String TagLocalAction_0;
	public static String TagLocalAction_2;
	public static String TagAction_moveTagConfirmTitle;
	public static String TagAction_moveTagConfirmMessage;
	public static String TagAction_existingVersions;

	public static String TagInRepositoryAction_tagProblemsMessage;
	public static String TagInRepositoryAction_tagProblemsMessageMultiple;

	public static String UpdateAction_update;
	public static String UpdateAction_promptForUpdateSeveral;
	public static String UpdateAction_promptForUpdateOne;
	public static String UpdateAction_promptForUpdateTitle;

	public static String UpdateWizard_title;
	public static String UpdateWizard_0;
	public static String UpdateWizard_1;
	public static String UserValidationDialog_required;
	public static String UserValidationDialog_labelUser;
	public static String UserValidationDialog_labelPassword;
	public static String UserValidationDialog_password;
	public static String UserValidationDialog_user;

	public static String KeyboradInteractiveDialog_message;

	public static String VersionsElement_versions;

	public static String WorkbenchUserAuthenticator_cancelled;
	public static String WorkbenchUserAuthenticator_1;
	public static String WorkbenchUserAuthenticator_2;
	public static String Unmanage_title;
	public static String Unmanage_titleN;
	public static String Unmanage_option1;
	public static String Unmanage_option2;
	public static String Unmanage_unmanagingError;

	public static String Unmanage_message;
	public static String Unmanage_messageN;

	public static String Save_To_Clipboard_2;
	public static String Save_In_File_System_3;
	public static String Browse____4;
	public static String Save_Patch_As_5;
	public static String patch_txt_6;
	public static String Save_In_Workspace_7;
	public static String WorkspacePatchDialogTitle;
	public static String WorkspacePatchDialogDescription;
	public static String Fi_le_name__9;
	public static String Diff_output_format_12;
	public static String Unified__format_required_by_Compare_With_Patch_feature__13;
	public static String Context_14;
	public static String Standard_15;
	public static String Advanced_options_19;
	public static String Configure_the_options_used_for_the_CVS_diff_command_20;

	public static String TagSelectionDialog_Select_a_Tag_1;
	public static String TagSelectionDialog_recurseOption;
	public static String TagSelectionDialog_0;
	public static String TagSelectionDialog_1;
	public static String TagSelectionDialog_7;
	public static String TagSelectionDialog_8;

	public static String TagSelectionArea_0;
	public static String TagSelectionArea_1;
	public static String TagSelectionArea_2;
	public static String TagSelectionArea_3;
	public static String TagSelectionWizardPage_0;
	public static String TagSelectionWizardPage_1;

	public static String ExtMethodPreferencePage_message;
	public static String ExtMethodPreferencePage_CVS_RSH;
	public static String ExtMethodPreferencePage_Browse;
	public static String ExtMethodPreferencePage_0;
	public static String ExtMethodPreferencePage_2;
	public static String ExtMethodPreferencePage_1;
	public static String ExtMethodPreferencePage_Details;
	public static String ExtMethodPreferencePage_CVS_RSH_Parameters;
	public static String ExtMethodPreferencePage_CVS_SERVER__7;
	public static String UpdateMergeActionProblems_merging_remote_resources_into_workspace_1;
	public static String TagConfigurationDialog_1;
	public static String TagConfigurationDialog_5;
	public static String TagConfigurationDialog_6;
	public static String TagConfigurationDialog_7;
	public static String TagConfigurationDialog_8;
	public static String TagConfigurationDialog_9;
	public static String TagConfigurationDialog_0;
	public static String TagConfigurationDialog_10;
	public static String TagConfigurationDialog_11;
	public static String TagConfigurationDialog_12;
	public static String TagConfigurationDialog_13;
	public static String TagConfigurationDialog_14;
	public static String TagConfigurationDialog_20;
	public static String TagConfigurationDialog_21;
	public static String TagConfigurationDialog_22;
	public static String TagConfigurationDialog_AddDateTag;

	public static String ConfigureTagsFromRepoViewConfigure_Tag_Error_1;
	public static String RemoteRootAction_label;
	public static String RemoteLogOperation_0;
	public static String RemoteLogOperation_1;
	public static String RemoveDateTagAction_0;
	public static String RemoteRootAction_Unable_to_Discard_Location_1;
	public static String RemoteRootAction_Projects_in_the_local_workspace_are_shared_with__2;
	public static String RemoteRootAction_The_projects_that_are_shared_with_the_above_repository_are__4;

	public static String BranchCategory_Branches_1;
	public static String VersionCategory_Versions_1;
	public static String HistoryView_______4;

	public static String CVSProjectPropertiesPage_connectionType;
	public static String CVSProjectPropertiesPage_user;
	public static String CVSProjectPropertiesPage_Select_a_Repository_1;
	public static String CVSProjectPropertiesPage_Select_a_CVS_repository_location_to_share_the_project_with__2;
	public static String CVSProjectPropertiesPage_Change_Sharing_5;
	public static String CVSProjectPropertiesPage_fetchAbsentDirectoriesOnUpdate;
	public static String CVSProjectPropertiesPage_configureForWatchEdit;
	public static String CVSProjectPropertiesPage_progressTaskName;
	public static String CVSProjectPropertiesPage_setReadOnly;
	public static String CVSProjectPropertiesPage_clearReadOnly;
	public static String CVSRepositoryPropertiesPage_Confirm_Project_Sharing_Changes_1;
	public static String CVSRepositoryPropertiesPage_There_are_projects_in_the_workspace_shared_with_this_repository_2;
	public static String CVSRepositoryPropertiesPage_sharedProject;
	public static String CVSRepositoryPropertiesPage_useLocationAsLabel;
	public static String CVSRepositoryPropertiesPage_useCustomLabel;

	public static String CVSProjectSetSerializer_Confirm_Overwrite_Project_8;
	public static String CVSProjectSetSerializer_The_project__0__already_exists__Do_you_wish_to_overwrite_it__9;

	public static String IgnoreResourcesDialog_dialogTitle;
	public static String IgnoreResourcesDialog_title;
	public static String IgnoreResourcesDialog_messageSingle;
	public static String IgnoreResourcesDialog_messageMany;
	public static String IgnoreResourcesDialog_filesWithSpaceWarningMessage;
	public static String IgnoreResourcesDialog_filesWithSpaceWarning;
	public static String IgnoreResourcesDialog_filesWithNoExtensionWarningMessage;
	public static String IgnoreResourcesDialog_addNameEntryButton;
	public static String IgnoreResourcesDialog_addNameEntryExample;
	public static String IgnoreResourcesDialog_addExtensionEntryButton;
	public static String IgnoreResourcesDialog_addExtensionEntryExample;
	public static String IgnoreResourcesDialog_addCustomEntryButton;
	public static String IgnoreResourcesDialog_addCustomEntryExample;
	public static String IgnoreResourcesDialog_patternMustNotBeEmpty;
	public static String IgnoreResourcesDialog_patternDoesNotMatchFile;

	public static String CVSProjectPropertiesPage_You_can_change_the_sharing_of_this_project_to_another_repository_location__However__this_is_only_possible_if_the_new_location_is___compatible____on_the_same_host_with_the_same_repository_path___1;

	public static String ConfigurationWizardMainPage_Location_1;
	public static String ConfigurationWizardMainPage_Authentication_2;
	public static String ConfigurationWizardMainPage_Connection_3;
	public static String AlternateUserValidationDialog_Enter_Password_2;
	public static String AlternateUserValidationDialog_OK_6;
	public static String AlternateUserValidationDialog_Cancel_7;
	public static String AlternateUserValidationDialog_message;
	public static String WorkbenchUserAuthenticator_The_operation_was_canceled_by_the_user_1;
	public static String WorkingSetSelectionArea_workingSetOther;
	public static String ListSelectionArea_selectAll;
	public static String ListSelectionArea_deselectAll;

	public static String RestoreFromRepositoryWizard_fileSelectionPageTitle;
	public static String RestoreFromRepositoryWizard_fileSelectionPageDescription;
	public static String RestoreFromRepositoryFileSelectionPage_fileSelectionPaneTitle;
	public static String RestoreFromRepositoryFileSelectionPage_revisionSelectionPaneTitle;
	public static String RestoreFromRepositoryFileSelectionPage_fileToRestore;
	public static String RestoreFromRepositoryFileSelectionPage_fileContentPaneTitle;
	public static String RestoreFromRepositoryFileSelectionPage_emptyRevisionPane;
	public static String RestoreFromRepositoryFileSelectionPage_fileExists;
	public static String RestoreFromRepositoryFileSelectionPage_revisionIsDeletion;
	public static String RestoreFromRepositoryAction_noFilesTitle;
	public static String RestoreFromRepositoryAction_noFilesMessage;

	public static String RepositoryRoot_folderInfoMissing;

	public static String RepositoriesViewContentHandler_unmatchedTag;
	public static String RepositoriesViewContentHandler_missingAttribute;
	public static String RepositoriesViewContentHandler_errorCreatingRoot;

	public static String WatchEditPreferencePage_description;
	public static String WatchEditPreferencePage_checkoutReadOnly;
	public static String WatchEditPreferencePage_validateEditSaveAction;
	public static String WatchEditPreferencePage_edit;
	public static String WatchEditPreferencePage_editInBackground;
	public static String WatchEditPreferencePage_highjack;
	public static String WatchEditPreferencePage_editPrompt;
	public static String WatchEditPreferencePage_neverPrompt;
	public static String WatchEditPreferencePage_alwaysPrompt;
	public static String WatchEditPreferencePage_onlyPrompt;
	public static String WatchEditPreferencePage_updatePrompt;
	public static String WatchEditPreferencePage_autoUpdate;
	public static String WatchEditPreferencePage_promptUpdate;
	public static String WatchEditPreferencePage_neverUpdate;
	public static String WatchEditPreferencePage_0;

	public static String Uneditaction_confirmMessage;
	public static String Uneditaction_confirmTitle;

	public static String FileModificationValidator_vetoMessage;

	public static String RefreshRemoteProjectWizard_title;
	public static String RefreshRemoteProjectWizard_0;
	public static String RefreshRemoteProjectWizard_1;
	public static String RefreshRemoteProjectWizard_2;
	public static String RefreshRemoteProjectWizard_3;
	public static String RefreshRemoteProjectWizard_4;
	public static String RefreshRemoteProjectSelectionPage_pageTitle;
	public static String RefreshRemoteProjectSelectionPage_pageDescription;
	public static String RefreshRemoteProjectSelectionPage_selectRemoteProjects;
	public static String RefreshRemoteProjectSelectionPage_noWorkingSet;
	public static String RefreshRemoteProjectSelectionPage_workingSet;

	public static String EditorsView_file;
	public static String EditorsView_user;
	public static String EditorsView_date;
	public static String EditorsView_computer;

	public static String EditorsDialog_title;
	public static String EditorsDialog_question;
	public static String EditorsAction_classNotInitialized;

	public static String CheckoutOperation_thisResourceExists;
	public static String CheckoutOperation_thisExternalFileExists;
	public static String CheckoutOperation_confirmOverwrite;
	public static String CheckoutOperation_scrubbingProject;
	public static String CheckoutOperation_refreshingProject;

	public static String CheckoutSingleProjectOperation_taskname;
	public static String CheckoutMultipleProjectsOperation_taskName;

	public static String CheckoutIntoOperation_taskname;
	public static String CheckoutIntoOperation_targetIsFile;
	public static String CheckoutIntoOperation_targetIsFolder;
	public static String CheckoutIntoOperation_targetIsPrunedFolder;
	public static String CheckoutIntoOperation_mappingAlreadyExists;
	public static String CheckoutIntoOperation_cancelled;
	public static String CheckoutIntoOperation_overwriteMessage;

	public static String CheckoutAsWizard_title;
	public static String CheckoutAsWizard_error;
	public static String CheckoutAsMainPage_title;
	public static String CheckoutAsMainPage_description;
	public static String CheckoutAsMainPage_singleFolder;
	public static String CheckoutAsMainPage_asConfiguredProject;
	public static String CheckoutAsMainPage_asSimpleProject;
	public static String CheckoutAsMainPage_projectNameLabel;
	public static String CheckoutAsMainPage_multipleFolders;
	public static String CheckoutAsMainPage_asProjects;
	public static String CheckoutAsMainPage_intoProject;

	public static String CheckoutAsLocationSelectionPage_title;
	public static String CheckoutAsLocationSelectionPage_description;
	public static String CheckoutAsLocationSelectionPage_useDefaultLabel;
	public static String CheckoutAsLocationSelectionPage_locationLabel;
	public static String CheckoutAsLocationSelectionPage_parentDirectoryLabel;
	public static String CheckoutAsLocationSelectionPage_browseLabel;
	public static String CheckoutAsLocationSelectionPage_locationEmpty;
	public static String CheckoutAsLocationSelectionPage_invalidLocation;
	public static String CheckoutAsLocationSelectionPage_messageForSingle;
	public static String CheckoutAsLocationSelectionPage_messageForMulti;

	public static String CheckoutAsProjectSelectionPage_title;
	public static String CheckoutAsProjectSelectionPage_description;
	public static String CheckoutAsProjectSelectionPage_name;
	public static String CheckoutAsProjectSelectionPage_treeLabel;
	public static String CheckoutAsProjectSelectionPage_showLabel;
	public static String CheckoutAsProjectSelectionPage_recurse;
	public static String CheckoutAsProjectSelectionPage_showAll;
	public static String CheckoutAsProjectSelectionPage_showUnshared;
	public static String CheckoutAsProjectSelectionPage_showSameRepo;
	public static String CheckoutAsProjectSelectionPage_invalidFolderName;

	public static String WorkspaceChangeSetCapability_1;
	public static String OpenCommitSetAction_20;
	public static String OpenCommitSetAction_21;
	public static String WorkspaceChangeSetCapability_2;
	public static String WorkspaceChangeSetCapability_3;
	public static String CVSChangeSetCollector_4;
	public static String CVSChangeSetCollector_0;
	public static String WorkspaceChangeSetCapability_7;
	public static String WorkspaceChangeSetCapability_8;
	public static String WorkspaceChangeSetCapability_9;

	public static String ProjectMetaFile_taskName;
	public static String TagFromWorkspace_taskName;
	public static String TagFromRepository_taskName;
	public static String UpdateOnlyMergeable_taskName;
	public static String UpdateDialog_overwriteTitle;
	public static String UpdateDialog_overwriteMessage;
	public static String ReplaceOperation_taskName;
	public static String UpdateOperation_taskName;

	public static String SafeUpdateAction_warnFilesWithConflictsTitle;
	public static String SafeUpdateAction_warnFilesWithConflictsDescription;

	public static String ShowAnnotationAction_2;
	public static String ShowAnnotationAction_3;

	public static String UpdateAction_jobName;
	public static String MergeUpdateAction_jobName;
	public static String MergeUpdateAction_invalidSubscriber;
	public static String CommitAction_jobName;

	public static String CommitCommentArea_0;
	public static String CommitCommentArea_1;
	public static String CommitCommentArea_2;
	public static String CommitCommentArea_3;
	public static String CommitCommentArea_4;
	public static String CommitCommentArea_5;
	public static String CommitCommentArea_6;
	
	public static String CommentTemplatesPreferencePage_Description;
	public static String CommentTemplatesPreferencePage_New;
	public static String CommentTemplatesPreferencePage_Edit;
	public static String CommentTemplatesPreferencePage_Remove;
	public static String CommentTemplatesPreferencePage_Preview;
	public static String CommentTemplatesPreferencePage_EditCommentTemplateTitle;
	public static String CommentTemplatesPreferencePage_EditCommentTemplateMessage;

	public static String CheckoutProjectOperation_8;
	public static String CheckoutProjectOperation_9;
	public static String CheckoutProjectOperation_0;
	public static String CheckoutProjectOperation_1;
	public static String CVSOperation_0;
	public static String CVSOperation_1;
	public static String CVSOperation_2;
	public static String CVSModelElement_0;
	public static String CVSModelElement_1;
	public static String CVSDecorator_exceptionMessage;
	public static String FetchMembersOperation_0;

	public static String RemoteRevisionQuickDiffProvider_readingFile;
	public static String RemoteRevisionQuickDiffProvider_closingFile;
	public static String RemoteRevisionQuickDiffProvider_fetchingFile;
	public static String RemoteCompareOperation_0;

	public static String RefreshDirtyStateOperation_0;
	public static String RefreshDirtyStateOperation_1;
	public static String IgnoreAction_0;
	public static String IgnoreAction_1;
	public static String GenerateDiffFileOperation_0;
	public static String DiffOperation_0;
	public static String DiffOperation_1;
	public static String GenerateDiffFileWizard_6;
	public static String GenerateDiffFileWizard_7;
	public static String GenerateDiffFileWizard_8;
	public static String GenerateDiffFileWizard_9;
	public static String GenerateDiffFileWizard_10;
	public static String GenerateDiffFileWizard_SelectAll;
	public static String GenerateDiffFileWizard_DeselectAll;
	public static String GenerateDiffFileWizard_0;
	public static String GenerateDiffFileWizard_2;
	public static String GenerateDiffFileWizard_3;
	public static String GenerateDiffFileWizard_4;
	public static String GenerateDiffFileWizard_5;
	public static String GenerateDiffFileWizard_browseFilesystem;
	public static String GenerateDiffFileWizard_noChangesSelected;
	public static String GenerateDiffFileWizard_FolderExists;
	public static String GenerateDiffFileWizard_ProjectClosed;
	public static String MergeSynchronizeParticipant_8;
	public static String MergeSynchronizeParticipant_9;
	public static String MergeSynchronizeParticipant_10;
	public static String MergeSynchronizeParticipant_11;
	public static String MergeSynchronizeParticipant_12;
	public static String DisconnectOperation_0;
	public static String DisconnectOperation_1;
	public static String SubscriberConfirmMergedAction_0;
	public static String SubscriberConfirmMergedAction_jobName;
	public static String CVSSubscriberAction_0;
	public static String ReconcileProjectOperation_0;
	public static String CheckoutToRemoteFolderOperation_0;
	public static String CVSRepositoryPropertiesPage_0;
	public static String CVSRepositoryPropertiesPage_1;
	public static String CompareRevertAction_0;
	public static String CompareParticipant_0;

	public static String ComparePreferencePage_0;
	public static String ComparePreferencePage_2;
	public static String ComparePreferencePage_3;
	public static String ComparePreferencePage_4;
	public static String ComparePreferencePage_5;
	public static String ComparePreferencePage_6;
	public static String ComparePreferencePage_7;
	public static String ComparePreferencePage_8;

	public static String FileModificationValidator_3;
	public static String FileModificationValidator_4;
	public static String FileModificationValidator_5;
	public static String FileModificationValidator_6;
	public static String CVSSynchronizeWizard_0;
	public static String Participant_comparing;
	public static String Participant_merging;
	public static String CompareWithRevisionAction_4;
	public static String ReplaceWithRevisionAction_0;
	public static String ReplaceWithRevisionAction_1;

	public static String ConsolePreferencesPage_4;
	public static String ConsolePreferencesPage_5;
	public static String ConsolePreferencesPage_6;
	public static String ConsolePreferencesPage_7;
	public static String ConsolePreferencesPage_8;
	public static String ConsolePreferencesPage_9;

	public static String SharingWizard_23;
	public static String SharingWizard_24;
	public static String SharingWizard_25;
	public static String ReconcileProjectOperation_1;
	public static String ReconcileProjectOperation_2;
	public static String ShareProjectOperation_0;
	public static String SharingWizard_26;
	public static String SharingWizard_27;
	public static String SharingWizardSyncPage_3;
	public static String SharingWizardSyncPage_8;
	public static String SharingWizardSyncPage_9;
	public static String SharingWizardSyncPage_12;
	public static String ShareProjectOperation_2;
	public static String ShareProjectOperation_3;
	public static String CVSProjectPropertiesPage_31;
	public static String CVSProjectPropertiesPage_32;
	public static String CVSProjectPropertiesPage_33;
	public static String RepositoryEncodingPropertyPage_2;
	public static String RepositoryEncodingPropertyPage_3;
	public static String RepositoryEncodingPropertyPage_0;
	public static String RepositoryEncodingPropertyPage_1;
	public static String RepositoryEncodingPropertyPage_4;
	public static String CheckoutWizard_7;
	public static String CheckoutWizard_8;
	public static String CheckoutWizard_0;
	public static String CheckoutWizard_10;
	public static String CheckoutWizard_11;
	public static String ModuleSelectionPage_2;
	public static String ModuleSelectionPage_3;
	public static String CheckoutAsWizard_3;
	public static String CheckoutAsWizard_4;
	public static String CheckoutAsMainPage_10;
	public static String CVSTeamProvider_updatingFile;
	public static String CVSTeamProvider_makeBranch;
	public static String CVSTeamProvider_folderInfo;
	public static String CVSTeamProvider_updatingFolder;
	public static String AddOperation_0;
	public static String BranchOperation_0;
	public static String BranchOperation_1;
	public static String CommitOperation_0;
	public static String CommitSetDialog_0;
	public static String CommitSetDialog_2;
	public static String CommitWizard_0;
	public static String CommitWizard_1;
	public static String CommitWizard_2;
	public static String CommitWizard_4;
	public static String CommitWizard_6;
	public static String CommitWizard_7;
	public static String UpdateOperation_0;
	public static String ReplaceOperation_0;
	public static String ReplaceOperation_1;
	public static String TagOperation_0;
	public static String RemoteAnnotationStorage_1;
	public static String DateTagCategory_0;
	public static String DateTagDialog_0;
	public static String DateTagDialog_1;
	public static String DateTagDialog_2;
	public static String DateTagDialog_3;
	public static String DateTagDialog_4;
	public static String DateTagDialog_5;
	public static String DateTagDialog_6;
	public static String DateTagDialog_7;	
	public static String LogEntryCacheUpdateHandler_0;
	public static String LogEntryCacheUpdateHandler_1;
	public static String LogEntryCacheUpdateHandler_2;
	public static String MultiFolderTagSource_0;
	public static String LocalProjectTagSource_0;
	public static String ModeWizard_0;
	public static String ModeWizard_1;
	public static String ModeWizard_2;
	public static String ModeWizard_3;
	public static String ModeWizard_4;
	public static String ModeWizard_5;
	public static String ModeWizard_6;

	public static String ModeWizardSelectionPage_2;
	public static String ModeWizardSelectionPage_3;
	public static String ModeWizardSelectionPage_4;
	public static String ModeWizardSelectionPage_8;
	public static String ModeWizardSelectionPage_9;

	public static String ReplaceWithTagAction_0;
	public static String ReplaceWithTagAction_1;
	public static String ReplaceWithTagAction_2;
	public static String AddWizard_0;
	
	public static String OpenChangeSetAction_0;
	public static String OpenChangeSetAction_1;
	
	
	public static String WorkInProgressPage_0;
	public static String WorkInProgressPage_1;
	public static String WorkInProgressPage_2;
	public static String FetchAllMembersOperation_0;
	public static String CacheTreeContentsOperation_0;
	public static String CacheTreeContentsOperation_1;
	public static String CVSMergeContext_1;
	public static String UpdateMergePreferencePage_0;
	public static String UpdateMergePreferencePage_1;
	public static String UpdateMergePreferencePage_2;
	public static String UpdateMergePreferencePage_3;
	public static String WorkspaceSubscriberContext_0;
	public static String ModelReplaceOperation_0;
	public static String ModelReplaceOperation_1;
	public static String ModelReplaceOperation_2;
	public static String ModelReplaceOperation_3;
	public static String MergeWizardPage_14;
	public static String CVSHistoryTableProvider_base;
	public static String WorkspaceTraversalAction_0;
	public static String OutgoingChangesDialog_0;
	public static String OutgoingChangesDialog_1;
	public static String CVSHistoryPage_LocalModeAction;
	public static String CVSHistoryPage_LocalModeTooltip;
	public static String CVSHistoryPage_RemoteModeAction;
	public static String CVSHistoryPage_RemoteModeTooltip;
	public static String CVSHistoryPage_NoRevisions;
	public static String CVSHistoryPage_CombinedModeAction;
	public static String CVSHistoryPage_CombinedModeTooltip;
	public static String CVSHistoryPage_CompareRevisionAction;
	public static String CVSHistoryPage_CompareModeToggleAction;
	public static String CVSHistoryPage_FilterHistoryTooltip;
	public static String CVSHistoryPage_OpenAction;
	public static String CVSHistoryPage_OpenWithMenu;
	public static String CVSHistoryPage_GroupByDate;
	public static String CVSHistoryPage_Today;
	public static String CVSHistoryPage_Yesterday;
	public static String CVSHistoryPage_ThisMonth;
	public static String CVSHistoryPage_Previous;
	public static String CVSHistoryPage_NoRevisionsForMode;
	public static String CVSHistoryPage_NoFilter;
	
	public static String NewLocationWizard_1;
	public static String NewLocationWizard_2;
	public static String NewLocationWizard_3;
	public static String NewLocationWizard_4;
	public static String ClipboardDiffOperation_Clipboard;
	public static String CVSAction_doNotShowThisAgain;
	
	public static String CVSScmUrlImportWizardPage_0;
	public static String CVSScmUrlImportWizardPage_1;
	public static String CVSScmUrlImportWizardPage_2;
	public static String CVSScmUrlImportWizardPage_3;
	public static String CVSScmUrlImportWizardPage_4;
}
