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


import org.eclipse.core.runtime.ListenerList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.Set;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.NLS;
import org.eclipse.update.configuration.IActivity;
import org.eclipse.update.configuration.IConfiguredSite;
import org.eclipse.update.configuration.IConfiguredSiteChangedListener;
import org.eclipse.update.configuration.IInstallConfiguration;
import org.eclipse.update.configuration.IProblemHandler;
import org.eclipse.update.configurator.ConfiguratorUtils;
import org.eclipse.update.configurator.IPlatformConfiguration;
import org.eclipse.update.core.IFeature;
import org.eclipse.update.core.IFeatureReference;
import org.eclipse.update.core.IIncludedFeatureReference;
import org.eclipse.update.core.IPluginEntry;
import org.eclipse.update.core.ISite;
import org.eclipse.update.core.ISiteFeatureReference;
import org.eclipse.update.core.IVerificationListener;
import org.eclipse.update.core.SiteManager;
import org.eclipse.update.core.Utilities;
import org.eclipse.update.core.VersionedIdentifier;
import org.eclipse.update.core.model.InstallAbortedException;
import org.eclipse.update.internal.model.ConfiguredSiteModel;
import org.eclipse.update.internal.operations.UpdateUtils;

/**
 * A Configured site manages the Configured and unconfigured features of a Site
 */
public class ConfiguredSite extends ConfiguredSiteModel implements IConfiguredSite {

	private static final String PRODUCT_SITE_MARKER = ".eclipseproduct"; //$NON-NLS-1$
	private static final String EXTENSION_SITE_MARKER = ".eclipseextension"; //$NON-NLS-1$

	// listeners	
	private ListenerList listeners = new ListenerList(ListenerList.IDENTITY);

	// verification status
	private IStatus verifyStatus;

	// transient: true if the site was just created so we can remove it
	private transient boolean justCreated = false;

	/*
	 * Default Constructor
	 */
	public ConfiguredSite() {
	}

	/*
	 * Copy Constructor
	 * As of now, configSite can only be of type ConfiguredSite
	 */
	public ConfiguredSite(IConfiguredSite configSite) {
		ConfiguredSite cSite = (ConfiguredSite) configSite;
		setSiteModel(cSite.getSiteModel());
		setConfigurationPolicyModel(new ConfigurationPolicy(cSite.getConfigurationPolicy()));
		setUpdatable(cSite.isUpdatable());
		setEnabled(cSite.isEnabled());
		setPreviousPluginPath(cSite.getPreviousPluginPath());
		setPlatformURLString(cSite.getPlatformURLString());
	}

	/*
	 *  Adds a listener
	 */
	public void addConfiguredSiteChangedListener(IConfiguredSiteChangedListener listener) {
		listeners.add(listener);
	}

	/*
	 * Removes a listener
	 */
	public void removeConfiguredSiteChangedListener(IConfiguredSiteChangedListener listener) {
		listeners.remove(listener);
	}

	/*
	 * @see IConfiguredSite#install(IFeature,IVerificationListener, IProgressMonitor)
	 */
	public IFeatureReference install(IFeature feature, IVerificationListener verificationListener, IProgressMonitor monitor) throws InstallAbortedException, CoreException {
		return install(feature, null, verificationListener, monitor);
	}

	/*
	 * @see IConfiguredSite#install(IFeature, IFeatureReference, IVerificationListener, IProgressMonitor)
	 */
	public IFeatureReference install(IFeature feature, IFeatureReference[] optionalFeatures, IVerificationListener verificationListener, IProgressMonitor monitor) throws InstallAbortedException, CoreException {

		// change the status if justCreated
		if (justCreated) justCreated=false;

		// ConfigSite is read only 
		if (!isUpdatable()) {
			String errorMessage = NLS.bind(Messages.ConfiguredSite_NonInstallableSite, (new String[] { getSite().getURL().toExternalForm() }));
			IStatus status = verifyUpdatableStatus();
			if (status != null)
				errorMessage += " " + status.getMessage(); //$NON-NLS-1$
			throw Utilities.newCoreException(errorMessage, null);
		}

		// feature is null
		if (feature == null) {
			String errorMessage = Messages.ConfiguredSite_NullFeatureToInstall; 
			throw Utilities.newCoreException(errorMessage, null);
		}

		// feature reference to return
		IFeatureReference installedFeatureRef;
		IFeature installedFeature = null;

		// create the Activity (INSTALL)
		ConfigurationActivity activity = new ConfigurationActivity(IActivity.ACTION_FEATURE_INSTALL);
		activity.setLabel(feature.getVersionedIdentifier().toString());
		activity.setDate(new Date());

		try {
			installedFeatureRef = getSite().install(feature, optionalFeatures, verificationListener, monitor);

			if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_INSTALL) {
				UpdateCore.debug("Sucessfully installed: " + installedFeatureRef.getURL().toExternalForm()); //$NON-NLS-1$
			}

			if (installedFeatureRef != null) {
				try {
					installedFeature = installedFeatureRef.getFeature(null);
				} catch (CoreException e) {
					UpdateCore.warn(null, e);
				}
			}

			// everything done ok
			activity.setStatus(IActivity.STATUS_OK);

			// notify listeners
			Object[] siteListeners = listeners.getListeners();
			for (int i = 0; i < siteListeners.length; i++) {
				if (installedFeature != null) {
					IConfiguredSiteChangedListener listener = ((IConfiguredSiteChangedListener) siteListeners[i]);
					listener.featureInstalled(installedFeature);
				}
			}
		} catch (CoreException e) {
			// not ok, set Activity status
			activity.setStatus(IActivity.STATUS_NOK);
			throw e;
		} finally {
			IInstallConfiguration current = SiteManager.getLocalSite().getCurrentConfiguration();
			((InstallConfiguration) current).addActivity(activity);
		}
		// call the configure task	
		if (installedFeature != null)
			configure(installedFeature, optionalFeatures, true);
		/*callInstallHandler*/

		return installedFeatureRef;
	}

	/*
	 * @see IConfiguredSite#remove(IFeature, IProgressMonitor)
	 */
	public void remove(IFeature feature, IProgressMonitor monitor) throws CoreException {

		// ConfigSite is read only
		if (!isUpdatable()) {
			String errorMessage = NLS.bind(Messages.ConfiguredSite_NonUninstallableSite, (new String[] { getSite().getURL().toExternalForm() }));
			throw Utilities.newCoreException(errorMessage, null);
		}

		// create the Activity
		ConfigurationActivity activity = new ConfigurationActivity(IActivity.ACTION_FEATURE_REMOVE);
		activity.setLabel(feature.getVersionedIdentifier().toString());
		activity.setDate(new Date());

		try {
			IFeatureReference referenceToRemove = null;
			ISiteFeatureReference[] featureRef = getSite().getFeatureReferences();
			ISiteFeatureReference ref = getSite().getFeatureReference(feature);
			for (int i = 0; i < featureRef.length; i++) {
				if (featureRef[i].equals(ref)) {
					referenceToRemove = featureRef[i];
					break;
				}
			}

			// we found a feature reference on the site matching the feature			
			if (referenceToRemove != null) {
				// Check if feature is unconfigured before we remove it
				// our UI will check.
				// For non-UI application, throw error is feature is configured
				if (getConfigurationPolicy().isConfigured(referenceToRemove)) {
					IFeature featureToRemove = referenceToRemove.getFeature(null);
					String featureLabel = (featureToRemove == null) ? null : featureToRemove.getLabel();
					throw Utilities.newCoreException(NLS.bind(Messages.ConfiguredSite_UnableToRemoveConfiguredFeature, (new String[] { featureLabel })), null);
				}
			} else {
				throw Utilities.newCoreException(NLS.bind(Messages.ConfiguredSite_UnableToFindFeature, (new String[] { feature.getURL().toString() })),
				null);
			}

			// remove the feature
			getSite().remove(feature, monitor);
			getConfigurationPolicy().removeFeatureReference(referenceToRemove);
			// everything done ok
			activity.setStatus(IActivity.STATUS_OK);
			// notify listeners
			Object[] siteListeners = listeners.getListeners();
			for (int i = 0; i < siteListeners.length; i++) {
				((IConfiguredSiteChangedListener) siteListeners[i]).featureRemoved(feature);
			}
		} catch (CoreException e) {
			activity.setStatus(IActivity.STATUS_NOK);
			throw e;
		} finally {
			IInstallConfiguration current = SiteManager.getLocalSite().getCurrentConfiguration();
			((InstallConfiguration) current).addActivity(activity);
		}
	}

	/*
	 * @see IConfiguredSite#configure(IFeature) 
	 */
	public void configure(IFeature feature) throws CoreException {
		configure(feature, null, true /*callInstallHandler*/
		);
	}

	/*
	 * 
	 */
	private void configure(IFeature feature, IFeatureReference[] optionalFeatures, boolean callInstallHandler) throws CoreException {

		if (feature == null) {
			UpdateCore.warn("Attempting to configure a null feature in site:" + getSite().getURL().toExternalForm()); //$NON-NLS-1$
			return;
		}

		ConfigurationPolicy configPolicy = getConfigurationPolicy();
		if (configPolicy == null)
			return;

		// bottom up approach, same configuredSite
		IIncludedFeatureReference[] childrenRef = feature.getIncludedFeatureReferences();
		if (optionalFeatures != null) {
			childrenRef = childrenToConfigure(childrenRef, optionalFeatures);
		}

		for (int i = 0; i < childrenRef.length; i++) {
			try {
				IFeature child = childrenRef[i].getFeature(null);
				configure(child, optionalFeatures, callInstallHandler);
			} catch (CoreException e) {
				// if not an optional feature, throw exception
				if (!childrenRef[i].isOptional()) {
					UpdateCore.warn("Unable to configure child feature: " + childrenRef[i] + " " + e); //$NON-NLS-1$ //$NON-NLS-2$
					throw e;
				}
			}
		}

		// configure root feature 	
		IFeatureReference featureReference = getSite().getFeatureReference(feature);
		configPolicy.configure(featureReference, callInstallHandler, true);

		// notify listeners
		Object[] siteListeners = listeners.getListeners();
		for (int i = 0; i < siteListeners.length; i++) {
			((IConfiguredSiteChangedListener) siteListeners[i]).featureConfigured(feature);
		}
	}

	/*
	 * Return the optional children to configure
	 * 
	 * @param children all the nested features
	 * @param optionalfeatures optional features to install
	 * @return IFeatureReference[]
	 */
	private IIncludedFeatureReference[] childrenToConfigure(IIncludedFeatureReference[] children, IFeatureReference[] optionalfeatures) {

		List childrenToInstall = new ArrayList();
		for (int i = 0; i < children.length; i++) {
			IIncludedFeatureReference optionalFeatureToConfigure = children[i];
			if (!optionalFeatureToConfigure.isOptional()) {
				childrenToInstall.add(optionalFeatureToConfigure);
			} else {
				for (int j = 0; j < optionalfeatures.length; j++) {
					// must compare feature as optionalFeatures are from the install site
					// where children are on the local site
					try {
						IFeature installedChildren = optionalfeatures[j].getFeature(null);
						if (installedChildren.equals(optionalFeatureToConfigure.getFeature(null))) {
							childrenToInstall.add(optionalFeatureToConfigure);
							break;
						}
					} catch (CoreException e) {
						UpdateCore.warn("", e); //$NON-NLS-1$
					}
				}
			}
		}

		IIncludedFeatureReference[] result = new IIncludedFeatureReference[childrenToInstall.size()];
		if (childrenToInstall.size() > 0) {
			childrenToInstall.toArray(result);
		}

		return result;
	}

	/*
	 * @see IConfiguredSite#unconfigure(IFeature)
	 */
	public boolean unconfigure(IFeature feature) throws CoreException {
		// the first call sould disable without checking for enable parent
		return unconfigure(feature, true, false);
	}

	private boolean unconfigure(IFeature feature, boolean includePatches, boolean verifyEnableParent) throws CoreException {
		IFeatureReference featureReference = getSite().getFeatureReference(feature);

		if (featureReference == null) {
			UpdateCore.warn("Unable to retrieve Feature Reference for feature" + feature); //$NON-NLS-1$
			return false;
		}

		ConfigurationPolicy configPolicy = getConfigurationPolicy();
		if (configPolicy == null)
			return false;

		// verify no enable parent
		if (verifyEnableParent && !validateNoConfiguredParents(feature)) {
			UpdateCore.warn("The feature " + feature.getVersionedIdentifier() + " to disable is needed by another enable feature"); //$NON-NLS-1$ //$NON-NLS-2$
			return false;
		}

		boolean sucessfullyUnconfigured = false;
		try {
			sucessfullyUnconfigured = configPolicy.unconfigure(featureReference, true, true);
		} catch (CoreException e) {
			URL url = featureReference.getURL();
			String urlString = (url != null) ? url.toExternalForm() : "<no feature reference url>"; //$NON-NLS-1$
			UpdateCore.warn("Unable to unconfigure" + urlString, e); //$NON-NLS-1$
			throw e;
		}
		if (sucessfullyUnconfigured) {
			// 2.0.2: unconfigure patches that reference this feature.
			// A patch is a feature that contains an import
			// statement with patch="true" and an id/version
			// that matches an already installed and configured
			// feature. When patched feature is unconfigured,
			// all the patches that reference it must be 
			// unconfigured as well
			// (in contrast, patched features can be
			// configured without the patches).
			if (includePatches)
				unconfigurePatches(feature);

			// top down approach, same configuredSite
			IIncludedFeatureReference[] childrenRef = feature.getIncludedFeatureReferences();
			for (int i = 0; i < childrenRef.length; i++) {
				try {
					IFeature child = childrenRef[i].getFeature(null); // disable the exact feature
					unconfigure(child, includePatches, true); // check for parent as we should be the only parent.
				} catch (CoreException e) {
					// skip any bad children
					UpdateCore.warn("Unable to unconfigure child feature: " + childrenRef[i] + " " + e); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}

			// notify listeners
			Object[] siteListeners = listeners.getListeners();
			for (int i = 0; i < siteListeners.length; i++) {
				IConfiguredSiteChangedListener listener = ((IConfiguredSiteChangedListener) siteListeners[i]);
				listener.featureUnconfigured(feature);
			}

			return true;
		} else {
			URL url = featureReference.getURL();
			String urlString = (url != null) ? url.toExternalForm() : "<no feature reference url>"; //$NON-NLS-1$
			UpdateCore.warn("Unable to unconfigure:" + urlString); //$NON-NLS-1$
			return false;
		}
	}

	/*
	 * Look for features that have an import reference
	 * that points to this feature and where patch=true.
	 * Unconfigure all the matching patches, but
	 * do not do the same lookup for them
	 * because patches cannot have patches themselves.
	 */

	private void unconfigurePatches(IFeature feature) {
		IFeatureReference[] frefs = getConfiguredFeatures();
		for (int i = 0; i < frefs.length; i++) {
			IFeatureReference fref = frefs[i];
			try {
				IFeature candidate = fref.getFeature(null);
				if (candidate.equals(feature))
					continue;

				if (UpdateUtils.isPatch(feature, candidate))
					unconfigure(candidate, false, false);
			} catch (CoreException e) {
				UpdateCore.warn("", e); //$NON-NLS-1$
			}
		}
	}

	/*
	 * @see IConfiguredSite#getConfiguredFeatures()
	 */
	public IFeatureReference[] getConfiguredFeatures() {
		if (isEnabled())
			return getRawConfiguredFeatures();
		else
			return new ISiteFeatureReference[0];
	}

	/*
	 * @see IConfiguredSite#getConfiguredFeatures()
	 */
	private IFeatureReference[] getRawConfiguredFeatures() {
		ConfigurationPolicy configPolicy = getConfigurationPolicy();
		if (configPolicy == null)
			return new ISiteFeatureReference[0];

		return configPolicy.getConfiguredFeatures();
	}

	/*
	 * adds configured and unconfigured feature references
	 */
	public IFeatureReference[] getFeatureReferences() {

		ConfigurationPolicy configPolicy = getConfigurationPolicy();
		if (configPolicy == null)
			return new ISiteFeatureReference[0];

		IFeatureReference[] configuredFeatures = getConfiguredFeatures();
		int confLen = configuredFeatures.length;
		IFeatureReference[] unconfiguredFeatures = configPolicy.getUnconfiguredFeatures();
		int unconfLen = unconfiguredFeatures.length;

		IFeatureReference[] result = new IFeatureReference[confLen + unconfLen];
		if (confLen > 0) {
			System.arraycopy(configuredFeatures, 0, result, 0, confLen);
		}
		if (unconfLen > 0) {
			System.arraycopy(unconfiguredFeatures, 0, result, confLen, unconfLen);
		}

		return result;
	}

	/*
	 * Configure and unconfigure appropriate feature to
	 * become 'like' currentConfiguration which is the configuration
	 * the user wants to revert to.
	 * 
	 * All features from currentConfiguration should be configured
	 */
	public void revertTo(IConfiguredSite oldConfiguration, IProgressMonitor monitor, IProblemHandler handler) throws CoreException, InterruptedException {

		ConfiguredSite oldConfiguredSite = (ConfiguredSite) oldConfiguration;

		// retrieve the feature that were configured
		IFeatureReference[] configuredFeatures = oldConfiguredSite.validConfiguredFeatures(handler);

		for (int i = 0; i < configuredFeatures.length; i++) {
			getConfigurationPolicy().configure(configuredFeatures[i], true, true);
		}

		// calculate all the features we have to unconfigure from the current state to this state
		// in the history. 				
		List featureToUnconfigure = oldConfiguredSite.calculateUnconfiguredFeatures(configuredFeatures);

		// for each unconfigured feature check if it still exists
		// if so add as unconfigured
		Iterator iter = featureToUnconfigure.iterator();
		while (iter.hasNext()) {
			IFeatureReference element = (IFeatureReference) iter.next();
			try {
				// do not log activity
				getConfigurationPolicy().unconfigure(element, true, true);
			} catch (CoreException e) {
				// log no feature to unconfigure
				String url = element.getURL().toString();
				ISite site = element.getSite();
				String siteString = (site != null) ? site.getURL().toExternalForm() : Messages.ConfiguredSite_NoSite; 
				UpdateCore.warn(NLS.bind(Messages.ConfiguredSite_CannotFindFeatureToUnconfigure, (new String[] { url, siteString })), e); 
			}
		}
		//} // end USER_EXCLUDE
	}

	/*
	 * We have to keep our configured feature
	 * check if they are all valid
	 * Return the valid configured features
	 */
	private IFeatureReference[] validConfiguredFeatures(IProblemHandler handler) throws InterruptedException {

		IFeatureReference[] configuredFeatures = getConfiguredFeatures();
		if (configuredFeatures != null) {
			for (int i = 0; i < configuredFeatures.length; i++) {
				IFeature feature = null;

				// attempt to access the feature
				try {
					feature = configuredFeatures[i].getFeature(null);
				} catch (CoreException e) {
					// notify we cannot find the feature
					UpdateCore.warn(null, e);
					String featureString = configuredFeatures[i].getURL().toExternalForm();
					if (!handler.reportProblem(NLS.bind(Messages.ConfiguredSite_CannotFindFeatureToConfigure, (new String[] { featureString })))) {
						throw new InterruptedException();
					}
				}

				// verify all the plugins still exist
				if (feature != null) {
					// get plugin identifier
					List sitePluginIdentifiers = new ArrayList();
					ISite site = feature.getSite();
					IPluginEntry[] sitePluginEntries = null;

					if (site != null) {
						sitePluginEntries = site.getPluginEntries();
						for (int index = 0; index < sitePluginEntries.length; index++) {
							IPluginEntry entry = sitePluginEntries[index];
							sitePluginIdentifiers.add(entry.getVersionedIdentifier());
						}
					}

					if (sitePluginEntries.length > 0) {
						IPluginEntry[] featurePluginEntries = feature.getPluginEntries();
						for (int index = 0; index < featurePluginEntries.length; index++) {
							IPluginEntry currentFeaturePluginEntry = featurePluginEntries[index];
							if (!contains(currentFeaturePluginEntry.getVersionedIdentifier(), sitePluginIdentifiers)) {
								// the plugin defined by the feature
								// doesn't seem to exist on the site
								String msg = "Error verifying existence of plugin:" + currentFeaturePluginEntry.getVersionedIdentifier().toString(); //$NON-NLS-1$
								UpdateCore.log(msg, new Exception());

								String siteString = (site != null) ? site.getURL().toExternalForm() : Messages.ConfiguredSite_NoSite;	
								String errorLabel = NLS.bind(Messages.ConfiguredSite_CannotFindPluginEntry, (new String[] { currentFeaturePluginEntry.getVersionedIdentifier().toString(), siteString }));
								if (handler == null) {
									throw new InterruptedException(errorLabel);
								}
								if (!handler.reportProblem(errorLabel)) {
									throw new InterruptedException();
								}
							} // end if not found in site
						} // end for
					}
				}
			} // end for configured feature
		}
		return configuredFeatures;
	}

	/*
	 * We are in the process of calculating the delta between what was configured in the current
	 * configuration that is not configured now
	 * 
	 * we have to figure out what feature have been unconfigured for the whole
	 * history between current and us... 
	 * 
	 * is it as simple as  get all configured, and unconfigured,
	 * the do the delta with what should be configured
	 * 
	 */
	private List calculateUnconfiguredFeatures(IFeatureReference[] configuredFeatures) throws CoreException {

		Set featureToUnconfigureSet = new HashSet();

		// loop for all history
		// try to see if the configured site existed
		// if it does, get the unconfigured features 
		// and the configured one
		IInstallConfiguration[] history = SiteManager.getLocalSite().getConfigurationHistory();

		for (int i = 0; i < history.length; i++) {
			IInstallConfiguration element = history[i];
			IConfiguredSite[] configSites = element.getConfiguredSites();
			for (int j = 0; j < configSites.length; j++) {
				ConfiguredSite configSite = (ConfiguredSite) configSites[j];
				if (configSite.getSite().equals(getSite())) {
					featureToUnconfigureSet.addAll(Arrays.asList(configSite.getConfigurationPolicy().getUnconfiguredFeatures()));
					featureToUnconfigureSet.addAll(Arrays.asList(configSite.getConfigurationPolicy().getConfiguredFeatures()));
				}
			}
		}

		// remove the unconfigured feature we found that are now to be configured 
		// (they may have been unconfigured in the past, but the revert makes them configured)
		List featureToUnconfigureList = remove(configuredFeatures, featureToUnconfigureSet);

		return featureToUnconfigureList;
	}

	/*
	 * Utilities: Remove an array of feature references
	 * from a list
	 */
	private List remove(IFeatureReference[] featureRefs, Set set) {
		List result = new ArrayList();

		if (set == null)
			return result;

		// if an element of the list is NOT found in the array,
		// add it to the result list			
		Iterator iter = set.iterator();
		while (iter.hasNext()) {
			IFeatureReference element = (IFeatureReference) iter.next();
			boolean found = false;
			for (int i = 0; i < featureRefs.length; i++) {
				if (element.equals(featureRefs[i])) {
					found = true;
				}
			}

			if (!found)
				result.add(element);
		}
		return result;
	}

	/*
	 * I have issues when running list.contain(versionedIdentifier)
	 * The code runs the Object.equals instead of the VersionedIdentifier.equals
	 */
	private boolean contains(VersionedIdentifier id, List list) {
		boolean found = false;
		if (list != null && !list.isEmpty()) {
			Iterator iter = list.iterator();
			while (iter.hasNext() && !found) {
				VersionedIdentifier element = (VersionedIdentifier) iter.next();
				if (element.equals(id)) {
					found = true;
				}
			}
		}
		return found;
	}

	/*
	 * 
	 */
	public ConfigurationPolicy getConfigurationPolicy() {
		return (ConfigurationPolicy) getConfigurationPolicyModel();
	}

	/*
	 * 
	 */
	public ISite getSite() {
		return (ISite) getSiteModel();
	}

	/*
	 * 
	 */
	public IInstallConfiguration getInstallConfiguration() {
		return (IInstallConfiguration) getInstallConfigurationModel();
	}

	/*
	 * 
	 */
	public IStatus getBrokenStatus(IFeature feature) {

		IStatus featureStatus = createStatus(IStatus.OK, IFeature.STATUS_HAPPY, "", null); //$NON-NLS-1$

		// check the Plugins of all the features
		// every plugin of the feature must be on the site
		IPluginEntry[] siteEntries = getSite().getPluginEntries();
		IPluginEntry[] featuresEntries = feature.getPluginEntries();
		IPluginEntry[] result = UpdateManagerUtils.diff(featuresEntries, siteEntries);
		if (result != null && (result.length != 0)) {
			String msg = Messages.SiteLocal_FeatureUnHappy; 
			MultiStatus multi = new MultiStatus(featureStatus.getPlugin(), IFeature.STATUS_UNHAPPY, msg, null);

			for (int k = 0; k < result.length; k++) {
				VersionedIdentifier id = result[k].getVersionedIdentifier();
				Object[] values = new String[] { "", "" }; //$NON-NLS-1$ //$NON-NLS-2$
				if (id != null) {
					values = new Object[] { id.getIdentifier(), id.getVersion()};
				}
				String msg1 = NLS.bind(Messages.ConfiguredSite_MissingPluginsBrokenFeature, values);
				UpdateCore.warn(msg1);
				IStatus status = createStatus(IStatus.ERROR, IFeature.STATUS_UNHAPPY, msg1, null);
				multi.add(status);
			}
			return multi;
		}

		// check os, arch, and ws

		String msg = Messages.SiteLocal_FeatureHappy; 
		return createStatus(IStatus.OK, IFeature.STATUS_HAPPY, msg, null);
	}

	/*
	 * 
	 */
	public boolean isConfigured(IFeature feature) {
		if (!isEnabled())
			return false;

		if (getConfigurationPolicy() == null)
			return false;
		IFeatureReference featureReference = getSite().getFeatureReference(feature);
		if (featureReference == null) {
			if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_WARNINGS)
				UpdateCore.warn("Unable to retrieve featureReference for feature:" + feature); //$NON-NLS-1$
			return false;
		}
		return getConfigurationPolicy().isConfigured(featureReference);
	}

	/**
	 * @see Object#toString()
	 */
	public String toString() {
		if (getSite() == null)
			return "No Site"; //$NON-NLS-1$
		if (getSite().getURL() == null)
			return "No URL"; //$NON-NLS-1$
		return getSite().getURL().toExternalForm();
	}

	/**
	 * @see IConfiguredSite#verifyUpdatableStatus()
	 */
	public IStatus verifyUpdatableStatus() {

		if (verifyStatus != null)
			return verifyStatus;

		URL siteURL = getSite().getURL();
		if (siteURL == null) {
			verifyStatus = createStatus(IStatus.ERROR, Messages.ConfiguredSite_SiteURLNull, null); 
			return verifyStatus;
		}

		if (!"file".equalsIgnoreCase(siteURL.getProtocol())) { //$NON-NLS-1$
			verifyStatus = createStatus(IStatus.ERROR, Messages.ConfiguredSite_NonLocalSite, null); 
			return verifyStatus;
		}

		String siteLocation = siteURL.getFile();
		File file = new File(siteLocation);

		// get the product name of the private marker
		// if there is no private marker, check if the site is contained in another site
		// if there is a marker and this is a different product, return false
		// otherwise don't check if we are contained in another site
		String productName = getProductName(file);
		if (productName != null) {
			if (!productName.equals(getProductIdentifier("id", getProductFile()))) { //$NON-NLS-1$
				verifyStatus = createStatus(IStatus.ERROR, NLS.bind(Messages.ConfiguredSite_NotSameProductId, (new String[] { productName })), null);
				return verifyStatus;
			}
		} else {
			File container = getSiteContaining(file);
			// allow the install location to pass even though it looks like this
			// site is contained in another site
			if (container != null && !siteLocation.equals(Platform.getInstallLocation().getURL().getFile())) {
				verifyStatus = createStatus(IStatus.ERROR, NLS.bind(Messages.ConfiguredSite_ContainedInAnotherSite, (new String[] { container.getAbsolutePath() })), null);
				return verifyStatus;
			}
		}

		if (!canWrite(file)) {
			verifyStatus = createStatus(IStatus.ERROR, Messages.ConfiguredSite_ReadOnlySite, null); 
			return verifyStatus;
		}

		verifyStatus = createStatus(IStatus.OK, "", null); //$NON-NLS-1$
		setUpdatable(true);
		return verifyStatus;
	}

	/*
	 * Verify we can write on the file system
	 */
    public static boolean canWrite(File file) {
        if (file.canWrite() == false)
            return false;

        if (!file.isDirectory())
            return false;

        File fileTest = null;
        try {
        	// we use the .dll suffix to properly test on Vista virtual directories
        	// on Vista you are not allowed to write executable files on virtual directories like "Program Files"
            fileTest = File.createTempFile("writtableArea", ".dll", file); //$NON-NLS-1$ //$NON-NLS-2$
        } catch (IOException e) {
            //If an exception occurred while trying to create the file, it means that it is not writable
            return false;
        } finally {
            if (fileTest != null)
                fileTest.delete();
        }
        return true;
    }

	/*
	 * Check if the directory contains a marker
	 * if not ask all directory children to check
	 * if one validates the condition, returns the marker
	 */
	private static File getSiteContaining(File file) {

		if (file == null)
			return null;

		UpdateCore.warn("IsContained: Checking for markers at:" + file); //$NON-NLS-1$
		if (file.exists() && file.isDirectory()) {
			File productFile = new File(file, PRODUCT_SITE_MARKER);
			File extensionFile = new File(file, EXTENSION_SITE_MARKER);
			if (productFile.exists() || extensionFile.exists())
				return file;
//			// do not check if a marker exists in the current but start from the parent
//			// the current is analyze by getProductname()
//			if (file.getParentFile() != null) {
//				File privateFile = new File(file.getParentFile(), PRIVATE_SITE_MARKER);
//				if (privateFile.exists())
//					return file.getParentFile();
//			}
		}
		return getSiteContaining(file.getParentFile());
	}

	/*
	 * Returns the name of the product if the identifier of the private Site markup is not
	 * the same as the identifier of the product the workbench was started with.
	 * If the product is the same, return null.
	 */
	private static String getProductName(File file) {

		if (file == null)
			return null;

		File markerFile = new File(file, EXTENSION_SITE_MARKER );
		if (!markerFile.exists()) {
			return null;
		}

		File productFile = getProductFile();
		String productId = null;
		String privateId = null;
		if (productFile != null) {
			productId = getProductIdentifier("id", productFile); //$NON-NLS-1$
			privateId = getProductIdentifier("id", markerFile); //$NON-NLS-1$
			if (productId == null) {
				UpdateCore.warn("Product ID is null at:" + productFile); //$NON-NLS-1$
				return null;
			}
			if (!productId.equalsIgnoreCase(privateId)) {
				UpdateCore.warn("Product id at" + productFile + " Different than:" + markerFile); //$NON-NLS-1$ //$NON-NLS-2$
				String name = getProductIdentifier("name", markerFile); //$NON-NLS-1$
				String version = getProductIdentifier("version", markerFile); //$NON-NLS-1$
				String markerID = (name == null) ? version : name + ":" + version; //$NON-NLS-1$
				if (markerID == null)
					markerID = ""; //$NON-NLS-1$
				return markerID;
			} else {
				return privateId;
			}
		} else {
			UpdateCore.warn("Product Marker doesn't exist:" + productFile); //$NON-NLS-1$
		}

		return null;
	}

	/*
	 * Returns the identifier of the product from the property file
	 */
	private static String getProductIdentifier(String identifier, File propertyFile) {
		String result = null;
		if (identifier == null)
			return result;
		InputStream in = null;
		try {
			in = new FileInputStream(propertyFile);
			PropertyResourceBundle bundle = new PropertyResourceBundle(in);
			result = bundle.getString(identifier);
		} catch (IOException e) {
			if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_INSTALL)
				UpdateCore.debug("Exception reading property file:" + propertyFile); //$NON-NLS-1$
		} catch (MissingResourceException e) {
			if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_INSTALL)
				UpdateCore.debug("Exception reading '" + identifier + "' from property file:" + propertyFile); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			if (in != null)
				try {
					in.close();
				} catch (IOException e1) {
				}
		}
		return result;
	}

	/*
	 * Returns the identifier of the product from the property file
	 */
	private static File getProductFile() {

		String productInstallDirectory = ConfiguratorUtils.getInstallURL().getFile();
		if (productInstallDirectory != null) {
			File productFile = new File(productInstallDirectory, PRODUCT_SITE_MARKER);
			if (productFile.exists()) {
				return productFile;
			} else {
				UpdateCore.warn("Product marker doesn't exist:" + productFile); //$NON-NLS-1$
			}
		} else {
			UpdateCore.warn("Cannot retrieve install URL from BootLoader"); //$NON-NLS-1$
		}
		return null;
	}

	/*
	 * 
	 */
	/*package*/
	boolean createPrivateSiteMarker() {
		URL siteURL = getSite().getURL();
		if (siteURL == null) {
			UpdateCore.warn("Unable to create marker. The Site url is null."); //$NON-NLS-1$
			return false;
		}

		if (!"file".equalsIgnoreCase(siteURL.getProtocol())) { //$NON-NLS-1$
			UpdateCore.warn("Unable to create private marker. The Site is not on the local file system."); //$NON-NLS-1$
			return false;
		}

		String siteLocation = siteURL.getFile();
		File productFile = getProductFile();
		boolean success = false;
		if (productFile != null) {
			String productId = getProductIdentifier("id", productFile); //$NON-NLS-1$
			String productName = getProductIdentifier("name", productFile); //$NON-NLS-1$
			String productVer = getProductIdentifier("version", productFile); //$NON-NLS-1$
			if (productId != null) {
				File file = new File(siteLocation, EXTENSION_SITE_MARKER);
				if (!file.exists()) {
					OutputStream out = null;
					OutputStreamWriter outWriter = null;
					try {
						out = new FileOutputStream(file);
						outWriter = new OutputStreamWriter(out, "UTF8"); //$NON-NLS-1$
						outWriter.write("id=" + productId+"\n"); //$NON-NLS-1$ //$NON-NLS-2$
						if (productName != null)
							outWriter.write("name=" + productName+"\n"); //$NON-NLS-1$ //$NON-NLS-2$
						if (productVer != null)
							outWriter.write("version=" + productVer+"\n"); //$NON-NLS-1$ //$NON-NLS-2$
						success = true;
						justCreated = true;
					} catch (Exception e) {
						UpdateCore.warn("Unable to create private Marker at:" + file, e); //$NON-NLS-1$
					} finally {
						try {
							if (outWriter != null)
								outWriter.close();
						} catch (IOException e1) {
						}
						try {
							if (out != null)
								out.close();
						} catch (IOException e2) {
						}
					}
				}
			}
		}
		return success;
	}


	/*
	 * Returns true if the directory of the Site contains
	 * .eclipseextension
	 */
	public boolean isExtensionSite() {
		return containsMarker(EXTENSION_SITE_MARKER);
	}

	/*
	 * Returns true if the directory of the Site contains
	 * .eclipseextension
	 */
	public boolean isProductSite() {
		return containsMarker(PRODUCT_SITE_MARKER);
	}

	/*
	 * Returns true if the directory of the Site contains
	 * .eclipseextension
	 */
	public boolean isPrivateSite() {
		return isExtensionSite();
	}

	/*
	 * 
	 */
	private boolean containsMarker(String marker) {
		ISite site = getSite();
		if (site == null) {
			UpdateCore.warn("Contains Markers:The site is null"); //$NON-NLS-1$
			return false;
		}

		URL url = site.getURL();
		if (url == null) {
			UpdateCore.warn("Contains Markers:Site URL is null"); //$NON-NLS-1$
			return false;
		}
		if (!"file".equalsIgnoreCase(url.getProtocol())) { //$NON-NLS-1$
			UpdateCore.warn("Contains Markers:Non file protocol"); //$NON-NLS-1$
			return false;
		}
		File file = new File(url.getFile());
		if (!file.exists()) {
			UpdateCore.warn("Contains Markers:The site doesn't exist:" + file); //$NON-NLS-1$
			return false;
		}
		File extension = new File(file, marker);
		if (!extension.exists()) {
			UpdateCore.warn("Contains Markers:The extensionfile does not exist:" + extension); //$NON-NLS-1$
			return false;
		}
		return true;
	}

	/*
	 * Returns true if the Site is already natively linked
	 */
	public boolean isNativelyLinked() throws CoreException {
		String platformString = getPlatformURLString();
		if (platformString == null) {
			UpdateCore.warn("Unable to retrieve platformString"); //$NON-NLS-1$
			return false;
		}

		URL siteURL = null;
		try {
			// check if the site exists and is updateable
			// update configSite
			URL urlToCheck = new URL(platformString);
			IPlatformConfiguration runtimeConfig = ConfiguratorUtils.getCurrentPlatformConfiguration();
			IPlatformConfiguration.ISiteEntry entry = runtimeConfig.findConfiguredSite(urlToCheck);
			if (entry != null) {
				return entry.isNativelyLinked();
			} else {
				UpdateCore.warn("Unable to retrieve site:" + platformString + " from platform."); //$NON-NLS-1$ //$NON-NLS-2$
			}

			// check by comparing URLs
			IPlatformConfiguration.ISiteEntry[] sites = runtimeConfig.getConfiguredSites();
			for (int i = 0; i < sites.length; i++) {
				siteURL = sites[i].getURL();
				URL resolvedURL = FileLocator.resolve(siteURL);
				if (UpdateManagerUtils.sameURL(resolvedURL, urlToCheck))
					return true;
			}
		} catch (MalformedURLException e) {
			String msg = NLS.bind(Messages.ConfiguredSite_UnableResolveURL, (new String[] { platformString }));
			throw Utilities.newCoreException(msg, e);
		} catch (IOException e) {
			String msg = NLS.bind(Messages.ConfiguredSite_UnableToAccessSite, (new Object[] { siteURL }));
			throw Utilities.newCoreException(msg, e);
		}

		return false;
	}

	/*
	* we have to check that no configured/enable parent include this feature
	*/
	private boolean validateNoConfiguredParents(IFeature feature) throws CoreException {
		if (feature == null) {
			UpdateCore.warn("ConfigurationPolicy: validate Feature is null"); //$NON-NLS-1$
			return true;
		}

		IFeatureReference[] parents = UpdateManagerUtils.getParentFeatures(feature, getConfiguredFeatures(), false);
		return (parents.length == 0);
	}

}
