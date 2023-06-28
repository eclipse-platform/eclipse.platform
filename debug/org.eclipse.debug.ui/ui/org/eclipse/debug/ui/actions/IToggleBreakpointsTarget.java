/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
 * An adapter to support breakpoint creation/deletion for an active part
 * or selection within an active part. The debug platform provides
 * retargettable actions for toggling line breakpoints, method breakpoints,
 * and watchpoints. A debug implementation can plug into the global actions
 * by providing an adapter of this type on relevant parts and objects.
 * The debug platform provides one command and key binding for each breakpoint
 * operation.
 * <p>
 * When a part is activated, a retargettable action asks the part
 * for its <code>IToggleBreakpointTarget</code> adapter. If one exists,
 * that adapter is delegated to to perform breakpoint operations when
 * the user invokes an associated action. If an adapter does not exist
 * for the part, the retargettable actions asks selected objects in the
 * active part for an adapter. Generally, a debug implementation will
 * provide breakpoint adapters for relevant editors and model objects.
 * </p>
 * <p>
 * Clients are intended to implement this interface and provide instances as
 * an adapter on applicable parts (for example, editors) and objects (for
 * example, methods and fields) that support breakpoint toggling.
 * </p>
 * @since 3.0
 */
public interface IToggleBreakpointsTarget {

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
	void toggleLineBreakpoints(IWorkbenchPart part, ISelection selection) throws CoreException;

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
	boolean canToggleLineBreakpoints(IWorkbenchPart part, ISelection selection);

	/**
	 * Creates new method breakpoints or removes existing breakpoints.
	 * The selection varies depending on the given part. For example,
	 * a text selection is provided for text editors, and a structured
	 * selection is provided for tree views, and may be a multi-selection.
	 *
	 * @param part the part on which the action has been invoked
	 * @param selection selection on which method breakpoints should be toggled
	 * @throws CoreException if unable to perform the action
	 */
	void toggleMethodBreakpoints(IWorkbenchPart part, ISelection selection) throws CoreException;

	/**
	 * Returns whether method breakpoints can be toggled on the given selection.
	 * The selection varies depending on the given part. For example,
	 * a text selection is provided for text editors, and a structured
	 * selection is provided for tree views, and may be a multi-selection.
	 *
	 * @param part the part on which the action has been invoked
	 * @param selection selection on which method breakpoints may be toggled
	 * @return whether method breakpoints can be toggled on the given selection
	 */
	boolean canToggleMethodBreakpoints(IWorkbenchPart part, ISelection selection);

	/**
	 * Creates new watchpoints or removes existing breakpoints.
	 * The selection varies depending on the given part. For example,
	 * a text selection is provided for text editors, and a structured
	 * selection is provided for tree views, and may be a multi-selection.
	 *
	 * @param part the part on which the action has been invoked
	 * @param selection selection on which watchpoints should be toggled
	 * @throws CoreException if unable to perform the action
	 */
	void toggleWatchpoints(IWorkbenchPart part, ISelection selection) throws CoreException;

	/**
	 * Returns whether watchpoints can be toggled on the given selection.
	 * The selection varies depending on the given part. For example,
	 * a text selection is provided for text editors, and a structured
	 * selection is provided for tree views, and may be a multi-selection.
	 *
	 * @param part the part on which the action has been invoked
	 * @param selection selection on which watchpoints may be toggled
	 * @return whether watchpoints can be toggled on the given selection
	 */
	boolean canToggleWatchpoints(IWorkbenchPart part, ISelection selection);
}
