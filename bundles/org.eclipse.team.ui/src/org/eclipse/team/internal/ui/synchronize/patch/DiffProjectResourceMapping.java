/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize.patch;

import org.eclipse.compare.internal.core.patch.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.resources.mapping.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class DiffProjectResourceMapping extends ResourceMapping {

	private final DiffProject object;

	public DiffProjectResourceMapping(DiffProject adaptableObject) {
		object = adaptableObject;
	}

	@Override
	public Object getModelObject() {
		return object;
	}

	@Override
	public String getModelProviderId() {
		return PatchModelProvider.ID;
	}

	@Override
	public IProject[] getProjects() {
		IProject p = ResourcesPlugin.getWorkspace().getRoot().getProject(
				object.getName());
		return new IProject[] { p };
	}

	@Override
	public ResourceTraversal[] getTraversals(ResourceMappingContext context,
			IProgressMonitor monitor) throws CoreException {
		return new ResourceTraversal[] { new ResourceTraversal(
				new IResource[] { getResource() }, IResource.DEPTH_INFINITE,
				IResource.NONE) };
	}

	private IResource getResource() {
		return getProjects()[0];
	}

	@Override
	public boolean contains(ResourceMapping mapping) {
		if (mapping instanceof DiffProjectResourceMapping) {
			DiffProject diffProject = (DiffProject) mapping.getModelObject();
			return diffProject.getName().equals(object.getName());
		} else if (mapping instanceof FilePatchResourceMapping) {
			FileDiffResult filePatch = (FileDiffResult) mapping
					.getModelObject();
			FilePatch2[] filePatches = object.getFileDiffs();
			for (int i = 0; i < filePatches.length; i++) {
				if (filePatches[i].getPath(false).equals(
						filePatch.getTargetPath()))
					return true;
			}
		} else if (mapping instanceof HunkResourceMapping) {
			HunkResult hunk = (HunkResult) mapping.getModelObject();
			DiffProject diffProject = hunk.getHunk().getParent().getProject();
			return diffProject.getName().equals(object.getName());
		}
		return super.contains(mapping);
	}

}
