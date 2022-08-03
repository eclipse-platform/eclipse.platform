/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreePathContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.team.core.diff.FastDiffFilter;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IDiffChangeEvent;
import org.eclipse.team.core.diff.IDiffTree;
import org.eclipse.team.core.mapping.IResourceDiffTree;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.core.mapping.provider.ResourceDiffTree;
import org.eclipse.team.internal.ui.IPreferenceIds;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.mapping.ITeamContentProviderManager;
import org.eclipse.team.ui.mapping.SynchronizationContentProvider;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;

/**
 * This content provider displays the mappings as a flat list
 * of elements.
 * <p>
 * There are three use-cases we need to consider. The first is when there
 * are resource level mappings to be displayed. The second is when there
 * are mappings from a model provider that does not have a content provider
 * registered. The third is for the case where a resource mapping does not
 * have a model provider registered (this may be considered an error case).
 *
 */
public class ResourceModelContentProvider extends SynchronizationContentProvider implements ITreePathContentProvider {

	private WorkbenchContentProvider provider;

	@Override
	protected ITreeContentProvider getDelegateContentProvider() {
		if (provider == null)
			provider = new WorkbenchContentProvider();
		return provider;
	}

	@Override
	protected String getModelProviderId() {
		return ModelProvider.RESOURCE_MODEL_PROVIDER_ID;
	}

	@Override
	protected Object getModelRoot() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}

	@Override
	protected boolean isInScope(ISynchronizationScope scope, Object parent, Object elementOrPath) {
		Object object = internalGetElement(elementOrPath);
		if (object instanceof IResource) {
			IResource resource = (IResource) object;
			if (!resource.getProject().isAccessible())
				return false;
			if (scope.contains(resource))
				return true;
			if (hasChildrenInScope(scope, object, resource)) {
				return true;
			}
		}
		return false;
	}

	private boolean hasChildrenInScope(ISynchronizationScope scope, Object object, IResource resource) {
		if (!resource.isAccessible())
			return false;
		IResource[] roots = scope.getRoots();
		for (IResource root : roots) {
			if (resource.getFullPath().isPrefixOf(root.getFullPath()))
				return true;
		}
		return false;
	}

	@Override
	public void init(ICommonContentExtensionSite site) {
		super.init(site);
		TeamUIPlugin.getPlugin().getPreferenceStore().addPropertyChangeListener(this);
	}

	@Override
	public void dispose() {
		if (provider != null)
			provider.dispose();
		super.dispose();
		TeamUIPlugin.getPlugin().getPreferenceStore().removePropertyChangeListener(this);
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(IPreferenceIds.SYNCVIEW_DEFAULT_LAYOUT)) {
			refresh();
		}
		super.propertyChange(event);
	}

	@Override
	protected Object[] getChildrenInContext(ISynchronizationContext context, Object parentOrPath, Object[] children) {
		Object parent = internalGetElement(parentOrPath);
		if (parent instanceof IResource) {
			IResource resource = (IResource) parent;
			if (resource.getType() == IResource.PROJECT && !resource.getProject().isAccessible())
				return new Object[0];
			IResourceDiffTree diffTree = context.getDiffTree();
			// TODO: Could optimize this to a single pass over the children instead of 3
			children = getTraversalCalculator().filterChildren(diffTree, resource, parentOrPath, children);
			if (children.length != 0)
				children = getChildrenInScope(context.getScope(), parentOrPath, children);
			if (children.length != 0)
				children = internalGetChildren(context, parentOrPath, children);
			return children;
		}
		return super.getChildrenInContext(context, parentOrPath, children);
	}

	private Object[] internalGetChildren(ISynchronizationContext context, Object parent, Object[] children) {
		List<Object> result = new ArrayList<>(children.length);
		for (Object object : children) {
			// If the parent is a TreePath then the subclass is
			// TreePath aware and we can send a TrePath to the
			// isVisible method
			if (parent instanceof TreePath) {
				TreePath tp = (TreePath) parent;
				object = tp.createChildPath(object);
			}
			if (isVisible(context, object))
				result.add(internalGetElement(object));
		}
		return result.toArray(new Object[result.size()]);
	}

	@Override
	protected ResourceTraversal[] getTraversals(ISynchronizationContext context, Object elementOrPath) {
		Object object = internalGetElement(elementOrPath);
		ISynchronizationScope scope = context.getScope();
		// First see if the object is a root of the scope
		ResourceMapping mapping = scope.getMapping(object);
		if (mapping != null) {
			ResourceTraversal[] traversals = scope.getTraversals(mapping);
			if (traversals == null)
				return new ResourceTraversal[0];
			return traversals;
		}
		// Next, check if the object is within the scope
		if (object instanceof IResource) {
			IResource resource = (IResource) object;
			if (scope.contains(resource)) {
				List<ResourceTraversal> result = new ArrayList<>();
				ResourceTraversal[] traversals = scope.getTraversals();
				if(traversals == null) {
					return result.toArray(new ResourceTraversal[result.size()]);
				}
				for (ResourceTraversal traversal : traversals) {
					if (traversal.contains(resource)) {
						boolean include = false;
						int depth = traversal.getDepth();
						if (depth == IResource.DEPTH_INFINITE) {
							include = true;
						} else {
							IResource[] roots = traversal.getResources();
							for (IResource root : roots) {
								if (root.equals(resource)) {
									include = true;
									break;
								}
								if (root.getFullPath().equals(resource.getFullPath().removeLastSegments(1)) && depth == IResource.DEPTH_ONE) {
									include = true;
									depth = IResource.DEPTH_ZERO;
									break;
								}
							}
						}
						if (include) {
							int layoutDepth = getTraversalCalculator().getLayoutDepth(resource, internalGetPath(elementOrPath));
							result.add(new ResourceTraversal(new IResource[] { resource}, Math.min(depth, layoutDepth), IResource.NONE));
						}
					}
				}
				return result.toArray(new ResourceTraversal[result.size()]);
			} else {
				// The resource is a parent of an in-scope resource
				// TODO: fails due to use of roots
				ResourceMapping[] mappings = scope.getMappings(ModelProvider.RESOURCE_MODEL_PROVIDER_ID);
				List<ResourceTraversal> result = new ArrayList<>();
				for (ResourceMapping resourceMapping : mappings) {
					Object element = resourceMapping.getModelObject();
					IResource root = getResource(element);
					if (root != null) {
						if (resource.getFullPath().isPrefixOf(root.getFullPath())) {
							mapping = scope.getMapping(element);
							if (mapping != null) {
								ResourceTraversal[] traversals = scope.getTraversals(mapping);
								if(traversals != null) {
									result.addAll(Arrays.asList(traversals));
								}
							}
						}
					}
				}
				return result.toArray(new ResourceTraversal[result.size()]);
			}
		}
		return new ResourceTraversal[0];
	}

	private IResource getResource(Object element) {
		if (element instanceof IResource) {
			return (IResource) element;
		}
		return Utils.getResource(element);
	}

	@Override
	protected boolean hasChildrenInContext(ISynchronizationContext context, Object elementOrPath) {
		return getTraversalCalculator().hasChildren(context, elementOrPath);
	}

	@Override
	public void propertyChanged(IDiffTree tree, final int property, final IPath[] paths) {
		Utils.asyncExec((Runnable) () -> {
			ISynchronizationContext context = getContext();
			if (context != null) {
				updateLabels(context, paths);
			}
		}, (StructuredViewer)getViewer());
	}

	private IResource[] getResources(ISynchronizationContext context, IPath[] paths) {
		List<IResource> resources = new ArrayList<>();
		for (IPath path : paths) {
			IResource resource = getResource(context, path);
			if (resource != null)
				resources.add(resource);
		}
		return resources.toArray(new IResource[resources.size()]);
	}

	private IResource getResource(ISynchronizationContext context, IPath path) {
		// Does the resource exist locally
		IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
		if (resource != null) {
			return resource;
		}
		// Look in the diff tree for a phantom
		if (context != null) {
			IResourceDiffTree diffTree = context.getDiffTree();
			// Is there a diff for the path
			IDiff node = diffTree.getDiff(path);
			if (node != null) {
				return diffTree.getResource(node);
			}
			// Is there any descendants of the path
			if (diffTree.getChildren(path).length > 0) {
				if (path.segmentCount() == 1) {
					return ResourcesPlugin.getWorkspace().getRoot().getProject(path.segment(0));
				} else if (path.segmentCount() > 1) {
					return ResourcesPlugin.getWorkspace().getRoot().getFolder(path);
				}
			}
		}
		return null;
	}

	protected StructuredViewer getStructuredViewer() {
		return (StructuredViewer)getViewer();
	}

	@Override
	public Object[] getChildren(Object parent) {
		if (parent instanceof ISynchronizationContext) {
			// Put the resource projects directly under the context
			parent = getModelRoot();
		}
		return super.getChildren(parent);
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof ISynchronizationContext) {
			// Put the resource projects directly under the context
			element = getModelRoot();
		}
		return super.hasChildren(element);
	}

	@Override
	public Object[] getElements(Object parent) {
		if (parent instanceof ISynchronizationContext) {
			// Put the resource projects directly under the context
			parent = getModelRoot();
		}
		return super.getElements(parent);
	}

	@Override
	public Object getParent(Object elementOrPath) {
		Object element = internalGetElement(elementOrPath);
		if (element instanceof IProject) {
			ISynchronizationContext context = getContext();
			if (context != null)
				return context;
		}
		return super.getParent(elementOrPath);
	}

	@Override
	protected void refresh() {
		Utils.syncExec((Runnable) () -> {
			TreeViewer treeViewer = ((TreeViewer)getViewer());
			treeViewer.refresh();
		}, getViewer().getControl());
	}

	protected void updateLabels(ISynchronizationContext context, final IPath[] paths) {
		IResource[] resources = getResources(context, paths);
		if (resources.length > 0)
			((AbstractTreeViewer)getViewer()).update(resources, null);
	}

	protected ResourceModelTraversalCalculator getTraversalCalculator() {
		return ResourceModelTraversalCalculator.getTraversalCalculator(getConfiguration());
	}

	@Override
	protected boolean isVisible(IDiff diff) {
		return super.isVisible(diff);
	}

	@Override
	public Object[] getChildren(TreePath parentPath) {
		return getChildren((Object)parentPath);
	}

	@Override
	public boolean hasChildren(TreePath path) {
		return hasChildren((Object)path);
	}

	@Override
	public TreePath[] getParents(Object element) {
		TreePath path = getTraversalCalculator().getParentPath(getContext(), getModelProvider(), element);
		if (path != null) {
			return new TreePath[] { path };
		}
		return new TreePath[0];
	}

	private Object internalGetElement(Object elementOrPath) {
		if (elementOrPath instanceof TreePath) {
			TreePath tp = (TreePath) elementOrPath;
			return tp.getLastSegment();
		}
		return elementOrPath;
	}

	private TreePath internalGetPath(Object elementOrPath) {
		if (elementOrPath instanceof TreePath) {
			return (TreePath) elementOrPath;
		}
		return null;
	}

	@Override
	public void diffsChanged(final IDiffChangeEvent event, IProgressMonitor monitor) {
		Utils.syncExec((Runnable) () -> handleChange(event), (StructuredViewer)getViewer());
	}

	private void handleChange(IDiffChangeEvent event) {
		List<IResource> refreshes = new ArrayList<>();
		List<IResource> additions = new ArrayList<>();
		List<IResource> removals = new ArrayList<>();
		if (isFlatPresentation()) {
			Set existingResources = getVisibleResources();
			IResource[] changedResources = getChangedResources(event, existingResources);
			for (IResource resource : changedResources) {
				if (event.getTree().getDiff(resource.getFullPath()) != null) {
					if (existingResources.contains(resource)) {
						refreshes.add(resource);
					} else {
						additions.add(resource);
					}
				} else if (existingResources.contains(resource)) {
					removals.add(resource);

				}
			}
		} else {
			IProject[] changedProjects = getChangedProjects(event);
			Set existingProjects = getVisibleProjects();
			for (IProject project : changedProjects) {
				if (hasVisibleChanges(event.getTree(), project)) {
					if (existingProjects.contains(project)) {
						refreshes.add(project);
					} else {
						additions.add(project);
					}
				} else if (existingProjects.contains(project)) {
					removals.add(project);

				}
			}
		}
		if (!removals.isEmpty() || !additions.isEmpty() || !refreshes.isEmpty()) {
			TreeViewer viewer = (TreeViewer)getViewer();
			Tree tree = viewer.getTree();
			try {
				tree.setRedraw(false);
				if (!additions.isEmpty())
					viewer.add(viewer.getInput(), additions.toArray());
				if (!removals.isEmpty())
					viewer.remove(viewer.getInput(), removals.toArray());
				if (!refreshes.isEmpty()) {
					for (Object element : refreshes) {
						viewer.refresh(element);
					}
				}
			} finally {
				tree.setRedraw(true);
			}
		}
	}

	private boolean isFlatPresentation() {
		ISynchronizePageConfiguration configuration = getConfiguration();
		if (configuration != null) {
			String p = (String)configuration.getProperty(ITeamContentProviderManager.PROP_PAGE_LAYOUT);
			return p != null && p.equals(ITeamContentProviderManager.FLAT_LAYOUT);
		}
		return false;
	}

	private boolean hasVisibleChanges(IDiffTree tree, IResource resource) {
		return tree.hasMatchingDiffs(resource.getFullPath(), new FastDiffFilter() {
			@Override
			public boolean select(IDiff diff) {
				return isVisible(diff);
			}
		});
	}

	private IProject[] getChangedProjects(IDiffChangeEvent event) {
		Set<IResource> result = new HashSet<>();
		IDiff[] changes = event.getChanges();
		for (IDiff diff : changes) {
			IResource resource = ResourceDiffTree.getResourceFor(diff);
			if (resource != null) {
				result.add(resource.getProject());
			}
		}
		IDiff[] additions = event.getAdditions();
		for (IDiff diff : additions) {
			IResource resource = ResourceDiffTree.getResourceFor(diff);
			if (resource != null) {
				result.add(resource.getProject());
			}
		}
		IPath[] removals = event.getRemovals();
		for (IPath path : removals) {
			if (path.segmentCount() > 0) {
				IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(path.segment(0));
				result.add(project);
			}
		}
		return result.toArray(new IProject[result.size()]);
	}

	private Set getVisibleProjects() {
		TreeViewer viewer = (TreeViewer)getViewer();
		Tree tree = viewer.getTree();
		TreeItem[] children = tree.getItems();
		Set<IResource> result = new HashSet<>();
		for (TreeItem control : children) {
			Object data = control.getData();
			IResource resource = Utils.getResource(data);
			if (resource != null && resource.getType() == IResource.PROJECT) {
				result.add(resource);
			}
		}
		return result;
	}

	private Set getVisibleResources() {
		TreeViewer viewer = (TreeViewer)getViewer();
		Tree tree = viewer.getTree();
		TreeItem[] children = tree.getItems();
		Set<IResource> result = new HashSet<>();
		for (TreeItem control : children) {
			Object data = control.getData();
			IResource resource = Utils.getResource(data);
			if (resource != null) {
				result.add(resource);
			}
		}
		return result;
	}

	private IResource[] getChangedResources(IDiffChangeEvent event, Set existingResources) {
		Set<IResource> result = new HashSet<>();
		IDiff[] changes = event.getChanges();
		for (IDiff diff : changes) {
			IResource resource = ResourceDiffTree.getResourceFor(diff);
			if (resource != null) {
				result.add(resource);
			}
		}
		IDiff[] additions = event.getAdditions();
		for (IDiff diff : additions) {
			IResource resource = ResourceDiffTree.getResourceFor(diff);
			if (resource != null) {
				result.add(resource);
			}
		}
		IPath[] removals = event.getRemovals();
		for (IPath path : removals) {
			if (path.segmentCount() > 0) {
				IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
				if (resource != null) {
					result.add(resource);
				} else {
					// We need to check the list of displayed resources to see if one matches the given path
					for (Object element : existingResources) {
						resource = (IResource) element;
						if (resource.getFullPath().equals(path)) {
							result.add(resource);
							break;
						}
					}
				}
			}
		}
		return result.toArray(new IResource[result.size()]);
	}
}
