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
import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.update.configuration.*;
import org.eclipse.update.configurator.*;
import org.eclipse.update.core.*;
import org.eclipse.update.core.model.*;
import org.eclipse.update.internal.model.*;

/**
 * 
 */
public class ConfigurationPolicy extends ConfigurationPolicyModel {

	/**
	 * Constructor for ConfigurationPolicyModel.
	 */
	public ConfigurationPolicy() {
	}

	/**
	 * Copy Constructor for ConfigurationPolicyModel.
	 */
	public ConfigurationPolicy(ConfigurationPolicy configPolicy) {
		super();
		setPolicy(configPolicy.getPolicy());
		setConfiguredFeatureReferences(configPolicy.getConfiguredFeatures());
		setUnconfiguredFeatureReferences(configPolicy.getUnconfiguredFeatures());
		setConfiguredSiteModel(configPolicy.getConfiguredSiteModel());
	}

	/**
	 * @since 2.0
	 */
	private boolean isUnconfigured(IFeatureReference featureReference) {

		if (featureReference == null)
			return false;

		// returns true if the feature is part of the configured list
		IFeatureReference[] refs = getUnconfiguredFeatures();
		for (int i = 0; i < refs.length; i++) {
			if (featureReference.equals(refs[i])) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @since 2.0
	 */
	public boolean isConfigured(IFeatureReference featureReference) {

		if (featureReference == null)
			return false;

		// returns true if the feature is part of the configured list
		IFeatureReference[] refs = getConfiguredFeatures();
		for (int i = 0; i < refs.length; i++) {
			if (featureReference.equals(refs[i])) {
				return true;
			}
		}
		return false;
	}

	/**
	 * adds the feature to the list of features if the policy is USER_INCLUDE
	 */
	public void configure(IFeatureReference featureReference, boolean callInstallHandler, boolean createActivity) throws CoreException {

		if (isConfigured(featureReference)) // already configured
			return;

		if (featureReference == null) {
			UpdateCore.warn("The feature reference to configure is null"); //$NON-NLS-1$
			return;
		}

		IFeature feature = null;
		try {
			feature = featureReference.getFeature(null);
		} catch (CoreException e) {
			if (!UpdateManagerUtils.isOptional(featureReference)) {
				URL url = featureReference.getURL();
				String urlString = (url != null) ? url.toExternalForm() : "<no feature reference url>"; //$NON-NLS-1$
				UpdateCore.warn("Error retrieving feature:" + urlString, e); //$NON-NLS-1$
				return;
			}
		}
		if (feature == null) {
			URL url = featureReference.getURL();
			String urlString = (url != null) ? url.toExternalForm() : "<no feature reference url>"; //$NON-NLS-1$
			UpdateCore.warn("The feature to unconfigure is null: feature reference is:" + urlString); //$NON-NLS-1$
		}

		// Setup optional install handler
		InstallHandlerProxy handler = null;
		if (callInstallHandler && feature.getInstallHandlerEntry() != null)
			handler = new InstallHandlerProxy(IInstallHandler.HANDLER_ACTION_CONFIGURE, feature, feature.getInstallHandlerEntry(), null);
		boolean success = false;
		Throwable originalException = null;

		// do the configure action
		try {
			if (handler != null)
				handler.configureInitiated();

			ConfigurationActivity activity = null;
			if (createActivity) {
				activity = new ConfigurationActivity(IActivity.ACTION_CONFIGURE);
				activity.setLabel(feature.getVersionedIdentifier().toString());
				activity.setDate(new Date());
			}

			addConfiguredFeatureReference((FeatureReferenceModel) featureReference);

			// everything done ok
			if (activity != null) {
				InstallConfiguration installConfig = (InstallConfiguration) SiteManager.getLocalSite().getCurrentConfiguration();
				activity.setStatus(IActivity.STATUS_OK);
				installConfig.addActivity(activity);
			}

			if (handler != null)
				handler.completeConfigure();

			success = true;
		} catch (Throwable t) {
			originalException = t;
		} finally {
			Throwable newException = null;
			try {
				if (handler != null)
					handler.configureCompleted(success);
			} catch (Throwable t) {
				newException = t;
			}
			if (originalException != null) // original exception wins
				throw Utilities.newCoreException(NLS.bind(Messages.InstallHandler_error, (new String[] { feature.getLabel() })), originalException);
			if (newException != null)
				throw Utilities.newCoreException(NLS.bind(Messages.InstallHandler_error, (new String[] { feature.getLabel() })), newException);
		}
	}

	/**
	 * check if the plugins to unconfigure are required by other configured feature and
	 * adds the feature to the list of unconfigured features 
	 */
	public boolean unconfigure(IFeatureReference featureReference, boolean callInstallHandler, boolean createActivity) throws CoreException {

		if (isUnconfigured(featureReference)) {
			UpdateCore.warn("Feature already unconfigured"); //$NON-NLS-1$
			return true;
		}

		if (featureReference == null) {
			UpdateCore.warn("The feature reference to unconfigure is null"); //$NON-NLS-1$
			return false;
		}

		IFeature feature = null;
		try {
			feature = featureReference.getFeature(null);
		} catch (CoreException e) {
			if (!UpdateManagerUtils.isOptional(featureReference)) {
				URL url = featureReference.getURL();
				String urlString = (url != null) ? url.toExternalForm() : "<no feature reference url>"; //$NON-NLS-1$
				UpdateCore.warn("Error retrieving feature:" + urlString, e); //$NON-NLS-1$
				return false;
			}
		}

		if (feature == null) {
			URL url = featureReference.getURL();
			String urlString = (url != null) ? url.toExternalForm() : "<no feature reference url>"; //$NON-NLS-1$
			UpdateCore.warn("The feature to unconfigure is null: feature reference is:" + urlString); //$NON-NLS-1$
			return false;
		}

		// Setup optional install handler
		InstallHandlerProxy handler = null;
		if (callInstallHandler && feature.getInstallHandlerEntry() != null) {
			handler = new InstallHandlerProxy(IInstallHandler.HANDLER_ACTION_UNCONFIGURE, feature, feature.getInstallHandlerEntry(), null);
		}

		boolean success = false;
		Throwable originalException = null;

		// do the unconfigure action
		try {

			ConfigurationActivity activity = null;
			if (createActivity) {
				activity = new ConfigurationActivity(IActivity.ACTION_UNCONFIGURE);
				activity.setLabel(feature.getVersionedIdentifier().toString());
				activity.setDate(new Date());
			}

			InstallConfiguration installConfig = null;

			// only ask for install config is activity created.
			// prevents loops during reconciliation
			if (activity != null)
				installConfig = ((InstallConfiguration) SiteManager.getLocalSite().getCurrentConfiguration());

			// Allow unconfigure if the feature is optional from all the parents
			// or if the feature is mandatory and non of its parent are configured
			// removed, not a core issue (so deep down)
			//if (validateNoConfiguredParents(feature)) {
			if (handler != null)
				handler.unconfigureInitiated();
			addUnconfiguredFeatureReference((FeatureReferenceModel) featureReference);
			if (handler != null)
				handler.completeUnconfigure();

			// everything done ok
			if (activity != null) {
				activity.setStatus(IActivity.STATUS_OK);
				installConfig.addActivity(activity);
			}
			success = true;
			//} else {
			//	if (activity != null) {
			//		activity.setStatus(IActivity.STATUS_NOK);
			//		installConfig.addActivityModel((ConfigurationActivityModel) activity);
			//	}
			//}
		} catch (Throwable t) {
			originalException = t;
		} finally {
			Throwable newException = null;
			try {
				if (handler != null)
					handler.unconfigureCompleted(success);
			} catch (Throwable t) {
				newException = t;
			}
			if (originalException != null) // original exception wins
				throw Utilities.newCoreException(NLS.bind(Messages.InstallHandler_error, (new String[] { feature.getLabel() })), originalException);
			if (newException != null)
				throw Utilities.newCoreException(NLS.bind(Messages.InstallHandler_error, (new String[] { feature.getLabel() })), newException);
		}

		if (!success) {
			URL url = featureReference.getURL();
			String urlString = (url != null) ? url.toExternalForm() : "<no feature reference url>"; //$NON-NLS-1$
			UpdateCore.warn("Unable to unconfigure:" + urlString); //$NON-NLS-1$
		}
		return success;
	}

	/**
	 * Calculates the plugin list for the policy. For "INCLUDE" policy, this
	 * corresponds to the plugins for configured features. For "EXCLUDE"
	 * policy, this corresponds to the plugins for unconfigured features that
	 * are not referenced by any configured features.
	 */
	public String[] getPluginPath(ISite site) throws CoreException {
		// TODO we may need to exclude patched plugins here, but this should be good enough for now
		if (getPolicy() == IPlatformConfiguration.ISitePolicy.MANAGED_ONLY)
			return new String[0];
			
		String[] pluginPaths;
		// Note: Since 3.0M7 we leave patched features configured,
		// and take this into account when computing configured plugins
		// all unconfigured features. Note that patched features are still
		// configured
		IFeatureReference[] unconfiguredFeatures = getUnconfiguredFeatures();
		// all configured features, including patches and patched features
		IFeatureReference[] configuredFeatures = getConfiguredFeatures();
		if (!isEnabled()) {
			if (getPolicy() == IPlatformConfiguration.ISitePolicy.USER_INCLUDE) {
				// disabled site, INCLUDE policy
				pluginPaths = new String[0];
			} else {
				// disabled site, EXCLUDE policy
				pluginPaths = getAllKnownPluginStrings(site,
						configuredFeatures, unconfiguredFeatures);
			}
		} else {
			// PatchedFeatures (may have no patches) with corresponding patches
			PatchedFeature[] patchedFeatures = buildPatchedFeatures(configuredFeatures);
			if (getPolicy() == IPlatformConfiguration.ISitePolicy.USER_INCLUDE) {
				// enabled site, INCLUDE policy
				pluginPaths = getConfiguredPluginStrings(site, patchedFeatures);
			} else {
				// enabled site, EXCLUDE policy - the usual scenario for local
				// site.
				// return all known MINUS configured plugins
				pluginPaths = subtract(getAllKnownPluginStrings(site,
						configuredFeatures, unconfiguredFeatures),
						getConfiguredPluginStrings(site, patchedFeatures));
			}
		}
		//TRACE
		if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_RECONCILER) {
			UpdateCore
					.debug("GetPluginPath for: " //$NON-NLS-1$
							+ ((site == null) ? "<No site>" : site.getURL() //$NON-NLS-1$
									.toString()));
			for (int i = 0; i < pluginPaths.length; i++) {
				UpdateCore.debug("To write:" + pluginPaths[i]); //$NON-NLS-1$
			}
		}
		return pluginPaths;
	}
	
	/**
	 * Obtains PatchedFeatures - non patch features with corresponding patches if any
	 * 
	 * @param features
	 *            array of features to operate with
	 * @return Patches
	 */
	private PatchedFeature[] buildPatchedFeatures(IFeatureReference[] features) {
		// PatchedFeatures by VersionedIdentifier
		Map map = new HashMap();
		// Create a map of features (not patches)
		for (int f = 0; f < features.length; f++) {
			IFeatureReference featureRef = features[f];
			try {
				if(featureRef.isPatch()){
					continue;
				}
				VersionedIdentifier vi = featureRef.getVersionedIdentifier();
				map.put(vi, new PatchedFeature(features[f]));
			} catch (CoreException e) {
				UpdateCore.warn(null, e);
			}
		}
		// attach patches to features
		for (int f = 0; f < features.length; f++) {
			IFeatureReference patchCandidate = features[f];
			try {
				IFeature feature = patchCandidate.getFeature(null);
				IImport[] imports = feature.getImports();
				for (int i = 0; i < imports.length; i++) {
					IImport oneImport = imports[i];
					if (!oneImport.isPatch())
						continue;
					// it is a patch for
					VersionedIdentifier patchedIdentifier =
						oneImport.getVersionedIdentifier();
					PatchedFeature pf=(PatchedFeature) map.get(patchedIdentifier);
					if (pf!=null) {
						pf.addPatch(patchCandidate);
					} else {
						// patched feature not enabled
					}
				}
			} catch (CoreException e) {
				UpdateCore.warn(null, e);
			}
		}
		Collection patchedFeatures=map.values();
		return (PatchedFeature[])patchedFeatures.toArray(new PatchedFeature[patchedFeatures.size()]);
	}
	
	/**
	 * @since 2.0
	 */
	public IFeatureReference[] getConfiguredFeatures() {
		FeatureReferenceModel[] result = getConfiguredFeaturesModel();
		if (result.length == 0)
			return new IFeatureReference[0];
		else
			return (IFeatureReference[]) result;
	}

	/**
	 * @since 2.0
	 */
	public IFeatureReference[] getUnconfiguredFeatures() {
		FeatureReferenceModel[] result = getUnconfiguredFeaturesModel();
		if (result.length == 0)
			return new IFeatureReference[0];
		else
			return (IFeatureReference[]) result;
	}

	/**
	 * Gets the configuredSite.
	 * @return Returns a IConfiguredSite
	 */
	public IConfiguredSite getConfiguredSite() {
		return (IConfiguredSite) getConfiguredSiteModel();
	}

	/**
	 * removes a feature reference
	 */
	public void removeFeatureReference(IFeatureReference featureRef) {
		if (featureRef instanceof FeatureReferenceModel) {
			removeFeatureReference((FeatureReferenceModel) featureRef);
		}
	}

	/**
	 * @return an array of plugin path for the array of feature reference. For
	 *         features that have patches, plugin path will
	 *         point to plugin with the same ID provided by the patch if it
	 *         exists. Each plugin path only appears once [bug 21750]
	 */
	private String[] getConfiguredPluginStrings(ISite site, PatchedFeature[] features) throws CoreException {
		if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_CONFIGURATION){
			UpdateCore.warn("CONFIGURED PLUGINS"); //$NON-NLS-1$
		}
	
		// Use set to eliminate plugins with same ID and version.
		// Different versions of plugins with same ID are allowed if coming from different features
		Set featurePlugins = new HashSet();
		for (int i = 0; i < features.length; i++) {
			FeaturePlugin[] plugins = features[i].getPlugins();
			featurePlugins.addAll(Arrays.asList(plugins));
		}
		Set pluginStrings = getPluginStrings(site, (FeaturePlugin[]) featurePlugins.toArray(new FeaturePlugin[featurePlugins.size()]));
		return (String[]) pluginStrings.toArray(new String[pluginStrings.size()]);
	}
	/**
	 * @return an array of plugin path for every plugin in known features
	 */
	private String[] getAllKnownPluginStrings(ISite site, IFeatureReference[] configured,IFeatureReference[] unconfigured) throws CoreException {
		if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_CONFIGURATION){
			UpdateCore.warn("ALL PLUGINS"); //$NON-NLS-1$
		}
		// Add features, patched features, or patches
		IFeatureReference[] all=new IFeatureReference[configured.length+unconfigured.length];
		System.arraycopy(configured, 0, all, 0, configured.length);
		System.arraycopy(unconfigured, 0, all, configured.length, unconfigured.length);
		//
		Set patchedPlugins = new HashSet();
		for (int i=0; i< all.length; i++) {
			try {
				IFeature feature = all[i].getFeature(null);
				if (feature == null) {
					UpdateCore.warn("Null Feature", new Exception()); //$NON-NLS-1$
					continue;
				}

				IPluginEntry[] entries = feature.getPluginEntries();
				// add every plugin to the map
				for (int entr = 0; entr < entries.length; entr++) {
					patchedPlugins.add(new FeaturePlugin(entries[entr], feature));
				}

			} catch (CoreException e) {
				UpdateCore.warn(null, e);
			}
		}
		Set pluginStrings = getPluginStrings(site,  (FeaturePlugin[])patchedPlugins.toArray(new FeaturePlugin[patchedPlugins.size()]));
		return (String[]) pluginStrings.toArray(new String[pluginStrings.size()]);
	}
	/**
	 * @param site
	 * @param plugins[]
	 * @return valid string pointing to plugins in given features
	 * @throws CoreException
	 */
	private Set getPluginStrings(ISite site, FeaturePlugin[] plugins) throws CoreException {
		Set pluginStrings=new HashSet();
		for (int i=0; i< plugins.length; i++) {
			IPluginEntry entry = plugins[i].getEntry();
			IFeature feature=plugins[i].getFeature();

			// obtain the path of the plugin directories on the site
			ContentReference[] featureContentReference = null;
			try {
				featureContentReference = feature.getFeatureContentProvider().getPluginEntryArchiveReferences(entry, null /*IProgressMonitor*/
				);
			} catch (CoreException e) {
				UpdateCore.warn(null, e);
			}

			// transform into a valid String
			if (featureContentReference != null) {
				for (int j = 0; j < featureContentReference.length; j++) {
					URL url = site.getSiteContentProvider().getArchiveReference(featureContentReference[j].getIdentifier());
					if (url != null) {
						// make it relative to the site
						String path = UpdateManagerUtils.getURLAsString(site.getURL(), url);
						// add end "/"
						if(!path.endsWith(".jar")) //$NON-NLS-1$
							path += (path.endsWith(File.separator) || path.endsWith("/")) ? "" : "/"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						pluginStrings.add(path);
						if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_CONFIGURATION)
							UpdateCore.warn("Add plugin: " + path + " to the list"); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
			}
		}
		return pluginStrings;
	}

	/**
	 *	 Obtains strings existing in the allStrings array, but not in the stringsToRemove
	 */
	private String[] subtract(String[] allStrings, String[] stringsToRemove) {
		HashSet resultList = new HashSet(Arrays.asList(allStrings));
		resultList.removeAll(Arrays.asList(stringsToRemove));
		return (String[])resultList.toArray(new String[resultList.size()]);
	}
}
