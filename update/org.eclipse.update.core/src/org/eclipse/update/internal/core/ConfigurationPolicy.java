package org.eclipse.update.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
import java.io.File;
import java.net.URL;
import java.util.*;

import org.eclipse.core.boot.IPlatformConfiguration;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.update.configuration.*;
import org.eclipse.update.configuration.IActivity;
import org.eclipse.update.configuration.IConfiguredSite;
import org.eclipse.update.core.*;
import org.eclipse.update.core.model.FeatureReferenceModel;
import org.eclipse.update.internal.model.ConfigurationActivityModel;
import org.eclipse.update.internal.model.ConfigurationPolicyModel;

/**
 * 
 */
public class ConfigurationPolicy extends ConfigurationPolicyModel {

	private IConfiguredSite configuredSite;

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
		setConfiguredSite(configPolicy.getConfiguredSite());
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
			UpdateManagerPlugin.warn("The feature reference to configure is null");
			return;
		}

		IFeature feature = null;
		try {
			feature = featureReference.getFeature();
		} catch (CoreException e) {
			URL url = featureReference.getURL();
			String urlString = (url != null) ? url.toExternalForm() : "<no feature reference url>";
			UpdateManagerPlugin.warn("Error retrieving feature:" + urlString, e);
			return;
		}
		if (feature == null) {
			URL url = featureReference.getURL();
			String urlString = (url != null) ? url.toExternalForm() : "<no feature reference url>";
			UpdateManagerPlugin.warn("The feature to unconfigure is null: feature reference is:" + urlString);
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
				installConfig.addActivityModel((ConfigurationActivityModel) activity);
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
				throw Utilities.newCoreException(Policy.bind("InstallHandler.error", feature.getLabel()), originalException);
			if (newException != null)
				throw Utilities.newCoreException(Policy.bind("InstallHandler.error", feature.getLabel()), newException);
		}
	}

	/**
	 * check if the plugins to unconfigure are required by other configured feature and
	 * adds the feature to the list of unconfigured features 
	 */
	public boolean unconfigure(IFeatureReference featureReference, boolean callInstallHandler, boolean createActivity) throws CoreException {

		if (!isConfigured(featureReference))
			return true;

		if (featureReference == null) {
			UpdateManagerPlugin.warn("The feature reference to unconfigure is null");
			return false;
		}

		IFeature feature = null;
		try {
			feature = featureReference.getFeature();
		} catch (CoreException e) {
			URL url = featureReference.getURL();
			String urlString = (url != null) ? url.toExternalForm() : "<no feature reference url>";
			UpdateManagerPlugin.warn("Error retrieving feature:" + urlString, e);
			return false;
		}

		if (feature == null) {
			URL url = featureReference.getURL();
			String urlString = (url != null) ? url.toExternalForm() : "<no feature reference url>";
			UpdateManagerPlugin.warn("The feature to unconfigure is null: feature reference is:" + urlString);
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

			InstallConfiguration installConfig = ((InstallConfiguration) SiteManager.getLocalSite().getCurrentConfiguration());

			// Allow unconfigure if the feature is optional from all the parents
			// or if the feature is mandatory and non of its parent are configured
			if (validateNoConfiguredParents(feature)) {
				if (handler != null)
					handler.unconfigureInitiated();
				addUnconfiguredFeatureReference((FeatureReferenceModel) featureReference);
				if (handler != null)
					handler.completeUnconfigure();

				// everything done ok
				if (activity != null) {
					activity.setStatus(IActivity.STATUS_OK);
					installConfig.addActivityModel((ConfigurationActivityModel) activity);
				}
				success = true;
			} else {
				if (activity != null) {
					activity.setStatus(IActivity.STATUS_NOK);
					installConfig.addActivityModel((ConfigurationActivityModel) activity);
				}
			}
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
				throw Utilities.newCoreException(Policy.bind("InstallHandler.error", feature.getLabel()), originalException);
			if (newException != null)
				throw Utilities.newCoreException(Policy.bind("InstallHandler.error", feature.getLabel()), newException);
		}

		if (!success) {
			URL url = featureReference.getURL();
			String urlString = (url != null) ? url.toExternalForm() : "<no feature reference url>";
			UpdateManagerPlugin.warn("Unable to unconfigure:" + urlString);
		}
		return success;
	}

	/**
	 * Calculates the plugin list for the policy. 
	 * For "INCLUDE" policy, this corresponds to the plugins for 
	 * configured features. For "EXCLUDE" policy, this corresponds to the
	 * plugins for unconfigured features that are not referenced
	 * by any configured features.
	 */
	public String[] getPluginPath(ISite site, String[] pluginRead) throws CoreException {

		String[] result;
		String[] pluginsToWrite;
		if (getPolicy() == IPlatformConfiguration.ISitePolicy.USER_EXCLUDE) {
			//	EXCLUDE: return unconfigured plugins MINUS any plugins that
			//           are configured
			String[] unconfigured = getPluginString(site, getUnconfiguredFeatures());
			String[] configured = getPluginString(site, getConfiguredFeatures());
			pluginsToWrite = delta(configured, unconfigured);
		} else {
			// INCLUDE: return configured plugins
			pluginsToWrite = getPluginString(site, getConfiguredFeatures());
		}

		//TRACE
		if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_RECONCILER) {
			UpdateManagerPlugin.debug("GetPluginPath for: " + ((site == null) ? "<No site>" : site.getURL().toString()));
			for (int i = 0; i < pluginsToWrite.length; i++) {
				UpdateManagerPlugin.debug("To write:" + pluginsToWrite[i]);
			}
		}

		// Calculate which plugins we read should still be written out
		// (pluginNotToWrite-pluginRead = delta that should be written out)
		// pluginsToWrite+delta = all that should be written out
		/*IFeatureReference[] arrayOfFeatureRef = null;		
		if (getPolicy() == IPlatformConfiguration.ISitePolicy.USER_EXCLUDE) {
			if (getConfiguredFeatures() != null)
				arrayOfFeatureRef = getConfiguredFeatures();
		} else {
			if (getUnconfiguredFeatures() != null)
				arrayOfFeatureRef = getUnconfiguredFeatures();
		}
		String[] pluginsNotToWrite = getPluginString(site, arrayOfFeatureRef);
		//TRACE
		if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_RECONCILER){
			for (int i = 0; i < pluginsNotToWrite.length; i++) {
				UpdateManagerPlugin.debug("Not to write:"+pluginsNotToWrite[i]);
			}
		}		
		
		String[] included = delta(pluginsNotToWrite, pluginRead);
		//TRACE
		if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_RECONCILER){
			if (included!=null)
			for (int i = 0; i < included.length; i++) {
				UpdateManagerPlugin.debug("Delta with read:"+included[i]);
			}
		}		
		result = union(included, pluginsToWrite);*/

		result = pluginsToWrite;

		return result;
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
		return configuredSite;
	}

	/**
	 * Sets the configuredSite.
	 * @param configuredSite The configuredSite to set
	 */
	public void setConfiguredSite(IConfiguredSite configuredSite) {
		this.configuredSite = configuredSite;
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
	 * return an array of plugin path for the array of feature reference
	 * Each plugin path only appears once [bug 21750]
	 */
	private String[] getPluginString(ISite site, IFeatureReference[] arrayOfFeatureRef) throws CoreException {

		String[] result = new String[0];

		// obtain path for each feature
		if (arrayOfFeatureRef != null) {
			//[bug 21750] replace the List by a Set
			Set pluginsString = new HashSet();
			for (int i = 0; i < arrayOfFeatureRef.length; i++) {
				IFeatureReference element = arrayOfFeatureRef[i];
				IFeature feature = null;
				try {
					feature = element.getFeature();
				} catch (CoreException e) {
					UpdateManagerPlugin.warn(null, e);
				};
				IPluginEntry[] entries = null;
				if (feature == null) {
					UpdateManagerPlugin.warn("Null Feature", new Exception());
					entries = new IPluginEntry[0];
				} else {
					entries = feature.getPluginEntries();
				}

				for (int index = 0; index < entries.length; index++) {
					IPluginEntry entry = entries[index];

					// obtain the path of the plugin directories on the site
					ContentReference[] featureContentReference = null;
					try {
						featureContentReference = feature.getFeatureContentProvider().getPluginEntryArchiveReferences(entry, null /*IProgressMonitor*/
						);
					} catch (CoreException e) {
						UpdateManagerPlugin.warn(null, e);
					}

					// transform into a valid String
					if (featureContentReference != null) {
						for (int j = 0; j < featureContentReference.length; j++) {
							URL url = site.getSiteContentProvider().getArchiveReference(featureContentReference[j].getIdentifier());
							if (url != null) {
								// make it relative to teh site
								String path = UpdateManagerUtils.getURLAsString(site.getURL(), url);
								// add end "/"
								path += (path.endsWith(File.separator) || path.endsWith("/")) ? "" : "/";
								//$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								// add plugin.xml or fragment.xml
								path += entry.isFragment() ? "fragment.xml" : "plugin.xml";
								//$NON-NLS-1$ //$NON-NLS-2$
								pluginsString.add(path);
							}
						}
					}
				}
			}

			// transform in String[]
			if (!pluginsString.isEmpty()) {
				result = new String[pluginsString.size()];
				pluginsString.toArray(result);
			}
		}
		return result;
	}

	/**
	*	 we need to figure out which plugin SHOULD NOT be written and
	*	 remove them from include
	*	 we can compare the String of the URL
	*/
	private String[] delta(String[] pluginsToRemove, String[] allPlugins) {
		// No plugins to remove, return allPlugins 
		if (pluginsToRemove == null || pluginsToRemove.length == 0) {
			return allPlugins;
		}

		// We didn't read any plugins in platform.cfg
		if (allPlugins == null || allPlugins.length == 0) {
			return new String[0];
		}

		// if a String from pluginsToRemove IS in
		// allPlugins, remove it from allPlugins
		List list1 = new ArrayList();
		list1.addAll(Arrays.asList(allPlugins));
		for (int i = 0; i < pluginsToRemove.length; i++) {
			if (list1.contains(pluginsToRemove[i]))
				list1.remove(pluginsToRemove[i]);
		}

		String[] resultEntry = new String[list1.size()];
		if (list1.size() > 0)
			list1.toArray(resultEntry);

		return resultEntry;
	}

	/**
	 * Returns and array with the union of plugins
	*/
	private String[] union(String[] targetArray, String[] sourceArray) {

		// No string 
		if (sourceArray == null || sourceArray.length == 0) {
			return targetArray;
		}

		// No string
		if (targetArray == null || targetArray.length == 0) {
			return sourceArray;
		}

		// if a String from sourceArray is NOT in
		// targetArray, add it to targetArray
		List list1 = new ArrayList();
		list1.addAll(Arrays.asList(targetArray));
		for (int i = 0; i < sourceArray.length; i++) {
			if (!list1.contains(sourceArray[i]))
				list1.add(sourceArray[i]);
		}

		String[] resultEntry = new String[list1.size()];
		if (list1.size() > 0)
			list1.toArray(resultEntry);

		return resultEntry;
	}

	/*
	 * 
	 */
	private boolean validateNoConfiguredParents(IFeature feature) throws CoreException {
		if (feature == null) {
			UpdateManagerPlugin.warn("ConfigurationPolicy: validate Feature is null");
			return true;
		}

		IFeatureReference[] parents = UpdateManagerUtils.getParentFeatures(feature, getConfiguredFeatures(),false);
		if (parents.length == 0)
			return true;

		String msg = Policy.bind("ConfigurationPolicy.UnableToDisable", feature.getLabel());
		UpdateManagerPlugin.warn(msg);
		IFeature parentFeature = null;
		for (int i = 0; i < parents.length; i++) {
			try {
				parentFeature = parents[i].getFeature();
			} catch (CoreException e) {
			}
			String featureLabel = (parentFeature == null) ? parents[i].getURL().toExternalForm() : parentFeature.getLabel();
			UpdateManagerPlugin.warn(Policy.bind("ConfigurationPolicy.ParentIsEnable", featureLabel));
		}
		return false;
	}
}