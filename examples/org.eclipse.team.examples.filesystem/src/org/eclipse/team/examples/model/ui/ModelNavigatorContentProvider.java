/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
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
package org.eclipse.team.examples.model.ui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.examples.filesystem.FileSystemPlugin;
import org.eclipse.team.examples.model.ModelObject;
import org.eclipse.team.examples.model.ModelProject;
import org.eclipse.team.examples.model.ModelWorkspace;
import org.eclipse.team.ui.mapping.ITeamStateChangeEvent;
import org.eclipse.team.ui.mapping.ITeamStateChangeListener;
import org.eclipse.team.ui.mapping.SynchronizationStateTester;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;
import org.eclipse.ui.navigator.ICommonContentProvider;
import org.eclipse.ui.navigator.IPipelinedTreeContentProvider;
import org.eclipse.ui.navigator.PipelinedShapeModification;
import org.eclipse.ui.navigator.PipelinedViewerUpdate;
import org.eclipse.ui.navigator.SaveablesProvider;

/**
 * Model content provider for use with the Common Navigator framework.
 * It makes use of an IWorkbenchAdapter to get the children and parent
 * of model objects. It also makes use of the Common Navigator pipeline
 * to override the resource content extension so that model projects will
 * replace the corresponding resource project in the Project Explorer.
 */
public class ModelNavigatorContentProvider extends BaseWorkbenchContentProvider
implements ICommonContentProvider, IResourceChangeListener, IPipelinedTreeContentProvider, ITeamStateChangeListener, IAdaptable {

	private ICommonContentExtensionSite extensionSite;
	private boolean isWorkspaceRoot;
	private Viewer viewer;
	private final boolean updateViewer;
	private SynchronizationStateTester syncStateTester;
	private Object saveablesProvider = new ModelSaveablesProvider();

	public ModelNavigatorContentProvider() {
		super();
		updateViewer = true;
	}

	/**
	 * Create a contentProvider
	 * @param updateViewer whether this content provider is reponsible for updating the viewer
	 */
	public ModelNavigatorContentProvider(boolean updateViewer) {
		this.updateViewer = updateViewer;
	}

	@Override
	public void init(ICommonContentExtensionSite aConfig) {
		extensionSite = aConfig;
		if (updateViewer) {
			ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
			// Use a synchronization state tester to listen for team state changes
			syncStateTester = new SynchronizationStateTester();
			syncStateTester.getTeamStateProvider().addDecoratedStateChangeListener(this);
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		if (syncStateTester != null)
			syncStateTester.getTeamStateProvider().removeDecoratedStateChangeListener(this);
	}

	@Override
	public void restoreState(IMemento aMemento) {
		// Nothing to do
	}

	@Override
	public void saveState(IMemento aMemento) {
		// Nothing to do
	}

	/**
	 * Return the extension site for this label provider.
	 * @return the extension site for this label provider
	 */
	public ICommonContentExtensionSite getExtensionSite() {
		return extensionSite;
	}

	@Override
	public Object[] getElements(Object element) {
		// Since we are used in the project explorer, the root may be
		// an IWorkspaceRoot. We need to change it to the ModelWorkspace
		if (element instanceof IWorkspaceRoot) {
			isWorkspaceRoot = true;
			return super.getElements(ModelObject.create((IWorkspaceRoot)element));

		}
		return super.getElements(element);
	}

	@Override
	public Object getParent(Object element) {
		Object parent = super.getParent(element);
		if (isWorkspaceRoot && parent instanceof ModelWorkspace) {
			return ((ModelWorkspace)parent).getResource();
		}
		return parent;
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.viewer = viewer;
		super.inputChanged(viewer, oldInput, newInput);
	}

	@Override
	public void teamStateChanged(ITeamStateChangeEvent event) {
		// We need to listen to team state changes in order to determine when we need
		// to perform label updates on model elements.
		// We actually just refresh all projects that contain changes.
		// This is inefficient but will do for an example
		Set<ModelProject> refreshProjects = new HashSet<>();
		IResource[] addedRoots = event.getAddedRoots();
		for (IResource resource : addedRoots) {
			if (isModelProject(resource.getProject())) {
				refreshProjects.add((ModelProject) ModelObject.create(resource.getProject()));
			}
		}
		IResource[] removedRoots = event.getRemovedRoots();
		for (IResource resource : removedRoots) {
			if (isModelProject(resource.getProject())) {
				refreshProjects.add((ModelProject) ModelObject.create(resource.getProject()));
			}
		}
		IResource[] changed = event.getChangedResources();
		for (IResource resource : changed) {
			if (isModelProject(resource.getProject())) {
				refreshProjects.add((ModelProject) ModelObject.create(resource.getProject()));
			}
		}

		refreshProjects(refreshProjects.toArray(new ModelProject[refreshProjects.size()]));
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		if (viewer == null) return;
		IResourceDelta delta = event.getDelta();
		IResourceDelta[] children = delta.getAffectedChildren();
		boolean refreshAll = false;
		List<ModelProject> refreshProjects = new ArrayList<>();
		for (IResourceDelta childDelta : children) {
			if (isModelProject(childDelta.getResource())) {
				if (isProjectChange(childDelta)) {
					refreshAll = true;
					break;
				}
				refreshProjects.add((ModelProject) ModelObject.create(childDelta.getResource()));
			}
		}
		if (refreshAll || !refreshProjects.isEmpty()) {
			if (refreshAll)
				refreshViewer();
			else
				refreshProjects(refreshProjects.toArray(new ModelProject[refreshProjects.size()]));
		}
	}

	private void refreshProjects(final ModelProject[] projects) {
		Display.getDefault().asyncExec(() -> {
			if (!getViewer().getControl().isDisposed()) {
				for (ModelProject project : projects) {
					((AbstractTreeViewer)getViewer()).refresh(project, true);
				}
			}
		});
	}

	private void refreshViewer() {
		Display.getDefault().asyncExec(() -> {
			if (!getViewer().getControl().isDisposed()) {
				getViewer().refresh();
			}
		});
	}

	private boolean isProjectChange(IResourceDelta childDelta) {
		if ((childDelta.getFlags() & (IResourceDelta.DESCRIPTION | IResourceDelta.OPEN)) > 0)
			return true;
		return false;
	}

	private boolean isModelProject(IResource resource) {
		try {
			return ModelProject.isModProject(resource.getProject());
		} catch (CoreException e) {
			FileSystemPlugin.log(e);
			return false;
		}
	}

	Viewer getViewer() {
		return viewer;
	}

	@Override
	public void getPipelinedChildren(Object aParent, Set theCurrentChildren) {
		// Nothing to do
	}

	@Override
	public void getPipelinedElements(Object anInput, Set theCurrentElements) {
		// Replace any model projects with a ModelProject
		if (anInput instanceof IWorkspaceRoot) {
			List newProjects = new ArrayList();
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
		}
	}

	@Override
	public Object getPipelinedParent(Object anObject, Object aSuggestedParent) {
		// We're not changing the parenting of any resources
		return aSuggestedParent;
	}

	@Override
	public PipelinedShapeModification interceptAdd(PipelinedShapeModification anAddModification) {
		if (anAddModification.getParent() instanceof IWorkspaceRoot) {
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
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == SaveablesProvider.class) {
			return adapter.cast(saveablesProvider);
		}
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

}
