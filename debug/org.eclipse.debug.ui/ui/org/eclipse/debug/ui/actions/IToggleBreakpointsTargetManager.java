/*******************************************************************************
 * Copyright (c) 2012, 2013 Wind River Systems and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *     IBM Corporation - bug fixing
 *******************************************************************************/
package org.eclipse.debug.ui.actions;

import java.util.Set;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Organizes the toggle breakpoints target factories contributed through the
 * extension point and keeps track of the toggle breakpoints target that
 * the factories produce.  The manager is accessed as a singleton through
 * the <code>getDefault()</code> method.
 * <p>
 * The adapter mechanism for obtaining a toggle breakpoints target is
 * still supported through a specialized toggle target factory.  Targets
 * contributed through this mechanism are labeled as "Default" in the UI.
 * </p>
 * <p>
 * Clients should call {@link org.eclipse.debug.ui.DebugUITools#getToggleBreakpointsTargetManager()}
 * for an instance of this instance.
 * </p>
 *
 * @see IToggleBreakpointsTargetFactory
 * @see IToggleBreakpointsTarget
 * @see IToggleBreakpointsTargetExtension
 *
 * @since 3.8
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IToggleBreakpointsTargetManager {

	/**
	 * Returns the set of <code>String</code> IDs of toggle breakpoint targets,
	 * which are enabled for the given active part and selection.  The IDs can be used
	 * to create the {@link IToggleBreakpointsTarget} instance.
	 * @param part active part
	 * @param selection active selection in part
	 * @return Set of toggle target IDs or an empty set
	 */
	Set<String> getEnabledToggleBreakpointsTargetIDs(IWorkbenchPart part, ISelection selection);

	/**
	 * Returns the ID of the calculated preferred toggle breakpoints target for the
	 * given active part and selection.  The returned ID is chosen based on factory
	 * enablement, whether the target is a default one, and on user choice.
	 * @param part active part
	 * @param selection active selection in part
	 * @return The toggle target IDs or null if none.
	 */
	String getPreferredToggleBreakpointsTargetID(IWorkbenchPart part, ISelection selection);

	/**
	 * Given the ID of toggle breakpoint target, this method will try to find the factory
	 * that creates it and return an instance of it.
	 *
	 * @param part The workbench part in which toggle target is to be used
	 * @param selection The active selection to use with toggle target
	 * @return The instantiated target or null
	 */
	IToggleBreakpointsTarget getToggleBreakpointsTarget(IWorkbenchPart part, ISelection selection);

	/**
	 * Given the ID of a toggle breakpoints target, this method will try
	 * to find the factory that creates it and ask it for the name of it.
	 *
	 * @param id The ID of the requested toggle breakpoint target.
	 * @return The name of the target.
	 */
	String getToggleBreakpointsTargetName(String id);

	/**
	 * Given the ID of a toggle breakpoints target, this method will try
	 * to find the factory that creates it and ask it for the description of it.
	 *
	 * @param id The ID of the requested toggle breakpoint target.
	 * @return The description of the target or null.
	 */
	String getToggleBreakpointsTargetDescription(String id);

	/**
	 * Adds the given listener to the list of listeners notified when the preferred
	 * toggle breakpoints targets change.
	 * @param listener The listener to add.
	 */
	void addChangedListener(IToggleBreakpointsTargetManagerListener listener);

	/**
	 * Removes the given listener from the list of listeners notified when the preferred
	 * toggle breakpoints targets change.
	 * @param listener The listener to add.
	 */
	void removeChangedListener(IToggleBreakpointsTargetManagerListener listener);
}
