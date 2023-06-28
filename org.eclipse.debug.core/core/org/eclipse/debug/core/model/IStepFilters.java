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
package org.eclipse.debug.core.model;

/**
 * Support for step filters for a debug target. A debug target
 * that supports step filters should implement this interface.
 * Step filters can be toggled on/off for a debug target via
 * this interface. When a step method is called (see
 * <code>IStep</code>), the step implementation must respect
 * the state of the step filters as defined by this interface.
 * This allows step filters to be toggled on/off for
 * all stepping operations (in, over, return).
 * <p>
 * Step filter management is debug model specific - this interface
 * is used only to turn them on/off.
 * </p>
 * <p>
 * In 2.1, the <code>IFilteredStep</code> interface was used
 * to implement step filtering. The <code>IFilteredStep</code>
 * interface is now deprecated, and this interface should be used
 * in its place to allow filters to be applied to any step
 * function.
 * </p>
 * <p>
 * Clients may implement this interface. Debug targets that support
 * step filters should implement this interface.
 * </p>
 * @see org.eclipse.debug.core.model.IStep
 * @since 3.0
 */
public interface IStepFilters {

	/**
	 * Returns whether this debug target supports step filters.
	 *
	 * @return whether this debug target supports step filters
	 */
	boolean supportsStepFilters();

	/**
	 * Returns whether step filters are currently enabled in this
	 * debug target.
	 *
	 * @return whether step filters are currently enabled in this
	 * debug target
	 */
	boolean isStepFiltersEnabled();

	/**
	 * Sets whether step filters are enabled in this debug target.
	 *
	 * @param enabled whether step filters are enabled in this debug target
	 */
	void setStepFiltersEnabled(boolean enabled);
}
