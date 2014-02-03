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
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Date;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.update.configurator.IPlatformConfiguration;
import org.eclipse.update.core.ISite;
import org.eclipse.update.core.SiteFeatureReference;
import org.eclipse.update.core.SiteManager;
import org.eclipse.update.core.model.SiteModel;
import org.eclipse.update.internal.configurator.FeatureEntry;
import org.eclipse.update.internal.configurator.PlatformConfiguration;
import org.eclipse.update.internal.configurator.SiteEntry;
import org.eclipse.update.internal.core.BaseSiteLocalFactory;
import org.eclipse.update.internal.core.Messages;
import org.eclipse.update.internal.core.UpdateCore;
import org.eclipse.update.internal.core.UpdateManagerUtils;


/**
 * parse the default site.xml
 */

public class InstallConfigurationParser {
	private PlatformConfiguration platformConfig;
	private URL siteURL;
	private InstallConfigurationModel config;
	private ConfiguredSiteModel configSite;

	/**
	 * Constructor for DefaultSiteParser
	 */
	public InstallConfigurationParser(
		IPlatformConfiguration platformConfig,
		InstallConfigurationModel config, boolean light)
		throws IOException, CoreException {

		Assert.isTrue(platformConfig instanceof PlatformConfiguration);
		this.platformConfig = (PlatformConfiguration)platformConfig;
		
		this.config = config;

		// DEBUG:		
		if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_PARSING) {
			UpdateCore.debug("Start parsing Configuration:" + (config).getURL().toExternalForm()); //$NON-NLS-1$
		}
		
		if (light) {
			processConfigurationLight(this.platformConfig);
		} else {
			processConfig(this.platformConfig);
		}
	}
	
	



	/** 
	 * process the Site info
	 */
	private void processSite(SiteEntry siteEntry) throws CoreException, IOException {

		//site url
		siteURL = siteEntry.getURL();
		try {
			siteURL = FileLocator.toFileURL(siteURL);
			// TODO workaround bug in platform url resolution
			if (siteURL.getProtocol().equals("file")) //$NON-NLS-1$
				siteURL = new File(siteURL.getFile()).toURL();
		} catch (IOException e) {
			// keep original url
		}

		// policy
		ISite site = SiteManager.getSite(siteURL,null);

		// configuration site
		BaseSiteLocalFactory factory = new BaseSiteLocalFactory();
		configSite = factory.createConfigurationSiteModel((SiteModel) site, siteEntry.getSitePolicy().getType());

		//platform url
		configSite.setPlatformURLString(siteEntry.getURL().toExternalForm());
		
		// configured
		configSite.setEnabled(siteEntry.isEnabled());

		// check if the site exists and is updatable
	 	configSite.setUpdatable(siteEntry.isUpdateable());
	 	
		// add to install configuration
	 	config.addConfigurationSiteModel(configSite);
	 	configSite.setInstallConfigurationModel(config);
		
		FeatureEntry[] features = siteEntry.getFeatureEntries();
		for (int i=0; i<features.length; i++) {
			processFeature(features[i]);
		}
	}

	/** 
	 * process the DefaultFeature info
	 */
	private void processFeature(FeatureEntry feature) throws CoreException, IOException {

		// url
		String path = feature.getURL(); 
		URL url = UpdateManagerUtils.getURL(siteURL, path, null);

		if (url != null) {
			SiteFeatureReference ref = new SiteFeatureReference();
			ref.setSite((ISite) configSite.getSiteModel());
			ref.setURL(url);
			(configSite.getConfigurationPolicyModel()).addConfiguredFeatureReference(ref);

			//updateURL
//TODO do we need the update url and to resolve it?
//			String updateURLString = attributes.getValue("updateURL"); //$NON-NLS-1$
//			URLEntry entry = new URLEntry();
//			entry.setURLString(updateURLString);
//			entry.resolve(siteURL,null);

			// DEBUG:		
			if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_PARSING) {
				UpdateCore.debug("End Processing DefaultFeature Tag: url:" + url.toExternalForm()); //$NON-NLS-1$
			}

		} else {
			UpdateCore.log( Messages.InstallConfigurationParser_FeatureReferenceNoURL, new Exception()); 
		}

	}

	/** 
	 * process the Activity info
	 */
//	private void processActivity(Attributes attributes) {
//
//		// action
//		String actionString = attributes.getValue("action"); //$NON-NLS-1$
//		int action = Integer.parseInt(actionString);
//
//		// create
//		ConfigurationActivityModel activity =
//			new BaseSiteLocalFactory().createConfigurationActivityModel();
//		activity.setAction(action);
//
//		// label
//		String label = attributes.getValue("label"); //$NON-NLS-1$
//		if (label != null)
//			activity.setLabel(label);
//
//		// date
//		String dateString = attributes.getValue("date"); //$NON-NLS-1$
//		Date date = new Date(Long.parseLong(dateString));
//		activity.setDate(date);
//
//		// status
//		String statusString = attributes.getValue("status"); //$NON-NLS-1$
//		int status = Integer.parseInt(statusString);
//		activity.setStatus(status);
//
//		config.addActivityModel(activity);
//
//		// DEBUG:		
//		if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_PARSING) {
//			UpdateCore.debug("End Processing Activity: action:" + actionString + " label: " + label + " date:" + dateString + " status" + statusString); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
//		}
//
//	}

	/** 
	 * process the Config info
	 */
	private void processConfig(PlatformConfiguration platformConfig) throws IOException, CoreException {

		// date
		processConfigurationLight(platformConfig);
		
		//timeline
//		String timelineString = attributes.getValue("timeline"); //$NON-NLS-1$
//		long timeline = config.getCreationDate().getTime();
//		if (timelineString!=null) {
//			timeline = Long.parseLong(timelineString);
//		}
//		config.setTimeline(timeline);

		SiteEntry[] sites = platformConfig.getConfiguration().getSites();
		for (int i=0; i<sites.length; i++)
			processSite(sites[i]);

	}



	private void processConfigurationLight(PlatformConfiguration platformConfig) {
		Date date = new Date(platformConfig.getChangeStamp());
		config.setCreationDate(date);
		config.setLabel(date.toString());
		
		config.setCurrent( date.equals(org.eclipse.update.internal.configurator.PlatformConfiguration.getCurrent().getConfiguration().getDate()));
	}
	
}
