/*******************************************************************************
 * Copyright (c) 2003, 2016 IBM Corporation and others.
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
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 490755
 *******************************************************************************/
package org.eclipse.debug.internal.ui.sourcelookup;

import java.util.ArrayList;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.jface.viewers.ITreeContentProvider;
/**
 * Provides content for a tree viewer that shows only containers.
 *
 * @since 3.0
 */
public class BasicContainerContentProvider implements ITreeContentProvider {

	private boolean fShowClosedProjects = true;
	/**
	 * Creates a new ResourceContentProvider.
	 */
	public BasicContainerContentProvider() {
	}

	@Override
	public Object[] getChildren(Object element) {
		if (element instanceof IWorkspaceRoot) {
			// check if closed projects should be shown
			IProject[] allProjects = ((IWorkspaceRoot) element).getProjects();
			if (fShowClosedProjects) {
				return allProjects;
			}

			ArrayList<IProject> accessibleProjects = new ArrayList<>();
			for (IProject p : allProjects) {
				if (p.isOpen()) {
					accessibleProjects.add(p);
				}
			}
			return accessibleProjects.toArray();
		}
		return new Object[0];
	}

	@Override
	public Object[] getElements(Object element) {
		return getChildren(element);
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof IResource) {
			return ((IResource) element).getParent();
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return getChildren(element).length > 0;
	}
}
