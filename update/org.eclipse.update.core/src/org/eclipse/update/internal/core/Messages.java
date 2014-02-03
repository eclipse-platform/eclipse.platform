/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.core;

import org.eclipse.osgi.util.NLS;

public final class Messages extends NLS {

	private static final String BUNDLE_NAME = "org.eclipse.update.internal.core.messages";//$NON-NLS-1$

	private Messages() {
		// Do not instantiate
	}

	public static String BaseSiteFactory_CannotRetriveParentDirectory;
	public static String ContentReference_UnableToCreateInputStream;
	public static String ContentReference_UnableToReturnReferenceAsFile;
	public static String ContentReference_UnableToReturnReferenceAsURL;
	public static String ContentReference_HttpNok;
	public static String ContentReference_FileDoesNotExist;
	public static String Feature_SiteAlreadySet;
	public static String Feature_TaskInstallFeatureFiles;
	public static String Feature_TaskInstallPluginFiles;
	public static String Feature_NoContentProvider;
	public static String Feature_NoFeatureContentConsumer;
	public static String Feature_NoURL;
	public static String Feature_FeatureVersionToString;
	public static String Feature_InstallationCancelled;
	public static String Feature_UnableToInitializeFeatureReference;
	public static String FeatureContentProvider_Downloading;
	public static String FeatureContentProvider_UnableToRetrieve;
	public static String FeatureContentProvider_UnableToCreate;
	public static String FeatureContentProvider_ExceptionDownloading;
	public static String InstallHandler_unableToCreateHandler;
	public static String InstallHandler_notFound;
	public static String InstallHandler_invalidHandler;
	public static String InstallHandler_callException;
	public static String InstallHandler_error;
	public static String InstallMonitor_DownloadSize;
	public static String InstallMonitor_DownloadSizeLong;
	public static String ProductProvider;
	public static String JarContentReference_Unpacking;
	public static String Site_CannotFindCategory;
	public static String Site_NoCategories;
	public static String Site_NoContentProvider;
	public static String VersionedIdentifier_IdOrVersionNull;
	public static String SiteFile_CannotRemovePlugin;
	public static String SiteFile_CannotRemoveFeature;
	public static String SiteFile_UnableToCreateURL;
	public static String SiteFile_Removing;
	public static String SiteFileFactory_UnableToCreateURL;
	public static String SiteFileFactory_UnableToAccessSite;
	public static String SiteFileFactory_DirectoryDoesNotExist;
	public static String DefaultFeatureParser_location;
	public static String DefaultFeatureParser_NoFeatureTag;
	public static String DefaultFeatureParser_WrongParsingStack;
	public static String DefaultFeatureParser_UnknownElement;
	public static String DefaultFeatureParser_TooManyURLtag;
	public static String DefaultFeatureParser_UnknownStartState;
	public static String DefaultFeatureParser_IdOrVersionInvalid;
	public static String DefaultFeatureParser_MissingId;
	public static String DefaultFeatureParser_ParsingStackBackToInitialState;
	public static String DefaultFeatureParser_ElementAlreadySet;
	public static String DefaultFeatureParser_StateIncludeWrongElement;
	public static String DefaultFeatureParser_RequireStateWithoutImportElement;
	public static String DefaultFeatureParser_MissingPatchVersion;
	public static String DefaultFeatureParser_wrongMatchForPatch;
	public static String DefaultFeatureParser_patchWithPlugin;
	public static String DefaultFeatureParser_MultiplePatchImports;
	public static String DefaultFeatureParser_UnknownEndState;
	public static String DefaultFeatureParser_ErrorParsing;
	public static String DefaultFeatureParser_ErrorlineColumnMessage;
	public static String DefaultFeatureParser_ErrorParsingFeature;
	public static String DefaultFeatureParser_UnknownState;
	public static String DefaultFeatureParser_NoLicenseText;
	public static String DefaultFeatureParser_PluginAndFeatureId;
	public static String SiteContentProvider_ErrorCreatingURLForArchiveID;
	public static String DefaultSiteParser_NoSiteTag;
	public static String DefaultSiteParser_WrongParsingStack;
	public static String DefaultSiteParser_UnknownElement;
	public static String DefaultSiteParser_UnknownStartState;
	public static String DefaultSiteParser_Missing;
	public static String DefaultSiteParser_ParsingStackBackToInitialState;
	public static String DefaultSiteParser_ElementAlreadySet;
	public static String DefaultSiteParser_UnknownEndState;
	public static String DefaultSiteParser_ErrorParsing;
	public static String DefaultSiteParser_ErrorlineColumnMessage;
	public static String DefaultSiteParser_ErrorParsingSite;
	public static String DefaultSiteParser_UnknownState;
	public static String DefaultSiteParser_InvalidXMLStream;
	public static String ModelObject_ModelReadOnly;
	public static String SiteModelObject_ErrorParsingSiteStream;
	public static String SiteModelObject_ErrorAccessingSiteStream;
	public static String InstallConfiguration_ErrorDuringFileAccess;
	public static String InstallConfigurationParser_FeatureReferenceNoURL;
	public static String FeatureExecutableContentProvider_FileDoesNotExist;
	public static String FeatureExecutableContentProvider_InvalidDirectory;
	public static String FeatureExecutableContentProvider_UnableToCreateURLFor;
	public static String FeatureExecutableContentProvider_UnableToRetrieveNonPluginEntry;
	public static String FeatureExecutableContentProvider_UnableToRetrieveFeatureEntry;
	public static String FeatureExecutableContentProvider_UnableToRetrievePluginEntry;
	public static String ConfiguredSite_NonInstallableSite;
	public static String ConfiguredSite_NullFeatureToInstall;
	public static String ConfiguredSite_NonUninstallableSite;
	public static String ConfiguredSite_NoSite;
	public static String ConfiguredSite_CannotFindFeatureToUnconfigure;
	public static String ConfiguredSite_CannotFindFeatureToConfigure;
	public static String ConfiguredSite_CannotFindPluginEntry;
	public static String ConfiguredSite_MissingPluginsBrokenFeature;
	public static String ConfiguredSite_UnableToRemoveConfiguredFeature;
	public static String ConfiguredSite_UnableToFindFeature;
	public static String ConfiguredSite_SiteURLNull;
	public static String ConfiguredSite_NonLocalSite;
	public static String ConfiguredSite_NotSameProductId;
	public static String ConfiguredSite_ContainedInAnotherSite;
	public static String ConfiguredSite_ReadOnlySite;
	public static String ConfiguredSite_UnableResolveURL;
	public static String ConfiguredSite_UnableToAccessSite;
	public static String FeatureFactory_CreatingError;
	public static String FeatureModelFactory_ErrorAccesingFeatureStream;
	public static String FeatureExecutableFactory_NullURL;
	public static String FeatureExecutableFactory_CannotCreateURL;
	public static String FeaturePackagedContentProvider_NoManifestFile;
	public static String FeaturePackagedContentProvider_InvalidDirectory;
	public static String FeaturePackagedContentProvider_ErrorRetrieving;
	public static String FeatureReference_UnableToResolveURL;
	public static String FeatureTypeFactory_UnableToFindFeatureFactory;
	public static String InstallConfiguration_UnableToCreateURL;
	public static String InstallConfiguration_UnableToCast;
	public static String InstallConfiguration_UnableToSavePlatformConfiguration;
	public static String InstallConfiguration_AlreadyNativelyLinked;
	public static String InstallConfiguration_AlreadyProductSite;
	public static String InstallConfiguration_unableToFindSite;
	public static String InternalSiteManager_UnableToCreateSiteWithType;
	public static String InternalSiteManager_UnableToAccessURL;
	public static String InternalSiteManager_UnableToCreateURL;
	public static String InternalSiteManager_FailedRetryAccessingSite;
	public static String InternalSiteManager_ConnectingToSite;
	public static String GlobalConsumer_ErrorCreatingFile;
	public static String SiteFileContentConsumer_UnableToCreateURL;
	public static String SiteFileContentConsumer_UnableToCreateURLForFile;
	public static String SiteFileContentConsumer_unableToDelete;
	public static String ContentConsumer_UnableToRename;
	public static String SiteFileFactory_UnableToObtainParentDirectory;
	public static String SiteFileFactory_FileDoesNotExist;
	public static String SiteFileFactory_UnableToCreateURLForFile;
	public static String SiteFileFactory_ErrorParsingFile;
	public static String SiteFileFactory_ErrorAccessing;
	public static String SiteTypeFactory_UnableToFindSiteFactory;
	public static String UpdateManagerUtils_UnableToRemoveFile;
	public static String UpdateManagerUtils_FileAlreadyExists;
	public static String SiteLocal_UnableToCreateURLFor;
	public static String SiteLocal_UnableToDetermineFeatureStatusSiteNull;
	public static String SiteLocal_TwoVersionSamePlugin1;
	public static String SiteLocal_TwoVersionSamePlugin2;
	public static String SiteLocal_FeatureUnHappy;
	public static String SiteLocal_FeatureHappy;
	public static String SiteLocal_FeatureAmbiguous;
	public static String SiteLocal_NestedFeatureUnHappy;
	public static String SiteLocal_NestedFeatureUnavailable;
	public static String SiteLocal_NoPluginVersion;
	public static String SiteLocal_UnableToDetermineFeatureStatusConfiguredSiteNull;
	public static String SiteLocal_FeatureDisable;
	public static String SiteLocal_FeatureStatusUnknown;
	public static String SiteLocal_NestedFeatureDisable;
	public static String SiteURLFactory_UnableToCreateURL;
	public static String SiteURLFactory_UnableToAccessSiteStream;
	public static String JarVerifier_Verify;
	public static String JarVerifier_UnableToFindEncryption;
	public static String JarVerifier_UnableToLoadCertificate;
	public static String JarVerifier_UnableToFindProviderForKeystore;
	public static String JarVerifier_KeyStoreNotLoaded;
	public static String JarVerifier_UnableToAccessJar;
	public static String JarVerifier_InvalidFile;
	public static String JarVerifier_InvalidJar;
	public static String JarVerificationResult_ValidBetween;
	public static String JarVerificationResult_ExpiredCertificate;
	public static String JarVerificationResult_CertificateNotYetValid;
	public static String JarVerificationResult_CertificateValid;
	public static String JarVerificationService_UnsucessfulVerification;
	public static String JarVerificationService_CancelInstall;
	public static String UpdateManagerUtils_UnableToLog;
	public static String ConnectionThreadManager_tooManyConnections;
	public static String ConnectionThreadManager_unresponsiveURL;
	public static String IncludedFeatureReference_featureUninstalled;
	public static String ActivityConstraints_warning;
	public static String ActivityConstraints_rootMessage;
	public static String ActivityConstraints_rootMessageInitial;
	public static String ActivityConstraints_beforeMessage;
	public static String ActivityConstraints_afterMessage;
	public static String ActivityConstraints_platform;
	public static String ActivityConstraints_primary;
	public static String ActivityConstaints_prereq_plugin;
	public static String ActivityConstaints_prereq_feature;
	public static String ActivityConstraints_prereq;
	public static String ActivityConstraints_prereqPerfect;
	public static String ActivityConstraints_prereqEquivalent;
	public static String ActivityConstraints_prereqCompatible;
	public static String ActivityConstraints_prereqGreaterOrEqual;
	public static String ActivityConstraints_os;
	public static String ActivityConstraints_ws;
	public static String ActivityConstraints_arch;
	public static String ActivityConstraints_cycle;
	public static String ActivityConstraints_childMessage;
	public static String ActivityConstraints_optionalChild;
	public static String ActivityConstraints_exclusive;
	public static String ActivityConstraints_noLicense;
	public static String ActivityConstraints_readOnly;
	public static String ActivityConstraints_platformModified;
	public static String DuplicateConflictsDialog_conflict;
	public static String OperationsManager_error_old;
	public static String OperationsManager_installing;
	public static String OperationsManager_error_uninstall;
	public static String Search_networkProblems;
	public static String InstallConfiguration_location_exists;
	public static String InstallLogParser_errors;
	public static String SiteLocal_cloneConfig;
	public static String UpdateManagerUtils_inputStreamEnded;
	public static String UpdateSearchRequest_loadingPolicy;
	public static String UpdateManagerUtils_copy;
	public static String UpdatePolicy_parsePolicy;
	public static String UpdatePolicy_policyExpected;
	public static String UpdateSearchRequest_searching;
	public static String UpdateSearchRequest_contacting;
	public static String UpdateSearchRequest_checking;
	public static String UpdatePolicy_invalidURL;
	public static String UpdatePolicy_nameNoNull;
	public static String UpdatePolicy_UpdatePolicy;
	public static String SiteFile_featureNotRemoved;
	public static String SiteFile_pluginNotRemoved;
	public static String ErrorRecoveryLog_noFiletoRemove;
	public static String UpdatesSearchCategory_errorSearchingForUpdates;
    public static String UninstallCommand_featureNotInstalledByUM;
	public static String Standalone_siteConfigured;
	public static String Standalone_noSite;
	public static String Standalone_noSite3;
	public static String Standalone_noConfiguredSite;
	public static String Standalone_installing;
	public static String Standalone_notFoundOrNewer;
	public static String Standalone_duplicate;
	public static String Standalone_installed;
	public static String Standalone_cannotInstall;
	public static String Standalone_noFeatures1;
	public static String Standalone_noFeatures2;
	public static String Standalone_noFeatures3;
	public static String Standalone_noFeatures4;
	public static String Standalone_noConfigSiteForFeature;
	public static String Standalone_invalidCmd;
	public static String Standalone_connection;
	public static String Standalone_searching;
	public static String Standalone_cmdFailed;
	public static String Standalone_cmdFailedNoLog;
	public static String Standalone_cmdCompleteWithErrors;
	public static String Standalone_cmdOK;
	public static String Standalone_updating;
	public static String Standalone_noUpdate;
	public static String Standalone_updated;
	
	public static String SiteFilePluginContentConsumer_unableToDelete;
	public static String SiteFilePackedPluginContentConsumer_unableToDelete;	
	
	public static String HttpResponse_rangeExpected;
	public static String HttpResponse_wrongRange;
	public static String DefaultSiteParser_mirrors;
	public static String FeatureExecutableContentProvider_UnableToRetriveArchiveContentRef;
	
	public static String JarProcessor_unpackNotFound;
	public static String JarProcessor_noPackUnpack;
	public static String JarProcessor_packNotFound;
	
	public static String SiteOptimizer_inputNotSpecified;
	public static String SiteOptimizer_inputFileNotFound;
	public static String SiteCategory_other_label;
	public static String SiteCategory_other_description;

	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	public static String InstallCommand_site;
}
