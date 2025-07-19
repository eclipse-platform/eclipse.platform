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

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointsListener;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.ui.AbstractBreakpointOrganizerDelegate;
import org.eclipse.debug.ui.BreakpointTypeCategory;

/**
 * Breakpoint organizers for breakpoint types based on breakpoint enablement
 * state.
 */
public class BreakpointEnablement extends AbstractBreakpointOrganizerDelegate implements IBreakpointsListener {

	private final BreakpointTypeCategory ENABLED = new BreakpointTypeCategory("Enabled", 0); //$NON-NLS-1$
	private final BreakpointTypeCategory DISABLED = new BreakpointTypeCategory("Disabled", 1); //$NON-NLS-1$

	private final Map<IBreakpoint, Boolean> breakpointCache;

	public BreakpointEnablement() {
		DebugPlugin.getDefault().getBreakpointManager().addBreakpointListener(this);
		breakpointCache = Collections.synchronizedMap(new WeakHashMap<>());
	}

	@Override
	public void dispose() {
		DebugPlugin.getDefault().getBreakpointManager().removeBreakpointListener(this);
		breakpointCache.clear();
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
			breakpointCache.put(breakpoint, breakpoint.isEnabled());
			return categories;
		} catch (CoreException e) {
			DebugPlugin.log(e);
		}
		return null;
	}

	@Override
	public void breakpointsAdded(IBreakpoint[] breakpoints) {
		for (IBreakpoint breakpoint : breakpoints) {
			try {
				breakpointCache.put(breakpoint, breakpoint.isEnabled());
			} catch (CoreException e) {
				DebugPlugin.log(e);
			}
		}
	}

	@Override
	public void breakpointsRemoved(IBreakpoint[] breakpoints, IMarkerDelta[] deltas) {
		for (IBreakpoint breakpoint : breakpoints) {
			breakpointCache.remove(breakpoint);
		}
	}

	@Override
	public void breakpointsChanged(IBreakpoint[] breakpoints, IMarkerDelta[] deltas) {
		boolean updateCategories = false;
		for (IBreakpoint breakpoint : breakpoints) {
			try {
				boolean isEnabled = breakpoint.isEnabled();
				Boolean wasEnabled = breakpointCache.get(breakpoint);
				if (wasEnabled == null || wasEnabled != isEnabled) {
					breakpointCache.put(breakpoint, isEnabled);
					updateCategories = true;
				}
			} catch (CoreException e) {
				DebugPlugin.log(e);
			}
		}
		if (updateCategories) {
			fireCategoryChanged(ENABLED);
		}
	}
}
