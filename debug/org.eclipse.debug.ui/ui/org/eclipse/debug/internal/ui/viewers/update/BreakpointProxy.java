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
import org.eclipse.debug.internal.ui.viewers.model.provisional.ICheckboxModelProxy;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.internal.ui.viewers.provisional.AbstractModelProxy;
import org.eclipse.jface.viewers.TreePath;

/**
 * Breakpoint model proxy.
 *
 * @since 3.6
 */
public class BreakpointProxy extends AbstractModelProxy implements ICheckboxModelProxy {

	/**
	 * Breakpoint object
	 */
	protected IBreakpoint fBreakpoint;

	/**
	 * Constructor.
	 *
	 * @param breakpoint the breakpoint for this model proxy
	 */
	public BreakpointProxy(IBreakpoint breakpoint) {
		fBreakpoint = breakpoint;
	}

	@Override
	public boolean setChecked(IPresentationContext context, Object viewerInput, TreePath path, boolean checked) {
		try {
			fBreakpoint.setEnabled(checked);
			return fBreakpoint.isEnabled() == checked;
		} catch (CoreException e) {
			return false;
		}
	}

}
