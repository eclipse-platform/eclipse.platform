/*******************************************************************************
 * Copyright (c) 2020 Paul Pazderski and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Paul Pazderski - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.views.breakpoints;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.ui.ide.undo.DeleteMarkersOperation;

/**
 * Extension of the default undoable delete marker operation to additional
 * update the 'breakpointIsDeleted' attribute.
 */
public class DeleteBreakpointMarkersOperation extends DeleteMarkersOperation {

	/**
	 * Create an undoable operation that can delete the specified markers.
	 *
	 * @param markers the markers to be deleted
	 * @param name    the name used to describe the operation that deletes the
	 *                markers
	 */
	public DeleteBreakpointMarkersOperation(IMarker[] markers, String name) {
		super(markers, name);
	}

	@Override
	protected void doExecute(IProgressMonitor monitor, org.eclipse.core.runtime.IAdaptable info) throws CoreException {
		IMarker[] markers = getMarkers();
		if (markers != null) {
			for (IMarker marker : markers) {
				marker.setAttribute(DebugPlugin.ATTR_BREAKPOINT_IS_DELETED, true);
			}
		}

		super.doExecute(monitor, info);
	}

	// Note: do not update 'breakpointIsDeleted' on doUndo (i.e. don't set
	// breakpointIsDeleted=false on undo). It is important that the recreated
	// breakpoint has breakpointIsDeleted=true to recognize the recreation in later
	// code.
}
