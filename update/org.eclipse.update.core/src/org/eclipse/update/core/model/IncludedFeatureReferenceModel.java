package org.eclipse.update.core.model;

import org.eclipse.update.core.*;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */

/**
 * Included Feature reference model object.
 * <p>
 * This class may be instantiated or subclassed by clients. However, in most 
 * cases clients should instead instantiate or subclass the provided 
 * concrete implementation of this model.
 * </p>
 * @see org.eclipse.update.core.IncludedFeatureReference
 * @since 2.1
 */
public class IncludedFeatureReferenceModel extends FeatureReference {

	// since 2.0.2
	private boolean isOptional;
	private String name;	
	private int matchingRule;
	private int searchLocation;
	
	// since 2.1
	private String os;
	private String ws;
	private String arch;
	private String nl;
	
	/**
	 * Construct a included feature reference
	 * 
	 * @since 2.1
	 */
	public IncludedFeatureReferenceModel() {
		super();
		isOptional(false);
		setMatchingRule(IImport.RULE_PERFECT);
		setSearchLocation(IUpdateConstants.SEARCH_ROOT);
	}
	
	
	
	/**
	 * Construct a included feature reference model
	 * 
	 * @param includedFeatureRef the included reference model to copy
	 * @since 2.1
	 */
	public IncludedFeatureReferenceModel(IncludedFeatureReferenceModel includedFeatureRef) {
		super((FeatureReferenceModel)includedFeatureRef);
		isOptional(includedFeatureRef.isOptional());
		setName(includedFeatureRef.getName());
		setMatchingRule(includedFeatureRef.getMatch());
		setSearchLocation(includedFeatureRef.getSearchLocation());
	}

	/**
	 * Constructor IncludedFeatureReferenceModel.
	 * @param featureReference
	 */
	public IncludedFeatureReferenceModel(IFeatureReference featureReference) {
		super((FeatureReferenceModel)featureReference);
		isOptional(false);
		setMatchingRule(IImport.RULE_PERFECT);
		setSearchLocation(IUpdateConstants.SEARCH_ROOT);		
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
	 * Returns a string representation of the feature identifier.
	 * 
	 * @return string representation of feature identifier or <code>null</code>.
	 * @since 2.0.1
	 */
	public String getName() {
		return name;
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
	 * Sets the isOptional.
	 * @param isOptional The isOptional to set
	 */
	public void isOptional(boolean isOptional) {
		this.isOptional = isOptional;
	}

	/**
	 * Sets the matchingRule.
	 * @param matchingRule The matchingRule to set
	 */
	public void setMatchingRule(int matchingRule) {
		this.matchingRule = matchingRule;
	}

	/**
	 * Sets the name.
	 * @param name The name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Sets the searchLocation.
	 * @param searchLocation The searchLocation to set
	 */
	public void setSearchLocation(int searchLocation) {
		this.searchLocation = searchLocation;
	}

	/**
	 * Returns the arch.
	 * @return String
	 */
	public String getOSArch() {
		return arch;
	}

	/**
	 * Returns the os.
	 * @return String
	 */
	public String getOS() {
		return os;
	}

	/**
	 * Returns the ws.
	 * @return String
	 */
	public String getWS() {
		return ws;
	}

	/**
	 * Sets the arch.
	 * @param arch The arch to set
	 */
	public void setArch(String arch) {
		this.arch = arch;
	}

	/**
	 * Sets the os.
	 * @param os The os to set
	 */
	public void setOS(String os) {
		this.os = os;
	}

	/**
	 * Sets the ws.
	 * @param ws The ws to set
	 */
	public void setWS(String ws) {
		this.ws = ws;
	}

	/**
	 * Returns the nl.
	 * @return String
	 */
	public String getNL() {
		return nl;
	}

	/**
	 * Sets the nl.
	 * @param nl The nl to set
	 */
	public void setNL(String nl) {
		this.nl = nl;
	}

}