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
package org.eclipse.update.standalone;
import java.net.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.*;
import org.eclipse.update.internal.operations.*;
import org.eclipse.update.operations.*;
import org.eclipse.update.search.*;

/**
 * Command to udpate and existing feature.
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @since 3.0
 */
public class UpdateCommand extends ScriptedCommand {

	private IConfiguredSite targetSite;
	private UpdateSearchRequest searchRequest;
	private UpdateSearchResultCollector collector;
	private String featureId;
	private String version;
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
			this.version = version;
			if (featureId != null) {
				this.targetSite =
					UpdateUtils.getSiteWithFeature(
						getConfiguration(),
						featureId);
				if (targetSite == null) {
					throw new Exception(Policy.bind("Standalone.noConfigSiteForFeature", featureId)); //$NON-NLS-1$
				}
				IFeature[] currentFeatures =
					UpdateUtils.searchSite(featureId, targetSite, true);
				if (currentFeatures == null || currentFeatures.length == 0) {
					throw new Exception(Policy.bind("Standalone.noFeatures3", featureId)); //$NON-NLS-1$
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
		try {
			monitor.beginTask(Policy.bind("Standalone.updating"), 4);  //$NON-NLS-1$
			searchRequest.performSearch(collector, new SubProgressMonitor(monitor,1));
			IInstallFeatureOperation[] operations = collector.getOperations();
			if (operations == null || operations.length == 0) {
				StandaloneUpdateApplication.exceptionLogged();
				UpdateCore.log(Utilities.newCoreException(Policy.bind("Standalone.noUpdate", featureId),	null));  //$NON-NLS-1$
				return false;
			}
			JobTargetSite[] jobTargetSites =
				new JobTargetSite[operations.length];
			for (int i = 0; i < operations.length; i++) {
				jobTargetSites[i] = new JobTargetSite();
				jobTargetSites[i].job = operations[i];
				jobTargetSites[i].targetSite = operations[i].getTargetSite();
			}

			// Check for duplication conflicts
			ArrayList conflicts =
				DuplicateConflictsValidator.computeDuplicateConflicts(
					jobTargetSites,
					getConfiguration());
			if (conflicts != null) {
				StandaloneUpdateApplication.exceptionLogged();
				UpdateCore.log(Utilities.newCoreException(Policy.bind("Standalone.duplicate"), null)); //$NON-NLS-1$
				return false;
			}

			if (isVerifyOnly())
				return true;

			IBatchOperation installOperation =
				OperationsManager
					.getOperationFactory()
					.createBatchInstallOperation(
					operations);
			try {
				installOperation.execute(new SubProgressMonitor(monitor,3), this);
				System.out.println(
						Policy.bind("Standalone.feature") //$NON-NLS-1$
						+ featureId
						+ " " //$NON-NLS-1$
						+ Policy.bind("Standalone.updated")); //$NON-NLS-1$
				return true;
			} catch (Exception e) {
				StandaloneUpdateApplication.exceptionLogged();
				UpdateCore.log(
					Utilities.newCoreException(
							Policy.bind("Standalone.noUpdate", featureId),  //$NON-NLS-1$
						e));
				return false;
			}
		} catch (CoreException ce) {
			IStatus status = ce.getStatus();
			if (status != null
				&& status.getCode() == ISite.SITE_ACCESS_EXCEPTION) {
				// Just show this but do not throw exception
				// because there may be results anyway.
				System.out.println(Policy.bind("Standalone.connection")); //$NON-NLS-1$
			} else {
				StandaloneUpdateApplication.exceptionLogged();
				UpdateCore.log(ce);
			}
			return false;
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
