/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.util.TransferDragSourceListener;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Item;

/**
 * A drag adapter for the breakpoints viewer
 */
public class BreakpointsDragAdapter extends DragSourceAdapter implements TransferDragSourceListener {

	/**
	 * the associated viewer for the adapter
	 */
	private AbstractTreeViewer fViewer;
	private Item[] fItems = null;

	private BreakpointsView fView;
	private TreePath[] fTreePaths = null;

	/**
	 * Constructor
	 * @param viewer the associated viewer, which acts as the selection provider and therefore <b>must</b> implement <code>ISelectionProvider</code>
	 */
	public BreakpointsDragAdapter(BreakpointsViewer viewer) {
		Assert.isNotNull(viewer);
		fViewer = viewer;
	}
	public BreakpointsDragAdapter(AbstractTreeViewer viewer, BreakpointsView view) {
		Assert.isNotNull(view);
		fViewer = viewer;
		fView = view;
	}

	@Override
	public Transfer getTransfer() {
		return LocalSelectionTransfer.getTransfer();
	}

	@Override
	public void dragStart(DragSourceEvent event) {
		ISelection selection = fViewer.getSelection();
		LocalSelectionTransfer.getTransfer().setSelection(selection);
		LocalSelectionTransfer.getTransfer().setSelectionSetTime(event.time & 0xFFFFFFFFL);
		if (fViewer instanceof BreakpointsViewer) {
			BreakpointsViewer viewer = (BreakpointsViewer)fViewer;
			fItems = viewer.getSelectedItems();
			event.doit = viewer.canDrag(fItems);
		} else {
			if (selection instanceof ITreeSelection) {
				fTreePaths = ((ITreeSelection) selection).getPaths();
			} else {
				fTreePaths = new TreePath[0];
			}
			event.doit = fView.canDrag(fTreePaths);
		}
	}

	/* non Java-doc
	 * @see org.eclipse.swt.dnd.DragSourceListener#dragSetData
	 */
	@Override
	public void dragSetData(DragSourceEvent event) {
		// For consistency set the data to the selection even though
		// the selection is provided by the LocalSelectionTransfer
		// to the drop target adapter.
		event.data = LocalSelectionTransfer.getTransfer().getSelection();
	}

	/* non Java-doc
	 * @see org.eclipse.swt.dnd.DragSourceListener#dragFinished
	 */
	@Override
	public void dragFinished(DragSourceEvent event) {
		if (event.detail == DND.DROP_MOVE) {
			// remove from source on move operation
			if (fViewer instanceof BreakpointsViewer) {
				BreakpointsViewer viewer = (BreakpointsViewer)fViewer;
				viewer.performDrag(fItems);
			} else {
				fView.performDrag(fTreePaths);
			}
		}
		fItems = null;
		LocalSelectionTransfer.getTransfer().setSelection(null);
		LocalSelectionTransfer.getTransfer().setSelectionSetTime(0);
	}
}
