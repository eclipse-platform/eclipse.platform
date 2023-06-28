/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
 *     Patrick Chuong (Texas Instruments) - Improve usability of the breakpoint view (Bug 238956)
 *******************************************************************************/
package org.eclipse.debug.internal.ui.views.breakpoints;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.ui.breakpoints.provisional.IBreakpointContainer;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.IWorkbenchAdapter2;

/**
 * BreakpointContainerWorkbenchAdapter
 */
public class BreakpointContainerWorkbenchAdapter implements IWorkbenchAdapter, IWorkbenchAdapter2{

	@Override
	public Object[] getChildren(Object o) {
		// not used
		return null;
	}

	@Override
	public ImageDescriptor getImageDescriptor(Object object) {
		if (object instanceof IBreakpointContainer) {
			IBreakpointContainer container = (IBreakpointContainer) object;
			IAdaptable category = container.getCategory();
			if (category != null) {
				IWorkbenchAdapter adapter = category.getAdapter(IWorkbenchAdapter.class);
				if (adapter != null) {
					return adapter.getImageDescriptor(category);
				}
				return container.getOrganizer().getImageDescriptor();
			}
		}
		return null;
	}

	@Override
	public String getLabel(Object object) {
		if (object instanceof IBreakpointContainer) {
			IBreakpointContainer container = (IBreakpointContainer) object;
			IAdaptable category = container.getCategory();
			if (category != null) {
				IWorkbenchAdapter adapter = category.getAdapter(IWorkbenchAdapter.class);
				if (adapter != null) {
					return adapter.getLabel(category);
				}
				return container.getOrganizer().getLabel();
			}
		}
		return IInternalDebugCoreConstants.EMPTY_STRING;
	}

	@Override
	public Object getParent(Object o) {
		return null;
	}

	@Override
	public RGB getForeground(Object object) {
		if (object instanceof IBreakpointContainer) {
			IBreakpointContainer container = (IBreakpointContainer) object;
			IAdaptable category = container.getCategory();
			IWorkbenchAdapter2 adapter = category.getAdapter(IWorkbenchAdapter2.class);
			if (adapter != null) {
				return adapter.getForeground(category);
			}
		}
		return null;
	}

	@Override
	public RGB getBackground(Object object) {
		if (object instanceof IBreakpointContainer) {
			IBreakpointContainer container = (IBreakpointContainer) object;
			IAdaptable category = container.getCategory();
			IWorkbenchAdapter2 adapter = category.getAdapter(IWorkbenchAdapter2.class);
			if (adapter != null) {
				return adapter.getBackground(category);
			}
		}
		return null;
	}

	@Override
	public FontData getFont(Object object) {
		if (object instanceof IBreakpointContainer) {
			IBreakpointContainer container = (IBreakpointContainer) object;
			IAdaptable category = container.getCategory();
			IWorkbenchAdapter2 adapter = category.getAdapter(IWorkbenchAdapter2.class);
			if (adapter != null) {
				return adapter.getFont(category);
			}
		}
		return null;
	}

}
