/*******************************************************************************
 * Copyright (c) 2004, 2013 IBM Corporation and others.
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
package org.eclipse.ant.internal.ui.model;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.preference.JFacePreferences;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceColors;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;

public class AntModelLabelProvider extends StyledCellLabelProvider implements IColorProvider, IStyledLabelProvider {

	private Color normalForeground;
	private Color defaultForeground;

	public AntModelLabelProvider() {
		ColorRegistry colorRegistry = JFaceResources.getColorRegistry();

		normalForeground = colorRegistry.get(JFacePreferences.INFORMATION_FOREGROUND_COLOR);
		if (normalForeground == null) {
			normalForeground = JFaceColors.getInformationViewerForegroundColor(Display.getDefault());
		}
		defaultForeground = colorRegistry.get(JFacePreferences.ACTIVE_HYPERLINK_COLOR);
		if (defaultForeground == null) {
			defaultForeground = JFaceColors.getInformationViewerForegroundColor(Display.getDefault());
		}
	}

	@Override
	public Image getImage(Object anElement) {
		AntElementNode node = (AntElementNode) anElement;
		return node.getImage();
	}

	@Override
	public void update(ViewerCell cell) {
		Object obj = cell.getElement();
		StyledString str = getStyledText(obj);
		cell.setText(str.toString());
		cell.setStyleRanges(str.getStyleRanges());
		cell.setImage(getImage(obj));
	}

	@Override
	public Color getForeground(Object node) {
		if (node instanceof AntTargetNode && ((AntTargetNode) node).isDefaultTarget()) {
			return defaultForeground;
		}
		return normalForeground;
	}

	@Override
	public StyledString getStyledText(Object element) {
		if (element instanceof AntTargetNode node) {
			return new StyledString(node.getLabel());
		} else if (element instanceof AntTaskNode node) {
			return new StyledString(node.getLabel());
		} else if (element instanceof AntProjectNodeProxy node) {
			StyledString buff = new StyledString(node.getLabel());
			IFile buildfile = node.getBuildFileResource();
			if (buildfile != null) {
				buff.append("  "); //$NON-NLS-1$
				buff.append('[', StyledString.DECORATIONS_STYLER);
				buff.append(buildfile.getFullPath().makeRelative().toString(), StyledString.DECORATIONS_STYLER);
				buff.append(']', StyledString.DECORATIONS_STYLER);
			}
			return buff;
		} else if (element instanceof IAntElement) {
			return new StyledString(((IAntElement) element).getLabel());
		}
		return null;
	}

	@Override
	public Color getBackground(Object element) {
		return null;
	}
}
