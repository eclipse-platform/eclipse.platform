package org.eclipse.update.core;

import java.util.*;

/**
 * @author celek
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class SiteFeatureReferenceModel extends FeatureReference {

	private List /* of String*/
	categoryNames;

	// [2.0.1]
	private IncludedFeatureReference options;

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
