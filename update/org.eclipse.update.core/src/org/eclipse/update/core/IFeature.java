package org.eclipse.update.core;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import java.net.URL;

import org.eclipse.core.runtime.*;
import org.eclipse.update.core.model.InstallAbortedException;

/**
 * Feature defines the packaging "container" for a group of related plug-ins,
 * plug-in fragments, and optionally non-plug-in files. 
 * <p>
 * Features are treated purely as an installation and packaging construct. 
 * They do not play a role during Eclipse plug-in execution.
 * They are simply an inclusive "manifest" of the plug-ins, fragments 
 * and other files that make up that feature. If features are logically made 
 * up of plug-ins from "sub-features", the top-level feature "manifest"
 * must be fully resolved at packaging time.
 * </p>
 * <p>
 * Clients may implement this interface. However, in most cases clients should 
 * directly instantiate or subclass the provided implementation of this 
 * interface.
 * </p>
 * @see org.eclipse.update.core.Feature
 * @since 2.0
 */
public interface IFeature extends IAdaptable, IPlatformEnvironment {

	/**
	 * Indicates a 'happy' feature
	 * A feature is considered to be 'happy' in the context of a local site
	 * if all the plug-ins referenced by the feature are installed on the site and no other
	 * version of any of the plug-ins are installed on any other site of the local site.
	 * 
	 * @see ILocalSite#getStatus(IFeature)
	 * @since 2.0
	 */
	public static final int STATUS_HAPPY = 0;

	/**
	 * Indicates a 'happy' feature
	 * A feature is considered to be 'ambiguous' in the context of a local site
	 * if all the plug-ins referenced by the feature are installed on the site and other
	 * version of any of the plug-ins are installed on any other site of the local site.
	 * 
	 * @see ILocalSite#getStatus(IFeature)
	 * @since 2.0
	 */	
	public static final int STATUS_AMBIGUOUS = 1;
	
	/**
	 * Indicates an 'unhappy' feature
	 * A feature is considered to be 'unhappy' in the context of this site,
	 * if some of the plug-ins referenced by the feature are not installed on this site.
	 * 
	 * @see ILocalSite#getStatus(IFeature)
	 * @since 2.0
	 */	
	public static final int STATUS_UNHAPPY = 2;
	
	
	/**
	 * Indicates a disable feature
	 * 
	 * @see ILocalSite#getStatus(IFeature)
	 * @since 2.0.2
	 */	
	public static final int STATUS_DISABLED = -1;
		
	/**
	 * Indicates the one-click update will search the 
	 * location of the nesting root feature.
	 * 
	 * @since 2.0.1
	 */	
	public static final int SEARCH_LOCATION_DEFAULT = 0;	

	/**
	 * Indicates the one-click update will search the 
	 * location defined by the feature.
	 * 
	 * @since 2.0.1
	 */	
	public static final int SEARCH_LOCATION_FEATURE = 1;	

	/**
	 * Indicates the one-click update will search both the 
	 * location of the nesting root feature and the 
	 * location defined by the feature.
	 * 
	 * @since 2.0.1
	 */	
	public static final int SEARCH_LOCATION_BOTH = 2;	

	
	/**
	 * Returns the feature identifier.
	 * 
	 * @return the feature identifier.
	 * @since 2.0 
	 */
	public VersionedIdentifier getVersionedIdentifier();

	/**
	 * Returns the site this feature is associated with.
	 * 
	 * @return the site for this feature
	 * @since 2.0 
	 */
	public ISite getSite();

	/**
	 * Returns the displayable label of the feature.
	 * 
	 * @return feature label, or <code>null</code>.
	 * @since 2.0 
	 */
	public String getLabel();

	/**
	 * Returns the feature URL.
	 * This is the URL that was used to create the feature. The interpretation
	 * of the URL is dependent on the concrete feature implementation.  * 
	 * 
	 * @see IFeatureFactory#createFeature(URL, ISite)
	 * @return feature URL
	 * @since 2.0 
	 */
	public URL getURL();

	/**
	 * Returns an information entry referencing the location of the
	 * feature update site. The update site can be accessed to obtain
	 * feature updates for this feature.
	 * 
	 * @return update site entry, or <code>null</code>.
	 * @since 2.0 
	 */
	public IURLEntry getUpdateSiteEntry();

	/**
	 * Return an array of information entries referencing locations of other
	 * update sites. This mechanism can be used by features to distribute
	 * location information about general update sites to clients.
	 * 
	 * @return an array of site entries, or an empty array.
	 * @since 2.0 
	 */
	public IURLEntry[] getDiscoverySiteEntries();

	/**
	 * Returns a displayable label identifying the provider of this feature
	 * 
	 * @return provider label, or <code>null</code>.
	 * @since 2.0 
	 */
	public String getProvider();

	/**
	 * Returns and optional custom install handler entry.
	 * 
	 * @return install handler entry, or <code>null</code> if
	 * none was specified
	 * @since 2.0
	 */
	public IInstallHandlerEntry getInstallHandlerEntry();

	/**
	 * Returns the feature description.
	 * 
	 * @return feature rescription, or <code>null</code>.
	 * @since 2.0 
	 */
	public IURLEntry getDescription();

	/**
	 * Returns the copyright information for the feature.
	 * 
	 * @return copyright information, or <code>null</code>.
	 * @since 2.0 
	 */
	public IURLEntry getCopyright();

	/**
	 * Returns the license information for the feature.
	 * 
	 * @return feature license, or <code>null</code>.
	 * @since 2.0 
	 */
	public IURLEntry getLicense();

	/**
	 * Return optional image for the feature.
	 * 
	 * @return the URL pointing to the image, , or <code>null</code>.
	 * @since 2.0 
	 */
	public URL getImage();

	/**
	 * Return a list of plug-in dependencies for this feature. A plug-in
	 * dependency is a reference to a plug-in required for feature execution
	 * that is not packaged as part of the feature.
	 * 
	 * @return the list of required plug-in dependencies, or an empty array.
	 * @since 2.0 
	 */
	public IImport[] getImports();

	/**
	 * Install the contents of this feature into the specified target feature.
	 * All optional features will be installed
	 * 
	 * @param targetFeature
	 * @param verificationListener
	 * @param monitor
	 * @exception InstallAbortedException when the user cancels the install
	 * @exception CoreException
	 * @since 2.0
	 */
	public IFeatureReference install(
		IFeature targetFeature,
		IVerificationListener verificationListener,
		IProgressMonitor monitor)
		throws InstallAbortedException,CoreException;

	/**
	 * Install the contents of this feature into the specified target feature.
	 * Only the listed optional features will be installed.
	 * 
	 * @param targetFeature
	 * @param optionalFeatures the optional features to be installed
	 * @param verificationListener
	 * @param monitor
	 * @exception InstallAbortedException when the user cancels the install
	 * @exception CoreException
	 * @since 2.0.1
	 */
	public IFeatureReference install(
		IFeature targetFeature,
		IFeatureReference[] optionalFeatures,
		IVerificationListener verificationListener,
		IProgressMonitor monitor)
		throws InstallAbortedException,CoreException;

	/**
	 * Returns an array of feature references included by this feature
	 * filtered by the operating system, windowing system and architecture system
	 * set in <code>Sitemanager</code>
	 * 
	 * @return an erray of feature references, or an empty array.
	 * @since 2.0
	 */
	public IIncludedFeatureReference[] getIncludedFeatureReferences() throws CoreException;

	/**
	 * Returns an array of feature references included by this feature
	 * No filtering occurs
	 * 
	 * @return an erray of feature references, or an empty array.
	 * @since 2.0
	 */
	public IIncludedFeatureReference[] getRawIncludedFeatureReferences() throws CoreException;

	/**
	 * Returns an array of plug-in entries referenced by this feature
	 * filtered by the operating system, windowing system and architecture system
	 * set in <code>Sitemanager</code>
	 * 
	 * @return an erray of plug-in entries, or an empty array.
	 * @since 2.0
	 */
	public IPluginEntry[] getPluginEntries();

	/**
	 * Returns an array of plug-in entries referenced by this feature
	 * No filtering occurs
	 * 
	 * @return an erray of plug-in entries, or an empty array.
	 * @since 2.0
	 */
	public IPluginEntry[] getRawPluginEntries();

	/**
	 * Returns the count of referenced plug-in entries.
	 * 
	 * @return plug-in entry count
	 * @since 2.0
	 */
	public int getPluginEntryCount();

	/**
	 * Returns an array of non-plug-in entries referenced by this feature
	 * filtered by the operating system, windowing system and architecture system
	 * set in <code>Sitemanager</code>
	 * 
	 * @return an erray of non-plug-in entries, or an empty array.
	 * @since 2.0
	 */
	public INonPluginEntry[] getNonPluginEntries();

	/**
	 * Returns an array of non-plug-in entries referenced by this feature
	 * No filtering occurs
	 * 
	 * @return an erray of non-plug-in entries, or an empty array.
	 * @since 2.0
	 */
	public INonPluginEntry[] getRawNonPluginEntries();

	/**
	 * Returns the count of referenced non-plug-in entries.
	 * 
	 * @return non-plug-in entry count
	 * @since 2.0
	 */
	public int getNonPluginEntryCount();

	/**
	 * Returns the download size of the feature, if it can be determined.
	 * 
	 * @see org.eclipse.update.core.model.ContentEntryModel#UNKNOWN_SIZE
	 * @return download size of the feature in KiloBytes, or an indication 
	 * the size could not be determined
	 * @since 2.0 
	 */
	public long getDownloadSize();

	/**
	 * Returns the install size of the feature, if it can be determined.
	 * 
	 * @see org.eclipse.update.core.model.ContentEntryModel#UNKNOWN_SIZE
	 * @return install size of the feature in KiloBytes, or an indication 
	 * the size could not be determined
	 * @since 2.0 
	 */
	public long getInstallSize();

	/**
	 * Indicates whether the feature can be used as a primary feature.
	 * 
	 * @return <code>true</code> if this is a primary feature, 
	 * otherwise <code>false</code>
	 * @since 2.0 
	 */
	public boolean isPrimary();

	/**
	 * Returns an optional identifier of an application to be used when
	 * starting up the platform with this feature as the primary feature.
	 * The application identifier must represent a valid application registered
	 * in the <code>org.eclipse.core.runtime.applications</code> extension point.
	 * 
	 * @return application identifier, or <code>null</code>
	 * @since 2.0 
	 */
	public String getApplication();
	
	/**
	 * Returns an optional identifier of a colocation affinity feature.
	 * 
	 * @return feature identifier, or <code>null</code>.
	 * @since 2.0
	 */
	public String getAffinityFeature();

	/**
	 * Returns the content provider for this feature. A content provider
	 * is an abstraction of each feature internal packaging structure.
	 * It allows the feature content to be accessed in a standard way
	 * regardless of the internal packaging. All concrete features
	 * need to be able to return a content provider.
	 * 
	 * @return feature content provider
	 * @exception CoreExcepton
	 * @since 2.0
	 */
	public IFeatureContentProvider getFeatureContentProvider()
		throws CoreException;

	/**
	 * Returns the content consumer for this feature. A content consumer
	 * is an abstraction of each feature internal packaging mechanism.
	 * It allows content to be stored into a feature in a standard way
	 * regardless of the packaging mechanism used. Only concrete features
	 * that support storing need to implement a content consumer. The platform
	 * implements at least one feature type supporting content consumer.
	 * This is the feature type representing a locally-installed
	 * feature.
	 * 
	 * @return feature content consumer
	 * @exception CoreException
	 * @exception UnsupportedOperationException
	 * @since 2.0
	 */
	public IFeatureContentConsumer getFeatureContentConsumer()
		throws CoreException;

	/**
	 * Sets the site for this feature. This is typically performed as part
	 * of the feature creation operation. Once set, the site
	 * should not be reset.
	 * 
	 * @param site the site
	 * @throws CoreException site for this feature is already set
	 * @since 2.0 
	 */
	public void setSite(ISite site) throws CoreException;

	/**
	 * Sets the content provider for this feature. This is typically
	 * performed as part of the feature creation operation. Once set, the 
	 * provider should not be reset.
	 * 
	 * @see IFeatureFactory#createFeature(URL, ISite)
	 * @param featureContentProvider content provider
	 * @since 2.0
	 */
	public void setFeatureContentProvider(IFeatureContentProvider featureContentProvider);

}