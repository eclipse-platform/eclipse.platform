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
package org.eclipse.update.internal.core;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.core.runtime.*;
import org.eclipse.update.core.*;
/**
 * Feature and corresponding patch features
 */
public class PatchedFeature {
	private IFeatureReference feature;
	private Collection patches = new HashSet();
	/**
	 *  
	 */
	public PatchedFeature(IFeatureReference feature) {
		super();
		this.feature = feature;
	}
	public void addPatch(IFeatureReference patch) {
		patches.add(patch);
	}
	/**
	 * @return Returns the feature.
	 */
	public IFeatureReference getFeature() {
		return feature;
	}
	/**
	 * @return Returns the patches.
	 */
	public IFeatureReference[] getPatches() {
		return (IFeatureReference[]) patches.toArray(new IFeatureReference[patches.size()]);
	}
	/**
	 * @return Returns the feature and the patches.
	 */
	public IFeatureReference[] getFeatureAndPatches() {
		IFeatureReference[] features = new IFeatureReference[patches.size() + 1];
		features[0] = feature;
		System.arraycopy(getPatches(), 0, features, 1, patches.size());
		return features;
	}
	/**
	 * Obtains all plugins from the feature and its patches. Each plugin will
	 * have unique ID.
	 * If there are multiple version of plugin with same ID among the feature
	 * and its patches, highest version plugins are chosen.
	 * 
	 * @return FeaturePlugin[]
	 */
	public FeaturePlugin[] getPlugins() {
		// Use a map of PatchedPluigns by plugin ID
		// to collect one version of each plugin
		Map plugins = new HashMap();
		IFeatureReference[] featureRefs = getFeatureAndPatches();
		// for each (feature or any patch)
		for (int i = 0; i < featureRefs.length; i++) {
			try {
				IFeature feature = featureRefs[i].getFeature(null);
				if (feature == null) {
					UpdateCore.warn("Null Feature", new Exception()); //$NON-NLS-1$
					continue;
				}
				// get plugin entries
				IPluginEntry[] entries = feature.getPluginEntries();
				for (int entr = 0; entr < entries.length; entr++) {
					String pluginId = entries[entr].getVersionedIdentifier().getIdentifier();
					PluginVersionIdentifier pluginVersion = entries[entr].getVersionedIdentifier().getVersion();
					// check if map contains >= version of same plugin
					FeaturePlugin existingPlugin = (FeaturePlugin) plugins.get(pluginId);
					if (existingPlugin != null && existingPlugin.getEntry().getVersionedIdentifier().getVersion().isGreaterOrEqualTo(pluginVersion)) {
						// same or newer plugin already collected
						continue;
					} else {
						plugins.put(pluginId, new FeaturePlugin(entries[entr], feature));
					}
				}
			} catch (CoreException e) {
				UpdateCore.warn(null, e);
			}
		}
		return (FeaturePlugin[]) plugins.values().toArray(new FeaturePlugin[plugins.size()]);
	}
	public String toString() {
		StringBuffer str = new StringBuffer(feature.toString());
		IFeatureReference[] patches = getFeatureAndPatches();
		for (int i = 0; i < patches.length; i++) {
			str.append(" +patch=" + patches[i].toString() + " "); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return str.toString();
	}
}
