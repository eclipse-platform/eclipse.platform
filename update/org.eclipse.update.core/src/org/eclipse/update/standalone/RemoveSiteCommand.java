/*******************************************************************************
 *  Copyright (c) 2000, 2012 IBM Corporation and others.
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

import org.eclipse.core.runtime.*;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.*;
import org.eclipse.update.operations.*;


/**
 * Command to remove a product extension site.
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
public class RemoveSiteCommand extends ScriptedCommand {
	private IConfiguredSite csite;
	private File sitePath;
	
	/**
	 * @param toSite if specified, list only the features from the specified local install site
	 */
	public RemoveSiteCommand(String toSite) throws Exception {
		try {
			if (toSite != null) {
				sitePath = new File(toSite);
				if (!sitePath.getName().equals("eclipse")) //$NON-NLS-1$
					sitePath = new File(sitePath, "eclipse"); //$NON-NLS-1$
				if (!sitePath.exists())
					throw new Exception(Messages.Standalone_noSite + toSite); 
					
				IConfiguredSite[] csites = SiteManager.getLocalSite().getCurrentConfiguration().getConfiguredSites();
				for (int i=0; i<csites.length; i++) {
					File f = new File(csites[i].getSite().getURL().getFile());
					if (f.equals(sitePath)) {
						csite = csites[i];
						break;
					}
				}
				
				if (csite == null)
					throw new Exception(Messages.Standalone_noConfiguredSite + toSite); 
			} else {
				throw new Exception(Messages.Standalone_noSite3); 
			}
		
		} catch (Exception e) {
			throw e;
		} 
	}

	/**
	 */
	public boolean run(IProgressMonitor monitor) {
		// check if the config file has been modifed while we were running
		IStatus status = OperationsManager.getValidator().validatePlatformConfigValid();
		if (status != null) {
			UpdateCore.log(status);
			return false;
		}	
		try {
			getConfiguration().removeConfiguredSite(csite);
			// update the sites array
			getConfiguration().getConfiguredSites();
			SiteManager.getLocalSite().save();
			return true;
		} catch (CoreException e) {
			UpdateCore.log(e);
			return false;
		}
	}
}
