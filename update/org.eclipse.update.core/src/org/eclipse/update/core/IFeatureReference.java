package org.eclipse.update.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
/**
 * Feature reference.
 * A reference to a feature on a particular update site.
 * <p>
 * Clients may implement this interface. However, in most cases clients should 
 * directly instantiate or subclass the provided implementation of this 
 * interface.
 * </p>
 * @see org.eclipse.update.core.FeatureReference
 * @since 2.0
 */
public interface IFeatureReference extends IAdaptable {

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
	 * Returns an array of categories the referenced feature belong to.
	 * 
	 * @return an array of categories, or an empty array
	 * @since 2.0 
	 */
	public ICategory[] getCategories();

	/**
	 * Returns the referenced feature.
	 * This is a factory method that creates the full feature object.
	 * 
	 * @return the referenced feature
	 * @since 2.0 
	 */
	public IFeature getFeature() throws CoreException;

	/**
	 * Returns the feature identifier.
	 * 
	 * @return the feature identifier.
	 * @exception CoreException
	 * @since 2.0 
	 */
	public VersionedIdentifier getVersionedIdentifier() throws CoreException;

	/**
	 * Adds a category to the referenced feature.
	 * 
	 * @param category new category
	 * @since 2.0 
	 */
	public void addCategory(ICategory category);

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
	 * Returns <code>true</code> if the feature is optional, <code>false</code> otherwise.
	 * 
	 * @return boolean
	 * @since 2.0.1
	 */
	public boolean isOptional();

	/**
	 * Returns the name of the feature reference.
	 * 
	 * @return feature reference name
	 * @since 2.0.1
	 */
	public String getName();	
	
	
		/**
	 * Returns the matching rule for this included feature.
	 * The rule will determine the ability of the included feature to move version 
	 * without causing the overall feature to appear broken.
	 * 
	 * The default is <code>MATCH_PERFECT</code>
	 * 
	 * @see IImport#RULE_PERFECT
	 * @see IImport#RULE_EQUIVALENT
	 * @see IImport#RULE_COMPATIBLE
	 * @see IImport#RULE_GREATER_OR_EQUAL
	 * @return int representation of feature matching rule.
	 * @since 2.0.2
	 */
	public int getMatch();
	
	/**
	 * If the included feature is not updatable, we cannot install or enable another version, unless the 
	 * root feature installs it. 
	 * 
	 * The default is <code>true</code>.
	 *
	 * @return <code>true</code> if a new version of the feature can be installed and enabled,
	 * <code>false  </code>otherwise.
	 * @since 2.0.2
	 */
	public boolean isUpdateAllowed();

	/**
	 * Returns the search location for this included feature.
	 * The location will be used to search updates for this feature.
	 * 
	 * The default is <code>SEARCH_ROOT</code>
	 * 
	 * @see IFeatureReference#SEARCH_ROOT
	 * @see IFeatureReference#SEARCH_SELF
	 * @return int representation of feature searching rule.
	 * @since 2.0.2
	 */

	public int getSearchLocation();
}