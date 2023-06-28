/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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
 *      Richard Birenheide - Bug 459664
 *******************************************************************************/

package org.eclipse.debug.internal.ui;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.texteditor.IAnnotationImageProvider;

public class InstructionPointerImageProvider implements IAnnotationImageProvider {

	@Override
	public Image getManagedImage(Annotation annotation) {
		if (annotation instanceof InstructionPointerAnnotation) {
			return ((InstructionPointerAnnotation) annotation).getImage();
		} else {
			return null;
		}

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
