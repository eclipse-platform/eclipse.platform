/*******************************************************************************
 * Copyright (c) 2002, 2015 IBM Corporation and others.
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
import org.eclipse.core.tools.CopyStructuredSelectionAction;
import org.eclipse.core.tools.SpyView;
import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;

/**
 * Project Spy view. This view shows detailed information about the currently
 * selected resource in any other view in the platform. For
 * details on the information being presented, see <code>ProjectContentProvider
 * </code> documentation (link below).
 *
 * @see org.eclipse.core.tools.resources.ProjectContentProvider
 */
@SuppressWarnings("restriction")
public class ProjectView extends SpyView {

	/** JFace's tree component used to present project details. */
	protected AbstractTreeViewer viewer;

	/**
	 * Our listener to selection changes. Every time a new resource is
	 * selected, this view gets updated.
	 */
	private ISelectionListener selectionListener;

	/** The content provider for this view's TreeViewer. */
	private ProjectContentProvider contentProvider;

	/**
	 * Our listener to resource changes. Every time the current selected
	 * project is changed, this view gets updated.
	 */
	private IResourceChangeListener resourceChangeListener;

	/**
	 * Constructs a project view object, registering a resource change listener.
	 */
	public ProjectView() {
		addResourceChangeListener();
	}

	/**
	 * Creates this view widgets and actions.
	 *
	 * @param parent the parent control
	 * @see IWorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		viewer = new TreeViewer(parent);
		contentProvider = new ProjectContentProvider();
		viewer.setContentProvider(contentProvider);

		addSelectionListener();
		createActions();
	}

	/**
	 * Creates and publishes this view's actions.
	 */
	private void createActions() {
		final IAction copyAction = new CopyStructuredSelectionAction(viewer);

		final MenuManager menuMgr = new MenuManager();
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(manager -> {
			manager.add(copyAction);
			// Other plug-ins can contribute their actions here
			manager.add(new Separator("Additions")); //$NON-NLS-1$
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
	}

	/**
	 * Process the given selection. If it is not a structured selection, the event
	 * is discarded. If the selection is a resource (or has a resource),
	 * updates the resource view. Otherwise, the resource view is cleaned out.
	 *
	 * @param selection the selection object to be processed
	 */
	protected void processSelection(ISelection selection) {
		// if it is not a strucutured selection, ignore
		if (!(selection instanceof IStructuredSelection)) {
			return;
		}
		IResource resource = null;

		IStructuredSelection structuredSel = (IStructuredSelection) selection;
		if (!structuredSel.isEmpty()) {
			Object item = ((IStructuredSelection) selection).getFirstElement();

			if (item instanceof IResource) {
				resource = (IResource) item;
			} else if (item instanceof IAdaptable) {
				resource = ((IAdaptable) item).getAdapter(IResource.class);
			}
		}

		// loads a new resource (or cleans the resource view, if resource == null)
		loadResource(resource);

	}

	/**
	 * Creates a selection listener that will be notified any time a selection has
	 * changed in any place.
	 *
	 * @see #processSelection(ISelection)
	 */
	private void addSelectionListener() {
		ISelectionService selectionService = getSite().getPage().getWorkbenchWindow().getSelectionService();
		selectionListener = (part, sel) -> processSelection(sel);
		selectionService.addSelectionListener(selectionListener);
		processSelection(selectionService.getSelection());
	}

	/**
	 * Loads a resource to be shown on this project view (or cleans it,
	 * if resource == null).
	 * This method must be run in the SWT thread.
	 *
	 * @param resource the resource to be shown
	 * @see ProjectContentProvider#inputChanged(Viewer, Object, Object)
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
		ISelectionService selectionService = getSite().getPage().getWorkbenchWindow().getSelectionService();
		selectionService.removeSelectionListener(selectionListener);

		ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceChangeListener);
	}

	/**
	 * Creates a resource change listener so we can know if the currently
	 * selected resource has changed.
	 */
	private void addResourceChangeListener() {
		resourceChangeListener = new SelectedResourceChangeListener();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceChangeListener, IResourceChangeEvent.POST_CHANGE);
	}

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
			// looks for a delta for the currently loaded resource's project
			IResourceDelta rootDelta = event.getDelta();
			IResourceDelta projectDelta = rootDelta.findMember(currentResource.getProject().getFullPath());

			// if there is no delta, ignore
			if (projectDelta == null) {
				return;
			}
			// rebuild the project view contents with the new state
			Display.getDefault().asyncExec(() -> loadResource(currentResource));
		}
	}

}
