/******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.operations;

//import java.io.*;
//import java.net.*;
//import java.nio.channels.*;
import java.io.*;
import java.net.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.update.configuration.*;
import org.eclipse.update.configurator.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.*;
import org.eclipse.update.internal.api.operations.*;

/**
 *  
 */
public class OperationValidator implements IOperationValidator {
	private static final String KEY_ROOT_MESSAGE =
		"ActivityConstraints.rootMessage";
	private static final String KEY_ROOT_MESSAGE_INIT =
		"ActivityConstraints.rootMessageInitial";
	private static final String KEY_CHILD_MESSAGE =
		"ActivityConstraints.childMessage";
	private static final String KEY_PLATFORM = "ActivityConstraints.platform";
	private static final String KEY_PRIMARY = "ActivityConstraints.primary";
	private static final String KEY_OS = "ActivityConstraints.os";
	private static final String KEY_WS = "ActivityConstraints.ws";
	private static final String KEY_ARCH = "ActivityConstraints.arch";
	private static final String KEY_PREREQ = "ActivityConstraints.prereq";
	private static final String KEY_PREREQ_PLUGIN =
		"ActivityConstaints.prereq.plugin";
	private static final String KEY_PREREQ_FEATURE =
		"ActivityConstaints.prereq.feature";
	private static final String KEY_PREREQ_PERFECT =
		"ActivityConstraints.prereqPerfect";
	private static final String KEY_PREREQ_EQUIVALENT =
		"ActivityConstraints.prereqEquivalent";
	private static final String KEY_PREREQ_COMPATIBLE =
		"ActivityConstraints.prereqCompatible";
	private static final String KEY_PREREQ_GREATER =
		"ActivityConstraints.prereqGreaterOrEqual";
	private static final String KEY_PATCH_UNCONFIGURE =
		"ActivityConstraints.patchUnconfigure";
	private static final String KEY_PATCH_UNCONFIGURE_BACKUP =
		"ActivityConstraints.patchUnconfigureBackup";
	private static final String KEY_OPTIONAL_CHILD =
		"ActivityConstraints.optionalChild";
	private static final String KEY_CYCLE = "ActivityConstraints.cycle";
	private static final String KEY_CONFLICT = "ActivityConstraints.conflict";
	private static final String KEY_EXCLUSIVE = "ActivityConstraints.exclusive";
	private static final String KEY_NO_LICENSE =
		"ActivityConstraints.noLicense";

	/**
	 * Checks if the platform configuration has been modified outside this program.
	 * @return the error status, or null if no errors
	 */
	public IStatus validatePlatformConfigValid() {
		ArrayList status = new ArrayList(1);
		checkPlatformWasModified(status);
		
		// report status
		if (status.size() > 0)
			return createMultiStatus(KEY_ROOT_MESSAGE, status, IStatus.ERROR);
		return null;
	}
	
	/*
	 * Called by UI before performing operation. Returns null if no errors, a
	 * status with IStatus.WARNING code when the initial configuration is
	 * broken, or a status with IStatus.ERROR when there the operation
	 * introduces new errors
	 */
	public IStatus validatePendingInstall(
		IFeature oldFeature,
		IFeature newFeature) {
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
	public IStatus validatePendingReplaceVersion(
		IFeature feature,
		IFeature anotherFeature) {
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
	 * Called by UI before processing a delta
	 */
	public IStatus validateSessionDelta(
		ISessionDelta delta,
		IFeatureReference[] deltaRefs) {
		// check initial state
		ArrayList beforeStatus = new ArrayList();
		validateInitialState(beforeStatus);

		// check proposed change
		ArrayList status = new ArrayList();
		switch (delta.getType()) {
			case ISessionDelta.ENABLE :
				validateDeltaConfigure(delta, deltaRefs, status);
				break;
		}

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
	 * Check the current state.
	 */
	public IStatus validateCurrentState() {
		// check the state
		ArrayList status = new ArrayList();
		checkPlatformWasModified(status);
		validateInitialState(status);

		// report status
		if (status.size() > 0)
			return createMultiStatus(KEY_ROOT_MESSAGE, status, IStatus.ERROR);
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
	private static void validateUnconfigure(
		IFeature feature,
		ArrayList status) {
		try {
			checkSiteReadOnly(feature,status);
			if (validateUnconfigurePatch(feature, status))
				return;
			ArrayList features = computeFeatures();
			features = computeFeaturesAfterOperation(features, null, feature);
			checkConstraints(features, status);
		} catch (CoreException e) {
			status.add(e.getStatus());
		}
	}

	private static boolean validateUnconfigurePatch(
		IFeature feature,
		ArrayList status)
		throws CoreException {
		if (feature.isPatch()) {
			IInstallConfiguration backup =
				UpdateUtils.getBackupConfigurationFor(feature);
			String msg;
			if (backup != null)
				msg =
					UpdateUtils.getFormattedMessage(
						KEY_PATCH_UNCONFIGURE_BACKUP,
						backup.getLabel());
			else
				msg = UpdateUtils.getString(KEY_PATCH_UNCONFIGURE);
			status.add(createStatus(feature, FeatureStatus.CODE_OTHER, msg));
			return true;
		}
		return false;
	}

	/*
	 * handle configure
	 */
	private static void validateConfigure(IFeature feature, ArrayList status) {
		try {
			checkSiteReadOnly(feature,status);
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
	private static void validateReplaceVersion(
		IFeature feature,
		IFeature anotherFeature,
		ArrayList status) {
		try {
			checkSiteReadOnly(feature,status);
			ArrayList features = computeFeatures();
			checkForCycles(feature, null, features);
			features =
				computeFeaturesAfterOperation(
					features,
					anotherFeature,
					feature);
			checkConstraints(features, status);
		} catch (CoreException e) {
			status.add(e.getStatus());
		}
	}

	/*
	 * handle install and update
	 */
	private static void validateInstall(
		IFeature oldFeature,
		IFeature newFeature,
		ArrayList status) {
		try {
			checkSiteReadOnly(oldFeature,status);
			ArrayList features = computeFeatures();
			checkForCycles(newFeature, null, features);
			features =
				computeFeaturesAfterOperation(features, newFeature, oldFeature);
			checkConstraints(features, status);
			checkLicense(newFeature, status);
		} catch (CoreException e) {
			status.add(e.getStatus());
		}
	}

	/*
	 * handle revert and restore
	 */
	private static void validateRevert(
		IInstallConfiguration config,
		ArrayList status) {
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
	 * Handle delta addition
	 */
	private static void validateDeltaConfigure(
		ISessionDelta delta,
		IFeatureReference[] deltaRefs,
		ArrayList status) {
		try {
			ArrayList features = computeFeaturesAfterDelta(delta, deltaRefs);
			checkConstraints(features, status);

		} catch (CoreException e) {
			status.add(e.getStatus());
		}
	}

	/*
	 * Handle one-click changes as a batch
	 */
	private static void validatePendingChanges(
		IInstallFeatureOperation[] jobs,
		ArrayList status,
		ArrayList beforeStatus) {
		try {
			ArrayList features = computeFeatures();
			ArrayList savedFeatures = features;
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
					status.add(
						createStatus(
							newFeature,
							FeatureStatus.CODE_EXCLUSIVE,
							UpdateUtils.getString(KEY_EXCLUSIVE)));
					continue;
				}
				checkForCycles(newFeature, null, features);
				features =
					computeFeaturesAfterOperation(
						features,
						newFeature,
						oldFeature);
			}
			if (nexclusives > 0)
				return;
			checkConstraints(features, tmpStatus);
			if (tmpStatus.size() == 0) // the whole "batch" is OK
				return;

			// pass 2: we have conflicts
			features = savedFeatures;
			for (int i = 0; i < jobs.length; i++) {
				IInstallFeatureOperation job = jobs[i];
				IFeature newFeature = job.getFeature();
				IFeature oldFeature = job.getOldFeature();

				features =
					computeFeaturesAfterOperation(
						features,
						newFeature,
						oldFeature);

				checkConstraints(features, status);
				if (status.size() > 0
					&& !isBetterStatus(beforeStatus, status)) {
					IStatus conflict =
						createStatus(
							newFeature,
							FeatureStatus.CODE_OTHER,
							UpdateUtils.getString(KEY_CONFLICT));
					status.add(0, conflict);
					return;
				}
			}
		} catch (CoreException e) {
			status.add(e.getStatus());
		}
	}
	
	private static void checkPlatformWasModified(ArrayList status) {
		try {
			// checks if the platform has been modified outside this eclipse instance
			IPlatformConfiguration platformConfig = ConfiguratorUtils.getCurrentPlatformConfiguration();
			// Divide timestamp by 1000 so we compare up to second, to account for filesystem particulars
			long currentTimeStamp = platformConfig.getChangeStamp()/1000;
			URL platformXML = platformConfig.getConfigurationLocation();
			URLConnection connection = platformXML.openConnection();
			long actualTimeStamp = connection.getLastModified()/1000;
			if (currentTimeStamp != actualTimeStamp)
				status.add(createStatus(
								null,
								FeatureStatus.CODE_OTHER,
								UpdateUtils.getString("ActivityConstraints.platformModified")));
		} catch (IOException e) {
			// ignore
		}
	}
	
	private static void checkSiteReadOnly(IFeature feature, ArrayList status) {
		if(feature == null){
			return;
		}
		IConfiguredSite csite = feature.getSite().getCurrentConfiguredSite();
		if (csite != null && !csite.isUpdatable())
			status.add(createStatus(feature, FeatureStatus.CODE_OTHER,
					UpdateUtils
					.getFormattedMessage("ActivityConstraints.readOnly",
							csite.getSite().getURL().toExternalForm())));
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
	private static ArrayList computeFeatures(boolean configuredOnly)
		throws CoreException {
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
	private static ArrayList computeFeatureSubtree(
			IFeature top,
			IFeature feature,
			ArrayList features,
			boolean tolerateMissingChildren,
			ArrayList configuredFeatures,
			ArrayList visitedFeatures)
	throws CoreException {

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
			IStatus status =
			createStatus(top, FeatureStatus.CODE_CYCLE, UpdateUtils.getString(KEY_CYCLE));
			throw new CoreException(status);
		} else {
			// keep track of visited features so we can detect cycles
			visitedFeatures.add(feature);
		}

		// return specified base feature and all its children
		if (!features.contains(feature))
			features.add(feature);
		IIncludedFeatureReference[] children =
		feature.getIncludedFeatureReferences();
		for (int i = 0; i < children.length; i++) {
			try {
				IFeature child = children[i].getFeature(null);
				features =
				computeFeatureSubtree(
						top,
						child,
						features,
						tolerateMissingChildren,
						null,
						visitedFeatures);
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
		status.add(
			createStatus(feature, FeatureStatus.CODE_OTHER, UpdateUtils.getString(KEY_NO_LICENSE)));
	}

	/*
	 * Compute a list of features that will be configured after the operation
	 */
	private static ArrayList computeFeaturesAfterOperation(
		ArrayList features,
		IFeature add,
		IFeature remove)
		throws CoreException {

		ArrayList addTree = computeFeatureSubtree(add, null, null, false,
		/* do not tolerate missing children */
		features, null);
		ArrayList removeTree =
			computeFeatureSubtree(
				remove,
				null,
				null,
				true /* tolerate missing children */,
				null,
				null
		);
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

	private static void contributePatchesFor(
		ArrayList removeTree,
		ArrayList features,
		ArrayList result)
		throws CoreException {

		for (int i = 0; i < removeTree.size(); i++) {
			IFeature feature = (IFeature) removeTree.get(i);
			contributePatchesFor(feature, features, result);
		}
	}

	private static void contributePatchesFor(
		IFeature feature,
		ArrayList features,
		ArrayList result)
		throws CoreException {
		for (int i = 0; i < features.size(); i++) {
			IFeature candidate = (IFeature) features.get(i);
			if (UpdateUtils.isPatch(feature, candidate)) {
				ArrayList removeTree =
					computeFeatureSubtree(candidate, null, null, true,null,null);
				result.addAll(removeTree);
			}
		}
	}

	/*
	 * Compute a list of features that will be configured after performing the
	 * revert
	 */
	private static ArrayList computeFeaturesAfterRevert(IInstallConfiguration config)
		throws CoreException {

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
	 * Compute a list of features that will be configured after applying the
	 * specified delta
	 */
	private static ArrayList computeFeaturesAfterDelta(
		ISessionDelta delta,
		IFeatureReference[] deltaRefs)
		throws CoreException {

		if (delta == null || deltaRefs == null)
			deltaRefs = new IFeatureReference[0];
		else if (deltaRefs == null)
			deltaRefs = delta.getFeatureReferences();

		ArrayList features = new ArrayList(); // cumulative results list
		ILocalSite localSite = SiteManager.getLocalSite();
		IInstallConfiguration config = localSite.getCurrentConfiguration();
		IConfiguredSite[] csites = config.getConfiguredSites();

		// compute changes for each site
		for (int i = 0; i < csites.length; i++) {
			IConfiguredSite csite = csites[i];
			ArrayList siteFeatures = new ArrayList();

			// collect currently configured features on site
			IFeatureReference[] crefs = csite.getConfiguredFeatures();
			for (int j = 0; crefs != null && j < crefs.length; j++) {
				IFeatureReference cref = crefs[j];
				IFeature cfeature = cref.getFeature(null);
				siteFeatures.add(cfeature);
			}

			// add deltas for the site
			for (int j = 0; j < deltaRefs.length; j++) {
				ISite deltaSite = deltaRefs[j].getSite();
				if (deltaSite.equals(csite.getSite())) {
					IFeature dfeature = deltaRefs[j].getFeature(null);
					if (!siteFeatures.contains(dfeature)) // don't add dups
						siteFeatures.add(dfeature);
				}
			}

			// reduce the list if needed
			IFeature[] array =
				(IFeature[]) siteFeatures.toArray(
					new IFeature[siteFeatures.size()]);
			ArrayList removeTree = new ArrayList();
			for (int j = 0; j < array.length; j++) {
				VersionedIdentifier id1 = array[j].getVersionedIdentifier();
				for (int k = 0; k < array.length; k++) {
					if (j == k)
						continue;
					VersionedIdentifier id2 = array[k].getVersionedIdentifier();
					if (id1.getIdentifier().equals(id2.getIdentifier())) {
						if (id2.getVersion().isGreaterThan(id1.getVersion())) {
							removeTree.add(array[j]);
							siteFeatures.remove(array[j]);
							break;
						}
					}
				}
			}
			// Compute patches that will need to be removed together with
			// the removed features
			ArrayList patchesTree = new ArrayList();
			contributePatchesFor(removeTree, siteFeatures, patchesTree);
			siteFeatures.removeAll(patchesTree);

			// accumulate site results
			features.addAll(siteFeatures);
		}

		return features;
	}

	/*
	 * Compute a list of plugin entries for the specified features.
	 */
	private static ArrayList computePluginsForFeatures(ArrayList features)
		throws CoreException {
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
	private static void checkForCycles(
			IFeature feature,
			ArrayList candidates,
			ArrayList configuredFeatures)
	throws CoreException {

		// check arguments
		if (feature == null)
			return;
		if (configuredFeatures == null)
			configuredFeatures = new ArrayList();
		if (candidates == null)
			candidates = new ArrayList();
		
		// check for <includes> cycle
		if (candidates.contains(feature)) {
			String msg = UpdateUtils.getFormattedMessage(
					KEY_CYCLE, 
					new String[] {feature.getLabel(), 
							feature.getVersionedIdentifier().toString()});
			IStatus status = createStatus(feature, FeatureStatus.CODE_CYCLE, msg);
			throw new CoreException(status);
		}

		// potential candidate
		candidates.add(feature);
		
		// recursively, check cycles with children
		IIncludedFeatureReference[] children =
		feature.getIncludedFeatureReferences();
		for (int i = 0; i < children.length; i++) {
			try {
				IFeature child = children[i].getFeature(null);
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
	private static void checkConstraints(ArrayList features, ArrayList status)
		throws CoreException {
		if (features == null)
			return;

		ArrayList plugins = computePluginsForFeatures(features);

		checkEnvironment(features, status);
		checkPlatformFeature(features, plugins, status);
		checkPrimaryFeature(features, status);
		checkPrereqs(features, plugins, status);
	}

	/*
	 * Verify all features are either portable, or match the current
	 * environment
	 */
	private static void checkEnvironment(
		ArrayList features,
		ArrayList status) {

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
					IStatus s =
						createStatus(feature, FeatureStatus.CODE_ENVIRONMENT, UpdateUtils.getString(KEY_OS));
					if (!status.contains(s))
						status.add(s);
					continue;
				}
			}

			if (fws.size() > 0) {
				if (!fws.contains(ws)) {
					IStatus s =
						createStatus(feature, FeatureStatus.CODE_ENVIRONMENT, UpdateUtils.getString(KEY_WS));
					if (!status.contains(s))
						status.add(s);
					continue;
				}
			}

			if (farch.size() > 0) {
				if (!farch.contains(arch)) {
					IStatus s =
						createStatus(feature, FeatureStatus.CODE_ENVIRONMENT, UpdateUtils.getString(KEY_ARCH));
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
	private static void checkPlatformFeature(
		ArrayList features,
		ArrayList plugins,
		ArrayList status) {

		String[] bootstrapPlugins =
			ConfiguratorUtils
				.getCurrentPlatformConfiguration()
				.getBootstrapPluginIdentifiers();

		for (int i = 0; i < bootstrapPlugins.length; i++) {
			boolean found = false;
			for (int j = 0; j < plugins.size(); j++) {
				IPluginEntry plugin = (IPluginEntry) plugins.get(j);
				if (bootstrapPlugins[i]
					.equals(plugin.getVersionedIdentifier().getIdentifier())) {
					found = true;
					break;
				}
			}
			if (!found) {
				IStatus s =
					createStatus(null, FeatureStatus.CODE_OTHER, UpdateUtils.getString(KEY_PLATFORM));
				if (!status.contains(s))
					status.add(s);

				return;
			}
		}
	}

	/*
	 * Verify we end up with a version of primary feature configured
	 */
	private static void checkPrimaryFeature(
		ArrayList features,
		ArrayList status) {

		String featureId =
			ConfiguratorUtils
				.getCurrentPlatformConfiguration()
				.getPrimaryFeatureIdentifier();
		
		if (featureId == null)
			return; // no existing primary feature, nothing to worry about

		for (int i = 0; i < features.size(); i++) {
			IFeature feature = (IFeature) features.get(i);
			if (featureId
				.equals(feature.getVersionedIdentifier().getIdentifier()))
				return;
		}

		IStatus s = createStatus(null, FeatureStatus.CODE_OTHER, UpdateUtils.getString(KEY_PRIMARY));
		if (!status.contains(s))
			status.add(s);
	}

	/*
	 * Verify we do not break prereqs
	 */
	private static void checkPrereqs(
		ArrayList features,
		ArrayList plugins,
		ArrayList status) {

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
				boolean featurePrereq =
					iimport.getKind() == IImport.KIND_FEATURE;
				boolean ignoreVersion =
					version.getMajorComponent() == 0
						&& version.getMinorComponent() == 0
						&& version.getServiceComponent() == 0;
				int rule = iimport.getRule();
				if (rule == IImport.RULE_NONE)
					rule = IImport.RULE_COMPATIBLE;

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
						else if (
							rule == IImport.RULE_PERFECT
								&& cversion.isPerfect(version))
							found = true;
						else if (
							rule == IImport.RULE_EQUIVALENT
								&& cversion.isEquivalentTo(version))
							found = true;
						else if (
							rule == IImport.RULE_COMPATIBLE
								&& cversion.isCompatibleWith(version))
							found = true;
						else if (
							rule == IImport.RULE_GREATER_OR_EQUAL
								&& cversion.isGreaterOrEqualTo(version))
							found = true;
					}
					if (found)
						break;
				}

				if (!found) {
					// report status
					String target =
						featurePrereq
							? UpdateUtils.getString(KEY_PREREQ_FEATURE)
							: UpdateUtils.getString(KEY_PREREQ_PLUGIN);
					int errorCode = featurePrereq
							? FeatureStatus.CODE_PREREQ_FEATURE
							: FeatureStatus.CODE_PREREQ_PLUGIN;
					String msg =
						UpdateUtils.getFormattedMessage(
							KEY_PREREQ,
							new String[] { target, id });

					if (!ignoreVersion) {
						if (rule == IImport.RULE_PERFECT)
							msg =
								UpdateUtils.getFormattedMessage(
									KEY_PREREQ_PERFECT,
									new String[] {
										target,
										id,
										version.toString()});
						else if (rule == IImport.RULE_EQUIVALENT)
							msg =
								UpdateUtils.getFormattedMessage(
									KEY_PREREQ_EQUIVALENT,
									new String[] {
										target,
										id,
										version.toString()});
						else if (rule == IImport.RULE_COMPATIBLE)
							msg =
								UpdateUtils.getFormattedMessage(
									KEY_PREREQ_COMPATIBLE,
									new String[] {
										target,
										id,
										version.toString()});
						else if (rule == IImport.RULE_GREATER_OR_EQUAL)
							msg =
								UpdateUtils.getFormattedMessage(
									KEY_PREREQ_GREATER,
									new String[] {
										target,
										id,
										version.toString()});
					}
					IStatus s = createStatus(feature, errorCode, msg);
					if (!status.contains(s))
						status.add(s);
				}
			}
		}
	}

	/*
	 * Verify we end up with valid nested features after revert
	 */
	private static void checkRevertConstraints(
		ArrayList features,
		ArrayList status) {

		for (int i = 0; i < features.size(); i++) {
			IFeature feature = (IFeature) features.get(i);
			try {
				computeFeatureSubtree(
					feature,
					null,
					null,
					false /* do not tolerate missing children */,
					null,
					null
				);
			} catch (CoreException e) {
				status.add(e.getStatus());
			}
		}
	}

	/*
	 * Verify that a parent of an optional child is configured before we allow
	 * the child to be configured as well
	 */

	private static void checkOptionalChildConfiguring(
		IFeature feature,
		ArrayList status)
		throws CoreException {
		ILocalSite localSite = SiteManager.getLocalSite();
		IInstallConfiguration config = localSite.getCurrentConfiguration();
		IConfiguredSite[] csites = config.getConfiguredSites();

		boolean included = false;
		for (int i = 0; i < csites.length; i++) {
			IConfiguredSite csite = csites[i];
			ISiteFeatureReference[] crefs =
				csite.getSite().getFeatureReferences();
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
			String msg = UpdateUtils.getString(KEY_OPTIONAL_CHILD);
			status.add(createStatus(feature, FeatureStatus.CODE_OPTIONAL_CHILD, msg));
		} else {
			//feature is root - can be configured
		}
	}

	/**
	 * Checks if the configuration is locked by other instances
	 * 
	 * @param status
	 */
	private static void checkConfigurationLock(ArrayList status) {
/*		IPlatformConfiguration config =
			BootLoader.getCurrentPlatformConfiguration();
		URL configURL = config.getConfigurationLocation();
		if (!"file".equals(configURL.getProtocol())) {
			status.add(
				createStatus(
					null,
					"Configuration location is not writable:" + configURL));
			return;
		}
		String locationString = configURL.getFile();
		File configDir = new File(locationString);
		if (!configDir.isDirectory())
			configDir = configDir.getParentFile();
		if (!configDir.exists()) {
			status.add(
				createStatus(null, "Configuration location does not exist"));
			return;
		}
		File locksDir = new File(configDir, "locks");
		// check all the possible lock files
		File[] lockFiles = locksDir.listFiles();
		File configLock = BootLoader.getCurrentPlatformConfiguration().getLockFile();
		for (int i = 0; i < lockFiles.length; i++) {
			if (lockFiles[i].equals(configLock))
				continue;
			try {
				RandomAccessFile raf = new RandomAccessFile(lockFiles[i], "rw");
				FileChannel channel = raf.getChannel();
				System.out.println(channel.isOpen());
				FileLock lock = channel.tryLock();
				if (lock == null){
					// there is another eclipse instance running
					raf.close();
					status.add(
						createStatus(
							null,
							"Another instance is running, please close it before performing any configuration operations"));
					return;
				}

			} catch (Exception e) {
				status.add(createStatus(null, "Failed to create lock:"+lockFiles[i]));
				return;
			} 
		}*/
	}

	private static boolean isParent(
		IFeature candidate,
		IFeature feature,
		boolean optionalOnly)
		throws CoreException {
		IIncludedFeatureReference[] refs =
			candidate.getIncludedFeatureReferences();
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

	private static IStatus createMultiStatus(
		String rootKey,
		ArrayList children,
		int code) {
		IStatus[] carray =
			(IStatus[]) children.toArray(new IStatus[children.size()]);
		String message = UpdateUtils.getString(rootKey);
		return new MultiStatus(
			UpdateCore.getPlugin().getBundle().getSymbolicName(),
			code,
			carray,
			message,
			null);
	}

	private static IStatus createStatus(IFeature feature, int errorCode, String message) {

		String fullMessage;
		if (feature == null)
			fullMessage = message;
		else {
			PluginVersionIdentifier version =
				feature.getVersionedIdentifier().getVersion();
			fullMessage =
				UpdateUtils.getFormattedMessage(
					KEY_CHILD_MESSAGE,
					new String[] {
						feature.getLabel(),
						version.toString(),
						message });
		}

		return new FeatureStatus(
			feature,
			IStatus.ERROR,
			UpdateCore.getPlugin().getBundle().getSymbolicName(),
			errorCode,
			fullMessage,
			null);
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

	private static IStatus createCombinedReportStatus(
		ArrayList beforeStatus,
		ArrayList status) {
		if (beforeStatus.size() == 0) { // good initial config
			if (status.size() == 0) {
				return null; // all fine
			} else {
				return createMultiStatus(
					KEY_ROOT_MESSAGE,
					status,
					IStatus.ERROR);
				// error after operation
			}
		} else { // beforeStatus.size() > 0 : initial config errors
			if (status.size() == 0) {
				return null; // errors will be fixed
			} else {
				if (isBetterStatus(beforeStatus, status)) {
					return createMultiStatus(
						"ActivityConstraints.warning",
						beforeStatus,
						IStatus.WARNING);
					// errors may be fixed
				} else {
					ArrayList combined = new ArrayList();
					combined.add(
						createMultiStatus(
							"ActivityConstraints.beforeMessage",
							beforeStatus,
							IStatus.ERROR));
					combined.add(
						createMultiStatus(
							"ActivityConstraints.afterMessage",
							status,
							IStatus.ERROR));
					return createMultiStatus(
						KEY_ROOT_MESSAGE_INIT,
						combined,
						IStatus.ERROR);
				}
			}
		}
	}

	private static ArrayList createList(String commaSeparatedList) {
		ArrayList list = new ArrayList();
		if (commaSeparatedList != null) {
			StringTokenizer t =
				new StringTokenizer(commaSeparatedList.trim(), ",");
			while (t.hasMoreTokens()) {
				String token = t.nextToken().trim();
				if (!token.equals(""))
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
	private static boolean isBetterStatus(
		ArrayList beforeStatus,
		ArrayList status) {
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

}