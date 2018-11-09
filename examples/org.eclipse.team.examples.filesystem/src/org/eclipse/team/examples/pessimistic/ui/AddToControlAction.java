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
import org.eclipse.ui.IActionDelegate;

/**
 * Adds the selected resources and their parent resources to
 * the control of the provider.
 */
public class AddToControlAction extends PessimisticProviderAction {

	/**
	 * Collects the selected resources, sorts them by project
	 * and adds them to their respective repository providers.
	 *
	 * @see IActionDelegate#run(IAction)
	 */
	@Override
	public void run(IAction action) {
		IResource[] resources= getSelectedResources();
		if (resources == null || resources.length == 0)
			return;
		Set<IResource> resourceSet = new HashSet<>(resources.length);
		for (IResource resource2 : resources) {
			IResource resource= resource2;
			while (resource.getType() != IResource.PROJECT && !isControlled(resource)) {
				resourceSet.add(resource);
				resource= resource.getParent();
			}
		}
		if (!resourceSet.isEmpty()) {
			final Map<IProject, Set<IResource>> byProject = sortByProject(resourceSet);
			IRunnableWithProgress runnable= monitor -> {
				for (Object element : byProject.keySet()) {
					IProject project= (IProject) element;
					PessimisticFilesystemProvider provider= getProvider(project);
					if (provider != null) {
						Set<IResource> set = byProject.get(project);
						IResource[] resources1= new IResource[set.size()];
						set.toArray(resources1);
						provider.addToControl(resources1, monitor);
					}
				}
			};
			runWithProgressDialog(runnable);
		}
	}

	/**
	 * Answers <code>true</code> if the selected resource is not
	 * a project (or the workspace root) and is not controlled.
	 *
	 * @see PessimisticProviderAction#shouldEnableFor(IResource)
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
		return !provider.isControlled(resource);
	}

}
