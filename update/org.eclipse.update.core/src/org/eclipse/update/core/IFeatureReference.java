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
	
}