/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.model;

import java.util.HashMap;

import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFolderTree;
import org.eclipse.team.internal.ccvs.core.resources.RemoteResource;
import org.eclipse.team.internal.ccvs.ui.repo.RepositoryRoot;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.progress.DeferredTreeContentManager;

/**
 * Extension to the generic workbench content provider mechanism
 * to lazily determine whether an element has children.  That is,
 * children for an element aren't fetched until the user clicks
 * on the tree expansion box.
 */
public class RemoteContentProvider extends WorkbenchContentProvider {

	IWorkingSet workingSet;
	DeferredTreeContentManager manager;

	HashMap<ICVSRemoteFolder, RemoteFolderTree> cachedTrees;
	
	public RemoteContentProvider(){
		cachedTrees = new HashMap<>();
	}
	
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (viewer instanceof AbstractTreeViewer) {
			manager = new DeferredTreeContentManager((AbstractTreeViewer) viewer);
		}
		super.inputChanged(viewer, oldInput, newInput);
	}

	@Override
	public boolean hasChildren(Object element) {
		// the + box will always appear, but then disappear
		// if not needed after you first click on it.
		if (element instanceof ICVSRemoteResource) {
			if (element instanceof ICVSRemoteFolder) {
				return ((ICVSRemoteFolder) element).isExpandable();
			}
			return ((ICVSRemoteResource) element).isContainer();
		} else if (element instanceof CVSResourceElement) {
			ICVSResource r = ((CVSResourceElement) element).getCVSResource();
			if (r instanceof RemoteResource) {
				return r.isFolder();
			}
		} else if (element instanceof VersionCategory) {
			return true;
		} else if (element instanceof BranchCategory) {
			return true;
		} else if (element instanceof CVSTagElement) {
			return true;
		} else if (element instanceof RemoteModule) {
			return true;
		}
		if (manager != null) {
			if (manager.isDeferredAdapter(element))
				return manager.mayHaveChildren(element);
		}

		return super.hasChildren(element);
	}

	/**
	 * Sets the workingSet.
	 * @param workingSet The workingSet to set
	 */
	public void setWorkingSet(IWorkingSet workingSet) {
		this.workingSet = workingSet;
	}

	/**
	 * Returns the workingSet.
	 * @return IWorkingSet
	 */
	public IWorkingSet getWorkingSet() {
		return workingSet;
	}

	@Override
	public Object[] getChildren(Object element) {
		//check to see if we already have the children cached in the tree map
		Object tree = cachedTrees.get(element);
		if (tree != null) {
			return ((RemoteFolderTree) tree).getChildren();
		}
		
		if (manager != null) {
			Object[] children = manager.getChildren(element);
			if (children != null) {
				// This will be a placeholder to indicate 
				// that the real children are being fetched
				return children;
			}
		}
		Object[] children = super.getChildren(element);
		for (Object object : children) {
			if (object instanceof CVSModelElement) 
				((CVSModelElement)object).setWorkingSet(getWorkingSet());
		}
		return children;
	}

	public void cancelJobs(RepositoryRoot[] roots) {
		if (manager != null) {
			for (RepositoryRoot root : roots) {
				cancelJobs(root.getRoot());
			}
		}
	}
	
	/**
	 * Cancel any jobs that are fetching content from the given location.
	 * @param location
	 */
	public void cancelJobs(ICVSRepositoryLocation location) {
		if (manager != null) {
			manager.cancel(location);
		}
	}
	
	/**
	 * Adds a remote folder tree to the cache
	 * @param project 
	 * 
	 */
	public void addCachedTree(ICVSRemoteFolder project, RemoteFolderTree tree){
		cachedTrees.put(project, tree); 
	}
	
	public void purgeCache(){
		cachedTrees.clear();
	}
	
	
}
