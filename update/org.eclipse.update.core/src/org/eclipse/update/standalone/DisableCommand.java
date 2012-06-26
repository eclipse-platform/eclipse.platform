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
import java.lang.reflect.*;
import java.net.*;

import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.*;
import org.eclipse.update.internal.operations.*;
import org.eclipse.update.operations.*;

/**
 * Command to disable (unconfigure) a feature
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
public class DisableCommand extends ScriptedCommand {

	private IConfiguredSite targetSite;
	private IFeature feature;

	public DisableCommand(
		String featureId,
		String version,
		String toSite,
		String verifyOnly)
		throws Exception {

		super(verifyOnly);

		try {
			IConfiguredSite[] sites = getConfiguration().getConfiguredSites();

			// Get site containing the feature to disable
			if (toSite != null) {
				URL toSiteURL = new File(toSite).toURL();
				if (SiteManager.getSite(toSiteURL, null) == null) {
					throw new Exception(Messages.Standalone_noSite + toSite); 
				}
				targetSite =
					SiteManager
						.getSite(toSiteURL, null)
						.getCurrentConfiguredSite();
			}
			if (targetSite == null) {
				for (int i = 0; i < sites.length; i++) {
					if (sites[i].isProductSite()) {
						targetSite = sites[i];
						break;
					}
				}
			}

			IFeature[] features =
				UpdateUtils.searchSite(featureId, targetSite, true);
			if (features == null || features.length == 0) {
				throw new Exception(NLS.bind(Messages.Standalone_noFeatures3, (new String[] { featureId })));
			}
			if (version == null || version.trim().length() == 0)
				feature = features[0]; // pick the first feature
			else
				for (int i = 0; features != null && i < features.length; i++) {
					if (features[i]
						.getVersionedIdentifier()
						.getVersion()
						.toString()
						.equals(version)) {
						feature = features[i];
						break;
					}
				}
			if (feature == null) {
				throw new Exception(NLS.bind(Messages.Standalone_noFeatures4, (new String[] { featureId, version })));
			}

		} catch (MalformedURLException e) {
			throw e;
		} catch (CoreException e) {
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
		if (isVerifyOnly()) {
			status =
				OperationsManager.getValidator().validatePendingUnconfig(
					feature);
			if (status != null && status.getCode() == IStatus.WARNING)
				UpdateCore.log(status);
			return status == null || status.getCode() == IStatus.WARNING;
		}

		final IUnconfigFeatureOperation configOperation =
			OperationsManager.getOperationFactory().createUnconfigOperation(
				targetSite,
				feature);

		try {
			configOperation.execute(monitor, this);
			return true;
		} catch (CoreException e) {
			StandaloneUpdateApplication.exceptionLogged();
			UpdateCore.log(e);
			return false;
		} catch (InvocationTargetException e) {
			StandaloneUpdateApplication.exceptionLogged();
			UpdateCore.log(e);
			return false;
		}
	}

}
