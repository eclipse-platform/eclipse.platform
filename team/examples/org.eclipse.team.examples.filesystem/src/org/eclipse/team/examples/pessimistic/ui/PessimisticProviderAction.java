/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
package org.eclipse.team.examples.pessimistic.ui;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.examples.pessimistic.PessimisticFilesystemProvider;
import org.eclipse.team.examples.pessimistic.PessimisticFilesystemProviderPlugin;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;


/**
 * Abstract base action implementation for all pessimistic provider actions.
 * Provides convenience methods an abstractions.
 */
public abstract class PessimisticProviderAction
implements IObjectActionDelegate {

	/*
	 * The current selection.
	 */
	protected ISelection fSelection;
	/*
	 * The current shell.
	 */
	protected Shell fShell;

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		fSelection = selection;

		boolean enabled= action.isEnabled();
		if (enabled != checkEnablement()) {
			action.setEnabled(!enabled);
		}
	}

	@Override
	public void setActivePart(IAction action, IWorkbenchPart part) {
		fShell= part.getSite().getShell();
	}

	/**
	 * Answers <code>true</code> if this action should be enabled
	 * for the given <code>resource</code>.
	 */
	protected abstract boolean shouldEnableFor(IResource resource);

	/*
	 * Checks to see if this action should be enabled.
	 */
	protected boolean checkEnablement() {
		IResource[] resources= getSelectedResources();
		if (resources == null || resources.length == 0) {
			return false;
		}
		boolean enabled= false;
		for(int i= 0; !enabled && i < resources.length; i++) {
			if (shouldEnableFor(resources[i])) {
				enabled= true;
			}
		}
		return enabled;
	}

	/**
	 * Convenience method to get an array of resources from the selection.
	 */
	protected IResource[] getSelectedResources() {
		ArrayList<IResource> resources = null;
		if (!fSelection.isEmpty()) {
			resources = new ArrayList<>();
			Iterator elements = ((IStructuredSelection) fSelection).iterator();
			while (elements.hasNext()) {
				Object next = elements.next();
				if (next instanceof IResource) {
					resources.add((IResource) next);
					continue;
				}
				if (next instanceof IAdaptable) {
					IAdaptable a = (IAdaptable) next;
					Object adapter = a.getAdapter(IResource.class);
					if (adapter instanceof IResource) {
						resources.add((IResource) adapter);
						continue;
					}
				}
			}
		}
		if (resources != null && !resources.isEmpty()) {
			IResource[] result = new IResource[resources.size()];
			resources.toArray(result);
			return result;
		}
		return new IResource[0];
	}

	/**
	 * Convenience method which answers <code>true</code> if the
	 * resource is controlled by a <code>PessimisticFilesystemProvider</code>.
	 */
	protected boolean isControlled(IResource resource) {
		PessimisticFilesystemProvider provider= getProvider(resource);
		if (provider == null)
			return false;
		return provider.isControlled(resource);
	}

	/**
	 * Convenience method which answers <code>true</code> if and only if the
	 * resource is controlled by a <code>PessimisticFilesystemProvider</code>
	 * and is checked out.
	 */
	protected boolean isCheckedOut(IResource resource) {
		PessimisticFilesystemProvider provider= getProvider(resource);
		if (provider == null)
			return false;
		return provider.isCheckedout(resource);
	}

	/**
	 * Convenience method which answers <code>true</code> if and only if the
	 * resource is controlled by a <code>PessimisticFilesystemProvider</code>
	 * and the resource is ignored.
	 */
	protected boolean isIgnored(IResource resource) {
		PessimisticFilesystemProvider provider= getProvider(resource);
		if (provider == null)
			return false;
		return provider.isIgnored(resource);
	}

	/**
	 * Convenience method which answers the <code>PessimisticFilesystemProvider</code>
	 * for the given <code>resource</code> or <code>null</code> if the
	 * <code>resource</code> is not associated with a <code>PessimisticFilesystemProvider</code>.
	 */
	protected PessimisticFilesystemProvider getProvider(IResource resource) {
		if (resource == null) {
			return null;
		}
		IProject project= resource.getProject();
		if (project == null) {
			return null;
		}
		return (PessimisticFilesystemProvider)RepositoryProvider.getProvider(project, PessimisticFilesystemProviderPlugin.NATURE_ID);
	}

	/**
	 * Convenience method which walks a resource tree and collects the
	 * resources that this action would enable for.
	 */
	protected void recursivelyAdd(IResource resource, Set<IResource> resources) {
		if (isControlled(resource) && !isIgnored(resource)) {
			if (shouldEnableFor(resource)) {
				resources.add(resource);
			}

			if (resource instanceof IContainer) {
				IContainer container = (IContainer) resource;
				IResource[] members= null;
				try {
					members = container.members();
				} catch (CoreException e) {
					PessimisticFilesystemProviderPlugin.getInstance().logError(e, "Exception traversing members");
				}
				if (members != null) {
					for (IResource member : members) {
						recursivelyAdd(member, resources);
					}
				}
			}
		}
	}

	/**
	 * Convenience method which sorts the given <code>resources</code>
	 * into a map of IProject -> Set of IResource objects.
	 */
	protected Map<IProject, Set<IResource>> sortByProject(Set<IResource> resources) {
		Map<IProject, Set<IResource>> byProject = new HashMap<>();
		if (resources != null) {
			for (IResource resource : resources) {
				IProject project= resource.getProject();
				Set<IResource> set = byProject.get(project);
				if (set == null) {
					set = new HashSet<>(1);
					byProject.put(project, set);
				}
				set.add(resource);
			}
		}
		return byProject;
	}

	/**
	 * Convenience method for displaying runnable progress
	 * with a <code>ProgressMonitorDialog</code>.
	 */
	protected void runWithProgressDialog(IRunnableWithProgress runnable) {
		try {
			new ProgressMonitorDialog(fShell).run(true, false, runnable);
		} catch (InvocationTargetException e) {
			PessimisticFilesystemProviderPlugin.getInstance().logError(e, "Problems running action " + this);
		} catch (InterruptedException e) {
			PessimisticFilesystemProviderPlugin.getInstance().logError(e, "Problems running action " + this);
		}
	}
}
