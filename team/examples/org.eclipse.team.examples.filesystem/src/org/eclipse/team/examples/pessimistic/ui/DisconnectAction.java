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
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.examples.pessimistic.PessimisticFilesystemProvider;
import org.eclipse.team.examples.pessimistic.PessimisticFilesystemProviderPlugin;
import org.eclipse.ui.IActionDelegate;

public class DisconnectAction extends PessimisticProviderAction {
	/**
	 * Collects the selected resources, extracts the projects selected
	 * and disconnects the projects from their respective providers.
	 *
	 * @see IActionDelegate#run(IAction)
	 */
	@Override
	public void run(IAction action) {
		if (PessimisticFilesystemProviderPlugin.getInstance().isDebugging())
			System.out.println("Disconnect");

		IResource[] resources= getSelectedResources();
		if (resources == null || resources.length == 0)
			return;
		final Set<IProject> projects = new HashSet<>(resources.length);
		for (IResource resource : resources) {
			if (resource.getType() == IResource.PROJECT) {
				projects.add(resource.getProject());
			}
		}
		if (!projects.isEmpty()) {
			IRunnableWithProgress runnable= monitor -> {
				IWorkspaceRunnable runnable1= monitor1 -> {
					for (IProject project : projects) {
						PessimisticFilesystemProvider provider= getProvider(project);
						if (provider != null) {
							try {
								RepositoryProvider.unmap(project);
							} catch (TeamException e1) {
								PessimisticFilesystemProviderPlugin.getInstance().logError(e1, "Could not unmap " + project);
							}
						}
					}
				};
				try {
					ResourcesPlugin.getWorkspace().run(runnable1, monitor);
				} catch (CoreException e2) {
					PessimisticFilesystemProviderPlugin.getInstance().logError(e2, "Problem during unmap runnable");
				}

			};
			runWithProgressDialog(runnable);
		}
	}

	/**
	 * Answers <code>true</code> if and only if the resource is a
	 * project and is controlled by the pessimistic filesystem provider.
	 *
	 * @see PessimisticProviderAction#shouldEnableFor(IResource)
	 */
	@Override
	protected boolean shouldEnableFor(IResource resource) {
		if (resource.getType() == IResource.PROJECT) {
			PessimisticFilesystemProvider provider= getProvider(resource);
			if (provider == null)
				return false;
			return true;
		}
		return false;
	}
}
