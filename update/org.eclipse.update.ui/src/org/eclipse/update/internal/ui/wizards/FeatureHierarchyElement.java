package org.eclipse.update.internal.ui.wizards;

import java.util.ArrayList;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.PluginVersionIdentifier;
import org.eclipse.update.core.IFeature;
import org.eclipse.update.core.IFeatureReference;
import org.eclipse.update.core.VersionedIdentifier;
import org.eclipse.update.internal.ui.UpdateUIPlugin;
import org.eclipse.update.internal.ui.preferences.MainPreferencePage;

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
	private ArrayList children;
	private IFeatureReference oldFeatureRef;
	private IFeatureReference newFeatureRef;
	private boolean checked;

	public FeatureHierarchyElement(
		IFeatureReference oldRef,
		IFeatureReference newRef) {
		oldFeatureRef = oldRef;
		newFeatureRef = newRef;
	}

	/*
	 * Return true if element can be checked, false otherwise.
	 */
	public boolean isEditable() {
		// cannot uncheck non-optional features
		if (newFeatureRef.isOptional() == false)
			return false;
		// cannot uncheck optional feature that
		// has already been installed
		if (oldFeatureRef != null)
			return false;
		return true;
	}
	/**
	 * Returns true if feature is included as optional.
	 */
	public boolean isOptional() {
		return newFeatureRef.isOptional();
	}
	/**
	 * Returns true if this optional feature is selected
	 * for installation. Non-optional features or non-editable
	 * features are always checked.
	 */
	public boolean isChecked() {
		return checked;
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
			IFeature feature = newFeatureRef.getFeature();
			return getFeatureLabel(feature);
		} catch (CoreException e) {
			if (newFeatureRef.getName() != null)
				return newFeatureRef.getName();
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
	private String getFeatureLabel(IFeature feature) {
		return feature.getLabel()
			+ " "
			+ feature.getVersionedIdentifier().getVersion().toString();
	}
	/**
	 * Computes children by linking matching features from the
	 * old feature's and new feature's hierarchy.
	 */
	public Object[] getChildren(boolean update) {
		computeChildren(update);
		return children.toArray();
	}
	/**
	 * Computes children of this node.
	 */
	public void computeChildren(boolean update) {
		if (children == null) {
			children = new ArrayList();
			try {
				IFeature oldFeature = null;
				IFeature newFeature = null;
				newFeature = newFeatureRef.getFeature();
				if (oldFeatureRef != null)
					oldFeature = oldFeatureRef.getFeature();
				computeElements(oldFeature, newFeature, update, children);
			} catch (CoreException e) {
			}
		}
	}
	/**
	 * Adds checked optional features to the provided set.
	 */
	public void addCheckedOptionalFeatures(boolean update, Set set) {
		if (isOptional() && isChecked())
			set.add(newFeatureRef);
		Object[] list = getChildren(update);
		for (int i = 0; i < list.length; i++) {
			FeatureHierarchyElement element = (FeatureHierarchyElement) list[i];
			element.addCheckedOptionalFeatures(update, set);
		}
	}

	/**
	 * Computes first-level children of the linked hierarchy
	 * for the provided old and new features (same ID, different version
	 * where new version is greater or equal the old version).
	 * Old feature may be null. 
	 */
	public static void computeElements(
		IFeature oldFeature,
		IFeature newFeature,
		boolean update,
		ArrayList list) {
		Object[] oldChildren = null;
		Object[] newChildren = getIncludedFeatures(newFeature);

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
				}
				FeatureHierarchyElement element =
					new FeatureHierarchyElement(oldRef, newRef);
				// If this is an update (old feature exists), 
				// only check the new optional feature if the old exists.
				// Otherwise, always check.
				if (newRef.isOptional() && update) {
					element.setChecked(oldRef != null);
					if (oldRef==null) {
						// Does not have an old reference,
						// but it may contain an older
						// feature that may still qualify
						// for update. For example,
						// an older version may have been
						// installed natively from the CD-ROM.
						if (hasOlderVersion(newRef)) {
							element.setChecked(true);
						}
					}
				}
				else
					element.setChecked(true);
				list.add(element);
				element.computeChildren(update);
			}
		} catch (CoreException e) {
		}
	}
	private static boolean hasOlderVersion(IFeatureReference newRef) {
		try {
			IFeature feature = newRef.getFeature();
			VersionedIdentifier vid = feature.getVersionedIdentifier();
			PluginVersionIdentifier version = vid.getVersion();
			String mode = MainPreferencePage.getUpdateVersionsMode();
			
			IFeature [] allInstalled = UpdateUIPlugin.getInstalledFeatures(feature, false);
			for (int i=0; i<allInstalled.length; i++) {
				IFeature candidate = allInstalled[i];
				PluginVersionIdentifier cversion = candidate.getVersionedIdentifier().getVersion();
				// Verify that the difference qualifies as
				// an update.
				if (mode.equals(MainPreferencePage.EQUIVALENT_VALUE)) {
					if (version.isEquivalentTo(cversion))
						return true;
				}
				else if (mode.equals(MainPreferencePage.COMPATIBLE_VALUE)) {
					if (version.isCompatibleWith(cversion))
						return true;
				}
			}
		}
		catch (CoreException e) {
		}
		return false;
	}
	/**
	 * Returns included feature references for the given reference.
	 */
	public static Object[] getIncludedFeatures(IFeatureReference ref) {
		try {
			IFeature feature = ref.getFeature();
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
}
