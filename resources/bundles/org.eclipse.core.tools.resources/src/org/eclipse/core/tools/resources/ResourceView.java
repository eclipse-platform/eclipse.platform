/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tools.resources;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.tools.*;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;

/**
 * Resource Spy view. This view shows detailed information about the currently
 * selected resource in any other view in the platform. For
 * details on the information being presented, see <code>ResourceContentProvider
 * </code> documentation (link below).
 *
 * @see org.eclipse.core.tools.resources.ResourceContentProvider
 */
@SuppressWarnings("restriction")
public class ResourceView extends SpyView {
	/** JFace's tree component used to present resource details. */
	protected AbstractTreeViewer viewer;

	/**
	 * Our listener to selection changes. Every time a new resource is
	 * selected, this view gets updated.
	 */
	private ISelectionListener selectionListener;

	/**
	 * Our listener to resource changes. Every time the current selected
	 * resource is changed, this view gets updated.
	 */
	private IResourceChangeListener resourceChangeListener;

	/** The content provider for this view's TreeViewer. */
	private ResourceContentProvider contentProvider;

	/**
	 * Constructs a resource view object, registering a resource change listener.
	 */
	public ResourceView() {
		addResourceChangeListener();
	}

	/**
	 * Creates the SWT controls for the resource view.
	 *
	 * @param parent the parent control
	 * @see IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {

		viewer = new TreeViewer(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		contentProvider = new ResourceContentProvider();
		viewer.setContentProvider(contentProvider);
		addSelectionListener();
		createActions();

	}

	/**
	 * Creates and publishes this view's actions.
	 */
	private void createActions() {
		IActionBars actionBars = this.getViewSite().getActionBars();

		final GlobalAction copyAction = new CopyStructuredSelectionAction(new TreeSelectionProviderDecorator(viewer));
		copyAction.registerAsGlobalAction(actionBars);

		final GlobalAction selectAllAction = new SelectAllAction(new TreeTextOperationTarget((Tree) viewer.getControl()));
		selectAllAction.registerAsGlobalAction(actionBars);

		actionBars.updateActionBars();

		final MenuManager menuMgr = new MenuManager();
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(manager -> manager.add(copyAction));
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
	}

	/**
	 * Process the given selection. If it is not an structured selection, the event
	 * is discarded. If the selection is a resource (or maps to a resource),
	 * updates the resource view. Otherwise, the resource view is cleaned out.
	 *
	 * @param sel the selection object to be processed
	 */
	protected void processSelection(ISelection sel) {
		// if it is not a strucutured selection, ignore
		if (!(sel instanceof IStructuredSelection)) {
			return;
		}
		IResource resource = null;

		IStructuredSelection structuredSel = (IStructuredSelection) sel;

		if (!structuredSel.isEmpty()) {
			Object item = ((IStructuredSelection) sel).getFirstElement();

			if (item instanceof IResource) {
				resource = (IResource) item;
			} else if (item instanceof IAdaptable) {
				resource = ((IAdaptable) item).getAdapter(IResource.class);
			}
		}
		// loads the selected resource
		if (resource != null) {
			loadResource(resource);
		}
	}

	/**
	 * Creates a selection listener that will call
	 * <code>processSelection(ISelection)</code> everytime a selection has
	 * changed in any place.
	 *
	 * @see #processSelection(ISelection)
	 */
	private void addSelectionListener() {
		ISelectionService selectionService = getSite().getPage().getWorkbenchWindow().getSelectionService();

		// creates a selection listener that ignores who generated the event
		selectionListener = (part, sel) -> processSelection(sel);

		selectionService.addSelectionListener(selectionListener);

		// forces a call to processSelection() to recognize any existing selection
		processSelection(selectionService.getSelection());

	}

	/**
	 * Creates a resource change listener so we can know if the currently selected
	 * resource has changed or a marker has been added to or removed from it.
	 */
	private void addResourceChangeListener() {
		resourceChangeListener = new SelectedResourceChangeListener();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceChangeListener, IResourceChangeEvent.POST_CHANGE);
	}

	/**
	 * Loads a resource to be shown on this resource view (or cleans it, if
	 * resource == null). This method must be run in the SWT thread.
	 *
	 * @param resource the resource to be shown on this view
	 * @see ResourceContentProvider#inputChanged(Viewer, Object, Object)
	 */
	public void loadResource(IResource resource) {

		if (viewer.getControl().isDisposed()) {
			return;
		}
		// turn redraw off so the UI will reflect changes only after we are done
		viewer.getControl().setRedraw(false);

		// fires viewer update
		viewer.setInput(resource);

		// shows all nodes in the resource tree
		viewer.expandAll();

		// we are done, turn redraw on
		viewer.getControl().setRedraw(true);
	}

	/**
	 * Removes all listeners added.
	 *
	 * @see org.eclipse.ui.IWorkbenchPart#dispose()
	 */
	@Override
	public void dispose() {
		super.dispose();
		removeSelectionListener();
		removeResourceChangeListener();
	}

	/**
	 * Removes the resource change listener added by this view.
	 */
	private void removeResourceChangeListener() {
		if (resourceChangeListener == null) {
			return;
		}
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangeListener);

		resourceChangeListener = null;
	}

	/**
	 * Removes the selection listener added by this view.
	 */
	private void removeSelectionListener() {
		if (selectionListener == null) {
			return;
		}
		ISelectionService selectionService = getSite().getPage().getWorkbenchWindow().getSelectionService();

		selectionService.removeSelectionListener(selectionListener);

		selectionListener = null;
	}

	/**
	 * A resource change listener that is interested on changes in the resource
	 * being currently shown.
	 */
	private class SelectedResourceChangeListener implements IResourceChangeListener {
		public SelectedResourceChangeListener() {
		}

		@Override
		public void resourceChanged(IResourceChangeEvent event) {

			final IResource currentResource = (IResource) viewer.getInput();

			// if we don't have a resource currently loaded, ignore the event
			if (event.getType() != IResourceChangeEvent.POST_CHANGE || currentResource == null) {
				return;
			}
			// looks for a delta for the currently loaded resource
			IResourceDelta rootDelta = event.getDelta();
			IResourceDelta resourceDelta = rootDelta.findMember(currentResource.getFullPath());

			// if there is a delta, something has changed
			if (resourceDelta != null) {
				// so rebuild the resource view contents with the new state
				Display.getDefault().asyncExec(() -> loadResource(currentResource));
			}
		}
	}

}
