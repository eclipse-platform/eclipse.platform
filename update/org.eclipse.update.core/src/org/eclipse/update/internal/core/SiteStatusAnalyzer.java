/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.core;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.osgi.framework.*;
import org.osgi.service.packageadmin.*;

/**
 * This class manages the configurations.
 */

public class SiteStatusAnalyzer {

	private static List allConfiguredFeatures; /*VersionedIdentifier */
	private SiteLocal siteLocal;

	/**
	 * 
	 */
	public SiteStatusAnalyzer(SiteLocal siteLocal) {
		this.siteLocal = siteLocal;
	}

	/**
	 * manages the versionedIdentifier and location of parsed plugins
	 */
	public class PluginIdentifier {
		private VersionedIdentifier id;
		private String label;
		private boolean isFragment = false;

		public PluginIdentifier(VersionedIdentifier id, String label, boolean fragment) {
			this.id = id;
			this.label = label;
			this.isFragment = fragment;
		}

		public VersionedIdentifier getVersionedIdentifier() {
			return id;
		}

		public boolean isFragment() {
			return isFragment;
		}

		public String getLabel() {
			return label;
		}
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
				UpdateCore.debug("Cannot determine status of feature:" + feature.getLabel() + ". Site is NULL.");
			String msg = Policy.bind("SiteLocal.UnableToDetermineFeatureStatusSiteNull", new Object[] { feature.getURL()});
			return createStatus(IStatus.ERROR, IFeature.STATUS_AMBIGUOUS, msg, null);
		}

		// validate configured site		
		ConfiguredSite cSite = (ConfiguredSite) featureSite.getCurrentConfiguredSite();
		if (cSite == null) {
			if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_CONFIGURATION)
				UpdateCore.warn("Cannot determine status of feature: " + feature.getLabel() + ". Configured Site is NULL.");
			String msg = Policy.bind("SiteLocal.UnableToDetermineFeatureStatusConfiguredSiteNull", new Object[] { feature.getURL()});
			return createStatus(IStatus.ERROR, IFeature.STATUS_AMBIGUOUS, msg, null);
		}

		// check if disable, if so return
		IFeatureReference ref = cSite.getSite().getFeatureReference(feature);
		if (ref != null) {
			if (!cSite.getConfigurationPolicy().isConfigured(ref))
				return createStatus(IStatus.OK, IFeature.STATUS_DISABLED, "", null);
		} else {
			if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_CONFIGURATION)
				UpdateCore.warn("Unable to find reference for feature " + feature + " in site " + cSite.getSite().getURL());
		}

		// check if broken
		IStatus status = cSite.getBrokenStatus(feature);
		if (status.getSeverity() != IStatus.OK) {
			if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_CONFIGURATION)
				UpdateCore.debug("Feature broken:" + feature.getLabel() + ".Site:" + cSite.toString());
			return status;
		}

		// check ambiguous against registry [17015]
		IPluginEntry[] featuresEntries = feature.getPluginEntries();
		return status(featuresEntries);
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
		String msg = Policy.bind("SiteLocal.FeatureDisable");
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
						if (!UpdateManagerUtils.isOptional(children[i]))
							UpdateCore.warn("Error retrieving feature:" + children[i]);
					}

					if (childFeature == null) {
						UpdateCore.warn("getFeatureStatus: Feature is null for:" + children[i]);
						// Unable to find children feature, broken
						Object featureAsPrintableObject = children[i].getURL();
						featureAsPrintableObject = children[i].getVersionedIdentifier();
						String msg1 = Policy.bind("SiteLocal.NestedFeatureUnavailable", new Object[] { featureAsPrintableObject });
						multiTemp.add(createStatus(IStatus.ERROR, IFeature.STATUS_UNHAPPY, msg1, null));
						if (IFeature.STATUS_UNHAPPY > code)
							code = IFeature.STATUS_UNHAPPY;
					} else {
						childStatus = getFeatureStatus(childFeature);
						// do not add the status, add the children status as getFeatureStatus
						// returns a multiStatus 
						if (childStatus.getCode() == IFeature.STATUS_DISABLED) {
							VersionedIdentifier versionID = childFeature.getVersionedIdentifier();
							String featureVer = (versionID == null) ? "" : versionID.getVersion().toString();
							String msg1 = Policy.bind("SiteLocal.NestedFeatureDisable", childFeature.getLabel(), featureVer);
							multiTemp.add(createStatus(IStatus.ERROR, childStatus.getCode(), msg1, null));
							if (IFeature.STATUS_UNHAPPY > code)
								code = IFeature.STATUS_UNHAPPY;
						}
						if (childStatus.getSeverity() != IStatus.OK) {
							VersionedIdentifier versionID = childFeature.getVersionedIdentifier();
							String featureVer = (versionID == null) ? "" : versionID.getVersion().toString();
							String msg1 = Policy.bind("SiteLocal.NestedFeatureUnHappy", childFeature.getLabel(), featureVer);
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
				msg = Policy.bind("SiteLocal.FeatureHappy");
				break;
			case IFeature.STATUS_UNHAPPY :
				msg = Policy.bind("SiteLocal.FeatureUnHappy");
				break;
			case IFeature.STATUS_AMBIGUOUS :
				msg = Policy.bind("SiteLocal.FeatureAmbiguous");
				break;
			case IFeature.STATUS_DISABLED :
				msg = Policy.bind("SiteLocal.FeatureDisable");
				break;
			default :
				msg = Policy.bind("SiteLocal.FeatureStatusUnknown");
				break;
		}
		MultiStatus multi = new MultiStatus(featureStatus.getPlugin(), code, msg, null);
		multi.addAll(multiTemp);
		return multi;
	}

	/*
	 * compute the status based on getStatus() rules 
	 */
	private IStatus status(IPluginEntry[] featurePlugins) {
		VersionedIdentifier featurePluginID;

		String happyMSG = Policy.bind("SiteLocal.FeatureHappy");
		String ambiguousMSG = Policy.bind("SiteLocal.FeatureAmbiguous");
		IStatus featureStatus = createStatus(IStatus.OK, IFeature.STATUS_HAPPY, "", null);
		MultiStatus multi = new MultiStatus(featureStatus.getPlugin(), IFeature.STATUS_AMBIGUOUS, ambiguousMSG, null);
		PackageAdmin pkgAdmin = UpdateCore.getPlugin().getPackageAdmin();
		
		// is Ambigous if we find a plugin from the feature
		// with a different version and not the one we are looking
		for (int i = 0; i < featurePlugins.length; i++) {
			MultiStatus tempmulti = new MultiStatus(featureStatus.getPlugin(), IFeature.STATUS_AMBIGUOUS, ambiguousMSG, null);
			featurePluginID = featurePlugins[i].getVersionedIdentifier();
			boolean found = false;
			
			Bundle[] bundles = pkgAdmin.getBundles(featurePluginID.getIdentifier(), featurePluginID.getVersion().toString(), Constants.VERSION_MATCH_QUALIFIER);
			if (bundles != null && bundles.length == 1) {
				found = true;
				continue;
			}
			
			// Check if there is another feature with this plugin (but different version)
			// log it
			bundles = pkgAdmin.getBundles(featurePluginID.getIdentifier(), null, null);
			for (int j=0; bundles != null && j<bundles.length; j++ ) {
				String bundleVersion = (String)bundles[j].getHeaders().get(Constants.BUNDLE_VERSION);
				IFeature feature = getFeatureForId(new VersionedIdentifier(bundles[j].getSymbolicName(), bundleVersion ));
				String msg = null;
				if (feature == null) {
					Object[] values = new Object[] {bundles[j].getSymbolicName(), featurePluginID.getVersion(), bundleVersion};
					msg = Policy.bind("SiteLocal.TwoVersionSamePlugin1", values);
				} else {
					String label = feature.getLabel();
					String featureVersion = feature.getVersionedIdentifier().getVersion().toString();
					Object[] values = new Object[] { bundles[j].getSymbolicName(), featurePluginID.getVersion(), bundleVersion, label, featureVersion };
					msg = Policy.bind("SiteLocal.TwoVersionSamePlugin2", values);
				}

				UpdateCore.warn("Found another version of the same plugin on the path:" + bundles[j].getSymbolicName() + " " + bundleVersion);
				tempmulti.add(createStatus(IStatus.ERROR, IFeature.STATUS_AMBIGUOUS, msg, null));
			}
	

			// if we haven't found the exact plugin, add the children
			// of tempMulti (i,e the other we found) 
			// if we have no children, we have a problem as a required plugin is not there at all
			if (!found) {
				if (tempmulti.getChildren().length > 0) {
					multi.addAll(tempmulti);
				} else {
					if (multi.getCode() != IFeature.STATUS_UNHAPPY) {
						String unhappyMSG = Policy.bind("SiteLocal.FeatureUnHappy");
						MultiStatus newMulti = new MultiStatus(featureStatus.getPlugin(), IFeature.STATUS_UNHAPPY, unhappyMSG, null);
						newMulti.addAll(multi);
						multi = newMulti;
					}
					String msg = Policy.bind("SiteLocal.NoPluginVersion", featurePluginID.getIdentifier());
					multi.add(createStatus(IStatus.ERROR, IFeature.STATUS_UNHAPPY, msg, null));
				}
			}
		}

		if (!multi.isOK())
			return multi;

		// we return happy as we consider the isBroken verification has been done
		return createStatus(IStatus.OK, IFeature.STATUS_HAPPY, happyMSG, null);
	}
	/*
	 * creates a Status
	 */
	private IStatus createStatus(int statusSeverity, int statusCode, String msg, Exception e) {
		String id = UpdateCore.getPlugin().getBundle().getSymbolicName();

		StringBuffer completeString = new StringBuffer("");
		if (msg != null)
			completeString.append(msg);
		if (e != null) {
			completeString.append("\r\n[");
			completeString.append(e.toString());
			completeString.append("]\r\n");
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
