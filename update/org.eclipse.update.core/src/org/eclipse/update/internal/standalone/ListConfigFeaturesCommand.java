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
 * Lists the configured features.
 */
public class ListConfigFeaturesCommand extends ScriptedCommand {
	private IConfiguredSite[] sites;
	
	/**
	 * @param fromSite if specified, list only the features from the specified local install site
	 */
	public ListConfigFeaturesCommand(String fromSite) throws Exception {
		try {
			if (fromSite != null) {
				File sitePath = new File(fromSite);
				if (!sitePath.exists())
					throw new Exception("Cannot find site: " + fromSite);
					
				URL fromSiteURL = sitePath.toURL();
				ISite site = SiteManager.getSite(fromSiteURL, null);
				if (site == null) {
					throw new Exception(
						"Cannot find site : " + fromSite);
				}
				IConfiguredSite csite = site.getCurrentConfiguredSite();
				if (csite == null)
					throw new Exception("Cannot find configured site: " + fromSite);
				sites = new IConfiguredSite[] { csite };
			} else {
				sites = getConfiguration().getConfiguredSites();
			}
		
		} catch (Exception e) {
			throw e;
		} 
	}

	/**
	 */
	public boolean run(IProgressMonitor monitor) {
			try {
				if (sites != null) {
					for (int i=0; i<sites.length; i++) {
						System.out.println("Site:" + sites[i].getSite().getURL());
						IFeatureReference[] features = sites[i].getConfiguredFeatures();
						for (int f=0; f<features.length; f++)
							System.out.println("  Feature: " + features[f].getVersionedIdentifier());
					}
				}
				return true;
			} catch (CoreException e) {
				StandaloneUpdateApplication.exceptionLogged();
				UpdateCore.log(e);
				return false;
			}
	}
}
