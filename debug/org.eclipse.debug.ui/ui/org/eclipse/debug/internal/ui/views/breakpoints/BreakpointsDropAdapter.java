/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
 *     Patrick Chuong (Texas Instruments) - Improve usability of the breakpoint view (Bug 238956)
 *******************************************************************************/
package org.eclipse.debug.internal.ui.views.breakpoints;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.TreeItem;

/**
 * BreakpointsDropAdapter
 */
public class BreakpointsDropAdapter extends ViewerDropAdapter {

	private Item fTarget = null;
	private TreePath fPath = null;
	private BreakpointsView fView;

	/**
	 * Constructor
	 * @param viewer the backing viewer
	 */
	protected BreakpointsDropAdapter(TreeViewer viewer) {
		super(viewer);
		setFeedbackEnabled(false);
	}

	protected BreakpointsDropAdapter(TreeViewer viewer, BreakpointsView view) {
		this(viewer);
		fView = view;
	}

	/**
	 * @see org.eclipse.jface.viewers.ViewerDropAdapter#performDrop(java.lang.Object)
	 */
	@Override
	public boolean performDrop(Object data) {
		// This is temporary
		if (getViewer() instanceof BreakpointsViewer) {
			return ((BreakpointsViewer)getViewer()).performDrop(fTarget, (IStructuredSelection) LocalSelectionTransfer.getTransfer().getSelection());
		} else if (fView != null) {
			ISelection selection = LocalSelectionTransfer.getTransfer().getSelection();
			if (fPath != null && selection instanceof ITreeSelection) {
				return fView.performDrop(fPath, (ITreeSelection) selection);
			}
		}
		return false;
	}

	/**
	 * @see org.eclipse.jface.viewers.ViewerDropAdapter#determineTarget(org.eclipse.swt.dnd.DropTargetEvent)
	 */
	@Override
	protected Object determineTarget(DropTargetEvent event) {
		fTarget = (Item) event.item;
		if (fTarget instanceof TreeItem) {
			List<Object> list = new ArrayList<>();
			TreeItem item = (TreeItem)fTarget;
			while (item != null) {
				list.add(item.getData());
				item = item.getParentItem();
			}
			fPath = new TreePath(list.toArray());
		} else {
			fPath = null;
		}
		return fTarget;
	}

	/**
	 * @see org.eclipse.jface.viewers.ViewerDropAdapter#validateDrop(java.lang.Object, int, org.eclipse.swt.dnd.TransferData)
	 */
	@Override
	public boolean validateDrop(Object target, int operation, TransferData transferType) {
		// This is temporary
		if (getViewer() instanceof BreakpointsViewer) {
			return ((BreakpointsViewer)getViewer()).canDrop(fTarget, (IStructuredSelection) LocalSelectionTransfer.getTransfer().getSelection());
		} else {
			ISelection selection = LocalSelectionTransfer.getTransfer().getSelection();
			if (fPath != null && selection instanceof ITreeSelection) {
				return fView.canDrop(fPath, (ITreeSelection) LocalSelectionTransfer.getTransfer().getSelection());
			}
		}
		return false;
	}
}
