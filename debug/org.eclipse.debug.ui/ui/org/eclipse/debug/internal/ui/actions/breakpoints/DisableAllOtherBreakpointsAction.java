/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.actions.breakpoints;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.debug.internal.ui.breakpoints.provisional.IBreakpointContainer;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;

public class DisableAllOtherBreakpointsAction extends EnableBreakpointsAction {

	/**
	 * If this action can enable breakpoints
	 *
	 * @return always <code>false</code>
	 */
	@Override
	protected boolean isEnableAction() {
		return false;
	}

	@Override
	public void run(IAction action) {
		IStructuredSelection selection = getSelection();
		if (selection.isEmpty()) {
			return;
		}
		final List<Object> selectedBreakpoints = selection.toList();
		final MultiStatus ms = new MultiStatus(DebugUIPlugin.getUniqueIdentifier(), DebugException.REQUEST_FAILED,
				ActionMessages.EnableBreakpointAction_Enable_breakpoint_s__failed_2, null);
		IWorkspaceRunnable runnable = monitor -> {
			try {
				Set<IBreakpoint> excludedBreakpoints = new HashSet<>();
				for (Object selectedObj : selectedBreakpoints) {
					if (selectedObj instanceof IBreakpoint breakPoint && breakPoint.isEnabled()) {
						excludedBreakpoints.add(breakPoint);
					}
					if (selectedObj instanceof IBreakpointContainer breakPointContainer) {
						for (IBreakpoint bp : breakPointContainer.getBreakpoints()) {
							if (bp.isEnabled()) {
								excludedBreakpoints.add(bp);
							}
						}
					}
				}
				for (IBreakpoint brk : DebugPlugin.getDefault().getBreakpointManager().getBreakpoints()) {
					if (!excludedBreakpoints.contains(brk)) {
						brk.setEnabled(false);
					}
				}

			} catch (CoreException e) {
				ms.merge(e.getStatus());
			}
		};

		try {
			ResourcesPlugin.getWorkspace().run(runnable, null, 0, new NullProgressMonitor());
		} catch (CoreException e) {
			DebugUIPlugin.log(e);
		}

		if (!ms.isOK()) {
			IWorkbenchWindow window = DebugUIPlugin.getActiveWorkbenchWindow();
			if (window != null) {
				DebugUIPlugin.errorDialog(window.getShell(),
						ActionMessages.EnableBreakpointAction_Enabling_breakpoints_3,
						ActionMessages.EnableBreakpointAction_Exceptions_occurred_enabling_the_breakpoint_s___4, ms); //
			} else {
				DebugUIPlugin.log(ms);
			}
		}
	}
}
