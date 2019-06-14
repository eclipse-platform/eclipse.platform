/*******************************************************************************
 * Copyright (c) 2006, 2010 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.mapping;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.IModelProviderDescriptor;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreePathViewerSorter;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.ui.navigator.CommonViewerSorter;
import org.eclipse.ui.views.navigator.ResourceComparator;

public class TeamViewerSorter extends TreePathViewerSorter {

	private CommonViewerSorter sorter;
	private ResourceComparator resourceComparator;

	public TeamViewerSorter(CommonViewerSorter sorter) {
		this.sorter = sorter;
		this.resourceComparator = new ResourceComparator(ResourceComparator.NAME);
	}

	@Override
	public int category(Object element) {
		if (element instanceof ModelProvider) {
			return 2;
		}
		IResource resource = Utils.getResource(element);
		if (resource != null && resource.getType() == IResource.PROJECT) {
			return 1;
		}

		return super.category(element);
	}

	@Override
	public int compare(Viewer viewer, TreePath parentPath, Object e1, Object e2) {
		if (parentPath == null || parentPath.getSegmentCount() == 0) {
			// We need to handle the sorting at the top level
			int cat1 = category(e1);
			int cat2 = category(e2);

			if (cat1 != cat2)
				return cat1 - cat2;

			if (e1 instanceof ModelProvider && e2 instanceof ModelProvider) {
				ModelProvider mp1 = (ModelProvider) e1;
				ModelProvider mp2 = (ModelProvider) e2;
				if (isExtends(mp1, mp2.getDescriptor())) {
					return 1;
				}
				if (isExtends(mp2, mp1.getDescriptor())) {
					return -1;
				}
				return mp1.getDescriptor().getLabel().compareTo(mp2.getDescriptor().getLabel());
			}
			IResource r1 = Utils.getResource(e1);
			IResource r2 = Utils.getResource(e2);
			if (r1 != null && r2 != null) {
				return resourceComparator.compare(viewer, r1, r2);
			}
		}
		return sorter.compare(viewer, parentPath, e1, e2);
	}

	private boolean isExtends(ModelProvider mp1, IModelProviderDescriptor desc) {
		String[] extended = mp1.getDescriptor().getExtendedModels();
		for (String id : extended) {
			if (id.equals(desc.getId())) {
				return true;
			}
		}
		for (String id : extended) {
			IModelProviderDescriptor desc2 = ModelProvider.getModelProviderDescriptor(id);
			if (isExtends(mp1, desc2)) {
				return true;
			}
		}
		return false;
	}
}
