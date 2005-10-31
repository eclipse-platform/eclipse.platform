/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.configurator;

import org.eclipse.osgi.util.NLS;

public final class Messages extends NLS {

	private static final String BUNDLE_NAME = "org.eclipse.update.internal.configurator.messages";//$NON-NLS-1$

	private Messages() {
		// Do not instantiate
	}

	public static String ok;
	public static String url_badVariant;
	public static String url_invalidURL;
	public static String url_createConnection;
	public static String url_noaccess;
	public static String cfig_inUse;
	public static String cfig_failCreateLock;
	public static String cfig_badUrlArg;
	public static String cfig_unableToLoad_incomplete;
	public static String cfig_unableToLoad_noURL;
	public static String cfig_unableToSave_noURL;
	public static String cfig_unableToSave;
	public static String cfig_badVersion;
	public static String cfg_unableToCreateConfig_ini;
	public static String platform_running;
	public static String platform_mustNotBeRunning;
	public static String platform_notRunning;
	public static String ignore_plugin;
	public static String application_notFound;
	public static String error_fatal;
	public static String error_boot;
	public static String error_runtime;
	public static String error_xerces;
	public static String error_badNL;
	public static String InstalledSiteParser_DirectoryDoesNotExist;
	public static String InstalledSiteParser_UnableToCreateURL;
	public static String InstalledSiteParser_FileDoesNotExist;
	public static String InstalledSiteParser_UnableToCreateURLForFile;
	public static String InstalledSiteParser_ErrorParsingFile;
	public static String InstalledSiteParser_ErrorAccessing;
	public static String InstalledSiteParser_date;
	public static String BundleManifest_noVersion;
	public static String FeatureParser_IdOrVersionInvalid;
	public static String BundleGroupProvider;
	public static String ProductProvider;
	public static String AboutInfo_notSpecified;
	public static String ConfigurationActivator_initialize;
	public static String ConfigurationActivator_createConfig;
	public static String ConfigurationActivator_uninstallBundle;
	public static String ConfigurationParser_cannotLoadSharedInstall;
	public static String ConfigurationActivator_installBundle;
	public static String PluginEntry_versionError;
	public static String IniFileReader_MissingDesc;
	public static String IniFileReader_OpenINIError;
	public static String IniFileReader_OpenPropError;
	public static String IniFileReader_OpenMapError;
	public static String IniFileReader_ReadIniError;
	public static String IniFileReader_ReadPropError;
	public static String IniFileReader_ReadMapError;
	public static String SiteEntry_computePluginStamp;
	public static String SiteEntry_cannotFindFeatureInDir;
	public static String SiteEntry_duplicateFeature;
	public static String SiteEntry_pluginsDir;
	public static String PlatformConfiguration_expectingPlatformXMLorDirectory;
	public static String PlatformConfiguration_cannotBackupConfig;
	public static String PlatformConfiguration_cannotCloseStream;
	public static String PlatformConfiguration_cannotCloseTempFile;
	public static String PlatformConfiguration_cannotRenameTempFile;
	public static String PlatformConfiguration_cannotLoadConfig;
	public static String PlatformConfiguration_cannotLoadDefaultSite;
	public static String PlatformConfiguration_cannotFindConfigFile;
	public static String PlatformConfiguration_cannotSaveNonExistingConfig;
	public static String HttpResponse_rangeExpected;
	public static String HttpResponse_wrongRange;
	public static String PluginParser_plugin_no_id;
	public static String PluginParser_plugin_no_version;

	static {
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	public static String XMLPrintHandler_unsupportedNodeType;
}