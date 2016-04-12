/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.*;

class MultiLabelDecorator extends LabelProvider implements ILabelDecorator, IFontDecorator, IColorDecorator {
	private ILabelDecorator[] decorators;

	public MultiLabelDecorator(ILabelDecorator[] decorators) {
		this.decorators = decorators;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ILabelDecorator#decorateImage(org.eclipse.swt.graphics.Image, java.lang.Object)
	 */
	@Override
	public Image decorateImage(Image image, Object element) {
		for (int i = 0; i < decorators.length; i++) {
			ILabelDecorator decorator = decorators[i];
			Image newImage = decorator.decorateImage(image, element);
			if (newImage != null) {
				image = newImage;
			}
		}
		return image;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ILabelDecorator#decorateText(java.lang.String, java.lang.Object)
	 */
	@Override
	public String decorateText(String text, Object element) {
		for (int i = 0; i < decorators.length; i++) {
			ILabelDecorator decorator = decorators[i];
			String newText = decorator.decorateText(text, element);
			if (newText != null) {
				text = newText;
			}
		}
		return text;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
	 */
	@Override
	public void dispose() {
		for (int i = 0; i < decorators.length; i++) {
			ILabelDecorator d = decorators[i];
			d.dispose();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IFontDecorator#decorateFont(java.lang.Object)
	 */
	@Override
	public Font decorateFont(Object element) {
		for (int i = 0; i < decorators.length; i++) {
			ILabelDecorator decorator = decorators[i];
			if(decorator instanceof IFontDecorator) {
				return ((IFontDecorator)decorator).decorateFont(element);
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IColorDecorator#decorateForeground(java.lang.Object)
	 */
	@Override
	public Color decorateForeground(Object element) {
		for (int i = 0; i < decorators.length; i++) {
			ILabelDecorator decorator = decorators[i];
			if(decorator instanceof IColorDecorator) {
				return ((IColorDecorator)decorator).decorateForeground(element);
			}
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IColorDecorator#decorateBackground(java.lang.Object)
	 */
	@Override
	public Color decorateBackground(Object element) {
		for (int i = 0; i < decorators.length; i++) {
			ILabelDecorator decorator = decorators[i];
			if(decorator instanceof IColorDecorator) {
				return ((IColorDecorator)decorator).decorateBackground(element);
			}
		}
		return null;
	}
}