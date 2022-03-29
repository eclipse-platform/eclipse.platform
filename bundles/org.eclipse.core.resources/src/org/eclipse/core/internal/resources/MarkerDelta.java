/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import java.util.Map;
import java.util.stream.Collectors;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IPath;

/**
 * @see IMarkerDelta
 */
public class MarkerDelta implements IMarkerDelta, IMarkerSetElement {
	protected int kind;
	protected IResource resource;
	protected MarkerInfo info;

	/**
	 * Creates a new marker delta.
	 */
	public MarkerDelta(int kind, IResource resource, MarkerInfo info) {
		this.kind = kind;
		this.resource = resource;
		this.info = info;
	}

	@Override
	public Object getAttribute(String attributeName) {
		return info.getAttribute(attributeName);
	}

	@Override
	public int getAttribute(String attributeName, int defaultValue) {
		Object value = info.getAttribute(attributeName);
		if (value instanceof Integer)
			return ((Integer) value).intValue();
		return defaultValue;
	}

	@Override
	public String getAttribute(String attributeName, String defaultValue) {
		Object value = info.getAttribute(attributeName);
		if (value instanceof String)
			return (String) value;
		return defaultValue;
	}

	@Override
	public boolean getAttribute(String attributeName, boolean defaultValue) {
		Object value = info.getAttribute(attributeName);
		if (value instanceof Boolean)
			return ((Boolean) value).booleanValue();
		return defaultValue;
	}

	@Override
	public Map<String, Object> getAttributes() {
		return info.getAttributes();
	}

	@Override
	public Object[] getAttributes(String[] attributeNames) {
		return info.getAttributes(attributeNames);
	}

	@Override
	public long getId() {
		return info.getId();
	}

	@Override
	public int getKind() {
		return kind;
	}

	@Override
	public IMarker getMarker() {
		return new Marker(resource, getId());
	}

	@Override
	public IResource getResource() {
		return resource;
	}

	@Override
	public String getType() {
		return info.getType();
	}

	@Override
	public boolean isSubtypeOf(String superType) {
		return ((Workspace) getResource().getWorkspace()).getMarkerManager().isSubtype(getType(), superType);
	}

	/**
	 * Merge two Maps of (IPath-&gt;MarkerSet) representing changes. Use the old map
	 * to store the result so we don't have to build a new map to return.
	 */
	public static Map<IPath, MarkerSet> merge(Map<IPath, MarkerSet> oldChanges, Map<IPath, MarkerSet> newChanges) {
		if (oldChanges == null)
			//don't worry about copying since the new changes are no longer used
			return newChanges;
		if (newChanges == null)
			return oldChanges;
		for (Map.Entry<IPath, MarkerSet> newEntry : newChanges.entrySet()) {
			IPath key = newEntry.getKey();
			MarkerSet oldSet = oldChanges.get(key);
			MarkerSet newSet = newEntry.getValue();
			if (oldSet == null)
				oldChanges.put(key, newSet);
			else
				merge(oldSet, newSet.elements());
		}
		return oldChanges;
	}

	/**
	 * Merge two sets of marker changes.  Both sets must be on the same resource. Use the original set
	 * of changes to store the result so we don't have to build a completely different set to return.
	 *
	 * <pre>
	 * add + add = N/A
	 * add + remove = nothing (no delta)
	 * add + change = add
	 * remove + add = N/A
	 * remove + remove = N/A
	 * remove + change = N/A
	 * change + add = N/A
	 * change + change = change  (note: info held onto by the marker delta should be that of the oldest change, and not replaced when composed)
	 * change + remove = remove (note: info held onto by the marker delta should be that of the oldest change, and not replaced when changed to a remove)
	 * </pre>
	 */
	protected static MarkerSet merge(MarkerSet oldChanges, IMarkerSetElement[] newChanges) {
		if (oldChanges == null) {
			MarkerSet result = new MarkerSet(newChanges.length);
			for (IMarkerSetElement newChange : newChanges)
				result.add(newChange);
			return result;
		}
		if (newChanges == null)
			return oldChanges;

		for (IMarkerSetElement newChange : newChanges) {
			MarkerDelta newDelta = (MarkerDelta) newChange;
			MarkerDelta oldDelta = (MarkerDelta) oldChanges.get(newDelta.getId());
			if (oldDelta == null) {
				oldChanges.add(newDelta);
				continue;
			}
			switch (oldDelta.getKind()) {
				case IResourceDelta.ADDED :
					switch (newDelta.getKind()) {
						case IResourceDelta.ADDED :
							// add + add = N/A
							break;
						case IResourceDelta.REMOVED :
							// add + remove = nothing
							// Remove the original ADD delta.
							oldChanges.remove(oldDelta);
							break;
						case IResourceDelta.CHANGED :
							// add + change = add
							break;
					}
					break;
				case IResourceDelta.REMOVED :
					switch (newDelta.getKind()) {
						case IResourceDelta.ADDED :
							// remove + add = N/A
							break;
						case IResourceDelta.REMOVED :
							// remove + remove = N/A
							break;
						case IResourceDelta.CHANGED :
							// remove + change = N/A
							break;
					}
					break;
				case IResourceDelta.CHANGED :
					switch (newDelta.getKind()) {
						case IResourceDelta.ADDED :
							// change + add = N/A
							break;
						case IResourceDelta.REMOVED :
							// change + remove = remove
							// Change the delta kind.
							oldDelta.setKind(IResourceDelta.REMOVED);
							break;
						case IResourceDelta.CHANGED :
							// change + change = change
							break;
					}
					break;
			}
		}
		return oldChanges;
	}

	private void setKind(int kind) {
		this.kind = kind;
	}

	/** for debugging only **/
	@Override
	public String toString() {
		return getClass().getSimpleName() + "[resource=" + getResource() + ", attributes=" //$NON-NLS-1$ //$NON-NLS-2$
				+ getAttributes() == null ? null
						: getAttributes().entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()) //$NON-NLS-1$
						.collect(Collectors.joining(", ", "{", "}")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ "]"; //$NON-NLS-1$
	}
}
