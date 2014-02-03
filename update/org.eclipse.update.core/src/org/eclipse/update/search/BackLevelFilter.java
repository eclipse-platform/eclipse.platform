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

import org.eclipse.core.runtime.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.operations.*;

/**
 * This class can be added to the update search request
 * to filter out features that are back-level (are
 * older or the same as the features already installed).
 * 
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @see UpdateSearchRequest
 * @see IUpdateSearchFilter
 * @since 3.0
 * @deprecated The org.eclipse.update component has been replaced by Equinox p2.
 * This API will be deleted in a future release. See bug 311590 for details.
 */
public class BackLevelFilter extends BaseFilter {
	
	public boolean accept(IFeature match) {
		
			PluginVersionIdentifier matchVid = match.getVersionedIdentifier().getVersion();
			IFeature [] installed = UpdateUtils.getInstalledFeatures(match.getVersionedIdentifier(), false);
			if (installed.length==0) return true;
			
			for (int i=0; i<installed.length; i++) {
				PluginVersionIdentifier ivid = installed[i].getVersionedIdentifier().getVersion();
				if (matchVid.isGreaterThan(ivid))
					continue;
				// installed version is the same or newer than
				// the match - filter out
				return false;
			}
			return true;
		
	}
	
	public boolean accept(IFeatureReference match) {		
		try {
			PluginVersionIdentifier matchVid = match.getVersionedIdentifier().getVersion();
			IFeature [] installed = UpdateUtils.getInstalledFeatures(match.getVersionedIdentifier(), false);
			if (installed.length==0) return true;
			
			for (int i=0; i<installed.length; i++) {
				PluginVersionIdentifier ivid = installed[i].getVersionedIdentifier().getVersion();
				if (matchVid.isGreaterThan(ivid))
					continue;
				// installed version is the same or newer than
				// the match - filter out
				return false;
			}
			return true;
		} catch (CoreException e) {
			return false;
		}
	}
}
