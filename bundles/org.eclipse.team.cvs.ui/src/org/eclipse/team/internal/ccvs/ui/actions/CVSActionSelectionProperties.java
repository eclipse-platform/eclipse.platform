/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.actions;

import java.util.*;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ui.Utils;

/**
 * This class represents a selection for a set of CVS actions.
 */
public class CVSActionSelectionProperties {
	
	// Use a weak hash map so that the properties ae collected when the selection is no longer referenced
	private static Map<IStructuredSelection, CVSActionSelectionProperties> selectionProperties = new WeakHashMap<>();
	
	private Object[] selection;
	private Map<String, Object> properties = new HashMap<>();
	
	private static final String SELECTED_RESOURCES = "selectedResources"; //$NON-NLS-1$
	private static final String NONOVERLAPPING_SELECTED_RESOURCES = "nonoverlappingSelectedResources"; //$NON-NLS-1$
	private static final String CVS_RESOURCE_MAP = "cvsResourceMap"; //$NON-NLS-1$
	
	public static CVSActionSelectionProperties getProperties(IStructuredSelection selection) {
		if (selection == null) return null;
		CVSActionSelectionProperties props = selectionProperties.get(selection);
		if (props == null) {
			props = new CVSActionSelectionProperties(selection);
			selectionProperties.put(selection, props);
		}
		return props;
	}
	
	public CVSActionSelectionProperties(IStructuredSelection selection) {
		// Copy the selection so that the WeakHashMap will not think the seleciton used for the key is still in use
		this.selection = selection.toArray();
	}

	public void put(String key, Object value) {
		properties.put(key, value);
	}
	
	public Object get(String key) {
		return properties.get(key);
	}
	
	public IResource[] getAllSelectedResources() {
		IResource[] resources = (IResource[])get(SELECTED_RESOURCES);
		if (resources == null) {
			resources = getResources(selection);
			put(SELECTED_RESOURCES, resources);
		}
		return resources;
	}

	/*
	 * This method gets the resources from the given objects.
	 * It does so in a manner that is consistent with how the workbench does it.
	 * Tha is, it first uses IContributionResourceAdapter, then IResource,
	 * then ResourceMapping.
	 */
	private IResource[] getResources(Object[] objects) {
		return Utils.getContributedResources(objects);
	}
	
	public IResource[] getNonoverlappingSelectedResources() {
		IResource[] resources = (IResource[])get(NONOVERLAPPING_SELECTED_RESOURCES);
		if (resources == null) {
			resources = getNonOverlapping(getAllSelectedResources());
			put (NONOVERLAPPING_SELECTED_RESOURCES, resources);
		}
		return resources;
	}
		
	public ICVSResource getCVSResourceFor(IResource resource) {
		Map map = (Map)get(CVS_RESOURCE_MAP);
		if (map == null) {
			map = new HashMap();
			put(CVS_RESOURCE_MAP, map);
		}
		ICVSResource cvsResource = (ICVSResource)map.get(resource);
		if (cvsResource == null) {
			cvsResource = CVSWorkspaceRoot.getCVSResourceFor(resource);
			map.put(resource, cvsResource);
		}
		return cvsResource;
	}
	
	/**
	 * Method getNonOverlapping ensures that a resource is not covered more than once.
	 * @param resources
	 * @return IResource[]
	 */
	public static IResource[] getNonOverlapping(IResource[] resources) {
		if (resources == null || resources.length == 0 || resources.length == 1) {
			return resources;
		}
		// Sort the resources so the shortest paths are first
		List<IResource> sorted = new ArrayList<>();
		sorted.addAll(Arrays.asList(resources));
		Collections.sort(sorted, new Comparator<IResource>() {
			@Override
			public int compare(IResource resource0, IResource resource1) {
				return resource0.getFullPath().segmentCount() - resource1.getFullPath().segmentCount();
			}
			@Override
			public boolean equals(Object arg0) {
				return false;
			}
		});
		// Collect all non-overlapping resources
		List<IPath> coveredPaths = new ArrayList<>();
		for (Iterator iter = sorted.iterator(); iter.hasNext();) {
			IResource resource = (IResource) iter.next();
			IPath resourceFullPath = resource.getFullPath();
			boolean covered = false;
			for (Iterator it = coveredPaths.iterator(); it.hasNext();) {
				IPath path = (IPath) it.next();
				if(path.isPrefixOf(resourceFullPath)) {
					covered = true;
				}
			}
			if (covered) {
				// if the resource is covered by a parent, remove it
				iter.remove();
			} else {
				// if the resource is a non-covered folder, add it to the covered paths
				if (resource.getType() == IResource.FOLDER) {
					coveredPaths.add(resource.getFullPath());
				}
			}
		}
		return sorted.toArray(new IResource[sorted.size()]);
	}
	
}
