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
	
	/**
	 * Returns the matching rule for this included feature.
	 * The rule will determine the ability of the included feature to move version 
	 * without causing the overall feature to appear broken.
	 * 
	 * The default is <code>RULE_PERFECT</code>
	 * 
	 * @see IUpdateConstants#RULE_PERFECT
	 * @see IUpdateConstants#RULE_EQUIVALENT
	 * @see IUpdateConstants#RULE_COMPATIBLE
	 * @see IUpdateConstants#RULE_GREATER_OR_EQUAL
	 * @return int representation of feature matching rule.
	 * @since 2.0.2
	 */
	public int getMatch();
	
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