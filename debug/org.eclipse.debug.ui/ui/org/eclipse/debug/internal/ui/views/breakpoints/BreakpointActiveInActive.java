/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.views.breakpoints;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointListener;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.ui.AbstractBreakpointOrganizerDelegate;
import org.eclipse.debug.ui.BreakpointTypeCategory;

/**
 * Breakpoint organizers for breakpoint types.
 *
 * @since 3.1
 */
public class BreakpointActiveInActive extends AbstractBreakpointOrganizerDelegate implements IBreakpointListener {
	private final BreakpointTypeCategory ACTIVE = new BreakpointTypeCategory("Active"); //$NON-NLS-1$
	private final BreakpointTypeCategory INACTIVE = new BreakpointTypeCategory("In-Active"); //$NON-NLS-1$

	public BreakpointActiveInActive() {
		DebugPlugin.getDefault().getBreakpointManager().addBreakpointListener(this);
	}

	@Override
	public IAdaptable[] getCategories(IBreakpoint breakpoint) {
		try {
			IAdaptable[] categories;
			if (breakpoint.isEnabled()) {
				categories = new IAdaptable[] { ACTIVE };
			} else {
				categories = new IAdaptable[] { INACTIVE };
			}
			return categories;
		} catch (CoreException e) {
			DebugPlugin.log(e);
		}
		return null;
	}

	@Override
	public void dispose() {
		DebugPlugin.getDefault().getBreakpointManager().removeBreakpointListener(this);
	}


	@Override
	public void breakpointAdded(IBreakpoint breakpoint) {
	}

	@Override
	public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
	}

	@Override
	public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
		fireCategoryChanged(ACTIVE);
	}
}
