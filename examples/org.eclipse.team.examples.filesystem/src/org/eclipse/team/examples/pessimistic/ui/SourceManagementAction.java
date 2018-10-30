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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.team.examples.pessimistic.PessimisticFilesystemProvider;

/**
 * An abstract action used to centralize the implementation of
 * source management actions.
 */
public abstract class SourceManagementAction extends PessimisticProviderAction {

	/**
	 * Collects the selected resources by project, then iterates
	 * over the projects finding the associated provider.  If a 
	 * provider is found it requests that this action manage the resources
	 * using the found provider.
	 * 
	 * @see org.eclipse.ui.IActionDelegate#run(IAction)
	 */
	public void run(IAction action) {
		IResource[] resources= getSelectedResources();
		if (resources == null || resources.length == 0)
			return;
		Set resourceSet= new HashSet(resources.length);
		for(int i= 0; i < resources.length; i++) {
			IResource resource= resources[i];
			recursivelyAdd(resource, resourceSet);
		}
		if (!resourceSet.isEmpty()) {
			final Map byProject= sortByProject(resourceSet);
			IRunnableWithProgress runnable= monitor -> {
				for (Iterator i= byProject.keySet().iterator(); i.hasNext();) {
					IProject project= (IProject) i.next();
					PessimisticFilesystemProvider provider= getProvider(project);
					if (provider != null) {
						Set set= (Set)byProject.get(project);
						IResource[] resources1= new IResource[set.size()];
						set.toArray(resources1);
						manageResources(provider, resources1, monitor);
					}
				}
			};
			runWithProgressDialog(runnable);
		}		
	}

	/**
	 * Manages the <code>resources</code> using the given <code>provider</code>.
	 * 
	 * @param provider		The provider associated with the resources.
	 * @param resources	The resources to be managed.
	 * @param monitor		A progress monitor to give feedback.
	 */
	protected abstract void manageResources(PessimisticFilesystemProvider provider, IResource[] resources, IProgressMonitor monitor);
}
