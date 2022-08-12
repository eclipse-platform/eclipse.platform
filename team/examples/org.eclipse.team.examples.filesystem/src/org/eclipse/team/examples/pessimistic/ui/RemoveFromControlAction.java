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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.team.examples.pessimistic.PessimisticFilesystemProvider;

/**
 * Removes the selected resources and their children resources from
 * the control of the provider.
 */
public class RemoveFromControlAction extends PessimisticProviderAction {

	/**
	 * Collects the selected resources into sets by project,
	 * then removes the resources from the provider associated
	 * with their containing project.
	 *
	 * @see org.eclipse.ui.IActionDelegate#run(IAction)
	 */
	@Override
	public void run(IAction action) {
		IResource[] resources= getSelectedResources();
		if (resources == null || resources.length == 0)
			return;
		Set<IResource> resourceSet = new HashSet<>(resources.length);
		for (IResource resource : resources) {
			recursivelyAdd(resource, resourceSet);
		}
		if (!resourceSet.isEmpty()) {
			final Map<IProject, Set<IResource>> byProject = sortByProject(resourceSet);
			IRunnableWithProgress runnable= monitor -> {
				for (IProject project : byProject.keySet()) {
					PessimisticFilesystemProvider provider= getProvider(project);
					if (provider != null) {
						Set<IResource> set = byProject.get(project);
						IResource[] resources1= new IResource[set.size()];
						set.toArray(resources1);
						provider.removeFromControl(resources1, monitor);
					}
				}
			};
			runWithProgressDialog(runnable);
		}
	}

	/**
	 * Answers <code>true</code> if and only if the resource is not <code>null</code>,
	 * not a project or the workspace root, and is controlled by the provider.
	 *
	 * @see org.eclipse.team.examples.pessimistic.ui.PessimisticProviderAction#shouldEnableFor(IResource)
	 */
	@Override
	protected boolean shouldEnableFor(IResource resource) {
		if (resource == null) {
			return false;
		}
		if ((resource.getType() & (IResource.ROOT | IResource.PROJECT)) != 0) {
			return false;
		}
		PessimisticFilesystemProvider provider= getProvider(resource);
		if (provider == null)
			return false;
		return provider.isControlled(resource);
	}

}
