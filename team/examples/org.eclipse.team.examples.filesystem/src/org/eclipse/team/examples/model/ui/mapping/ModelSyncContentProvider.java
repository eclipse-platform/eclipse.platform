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
package org.eclipse.team.examples.model.ui.mapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.resources.mapping.ResourceTraversal;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.team.core.diff.FastDiffFilter;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.diff.IDiffChangeEvent;
import org.eclipse.team.core.diff.IDiffTree;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.core.mapping.provider.ResourceDiffTree;
import org.eclipse.team.examples.filesystem.FileSystemPlugin;
import org.eclipse.team.examples.model.ModelContainer;
import org.eclipse.team.examples.model.ModelObject;
import org.eclipse.team.examples.model.ModelObjectDefinitionFile;
import org.eclipse.team.examples.model.ModelObjectElementFile;
import org.eclipse.team.examples.model.ModelProject;
import org.eclipse.team.examples.model.ModelWorkspace;
import org.eclipse.team.examples.model.mapping.ExampleModelProvider;
import org.eclipse.team.examples.model.ui.ModelNavigatorContentProvider;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.mapping.SynchronizationResourceMappingContext;
import org.eclipse.team.ui.mapping.SynchronizationContentProvider;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.IPipelinedTreeContentProvider;
import org.eclipse.ui.navigator.PipelinedShapeModification;
import org.eclipse.ui.navigator.PipelinedViewerUpdate;

/**
 * The content provider that is used for synchronizations.
 * It also makes use of the Common Navigator pipeline
 * to override the resource content extension so that model projects will
 * replace the corresponding resource project in the Synchronize view.
 */
public class ModelSyncContentProvider extends SynchronizationContentProvider implements IPipelinedTreeContentProvider {

	private ModelNavigatorContentProvider delegate;

	public ModelSyncContentProvider() {
		super();
	}

	@Override
	public void init(ICommonContentExtensionSite site) {
		super.init(site);
		delegate = new ModelNavigatorContentProvider(getContext() != null);
		delegate.init(site);
	}

	@Override
	public void dispose() {
		super.dispose();
		if (delegate != null)
			delegate.dispose();
	}

	@Override
	protected ITreeContentProvider getDelegateContentProvider() {
		return delegate;
	}

	@Override
	protected String getModelProviderId() {
		return ExampleModelProvider.ID;
	}

	@Override
	protected Object getModelRoot() {
		return ModelWorkspace.getRoot();
	}

	@Override
	protected ResourceTraversal[] getTraversals(
			ISynchronizationContext context, Object object) {
		if (object instanceof ModelObject) {
			ModelObject mo = (ModelObject) object;
			ResourceMapping mapping = mo.getAdapter(ResourceMapping.class);
			ResourceMappingContext rmc = new SynchronizationResourceMappingContext(context);
			try {
				// Technically speaking, this may end up being too long running for this
				// (i.e. we may end up hitting the server) but it will do for illustration purposes
				return mapping.getTraversals(rmc, new NullProgressMonitor());
			} catch (CoreException e) {
				FileSystemPlugin.log(e);
			}
		}
		return new ResourceTraversal[0];
	}

	@Override
	protected Object[] getChildrenInContext(ISynchronizationContext context, Object parent, Object[] children) {
		Set<Object> allChildren = new HashSet<>();
		allChildren.addAll(Arrays.asList(super.getChildrenInContext(context, parent, children)));
		// We need to override this method in order to ensure that any elements
		// that exist in the context but do not exist locally are included
		if (parent instanceof ModelContainer) {
			ModelContainer mc = (ModelContainer) parent;
			IDiff[] diffs = context.getDiffTree().getDiffs(mc.getResource(), IResource.DEPTH_ONE);
			for (IDiff diff : diffs) {
				IResource resource = ResourceDiffTree.getResourceFor(diff);
				if (!resource.exists() && ModelObjectDefinitionFile.isModFile(resource)) {
					ModelObject o = ModelObject.create(resource);
					if (o != null)
						allChildren.add(o);
				}
			}
		}
		if (parent instanceof ModelObjectDefinitionFile) {
			ResourceTraversal[] traversals = getTraversals(context, parent);
			IDiff[] diffs = context.getDiffTree().getDiffs(traversals);
			for (IDiff diff : diffs) {
				IResource resource = ResourceDiffTree.getResourceFor(diff);
				if (!resource.exists() && ModelObjectElementFile.isMoeFile(resource)) {
					ModelObject o = new ModelObjectElementFile((ModelObjectDefinitionFile)parent, (IFile)resource);
					if (o != null)
						allChildren.add(o);
				}
			}
		}
		return allChildren.toArray(new Object[allChildren.size()]);
	}

	@Override
	public void getPipelinedChildren(Object aParent, Set theCurrentChildren) {
		// Nothing to do
	}

	@Override
	public void getPipelinedElements(Object anInput, Set theCurrentElements) {
		// Replace any model projects with a ModelProject if the input
		// is a synchronization context
		if (anInput instanceof ISynchronizationContext) {
			List<ModelObject> newProjects = new ArrayList<>();
			for (Iterator iter = theCurrentElements.iterator(); iter.hasNext();) {
				Object element = iter.next();
				if (element instanceof IProject) {
					IProject project = (IProject) element;
					try {
						if (ModelProject.isModProject(project)) {
							iter.remove();
							newProjects.add(ModelObject.create(project));
						}
					} catch (CoreException e) {
						FileSystemPlugin.log(e);
					}
				}
			}
			theCurrentElements.addAll(newProjects);
		} else if (anInput instanceof ISynchronizationScope) {
			// When the root is a scope, we should return
			// our model provider so all model providers appear
			// at the root of the viewer.
			theCurrentElements.add(getModelProvider());
		}
	}

	@Override
	public Object getPipelinedParent(Object anObject, Object aSuggestedParent) {
		// We're not changing the parenting of any resources
		return aSuggestedParent;
	}

	@Override
	public PipelinedShapeModification interceptAdd(PipelinedShapeModification anAddModification) {
		if (anAddModification.getParent() instanceof ISynchronizationContext) {
			for (Iterator iter = anAddModification.getChildren().iterator(); iter.hasNext();) {
				Object element = iter.next();
				if (element instanceof IProject) {
					IProject project = (IProject) element;
					try {
						if (ModelProject.isModProject(project)) {
							iter.remove();
						}
					} catch (CoreException e) {
						FileSystemPlugin.log(e);
					}
				}
			}
		}
		return null;
	}

	@Override
	public boolean interceptRefresh(PipelinedViewerUpdate aRefreshSynchronization) {
		// No need to intercept the refresh
		return false;
	}

	@Override
	public PipelinedShapeModification interceptRemove(PipelinedShapeModification aRemoveModification) {
		// No need to intercept the remove
		return aRemoveModification;
	}

	@Override
	public boolean interceptUpdate(PipelinedViewerUpdate anUpdateSynchronization) {
		// No need to intercept the update
		return false;
	}

	@Override
	public void diffsChanged(final IDiffChangeEvent event, IProgressMonitor monitor) {
		// Override in order to perform custom viewer updates when the diff tree changes
		Utils.syncExec((Runnable) () -> handleChange(event), (StructuredViewer)getViewer());
	}

	void handleChange(IDiffChangeEvent event) {
		Set existingProjects = getVisibleModelProjects();
		IProject[] changedProjects = getChangedModelProjects(event);
		List<ModelObject> refreshes = new ArrayList<>(changedProjects.length);
		List<ModelObject> additions = new ArrayList<>(changedProjects.length);
		List<ModelObject> removals = new ArrayList<>(changedProjects.length);
		for (IProject project : changedProjects) {
			if (hasVisibleChanges(event.getTree(), project)) {
				if (existingProjects.contains(project)) {
					refreshes.add(ModelObject.create(project));
				} else {
					additions.add(ModelObject.create(project));
				}
			} else if (existingProjects.contains(project)) {
				removals.add(ModelObject.create(project));

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

	private boolean hasVisibleChanges(IDiffTree tree, IProject project) {
		return tree.hasMatchingDiffs(project.getFullPath(), new FastDiffFilter() {
			@Override
			public boolean select(IDiff diff) {
				return isVisible(diff);
			}
		});
	}

	/*
	 * Return the list of all projects that are model projects
	 */
	private IProject[] getChangedModelProjects(IDiffChangeEvent event) {
		Set<IProject> result = new HashSet<>();
		IDiff[] changes = event.getChanges();
		for (IDiff diff : changes) {
			IResource resource = ResourceDiffTree.getResourceFor(diff);
			if (resource != null && isModProject(resource.getProject())) {
				result.add(resource.getProject());
			}
		}
		IDiff[] additions = event.getAdditions();
		for (IDiff diff : additions) {
			IResource resource = ResourceDiffTree.getResourceFor(diff);
			if (resource != null && isModProject(resource.getProject())) {
				result.add(resource.getProject());
			}
		}
		IPath[] removals = event.getRemovals();
		for (IPath path : removals) {
			if (path.segmentCount() > 0) {
				IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(path.segment(0));
				if (isModProject(project))
					result.add(project);
			}
		}
		return result.toArray(new IProject[result.size()]);
	}

	private boolean isModProject(IProject project) {
		try {
			return ModelProject.isModProject(project);
		} catch (CoreException e) {
			FileSystemPlugin.log(e);
		}
		return false;
	}

	/*
	 * Return the set of visible model projects
	 */
	private Set getVisibleModelProjects() {
		TreeViewer viewer = (TreeViewer)getViewer();
		Tree tree = viewer.getTree();
		TreeItem[] children = tree.getItems();
		Set result = new HashSet();
		for (TreeItem control : children) {
			Object data = control.getData();
			if (data instanceof ModelProject) {
				result.add(((ModelProject) data).getProject());
			}
		}
		return result;
	}

	@Override
	public void propertyChanged(IDiffTree tree, int property, IPath[] paths) {
		// We're overriding this message so that label updates occur for any elements
		// whose labels may have changed
		if (getContext() == null)
			return;
		final Set<ModelObject> updates = new HashSet<>();
		boolean refresh = false;
		for (IPath path : paths) {
			IDiff diff = tree.getDiff(path);
			if (diff != null) {
				IResource resource = ResourceDiffTree.getResourceFor(diff);
				ModelObject object = ModelObject.create(resource);
				if (object != null) {
					updates.add(object);
				} else {
					// If the resource is a MOE file, we need to update both the MOE and the MOD file
					// Unfortunately, there's no good way to find the parent file so we'll just refresh everything
					refresh = true;
				}
			}
		}
		if (!updates.isEmpty() || refresh) {
			final boolean refreshAll = refresh;
			final StructuredViewer viewer = (StructuredViewer)getViewer();
			Utils.syncExec((Runnable) () -> {
				if (refreshAll)
					viewer.refresh(true);
				else
					viewer.update(updates.toArray(new ModelObject[updates.size()]), null);
			}, viewer);
		}
	}

}
