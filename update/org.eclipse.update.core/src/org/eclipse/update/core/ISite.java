package org.eclipse.update.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
import java.net.URL;

import org.eclipse.core.runtime.*;
import org.eclipse.update.configuration.IConfiguredSite;
import org.eclipse.update.core.model.InstallAbortedException;

/**
 * Site represents a location containing some number of features (packaged
 * or installed). Sites are treated purely as an installation and packaging
 * construct. They do not play a role during Eclipse plug-in execution. 
 * <p>
 * Clients may implement this interface. However, in most cases clients should 
 * directly instantiate or subclass the provided implementation of this 
 * interface.
 * </p>
 * @see org.eclipse.update.core.Site
 * @since 2.0
 */
public interface ISite extends IAdaptable {

	/**
	 * Default type for an installed feature. Different concrete feature
	 * implementations can be registered together with their corresponding type
	 * using the <code>org.eclipse.update.core.featureTypes</code> 
	 * extension point.
	 * 
	 * @since 2.0
	 */
	public static final String DEFAULT_INSTALLED_FEATURE_TYPE = "org.eclipse.update.core.installed";
	//$NON-NLS-1$		

	/**
	 * Default type for a packaged feature. Different concrete feature
	 * implementations can be registered together with their corresponding type
	 * using the <code>org.eclipse.update.core.featureTypes</code> 
	 * extension point.
	 * 
	 * @since 2.0
	 */
	public static final String DEFAULT_PACKAGED_FEATURE_TYPE = "org.eclipse.update.core.packaged";
	//$NON-NLS-1$		

	/**
	 * If we are unable to access a site, the returned CoreException will contain
	 * this return code.
	 * 
	 * @since 2.0.1
	 */
	public static final int SITE_ACCESS_EXCEPTION = 42;

	/**
	 * Returns the site URL
	 * 
	 * @return site URL
	 * @since 2.0 
	 */
	public URL getURL();

	/**
	 * Return the site type. Different concrete site implementations can be
	 * registered together with their corresponding type using the
	 * <code>org.eclipse.update.core.siteTypes</code> extension point.
	 * 
	 * @return site type, or <code>null</code>.
	 * @since 2.0 
	 */
	public String getType();

	/**
	 * Returns the site description.
	 * 
	 * @return site description, or <code>null</code>.
	 * @since 2.0 
	 */
	public IURLEntry getDescription();

	/**
	 * Returns an array of categories defined by the site.
	 * 
	 * @return array of site categories, or an empty array.
	 * @since 2.0 
	 */
	public ICategory[] getCategories();

	/**
	 * Returns the named site category.
	 * 
	 * @param name category name
	 * @return named category, or <code>null</code> ifit does not exist
	 * @since 2.0
	 */
	public ICategory getCategory(String name);

	/**
	 * Returns an array of references to features on this site.
	 * 
	 * @return an array of feature references, or an empty array.
	 * @since 2.0 
	 */
	public ISiteFeatureReference[] getFeatureReferences();

	/**
	 * Returns a reference to the specified feature if 
	 * it is installed on this site.
	 * 
	 * @param feature feature
	 * @return feature reference, or <code>null</code> if this feature
	 * cannot be located on this site.
	 * @since 2.0
	 */
	public ISiteFeatureReference getFeatureReference(IFeature feature);

	/**
	 * Returns an array of plug-in and non-plug-in archives located
	 * on this site
	 * 
	 * @return an array of archive references, or an empty array if there are
	 * no archives known to this site. Note, that an empty array does not
	 * necessarily indicate there are no archives accessible on this site.
	 * It simply indicates the site has no prior knowledge of such archives.
	 * @since 2.0 
	 */
	public IArchiveReference[] getArchives();

	/**
	 * Returns the content provider for this site. A content provider
	 * is an abstraction of each site organization. It allows the 
	 * content of the site to be accessed in a standard way
	 * regardless of the organization. All concrete sites
	 * need to be able to return a content provider.
	 * 
	 * @return site content provider
	 * @exception CoreException
	 * @since 2.0
	 */
	public ISiteContentProvider getSiteContentProvider() throws CoreException;

	/**
	 * Returns the default type for a packaged feature supported by this site
	 * 
	 * @return feature type, as registered in the
	 * <code>org.eclipse.update.core.featureTypes</code> extension point.
	 * @since 2.0
	 */
	public String getDefaultPackagedFeatureType();

	/**
	 * Returns an array of entries corresponding to plug-ins installed
	 * on this site.
	 * 
	 * @return array of plug-in entries,or an empty array.
	 * @since 2.0
	 */
	public IPluginEntry[] getPluginEntries();

	/**
	 * Returns the number of plug-ins installed on this site
	 * 
	 * @return number of installed plug-ins
	 * @since 2.0
	 */
	public int getPluginEntryCount();

	/**
	 * Adds a new plug-in entry to this site.
	 * 
	 * @param pluginEntry plug-in entry
	 * @since 2.0
	 */
	public void addPluginEntry(IPluginEntry pluginEntry);

	/**
	 * Returns an array of entries corresponding to plug-ins that are
	 * installed on this site and are referenced only by the specified
	 * feature. These are plug-ins that are not shared with any other
	 * feature.
	 * 
	 * @param feature feature
	 * @return an array of plug-in entries, or an empty array.
	 * @exception CoreException
	 * @since 2.0
	 */
	public IPluginEntry[] getPluginEntriesOnlyReferencedBy(IFeature feature) throws CoreException;

	/**
	 * Returns the size of the files that need to be downloaded in order
	 * to install the specified feature on this site, if it can be determined.
	 * This method takes into account any plug-ins that are already
	 * available on this site.
	 * 
	 * @see org.eclipse.update.core.model.ContentEntryModel#UNKNOWN_SIZE
	 * @param feature candidate feature
	 * @return download size of the feature in KiloBytes, or an indication 
	 * the size could not be determined
	 * @since 2.0 
	 */
	public long getDownloadSizeFor(IFeature feature);

	/**
	 * Returns the size of the files that need to be installed
	 * for the specified feature on this site, if it can be determined.
	 * This method takes into account any plug-ins that are already
	 * installed on this site.
	 * 
	 * @see org.eclipse.update.core.model.ContentEntryModel#UNKNOWN_SIZE
	 * @param feature candidate feature
	 * @return install size of the feature in KiloBytes, or an indication 
	 * the size could not be determined
	 * @since 2.0 
	 */
	public long getInstallSizeFor(IFeature site);

	/**
	 * Installs the specified feature on this site.
	 * 
	 * @param feature feature to install
	 * @param verificationListener install verification listener
	 * @param monitor install monitor, can be <code>null</code>
	 * @exception InstallAbortedException when the user cancels the install
	 * @exception CoreException
	 * @since 2.0 
	 */
	public IFeatureReference install(IFeature feature, IVerificationListener verificationListener, IProgressMonitor monitor) throws InstallAbortedException, CoreException;

	/**
	 * Installs the specified feature on this site.
	 * Only optional features passed as parameter will be installed.
	 * 
	 * @param feature feature to install
	 * @param optionalfeatures list of optional features to be installed
	 * @param verificationListener install verification listener
	 * @param monitor install monitor, can be <code>null</code>
	 * @exception InstallAbortedException when the user cancels the install
	 * @exception CoreException
	 * @since 2.0 
	 */
	public IFeatureReference install(IFeature feature, IFeatureReference[] optionalfeatures, IVerificationListener verificationListener, IProgressMonitor monitor) throws InstallAbortedException, CoreException;

	/**
	 * Removes (uninstalls) the specified feature from this site. This method
	 * takes into account plug-in entries referenced by the specified fetaure
	 * that continue to be required by other features installed on this site.
	 * 
	 * @param feature feature to remove
	 * @param monitor progress monitor
	 * @exception CoreException
	 * @since 2.0 
	 */
	public void remove(IFeature feature, IProgressMonitor monitor) throws CoreException;

	/**
	 * Sets the site content provider. This is typically performed
	 * as part of the site creation operation. Once set, the 
	 * provider should not be reset.
	 * 
	 * @param siteContentProvider site content provider
	 * @since 2.0
	 */
	public void setSiteContentProvider(ISiteContentProvider siteContentProvider);

	/** 
	 * Returns the <code>IConfiguredSite</code> for this site in the current 
	 * configuration or <code>null</code> if none found.
	 * 
	 * @since 2.0.2
	 */
	public IConfiguredSite getCurrentConfiguredSite();

	/**
	* Creates a new feature object. The feature must exist on this site
	* or a core exception will be thrown. Concrete implementations 
	* may elect to cache instances, in which case subsequent calls 
	* to create a feature with the same URL will
	* return the same instance.
	* param type the feature type that will be used to select the factory. If
	* <code>null</code> is passed, default feature type will be used.
	* param url URL of the feature archive as listed in the site.
	* return newly created feature object, or a cached value if
	* caching is implemented by this site.
	*/

	IFeature createFeature(String type, URL url) throws CoreException;

}