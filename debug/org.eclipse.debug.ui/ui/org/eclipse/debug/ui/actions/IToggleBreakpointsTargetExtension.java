/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
import org.eclipse.ui.IWorkbenchPart;

/**
 * Extension interface for {@link org.eclipse.debug.ui.actions.IToggleBreakpointsTarget}.
 * This interface provides the ability to selectively create any type of breakpoint
 * when invoked, rather than a specific type of breakpoint (for example, a line
 * breakpoint). This allows targets to choose the type of breakpoint to create
 * when the user double-clicks in the vertical ruler.
 * <p>
 * Clients implementing <code>IToggleBreakpointsTarget</code> may optionally
 * implement this interface.
 * </p>
 * @since 3.1
 * @see org.eclipse.debug.ui.actions.ToggleBreakpointAction
 */
public interface IToggleBreakpointsTargetExtension extends IToggleBreakpointsTarget {

	/**
	 * Creates new line breakpoints or removes existing breakpoints.
	 * The selection varies depending on the given part. For example,
	 * a text selection is provided for text editors, and a structured
	 * selection is provided for tree views, and may be a multi-selection.
	 *
	 * @param part the part on which the action has been invoked
	 * @param selection selection on which line breakpoints should be toggled
	 * @throws CoreException if unable to perform the action
	 */
	void toggleBreakpoints(IWorkbenchPart part, ISelection selection) throws CoreException;

	/**
	 * Returns whether line breakpoints can be toggled on the given selection.
	 * The selection varies depending on the given part. For example,
	 * a text selection is provided for text editors, and a structured
	 * selection is provided for tree views, and may be a multi-selection.
	 *
	 * @param part the part on which the action has been invoked
	 * @param selection selection on which line breakpoints may be toggled
	 * @return whether line breakpoints can be toggled on the given selection
	 */
	boolean canToggleBreakpoints(IWorkbenchPart part, ISelection selection);

}
