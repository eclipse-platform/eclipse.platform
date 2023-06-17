/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.mapping.IResourceDiffTree;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.mapping.provider.ResourceDiffTree;
import org.eclipse.team.internal.core.subscribers.ChangeSet;
import org.eclipse.team.internal.core.subscribers.DiffChangeSet;
import org.eclipse.team.internal.ui.IPreferenceIds;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.mapping.ITeamContentProviderManager;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;

public class ResourceModelTraversalCalculator {

	public static final String PROP_TRAVERSAL_CALCULATOR = "org.eclipse.team.ui.resourceModelTraversalCalculator"; //$NON-NLS-1$
	private static ResourceModelTraversalCalculator instance;
	private final ISynchronizePageConfiguration configuration;

	public ResourceModelTraversalCalculator(
			ISynchronizePageConfiguration configuration) {
				this.configuration = configuration;
	}

	public ResourceModelTraversalCalculator() {
		configuration = null;
	}

	public int getLayoutDepth(IResource resource, TreePath path) {
		if (isFlatPageLayout()) {
			return (resource instanceof IWorkspaceRoot) ? IResource.DEPTH_INFINITE : IResource.DEPTH_ZERO;
		}
		if (resource.getType() == IResource.PROJECT)
			return IResource.DEPTH_INFINITE;
		if (resource.getType() == IResource.FILE)
			return IResource.DEPTH_ZERO;
		if (path != null && hasNonResource(path))
			return IResource.DEPTH_INFINITE;
		if (getLayout().equals(IPreferenceIds.FLAT_LAYOUT)) {
			return IResource.DEPTH_ZERO;
		} else if (getLayout().equals(IPreferenceIds.COMPRESSED_LAYOUT)) {
			return IResource.DEPTH_ONE;
		}
		return IResource.DEPTH_INFINITE;
	}

	public String getLayout() {
		return TeamUIPlugin.getPlugin().getPreferenceStore().getString(IPreferenceIds.SYNCVIEW_DEFAULT_LAYOUT);
	}

	public Object[] filterChildren(IResourceDiffTree diffTree, IResource resource, Object parentOrPath, Object[] children) {
		if (parentOrPath instanceof TreePath) {
			TreePath tp = (TreePath) parentOrPath;
			if (hasNonResource(tp)) {
				return getTreeChildren(diffTree, resource, children);
			}
		}
		if (isFlatPageLayout()) {
			if (resource instanceof IWorkspaceRoot) {
				return diffTree.getAffectedResources();
			} else {
				return new Object[0];
			}
		} else if (getLayout().equals(IPreferenceIds.FLAT_LAYOUT) && resource.getType() == IResource.PROJECT) {
			return getFlatChildren(diffTree, resource);
		} else if (getLayout().equals(IPreferenceIds.COMPRESSED_LAYOUT) && resource.getType() == IResource.PROJECT) {
			return getCompressedChildren(diffTree, (IProject)resource, children);
		} else if (getLayout().equals(IPreferenceIds.COMPRESSED_LAYOUT) && resource.getType() == IResource.FOLDER) {
			return getCompressedChildren(diffTree, (IFolder)resource, children);
		}
		return getTreeChildren(diffTree, resource, children);
	}

	private boolean hasNonResource(TreePath parentPath) {
		for (int i = 0; i < parentPath.getSegmentCount(); i++) {
			Object o = parentPath.getSegment(i);
			if (!(o instanceof IResource) && !(o instanceof ModelProvider) && !(o instanceof ChangeSet)) {
				return true;
			}
		}
		return false;
	}

	private Object[] getCompressedChildren(IResourceDiffTree diffTree, IProject project, Object[] children) {
		Set<Object> result = new HashSet<>();
		IDiff[] diffs = diffTree.getDiffs(project, IResource.DEPTH_INFINITE);
		for (IDiff diff : diffs) {
			IResource resource = diffTree.getResource(diff);
			if (resource.getType() == IResource.FILE) {
				IContainer parent = resource.getParent();
				if (parent.getType() == IResource.FOLDER)
					result.add(parent);
				else
					result.add(resource);
			} else if (resource.getType() == IResource.FOLDER)
				result.add(resource);
		}
		return result.toArray();
	}

	/*
	 * Only return the files that are direct children of the folder
	 */
	private Object[] getCompressedChildren(IResourceDiffTree diffTree, IFolder folder, Object[] children) {
		Set<Object> result = new HashSet<>();
		for (Object object : children) {
			if (object instanceof IResource) {
				IResource resource = (IResource) object;
				if (resource.getType() == IResource.FILE)
					result.add(resource);
			}
		}
		IDiff[] diffs = diffTree.getDiffs(folder, IResource.DEPTH_ONE);
		for (IDiff diff : diffs) {
			IResource resource = diffTree.getResource(diff);
			if (resource.getType() == IResource.FILE)
				result.add(resource);
		}
		return result.toArray();
	}

	private Object[] getFlatChildren(IResourceDiffTree diffTree, IResource resource) {
		Object[] allChildren;
		IDiff[] diffs = diffTree.getDiffs(resource, IResource.DEPTH_INFINITE);
		ArrayList<Object> result = new ArrayList<>();
		for (IDiff diff : diffs) {
			result.add(diffTree.getResource(diff));
		}
		allChildren = result.toArray();
		return allChildren;
	}

	private Object[] getTreeChildren(IResourceDiffTree diffTree, IResource resource, Object[] children) {
		Set<Object> result = new HashSet<>();
		Collections.addAll(result, children);
		IResource[] setChildren = getChildren(diffTree, resource);
		Collections.addAll(result, setChildren);
		Object[] allChildren = result.toArray(new Object[result.size()]);
		return allChildren;
	}

	public static IResource[] getChildren(IResourceDiffTree diffTree, IResource resource) {
		Set<IResource> result = new HashSet<>();
		IPath[] childPaths = diffTree.getChildren(resource.getFullPath());
		for (IPath path : childPaths) {
			IDiff delta = diffTree.getDiff(path);
			IResource child;
			if (delta == null) {
				// the path has descendent deltas so it must be a folder
				if (path.segmentCount() == 1) {
					child = ((IWorkspaceRoot)resource).getProject(path.lastSegment());
				} else {
					child = ((IContainer)resource).getFolder(IPath.fromOSString(path.lastSegment()));
				}
			} else {
				child = diffTree.getResource(delta);
			}
			result.add(child);
		}
		return result.toArray(new IResource[result.size()]);
	}

	public ResourceTraversal[] getTraversals(DiffChangeSet dcs, TreePath tp) {
		IResource[] resources = getResource(dcs, tp);
		return new ResourceTraversal[] { new ResourceTraversal(resources, IResource.DEPTH_ZERO, IResource.NONE) };
	}

	private IResource[] getResource(DiffChangeSet dcs, TreePath tp) {
		if (tp.getSegmentCount() == 1 && tp.getFirstSegment() == dcs) {
			return dcs.getResources();
		}
		Set<IResource> result = new HashSet<>();
		Object o = tp.getLastSegment();
		if (o instanceof IResource) {
			IResource resource = (IResource) o;
			int depth = getLayoutDepth(resource, tp);
			IDiff[] diffs = dcs.getDiffTree().getDiffs(resource, depth);
			for (IDiff diff : diffs) {
				IResource r = ResourceDiffTree.getResourceFor(diff);
				if (r != null)
					result.add(r);
			}
		}
		return result.toArray(new IResource[result.size()]);
	}

	public ResourceTraversal[] getTraversals(IResource resource, TreePath tp) {
		return new ResourceTraversal[] { new ResourceTraversal(new IResource[] { resource }, getLayoutDepth(resource, tp), IResource.NONE) };
	}

	public boolean isResourcePath(TreePath path) {
		for (int i = 0; i < path.getSegmentCount(); i++) {
			Object o = path.getSegment(i);
			if (!(o instanceof IResource)) {
				return false;
			}
		}
		return true;
	}

	public String getLabel(Object elementOrPath) {
		if (elementOrPath instanceof TreePath && hasNonResource((TreePath)elementOrPath)) {
			return null;
		}
		Object element = internalGetElement(elementOrPath);
		Object parent = internalGetElementParent(elementOrPath);
		if (element instanceof IResource) {
			IResource resource = (IResource) element;
			if (isFlatPageLayout()) {
				IPath path = resource.getFullPath();
				if (!path.isEmpty())
					return NLS.bind(TeamUIMessages.ResourceModelLabelProvider_0, resource.getName(), path.toString());
			}
			if (getLayout().equals(IPreferenceIds.COMPRESSED_LAYOUT)
					&& resource.getType() == IResource.FOLDER
					&& (parent == null || parent instanceof IProject)) {
				return resource.getProjectRelativePath().toString();
			}
			if (getLayout().equals(IPreferenceIds.FLAT_LAYOUT)
					&& resource.getType() == IResource.FILE
					&& (parent == null || parent instanceof IProject)) {
				IPath parentPath = resource.getProjectRelativePath().removeLastSegments(1);
				if (!parentPath.isEmpty())
					return NLS.bind(TeamUIMessages.ResourceModelLabelProvider_0, resource.getName(), parentPath.toString());
			}
		}
		return null;
	}

	public boolean isCompressedFolder(Object elementOrPath) {
		if (elementOrPath instanceof TreePath && hasNonResource((TreePath)elementOrPath)) {
			return false;
		}
		Object element = internalGetElement(elementOrPath);
		Object parent = internalGetElementParent(elementOrPath);
		if (element instanceof IResource) {
			IResource resource = (IResource) element;
			// Only use the compressed folder icon if the parent is not known
			// or the parent is a project
			return getLayout().equals(IPreferenceIds.COMPRESSED_LAYOUT)
				&& resource.getType() == IResource.FOLDER
				&& (parent == null || parent instanceof IProject);
		}
		return false;
	}

	private TreePath internalGetPath(Object elementOrPath) {
		if (elementOrPath instanceof TreePath) {
			return (TreePath) elementOrPath;
		}
		return null;
	}

	private Object internalGetElement(Object elementOrPath) {
		if (elementOrPath instanceof TreePath) {
			TreePath tp = (TreePath) elementOrPath;
			return tp.getLastSegment();
		}
		return elementOrPath;
	}

	private Object internalGetElementParent(Object elementOrPath) {
		if (elementOrPath instanceof TreePath) {
			TreePath tp = (TreePath) elementOrPath;
			if (tp.getSegmentCount() > 1) {
				return tp.getSegment(tp.getSegmentCount() - 2);
			}

		}
		return null;
	}

	public boolean hasChildren(ISynchronizationContext context, Object elementOrPath) {
		Object element = internalGetElement(elementOrPath);
		if (element instanceof IContainer) {
			IContainer container = (IContainer) element;
			// For containers check to see if the delta contains any children
			if (context != null) {
				int depth = getLayoutDepth(container, internalGetPath(elementOrPath));
				if (depth == IResource.DEPTH_ZERO)
					return false;
				IResourceDiffTree tree = context.getDiffTree();
				IResource[] members = tree.members(container);
				if (members.length > 0) {
					if (depth == IResource.DEPTH_INFINITE)
						return true;
					for (IResource resource : members) {
						if (resource.getType() == IResource.FILE)
							return true;
					}
				}
			}
		}
		return false;
	}

	public TreePath getParentPath(ISynchronizationContext context, ModelProvider provider, Object element) {
		if (element instanceof IResource) {
			IResource resource = (IResource) element;
			TreePath treePath = getProviderRootPath(context, provider);
			if (resource.getType() == IResource.ROOT){
				return null;
			}
			if (resource.getType() == IResource.PROJECT){
				return treePath;
			}
			if (getLayout().equals(IPreferenceIds.FLAT_LAYOUT)) {
				return treePath.createChildPath(resource.getProject());
			} else if (getLayout().equals(IPreferenceIds.COMPRESSED_LAYOUT) && resource.getType() == IResource.FOLDER) {
				return treePath.createChildPath(resource.getProject());
			} else if (getLayout().equals(IPreferenceIds.COMPRESSED_LAYOUT) && resource.getType() == IResource.FILE) {
				if (resource.getParent().getType() == IResource.PROJECT)
					return treePath.createChildPath(resource.getProject());
				return treePath.createChildPath(resource.getProject()).createChildPath(resource.getParent());
			}
			IResource parent = resource.getParent();
			IResource[] resourcePath = new IResource[parent.getFullPath().segmentCount()];
			for (int i = resourcePath.length - 1; i >= 0; i--) {
				resourcePath[i] = parent;
				parent = parent.getParent();
			}
			for (IResource r : resourcePath) {
				treePath = treePath.createChildPath(r);
			}
			return treePath;
		}
		return null;
	}

	private TreePath getProviderRootPath(ISynchronizationContext context, ModelProvider provider) {
		if (context == null)
			return TreePath.EMPTY.createChildPath(provider);
		return TreePath.EMPTY;
	}

	private boolean isFlatPageLayout() {
		if (configuration != null) {
			String p = (String)configuration.getProperty(ITeamContentProviderManager.PROP_PAGE_LAYOUT);
			return p != null && p.equals(ITeamContentProviderManager.FLAT_LAYOUT);
		}
		return false;
	}

	public synchronized static ResourceModelTraversalCalculator getDefault() {
		if (instance == null)
			instance = new ResourceModelTraversalCalculator();
		return instance;
	}

	public synchronized static ResourceModelTraversalCalculator getTraversalCalculator(ISynchronizePageConfiguration configuration) {
		if (configuration == null)
			return ResourceModelTraversalCalculator.getDefault();
		ResourceModelTraversalCalculator tc = (ResourceModelTraversalCalculator)configuration.getProperty(ResourceModelTraversalCalculator.PROP_TRAVERSAL_CALCULATOR);
		if (tc == null) {
			tc = new ResourceModelTraversalCalculator(configuration);
			configuration.setProperty(ResourceModelTraversalCalculator.PROP_TRAVERSAL_CALCULATOR, tc);
		}
		return tc;
	}

}
