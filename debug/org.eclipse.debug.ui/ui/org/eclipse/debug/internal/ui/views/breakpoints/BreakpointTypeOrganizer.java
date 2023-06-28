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
package org.eclipse.debug.internal.ui.views.breakpoints;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.ui.AbstractBreakpointOrganizerDelegate;
import org.eclipse.debug.ui.BreakpointTypeCategory;
import org.eclipse.debug.ui.IBreakpointTypeCategory;

/**
 * Breakpoint organizers for breakpoint types.
 *
 * @since 3.1
 */
public class BreakpointTypeOrganizer extends AbstractBreakpointOrganizerDelegate {

	private Map<String, IAdaptable[]> fTypes = new HashMap<>();

	@Override
	public IAdaptable[] getCategories(IBreakpoint breakpoint) {
		IBreakpointTypeCategory category = breakpoint.getAdapter(IBreakpointTypeCategory.class);
		if (category != null) {
			return new IAdaptable[]{category};
		}
		String name = DebugPlugin.getDefault().getBreakpointManager().getTypeName(breakpoint);
		if (name != null) {
			IAdaptable[] categories = fTypes.get(name);
			if (categories == null) {
				categories = new IAdaptable[]{new BreakpointTypeCategory(name)};
				fTypes.put(name, categories);
			}
			return categories;
		}
		return null;
	}

	@Override
	public void dispose() {
		fTypes.clear();
	}
}
