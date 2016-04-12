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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.internal.core.patch.*;
import org.eclipse.compare.internal.patch.*;
import org.eclipse.compare.structuremergeviewer.*;
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
		List result = new ArrayList();
		for (int i = 0; i < diffs.length; i++) {
			result.addAll(processDiff(diffs[i], this));
		}
		return (IDiffElement[]) result.toArray(new IDiffElement[result.size()]);
	}

	// see org.eclipse.compare.internal.patch.PatchCompareEditorInput.processProjects(DiffProject[])
	private IDiffElement[] processProjects(DiffProject[] diffProjects) {
		List result = new ArrayList();
		for (int i = 0; i < diffProjects.length; i++) {
			PatchProjectDiffNode projectNode = new PatchProjectDiffNode(this, diffProjects[i], getPatcher().getConfiguration());
			result.add(projectNode);
			FilePatch2[] diffs = diffProjects[i].getFileDiffs();
			for (int j = 0; j < diffs.length; j++) {
				FilePatch2 fileDiff = diffs[j];
				processDiff(fileDiff, projectNode);
			}
		}
		return (IDiffElement[]) result.toArray(new IDiffElement[result.size()]);
	}

	// see org.eclipse.compare.internal.patch.PatchCompareEditorInput.processDiff(FilePatch2, DiffNode)
	private List/*<IDiffElement>*/ processDiff(FilePatch2 diff, DiffNode parent) {
		List result = new ArrayList();
		FileDiffResult diffResult = getPatcher().getDiffResult(diff);
		PatchFileDiffNode node = new PatchFileDiffNode(diffResult, parent, PatchFileDiffNode.getKind(diffResult), PatchFileDiffNode.getAncestorElement(diffResult), getLeftElement(diffResult), PatchFileDiffNode.getRightElement(diffResult));
		result.add(node);
		HunkResult[] hunkResults = diffResult.getHunkResults();
		for (int i = 0; i < hunkResults.length; i++) {
			HunkResult hunkResult = hunkResults[i];
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
	public Object getAdapter(Class adapter) {
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}
}
