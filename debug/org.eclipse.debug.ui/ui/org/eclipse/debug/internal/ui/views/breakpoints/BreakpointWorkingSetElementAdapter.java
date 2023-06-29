/*******************************************************************************
 * Copyright (c) 2009, 2013 IBM Corporation and others.
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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetElementAdapter;

/**
 * Consulted by workbench pull down actions that add/remove selected elements to/from
 * working sets. Allows breakpoint working sets to select which elements are applicable
 * for adding/removing.
 *
 * @since 3.3
 */
public class BreakpointWorkingSetElementAdapter implements IWorkingSetElementAdapter {

	@Override
	public IAdaptable[] adaptElements(IWorkingSet ws, IAdaptable[] elements) {
		for (IAdaptable element : elements) {
			IBreakpoint breakpoint = (IBreakpoint)DebugPlugin.getAdapter(element, IBreakpoint.class);
			if (breakpoint != null) {
				return selectBreakpoints(elements);
			}
		}
		return elements;
	}

	private IAdaptable[] selectBreakpoints(IAdaptable[] elements) {
		List<IBreakpoint> breakpoints = new ArrayList<>(elements.length);
		for (IAdaptable element : elements) {
			IBreakpoint breakpoint = (IBreakpoint)DebugPlugin.getAdapter(element, IBreakpoint.class);
			if (breakpoint != null) {
				breakpoints.add(breakpoint);
			}
		}
		return breakpoints.toArray(new IAdaptable[breakpoints.size()]);
	}

	@Override
	public void dispose() {
	}

}
