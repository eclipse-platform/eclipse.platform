/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.core.model;

import org.eclipse.update.core.*;


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
	private int searchLocation;
	
	// Reuse the fields defined in FeatureReferenceModel, do not duplicate them.
//	// since 2.1
//	private String os;
//	private String ws;
//	private String arch;
//	private String nl;
	
	/**
	 * Construct a included feature reference
	 * 
	 * @since 2.1
	 */
	public IncludedFeatureReferenceModel() {
		super();
		isOptional(false);
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
		setLabel(includedFeatureRef.getLabel());
		setSearchLocation(includedFeatureRef.getSearchLocation());
		setArch(includedFeatureRef.getOSArch());
		setWS(includedFeatureRef.getWS());
		setOS(includedFeatureRef.getOS());
	}

	/**
	 * Constructor IncludedFeatureReferenceModel.
	 * @param featureReference
	 */
	public IncludedFeatureReferenceModel(IFeatureReference featureReference) {
		super((FeatureReferenceModel)featureReference);
		isOptional(false);
		setSearchLocation(IUpdateConstants.SEARCH_ROOT);
		setLabel(getLabel());		
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
	 * @deprecated since 3.0 included feature version is exactly specified
	 */
	public int getMatch(){
		return Import.RULE_PERFECT;
	}

	/**
	 * Returns the search location for this included feature.
	 * The location will be used to search updates for this feature.
	 * 
	 * The default is <code>SEARCH_ROOT</code>
	 * 
	 * @see IUpdateConstants#SEARCH_ROOT
	 * @see IUpdateConstants#SEARCH_SELF
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
	 * @deprecated since 3.0 included feature version is exactly specified
	 */
	public void setMatchingRule(int matchingRule) {
	}

	/**
	 * Sets the searchLocation.
	 * @param searchLocation The searchLocation to set
	 */
	public void setSearchLocation(int searchLocation) {
		this.searchLocation = searchLocation;
	}

}
