package org.eclipse.update.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import java.io.File;
import java.net.URL;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.update.core.model.*;
import org.eclipse.update.internal.core.Policy;
import org.eclipse.update.internal.core.UpdateManagerPlugin;

/**
 * Convenience implementation of a site.
 * <p>
 * This class may be instantiated or subclassed by clients.
 * </p> 
 * @see org.eclipse.update.core.ISite
 * @see org.eclipse.update.core.model.SiteModel
 * @since 2.0
 */
public class Site extends SiteModel implements ISite {

	/**
	 * Default installation path for features
	 * 
	 * @since 2.0
	 */
	public static final String DEFAULT_INSTALLED_FEATURE_PATH = "features/";
	//$NON-NLS-1$

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

	private static final String PACKAGED_FEATURE_TYPE = "packaged"; //$NON-NLS-1$
	private static final String INSTALLED_FEATURE_TYPE = "installed";
	//$NON-NLS-1$	
	private ISiteContentProvider siteContentProvider;

	/**
	 * Constructor for Site
	 */
	public Site() {
		super();
	}

	/**
	 * Compares two sites for equality
	 * 
	 * @param object site object to compare with
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

		if (getURL().equals(otherSite.getURL())) return true;
		
		// check if URL are file: URL as we may
		// have 2 URL pointing to the same featureReference
		// but with different representation
		// (i.e. file:/C;/ and file:C:/)
		if (!"file".equalsIgnoreCase(getURL().getProtocol())) return false;
		if (!"file".equalsIgnoreCase(otherSite.getURL().getProtocol())) return false;		
		
		File file1 = new File(getURL().getFile());
		File file2 = new File(otherSite.getURL().getFile());
		
		if (file1==null) return false;
		return (file1.equals(file2));		
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
			UpdateManagerPlugin.getPlugin().getLog().log(e.getStatus());
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
		if (UpdateManagerPlugin.DEBUG
			&& UpdateManagerPlugin.DEBUG_SHOW_WARNINGS
			&& !found) {
			UpdateManagerPlugin.getPlugin().debug(
				Policy.bind("Site.CannotFindCategory", key, this.getURL().toExternalForm()));
			//$NON-NLS-1$ //$NON-NLS-2$
			if (getCategoryModels().length <= 0)
				UpdateManagerPlugin.getPlugin().debug(Policy.bind("Site.NoCategories"));
			//$NON-NLS-1$
		}

		return result;
	}

	/**
	 * Returns an array of references to features on this site.
	 * 
	 * @see ISite#getFeatureReferences()
	 * @since 2.0
	 */
	public IFeatureReference[] getFeatureReferences() {
		FeatureReferenceModel[] result = getFeatureReferenceModels();
		if (result.length == 0)
			return new IFeatureReference[0];
		else
			return (IFeatureReference[]) result;
	}

	/**
	 * Returns a reference to the specified feature on this site.
	 * 
	 * @see ISite#getFeatureReference(IFeature)
	 * @since 2.0
	 */
	public IFeatureReference getFeatureReference(IFeature feature) {

		if (feature == null) {
			// DEBUG
			if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_WARNINGS) {
				UpdateManagerPlugin.getPlugin().debug(
					"Site:getFeatureReference: The feature is null");
			}
			return null;
		}

		IFeatureReference[] references = getFeatureReferences();
		IFeatureReference currentReference = null;
		for (int i = 0; i < references.length; i++) {
			currentReference = references[i];
			// do not compare the URL 
			try {
				if (feature.equals(currentReference.getFeature()))
					return currentReference;

			} catch (CoreException e) {
				// DEBUG
				if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_WARNINGS) {
					UpdateManagerPlugin.getPlugin().getLog().log(e.getStatus());
				}
			}
		}
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
			throw Utilities.newCoreException(
					Policy.bind("Site.NoContentProvider"),
					null);
			//$NON-NLS-1$
		}
		return siteContentProvider;
	}

	/**
	 * Returns the default type for a packaged feature supported by this site
	 * 
	 * @see ISite#getDefaultInstallableFeatureType()
	 * @since 2.0
	 */
	public String getDefaultPackagedFeatureType() {
		return DEFAULT_PACKAGED_FEATURE_TYPE;
	}

	/**
	 * Returns an array of entries corresponding to plug-ins installed
	 * on this site.
	 * 
	 * @see IPluginContainer#getPluginEntries()
	 * @since 2.0
	 */
	public IPluginEntry[] getPluginEntries() {
		throw new UnsupportedOperationException();
	}

	/**
	 * Returns the number of plug-ins installed on this site
	 * 
	 * @see IPluginContainer#getPluginEntryCount()
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
	public IPluginEntry[] getPluginEntriesOnlyReferencedBy(IFeature feature)
		throws CoreException {

		IPluginEntry[] pluginsToRemove = new IPluginEntry[0];
		if (feature == null)
			return pluginsToRemove;

		// get the plugins from the feature
		IPluginEntry[] entries = feature.getPluginEntries();
		if (entries != null) {
			// get all the other plugins from all the other features
			Set allPluginID = new HashSet();
			IFeatureReference[] features = getFeatureReferences();
			if (features != null) {
				for (int indexFeatures = 0; indexFeatures < features.length; indexFeatures++) {
					IFeature featureToCompare = features[indexFeatures].getFeature();
					if (!feature.equals(featureToCompare)) {
						IPluginEntry[] pluginEntries =
							features[indexFeatures].getFeature().getPluginEntries();
						if (pluginEntries != null) {
							for (int indexEntries = 0;
								indexEntries < pluginEntries.length;
								indexEntries++) {
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
	 * @exception java.jang.UnsupportedOperationException
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
	 * @exception java.jang.UnsupportedOperationException
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
	 * @exception java.jang.UnsupportedOperationException
	 * @since 2.0
	 */
	public long getInstallSizeFor(IFeature feature) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Install the specified feature on this site.
	 * This implementation always throws UnsupportedOperationException
	 * because this implementation does not support the install action.
	 * 
	 * @see ISite#install(IFeature, IVerificationListener, IProgressMonitor)
	 * @exception java.jang.UnsupportedOperationException
	 * @since 2.0
	 */
	public IFeatureReference install(
		IFeature sourceFeature,
		IVerificationListener verificationListener,
		IProgressMonitor progress)
		throws CoreException {
		throw new UnsupportedOperationException();
	}

	/**
	 * Remove (uninstall) the specified feature from this site.
	 * This implementation always throws UnsupportedOperationException
	 * because this implementation does not support the remove action.
	 * 
	 * @see ISite#remove(IFeature, IProgressMonitor)
	 * @exception java.jang.UnsupportedOperationException
	 * @since 2.0
	 */
	public void remove(IFeature feature, IProgressMonitor progress)
		throws CoreException {
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
}