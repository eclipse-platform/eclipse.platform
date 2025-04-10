/*******************************************************************************
 * Copyright (c) 2010, 2013 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.actions.breakpointGroups;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointsListener;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.internal.ui.viewers.model.VirtualCopyToClipboardActionDelegate;
import org.eclipse.debug.ui.AbstractDebugView;
import org.eclipse.debug.ui.IDebugView;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IViewPart;

/**
 * Copies breakpoint labels to the text clipboard and breakpoint objects
 * to the breakpoint paste action.
 */
public class CopyBreakpointsActionDelegate extends VirtualCopyToClipboardActionDelegate implements IBreakpointsListener {

	private long fStamp;

	@Override
	public void run(IAction action) {
		super.run(action);
		LocalSelectionTransfer.getTransfer().setSelection(getSelection());
		fStamp = System.currentTimeMillis();
		LocalSelectionTransfer.getTransfer().setSelectionSetTime(fStamp);
		IAction pasteAction = ((AbstractDebugView)getView()).getAction(IDebugView.PASTE_ACTION);
		// update the enablement of the paste action
		// workaround since the clipboard does not suppot callbacks
		if (pasteAction instanceof PasteBreakpointsAction) {
			PasteBreakpointsAction pba = (PasteBreakpointsAction) pasteAction;
			if (pba.getStructuredSelection() != null) {
				pba.selectionChanged(pba.getStructuredSelection());
			}
		}
	}

	@Override
	public void init(IViewPart view) {
		super.init(view);
		DebugPlugin.getDefault().getBreakpointManager().addBreakpointListener(this);
	}

	@Override
	public void dispose() {
		DebugPlugin.getDefault().getBreakpointManager().removeBreakpointListener(this);
		super.dispose();
	}

	@Override
	public void breakpointsAdded(IBreakpoint[] breakpoints) {
	}

	@Override
	public void breakpointsRemoved(IBreakpoint[] breakpoints, IMarkerDelta[] deltas) {
		// remove deleted breakpoints from drag/drop clipboard
		if (fStamp == LocalSelectionTransfer.getTransfer().getSelectionSetTime()) {
			ISelection selection = LocalSelectionTransfer.getTransfer().getSelection();
			if (selection instanceof IStructuredSelection) {
				Set<IBreakpoint> removed = new HashSet<>();
				Collections.addAll(removed, breakpoints);
				boolean modified = false;
				List<Object> remain = new ArrayList<>();
				IStructuredSelection ss = (IStructuredSelection) selection;
				Iterator<?> iterator = ss.iterator();
				while (iterator.hasNext()) {
					Object bp = iterator.next();
					if (removed.contains(bp)) {
						modified = true;
					} else {
						remain.add(bp);
					}
				}
				if (modified) {
					LocalSelectionTransfer.getTransfer().setSelection(new StructuredSelection(remain));
					fStamp = System.currentTimeMillis();
					LocalSelectionTransfer.getTransfer().setSelectionSetTime(fStamp);
				}
			}
		}
	}

	@Override
	public void breakpointsChanged(IBreakpoint[] breakpoints, IMarkerDelta[] deltas) {
	}
}
