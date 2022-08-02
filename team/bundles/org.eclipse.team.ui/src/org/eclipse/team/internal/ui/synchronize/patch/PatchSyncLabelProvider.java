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

import org.eclipse.compare.internal.patch.PatchDiffNode;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.ui.mapping.SynchronizationLabelProvider;
import org.eclipse.ui.navigator.ICommonContentExtensionSite;

public class PatchSyncLabelProvider extends SynchronizationLabelProvider {

	private PatchWorkbenchLabelProvider delegate;

	public PatchSyncLabelProvider() {
		super();
	}

	@Override
	public void init(ICommonContentExtensionSite site) {
		super.init(site);
		delegate = new PatchWorkbenchLabelProvider();
	}

	@Override
	public void dispose() {
		super.dispose();
		if (delegate != null)
			delegate.dispose();
	}

	@Override
	protected ILabelProvider getDelegateLabelProvider() {
		return delegate;
	}

	@Override
	protected IDiff getDiff(Object element) {
		if (element instanceof PatchDiffNode) {
			IResource resource = PatchModelProvider
					.getResource((PatchDiffNode) element);
			return getContext().getDiffTree().getDiff(resource);
		}
		return super.getDiff(element);
	}

	@Override
	protected Image getCompareImage(Image base, int kind) {
		/*
		 * Need to swap left and right for PatchDiffNodes as done in Apply Patch
		 * wizard. See org.eclipse.compare.structuremergeviewer.DiffTreeViewer.
		 * DiffViewerLabelProvider.getImage(Object).
		 */
		switch (kind & Differencer.DIRECTION_MASK) {
		case Differencer.LEFT:
			kind = (kind & ~Differencer.LEFT) | Differencer.RIGHT;
			break;
		case Differencer.RIGHT:
			kind = (kind & ~Differencer.RIGHT) | Differencer.LEFT;
			break;
		}
		return super.getCompareImage(base, kind);
	}

}
