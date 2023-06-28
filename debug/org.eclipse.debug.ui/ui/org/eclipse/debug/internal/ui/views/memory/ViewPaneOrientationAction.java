/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.views.memory;

import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.DebugUIMessages;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.action.Action;
import org.eclipse.ui.PlatformUI;

class ViewPaneOrientationAction extends Action {
	private MemoryView fView;
	private int fOrientation;

	ViewPaneOrientationAction(MemoryView view, int orientation) {
		super(IInternalDebugCoreConstants.EMPTY_STRING, AS_RADIO_BUTTON);
		fView = view;
		fOrientation = orientation;

		if (orientation == MemoryView.HORIZONTAL_VIEW_ORIENTATION) {
			setText(DebugUIMessages.ViewPaneOrientationAction_0);
			setToolTipText(DebugUIMessages.ViewPaneOrientationAction_1);
			setImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_DETAIL_PANE_RIGHT));
			setDisabledImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_DLCL_DETAIL_PANE_RIGHT));
			setHoverImageDescriptor(DebugPluginImages.getImageDescriptor(IDebugUIConstants.IMG_LCL_DETAIL_PANE_RIGHT));
		} else if (orientation == MemoryView.VERTICAL_VIEW_ORIENTATION) {
			setText(DebugUIMessages.ViewPaneOrientationAction_2);
			setToolTipText(DebugUIMessages.ViewPaneOrientationAction_3);
			setImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_DETAIL_PANE_UNDER));
			setDisabledImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_DLCL_DETAIL_PANE_UNDER));
			setHoverImageDescriptor(DebugPluginImages.getImageDescriptor(IDebugUIConstants.IMG_LCL_DETAIL_PANE_UNDER));
		}
		PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IDebugHelpContextIds.MEMORY_VIEW_PANE_ORIENTATION_ACTION);
	}

	@Override
	public void run() {
		fView.setViewPanesOrientation(fOrientation);
	}

	public int getOrientation() {
		return fOrientation;
	}
}
