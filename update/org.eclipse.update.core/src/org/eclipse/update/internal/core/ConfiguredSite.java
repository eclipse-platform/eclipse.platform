package org.eclipse.update.internal.core;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.eclipse.core.boot.BootLoader;
import org.eclipse.core.boot.IPlatformConfiguration;
import org.eclipse.core.runtime.*;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.update.core.model.InstallAbortedException;
import org.eclipse.update.internal.model.ConfiguredSiteModel;
import org.eclipse.update.internal.model.InstallConfigurationParser;

/**
 * A Configured site manages the Configured and unconfigured features of a Site
 */
public class ConfiguredSite extends ConfiguredSiteModel implements IConfiguredSite, IWritable {

	private static final String PRODUCT_SITE_MARKER = ".eclipseproduct";
	private static final String EXTENSION_SITE_MARKER = ".eclipseextension";
	private static final String PRIVATE_SITE_MARKER = ".eclipseUM";

	// listeners	
	private ListenersList listeners = new ListenersList();

	// verification status
	private IStatus verifyStatus;

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
		isUpdatable(cSite.isUpdatable());
		setPreviousPluginPath(cSite.getPreviousPluginPath());
		setPlatformURLString(cSite.getPlatformURLString());
	}

	/*
	 *  Adds a listener
	 */
	public void addConfiguredSiteChangedListener(IConfiguredSiteChangedListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}
	}

	/*
	 * Removes a listener
	 */
	public void removeConfiguredSiteChangedListener(IConfiguredSiteChangedListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}

	/*
	 * @see IWritable#write(int, PrintWriter)
	 */
	public void write(int indent, PrintWriter w) {
		String gap = ""; //$NON-NLS-1$
		for (int i = 0; i < indent; i++)
			gap += " "; //$NON-NLS-1$
		String increment = ""; //$NON-NLS-1$
		for (int i = 0; i < IWritable.INDENT; i++)
			increment += " "; //$NON-NLS-1$

		// CONFIGURATION SITE	
		w.print(gap + "<" + InstallConfigurationParser.CONFIGURATION_SITE + " ");
		//$NON-NLS-1$ //$NON-NLS-2$
		w.println("url=\"" + getSite().getURL().toExternalForm() + "\"");
		//$NON-NLS-1$ //$NON-NLS-2$
		w.println(gap + increment + "platformURL=\"" + getPlatformURLString() + "\"");
		//$NON-NLS-1$ //$NON-NLS-2$
		w.println(gap + increment + "policy=\"" + getConfigurationPolicy().getPolicy() + "\" >");
		//$NON-NLS-1$ //$NON-NLS-2$

		// configured features ref
		IFeatureReference[] featuresReferences = getConfiguredFeatures();
		if (featuresReferences != null) {
			for (int index = 0; index < featuresReferences.length; index++) {
				IFeatureReference element = featuresReferences[index];
				w.print(gap + increment + "<" + InstallConfigurationParser.FEATURE + " ");
				//$NON-NLS-1$ //$NON-NLS-2$
				// configured = true
				w.print("configured = \"true\" "); //$NON-NLS-1$
				// feature URL
				String URLInfoString = null;
				if (element.getURL() != null) {
					ISite featureSite = element.getSite();
					URLInfoString = UpdateManagerUtils.getURLAsString(featureSite.getURL(), element.getURL());
					w.print("url=\"" + Writer.xmlSafe(URLInfoString) + "\" ");
					//$NON-NLS-1$ //$NON-NLS-2$
				}
				w.println("/>"); //$NON-NLS-1$
			}
		}

		// unconfigured features ref
		featuresReferences = getConfigurationPolicy().getUnconfiguredFeatures();
		if (featuresReferences != null) {
			for (int index = 0; index < featuresReferences.length; index++) {
				IFeatureReference element = featuresReferences[index];
				w.print(gap + increment + "<" + InstallConfigurationParser.FEATURE + " ");
				//$NON-NLS-1$ //$NON-NLS-2$
				// configured = true
				w.print("configured = \"false\" "); //$NON-NLS-1$
				// feature URL
				String URLInfoString = null;
				if (element.getURL() != null) {
					ISite featureSite = element.getSite();
					URLInfoString = UpdateManagerUtils.getURLAsString(featureSite.getURL(), element.getURL());
					w.print("url=\"" + Writer.xmlSafe(URLInfoString) + "\" ");
					//$NON-NLS-1$ //$NON-NLS-2$
				}
				w.println("/>"); //$NON-NLS-1$
			}
		}

		// end
		w.println(gap + "</" + InstallConfigurationParser.CONFIGURATION_SITE + ">");
		//$NON-NLS-1$ //$NON-NLS-2$
		w.println(""); //$NON-NLS-1$		
	}

	/*
	 * @see IConfiguredSite#install(IFeature,IVerificationListener, IProgressMonitor)
	 */
	public IFeatureReference install(IFeature feature, IVerificationListener verificationListener, IProgressMonitor monitor) throws InstallAbortedException, CoreException {
		return install(feature,null,verificationListener,monitor);
	}

	/*
	 * @see IConfiguredSite#install(IFeature, IFeatureReference, IVerificationListener, IProgressMonitor)
	 */
	public IFeatureReference install(IFeature feature, IFeatureReference[] optionalFeatures, IVerificationListener verificationListener, IProgressMonitor monitor) throws InstallAbortedException, CoreException {

		// ConfigSite is read only 
		if (!isUpdatable()) {
			String errorMessage = Policy.bind("ConfiguredSite.NonInstallableSite", getSite().getURL().toExternalForm());
			//$NON-NLS-1$
			throw Utilities.newCoreException(errorMessage, null);
		}

		// feature is null
		if (feature == null) {
			String errorMessage = Policy.bind("ConfiguredSite.NullFeatureToInstall");
			//$NON-NLS-1$
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

			if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_INSTALL) {
				UpdateManagerPlugin.debug("Sucessfully installed: " + installedFeatureRef.getURL().toExternalForm());
			}

			if (installedFeatureRef != null) {
				try {
					installedFeature = installedFeatureRef.getFeature();
				} catch (CoreException e) {
					UpdateManagerPlugin.warn(null, e);
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
			configure(installedFeature, optionalFeatures, false);
		/*callInstallHandler*/

		return installedFeatureRef;
	}

	/*
	 * @see IConfiguredSite#remove(IFeature, IProgressMonitor)
	 */
	public void remove(IFeature feature, IProgressMonitor monitor) throws CoreException {

		// ConfigSite is read only
		if (!isUpdatable()) {
			String errorMessage = Policy.bind("ConfiguredSite.NonUninstallableSite", getSite().getURL().toExternalForm());
			//$NON-NLS-1$
			throw Utilities.newCoreException(errorMessage, null);
		}

		// create the Activity
		ConfigurationActivity activity = new ConfigurationActivity(IActivity.ACTION_FEATURE_REMOVE);
		activity.setLabel(feature.getVersionedIdentifier().toString());
		activity.setDate(new Date());

		try {
			IFeatureReference referenceToRemove = null;
			IFeatureReference[] featureRef = getSite().getFeatureReferences();
			IFeatureReference ref = getSite().getFeatureReference(feature);
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
					IFeature featureToRemove = ((IFeatureReference) referenceToRemove).getFeature();
					String featureLabel = (featureToRemove == null) ? null : featureToRemove.getLabel();
					throw Utilities.newCoreException(Policy.bind("ConfiguredSite.UnableToRemoveConfiguredFeature"
					//$NON-NLS-1$
					, featureLabel), null);
				}
			} else {
				throw Utilities.newCoreException(Policy.bind("ConfiguredSite.UnableToFindFeature", feature.getURL().toString()),
				//$NON-NLS-1$
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
			UpdateManagerPlugin.warn("Attempting to configure a null feature in site:" + getSite().getURL().toExternalForm());
			return;
		}

		ConfigurationPolicy configPolicy = getConfigurationPolicy();
		if (configPolicy == null)
			return;

		// bottom up approach, same configuredSite
		IFeatureReference[] childrenRef = feature.getIncludedFeatureReferences();
		if (optionalFeatures!=null){
			childrenRef = childrenToConfigure(childrenRef,optionalFeatures);
		}	
		for (int i = 0; i < childrenRef.length; i++) {
			try {
				IFeature child = childrenRef[i].getFeature();
				configure(child, optionalFeatures, callInstallHandler);
			} catch (CoreException e) {
				// will skip any bad children
				if (!childrenRef[i].isOptional())
					UpdateManagerPlugin.warn("Unable to configure child feature: " + childrenRef[i] + " " + e);
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
	private IFeatureReference[] childrenToConfigure(IFeatureReference[] children, IFeatureReference[] optionalfeatures) {
		 
		List childrenToInstall = new ArrayList(); 
		for (int i = 0; i < children.length; i++) {
			IFeatureReference optionalFeatureToConfigure = children[i];
			if (!optionalFeatureToConfigure.isOptional()){
				childrenToInstall.add(optionalFeatureToConfigure);
			} else {
				for (int j = 0; j < optionalfeatures.length; j++) {
					// must compare feature as optionalFeatures are from the install site
					// where children are on the local site
					try {
						IFeature installedChildren = optionalfeatures[j].getFeature();
						if (installedChildren.equals(optionalFeatureToConfigure.getFeature(true,null))){
							childrenToInstall.add(optionalFeatureToConfigure);
							break;
						}
					} catch (CoreException e){
						UpdateManagerPlugin.warn("",e);
					}
				}
			}
		}
		
		IFeatureReference[] result = new IFeatureReference[childrenToInstall.size()];
		if (childrenToInstall.size()>0){
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
			UpdateManagerPlugin.warn("Unable to retrieve Feature Reference for feature" + feature);
			return false;
		}

		ConfigurationPolicy configPolicy = getConfigurationPolicy();
		if (configPolicy == null)
			return false;

		// verify no enable parent
		if (verifyEnableParent && !validateNoConfiguredParents(feature)){
			UpdateManagerPlugin.warn("The feature "+feature.getVersionedIdentifier()+" to disable is needed by another enable feature");
			return false;
		}		

		boolean sucessfullyUnconfigured = false;
		try {
			sucessfullyUnconfigured = configPolicy.unconfigure(featureReference, true, true);
		} catch (CoreException e) {
			URL url = featureReference.getURL();
			String urlString = (url != null) ? url.toExternalForm() : "<no feature reference url>";
			UpdateManagerPlugin.warn("Unable to unconfigure" + urlString, e);
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
			if (includePatches) unconfigurePatches(feature);
			
			// top down approach, same configuredSite
			IFeatureReference[] childrenRef = feature.getIncludedFeatureReferences();
			for (int i = 0; i < childrenRef.length; i++) {
				try {
					IFeature child = childrenRef[i].getFeature(true,null); // disable the exact feature
					unconfigure(child, includePatches,true);
				} catch (CoreException e) {
					// skip any bad children
					UpdateManagerPlugin.warn("Unable to unconfigure child feature: " + childrenRef[i] + " " + e);
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
			String urlString = (url != null) ? url.toExternalForm() : "<no feature reference url>";
			UpdateManagerPlugin.warn("Unable to unconfigure:" + urlString);
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
		IFeatureReference [] frefs = getConfiguredFeatures();
		for (int i=0; i<frefs.length; i++) {
			IFeatureReference fref = frefs[i];
			try {
				IFeature candidate = fref.getFeature();
				if (candidate.equals(feature)) continue;
				
				IImport [] imports = candidate.getImports();
				for (int j=0; j<imports.length; j++) {
					IImport iimport = imports[j];
					if (iimport.isPatch()) {
						if (iimport.getVersionedIdentifier().equals(feature.getVersionedIdentifier())) {
							// bingo - unconfigure this patch
							unconfigure(candidate, false,false);
							break;
						}
					}
				}
			}
			catch (CoreException e) {
				UpdateManagerPlugin.warn("",e);
			}
		}
	}

	/*
	 * @see IConfiguredSite#getConfiguredFeatures()
	 */
	public IFeatureReference[] getConfiguredFeatures() {
		ConfigurationPolicy configPolicy = getConfigurationPolicy();
		if (configPolicy == null)
			return new IFeatureReference[0];

		return configPolicy.getConfiguredFeatures();
	}

	/*
	 * adds configured and unconfigured feature references
	 */
	public IFeatureReference[] getFeatureReferences() {

		ConfigurationPolicy configPolicy = getConfigurationPolicy();
		if (configPolicy == null)
			return new IFeatureReference[0];

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
				String siteString = (site != null) ? site.getURL().toExternalForm() : Policy.bind("ConfiguredSite.NoSite"); //$NON-NLS-1$
				UpdateManagerPlugin.warn(Policy.bind("ConfiguredSite.CannotFindFeatureToUnconfigure", url, siteString), e); //$NON-NLS-1$ 
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
					feature = configuredFeatures[i].getFeature();
				} catch (CoreException e) {
					// notify we cannot find the feature
					UpdateManagerPlugin.warn(null, e);
					String featureString = configuredFeatures[i].getURL().toExternalForm();
					if (!handler.reportProblem(Policy.bind("ConfiguredSite.CannotFindFeatureToConfigure", featureString))) { //$NON-NLS-1$
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
								String msg = "Error verifying existence of plugin:" + currentFeaturePluginEntry.getVersionedIdentifier().toString();
								//$NON-NLS-1$
								UpdateManagerPlugin.log(msg, new Exception());

								String siteString = (site != null) ? site.getURL().toExternalForm() : Policy.bind("ConfiguredSite.NoSite");
								//$NON-NLS-1$
								String errorLabel = Policy.bind("ConfiguredSite.CannotFindPluginEntry", currentFeaturePluginEntry.getVersionedIdentifier().toString(), siteString);
								//$NON-NLS-1$ //$NON-NLS-2$
								if (handler == null) {
									throw new InterruptedException(errorLabel);
								}
								if (!handler.reportProblem(Policy.bind(errorLabel))) {
									//$NON-NLS-1$ //$NON-NLS-2$
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

		IStatus featureStatus = createStatus(IStatus.OK, IFeature.STATUS_HAPPY, "", null);

		// check the Plugins of all the features
		// every plugin of the feature must be on the site
		IPluginEntry[] siteEntries = getSite().getPluginEntries();
		IPluginEntry[] featuresEntries = feature.getPluginEntries();
		IPluginEntry[] result = UpdateManagerUtils.diff(featuresEntries, siteEntries);
		if (result != null && (result.length != 0)) {
			String msg = Policy.bind("SiteLocal.FeatureUnHappy");
			MultiStatus multi = new MultiStatus(featureStatus.getPlugin(), IFeature.STATUS_UNHAPPY, msg, null);

			for (int k = 0; k < result.length; k++) {
				VersionedIdentifier id = result[k].getVersionedIdentifier();
				Object[] values = new String[] { "", "" };
				if (id != null) {
					values = new Object[] { id.getIdentifier(), id.getVersion()};
				}
				String msg1 = Policy.bind("ConfiguredSite.MissingPluginsBrokenFeature", values);
				//$NON-NLS-1$
				UpdateManagerPlugin.warn(msg1);
				IStatus status = createStatus(IStatus.ERROR, IFeature.STATUS_UNHAPPY, msg1, null);
				multi.add(status);
			}
			return multi;
		}

		String msg = Policy.bind("SiteLocal.FeatureHappy");
		return createStatus(IStatus.OK, IFeature.STATUS_HAPPY, msg, null);
	}

	/*
	 * 
	 */
	public boolean isConfigured(IFeature feature) {
		if (getConfigurationPolicy() == null)
			return false;
		IFeatureReference featureReference = getSite().getFeatureReference(feature);
		if (featureReference == null)
			return false;
		return getConfigurationPolicy().isConfigured(featureReference);
	}

	/**
	 * @see Object#toString()
	 */
	public String toString() {
		if (getSite() == null)
			return "No Site";
		if (getSite().getURL() == null)
			return "No URL";
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
			verifyStatus = createStatus(IStatus.ERROR, Policy.bind("ConfiguredSite.SiteURLNull"), null); //$NON-NLS-1$
			return verifyStatus;
		}

		if (!"file".equalsIgnoreCase(siteURL.getProtocol())) {
			verifyStatus = createStatus(IStatus.ERROR, Policy.bind("ConfiguredSite.NonLocalSite"), null); //$NON-NLS-1$
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
			if (!productName.equals(getProductIdentifier("id", getProductFile()))) {
				verifyStatus = createStatus(IStatus.ERROR, Policy.bind("ConfiguredSite.NotSameProductId", productName), null); //$NON-NLS-1$
				return verifyStatus;
			}
		} else {
			File container = getSiteContaining(file);
			if (container != null) {
				verifyStatus = createStatus(IStatus.ERROR, Policy.bind("ConfiguredSite.ContainedInAnotherSite", container.getAbsolutePath()), null); //$NON-NLS-1$
				return verifyStatus;
			}
		}

		if (!canWrite(file)) {
			verifyStatus = createStatus(IStatus.ERROR, Policy.bind("ConfiguredSite.ReadOnlySite"), null); //$NON-NLS-1$
			return verifyStatus;
		}

		verifyStatus = createStatus(IStatus.OK, "", null);
		isUpdatable(true);
		return verifyStatus;
	}

	/*
	 * Verify we can write on the file system
	 */
	private static boolean canWrite(File file) {
		if (!file.isDirectory() && file.getParentFile() != null) {
			file = file.getParentFile();
		}

		File tryFile = null;
		FileOutputStream out = null;
		try {
			tryFile = new File(file, "toDelete");
			out = new FileOutputStream(tryFile);
			out.write(0);
		} catch (IOException e) {
			return false;
		} finally {
			try {
				out.close();
				tryFile.delete();
			} catch (Exception e) {
			};
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

		UpdateManagerPlugin.warn("IsContained: Checking for markers at:" + file);
		if (file.exists() && file.isDirectory()) {
			File productFile = new File(file, PRODUCT_SITE_MARKER);
			File extensionFile = new File(file, EXTENSION_SITE_MARKER);
			if (productFile.exists() || extensionFile.exists())
				return file;
			// do not check if a marker exists in the current but start from the parent
			// the current is analyze by getProductname()
			if (file.getParentFile() != null) {
				File privateFile = new File(file.getParentFile(), PRIVATE_SITE_MARKER);
				if (privateFile.exists())
					return file.getParentFile();
			}
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

		File markerFile = new File(file, PRIVATE_SITE_MARKER);
		if (!markerFile.exists()) {
			return null;
		}

		File productFile = getProductFile();
		String productId = null;
		String privateId = null;
		if (productFile != null) {
			productId = getProductIdentifier("id", productFile);
			privateId = getProductIdentifier("id", markerFile);
			if (productId == null) {
				UpdateManagerPlugin.warn("Product ID is null at:" + productFile);
				return null;
			}
			if (!productId.equalsIgnoreCase(privateId)) {
				UpdateManagerPlugin.warn("Product id at" + productFile + " Different than:" + markerFile);
				String name = getProductIdentifier("name", markerFile);
				String version = getProductIdentifier("version", markerFile);
				String markerID = (name == null) ? version : name + ":" + version;
				if (markerID == null)
					markerID = "";
				return markerID;
			} else {
				return privateId;
			}
		} else {
			UpdateManagerPlugin.warn("Product Marker doesn't exist:" + productFile);
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
		try {
			InputStream in = new FileInputStream(propertyFile);
			PropertyResourceBundle bundle = new PropertyResourceBundle(in);
			result = bundle.getString(identifier);
		} catch (IOException e) {
			if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_INSTALL)
				UpdateManagerPlugin.debug("Exception reading property file:" + propertyFile);
		} catch (MissingResourceException e) {
			if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_INSTALL)
				UpdateManagerPlugin.debug("Exception reading '" + identifier + "' from property file:" + propertyFile);
		}
		return result;
	}

	/*
	 * Returns the identifier of the product from the property file
	 */
	private static File getProductFile() {

		String productInstallDirectory = BootLoader.getInstallURL().getFile();
		if (productInstallDirectory != null) {
			File productFile = new File(productInstallDirectory, PRODUCT_SITE_MARKER);
			if (productFile.exists()) {
				return productFile;
			} else {
				UpdateManagerPlugin.warn("Product marker doesn't exist:" + productFile);
			}
		} else {
			UpdateManagerPlugin.warn("Cannot retrieve install URL from BootLoader");
		}
		return null;
	}

	/*
	 * 
	 */
	/*package*/
	void createPrivateSiteMarker() {
		URL siteURL = getSite().getURL();
		if (siteURL == null)
			UpdateManagerPlugin.warn("Unable to create marker. The Site url is null.");

		if (!"file".equalsIgnoreCase(siteURL.getProtocol()))
			UpdateManagerPlugin.warn("Unable to create private marker. The Site is not on the local file system.");

		String siteLocation = siteURL.getFile();
		File productFile = getProductFile();
		if (productFile != null) {
			String productId = getProductIdentifier("id", productFile);
			String productName = getProductIdentifier("name", productFile);
			String productVer = getProductIdentifier("version", productFile);
			if (productId != null) {
				File file = new File(siteLocation, PRIVATE_SITE_MARKER);
				if (!file.exists()) {
					PrintWriter w = null;
					try {
						OutputStream out = new FileOutputStream(file);
						OutputStreamWriter outWriter = new OutputStreamWriter(out, "UTF8"); //$NON-NLS-1$
						BufferedWriter buffWriter = new BufferedWriter(outWriter);
						w = new PrintWriter(buffWriter);
						w.println("id=" + productId);
						if (productName != null)
							w.println("name=" + productName);
						if (productVer != null)
							w.println("version=" + productVer);
					} catch (Exception e) {
						UpdateManagerPlugin.warn("Unable to create private Marker at:" + file, e);
					} finally {
						try {
							w.close();
						} catch (Exception e) {
						};
					}
				}
			}
		}
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
		return containsMarker(PRIVATE_SITE_MARKER);
	}

	/*
	 * 
	 */
	private boolean containsMarker(String marker) {
		ISite site = getSite();
		if (site == null) {
			UpdateManagerPlugin.warn("Contains Markers:The site is null");
			return false;
		}

		URL url = site.getURL();
		if (url == null) {
			UpdateManagerPlugin.warn("Contains Markers:Site URL is null");
			return false;
		}
		if (!"file".equalsIgnoreCase(url.getProtocol())) {
			UpdateManagerPlugin.warn("Contains Markers:Non file protocol");
			return false;
		}
		File file = new File(url.getFile());
		if (!file.exists()) {
			UpdateManagerPlugin.warn("Contains Markers:The site doesn't exist:" + file);
			return false;
		}
		File extension = new File(file, marker);
		if (!extension.exists()) {
			UpdateManagerPlugin.warn("Contains Markers:The extensionfile does not exist:" + extension);
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
			UpdateManagerPlugin.warn("Unable to retrieve platformString");
			return false;
		}

		URL siteURL = null;
		try {
			// check if the site exists and is updatable
			// update configSite
			URL urlToCheck = new URL(platformString);
			IPlatformConfiguration runtimeConfig = BootLoader.getCurrentPlatformConfiguration();
			IPlatformConfiguration.ISiteEntry entry = runtimeConfig.findConfiguredSite(urlToCheck);
			if (entry != null) {
				return entry.isNativelyLinked();
			} else {
				UpdateManagerPlugin.warn("Unable to retrieve site:" + platformString + " from platform.");
			}

			// check by comparing URLs
			IPlatformConfiguration.ISiteEntry[] sites = runtimeConfig.getConfiguredSites();
			for (int i = 0; i < sites.length; i++) {
				siteURL = sites[i].getURL();
				URL resolvedURL = Platform.resolve(siteURL);
				if (UpdateManagerUtils.sameURL(resolvedURL, urlToCheck))
					return true;
			}
		} catch (MalformedURLException e) {
			String msg = Policy.bind("ConfiguredSite.UnableResolveURL", platformString);
			throw Utilities.newCoreException(msg, e);
		} catch (IOException e) {
			String msg = Policy.bind("ConfiguredSite.UnableToAccessSite", new Object[] { siteURL });
			throw Utilities.newCoreException(msg, e);
		}

		return false;
	}
	
	
		/*
	 * we have to check that no configured/enable parent include this feature
	 */
	private boolean validateNoConfiguredParents(IFeature feature) throws CoreException {
		if (feature == null) {
			UpdateManagerPlugin.warn("ConfigurationPolicy: validate Feature is null");
			return true;
		}

		IFeatureReference[] parents = UpdateManagerUtils.getParentFeatures(feature, getConfiguredFeatures(), false);
		return (parents.length == 0);
	}
}