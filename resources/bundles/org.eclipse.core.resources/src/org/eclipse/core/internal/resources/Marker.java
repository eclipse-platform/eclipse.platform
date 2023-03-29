/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
 *     Mickael Istria (Red Hat Inc.) - Bug 488937
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import java.text.DateFormat;
import java.util.*;
import java.util.Map.Entry;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.osgi.util.NLS;

/**
 * An abstract marker implementation.
 * Subclasses must implement the <code>clone</code> method, and
 * are free to declare additional field and method members.
 * <p>
 * Note: Marker objects do not store whether they are "standalone"
 * vs. "attached" to the workspace. This information is maintained
 * by the workspace.
 * </p>
 *
 * @see IMarker
 */
public class Marker extends PlatformObject implements IMarker {

	/** Marker identifier. */
	protected final long id;

	/** Resource with which this marker is associated. */
	protected final IResource resource;

	/**
	 * Constructs a new marker object.
	 */
	Marker(IResource resource, long id) {
		Assert.isLegal(resource != null);
		this.resource = resource;
		this.id = id;
	}

	/**
	 * Checks the given marker info to ensure that it is not null.
	 * Throws an exception if it is.
	 */
	private void checkInfo(MarkerInfo info) throws CoreException {
		if (info == null) {
			String message = NLS.bind(Messages.resources_markerNotFound, Long.toString(id));
			throw new ResourceException(new ResourceStatus(IResourceStatus.MARKER_NOT_FOUND, resource.getFullPath(), message, new IllegalStateException()));
		}
	}

	/**
	 * @see IMarker#delete()
	 */
	@Override
	public void delete() throws CoreException {
		final ISchedulingRule rule = getWorkspace().getRuleFactory().markerRule(resource);
		try {
			getWorkspace().prepareOperation(rule, null);
			getWorkspace().beginOperation(true);
			getWorkspace().getMarkerManager().removeMarker(getResource(), getId());
		} finally {
			getWorkspace().endOperation(rule, false);
		}
	}

	/**
	 * @see IMarker#equals(Object)
	 */
	@Override
	public boolean equals(Object object) {
		if (!(object instanceof IMarker))
			return false;
		IMarker other = (IMarker) object;
		return (id == other.getId() && resource.equals(other.getResource()));
	}

	/**
	 * @see IMarker#exists()
	 */
	@Override
	public boolean exists() {
		return getInfo() != null;
	}

	/**
	 * @see IMarker#getAttribute(String)
	 */
	@Override
	public Object getAttribute(String attributeName) throws CoreException {
		Assert.isNotNull(attributeName);
		MarkerInfo info = getInfo();
		checkInfo(info);
		return info.getAttribute(attributeName);
	}

	/**
	 * @see IMarker#getAttribute(String, int)
	 */
	@Override
	public int getAttribute(String attributeName, int defaultValue) {
		Assert.isNotNull(attributeName);
		MarkerInfo info = getInfo();
		if (info == null)
			return defaultValue;
		Object value = info.getAttribute(attributeName);
		if (value instanceof Integer)
			return ((Integer) value).intValue();
		return defaultValue;
	}

	/**
	 * @see IMarker#getAttribute(String, String)
	 */
	@Override
	public String getAttribute(String attributeName, String defaultValue) {
		Assert.isNotNull(attributeName);
		MarkerInfo info = getInfo();
		if (info == null)
			return defaultValue;
		Object value = info.getAttribute(attributeName);
		if (value instanceof String)
			return (String) value;
		return defaultValue;
	}

	/**
	 * @see IMarker#getAttribute(String, boolean)
	 */
	@Override
	public boolean getAttribute(String attributeName, boolean defaultValue) {
		Assert.isNotNull(attributeName);
		MarkerInfo info = getInfo();
		if (info == null)
			return defaultValue;
		Object value = info.getAttribute(attributeName);
		if (value instanceof Boolean)
			return ((Boolean) value).booleanValue();
		return defaultValue;
	}

	/**
	 * @see IMarker#getAttributes()
	 */
	@Override
	public Map<String, Object> getAttributes() throws CoreException {
		MarkerInfo info = getInfo();
		checkInfo(info);
		return info.getAttributes();
	}

	/**
	 * @see IMarker#getAttributes(String[])
	 */
	@Override
	public Object[] getAttributes(String[] attributeNames) throws CoreException {
		Assert.isNotNull(attributeNames);
		MarkerInfo info = getInfo();
		checkInfo(info);
		return info.getAttributes(attributeNames);
	}

	/**
	 * @see IMarker#getCreationTime()
	 */
	@Override
	public long getCreationTime() throws CoreException {
		MarkerInfo info = getInfo();
		checkInfo(info);
		return info.getCreationTime();
	}

	/**
	 * @see IMarker#getId()
	 */
	@Override
	public long getId() {
		return id;
	}

	protected MarkerInfo getInfo() {
		return getWorkspace().getMarkerManager().findMarkerInfo(resource, id);
	}

	/**
	 * @see IMarker#getResource()
	 */
	@Override
	public IResource getResource() {
		return resource;
	}

	/**
	 * @see IMarker#getType()
	 */
	@Override
	public String getType() throws CoreException {
		MarkerInfo info = getInfo();
		checkInfo(info);
		return info.getType();
	}

	/**
	 * Returns the workspace which manages this marker.  Returns
	 * <code>null</code> if this resource does not have an associated
	 * resource.
	 */
	private Workspace getWorkspace() {
		return resource == null ? null : (Workspace) resource.getWorkspace();
	}

	@Override
	public int hashCode() {
		return (int) id + resource.hashCode();
	}

	/**
	 * @see IMarker#isSubtypeOf(String)
	 */
	@Override
	public boolean isSubtypeOf(String type) throws CoreException {
		return getWorkspace().getMarkerManager().isSubtype(getType(), type);
	}

	/**
	 * @see IMarker#setAttribute(String, int)
	 */
	@Override
	public void setAttribute(String attributeName, int value) throws CoreException {
		setAttribute(attributeName, Integer.valueOf(value));
	}

	/**
	 * @see IMarker#setAttribute(String, Object)
	 */
	@Override
	public void setAttribute(String attributeName, Object value) throws CoreException {
		Assert.isNotNull(attributeName);
		Workspace workspace = getWorkspace();
		MarkerManager manager = workspace.getMarkerManager();
		try {
			workspace.prepareOperation(null, null);
			workspace.beginOperation(true);
			MarkerInfo markerInfo = getInfo();
			checkInfo(markerInfo);

			//only need to generate delta info if none already
			boolean needDelta = !manager.hasDelta(resource.getFullPath(), id);
			MarkerInfo oldInfo = needDelta ? (MarkerInfo) markerInfo.clone() : null;
			boolean validate = manager.isPersistentType(markerInfo.getType());
			markerInfo.setAttribute(attributeName, value, validate);
			if (manager.isPersistent(markerInfo))
				((Resource) resource).getResourceInfo(false, true).set(ICoreConstants.M_MARKERS_SNAP_DIRTY);
			if (needDelta) {
				MarkerDelta delta = new MarkerDelta(IResourceDelta.CHANGED, resource, oldInfo);
				manager.changedMarkers(resource, new MarkerDelta[] {delta});
			}
		} finally {
			workspace.endOperation(null, false);
		}
	}

	/**
	 * @see IMarker#setAttribute(String, boolean)
	 */
	@Override
	public void setAttribute(String attributeName, boolean value) throws CoreException {
		setAttribute(attributeName, value ? Boolean.TRUE : Boolean.FALSE);
	}

	/**
	 * adds all Entries
	 * @see IMarker#setAttributes(String[], Object[])
	 */
	@Override
	public void setAttributes(String[] attributeNames, Object[] values) throws CoreException {
		Assert.isNotNull(attributeNames);
		Assert.isNotNull(values);
		Workspace workspace = getWorkspace();
		MarkerManager manager = workspace.getMarkerManager();
		try {
			workspace.prepareOperation(null, null);
			workspace.beginOperation(true);
			MarkerInfo markerInfo = getInfo();
			checkInfo(markerInfo);

			//only need to generate delta info if none already
			boolean needDelta = !manager.hasDelta(resource.getFullPath(), id);
			MarkerInfo oldInfo = needDelta ? (MarkerInfo) markerInfo.clone() : null;
			boolean validate = manager.isPersistentType(markerInfo.getType());
			markerInfo.addAttributes(attributeNames, values, validate);
			if (manager.isPersistent(markerInfo))
				((Resource) resource).getResourceInfo(false, true).set(ICoreConstants.M_MARKERS_SNAP_DIRTY);
			if (needDelta) {
				MarkerDelta delta = new MarkerDelta(IResourceDelta.CHANGED, resource, oldInfo);
				manager.changedMarkers(resource, new MarkerDelta[] {delta});
			}
		} finally {
			workspace.endOperation(null, false);
		}
	}

	/**
	 * clears current map and puts entries
	 * @see IMarker#setAttributes(Map)
	 */
	@Override
	public void setAttributes(Map<String, ? extends Object> values) throws CoreException {
		Workspace workspace = getWorkspace();
		MarkerManager manager = workspace.getMarkerManager();
		try {
			workspace.prepareOperation(null, null);
			workspace.beginOperation(true);
			MarkerInfo markerInfo = getInfo();
			checkInfo(markerInfo);

			//only need to generate delta info if none already
			boolean needDelta = !manager.hasDelta(resource.getFullPath(), id);
			MarkerInfo oldInfo = needDelta ? (MarkerInfo) markerInfo.clone() : null;
			boolean validate = manager.isPersistentType(markerInfo.getType());
			markerInfo.setAttributes(values, validate);
			if (manager.isPersistent(markerInfo))
				((Resource) resource).getResourceInfo(false, true).set(ICoreConstants.M_MARKERS_SNAP_DIRTY);
			if (needDelta) {
				MarkerDelta delta = new MarkerDelta(IResourceDelta.CHANGED, resource, oldInfo);
				manager.changedMarkers(resource, new MarkerDelta[] {delta});
			}
		} finally {
			workspace.endOperation(null, false);
		}
	}

	/** For debugging only */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("Marker ["); //$NON-NLS-1$
		sb.append("on: ").append(resource.getFullPath()); //$NON-NLS-1$
		MarkerInfo info = getInfo();
		if (info == null) {
			sb.append(", not found]"); //$NON-NLS-1$
			return sb.toString();
		}
		sb.append(", id: ").append(info.getId()); //$NON-NLS-1$
		sb.append(", type: ").append(info.getType()); //$NON-NLS-1$
		Map<String, Object> attributes = info.getAttributes();
		if (attributes != null) {
			TreeMap<String, Object> tm = new TreeMap<>(attributes);
			Set<Entry<String, Object>> set = tm.entrySet();
			if (!set.isEmpty()) {
				sb.append(", attributes: ["); //$NON-NLS-1$
				for (Entry<String, Object> entry : set) {
					sb.append(entry.getKey()).append(": ").append(entry.getValue()).append(", "); //$NON-NLS-1$ //$NON-NLS-2$
				}
				sb.setLength(sb.length() - 2);
				sb.append(']');
			}
		}
		sb.append(", created: ").append(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(info.getCreationTime()))); //$NON-NLS-1$
		sb.append(']');
		return sb.toString();
	}
}
