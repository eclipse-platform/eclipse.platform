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
package org.eclipse.update.standalone;
import java.io.*;
import java.net.*;

import org.eclipse.core.runtime.*;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.*;


/**
 * Command to list all installed features.
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @since 3.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public class ListFeaturesCommand extends ScriptedCommand {
	private IConfiguredSite[] sites = getConfiguration().getConfiguredSites();
	
	/**
	 * @param fromSite if specified, list only the features from the specified local install site
	 */
	public ListFeaturesCommand(String fromSite) throws Exception {
		try {
			if (fromSite != null) {
				File sitePath = new File(fromSite);
				if (!sitePath.exists())
					throw new Exception(Messages.Standalone_noSite + fromSite); 
					
				URL fromSiteURL = sitePath.toURL();
				ISite site = SiteManager.getSite(fromSiteURL, null);
				if (site == null) {
					throw new Exception(Messages.Standalone_noSite + fromSite); 
				}
				IConfiguredSite csite = site.getCurrentConfiguredSite();
				if (csite == null)
					throw new Exception(Messages.Standalone_noConfiguredSite + fromSite); 
				sites = new IConfiguredSite[] { csite };
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
				for (int i = 0; i < sites.length; i++) {
					System.out.println("Site: " + sites[i].getSite().getURL()); //$NON-NLS-1$
					IFeatureReference[] features = sites[i]
							.getFeatureReferences();
					for (int f = 0; f < features.length; f++) {
						boolean configured = sites[i].isConfigured(features[f]
								.getFeature(null));
						System.out.println("  Feature: " //$NON-NLS-1$
								+ features[f].getVersionedIdentifier()
										.getIdentifier()
								+ " " //$NON-NLS-1$
								+ features[f].getVersionedIdentifier()
										.getVersion() + "  " //$NON-NLS-1$
								+ (configured ? "enabled" : "disabled")); //$NON-NLS-1$ //$NON-NLS-2$
					}
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
