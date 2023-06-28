/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package org.eclipse.debug.ui.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Extension interface for {@link org.eclipse.debug.ui.actions.IToggleBreakpointsTargetExtension}.
 * This interface provides the ability to inspect the key modifiers
 * being used when toggling a breakpoint. This allows targets to choose the type of breakpoint to create
 * when the user double-clicks in the vertical ruler.
 * <p>
 * Clients implementing <code>IToggleBreakpointsTarget</code> or <code>IToggleBreakpointsTargetExtension</code> may optionally
 * implement or adapt to this interface.
 * </p>
 * @since 3.8
 * @see org.eclipse.debug.ui.actions.ToggleBreakpointAction
 */
public interface IToggleBreakpointsTargetExtension2 extends IToggleBreakpointsTargetExtension {

	/**
	 * Creates or removes existing breakpoints based on any modifiers in the given {@link Event}.
	 * The selection varies depending on the given part. For example,
	 * a text selection is provided for text editors, and a structured
	 * selection is provided for tree views, and may be a multi-selection.
	 *
	 * @param part the part on which the action has been invoked
	 * @param selection selection on which line breakpoints should be toggled
	 * @param event the accompanying {@link Event} which can be <code>null</code> if unavailable
	 * @throws CoreException if unable to perform the action
	 */
	void toggleBreakpointsWithEvent(IWorkbenchPart part, ISelection selection, Event event) throws CoreException;

	/**
	 * Returns whether breakpoints can be toggled on the given selection with the given {@link Event}.
	 * The selection varies depending on the given part. For example,
	 * a text selection is provided for text editors, and a structured
	 * selection is provided for tree views, and may be a multi-selection.
	 *
	 * @param part the part on which the action has been invoked
	 * @param selection selection on which line breakpoints should be toggled
	 * @param event the accompanying {@link Event} which can be <code>null</code> if unavailable
	 * @return whether breakpoints can be toggled on the given selection with the given {@link Event}
	 */
	boolean canToggleBreakpointsWithEvent(IWorkbenchPart part, ISelection selection, Event event);
}
