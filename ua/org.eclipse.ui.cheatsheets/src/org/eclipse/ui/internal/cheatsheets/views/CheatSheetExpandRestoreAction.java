/*******************************************************************************
 * Copyright (c) 2002, 2019 IBM Corporation and others.
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
package org.eclipse.ui.internal.cheatsheets.views;


import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.internal.cheatsheets.CheatSheetPlugin;
import org.eclipse.ui.internal.cheatsheets.Messages;

/**
 * Action used to enable / disable method filter properties
 */
public class CheatSheetExpandRestoreAction extends Action {
	private CheatSheetViewer viewer;
	private boolean collapsed = false;

	private ImageDescriptor collapseImage;
	private ImageDescriptor disabledImage;

	public CheatSheetExpandRestoreAction(String title, boolean initValue, CheatSheetViewer viewer) {
		super(title);
		this.viewer = viewer;

		IPath path = CheatSheetPlugin.ICONS_PATH.append(CheatSheetPlugin.T_ELCL).append("collapse_expand_all.png");//$NON-NLS-1$
		collapseImage = CheatSheetPlugin.createImageDescriptor(CheatSheetPlugin.getPlugin().getBundle(), path);
		path = CheatSheetPlugin.ICONS_PATH.append(CheatSheetPlugin.T_DLCL).append("collapse_expand_all.png");//$NON-NLS-1$
		disabledImage = CheatSheetPlugin.createImageDescriptor(CheatSheetPlugin.getPlugin().getBundle(), path);
		setDisabledImageDescriptor(disabledImage);
		setImageDescriptor(collapseImage);
		setCollapsed(initValue);
	}

	@Override
	public void run() {
		viewer.toggleExpandRestore();
	}

	public boolean isCollapsed() {
		return collapsed;
	}

	public void setCollapsed(boolean value) {
		super.setChecked(value);
		collapsed = value;
		if(value) {
			setToolTipText(Messages.RESTORE_ALL_TOOLTIP);
		} else {
			setToolTipText(Messages.COLLAPSE_ALL_BUT_CURRENT_TOOLTIP);
		}
	}
}
