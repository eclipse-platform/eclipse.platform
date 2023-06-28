/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.ui.AbstractBreakpointOrganizerDelegate;

/**
 * Breakpoint organizers for files.
 *
 * @since 3.1
 */
public class FileBreakpointOrganizer extends AbstractBreakpointOrganizerDelegate {

	@Override
	public IAdaptable[] getCategories(IBreakpoint breakpoint) {
		IMarker marker = breakpoint.getMarker();
		if (marker != null) {
			IResource resource = marker.getResource();
			if (resource.getType() == IResource.FILE) {
				return new IAdaptable[]{(IFile)resource};
			}
		}
		return null;
	}

}
