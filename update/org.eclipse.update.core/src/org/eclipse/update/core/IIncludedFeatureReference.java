package org.eclipse.update.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */


/**
 * Included Feature reference.
 * A reference to a included feature on a particular update site.
 * <p>
 * Clients may implement this interface. However, in most cases clients should 
 * directly instantiate or subclass the provided implementation of this 
 * interface.
 * </p>
 * @see org.eclipse.update.core.FeatureReference
 * @since 2.0.1
 */
public interface IIncludedFeatureReference extends IFeatureReference {

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
}