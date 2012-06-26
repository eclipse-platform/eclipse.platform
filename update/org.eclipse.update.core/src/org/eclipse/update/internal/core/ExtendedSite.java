/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.core;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.update.core.IURLEntry;
import org.eclipse.update.core.VersionedIdentifier;
import org.eclipse.update.internal.model.SiteWithTimestamp;

public class ExtendedSite extends SiteWithTimestamp /*Site*/ {
	
	private boolean digestExist;
	private String[] availableLocals;
	private String digestURL;
	private LiteFeature[] liteFeatures;
	private LiteFeature[] allLiteFeatures;
	private IURLEntry[] associateSites;
	private boolean pack200 = false;
	private IURLEntry selectedMirror;
	
	
	public String getDigestURL() {
		return digestURL;
	}
	public void setDigestURL(String digestURL) {
		this.digestURL = digestURL;
	}

	public String[] getAvailableLocals() {
		return availableLocals;
	}
	public void setAvailableLocals(String[] availableLocals) {
		this.availableLocals = availableLocals;
	}
	public boolean isDigestExist() {
		return digestExist;
	}
	public void setDigestExist(boolean digestExist) {
		this.digestExist = digestExist;
	}
	public LiteFeature[] getLiteFeatures() {
		if (getCurrentConfiguredSite()!=null)
			return filterFeatures(getNonFilteredLiteFeatures());
		else 
			return getNonFilteredLiteFeatures();
	}
	public void setLiteFeatures(LiteFeature[] liteFeatures) {
		
		if ((liteFeatures == null) || (liteFeatures.length == 0))
			return;
		this.allLiteFeatures = liteFeatures;
		List temp = new ArrayList();
		for(int i = 0; i < allLiteFeatures.length ; i++) {
			if (getFeatureReference(allLiteFeatures[i]) != null) {
				temp.add(allLiteFeatures[i]);
			}
		}
		if (!temp.isEmpty()) {
			this.liteFeatures = (LiteFeature[])temp.toArray( new LiteFeature[temp.size()]);
		}
	}
	
	public LiteFeature getLiteFeature(VersionedIdentifier vid) {
		if (allLiteFeatures == null)
			return null;
		
		for(int i = 0; i < allLiteFeatures.length ; i++) {
			if (vid.equals(allLiteFeatures[i].getVersionedIdentifier())) {
				return allLiteFeatures[i];
			}
		}
		return null;
	}
	
	public LiteFeature[] getNonFilteredLiteFeatures() {
		return liteFeatures;
	}
	public void setNonFilteredLiteFeatures(LiteFeature[] liteFeatures) {
		this.liteFeatures = liteFeatures;
	}
	
	
	/**
	 * Get whether or not this site may contain jars packed with pack200.
	 * The packed version of foo.jar, is expected to be foo.jar.pack.gz
	 * @return
	 */
	public boolean supportsPack200() {
		return pack200;
	}
	
	/**
	 * Set whether or not this site may contain jars packed with pack200
	 * The packed version of foo.jar is expected to be foo.jar.pack.gz
	 * @param pack
	 */
	public void setSupportsPack200(boolean pack){
		pack200 = pack;
	}
	
	/*
	 * Method filterFeatures.
	 * Also implemented in Feature
	 *  
	 * @param list
	 * @return List
	 */
	private LiteFeature[] filterFeatures(LiteFeature[] allIncluded) {
		List list = new ArrayList();
		if (allIncluded!=null){
			for (int i = 0; i < allIncluded.length; i++) {
				LiteFeature included = allIncluded[i];
				if (UpdateManagerUtils.isValidEnvironment(included))
					list.add(included);
				else{
					if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_WARNINGS){
						UpdateCore.warn("Filtered out feature reference:"+included); //$NON-NLS-1$
					}
				}
			}
		}
		
		LiteFeature[] result = new LiteFeature[list.size()];
		if (!list.isEmpty()){
			list.toArray(result);
		}
		
		return result;	
	}
	public IURLEntry[] getAssociateSites() {
		return associateSites;
	}
	public void setAssociateSites(IURLEntry[] associateSites) {
		this.associateSites = associateSites;
	}
	
	public IURLEntry getSelectedMirror() {
		return selectedMirror;
	}
	
	public void setSelectedMirror(IURLEntry selectedMirror) {
		this.selectedMirror = selectedMirror;
	}

}
