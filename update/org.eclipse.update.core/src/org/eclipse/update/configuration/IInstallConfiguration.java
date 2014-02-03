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
package org.eclipse.update.configuration;

import java.io.File;
import java.util.Date;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;

/**
 * Installation configuration.
 * Represents a specific configuration of a number of sites as a point
 * in time. Maintains a record of the specific activities that resulted
 * in this configuration. Current installation configuration is
 * the configuration the platform was started with.
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @since 2.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public interface IInstallConfiguration extends IAdaptable {

	/**
	 * Indicates if this is the current configuration
	 * 
	 * @return <code>true</code> if this is the current configuration,
	 * <code>false</code> otherwise
	 * @since 2.0 
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public boolean isCurrent();

	/**
	 * Return the sites that are part of this configuration.
	 * 
	 * @return an array of configured sites, or an empty array.
	 * @since 2.0 
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public IConfiguredSite[] getConfiguredSites();

	/**
	 * Create a new installation site, based on a local file 
	 * system directory. Note, the site is not added to the
	 * configuration as a result of this call.
	 * 
	 * @param directory file directory
	 * @return new site
	 * @exception CoreException
	 * @since 2.0 
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public IConfiguredSite createConfiguredSite(File directory) throws CoreException;

	/**
	 * Create a new linked site, based on a local file 
	 * system directory. Note, the site is not added to the
	 * configuration as a result of this call.
	 * The linked site is only created if the directory is an
	 * already existing extension site and if it is not already
	 * natively linked to the local site.
	 * 
	 * @param directory file directory
	 * @return new linked site
	 * @exception CoreException
	 * @since 2.0 
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public IConfiguredSite createLinkedConfiguredSite(File directory) throws CoreException;

	/**
	 * Adds the specified site to this configuration.
	 * 
	 * @param site new site
	 * @since 2.0 
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public void addConfiguredSite(IConfiguredSite site);

	/**
	 * Removes the specified site from this configuration.
	 * 
	 * @param site site to remove
	 * @since 2.0 
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public void removeConfiguredSite(IConfiguredSite site);

	/**
	 * Adds a configuration change listener.
	 * 
	 * @param listener the listener
	 * @since 2.0 
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public void addInstallConfigurationChangedListener(IInstallConfigurationChangedListener listener);

	/**
	 * Removes a configuration change listener.
	 * 
	 * @param listener the listener
	 * @since 2.0 
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public void removeInstallConfigurationChangedListener(IInstallConfigurationChangedListener listener);

	/**
	 * Return the list of activities that resulted in this configuration.
	 * There is always at least one activity
	 * 
	 * @return an array of activities
	 * @since 2.0 
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public IActivity[] getActivities();

	/**
	 * Retrun the date the configuration was created.
	 * 
	 * @return create date
	 * @since 2.0 
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public Date getCreationDate();

	/**
	 * Return the configuration label.
	 * 
	 * @return the configuration label. If the configuration label was not
	 * explicitly set, a default label is generated based on the creation
	 * date
	 * @since 2.0 
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public String getLabel();

	/**
	 * Sets the configuration label.
	 * 
	 * @param label the label
	 * @since 2.0 
	 * <p>
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public void setLabel(String label);

	/**
	 * Returns an integer that represents a time stamp created at the beginning of a new configuration time line.
	 * Time line is started when configuration state is created by a full file system reconciliation. All configurations
	 * subsequently created will have the same time line until the next full reconciliation. Certain operations
 	 * (e.g. revert) make sense only between objects that belong to the same time line.
 	 * 
	 * @since 2.0.2
	 * @return the time stamp of the full system reconciliation
	 * 
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	public long getTimeline();

}
