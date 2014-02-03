/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     James D Miles (IBM Corp.) - bug 191368, Policy URL doesn't support UTF-8 characters
 *******************************************************************************/
package org.eclipse.update.internal.operations;

import java.lang.reflect.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Vector;

import org.eclipse.core.runtime.*;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.update.core.model.*;
import org.eclipse.update.internal.core.*;
import org.eclipse.update.internal.core.URLEncoder;
import org.eclipse.update.internal.search.*;
import org.eclipse.update.operations.*;
import org.eclipse.update.search.*;


/**
 * Helper class for performing update related tasks, queries, etc.
 * All the methods are static and this class should be a singleton.
 */
public class UpdateUtils {
	public static final String P_UPDATE_POLICY_URL = "updatePolicyURL"; //$NON-NLS-1$

	/**
	 * Private constructor
	 */
	private UpdateUtils() {
	}
	

	public static String getPluginId() {
		return UpdateCore.getPlugin().getBundle().getSymbolicName();
	}


	public static void logException(Throwable e) {
		
		if (e instanceof InvocationTargetException) {
			e = ((InvocationTargetException) e).getTargetException();
		}

		IStatus status = null;
		if (e instanceof CoreException) {
			status = ((CoreException) e).getStatus();
		} else {
			String message = e.getMessage();
			if (message == null)
				message = e.toString();
			status = new Status(IStatus.ERROR, getPluginId(), IStatus.OK, message, e);
		}
		log(status);
	}

	public static void log(IStatus status) {
		if (status.getSeverity() != IStatus.INFO) {
			UpdateCore.getPlugin().getLog().log(status);
		} 
	}

	public static IFeature[] searchSite(String featureId, IConfiguredSite site, boolean onlyConfigured) throws CoreException {
		IFeatureReference[] references = null;

		if (onlyConfigured)
			references = site.getConfiguredFeatures();
		else
			references = site.getSite().getFeatureReferences();
		Vector result = new Vector();

		for (int i = 0; i < references.length; i++) {
			IFeature feature = references[i].getFeature(null);
			String id = feature.getVersionedIdentifier().getIdentifier();
			if (featureId.equals(id)) {
				result.add(feature);
			}
		}
		return (IFeature[]) result.toArray(new IFeature[result.size()]);
	}

	public static IFeature[] getInstalledFeatures(IFeature feature) {
		return getInstalledFeatures(feature, true);
	}

	/**
	 * 
	 * @param feature
	 * @param onlyConfigured
	 * @return IFeature[] with features matching feature ID
	 */
	public static IFeature[] getInstalledFeatures(IFeature feature, boolean onlyConfigured) {
		return getInstalledFeatures(feature.getVersionedIdentifier(), onlyConfigured);
	}
	/**
	 * @param vid
	 * @param onlyConfigured
	 * @return IFeature[] with features matching feature ID
	 */
	public static IFeature[] getInstalledFeatures(VersionedIdentifier vid, boolean onlyConfigured) {
		Vector features = new Vector();
		try {
			ILocalSite localSite = SiteManager.getLocalSite();
			IInstallConfiguration config = localSite.getCurrentConfiguration();
			IConfiguredSite[] isites = config.getConfiguredSites();
			String id = vid.getIdentifier();

			for (int i = 0; i < isites.length; i++) {
				IConfiguredSite isite = isites[i];
				IFeature[] result = UpdateUtils.searchSite(id, isite, onlyConfigured);
				for (int j = 0; j < result.length; j++) {
					IFeature installedFeature = result[j];
					features.add(installedFeature);
				}
			}
		} catch (CoreException e) {
			UpdateUtils.logException(e);
		}
		return (IFeature[]) features.toArray(new IFeature[features.size()]);
	}
	
	/**
	 * @param patch
	 * @return IFeature or null
	 */
	public static IFeature getPatchedFeature(IFeature patch) {
		IImport[] imports = patch.getImports();
		for (int i = 0; i < imports.length; i++) {
			IImport iimport = imports[i];
			if (iimport.isPatch()) {
				VersionedIdentifier patchedVid = iimport
						.getVersionedIdentifier();
				// features with matching id
				IFeature[] features = getInstalledFeatures(patchedVid, false);
				for (int f = 0; f < features.length; f++) {
					// check if version match
					if (patchedVid.equals(features[f].getVersionedIdentifier())) {
						return features[f];
					}
				}
			}
		}
		return null;
	}

	public static boolean isPatch(IFeature candidate) {
		IImport[] imports = candidate.getImports();

		for (int i = 0; i < imports.length; i++) {
			IImport iimport = imports[i];
			if (iimport.isPatch()) return true;
		}
		return false;
	}

	public static boolean isPatch(IFeature target, IFeature candidate) {
		VersionedIdentifier vid = target.getVersionedIdentifier();
		IImport[] imports = candidate.getImports();

		for (int i = 0; i < imports.length; i++) {
			IImport iimport = imports[i];
			if (iimport.isPatch()) {
				VersionedIdentifier ivid = iimport.getVersionedIdentifier();
				if (vid.equals(ivid)) {
					// Bingo.
					return true;
				}
			}
		}
		return false;
	}

	public static IInstallConfiguration getBackupConfigurationFor(IFeature feature) {
		VersionedIdentifier vid = feature.getVersionedIdentifier();
		String key = "@" + vid.getIdentifier() + "_" + vid.getVersion(); //$NON-NLS-1$ //$NON-NLS-2$
		try {
			ILocalSite lsite = SiteManager.getLocalSite();
			IInstallConfiguration[] configs = lsite.getPreservedConfigurations();
			for (int i = 0; i < configs.length; i++) {
				IInstallConfiguration config = configs[i];
				if (config.getLabel().startsWith(key))
					return config;
			}
		} catch (CoreException e) {
		}
		return null;
	}
	
	
	public static boolean hasLicense(IFeature feature) {
		IURLEntry info = feature.getLicense();
		if (info == null)
			return false;
		String licenseTxt = info.getAnnotation();
		if (licenseTxt == null)
			return false;
		return licenseTxt.trim().length() > 0;
	}
	public static boolean hasOptionalFeatures(IFeature feature) {
		try {
			IIncludedFeatureReference[] irefs = feature.getIncludedFeatureReferences();
			for (int i = 0; i < irefs.length; i++) {
				IIncludedFeatureReference iref = irefs[i];
				if (iref.isOptional())
					return true;
				// see if it has optional children
				IFeature child = getIncludedFeature( feature, iref);
				if (hasOptionalFeatures(child))
					return true;
			}
		} catch (CoreException e) {
		}
		return false;
	}

	public static IFeature getLocalFeature(
		IConfiguredSite csite,
		IFeature feature)
		throws CoreException {
		IFeatureReference[] refs = csite.getConfiguredFeatures();
		for (int i = 0; i < refs.length; i++) {
			IFeatureReference ref = refs[i];
			VersionedIdentifier refVid = ref.getVersionedIdentifier();
			if (feature.getVersionedIdentifier().equals(refVid))
				return ref.getFeature(null);
		}
		return null;
	}
	
	public static IConfiguredSite getConfigSite(
		IFeature feature,
		IInstallConfiguration config)
		throws CoreException {
		IConfiguredSite[] configSites = config.getConfiguredSites();
		for (int i = 0; i < configSites.length; i++) {
			IConfiguredSite site = configSites[i];
			if (site.getSite().equals(feature.getSite())) {
				return site;
			}
		}
		return null;
	}
	
	public static IConfiguredSite getDefaultTargetSite(
		IInstallConfiguration config,
		IInstallFeatureOperation pendingChange) {
		return getDefaultTargetSite(config, pendingChange, true);
	}

	public  static IConfiguredSite getDefaultTargetSite(
		IInstallConfiguration config,
		IInstallFeatureOperation pendingChange,
		boolean checkAffinityFeature) {
		IFeature oldFeature = pendingChange.getOldFeature();
		IFeature newFeature = pendingChange.getFeature();
		if (oldFeature != null) {
			// We should install into the same site as
			// the old feature
			try {
				return getConfigSite(oldFeature, config);
			} catch (CoreException e) {
				logException(e);
				return null;
			}
		}

		// This is a new install. Check if there is 
		// a disabled feature with the same ID
		String newFeatureID =
			newFeature.getVersionedIdentifier().getIdentifier();
		IConfiguredSite sameSite = getSiteWithFeature(config, newFeatureID);
		if (sameSite != null) {
			return sameSite;
		}

		if (checkAffinityFeature) {
			return getAffinitySite(config, newFeature);
		}
		return null;
	}
	
	public static IConfiguredSite getAffinitySite(
		IInstallConfiguration config,
		IFeature newFeature) {
		// check if the affinity feature is installed
		String affinityID = newFeature.getAffinityFeature();
		if (affinityID != null) {
			IConfiguredSite affinitySite =
				getSiteWithFeature(config, affinityID);
			if (affinitySite != null)
				return affinitySite;
		} else {
			// if this is a patch, collocate with the feature
			IFeature patchedFeature = getPatchedFeature(newFeature);
			if (patchedFeature != null)
				return getSiteWithFeature(config, patchedFeature.getVersionedIdentifier().getIdentifier());
		}
		return null;
	}

	public static IConfiguredSite getSiteWithFeature(
		IInstallConfiguration config,
		String featureID) {
		if (featureID == null)
			return null;
		IConfiguredSite[] sites = config.getConfiguredSites();
		for (int i = 0; i < sites.length; i++) {
			IConfiguredSite site = sites[i];
			IFeatureReference[] refs = site.getFeatureReferences();
			for (int j = 0; j < refs.length; j++) {
				IFeatureReference ref = refs[j];
				try {
					IFeature feature = ref.getFeature(null);
					if (featureID
						.equals(
							feature
								.getVersionedIdentifier()
								.getIdentifier())) {
						// found it
						return site;
					}
				} catch (CoreException e) {
					logException(e);
				}
			}
		}
		return null;
	}
	
	public static void collectOldFeatures(
		IFeature feature,
		IConfiguredSite targetSite,
		ArrayList result)
		throws CoreException {
		IIncludedFeatureReference[] included =
			feature.getIncludedFeatureReferences();
		for (int i = 0; i < included.length; i++) {
			IIncludedFeatureReference iref = included[i];

			IFeature ifeature;

			try {
				ifeature = iref.getFeature(null);
			} catch (CoreException e) {
				if (iref.isOptional())
					continue;
				throw e;
			}
			// find other features and unconfigure
			String id = iref.getVersionedIdentifier().getIdentifier();
			IFeature[] sameIds = UpdateUtils.searchSite(id, targetSite, true);
			for (int j = 0; j < sameIds.length; j++) {
				IFeature sameId = sameIds[j];
				// Ignore self.
				if (sameId.equals(ifeature))
					continue;
				result.add(sameId);
			}
			collectOldFeatures(ifeature, targetSite, result);
		}
	}

//
//	public static IInstallConfiguration createInstallConfiguration() throws CoreException{
//		try {
//			ILocalSite localSite = SiteManager.getLocalSite();
//			IInstallConfiguration config =
//				localSite.cloneCurrentConfiguration();
//			config.setLabel(Utilities.format(config.getCreationDate()));
//			return config;
//		} catch (CoreException e) {
//			// Let callers handle logging
//			//logException(e);
//			throw e;
//		}
//	}
	
	public static UpdateSearchRequest createNewUpdatesRequest(IFeature [] features) {
		return createNewUpdatesRequest(features, true);
	}
	
	public static UpdateSearchRequest createNewUpdatesRequest(IFeature [] features, boolean automatic) {
		UpdateSearchScope scope = new UpdateSearchScope();
		scope.setUpdateMapURL(UpdateUtils.getUpdateMapURL());
		UpdatesSearchCategory category = new UpdatesSearchCategory(automatic);
		if (features!=null)
			category.setFeatures(features);
		UpdateSearchRequest searchRequest = new UpdateSearchRequest(category, scope);
		searchRequest.addFilter(new EnvironmentFilter());
		searchRequest.addFilter(new BackLevelFilter());
		return searchRequest;
	}

	public static boolean isNestedChild(IInstallConfiguration config, IFeature feature) {
		IConfiguredSite[] csites = config.getConfiguredSites();
		try {
			for (int i = 0; csites != null && i < csites.length; i++) {
				IFeatureReference[] refs = csites[i].getConfiguredFeatures();
				for (int j = 0; refs != null && j < refs.length; j++) {
					IFeature parent = refs[j].getFeature(null);
					IFeatureReference[] children =
						parent.getIncludedFeatureReferences();
					for (int k = 0;
						children != null && k < children.length;
						k++) {
						IFeature child = children[k].getFeature(null);
						if (feature.equals(child))
							return true;
					}
				}
			}
		} catch (CoreException e) {
			// will return false
		}
		return false;
	}
	
	
	public static boolean hasObsoletePatches(IFeature feature) {
		// Check all the included features that
		// are unconfigured, and see if their patch 
		// references are better than the original.
		try {
			IFeatureReference[] irefs = feature.getIncludedFeatureReferences();
			for (int i = 0; i < irefs.length; i++) {
				IFeatureReference iref = irefs[i];
				IFeature ifeature = iref.getFeature(null);
				IConfiguredSite csite = ifeature.getSite().getCurrentConfiguredSite();
				if (!csite.isConfigured(ifeature)) {
					if (!isPatchHappy(ifeature))
						return false;
				}
			}
		} catch (CoreException e) {
			return false;
		}
		// All checks went well
		return true;
	}
	
	public static boolean isPatchHappy(IFeature feature) throws CoreException {
		// If this is a patch and it includes 
		// another patch and the included patch
		// is disabled but the feature it was declared
		// to patch is now newer (and is presumed to
		// contain the now disabled patch), and
		// the newer patched feature is enabled,
		// a 'leap of faith' assumption can be
		// made:

		// Although the included patch is disabled,
		// the feature it was designed to patch
		// is now newer and most likely contains
		// the equivalent fix and more. Consequently,
		// we can claim that the status and the error
		// icon overlay are misleading because
		// all the right plug-ins are configured.
		IImport[] imports = feature.getImports();
		IImport patchReference = null;
		for (int i = 0; i < imports.length; i++) {
			IImport iimport = imports[i];
			if (iimport.isPatch()) {
				patchReference = iimport;
				break;
			}
		}
		if (patchReference == null)
			return false;
		VersionedIdentifier refVid = patchReference.getVersionedIdentifier();

		// Find the patched feature and 
		IConfiguredSite csite = feature.getSite().getCurrentConfiguredSite();
		if (csite == null)
			return false;

		IFeatureReference[] crefs = csite.getConfiguredFeatures();
		for (int i = 0; i < crefs.length; i++) {
			IFeatureReference cref = crefs[i];
			VersionedIdentifier cvid = cref.getVersionedIdentifier();
			if (cvid.getIdentifier().equals(refVid.getIdentifier())) {
				// This is the one.
				if (cvid.getVersion().isGreaterThan(refVid.getVersion())) {
					// Bingo: we found the referenced feature
					// and its version is greater - 
					// we can assume that it contains better code
					// than the patch that referenced the
					// older version.
					return true;
				}
			}
		}
		return false;
	}

	public static URL getUpdateMapURL() {
		Preferences pref = UpdateCore.getPlugin().getPluginPreferences();
		String mapFile = pref.getString(UpdateUtils.P_UPDATE_POLICY_URL);
		if (mapFile!=null && mapFile.length()>0) {
			try {
				URL url = new URL(mapFile);
				URL resolvedURL = URLEncoder.encode(url);
				return resolvedURL;
			}
			catch (MalformedURLException e) {
				UpdateUtils.logException(e);
			}
		}
		return null;
	}
	
	/*
	 * Load the update map using the map URL found in the scope.
	 */	
	public static IStatus loadUpdatePolicy(UpdatePolicy map, URL url, IProgressMonitor monitor) throws CoreException {
		monitor.subTask(Messages.UpdateSearchRequest_loadingPolicy); 
		try {
			map.load(url, monitor);
			monitor.worked(1);
		}
		catch (CoreException e) {
			IStatus status = e.getStatus();
			if (status == null
				|| status.getCode() != ISite.SITE_ACCESS_EXCEPTION)
				throw e;
			monitor.worked(1);
			return status;
		}
		return null;
	}

	public static void downloadFeatureContent(
        IConfiguredSite targetSite,
		IFeature feature,
		IFeatureReference[] optionalChildren, // null when feature has no optional features
		IProgressMonitor progress)
		throws InstallAbortedException, CoreException {
		
		//DEBUG
		if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_INSTALL) {
			UpdateCore.debug(
				"Downloading...:" + feature.getURL().toExternalForm()); //$NON-NLS-1$
		}

		// Get source feature provider and verifier.
		// Initialize target variables.
		final IFeatureContentProvider provider =
			feature.getFeatureContentProvider();
		IPluginEntry[] targetSitePluginEntries = null;

		// determine list of plugins to install
		// find the intersection between the plugin entries already contained
		// on the target site, and plugin entries packaged in source feature
		IPluginEntry[] sourceFeaturePluginEntries = feature.getPluginEntries();

        boolean featureAlreadyInstalled = false;
        if (targetSite == null)
            targetSite = getSiteWithFeature(SiteManager.getLocalSite()
                    .getCurrentConfiguration(), ((Feature) feature)
                    .getFeatureIdentifier());
		if (targetSite == null) {
			if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_INSTALL) {
				UpdateCore.debug("The site to install in is null"); //$NON-NLS-1$
			}

			targetSitePluginEntries = new IPluginEntry[0];
		} else {
			targetSitePluginEntries = targetSite.getSite().getPluginEntries();
            featureAlreadyInstalled = UpdateUtils.getLocalFeature(targetSite,feature) != null;
		}
		IPluginEntry[] pluginsToInstall =
			UpdateManagerUtils.diff(
				sourceFeaturePluginEntries,
				targetSitePluginEntries);
		INonPluginEntry[] nonPluginsToInstall = feature.getNonPluginEntries();
		
		IFeatureReference[] children = feature.getIncludedFeatureReferences();
		if (optionalChildren != null) {
			children =
				UpdateManagerUtils.optionalChildrenToInstall(
					children,
					optionalChildren);
		}

		// make sure we have an InstallMonitor		
		InstallMonitor monitor;
		if (progress == null)
			monitor = new InstallMonitor(new NullProgressMonitor());
		else if (progress instanceof InstallMonitor)
			monitor = (InstallMonitor) progress;
		else
			monitor = new InstallMonitor(progress);

		try {
			// determine number of monitor tasks
			//   1 task1 for the feature jar (download)
			// + n tasks for plugin entries (download for each)
			// + m tasks per non-plugin data entry (download for each)
			// TODO custom install handler  + 1 task for custom non-plugin entry handling (1 for all combined)
			// + 3*x tasks for children features (3 subtasks per install)
			int taskCount =
					1
					+ pluginsToInstall.length
					+ nonPluginsToInstall.length
//				+ 1
					+ 3 * children.length;
			monitor.beginTask("", taskCount); //$NON-NLS-1$
			
			// Download feature archive(s)
			provider.getFeatureEntryArchiveReferences(monitor);
			monitorWork(monitor,1);
			
			// Download plugin archives
			for (int i = 0; i < pluginsToInstall.length; i++) {
				provider.getPluginEntryArchiveReferences(pluginsToInstall[i], monitor);
				monitorWork(monitor,1);
			}
			
			

			// Download non-plugin archives. Verification handled by optional install handler
            // Note: do not download non-plugin archives for installed features
            if (nonPluginsToInstall.length > 0) {
                // Setup optional install handler
                InstallHandlerProxy handler = null;
                if (feature.getInstallHandlerEntry()!=null)
                    handler = new InstallHandlerProxy(
                        IInstallHandler.HANDLER_ACTION_INSTALL,
                        feature,
                        feature.getInstallHandlerEntry(),
                        monitor);
            	
                if (!featureAlreadyInstalled)
                    for (int i = 0; i < nonPluginsToInstall.length; i++) {
                        if (handler==null || handler.acceptNonPluginData(nonPluginsToInstall[i]))
                        	provider.getNonPluginEntryArchiveReferences(
                                nonPluginsToInstall[i], monitor);
                        monitorWork(monitor, 1);
                    }
                else
                    monitorWork(monitor, nonPluginsToInstall.length);
            }
           
			// Download child features
			for (int i = 0; i < children.length; i++) {
				IFeature childFeature = null;
				try {
					childFeature = children[i].getFeature(null);
				} catch (CoreException e) {
					UpdateCore.warn(null, e);
				}
				if (childFeature != null) {
					SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 3);
					downloadFeatureContent(targetSite, childFeature, optionalChildren, subMonitor);
				}
			}
		} finally {
			if (monitor != null)
				monitor.done();
		}
	}
	
	private static void monitorWork(IProgressMonitor monitor, int tick)
	throws CoreException {
	if (monitor != null) {
		monitor.worked(tick);
		if (monitor.isCanceled()) {
			String msg = "download cancelled";//Policy.bind("Feature.InstallationCancelled"); //$NON-NLS-1$
			throw new InstallAbortedException(msg, null);
		}
	}
}
	public static IFeature getIncludedFeature(IFeature feature, IFeatureReference iref) throws CoreException {
		IFeature ifeature = null;
		if (feature.getSite() instanceof ExtendedSite) {
			ifeature = ((ExtendedSite)feature.getSite()).getLiteFeature(iref.getVersionedIdentifier());
		}
		if (ifeature == null) {
			ifeature = iref.getFeature(new NullProgressMonitor());
		}
		return ifeature;
	}
}
