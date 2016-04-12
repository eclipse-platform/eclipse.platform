/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.jface.viewers.DecoratingLabelProvider;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;

/**
 * Decorating label provider that supports multiple decorators that are font and color decorators.
 * 
 * @since 3.0
 */
public class DecoratingColorLabelProvider extends DecoratingLabelProvider implements IColorProvider, IFontProvider {

	public DecoratingColorLabelProvider(ILabelProvider provider, ILabelDecorator[] decorators) {
		super(provider, new MultiLabelDecorator(decorators));
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IColorProvider#getForeground(java.lang.Object)
	 */
	@Override
	public Color getForeground(Object element) {
		ILabelProvider p = getLabelProvider();
		if (p instanceof IColorProvider) {
			return ((IColorProvider) p).getForeground(element);
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IColorProvider#getBackground(java.lang.Object)
	 */
	@Override
	public Color getBackground(Object element) {
		ILabelProvider p = getLabelProvider();
		if (p instanceof IColorProvider) {
			return ((IColorProvider) p).getBackground(element);
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IFontProvider#getFont(java.lang.Object)
	 */
	@Override
	public Font getFont(Object element) {
		ILabelProvider p = getLabelProvider();
		if (p instanceof IFontProvider) {
			return ((IFontProvider) p).getFont(element);
		}
		return null;
	}
}
