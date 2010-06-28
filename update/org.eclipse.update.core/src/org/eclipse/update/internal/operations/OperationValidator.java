/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.operations;

import org.osgi.framework.BundleContext;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.service.resolver.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.update.configuration.*;
import org.eclipse.update.configurator.ConfiguratorUtils;
import org.eclipse.update.configurator.IPlatformConfiguration;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.configurator.PlatformConfiguration;
import org.eclipse.update.internal.core.Messages;
import org.eclipse.update.internal.core.UpdateCore;
import org.eclipse.update.internal.verifier.PlanVerifier;
import org.eclipse.update.operations.IInstallFeatureOperation;
import org.eclipse.update.operations.IOperationValidator;
import org.osgi.framework.*;

/**
 *  
 */
public class OperationValidator implements IOperationValidator {

	private static ServiceReference verifierServiceReference = null;

	/**
	 * Checks if the platform configuration has been modified outside this program.
	 * @return the error status, or null if no errors
	 */
	public IStatus validatePlatformConfigValid() {
		ArrayList status = new ArrayList(1);
		checkPlatformWasModified(status);

		// report status
		if (status.size() > 0)
			return createMultiStatus(Messages.ActivityConstraints_rootMessage, status, IStatus.ERROR);
		return null;
	}

	/*
	 * Called by UI before performing operation. Returns null if no errors, a
	 * status with IStatus.WARNING code when the initial configuration is
	 * broken, or a status with IStatus.ERROR when there the operation
	 * introduces new errors
	 */
	public IStatus validatePendingInstall(IFeature oldFeature, IFeature newFeature) {
		// check initial state
		ArrayList beforeStatus = new ArrayList();
		validateInitialState(beforeStatus);

		// check proposed change
		ArrayList status = new ArrayList();
		checkPlatformWasModified(status);
		validateInstall(oldFeature, newFeature, status);

		// report status
		return createCombinedReportStatus(beforeStatus, status);
	}

	/*
	 * Called by UI before performing operation
	 */
	public IStatus validatePendingUnconfig(IFeature feature) {
		// check initial state
		ArrayList beforeStatus = new ArrayList();
		validateInitialState(beforeStatus);

		// check proposed change
		ArrayList status = new ArrayList();
		checkPlatformWasModified(status);
		validateUnconfigure(feature, status);

		// report status
		return createCombinedReportStatus(beforeStatus, status);
	}

	/*
	 * Called by UI before performing operation
	 */
	public IStatus validatePendingConfig(IFeature feature) {
		// check initial state
		ArrayList beforeStatus = new ArrayList();
		validateInitialState(beforeStatus);

		// check proposed change
		ArrayList status = new ArrayList();
		checkPlatformWasModified(status);
		validateConfigure(feature, status);

		// report status
		return createCombinedReportStatus(beforeStatus, status);
	}

	/**
	 * Called before performing operation.
	 */
	public IStatus validatePendingReplaceVersion(IFeature feature, IFeature anotherFeature) {
		// check initial state
		ArrayList beforeStatus = new ArrayList();
		validateInitialState(beforeStatus);

		// check proposed change
		ArrayList status = new ArrayList();
		checkPlatformWasModified(status);
		validateReplaceVersion(feature, anotherFeature, status);

		// report status
		return createCombinedReportStatus(beforeStatus, status);
	}

	/*
	 * Called by the UI before doing a revert/ restore operation
	 */
	public IStatus validatePendingRevert(IInstallConfiguration config) {
		// check initial state
		ArrayList beforeStatus = new ArrayList();
		validateInitialState(beforeStatus);

		// check proposed change
		ArrayList status = new ArrayList();
		checkPlatformWasModified(status);
		validateRevert(config, status);

		// report status
		return createCombinedReportStatus(beforeStatus, status);
	}

	/*
	 * Called by the UI before doing a batched processing of several pending
	 * changes.
	 */
	public IStatus validatePendingChanges(IInstallFeatureOperation[] jobs) {
		// check initial state
		ArrayList beforeStatus = new ArrayList();
		validateInitialState(beforeStatus);
		checkPlatformWasModified(beforeStatus);

		// check proposed change
		ArrayList status = new ArrayList();
		validatePendingChanges(jobs, status, beforeStatus);

		// report status
		return createCombinedReportStatus(beforeStatus, status);
	}

	/*
	 * Called by the UI before doing a batched processing of several pending
	 * changes.
	 */
	public RequiredFeaturesResult getRequiredFeatures(IInstallFeatureOperation[] jobs) {

		RequiredFeaturesResult requiredFeaturesResult = new RequiredFeaturesResult();
		// check initial state
		ArrayList beforeStatus = new ArrayList();
		validateInitialState(beforeStatus);
		checkPlatformWasModified(beforeStatus);

		// check proposed change
		ArrayList status = new ArrayList();
		Set requiredFeatures = validatePendingChanges(jobs, status, beforeStatus);

		// report status
		//return createCombinedReportStatus(beforeStatus, status);
		requiredFeaturesResult.setRequiredFeatures(requiredFeatures);
		requiredFeaturesResult.setStatus(createCombinedReportStatus(beforeStatus, status));
		return requiredFeaturesResult;
	}

	/*
	 * Check the current state.
	 */
	public IStatus validateCurrentState() {
		// check the state
		ArrayList status = new ArrayList();
		checkPlatformWasModified(status);
		validateInitialState(status);

		// report status
		if (status.size() > 0)
			return createMultiStatus(Messages.ActivityConstraints_rootMessage, status, IStatus.ERROR);
		return null;
	}

	/*
	 * Check to see if we are not broken even before we start
	 */
	private static void validateInitialState(ArrayList status) {
		try {
			ArrayList features = computeFeatures();
			// uncomment this when patch released in boot
			//checkConfigurationLock(status);
			checkConstraints(features, status);
		} catch (CoreException e) {
			status.add(e.getStatus());
		}
	}

	/*
	 * handle unconfigure
	 */
	private static void validateUnconfigure(IFeature feature, ArrayList status) {
		try {
			checkSiteReadOnly(feature, status);
			ArrayList features = computeFeatures();
			features = computeFeaturesAfterOperation(features, null, feature);
			checkConstraints(features, status);
		} catch (CoreException e) {
			status.add(e.getStatus());
		}
	}

	/*
	 * handle configure
	 */
	private static void validateConfigure(IFeature feature, ArrayList status) {
		try {
			checkSiteReadOnly(feature, status);
			ArrayList features = computeFeatures();
			checkOptionalChildConfiguring(feature, status);
			checkForCycles(feature, null, features);
			features = computeFeaturesAfterOperation(features, feature, null);
			checkConstraints(features, status);

		} catch (CoreException e) {
			status.add(e.getStatus());
		}
	}

	/*
	 * handle replace version
	 */
	private static void validateReplaceVersion(IFeature feature, IFeature anotherFeature, ArrayList status) {
		try {
			checkSiteReadOnly(feature, status);
			ArrayList features = computeFeatures();
			checkForCycles(feature, null, features);
			features = computeFeaturesAfterOperation(features, anotherFeature, feature);
			checkConstraints(features, status);
		} catch (CoreException e) {
			status.add(e.getStatus());
		}
	}

	/*
	 * handle install and update
	 */
	private static void validateInstall(IFeature oldFeature, IFeature newFeature, ArrayList status) {
		try {
			checkSiteReadOnly(oldFeature, status);
			ArrayList features = computeFeatures();
			checkForCycles(newFeature, null, features);
			features = computeFeaturesAfterOperation(features, newFeature, oldFeature);
			checkConstraints(features, status);
			checkLicense(newFeature, status);
		} catch (CoreException e) {
			status.add(e.getStatus());
		}
	}

	/*
	 * handle revert and restore
	 */
	private static void validateRevert(IInstallConfiguration config, ArrayList status) {
		try {
			//			// check the timeline and don't bother
			//			// to check anything else if negative
			//			if (!checkTimeline(config, status))
			//				return;
			ArrayList features = computeFeaturesAfterRevert(config);
			checkConstraints(features, status);
			checkRevertConstraints(features, status);

		} catch (CoreException e) {
			status.add(e.getStatus());
		}
	}

	/*
	 * Handle one-click changes as a batch
	 */
	private static Set validatePendingChanges(IInstallFeatureOperation[] jobs, ArrayList status, ArrayList beforeStatus) {
		try {
			ArrayList features = computeFeatures();
			ArrayList savedFeatures = (ArrayList) features.clone();

			int nexclusives = 0;

			// pass 1: see if we can process the entire "batch"
			ArrayList tmpStatus = new ArrayList();
			for (int i = 0; i < jobs.length; i++) {
				IInstallFeatureOperation job = jobs[i];

				IFeature newFeature = job.getFeature();
				IFeature oldFeature = job.getOldFeature();
				checkLicense(newFeature, status);
				if (jobs.length > 1 && newFeature.isExclusive()) {
					nexclusives++;
					status.add(createStatus(newFeature, FeatureStatus.CODE_EXCLUSIVE, Messages.ActivityConstraints_exclusive));
					continue;
				}
				checkForCycles(newFeature, null, features);
				features = computeFeaturesAfterOperation(features, newFeature, oldFeature);
				// give clients the opportunity to veto the plan
				verifyPlan(job, savedFeatures, features, status);
			}
			if (nexclusives > 0)
				return Collections.EMPTY_SET;
			checkConstraints(features, tmpStatus);
			if (tmpStatus.size() == 0) // the whole "batch" is OK
				return Collections.EMPTY_SET;

			// pass 2: we have conflicts
			features = (ArrayList) savedFeatures.clone();
			for (int i = 0; i < jobs.length; i++) {
				IInstallFeatureOperation job = jobs[i];
				IFeature newFeature = job.getFeature();
				IFeature oldFeature = job.getOldFeature();

				features = computeFeaturesAfterOperation(features, newFeature, oldFeature);

				Set result = checkConstraints(features, status);

				// give clients the opportunity to veto the plan
				verifyPlan(job, savedFeatures, features, status);

				if (status.size() > 0 && !isBetterStatus(beforeStatus, status)) {
					// bug 75613
					//					IStatus conflict =
					//						createStatus(
					//							newFeature,
					//							FeatureStatus.CODE_OTHER,
					//							Policy.bind(KEY_CONFLICT));
					//					status.add(0, conflict);
					return result;
				}
			}
		} catch (CoreException e) {
			status.add(e.getStatus());
		} finally {
			if (verifierServiceReference != null) {
				BundleContext context = UpdateCore.getPlugin().getBundleContext();
				if (context != null)
					context.ungetService(verifierServiceReference);
			}
		}

		return Collections.EMPTY_SET;
	}

	/*
	 * Load and return the plan verifier if there is one registered and we don't have verification disabled.
	 */
	private static ServiceReference loadVerifier() {
		final BundleContext context = UpdateCore.getPlugin().getBundleContext();
		if (context == null)
			return null;
		String value = context.getProperty("eclipse.p2.verifyPlan"); //$NON-NLS-1$
		if ("false".equalsIgnoreCase(value)) { //$NON-NLS-1$
			if (UpdateCore.DEBUG_VERIFIER)
				UpdateCore.debug("Plan verification disabled by user."); //$NON-NLS-1$
			return null;
		}
		verifierServiceReference = context.getServiceReference(PlanVerifier.class.getName());
		return verifierServiceReference;
	}

	/*
	 * Give clients the opportunity to veto install/update changes. 
	 */
	private static void verifyPlan(final IInstallFeatureOperation installOperation, final ArrayList currentFeatures, final ArrayList featuresAfterOperation, final ArrayList status) {
		ServiceReference ref = loadVerifier();
		BundleContext context = UpdateCore.getPlugin().getBundleContext();
		final PlanVerifier verifier;
		if (context == null || ref == null)
			verifier = null;
		else
			verifier = (PlanVerifier) context.getService(ref);
		if (verifier == null) {
			if (UpdateCore.DEBUG_VERIFIER)
				UpdateCore.debug("No plan verifier available. Skipping plan verification."); //$NON-NLS-1$
			return;
		}
		ISafeRunnable job = new ISafeRunnable() {
			public void handleException(Throwable exception) {
				if (UpdateCore.DEBUG_VERIFIER)
					UpdateCore.debug("Exception while running verifier. Check log for details."); //$NON-NLS-1$
				// log the exception 
				UpdateCore.log("Exception while running plan verifier.", exception); //$NON-NLS-1$
				// don't let a bad verifier prevent the operation. fall through and return OK so execution of the plan continues
			}

			public void run() throws Exception {
				if (UpdateCore.DEBUG_VERIFIER)
					UpdateCore.debug("Running plan verifier."); //$NON-NLS-1$
				long start = System.currentTimeMillis();
				verifier.verify(installOperation, currentFeatures, featuresAfterOperation, status);
				if (UpdateCore.DEBUG_VERIFIER)
					UpdateCore.debug("Verification complete in " + (System.currentTimeMillis() - start) + "ms."); //$NON-NLS-1$ //$NON-NLS-2$
			}
		};
		try {
			SafeRunner.run(job);
		} finally {
			context.ungetService(ref);
		}
	}

	private static void checkPlatformWasModified(ArrayList status) {
		try {
			// checks if the platform has been modified outside this eclipse instance
			IPlatformConfiguration platformConfig = ConfiguratorUtils.getCurrentPlatformConfiguration();

			long currentTimeStamp = platformConfig.getChangeStamp();
			// get the last modified value for this config, from this process point of view
			if (platformConfig instanceof PlatformConfiguration)
				currentTimeStamp = ((PlatformConfiguration) platformConfig).getConfiguration().lastModified();

			// get the real last modified value
			URL platformXML = platformConfig.getConfigurationLocation();
			long actualTimeStamp = currentTimeStamp;
			if ("file".equals(platformXML.getProtocol())) //$NON-NLS-1$
				actualTimeStamp = new File(platformXML.getFile()).lastModified();
			else {
				URLConnection connection = platformXML.openConnection();
				actualTimeStamp = connection.getLastModified();
			}
			if (currentTimeStamp != actualTimeStamp)
				status.add(createStatus(null, FeatureStatus.CODE_OTHER, Messages.ActivityConstraints_platformModified));
		} catch (IOException e) {
			// ignore
		}
	}

	private static void checkSiteReadOnly(IFeature feature, ArrayList status) {
		if (feature == null) {
			return;
		}
		IConfiguredSite csite = feature.getSite().getCurrentConfiguredSite();
		if (csite != null && !csite.isUpdatable())
			status.add(createStatus(feature, FeatureStatus.CODE_OTHER, NLS.bind(Messages.ActivityConstraints_readOnly, (new String[] {csite.getSite().getURL().toExternalForm()}))));
	}

	/*
	 * Compute a list of configured features
	 */
	private static ArrayList computeFeatures() throws CoreException {
		return computeFeatures(true);
	}

	/*
	 * Compute a list of configured features
	 */
	private static ArrayList computeFeatures(boolean configuredOnly) throws CoreException {
		ArrayList features = new ArrayList();
		ILocalSite localSite = SiteManager.getLocalSite();
		IInstallConfiguration config = localSite.getCurrentConfiguration();
		IConfiguredSite[] csites = config.getConfiguredSites();

		for (int i = 0; i < csites.length; i++) {
			IConfiguredSite csite = csites[i];

			IFeatureReference[] crefs;

			if (configuredOnly)
				crefs = csite.getConfiguredFeatures();
			else
				crefs = csite.getSite().getFeatureReferences();
			for (int j = 0; j < crefs.length; j++) {
				IFeatureReference cref = crefs[j];
				IFeature cfeature = cref.getFeature(null);
				features.add(cfeature);
			}
		}

		return features;
	}

	/*
	 * Compute the nested feature subtree starting at the specified base
	 * feature
	 */
	public static ArrayList computeFeatureSubtree(IFeature top, IFeature feature, ArrayList features, boolean tolerateMissingChildren, ArrayList configuredFeatures, ArrayList visitedFeatures) throws CoreException {

		// check arguments
		if (top == null)
			return features;
		if (feature == null)
			feature = top;
		if (features == null)
			features = new ArrayList();
		if (visitedFeatures == null)
			visitedFeatures = new ArrayList();

		// check for <includes> cycle
		if (visitedFeatures.contains(feature)) {
			IStatus status = createStatus(top, FeatureStatus.CODE_CYCLE, Messages.ActivityConstraints_cycle);
			throw new CoreException(status);
		} else {
			// keep track of visited features so we can detect cycles
			visitedFeatures.add(feature);
		}

		// return specified base feature and all its children
		if (!features.contains(feature))
			features.add(feature);
		IIncludedFeatureReference[] children = feature.getIncludedFeatureReferences();
		for (int i = 0; i < children.length; i++) {
			try {
				IFeature child = UpdateUtils.getIncludedFeature(feature, children[i]);
				features = computeFeatureSubtree(top, child, features, tolerateMissingChildren, null, visitedFeatures);
			} catch (CoreException e) {
				if (!children[i].isOptional() && !tolerateMissingChildren)
					throw e;
			}
		}
		// no cycles for this feature during DFS
		visitedFeatures.remove(feature);
		return features;
	}

	private static void checkLicense(IFeature feature, ArrayList status) {
		IURLEntry licenseEntry = feature.getLicense();
		if (licenseEntry != null) {
			String license = licenseEntry.getAnnotation();
			if (license != null && license.trim().length() > 0)
				return;
		}
		status.add(createStatus(feature, FeatureStatus.CODE_OTHER, Messages.ActivityConstraints_noLicense));
	}

	/*
	 * Compute a list of features that will be configured after the operation
	 */
	private static ArrayList computeFeaturesAfterOperation(ArrayList features, IFeature add, IFeature remove) throws CoreException {

		ArrayList addTree = computeFeatureSubtree(add, null, null, false,
		/* do not tolerate missing children */
		features, null);
		ArrayList removeTree = computeFeatureSubtree(remove, null, null, true /* tolerate missing children */, null, null);
		if (remove != null) {
			// Patches to features are removed together with
			// those features. Include them in the list.
			contributePatchesFor(removeTree, features, removeTree);
		}

		if (remove != null)
			features.removeAll(removeTree);

		if (add != null)
			features.addAll(addTree);

		return features;
	}

	private static void contributePatchesFor(ArrayList removeTree, ArrayList features, ArrayList result) throws CoreException {

		for (int i = 0; i < removeTree.size(); i++) {
			IFeature feature = (IFeature) removeTree.get(i);
			contributePatchesFor(feature, features, result);
		}
	}

	private static void contributePatchesFor(IFeature feature, ArrayList features, ArrayList result) throws CoreException {
		for (int i = 0; i < features.size(); i++) {
			IFeature candidate = (IFeature) features.get(i);
			if (UpdateUtils.isPatch(feature, candidate)) {
				ArrayList removeTree = computeFeatureSubtree(candidate, null, null, true, null, null);
				result.addAll(removeTree);
			}
		}
	}

	/*
	 * Compute a list of features that will be configured after performing the
	 * revert
	 */
	private static ArrayList computeFeaturesAfterRevert(IInstallConfiguration config) throws CoreException {

		ArrayList list = new ArrayList();
		IConfiguredSite[] csites = config.getConfiguredSites();
		for (int i = 0; i < csites.length; i++) {
			IConfiguredSite csite = csites[i];
			IFeatureReference[] features = csite.getConfiguredFeatures();
			for (int j = 0; j < features.length; j++) {
				list.add(features[j].getFeature(null));
			}
		}
		return list;
	}

	/*
	 * Compute a list of plugin entries for the specified features.
	 */
	private static ArrayList computePluginsForFeatures(ArrayList features) throws CoreException {
		if (features == null)
			return new ArrayList();

		HashMap plugins = new HashMap();
		for (int i = 0; i < features.size(); i++) {
			IFeature feature = (IFeature) features.get(i);
			IPluginEntry[] entries = feature.getPluginEntries();
			for (int j = 0; j < entries.length; j++) {
				IPluginEntry entry = entries[j];
				plugins.put(entry.getVersionedIdentifier(), entry);
			}
		}
		ArrayList result = new ArrayList();
		result.addAll(plugins.values());
		return result;
	}

	/**
	 * Check for feature cycles:
	 * - visit feature
	 * - if feature is in the cycle candidates list, then cycle found, else add it to candidates list
	 * - DFS children 
	 * - when return from DFS remove the feature from the candidates list
	 */
	private static void checkForCycles(IFeature feature, ArrayList candidates, ArrayList configuredFeatures) throws CoreException {

		// check arguments
		if (feature == null)
			return;
		if (configuredFeatures == null)
			configuredFeatures = new ArrayList();
		if (candidates == null)
			candidates = new ArrayList();

		// check for <includes> cycle
		if (candidates.contains(feature)) {
			String msg = NLS.bind(Messages.ActivityConstraints_cycle, (new String[] {feature.getLabel(), feature.getVersionedIdentifier().toString()}));
			IStatus status = createStatus(feature, FeatureStatus.CODE_CYCLE, msg);
			throw new CoreException(status);
		}

		// potential candidate
		candidates.add(feature);

		// recursively, check cycles with children
		IIncludedFeatureReference[] children = feature.getIncludedFeatureReferences();
		for (int i = 0; i < children.length; i++) {
			try {
				IFeature child = UpdateUtils.getIncludedFeature(feature, children[i]);
				checkForCycles(child, candidates, configuredFeatures);
			} catch (CoreException e) {
				if (!children[i].isOptional())
					throw e;
			}
		}
		// no longer a candidate, because no cycles with children
		candidates.remove(feature);
	}

	/*
	 * validate constraints
	 */
	private static Set checkConstraints(ArrayList features, ArrayList status) throws CoreException {
		if (features == null)
			return Collections.EMPTY_SET;

		ArrayList plugins = computePluginsForFeatures(features);

		checkEnvironment(features, status);
		checkPlatformFeature(features, plugins, status);
		checkPrimaryFeature(features, plugins, status);
		return checkPrereqs(features, plugins, status);
	}

	/*
	 * Verify all features are either portable, or match the current
	 * environment
	 */
	private static void checkEnvironment(ArrayList features, ArrayList status) {

		String os = Platform.getOS();
		String ws = Platform.getWS();
		String arch = Platform.getOSArch();

		for (int i = 0; i < features.size(); i++) {
			IFeature feature = (IFeature) features.get(i);
			ArrayList fos = createList(feature.getOS());
			ArrayList fws = createList(feature.getWS());
			ArrayList farch = createList(feature.getOSArch());

			if (fos.size() > 0) {
				if (!fos.contains(os)) {
					IStatus s = createStatus(feature, FeatureStatus.CODE_ENVIRONMENT, Messages.ActivityConstraints_os);
					if (!status.contains(s))
						status.add(s);
					continue;
				}
			}

			if (fws.size() > 0) {
				if (!fws.contains(ws)) {
					IStatus s = createStatus(feature, FeatureStatus.CODE_ENVIRONMENT, Messages.ActivityConstraints_ws);
					if (!status.contains(s))
						status.add(s);
					continue;
				}
			}

			if (farch.size() > 0) {
				if (!farch.contains(arch)) {
					IStatus s = createStatus(feature, FeatureStatus.CODE_ENVIRONMENT, Messages.ActivityConstraints_arch);
					if (!status.contains(s))
						status.add(s);
					continue;
				}
			}
		}
	}

	/*
	 * Verify we end up with a version of platform configured
	 */
	private static void checkPlatformFeature(ArrayList features, ArrayList plugins, ArrayList status) {

		// find the plugin that defines the product
		IProduct product = Platform.getProduct();
		if (product == null)
			return; // normally this shouldn't happen
		Bundle primaryBundle = product.getDefiningBundle();
		// check if that plugin is among the resulting plugins
		boolean found = false;
		for (int j = 0; j < plugins.size(); j++) {
			IPluginEntry plugin = (IPluginEntry) plugins.get(j);
			if (primaryBundle.getSymbolicName().equals(plugin.getVersionedIdentifier().getIdentifier())) {
				found = true;
				break;
			}
		}

		if (!found) {
			IStatus s = createStatus(null, FeatureStatus.CODE_OTHER, Messages.ActivityConstraints_platform);
			if (!status.contains(s))
				status.add(s);
		}
	}

	/*
	 * Verify we end up with a version of primary feature configured
	 */
	private static void checkPrimaryFeature(ArrayList features, ArrayList plugins, ArrayList status) {

		String featureId = ConfiguratorUtils.getCurrentPlatformConfiguration().getPrimaryFeatureIdentifier();

		if (featureId != null) {
			// primary feature is defined
			for (int i = 0; i < features.size(); i++) {
				IFeature feature = (IFeature) features.get(i);
				if (featureId.equals(feature.getVersionedIdentifier().getIdentifier()))
					return;
			}

			IStatus s = createStatus(null, FeatureStatus.CODE_OTHER, Messages.ActivityConstraints_primary);
			if (!status.contains(s))
				status.add(s);
		} else {
			// check if the product still ends up contributed
			// find the plugin that defines the product
			IProduct product = Platform.getProduct();
			if (product == null)
				return; // normally this shouldn't happen
			Bundle primaryBundle = product.getDefiningBundle();
			// check if that plugin is among the resulting plugins

			for (int j = 0; j < plugins.size(); j++) {
				IPluginEntry plugin = (IPluginEntry) plugins.get(j);
				if (primaryBundle.getSymbolicName().equals(plugin.getVersionedIdentifier().getIdentifier())) {
					return; // product found
				}
			}
			IStatus s = createStatus(null, FeatureStatus.CODE_OTHER, Messages.ActivityConstraints_primary);
			if (!status.contains(s))
				status.add(s);
		}
	}

	/*
	 * Verify we do not break prereqs
	 */
	private static Set checkPrereqs(ArrayList features, ArrayList plugins, ArrayList status) {

		HashSet result = new HashSet();

		for (int i = 0; i < features.size(); i++) {
			IFeature feature = (IFeature) features.get(i);
			IImport[] imports = feature.getImports();

			for (int j = 0; j < imports.length; j++) {
				IImport iimport = imports[j];
				// for each import determine plugin or feature, version, match
				// we need
				VersionedIdentifier iid = iimport.getVersionedIdentifier();
				String id = iid.getIdentifier();
				PluginVersionIdentifier version = iid.getVersion();
				boolean featurePrereq = iimport.getKind() == IImport.KIND_FEATURE;
				boolean ignoreVersion = version.getMajorComponent() == 0 && version.getMinorComponent() == 0 && version.getServiceComponent() == 0;
				int rule = iimport.getRule();
				if (rule == IUpdateConstants.RULE_NONE)
					rule = IUpdateConstants.RULE_COMPATIBLE;

				boolean found = false;

				ArrayList candidates;

				if (featurePrereq)
					candidates = features;
				else
					candidates = plugins;
				for (int k = 0; k < candidates.size(); k++) {
					VersionedIdentifier cid;
					if (featurePrereq) {
						// the candidate is a feature
						IFeature candidate = (IFeature) candidates.get(k);
						// skip self
						if (feature.equals(candidate))
							continue;
						cid = candidate.getVersionedIdentifier();
					} else {
						// the candidate is a plug-in
						IPluginEntry plugin = (IPluginEntry) candidates.get(k);
						cid = plugin.getVersionedIdentifier();
					}
					PluginVersionIdentifier cversion = cid.getVersion();
					if (id.equals(cid.getIdentifier())) {
						// have a candidate
						if (ignoreVersion)
							found = true;
						else if (rule == IUpdateConstants.RULE_PERFECT && cversion.isPerfect(version))
							found = true;
						else if (rule == IUpdateConstants.RULE_EQUIVALENT && cversion.isEquivalentTo(version))
							found = true;
						else if (rule == IUpdateConstants.RULE_COMPATIBLE && cversion.isCompatibleWith(version))
							found = true;
						else if (rule == IUpdateConstants.RULE_GREATER_OR_EQUAL && cversion.isGreaterOrEqualTo(version))
							found = true;
					}
					if (found)
						break;
				}

				// perhaps the bundle that we are looking for was installed
				// but isn't a part of a feature
				if (!found && !featurePrereq)
					found = isInstalled(iid, rule, ignoreVersion);

				if (!found) {
					// report status
					String target = featurePrereq ? Messages.ActivityConstaints_prereq_feature : Messages.ActivityConstaints_prereq_plugin;
					int errorCode = featurePrereq ? FeatureStatus.CODE_PREREQ_FEATURE : FeatureStatus.CODE_PREREQ_PLUGIN;
					String msg = NLS.bind(Messages.ActivityConstraints_prereq, (new String[] {target, id}));

					if (!ignoreVersion) {
						if (rule == IUpdateConstants.RULE_PERFECT)
							msg = NLS.bind(Messages.ActivityConstraints_prereqPerfect, (new String[] {target, id, version.toString()}));
						else if (rule == IUpdateConstants.RULE_EQUIVALENT)
							msg = NLS.bind(Messages.ActivityConstraints_prereqEquivalent, (new String[] {target, id, version.toString()}));
						else if (rule == IUpdateConstants.RULE_COMPATIBLE)
							msg = NLS.bind(Messages.ActivityConstraints_prereqCompatible, (new String[] {target, id, version.toString()}));
						else if (rule == IUpdateConstants.RULE_GREATER_OR_EQUAL)
							msg = NLS.bind(Messages.ActivityConstraints_prereqGreaterOrEqual, (new String[] {target, id, version.toString()}));
					}
					IStatus s = createStatus(feature, errorCode, msg);
					result.add(new InternalImport(iimport));
					if (!status.contains(s))
						status.add(s);
				}
			}
		}

		return result;
	}

	/*
	 * Return a boolean value indicating whether or not the bundle with the given id and version
	 * is installed in the system.
	 */
	private static boolean isInstalled(VersionedIdentifier vid, int rule, boolean ignoreVersion) {
		BundleContext context = UpdateCore.getPlugin().getBundleContext();
		if (context == null)
			return false;
		ServiceReference reference = context.getServiceReference(PlatformAdmin.class.getName());
		if (reference == null)
			return false;
		PlatformAdmin admin = (PlatformAdmin) context.getService(reference);
		try {
			State state = admin.getState(false);
			String id = vid.getIdentifier();
			PluginVersionIdentifier version = vid.getVersion();
			BundleDescription[] bundles = state.getBundles(id);
			if (bundles == null || bundles.length == 0)
				return false;
			for (int i = 0; i < bundles.length; i++) {
				BundleDescription bundle = bundles[i];
				PluginVersionIdentifier cversion = new PluginVersionIdentifier(bundle.getVersion().toString());
				// have a candidate
				if (ignoreVersion)
					return true;
				if (rule == IUpdateConstants.RULE_PERFECT && cversion.isPerfect(version))
					return true;
				else if (rule == IUpdateConstants.RULE_EQUIVALENT && cversion.isEquivalentTo(version))
					return true;
				else if (rule == IUpdateConstants.RULE_COMPATIBLE && cversion.isCompatibleWith(version))
					return true;
				else if (rule == IUpdateConstants.RULE_GREATER_OR_EQUAL && cversion.isGreaterOrEqualTo(version))
					return true;
			}
			return false;
		} finally {
			context.ungetService(reference);
		}
	}

	/*
	 * Verify we end up with valid nested features after revert
	 */
	private static void checkRevertConstraints(ArrayList features, ArrayList status) {

		for (int i = 0; i < features.size(); i++) {
			IFeature feature = (IFeature) features.get(i);
			try {
				computeFeatureSubtree(feature, null, null, false /* do not tolerate missing children */, null, null);
			} catch (CoreException e) {
				status.add(e.getStatus());
			}
		}
	}

	/*
	 * Verify that a parent of an optional child is configured before we allow
	 * the child to be configured as well
	 */

	private static void checkOptionalChildConfiguring(IFeature feature, ArrayList status) throws CoreException {
		ILocalSite localSite = SiteManager.getLocalSite();
		IInstallConfiguration config = localSite.getCurrentConfiguration();
		IConfiguredSite[] csites = config.getConfiguredSites();

		boolean included = false;
		for (int i = 0; i < csites.length; i++) {
			IConfiguredSite csite = csites[i];
			ISiteFeatureReference[] crefs = csite.getSite().getFeatureReferences();
			for (int j = 0; j < crefs.length; j++) {
				IFeatureReference cref = crefs[j];
				IFeature cfeature = null;
				try {
					cfeature = cref.getFeature(null);
				} catch (CoreException e) {
					//FIXME: cannot ask 'isOptional' here
					// Ignore missing optional feature.
					/*
					 * if (cref.isOptional()) continue;
					 */
					throw e;
				}
				if (isParent(cfeature, feature, true)) {
					// Included in at least one feature as optional
					included = true;
					if (csite.isConfigured(cfeature)) {
						// At least one feature parent
						// is enabled - it is OK to
						// configure optional child.
						return;
					}
				}
			}
		}
		if (included) {
			// feature is included as optional but
			// no parent is currently configured.
			String msg = Messages.ActivityConstraints_optionalChild;
			status.add(createStatus(feature, FeatureStatus.CODE_OPTIONAL_CHILD, msg));
		} else {
			//feature is root - can be configured
		}
	}

	//
	//	/**
	//	 * Checks if the configuration is locked by other instances
	//	 * 
	//	 * @param status
	//	 */
	//	private static void checkConfigurationLock(ArrayList status) {
	//		IPlatformConfiguration config =
	//			BootLoader.getCurrentPlatformConfiguration();
	//		URL configURL = config.getConfigurationLocation();
	//		if (!"file".equals(configURL.getProtocol())) {
	//			status.add(
	//				createStatus(
	//					null,
	//					"Configuration location is not writable:" + configURL));
	//			return;
	//		}
	//		String locationString = configURL.getFile();
	//		File configDir = new File(locationString);
	//		if (!configDir.isDirectory())
	//			configDir = configDir.getParentFile();
	//		if (!configDir.exists()) {
	//			status.add(
	//				createStatus(null, "Configuration location does not exist"));
	//			return;
	//		}
	//		File locksDir = new File(configDir, "locks");
	//		// check all the possible lock files
	//		File[] lockFiles = locksDir.listFiles();
	//		File configLock = BootLoader.getCurrentPlatformConfiguration().getLockFile();
	//		for (int i = 0; i < lockFiles.length; i++) {
	//			if (lockFiles[i].equals(configLock))
	//				continue;
	//			try {
	//				RandomAccessFile raf = new RandomAccessFile(lockFiles[i], "rw");
	//				FileChannel channel = raf.getChannel();
	//				System.out.println(channel.isOpen());
	//				FileLock lock = channel.tryLock();
	//				if (lock == null){
	//					// there is another eclipse instance running
	//					raf.close();
	//					status.add(
	//						createStatus(
	//							null,
	//							"Another instance is running, please close it before performing any configuration operations"));
	//					return;
	//				}
	//
	//			} catch (Exception e) {
	//				status.add(createStatus(null, "Failed to create lock:"+lockFiles[i]));
	//				return;
	//			} 
	//		}
	//	}

	private static boolean isParent(IFeature candidate, IFeature feature, boolean optionalOnly) throws CoreException {
		IIncludedFeatureReference[] refs = candidate.getIncludedFeatureReferences();
		for (int i = 0; i < refs.length; i++) {
			IIncludedFeatureReference child = refs[i];
			VersionedIdentifier fvid = feature.getVersionedIdentifier();
			VersionedIdentifier cvid = child.getVersionedIdentifier();

			if (fvid.getIdentifier().equals(cvid.getIdentifier()) == false)
				continue;
			// same ID
			PluginVersionIdentifier fversion = fvid.getVersion();
			PluginVersionIdentifier cversion = cvid.getVersion();

			if (fversion.equals(cversion)) {
				// included and matched; return true if optionality is not
				// important or it is and the inclusion is optional
				return optionalOnly == false || child.isOptional();
			}
		}
		return false;
	}

	//	private static boolean checkTimeline(
	//		IInstallConfiguration config,
	//		ArrayList status) {
	//		try {
	//			ILocalSite lsite = SiteManager.getLocalSite();
	//			IInstallConfiguration cconfig = lsite.getCurrentConfiguration();
	//			if (cconfig.getTimeline() != config.getTimeline()) {
	//				// Not the same timeline - cannot revert
	//				String msg =
	//					UpdateUtils.getFormattedMessage(
	//						KEY_WRONG_TIMELINE,
	//						config.getLabel());
	//				status.add(createStatus(null, FeatureStatus.CODE_OTHER, msg));
	//				return false;
	//			}
	//		} catch (CoreException e) {
	//			status.add(e.getStatus());
	//		}
	//		return true;
	//	}

	private static IStatus createMultiStatus(String message, ArrayList children, int code) {
		IStatus[] carray = (IStatus[]) children.toArray(new IStatus[children.size()]);
		return new MultiStatus(UpdateCore.getPlugin().getBundle().getSymbolicName(), code, carray, message, null);
	}

	private static IStatus createStatus(IFeature feature, int errorCode, String message) {

		String fullMessage;
		if (feature == null)
			fullMessage = message;
		else {
			PluginVersionIdentifier version = feature.getVersionedIdentifier().getVersion();
			fullMessage = NLS.bind(Messages.ActivityConstraints_childMessage, (new String[] {feature.getLabel(), version.toString(), message}));
		}

		return new FeatureStatus(feature, IStatus.ERROR, UpdateCore.getPlugin().getBundle().getSymbolicName(), errorCode, fullMessage, null);
	}

	//	private static IStatus createReportStatus(ArrayList beforeStatus,
	// ArrayList status) {
	//		// report status
	//		if (status.size() > 0) {
	//			if (beforeStatus.size() > 0)
	//				return createMultiStatus(KEY_ROOT_MESSAGE_INIT,
	// beforeStatus,IStatus.ERROR);
	//			else
	//				return createMultiStatus(KEY_ROOT_MESSAGE, status,IStatus.ERROR);
	//		}
	//		return null;
	//	}

	private static IStatus createCombinedReportStatus(ArrayList beforeStatus, ArrayList status) {
		if (beforeStatus.size() == 0) { // good initial config
			if (status.size() == 0) {
				return null; // all fine
			} else {
				return createMultiStatus(Messages.ActivityConstraints_rootMessage, status, IStatus.ERROR);
				// error after operation
			}
		} else { // beforeStatus.size() > 0 : initial config errors
			if (status.size() == 0) {
				return null; // errors will be fixed
			} else {
				if (isBetterStatus(beforeStatus, status)) {
					return createMultiStatus(Messages.ActivityConstraints_warning, beforeStatus, IStatus.WARNING);
					// errors may be fixed
				} else {
					ArrayList combined = new ArrayList();
					combined.add(createMultiStatus(Messages.ActivityConstraints_beforeMessage, beforeStatus, IStatus.ERROR));
					combined.add(createMultiStatus(Messages.ActivityConstraints_afterMessage, status, IStatus.ERROR));
					return createMultiStatus(Messages.ActivityConstraints_rootMessageInitial, combined, IStatus.ERROR);
				}
			}
		}
	}

	private static ArrayList createList(String commaSeparatedList) {
		ArrayList list = new ArrayList();
		if (commaSeparatedList != null) {
			StringTokenizer t = new StringTokenizer(commaSeparatedList.trim(), ","); //$NON-NLS-1$
			while (t.hasMoreTokens()) {
				String token = t.nextToken().trim();
				if (!token.equals("")) //$NON-NLS-1$
					list.add(token);
			}
		}
		return list;
	}

	/**
	 * Returns true if status is a subset of beforeStatus
	 * 
	 * @param beforeStatus
	 * @param status
	 * @return
	 */
	private static boolean isBetterStatus(ArrayList beforeStatus, ArrayList status) {
		// if no status at all, then it's a subset
		if (status == null || status.size() == 0)
			return true;
		// there is some status, so if there is no initial status, then it's
		// not a subset
		if (beforeStatus == null || beforeStatus.size() == 0)
			return false;
		// quick check
		if (beforeStatus.size() < status.size())
			return false;

		// check if all the status elements appear in the original status
		for (int i = 0; i < status.size(); i++) {
			IStatus s = (IStatus) status.get(i);
			// if this is not a feature status, something is wrong, so return
			// false
			if (!(s instanceof FeatureStatus))
				return false;
			FeatureStatus fs = (FeatureStatus) s;
			// check against all status elements
			boolean found = false;
			for (int j = 0; !found && j < beforeStatus.size(); j++) {
				if (fs.equals(beforeStatus.get(j)))
					found = true;
			}
			if (!found)
				return false;
		}
		return true;
	}

	public class RequiredFeaturesResult {

		private IStatus status;
		private Set requiredFeatures;

		public Set getRequiredFeatures() {
			return requiredFeatures;
		}

		public void setRequiredFeatures(Set requiredFeatures) {
			this.requiredFeatures = requiredFeatures;
		}

		public void addRequiredFeatures(Set requiredFeatures) {
			if (requiredFeatures == null) {
				requiredFeatures = new HashSet();
			}
			this.requiredFeatures.addAll(requiredFeatures);
		}

		public IStatus getStatus() {
			return status;
		}

		public void setStatus(IStatus status) {
			this.status = status;
		}

	}

	public static class InternalImport {

		private IImport iimport;

		public InternalImport(IImport iimport) {
			this.iimport = iimport;
		}

		public IImport getImport() {
			return iimport;
		}

		public void setImport(IImport iimport) {
			this.iimport = iimport;
		}

		public boolean equals(Object object) {

			if ((object == null) || !(object instanceof InternalImport))
				return false;

			if (object == this)
				return true;

			return iimport.getVersionedIdentifier().equals(((InternalImport) object).getImport().getVersionedIdentifier()) && (getImport().getRule() == ((InternalImport) object).getImport().getRule());

		}

		public int hashCode() {
			return iimport.getVersionedIdentifier().hashCode() * iimport.getRule();
		}

	}

}
