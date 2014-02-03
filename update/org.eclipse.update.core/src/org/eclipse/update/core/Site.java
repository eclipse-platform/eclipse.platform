/*******************************************************************************
 *  Copyright (c) 2000, 2012 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     James D Miles (IBM Corp.) - bug 191783, NullPointerException in FeatureDownloader
 *******************************************************************************/
package org.eclipse.update.core;

import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.model.*;
import org.eclipse.update.internal.core.*;

/**
 * Convenience implementation of a site.
 * <p>
 * This class may be instantiated or subclassed by clients.
 * </p> 
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @see org.eclipse.update.core.ISite
 * @see org.eclipse.update.core.model.SiteModel
 * @since 2.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public class Site extends SiteModel implements ISiteWithMirrors {

	/**
	 * Default installation path for features
	 * 
	 * @since 2.0
	 */
	public static final String DEFAULT_INSTALLED_FEATURE_PATH = "features/"; //$NON-NLS-1$

	/**
	 * Default installation path for plug-ins and plug-in fragments
	 * 
	 * @since 2.0
	 */
	public static final String DEFAULT_PLUGIN_PATH = "plugins/"; //$NON-NLS-1$

	/**
	 * Default path on a site where packaged features are located
	 * 
	 * @since 2.0
	 */
	public static final String DEFAULT_FEATURE_PATH = "features/"; //$NON-NLS-1$

	/**
	 * Default site manifest file name
	 * 
	 * @since 2.0
	 */
	public static final String SITE_FILE = "site"; //$NON-NLS-1$

	/**
	 * Default site manifest extension
	 * 
	 * @since 2.0
	 */
	public static final String SITE_XML = SITE_FILE + ".xml"; //$NON-NLS-1$

	private ISiteContentProvider siteContentProvider;
	
	private Map featureCache = Collections.synchronizedMap(new HashMap()); // key=URLKey value=IFeature
	
	/**
	 * Constructor for Site
	 */
	public Site() {
		super();
	}

	/**
	 * Compares two sites for equality
	 * 
	 * @param obj site object to compare with
	 * @return <code>true</code> if the two sites are equal, 
	 * <code>false</code> otherwise
	 * @since 2.0
	 */
	public boolean equals(Object obj) {
		if (!(obj instanceof ISite))
			return false;
		if (getURL() == null)
			return false;
		ISite otherSite = (ISite) obj;

		return UpdateManagerUtils.sameURL(getURL(), otherSite.getURL());
	}

	/**
	 * Returns the site URL
	 * 
	 * @see ISite#getURL()
	 * @since 2.0
	 */
	public URL getURL() {
		URL url = null;
		try {
			url = getSiteContentProvider().getURL();
		} catch (CoreException e) {
			UpdateCore.warn(null, e);
		}
		return url;
	}

	/**
	 * Returns the site description.
	 * 
	 * @see ISite#getDescription()
	 * @since 2.0
	 */
	public IURLEntry getDescription() {
		return (IURLEntry) getDescriptionModel();
	}

	/**
	 * Returns an array of categories defined by the site.
	 * 
	 * @see ISite#getCategories()
	 * @since 2.0
	 */
	public ICategory[] getCategories() {
		CategoryModel[] result = getCategoryModels();
		if (result.length == 0)
			return new ICategory[0];
		else
			return (ICategory[]) result;
	}

	/**
	 * Returns the named site category.
	 * 
	 * @see ISite#getCategory(String)
	 * @since 2.0
	 */
	public ICategory getCategory(String key) {
		ICategory result = null;
		boolean found = false;
		int length = getCategoryModels().length;

		for (int i = 0; i < length; i++) {
			if (getCategoryModels()[i].getName().equals(key)) {
				result = (ICategory) getCategoryModels()[i];
				found = true;
				break;
			}
		}

		//DEBUG:
		if (!found) {
			String URLString = (this.getURL() != null) ? this.getURL().toExternalForm() : "<no site url>"; //$NON-NLS-1$
			UpdateCore.warn(NLS.bind(Messages.Site_CannotFindCategory, (new String[] { key, URLString })));
			if (getCategoryModels().length <= 0)
				UpdateCore.warn(Messages.Site_NoCategories);	
		}

		return result;
	}

	/**
	 * Returns an array of references to features on this site.
	 * 
	 * @see ISite#getFeatureReferences()
	 * @since 2.0
	 */
	public ISiteFeatureReference[] getRawFeatureReferences() {
		FeatureReferenceModel[] result = getFeatureReferenceModels();
		if (result.length == 0)
			return new ISiteFeatureReference[0];
		else
			return (ISiteFeatureReference[]) result;
	}

	/**
	 * @see org.eclipse.update.core.ISite#getFeatureReferences()
	 */
	public ISiteFeatureReference[] getFeatureReferences() {
		// only filter local site
		if (getCurrentConfiguredSite()!=null)
			return filterFeatures(getRawFeatureReferences());
		else 
			return getRawFeatureReferences();
		
	}

	/*
	 * Method filterFeatures.
	 * Also implemented in Feature
	 *  
	 * @param list
	 * @return List
	 */
	private ISiteFeatureReference[] filterFeatures(ISiteFeatureReference[] allIncluded) {
		List list = new ArrayList();
		if (allIncluded!=null){
			for (int i = 0; i < allIncluded.length; i++) {
				ISiteFeatureReference included = allIncluded[i];
				if (UpdateManagerUtils.isValidEnvironment(included))
					list.add(included);
				else{
					if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_WARNINGS){
						UpdateCore.warn("Filtered out feature reference:"+included); //$NON-NLS-1$
					}
				}
			}
		}
		
		ISiteFeatureReference[] result = new ISiteFeatureReference[list.size()];
		if (!list.isEmpty()){
			list.toArray(result);
		}
		
		return result;	
	}

	/**
	 * Returns a reference to the specified feature on this site.
	 * 
	 * @see ISite#getFeatureReference(IFeature)
	 * @since 2.0
	 */
	public ISiteFeatureReference getFeatureReference(IFeature feature) {

		if (feature == null) {
			UpdateCore.warn("Site:getFeatureReference: The feature is null"); //$NON-NLS-1$
			return null;
		}

		ISiteFeatureReference[] references = getFeatureReferences();
		ISiteFeatureReference currentReference = null;
		for (int i = 0; i < references.length; i++) {
			currentReference = references[i];
			//if (UpdateManagerUtils.sameURL(feature.getURL(), currentReference.getURL()))
			//	return currentReference;
			try {
				if (feature.getVersionedIdentifier().equals(currentReference.getVersionedIdentifier()))
					return currentReference;
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		UpdateCore.warn("Feature " + feature + " not found on site" + this.getURL()); //$NON-NLS-1$ //$NON-NLS-2$
		return null;
	}

	/**
	 * Returns an array of plug-in and non-plug-in archives located
	 * on this site
	 * 
	 * @see ISite#getArchives()
	 * @since 2.0
	 */
	public IArchiveReference[] getArchives() {
		ArchiveReferenceModel[] result = getArchiveReferenceModels();
		if (result.length == 0)
			return new IArchiveReference[0];
		else
			return (IArchiveReference[]) result;
	}

	/**
	 * Returns the content provider for this site.
	 * 
	 * @see ISite#getSiteContentProvider()
	 * @since 2.0
	 */
	public ISiteContentProvider getSiteContentProvider() throws CoreException {
		if (siteContentProvider == null) {
			throw Utilities.newCoreException(Messages.Site_NoContentProvider, null);	
		}
		return siteContentProvider;
	}

	/**
	 * Returns the default type for a packaged feature supported by this site
	 * 
	 * @see ISite#getDefaultPackagedFeatureType()
	 * @since 2.0
	 */
	public String getDefaultPackagedFeatureType() {
		return DEFAULT_PACKAGED_FEATURE_TYPE;
	}

	/**
	 * Returns an array of entries corresponding to plug-ins installed
	 * on this site.
	 * 
	 * @see ISite#getPluginEntries()
	 * @since 2.0
	 */
	public IPluginEntry[] getPluginEntries() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns the number of plug-ins installed on this site
	 * 
	 * @see ISite#getPluginEntryCount()
	 * @since 2.0
	 */
	public int getPluginEntryCount() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns an array of entries corresponding to plug-ins that are
	 * installed on this site and are referenced only by the specified
	 * feature. 
	 * 
	 * @see ISite#getPluginEntriesOnlyReferencedBy(IFeature)	 * 
	 * @since 2.0
	 */
	public IPluginEntry[] getPluginEntriesOnlyReferencedBy(IFeature feature) throws CoreException {

		IPluginEntry[] pluginsToRemove = new IPluginEntry[0];
		if (feature == null)
			return pluginsToRemove;

		// get the plugins from the feature
		IPluginEntry[] entries = feature.getPluginEntries();
		if (entries != null) {
			// get all the other plugins from all the other features
			Set allPluginID = new HashSet();
			ISiteFeatureReference[] features = getFeatureReferences();
			if (features != null) {
				for (int indexFeatures = 0; indexFeatures < features.length; indexFeatures++) {
					IFeature featureToCompare = null;
					try {
						featureToCompare = features[indexFeatures].getFeature(null);
					} catch (CoreException e) {
						UpdateCore.warn(null, e);
					}
					if (!feature.equals(featureToCompare)) {
						IPluginEntry[] pluginEntries = features[indexFeatures].getFeature(null).getPluginEntries();
						if (pluginEntries != null) {
							for (int indexEntries = 0; indexEntries < pluginEntries.length; indexEntries++) {
								allPluginID.add(pluginEntries[indexEntries].getVersionedIdentifier());
							}
						}
					}
				}
			}

			// create the delta with the plugins that may be still used by other configured or unconfigured feature
			List plugins = new ArrayList();
			for (int indexPlugins = 0; indexPlugins < entries.length; indexPlugins++) {
				if (!allPluginID.contains(entries[indexPlugins].getVersionedIdentifier())) {
					plugins.add(entries[indexPlugins]);
				}
			}

			// move List into Array
			if (!plugins.isEmpty()) {
				pluginsToRemove = new IPluginEntry[plugins.size()];
				plugins.toArray(pluginsToRemove);
			}
		}

		return pluginsToRemove;
	}

	/**
	 * Adds a new plug-in entry to this site.
	 * This implementation always throws UnsupportedOperationException
	 * because this implementation does not support the install action.
	 * 
	 * @see ISite#addPluginEntry(IPluginEntry)
	 * @exception java.lang.UnsupportedOperationException
	 * @since 2.0
	 */
	public void addPluginEntry(IPluginEntry pluginEntry) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Get download size for the specified feature on this site.
	 * This implementation always throws UnsupportedOperationException
	 * because this implementation does not support the install action.
	 * 
	 * @see ISite#getDownloadSizeFor(IFeature)
	 * @exception java.lang.UnsupportedOperationException
	 * @since 2.0
	 */
	public long getDownloadSizeFor(IFeature feature) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Get install size for the specified feature on this site.
	 * This implementation always throws UnsupportedOperationException
	 * because this implementation does not support the install action.
	 * 
	 * @see ISite#getInstallSizeFor(IFeature)
	 * @exception java.lang.UnsupportedOperationException
	 * @since 2.0
	 */
	public long getInstallSizeFor(IFeature feature) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Install the specified feature and all optional features on this site.
	 * This implementation always throws UnsupportedOperationException
	 * because this implementation does not support the install action.
	 * 
	 * @see ISite#install(IFeature, IVerificationListener, IProgressMonitor)
	 * @exception InstallAbortedException when the user cancels the install
	 * @exception CoreException
	 * @exception java.lang.UnsupportedOperationException
	 * @since 2.0
	 */
	public IFeatureReference install(IFeature sourceFeature, IVerificationListener verificationListener, IProgressMonitor progress) throws InstallAbortedException, CoreException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Install the specified feature and listed optional features on this site.
	 * This implementation always throws UnsupportedOperationException
	 * because this implementation does not support the install action.
	 * 
	 * @see ISite#install(IFeature, IVerificationListener, IProgressMonitor)
	 * @exception InstallAbortedException when the user cancels the install
	 * @exception CoreException
	 * @exception java.lang.UnsupportedOperationException
	 * @since 2.0
	 */
	public IFeatureReference install(IFeature sourceFeature, IFeatureReference[] optionalFeatures, IVerificationListener verificationListener, IProgressMonitor progress) throws InstallAbortedException, CoreException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Install the specified feature on this site using the content consumer as 
	 * a context to install the feature in.
	 * This implementation always throws UnsupportedOperationException
	 * because this implementation does not support the install action.
	 * 
	 * @param sourceFeature feature to install
	 * @param parentContentConsumer content consumer of the parent feature
	 * @param parentVerifier verifier of the parent feature
	 * @param verificationListener install verification listener
	 * @param progress install monitor, can be <code>null</code>
	 * @exception InstallAbortedException when the user cancels the install
	 * @exception CoreException
	 * @exception java.lang.UnsupportedOperationException 
	 * @since 2.0 
	 */
	public IFeatureReference install(IFeature sourceFeature, IFeatureReference[] optionalFeatures, IFeatureContentConsumer parentContentConsumer, IVerifier parentVerifier, IVerificationListener verificationListener, IProgressMonitor progress)
		throws CoreException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Remove (uninstall) the specified feature from this site.
	 * This implementation always throws UnsupportedOperationException
	 * because this implementation does not support the remove action.
	 * 
	 * @see ISite#remove(IFeature, IProgressMonitor)
	 * @exception java.lang.UnsupportedOperationException
	 * @since 2.0
	 */
	public void remove(IFeature feature, IProgressMonitor progress) throws CoreException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Sets the site content provider.
	 * 
	 * @see ISite#setSiteContentProvider(ISiteContentProvider)
	 * @since 2.0
	 */
	public void setSiteContentProvider(ISiteContentProvider siteContentProvider) {
		this.siteContentProvider = siteContentProvider;
	}
	/**
	 * @see ISite#getCurrentConfiguredSite()
	 */
	public IConfiguredSite getCurrentConfiguredSite() {
		return (IConfiguredSite) getConfiguredSiteModel();
	}

	/**
	 * @see org.eclipse.update.core.ISite#createFeature(String, URL)
	 * @deprecated
	 */
	public IFeature createFeature(String type, URL url) throws CoreException {
		return createFeature(type,url,null);
	}

	/**
	 * @see org.eclipse.update.core.ISite#createFeature(String, URL,
	 * IProgressMonitor)
	 */
	public IFeature createFeature(String type, URL url, IProgressMonitor monitor) throws CoreException {

		if(url == null) {
			UpdateCore.warn("The feature URL passed is null");
			return null;
		}
		
		// First check the cache
		URLKey key = new URLKey(url);
		IFeature feature = (IFeature) featureCache.get(key);
		if (feature != null) return feature;

		// Create a new one
		if (type == null || type.equals("")) { //$NON-NLS-1$
			// ask the Site for the default type
			type = getDefaultPackagedFeatureType();
		}

		IFeatureFactory factory = FeatureTypeFactory.getInstance().getFactory(type);
		feature = factory.createFeature(url, this, monitor);
		if (feature != null) {
			// Add the feature to the cache
			featureCache.put(key, feature);
		}
		return feature;
	}

	protected void removeFeatureFromCache(URL featureURL) {
		URLKey key = new URLKey(featureURL);
		featureCache.remove(key);
	}

	/**
	 * Return an array of mirror update sites.
	 * 
	 * @return an array of mirror update sites
	 * @since 2.0
	 */
	public IURLEntry[] getMirrorSiteEntries() {
		URLEntryModel[] result = getMirrorSiteEntryModels();
		if (result.length == 0)
			return new IURLEntry[0];
		else
			return (IURLEntry[]) result;
	}
}
