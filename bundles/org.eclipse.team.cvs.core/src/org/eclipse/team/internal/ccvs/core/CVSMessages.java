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
 * IBM - Initial API and implementation
 *     Olexiy Buyanskyy <olexiyb@gmail.com> - Bug 76386 - [History View] CVS Resource History shows revisions from all branches
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.core;

import org.eclipse.osgi.util.NLS;

public class CVSMessages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.team.internal.ccvs.core.messages";//$NON-NLS-1$
	public static String CVSResourceVariantTree_GettingSyncInfoError;
	public static String FolderSyncInfo_InvalidSyncInfoBytes;
	public static String LogEntry_0;
	public static String ok;
	public static String AbstractStructureVisitor_sendingFolder;
	public static String AbstractStructureVisitor_sendingFile;
	public static String AbstractStructureVisitor_noRemote;

	public static String CVSAuthenticationException_detail;
	public static String CVSCommunicationException_io;
	public static String CVSCommunicationException_interruptCause;
	public static String CVSCommunicationException_interruptSolution;
	public static String CVSCommunicationException_alternateInterruptCause;
	public static String CVSCommunicationException_alternateInterruptSolution;
	public static String CVSStatus_messageWithRoot;

	public static String CVSTag_nullName;
	public static String CVSTag_emptyName;
	public static String CVSTag_beginName;
	public static String CVSTag_badCharName;
	public static String CVSTag_unknownBranch;

	public static String CVSWorkspaceRoot_notCVSFolder;

	public static String java_io_IOException;
	public static String java_io_EOFException;
	public static String java_io_FileNotFoundException;
	public static String java_io_InterruptedIOException;
	public static String java_net_UnknownHostException;
	public static String java_net_ConnectException;
	public static String java_net_SocketException;
	public static String java_net_NoRouteToHostException;
	
	public static String Connection_cannotClose;
	public static String Connection_readUnestablishedConnection;
	public static String Connection_writeUnestablishedConnection;
	public static String Connection_0;
	public static String PrepareForReplaceVisitor_DeletedFileWithoutHistoryCannotBeRestoredWhileRevertToBase;
	public static String PrepareForReplaceVisitor_FileCannotBeReplacedWithBase;

	public static String PServerConnection_invalidChars;
	public static String PServerConnection_loginRefused;
	public static String PServerConnection_invalidUser;
	public static String PServerConnection_socket;
	public static String PServerConnection_connectionRefused;
	public static String PServerConnection_noResponse;
	public static String PServerConnection_authenticating;

	public static String CVSProviderPlugin_unknownStateFileVersion;

	public static String CVSProvider_ioException;
	public static String CVSProvider_errorLoading;
	public static String CVSProvider_infoMismatch;

	public static String CVSTeamProvider_noFolderInfo;
	public static String CVSTeamProvider_invalidResource;
	public static String CVSTeamProvider_typesDiffer;
	public static String CVSTeamProvider_settingKSubst;
	public static String CVSTeamProvider_cleanLineDelimitersException;
	public static String CVSTeamProvider_changingKeywordComment;
	public static String CVSTeamProvider_errorGettingFetchProperty;
	public static String CVSTeamProvider_errorSettingFetchProperty;
	public static String CVSTeamProvider_overlappingRemoteFolder;
	public static String CVSTeamProvider_overlappingFileDeletion;
	public static String CVSTeamProvider_errorGettingWatchEdit;
	public static String CVSTeamProvider_errorSettingWatchEdit;
	public static String CVSTeamProvider_updatingFolder;
	public static String CVSCoreFileModificationValidator_editJob;

	public static String ResourceDeltaVisitor_visitError;

	public static String EclipseResource_invalidResourceClass;

	public static String RemoteResource_invalidOperation;
	public static String RemoteFolder_invalidChild;
	public static String RemoteFolder_errorFetchingRevisions;
	public static String RemoteFolder_errorFetchingMembers;
	public static String RemoteFolder_doesNotExist;

	public static String RemoteFolderTreeBuilder_buildingBase;
	public static String RemoteFolderTreeBuilder_0;
	public static String RemoteFolderTreeBuilder_receivingDelta;
	public static String RemoteFolderTreeBuilder_receivingRevision;
	public static String RemoteFolderTreeBuilder_missingParent;
	public static String RemoteFolderTreeBuild_folderDeletedFromServer;

	public static String Session_badInt;
	public static String Session_receiving;
	public static String Session_transfer;
	public static String Session_transferNoSize;
	public static String Session_calculatingCompressedSize;
	public static String Session_0;
	public static String Session_sending;

	public static String Command_receivingResponses;
	public static String Command_warnings;
	public static String Command_serverError;
	public static String Command_noMoreInfoAvailable;
	public static String Command_unsupportedResponse;
	public static String Command_argumentNotManaged;
	public static String Command_invalidTag;
	public static String Command_noOpenSession;
	public static String Command_seriousServerError;

	public static String Add_invalidParent;

	public static String Commit_syncInfoMissing;
	public static String Commit_timestampReset;

	public static String Diff_serverError;

	public static String Tag_notVersionOrBranchError;

	public static String ModTimeHandler_invalidFormat;
	public static String UpdateListener_0;
	public static String RemovedHandler_invalid;
	public static String RemovedHandler_0;
	public static String CheckInHandler_checkedIn;

	public static String KSubstOption__kb_short;
	public static String KSubstOption__kb_long;
	public static String KSubstOption__ko_short;
	public static String KSubstOption__ko_long;
	public static String KSubstOption__kkv_short;
	public static String KSubstOption__kkv_long;
	public static String KSubstOption__kkvl_short;
	public static String KSubstOption__kkvl_long;
	public static String KSubstOption__kv_short;
	public static String KSubstOption__kv_long;
	public static String KSubstOption__kk_short;
	public static String KSubstOption__kk_long;
	public static String KSubstOption_unknown_short;
	public static String KSubstOption_unknown_long;
	
	public static String AdminKSubstListener_expectedRCSFile;
	public static String AdminKSubstListener_commandRootNotManaged;
	public static String AdminKSubstListener_expectedChildOfCommandRoot;
	public static String CVSRepositoryLocation_locationForm;
	public static String CVSRepositoryLocation_methods;
	public static String CVSRepositoryLocation_parsingMethod;
	public static String CVSRepositoryLocation_parsingUser;
	public static String CVSRepositoryLocation_parsingPassword;
	public static String CVSRepositoryLocation_parsingHost;
	public static String CVSRepositoryLocation_parsingPort;
	public static String CVSRepositoryLocation_parsingRoot;
	public static String CVSRepositoryLocation_invalidFormat;
	public static String CVSRepositoryLocation_openingConnection;
	public static String CVSRepositoryLocation_usernameRequired;
	public static String CVSRepositoryLocation_hostRequired;
	public static String CVSRepositoryLocation_rootRequired;
	public static String CVSRepositoryLocation_noAuthenticator;

	public static String Util_timeout;
	public static String Util_processTimeout;
	public static String Util_truncatedPath;

	public static String ResourceSyncInfo_malformedSyncBytes;
	public static String Malformed_entry_line___11;
	public static String Malformed_entry_line__missing_name___12;
	public static String Malformed_entry_line__missing_revision___13;
	public static String FolderSyncInfo_Maleformed_root_4;
	public static String SyncFileWriter_baseNotAvailable;
	public static String BaseRevInfo_malformedEntryLine;

	public static String EXTServerConnection_invalidPort;
	public static String EXTServerConnection_varsNotSet;
	public static String CVSRemoteSyncElement_rootDiffers;
	public static String CVSRemoteSyncElement_repositoryDiffers;
	public static String Util_Internal_error__resource_does_not_start_with_root_3;

	public static String CVSProvider_Scrubbing_local_project_1;
	public static String CVSProvider_Scrubbing_projects_1;
	public static String CVSProvider_Creating_projects_2;

	public static String EclipseFile_Problem_deleting_resource;
	public static String EclipseFile_Problem_accessing_resource;
	public static String EclipseFile_Problem_writing_resource;
	public static String EclipseFolder_problem_creating;
	public static String EclipseFolder_isModifiedProgress;
	public static String EclipseFolder_0;

	public static String EclipseSynchronizer_UpdatingSyncEndOperation;
	public static String EclipseSynchronizer_UpdatingSyncEndOperationCancelled;
	public static String EclipseSynchronizer_NotifyingListeners;
	public static String EclipseSynchronizer_ErrorSettingFolderSync;
	public static String EclipseSynchronizer_ErrorSettingResourceSync;
	public static String EclipseSynchronizer_ErrorSettingIgnorePattern;
	public static String EclipseSynchronizer_ErrorCommitting;
	public static String EclipseSynchronizer_folderSyncInfoMissing;
	public static String SyncFileChangeListener_errorSettingTeamPrivateFlag;

	public static String RemoteFile_getContents;
	public static String RemoteFile_getLogEntries;
	public static String RemoteFolder_exists;
	public static String RemoteFolder_getMembers;
	public static String RemoteModule_getRemoteModules;
	public static String RemoteModule_invalidDefinition;

	public static String Version_unsupportedVersion;
	public static String Version_unknownVersionFormat;
	public static String Version_versionNotValidRequest;

	public static String LogListener_invalidRevisionFormat;
	public static String NotifyInfo_MalformedLine;
	public static String NotifyInfo_MalformedNotificationType;
	public static String NotifyInfo_MalformedNotifyDate;

	public static String ResourceSynchronizer_missingParentBytesOnGet;
	public static String ResourceSynchronizer_missingParentBytesOnSet;
	public static String CVSAnnotateBlock_4;
	public static String CVSAnnotateBlock_5;
	public static String CVSAnnotateBlock_6;
	public static String CVSMergeSubscriber_2;
	public static String CVSProviderPlugin_20;
	public static String CRLFDetectInputStream_0;
	public static String DeferredResourceChangeHandler_0;
	public static String DeferredResourceChangeHandler_1;
	public static String CVSWorkspaceRoot_11;
	public static String RemoveEntryHandler_2;
	public static String ServerMessageLineMatcher_5;
	public static String ServerMessageLineMatcher_6;
	public static String ServerMessageLineMatcher_7;
	public static String CVSSyncInfo_7;
	public static String CVSSyncInfo_8;
	public static String CVSSyncInfo_9;
	public static String CVSSyncInfo_10;
	public static String CVSCompareSubscriber_2;
	public static String AnnotateListener_3;
	public static String AnnotateListener_4;
	public static String CVSWorkspaceSubscriber_1;
	public static String CVSWorkspaceSubscriber_2;
	public static String KnownRepositories_0;
	public static String CVSRepositoryLocation_72;
	public static String CVSRepositoryLocation_73;
	public static String CVSRepositoryLocation_74;
	public static String CVSRepositoryLocation_75;
	public static String SyncFileWriter_0;
	public static String ResponseHandler_0;

	public static String CVSTeamProviderType_0;
	public static String CVSFileSystem_FetchTree;
	public static String CVSURI_InvalidURI;
	public static String ThePatchDoesNotContainChangesFor_0;

	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, CVSMessages.class);
	}

	public static String CVSFileHistory_0;
}
