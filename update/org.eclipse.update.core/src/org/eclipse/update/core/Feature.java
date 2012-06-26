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
package org.eclipse.update.core;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.update.core.model.ContentEntryModel;
import org.eclipse.update.core.model.FeatureModel;
import org.eclipse.update.core.model.FeatureReferenceModel;
import org.eclipse.update.core.model.ImportModel;
import org.eclipse.update.core.model.InstallAbortedException;
import org.eclipse.update.core.model.NonPluginEntryModel;
import org.eclipse.update.core.model.PluginEntryModel;
import org.eclipse.update.core.model.URLEntryModel;
import org.eclipse.update.internal.core.ErrorRecoveryLog;
import org.eclipse.update.internal.core.InstallHandlerProxy;
import org.eclipse.update.internal.core.InstallRegistry;
import org.eclipse.update.internal.core.Messages;
import org.eclipse.update.internal.core.TargetFeature;
import org.eclipse.update.internal.core.UpdateCore;
import org.eclipse.update.internal.core.UpdateManagerUtils;
import org.eclipse.update.internal.core.UpdateSiteIncludedFeatureReference;

/**
 * Convenience implementation of a feature.
 * <p>
 * This class may be instantiated or subclassed by clients.
 * </p>
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p> 
 * @see org.eclipse.update.core.IFeature
 * @see org.eclipse.update.core.model.FeatureModel
 * @since 2.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public class Feature extends FeatureModel implements IFeature {

	/**
	 * Simple file name of the default feature manifest file
	 * @since 2.0
	 */
	public static final String FEATURE_FILE = "feature"; //$NON-NLS-1$

	/**
	 * File extension of the default feature manifest file
	 * @since 2.0
	 */
	public static final String FEATURE_XML = FEATURE_FILE + ".xml"; //$NON-NLS-1$

	private ISite site; // feature site
	private IFeatureContentProvider featureContentProvider; // content provider
	private List /*of IFeatureReference*/
	includedFeatureReferences;

	//PERF: new instance variable
	private VersionedIdentifier versionId;

	private InstallAbortedException abortedException = null;

	/**
	 * Feature default constructor
	 * 
	 * @since 2.0
	 */
	public Feature() {
	}

	/**
	 * Compares two features for equality
	 * 
	 * @param object feature object to compare with
	 * @return <code>true</code> if the two features are equal, 
	 * <code>false</code> otherwise
	 * @since 2.0
	 */
	public boolean equals(Object object) {
		if (!(object instanceof IFeature))
			return false;
		IFeature f = (IFeature) object;
		return getVersionedIdentifier().equals(f.getVersionedIdentifier());
	}

	/**
	 * Returns the feature identifier.
	 * 
	 * @see IFeature#getVersionedIdentifier()
	 * @since 2.0
	 */
	public VersionedIdentifier getVersionedIdentifier() {
		if (versionId != null)
			return versionId;

		String id = getFeatureIdentifier();
		String ver = getFeatureVersion();
		if (id != null && ver != null) {
			try {
				versionId = new VersionedIdentifier(id, ver);
				return versionId;
			} catch (Exception e) {
				UpdateCore.warn(
					"Unable to create versioned identifier:" + id + ":" + ver); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		versionId = new VersionedIdentifier(getURL().toExternalForm(), null);
		return versionId;
	}

	/**
	 * Returns the site this feature is associated with.
	 * 
	 * @see IFeature#getSite()
	 * @since 2.0
	 */
	public ISite getSite() {
		return site;
	}

	/**
	 * Returns the feature URL.
	 * 
	 * @see IFeature#getURL()
	 * @since 2.0
	 */
	public URL getURL() {
		IFeatureContentProvider contentProvider = null;
		try {
			contentProvider = getFeatureContentProvider();
		} catch (CoreException e) {
			UpdateCore.warn("No content Provider", e); //$NON-NLS-1$
		}
		return (contentProvider != null) ? contentProvider.getURL() : null;
	}

	/**
	 * Returns an information entry referencing the location of the
	 * feature update site. 
	 * 
	 * @see IFeature#getUpdateSiteEntry()
	 * @since 2.0
	 */
	public IURLEntry getUpdateSiteEntry() {
		return (IURLEntry) getUpdateSiteEntryModel();
	}

	/**
	 * Return an array of information entries referencing locations of other
	 * update sites.
	 * 
	 * @see IFeature#getDiscoverySiteEntries()
	 * @since 2.0
	 */
	public IURLEntry[] getDiscoverySiteEntries() {
		URLEntryModel[] result = getDiscoverySiteEntryModels();
		if (result.length == 0)
			return new IURLEntry[0];
		else
			return (IURLEntry[]) result;
	}

	/**
	 * Returns and optional custom install handler entry.
	 * 
	 * @see IFeature#getInstallHandlerEntry()
	 * @since 2.0
	 */
	public IInstallHandlerEntry getInstallHandlerEntry() {
		return (IInstallHandlerEntry) getInstallHandlerModel();
	}

	/**
	 * Returns the feature description.
	 * 
	 * @see IFeature#getDescription()
	 * @since 2.0
	 */
	public IURLEntry getDescription() {
		return (IURLEntry) getDescriptionModel();
	}

	/**
	 * Returns the copyright information for the feature.
	 * 
	 * @see IFeature#getCopyright()
	 * @since 2.0
	 */
	public IURLEntry getCopyright() {
		return (IURLEntry) getCopyrightModel();
	}

	/**
	 * Returns the license information for the feature.
	 * 
	 * @see IFeature#getLicense()
	 * @since 2.0
	 */
	public IURLEntry getLicense() {
		return (IURLEntry) getLicenseModel();
	}

	/**
	 * Return optional image for the feature.
	 * 
	 * @see IFeature#getImage()
	 * @since 2.0
	 */
	public URL getImage() {
		return getImageURL();
	}

	/**
	 * Return a list of plug-in dependencies for this feature.
	 * 
	 * @see IFeature#getRawImports()
	 * @since 2.0
	 */
	public IImport[] getRawImports() {
		ImportModel[] result = getImportModels();
		if (result.length == 0)
			return new IImport[0];
		else
			return (IImport[]) result;
	}

	/**
	 * Install the contents of this feature into the specified target feature.
	 * This method is a reference implementation of the feature installation
	 * protocol. Other concrete feature implementation that override this
	 * method need to implement this protocol.
	 * 
	 * @see IFeature#install(IFeature, IVerificationListener, IProgressMonitor)
	 * @since 2.0
	 */
	public IFeatureReference install(
		IFeature targetFeature,
		IVerificationListener verificationListener,
		IProgressMonitor progress)
		throws InstallAbortedException, CoreException {
		// call other API with all optional features, or setup variable meaning install all
		return install(targetFeature, null, verificationListener, progress);
	}

	/**
	 * Install the contents of this feature into the specified target feature.
	 * This method is a reference implementation of the feature installation
	 * protocol. Other concrete feature implementation that override this
	 * method need to implement this protocol.
	 * 
	 * @see IFeature#install(IFeature, IVerificationListener, IProgressMonitor)
	 * @since 2.0
	 */
	public IFeatureReference install(
		IFeature targetFeature,
		IFeatureReference[] optionalfeatures,
		IVerificationListener verificationListener,
		IProgressMonitor progress)
		throws InstallAbortedException, CoreException {

		//DEBUG
		debug("Installing...:" + getURL().toExternalForm()); //$NON-NLS-1$
		ErrorRecoveryLog recoveryLog = ErrorRecoveryLog.getLog();

		// make sure we have an InstallMonitor		
		InstallMonitor monitor;
		if (progress == null)
			monitor = new InstallMonitor(new NullProgressMonitor());
		else if (progress instanceof InstallMonitor)
			monitor = (InstallMonitor) progress;
		else
			monitor = new InstallMonitor(progress);

		// Setup optional install handler
		InstallHandlerProxy handler =
			new InstallHandlerProxy(
				IInstallHandler.HANDLER_ACTION_INSTALL,
				this,
				this.getInstallHandlerEntry(),
				monitor);
		boolean success = false;
		Throwable originalException = null;
		abortedException = null;

		// Get source feature provider and verifier.
		// Initialize target variables.
		IFeatureContentProvider provider = getFeatureContentProvider();
		IVerifier verifier = provider.getVerifier();
		IFeatureReference result = null;
		IFeatureReference alreadyInstalledFeature = null;
		IFeatureContentConsumer consumer = null;
		IPluginEntry[] targetSitePluginEntries = null;
		ArrayList justInstalledPlugins = new ArrayList();

		try {
			// determine list of plugins to install
			// find the intersection between the plugin entries already contained
			// on the target site, and plugin entries packaged in source feature
			IPluginEntry[] sourceFeaturePluginEntries = getPluginEntries();
			ISite targetSite = targetFeature.getSite();
			if (targetSite == null) {
				debug("The site to install in is null"); //$NON-NLS-1$
				targetSitePluginEntries = new IPluginEntry[0];
			} else {
				targetSitePluginEntries = targetSite.getPluginEntries();
			}
			IPluginEntry[] pluginsToInstall =
				UpdateManagerUtils.diff(
					sourceFeaturePluginEntries,
					targetSitePluginEntries);
			INonPluginEntry[] nonPluginsToInstall = getNonPluginEntries();

			IFeatureReference[] children = getIncludedFeatureReferences();
			if (optionalfeatures != null) {
				children =
					UpdateManagerUtils.optionalChildrenToInstall(
						children,
						optionalfeatures);
			}

			// determine number of monitor tasks
			//   2 tasks for the feature jar (download/verify + install)
			// + 2*n tasks for plugin entries (download/verify + install for each)
			// + 1*m tasks per non-plugin data entry (download for each)
			// + 1 task for custom non-plugin entry handling (1 for all combined)
			// + 5*x tasks for children features (5 subtasks per install)
			int taskCount =
				2
					+ 2 * pluginsToInstall.length
					+ nonPluginsToInstall.length
					+ 1
					+ 5 * children.length;
			monitor.beginTask("", taskCount); //$NON-NLS-1$
			SubProgressMonitor subMonitor = null;

			// start log
			recoveryLog.open(ErrorRecoveryLog.START_INSTALL_LOG);

			// Start the installation tasks			
			handler.installInitiated();

			// Download and verify feature archive(s)
			ContentReference[] references =
				provider.getFeatureEntryArchiveReferences(monitor);
			verifyReferences(
				verifier,
				references,
				monitor,
				verificationListener,
				true);
			monitorWork(monitor, 1);
			
			// Download and verify plugin archives
			for (int i = 0; i < pluginsToInstall.length; i++) {
				references = provider.getPluginEntryArchiveReferences(
						pluginsToInstall[i], monitor);
				verifyReferences(verifier, references, monitor,
								verificationListener, false);
				monitorWork(monitor, 1);
			}
			
			handler.pluginsDownloaded(pluginsToInstall);

			Vector filteredPlugins = new Vector();
            // Download non-plugin archives. Verification handled by optional
            // install handler
            for (int i = 0; i < nonPluginsToInstall.length; i++) {
                if (handler.acceptNonPluginData(nonPluginsToInstall[i])) {
                    references = provider.getNonPluginEntryArchiveReferences(
                            nonPluginsToInstall[i], monitor);
                    monitorWork(monitor, 1);
                    filteredPlugins.add(nonPluginsToInstall[i]);
                }
            }
            nonPluginsToInstall = (INonPluginEntry[]) filteredPlugins
                    .toArray(new INonPluginEntry[0]);
            handler.nonPluginDataDownloaded(nonPluginsToInstall,
                    verificationListener);

			// All archives are downloaded and verified. Get ready to install
			consumer = targetFeature.getFeatureContentConsumer();

			// install the children feature
			// check if they are optional, and if they should be installed [2.0.1]
			for (int i = 0; i < children.length; i++) {
				IFeature childFeature = null;
				try {
					childFeature = children[i].getFeature(null);
				} catch (CoreException e) {
					UpdateCore.warn(null, e);
				}
				if (childFeature != null) {
					subMonitor = new SubProgressMonitor(monitor, 5);
					((Site) targetSite).install(// need to cast
					childFeature,
						optionalfeatures,
						consumer,
						verifier,
						verificationListener,
						subMonitor);
				}
			}

			// Install plugin files
			for (int i = 0; i < pluginsToInstall.length; i++) {
				// if another feature has already installed this plugin, skip it
				if (InstallRegistry.getInstance().isPluginJustInstalled(pluginsToInstall[i])) {
					monitor.worked(1);
					continue;
				}
				IContentConsumer pluginConsumer =
					consumer.open(pluginsToInstall[i]);
				// TODO consumer.open returns either
				// SiteFilePackedPluginContentConsumer or SiteFilePluginContentConsumer
				// and they are fed either
				// PluginEntryArchiveReference or PluginEntryContentReferences
				// it would be better to have one that is given PluginEntryArchiveReference
				// but it would break external IContentConsumers

				if(pluginsToInstall[i] instanceof PluginEntryModel && !((PluginEntryModel)pluginsToInstall[i]).isUnpack()){
					// plugin can run from a jar
					references = provider.getPluginEntryArchiveReferences(
							pluginsToInstall[i], monitor);
				} else{
					// plugin must be unpacked
					references =
						provider.getPluginEntryContentReferences(
							pluginsToInstall[i],
							monitor);
				}

				String msg = ""; //$NON-NLS-1$
				subMonitor = new SubProgressMonitor(monitor, 1);
				VersionedIdentifier pluginVerId =
					pluginsToInstall[i].getVersionedIdentifier();
				String pluginID =
					(pluginVerId == null) ? "" : pluginVerId.getIdentifier(); //$NON-NLS-1$
				msg = NLS.bind(Messages.Feature_TaskInstallPluginFiles, (new String[] { pluginID }));

				for (int j = 0; j < references.length; j++) {
					setMonitorTaskName(
						subMonitor,
						msg + references[j].getIdentifier());
					pluginConsumer.store(references[j], subMonitor);
				}

				if (monitor.isCanceled())
					abort();
				else {
					justInstalledPlugins.add(pluginsToInstall[i]);
					InstallRegistry.registerPlugin(pluginsToInstall[i]);
				}
			}

			// check if we need to install feature files [16718]	
			// store will throw CoreException if another feature is already
			// installed in the same place
			alreadyInstalledFeature = featureAlreadyInstalled(targetSite);
			// 18867
			if (alreadyInstalledFeature == null) {
				//Install feature files
				references = provider.getFeatureEntryContentReferences(monitor);

				String msg = ""; //$NON-NLS-1$
				subMonitor = new SubProgressMonitor(monitor, 1);
				msg = Messages.Feature_TaskInstallFeatureFiles; 

				for (int i = 0; i < references.length; i++) {
					setMonitorTaskName(
						subMonitor,
						msg + " " + references[i].getIdentifier()); //$NON-NLS-1$
					consumer.store(references[i], subMonitor);
				}

				if (monitor.isCanceled())
					abort();
				else
					InstallRegistry.registerFeature(this);
			} else {
				if (monitor.isCanceled())
					abort();
				else
					monitor.worked(1);
			}

			// call handler to complete installation (eg. handle non-plugin entries)
			handler.completeInstall(consumer);
			monitorWork(monitor, 1);

			// indicate install success
			success = true;

		} catch (InstallAbortedException e) {
			abortedException = e;
		} catch (CoreException e) {
			originalException = e;
		} finally {
			Exception newException = null;
			try {
				if (consumer != null) {
					if (success) {
						result = consumer.close();
						if (result == null) {
							result = alreadyInstalledFeature; // 18867
							if (result != null
								&& optionalfeatures != null
								&& optionalfeatures.length > 0) {
								// reinitialize as new optional children may have been installed
								reinitializeFeature(result);
							}
						}
						// close the log
						recoveryLog.close(ErrorRecoveryLog.END_INSTALL_LOG);
					} else {
						// unregister the just installed plugins
						for (int i=0; i<justInstalledPlugins.size(); i++)
							InstallRegistry.unregisterPlugin(((IPluginEntry)justInstalledPlugins.get(i)));
						consumer.abort();
					}
				}
				handler.installCompleted(success);
				// if abort is done, no need for the log to stay
				recoveryLog.delete();
			} catch (CoreException e) {
				newException = e;
			}

			// original exception wins unless it is InstallAbortedException
			// and an error occured during abort
			if (originalException != null) {
				throw Utilities.newCoreException(
					NLS.bind(Messages.InstallHandler_error, (new String[] { this.getLabel() })),
					originalException);
			}

			if (newException != null)
				throw Utilities.newCoreException(
					NLS.bind(Messages.InstallHandler_error, (new String[] { this.getLabel() })),
					newException);

			if (abortedException != null) {
				throw abortedException;
			}

		}
		return result;
	}

	/**
	 * Returns an array of plug-in entries referenced by this feature
	 * 
	 * @see IFeature#getPluginEntries()
	 * @since 2.0
	 */
	public IPluginEntry[] getRawPluginEntries() {
		PluginEntryModel[] result = getPluginEntryModels();
		if (result.length == 0)
			return new IPluginEntry[0];
		else
			return (IPluginEntry[]) result;
	}

	/*
	 * Method filter.
	 * @param result
	 * @return IPluginEntry[]
	 */
	private IPluginEntry[] filterPluginEntry(IPluginEntry[] all) {
		List list = new ArrayList();
		if (all != null) {
			for (int i = 0; i < all.length; i++) {
				if (UpdateManagerUtils.isValidEnvironment(all[i]))
					list.add(all[i]);
			}
		}

		IPluginEntry[] result = new IPluginEntry[list.size()];
		if (!list.isEmpty()) {
			list.toArray(result);
		}

		return result;
	}

	/**
	 * Returns the count of referenced plug-in entries.
	 * 
	 * @see IFeature#getPluginEntryCount()
	 * @since 2.0
	 */
	public int getPluginEntryCount() {
		return getPluginEntries().length;
	}

	/**
	 * Returns an array of non-plug-in entries referenced by this feature
	 * 
	 * @see IFeature#getNonPluginEntries()
	 * @since 2.0
	 */
	public INonPluginEntry[] getRawNonPluginEntries() {
		NonPluginEntryModel[] result = getNonPluginEntryModels();
		if (result.length == 0)
			return new INonPluginEntry[0];
		else
			return (INonPluginEntry[]) result;
	}

	/**
	 * Returns the count of referenced non-plug-in entries.
	 * 
	 * @see IFeature#getNonPluginEntryCount()
	 * @since 2.0
	 */
	public int getNonPluginEntryCount() {
		return getNonPluginEntryModels().length;
	}

	/**
	 * Returns an array of feature references included by this feature
	 * 
	 * @return an erray of feature references, or an empty array.
	 * @since 2.0
	 */
	public IIncludedFeatureReference[] getRawIncludedFeatureReferences()
		throws CoreException {
		if (includedFeatureReferences == null)
			initializeIncludedReferences();

		if (includedFeatureReferences.size() == 0)
			return new IncludedFeatureReference[0];

		return (IIncludedFeatureReference[]) includedFeatureReferences.toArray(
			new IIncludedFeatureReference[includedFeatureReferences.size()]);
	}
	/**
	 * Returns the download size of the feature, if it can be determined.
	 * 
	 * @see IFeature#getDownloadSize()
	 * @since 2.0
	 */
	public long getDownloadSize() {
		try {
			Set allPluginEntries = new HashSet();
			Set allNonPluginEntries = new HashSet();

			IPluginEntry[] plugins = getPluginEntries();
			allPluginEntries.addAll(Arrays.asList(plugins));
			INonPluginEntry[] nonPlugins = getNonPluginEntries();
			allNonPluginEntries.addAll(Arrays.asList(nonPlugins));

			IFeatureReference[] children = getIncludedFeatureReferences();
			for (int i = 0; i < children.length; i++) {
				plugins = children[i].getFeature(null).getPluginEntries();
				allPluginEntries.addAll(Arrays.asList(plugins));
				nonPlugins = children[i].getFeature(null).getNonPluginEntries();
				allNonPluginEntries.addAll(Arrays.asList(nonPlugins));
			}

			IPluginEntry[] totalPlugins =
				new IPluginEntry[allPluginEntries.size()];
			INonPluginEntry[] totalNonPlugins =
				new INonPluginEntry[allNonPluginEntries.size()];
			if (allPluginEntries.size() != 0) {
				allPluginEntries.toArray(totalPlugins);
			}
			if (allNonPluginEntries.size() != 0) {
				allNonPluginEntries.toArray(totalNonPlugins);
			}

			return getFeatureContentProvider().getDownloadSizeFor(
				totalPlugins,
				totalNonPlugins);

		} catch (CoreException e) {
			UpdateCore.warn(null, e);
			return ContentEntryModel.UNKNOWN_SIZE;
		}
	}

	/**
	 * Returns the install size of the feature, if it can be determined.
	 * 
	 * @see IFeature#getInstallSize()
	 * @since 2.0
	 */
	public long getInstallSize() {
		try {
			Set allPluginEntries = new HashSet();
			Set allNonPluginEntries = new HashSet();

			IPluginEntry[] plugins = getPluginEntries();
			allPluginEntries.addAll(Arrays.asList(plugins));
			INonPluginEntry[] nonPlugins = getNonPluginEntries();
			allNonPluginEntries.addAll(Arrays.asList(nonPlugins));

			IFeatureReference[] children = getIncludedFeatureReferences();
			for (int i = 0; i < children.length; i++) {
				plugins = children[i].getFeature(null).getPluginEntries();
				allPluginEntries.addAll(Arrays.asList(plugins));
				nonPlugins = children[i].getFeature(null).getNonPluginEntries();
				allNonPluginEntries.addAll(Arrays.asList(nonPlugins));
			}

			IPluginEntry[] totalPlugins =
				new IPluginEntry[allPluginEntries.size()];
			INonPluginEntry[] totalNonPlugins =
				new INonPluginEntry[allNonPluginEntries.size()];
			if (allPluginEntries.size() != 0) {
				allPluginEntries.toArray(totalPlugins);
			}
			if (allNonPluginEntries.size() != 0) {
				allNonPluginEntries.toArray(totalNonPlugins);
			}

			return getFeatureContentProvider().getInstallSizeFor(
				totalPlugins,
				totalNonPlugins);

		} catch (CoreException e) {
			UpdateCore.warn(null, e);
			return ContentEntryModel.UNKNOWN_SIZE;
		}
	}

	/**
	 * Returns the content provider for this feature.
	 * 
	 * @see IFeature#getFeatureContentProvider()
	 * @since 2.0
	 */
	public IFeatureContentProvider getFeatureContentProvider()
		throws CoreException {
		if (featureContentProvider == null) {
			throw Utilities.newCoreException(
				NLS.bind(Messages.Feature_NoContentProvider, (new String[] { getVersionedIdentifier().toString() })),
				null);	
		}
		return this.featureContentProvider;
	}

	/**
	 * Returns the content consumer for this feature.
	 * 
	 * @see IFeature#getFeatureContentConsumer()
	 * @since 2.0
	 */
	public IFeatureContentConsumer getFeatureContentConsumer()
		throws CoreException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Sets the site for this feature.
	 * 
	 * @see IFeature#setSite(ISite)
	 * @since 2.0
	 */
	public void setSite(ISite site) throws CoreException {
		if (this.site != null) {
			String featureURLString =
				(getURL() != null) ? getURL().toExternalForm() : ""; //$NON-NLS-1$
			throw Utilities.newCoreException(
				NLS.bind(Messages.Feature_SiteAlreadySet, (new String[] { featureURLString })),
				null);
		}
		this.site = site;
	}

	/**
	 * Sets the content provider for this feature.
	 * 
	 * @see IFeature#setFeatureContentProvider(IFeatureContentProvider)
	 * @since 2.0
	 */
	public void setFeatureContentProvider(IFeatureContentProvider featureContentProvider) {
		this.featureContentProvider = featureContentProvider;
		featureContentProvider.setFeature(this);
	}

	/**
	 * Return the string representation of this fetaure
	 * 
	 * @return feature as string
	 * @since 2.0
	 */
	public String toString() {
		String URLString =
			(getURL() == null)
				? Messages.Feature_NoURL
				: getURL().toExternalForm();

		String verString =
			NLS.bind(Messages.Feature_FeatureVersionToString, (new String[] { URLString, getVersionedIdentifier().toString() }));
		String label = getLabel() == null ? "" : getLabel(); //$NON-NLS-1$
		return verString + " [" + label + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/*
	 * Installation has been cancelled, abort and revert
	 */
	private void abort() throws CoreException {
		String msg = Messages.Feature_InstallationCancelled; 
		throw new InstallAbortedException(msg, null);
	}

	/*
	 * Initializes includes feature references
	 * If the included feature reference is found on the site, add it to the List
	 * Otherwise attempt to instanciate it using the same type as this feature and
	 * using the default location on the site.
	 */
	private void initializeIncludedReferences() throws CoreException {
		includedFeatureReferences = new ArrayList();

		IIncludedFeatureReference[] nestedFeatures = getFeatureIncluded();
		if (nestedFeatures.length == 0)
			return;

		ISite site = getSite();
		if (site == null)
			return;

		for (int i = 0; i < nestedFeatures.length; i++) {
			IIncludedFeatureReference include = nestedFeatures[i];
			IIncludedFeatureReference newRef =
				getPerfectIncludeFeature(site, include);
			includedFeatureReferences.add(newRef);
		}
	}

	/*
	 * 
	 */
	private IIncludedFeatureReference getPerfectIncludeFeature(
		ISite site,
		IIncludedFeatureReference include)
		throws CoreException {

		// [20367] no site, cannot initialize nested references
		ISiteFeatureReference[] refs = site.getFeatureReferences();
		VersionedIdentifier identifier = include.getVersionedIdentifier();

		// too long to compute if not a file system
		// other solution would be to parse feature.xml
		// when parsing file system to create archive features/FeatureId_Ver.jar
		if ("file".equals(site.getURL().getProtocol())) { //$NON-NLS-1$
			// check if declared on the Site
			if (refs != null) {
				for (int ref = 0; ref < refs.length; ref++) {
					if (refs[ref] != null) {
						VersionedIdentifier id =
							refs[ref].getVersionedIdentifier();
						if (identifier.equals(id)) {
							// found a ISiteFeatureReference that matches our IIncludedFeatureReference
							IncludedFeatureReference newRef =
								new IncludedFeatureReference(refs[ref]);
							newRef.isOptional(include.isOptional());
							if (include instanceof FeatureReferenceModel)
								newRef.setLabel(
									((FeatureReferenceModel) include)
										.getLabel());
							newRef.setSearchLocation(
								include.getSearchLocation());
							return newRef;
						}
					}
				}
			}
		}

        // instantiate by mapping it based on the site.xml
        // in future we may ask for a factory to create the feature ref
        IncludedFeatureReference newRef = new UpdateSiteIncludedFeatureReference(include);
        newRef.setSite(getSite());
        IFeatureReference parentRef = getSite().getFeatureReference(this);
        if (parentRef instanceof FeatureReference) {
         newRef.setType(((FeatureReference) parentRef).getType());
        }
        String featureID = Site.DEFAULT_FEATURE_PATH + identifier.toString();
        if(this instanceof TargetFeature)
         featureID = featureID + "/"; //$NON-NLS-1$
        else
         featureID = featureID + ".jar"; //$NON-NLS-1$
		URL featureURL =
			getSite().getSiteContentProvider().getArchiveReference(featureID);
		newRef.setURL(featureURL);
		newRef.setFeatureIdentifier(identifier.getIdentifier());
		newRef.setFeatureVersion(identifier.getVersion().toString());
		try {
			newRef.resolve(getSite().getURL(), null);
			// no need to get the bundle
			return newRef;
		} catch (Exception e) {
			throw Utilities.newCoreException(
				NLS.bind(Messages.Feature_UnableToInitializeFeatureReference, (new String[] { identifier.toString() })),
				e);
		}
	}

	/*
	 * 
	 */
	private void debug(String trace) {
		//DEBUG
		if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_INSTALL) {
			UpdateCore.debug(trace);
		}
	}

	/*
	 * 
	 */
	private void setMonitorTaskName(
		IProgressMonitor monitor,
		String taskName) {
		if (monitor != null)
			monitor.setTaskName(taskName);
	}

	/*
	 *
	 */
	private void monitorWork(IProgressMonitor monitor, int tick)
		throws CoreException {
		if (monitor != null) {
			monitor.worked(tick);
			if (monitor.isCanceled()) {
				abort();
			}
		}
	}

	/*
	 * 
	 */
	private void verifyReferences(
		IVerifier verifier,
		ContentReference[] references,
		InstallMonitor monitor,
		IVerificationListener verificationListener,
		boolean isFeature)
		throws CoreException {
		IVerificationResult vr = null;
		if (verifier != null) {
			for (int j = 0; j < references.length; j++) {
				vr = verifier.verify(this, references[j], isFeature, monitor);
				if (vr != null) {
					if (verificationListener == null)
						return;

					int result = verificationListener.prompt(vr);

					if (result == IVerificationListener.CHOICE_ABORT) {
						String msg = Messages.JarVerificationService_CancelInstall; 
						Exception e = vr.getVerificationException();
						throw new InstallAbortedException(msg, e);
					}
					if (result == IVerificationListener.CHOICE_ERROR) {
						throw Utilities
							.newCoreException(
								Messages.JarVerificationService_UnsucessfulVerification,	
						vr.getVerificationException());
					}
				}
			}
		}
	}

	/*
	 * returns reference if the same feature is installed on the site
	 * [18867]
	 */
	private IFeatureReference featureAlreadyInstalled(ISite targetSite) {

		ISiteFeatureReference[] references = targetSite.getFeatureReferences();
		IFeatureReference currentReference = null;
		for (int i = 0; i < references.length; i++) {
			currentReference = references[i];
			// do not compare URL
			try {
				if (this.equals(currentReference.getFeature(null)))
					return currentReference; // 18867
			} catch (CoreException e) {
				UpdateCore.warn(null, e);
			}
		}

		UpdateCore.warn(
			"ValidateAlreadyInstalled:Feature " //$NON-NLS-1$
				+ this
				+ " not found on site:" //$NON-NLS-1$
				+ this.getURL());
		return null;
	}

	/*
	 * re initialize children of the feature, invalidate the cache
	 * @param result FeatureReference to reinitialize.
	 */
	private void reinitializeFeature(IFeatureReference referenceToReinitialize) {

		if (referenceToReinitialize == null)
			return;

		if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_CONFIGURATION)
			UpdateCore.debug(
				"Re initialize feature reference:" + referenceToReinitialize); //$NON-NLS-1$

		IFeature feature = null;
		try {
			feature = referenceToReinitialize.getFeature(null);
			if (feature != null && feature instanceof Feature) {
				((Feature) feature).initializeIncludedReferences();
			}
			// bug 24981 - recursively go into hierarchy
			// only if site if file 
			ISite site = referenceToReinitialize.getSite();
			if (site == null)
				return;
			URL url = site.getURL();
			if (url == null)
				return;
			if ("file".equals(url.getProtocol())) { //$NON-NLS-1$
				IFeatureReference[] included =
					feature.getIncludedFeatureReferences();
				for (int i = 0; i < included.length; i++) {
					reinitializeFeature(included[i]);
				}
			}
		} catch (CoreException e) {
			UpdateCore.warn("", e); //$NON-NLS-1$
		}
	}

	/**
	 * @see org.eclipse.update.core.IFeature#getRawIncludedFeatureReferences()
	 */
	public IIncludedFeatureReference[] getIncludedFeatureReferences()
		throws CoreException {
		return filterFeatures(getRawIncludedFeatureReferences());
	}

	/*
	 * Method filterFeatures.
	 * Also implemented in Site
	 * 
	 * @param list
	 * @return List
	 */
	private IIncludedFeatureReference[] filterFeatures(IIncludedFeatureReference[] allIncluded) {
		List list = new ArrayList();
		if (allIncluded != null) {
			for (int i = 0; i < allIncluded.length; i++) {
				IIncludedFeatureReference included = allIncluded[i];
				if (UpdateManagerUtils.isValidEnvironment(included))
					list.add(included);
				else {
					if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_WARNINGS) {
						UpdateCore.warn(
							"Filtered out feature reference:" + included); //$NON-NLS-1$
					}
				}
			}
		}

		IIncludedFeatureReference[] result =
			new IIncludedFeatureReference[list.size()];
		if (!list.isEmpty()) {
			list.toArray(result);
		}

		return result;
	}

	/**
	 * @see org.eclipse.update.core.IFeature#getRawNonPluginEntries()
	 */
	public INonPluginEntry[] getNonPluginEntries() {
		return filterNonPluginEntry(getRawNonPluginEntries());
	}

	/**
	 * Method filterPluginEntry.
	 * @param all
	 * @return INonPluginEntry[]
	 */
	private INonPluginEntry[] filterNonPluginEntry(INonPluginEntry[] all) {
		List list = new ArrayList();
		if (all != null) {
			for (int i = 0; i < all.length; i++) {
				if (UpdateManagerUtils.isValidEnvironment(all[i]))
					list.add(all[i]);
			}
		}

		INonPluginEntry[] result = new INonPluginEntry[list.size()];
		if (!list.isEmpty()) {
			list.toArray(result);
		}

		return result;
	}

	/**
	 * @see org.eclipse.update.core.IFeature#getRawPluginEntries()
	 */
	public IPluginEntry[] getPluginEntries() {
		return filterPluginEntry(getRawPluginEntries());
	}

	/**
	 * @see org.eclipse.update.core.IFeature#getImports()
	 */
	public IImport[] getImports() {
		return filterImports(getRawImports());
	}

	/**
	 * Method filterImports.
	 * @param all
	 * @return IImport[]
	 */
	private IImport[] filterImports(IImport[] all) {
		List list = new ArrayList();
		if (all != null) {
			for (int i = 0; i < all.length; i++) {
				if (UpdateManagerUtils.isValidEnvironment(all[i]))
					list.add(all[i]);
			}
		}

		IImport[] result = new IImport[list.size()];
		if (!list.isEmpty()) {
			list.toArray(result);
		}

		return result;
	}

}