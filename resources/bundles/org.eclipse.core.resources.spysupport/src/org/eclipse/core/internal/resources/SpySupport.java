/*******************************************************************************
 * Copyright (c) 2002, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.internal.utils.Cache;
import org.eclipse.core.internal.watson.ElementTree;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.QualifiedName;

/**
 * Provides special internal access to the workspace resource implementation.
 * This class is to be used for testing purposes only.
 */
public class SpySupport {

	/*
	 * Class cannot be instantiated.
	 */
	private SpySupport() {
		// not allowed
	}
	/**
	 * Returns a copy of the session properties for the given resource. If the resource
	 * is not accessible or any problems occur accessing it, then <code>null</code> is
	 * returned.
	 *
	 * @param resource the resource to get the properties from
	 * @return the resource's session properties or <code>null</code>
	 */
	public static Map<QualifiedName, Object> getSessionProperties(IResource resource) {
		ResourceInfo info = ((Resource) resource).getResourceInfo(true, false);
		if (info == null)
			return null;
		return getSessionProperties(info);
	}
	@SuppressWarnings("unchecked")
	public static Map<QualifiedName, Object> getSessionProperties(ResourceInfo info) {
		return info.sessionProperties == null ? null : (Map<QualifiedName, Object>) info.sessionProperties.clone();
	}
	public static Map<QualifiedName, Object> getSyncInfo(ResourceInfo info) {
		return info.syncInfo;
	}
	public static ElementTree getOldestTree() {
		return ((Workspace) ResourcesPlugin.getWorkspace()).getSaveManager().lastSnap;
	}
	/**
	 * Returns the number of bytes of memory occupied by the given marker set
	 */
	public static IMarkerSetElement[] getElements(MarkerSet markerSet) {
		return markerSet.elements;
	}
	public static Object[] getElements(MarkerAttributeMap markerMap) {
		ArrayList<Object> legacyElements = new ArrayList<>();
		for (Entry<String, ?> e : markerMap.entrySet()) {
			legacyElements.add(e.getKey());
			legacyElements.add(e.getValue());
		}
		return legacyElements.toArray();
	}
	public static boolean isContentDescriptionCached(File file) {
		ResourceInfo info = file.getResourceInfo(false, false);
		Cache.Entry entry = ((Workspace) ResourcesPlugin.getWorkspace()).getContentDescriptionManager().getCache().getEntry(file.getFullPath(), false);
		return entry != null && info.getContentId() == entry.getTimestamp();
	}
}
