package org.eclipse.update.internal.core;

/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */ 

import org.eclipse.update.core.ISite;

/**
 * A default implementation for IFeatureContentConsumer
 * </p>
 * @since 2.0
 */

public abstract class SiteContentConsumer implements ISiteContentConsumer {
	
	private ISite site;
	
	/*
	 * @see ISiteContentConsumer#setSite(ISite)
	 */
	/**
	 * Sets the site.
	 * @param site The site to set
	 */
	public void setSite(ISite site) {
		this.site = site;
	}

	/**
	 * Gets the site.
	 * @return Returns a ISite
	 */
	public ISite getSite() {
		return site;
	}

}
