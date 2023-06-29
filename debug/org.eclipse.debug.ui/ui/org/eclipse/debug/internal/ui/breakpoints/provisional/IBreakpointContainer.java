/*******************************************************************************
 * Copyright (c) 2008, 2009 Wind River Systems and others.
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
 *     Patrick Chuong (Texas Instruments) - Improve usability of the breakpoint view (Bug 238956)
 *******************************************************************************/
package org.eclipse.debug.internal.ui.breakpoints.provisional;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.model.IBreakpoint;

/**
 * Interface representing a breakpoint category container elements in
 * Breakpoints view. Clients which provide custom content in the
 * Breakpoints view may implement this interface to represent container
 * elements.  This will allow the breakpoints view to properly handle
 * drag-and-drop and copy-paste operations.
 *
 * @since 3.6
 */
public interface IBreakpointContainer {

	/**
	 * Returns the breakpoint organizer that this container uses.
	 */
	IBreakpointOrganizer getOrganizer();

	/**
	 * Returns the breakpoint category that this container is based on.
	 * @return
	 */
	IAdaptable getCategory();

	/**
	 * Returns whether this breakpoint container contains the given breakpoint.
	 *
	 * @param breakpoint Breakpoint to check
	 * @return Returns <code>true</code> if this container contains the
	 * given breakpoint.
	 */
	boolean contains(IBreakpoint breakpoint);

	/**
	 * Returns the array of breakpoints in this container.
	 */
	IBreakpoint[] getBreakpoints();
}
