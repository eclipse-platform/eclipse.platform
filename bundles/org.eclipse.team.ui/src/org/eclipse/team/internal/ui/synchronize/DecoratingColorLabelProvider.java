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

	@Override
	public Color getForeground(Object element) {
		ILabelProvider p = getLabelProvider();
		if (p instanceof IColorProvider) {
			return ((IColorProvider) p).getForeground(element);
		}
		return null;
	}

	@Override
	public Color getBackground(Object element) {
		ILabelProvider p = getLabelProvider();
		if (p instanceof IColorProvider) {
			return ((IColorProvider) p).getBackground(element);
		}
		return null;
	}

	@Override
	public Font getFont(Object element) {
		ILabelProvider p = getLabelProvider();
		if (p instanceof IFontProvider) {
			return ((IFontProvider) p).getFont(element);
		}
		return null;
	}
}
