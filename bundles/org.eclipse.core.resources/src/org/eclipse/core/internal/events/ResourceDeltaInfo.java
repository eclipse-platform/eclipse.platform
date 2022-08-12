/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.core.internal.events;

import java.util.Map;
import java.util.stream.Collectors;
import org.eclipse.core.internal.resources.MarkerSet;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.runtime.IPath;

public class ResourceDeltaInfo {
	protected Workspace workspace;
	protected Map<IPath, MarkerSet> allMarkerDeltas;
	protected NodeIDMap nodeIDMap;
	protected ResourceComparator comparator;

	public ResourceDeltaInfo(Workspace workspace, Map<IPath, MarkerSet> markerDeltas, ResourceComparator comparator) {
		super();
		this.workspace = workspace;
		this.allMarkerDeltas = markerDeltas;
		this.comparator = comparator;
	}

	public ResourceComparator getComparator() {
		return comparator;
	}

	/**
	 * Table of all marker deltas, IPath -&gt; MarkerSet
	 */
	public Map<IPath, MarkerSet> getMarkerDeltas() {
		return allMarkerDeltas;
	}

	public NodeIDMap getNodeIDMap() {
		return nodeIDMap;
	}

	public Workspace getWorkspace() {
		return workspace;
	}

	public void setMarkerDeltas(Map<IPath, MarkerSet> value) {
		allMarkerDeltas = value;
	}

	public void setNodeIDMap(NodeIDMap map) {
		nodeIDMap = map;
	}

	/** for debugging only **/
	@Override
	public String toString() {
		return getClass().getSimpleName() + "[allMarkerDeltas=" //$NON-NLS-1$
				+ ((allMarkerDeltas == null) ? null
						: allMarkerDeltas.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()) //$NON-NLS-1$
								.collect(Collectors.joining(", ", "{", "}"))) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				+ "]"; //$NON-NLS-1$
	}
}
