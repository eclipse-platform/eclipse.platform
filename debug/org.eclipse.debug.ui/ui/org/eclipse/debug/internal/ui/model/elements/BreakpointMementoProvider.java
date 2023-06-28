/*******************************************************************************
 * Copyright (c) 2009 Freescale Semiconductor and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     2009 Freescale - initial API and implementation (Bug 238956)
 *******************************************************************************/
package org.eclipse.debug.internal.ui.model.elements;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.ui.IMemento;

/**
 * Memento provider for breakpoints
 *
 * @since 3.6
 */
public class BreakpointMementoProvider extends ElementMementoProvider {

	/**
	 * Marker ID. Stored as string in order to support the full range of the long type.
	 */
	private static final String MARKER_ID = "MARKER_ID"; 	//$NON-NLS-1$

	/**
	 * Full path from the workspace to the resource referred to by the breakpoint marker.
	 *
	 * Stored as String.
	 */
	private static final String RESOURCE_PATH = "RESOURCE_PATH"; 	//$NON-NLS-1$

	protected boolean supportsContextId(String id) {
		return IDebugUIConstants.ID_BREAKPOINT_VIEW.equals(id);
	}

	@Override
	protected boolean encodeElement(Object element, IMemento memento, IPresentationContext context) throws CoreException {
		if (element instanceof IBreakpoint) {

			IMarker marker = ((IBreakpoint)element).getMarker();
			if (marker != null) {

				long markerId = marker.getId();
				memento.putString(MARKER_ID, Long.toString(markerId));

				IPath fullPath = marker.getResource().getFullPath();
				String path = fullPath.toString();
				memento.putString(RESOURCE_PATH, path);
				return true;
			}
		}

		return false;
	}

	@Override
	protected boolean isEqual(Object element, IMemento memento, IPresentationContext context) throws CoreException {
		if (element instanceof IBreakpoint) {
			IBreakpoint breakpoint =(IBreakpoint)element;
			IMarker marker = breakpoint.getMarker();

			long markerId = marker.getId();
			String mementoMarkerId = memento.getString(MARKER_ID);
			if (!Long.toString(markerId).equals(mementoMarkerId)) {
				return false;
			}

			IPath fullPath = marker.getResource().getFullPath();
			String path = fullPath.toString();
			String mementoPath = memento.getString(RESOURCE_PATH);
			if (!path.equals(mementoPath)) {
				return false;
			}
			return true;
		}
		return false;
	}

}
