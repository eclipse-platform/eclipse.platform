package org.eclipse.update.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.IAdaptable;

/**
 * Site Feature reference.
 * A reference to a feature on a particular update site.
 * <p>
 * Clients may implement this interface. However, in most cases clients should 
 * directly instantiate or subclass the provided implementation of this 
 * interface.
 * </p>
 * @see org.eclipse.update.core.FeatureReference
 * @since 2.0
 */
public interface ISiteFeatureReference extends IFeatureReference, IAdaptable {

	/**
	 * Returns an array of categories the referenced feature belong to.
	 * 
	 * @return an array of categories, or an empty array
	 * @since 2.0 
	 */
	public ICategory[] getCategories();

	/**
	 * Adds a category to the referenced feature.
	 * 
	 * @param category new category
	 * @since 2.0 
	 */
	public void addCategory(ICategory category);

}