package org.eclipse.update.internal.ui.forms;

import java.util.*;

import org.eclipse.core.boot.BootLoader;
import org.eclipse.core.runtime.*;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.ui.UpdateUIPlugin;
import org.eclipse.update.internal.ui.model.PendingChange;

/**
 *
 */
public class ActivityConstraints {
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
	private static final String KEY_PATCH_REGRESSION = 
		"ActivityConstraints.patchRegression";
	private static final String KEY_PATCH_UNCONFIGURE = 
		"ActivityConstraints.patchUnconfigure";
	private static final String KEY_PATCH_UNCONFIGURE_BACKUP = 
		"ActivityConstraints.patchUnconfigureBackup";
	private static final String KEY_PATCH_MISSING_TARGET = 
		"ActivityConstraints.patchMissingTarget";
	private static final String KEY_OPTIONAL_CHILD =
		"ActivityConstraints.optionalChild";
	private static final String KEY_CYCLE = "ActivityConstraints.cycle";
	private static final String KEY_CONFLICT = "ActivityConstraints.conflict";
	private static final String KEY_WRONG_TIMELINE = "ActivityConstraints.timeline";

	/*
	 * Called by UI before performing operation
	 */
	public static IStatus validatePendingChange(PendingChange job) {
		// check initial state
		ArrayList beforeStatus = new ArrayList();
		validateInitialState(beforeStatus);

		// check proposed change
		ArrayList status = new ArrayList();
		switch (job.getJobType()) {
			case PendingChange.UNCONFIGURE :
				validateUnconfigure(job.getFeature(), status);
				break;
			case PendingChange.CONFIGURE :
				validateConfigure(job.getFeature(), status);
				break;
			case PendingChange.INSTALL :
				validateInstall(job.getOldFeature(), job.getFeature(), status);
				break;
		}

		// report status
		if (status.size() > 0) {
			if (beforeStatus.size() > 0)
				return createMultiStatus(KEY_ROOT_MESSAGE_INIT, beforeStatus);
			else
				return createMultiStatus(KEY_ROOT_MESSAGE, status);
		}
		return null;
	}

	/*
	 * Called by UI before processing a delta
	 */
	public static IStatus validateSessionDelta(ISessionDelta delta) {
		// check initial state
		ArrayList beforeStatus = new ArrayList();
		validateInitialState(beforeStatus);

		// check proposed change
		ArrayList status = new ArrayList();
		switch (delta.getType()) {
			case ISessionDelta.ENABLE :
				validateDeltaConfigure(delta, status);
				break;
		}

		// report status
		if (status.size() > 0) {
			if (beforeStatus.size() > 0)
				return createMultiStatus(KEY_ROOT_MESSAGE_INIT, beforeStatus);
			else
				return createMultiStatus(KEY_ROOT_MESSAGE, status);
		}
		return null;
	}

	/*
	 * Called by the UI before doing a revert/ restore operation
	 */
	public static IStatus validatePendingRevert(IInstallConfiguration config) {
		// check initial state
		ArrayList beforeStatus = new ArrayList();
		validateInitialState(beforeStatus);

		// check proposed change
		ArrayList status = new ArrayList();
		validateRevert(config, status);

		// report status
		if (status.size() > 0) {
			if (beforeStatus.size() > 0)
				return createMultiStatus(KEY_ROOT_MESSAGE_INIT, beforeStatus);
			else
				return createMultiStatus(KEY_ROOT_MESSAGE, status);
		}
		return null;
	}

	/*
	 * Called by the UI before doing a one-click update operation
	 */
	public static IStatus validatePendingOneClickUpdate(PendingChange[] jobs) {
		// check initial state
		ArrayList beforeStatus = new ArrayList();
		validateInitialState(beforeStatus);

		// check proposed change
		ArrayList status = new ArrayList();
		validateOneClickUpdate(jobs, status);

		// report status
		if (status.size() > 0) {
			if (beforeStatus.size() > 0)
				return createMultiStatus(KEY_ROOT_MESSAGE_INIT, beforeStatus);
			else
				return createMultiStatus(KEY_ROOT_MESSAGE, status);
		}
		return null;
	}

	/*
	 * Check to see if we are not broken even before we start
	 */
	private static void validateInitialState(ArrayList status) {
		try {
			ArrayList features = computeFeatures();
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
			if (UpdateUIPlugin.isPatch(feature)) {
				IInstallConfiguration backup = UpdateUIPlugin.getBackupConfigurationFor(feature);
				String msg;
				if (backup!=null)
					msg = UpdateUIPlugin.getFormattedMessage(KEY_PATCH_UNCONFIGURE_BACKUP, backup.getLabel());
				else
					msg = UpdateUIPlugin.getResourceString(KEY_PATCH_UNCONFIGURE);
				status.add(createStatus(feature, msg));
				return;
			}
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
			ArrayList features = computeFeatures();
			checkOptionalChildConfiguring(feature, status);
			features = computeFeaturesAfterOperation(features, feature, null);
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
			ArrayList features = computeFeatures();
			if (oldFeature == null && isPatch(newFeature))
				checkUnique(newFeature, features, status);
			features =
				computeFeaturesAfterOperation(features, newFeature, oldFeature);
			checkConstraints(features, status);
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
			// check the timeline and don't bother
			// to check anything else if negative
			if (!checkTimeline(config, status)) return;
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
		ArrayList status) {
		try {
			ArrayList features = computeFeaturesAfterDelta(delta);
			checkConstraints(features, status);

		} catch (CoreException e) {
			status.add(e.getStatus());
		}
	}

	/*
	 * Handle one-click changes as a batch
	 */
	private static void validateOneClickUpdate(
		PendingChange[] jobs,
		ArrayList status) {
		try {
			ArrayList features = computeFeatures();
			ArrayList savedFeatures = features;

			// pass 1: see if we can process the entire "batch"
			ArrayList tmpStatus = new ArrayList();
			for (int i = 0; i < jobs.length; i++) {
				IFeature newFeature = jobs[i].getFeature();
				IFeature oldFeature = jobs[i].getOldFeature();
				features =
					computeFeaturesAfterOperation(
						features,
						newFeature,
						oldFeature);
			}
			checkConstraints(features, tmpStatus);
			if (tmpStatus.size() == 0) // the whole "batch" is OK
				return;

			// pass 2: we have conflicts
			features = savedFeatures;
			for (int i = 0; i < jobs.length; i++) {
				IFeature newFeature = jobs[i].getFeature();
				IFeature oldFeature = jobs[i].getOldFeature();
				features =
					computeFeaturesAfterOperation(
						features,
						newFeature,
						oldFeature);
				checkConstraints(features, status);
				if (status.size() > 0) {
					IStatus conflict =
						createStatus(
							newFeature,
							UpdateUIPlugin.getResourceString(KEY_CONFLICT));
					status.add(0, conflict);
					return;
				}
			}
		} catch (CoreException e) {
			status.add(e.getStatus());
		}
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
				IFeature cfeature = cref.getFeature();
				features.add(cfeature);
			}
		}

		return features;
	}

	/*
	 * Compute the nested feature subtree starting at the specified base feature
	 */
	private static ArrayList computeFeatureSubtree(
		IFeature top,
		IFeature feature,
		ArrayList features,
		boolean tolerateMissingChildren)
		throws CoreException {

		// check arguments
		if (features == null)
			features = new ArrayList();
		if (top == null)
			return features;
		if (feature == null)
			feature = top;

		// check for <includes> cycle
		if (features.contains(feature)) {
			IStatus status =
				createStatus(top, UpdateUIPlugin.getResourceString(KEY_CYCLE));
			throw new CoreException(status);
		}

		// return specified base feature and all its children
		features.add(feature);
		IFeatureReference[] children = feature.getIncludedFeatureReferences();
		for (int i = 0; i < children.length; i++) {
			try {
				IFeature child = children[i].getFeature();
				features =
					computeFeatureSubtree(
						top,
						child,
						features,
						tolerateMissingChildren);
			} catch (CoreException e) {
				if (!children[i].isOptional() && !tolerateMissingChildren)
					throw e;
			}
		}
		return features;
	}

	/*
	 * Compute a list of features that will be configured after the operation
	 */
	private static ArrayList computeFeaturesAfterOperation(
		ArrayList features,
		IFeature add,
		IFeature remove)
		throws CoreException {

		ArrayList addTree =
			computeFeatureSubtree(
				add,
				null,
				null,
				false /* do not tolerate missing children */
		);
		ArrayList removeTree =
			computeFeatureSubtree(
				remove,
				null,
				null,
				true /* tolerate missing children */
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
		
		for (int i=0; i<removeTree.size(); i++) {
			IFeature feature = (IFeature)removeTree.get(i);
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
			if (UpdateUIPlugin.isPatch(feature, candidate)) {
				ArrayList removeTree =
					computeFeatureSubtree(candidate, null, null, true);
				result.addAll(removeTree);
			}
		}
	}

	/*
	 * Compute a list of features that will be configured after performing the revert
	 */
	private static ArrayList computeFeaturesAfterRevert(IInstallConfiguration config)
		throws CoreException {

		ArrayList list = new ArrayList();
		IConfiguredSite[] csites = config.getConfiguredSites();
		for (int i = 0; i < csites.length; i++) {
			IConfiguredSite csite = csites[i];
			IFeatureReference[] features = csite.getConfiguredFeatures();
			for (int j = 0; j < features.length; j++) {
				list.add(features[j].getFeature());
			}
		}
		return list;
	}

	/*
	 * Compute a list of features that will be configured after applying the
	 * specified delta
	 */
	private static ArrayList computeFeaturesAfterDelta(ISessionDelta delta)
		throws CoreException {

		IFeatureReference[] deltaRefs;
		if (delta == null)
			deltaRefs = new IFeatureReference[0];
		else
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
				IFeature cfeature = cref.getFeature();
				siteFeatures.add(cfeature);
			}

			// add deltas for the site			
			for (int j = 0; j < deltaRefs.length; j++) {
				ISite deltaSite = deltaRefs[j].getSite();
				if (deltaSite.equals(csite.getSite())) {
					IFeature dfeature = deltaRefs[j].getFeature();
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
							siteFeatures.remove(array[j]);
							removeTree.add(array[j]);
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

	private static boolean isPatch(IFeature feature) {
		IImport[] imports = feature.getImports();
		for (int i = 0; i < imports.length; i++) {
			IImport iimport = imports[i];
			if (iimport.isPatch()) {
				return true;
			}
		}
		return false;
	}

	/*
	 * 
	 */
	private static void checkUnique(
		IFeature feature,
		ArrayList features,
		ArrayList status)
		throws CoreException {
		if (features == null)
			return;
		IFeatureReference[] irefs = feature.getIncludedFeatureReferences();
		for (int i = 0; i < irefs.length; i++) {
			IFeatureReference iref = irefs[i];
			IFeature ifeature = iref.getFeature();
			boolean patch = isPatch(ifeature);
			VersionedIdentifier vid = ifeature.getVersionedIdentifier();
			String id = vid.getIdentifier();
			PluginVersionIdentifier version = vid.getVersion();
			boolean found = false;
			for (int j = 0; j < features.size(); j++) {
				IFeature candidate = (IFeature) features.get(j);
				VersionedIdentifier cvid = candidate.getVersionedIdentifier();
				String cid = cvid.getIdentifier();
				PluginVersionIdentifier cversion = cvid.getVersion();
				if (cid.equals(id)) {
					// The same identifier - this one will
					// be unconfigured. Check if it is lower,
					// otherwise flag.
					found = true;
					// Ignore equal - will be filtered in the download
					if (version.equals(cversion)) continue;
					// Flag only the case when the installed one is
					// newer than the one that will be installed.
					if (!version.isGreaterThan(cversion)) {
						// Don't allow this.
						String msg = UpdateUIPlugin.getFormattedMessage(KEY_PATCH_REGRESSION,
									new String[] { ifeature.getLabel(), version.toString() });
						status.add(createStatus(feature, msg));

					}
				}
			}
			if (!found) {
				// All the features carried in a patch must
				// already be present, unless this feature
				// is a patch itself.
				//
				// 30849: optional feature does not need
				// to be present.
				if (!patch && iref.isOptional()==false) {
					String msg = UpdateUIPlugin.getFormattedMessage(KEY_PATCH_MISSING_TARGET,
							new String[] { ifeature.getLabel(), version.toString() });
					status.add(createStatus(feature, msg));
				}
			}
			if (patch) checkUnique(ifeature, features, status);
		}
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
	 * Verify all features are either portable, or match the current environment
	 */
	private static void checkEnvironment(
		ArrayList features,
		ArrayList status) {

		String os = BootLoader.getOS();
		String ws = BootLoader.getWS();
		String arch = BootLoader.getOSArch();

		for (int i = 0; i < features.size(); i++) {
			IFeature feature = (IFeature) features.get(i);
			ArrayList fos = createList(feature.getOS());
			ArrayList fws = createList(feature.getWS());
			ArrayList farch = createList(feature.getArch());

			if (fos.size() > 0) {
				if (!fos.contains(os)) {
					status.add(
						createStatus(
							feature,
							UpdateUIPlugin.getResourceString(KEY_OS)));
					continue;
				}
			}

			if (fws.size() > 0) {
				if (!fws.contains(ws)) {
					status.add(
						createStatus(
							feature,
							UpdateUIPlugin.getResourceString(KEY_WS)));
					continue;
				}
			}

			if (farch.size() > 0) {
				if (!farch.contains(arch)) {
					status.add(
						createStatus(
							feature,
							UpdateUIPlugin.getResourceString(KEY_ARCH)));
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
			BootLoader
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
				status.add(
					createStatus(
						null,
						UpdateUIPlugin.getResourceString(KEY_PLATFORM)));
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
			BootLoader
				.getCurrentPlatformConfiguration()
				.getPrimaryFeatureIdentifier();

		for (int i = 0; i < features.size(); i++) {
			IFeature feature = (IFeature) features.get(i);
			if (featureId
				.equals(feature.getVersionedIdentifier().getIdentifier()))
				return;
		}

		status.add(
			createStatus(null, UpdateUIPlugin.getResourceString(KEY_PRIMARY)));
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
				// for each import determine plugin or feature, version, match we need
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
							? UpdateUIPlugin.getResourceString(
								KEY_PREREQ_FEATURE)
							: UpdateUIPlugin.getResourceString(KEY_PREREQ_PLUGIN);
					String msg =
						UpdateUIPlugin.getFormattedMessage(
							KEY_PREREQ,
							new String[] { target, id });

					if (!ignoreVersion) {
						if (rule == IImport.RULE_PERFECT)
							msg =
								UpdateUIPlugin.getFormattedMessage(
									KEY_PREREQ_PERFECT,
									new String[] {
										target,
										id,
										version.toString()});
						else if (rule == IImport.RULE_EQUIVALENT)
							msg =
								UpdateUIPlugin.getFormattedMessage(
									KEY_PREREQ_EQUIVALENT,
									new String[] {
										target,
										id,
										version.toString()});
						else if (rule == IImport.RULE_COMPATIBLE)
							msg =
								UpdateUIPlugin.getFormattedMessage(
									KEY_PREREQ_COMPATIBLE,
									new String[] {
										target,
										id,
										version.toString()});
						else if (rule == IImport.RULE_GREATER_OR_EQUAL)
							msg =
								UpdateUIPlugin.getFormattedMessage(
									KEY_PREREQ_GREATER,
									new String[] {
										target,
										id,
										version.toString()});
					}
					status.add(createStatus(feature, msg));
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
					false /* do not tolerate missing children */
				);
			} catch (CoreException e) {
				status.add(e.getStatus());
			}
		}
	}

	/*
	 * Verify that a parent of an optional child is configured
	 * before we allow the child to be configured as well
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
			IFeatureReference[] crefs = csite.getSite().getFeatureReferences();
			for (int j = 0; j < crefs.length; j++) {
				IFeatureReference cref = crefs[j];
				IFeature cfeature = null;
				try {
					cfeature = cref.getFeature();
				} catch (CoreException e) {
					// Ignore missing optional feature.
					if (cref.isOptional())
						continue;
					else
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
			String msg = UpdateUIPlugin.getResourceString(KEY_OPTIONAL_CHILD);
			status.add(createStatus(feature, msg));
		} else {
			//feature is root - can be configured
		}
	}

	private static boolean isParent(
		IFeature candidate,
		IFeature feature,
		boolean optionalOnly)
		throws CoreException {
		IFeatureReference[] refs = candidate.getIncludedFeatureReferences();
		for (int i = 0; i < refs.length; i++) {
			IFeatureReference child = refs[i];
			VersionedIdentifier cvid;
			try {
				cvid = child.getVersionedIdentifier();
			} catch (CoreException e) {
				// Ignore missing optional children
				if (child.isOptional())
					continue;
				else
					throw e;
			}
			if (feature.getVersionedIdentifier().equals(cvid)) {
				// included; return true if optionality is not 
				// important or it is and the inclusion is optional
				return optionalOnly == false || child.isOptional();
			}
		}
		return false;
	}
	
	private static boolean checkTimeline(IInstallConfiguration config, ArrayList status) {
		try {
			ILocalSite lsite = SiteManager.getLocalSite();
			IInstallConfiguration cconfig = lsite.getCurrentConfiguration();
			if (cconfig.getTimeline()!=config.getTimeline()) {
				// Not the same timeline - cannot revert
				String msg = UpdateUIPlugin.getFormattedMessage(KEY_WRONG_TIMELINE, config.getLabel());
				status.add(createStatus(null, msg));
				return false;
			}
		}
		catch (CoreException e) {
			status.add(e.getStatus());
		}
		return true;
	}

	private static IStatus createMultiStatus(
		String rootKey,
		ArrayList children) {
		IStatus[] carray =
			(IStatus[]) children.toArray(new IStatus[children.size()]);
		String message = UpdateUIPlugin.getResourceString(rootKey);
		return new MultiStatus(
			UpdateUIPlugin.getPluginId(),
			IStatus.ERROR,
			carray,
			message,
			null);
	}

	private static IStatus createStatus(IFeature feature, String message) {

		String fullMessage;
		if (feature == null)
			fullMessage = message;
		else {
			PluginVersionIdentifier version =
				feature.getVersionedIdentifier().getVersion();
			fullMessage =
				UpdateUIPlugin.getFormattedMessage(
					KEY_CHILD_MESSAGE,
					new String[] {
						feature.getLabel(),
						version.toString(),
						message });
		}

		return new Status(
			IStatus.ERROR,
			UpdateUIPlugin.getPluginId(),
			IStatus.OK,
			fullMessage,
			null);
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
}