/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.team.internal.core;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.team.internal.core.messages";//$NON-NLS-1$

	public static String LocalFileHistory_RefreshLocalHistory;

	public static String MergeContext_5;

	public static String MergeContext_6;

	public static String ok;
	public static String concatStrings;

	public static String AbstractResourceVariantTree_0;

	public static String Assert_assertionFailed;

	public static String FileModificationValidator_someReadOnly;
	public static String FileModificationValidator_fileIsReadOnly;
	public static String FileModificationValidator_editFailed;

	public static String RepositoryProvider_Error_removing_nature_from_project___1;
	public static String RepositoryProvider_couldNotInstantiateProvider;
	public static String RepositoryProvider_No_Provider_Registered;
	public static String RepositoryProvider_linkedResourcesExist;
	public static String RepositoryProvider_linkedResourcesNotSupported;
	public static String RepositoryProvider_linkedURIsExist;
	public static String RepositoryProvider_linkedURIsNotSupported;
	public static String RepositoryProvider_couldNotClearAfterError;
	public static String RepositoryProvider_invalidClass;
	public static String RepositoryProvider_toString;

	public static String SubscriberDiffTreeEventHandler_0;

	public static String Team_readError;
	public static String PollingInputStream_readTimeout;
	public static String PollingInputStream_closeTimeout;
	public static String PollingOutputStream_writeTimeout;
	public static String PollingOutputStream_closeTimeout;
	public static String TimeoutOutputStream_cannotWriteToStream;

	public static String RemoteSyncElement_delimit;
	public static String RemoteSyncElement_insync;
	public static String RemoteSyncElement_conflicting;
	public static String RemoteSyncElement_outgoing;
	public static String RemoteSyncElement_incoming;
	public static String RemoteSyncElement_change;
	public static String RemoteSyncElement_addition;
	public static String RemoteSyncElement_deletion;
	public static String RemoteSyncElement_manual;
	public static String RemoteSyncElement_auto;

	public static String Team_Error_loading_ignore_state_from_disk_1;
	public static String Team_Conflict_occured_for_ignored_resources_pattern;

	public static String RemoteContentsCache_cacheDisposed;
	public static String RemoteContentsCache_fileError;
	public static String SubscriberEventHandler_2;
	public static String SubscriberEventHandler_jobName;
	public static String SubscriberChangeSetCollector_0;
	public static String SubscriberChangeSetCollector_1;
	public static String SubscriberChangeSetCollector_2;
	public static String SubscriberChangeSetCollector_3;
	public static String SubscriberChangeSetCollector_4;
	public static String SubscriberChangeSetCollector_5;
	public static String SubscriberEventHandler_errors;
	public static String RemoteContentsCacheEntry_3;
	public static String SynchronizationCacheRefreshOperation_0;
	public static String SubscriberEventHandler_8;
	public static String SubscriberEventHandler_9;
	public static String SubscriberEventHandler_10;
	public static String SubscriberEventHandler_11;
	public static String CachedResourceVariant_0;
	public static String CachedResourceVariant_1;
	public static String SyncInfoTree_0;
	public static String ResourceVariantTreeSubscriber_1;
	public static String ResourceVariantTreeSubscriber_2;
	public static String ResourceVariantTreeSubscriber_3;
	public static String ResourceVariantTreeSubscriber_4;
	public static String SyncByteConverter_1;
	public static String BatchingLock_11;
	public static String SubscriberEventHandler_12;
	public static String ProjectSetCapability_0;
	public static String ProjectSetCapability_1;

	public static String SubscriberResourceMappingContext_0;
	public static String SubscriberResourceMappingContext_1;
	public static String MergeContext_0;
	public static String MergeContext_1;
	public static String MergeContext_2;
	public static String MergeContext_3;
	public static String MergeContext_4;

	public static String LocalFileRevision_currentVersion;
	public static String LocalFileRevision_currentVersionTag;
	public static String LocalFileRevision_localRevisionTag;
	static {
		// load message values from bundle file
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	public static String DelegatingStorageMerger_0;

	public static String DelegatingStorageMerger_1;

	public static String WorkspaceSubscriber_0;

	public static String WorkspaceSubscriber_1;

	public static String ScopeManagerEventHandler_0;

	public static String ScopeManagerEventHandler_1;

	public static String TextAutoMerge_inputEncodingError;
	public static String TextAutoMerge_conflict;
	public static String TextAutoMerge_outputEncodingError;
	public static String TextAutoMerge_outputIOError;
}
