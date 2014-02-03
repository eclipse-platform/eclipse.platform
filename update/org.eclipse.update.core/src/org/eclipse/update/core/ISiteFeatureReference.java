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

import org.eclipse.core.runtime.*;

/**
 * Site Feature reference.
 * A reference to a feature on a particular update site.
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
 * @see org.eclipse.update.core.SiteFeatureReference
 * @since 2.1
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public interface ISiteFeatureReference extends IFeatureReference, IAdaptable {

	/**
	 * Returns an array of categories the referenced feature belong to.
	 * 
	 * @return an array of categories, or an empty array
	 * @since 2.1 
	 */
	public ICategory[] getCategories();

	/**
	 * Adds a category to the referenced feature.
	 * 
	 * @param category new category
	 * @since 2.1 
	 */
	public void addCategory(ICategory category);

}
