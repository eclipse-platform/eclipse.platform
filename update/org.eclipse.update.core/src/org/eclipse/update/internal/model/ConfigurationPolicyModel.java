/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.model;

import java.net.*;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.update.core.*;
import org.eclipse.update.core.model.*;
import org.eclipse.update.internal.core.*;

/**
 * 
 */
public class ConfigurationPolicyModel extends ModelObject {

	
	

	private int policy;
	private Map /* of FeatureReferenceModel */configuredFeatureReferences;
	private Map /* of FeatureReferenceModel */unconfiguredFeatureReferences;
	
	// since 2.0.2
	private ConfiguredSiteModel configuredSiteModel;
	
	// since 2.1
	private boolean enable;

	/**
	 * Constructor for ConfigurationPolicyModel.
	 */
	public ConfigurationPolicyModel() {
		super();
		enable = true;
		configuredFeatureReferences = new HashMap();
//		unconfiguredFeatureReferences = new HashMap();		
	}

	/**
	 * @since 2.0
	 */
	public int getPolicy() {
		return policy;
	}

	/**
	 * Sets the policy.
	 * @param policy The policy to set
	 */
	public void setPolicy(int policy) {
		assertIsWriteable();
		this.policy = policy;
	}

	/**
	 * @since 2.0
	 */
	public FeatureReferenceModel[] getConfiguredFeaturesModel() {
		if (configuredFeatureReferences==null || configuredFeatureReferences.isEmpty())
			return new FeatureReferenceModel[0];
		return (FeatureReferenceModel[]) configuredFeatureReferences.keySet().toArray(arrayTypeFor(configuredFeatureReferences.keySet()));
	}

	/**
	 * @since 2.0
	 */
	public FeatureReferenceModel[] getUnconfiguredFeaturesModel() {
		// obtain unconfigured features by comparing configured ones with those installed
		if (unconfiguredFeatureReferences == null
				&& configuredSiteModel != null
				&& configuredSiteModel.getSiteModel() != null) {
			ISite site = (ISite) configuredSiteModel.getSiteModel();
			ISiteFeatureReference[] siteFeatures = site.getFeatureReferences();
			if (siteFeatures.length > getConfiguredFeaturesModel().length) {
				for (int i=0; i<siteFeatures.length; i++) {
					if (!(siteFeatures[i] instanceof SiteFeatureReference))
						continue;
					Iterator iterator = configuredFeatureReferences.keySet().iterator();
					boolean found = false;
					while(!found && iterator.hasNext()) {
						FeatureReferenceModel f = (FeatureReferenceModel)iterator.next();
						if (UpdateManagerUtils.sameURL(f.getURL(), siteFeatures[i].getURL()))
							found = true;
					}
					if (!found)
						addUnconfiguredFeatureReference((SiteFeatureReference)siteFeatures[i]);
				}
			}
		}
		if (unconfiguredFeatureReferences == null
				|| unconfiguredFeatureReferences.isEmpty())
			return new FeatureReferenceModel[0];
		return (FeatureReferenceModel[]) unconfiguredFeatureReferences.keySet()
				.toArray(arrayTypeFor(unconfiguredFeatureReferences.keySet()));
	}

	/**
	 * Gets the configuredSiteModel.
	 * @return Returns a ConfiguredSiteModel
	 * @since 2.0.2
	 */
	public ConfiguredSiteModel getConfiguredSiteModel() {
		return configuredSiteModel;
	}

	/**
	 * Sets the configuredSiteModel.
	 * @param configuredSiteModel The configuredSiteModel to set
	 * @since 2.0.2
	 */
	public void setConfiguredSiteModel(ConfiguredSiteModel configuredSiteModel) {
		this.configuredSiteModel = configuredSiteModel;
	}

	/**
	 * 
	 */
	private boolean remove(FeatureReferenceModel feature, Map list) {
		URL featureURL = feature.getURL();
		boolean found = false;
		Iterator iter = list.keySet().iterator();
		while (iter.hasNext() && !found) {
			FeatureReferenceModel element = (FeatureReferenceModel) iter.next();
			if (UpdateManagerUtils.sameURL(element.getURL(),featureURL)) {
				list.remove(element);
				found = true;
			}
		}
		return found;
	}

	/**
	 * returns an array of string corresponding to plugins file
	 */
	/*package*/

	
	/**
	 * 
	 */
	private void add(FeatureReferenceModel feature, Map list) {
		URL featureURL = feature.getURL();
		boolean found = false;
		Iterator iter = list.keySet().iterator();
		while (iter.hasNext() && !found) {
			FeatureReferenceModel element = (FeatureReferenceModel) iter.next();
			if (UpdateManagerUtils.sameURL(element.getURL(),featureURL)) {
				found = true;
			}
		}

		if (!found) {
			list.put(feature,null);
		} else {
			UpdateCore.warn("Feature Reference :"+feature+" already part of the list."); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	/**
	 * adds a feature in the configuredReference list
	 * also used by the parser to avoid creating another activity
	 */
	public void addConfiguredFeatureReference(FeatureReferenceModel feature) {
		assertIsWriteable();
		
		if (configuredFeatureReferences == null)
			this.configuredFeatureReferences = new HashMap();
		if (!configuredFeatureReferences.containsKey(feature)){
			//DEBUG:
			if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_CONFIGURATION){
				UpdateCore.debug("Configuring "+feature.getURLString()); //$NON-NLS-1$
			}
			this.add(feature, configuredFeatureReferences);
		}	

		// when user configure a feature,
		// we have to remove it from unconfigured feature if it exists
		// because the user doesn't know...
		if (unconfiguredFeatureReferences != null) {
			boolean success = remove(feature, unconfiguredFeatureReferences);
			if (!success)
				UpdateCore.warn("Feature not part of Unconfigured list: "+feature.getURLString());			 //$NON-NLS-1$
		}

	}

	/**
	 * adds a feature in the list
	 * also used by the parser to avoid creating another activity
	 */
	public void addUnconfiguredFeatureReference(FeatureReferenceModel feature) {
		assertIsWriteable();
		if (unconfiguredFeatureReferences == null)
			this.unconfiguredFeatureReferences = new HashMap();
		if (!unconfiguredFeatureReferences.containsKey(feature)){
			if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_CONFIGURATION){
				UpdateCore.debug("Unconfiguring "+feature.getURLString()); //$NON-NLS-1$
			}
			this.add(feature, unconfiguredFeatureReferences);
		}	

		// an unconfigured feature is always from a configured one no ?
		// unless it was parsed right ?
		if (configuredFeatureReferences != null) {
			boolean success = remove(feature, configuredFeatureReferences);
			if (!success)
				UpdateCore.warn("Feature not part of Configured list: "+feature.getURLString());				 //$NON-NLS-1$
		}
	}

	/**
	 * removes a feature from any list
	 */
	public void removeFeatureReference(FeatureReferenceModel feature) {
		assertIsWriteable();
		if (unconfiguredFeatureReferences!=null){
			boolean success = remove(feature, unconfiguredFeatureReferences);
			if (!success)
				UpdateCore.warn(feature.getURLString()+" not part of unconfigured list.");							 //$NON-NLS-1$
		}

		if (configuredFeatureReferences != null) {
			boolean success = remove(feature, configuredFeatureReferences);
			if (!success)
				UpdateCore.warn(feature.getURLString()+" not part of configured list.");							 //$NON-NLS-1$
		}
	}
	
	/**
	 * Sets the unconfiguredFeatureReferences.
	 * @param featureReferences The unconfiguredFeatureReferences to set
	 */
	protected void setUnconfiguredFeatureReferences(IFeatureReference[] featureReferences) {
		unconfiguredFeatureReferences = new HashMap();
		for (int i = 0; i < featureReferences.length; i++) {
			unconfiguredFeatureReferences.put(featureReferences[i],null);
		}
	}


	/**
	 * Sets the configuredFeatureReferences.
	 * @param featureReferences The configuredFeatureReferences to set
	 */
	protected void setConfiguredFeatureReferences(IFeatureReference[] featureReferences) {
		configuredFeatureReferences = new HashMap();
		for (int i = 0; i < featureReferences.length; i++) {
			configuredFeatureReferences.put(featureReferences[i],null);
		}		
	
	}

	/**
	 * @return boolean
	 */
	public boolean isEnabled() {
		return enable;
	}

	/**
	 * @param value
	 */
	public void setEnabled(boolean value) {
		enable = value;
	}


	
}
