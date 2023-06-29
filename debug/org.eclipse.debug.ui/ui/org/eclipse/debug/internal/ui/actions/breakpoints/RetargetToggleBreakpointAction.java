/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTargetExtension;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;


/**
 * Global retargettable toggle breakpoint action.
 *
 * @since 3.3
 */
public class RetargetToggleBreakpointAction extends RetargetBreakpointAction {

	@Override
	protected void performAction(Object target, ISelection selection, IWorkbenchPart part) throws CoreException {
		if (target instanceof IToggleBreakpointsTargetExtension) {
			IToggleBreakpointsTargetExtension ext = (IToggleBreakpointsTargetExtension) target;
			ext.toggleBreakpoints(part, selection);
		} else {
			((IToggleBreakpointsTarget)target).toggleLineBreakpoints(part, selection);
		}
	}

	@Override
	protected boolean canPerformAction(Object target, ISelection selection, IWorkbenchPart part) {
		if (target instanceof IToggleBreakpointsTargetExtension) {
			IToggleBreakpointsTargetExtension ext = (IToggleBreakpointsTargetExtension) target;
			return ext.canToggleBreakpoints(part, selection);
		} else {
			return ((IToggleBreakpointsTarget)target).canToggleLineBreakpoints(part, selection);
		}
	}

	@Override
	protected String getOperationUnavailableMessage() {
		return Messages.RetargetToggleBreakpointAction_0;
	}
}
