/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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


import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IBreakpointImportParticipant;

/**
 * The breakpoint manager manages the collection of breakpoints
 * in the workspace. A breakpoint suspends the execution of a
 * program being debugged. The kinds of breakpoints supported by each
 * debug architecture and the information required to create those
 * breakpoints is defined by each debug architecture.
 * Breakpoint creation is a client responsibility.
 * <p>
 * Clients interested in breakpoint change notification may
 * register with the breakpoint manager - see
 * <code>IBreakpointListener</code> and <code>IBreakpointsListener</code>
 * </p>
 * @see org.eclipse.debug.core.IBreakpointListener
 * @see org.eclipse.debug.core.IBreakpointsListener
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IBreakpointManager {
	/**
	 * Adds the given breakpoint to the collection of registered breakpoints
	 * in the workspace and notifies all registered listeners. This has no effect
	 * if the given breakpoint is already registered.
	 *
	 * @param breakpoint the breakpoint to add
	 *
	 * @exception CoreException if adding fails. Reasons include:<ul>
	 * <li>CONFIGURATION_INVALID - the required <code>MODEL_IDENTIFIER</code> attribute
	 * 	is not set on the breakpoint marker.</li>
	 * <li>A <code>CoreException</code> occurred while verifying the <code>MODEL_IDENTIFIER</code>
	 *	attribute.</li>
	 * </ul>
	 * @since 2.0
	 */
	void addBreakpoint(IBreakpoint breakpoint) throws CoreException;

	/**
	 * Adds the given breakpoints to the collection of registered breakpoints
	 * in the workspace and notifies all registered listeners. Has no effect
	 * on breakpoints that are already registered.
	 *
	 * @param breakpoints the breakpoints to add
	 *
	 * @exception CoreException if adding fails. Reasons include:<ul>
	 * <li>CONFIGURATION_INVALID - the required <code>MODEL_IDENTIFIER</code> attribute
	 * 	is not set on a breakpoint marker.</li>
	 * <li>A <code>CoreException</code> occurred while verifying a <code>MODEL_IDENTIFIER</code>
	 *	attribute.</li>
	 * </ul>
	 * @since 2.1
	 */
	void addBreakpoints(IBreakpoint[] breakpoints) throws CoreException;

	/**
	 * Returns the breakpoint associated with the given marker or
	 * <code>null</code> if no such breakpoint exists
	 *
	 * @param marker the marker
	 * @return the breakpoint associated with the marker
	 * 	or <code>null</code> if none exists
	 * @since 2.0
	 */
	IBreakpoint getBreakpoint(IMarker marker);

	/**
	 * Returns a collection of all registered breakpoints.
	 * Returns an empty array if no breakpoints are registered.
	 *
	 * @return an array of breakpoints
	 * @since 2.0
	 */
	IBreakpoint[] getBreakpoints();

	/**
	 * Returns whether there are any registered breakpoints.
	 *
	 * @return whether there are any registered breakpoints
	 * @since 2.0
	 */
	boolean hasBreakpoints();

	/**
	 * Returns a collection of all breakpoints registered for the
	 * given debug model. Answers an empty array if no breakpoints are registered
	 * for the given debug model.
	 *
	 * @param modelIdentifier identifier of a debug model plug-in
	 * @return an array of breakpoints
	 * @since 2.0
	 */
	IBreakpoint[] getBreakpoints(String modelIdentifier);

	/**
	 * Returns whether the given breakpoint is currently
	 * registered with this breakpoint manager.
	 *
	 * @param breakpoint a breakpoint
	 * @return whether the breakpoint is registered
	 * @since 2.0
	 */
	boolean isRegistered(IBreakpoint breakpoint);

	/**
	 * Notifies all registered listeners that the given
	 * breakpoint has changed. Has no effect if the given
	 * breakpoint is not currently registered.
	 *
	 * This method is intended to be used when a breakpoint
	 * attribute is changed that does not alter the breakpoint's
	 * underlying marker, that is, when notification will not occur
	 * via the marker delta mechanism.
	 *
	 * @param breakpoint the breakpoint that has changed.
	 * @since 2.0
	 */
	void fireBreakpointChanged(IBreakpoint breakpoint);

	/**
	 * Removes the given breakpoint from the breakpoint manager, deletes
	 * the marker associated with the breakpoint if the <code>delete</code> flag
	 * is <code>true</code>, and notifies all registered
	 * listeners. Has no effect if the given breakpoint is not currently
	 * registered.
	 *
	 * @param breakpoint the breakpoint to remove
	 * @param delete whether to delete the marker associated with the
	 *  breakpoint
	 * @exception CoreException if an exception occurs while deleting the
	 * 	underlying marker.
	 * @since 2.0
	 */
	void removeBreakpoint(IBreakpoint breakpoint, boolean delete) throws CoreException;

	/**
	 * Removes the given breakpoints from the breakpoint manager, deletes
	 * the markers associated with the breakpoints if the <code>delete</code> flag
	 * is <code>true</code>, and notifies all registered
	 * listeners. Has no effect on breakpoints not currently
	 * registered.
	 *
	 * @param breakpoints the breakpoints to remove
	 * @param delete whether to delete the markers associated with the
	 *  breakpoints
	 * @exception CoreException if an exception occurs while deleting an
	 * 	underlying marker.
	 * @since 2.1
	 */
	void removeBreakpoints(IBreakpoint[] breakpoints, boolean delete) throws CoreException;

	/**
	 * Adds the given listener to the collection of registered breakpoint listeners.
	 * Has no effect if an identical listener is already registered.
	 *
	 * @param listener the listener to add
	 */
	void addBreakpointListener(IBreakpointListener listener);

	/**
	 * Removes the given listener from the collection of registered breakpoint listeners.
	 * Has no effect if an identical listener is not already registered.
	 *
	 * @param listener the listener to remove
	 */
	void removeBreakpointListener(IBreakpointListener listener);

	/**
	 * Adds the given listener to the collection of registered breakpoint listeners.
	 * Has no effect if an identical listener is already registered.
	 *
	 * @param listener the listener to add
	 * @since 2.1
	 */
	void addBreakpointListener(IBreakpointsListener listener);

	/**
	 * Removes the given listener from the collection of registered breakpoint listeners.
	 * Has no effect if an identical listener is not already registered.
	 *
	 * @param listener the listener to remove
	 * @since 2.1
	 */
	void removeBreakpointListener(IBreakpointsListener listener);

	/**
	 * Adds the given listener to the collection of registered breakpoint manager
	 * listeners. Has no effect if an identical listener is already registered.
	 *
	 * @param listener the listener to add
	 * @since 3.0
	 */
	void addBreakpointManagerListener(IBreakpointManagerListener listener);

	/**
	 * Removes the given listener from the collection of registered breakpoint manager
	 * listeners. Has no effect if an identical listener is not already registered.
	 *
	 * @param listener the listener to remove
	 * @since 3.0
	 */
	void removeBreakpointManagerListener(IBreakpointManagerListener listener);

	/**
	 * Returns whether or not this breakpoint manager is enabled.
	 * When a breakpoint manager is enabled, all breakpoints
	 * should be honored. When it is disabled, breakpoints should
	 * not be honored, regardless of each breakpoint's enabled state.
	 *
	 * @return whether or not this breakpoint manager is enabled
	 * @since 3.0
	 */
	boolean isEnabled();

	/**
	 * Sets the enabled state of this breakpoint manager. When
	 * enabled, breakpoints should be honoured. When disabled, all
	 * breakpoints should be ignored.
	 *
	 * @param enabled whether this breakpoint manager should be
	 *  enabled
	 * @since 3.0
	 */
	void setEnabled(boolean enabled);

	/**
	 * Returns the name (user readable String) of the given
	 * breakpoint's type or <code>null</code> if none has been
	 * specified.
	 *
	 * @param breakpoint the breakpoint
	 * @return the name of the given breakpoint's type or <code>null</code>
	 * @since 3.1
	 */
	String getTypeName(IBreakpoint breakpoint);

	/**
	 * Returns an array of {@link IBreakpointImportParticipant}s for the given
	 * breakpoint marker id, never <code>null</code>.
	 *
	 * @param markertype the {@link String} identifier of the marker type
	 * @return an array of {@link IBreakpointImportParticipant}s for the given marker type,
	 * never <code>null</code>
	 * @throws CoreException if an exception occurs
	 * @since 3.5
	 */
	IBreakpointImportParticipant[] getImportParticipants(String markertype) throws CoreException;

	/**
	 * Returns the triggers for the breakpoints associated with the workspace or
	 * <code>null</code> if no such breakpoint exists
	 *
	 * @return the triggers breakpoint associated with the workspace or
	 *         <code>null</code> if none exists
	 * @since 3.11
	 */
	IBreakpoint[] getTriggerPoints();

	/**
	 * Adds the given breakpoint as the triggering breakpoint in the workspace
	 * and notifies all registered listeners.
	 *
	 * @param breakpoint the breakpoint to be added as the trigger point
	 *
	 * @exception CoreException if adding fails. Reasons include:
	 *                <ul>
	 *                <li>CONFIGURATION_INVALID - the required
	 *                <code>MODEL_IDENTIFIER</code> attribute is not set on the
	 *                breakpoint marker.</li>
	 *                <li>A <code>CoreException</code> occurred while verifying
	 *                the <code>MODEL_IDENTIFIER</code> attribute.</li>
	 *                </ul>
	 * @since 3.11
	 */
	void addTriggerPoint(IBreakpoint breakpoint) throws CoreException;

	/**
	 * Removes the given breakpoint as the trigger breakpoint in the workspace
	 * and notifies all registered listeners.
	 *
	 * @param breakpoint the breakpoint to be removed as the trigger point
	 *
	 * @exception CoreException if adding fails. Reasons include:
	 *                <ul>
	 *                <li>CONFIGURATION_INVALID - the required
	 *                <code>MODEL_IDENTIFIER</code> attribute is not set on the
	 *                breakpoint marker.</li>
	 *                <li>A <code>CoreException</code> occurred while verifying
	 *                the <code>MODEL_IDENTIFIER</code> attribute.</li>
	 *                </ul>
	 * @since 3.11
	 */
	void removeTriggerPoint(IBreakpoint breakpoint) throws CoreException;

	/**
	 * Removes all the trigger points from the breakpoint manager.
	 *
	 * @exception CoreException if an exception occurs while deleting an
	 *                underlying marker.
	 * @since 3.11
	 */
	void removeAllTriggerPoints() throws CoreException;

	/**
	 * Returns whether a workspace has active TriggerPoints
	 *
	 * @return return <code>true</code> if a breakpoint has active triggers and
	 *         cannot suspend and return <code>false</code> otherwise.
	 * @since 3.11
	 */
	boolean hasActiveTriggerPoints();

	/**
	 * Revisit all the trigger points to activate/deactivate trigger points.
	 *
	 * @param triggerPoints list of trigger points to be deactivated or
	 *            <code>null</code> to deactivate all trigger points
	 * @param enable enable if <code>true</code> or disable if
	 *            <code>false</code>
	 * @since 3.11
	 */
	void enableTriggerPoints(IBreakpoint[] triggerPoints, boolean enable);

	/**
	 * Touch and refresh the display of all breakpoints.
	 *
	 * @since 3.11
	 */
	void refreshTriggerpointDisplay();

}


