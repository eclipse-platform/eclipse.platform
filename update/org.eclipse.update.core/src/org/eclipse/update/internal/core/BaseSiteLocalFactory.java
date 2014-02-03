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
package org.eclipse.update.internal.core;
import org.eclipse.update.core.model.*;
import org.eclipse.update.internal.model.*;
/**
 * 
 */
public class BaseSiteLocalFactory {
	/*
	 * 
	 */
	public InstallConfigurationModel createInstallConfigurationModel() {
		return new InstallConfiguration();
	}
	/*
	 * 
	 */
	public ConfigurationActivityModel createConfigurationActivityModel() {
		return new ConfigurationActivity();
	}
	/*
	 * 
	 */
	public ConfiguredSiteModel createConfigurationSiteModel() {
		return new ConfiguredSite();
	}
	/*
	 * 
	 */
	public ConfigurationPolicyModel createConfigurationPolicyModel() {
		return new ConfigurationPolicy();
	}
	/**
	 * 
	 */
	public ConfiguredSiteModel createConfigurationSiteModel(SiteModel site, int policy) {
		//create config site
		ConfiguredSiteModel configSite = this.createConfigurationSiteModel();
		configSite.setSiteModel(site);
		ConfigurationPolicyModel policyModel = this.createConfigurationPolicyModel();
		policyModel.setPolicy(policy);
		configSite.setConfigurationPolicyModel(policyModel);
		((ConfigurationPolicy) policyModel).setConfiguredSiteModel(configSite);
		return configSite;
	}
}
