/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.core;

import java.util.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.update.configuration.IConfiguredSite;
import org.eclipse.update.core.*;
import org.eclipse.update.core.model.ModelObject;

/**
 * This class manages the reconciliation.
 */

public class SiteReconciler extends ModelObject {

	private SiteReconciler(LocalSite siteLocal) {
		//never instantiated
	}

	/**
	 * Validate the list of configured features eliminating extra
	 * entries (if possible). Make sure we do not leave configured
	 * nested features with "holes" (ie. unconfigured children)
	 */
	public static void checkConfiguredFeatures(IConfiguredSite configuredSite) {

		// Note: if we hit errors in the various computation
		// methods and throw a CoreException, we will not catch it
		// in this method. Consequently we will not attempt to
		// unconfigure any "extra" features because we would 
		// likely get it wrong. The platform will run with extra features
		// configured. The runtime will eliminate extra plugins based
		// on runtime binding rules.

		// determine "proposed" list of configured features
		ConfiguredSite cSite = (ConfiguredSite) configuredSite;
		// debug
		if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_RECONCILER) {
			UpdateCore.debug("Validate configuration of site " + cSite.getSite().getURL()); //$NON-NLS-1$
		}
		IFeatureReference[] configuredRefs = cSite.getConfiguredFeatures();
		ArrayList allPossibleConfiguredFeatures = new ArrayList();
		for (int i = 0; i < configuredRefs.length; i++) {
			try {
				IFeature feature = configuredRefs[i].getFeature(null);
				allPossibleConfiguredFeatures.add(feature);
				// debug
				if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_RECONCILER) {
					UpdateCore.debug("   configured feature " + feature.getVersionedIdentifier().toString()); //$NON-NLS-1$
				}
			} catch (CoreException e) {
				UpdateCore.warn("", e); //$NON-NLS-1$
			}
		}

		// find top level features
		ArrayList topFeatures = computeTopFeatures(allPossibleConfiguredFeatures);

		// find non efix top level features
		ArrayList topNonEfixFeatures = getNonEfixFeatures(topFeatures);

		// expand non efix top level features (compute full nesting structures).
		ArrayList configuredFeatures = expandFeatures(topNonEfixFeatures, configuredSite);

		// retrieve efixes that patch enable feature
		// they must be kept enabled
		if (topFeatures.size() != topNonEfixFeatures.size()) {
			Map patches = getPatchesAsFeature(allPossibleConfiguredFeatures);
			if (!patches.isEmpty()) {
				// calculate efixes to enable
				List efixesToEnable = getPatchesToEnable(patches, configuredFeatures);
				// add efies to keep enable
				//add them to the enable list
				for (Iterator iter = efixesToEnable.iterator(); iter.hasNext();) {
					IFeature element = (IFeature) iter.next();
					ArrayList expandedEfix = new ArrayList();
					expandEfixFeature(element, expandedEfix, configuredSite);
					configuredFeatures.addAll(expandedEfix);
				}
			}
		}

		// compute extra features
		ArrayList extras = diff(allPossibleConfiguredFeatures, configuredFeatures);

		// unconfigure extra features
		ConfigurationPolicy cPolicy = cSite.getConfigurationPolicy();
		for (int i = 0; i < extras.size(); i++) {
			IFeature feature = (IFeature) extras.get(i);
			IFeatureReference ref = cSite.getSite().getFeatureReference(feature);
			try {
				cPolicy.unconfigure(ref, true, false);
				// debug
				if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_RECONCILER) {
					UpdateCore.debug("Unconfiguring \"extra\" feature " + feature.getVersionedIdentifier().toString()); //$NON-NLS-1$
				}
			} catch (CoreException e) {
				UpdateCore.warn("", e); //$NON-NLS-1$
			}
		}
	}

	/*
	 *  
	 */
	private static ArrayList computeTopFeatures(ArrayList features) {
		/* map of Feature by VersionedIdentifier */
		Map topFeatures = new HashMap(features.size());
		// start with the features passed in
		for (Iterator it = features.iterator(); it.hasNext();) {
			IFeature f = ((IFeature) it.next());
			topFeatures.put(f.getVersionedIdentifier(), f);
		}
		// remove all features that nest in some other feature
		for (Iterator it = features.iterator(); it.hasNext();) {
			try {
				IIncludedFeatureReference[] children = ((IFeature) it.next()).getIncludedFeatureReferences();
				for (int j = 0; j < children.length; j++) {
					try {
						topFeatures.remove(children[j].getVersionedIdentifier());
					} catch (CoreException e1) {
						if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_WARNINGS)
							UpdateCore.warn("", e1); //$NON-NLS-1$
					}
				}
			} catch (CoreException e) {
				UpdateCore.warn("", e); //$NON-NLS-1$
			}
		}
		ArrayList list = new ArrayList();
		list.addAll(topFeatures.values());
		// debug
		if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_RECONCILER) {
			UpdateCore.debug("Computed top-level features"); //$NON-NLS-1$
			for (int i = 0; i < topFeatures.size(); i++) {
				UpdateCore.debug("   " + ((IFeature) list.get(i)).getVersionedIdentifier().toString()); //$NON-NLS-1$
			}
		}
		return list;
	}

	/*
	 * 
	 */
	private static ArrayList expandFeatures(ArrayList features, IConfiguredSite configuredSite) {
		ArrayList result = new ArrayList();

		// expand all top level features
		for (int i = 0; i < features.size(); i++) {
			expandFeature((IFeature) features.get(i), result, configuredSite);
		}

		return result;
	}

	/*
	 * 
	 */
	private static void expandFeature(IFeature feature, ArrayList features, IConfiguredSite configuredSite) {

		// add feature
		if (!features.contains(feature)) {
			features.add(feature);
			// debug
			if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_RECONCILER) {
				UpdateCore.debug("Retaining configured feature " + feature.getVersionedIdentifier().toString()); //$NON-NLS-1$
			}
		}

		// add nested children to the list
		IIncludedFeatureReference[] children = null;
		try {
			children = feature.getIncludedFeatureReferences();
		} catch (CoreException e) {
			UpdateCore.warn("", e); //$NON-NLS-1$
			return;
		}

		for (int j = 0; j < children.length; j++) {
			IFeature child = null;
			try {
				child = children[j].getFeature(null);
			} catch (CoreException e) {
				if (!UpdateManagerUtils.isOptional(children[j]))
					UpdateCore.warn("", e); //$NON-NLS-1$
				// 25202 do not return right now, the peer children may be ok
			}
			if (child != null)
				expandFeature(child, features, configuredSite);
		}
	}

	/*
	 * 
	 */
	private static ArrayList diff(ArrayList left, ArrayList right) {
		ArrayList result = new ArrayList();

		// determine difference (left "minus" right)
		for (int i = 0; i < left.size(); i++) {
			IFeature feature = (IFeature) left.get(i);
			if (!right.contains(feature))
				result.add(feature);
		}
		return result;
	}

	/*
	 * get the list of enabled patches
	 */
	private static Map getPatchesAsFeature(ArrayList allConfiguredFeatures) {
		// get all efixes and the associated patched features
		Map patches = new HashMap();
		if (allConfiguredFeatures != null) {
			Iterator iter = allConfiguredFeatures.iterator();
			while (iter.hasNext()) {
				List patchedFeaturesID = new ArrayList();
				IFeature element = (IFeature) iter.next();
				// add the patched feature identifiers
				for (int i = 0; i < element.getImports().length; i++) {
					if (element.getImports()[i].isPatch()) {
						VersionedIdentifier id = element.getImports()[i].getVersionedIdentifier();
						if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_RECONCILER)
							UpdateCore.debug("Found patch " + element + " for feature identifier " + id); //$NON-NLS-1$ //$NON-NLS-2$
						patchedFeaturesID.add(id);
					}
				}

				if (!patchedFeaturesID.isEmpty()) {
					patches.put(element, patchedFeaturesID);
				}
			}
		}

		return patches;
	}

	/*
	 * retruns the list of pathes-feature who patch enabled features
	 */
	private static List getPatchesToEnable(Map efixes, ArrayList configuredFeatures) {

		ArrayList enabledVersionedIdentifier = new ArrayList();
		Iterator iter = configuredFeatures.iterator();
		while (iter.hasNext()) {
			IFeature element = (IFeature) iter.next();
			enabledVersionedIdentifier.add(element.getVersionedIdentifier());
		}

		// loop through the patches
		List result = new ArrayList();
		iter = efixes.keySet().iterator();
		while (iter.hasNext()) {
			boolean toEnable = false;
			IFeature efixFeature = (IFeature) iter.next();
			List patchedFeatures = (List) efixes.get(efixFeature);
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
				if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_RECONCILER)
					UpdateCore.debug("The Patch " + efixFeature + " does not patch any enabled features: it will be disabled"); //$NON-NLS-1$ //$NON-NLS-2$
			} else {
				if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_RECONCILER)
					UpdateCore.debug("The patch " + efixFeature + " will be enabled."); //$NON-NLS-1$ //$NON-NLS-2$
				result.add(efixFeature);
			}
		}
		return result;
	}

	/*
	 * returns the feature that are not patches
	 */
	private static ArrayList getNonEfixFeatures(ArrayList topFeatures) {
		Map efixFeatures = getPatchesAsFeature(topFeatures);
		Set keySet = efixFeatures.keySet();
		if (keySet == null || keySet.isEmpty())
			return topFeatures;

		Iterator iter = topFeatures.iterator();
		ArrayList result = new ArrayList();
		while (iter.hasNext()) {
			IFeature element = (IFeature) iter.next();
			if (!keySet.contains(element)) {
				result.add(element);
			}
		}
		return result;
	}

	/*
	 * only enable non-efix children recursively
	 */
	private static void expandEfixFeature(IFeature feature, ArrayList features, IConfiguredSite configuredSite) {

		// add feature
		if (!features.contains(feature)) {
			features.add(feature);
			// debug
			if (UpdateCore.DEBUG && UpdateCore.DEBUG_SHOW_RECONCILER) {
				UpdateCore.debug("Retaining configured feature " + feature.getVersionedIdentifier().toString()); //$NON-NLS-1$
			}
		}

		// add nested children to the list
		IIncludedFeatureReference[] children = null;
		try {
			children = feature.getIncludedFeatureReferences();
		} catch (CoreException e) {
			UpdateCore.warn("", e); //$NON-NLS-1$
			return;
		}

		for (int j = 0; j < children.length; j++) {
			IFeature child = null;
			try {
				child = children[j].getFeature(null);
			} catch (CoreException e) {
				if (!children[j].isOptional())
					UpdateCore.warn("", e); //$NON-NLS-1$
				// 25202 do not return right now, the peer children may be ok
			}
			if (child != null) {
				if (!UpdateCore.isPatch(child))
					expandEfixFeature(child, features, configuredSite);
			}
		}
	}

}
