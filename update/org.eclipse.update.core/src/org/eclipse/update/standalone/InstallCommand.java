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
import java.io.*;
import java.net.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.*;
import org.eclipse.update.internal.operations.*;
import org.eclipse.update.internal.search.*;
import org.eclipse.update.operations.*;
import org.eclipse.update.search.*;

/**
 * Command to install a feature.
 * <p>
 * <b>Note:</b> This class/interface is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 * @since 3.0
 */
public class InstallCommand extends ScriptedCommand {

	private IConfiguredSite targetSite;
	private UpdateSearchRequest searchRequest;
	private UpdateSearchResultCollector collector;
	private URL remoteSiteURL;
	private String featureId;
	private String version;

	public InstallCommand(
		String featureId,
		String version,
		String fromSite,
		String toSite,
		String verifyOnly)
		throws Exception {

		super(verifyOnly);

		try {
			this.featureId = featureId;
			this.version = version;

			this.remoteSiteURL = new URL(URLDecoder.decode(fromSite, "UTF-8")); //$NON-NLS-1$

			// Get site to install to
			IConfiguredSite[] sites = getConfiguration().getConfiguredSites();
			if (toSite != null) {
				File sitePath = new File(toSite);
				if (!sitePath.exists())
					sitePath.mkdirs();
				URL toSiteURL = sitePath.toURL();
				ISite site = SiteManager.getSite(toSiteURL, null);
				if (site == null) {
					throw new Exception(Policy.bind("Standalone.noSite") + toSite); //$NON-NLS-1$
				}
				targetSite = site.getCurrentConfiguredSite();
				if (targetSite == null) {
					targetSite = getConfiguration().createConfiguredSite(sitePath);
					IStatus status = targetSite.verifyUpdatableStatus();
					if (status.isOK())
						getConfiguration().addConfiguredSite(targetSite);
					else 
						throw new CoreException(status);

					// update the sites array to pick up new site
					sites = getConfiguration().getConfiguredSites();
				}
			}
			if (targetSite == null) {
				for (int i = 0; i < sites.length; i++) {
					if (sites[i].isProductSite()) {
						targetSite = sites[i];
						break;
					}
				}
			}
			UpdateSearchScope searchScope = new UpdateSearchScope();
			searchScope.addSearchSite(
				"remoteSite", //$NON-NLS-1$
				remoteSiteURL,
				new String[0]);

			searchRequest =
				new UpdateSearchRequest(new SiteSearchCategory(), searchScope);
			VersionedIdentifier vid =
				new VersionedIdentifier(featureId, version);
			searchRequest.addFilter(
				new VersionedIdentifiersFilter(
					new VersionedIdentifier[] { vid }));
			searchRequest.addFilter(new EnvironmentFilter());
			searchRequest.addFilter(new BackLevelFilter());

			collector = new UpdateSearchResultCollector();

		} catch (MalformedURLException e) {
			throw e;
		} catch (CoreException e) {
			throw e;
		}
	}

	/**
	 */
	public boolean run(IProgressMonitor monitor) {
		try {
			monitor.beginTask(Policy.bind("Standalone.installing"), 4); //$NON-NLS-1$
			searchRequest.performSearch(collector, new SubProgressMonitor(monitor,1));
			IInstallFeatureOperation[] operations = collector.getOperations();
			if (operations == null || operations.length == 0) {
				throw Utilities.newCoreException(
					Policy.bind("Standalone.feature") //$NON-NLS-1$
						+ featureId
						+ " " //$NON-NLS-1$
						+ version
						+ Policy.bind("Standalone.notFound") //$NON-NLS-1$
						+ remoteSiteURL
						+ Policy.bind("Standalone.newerInstalled"), //$NON-NLS-1$
					null);
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
				throw Utilities.newCoreException(Policy.bind("Standalone.duplicate"), null); //$NON-NLS-1$
			}

			if (isVerifyOnly()) {
				return (operations != null && operations.length > 1);
			}

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
						+ version
						+ Policy.bind("Standalone.installed")); //$NON-NLS-1$
				return true;
			} catch (Exception e) {
				throw Utilities.newCoreException(
					Policy.bind("Standalone.cannotInstall") + featureId + " " + version, //$NON-NLS-1$ //$NON-NLS-2$
					e);
			}
		} catch (CoreException ce) {
			StandaloneUpdateApplication.exceptionLogged();
			UpdateCore.log(ce);
			return false;
		} finally {
			monitor.done();
		}
	}


	class UpdateSearchResultCollector implements IUpdateSearchResultCollector {
		private ArrayList operations = new ArrayList();

		public void accept(IFeature feature) {
			if (feature
				.getVersionedIdentifier()
				.getIdentifier()
				.equals(featureId)
				&& feature
					.getVersionedIdentifier()
					.getVersion()
					.toString()
					.equals(
					version)) {
				operations.add(
					OperationsManager
						.getOperationFactory()
						.createInstallOperation(
						targetSite,
						feature,
						null,
						null,
						null));
			}
		}
		public IInstallFeatureOperation[] getOperations() {
			IInstallFeatureOperation[] opsArray =
				new IInstallFeatureOperation[operations.size()];
			operations.toArray(opsArray);
			return opsArray;
		}
	}
}
