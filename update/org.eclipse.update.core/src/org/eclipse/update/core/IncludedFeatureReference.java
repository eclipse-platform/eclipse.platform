package org.eclipse.update.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

/**
 * This is a utility class representing the options of a nested feature.
 * Feature will include other features. This class will represent the options of the inclusion.
 * <p>
 * Clients may instantiate; not intended to be subclassed by clients.
 * </p> 
 * @see org.eclipse.update.core.VersionedIdentifier
 * @since 2.0.1
 */
public class IncludedFeatureReference extends FeatureReference implements IIncludedFeatureReference {
	private boolean isOptional;
	private String name;
	
	// since 2.0.2
	private int matchingRule;
	private boolean updateAllowed;
	private int searchLocation;

	/**
	 * Construct a feature options from a string and a boolean
	 * The string is the representation of the name.
	 * The boolean is the representation of the optionality of the nested feature.
	 * 
	 * @param name string representation of the feature
	 * @param isOptional <code>true</code> if the feature is optional, <code>false</code> otherwise.
	 * @since 2.0.1
	 */
	public IncludedFeatureReference(String name, boolean isOptional) {
		this.isOptional = isOptional;
		this.name = name;
		this.matchingRule = IImport.RULE_PERFECT;
		this.updateAllowed=true;
		this.searchLocation=IUpdateConstants.SEARCH_ROOT;
	}


	/**
	 * Construct a feature options 
	 * 
	 * @param name string representation of the feature
	 * @param isOptional <code>true</code> if the feature is optional, <code>false</code> otherwise.
	 * @param matchingRule the matching rule
	 * @param isUpdatable <code>false</code> if the feature cannot be updated.
	 * @param searchLocation the location to search for this feature's updates.
	 * @since 2.0.2
	 */
	public IncludedFeatureReference(String name, boolean isOptional, int matchingRule, boolean isUpdatable, int searchLocation) {
		this.isOptional = isOptional;
		this.name = name;
		this.matchingRule = matchingRule;
		this.updateAllowed= isUpdatable;
		this.searchLocation=searchLocation;
	}

	/**
	 * Returns the isOptional
	 * 
	 * @return isOptional
	 * @since 2.0.1
	 */
	public boolean isOptional() {
		return isOptional;
	}

	/**
	 * Returns a string representation of the feature identifier.
	 * 
	 * @return string representation of feature identifier or <code>null</code>.
	 * @since 2.0.1
	 */
	public String getName() {
		return name;
	}

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
	public int getMatch(){
		return matchingRule;
	}
	
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
	public boolean isUpdateAllowed() {
		return updateAllowed;
	}

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

	public int getSearchLocation(){
		return searchLocation;
	}
}