package org.eclipse.update.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
import java.io.File;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.update.core.model.ModelObject;

/**
 *
 */
public class SessionDelta extends ModelObject implements ISessionDelta {

	private Date date;
	private List featureReferences;
	private File file;
	private int process;
	private boolean deleted = false;

	/**
	 * Constructor for SessionDelta.
	 */
	public SessionDelta() {
		super();
		process = ENABLE;
		deleted = false;
	}

	/**
	 * @see ISessionDelta#getFeatureReferences()
	 */
	public IFeatureReference[] getFeatureReferences() {
		if (featureReferences == null)
			return new IFeatureReference[0];

		return (IFeatureReference[]) featureReferences.toArray(arrayTypeFor(featureReferences));
	}

	/**
	 * @see ISessionDelta#getDate()
	 */
	public Date getDate() {
		return date;
	}

	/**
	 * @see ISessionDelta#process(IProgressMonitor)
	 */
	public void process(IProgressMonitor pm) throws CoreException {

		createInstallConfiguration();

		// process all feature references to configure
		// find the configured site each feature belongs to
		if (process == ENABLE) {
			if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_CONFIGURATION)
				UpdateManagerPlugin.warn("ENABLE SESSION DELTA");
			if (featureReferences != null && featureReferences.size() > 0) {
				// since 2.0.2 ISite.getConfiguredSite()
				// find the configuredSite that maintains this featureReference
				// configure the feature

				// remove duplicate and any efixes that should not be enabled
				List featureReferencesToEnable = processDuplicate(featureReferences);

				// manage ProgressMonitor
				if (pm != null) {
					int nbFeatures = featureReferencesToEnable.size();
					pm.beginTask(Policy.bind("SessionDelta.EnableFeatures"), nbFeatures);
				}

				IFeatureReference ref = null;
				IConfiguredSite configSite = null;
				IFeature featureToConfigure = null;
				for (Iterator iter = featureReferencesToEnable.iterator(); iter.hasNext();) {
					ref = (IFeatureReference) iter.next();

					try {
						featureToConfigure = ref.getFeature();
					} catch (CoreException e) {
						UpdateManagerPlugin.warn(null, e);
					}

					if (featureToConfigure != null) {
						if (pm != null)
							pm.worked(1);

						configSite = ref.getSite().getCurrentConfiguredSite();
						try {
							// make sure only the latest version of the configured features
							// is configured across sites [16502]
							if (enable(featureToConfigure)) {
								configSite.configure(featureToConfigure);
							} else {
								configSite.unconfigure(featureToConfigure);
							}
						} catch (CoreException e) {
							// if I cannot configure one, 
							//then continue with others 
							UpdateManagerPlugin.warn("Unable to configure feature:" + featureToConfigure, e);
						}
					} else {
						UpdateManagerPlugin.warn("Unable to configure null feature:" + ref, null);
					}
				}
			}
		}

		delete();
		saveLocalSite();
	}

	/*
	 * 
	 */
	public void delete() {
		if (deleted) {
			UpdateManagerPlugin.warn("Attempt to delete an already deleted session delta:" + file);
			return;
		}

		// remove the file from the file system
		if (file != null) {
			UpdateManagerUtils.removeFromFileSystem(file);
			UpdateManagerPlugin.warn("Removing SessionDelta:" + file);
		} else {
			UpdateManagerPlugin.warn("Unable to remove SessionDelta. File is null");
		}

		deleted = true;
	}

	/**
	 * @see IAdaptable#getAdapter(Class)
	 */
	public Object getAdapter(Class adapter) {
		return null;
	}

	/*
	 *
	 */
	public void addReference(IFeatureReference reference) {
		if (featureReferences == null)
			featureReferences = new ArrayList();
		featureReferences.add(reference);
	}

	/*
	 * 
	 */
	public void setCreationDate(Date date) {
		this.date = date;
	}

	/*
	 * Sets the file. 
	 * We will remove the file
	 */
	public void setFile(File file) {
		this.file = file;
	}

	/*@
	 * @see ISessionDelta#getType()
	 */
	public int getType() {
		return process;
	}

	private void createInstallConfiguration() throws CoreException {
		ILocalSite localSite = SiteManager.getLocalSite();
		IInstallConfiguration config = localSite.cloneCurrentConfiguration();
		config.setLabel(Utilities.format(config.getCreationDate()));
		localSite.addConfiguration(config);
	}

	private void saveLocalSite() throws CoreException {
		ILocalSite localSite = SiteManager.getLocalSite();
		localSite.save();
	}

	/**
	 * return true if this feature should be configured 
	 * A feature should be configure if it has the highest version across 
	 * all configured features with the same identifier
	 */
	private boolean enable(IFeature newlyConfiguredFeatures) throws CoreException {

		ILocalSite siteLocal = SiteManager.getLocalSite();
		IInstallConfiguration currentConfiguration = siteLocal.getCurrentConfiguration();
		IConfiguredSite[] configuredSites;
		IFeatureReference[] configuredFeaturesRef;
		IFeature feature;
		configuredSites = currentConfiguration.getConfiguredSites();
		for (int i = 0; i < configuredSites.length; i++) {
			configuredFeaturesRef = configuredSites[i].getConfiguredFeatures();
			for (int j = 0; j < configuredFeaturesRef.length; j++) {
				try {
					feature = configuredFeaturesRef[j].getFeature();
					int result = compare(newlyConfiguredFeatures, feature);
					if (result != 0) {
						if (result == 1) {
							ConfiguredSite cSite = (ConfiguredSite) configuredSites[i];
							cSite.unconfigure(feature);
							return true;
						}
						if (result == 2) {
							return false;
						}
					}
				} catch (CoreException e) {
					UpdateManagerPlugin.warn(null, e);
				}
			}
		}
		// feature not found, configure it then
		return true;
	}

	/**
	 * compare two feature references
	 * returns 0 if the feature are different
	 * returns 1 if the version of feature 1 is greater than the version of feature 2
	 * returns 2 if opposite
	 */
	private int compare(IFeature feature1, IFeature feature2) throws CoreException {

		// TRACE
		if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_RECONCILER) {
			UpdateManagerPlugin.debug("Compare: " + feature1 + " && " + feature2);
		}

		if (feature1 == null)
			return 0;

		if (feature1 == null || feature2 == null) {
			return 0;
		}

		VersionedIdentifier id1 = feature1.getVersionedIdentifier();
		VersionedIdentifier id2 = feature2.getVersionedIdentifier();

		if (id1 == null || id2 == null) {
			return 0;
		}

		if (id1.getIdentifier() != null && id1.getIdentifier().equals(id2.getIdentifier())) {
			PluginVersionIdentifier version1 = id1.getVersion();
			PluginVersionIdentifier version2 = id2.getVersion();
			if (version1 != null) {
				if (version1.isGreaterThan(version2)) {
					return 1;
				} else {
					return 2;
				}
			} else {
				return 2;
			}
		}
		return 0;
	};

	/*
	 * Method processDuplicate.
	 * @param featureReferences
	 * @return List of features to enable of empty list
	 */
	private List processDuplicate(List featureReferencesToProcess) {

		// eliminate duplicate versions (keep latest)
		IFeatureReference[] list = (IFeatureReference[]) featureReferencesToProcess.toArray(new IFeatureReference[0]);
		boolean foundDuplicate = false;
		for (int i = 0; i < list.length - 1; i++) {
			IFeatureReference left = list[i];
			try {
				VersionedIdentifier leftVid = left.getVersionedIdentifier();
				for (int j = i + 1; j < list.length; j++) {
					IFeatureReference right = list[j];
					try {
						VersionedIdentifier rightVid = right.getVersionedIdentifier();
						if (leftVid.getIdentifier().equals(rightVid.getIdentifier())) {
							// duplicate versions ... keep latest
							IFeatureReference oldest = null;
							// bug 31940. If right>left remove left ELSE REMOVE RIGHT
							if (rightVid.getVersion().isGreaterOrEqualTo(leftVid.getVersion()))
								oldest = left;
							else
								oldest = right;
							featureReferencesToProcess.remove(oldest);
							// debug
							if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_RECONCILER) {
								UpdateManagerPlugin.debug("Removing \"duplicate\" during delta processing" + oldest.getVersionedIdentifier().toString());
							}
							foundDuplicate = true;
						}
					} catch (CoreException e) {
					};
				}
			} catch (CoreException e1) {
			};
		}

		// duplicate found, remove any efixes of the removed duplicate
		if (foundDuplicate) {
			ArrayList result = new ArrayList();
			Map patches = getPatchesAsFeatureReference(featureReferencesToProcess);
			if (!patches.isEmpty()){
				List patchesToEnable = getFeatureReferencePatchesToEnable(patches,featureReferencesToProcess);
				featureReferencesToProcess.removeAll(patches.keySet());
				// add efixes first so they will be processed first and may be disabled
				result.addAll(patchesToEnable);
				result.addAll(featureReferencesToProcess);
				return result;
			}				
		}
		return featureReferencesToProcess;
	}

	/*
	 * get the map of enabled patches (as feature reference)  or an empty map
	 */
	private Map getPatchesAsFeatureReference(List listOfFeatureReferences) {
		// get all efixes and the associated patched features
		Map patches = new HashMap();
		if (listOfFeatureReferences != null) {
			Iterator iter = listOfFeatureReferences.iterator();
			while (iter.hasNext()) {
				List patchedFeaturesID = new ArrayList();
				IFeatureReference element = (IFeatureReference) iter.next();
				// add the patched feature identifiers
				try {
					IFeature feature = element.getFeature();
					if (feature != null) {
						IImport[] imports = feature.getImports();
						for (int i = 0; i < imports.length; i++) {
							if (imports[i].isPatch()) {
								VersionedIdentifier id = imports[i].getVersionedIdentifier();
								if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_RECONCILER)
									UpdateManagerPlugin.debug("Found patch " + element + " for feature identifier " + id);
								patchedFeaturesID.add(id);
							}
						}
					}

					if (!patchedFeaturesID.isEmpty()) {
						patches.put(element, patchedFeaturesID);
					}
				} catch (CoreException e) {
				}
			}
		}
		return patches;
	}

	/*
		 * retruns the list of pathes-featureReference who patch enabled features
		 */
	private List getFeatureReferencePatchesToEnable(Map efixes, List configuredFeatureReferences) {

		ArrayList enabledVersionedIdentifier = new ArrayList();
		Iterator iter = configuredFeatureReferences.iterator();
		while (iter.hasNext()) {
			IFeatureReference element = (IFeatureReference) iter.next();
			try {
				enabledVersionedIdentifier.add(element.getVersionedIdentifier());
			} catch (CoreException e){};
		}

		// loop through the patches
		List result = new ArrayList();
		iter = efixes.keySet().iterator();
		while (iter.hasNext()) {
			boolean toEnable = false;
			IFeatureReference efixFeatureReference = (IFeatureReference) iter.next();
			List patchedFeatures = (List) efixes.get(efixFeatureReference);
			// loop through the 'patched features identifier' the for this patch
			// see if it the patch patches at least one enable feature
			Iterator patchedFeaturesIter = patchedFeatures.iterator();
			while (patchedFeaturesIter.hasNext() && !toEnable) {
				VersionedIdentifier patchedFeatureID = (VersionedIdentifier) patchedFeaturesIter.next();
				if (enabledVersionedIdentifier.contains(patchedFeatureID)) {
					toEnable = true;
				}
			}

			if (!toEnable) {
				if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_RECONCILER)
					UpdateManagerPlugin.debug("The Patch " + efixFeatureReference + " does not patch any enabled features: it will be disabled");
			} else {
				if (UpdateManagerPlugin.DEBUG && UpdateManagerPlugin.DEBUG_SHOW_RECONCILER)
					UpdateManagerPlugin.debug("The patch " + efixFeatureReference + " will be enabled.");

				result.add(efixFeatureReference);
			}
		}
		return result;
	}

}