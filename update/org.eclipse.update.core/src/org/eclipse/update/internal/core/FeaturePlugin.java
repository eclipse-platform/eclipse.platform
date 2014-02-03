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
import org.eclipse.update.core.*;
/**
 * An IPluginEntry - IFeature pair.
 * The IFeature is a featue or a patch wich delivered the plugin
 */
public class FeaturePlugin {
	private IPluginEntry pluginEntry;
	private IFeature feature;
	public FeaturePlugin(IPluginEntry entry, IFeature feature) {
		pluginEntry = entry;
		this.feature = feature;
	}
	public IPluginEntry getEntry() {
		return pluginEntry;
	}
	public IFeature getFeature() {
		return feature;
	}
	/**
	 * Plugins are equal if their IDs and versions are the same.
	 */
	public boolean equals(Object o) {
		if (o instanceof FeaturePlugin) {
			FeaturePlugin p = (FeaturePlugin) o;
			return getEntry().getVersionedIdentifier().equals(p.getEntry().getVersionedIdentifier());
		}
		return false;
	}
	public int hashCode() {
		return getEntry().getVersionedIdentifier().hashCode();
	}
	public String toString() {
		return pluginEntry + " in " + feature; //$NON-NLS-1$
	}
}
