/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.standalone;
import java.io.*;
import java.net.*;

import org.eclipse.core.runtime.*;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.*;

/**
 * Adds a new site
 */
public class AddSiteCommand extends ScriptedCommand {
	private ISite site;
	private File sitePath;
	
	/**
	 * @param fromSite if specified, list only the features from the specified local install site
	 */
	public AddSiteCommand(String fromSite) throws Exception {
		try {
			if (fromSite != null) {
				sitePath = new File(fromSite);
				if (!sitePath.exists())
					throw new Exception("Cannot find site: " + fromSite);
					
				URL fromSiteURL = sitePath.toURL();
				site = SiteManager.getSite(fromSiteURL, null);
				if (site == null) {
					throw new Exception(
						"Cannot find site : " + fromSite);
				}
				IConfiguredSite csite = site.getCurrentConfiguredSite();
				if (csite != null)
					throw new Exception("Site is already configured " + fromSite);
			} else {
				throw new Exception("No site specified");
			}
		
		} catch (Exception e) {
			throw e;
		} 
	}

	/**
	 */
	public boolean run(IProgressMonitor monitor) {
			if (site == null)
				return false;
			
			try {
				IConfiguredSite csite = getConfiguration().createConfiguredSite(sitePath);
				getConfiguration().addConfiguredSite(csite);
				// update the sites array to pick up new site
				getConfiguration().getConfiguredSites();
				SiteLocal.getLocalSite().save();
				return true;
			} catch (CoreException e) {
				return false;
			}
	}
}
