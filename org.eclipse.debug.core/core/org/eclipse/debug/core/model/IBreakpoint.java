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
package org.eclipse.debug.core.model;


import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugPlugin;

/**
 * A breakpoint is capable of suspending the execution of a
 * program at a specific location when a program is running
 * in debug mode. Each breakpoint has an associated marker which
 * stores and persists all attributes associated with a breakpoint.
 * <p>
 * A breakpoint is defined in two parts:
 * <ol>
 * <li>By an extension of kind <code>"org.eclipse.debug.core.breakpoints"</code></li>
 * <li>By a marker definition that corresponds to the above breakpoint extension</li>
 * </ol>
 * <p>
 * For example, following is a definition of corresponding breakpoint
 * and breakpoint marker definitions. Note that the <code>markerType</code>
 * attribute defined by the breakpoint extension corresponds to the
 * type of the marker definition.
 * <pre>
 * &lt;extension point="org.eclipse.debug.core.breakpoints"&gt;
 *   &lt;breakpoint
 *      id="com.example.Breakpoint"
 *      class="com.example.Breakpoint"
 *      markerType="com.example.BreakpointMarker"&gt;
 *   &lt;/breakpoint&gt;
 * &lt;/extension&gt;
 * &lt;extension
 *      point="org.eclipse.core.resources.markers"
 *      id="com.example.BreakpointMarker"
 *      name="Example Breakpoint"&gt;
 *   &lt;super type="org.eclipse.debug.core.breakpointMarker"/&gt;
 *   &lt;attribute name="exampleAttribute"/&gt;
 * &lt;/extension&gt;
 * </pre>
 * <p>
 * The breakpoint manager instantiates persisted breakpoints by
 * traversing all markers that are a subtype of
 * <code>"org.eclipse.debug.core.breakpointMarker"</code>, and
 * instantiating the class defined by the <code>class</code> attribute
 * on the associated breakpoint extension. The method <code>setMarker</code>
 * is then called to associate a marker with the breakpoint.
 * </p>
 * <p>
 * Breakpoints may or may not be registered with the breakpoint manager, and
 * are persisted and restored as such. Since marker definitions only allow
 * all or none of a specific marker type to be persisted, breakpoints define
 * a <code>PERSISTED</code> attribute for selective persistence of breakpoints
 * of the same type.
 * </p>
 *
 * @since 2.0
 */

public interface IBreakpoint extends IAdaptable {

	/**
	 * Root breakpoint marker type
	 * (value <code>"org.eclipse.debug.core.breakpointMarker"</code>).
	 */
	String BREAKPOINT_MARKER = DebugPlugin.getUniqueIdentifier() + ".breakpointMarker"; //$NON-NLS-1$

	/**
	 * Line breakpoint marker type
	 * (value <code>"org.eclipse.debug.core.lineBreakpoint"</code>).
	 */
	String LINE_BREAKPOINT_MARKER = DebugPlugin.getUniqueIdentifier() + ".lineBreakpointMarker"; //$NON-NLS-1$

	/**
	 * Enabled breakpoint marker attribute (value <code>"org.eclipse.debug.core.enabled"</code>).
	 * The attribute is a <code>boolean</code> corresponding to the
	 * enabled state of a breakpoint.
	 *
	 * @see org.eclipse.core.resources.IMarker#getAttribute(String, boolean)
	 */
	String ENABLED= "org.eclipse.debug.core.enabled"; //$NON-NLS-1$

	/**
	 * Debug model identifier breakpoint marker attribute (value <code>"org.eclipse.debug.core.id"</code>).
	 * The attribute is a <code>String</code> corresponding to the
	 * identifier of the debug model a breakpoint is associated with.
	 */
	String ID= "org.eclipse.debug.core.id"; //$NON-NLS-1$

	/**
	 * Registered breakpoint marker attribute (value <code>"org.eclipse.debug.core.registered"</code>).
	 * The attribute is a <code>boolean</code> corresponding to
	 * whether a breakpoint has been registered with the breakpoint manager.
	 *
	 * @see org.eclipse.core.resources.IMarker#getAttribute(String, boolean)
	 */
	String REGISTERED= "org.eclipse.debug.core.registered"; //$NON-NLS-1$

	/**
	 * Persisted breakpoint marker attribute (value <code>"org.eclipse.debug.core.persisted"</code>).
	 * The attribute is a <code>boolean</code> corresponding to
	 * whether a breakpoint is to be persisted across workspace
	 * invocations.
	 *
	 * @see org.eclipse.core.resources.IMarker#getAttribute(String, boolean)
	 */
	String PERSISTED= "org.eclipse.debug.core.persisted"; //$NON-NLS-1$

	/**
	 * Deletes this breakpoint's underlying marker, and removes
	 * this breakpoint from the breakpoint manager.
	 *
	 * @exception CoreException if unable to delete this breakpoint's
	 *  underlying marker
	 */
	void delete() throws CoreException;

	/**
	 * Returns the marker associated with this breakpoint, or
	 * <code>null</code> if no marker is associated with this breakpoint.
	 *
	 * @return associated marker, or <code>null</code> if there is
	 * 	no associated marker.
	 */
	IMarker getMarker();
	/**
	 * Sets the marker associated with this breakpoint. This method is
	 * called once at breakpoint creation.
	 *
	 * @param marker the marker to associate with this breakpoint
	 * @exception CoreException if an error occurs accessing the marker
	 */
	void setMarker(IMarker marker) throws CoreException;
	/**
	 * Returns the identifier of the debug model this breakpoint is
	 * associated with.
	 *
	 * @return the identifier of the debug model this breakpoint is
	 * 	associated with
	 */
	String getModelIdentifier();
	/**
	 * Returns whether this breakpoint is enabled
	 *
	 * @return whether this breakpoint is enabled
	 * @exception CoreException if unable to access the associated
	 *  attribute from this breakpoint's underlying marker
	 */
	boolean isEnabled() throws CoreException;
	/**
	 * Sets the enabled state of this breakpoint. This has no effect
	 * if the current enabled state is the same as specified by the
	 * enabled parameter.
	 *
	 * @param enabled  whether this breakpoint should be enabled
	 * @exception CoreException if unable to set the associated attribute on
	 *  this breakpoint's underlying marker.
	 */
	void setEnabled(boolean enabled) throws CoreException;

	/**
	 * Returns whether this breakpoint is currently registered with
	 * the breakpoint manager.
	 *
	 * @return whether this breakpoint is currently registered with
	 *  the breakpoint manager
	 * @exception CoreException if unable to access the associated
	 *  attribute on this breakpoint's underlying marker
	 */
	boolean isRegistered() throws CoreException;

	/**
	 * Sets whether this breakpoint is currently registered with the
	 * breakpoint manager.
	 *
	 * @param registered whether this breakpoint is registered with the
	 *   breakpoint manager
	 * @exception CoreException if unable to set the associated attribute
	 *  on this breakpoint's underlying marker.
	 */
	void setRegistered(boolean registered) throws CoreException;

	/**
	 * Returns whether this breakpoint is to be persisted across
	 * workspace invocations, or when a project is closed and re-opened.
	 * Since marker definitions only allow all/none of a specific type
	 * of marker to be persisted (rather than selected markers of a
	 * specific type), breakpoints define this functionality.
	 *
	 * @return whether this breakpoint is to be persisted
	 * @exception CoreException if unable to access the associated attribute
	 *  on this breakpoint's underlying marker
	 */
	boolean isPersisted() throws CoreException;

	/**
	 * Sets whether this breakpoint is to be persisted across
	 * workspace invocations, or when a project is closed and re-opened.
	 * Has no effect if this breakpoint's marker definition is defined as not
	 * persisted. Sets the underlying <code>TRANSIENT</code> attribute on this
	 * breakpoint's marker to <code>true</code>.
	 *
	 * @param registered whether this breakpoint is to be persisted across
	 * workspace invocations
	 * @exception CoreException if unable to set the associated attribute on
	 *  this breakpoint's underlying marker.
	 */
	void setPersisted(boolean registered) throws CoreException;

}


