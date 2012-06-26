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
import org.eclipse.osgi.util.NLS;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.Messages;
import org.eclipse.update.operations.*;

/**
 * Configure a feature.
 * ConfigOperation
 */
public class UninstallOperation extends FeatureOperation implements IUninstallFeatureOperation{

	public UninstallOperation(IConfiguredSite site, IFeature feature) {
		super(site, feature);
	}

	public void setTargetSite(IConfiguredSite targetSite) {
		this.targetSite = targetSite;
	}

	public boolean execute(IProgressMonitor pm, IOperationListener listener) throws CoreException {
		
		if (targetSite == null)
			targetSite = UpdateUtils.getConfigSite(feature, SiteManager.getLocalSite().getCurrentConfiguration());

		// Restart not needed
		boolean restartNeeded = false;

		if (targetSite != null) {
			// if needed, unconfigure the feature first
			if (targetSite.isConfigured(feature)) {
				IStatus status = OperationsManager.getValidator().validatePendingUnconfig(feature);
				if (status != null && status.getCode() == IStatus.ERROR)
					throw new CoreException(status);
				if (unconfigure(feature, targetSite))
					restartNeeded = true;
				else
					throw Utilities.newCoreException(NLS.bind(Messages.OperationsManager_error_uninstall, (new String[] { feature.getVersionedIdentifier().toString() })), null);
			}
			targetSite.remove(feature, pm);
		} else {
			// we should do something here
			String message =
				NLS.bind(Messages.OperationsManager_error_uninstall, (new String[] { feature.getLabel() }));
			IStatus status =
				new Status(
					IStatus.ERROR,
					UpdateUtils.getPluginId(),
					IStatus.OK,
					message,
					null);
			throw new CoreException(status);
		}

		markProcessed();
		if (listener != null)
			listener.afterExecute(this, null);

		restartNeeded = SiteManager.getLocalSite().save() && restartNeeded;

		// notify the model
		OperationsManager.fireObjectChanged(feature, UNINSTALL);
		
		return restartNeeded;
	}

}
