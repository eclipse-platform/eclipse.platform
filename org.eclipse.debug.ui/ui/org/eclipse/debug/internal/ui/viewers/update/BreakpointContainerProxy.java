/*****************************************************************
 * Copyright (c) 2009 Texas Instruments and others
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
 *****************************************************************/
package org.eclipse.debug.internal.ui.viewers.update;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.internal.ui.breakpoints.provisional.IBreakpointContainer;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ICheckboxModelProxy;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.provisional.AbstractModelProxy;
import org.eclipse.jface.viewers.TreePath;

/**
 * Breakpoint container model proxy.
 *
 * @since 3.6
 */
public class BreakpointContainerProxy extends AbstractModelProxy implements	ICheckboxModelProxy {

	/**
	 * The breakpoint container
	 */
	private IBreakpointContainer fContainer;

	/**
	 * Constructor.
	 *
	 * @param container the breakpoint container.
	 */
	public BreakpointContainerProxy(IBreakpointContainer container) {
		fContainer = container;
	}

	@Override
	public boolean setChecked(IPresentationContext context, Object viewerInput, TreePath path, boolean checked) {
		boolean atLeastOne = false;
		for (IBreakpoint breakpoint : fContainer.getBreakpoints()) {
			try {
				breakpoint.setEnabled(checked);
				atLeastOne = true;
			} catch (CoreException e) {}
		}
		return atLeastOne;
	}

}
