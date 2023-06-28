/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui;

import org.eclipse.core.resources.IMarker;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.texteditor.IAnnotationImageProvider;
import org.eclipse.ui.texteditor.MarkerAnnotation;

/**
 * Provides managed images for breakpoint annotations.
 * @since 3.0
 */
public class BreakpointImageProvider implements IAnnotationImageProvider {
	@Override
	public Image getManagedImage(Annotation annotation) {
		if (annotation instanceof MarkerAnnotation) {
			IMarker marker = ((MarkerAnnotation)annotation).getMarker();
			if (marker != null) {
				IBreakpoint breakpoint = DebugPlugin.getDefault().getBreakpointManager().getBreakpoint(marker);
				if (breakpoint != null) {
					return DebugUIPlugin.getModelPresentation().getImage(breakpoint);
				}
			}
		}
		return null;
	}
	@Override
	public String getImageDescriptorId(Annotation annotation) {
		return null;
	}
	@Override
	public ImageDescriptor getImageDescriptor(String imageDescritporId) {
		return null;
	}
}
