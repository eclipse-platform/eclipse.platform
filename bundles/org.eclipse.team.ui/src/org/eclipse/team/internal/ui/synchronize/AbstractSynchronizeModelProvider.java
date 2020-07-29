/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.synchronize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.TreeEvent;
import org.eclipse.swt.events.TreeListener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.team.core.ITeamStatus;
import org.eclipse.team.core.synchronize.ISyncInfoSetChangeEvent;
import org.eclipse.team.core.synchronize.ISyncInfoSetChangeListener;
import org.eclipse.team.core.synchronize.ISyncInfoTreeChangeEvent;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.internal.core.TeamPlugin;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.synchronize.ISynchronizeModelElement;
import org.eclipse.team.ui.synchronize.ISynchronizePage;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.SynchronizePageActionGroup;

/**
 * This class is responsible for creating and maintaining a presentation model of
 * {@link SynchronizeModelElement} elements that can be shown in a viewer. The model
 * is based on the synchronization information contained in the provided {@link SyncInfoSet}.
 */
public abstract class AbstractSynchronizeModelProvider implements ISynchronizeModelProvider, ISyncInfoSetChangeListener, TreeListener {

	/**
	 * Property constant for the expansion state for the elements displayed by the page. The
	 * expansion state is a List of resource paths.
	 */
	public static final String P_VIEWER_EXPANSION_STATE = TeamUIPlugin.ID  + ".P_VIEWER_EXPANSION_STATE"; //$NON-NLS-1$

	/**
	 * Property constant for the selection state for the elements displayed by the page. The
	 * selection state is a List of resource paths.
	 */
	public static final String P_VIEWER_SELECTION_STATE = TeamUIPlugin.ID  + ".P_VIEWER_SELECTION_STATE"; //$NON-NLS-1$

	/**
	 * Property constant for the checked state for the elements displayed by the page. The
	 * checked state is a List of resource paths.
	 */
	public static final String P_VIEWER_CHECKED_STATE = TeamUIPlugin.ID  + ".P_VIEWER_CHECKED_STATE"; //$NON-NLS-1$

	private ISynchronizeModelElement root;

	private ISynchronizePageConfiguration configuration;

	private SyncInfoSet set;

	private SynchronizeModelUpdateHandler updateHandler;

	private boolean disposed = false;

	private SynchronizePageActionGroup actionGroup;

	private ListenerList<IPropertyChangeListener> listeners;

	private static final boolean DEBUG = false;

	/**
	 * Constructor for creating a sub-provider
	 * @param parentProvider the parent provider
	 * @param parentNode the root node of the model built by this provider
	 * @param configuration the sync page configuration
	 * @param set the sync info set from which the model is built
	 */
	protected AbstractSynchronizeModelProvider(AbstractSynchronizeModelProvider parentProvider, ISynchronizeModelElement parentNode, ISynchronizePageConfiguration configuration, SyncInfoSet set) {
		Assert.isNotNull(set);
		Assert.isNotNull(parentNode);
		this.root = parentNode;
		this.set = set;
		this.configuration = configuration;
		if (parentProvider == null) {
			// The update handler will register for sync change events
			// with the sync set when the handler is activated
			updateHandler = new SynchronizeModelUpdateHandler(this);
			getTree().addTreeListener(this);
		} else {
			// We will use the parent's update handler and register for changes with the given set
			updateHandler = parentProvider.updateHandler;
			set.addSyncSetChangedListener(this);
		}
	}

	private Tree getTree() {
		return ((Tree)((AbstractTreeViewer)getViewer()).getControl());
	}

	/**
	 * Constructor for creating a root model provider.
	 * @param configuration the sync page configuration
	 * @param set the sync info set from which the model is built
	 */
	protected AbstractSynchronizeModelProvider(ISynchronizePageConfiguration configuration, SyncInfoSet set) {
		this(null, new UnchangedResourceModelElement(null, ResourcesPlugin.getWorkspace().getRoot()) {
			/*
			 * Override to ensure that the diff viewer will appear in CompareEditorInputs
			 */
			@Override
			public boolean hasChildren() {
				return true;
			}
		}, configuration, set);
		// Register the action group for this provider, since it is the root provider
		SynchronizePageActionGroup actionGroup = getActionGroup();
		if (actionGroup != null) {
			configuration.addActionContribution(actionGroup);
		}
	}

	/**
	 * Return the action group for this provider or <code>null</code>
	 * if there are no actions associated with this provider. The action
	 * group will be registered with the configuration if this is
	 * the root provider. If this provider is a sub-provider, it
	 * is up to the parent provider to register the action group.
	 * <p>
	 * The action group for a provider is created by calling the
	 * <code>createdActionGroup</code> method. If this method returns
	 * a non-null group, it is cached so it can be disposed
	 * when the provider is disposed.
	 * @return the action group for this provider or <code>null</code>
	 * if there are no actions associated with this provider
	 */
	public final synchronized SynchronizePageActionGroup getActionGroup() {
		if (actionGroup == null) {
			actionGroup = createActionGroup();
		}
		return actionGroup;
	}

	/**
	 * Create the action group for this provider. By default,
	 * a <code>null</code> is returned. Subclasses may override.
	 * @return the action group for this provider or <code>null</code>
	 */
	protected SynchronizePageActionGroup createActionGroup() {
		return null;
	}

	/**
	 * Return the set that contains the elements this provider is using as
	 * a basis for creating a presentation model. This cannot be null.
	 *
	 * @return the set that contains the elements this provider is
	 * using as a basis for creating a presentation model.
	 */
	@Override
	public SyncInfoSet getSyncInfoSet() {
		return set;
	}

	/**
	 * Returns the input created by this provider or <code>null</code> if
	 * {@link #prepareInput(IProgressMonitor)} hasn't been called on this object yet.
	 *
	 * @return the input created by this provider.
	 */
	@Override
	public ISynchronizeModelElement getModelRoot() {
		return root;
	}

	/**
	 * Return the page configuration for this provider.
	 *
	 * @return the page configuration for this provider.
	 */
	public ISynchronizePageConfiguration getConfiguration() {
		return configuration;
	}

	/**
	 * Return the <code>AbstractTreeViewer</code> associated with this
	 * provider or <code>null</code> if the viewer is not of the proper type.
	 * @return the structured viewer that is displaying the model managed by this provider
	 */
	@Override
	public StructuredViewer getViewer() {
		ISynchronizePage page = configuration.getPage();
		if (page == null) return null;
		Viewer viewer = page.getViewer();
		if (viewer instanceof AbstractTreeViewer) {
			return (AbstractTreeViewer)viewer;
		}
		return null;
	}

	/**
	 * Builds the viewer model based on the contents of the sync set.
	 */
	@Override
	public ISynchronizeModelElement prepareInput(IProgressMonitor monitor) {
		// Connect to the sync set which will register us as a listener and give us a reset event
		// in a background thread
		if (isRootProvider()) {
			updateHandler.connect(monitor);
		} else {
			getSyncInfoSet().connect(this, monitor);
		}
		return getModelRoot();
	}

	/**
	 * Calculate the problem marker that should be shown on the given
	 * element. The returned property can be either
	 * ISynchronizeModelElement.PROPAGATED_ERROR_MARKER_PROPERTY or
	 * ISynchronizeModelElement.PROPAGATED_WARNING_MARKER_PROPERTY.
	 * @param element a synchronize model element
	 * @return the marker property that should be displayed on the element
	 * or <code>null</code> if no marker should be displayed
	 */
	public String calculateProblemMarker(ISynchronizeModelElement element) {
		IResource resource = element.getResource();
		String property = null;
		if (resource != null && resource.exists()) {
			try {
				IMarker[] markers = resource.findMarkers(IMarker.PROBLEM, true, getLogicalModelDepth(resource));
				for (IMarker marker : markers) {
					try {
						Integer severity = (Integer) marker.getAttribute(IMarker.SEVERITY);
						if (severity != null) {
							if (severity.intValue() == IMarker.SEVERITY_ERROR) {
								property = ISynchronizeModelElement.PROPAGATED_ERROR_MARKER_PROPERTY;
								break;
							} else if (severity.intValue() == IMarker.SEVERITY_WARNING) {
								property = ISynchronizeModelElement.PROPAGATED_WARNING_MARKER_PROPERTY;
								// Keep going because there may be errors on other resources
							}
						}
					} catch (CoreException e) {
						if (!resource.exists()) {
							// The resource was deleted concurrently. Forget any previously found property
							property = null;
							break;
						}
						// If the marker exists, log the exception and continue.
						// Otherwise, just ignore the exception and keep going
						if (marker.exists()) {
							TeamPlugin.log(e);
						}
					}
				}
			} catch (CoreException e) {
				// If the resource exists (is accessible), log the exception and continue.
				// Otherwise, just ignore the exception
				if (resource.isAccessible()
						&& e.getStatus().getCode() != IResourceStatus.RESOURCE_NOT_FOUND
						&& e.getStatus().getCode() != IResourceStatus.PROJECT_NOT_OPEN) {
					TeamPlugin.log(e);
				}
			}
		} else if (resource == null) {
			// For non-resource elements, show the same propogaqted marker as the children
			IDiffElement[] children = element.getChildren();
			for (IDiffElement child : children) {
				if (child instanceof ISynchronizeModelElement) {
					ISynchronizeModelElement childElement = (ISynchronizeModelElement)child;
					if (childElement.getProperty(ISynchronizeModelElement.PROPAGATED_ERROR_MARKER_PROPERTY)) {
						property = ISynchronizeModelElement.PROPAGATED_ERROR_MARKER_PROPERTY;
						break;
					} else if (childElement.getProperty(ISynchronizeModelElement.PROPAGATED_WARNING_MARKER_PROPERTY)) {
						property = ISynchronizeModelElement.PROPAGATED_WARNING_MARKER_PROPERTY;
						// Keep going because there may be errors on other resources
					}

				}
			}
		}
		return property;
	}

	/**
	 * Return the logical model depth used for marker propagation
	 * @param resource the resource
	 * @return the depth the resources should be traversed
	 */
	protected int getLogicalModelDepth(IResource resource) {
		return IResource.DEPTH_INFINITE;
	}

	/**
	 * Update the label of the given diff node. The label for nodes queued
	 * using this method will not be updated until <code>firePendingLabelUpdates</code>
	 * is called.
	 * @param diffNode the diff node to be updated
	 */
	protected void queueForLabelUpdate(ISynchronizeModelElement diffNode) {
		updateHandler.queueForLabelUpdate(diffNode);
	}

	/**
	 * Throw away any old state associated with this provider and
	 * rebuild the model from scratch.
	 */
	protected void reset() {
		// save expansion state
		if(isRootProvider() && hasViewerState()) {
			saveViewerState();
		}

		// Clear existing model, but keep the root node
		clearModelObjects(getModelRoot());

		// Rebuild the model
		buildModelObjects(getModelRoot());

		// Notify listeners that model has changed
		ISynchronizeModelElement root = getModelRoot();
		if(root instanceof SynchronizeModelElement) {
			((SynchronizeModelElement)root).fireChanges();
		}

		if (Utils.canUpdateViewer(getViewer())) {
			// If we can update the viewer, that means that the view was updated
			// when the model was rebuilt.
			refreshModelRoot();
		} else {
			// Only refresh the view if there is now background update in
			// progress. If there is, the background update will refresh
			if (!updateHandler.isPerformingBackgroundUpdate()) {
				Utils.asyncExec((Runnable) this::refreshModelRoot, getViewer());
			}
		}
	}

	private void refreshModelRoot() {
		StructuredViewer viewer = getViewer();
		if (viewer != null && !viewer.getControl().isDisposed()) {
			try {
				viewer.getControl().setRedraw(false);
				if (isRootProvider() || getModelRoot().getParent() == null) {
					// Refresh the entire view
					viewer.refresh();
				} else {
					// Only refresh the model root bu also ensure that
					// the parents of the model root and the model root
					// itself are added to the view
					addToViewer(getModelRoot());
				}
				//	restore expansion state
				if (isRootProvider())
					restoreViewerState();
			} finally {
				viewer.getControl().setRedraw(true);
			}
		}
	}

	/**
	 * For each node create children based on the contents of
	 * @param node
	 * @return the diff elements
	 */
	protected abstract IDiffElement[] buildModelObjects(ISynchronizeModelElement node);

	/**
	 * Returns whether the viewer has state to be saved.
	 * @return whether the viewer has state to be saved
	 */
	protected abstract boolean hasViewerState();

	/*
	 * Return all the resources that are expanded in the page.
	 * This method should only be called in the UI thread
	 * after validating that the viewer is still valid.
	 */
	protected IResource[] getExpandedResources() {
		Set<IResource> expanded = new HashSet<>();
		IResource[] savedExpansionState = getCachedResources(P_VIEWER_EXPANSION_STATE);
		Collections.addAll(expanded, savedExpansionState);
		StructuredViewer viewer = getViewer();
		Object[] objects = ((AbstractTreeViewer) viewer).getVisibleExpandedElements();
		IResource[] currentExpansionState = getResources(objects);
		Collections.addAll(expanded, currentExpansionState);
		return expanded.toArray(new IResource[expanded.size()]);
	}

	/*
	 * Return all the resources that are selected in the page.
	 * This method should only be called in the UI thread
	 * after validating that the viewer is still valid.
	 */
	protected IResource[] getSelectedResources() {
		StructuredViewer viewer = getViewer();
		return getResources(viewer.getStructuredSelection().toArray());
	}

	/*
	 * Return all the resources that are checked in the page.
	 * This method should only be called in the UI thread
	 * after validating that the viewer is still valid.
	 */
	protected IResource[] getCheckedResources() {
		StructuredViewer viewer = getViewer();
		if (viewer instanceof CheckboxTreeViewer){
			return getResources(((CheckboxTreeViewer)viewer).getCheckedElements());
		}

		return new IResource[0];
	}

	/*
	 * Expand the resources if they appear in the page.
	 * This method should only be called in the UI thread
	 * after validating that the viewer is still valid.
	 */
	protected void expandResources(IResource[] resources) {
		Set<ISynchronizeModelElement> expandedElements = new HashSet<>();
		StructuredViewer viewer = getViewer();
		for (IResource resource : resources) {
			ISynchronizeModelElement[] elements = getModelObjects(resource);
			// Only expand when there is one element per resource
			if (elements.length == 1) {
				for (ISynchronizeModelElement element : elements) {
					// Add all parents of the element to the expansion set
					while (element != null) {
						expandedElements.add(element);
						element = (ISynchronizeModelElement)element.getParent();
					}
				}
			}
		}
		if (!expandedElements.isEmpty())
			((AbstractTreeViewer) viewer).setExpandedElements(expandedElements.toArray());
	}

	protected IResource[] getResources(Object[] objects) {
		Set<IResource> result = new HashSet<>();
		if (objects.length > 0) {
			for (Object object : objects) {
				if (object instanceof ISynchronizeModelElement) {
					IResource resource = ((ISynchronizeModelElement) object).getResource();
					if(resource != null)
						result.add(resource);
				}
			}
		}
		return result.toArray(new IResource[result.size()]);
	}

	private void clearResourceCache(String configProperty) {
		getConfiguration().setProperty(configProperty, null);
	}

	private void cacheResources(IResource[] resources, String configProperty) {
		if (resources.length > 0) {
			ISynchronizePageConfiguration config = getConfiguration();
			ArrayList<String> paths = new ArrayList<>();
			for (IResource resource : resources) {
				String path = resource.getFullPath().toString();
				if (resource.getType() != IResource.FILE && path.charAt(path.length() - 1) != IPath.SEPARATOR) {
					// Include a trailing slash on folders and projects.
					// It is used when recreating cached resources that don't exist locally
					path += IPath.SEPARATOR;
				}
				paths.add(path);
			}
			config.setProperty(configProperty, paths);
		} else {
			clearResourceCache(configProperty);
		}
	}

	private IResource[] getCachedResources(String configProperty) {
		List paths = (List)getConfiguration().getProperty(configProperty);
		if (paths == null)
			return new IResource[0];
		IContainer container = ResourcesPlugin.getWorkspace().getRoot();
		ArrayList<IResource> resources = new ArrayList<>();
		for (Object path2 : paths) {
			String path = (String) path2;
			IResource resource = getResourceForPath(container, path);
			if (resource != null) {
				resources.add(resource);
			}
		}
		return resources.toArray(new IResource[resources.size()]);
	}

	/**
	 * Save the viewer state (expansion and selection)
	 */
	protected void saveViewerState() {
		//	save visible expanded elements and selection
		final StructuredViewer viewer = getViewer();
		if (viewer != null && !viewer.getControl().isDisposed() && viewer instanceof AbstractTreeViewer) {
			//check to see if we should store the checked states of the tree

			final boolean storeChecks = ((SynchronizePageConfiguration)configuration).getViewerStyle() == SynchronizePageConfiguration.CHECKBOX;
			final IResource[][] expandedResources = new IResource[1][0];
			final IResource[][] selectedResources = new IResource[1][0];
			final IResource[][] checkedResources = new IResource[1][0];
			viewer.getControl().getDisplay().syncExec(() -> {
				if (viewer != null && !viewer.getControl().isDisposed()) {
					expandedResources[0] = getExpandedResources();
					selectedResources[0] = getSelectedResources();
					if (storeChecks)
						checkedResources [0] = getCheckedResources();
				}
			});

			// Save expansion and selection
			cacheResources(expandedResources[0], P_VIEWER_EXPANSION_STATE);
			cacheResources(selectedResources[0], P_VIEWER_SELECTION_STATE);
			if (storeChecks)
				cacheResources(checkedResources[0], P_VIEWER_CHECKED_STATE);
		}
	}

	/**
	 * Restore the expansion state and selection of the viewer.
	 * This method must be invoked from within the UI thread.
	 */
	protected void restoreViewerState() {
		// restore expansion state and selection state
		final StructuredViewer viewer = getViewer();
		if (viewer != null && !viewer.getControl().isDisposed() && viewer instanceof AbstractTreeViewer) {
			IResource[] resourcesToExpand = getCachedResources(P_VIEWER_EXPANSION_STATE);
			IResource[] resourcesToSelect = getCachedResources(P_VIEWER_SELECTION_STATE);
			if (((SynchronizePageConfiguration)configuration).getViewerStyle() == SynchronizePageConfiguration.CHECKBOX){
				IResource[] resourcesToCheck = getCachedResources(P_VIEWER_CHECKED_STATE);
				checkResources(resourcesToCheck);
			}
			expandResources(resourcesToExpand);
			selectResources(resourcesToSelect);
		}
	}

	/*
	 * Select the given resources in the view. This method can
	 * only be invoked from the UI thread.
	 */
	protected void selectResources(IResource[] resourcesToSelect) {
		StructuredViewer viewer = getViewer();
		final ArrayList<ISynchronizeModelElement> selectedElements = new ArrayList<>();
		for (IResource resource : resourcesToSelect) {
			ISynchronizeModelElement[] elements = getModelObjects(resource);
			// Only preserve the selection if there is one element for the resource
			if (elements.length == 1) {
				selectedElements.add(elements[0]);
			}
		}
		if (!selectedElements.isEmpty())
			viewer.setSelection(new StructuredSelection(selectedElements));
	}

	/*
	 * Check the given resources in the view. This method can
	 * only be invoked from the UI thread.
	 */
	protected void checkResources(IResource[] resourcesToCheck) {
		Set<ISynchronizeModelElement> checkedElements = new HashSet<>();
		StructuredViewer viewer = getViewer();
		if (!(viewer instanceof CheckboxTreeViewer))
			return;

		for (IResource resource : resourcesToCheck) {
			if (resource.getType() != IResource.FILE)
				continue;
			ISynchronizeModelElement[] elements = getModelObjects(resource);
			// Only expand when there is one element per resource
			if (elements.length == 1) {
				Collections.addAll(checkedElements, elements);
			}
		}
		if (!checkedElements.isEmpty())
			((CheckboxTreeViewer) viewer).setCheckedElements(checkedElements.toArray());
	}

	/*
	 * Convert a path to a resource by first looking in the resource
	 * tree and, if that fails, by using the path format to create
	 * a handle.
	 */
	private IResource getResourceForPath(IContainer container, String path) {
		IResource resource = container.findMember(path, true /* include phantoms */);
		if (resource == null) {
			try {
				// The resource doesn't have an entry on the resources tree
				// but may still appear in the view so try to deduce the type
				// from the path
				if (path.endsWith(Character.toString(IPath.SEPARATOR))) {
					resource = container.getFolder(new Path(null, path));
				} else {
					resource = container.getFile(new Path(null, path));
				}
			} catch (IllegalArgumentException e) {
				// Couldn't get a resource handle so ignore
			}
		}
		return resource;
	}

	@Override
	public void treeCollapsed(TreeEvent e) {
		clearResourceCache(P_VIEWER_EXPANSION_STATE);
	}

	@Override
	public void treeExpanded(TreeEvent e) {
		clearResourceCache(P_VIEWER_EXPANSION_STATE);
	}

	/**
	 * Return all the model objects in this provider that represent the given resource
	 * @param resource the resource
	 * @return the model objects for the resource
	 */
	protected abstract ISynchronizeModelElement[] getModelObjects(IResource resource);

	@Override
	public void saveState() {
		saveViewerState();
	}

	/**
	 * Method invoked when a sync element is added or removed or its state changes.
	 * This method can be invoked from the UI thread or a background thread.
	 * @param element synchronize element
	 * @param clear <code>true</code> if the conflict bit of the element was cleared
	 * (i.e. the element has been deleted)
	 */
	protected void propogateConflictState(ISynchronizeModelElement element, boolean clear) {
		boolean isConflict = clear ? false : isConflicting(element);
		boolean wasConflict = element.getProperty(ISynchronizeModelElement.PROPAGATED_CONFLICT_PROPERTY);
		// Only propagate and update parent labels if the state of the element has changed
		if (isConflict != wasConflict) {
			element.setPropertyToRoot(ISynchronizeModelElement.PROPAGATED_CONFLICT_PROPERTY, isConflict);
			updateHandler.updateParentLabels(element);
		}
	}

	/**
	 * Return whether the given model element represents a conflict.
	 * @param element the element being tested
	 * @return whether the element is a conflict
	 */
	protected boolean isConflicting(ISynchronizeModelElement element) {
		return (element.getKind() & SyncInfo.DIRECTION_MASK) == SyncInfo.CONFLICTING;
	}

	/**
	 * Dispose of the provider
	 */
	@Override
	public void dispose() {
		// Only dispose the update handler if it is
		// directly associated with this provider
		if (isRootProvider()) {
			updateHandler.dispose();
			getTree().removeTreeListener(this);
		} else {
			set.removeSyncSetChangedListener(this);
		}
		if (actionGroup != null) {
			Utils.syncExec((Runnable) () -> actionGroup.dispose(), getViewer());
		}
		this.disposed = true;
	}

	private boolean isRootProvider() {
		return updateHandler.getProvider() == this;
	}

	/**
	 * Return whether this provide has been disposed.
	 * @return whether this provide has been disposed
	 */
	public boolean isDisposed() {
		return disposed;
	}

	/**
	 * Return the closest parent elements that represents a model element that
	 * could contains the given resource. Multiple elements need only be returned
	 * if two or more logical views are being shown and each view has an element
	 * that could contain the resource.
	 * @param resource the resource
	 * @return one or more lowest level parents that could contain the resource
	 */
	public abstract ISynchronizeModelElement[] getClosestExistingParents(IResource resource);

	/**
	 * Handle the changes made to the viewer's <code>SyncInfoSet</code>.
	 * This method delegates the changes to the three methods <code>handleResourceChanges(ISyncInfoSetChangeEvent)</code>,
	 * <code>handleResourceRemovals(ISyncInfoSetChangeEvent)</code> and
	 * <code>handleResourceAdditions(ISyncInfoSetChangeEvent)</code>.
	 * @param event
	 *            the event containing the changed resources.
	 */
	protected void handleChanges(ISyncInfoTreeChangeEvent event, IProgressMonitor monitor) {
		handleResourceChanges(event);
		handleResourceRemovals(event);
		handleResourceAdditions(event);
	}

	/**
	 * Update the viewer for the sync set additions in the provided event. This
	 * method is invoked by <code>handleChanges(ISyncInfoSetChangeEvent)</code>.
	 * Subclasses may override.
	 * @param event
	 */
	protected abstract void handleResourceAdditions(ISyncInfoTreeChangeEvent event);

	/**
	 * Update the viewer for the sync set changes in the provided event. This
	 * method is invoked by <code>handleChanges(ISyncInfoSetChangeEvent)</code>.
	 * Subclasses may override.
	 * @param event
	 */
	protected abstract void handleResourceChanges(ISyncInfoTreeChangeEvent event);

	/**
	 * Update the viewer for the sync set removals in the provided event. This
	 * method is invoked by <code>handleChanges(ISyncInfoSetChangeEvent)</code>.
	 * Subclasses may override.
	 * @param event
	 */
	protected abstract void handleResourceRemovals(ISyncInfoTreeChangeEvent event);

	@Override
	public void syncInfoChanged(final ISyncInfoSetChangeEvent event, final IProgressMonitor monitor) {
		if (! (event instanceof ISyncInfoTreeChangeEvent)) {
			reset();
		} else {
			updateHandler.runViewUpdate(() -> handleChanges((ISyncInfoTreeChangeEvent)event, monitor), true /* preserve expansion */);
		}
	}

	@Override
	public void syncInfoSetErrors(SyncInfoSet set, ITeamStatus[] errors, IProgressMonitor monitor) {
		// Not handled

	}

	@Override
	public void syncInfoSetReset(SyncInfoSet set, IProgressMonitor monitor) {
		reset();
	}

	protected void addToViewer(ISynchronizeModelElement node) {
		if (DEBUG) {
			System.out.println("Adding model element " + node.getName()); //$NON-NLS-1$
		}
		propogateConflictState(node, false);
		// Set the marker property on this node.
		// There is no need to propagate this to the parents
		// as they will be displaying the proper marker already
		String property = calculateProblemMarker(node);
		if (property != null) {
			node.setProperty(property, true);
			// Parent resource nodes would have been properly calculated when they were added.
			// However, non-resource nodes would not so we need to propagate the marker to them
			propogateMarkerPropertyToParent(node, property);
		}
		if (Utils.canUpdateViewer(getViewer())) {
			doAdd((SynchronizeModelElement)node.getParent(), node);
		}
		updateHandler.nodeAdded(node, this);
	}

	/*
	 * Propagate the marker property to the parent if it is not already there.
	 * Only propagate warnings if the parent isn't an error already.
	 */
	private void propogateMarkerPropertyToParent(ISynchronizeModelElement node, String property) {
		ISynchronizeModelElement parent = (ISynchronizeModelElement)node.getParent();
		if (parent != null
				&& !parent.getProperty(property)
				&& !parent.getProperty(ISynchronizeModelElement.PROPAGATED_ERROR_MARKER_PROPERTY)) {
			parent.setProperty(property, true);
			propogateMarkerPropertyToParent(parent, property);
		}
	}

	/**
	 * Remove any traces of the model element and any of it's descendants in the
	 * hierarchy defined by the content provider from the content provider and
	 * the viewer it is associated with.
	 * @param nodes the model elements to remove
	 */
	protected void removeFromViewer(ISynchronizeModelElement[] nodes) {
		List<ISynchronizeModelElement> rootsToClear = new ArrayList<>();
		for (ISynchronizeModelElement node : nodes) {
			if (DEBUG) {
				System.out.println("Removing model element " + node.getName()); //$NON-NLS-1$
			}
			ISynchronizeModelElement rootToClear= getRootToClear(node);
			if (DEBUG) {
				if (rootToClear != node) {
					System.out.println("Removing parent element " + rootToClear.getName()); //$NON-NLS-1$
				}
			}
			propogateConflictState(rootToClear, true /* clear the conflict */);
			clearModelObjects(rootToClear);
			rootsToClear.add(rootToClear);
		}
		ISynchronizeModelElement[] roots = rootsToClear.toArray(new ISynchronizeModelElement[rootsToClear.size()]);
		if (Utils.canUpdateViewer(getViewer())) {
			doRemove(roots);
		}
		for (ISynchronizeModelElement element : roots) {
			updateHandler.nodeRemoved(element, this);
		}
	}

	/**
	 * Clear the model objects from the diff tree, cleaning up any cached state
	 * (such as resource to model object map). This method recurses deeply on
	 * the tree to allow the cleanup of any cached state for the children as
	 * well.
	 * @param node the root node
	 */
	protected final void clearModelObjects(ISynchronizeModelElement node) {
		// When clearing model objects, any parents of the node
		// That are not out-of-sync, not the model root and that would
		// be empty as a result of this clear, should also be cleared.
		ISynchronizeModelElement rootToClear = getRootToClear(node);
		// Recursively clear the nodes from the root
		recursiveClearModelObjects(rootToClear);
		if (node == getModelRoot()) {
			IDiffElement[] children = node.getChildren();
			for (IDiffElement element : children) {
				((SynchronizeModelElement)node).remove(element);
			}
		} else {
			SynchronizeModelElement parent = ((SynchronizeModelElement)node.getParent());
			if (parent != null) parent.remove(node);
		}
	}

	/**
	 * Method that subclasses can override when clearing model objects.
	 * @param node the node to be cleared recursively
	 */
	protected void recursiveClearModelObjects(ISynchronizeModelElement node) {
		// Clear all the children of the node
		IDiffElement[] children = node.getChildren();
		for (IDiffElement element : children) {
			if (element instanceof ISynchronizeModelElement) {
				ISynchronizeModelElement sme = (ISynchronizeModelElement) element;
				ISynchronizeModelProvider provider = getProvider(sme);
				if (provider != null && provider instanceof AbstractSynchronizeModelProvider) {
					((AbstractSynchronizeModelProvider)provider).recursiveClearModelObjects(sme);
				} else {
					recursiveClearModelObjects(sme);
				}
			}
		}
		// Notify the update handler that the node has been cleared
		if (node != getModelRoot())
			updateHandler.modelObjectCleared(node);
	}

	/*
	 * Remove to root should only remove to the root of the provider and not the
	 * diff tree.
	 */
	private ISynchronizeModelElement getRootToClear(ISynchronizeModelElement node) {
		if (node == getModelRoot()) return node;
		ISynchronizeModelElement parent = (ISynchronizeModelElement)node.getParent();
		if (parent != null && parent != getModelRoot() && !isOutOfSync(parent) && parent.getChildren().length == 1) {
			return getRootToClear(parent);
		}
		return node;
	}

	/*
	 * Return whether the node represents an out-of-sync resource.
	 */
	protected boolean isOutOfSync(ISynchronizeModelElement node) {
		SyncInfo info = Utils.getSyncInfo(node);
		return (info != null && info.getKind() != SyncInfo.IN_SYNC);
	}

	protected boolean isOutOfSync(IResource resource) {
		SyncInfo info = getSyncInfoSet().getSyncInfo(resource);
		return (info != null && info.getKind() != SyncInfo.IN_SYNC);
	}

	/**
	 * Return the provider that created and manages the given
	 * model element. The default is to return the receiver.
	 * Subclasses may override.
	 * @param element the synchronize model element
	 * @return the provider that created the element
	 */
	protected ISynchronizeModelProvider getProvider(ISynchronizeModelElement element) {
		return this;
	}

	/**
	 * Add the element to the viewer.
	 * @param parent the parent of the element which is already added to the viewer
	 * @param element the element to be added to the viewer
	 */
	protected void doAdd(ISynchronizeModelElement parent, ISynchronizeModelElement element) {
		updateHandler.doAdd(parent, element);
	}

	/**
	 * Remove the element from the viewer
	 * @param elements the elements to be removed
	 */
	protected void doRemove(ISynchronizeModelElement[] elements) {
		AbstractTreeViewer viewer = (AbstractTreeViewer)getViewer();
		try {
			viewer.remove((Object[]) elements);
		} catch (SWTException e) {
			// The remove failed due to an SWT exception. Log it and continue
			TeamUIPlugin.log(IStatus.ERROR, "An error occurred removing elements from the synchronize view", e); //$NON-NLS-1$
		}
		if (DEBUG) {
			for (ISynchronizeModelElement element : elements) {
				System.out.println("Removing view item " + element.getName()); //$NON-NLS-1$
			}
		}
	}

	/**
	 * This is a callback from the model update handler that gets invoked
	 * when a node is added to the viewer. It is only invoked for the
	 * root level model provider.
	 * @param node
	 * @param provider the provider that added the node
	 */
	protected void nodeAdded(ISynchronizeModelElement node, AbstractSynchronizeModelProvider provider) {
		// Default is to do nothing
	}

	/**
	 * This is a callback from the model update handler that gets invoked
	 * when a node is removed from the viewer. It is only invoked for the
	 * root level model provider. A removed node may have children for
	 * which a <code>nodeRemoved</code> callback is not received (see
	 * <code>modelObjectCleared</code>).
	 * @param node
	 */
	protected void nodeRemoved(ISynchronizeModelElement node, AbstractSynchronizeModelProvider provider) {
		// Default is to do nothing
	}

	/**
	 * This is a callback from the model update handler that gets invoked
	 * when a node is cleared from the model. It is only invoked for the
	 * root level model provider. This callback is deep in the sense that
	 * a callback is sent for each node that is cleared.
	 * @param node the node that was cleared.
	 */
	public void modelObjectCleared(ISynchronizeModelElement node) {
		// Default is to do nothing
	}

	@Override
	public void addPropertyChangeListener(IPropertyChangeListener listener) {
		synchronized (this) {
			if (listeners == null) {
				listeners = new ListenerList<>(ListenerList.IDENTITY);
			}
			listeners.add(listener);
		}

	}
	@Override
	public void removePropertyChangeListener(IPropertyChangeListener listener) {
		if (listeners != null) {
			synchronized (this) {
				listeners.remove(listener);
				if (listeners.isEmpty()) {
					listeners = null;
				}
			}
		}
	}

	protected void firePropertyChange(String key, Object oldValue, Object newValue) {
		Object[] allListeners;
		synchronized(this) {
			allListeners = listeners.getListeners();
		}
		final PropertyChangeEvent event = new PropertyChangeEvent(this, key, oldValue, newValue);
		for (Object l : allListeners) {
			final IPropertyChangeListener listener = (IPropertyChangeListener) l;
			SafeRunner.run(new ISafeRunnable() {
				@Override
				public void handleException(Throwable exception) {
					// Error is logged by platform
				}
				@Override
				public void run() throws Exception {
					listener.propertyChange(event);
				}
			});
		}
	}

	/**
	 * Wait until the provider is done processing any events and
	 * the page content are up-to-date.
	 * This method is for testing purposes only.
	 * @param monitor
	 */
	public void waitUntilDone(IProgressMonitor monitor) {
		monitor.worked(1);
		// wait for the event handler to process changes.
		while(updateHandler.getEventHandlerJob().getState() != Job.NONE) {
			monitor.worked(1);
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
			}
			Policy.checkCanceled(monitor);
		}
		monitor.worked(1);
	}

	@Override
	public String toString() {
		ISynchronizeModelElement element = getModelRoot();
		String name = getClass().getName();
		int index = name.lastIndexOf("."); //$NON-NLS-1$
		if (index != -1) {
			name = name.substring(index + 1);
		}
		String name2 = element.getName();
		if (name2.length() == 0) {
			name2 = "/"; //$NON-NLS-1$
		}
		return name + ": " + name2; //$NON-NLS-1$
	}

	/**
	 * Execute a runnable which performs an update of the model being displayed
	 * by this provider. The runnable should be executed in a thread-safe manner
	 * which results in the view being updated.
	 * @param runnable the runnable which updates the model.
	 * @param preserveExpansion whether the expansion of the view should be preserver
	 * @param runInUIThread
	 */
	public void performUpdate(IWorkspaceRunnable runnable, boolean preserveExpansion, boolean runInUIThread) {
		updateHandler.performUpdate(runnable, preserveExpansion, runInUIThread);
	}
}
