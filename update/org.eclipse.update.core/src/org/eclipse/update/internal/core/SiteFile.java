/*******************************************************************************
 *  Copyright (c) 2000, 2012 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.update.core.ContentReference;
import org.eclipse.update.core.IFeature;
import org.eclipse.update.core.IFeatureContentConsumer;
import org.eclipse.update.core.IFeatureFactory;
import org.eclipse.update.core.IFeatureReference;
import org.eclipse.update.core.IInstallHandler;
import org.eclipse.update.core.INonPluginEntry;
import org.eclipse.update.core.IPluginEntry;
import org.eclipse.update.core.ISite;
import org.eclipse.update.core.ISiteFeatureReference;
import org.eclipse.update.core.IVerificationListener;
import org.eclipse.update.core.IVerifier;
import org.eclipse.update.core.InstallMonitor;
import org.eclipse.update.core.Site;
import org.eclipse.update.core.Utilities;
import org.eclipse.update.core.model.ContentEntryModel;
import org.eclipse.update.core.model.FeatureModel;
import org.eclipse.update.core.model.FeatureReferenceModel;
import org.eclipse.update.core.model.InstallAbortedException;
import org.eclipse.update.internal.operations.UpdateUtils;


/**
 * Site on the File System
 */
public class SiteFile extends Site {

	/**
	 * plugin entries 
	 */
	private List pluginEntries = new ArrayList(0);

	/**
	 * 
	 */
	public ISiteContentConsumer createSiteContentConsumer(IFeature targetFeature) throws CoreException {
		SiteFileContentConsumer consumer = new SiteFileContentConsumer(targetFeature);
		consumer.setSite(this);
		return consumer;
	}

	/**
	 */
	public String getDefaultPackagedFeatureType() {
		return DEFAULT_INSTALLED_FEATURE_TYPE;
	}

	/*
	 * @see ISite#install(IFeature, IVerifier, IProgressMonitor)
	 */
	public IFeatureReference install(IFeature sourceFeature, IVerificationListener verificationListener, IProgressMonitor progress) throws CoreException {
		return install(sourceFeature,null,verificationListener,progress);
	}

	/*
	 * @see ISite#install(IFeature, IVerifier, IProgressMonitor)
	 */
	public IFeatureReference install(IFeature sourceFeature, IFeatureReference[] optionalfeatures, IVerificationListener verificationListener, IProgressMonitor progress) throws CoreException {

		if (sourceFeature == null)
			return null;

		// make sure we have an InstallMonitor		
		InstallMonitor monitor;
		if (progress == null)
			monitor = null;
		else if (progress instanceof InstallMonitor)
			monitor = (InstallMonitor) progress;
		else
			monitor = new InstallMonitor(progress);

		// create new executable feature and install source content into it
		IFeature localFeature = createExecutableFeature(sourceFeature);

		IFeatureReference localFeatureReference = null;
		localFeatureReference = sourceFeature.install(localFeature, optionalfeatures, verificationListener, monitor);

		return localFeatureReference;
	}

	/*
	 * @see ISite#install(IFeature,IFeatureContentConsumer, IVerifier,IVerificationLIstener, IProgressMonitor)
	 */
	public IFeatureReference install(IFeature sourceFeature, IFeatureReference[] optionalfeatures, IFeatureContentConsumer parentContentConsumer, IVerifier parentVerifier, IVerificationListener verificationListener, IProgressMonitor progress)
		throws InstallAbortedException, CoreException {

		if (sourceFeature == null)
			return null;

		// make sure we have an InstallMonitor		
		InstallMonitor monitor;
		if (progress == null)
			monitor = null;
		else if (progress instanceof InstallMonitor)
			monitor = (InstallMonitor) progress;
		else
			monitor = new InstallMonitor(progress);

		// create new executable feature and install source content into it
		IFeature localFeature = createExecutableFeature(sourceFeature);
		parentContentConsumer.addChild(localFeature);

		// set the verifier
		IVerifier vr = sourceFeature.getFeatureContentProvider().getVerifier();
		if (vr != null)
			vr.setParent(parentVerifier);

		IFeatureReference localFeatureReference = null;
		localFeatureReference = sourceFeature.install(localFeature, optionalfeatures, verificationListener, monitor);

		return localFeatureReference;
	}

	/*
	 * @see ISite#remove(IFeature, IProgressMonitor)
	 */
	public void remove(IFeature feature, IProgressMonitor progress) throws CoreException {

		if (feature == null) {
			UpdateCore.warn("Feature to remove is null"); //$NON-NLS-1$
			return;
		}
		
		ErrorRecoveryLog recoveryLog = ErrorRecoveryLog.getLog();

		// make sure we have an InstallMonitor		
		InstallMonitor monitor;
		if (progress == null)
			monitor = null;
		else if (progress instanceof InstallMonitor)
			monitor = (InstallMonitor) progress;
		else
			monitor = new InstallMonitor(progress);

		// Setup optional install handler
		InstallHandlerProxy handler = new InstallHandlerProxy(IInstallHandler.HANDLER_ACTION_UNINSTALL, feature, feature.getInstallHandlerEntry(), monitor);
		boolean success = false;
		Throwable originalException = null;

		try {

			// start log
			recoveryLog.open(ErrorRecoveryLog.START_REMOVE_LOG);

			// log files have been downloaded
			recoveryLog.append(ErrorRecoveryLog.END_ABOUT_REMOVE);

			handler.uninstallInitiated();

			// remove the feature and the plugins if they are not used and not activated
			// get the plugins from the feature
			IPluginEntry[] pluginsToRemove = getPluginEntriesOnlyReferencedBy(feature);

			if (monitor != null) {
				monitor.beginTask(Messages.SiteFile_Removing + feature.getLabel(), pluginsToRemove.length + 1);	
			}

			// remove feature reference from the site
			ISiteFeatureReference[] featureReferences = getFeatureReferences();
			if (featureReferences != null) {
				for (int indexRef = 0; indexRef < featureReferences.length; indexRef++) {
					IFeatureReference element = featureReferences[indexRef];
					if (element.getVersionedIdentifier().equals(feature.getVersionedIdentifier())) {
						removeFeatureReferenceModel((FeatureReferenceModel) element);
						break;
					}
				}
			}

			if (InstallRegistry.getInstance().get("feature_"+feature.getVersionedIdentifier()) == null) { //$NON-NLS-1$
				UpdateCore.log(NLS.bind(Messages.SiteFile_featureNotRemoved, (new String[] { feature.getVersionedIdentifier().toString() })), null);
			} else {
				// remove the feature content
				ContentReference[] references = feature.getFeatureContentProvider().getFeatureEntryArchiveReferences(monitor);
				for (int i = 0; i < references.length; i++) {
					try {
						UpdateManagerUtils.removeFromFileSystem(references[i].asFile());
						if (monitor != null)
							monitor.worked(1);
					} catch (IOException e) {
						throw Utilities.newCoreException(NLS.bind(Messages.SiteFile_CannotRemoveFeature, (new String[] { feature.getVersionedIdentifier().getIdentifier(), getURL().toExternalForm() })), e);
					}
				}
				InstallRegistry.unregisterFeature(feature);
			}

			//finds the contentReferences for an IPluginEntry
			// and remove it
			for (int i = 0; i < pluginsToRemove.length; i++) {
				remove(feature, pluginsToRemove[i], monitor);
			}

			// remove any children feature
			IFeatureReference[] childrenRef = feature.getIncludedFeatureReferences();
			for (int i = 0; i < childrenRef.length; i++) {
				IFeature childFeature = null;
				try {
					childFeature = childrenRef[i].getFeature(null);
				} catch (CoreException e) {
					UpdateCore.warn("Unable to retrieve feature to remove for:" + childrenRef[i]); //$NON-NLS-1$
				}
                // do not remove nested feature if configured (i.e. used by another configured feature)
				if (childFeature != null && !getCurrentConfiguredSite().isConfigured(childFeature))
					remove(childrenRef[i].getFeature(null), monitor);
			}

			// remove the feature from the site cache
			removeFeatureFromCache(feature.getURL());
			
			handler.completeUninstall();

			success = true;
		} catch (Throwable t) {
			originalException = t;
		} finally {
			Throwable newException = null;
			try {
				if (success) {
					// close the log
					recoveryLog.close(ErrorRecoveryLog.END_REMOVE_LOG);
					recoveryLog.delete();
				} else {
					recoveryLog.close(ErrorRecoveryLog.END_REMOVE_LOG);
				}
				handler.uninstallCompleted(success);
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
	 * returns the download size
	 * of the feature to be installed on the site.
	 * If the site is <code>null</code> returns the maximum size
	 * 
	 * If one plug-in entry has an unknown size.
	 * then the download size is unknown.
	 * 
	 */
	public long getDownloadSizeFor(IFeature feature) {
		long result = 0;
		//[132029]
		//IPluginEntry[] entriesToInstall = feature.getPluginEntries();
		//IPluginEntry[] siteEntries = this.getPluginEntries();
		//entriesToInstall = UpdateManagerUtils.diff(entriesToInstall, siteEntries);
		//[18355]
		//INonPluginEntry[] nonPluginEntriesToInstall = feature.getNonPluginEntries();

		try {
			//[132029]
			//result = feature.getFeatureContentProvider().getDownloadSizeFor(entriesToInstall, nonPluginEntriesToInstall);
			IFeatureReference[] children = feature.getIncludedFeatureReferences();
			IFeature currentFeature = null;
			for (int i = 0; i < children.length; i++) {
				currentFeature = UpdateUtils.getIncludedFeature(feature, children[i]);
				if (currentFeature != null) {
					result += getDownloadSizeFor(currentFeature);
					if(result == ContentEntryModel.UNKNOWN_SIZE)
						return result;
				}
			}

			IPluginEntry[] entriesToInstall = feature.getPluginEntries();
			IPluginEntry[] siteEntries = this.getPluginEntries();
			entriesToInstall = UpdateManagerUtils.diff(entriesToInstall, siteEntries);
			//[18355]
			   INonPluginEntry[] nonPluginEntriesToInstall = feature.getNonPluginEntries();

			 result += feature.getFeatureContentProvider().getDownloadSizeFor(entriesToInstall, nonPluginEntriesToInstall);
		} catch (CoreException e) {
			UpdateCore.warn(null, e);
			result = ContentEntryModel.UNKNOWN_SIZE;
		}
		return result;
	}

	/**
	 * returns the download size
	 * of the feature to be installed on the site.
	 * If the site is <code>null</code> returns the maximum size
	 * 
	 * If one plug-in entry has an unknown size.
	 * then the download size is unknown.
	 * 
	 * @see ISite#getDownloadSizeFor(IFeature)
	 * 
	 */
	public long getInstallSizeFor(IFeature feature) {
		long result = 0;

		try {
			List pluginsToInstall = new ArrayList();

			// get all the plugins [17304]
			pluginsToInstall.addAll(Arrays.asList(feature.getPluginEntries()));
			IFeatureReference[] children = feature.getIncludedFeatureReferences();
			IFeature currentFeature = null;
			for (int i = 0; i < children.length; i++) {
				currentFeature = UpdateUtils.getIncludedFeature(feature, children[i]);
				if (currentFeature != null) {
					//[132029]
					//pluginsToInstall.addAll(Arrays.asList(currentFeature.getPluginEntries()));
					result += getInstallSizeFor(currentFeature);
					if (result == ContentEntryModel.UNKNOWN_SIZE)
						return result;
				}
			}

			IPluginEntry[] entriesToInstall = new IPluginEntry[0];
			if (pluginsToInstall.size() > 0) {
				entriesToInstall = new IPluginEntry[pluginsToInstall.size()];
				pluginsToInstall.toArray(entriesToInstall);
			}

			IPluginEntry[] siteEntries = this.getPluginEntries();
			entriesToInstall = UpdateManagerUtils.diff(entriesToInstall, siteEntries);

			//[18355]
			INonPluginEntry[] nonPluginEntriesToInstall = feature.getNonPluginEntries();
			
			//[132029]
			//result = feature.getFeatureContentProvider().getInstallSizeFor(entriesToInstall, nonPluginEntriesToInstall);
			result += feature.getFeatureContentProvider().getInstallSizeFor(entriesToInstall, nonPluginEntriesToInstall);
		} catch (CoreException e) {
			UpdateCore.warn(null, e);
			result = ContentEntryModel.UNKNOWN_SIZE;
		}

		return result;
	}

	/**
	 * Adds a plugin entry 
	 * Either from parsing the file system or 
	 * installing a feature
	 * 
	 * We cannot figure out the list of plugins by reading the Site.xml as
	 * the archives tag are optionals
	 */
	public void addPluginEntry(IPluginEntry pluginEntry) {
		pluginEntries.add(pluginEntry);
	}

	public IPluginEntry[] getPluginEntries() {
		IPluginEntry[] result = new IPluginEntry[0];
		if (!(pluginEntries == null || pluginEntries.isEmpty())) {
			result = new IPluginEntry[pluginEntries.size()];
			pluginEntries.toArray(result);
		}
		return result;
	}


	public int getPluginEntryCount() {
		return getPluginEntries().length;
	}

	/**
	 * 
	 */
	private IFeature createExecutableFeature(IFeature sourceFeature) throws CoreException {
		IFeature result = null;
		IFeatureFactory factory = FeatureTypeFactory.getInstance().getFactory(DEFAULT_INSTALLED_FEATURE_TYPE);
		result = factory.createFeature(/*URL*/null, this, null);

		// at least set the version identifier to be the same
		 ((FeatureModel) result).setFeatureIdentifier(sourceFeature.getVersionedIdentifier().getIdentifier());
		((FeatureModel) result).setFeatureVersion(sourceFeature.getVersionedIdentifier().getVersion().toString());
		return result;
	}

	/**
	 * 
	 */
	private void remove(IFeature feature, IPluginEntry pluginEntry, InstallMonitor monitor) throws CoreException {

		if (pluginEntry == null)
			return;
			
		if (InstallRegistry.getInstance().get("plugin_"+pluginEntry.getVersionedIdentifier()) == null) { //$NON-NLS-1$
			UpdateCore.log(NLS.bind(Messages.SiteFile_pluginNotRemoved, (new String[] { pluginEntry.getVersionedIdentifier().toString() })), null);
			return; 
		}

		ContentReference[] references = feature.getFeatureContentProvider().getPluginEntryArchiveReferences(pluginEntry, monitor);
		for (int i = 0; i < references.length; i++) {
			try {
				UpdateManagerUtils.removeFromFileSystem(references[i].asFile());
				if (monitor != null)
					monitor.worked(1);
			} catch (IOException e) {
				throw Utilities.newCoreException(NLS.bind(Messages.SiteFile_CannotRemovePlugin, (new String[] { pluginEntry.getVersionedIdentifier().toString(), getURL().toExternalForm() })), e);
			}
		}
		pluginEntries.remove(pluginEntry);
		InstallRegistry.unregisterPlugin(pluginEntry);
	}

}
