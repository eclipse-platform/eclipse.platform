/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.configurator;

import java.net.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.update.configurator.*;
import org.w3c.dom.*;


public class Configuration implements IConfigurationConstants {
	
	private HashMap sites = new HashMap();
	private Date date;
	private URL url;
	private String defaultFeature;
	private boolean transientConfig;
	private boolean isDirty;
	private Configuration linkedConfig; // shared configuration
	
	public Configuration() {
		this(new Date());
		// the config is created now or out of a platform.xml without a date
		isDirty = true;
	}
	public Configuration(Date date)  {
		this.date = date;
	}
	
	public void setURL(URL url) {
		this.url = url;
	}
	
	public URL getURL() {
		return url;
	}
	
	public void setLinkedConfig(Configuration linkedConfig) {
		this.linkedConfig = linkedConfig;
		// make all the sites read-only
		SiteEntry[] linkedSites = linkedConfig.getSites();
		for (int i=0; i<linkedSites.length; i++)
			linkedSites[i].setUpdateable(false);
	}
	
	public Configuration getLinkedConfig() {
		return linkedConfig;
	}
	
	/**
	 * @return true if the config needs to be saved
	 */
	public boolean isDirty() {
		return isDirty;
	}
	
	public void setDirty(boolean dirty) {
		isDirty = dirty;
	}
	
	public void addSiteEntry(String url, SiteEntry site) {
		// only add the same site once
		if (sites.get(url) == null && (linkedConfig == null || linkedConfig.sites.get(url) == null))
			sites.put(url, site);
	}
	
	public void removeSiteEntry(String url) {
		sites.remove(url);
	}
	
	public SiteEntry getSiteEntry(String url) {
		SiteEntry site = (SiteEntry)sites.get(url);
		if (site == null && linkedConfig != null)
			site = linkedConfig.getSiteEntry(url);
		return site;
	}
	
	public SiteEntry[] getSites() {
		if (linkedConfig == null)
			return (SiteEntry[]) sites.values().toArray(new SiteEntry[sites.size()]);
		else {
			ArrayList combinedSites = new ArrayList(sites.values());
			combinedSites.addAll(linkedConfig.sites.values());
			return (SiteEntry[]) combinedSites.toArray(new SiteEntry[combinedSites.size()]);
		}
	}
	
	public Element toXML(Document doc) throws CoreException {	
		try {
			Element configElement = doc.createElement(CFG);

			configElement.setAttribute(CFG_VERSION, VERSION);
			// If config is transient, we don't save the timestamp,
			// forcing the config to be read as is, without attempting a reconcile
			if (!isTransient()) {
				configElement.setAttribute(CFG_DATE, date.toString());
				configElement.setAttribute(CFG_TRANSIENT, "false");
			} else {
				configElement.setAttribute(CFG_TRANSIENT, "true"); //$NON-NLS-1$
			}

			if (defaultFeature != null) 
				configElement.setAttribute(CFG_FEATURE_ENTRY_DEFAULT, defaultFeature);
						
			if (linkedConfig != null) {
				configElement.setAttribute(CFG_SHARED_URL, linkedConfig.getURL().toExternalForm());
			}

			// collect site entries
			SiteEntry[] list = (SiteEntry[]) sites.values().toArray(new SiteEntry[0]);
			for (int i = 0; i < list.length; i++) {
				if (linkedConfig != null && linkedConfig.getSiteEntry(list[i].getURL().toExternalForm()) != null)
					continue;
				Element siteElement = list[i].toXML(doc);
				configElement.appendChild(siteElement);
			}
			
			return configElement;
			
		} catch (Exception e) {
			throw Utils.newCoreException("", e);
		} 
	}
	
	public String getDefaultFeature() {
		return defaultFeature;
	}
	
	public void setDefaultFeature(String defaultFeature) {
		this.defaultFeature = defaultFeature;
	}
	
	public boolean isTransient() {
		return transientConfig;
	}
	
	public void setTransient(boolean isTransient) {
		this.transientConfig = isTransient;
	}
	
	public Date getDate() {
		return date;
	}
	
	public void setDate(Date date) {
		this.date = date;
	}
	
	public boolean unconfigureFeatureEntry(IPlatformConfiguration.IFeatureEntry feature) {
		SiteEntry[] sites = getSites();
		for (int i=0; i<sites.length; i++)
			if (sites[i].unconfigureFeatureEntry(feature))
				return true;
		return false;
	}
}
