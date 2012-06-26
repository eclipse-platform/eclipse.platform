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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.update.configuration.IConfiguredSite;
import org.eclipse.update.core.IFeature;
import org.eclipse.update.core.ISite;
import org.eclipse.update.core.SiteManager;
import org.eclipse.update.core.Utilities;
import org.eclipse.update.core.VersionedIdentifier;
import org.eclipse.update.internal.configurator.UpdateURLDecoder;
import org.eclipse.update.internal.core.Messages;
import org.eclipse.update.internal.core.UpdateCore;
import org.eclipse.update.internal.core.UpdateManagerUtils;
import org.eclipse.update.internal.operations.DuplicateConflictsValidator;
import org.eclipse.update.internal.operations.UpdateUtils;
import org.eclipse.update.internal.search.SiteSearchCategory;
import org.eclipse.update.operations.IBatchOperation;
import org.eclipse.update.operations.IInstallFeatureOperation;
import org.eclipse.update.operations.OperationsManager;
import org.eclipse.update.search.BackLevelFilter;
import org.eclipse.update.search.EnvironmentFilter;
import org.eclipse.update.search.IUpdateSearchResultCollector;
import org.eclipse.update.search.UpdateSearchRequest;
import org.eclipse.update.search.UpdateSearchScope;
import org.eclipse.update.search.VersionedIdentifiersFilter;

/**
 * Command to install a feature.
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

			//PAL foundation
			this.remoteSiteURL = new URL(UpdateURLDecoder.decode(fromSite, "UTF-8")); //$NON-NLS-1$

			// Get site to install to
			targetSite = getTargetSite(toSite);
			// if no site, try selecting the site that already has the old feature
			if (targetSite == null)
				targetSite = UpdateUtils.getSiteWithFeature(getConfiguration(), featureId);
			// if still no site, pick the product site, if writeable
			if (targetSite == null) {
				IConfiguredSite[] sites = getConfiguration().getConfiguredSites();
				for (int i = 0; i < sites.length; i++) {
					if (sites[i].isProductSite() && sites[i].isUpdatable()) {
						targetSite = sites[i];
						break;
					}
				}
			}
			// if all else fails, pick the first updateable site
			if (targetSite == null) {
				IConfiguredSite[] sites = getConfiguration().getConfiguredSites();
				for (int i = 0; i < sites.length; i++) {
					if (sites[i].isUpdatable()) {
						targetSite = sites[i];
						break;
					}
				}
			}
			// are we still checking for sites? forget about it
			if (targetSite == null)
				throw Utilities.newCoreException(
						Messages.Standalone_cannotInstall + featureId + " " + version,  //$NON-NLS-1$
						null);
			
			UpdateSearchScope searchScope = new UpdateSearchScope();
			searchScope.addSearchSite(
				NLS.bind( Messages.InstallCommand_site, remoteSiteURL.toExternalForm()),
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
			monitor.beginTask(Messages.Standalone_installing, 4); 
			searchRequest.performSearch(collector, new SubProgressMonitor(monitor,1));
			IInstallFeatureOperation[] operations = collector.getOperations();
			if (operations == null || operations.length == 0) {
				throw Utilities.newCoreException(
					NLS.bind(Messages.Standalone_notFoundOrNewer, featureId + ' ' + version, remoteSiteURL),
					null);
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
				if (operations == null || operations.length == 0)
					return false;
				IStatus status = OperationsManager.getValidator().validatePendingChanges(operations);
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
				System.out.println(NLS.bind(Messages.Standalone_installed, featureId + ' ' + version));
				return true;
			} catch (Exception e) {
				throw Utilities.newCoreException(
					Messages.Standalone_cannotInstall + featureId + " " + version,  //$NON-NLS-1$
					e);
			}
		} catch (CoreException ce) {
			StandaloneUpdateApplication.exceptionLogged();
			UpdateCore.log(ce);
			return false;
		} catch (OperationCanceledException ce) {
			return true;
		} finally {
			monitor.done();
		}
	}

	private IConfiguredSite getTargetSite(String toSite) throws Exception {
		if (toSite == null) 
			return null;
			
		IConfiguredSite[] configuredSites = getConfiguration().getConfiguredSites();
		File sitePath = new File(toSite);
		File secondaryPath = sitePath.getName().equals("eclipse") ? //$NON-NLS-1$
							null : new File(sitePath, "eclipse"); //$NON-NLS-1$

		for (int i = 0; i < configuredSites.length; i++) {
			IConfiguredSite csite = configuredSites[i];
			if (UpdateManagerUtils.sameURL(csite.getSite().getURL(),sitePath.toURL()))
				return csite;
			else if (secondaryPath != null && UpdateManagerUtils.sameURL(csite.getSite().getURL(),secondaryPath.toURL()))
				return csite;
		}

		// extension site not found, need to create one
		if (!sitePath.exists())
			sitePath.mkdirs();
		URL toSiteURL = sitePath.toURL();
		ISite site = SiteManager.getSite(toSiteURL, null);
		if (site == null) {
			throw new Exception(Messages.Standalone_noSite + toSite); 
		}
		IConfiguredSite csite = site.getCurrentConfiguredSite();
		if (csite == null) {
			csite = getConfiguration().createConfiguredSite(sitePath);
			IStatus status = csite.verifyUpdatableStatus();
			if (status.isOK())
				getConfiguration().addConfiguredSite(csite);
			else 
				throw new CoreException(status);

			return csite;
		}
		return csite;
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
