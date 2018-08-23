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
	public void run(IAction action) {
		IResource[] resources= getSelectedResources();
		if (resources == null || resources.length == 0)
			return;
		Set resourceSet= new HashSet(resources.length);
		for(int i= 0; i < resources.length; i++) {
			IResource resource= resources[i];
			while (resource.getType() != IResource.PROJECT && !isControlled(resource)) {
				resourceSet.add(resource);
				resource= resource.getParent();
			}
		}
		if (!resourceSet.isEmpty()) {
			final Map byProject= sortByProject(resourceSet);
			IRunnableWithProgress runnable= new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {
					for (Iterator i= byProject.keySet().iterator(); i.hasNext();) {
						IProject project= (IProject) i.next();
						PessimisticFilesystemProvider provider= getProvider(project);
						if (provider != null) {
							Set set= (Set)byProject.get(project);
							IResource[] resources= new IResource[set.size()];
							set.toArray(resources);
							provider.addToControl(resources, monitor);
						}
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
