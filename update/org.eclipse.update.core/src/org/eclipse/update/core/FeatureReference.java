package org.eclipse.update.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.*;
import org.eclipse.update.configuration.IConfiguredSite;
import org.eclipse.update.core.model.FeatureReferenceModel;
import org.eclipse.update.core.model.SiteModel;
import org.eclipse.update.internal.core.*;

/**
 * Convenience implementation of a feature reference.
 * <p>
 * This class may be instantiated or subclassed by clients.
 * </p> 
 * @see org.eclipse.update.core.IFeatureReference
 * @see org.eclipse.update.core.model.FeatureReferenceModel
 * @since 2.0
 */
public class FeatureReference extends FeatureReferenceModel implements IFeatureReference {

	private IFeature feature; // best match
	private List categories;
	private VersionedIdentifier versionId;

	/**
	 * Feature reference default constructor
	 */
	public FeatureReference() {
		super();
	}

	/**
	 * Constructor FeatureReference.
	 * @param ref the reference to copy
	 */
	public FeatureReference(IFeatureReference ref) {
		super((FeatureReferenceModel)ref);
		setSite(ref.getSite());
		try {
			setURL(ref.getURL());
		} catch (CoreException e) {
			UpdateManagerPlugin.warn("", e);
		}
	}

	/**
	 * Returns the feature this reference points to based on match and resolution
	 *  @return the feature on the Site
	 */
	public IFeature getFeature() throws CoreException {
		return getFeature(false,null);
	}

	/**
	 * Returns the update site for the referenced feature
	 * 
	 * @see IFeatureReference#getSite()
	 * @since 2.0 
	 */
	public ISite getSite() {
		return (ISite) getSiteModel();
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

	/** 
	 * Sets the feature reference URL.
	 * This is typically performed as part of the feature reference creation
	 * operation. Once set, the url should not be reset.
	 * 
	 * @see IFeatureReference#setURL(URL)
	 * @since 2.0 
	 */
	public void setURL(URL url) throws CoreException {
		if (url != null) {
			setURLString(url.toExternalForm());
			try {
				resolve(url, null);
			} catch (MalformedURLException e) {
				throw Utilities.newCoreException(Policy.bind("FeatureReference.UnableToResolveURL", url.toExternalForm()), e);
				//$NON-NLS-1$
			}
		}
	}

	/**
	 * Associates a site with the feature reference.
	 * This is typically performed as part of the feature reference creation
	 * operation. Once set, the site should not be reset.
	 * 
	 * @see IFeatureReference#setSite(ISite)
	 * @since 2.0 
	 */
	public void setSite(ISite site) {
		setSiteModel((SiteModel) site);
	}

	/**
	* Returns the feature identifier.
	* 
	* @see IFeatureReference#getVersionedIdentifier()
	* @exception CoreException
	* @since 2.0
	*/
	public VersionedIdentifier getVersionedIdentifier() throws CoreException {

		if (versionId != null)
			return versionId;

		String id = getFeatureIdentifier();
		String ver = getFeatureVersion();
		if (id != null && ver != null) {
			try {
				versionId = new VersionedIdentifier(id, ver);
				return versionId;
			} catch (Exception e) {
				UpdateManagerPlugin.warn("Unable to create versioned identifier:" + id + ":" + ver);
			}
		}

		// we need the exact match or we may have an infinite loop
		return getFeature(true,null).getVersionedIdentifier();
	}
	/**
	 * @see org.eclipse.update.core.IFeatureReference#getName()
	 */
	public String getName() {
		if (getOptions() == null) {
			try {
				return getFeature().toString();
			} catch (CoreException e) {
				UpdateManagerPlugin.warn("", e);
			}
		}
		return getOptions().getName();
	}

	/**
	 * @see org.eclipse.update.core.IFeatureReference#isOptional()
	 */
	public boolean isOptional() {
		if (getOptions() == null)
			return false;
		return getOptions().isOptional();
	}

	/**
	 * @see org.eclipse.update.core.IFeatureReference#getMatch()
	 */
	public int getMatch() {
		if (getOptions() == null)
			return IImport.RULE_PERFECT;
		return getOptions().getMatch();
	}

	/**
	 * @see org.eclipse.update.core.IFeatureReference#getSearchLocation()
	 */
	public int getSearchLocation() {
		if (getOptions() == null)
			return IUpdateConstants.SEARCH_ROOT;
		return getOptions().getSearchLocation();
	}

	/*
	 * Method getBestMatch.
	 * @param enabledFeatures
	 * @param identifier
	 * @param options
	 * @return Object
	 */
	private IFeatureReference getBestMatch(IConfiguredSite configuredSite) throws CoreException {
		FeatureReference newRef = null;

		if (configuredSite==null) return this;
		IFeatureReference[] enabledFeatures = configuredSite.getConfiguredFeatures();

		// find the best feature based on match from enabled features
		for (int ref = 0; ref < enabledFeatures.length; ref++) {
			if (enabledFeatures[ref] != null) {
				VersionedIdentifier id = null;
				try {
					id = enabledFeatures[ref].getVersionedIdentifier();
				} catch (CoreException e) {
					UpdateManagerPlugin.warn(null, e);
				};
				if (matches(getVersionedIdentifier(), id, getOptions())) {
					if (newRef == null || id.getVersion().isGreaterThan(newRef.getVersionedIdentifier().getVersion())) {
						newRef = new FeatureReference(enabledFeatures[ref]);
						newRef.setOptions(getOptions());
					}
				}
			}
		}

		if (newRef != null)
			return newRef;
		else 
			return this;
	}

	/**
	 * Method isDisabled.
	 * @return boolean
	 */
	private boolean isDisabled() {
		/*IConfiguredSite cSite = getSite().getConfiguredSite();
		if (cSite==null) return false;
		IFeatureReference[] configured = cSite.getConfiguredFeatures();
		for (int i = 0; i < configured.length; i++) {
			if (this.equals(configured[i])) return false;
		}
		return true;*/
		// FIXME
		return false;
	}

	/**
	* Method matches.
	* @param identifier
	* @param id
	* @param options
	* @return boolean
	*/
	private boolean matches(VersionedIdentifier baseIdentifier, VersionedIdentifier id, IncludedFeatureReference options) {
		if (baseIdentifier == null || id == null)
			return false;
		if (!id.getIdentifier().equals(baseIdentifier.getIdentifier()))
			return false;
		int match = IImport.RULE_PERFECT;
		if (options != null) {
			match = options.getMatch();
		}

		switch (match) {
			case IImport.RULE_PERFECT :
				return id.getVersion().isPerfect(baseIdentifier.getVersion());
			case IImport.RULE_COMPATIBLE :
				return id.getVersion().isCompatibleWith(baseIdentifier.getVersion());
			case IImport.RULE_EQUIVALENT :
				return id.getVersion().isEquivalentTo(baseIdentifier.getVersion());
			case IImport.RULE_GREATER_OR_EQUAL :
				return id.getVersion().isGreaterOrEqualTo(baseIdentifier.getVersion());
		}
		UpdateManagerPlugin.warn("Unknown matching rule:" + match);
		return false;
	}

	/**
	 * Method retrieveEnabledFeatures.
	 * @param site
	 */
	private IFeatureReference[] retrieveEnabledFeatures(ISite site) {
		IConfiguredSite configuredSite = site.getCurrentConfiguredSite();
		if (configuredSite == null)
			return new IFeatureReference[0];
		return configuredSite.getConfiguredFeatures();
	}
	/**
	 * @see org.eclipse.update.core.IFeatureReference#getFeature(boolean)
	 */
	public IFeature getFeature(boolean perfectMatch,IConfiguredSite configuredSite) throws CoreException {

		if (configuredSite==null)
			configuredSite = getSite().getCurrentConfiguredSite();
		
		// if perfect match is asked or if the feature is disabled
		// we return the exact match 		
		if (perfectMatch || getMatch() == IImport.RULE_PERFECT || isDisabled()) {
			return getFeature(this);
		} else {
			if (feature == null) {
				// find best match
				IFeatureReference bestMatch = getBestMatch(configuredSite);
				feature = getFeature(bestMatch);
			}
			return feature;
		}
	}

	/*
	 * 
	 */
	private IFeature getFeature(IFeatureReference ref) throws CoreException {
		String type = getType();
		if (type == null || type.equals("")) { //$NON-NLS-1$
			// ask the Site for the default type 
			type = getSite().getDefaultPackagedFeatureType();
		}
		return getSite().createFeature(type, ref.getURL());
	}

}