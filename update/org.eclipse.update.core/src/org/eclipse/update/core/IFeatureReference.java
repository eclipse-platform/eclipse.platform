/*******************************************************************************
 *  Copyright (c) 2000, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.core;

import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Feature reference.
 * A reference to a feature.
 * <p>
 * Clients may implement this interface. However, in most cases clients should 
 * directly instantiate or subclass the provided implementation of this 
 * interface.
 * </p>
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @see org.eclipse.update.core.FeatureReference
 * @since 2.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public interface IFeatureReference extends IAdaptable,IPlatformEnvironment {

	/**
	 * Returns the referenced feature URL.
	 * 
	 * @return feature URL 
	 * @since 2.0 
	 */
	public URL getURL();

	/**
	 * Returns the update site for the referenced feature
	 * 
	 * @return feature site
	 * @since 2.0 
	 */
	public ISite getSite();

	/**
	 * Returns the label for the referenced feature
	 *
	 * @return the label
	 * @since 2.1
	 */
	public String getName();


	/**
	 * Returns the referenced feature.
	 * This is a factory method that creates the full feature object.
	 * 
	 * @return the referenced feature
	 * @deprecated use getFeature(IProgressMonitor) instead
	 * @since 2.0 
	 */
	public IFeature getFeature() throws CoreException;

	/**
	 * Returns the referenced feature.
	 * This is a factory method that creates the full feature object.
	 * 
	 * @param monitor the progress monitor
	 * @return the referenced feature
	 * @since 2.1 
	 */
	public IFeature getFeature(IProgressMonitor monitor) throws CoreException;


	/**
	 * Returns the feature identifier.
	 * 
	 * @return the feature identifier.
	 * @exception CoreException
	 * @since 2.0 
	 */
	public VersionedIdentifier getVersionedIdentifier() throws CoreException;

	/**
	 * Sets the feature reference URL.
	 * This is typically performed as part of the feature reference creation
	 * operation. Once set, the url should not be reset.
	 * 
	 * @param url reference URL
	 * @since 2.0 
	 */
	public void setURL(URL url) throws CoreException;

	/**
	 * Associates a site with the feature reference.
	 * This is typically performed as part of the feature reference creation
	 * operation. Once set, the site should not be reset.
	 * 
	 * @param site site for the feature reference
	 * @since 2.0 
	 */
	public void setSite(ISite site);
	
	/**
	 * Returns <code>true</code> if this feature is patching another feature,
	 * <code>false</code> otherwise
	 * @return boolean
	 * @since 2.1
	 */
	public boolean isPatch();
	
	
}
