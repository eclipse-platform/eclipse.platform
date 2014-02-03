/*******************************************************************************
 *  Copyright (c) 2000, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.search;

import java.net.*;
import java.util.Vector;

import org.eclipse.update.internal.search.*;

/**
 * This class encapsulates update scope of the update search.
 * Sites that need to be visited should be added to the scope.
 * If some categories should be skipped, their names must be
 * passed as array of strings to the method.
 *
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @see UpdateSearchRequest
 * @since 3.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public class UpdateSearchScope {
	private Vector sites;
	private URL updateMapURL;
	private boolean isFeatureProvidedSitesEnabled = true;
	
	private static class UpdateSearchSite
		extends UpdateSiteAdapter
		implements IUpdateSearchSite {
		private String[] categoriesToSkip;

		public UpdateSearchSite(
			String label,
			URL siteURL,
			String[] categoriesToSkip) {
			super(label, siteURL);
			this.categoriesToSkip = categoriesToSkip;
		}
		public String[] getCategoriesToSkip() {
			return categoriesToSkip;
		}
	}

	/**
	 * The default constructor.
	 */
	public UpdateSearchScope() {
		sites = new Vector();
	}

	/**
	 * Sets the optional URL of the update map file. This file
	 * is used to redirect search for new updates to other
	 * servers and is typically used when a local
	 * update site proxy (possibly behind the firewall) is
	 * set up.
	 * @param url the url of the Java properties file that
	 * contains the redirection information.
	 */
	
	public void setUpdateMapURL(URL url) {
		this.updateMapURL = url;
	}
	
	/**
	 * Returns the optional URL of the update map file. By 
	 * default, no map file is set.
	 * @return the URL of the map file or <samp>null</samp>
	 * if not set.
	 */
	
	public URL getUpdateMapURL() {
		return updateMapURL;
	}

	/**
	 * Adds the site to scan to the search scope.
	 * @param label the presentation name of the site to visit.
	 * @param siteURL the URL of the site to visit.
	 * @param categoriesToSkip an array of category names that should be skipped or <samp>null</samp> if all features should be considered.
	 */
	public void addSearchSite(
		String label,
		URL siteURL,
		String[] categoriesToSkip) {
		sites.add(new UpdateSearchSite(label, siteURL, categoriesToSkip));
	}

	/**
	 * Returns the sites that should be visited during the search.
	 * @return an array of site adapters
	 */
	public IUpdateSearchSite[] getSearchSites() {
		return (UpdateSearchSite[]) sites.toArray(
			new UpdateSearchSite[sites.size()]);
	}
	
	/**
	 * In addition to the sites added by addSearchSite(), features contribute their own update url's.
	 * This method returns true if those sites are also searched.
	 * @return true if update site provided by features are also searched. Default is true.
	 */
	public boolean isFeatureProvidedSitesEnabled(){
		return isFeatureProvidedSitesEnabled;
	}

	/**
	 * Enable or disable searching of feature provided update sites. 
	 * If disabled, only sites added by addSearchSite() are searched.
	 * @param enable false to disable searching of feature provided sites. By default, these sites are searched.
	 */
	public void setFeatureProvidedSitesEnabled(boolean enable){
		this.isFeatureProvidedSitesEnabled = enable;
	}
}
