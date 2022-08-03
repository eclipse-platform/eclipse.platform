/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.team.examples.pessimistic;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

public class ResourceSetContentProvider implements ITreeContentProvider {
	private static final Object[] EMPTY_ARRAY= new Object[0];
	private Map<IResource, Set<IResource>> fResourceTree;
	private IResource[] fRoots;

	public ResourceSetContentProvider(Set<IResource> resources) {
		fResourceTree = new HashMap<>(1);
		Set<IResource> roots= new HashSet<>(resources);
		for (Object element : resources) {
			IResource resource= (IResource)element;
			if(resource.getType() == IResource.ROOT) {
				continue; // root cannot be displayed
			}
			IResource parent= resource.getParent();
			if (roots.contains(parent)) {
				roots.remove(resource);
				Set<IResource> set= fResourceTree.get(parent);
				if (set == null) {
					set= new HashSet<>(1);
					fResourceTree.put(parent, set);
				}
				set.add(resource);
			}
		}
		fRoots= roots.toArray(new IResource[roots.size()]);
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		Set<IResource> set=  fResourceTree.get(parentElement);
		if (set != null) {
			return set.toArray();
		}
		return EMPTY_ARRAY;
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof IResource) {
			return ((IResource)element).getParent();
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return fResourceTree.get(element) != null;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return fRoots;
	}

	@Override
	public void dispose() {
		fResourceTree= null;
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
	}

}
