/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.synchronize.patch;

import org.eclipse.compare.internal.core.patch.DiffProject;
import org.eclipse.compare.internal.core.patch.FilePatch2;
import org.eclipse.compare.internal.patch.HunkDiffNode;
import org.eclipse.compare.internal.patch.PatchDiffNode;
import org.eclipse.compare.internal.patch.PatchFileDiffNode;
import org.eclipse.compare.internal.patch.PatchProjectDiffNode;
import org.eclipse.compare.internal.patch.WorkspacePatcher;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.mapping.ModelProvider;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.internal.core.TeamPlugin;

public class PatchModelProvider extends ModelProvider {

	public static final String ID = "org.eclipse.team.ui.patchModelProvider"; //$NON-NLS-1$
	private static PatchModelProvider provider;

	public static PatchModelProvider getProvider() {
		if (provider == null) {
			try {
				provider = (PatchModelProvider) ModelProvider
						.getModelProviderDescriptor(PatchModelProvider.ID)
						.getModelProvider();
			} catch (CoreException e) {
				TeamPlugin.log(e);
			}
		}
		return provider;
	}

	static ResourceMapping getResourceMapping(IDiffElement object) {
		if (object instanceof PatchProjectDiffNode) {
			return new DiffProjectResourceMapping(
					((PatchProjectDiffNode) object).getDiffProject());
		} else if (object instanceof PatchFileDiffNode) {
			return new FilePatchResourceMapping(((PatchFileDiffNode) object)
					.getDiffResult());
		} else if (object instanceof HunkDiffNode) {
			return new HunkResourceMapping(((HunkDiffNode) object)
					.getHunkResult());
		}
		return null;
	}

	/**
	 * Returns the resource associated with the corresponding model element.
	 *
	 * @param element
	 *            the model element
	 * @return the associated resource, or <code>null</code>
	 */
	static IResource getResource(PatchDiffNode element) {
		IResource resource= null;
		if (element instanceof PatchProjectDiffNode) {
			return ((PatchProjectDiffNode) element).getResource();
		} else if (element instanceof PatchFileDiffNode) {
			return ((PatchFileDiffNode) element).getResource();
		} else if (element instanceof HunkDiffNode) {
			return ((HunkDiffNode) element).getResource();
		}
		return resource;
	}

	static Object/* DiffProject or FilePatch2 */getPatchObject(
			IResource resource, WorkspacePatcher patcher) {
		if (resource.getType() == IResource.PROJECT) {
			if (patcher.isWorkspacePatch()) {
				DiffProject[] diffProjects = patcher.getDiffProjects();
				for (DiffProject diffProject : diffProjects) {
					if (diffProject.getName().equals(resource.getName())) {
						return diffProject;
					}
				}
			}
		} else if (resource.getType() == IResource.FILE) {
			FilePatch2[] diffs = patcher.getDiffs();
			for (FilePatch2 diff : diffs) {
				if (resource.equals(getFile(diff, patcher))) {
					return diff;
				}
			}
		}
		return null;
	}

	static IFile getFile(FilePatch2 diff, WorkspacePatcher patcher) {
		IProject project = null;
		if (patcher.isWorkspacePatch()) {
			DiffProject diffProject = diff.getProject();
			project = ResourcesPlugin.getWorkspace().getRoot().getProject(
					diffProject.getName());
			return project.getFile(diff.getPath(patcher.isReversed()));
		} else {
			IResource target = patcher.getTarget();
			if (target.getType() == IResource.PROJECT
					|| target.getType() == IResource.FOLDER) {
				IContainer container = (IContainer) target;
				return container.getFile(diff.getStrippedPath(patcher
						.getStripPrefixSegments(), patcher.isReversed()));
			}
			IContainer container = target.getParent();
			return container.getFile(diff.getStrippedPath(patcher
					.getStripPrefixSegments(), patcher.isReversed()));
		}
	}

	public static PatchWorkspace getPatchWorkspace(Subscriber subscriber) {
		if (subscriber instanceof ApplyPatchSubscriber) {
			ApplyPatchSubscriber aps = (ApplyPatchSubscriber) subscriber;
			return new PatchWorkspace(aps.getPatcher());
		}
		// TODO: assertion?
		return null;
	}
}
