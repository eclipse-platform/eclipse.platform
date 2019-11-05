/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.core.mapping;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.internal.core.subscribers.DiffChangeSet;

public class ChangeSetResourceMapping extends ResourceMapping {

	private final DiffChangeSet changeSet;

	public ChangeSetResourceMapping(DiffChangeSet changeSet) {
		this.changeSet = changeSet;
	}

	public Object getModelObject() {
		return changeSet;
	}

	public String getModelProviderId() {
		return ChangeSetModelProvider.ID;
	}

	public IProject[] getProjects() {
		Set<IProject> result = new HashSet<>();
		IResource[] resources = changeSet.getResources();
		for (IResource resource : resources) {
			result.add(resource.getProject());
		}
		return result.toArray(new IProject[result.size()]);
	}

	public ResourceTraversal[] getTraversals(ResourceMappingContext context,
			IProgressMonitor monitor) throws CoreException {
		IResource[] resources = changeSet.getResources();
		if (resources.length == 0) {
			return new ResourceTraversal[0];
		}
		return new ResourceTraversal[] {
				new ResourceTraversal(resources, IResource.DEPTH_ZERO, IResource.NONE)
		};
	}

}
