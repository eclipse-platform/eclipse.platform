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
 *     Freescale Semiconductor - bug 287863
 *     Patrick Chuong (Texas Instruments) - Improve usability of the breakpoint view (Bug 238956)
 *******************************************************************************/
package org.eclipse.debug.internal.ui.actions.breakpointGroups;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.internal.ui.breakpoints.provisional.IBreakpointContainer;
import org.eclipse.debug.internal.ui.views.breakpoints.BreakpointsView;
import org.eclipse.debug.internal.ui.views.breakpoints.WorkingSetCategory;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;

/**
 * Removes a breakpoint from a breakpoint working set.
 */
public class RemoveFromWorkingSetAction extends BreakpointSelectionAction {

	private BreakpointSetElement[] fBreakpoints;


	/**
	 * Constructs action to remove breakpoints from a category.
	 *
	 * @param view
	 */
	public RemoveFromWorkingSetAction(BreakpointsView view) {
		super(BreakpointGroupMessages.RemoveFromWorkingSetAction_0, view);
	}

	@Override
	public void run() {
		if (fBreakpoints != null) {
			for (BreakpointSetElement breakpoint : fBreakpoints) {
				breakpoint.container.getOrganizer().removeBreakpoint(breakpoint.breakpoint, breakpoint.container.getCategory());
			}
		}
	}

	protected static class BreakpointSetElement {
		BreakpointSetElement(IBreakpoint b, IBreakpointContainer c) { breakpoint = b; container = c; }
		IBreakpoint breakpoint;
		IBreakpointContainer container;
	}

	/**
	 * Returns a array of breakpoint/container pairs for the selection
	 *
	 *  All the returned elements contain a breakpoint and a working set container the breakpoint is contained and the breakpoint
	 *  can be removed from.
	 *
	 * @param selection
	 * @return
	 */
	protected BreakpointSetElement[] getRemovableBreakpoints(IStructuredSelection selection) {
		List<BreakpointSetElement> res = new ArrayList<>();
		if (selection instanceof ITreeSelection) {
			ITreeSelection tSel = (ITreeSelection)selection;

			for (TreePath path : tSel.getPaths()) {
				// We can remove Breakpoints from their working set if any of their parents is a non "Other" breakpoint working set
				IBreakpoint breakpoint = (IBreakpoint)DebugPlugin.getAdapter(path.getLastSegment(), IBreakpoint.class);
				if (breakpoint != null) {
					TreePath parents = path.getParentPath();

					for (int j = 0; j < parents.getSegmentCount(); j++) {
						Object parent = parents.getSegment(j);

						if (parent instanceof IBreakpointContainer) {
							IBreakpointContainer container = (IBreakpointContainer)parent;

							// Test if this is a working set container.
							if (container.getCategory() instanceof WorkingSetCategory) {
								// Test if this container allows to remove this breakpoint.
								if (container.getOrganizer().canRemove(breakpoint, container.getCategory())) {
									res.add(new BreakpointSetElement(breakpoint, container));
								}
							}
						}
					}
				}
			}
		}
		return res.toArray(new BreakpointSetElement[res.size()]);
	}

	@Override
	public boolean isEnabled() {
		if(fBreakpoints != null) {
			return fBreakpoints.length > 0;
		}
		return false;
	}

	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		fBreakpoints = getRemovableBreakpoints(selection);
		return fBreakpoints.length > 0;
	}
}
