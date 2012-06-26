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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Site Feature reference model object.
 * <p>
 * This class may be instantiated or subclassed by clients. However, in most 
 * cases clients should instead instantiate or subclass the provided 
 * concrete implementation of this model.
 * </p>
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @see org.eclipse.update.core.FeatureReference
 * @since 2.1
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public class SiteFeatureReferenceModel extends FeatureReference {

	private List /* of String*/
	categoryNames;


	/**
	 * Creates an uninitialized feature reference model object.
	 * 
	 * @since 2.0
	 */
	public SiteFeatureReferenceModel() {
		super();
	}

	/**
	 * Constructor FeatureReferenceModel.
	 * @param ref
	 */
	public SiteFeatureReferenceModel(ISiteFeatureReference ref) {
		super(ref);
		if (ref instanceof SiteFeatureReferenceModel) {
			SiteFeatureReferenceModel refModel = (SiteFeatureReferenceModel) ref;
			setCategoryNames(refModel.getCategoryNames());
		}
	}


	/**
	 * Returns the names of categories the referenced feature belongs to.
	 * 
	 * @return an array of names, or an empty array.
	 * @since 2.0
	 */
	public String[] getCategoryNames() {
		if (categoryNames == null)
			return new String[0];

		return (String[]) categoryNames.toArray(new String[0]);
	}

	/**
	 * Sets the names of categories this feature belongs to.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param categoryNames an array of category names
	 * @since 2.0
	 */
	public void setCategoryNames(String[] categoryNames) {
		assertIsWriteable();
		if (categoryNames == null)
			this.categoryNames = null;
		else
			this.categoryNames = new ArrayList(Arrays.asList(categoryNames));
	}

	/**
	 * Adds the name of a category this feature belongs to.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param categoryName category name
	 * @since 2.0
	 */
	public void addCategoryName(String categoryName) {
		assertIsWriteable();
		if (this.categoryNames == null)
			this.categoryNames = new ArrayList();
		if (!this.categoryNames.contains(categoryName))
			this.categoryNames.add(categoryName);
	}
	
	/**
	 * Removes the name of a categorys this feature belongs to.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param categoryName category name
	 * @since 2.0
	 */
	public void removeCategoryName(String categoryName) {
		assertIsWriteable();
		if (this.categoryNames != null)
			this.categoryNames.remove(categoryName);
	}
}
