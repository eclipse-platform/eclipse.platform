/*******************************************************************************
 * Copyright (c) 2016 IBM Corporation and others.
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


import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;

/**
 * TriggerPoint is a breakpoint property which defines a dependency relationship
 * between all the breakpoints of a workspace and trigger points defined in a
 * workspace. TriggerPoint defines the availability of breakpoints to be
 * suspended based on the order of hits of breakpoints. If there are active
 * TriggerPoints, breakpoints can not be suspended.
 *
 * @since 3.11
 */
public interface ITriggerPoint extends IAdaptable {
	/**
	 * Persisted breakpoint marker attribute (value
	 * <code>"org.eclipse.debug.core.triggerpoint"</code>). The attribute is a
	 * <code>boolean</code> corresponding to whether a breakpoint is a trigger
	 * breakpoint for the workspace.
	 *
	 * @see org.eclipse.core.resources.IMarker#getAttribute(String, boolean)
	 *
	 */
	String TRIGGERPOINT = "org.eclipse.debug.core.triggerpoint"; //$NON-NLS-1$

	/**
	 * Returns whether this breakpoint is defined as a trigger point in the
	 * workspace.
	 *
	 * @return whether this breakpoint is a trigger point
	 * @exception CoreException if unable to access the associated attribute on
	 *                this breakpoint's underlying marker
	 */
	boolean isTriggerPoint() throws CoreException;

	/**
	 * Sets whether this breakpoint is to be treated as a trigger point for the
	 * workspace. If it is a trigger point, then the
	 * {@link org.eclipse.debug.core.model.ITriggerPoint} attribute on this
	 * breakpoint's marker is set to <code>true</code>.
	 *
	 * @param trigger whether this breakpoint is to be treated as trigger point
	 *            for the workspace
	 * @exception CoreException if unable to set the associated attribute on
	 *                this breakpoint's underlying marker.
	 */
	void setTriggerPoint(boolean trigger) throws CoreException;

}


