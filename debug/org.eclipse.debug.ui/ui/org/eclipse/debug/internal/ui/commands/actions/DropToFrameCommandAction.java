/*******************************************************************************
 * Copyright (c) 2005, 2013 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.commands.actions;

import org.eclipse.debug.core.commands.IDropToFrameHandler;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.debug.ui.actions.DebugCommandAction;
import org.eclipse.jface.resource.ImageDescriptor;

/**
 * Drop to frame action.
 *
 * @since 3.3
 */
public class DropToFrameCommandAction extends DebugCommandAction {

	public DropToFrameCommandAction() {
		setActionDefinitionId("org.eclipse.debug.ui.commands.DropToFrame"); //$NON-NLS-1$
	}

	@Override
	public String getText() {
		return ActionMessages.DropToFrameAction_0;
	}

	@Override
	public String getHelpContextId() {
		return "org.eclipse.debug.ui.drop_to_frame_action_context"; //$NON-NLS-1$
	}

	@Override
	public String getId() {
		return "org.eclipse.debug.ui.debugview.toolbar.dropToFrame"; //$NON-NLS-1$
	}

	@Override
	public String getToolTipText() {
		return ActionMessages.DropToFrameAction_3;
	}

	@Override
	public ImageDescriptor getHoverImageDescriptor() {
		return DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_DROP_TO_FRAME);
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_DROP_TO_FRAME);
	}

	@Override
	public ImageDescriptor getDisabledImageDescriptor() {
		return null;
	}

	@Override
	protected Class<IDropToFrameHandler> getCommandType() {
		return IDropToFrameHandler.class;
	}
}
