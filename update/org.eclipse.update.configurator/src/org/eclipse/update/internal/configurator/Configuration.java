/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     James D Miles (IBM Corp.) - bug 176250, Configurator needs to handle more platform urls 
 *******************************************************************************/
package org.eclipse.update.internal.configurator;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.eclipse.core.runtime.*;
import org.eclipse.update.configurator.*;
import org.w3c.dom.*;

public class Configuration implements IConfigurationConstants {
	
	private HashMap sites = new HashMap();
	private HashMap platformURLs = new HashMap();
	private Date date;
	private long lastModified; // needed to account for file system limitations
	private URL url;
	private boolean transientConfig;
	private boolean isDirty;
	private Configuration linkedConfig; // shared configuration
	private URL associatedInstallURL = Utils.getInstallURL();
	
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
		url = Utils.canonicalizeURL(url);
		// only add the same site once
		if (sites.get(url) == null && (linkedConfig == null || linkedConfig.sites.get(url) == null)) {
			site.setConfig(this);
			sites.put(url, site);
			if(url.startsWith("platform:")){//$NON-NLS-1$
				URL pURL;
				try {
					URL relSite= null;
					if (url != null && url.startsWith("platform:/config")) {
						// url for location of configuration is relative to platform.xml
						URL config_loc = getURL();
						relSite = new URL(config_loc, "..");
					}else{
						relSite = getInstallURL();
					}
					
					pURL = new URL(url);
					URL rURL = PlatformConfiguration.resolvePlatformURL(pURL, relSite);
					String resolvedURL = rURL.toExternalForm();
					platformURLs.put(resolvedURL, pURL);
				} catch (IOException e) {
					// can't resolve so can't have look up.
				}
			}
		}
	}
	
	public void removeSiteEntry(String url) {
		url =Utils.canonicalizeURL(url);		
		sites.remove(url);
		if(url.startsWith("platform:")){ //$NON-NLS-1$
			URL pURL;
			try {
				URL relSite= null;
				if (url != null && url.startsWith("platform:/config")) {
					// url for location of configuration is relative to platform.xml
					URL config_loc = getURL();
					relSite = new URL(config_loc, "..");
				}else{
					relSite = getInstallURL();
				}
				
				pURL = new URL(url);
				URL rURL = PlatformConfiguration.resolvePlatformURL(pURL, relSite);
				String resolvedURL = rURL.toExternalForm();
				platformURLs.remove(resolvedURL);
			} catch (IOException e) {
				// can't resolve so can't have look up.
			}
		}
	}
	
	public SiteEntry getSiteEntry(String url) {
		url = Utils.canonicalizeURL(url);		
		SiteEntry site = (SiteEntry)sites.get(url);
		if (site == null && linkedConfig != null)
			site = linkedConfig.getSiteEntry(url);
		return site;
	}
	
	public SiteEntry[] getSites() {
		if (linkedConfig == null)
			return (SiteEntry[]) sites.values().toArray(new SiteEntry[sites.size()]);
		ArrayList combinedSites = new ArrayList(sites.values());
		combinedSites.addAll(linkedConfig.sites.values());
		return (SiteEntry[]) combinedSites.toArray(new SiteEntry[combinedSites.size()]);
	}
	
	public Element toXML(Document doc) throws CoreException {	
		try {
			Element configElement = doc.createElement(CFG);

			configElement.setAttribute(CFG_VERSION, VERSION);
			configElement.setAttribute(CFG_DATE, String.valueOf(date.getTime()));
			String transitory = isTransient() ? "true" : "false"; //$NON-NLS-1$ //$NON-NLS-2$
			configElement.setAttribute(CFG_TRANSIENT, transitory);
						
			if (linkedConfig != null) {
				// make externalized URL install relative 
				configElement.setAttribute(CFG_SHARED_URL, Utils.makeRelative(getInstallURL(), linkedConfig.getURL()).toExternalForm());
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
			throw Utils.newCoreException("", e); //$NON-NLS-1$
		} 
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
	
	public void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}
	
	public long lastModified() {
		return (lastModified != 0) ? lastModified : date.getTime();
	}
	
	/**
	 * Returns the url as a platform:/ url, if possible, else leaves it unchanged
	 * @param url
	 * @return
	 */
	public URL asPlatformURL(URL url) {
		try {
			if (url.getProtocol().equals("file")) {//$NON-NLS-1$
				String rUrl = url.toExternalForm();
				URL pUrl = (URL)platformURLs.get(rUrl);
				if(pUrl == null)
					return url;
				return pUrl;
			}
			return url;
		} catch (Exception e) {
			return url;
		}
	}
		
	public URL getInstallURL() {
		return associatedInstallURL;
	}
		
	public void setInstallLocation(URL installURL) {
		associatedInstallURL = installURL;
	}
}
