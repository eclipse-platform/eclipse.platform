package org.eclipse.update.core.model;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.eclipse.update.core.SiteFeatureReferenceModel;
import org.eclipse.update.internal.model.ConfiguredSiteModel;

/**
 * Site model object.
 * <p>
 * This class may be instantiated or subclassed by clients. However, in most 
 * cases clients should instead instantiate or subclass the provided 
 * concrete implementation of this model.
 * </p>
 * @see org.eclipse.update.core.Site
 * @since 2.0
 */
public class SiteModel extends ModelObject {

	private String type;
	private URLEntryModel description;
	private List /*of FeatureReferenceModel*/
	featureReferences;
	private List /*of ArchiveReferenceModel*/
	archiveReferences;
	private Set /*of CategoryModel*/
	categories;
	private String locationURLString;
	private URL locationURL;
	private ConfiguredSiteModel configuredSiteModel;

	/**
	 * Creates an uninitialized site model object.
	 * 
	 * @since 2.0
	 */
	public SiteModel() {
		super();
	}

	/** 
	 * Returns the site type.
	 * 
	 * @return site type, or <code>null</code>.
	 * @since 2.0
	 */
	public String getType() {
		return type;
	}

	/**
	 * Returns the site description.
	 * 
	 * @return site description, or <code>null</code>.
	 * @since 2.0
	 */
	public URLEntryModel getDescriptionModel() {
		return description;
	}

	/**
	 * Returns an array of feature reference models on this site.
	 * 
	 * @return an array of feature reference models, or an empty array.
	 * @since 2.0
	 */
	public SiteFeatureReferenceModel[] getFeatureReferenceModels() {
		if (featureReferences == null)
			return new SiteFeatureReferenceModel[0];

		return (SiteFeatureReferenceModel[]) featureReferences.toArray(
			arrayTypeFor(featureReferences));
	}

	/**
	 * Returns an array of plug-in and non-plug-in archive reference models
	 * on this site
	 * 
	 * @return an array of archive reference models, or an empty array if there are
	 * no archives known to this site.
	 * @since 2.0
	 */
	public ArchiveReferenceModel[] getArchiveReferenceModels() {
		if (archiveReferences == null)
			return new ArchiveReferenceModel[0];

		return (ArchiveReferenceModel[]) archiveReferences.toArray(
			arrayTypeFor(archiveReferences));
	}

	/**
	 * Returns an array of category models for this site.
	 * 
	 * @return array of site category models, or an empty array.
	 * @since 2.0
	 */
	public CategoryModel[] getCategoryModels() {
		if (categories == null)
			return new CategoryModel[0];

		return (CategoryModel[]) categories.toArray(arrayTypeFor(categories));
	}

	/**
	 * Returns the unresolved URL string for the site.
	 *
	 * @return url string, or <code>null</code>
	 * @since 2.0
	 */
	public String getLocationURLString() {
		return locationURLString;
	}

	/**
	 * Returns the resolved URL for the site.
	 * 
	 * @return url, or <code>null</code>
	 * @since 2.0
	 */
	public URL getLocationURL() {
		return locationURL;
	}

	/**
	 * Sets the site type.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param type site type
	 * @since 2.0
	 */
	public void setType(String type) {
		assertIsWriteable();
		this.type = type;
	}

	/**
	 * Sets the site description.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param description site description
	 * @since 2.0
	 */
	public void setDescriptionModel(URLEntryModel description) {
		assertIsWriteable();
		this.description = description;
	}

	/**
	 * Sets the feature references for this site.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param featureReferences an array of feature reference models
	 * @since 2.0
	 */
	public void setFeatureReferenceModels(FeatureReferenceModel[] featureReferences) {
		assertIsWriteable();
		if (featureReferences == null)
			this.featureReferences = null;
		else
			this.featureReferences = new ArrayList(Arrays.asList(featureReferences));
	}

	/**
	 * Sets the archive references for this site.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param archiveReferences an array of archive reference models
	 * @since 2.0
	 */
	public void setArchiveReferenceModels(ArchiveReferenceModel[] archiveReferences) {
		assertIsWriteable();
		if (archiveReferences == null)
			this.archiveReferences = null;
		else
			this.archiveReferences = new ArrayList(Arrays.asList(archiveReferences));
	}

	/**
	 * Sets the site categories.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param categories an array of category models
	 * @since 2.0
	 */
	public void setCategoryModels(CategoryModel[] categories) {
		assertIsWriteable();
		if (categories == null)
			this.categories = null;
		else {
			this.categories = new TreeSet(CategoryModel.getComparator());
			this.categories.addAll(Arrays.asList(categories));
		}
	}

	/**
	 * Sets the unresolved URL for the site.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param locationURLString url for the site (as a string)
	 * @since 2.0
	 */
	public void setLocationURLString(String locationURLString) {
		assertIsWriteable();
		this.locationURLString = locationURLString;
	}

	/**
	 * Adds a feature reference model to site.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param featureReference feature reference model
	 * @since 2.0
	 */
	public void addFeatureReferenceModel(SiteFeatureReferenceModel featureReference) {
		assertIsWriteable();
		if (this.featureReferences == null)
			this.featureReferences = new ArrayList();
		if (!this.featureReferences.contains(featureReference))
			this.featureReferences.add(featureReference);
	}

	/**
	 * Adds an archive reference model to site.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param archiveReference archive reference model
	 * @since 2.0
	 */
	public void addArchiveReferenceModel(ArchiveReferenceModel archiveReference) {
		assertIsWriteable();
		if (this.archiveReferences == null)
			this.archiveReferences = new ArrayList();
		if (!this.archiveReferences.contains(archiveReference))
			this.archiveReferences.add(archiveReference);
	}

	/**
	 * Adds a category model to site.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param category category model
	 * @since 2.0
	 */
	public void addCategoryModel(CategoryModel category) {
		assertIsWriteable();
		if (this.categories == null)
			this.categories = new TreeSet(CategoryModel.getComparator());
		if (!this.categories.contains(category))
			this.categories.add(category);
	}

	/**
	 * Removes a feature reference model from site.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param featureReference feature reference model
	 * @since 2.0
	 */
	public void removeFeatureReferenceModel(FeatureReferenceModel featureReference) {
		assertIsWriteable();
		if (this.featureReferences != null)
			this.featureReferences.remove(featureReference);
	}

	/**
	 * Removes an archive reference model from site.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param archiveReference archive reference model
	 * @since 2.0
	 */
	public void removeArchiveReferenceModel(ArchiveReferenceModel archiveReference) {
		assertIsWriteable();
		if (this.archiveReferences != null)
			this.archiveReferences.remove(archiveReference);
	}

	/**
	 * Removes a category model from site.
	 * Throws a runtime exception if this object is marked read-only.
	 * 
	 * @param category category model
	 * @since 2.0
	 */
	public void removeCategoryModel(CategoryModel category) {
		assertIsWriteable();
		if (this.categories != null)
			this.categories.remove(category);
	}

	/**
	 * Marks the model object as read-only.
	 * 
	 * @since 2.0
	 */
	public void markReadOnly() {
		super.markReadOnly();
		markReferenceReadOnly(getDescriptionModel());
		markListReferenceReadOnly(getFeatureReferenceModels());
		markListReferenceReadOnly(getArchiveReferenceModels());
		markListReferenceReadOnly(getCategoryModels());
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

		// Archives and feature are relative to location URL is teh Site element had 
		// a URL tag: see spec	
		locationURL = resolveURL(base, bundle, getLocationURLString());
		if (locationURL == null)
			locationURL = base;
		resolveListReference(getFeatureReferenceModels(), locationURL, bundle);
		resolveListReference(getArchiveReferenceModels(), locationURL, bundle);

		resolveReference(getDescriptionModel(), base, bundle);
		resolveListReference(getCategoryModels(), base, bundle);
	}

	/**
	 * 
	 */
	public ConfiguredSiteModel getConfiguredSiteModel() {
		return this.configuredSiteModel;
	}



	/**
	 * @see org.eclipse.update.core.ISite#setConfiguredSite(IConfiguredSite)
	 */
	public void setConfiguredSiteModel(ConfiguredSiteModel configuredSiteModel) {
		this.configuredSiteModel = configuredSiteModel;		
	}



}