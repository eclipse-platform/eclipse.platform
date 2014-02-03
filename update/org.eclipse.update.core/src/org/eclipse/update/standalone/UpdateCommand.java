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

import java.net.*;
import java.util.ArrayList;

import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.*;
import org.eclipse.update.internal.operations.*;
import org.eclipse.update.operations.*;
import org.eclipse.update.search.*;

/**
 * Command to update and existing feature.
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
public class UpdateCommand extends ScriptedCommand {

	private IConfiguredSite targetSite;
	private UpdateSearchRequest searchRequest;
	private UpdateSearchResultCollector collector;
	private String featureId;
	//private String version;
	private IFeature currentFeature;

	/**
	 * Update command for updating a feature to a newer version.
	 * @param featureId
	 * @param verifyOnly
	 * @throws Exception
	 */
	public UpdateCommand(String featureId, String verifyOnly)
	throws Exception {
		this(featureId, null, verifyOnly);
	}
	
	/**
	 * Update command for updating a feature to a specified newer version.
	 * @param featureId
	 * @param version
	 * @param verifyOnly
	 * @throws Exception
	 */
	public UpdateCommand(String featureId, String version, String verifyOnly)
		throws Exception {

		super(verifyOnly);

		try {
			this.featureId = featureId;
			//this.version = version;
			if (featureId != null) {
				this.targetSite =
					UpdateUtils.getSiteWithFeature(
						getConfiguration(),
						featureId);
				if (targetSite == null) {
					throw new Exception(NLS.bind(Messages.Standalone_noConfigSiteForFeature, (new String[] { featureId })));
				}
				IFeature[] currentFeatures =
					UpdateUtils.searchSite(featureId, targetSite, true);
				if (currentFeatures == null || currentFeatures.length == 0) {
					throw new Exception(NLS.bind(Messages.Standalone_noFeatures3, (new String[] { featureId })));
				}
				this.currentFeature = currentFeatures[0];
			} else {
				// Get site to install to
				IConfiguredSite[] sites =
					getConfiguration().getConfiguredSites();
				for (int i = 0; i < sites.length; i++) {
					if (sites[i].isProductSite()) {
						targetSite = sites[i];
						break;
					}
				}
			}
			if (currentFeature == null)
				searchRequest = UpdateUtils.createNewUpdatesRequest(null);
			else {
				searchRequest =
					UpdateUtils.createNewUpdatesRequest(
						new IFeature[] { currentFeature });
				if (version != null)
					searchRequest.addFilter(
						new VersionedIdentifiersFilter(
							new VersionedIdentifier[] { new VersionedIdentifier(featureId, version) }));
			}

			collector = new UpdateSearchResultCollector();

		} catch (MalformedURLException e) {
			StandaloneUpdateApplication.exceptionLogged();
			UpdateCore.log(e);
		} catch (CoreException e) {
			StandaloneUpdateApplication.exceptionLogged();
			UpdateCore.log(e);
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
			monitor.beginTask(Messages.Standalone_updating, 4);  
			searchRequest.performSearch(collector, new SubProgressMonitor(monitor,1));
			IInstallFeatureOperation[] operations = collector.getOperations();
			if (operations == null || operations.length == 0) {
				StandaloneUpdateApplication.exceptionLogged();
				UpdateCore.log(Utilities.newCoreException(NLS.bind(Messages.Standalone_noUpdate, (new String[] { featureId })),	null));
				return false;
			}

			// Check for duplication conflicts
			ArrayList conflicts =
				DuplicateConflictsValidator.computeDuplicateConflicts(
					operations,
					getConfiguration());
			if (conflicts != null) {
				StandaloneUpdateApplication.exceptionLogged();
				UpdateCore.log(Utilities.newCoreException(Messages.Standalone_duplicate, null)); 
				System.out.println(Messages.Standalone_duplicate);
			}
			
			if (isVerifyOnly()) {
				status = OperationsManager.getValidator().validatePendingChanges(operations);
				if (status != null && status.getCode() == IStatus.ERROR)
					throw new CoreException(status);
				else
					return true;
			}

			IBatchOperation installOperation =
				OperationsManager
					.getOperationFactory()
					.createBatchInstallOperation(
					operations);
			try {
				installOperation.execute(new SubProgressMonitor(monitor,3), this);
				System.out.println(NLS.bind(Messages.Standalone_updated, featureId));
				return true;
			} catch (Exception e) {
				StandaloneUpdateApplication.exceptionLogged();
				UpdateCore.log(
					Utilities.newCoreException(
							NLS.bind(Messages.Standalone_noUpdate, (new String[] { featureId })),
						e));
				return false;
			}
		} catch (CoreException ce) {
			status = ce.getStatus();
			if (status != null
				&& status.getCode() == ISite.SITE_ACCESS_EXCEPTION) {
				// Just show this but do not throw exception
				// because there may be results anyway.
				System.out.println(Messages.Standalone_connection); 
			} else {
				StandaloneUpdateApplication.exceptionLogged();
				UpdateCore.log(ce);
			}
			return false;
		} catch (OperationCanceledException ce) {
			return true;
		} finally {
			monitor.done();
		}
	}


	class UpdateSearchResultCollector implements IUpdateSearchResultCollector {
		private ArrayList operations = new ArrayList();

		public void accept(IFeature feature) {

			IInstallFeatureOperation op =
				OperationsManager.getOperationFactory().createInstallOperation(
					null,
					feature,
					null,
					null,
					null);

			IConfiguredSite site =
				UpdateUtils.getDefaultTargetSite(getConfiguration(), op);
			if (site == null)
				site = UpdateUtils.getAffinitySite(getConfiguration(), feature);
			if (site == null)
				site = targetSite;

			op.setTargetSite(site);
			operations.add(op);
		}
		public IInstallFeatureOperation[] getOperations() {
			IInstallFeatureOperation[] opsArray =
				new IInstallFeatureOperation[operations.size()];
			operations.toArray(opsArray);
			return opsArray;
		}
	}
}
