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
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 473427
 *******************************************************************************/
package org.eclipse.core.internal.events;

import java.util.*;
import org.eclipse.core.internal.resources.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IPath;

public class ResourceChangeEvent extends EventObject implements IResourceChangeEvent {
	private static final IMarkerDelta[] NO_MARKER_DELTAS = new IMarkerDelta[0];
	private static final long serialVersionUID = 1L;
	IResourceDelta delta;
	IResource resource;

	/**
	 * The build trigger for this event, or 0 if not applicable.
	 */
	private int trigger = 0;
	int type;

	protected ResourceChangeEvent(Object source, int type, IResource resource) {
		super(source);
		this.resource = resource;
		this.type = type;
	}

	public ResourceChangeEvent(Object source, int type, int buildKind, IResourceDelta delta) {
		super(source);
		this.delta = delta;
		this.trigger = buildKind;
		this.type = type;
	}

	/**
	 * @see IResourceChangeEvent#findMarkerDeltas(String, boolean)
	 */
	@Override
	public IMarkerDelta[] findMarkerDeltas(String findType, boolean includeSubtypes) {
		if (delta == null)
			return NO_MARKER_DELTAS;
		ResourceDeltaInfo info = ((ResourceDelta) delta).getDeltaInfo();
		if (info == null)
			return NO_MARKER_DELTAS;
		//Map of IPath -> MarkerSet containing MarkerDelta objects
		Map<IPath, MarkerSet> markerDeltas = info.getMarkerDeltas();
		if (markerDeltas == null || markerDeltas.isEmpty())
			return NO_MARKER_DELTAS;
		ArrayList<IMarkerDelta> matching = new ArrayList<>();
		Iterator<MarkerSet> deltaSets = markerDeltas.values().iterator();
		while (deltaSets.hasNext()) {
			MarkerSet deltas = deltaSets.next();
			IMarkerSetElement[] elements = deltas.elements();
			for (IMarkerSetElement element : elements) {
				MarkerDelta markerDelta = (MarkerDelta) element;
				//our inclusion test depends on whether we are considering subtypes
				if (findType == null || (includeSubtypes ? markerDelta.isSubtypeOf(findType) : markerDelta.getType().equals(findType)))
					matching.add(markerDelta);
			}
		}
		return matching.toArray(new IMarkerDelta[matching.size()]);
	}

	/**
	 * @see IResourceChangeEvent#getBuildKind()
	 */
	@Override
	public int getBuildKind() {
		return trigger;
	}

	/**
	 * @see IResourceChangeEvent#getDelta()
	 */
	@Override
	public IResourceDelta getDelta() {
		return delta;
	}

	/**
	 * @see IResourceChangeEvent#getResource()
	 */
	@Override
	public IResource getResource() {
		return resource;
	}

	/**
	 * @see IResourceChangeEvent#getType()
	 */
	@Override
	public int getType() {
		return type;
	}

	public void setDelta(IResourceDelta value) {
		delta = value;
	}

	public String toDebugString() {
		StringBuilder output = new StringBuilder();
		output.append("\nType: ");//$NON-NLS-1$
		switch (type) {
			case POST_CHANGE :
				output.append("POST_CHANGE"); //$NON-NLS-1$
				break;
			case PRE_CLOSE :
				output.append("PRE_CLOSE"); //$NON-NLS-1$
				break;
			case PRE_DELETE :
				output.append("PRE_DELETE"); //$NON-NLS-1$
				break;
			case PRE_BUILD :
				output.append("PRE_BUILD"); //$NON-NLS-1$
				break;
			case POST_BUILD :
				output.append("POST_BUILD"); //$NON-NLS-1$
				break;
			case PRE_REFRESH :
				output.append("PRE_REFRESH"); //$NON-NLS-1$
				break;
			default :
				output.append("?"); //$NON-NLS-1$
				break;
		}
		output.append("\nBuild kind: "); //$NON-NLS-1$
		switch (trigger) {
			case IncrementalProjectBuilder.FULL_BUILD :
				output.append("FULL_BUILD"); //$NON-NLS-1$
				break;
			case IncrementalProjectBuilder.AUTO_BUILD :
			case IncrementalProjectBuilder.INCREMENTAL_BUILD :
				output.append("INCREMENTAL_BUILD"); //$NON-NLS-1$
				break;
			case IncrementalProjectBuilder.CLEAN_BUILD :
				output.append("CLEAN_BUILD"); //$NON-NLS-1$
				break;
			default :
				output.append(trigger);
				break;
		}
		output.append("\nResource: " + (resource == null ? "null" : resource)); //$NON-NLS-1$ //$NON-NLS-2$
		output.append("\nDelta:" + (delta == null ? " null" : ((ResourceDelta) delta).toDeepDebugString())); //$NON-NLS-1$ //$NON-NLS-2$
		return output.toString();
	}
}
