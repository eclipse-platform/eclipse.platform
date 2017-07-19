/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize.patch;

import org.eclipse.compare.internal.core.patch.DiffProject;
import org.eclipse.compare.internal.core.patch.HunkResult;
import org.eclipse.core.resources.*;
import org.eclipse.core.resources.mapping.*;
import org.eclipse.core.runtime.*;

public class HunkResourceMapping extends ResourceMapping {

	private final HunkResult object;

	public HunkResourceMapping(HunkResult hunkResult) {
		this.object = hunkResult;
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
		DiffProject dp = object.getHunk().getParent().getProject();
		IProject p = ResourcesPlugin.getWorkspace().getRoot().getProject(
				dp.getName());
		return new IProject[] { p };
	}

	private IResource getResource() {
		IPath path = object.getHunk().getParent().getPath(false);
		return getProjects()[0].getFile(path);
	}

	@Override
	public ResourceTraversal[] getTraversals(ResourceMappingContext context,
			IProgressMonitor monitor) throws CoreException {
		return new ResourceTraversal[] { new ResourceTraversal(
				new IResource[] { getResource() }, IResource.DEPTH_ZERO,
				IResource.NONE) };
	}
}
