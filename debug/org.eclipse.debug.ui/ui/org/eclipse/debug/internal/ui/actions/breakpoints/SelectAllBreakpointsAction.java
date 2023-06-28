/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.actions.breakpoints;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointsListener;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.internal.ui.actions.SelectAllAction;
import org.eclipse.debug.ui.AbstractDebugView;
import org.eclipse.debug.ui.IDebugView;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Tree;

public class SelectAllBreakpointsAction extends SelectAllAction implements IBreakpointsListener {

	@Override
	protected boolean isEnabled() {
		return DebugPlugin.getDefault().getBreakpointManager().hasBreakpoints();
	}

	@Override
	public void run(IAction action) {
		Viewer viewer = ((AbstractDebugView) getView()).getViewer();
		((Tree) viewer.getControl()).selectAll();
		// ensure that the selection change callback is fired
		viewer.setSelection(viewer.getSelection());
	}

	@Override
	public void breakpointsAdded(IBreakpoint[] breakpoints) {
		if (getAction() != null && !getAction().isEnabled()) {
			update();
		}
	}

	@Override
	public void breakpointsChanged(IBreakpoint[] breakpoints, IMarkerDelta[] deltas) {
	}

	@Override
	public void breakpointsRemoved(IBreakpoint[] breakpoints, IMarkerDelta[] deltas) {
		if (getAction() != null) {
			update();
		}
	}

	@Override
	protected void initialize() {
		DebugPlugin.getDefault().getBreakpointManager().addBreakpointListener(this);
	}

	@Override
	public void dispose() {
		DebugPlugin.getDefault().getBreakpointManager().removeBreakpointListener(this);
		super.dispose();
	}

	@Override
	protected String getActionId() {
		return IDebugView.SELECT_ALL_ACTION;
	}
}
