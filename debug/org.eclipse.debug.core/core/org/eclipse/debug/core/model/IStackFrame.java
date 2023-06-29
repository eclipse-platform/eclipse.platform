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
package org.eclipse.debug.core.model;


import org.eclipse.debug.core.DebugException;

/**
 * A stack frame represents an execution context in a suspended thread.
 * A stack frame contains variables representing visible locals and arguments at
 * the current execution location. Minimally, a stack frame supports
 * the following:
 * <ul>
 * <li>suspend/resume (convenience to resume this stack frame's thread)
 * <li>stepping
 * <li>termination (convenience to terminate this stack frame's thread or debug target)
 * </ul>
 * <p>
 * A debug model implementation may choose to re-use or discard
 * stack frames on iterative thread suspensions. Clients
 * cannot assume that stack frames are identical or equal across
 * iterative thread suspensions and must check for equality on iterative
 * suspensions if they wish to re-use the objects.
 * </p>
 * <p>
 * A debug model implementation that preserves equality
 * across iterative suspensions may display more desirable behavior in
 * some clients. For example, if stack frames are preserved
 * while stepping, a UI client would be able to update the UI incrementally,
 * rather than collapse and redraw the entire list.
 * </p>
 * <p>
 * Clients may implement this interface.
 * </p>
 * @see IStep
 * @see ISuspendResume
 * @see ITerminate
 */
public interface IStackFrame extends IDebugElement, IStep, ISuspendResume, ITerminate {
	/**
	 * Returns the thread this stack frame is contained in.
	 *
	 * @return thread
	 * @since 2.0
	 */
	IThread getThread();
	/**
	 * Returns the visible variables in this stack frame. An empty
	 * collection is returned if there are no visible variables.
	 *
	 * @return collection of visible variables
	 * @exception DebugException if this method fails.  Reasons include:
	 * <ul><li>Failure communicating with the debug target.  The DebugException's
	 * status code contains the underlying exception responsible for
	 * the failure.</li>
	 * </ul>
	 * @since 2.0
	 */
	IVariable[] getVariables() throws DebugException;

	/**
	 * Returns whether this stack frame currently contains any visible variables.
	 *
	 * @return whether this stack frame currently contains any visible variables
	 * @exception DebugException if this method fails.  Reasons include:
	 * <ul><li>Failure communicating with the debug target.  The DebugException's
	 * status code contains the underlying exception responsible for
	 * the failure.</li>
	 * </ul>
	 * @since 2.0
	 */
	boolean hasVariables() throws DebugException;

	/**
	 * Returns the line number of the instruction pointer in
	 * this stack frame that corresponds to a line in an associated source
	 * element, or <code>-1</code> if line number information
	 * is unavailable.
	 *
	 * @return line number of instruction pointer in this stack frame, or
	 * <code>-1</code> if line number information is unavailable
	 * @exception DebugException if this method fails.  Reasons include:
	 * <ul><li>Failure communicating with the debug target.  The DebugException's
	 * status code contains the underlying exception responsible for
	 * the failure.</li>
	 * </ul>
	 */
	int getLineNumber() throws DebugException;

	/**
	 * Returns the index of the first character in the associated source
	 * element that corresponds to the current location of the instruction pointer
	 * in this stack frame, or <code>-1</code> if the information is unavailable.
	 * <p>
	 * If a debug model supports expression level stepping, the start/end
	 * character ranges are used to highlight the expression within a line
	 * that is being executed.
	 * </p>
	 * @return index of the first character in the associated source
	 * element that corresponds to the current location of the instruction pointer
	 * in this stack frame, or <code>-1</code> if the information is unavailable
	 * @exception DebugException if this method fails.  Reasons include:
	 * <ul><li>Failure communicating with the debug target.  The DebugException's
	 * status code contains the underlying exception responsible for
	 * the failure.</li>
	 * </ul>
	 * @since 2.0
	 */
	int getCharStart() throws DebugException;

	/**
	 * Returns the index of the last character in the associated source
	 * element that corresponds to the current location of the instruction pointer
	 * in this stack frame, or <code>-1</code> if the information is unavailable.
	 * <p>
	 * If a debug model supports expression level stepping, the start/end
	 * character ranges are used to highlight the expression within a line
	 * that is being executed.
	 * </p>
	 * @return index of the last character in the associated source
	 * element that corresponds to the current location of the instruction pointer
	 * in this stack frame, or <code>-1</code> if the information is unavailable
	 * @exception DebugException if this method fails.  Reasons include:
	 * <ul><li>Failure communicating with the debug target.  The DebugException's
	 * status code contains the underlying exception responsible for
	 * the failure.</li>
	 * </ul>
	 * @since 2.0
	 */
	int getCharEnd() throws DebugException;

	/**
	 * Returns the name of this stack frame. Name format is debug model
	 * specific, and should be specified by a debug model.
	 *
	 * @return this frame's name
	 * @exception DebugException if this method fails.  Reasons include:
	 * <ul><li>Failure communicating with the debug target.  The DebugException's
	 * status code contains the underlying exception responsible for
	 * the failure.</li>
	 * </ul>
	 */
	String getName() throws DebugException;

	/**
	 * Returns the register groups assigned to this stack frame,
	 * or an empty collection if no register groups are assigned
	 * to this stack frame.
	 *
	 * @return the register groups assigned to this stack frame
	 *  or an empty collection if no register groups are assigned
	 *  to this stack frame
	 * @exception DebugException if this method fails.  Reasons include:
	 * <ul><li>Failure communicating with the debug target.  The DebugException's
	 * status code contains the underlying exception responsible for
	 * the failure.</li>
	 * </ul>
	 * @since 2.0
	 */
	IRegisterGroup[] getRegisterGroups() throws DebugException;

	/**
	 * Returns whether this stack frame contains any register groups.
	 *
	 * @return whether this stack frame contains any visible register groups
	 * @exception DebugException if this method fails.  Reasons include:
	 * <ul><li>Failure communicating with the debug target.  The DebugException's
	 * status code contains the underlying exception responsible for
	 * the failure.</li>
	 * </ul>
	 * @since 2.0
	 */
	boolean hasRegisterGroups() throws DebugException;
}
