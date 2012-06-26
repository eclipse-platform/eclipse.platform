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
package org.eclipse.update.internal.operations;

import org.eclipse.core.runtime.*;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;

/**
 * This utility class attaches as a listened to the provided
 * configured site, and for every unconfigured feature 
 * it tests if it is a patch and cleans up its backup configuration.
 */

public class PatchCleaner {
	private IConfiguredSite csite;
	private SiteListener listener;
	class SiteListener implements IConfiguredSiteChangedListener {
		public void featureInstalled(IFeature feature) {
		}
		public void featureRemoved(IFeature feature) {
			cleanSavedConfigs(feature);
		}
		public void featureConfigured(IFeature feature) {
		}
		public void featureUnconfigured(IFeature feature) {
			cleanSavedConfigs(feature);
		}
	}
	public PatchCleaner(IConfiguredSite csite, IFeature root) {
		this.csite = csite;
		listener = new SiteListener();
		csite.addConfiguredSiteChangedListener(listener);
	}

	public void dispose() {
		csite.removeConfiguredSiteChangedListener(listener);
	}
	private void cleanSavedConfigs(IFeature feature) {
		if (feature.isPatch()) {
			IInstallConfiguration backupConfig = UpdateUtils.getBackupConfigurationFor(feature);
			if (backupConfig!=null) {
				// clean it
				remove(backupConfig);
			}
		}
	}
	private void remove(IInstallConfiguration config) {
		try {
			ILocalSite localSite = SiteManager.getLocalSite();
			localSite.removeFromPreservedConfigurations(config);
		}
		catch (CoreException e) {
		}
	}
}
