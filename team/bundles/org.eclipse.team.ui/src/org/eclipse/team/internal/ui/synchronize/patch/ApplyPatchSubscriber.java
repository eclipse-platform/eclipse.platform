/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.synchronize.patch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.compare.internal.core.patch.DiffProject;
import org.eclipse.compare.internal.core.patch.FilePatch2;
import org.eclipse.compare.internal.core.patch.Hunk;
import org.eclipse.compare.internal.patch.PatchProjectDiffNode;
import org.eclipse.compare.internal.patch.WorkspacePatcher;
import org.eclipse.compare.patch.IHunk;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.subscribers.SubscriberChangeEvent;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.core.variants.IResourceVariantComparator;
import org.eclipse.team.internal.core.mapping.LocalResourceVariant;

public class ApplyPatchSubscriber extends Subscriber {

	private class ApplyPatchSyncInfo extends SyncInfo {
		private ApplyPatchSyncInfo(IResource local, IResourceVariant base,
				IResourceVariant remote, IResourceVariantComparator comparator) {
			super(local, base, remote, comparator);
		}

		@Override
		protected int calculateKind() throws TeamException {
			// TODO: this works only for files, see bug 300214
			if (!getPatcher().isEnabled(PatchModelProvider.getPatchObject(getLocal(), patcher)))
				return IN_SYNC;

			// same story here, one merged hunk is enough to consider the file as merged
			if (getRemote() != null) {
				FilePatch2 filePatch2 = ((PatchedFileVariant)getRemote()).getDiff();
				IHunk[] hunks = filePatch2.getHunks();
				for (IHunk hunk : hunks) {
					if (patcher.isManuallyMerged((Hunk) hunk)) {
						return IN_SYNC;
					}
				}
			} else {
				// deletions don't have the remote variant, but still can be manually merged
				Object patchObject = PatchModelProvider.getPatchObject(getLocal(), patcher);
				if (patchObject instanceof FilePatch2) {
					FilePatch2 filePatch2 = (FilePatch2) patchObject;
					IHunk[] hunks = filePatch2.getHunks();
					for (IHunk hunk : hunks) {
						if (patcher.isManuallyMerged((Hunk) hunk)) {
							return IN_SYNC;
						}
					}
				}
			}
			int kind = super.calculateKind();
			// mark diffs with problems as conflicts
			if (getRemote() != null
					&& getPatcher().getDiffResult(((PatchedFileVariant)getRemote()).getDiff()).containsProblems())
				kind |= CONFLICTING;
			return kind;
		}
	}

	private WorkspacePatcher patcher;
	private IResourceVariantComparator comparator;

	public ApplyPatchSubscriber(WorkspacePatcher patcher) {
		this.patcher = patcher;
		this.comparator = new PatchedFileVariantComparator();
		getPatcher().refresh();
	}

	@Override
	public String getName() {
		return "Apply Patch Subscriber"; //$NON-NLS-1$
	}

	@Override
	public IResourceVariantComparator getResourceComparator() {
		return comparator;
	}

	@Override
	public SyncInfo getSyncInfo(IResource resource) throws TeamException {
		if (!isSupervised(resource)) return null;
		// TODO: called too many times, optimize
		refresh(new IResource[] { resource }, IResource.DEPTH_ZERO, null);
		try {
			FilePatch2 diff = (FilePatch2) PatchModelProvider.getPatchObject(resource, getPatcher());
			// use null as remote variant for deletions
			IResourceVariant remote = null;
			if (diff.getDiffType(patcher.isReversed()) != FilePatch2.DELETION)
				remote =  new PatchedFileVariant(getPatcher(), diff);
			IResourceVariant base = null;
			if (diff.getDiffType(patcher.isReversed()) != FilePatch2.ADDITION)
				base = new LocalResourceVariant(resource);
			SyncInfo info = new ApplyPatchSyncInfo(resource, base, remote, getResourceComparator());
			info.init();
			return info;
		} catch (CoreException e) {
			throw TeamException.asTeamException(e);
		}
	}

	@Override
	public boolean isSupervised(IResource resource) throws TeamException {
		return resource.getType() == IResource.FILE
				&& PatchModelProvider.getPatchObject(resource, getPatcher()) != null;
	}

	@Override
	public IResource[] members(IResource resource) throws TeamException {
		//XXX: what if there is an addition in the patch that needs to add 3 subfolders?
		try {
			if(resource.getType() == IResource.FILE)
				// file has no IResource members
				return new IResource[0];
			IContainer container = (IContainer) resource;

			// workspace container members
			List<IResource> existingChildren = new ArrayList<>();

			if (container.isAccessible())
				existingChildren.addAll(Arrays.asList(container.members()));

			// patch members, subscriber location
			FilePatch2[] diffs = getPatcher().getDiffs();
			for (FilePatch2 diff : diffs) {
				IResource file = PatchModelProvider.getFile(diff, getPatcher());
				if (container.getFullPath().isPrefixOf(file.getFullPath())) {
					// XXX: check segments
					if (!container.exists(file.getProjectRelativePath())) {
						existingChildren.add(file);
					}
				}
			}
			return existingChildren.toArray(new IResource[existingChildren.size()]);
		} catch (CoreException e) {
			throw TeamException.asTeamException(e);
		}
	}

	@Override
	public void refresh(IResource[] resources, int depth,
			IProgressMonitor monitor) throws TeamException {
		Set<FilePatch2> diffs = new HashSet<>();
		for (IResource resource : resources) {
			Object object = PatchModelProvider.getPatchObject(resource, getPatcher());
			if (object instanceof FilePatch2) {
				FilePatch2 filePatch = (FilePatch2) object;
				diffs.add(filePatch);
			}
		}
		getPatcher().refresh(diffs.toArray(new FilePatch2[0]));
	}

	@Override
	public IResource[] roots() {
		Set<IResource> roots = new HashSet<>();
		if (getPatcher().isWorkspacePatch()) {
			IDiffElement[] children = PatchModelProvider.getPatchWorkspace(this).getChildren();
			for (IDiffElement child : children) {
				// return array of projects from the patch
				DiffProject diffProject = ((PatchProjectDiffNode) child).getDiffProject();
				IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(diffProject.getName());
				if (project.isAccessible())
					roots.add(project);
			}
		} else {
			roots.add(getPatcher().getTarget());
		}
		return roots.toArray(new IResource[0]);
	}

	public WorkspacePatcher getPatcher() {
		return patcher;
	}

	public void merged(IResource[] resources) {
		fireTeamResourceChange(SubscriberChangeEvent.asSyncChangedDeltas(this, resources));
	}
}
