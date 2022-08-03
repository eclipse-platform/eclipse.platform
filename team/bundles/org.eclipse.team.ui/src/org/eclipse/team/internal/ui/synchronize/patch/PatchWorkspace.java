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
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize.patch;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.internal.core.patch.DiffProject;
import org.eclipse.compare.internal.core.patch.FileDiffResult;
import org.eclipse.compare.internal.core.patch.FilePatch2;
import org.eclipse.compare.internal.core.patch.HunkResult;
import org.eclipse.compare.internal.patch.HunkDiffNode;
import org.eclipse.compare.internal.patch.PatchFileDiffNode;
import org.eclipse.compare.internal.patch.PatchProjectDiffNode;
import org.eclipse.compare.internal.patch.WorkspaceFileDiffResult;
import org.eclipse.compare.internal.patch.WorkspacePatcher;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.team.internal.ui.synchronize.LocalResourceTypedElement;

// TODO: extend PatchDiffNode, update navigatorContent triggerPoints when done
public class PatchWorkspace extends DiffNode implements IAdaptable {

	private WorkspacePatcher patcher;

	public PatchWorkspace(WorkspacePatcher patcher) {
		super(null, Differencer.NO_CHANGE);
		this.patcher = patcher;
	}

	public WorkspacePatcher getPatcher() {
		return patcher;
	}

	public IResource getResource() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}

	@Override
	public String getName() {
		return "Patch Root Workspace"; //$NON-NLS-1$
	}

	@Override
	public IDiffContainer getParent() {
		return null;
	}

	@Override
	public IDiffElement[] getChildren() {
		/*
		 * Create a complete tree of patch model objects - elements of the
		 * patch, but return only top-level ones: PatchProjectDiffNode(s) for a
		 * workspace patch or FileDiffResult(s) otherwise. See
		 * org.eclipse.compare.internal
		 * .patch.PatchCompareEditorInput.buildTree()
		 */
		IDiffElement[] children;
		if (getPatcher().isWorkspacePatch()) {
			children = processProjects(getPatcher().getDiffProjects());
		} else {
			children = processDiffs(getPatcher().getDiffs());
		}
		return children;
	}

	// see org.eclipse.compare.internal.patch.PatchCompareEditorInput.processDiffs(FilePatch2[])
	private IDiffElement[] processDiffs(FilePatch2[] diffs) {
		List<IDiffElement> result = new ArrayList<>();
		for (FilePatch2 diff : diffs) {
			result.addAll(processDiff(diff, this));
		}
		return result.toArray(new IDiffElement[result.size()]);
	}

	// see org.eclipse.compare.internal.patch.PatchCompareEditorInput.processProjects(DiffProject[])
	private IDiffElement[] processProjects(DiffProject[] diffProjects) {
		List<IDiffElement> result = new ArrayList<>();
		for (DiffProject diffProject : diffProjects) {
			PatchProjectDiffNode projectNode = new PatchProjectDiffNode(this, diffProject, getPatcher().getConfiguration());
			result.add(projectNode);
			FilePatch2[] diffs = diffProject.getFileDiffs();
			for (FilePatch2 fileDiff : diffs) {
				processDiff(fileDiff, projectNode);
			}
		}
		return result.toArray(new IDiffElement[result.size()]);
	}

	// see org.eclipse.compare.internal.patch.PatchCompareEditorInput.processDiff(FilePatch2, DiffNode)
	private List<IDiffElement> processDiff(FilePatch2 diff, DiffNode parent) {
		List<IDiffElement> result = new ArrayList<>();
		FileDiffResult diffResult = getPatcher().getDiffResult(diff);
		PatchFileDiffNode node = new PatchFileDiffNode(diffResult, parent, PatchFileDiffNode.getKind(diffResult), PatchFileDiffNode.getAncestorElement(diffResult), getLeftElement(diffResult), PatchFileDiffNode.getRightElement(diffResult));
		result.add(node);
		HunkResult[] hunkResults = diffResult.getHunkResults();
		for (HunkResult hunkResult : hunkResults) {
			new HunkDiffNode(hunkResult, node, Differencer.CHANGE, HunkDiffNode.getAncestorElement(hunkResult, false), getLeftElement(hunkResult), HunkDiffNode.getRightElement(hunkResult, false));
			// result.add(hunkDiffNode);
		}
		return result;
	}

	private static ITypedElement getLeftElement(final FileDiffResult result) {
		return new LocalResourceTypedElement(((WorkspaceFileDiffResult)result).getTargetFile()) {
			@Override
			public String getName() {
				// as in org.eclipse.compare.internal.patch.PatchFileTypedElement
				return result.getTargetPath().toString();
			}
		};
	}

	private static ITypedElement getLeftElement(final HunkResult result) {
		return new LocalResourceTypedElement(((WorkspaceFileDiffResult)result.getDiffResult()).getTargetFile()) {
			@Override
			public String getName() {
				// as in org.eclipse.compare.internal.patch.HunkTypedElement
				return result.getHunk().getLabel();
			}
		};
	}

	// cannot extend PlatformObject (already extends DiffNode) so implement
	// IAdaptable
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}
}
