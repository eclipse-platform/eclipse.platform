/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
 * Provides the ability to perform a filtered step. Implementations must be non-
 * blocking. Filter implementation is debug model specific and may not be
 * supported by all debug models.
 * <p>
 * Clients may implement this interface.
 * </p>
 * @since 2.1
 * @deprecated clients should implement <code>IStepFilters</code> instead
 */
@Deprecated
public interface IFilteredStep extends IStep {
	/**
	 * Returns whether this element can currently perform a filtered step into.
	 *
	 * @return whether this element can currently perform a filtered step into
	 */
	boolean canStepWithFilters();
	/**
	 * Steps into the current statement, generating <code>RESUME</code>
	 * and <code>SUSPEND</code> events for the associated thread, applying step
	 * filters, as applicable for the associated thread. Can only be called when
	 * the associated thread is suspended. Implementations must implement
	 * stepping as non- blocking.
	 *
	 * @exception DebugException on failure. Reasons include:<ul>
	 * <li>TARGET_REQUEST_FAILED - The request failed in the target</li>
	 * <li>NOT_SUPPORTED - The capability is not supported by the target</li>
	 * </ul>
	 */
	void stepWithFilters() throws DebugException;
}
