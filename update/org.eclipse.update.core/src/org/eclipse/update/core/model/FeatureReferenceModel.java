package org.eclipse.update.core.model;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.eclipse.update.core.VersionedIdentifier;
import org.eclipse.update.internal.core.UpdateManagerUtils;

/**
 * Feature reference model object.
 * <p>
 * This class may be instantiated or subclassed by clients. However, in most 
 * cases clients should instead instantiate or subclass the provided 
 * concrete implementation of this model.
 * </p>
 * @see org.eclipse.update.core.FeatureReference
 * @since 2.0
 */
public class FeatureReferenceModel extends ModelObject {

	private String type;
	private URL url;
	private String urlString;
	private String featureId;
	private String featureVersion;	
	private SiteModel site;
	private List /* of String*/
	categoryNames;

	// [2.0.1]
	private String name;
	private boolean isOptional = false; 





	/**
	 * Creates an uninitialized feature reference model object.
	 * 
	 * @since 2.0
	 */
	public FeatureReferenceModel() {
		super();
	}

	/**
	 * Compares 2 feature reference models for equality
	 *  
	 * @param object feature reference model to compare with
	 * @return <code>true</code> if the two models are equal, 
	 * <code>false</code> otherwise
	 * @since 2.0 
	 */
	public boolean equals(Object object) {

		if (object == null)
			return false;
		if (getURL() == null)
			return false;

		if (!(object instanceof FeatureReferenceModel))
			return false;

		FeatureReferenceModel f = (FeatureReferenceModel) object;

		return UpdateManagerUtils.sameURL(getURL(),f.getURL());
	}

	/**
	 * Returns the referenced feature type.
	 * 
	 * @return feature type, or <code>null</code> representing the default
	 * feature type for the site
	 * @since 2.0
	 */
	public String getType() {
		return type;
	}

	/**
	 * Returns the site model for the reference.
	 * 
	 * @return site model
	 * @since 2.0
	 */
	public SiteModel getSiteModel() {
		return site;
	}

	/**
	 * Returns the unresolved URL string for the reference.
	 *
	 * @return url string
	 * @since 2.0
	 */
	public String getURLString() {
		return urlString;
	}

	/**
	 * Returns the resolved URL for the feature reference.
	 * 
	 * @return url string
	 * @since 2.0
	 */
	public URL getURL() {
		return url;
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
	 * Returns the feature identifier as a string
	 * 
	 * @see org.eclipse.update.core.IFeatureReference#getVersionedIdentifier()
	 * @return feature identifier
	 * @since 2.0
	 */
	public String getFeatureIdentifier() {
		return featureId;
	}

	/**
	 * Returns the feature version as a string
	 * 
	 * @see org.eclipse.update.core.IFeatureReference#getVersionedIdentifier()
	 * @return feature version 
	 * @since 2.0
	 */
	public String getFeatureVersion() {
		return featureVersion;
	}

	/**
	 * Sets the referenced feature type.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param type referenced feature type
	 * @since 2.0
	 */
	public void setType(String type) {
		assertIsWriteable();
		this.type = type;
	}

	/**
	 * Sets the site for the referenced.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param site site for the reference
	 * @since 2.0
	 */
	public void setSiteModel(SiteModel site) {
		assertIsWriteable();
		this.site = site;
	}

	/**
	 * Sets the unresolved URL for the feature reference.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param urlString unresolved URL string
	 * @since 2.0
	 */
	public void setURLString(String urlString) {
		assertIsWriteable();
		this.urlString = urlString;
		this.url = null;
	}

	/**
	 * Sets the feature identifier.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param featureId feature identifier
	 * @since 2.0
	 */
	public void setFeatureIdentifier(String featureId) {
		assertIsWriteable();
		this.featureId = featureId;
	}

	/**
	 * Sets the feature version.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param featureVersion feature version
	 * @since 2.0
	 */
	public void setFeatureVersion(String featureVersion) {
		assertIsWriteable();
		this.featureVersion = featureVersion;
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
	 * Returns the isOptional.
	 * @return boolean
	 */
	public boolean isOptional() {
		return isOptional;
	}


	/**
	 * Returns the name.
	 * @return String
	 */
	public String getName() {
		return name;
	}


	/**
	 * Sets the isOptional.
	 * @param isOptional The isOptional to set
	 */
	public void setOptional(boolean isOptional) {
		this.isOptional = isOptional;
	}


	/**
	 * Sets the name.
	 * @param name The name to set
	 */
	public void setName(String name) {
		this.name = name;
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

	/**
	 * Resolve the model object.
	 * Any URL strings in the model are resolved relative to the 
	 * base URL argument. Any translatable strings in the model that are
	 * specified as translation keys are localized using the supplied 
	 * resource bundle.
	 * 
	 * @param base URL
	 * @param bundle resource bundle
	 * @exception MalformedURLException
	 * @since 2.0
	 */
	public void resolve(URL base, ResourceBundle bundle)
		throws MalformedURLException {
		// resolve local elements
		url = resolveURL(base, bundle, urlString);
	}

	/**
	 * @see Object#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append(getClass().toString()+" :");
		buffer.append(" at ");
		if (url!=null) buffer.append(url.toExternalForm());
		return buffer.toString();
	}

}