/*******************************************************************************
 * Copyright (c) 2002, 2018 IBM Corporation and others.
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

import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.tools.ErrorUtil;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.*;

/**
 * This action pops-up a <code>MessageDialog</code> showing the number of
 * resources (and their child resources) currently selected.
 */
@SuppressWarnings("restriction")
public class CountResourcesAction implements IWorkbenchWindowActionDelegate {

	/** A reference to the window where this action will work in.*/
	private IWorkbenchWindow window;

	/**
	 * Executes this action, popping up a <code>MessageDialog</code> showing the
	 * number of resources (and their child resources) currently selected.
	 *
	 * @param action the action proxy that handles the presentation portion of the
	 *   action
	 * @see org.eclipse.jface.action.Action#run()
	 */
	@Override
	public void run(IAction action) {
		List<IResource> resources = getSelectedResources();
		try {
			int count = countResources(resources);
			showResourcesCount(resources, count);
		} catch (CoreException ce) {
			ErrorUtil.logException(ce, "Error counting resources."); //$NON-NLS-1$
			ErrorUtil.showErrorMessage(ce.toString(), "Error counting resources"); //$NON-NLS-1$
		}
	}

	/**
	 * Returns a list of resources currently selected. If no resource is currently
	 * selected, returns a list containing the workspace root.
	 *
	 * @return a list of resources
	 */
	private List<IResource> getSelectedResources() {

		List<IResource> resources = new LinkedList<>();

		ISelectionService selectionService = window.getSelectionService();
		ISelection selection = selectionService.getSelection();

		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			for (Object element : structuredSelection) {
				IResource resource = convertToResource(element);
				if (resource != null) {
					resources.add(resource);
				}
			}
			eliminateRedundancies(resources);
		}

		// if no resources were selected, add the workspace root to the list
		if (resources.size() == 0) {
			resources.add(ResourcesPlugin.getWorkspace().getRoot());
		}

		return resources;
	}

	/**
	 * Pops-up a <code>MessageDialog</code> showing the number of resources (and
	 * their child resources) currently selected.
	 *
	 * @param resources a list containing all resources used as a starting point in
	 * the search
	 * @param count the number of resources found
	 */

	private void showResourcesCount(List<IResource> resources, int count) {
		StringBuilder message = new StringBuilder();
		message.append("Number of resources visited: "); //$NON-NLS-1$
		message.append(count);
		message.append("\nStarting point(s): \n"); //$NON-NLS-1$
		for (IResource resource : resources) {
			message.append('\t');
			message.append(resource.getFullPath());
			message.append('\n');
		}
		MessageDialog.openInformation(window.getShell(), "Resource counting", message.toString()); //$NON-NLS-1$
	}

	/**
	 * Helper method that converts an object to the <code>IResource</code>
	 * interface. The conversion is a bare cast operation (if the object is instance
	 * of <code>IResource</code>, or an adaptation (if the object is instance of
	 * <code>IAdaptable</code>).
	 *
	 * @param object the object to be cast to <code>IResource</code>
	 * @return a reference to an IResource corresponding to the object provided, or
	 * null if it is not possible to convert the provided object to
	 * <code>IResource</code>.
	 */
	private IResource convertToResource(Object object) {

		if (object instanceof IResource) {
			return (IResource) object;
		}

		if (object instanceof IAdaptable) {
			IAdaptable adaptable = (IAdaptable) object;
			return adaptable.getAdapter(IResource.class);
		}

		return null;
	}

	/**
	 * Counts the number of resources (and its sub-resources) in the list of
	 * resources provided.
	 *
	 * @param resources a <code>List</code> object containing resource objects
	 * @return the number of resources found
	 * @throws CoreException if a visited resource does not exist
	 * @see IResource#accept(org.eclipse.core.resources.IResourceVisitor)
	 */
	private int countResources(List<IResource> resources) throws CoreException {

		ResourceCounterVisitor counter = new ResourceCounterVisitor();

		for (IResource resource : resources) {
			resource.accept(counter, IResource.DEPTH_INFINITE, IContainer.INCLUDE_PHANTOMS | IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS | IContainer.INCLUDE_HIDDEN);
		}

		return counter.count;
	}

	/**
	 * A helper class that implements <code>IResourceVisitor</code>. Visits each
	 * resource in a resource hierarchy, counting them.
	 */
	static class ResourceCounterVisitor implements IResourceVisitor {
		protected int count;

		/**
		 * @see org.eclipse.core.resources.IResourceVisitor#visit(org.eclipse.core.resources.IResource)
		 */
		@Override
		public boolean visit(IResource resource) {
			count++;
			return true;
		}
	}

	/**
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#dispose()
	 */
	@Override
	public void dispose() {
		// do nothing
	}

	/**
	 * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
	 */
	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		// do nothing
	}

	/**
	 * Initializes this action delegate with the workbench window it will work in.
	 *
	 * @param workbenchWindow the window that provides the context for this delegate
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.IWorkbenchWindow)
	 */
	@Override
	public void init(IWorkbenchWindow workbenchWindow) {
		this.window = workbenchWindow;
	}

	/**
	 * Eliminates from the resource list provided any redundant resources.
	 * Redundant resources are resources that have any other resources in the list
	 * as ancestors.
	 *
	 * @param resourcesList a <code>List</code> object containing resource objects.
	 */
	private void eliminateRedundancies(List<IResource> resourcesList) {
		if (resourcesList.size() <= 1) {
			return;
		}

		//	we sort the resources list by path so it is easier to check for redundancies
		resourcesList.sort((resource1, resource2) -> resource1.getFullPath().toString().compareTo(resource2.getFullPath().toString()));

		// We iterate through the list removing any resource which is descendant
		// from any resource previously visited
		Iterator<IResource> resourcesIter = resourcesList.iterator();
		IResource last = resourcesIter.next();
		while (resourcesIter.hasNext()) {
			IResource current = resourcesIter.next();
			if (last.getFullPath().isPrefixOf(current.getFullPath())) {
				resourcesIter.remove();
			} else {
				last = current;
			}
		}
	}
}
