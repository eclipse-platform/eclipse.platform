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

package org.eclipse.debug.internal.ui.views.memory;

import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.DebugUIMessages;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.ui.memory.IMemoryRendering;
import org.eclipse.debug.ui.memory.IMemoryRenderingContainer;
import org.eclipse.jface.action.Action;

/**
 * Remove Memory Rendering action This action serves two purposes: - remove
 * memory rendering from Memory Rendering Pane - quck way to remove a memory
 * block from Memory Rendering Pane
 *
 * When user clicks on the this tool bar action, it simply removes the top view
 * tab from Memory Rendering Pane.
 *
 * @since 3.0
 */
public class RemoveMemoryRenderingAction extends Action {
	private IMemoryRenderingContainer fViewPane;

	public RemoveMemoryRenderingAction(IMemoryRenderingContainer viewPane) {
		// create action as drop down
		super(DebugUIMessages.RemoveMemoryRenderingAction_Remove_rendering, AS_PUSH_BUTTON);
		setText(DebugUIMessages.RemoveMemoryRenderingAction_Remove_rendering);

		setToolTipText(DebugUIMessages.RemoveMemoryRenderingAction_Remove_rendering);
		setImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_REMOVE_MEMORY));
		setHoverImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_LCL_REMOVE_MEMORY));
		setDisabledImageDescriptor(DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_DLCL_REMOVE_MEMORY));
		fViewPane = viewPane;
	}

	@Override
	public void run() {

		// user has click on the RemoveMemoryRendering button
		IMemoryViewTab topTab = getViewTab();

		if (topTab != null) {
			IMemoryRendering rendering = topTab.getRendering();

			if (rendering != null) {
				fViewPane.removeMemoryRendering(rendering);
			}
		}
	}

	IMemoryViewTab getViewTab() {
		if (fViewPane instanceof IMemoryView) {
			return ((IMemoryView) fViewPane).getTopMemoryTab();
		}
		return null;
	}
}
