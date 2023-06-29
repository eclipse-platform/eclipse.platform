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
 *     Patrick Chuong (Texas Instruments) - bug fix 306768
 *****************************************************************/
package org.eclipse.debug.internal.ui.model.elements;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.TreePath;

/**
 * Breakpoint label provider.
 *
 * @since 3.6
 */
public class BreakpointLabelProvider extends DebugElementLabelProvider {

	@Override
	protected String getLabel(TreePath elementPath, IPresentationContext presentationContext, String columnId, int columnIndex) throws CoreException {
		if (columnIndex == 0) {
			return super.getLabel(elementPath, presentationContext, columnId, columnIndex);
		} else {
			return IInternalDebugCoreConstants.EMPTY_STRING;
		}
	}

	@Override
	protected ImageDescriptor getImageDescriptor(TreePath elementPath, IPresentationContext presentationContext, String columnId, int columnIndex) throws CoreException {
		if (columnIndex == 0) {
			return super.getImageDescriptor(elementPath, presentationContext, columnId, columnIndex);
		} else {
			return null;
		}
	}

	@Override
	public boolean getChecked(TreePath path, IPresentationContext presentationContext) throws CoreException {
		Object lastSegment = path.getLastSegment();
		if (lastSegment instanceof IBreakpoint) {
			return ((IBreakpoint) lastSegment).isEnabled();
		}

		return false;
	}
}
