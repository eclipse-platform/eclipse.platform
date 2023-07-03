/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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


import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.model.ITerminate;

/**
 * A launch is the result of launching a debug session
 * and/or one or more system processes.
 * <p>
 * Clients are not required to implement this interface - they should use the implementation
 * provided by the class <code>Launch</code>. However, clients may implement this interface
 * as required.
 * </p>
 * @see Launch
 * @see org.eclipse.debug.core.IProcessFactory
 */
public interface ILaunch extends ITerminate, IAdaptable {
	/**
	 * Returns the children of this launch - a collection
	 * of one or more debug targets and processes, possibly empty.
	 *
	 * @return an array (element type:<code>IDebugTarget</code> or <code>IProcess</code>),
	 * 	or an empty array
	 */
	Object[] getChildren();
	/**
	 * Returns the primary (first) debug target associated with this launch, or <code>null</code>
	 * if no debug target is associated with this launch. All debug targets
	 * associated with this launch may be retrieved by
	 * <code>getDebugTargets()</code>.
	 *
	 * @return the primary debug target associated with this launch, or <code>null</code>
	 */
	IDebugTarget getDebugTarget();

	/**
	 * Returns the processes that were launched,
	 * or an empty collection if no processes were launched.
	 *
	 * @return array of processes
	 */
	IProcess[] getProcesses();

	/**
	 * Returns all the debug targets associated with this launch,
	 * or an empty collection if no debug targets are associated
	 * with this launch. The primary debug target is the first
	 * in the collection (if any).
	 *
	 * @return array of debug targets
	 * @since 2.0
	 */
	IDebugTarget[] getDebugTargets();

	/**
	 * Adds the given debug target to this launch. Has no effect
	 * if the given debug target is already associated with this
	 * launch. Registered listeners are notified that this launch
	 * has changed.
	 *
	 * @param target debug target to add to this launch
	 * @since 2.0
	 */
	void addDebugTarget(IDebugTarget target);

	/**
	 * Removes the given debug target from this launch. Has no effect
	 * if the given debug target is not already associated with this
	 * launch. Registered listeners are notified that this launch
	 * has changed.
	 *
	 * @param target debug target to remove from this launch
	 * @since 2.0
	 */
	void removeDebugTarget(IDebugTarget target);

	/**
	 * Adds the given process to this launch. Has no effect
	 * if the given process is already associated with this
	 * launch. Registered listeners are notified that this launch
	 * has changed.
	 *
	 * @param process the process to add to this launch
	 * @since 2.0
	 */
	void addProcess(IProcess process);

	/**
	 * Removes the given process from this launch. Has no effect
	 * if the given process is not already associated with this
	 * launch. Registered listeners are notified that this launch
	 * has changed.
	 *
	 * @param process the process to remove from this launch
	 * @since 2.0
	 */
	void removeProcess(IProcess process);

	/**
	 * Returns the source locator to use for locating source elements for
	 * the debug target associated with this launch, or <code>null</code>
	 * if source lookup is not supported.
	 *
	 * @return the source locator
	 */
	ISourceLocator getSourceLocator();

	/**
	 * Sets the source locator to use for locating source elements for
	 * the debug target associated with this launch, or <code>null</code>
	 * if source lookup is not supported.
	 *
	 * @param sourceLocator source locator or <code>null</code>
	 * @since 2.0
	 */
	void setSourceLocator(ISourceLocator sourceLocator);

	/**
	 * Returns the mode of this launch - one of the mode constants defined by
	 * the launch manager.
	 *
	 * @return the launch mode
	 * @see ILaunchManager
	 */
	String getLaunchMode();

	/**
	 * Returns the configuration that was launched, or <code>null</code>
	 * if no configuration was launched.
	 *
	 * @return the launched configuration or <code>null</code>
	 * @since 2.0
	 */
	ILaunchConfiguration getLaunchConfiguration();

	/**
	 * Sets the value of a client defined attribute.
	 *
	 * @param key the attribute key
	 * @param value the attribute value
	 * @since 2.0
	 */
	void setAttribute(String key, String value);

	/**
	 * Returns the value of a client defined attribute.
	 *
	 * @param key the attribute key
	 * @return value the attribute value, or <code>null</code> if undefined
	 * @since 2.0
	 */
	String getAttribute(String key);

	/**
	 * Returns whether this launch contains at least one process
	 * or debug target.
	 *
	 * @return whether this launch contains at least one process
	 * or debug target
	 * @since 2.0
	 */
	boolean hasChildren();

}
