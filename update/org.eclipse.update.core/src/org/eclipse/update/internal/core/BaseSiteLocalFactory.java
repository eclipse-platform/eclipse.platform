package org.eclipse.update.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
import org.eclipse.update.core.model.SiteModel;
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
		// 2.0.2 set configSite into site
		site.setConfiguredSiteModel(configSite);
		return configSite;
	}
}