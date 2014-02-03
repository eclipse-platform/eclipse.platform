/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.operations;

import java.util.ArrayList;
import java.util.Set;

import org.eclipse.core.runtime.*;
import org.eclipse.update.configuration.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.core.*;

/**
 * This class is used to construct a joint feature hiearchy.
 * Old feature reference represents feature that is
 * found on in the current configuration. New feature
 * reference is found in the feature that is an install/update
 * candidate. The element is used to join nodes of the
 * hiearchy formed by including features so that
 * each node in the hiearchy contains references to the
 * old and the new feature. Old and new features have
 * the same IDs but different versions, except in 
 * the case of optional features, where the tree may
 * be constructed to bring in an optional feature
 * that was not installed initially. In that case,
 * some nodes may have old an new references with the
 * same ID and version. 
 * <p>
 * Old feature reference may be null. That means
 * that the older feature with the same ID but lower
 * version was not found in the current configuration.
 */
public class FeatureHierarchyElement {

	private Object root;
	private ArrayList children;
	private IFeatureReference oldFeatureRef;
	private IFeatureReference newFeatureRef;
	private boolean checked;
	private boolean optionalChildren;
	private boolean nativeUpgrade = false;

	public FeatureHierarchyElement(
		IFeatureReference oldRef,
		IFeatureReference newRef) {
		oldFeatureRef = oldRef;
		newFeatureRef = newRef;
	}

	public void setRoot(Object root) {
		this.root = root;
	}

	public Object getRoot() {
		return root;
	}

	/*
	 * Return true if element can be checked, false otherwise.
	 */
	public boolean isEditable() {
		// cannot uncheck non-optional features
		if (isOptional() == false)
			return false;
		// cannot uncheck optional feature that
		// has already been installed
		if (oldFeatureRef != null)
			return false;
		return true;
	}

	/**
	 * A hirearchy node represents a 'false update' if
	 * both old and new references exist and both
	 * point to the feature with the same ID and version.
	 * These nodes will not any bytes to be downloaded - 
	 * they simply exist to allow the hirarchy to
	 * reach the optional children that are missing
	 * and will be installed.
	 */

	public boolean isFalseUpdate() {
		if (oldFeatureRef != null && newFeatureRef != null) {
			try {
				return oldFeatureRef.getVersionedIdentifier().equals(
					newFeatureRef.getVersionedIdentifier());
			} catch (CoreException e) {
			}
		}
		return false;
	}
	/**
	 * Returns true if feature is included as optional.
	 */
	public boolean isOptional() {
		return newFeatureRef instanceof IIncludedFeatureReference
			&& ((IIncludedFeatureReference) newFeatureRef).isOptional();
	}
	/**
	 * Returns true if this optional feature is selected
	 * for installation. Non-optional features or non-editable
	 * features are always checked.
	 */
	public boolean isChecked() {
		return checked;
	}

	void setNativeUpgrade(boolean nativeUpgrade) {
		this.nativeUpgrade = nativeUpgrade;
	}

	/**
	 * Returns true if this optional feature should
	 * be enabled when installed. By default, all
	 * features in the hiearchy should be enabled.
	 * The exception is for optional features that
	 * are updated to a new version in case where
	 * the older version of the optional feature
	 * is disabled in the given configuration. 
	 * In this case, the feature is
	 * updated and disabled in order to maintain
	 * its state.
	 */
	public boolean isEnabled(IInstallConfiguration config) {
		if (nativeUpgrade)
			return true;
		if (isOptional() && oldFeatureRef != null) {
			try {
				IFeature oldFeature = oldFeatureRef.getFeature(null);
				IConfiguredSite csite =
					UpdateUtils.getConfigSite(oldFeature, config);
				return csite.isConfigured(oldFeature);
			} catch (CoreException e) {
			}
		}
		return true;
	}

	public IFeature getFeature() {
		try {
			IFeature feature = newFeatureRef.getFeature(null);
			return feature;
		} catch (CoreException e) {
			return null;
		}
	}

	/**
	 * Selects an editable feature for installation.
	 */
	public void setChecked(boolean checked) {
		this.checked = checked;
	}
	/**
	 * Returns label for UI presentation.
	 */
	public String getLabel() {
		try {
			return getFeatureLabel(newFeatureRef);
		} catch (CoreException e) {
			if (newFeatureRef instanceof IIncludedFeatureReference) {
				String iname =
					((IIncludedFeatureReference) newFeatureRef).getName();
				if (iname != null)
					return iname;
			}
			try {
				VersionedIdentifier vid =
					newFeatureRef.getVersionedIdentifier();
				return vid.toString();
			} catch (CoreException e2) {
			}
		}
		return null;
	}
	/**
	 * Computes label from the feature.
	 */
	private String getFeatureLabel(IFeatureReference featureRef)
		throws CoreException {
		IFeature feature = featureRef.getFeature(null);
		return feature.getLabel()
			+ " " //$NON-NLS-1$
			+ feature.getVersionedIdentifier().getVersion().toString();
	}
	/**
	 * Computes children by linking matching features from the
	 * old feature's and new feature's hierarchy.
	 */
	public FeatureHierarchyElement[] getChildren(
		boolean update,
		boolean patch,
		IInstallConfiguration config) {
		computeChildren(update, patch, config);
		FeatureHierarchyElement[] array =
			new FeatureHierarchyElement[children.size()];
		children.toArray(array);
		return array;
	}

	public FeatureHierarchyElement[] getChildren() {
		if (children != null) {
			FeatureHierarchyElement[] array =
				new FeatureHierarchyElement[children.size()];
			children.toArray(array);
			return array;
		}

		return new FeatureHierarchyElement[0];
	}
	/**
	 * Computes children of this node.
	 */
	public void computeChildren(
		boolean update,
		boolean patch,
		IInstallConfiguration config) {
		if (children == null) {
			children = new ArrayList();
			try {
				IFeature oldFeature = null;
				IFeature newFeature = null;
				newFeature = newFeatureRef.getFeature(null);
				if (oldFeatureRef != null)
					oldFeature = oldFeatureRef.getFeature(null);
				optionalChildren =
					computeElements(
						oldFeature,
						newFeature,
						update,
						patch,
						config,
						children);
				for (int i = 0; i < children.size(); i++) {
					FeatureHierarchyElement element =
						(FeatureHierarchyElement) children.get(i);
					element.setRoot(getRoot());
				}
			} catch (CoreException e) {
			}
		}
	}
	/**
	 * 
	 */
	public boolean hasOptionalChildren() {
		return optionalChildren;
	}
	/**
	 * Adds checked optional features to the provided set.
	 */
	public void addCheckedOptionalFeatures(
		boolean update,
		boolean patch,
		IInstallConfiguration config,
		Set set) {
		if (isOptional() && isChecked()) {
			// Do not add checked optional features
			// if this is an update case but
			// the node is not a 'true' update
			// (old and new feature are the equal)
			if (!update || !isFalseUpdate())
				set.add(newFeatureRef);
		}
		FeatureHierarchyElement[] elements = getChildren(update, patch, config);
		for (int i = 0; i < elements.length; i++) {
			elements[i].addCheckedOptionalFeatures(update, patch, config, set);
		}
	}

	/**
	 * Computes first-level children of the linked hierarchy
	 * for the provided old and new features (same ID, different version
	 * where new version is greater or equal the old version).
	 * Old feature may be null. 
	 */
	public static boolean computeElements(
		IFeature oldFeature,
		IFeature newFeature,
		boolean update,
		boolean patch,
		IInstallConfiguration config,
		ArrayList list) {
		Object[] oldChildren = null;
		Object[] newChildren = getIncludedFeatures(newFeature);
		boolean optionalChildren = false;

		try {
			if (oldFeature != null) {
				oldChildren = getIncludedFeatures(oldFeature);
			}
			for (int i = 0; i < newChildren.length; i++) {
				IFeatureReference oldRef = null;
				IFeatureReference newRef = (IFeatureReference) newChildren[i];
				if (oldChildren != null) {
					String newId =
						newRef.getVersionedIdentifier().getIdentifier();

					for (int j = 0; j < oldChildren.length; j++) {
						IFeatureReference cref =
							(IFeatureReference) oldChildren[j];
						try {
							if (cref
								.getVersionedIdentifier()
								.getIdentifier()
								.equals(newId)) {
								oldRef = cref;
								break;
							}
						} catch (CoreException ex) {
						}
					}
				} else if (patch) {
					// 30849 - find the old reference in the
					// configuration.
					if (!UpdateUtils.isPatch(newFeature)) {
						oldRef = findPatchedReference(newRef, config);
					}
				}
				// test if the old optional feature exists
				if (oldRef != null
					&& ((oldRef instanceof IIncludedFeatureReference
						&& ((IIncludedFeatureReference) oldRef).isOptional())
						|| patch)) {
					try {
						IFeature f = oldRef.getFeature(null);
						if (f == null)
							oldRef = null;
					} catch (CoreException e) {
						// missing
						oldRef = null;
					}
				}
				FeatureHierarchyElement element =
					new FeatureHierarchyElement(oldRef, newRef);
				// If this is an update (old feature exists), 
				// only check the new optional feature if the old exists.
				// Otherwise, always check.
				if (element.isOptional() && (update || patch)) {
					element.setChecked(oldRef != null);
					if (oldRef == null) {
						// Does not have an old reference,
						// but it may contain an older
						// feature that may still qualify
						// for update. For example,
						// an older version may have been
						// installed natively from the CD-ROM.
						if (hasOlderVersion(newRef)) {
							element.setNativeUpgrade(true);
							element.setChecked(true);
						}
					}
				} else
					element.setChecked(true);
				list.add(element);
				element.computeChildren(update, patch, config);
				if (element.isOptional() || element.hasOptionalChildren())
					optionalChildren = true;
			}
		} catch (CoreException e) {
		}
		return optionalChildren;
	}
	public static boolean hasOlderVersion(IFeatureReference newRef) {
		try {
			VersionedIdentifier vid = newRef.getVersionedIdentifier();
			PluginVersionIdentifier version = vid.getVersion();
			String mode = getUpdateVersionsMode();

			IFeature[] allInstalled =
				UpdateUtils.getInstalledFeatures(vid, false);
			for (int i = 0; i < allInstalled.length; i++) {
				IFeature candidate = allInstalled[i];
				PluginVersionIdentifier cversion =
					candidate.getVersionedIdentifier().getVersion();
				// Verify that the difference qualifies as
				// an update.
				if (mode.equals(UpdateCore.EQUIVALENT_VALUE)) {
					if (version.isEquivalentTo(cversion))
						return true;
				} else if (mode.equals(UpdateCore.COMPATIBLE_VALUE)) {
					if (version.isCompatibleWith(cversion))
						return true;
				}
			}
		} catch (CoreException e) {
		}
		return false;
	}

	private static IFeatureReference findPatchedReference(
		IFeatureReference newRef,
		IInstallConfiguration config)
		throws CoreException {
		VersionedIdentifier vid = newRef.getVersionedIdentifier();
		IConfiguredSite[] csites = config.getConfiguredSites();
		for (int i = 0; i < csites.length; i++) {
			IConfiguredSite csite = csites[i];
			IFeatureReference[] refs = csite.getConfiguredFeatures();
			for (int j = 0; j < refs.length; j++) {
				IFeatureReference ref = refs[j];
				VersionedIdentifier refVid = ref.getVersionedIdentifier();
				if (vid.getIdentifier().equals(refVid.getIdentifier()))
					return ref;
			}
		}
		return null;
	}

	/**
	 * Returns included feature references for the given reference.
	 */
	public static Object[] getIncludedFeatures(IFeatureReference ref) {
		try {
			IFeature feature = ref.getFeature(null);
			return getIncludedFeatures(feature);
		} catch (CoreException e) {
		}
		return new Object[0];
	}

	/**
	 * Returns included feature references for the given feature.
	 */

	public static Object[] getIncludedFeatures(IFeature feature) {
		try {
			return feature.getIncludedFeatureReferences();
		} catch (CoreException e) {
		}
		return new Object[0];
	}

	private static String getUpdateVersionsMode() {
		Preferences store = UpdateCore.getPlugin().getPluginPreferences();
		return store.getString(UpdateCore.P_UPDATE_VERSIONS);
	}
}
