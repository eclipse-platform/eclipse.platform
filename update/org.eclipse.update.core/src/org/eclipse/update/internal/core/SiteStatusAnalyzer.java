/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.update.configuration.IConfiguredSite;
import org.eclipse.update.configurator.ConfiguratorUtils;
import org.eclipse.update.configurator.IPlatformConfiguration;
import org.eclipse.update.core.IFeature;
import org.eclipse.update.core.IFeatureReference;
import org.eclipse.update.core.IImport;
import org.eclipse.update.core.IPluginEntry;
import org.eclipse.update.core.ISite;
import org.eclipse.update.core.VersionedIdentifier;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.packageadmin.PackageAdmin;

/**
 * This class manages the configurations.
 */

public class SiteStatusAnalyzer {

	private static final String SOURCE_BUNDLES_PATH = "org.eclipse.equinox.source/source.info"; //$NON-NLS-1$
	private static final String ID = "org.eclipse.update.core"; //$NON-NLS-1$
	private static List allConfiguredFeatures; /*VersionedIdentifier */
	private LocalSite siteLocal;

	// A list of versionedIdentifiers for source bundles; initialized on demand.
	private List sourceBundles = null;

	/**
	 * 
	 */
	public SiteStatusAnalyzer(LocalSite siteLocal) {
		this.siteLocal = siteLocal;
	}

	/*
	 *  check if the Plugins of the feature are on the plugin path
	 *  If all the plugins are on the plugin path, and the version match and there is no other version -> HAPPY
	 *  If all the plugins are on the plugin path, and the version match and there is other version -> AMBIGUOUS
	 *  If some of the plugins are on the plugin path, but not all -> UNHAPPY
	 * 	Check on all ConfiguredSites
	 */
	private IStatus getStatus(IFeature feature) {

		// validate site
		ISite featureSite = feature.getSite();
		if (featureSite == null) {
			if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_CONFIGURATION)
				UpdateCore.debug("Cannot determine status of feature:" + feature.getLabel() + ". Site is NULL."); //$NON-NLS-1$ //$NON-NLS-2$
			String msg = NLS.bind(Messages.SiteLocal_UnableToDetermineFeatureStatusSiteNull, (new Object[] {feature.getURL()}));
			return createStatus(IStatus.ERROR, IFeature.STATUS_AMBIGUOUS, msg, null);
		}

		// validate configured site		
		ConfiguredSite cSite = (ConfiguredSite) featureSite.getCurrentConfiguredSite();
		if (cSite == null) {
			if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_CONFIGURATION)
				UpdateCore.warn("Cannot determine status of feature: " + feature.getLabel() + ". Configured Site is NULL."); //$NON-NLS-1$ //$NON-NLS-2$
			String msg = NLS.bind(Messages.SiteLocal_UnableToDetermineFeatureStatusConfiguredSiteNull, (new Object[] {feature.getURL()}));
			return createStatus(IStatus.ERROR, IFeature.STATUS_AMBIGUOUS, msg, null);
		}

		// check if disable, if so return
		IFeatureReference ref = cSite.getSite().getFeatureReference(feature);
		if (ref != null) {
			if (!cSite.getConfigurationPolicy().isConfigured(ref))
				return createStatus(IStatus.OK, IFeature.STATUS_DISABLED, "", null); //$NON-NLS-1$
		} else {
			if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_CONFIGURATION)
				UpdateCore.warn("Unable to find reference for feature " + feature + " in site " + cSite.getSite().getURL()); //$NON-NLS-1$ //$NON-NLS-2$
		}

		// check if broken
		IStatus status = cSite.getBrokenStatus(feature);
		if (status.getSeverity() != IStatus.OK) {
			if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_CONFIGURATION)
				UpdateCore.debug("Feature broken:" + feature.getLabel() + ".Site:" + cSite.toString()); //$NON-NLS-1$ //$NON-NLS-2$
			return status;
		}

		// check ambiguous against registry [17015]
		IPluginEntry[] featuresEntries = feature.getPluginEntries();
		return status(feature, featuresEntries);
	}

	/*
	 *  check if the Plugins of the feature are on the plugin path
	 *  If all the plugins are on the plugin path, and the version match and there is no other version -> HAPPY
	 *  If all the plugins are on the plugin path, and the version match and there is other version -> AMBIGUOUS
	 *  If some of the plugins are on the plugin path, but not all -> UNHAPPY
	 * 	Check on all ConfiguredSites
	 */
	public IStatus getFeatureStatus(IFeature feature) throws CoreException {

		IFeature childFeature = null;
		IStatus childStatus;

		IFeatureReference[] children = feature.getIncludedFeatureReferences();

		// consider disable
		// check the current feature
		String msg = Messages.SiteLocal_FeatureDisable;
		int code = IFeature.STATUS_DISABLED;
		IStatus featureStatus = getStatus(feature);
		MultiStatus multiTemp = new MultiStatus(featureStatus.getPlugin(), code, msg, null);
		if (featureStatus.getSeverity() == IStatus.ERROR) {
			if (featureStatus.isMultiStatus()) {
				multiTemp.addAll(featureStatus);
			} else {
				multiTemp.add(featureStatus);
			}
		}
		// preserve the worse code through the method (self assesment + children assessment)
		if (featureStatus.getCode() > code)
			code = featureStatus.getCode();

		// do not check children if feature is disable
		if (!(code == IFeature.STATUS_DISABLED)) {
			for (int i = 0; i < children.length; i++) {
				if (!UpdateManagerUtils.isOptional(children[i])) {
					try {
						childFeature = children[i].getFeature(null);
					} catch (CoreException e) {
						childFeature = null;
						if (!UpdateManagerUtils.isOptional(children[i]))
							UpdateCore.warn("Error retrieving feature:" + children[i]); //$NON-NLS-1$
					}

					if (childFeature == null) {
						UpdateCore.warn("getFeatureStatus: Feature is null for:" + children[i]); //$NON-NLS-1$
						// Unable to find children feature, broken
						Object featureAsPrintableObject = children[i].getURL();
						featureAsPrintableObject = children[i].getVersionedIdentifier();
						String msg1 = NLS.bind(Messages.SiteLocal_NestedFeatureUnavailable, (new Object[] {featureAsPrintableObject}));
						multiTemp.add(createStatus(IStatus.ERROR, IFeature.STATUS_UNHAPPY, msg1, null));
						if (IFeature.STATUS_UNHAPPY > code)
							code = IFeature.STATUS_UNHAPPY;
					} else {
						childStatus = getFeatureStatus(childFeature);
						// do not add the status, add the children status as getFeatureStatus
						// returns a multiStatus 
						if (childStatus.getCode() == IFeature.STATUS_DISABLED) {
							VersionedIdentifier versionID = childFeature.getVersionedIdentifier();
							String featureVer = (versionID == null) ? "" : versionID.getVersion().toString(); //$NON-NLS-1$
							String msg1 = NLS.bind(Messages.SiteLocal_NestedFeatureDisable, (new String[] {childFeature.getLabel(), featureVer}));
							multiTemp.add(createStatus(IStatus.ERROR, childStatus.getCode(), msg1, null));
							if (IFeature.STATUS_UNHAPPY > code)
								code = IFeature.STATUS_UNHAPPY;
						}
						if (childStatus.getSeverity() != IStatus.OK) {
							VersionedIdentifier versionID = childFeature.getVersionedIdentifier();
							String featureVer = (versionID == null) ? "" : versionID.getVersion().toString(); //$NON-NLS-1$
							String msg1 = NLS.bind(Messages.SiteLocal_NestedFeatureUnHappy, (new String[] {childFeature.getLabel(), featureVer}));
							multiTemp.add(createStatus(IStatus.ERROR, childStatus.getCode(), msg1, null));
							if (childStatus.getCode() > code)
								code = childStatus.getCode();
						}
					}
				}
			}
		}

		// set message
		switch (code) {
			case IFeature.STATUS_HAPPY :
				msg = Messages.SiteLocal_FeatureHappy;
				break;
			case IFeature.STATUS_UNHAPPY :
				msg = Messages.SiteLocal_FeatureUnHappy;
				break;
			case IFeature.STATUS_AMBIGUOUS :
				msg = Messages.SiteLocal_FeatureAmbiguous;
				break;
			case IFeature.STATUS_DISABLED :
				msg = Messages.SiteLocal_FeatureDisable;
				break;
			default :
				msg = Messages.SiteLocal_FeatureStatusUnknown;
				break;
		}
		MultiStatus multi = new MultiStatus(featureStatus.getPlugin(), code, msg, null);
		multi.addAll(multiTemp);
		return multi;
	}

	/*
	 * compute the status based on getStatus() rules 
	 */
	private IStatus status(IFeature pluginsOriginatorFeature, IPluginEntry[] featurePlugins) {
		VersionedIdentifier featurePluginID;

		String happyMSG = Messages.SiteLocal_FeatureHappy;
		String ambiguousMSG = Messages.SiteLocal_FeatureAmbiguous;
		IStatus featureStatus = createStatus(IStatus.OK, IFeature.STATUS_HAPPY, "", null); //$NON-NLS-1$
		MultiStatus multi = new MultiStatus(featureStatus.getPlugin(), IFeature.STATUS_AMBIGUOUS, ambiguousMSG, null);
		PackageAdmin pkgAdmin = UpdateCore.getPlugin().getPackageAdmin();

		// is Ambigous if we find a plugin from the feature
		// with a different version and not the one we are looking
		for (int i = 0; i < featurePlugins.length; i++) {
			MultiStatus tempmulti = new MultiStatus(featureStatus.getPlugin(), IFeature.STATUS_AMBIGUOUS, ambiguousMSG, null);
			featurePluginID = featurePlugins[i].getVersionedIdentifier();
			boolean found = false;

			String singleVersionRange = '[' + featurePluginID.getVersion().toString() + ',' + featurePluginID.getVersion().toString() + ']';
			Bundle[] bundles = pkgAdmin.getBundles(featurePluginID.getIdentifier(), singleVersionRange);
			if (bundles != null && bundles.length == 1) {
				found = true;
				continue;
			}

			// Check if there is another feature with this plugin (but different version)
			// log it
			bundles = pkgAdmin.getBundles(featurePluginID.getIdentifier(), null);
			for (int j = 0; bundles != null && j < bundles.length && !found; j++) {
				String bundleVersion = (String) bundles[j].getHeaders().get(Constants.BUNDLE_VERSION);
				IFeature feature = getFeatureForId(new VersionedIdentifier(bundles[j].getSymbolicName(), bundleVersion));
				if ((feature != null) && (!isFeaturePatchOfThisFeature(pluginsOriginatorFeature, feature))) {
					String msg = null;
					String label = feature.getLabel();
					String featureVersion = feature.getVersionedIdentifier().getVersion().toString();
					Object[] values = new Object[] {bundles[j].getSymbolicName(), featurePluginID.getVersion(), bundleVersion, label, featureVersion};
					msg = NLS.bind(Messages.SiteLocal_TwoVersionSamePlugin2, values);
					UpdateCore.warn("Found another version of the same plugin on the path:" + bundles[j].getSymbolicName() + " " + bundleVersion); //$NON-NLS-1$ //$NON-NLS-2$
					tempmulti.add(createStatus(IStatus.ERROR, IFeature.STATUS_AMBIGUOUS, msg, null));
				} else {
					found = true;
				}

			}

			// check whether the plugin is a source bundle
			// that has not been configured into the runtime
			if (!found) {
				loadSourceBundlesList();
				for (Iterator iter = sourceBundles.iterator(); iter.hasNext();) {
					VersionedIdentifier nextId = (VersionedIdentifier) iter.next();
					if (featurePluginID.equals(nextId)) {
						found = true;
						break;
					}
				}
			}

			// if we haven't found the exact plugin, add the children
			// of tempMulti (i,e the other we found) 
			// if we have no children, we have a problem as a required plugin is not there at all
			if (!found) {
				if (tempmulti.getChildren().length > 0) {
					multi.addAll(tempmulti);
				} else {
					if (multi.getCode() != IFeature.STATUS_UNHAPPY) {
						String unhappyMSG = Messages.SiteLocal_FeatureUnHappy;
						MultiStatus newMulti = new MultiStatus(featureStatus.getPlugin(), IFeature.STATUS_UNHAPPY, unhappyMSG, null);
						newMulti.addAll(multi);
						multi = newMulti;
					}
					String msg = NLS.bind(Messages.SiteLocal_NoPluginVersion, (new String[] {featurePluginID.getIdentifier()}));
					multi.add(createStatus(IStatus.ERROR, IFeature.STATUS_UNHAPPY, msg, null));
				}
			}
		}

		if (!multi.isOK())
			return multi;

		// we return happy as we consider the isBroken verification has been done
		return createStatus(IStatus.OK, IFeature.STATUS_HAPPY, happyMSG, null);
	}

	public static File toFile(URL url) {
		try {
			if (!"file".equalsIgnoreCase(url.getProtocol())) //$NON-NLS-1$
				return null;
			//assume all illegal characters have been properly encoded, so use URI class to unencode
			return new File(new URI(url.toExternalForm()));
		} catch (Exception e) {
			//URL contains unencoded characters
			return new File(url.getFile());
		}
	}

	/**
	 * 	Get the contents of the source bundles text file.
	 */
	private void loadSourceBundlesList() {
		if (sourceBundles != null)
			return;

		sourceBundles = new ArrayList(32);
		IPlatformConfiguration config = ConfiguratorUtils.getCurrentPlatformConfiguration();
		URL configLocation = config.getConfigurationLocation();
		if (configLocation == null)
			return;
		// Drop off /org.eclipse.update/platform.xml
		File configDir = toFile(configLocation);
		configDir = configDir.getParentFile();
		if (configDir == null)
			return;
		configDir = configDir.getParentFile();
		if (configDir == null)
			return;
		File sourceBundlesFile = new File(configDir, SOURCE_BUNDLES_PATH);

		try {
			BufferedReader reader = new BufferedReader(new FileReader(sourceBundlesFile));
			String line;
			try {
				while ((line = reader.readLine()) != null) {
					if (line.startsWith("#"))
						continue;
					line = line.trim();// symbolicName,version,other ignored stuff
					if (line.length() == 0)
						continue;

					StringTokenizer tok = new StringTokenizer(line, ",", true);
					String symbolicName = tok.nextToken();
					if (symbolicName.equals(","))
						continue;
					else
						tok.nextToken(); // ,

					String version = tok.nextToken();
					if (version.equals(","))
						continue;
					else
						tok.nextToken(); // ,

					VersionedIdentifier sourceId = new VersionedIdentifier(symbolicName, version);
					sourceBundles.add(sourceId);
				}
			} finally {
				try {
					reader.close();
				} catch (IOException ex) {
					// ignore
				}
			}
		} catch (MalformedURLException e) {
			UpdateCore.log(new Status(IStatus.ERROR, ID, "Error occurred while reading source bundle list.", e)); //$NON-NLS-1$
		} catch (IOException e) {
			UpdateCore.log(new Status(IStatus.ERROR, ID, "Error occurred while reading source bundle list.", e)); //$NON-NLS-1$
		}
	}

	private boolean isFeaturePatchOfThisFeature(IFeature pluginsOriginatorFeature, IFeature feature) {

		if (!feature.isPatch())
			return false;

		IImport[] featureImports = feature.getImports();

		if (featureImports == null) {
			return false;
		}

		for (int i = 0; i < featureImports.length; i++) {
			if (featureImports[i].isPatch() && featureImports[i].getVersionedIdentifier().equals(pluginsOriginatorFeature.getVersionedIdentifier())) {
				return true;
			}
		}
		return false;
	}

	/*
	 * creates a Status
	 */
	private IStatus createStatus(int statusSeverity, int statusCode, String msg, Exception e) {
		String id = UpdateCore.getPlugin().getBundle().getSymbolicName();

		StringBuffer completeString = new StringBuffer(""); //$NON-NLS-1$
		if (msg != null)
			completeString.append(msg);
		if (e != null) {
			completeString.append("\r\n["); //$NON-NLS-1$
			completeString.append(e.toString());
			completeString.append("]\r\n"); //$NON-NLS-1$
		}
		return new Status(statusSeverity, id, statusCode, completeString.toString(), e);
	}

	/*
	 * returns all the configured fetaures
	 */
	private IFeature[] getAllConfiguredFeatures() {
		if (allConfiguredFeatures == null) {

			allConfiguredFeatures = new ArrayList();
			IConfiguredSite[] allConfiguredSites = siteLocal.getCurrentConfiguration().getConfiguredSites();

			for (int i = 0; i < allConfiguredSites.length; i++) {
				IFeatureReference[] refs = allConfiguredSites[i].getConfiguredFeatures();
				IFeature feature = null;
				for (int j = 0; j < refs.length; j++) {
					feature = null;
					try {
						feature = refs[j].getFeature(null);
					} catch (CoreException e) {
					}
					if (feature != null) {
						allConfiguredFeatures.add(feature);
					}
				}
			}
		}

		IFeature[] features = new IFeature[allConfiguredFeatures.size()];
		if (allConfiguredFeatures.size() > 0) {
			allConfiguredFeatures.toArray(features);
		}
		return features;
	}

	/*
	 * returns the Feature that declares this versionedIdentifier or null if none found
	 */
	private IFeature getFeatureForId(VersionedIdentifier id) {

		if (id == null)
			return null;

		IFeature[] allFeatures = getAllConfiguredFeatures();
		IFeature currentFeature = null;
		IPluginEntry[] allPlugins = null;
		IPluginEntry currentPlugin = null;
		for (int i = 0; i < allFeatures.length; i++) {
			currentFeature = allFeatures[i];
			allPlugins = currentFeature.getPluginEntries();
			for (int j = 0; j < allPlugins.length; j++) {
				currentPlugin = allPlugins[j];
				if (id.equals(currentPlugin.getVersionedIdentifier()))
					return currentFeature;
			}
		}
		return null;
	}
}
