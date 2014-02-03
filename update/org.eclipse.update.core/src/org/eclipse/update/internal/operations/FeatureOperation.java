/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.operations;

import org.eclipse.core.runtime.*;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.update.operations.*;


public abstract class FeatureOperation extends Operation implements IFeatureOperation {
	
	protected IFeature feature;
	protected IFeature oldFeature;
	protected IConfiguredSite targetSite;

//	private boolean optionalDelta;
	
	
	public FeatureOperation(IConfiguredSite targetSite, IFeature feature) {
		super();
		this.feature = feature;
		this.targetSite = targetSite;
	}

	public IFeature getFeature() {
		return feature;
	}
	
	public IFeature getOldFeature() {
		return oldFeature;
	}
	
//	public boolean isOptionalDelta() {
//		return optionalDelta;
//	}
	
	public IConfiguredSite getTargetSite() {
		return targetSite;
	}

	public void setTargetSite(IConfiguredSite targetSite) {
		this.targetSite = targetSite;
	}
	

	static boolean unconfigure(IFeature feature, IConfiguredSite site)
		throws CoreException {
		IInstallConfiguration config = SiteManager.getLocalSite().getCurrentConfiguration();
		if (site == null)
			site = UpdateUtils.getConfigSite(feature, config);
		
		if (site != null) {
			PatchCleaner cleaner = new PatchCleaner(site, feature);
			boolean result = site.unconfigure(feature);
			cleaner.dispose();
			return result;
		}
		return false;
	}

	public void setFeature(IFeature feature) {
		this.feature = feature;
	}
	

}
