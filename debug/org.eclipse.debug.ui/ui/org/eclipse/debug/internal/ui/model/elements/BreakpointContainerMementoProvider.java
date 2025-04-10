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
package org.eclipse.debug.internal.ui.model.elements;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.internal.ui.breakpoints.provisional.IBreakpointContainer;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.ui.model.IWorkbenchAdapter;

/**
 * This class provides memento for the breakpoint container.
 *
 * @since 3.6
 */
public class BreakpointContainerMementoProvider extends DebugElementMementoProvider {

	@Override
	protected String getElementName(Object element, IPresentationContext context) throws CoreException {
		if (element instanceof IBreakpointContainer) {
			IBreakpointContainer container = (IBreakpointContainer) element;
			IAdaptable category = container.getCategory();
			if (category != null) {
				IWorkbenchAdapter adapter = category.getAdapter(IWorkbenchAdapter.class);
				if (adapter != null) {
					return adapter.getLabel(category);
				}
				return container.getOrganizer().getLabel();
			}
		}
		return null;
	}

	@Override
	protected boolean supportsContextId(String id) {
		return IDebugUIConstants.ID_BREAKPOINT_VIEW.equals(id);
	}

}
