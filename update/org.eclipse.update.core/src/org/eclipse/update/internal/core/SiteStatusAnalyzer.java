package org.eclipse.update.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved. 
 */
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.model.PluginDescriptorModel;
import org.eclipse.core.runtime.model.PluginFragmentModel;
import org.eclipse.update.configuration.IConfiguredSite;
import org.eclipse.update.core.*;

/**
 * This class manages the configurations.
 */

public class SiteStatusAnalyzer {

	//
	private static List allRunningPlugins; /*VersionedIdentifier */
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
	/**
	* compare two feature references
	* returns 0 if the feature are different
	* returns 1 if the version of feature 1 is greater than version of feature 2
	* returns 2 if opposite
	*/
	private int compare(IFeatureReference featureRef1, IFeatureReference featureRef2) throws CoreException {
		if (featureRef1 == null)
			return 0;

		IFeature feature1 = null;
		IFeature feature2 = null;
		try {
			feature1 = featureRef1.getFeature();
			feature2 = featureRef2.getFeature();
		} catch (CoreException e) {
			UpdateManagerPlugin.warn(null, e);
			return 0;
		}

		if (feature1 == null || feature2 == null) {
			return 0;
		}

		VersionedIdentifier id1 = feature1.getVersionedIdentifier();
		VersionedIdentifier id2 = feature2.getVersionedIdentifier();

		if (id1 == null || id2 == null) {
			return 0;
		}

		if (id1.getIdentifier() != null && id1.getIdentifier().equals(id2.getIdentifier())) {
			PluginVersionIdentifier version1 = id1.getVersion();
			PluginVersionIdentifier version2 = id2.getVersion();
			if (version1 != null) {
				boolean greaterOrEqual = (version1.isGreaterOrEqualTo(version2));
				if (greaterOrEqual) {
					return 1;
				} else {
					return 2;
				}
			} else {
				return 2;
			}
		}
		return 0;
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
			if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_CONFIGURATION)
				UpdateManagerPlugin.debug("Cannot determine status of feature:" + feature.getLabel() + ". Site is NULL.");
			String msg = Policy.bind("SiteLocal.UnableToDetermineFeatureStatusSiteNull", new Object[] { feature.getURL()});
			return createStatus(IStatus.ERROR, IFeature.STATUS_AMBIGUOUS, msg, null);
		}

		// validate configured site		
		ConfiguredSite cSite = (ConfiguredSite) featureSite.getConfiguredSite();
		if (cSite == null) {
			if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_CONFIGURATION)
				UpdateManagerPlugin.warn("Cannot determine status of feature: " + feature.getLabel() + ". Configured Site is NULL.");
			String msg = Policy.bind("SiteLocal.UnableToDetermineFeatureStatusConfiguredSiteNull", new Object[] { feature.getURL()});
			return createStatus(IStatus.ERROR, IFeature.STATUS_AMBIGUOUS, msg, null);
		}

		// check if disable, if so return
		IFeatureReference ref = cSite.getSite().getFeatureReference(feature);
		if (ref != null) {
			if (!cSite.getConfigurationPolicy().isConfigured(ref))
				return createStatus(IStatus.OK, IFeature.STATUS_DISABLED, "", null);
		} else {
			if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_CONFIGURATION)
				UpdateManagerPlugin.warn("Unable to find reference for feature " + feature + " in site " + cSite.getSite().getURL());
		}

		// check if broken
		IStatus status = cSite.getBrokenStatus(feature);
		if (status.getSeverity() != IStatus.OK) {
			if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_CONFIGURATION)
				UpdateManagerPlugin.debug("Feature broken:" + feature.getLabel() + ".Site:" + cSite.toString());
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
				if (!children[i].isOptional()) {
					try {
						childFeature = children[i].getFeature();
					} catch (CoreException e) {
						if (!children[i].isOptional())
							UpdateManagerPlugin.warn("Error retrieving feature:" + children[i]);
					}

					if (childFeature == null) {
						UpdateManagerPlugin.warn("getFeatureStatus: Feature is null for:" + children[i]);
						// Unable to find children feature, broken
						Object featureAsPrintableObject = children[i].getURL();
						try {
							featureAsPrintableObject = children[i].getVersionedIdentifier();
						} catch (CoreException e){}//may fail
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
		VersionedIdentifier featureID;
		VersionedIdentifier compareID;
		String pluginName;

		String happyMSG = Policy.bind("SiteLocal.FeatureHappy");
		String ambiguousMSG = Policy.bind("SiteLocal.FeatureAmbiguous");
		IStatus featureStatus = createStatus(IStatus.OK, IFeature.STATUS_HAPPY, "", null);
		MultiStatus multi = new MultiStatus(featureStatus.getPlugin(), IFeature.STATUS_AMBIGUOUS, ambiguousMSG, null);

		PluginIdentifier[] ids = getAllRunningPlugins();

		// is Ambigous if we find a plugin from the feature
		// with a different version and not the one we are looking
		for (int i = 0; i < featurePlugins.length; i++) {
			MultiStatus tempmulti = new MultiStatus(featureStatus.getPlugin(), IFeature.STATUS_AMBIGUOUS, ambiguousMSG, null);
			featureID = featurePlugins[i].getVersionedIdentifier();
			boolean found = false;
			for (int k = 0; k < ids.length && !found; k++) {
				compareID = ids[k].getVersionedIdentifier();
				pluginName = null;
				if (featureID.getIdentifier().equals(compareID.getIdentifier())) {
					pluginName = ids[k].getLabel();
					if (featureID.getVersion().isPerfect(compareID.getVersion())) {
						found = true;
					} else {
						// there is a plugin with a different version on the path
						// log it
						IFeature feature = getFeatureForId(compareID);

						String msg = null;
						if (feature == null) {
							Object[] values = new Object[] { pluginName, featureID.getVersion(), compareID.getVersion()};
							msg = Policy.bind("SiteLocal.TwoVersionSamePlugin1", values);
						} else {
							String label = feature.getLabel();
							String version = feature.getVersionedIdentifier().getVersion().toString();
							Object[] values = new Object[] { pluginName, featureID.getVersion(), compareID.getVersion(), label, version };
							msg = Policy.bind("SiteLocal.TwoVersionSamePlugin2", values);
						}

						UpdateManagerPlugin.warn("Found another version of the same plugin on the path:" + compareID.toString());
						tempmulti.add(createStatus(IStatus.ERROR, IFeature.STATUS_AMBIGUOUS, msg, null));
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
						String unhappyMSG = Policy.bind("SiteLocal.FeatureUnHappy");
						MultiStatus newMulti = new MultiStatus(featureStatus.getPlugin(), IFeature.STATUS_UNHAPPY, unhappyMSG, null);
						newMulti.addAll(multi);
						multi = newMulti;
					}
					String msg = Policy.bind("SiteLocal.NoPluginVersion", featureID.getIdentifier());
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
		String id = UpdateManagerPlugin.getPlugin().getDescriptor().getUniqueIdentifier();

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
	 * returns all the configured plugins from the registry
	 */
	private PluginIdentifier[] getAllRunningPlugins() {
		if (allRunningPlugins == null) {
			PluginIdentifier pluginIdentifier;
			allRunningPlugins = new ArrayList();
			IPluginRegistry reg = Platform.getPluginRegistry();
			IPluginDescriptor[] desc = reg.getPluginDescriptors();
			for (int i = 0; i < desc.length; i++) {
				String id = desc[i].getUniqueIdentifier();
				String ver = desc[i].getVersionIdentifier().toString();
				VersionedIdentifier versionID = new VersionedIdentifier(id, ver);
				pluginIdentifier = new PluginIdentifier(versionID, desc[i].getLabel(), false);
				allRunningPlugins.add(pluginIdentifier);
				// check fragments
				if (desc[i] instanceof PluginDescriptorModel) {
					PluginDescriptorModel descModel = (PluginDescriptorModel) desc[i];
					PluginFragmentModel[] frags = descModel.getFragments();
					if (frags != null) {
						for (int j = 0; j < frags.length; j++) {
							String fragID = frags[j].getId();
							String fragVER = frags[j].getVersion();
							VersionedIdentifier fragVersionID = new VersionedIdentifier(fragID, fragVER);
							pluginIdentifier = new PluginIdentifier(fragVersionID, frags[j].getName(), true);
							allRunningPlugins.add(pluginIdentifier);
						}
					}
				}
			}
		}

		PluginIdentifier[] ids = new PluginIdentifier[allRunningPlugins.size()];
		if (allRunningPlugins.size() > 0) {
			allRunningPlugins.toArray(ids);
		}
		return ids;
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
						feature = refs[j].getFeature();
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