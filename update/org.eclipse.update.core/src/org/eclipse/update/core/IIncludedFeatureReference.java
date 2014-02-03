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
package org.eclipse.update.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.update.configuration.IConfiguredSite;



/**
 * Included Feature reference.
 * A reference to a included feature.
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
 * @since 2.0.1
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public interface IIncludedFeatureReference extends IFeatureReference, IAdaptable {

	/**
	 * Returns the referenced feature.
	 * This is a factory method that creates the full feature object.
	 * equivalent to getFeature(false,null);
	 * 
	 * @return the referenced feature
	 * @deprecated use getFeature(IProgressMonitor)
	 * @since 2.0 
	 */
	public IFeature getFeature() throws CoreException;

	/**
	 * Returns the referenced feature.
	 * This is a factory method that creates the full feature object.
	 * 
	 * @param perfectMatch <code>true</code> if the perfect match feature feature should be returned
	 * <code>false</code> if the best match feature should be returned.
	 * @param configuredSite the configured site to search for the Feature. If 
	 * the configured site is <code>null</code> the search will be done in the current configured site.
	 * @return the referenced feature
	 * instead
	 * @since 2.0.2
	 * <b>Note:</b> This method is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * @deprecated use getFeature(IProgressMonitor)
	 */
	public IFeature getFeature(boolean perfectMatch,IConfiguredSite configuredSite) throws CoreException;

	/**
	 * Returns the referenced feature.
	 * This is a factory method that creates the full feature object.
	 * 
	 * @param perfectMatch <code>true</code> if the perfect match feature feature should be returned
	 * <code>false</code> if the best match feature should be returned.
	 * @param configuredSite the configured site to search for the Feature. If 
	 * the configured site is <code>null</code> the search will be done in the current configured site.
	 * @param monitor the progress monitor
	 * @return the referenced feature
	 * @since 2.1
	 * <b>Note:</b> This method is part of an interim API that is still under
	 * development and expected to change significantly before reaching
	 * stability. It is being made available at this early stage to solicit
	 * feedback from pioneering adopters on the understanding that any code that
	 * uses this API will almost certainly be broken (repeatedly) as the API
	 * evolves.
	 * @deprecated use getFeature(IProgressMonitor)
	 */
	public IFeature getFeature(boolean perfectMatch,IConfiguredSite configuredSite, IProgressMonitor monitor) throws CoreException;


	/**
	 * Returns <code>true</code> if the feature is optional, <code>false</code> otherwise.
	 * 
	 * @return boolean
	 * @since 2.0.1
	 */
	public boolean isOptional();

	/**
	 * Returns the matching rule for this included feature.
	 * The rule will determine the ability of the included feature to move version 
	 * without causing the overall feature to appear broken.
	 * 
	 * The default is <code>RULE_PERFECT</code>
	 * 
	 * @see IUpdateConstants#RULE_PERFECT
	 * @see IUpdateConstants#RULE_EQUIVALENT
	 * @see IUpdateConstants#RULE_COMPATIBLE
	 * @see IUpdateConstants#RULE_GREATER_OR_EQUAL
	 * @return int representation of feature matching rule.
	 * @since 2.0.2
	 * @deprecated since 3.0 included feature version is exactly specified
	 */
	public int getMatch();
	
	/**
	 * Returns the search location for this included feature.
	 * The location will be used to search updates for this feature.
	 * 
	 * The default is <code>SEARCH_ROOT</code>
	 * 
	 * @see IUpdateConstants#SEARCH_ROOT
	 * @see IUpdateConstants#SEARCH_SELF
	 * @return int representation of feature searching rule.
	 * @since 2.0.2
	 */

	public int getSearchLocation();

}
