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
package org.eclipse.debug.core;


import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.debug.core.model.IBreakpoint;

/**
 * A breakpoints listener is notified of breakpoint additions,
 * removals, and changes. Listeners register and unregister with the
 * breakpoint manager.
 * <p>
 * This interface is analogous to <code>IBreakpointListener</code> except
 * notifications are batched for more than one breakpoint when possible.
 * </p>
 * <p>
 * Clients may implement this interface.
 * </p>
 * @see IBreakpointManager
 * @since 2.1
 */

public interface IBreakpointsListener {

	/**
	 * Notifies this listener that the given breakpoints have been added
	 * to the breakpoint manager.
	 *
	 * @param breakpoints the added breakpoints
	 */
	void breakpointsAdded(IBreakpoint[] breakpoints);
	/**
	 * Notifies this listener that the given breakpoints have been removed
	 * from the breakpoint manager.
	 * If a breakpoint has been removed because it has been deleted,
	 * the associated marker delta is also provided.
	 *
	 * @param breakpoints the removed breakpoints
	 * @param deltas the associated marker deltas. Entries may be
	 *  <code>null</code> when a breakpoint is removed from the breakpoint
	 *  manager without being deleted
	 *
	 * @see org.eclipse.core.resources.IMarkerDelta
	 */
	void breakpointsRemoved(IBreakpoint[] breakpoints, IMarkerDelta[] deltas);

	/**
	 * Notifies this listener that the given breakpoints have
	 * changed, as described by the corresponding deltas.
	 *
	 * @param breakpoints the changed breakpoints
	 * @param deltas the marker deltas that describe the changes
	 *  with the markers associated with the given breakpoints. Entries
	 *  may be <code>null</code> when a breakpoint change does not generate
	 *  a marker delta
	 *
	 * @see org.eclipse.core.resources.IMarkerDelta
	 */
	void breakpointsChanged(IBreakpoint[] breakpoints, IMarkerDelta[] deltas);

}
