package org.eclipse.update.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import java.util.ArrayList;
import java.util.List;

import org.eclipse.update.internal.core.UpdateManagerPlugin;

/**
 * Convenience implementation of a feature reference.
 * <p>
 * This class may be instantiated or subclassed by clients.
 * </p> 
 * @see org.eclipse.update.core.IFeatureReference
 * @see org.eclipse.update.core.model.FeatureReferenceModel
 * @since 2.0
 */
public class SiteFeatureReference extends SiteFeatureReferenceModel implements ISiteFeatureReference {

	private List categories;
	private VersionedIdentifier versionId;

	/**
	 * Feature reference default constructor
	 */
	public SiteFeatureReference() {
		super();
	}

	/**
	 * Constructor FeatureReference.
	 * @param ref the reference to copy
	 */
	public SiteFeatureReference(ISiteFeatureReference ref) {
		super(ref);
	}

	/**
	 * Returns an array of categories the referenced feature belong to.
	 * 
	 * @see IFeatureReference#getCategories()
	 * @since 2.0 
	 */
	public ICategory[] getCategories() {

		if (categories == null) {
			categories = new ArrayList();
			String[] categoriesAsString = getCategoryNames();
			for (int i = 0; i < categoriesAsString.length; i++) {
				ICategory siteCat = getSite().getCategory(categoriesAsString[i]);
				if (siteCat != null)
					categories.add(siteCat);
				else {
					String siteURL = getSite().getURL() != null ? getSite().getURL().toExternalForm() : null;
					UpdateManagerPlugin.warn("Category " + categoriesAsString[i] + " not found in Site:" + siteURL);
				}
			}
		}

		ICategory[] result = new ICategory[0];

		if (!(categories == null || categories.isEmpty())) {
			result = new ICategory[categories.size()];
			categories.toArray(result);
		}
		return result;
	}

	/**
	 * Adds a category to the referenced feature.
	 * 
	 * @see IFeatureReference#addCategory(ICategory)
	 * @since 2.0 
	 */
	public void addCategory(ICategory category) {
		this.addCategoryName(category.getName());
	}

}