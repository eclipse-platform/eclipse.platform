/*****************************************************************
 * Copyright (c) 2009, 2010 Texas Instruments and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Patrick Chuong (Texas Instruments) - Initial API and implementation (Bug 238956)
 *     Wind River Systems - ongoing enhancements and bug fixing
 *****************************************************************/
package org.eclipse.debug.internal.ui.elements.adapters;

import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;

/**
 * The default breakpoints view input populates the view with content
 * from the default breakpoint manager.
 *
 * @since 3.6
 */
public class DefaultBreakpointsViewInput {

	/**
	 * The presentation context of the breakpoints view.
	 */
	final private IPresentationContext fContext;

	/**
	 * Constructor.
	 *
	 * @param context the presentation context for this input
	 */
	public DefaultBreakpointsViewInput(IPresentationContext context) {
		fContext = context;
	}

	/**
	 * Returns the presentation context for this input.
	 *
	 * @return the presentation context
	 */
	public IPresentationContext getContext() {
		return fContext;
	}

	@Override
	public int hashCode() {
		if (getContext() != null) {
			return getContext().hashCode();
		} else {
			return 1;
		}
	}

	@Override
	public boolean equals(Object arg0) {
		if ( (arg0 != null) && arg0.getClass().equals(this.getClass()) ) {

			IPresentationContext context = ((DefaultBreakpointsViewInput) arg0).getContext();
			if (getContext() != null && context != null) {
				return getContext().equals(context);
			}
		}

		return super.equals(arg0);
	}

}
