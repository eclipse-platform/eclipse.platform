/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
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
 *     Wind River Systems - adapted to use with IToggleBreakpiontsTargetFactory extension
 *******************************************************************************/
package org.eclipse.debug.ui.actions;

import java.util.Set;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;

/**
 * A toggle breakpoints target factory creates one or more toggle breakpoint
 * targets. The toggle breakpoints targets are used by toggle breakpoint actions
 * to create breakpoints appropriate for the active editor, debug session,
 * project, or selection.
 *
 * <p>
 * Toggle breakpoints target factories are contributed via the
 * <code>org.eclipse.debug.ui.toggleBreakpointsTargetFactories</code> extension
 * point. Following is an example of a detail pane factory extension:
 * </p>
 *
 * <pre>
 * &lt;extension point="org.eclipse.debug.ui.toggleBreakpointsTargetFactories"&gt;
 *    &lt;toggleTargetFactory
 *            id="com.example.ExampleBreakpointToggleTargetFactory"
 *            class="com.example.BreakpointToggleTargetFactory"&gt;
 *        &lt;enablement&gt;
 *           &lt;!-- Test the active debug context.  Enable only if the active context
 *                is an element from "Example" debugger, or if there is no debug context
 *                associated with the context element.  Also enable if debug context is
 *                empty --&gt;
 *           &lt;with variable="debugContext"&gt;
 *              &lt;iterate&gt;
 *               &lt;or&gt;
 *                  &lt;test property="org.eclipse.debug.ui.getModelIdentifier" value="com.example.model"/&gt;
 *                  &lt;test property="org.eclipse.debug.ui.getModelIdentifier" value=""/&gt;
 *               &lt;/or&gt;
 *           &lt;/iterate&gt;
 *           &lt;/with&gt;
 *           &lt;!-- If there is no active debug context.  Enable the breakpoint toggle for
 *                the "Example" editors --&gt;
 *           &lt;instanceof value="com.example.Editor"/&gt;
 *        &lt;/enablement&gt;
 *    &lt;/toggleTargetFactory&gt;
 * &lt;/extension&gt;
 * </pre>
 *
 * Clients contributing a toggle breakpoints target factory are intended to
 * implement this interface.
 *
 *
 * @see IToggleBreakpointsTarget
 * @see IToggleBreakpointsTargetExtension
 * @see org.eclipse.debug.ui.actions.IToggleBreakpointsTargetManager
 * @since 3.5
 */
public interface IToggleBreakpointsTargetFactory {
	/**
	 * Returns all possible types of toggle breakpoints targets that this
	 * factory can create for the given selection and part, possibly empty.
	 * Toggle breakpoints targets  are returned as a set of IDs.
	 *
	 * @param part The active part.
	 * @param selection The current selection
	 * @return Set of <code>String</code> IDs for possible toggle breakpoint
	 * targets, possibly empty
	 */
	Set<String> getToggleTargets(IWorkbenchPart part, ISelection selection);

	/**
	 * Returns the identifier of the default toggle breakpoints target to use
	 * for the given selection, or <code>null</code> if this factory has no
	 * preference.
	 *
	 * @param part The active part.
	 * @param selection The current selection
	 * @return a breakpoint toggle target identifier or <code>null</code>
	 */
	String getDefaultToggleTarget(IWorkbenchPart part, ISelection selection);

	/**
	 * Creates and returns a toggle breakpoint target corresponding to the
	 * given identifier that this factory can produce (according to
	 * {@link #getToggleTargets}).
	 *
	 * @param targetID The id of the toggle target to be created
	 * @return toggle target or <code>null</code> if one could not be created
	 */
	IToggleBreakpointsTarget createToggleTarget(String targetID);

	/**
	 * Returns a human readable name for the breakpoint toggle target associated with the
	 * given ID. Used to populate the context menu with meaningful names of the types of
	 * breakpoints created by the given target.
	 *
	 * @param targetID toggle breakpoints target identifier
	 * @return toggle target name
	 */
	String getToggleTargetName(String targetID);

	/**
	 * Returns a description for the breakpoint toggle target associated with the
	 * given ID or <code>null</code> if none.
	 *
	 * @param targetID toggle breakpoints target identifier
	 * @return toggle target name or <code>null</code> if none
	 */
	String getToggleTargetDescription(String targetID);

}
