package org.eclipse.update.configuration;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.*;
import org.eclipse.update.core.*;

/**
 * Configured Site.
 * Represents an installation site "filtered" by configuration information.
 * Configured site is the target of the feature update operations (install
 * feature, remove feature, configure feature, unconfigure feature).
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @since 2.0
 */
public interface IConfiguredSite extends IAdaptable {

	/**
	 * Returns the underlying "unfiltered" site.
	 * 
	 * @return the underlying site 
	 * @since 2.0 
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public ISite getSite();

	/**
	 * Indicates whether updates can be applied to the site.
	 * 
	 * <code>IStatus.isOk()</code> return <code>true</code> if
	 * the site can be updated, <code>false</code> otherwise.
	 * 
	 * If updates cannot be aplied, the status contains the error message, and
	 * the possible exception. 
	 * 
	 * @see IStatus
	 * @return an IStatus
	 * @since 2.0 
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public IStatus verifyUpdatableStatus();

	/**
	 * Indicates whether updates can be applied to the site.
	 * 
	 * A configuration site is tagged a non-updatable by reading
	 * the platform configuration for this site.
	 * 
	 * @return <code>true</code> if the site can be updated, 
	 * <code>false</code> otherwise
	 * @since 2.0 
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public boolean isUpdatable();

	/**
	 * Install the specified feature on this site.
	 * 
	 * @param feature feature to install
	 * @param verificationListener verification listener, or <code>null</code>
	 * @param monitor progress monitor, or <code>null</code>
	 * @exception InstallAbortedException when the user cancels the install
	 * @exception CoreException
	 * @since 2.0 
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public IFeatureReference install(IFeature feature, IVerificationListener verificationListener, IProgressMonitor monitor) throws CoreException;

	/**
	 * Install the specified feature on this site.
	 * Only the specified optional features will be installed
	 * 
	 * @param feature feature to install
	 * @param optionalFeatures optional features to install
	 * @param verificationListener verification listener, or <code>null</code>
	 * @param monitor progress monitor, or <code>null</code>
	 * @exception InstallAbortedException when the user cancels the install
	 * @exception CoreException
	 * @since 2.0 
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public IFeatureReference install(IFeature feature, IFeatureReference[] optionalFeatures, IVerificationListener verificationListener, IProgressMonitor monitor) throws CoreException;


	/**
	 * Remove (uninstall) the specified feature from this site
	 * 
	 * @param feature feature to remove
	 * @param monitor progress monitor, or <code>null</code>
	 * @since 2.0 
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public void remove(IFeature feature, IProgressMonitor monitor) throws CoreException;

	/**
	 * Indicates if the specified feature is "broken". A feature is considered
	 * to be broken in the context of this site, if some of the plug-ins
	 * referenced by the feature are not installed on this site.
	 * 
	 * The status code is <code>IStatus.ERROR</code> if the feature is considered
	 * broken. The Status may contain the reason why the feature is broken.
	 * The status code is <code>IStatus.OK</code> if the feature is not considered
	 * broken.
	 * 
	 * @param feature the feature
	 * @return the status for this feature on this configured site
	 * @since 2.0
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public IStatus getBrokenStatus(IFeature feature);

	/**
	 * Indicates if the specified feature is configured on this site.
	 * 
	 * @param feature the feature
	 * @return <code>true</code> if the feature is configured,
	 * <code>false</code> otherwise
	 * @since 2.0
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public boolean isConfigured(IFeature feature);

	/**
	 * Configure the specified feature on this site. The configured
	 * feature will be included on next startup.
	 * 
	 * @param feature the feature
	 * @since 2.0 
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public void configure(IFeature feature) throws CoreException;

	/**
	 * Unconfigure the specified feature from this site. The unconfigured
	 * feature will be omitted on the next startup.
	 * 
	 * @param feature the feature
	 * @since 2.0 
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public boolean unconfigure(IFeature feature) throws CoreException;

	/**
	 * Return references to features configured on this site.
	 * 
	 * @return an array of feature references, or an empty array.
	 * @since 2.0 
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public IFeatureReference[] getConfiguredFeatures();

	/**
	 * Return all features installed on this site (configured as well
	 * as unconfigured). Note, that if the site requires reconciliation,
	 * the result may not match the result of the corresponding method
	 * on the underlying site.
	 * 
	 * @see ISite#getFeatureReferences()
	 * @return an array of feature references, or an empty array.
	 * @since 2.0 
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public IFeatureReference[] getFeatureReferences();

	/**
	 * Returns the install configuration object this site is part of.
	 * 
	 * @return install configuration object
	 * @since 2.0
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public IInstallConfiguration getInstallConfiguration();

	/**
	 * Adds a change listener to the configured site.
	 * 
	 * @param listener the listener to add
	 * @since 2.0 
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public void addConfiguredSiteChangedListener(IConfiguredSiteChangedListener listener);

	/**
	 * Removes a change listener from the configured site.
	 * 
	 * @param listener the listener to remove
	 * @since 2.0 
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public void removeConfiguredSiteChangedListener(IConfiguredSiteChangedListener listener);

	/**
	 * Indicates if the site is an extension site.
	 * 
	 * @return <code>true</code> if the site is an extension site,
	 * <code>false</code> otherwise
	 * @since 2.0
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public boolean isExtensionSite();

	/**
	 * Indicates if the site is a product site.
	 * 
	 * @return <code>true</code> if the site is a product site,
	 * <code>false</code> otherwise
	 * @since 2.0
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public boolean isProductSite();

	/**
	 * Indicates if the site is a private site.
	 * This does not check if this private site belongs to the
	 * product that is running.
	 * 
	 * @return <code>true</code> if the site is a private site,
	 * <code>false</code> otherwise
	 * @since 2.0
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public boolean isPrivateSite();

	/**
	 * Indicates if the site has been linked by a native
	 * installer.
	 * 
	 * @return <code>true</code> if the site is a natively linked site,
	 * <code>false</code> otherwise
	 * @since 2.0
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public boolean isNativelyLinked() throws CoreException;
}