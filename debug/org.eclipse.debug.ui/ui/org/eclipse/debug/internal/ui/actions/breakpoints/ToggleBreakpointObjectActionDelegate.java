/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
 *     Wind River Systems - added support for IToggleBreakpointsTargetFactory
 *******************************************************************************/
package org.eclipse.debug.internal.ui.actions.breakpoints;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;

/**
 * A toggle breakpoint action that can be contributed to an object. The action
 * will perform a toggle breakpoint operation for a selected object.
 *
 * <p>Clients may subclass this class.</p>
 *
 * @since 3.0
 */
public abstract class ToggleBreakpointObjectActionDelegate implements IObjectActionDelegate, IActionDelegate2 {

	private IWorkbenchPart fPart;
	private IStructuredSelection fSelection;

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		fPart = targetPart;
	}

	@Override
	public void run(IAction action) {
		IToggleBreakpointsTarget target =
			DebugUITools.getToggleBreakpointsTargetManager().getToggleBreakpointsTarget(fPart, fSelection);
		if (target != null) {
			try {
				performAction(target, fPart, fSelection);
			} catch (CoreException e) {
				DebugPlugin.log(e);
			}
		}
	}

	/**
	 * Performs the operation specific to this action.
	 *
	 * @param target adapter to toggle breakpoints
	 * @param part the active part
	 * @param selection the selection in the active part
	 * @exception CoreException if an exception occurs
	 */
	protected abstract void performAction(IToggleBreakpointsTarget target, IWorkbenchPart part, ISelection selection) throws CoreException;

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		boolean enabled = false;
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ss = (IStructuredSelection) selection;
			this.fSelection = ss;
			// selectionChagned() can sometimes be called before setActivePart().
			// Guard here against that possibility.
			if (fPart != null) {
				IToggleBreakpointsTarget target =
					DebugUITools.getToggleBreakpointsTargetManager().getToggleBreakpointsTarget(fPart, fSelection);
				enabled = target != null;
			}
		}
		action.setEnabled(enabled);
	}

	@Override
	public void init(IAction action) {
	}

	@Override
	public void dispose() {
		fSelection = null;
		fPart = null;
	}

	@Override
	public void runWithEvent(IAction action, Event event) {
		run(action);
	}
}
