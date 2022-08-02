/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.jface.viewers.IColorDecorator;
import org.eclipse.jface.viewers.IFontDecorator;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;

class MultiLabelDecorator extends LabelProvider implements ILabelDecorator, IFontDecorator, IColorDecorator {
	private ILabelDecorator[] decorators;

	public MultiLabelDecorator(ILabelDecorator[] decorators) {
		this.decorators = decorators;
	}

	@Override
	public Image decorateImage(Image image, Object element) {
		for (ILabelDecorator decorator : decorators) {
			Image newImage = decorator.decorateImage(image, element);
			if (newImage != null) {
				image = newImage;
			}
		}
		return image;
	}

	@Override
	public String decorateText(String text, Object element) {
		for (ILabelDecorator decorator : decorators) {
			String newText = decorator.decorateText(text, element);
			if (newText != null) {
				text = newText;
			}
		}
		return text;
	}

	@Override
	public void dispose() {
		for (ILabelDecorator d : decorators) {
			d.dispose();
		}
	}

	@Override
	public Font decorateFont(Object element) {
		for (ILabelDecorator decorator : decorators) {
			if(decorator instanceof IFontDecorator) {
				return ((IFontDecorator)decorator).decorateFont(element);
			}
		}
		return null;
	}

	@Override
	public Color decorateForeground(Object element) {
		for (ILabelDecorator decorator : decorators) {
			if(decorator instanceof IColorDecorator) {
				return ((IColorDecorator)decorator).decorateForeground(element);
			}
		}
		return null;
	}

	@Override
	public Color decorateBackground(Object element) {
		for (ILabelDecorator decorator : decorators) {
			if(decorator instanceof IColorDecorator) {
				return ((IColorDecorator)decorator).decorateBackground(element);
			}
		}
		return null;
	}
}