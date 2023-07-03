/*******************************************************************************
 * Copyright (c) 2012 Sebastian Schmidt and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Sebastian Schmidt - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.importexport.breakpoints;

import org.eclipse.core.resources.IMarker;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jface.viewers.BaseLabelProvider;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.swt.graphics.Image;

/**
 * A decorator which attaches the node path of breakpoints.
 */
public class BreakpointsPathDecorator extends BaseLabelProvider implements ILabelDecorator {

	@Override
	public Image decorateImage(Image image, Object element) {
		return null;
	}

	@Override
	public String decorateText(String text, Object element) {
		if (element instanceof IBreakpoint) {
			IBreakpoint breakpoint = (IBreakpoint) element;
			IMarker marker = breakpoint.getMarker();
			if (marker == null) {
				return null;
			}
			String path = marker.getAttribute(IImportExportConstants.IE_NODE_PATH, null);
			if (path != null && path.length() > 0) {
				return text + " [" + path + "]"; //$NON-NLS-1$ //$NON-NLS-2$
			}
		}

		return null;
	}
}