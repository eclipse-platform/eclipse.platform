/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;


/**
 * Global retargettable toggle method breakpoint action.
 *
 * @since 3.0
 */
public class RetargetMethodBreakpointAction extends RetargetBreakpointAction {

	@Override
	protected void performAction(Object target, ISelection selection, IWorkbenchPart part) throws CoreException {
		((IToggleBreakpointsTarget)target).toggleMethodBreakpoints(part, selection);
	}
	@Override
	protected boolean canPerformAction(Object target, ISelection selection, IWorkbenchPart part) {
		return ((IToggleBreakpointsTarget)target).canToggleMethodBreakpoints(part, selection);
	}
	@Override
	protected String getOperationUnavailableMessage() {
		return Messages.RetargetMethodBreakpointAction_0;
	}
}
