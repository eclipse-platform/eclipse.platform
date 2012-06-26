/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.core;


import org.eclipse.update.core.*;

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
