/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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


import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.swt.graphics.Image;

/**
 * Default instruction pointer annotation.
 */
public class InstructionPointerAnnotation extends DynamicInstructionPointerAnnotation {

	/**
	 * The image for this annotation.
	 */
	private Image fImage;

	/**
	 * Constructs an instruction pointer image.
	 *
	 * @param frame stack frame the instruction pointer is associated with
	 * @param annotationType the type of annotation to display (annotation identifier)
	 * @param text the message to display with the annotation as hover help
	 * @param image the image used to display the annotation
	 */
	public InstructionPointerAnnotation(IStackFrame frame, String annotationType, String text, Image image) {
		super(frame, annotationType, text);
		fImage = image;
	}

	/**
	 * Returns this annotation's image.
	 *
	 * @return image
	 */
	protected Image getImage() {
		return fImage;
	}

}
