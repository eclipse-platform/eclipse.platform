/*******************************************************************************
 * Copyright (c) 2006, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal.patch;

import org.eclipse.compare.IContentChangeListener;
import org.eclipse.compare.IContentChangeNotifier;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.internal.core.patch.FileDiffResult;
import org.eclipse.compare.internal.core.patch.FilePatch2;
import org.eclipse.compare.patch.PatchConfiguration;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IResource;

public class PatchFileDiffNode extends PatchDiffNode implements IContentChangeListener {

	private final FileDiffResult result;

	public static PatchFileDiffNode createDiffNode(DiffNode parent, FileDiffResult result) {
		return new PatchFileDiffNode(result, parent, getKind(result), getAncestorElement(result), getLeftElement(result), getRightElement(result));
	}
	
	public static int getKind(FileDiffResult result) {
		if (!result.hasMatches())
			return Differencer.NO_CHANGE;
		int fileDiffKind = result.getDiff().getDiffType(result.getConfiguration().isReversed());
		int kind = convertFileDiffTypeToDifferencerType(fileDiffKind);
		return kind | Differencer.RIGHT;
	}

	private static int convertFileDiffTypeToDifferencerType(int fileDiffKind) {
		int kind;
		switch (fileDiffKind) {
		case FilePatch2.ADDITION:
			kind = Differencer.ADDITION;
			break;
		case FilePatch2.DELETION:
			kind = Differencer.DELETION;
			break;
		case FilePatch2.CHANGE:
			kind = Differencer.CHANGE;
			break;
		default:
			kind = Differencer.CHANGE;
			break;
		}
		return kind;
	}

	public static ITypedElement getRightElement(FileDiffResult result) {
		return new PatchFileTypedElement(result, true);
	}

	private static ITypedElement getLeftElement(FileDiffResult result) {
		return new PatchFileTypedElement(result, false);
	}

	public static ITypedElement getAncestorElement(FileDiffResult result) {
		return new PatchFileTypedElement(result, false);
	}

	public PatchFileDiffNode(FileDiffResult result, IDiffContainer parent, int kind,
			ITypedElement ancestor, ITypedElement left, ITypedElement right) {
		super(result.getDiff(), parent, kind, ancestor, left, right);
		this.result = result;
	}

	public FileDiffResult getDiffResult() {
		return result;
	}
	
	protected PatchConfiguration getConfiguration() {
		return result.getConfiguration();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.compare.structuremergeviewer.DiffContainer#add(org.eclipse.compare.structuremergeviewer.IDiffElement)
	 */
	public void add(IDiffElement diff) {
		super.add(diff);
		// Listen for content changes in unmatched children so we can fire an input change
		if (diff instanceof HunkDiffNode) {
			HunkDiffNode node = (HunkDiffNode) diff;
			Object left = node.getLeft();
			if (left instanceof IContentChangeNotifier) {
				IContentChangeNotifier notifier = (IContentChangeNotifier) left;
				notifier.addContentChangeListener(this);
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.compare.IContentChangeListener#contentChanged(org.eclipse.compare.IContentChangeNotifier)
	 */
	public void contentChanged(IContentChangeNotifier source) {
		fireChange();
	}
	
	public int getKind() {
		int kind = super.getKind();
		if (kind == Differencer.NO_CHANGE && getPatcher().hasCachedContents(getDiffResult().getDiff())) {
			return Differencer.CHANGE | Differencer.RIGHT;
		}
		return kind;
	}

	public boolean fileExists() {
		IResource file = getResource();
		return file != null && file.isAccessible();
	}

	public IResource getResource() {
		return ((WorkspaceFileDiffResult)getDiffResult()).getTargetFile();
	}
}
