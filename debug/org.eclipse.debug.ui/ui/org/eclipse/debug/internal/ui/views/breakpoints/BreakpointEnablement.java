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
 * Breakpoint organizers for breakpoint types based on breakpoint enablement
 * state.
 */
public class BreakpointEnablement extends AbstractBreakpointOrganizerDelegate implements IBreakpointListener {

	private final BreakpointTypeCategory ENABLED = new BreakpointTypeCategory("Enabled", 0); //$NON-NLS-1$
	private final BreakpointTypeCategory DISABLED = new BreakpointTypeCategory("Disabled", 1); //$NON-NLS-1$

	public BreakpointEnablement() {
		DebugPlugin.getDefault().getBreakpointManager().addBreakpointListener(this);
	}

	@Override
	public void dispose() {
		DebugPlugin.getDefault().getBreakpointManager().removeBreakpointListener(this);
	}

	@Override
	public IAdaptable[] getCategories(IBreakpoint breakpoint) {
		try {
			IAdaptable[] categories;
			if (breakpoint.isEnabled()) {
				categories = new IAdaptable[] { ENABLED };
			} else {
				categories = new IAdaptable[] { DISABLED };
			}
			return categories;
		} catch (CoreException e) {
			DebugPlugin.log(e);
		}
		return null;
	}

	@Override
	public void breakpointAdded(IBreakpoint breakpoint) {
	}

	@Override
	public void breakpointRemoved(IBreakpoint breakpoint, IMarkerDelta delta) {
	}

	@Override
	public void breakpointChanged(IBreakpoint breakpoint, IMarkerDelta delta) {
		fireCategoryChanged(ENABLED);
	}
}
