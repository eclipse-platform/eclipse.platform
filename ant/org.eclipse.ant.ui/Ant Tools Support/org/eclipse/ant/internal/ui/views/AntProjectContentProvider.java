/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.views;


import java.util.ArrayList;
import java.util.List;

import org.eclipse.ant.internal.ui.views.elements.AntNode;
import org.eclipse.ant.internal.ui.views.elements.ProjectNode;
import org.eclipse.ant.internal.ui.views.elements.RootNode;
import org.eclipse.ant.internal.ui.views.elements.TargetNode;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * Content provider that provides a tree of ant projects.
 */
public class AntProjectContentProvider implements ITreeContentProvider {

	/**
	 * The root node of the project tree
	 */
	private RootNode rootNode;
	private boolean fIsFilteringInternalTargets= false;

	/**
	 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
	 */
	public Object[] getElements(Object inputElement) {
		return getChildren(inputElement);
	}

	/**
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	public void dispose() {
	}

	/**
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput instanceof RootNode) {
			rootNode= (RootNode) newInput;
		}
	}
	
	/**
	 * Adds the project to the tree. Has no effect if a project with the same buildfile name
	 * is already present in the tree.
	 * 
	 * @param project The project to add
	 */
	public void addProject(ProjectNode project) {
		ProjectNode[] projects= getRootNode().getProjects();
		for (int i = 0; i < projects.length; i++) {
			ProjectNode node = projects[i];
			if (node.getBuildFileName().equals(project.getBuildFileName())) {
				return;
			}
		}
		getRootNode().addProject(project);
	}
	
	/**
	 * Returns the root node of the tree or <code>null</code> if no root has
	 * been set as the input
	 * 
	 * @return RootNode the root node of the tree
	 */
	public RootNode getRootNode() {
		return rootNode;
	}
	
	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(Object)
	 */
	public Object[] getChildren(Object element) {
		if (element instanceof RootNode) {
			return ((RootNode) element).getProjects();
		} else if (element instanceof ProjectNode) { 
			if (!fIsFilteringInternalTargets) {
				return ((ProjectNode) element).getTargets();
			}
			TargetNode[] targets= ((ProjectNode) element).getTargets();
			List filteredTargets= new ArrayList();
			for (int i = 0; i < targets.length; i++) {
				TargetNode node = targets[i];
				if (!isInternal(node)) {
					filteredTargets.add(node);
				}
			}
			return filteredTargets.toArray();
		}
		return null;
	}

	/**
	 * Returns whether the given target is an internal target. Internal
	 * targets are targets which has no description. The default target
	 * is never considered internal.
	 * @param target the target to examine
	 * @return whether the given target is an internal target
	 */
	private boolean isInternal(TargetNode target) {
		return target != target.getProject().getDefaultTarget() && target.getDescription() == null;
	}
	
	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(Object)
	 */
	public Object getParent(Object element) {
		if (element instanceof AntNode) {
			return ((AntNode) element).getParent();
		}
		return null;
	}
	
	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(Object)
	 */
	public boolean hasChildren(Object element) {
		if (element instanceof TargetNode) {
			return false;
		}
		return true;
	}
	
	public boolean isFilterInternalTargets() {
		return fIsFilteringInternalTargets;
	}

	/**
	 * @param filter
	 */
	public void setFilterInternalTargets(boolean filter) {
		fIsFilteringInternalTargets= filter;
	}

}
