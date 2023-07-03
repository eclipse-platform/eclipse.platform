/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
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

import org.eclipse.debug.core.commands.IDisconnectHandler;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.actions.DebugCommandAction;
import org.eclipse.jface.resource.ImageDescriptor;
/**
 * Disconnect action.
 *
 * @since 3.3
 */
public class DisconnectCommandAction extends DebugCommandAction{

	public DisconnectCommandAction() {
		setActionDefinitionId("org.eclipse.debug.ui.commands.Disconnect"); //$NON-NLS-1$
	}

	@Override
	public String getText() {
		return ActionMessages.DisconnectAction_0;
	}

	@Override
	public String getHelpContextId() {
		return "org.eclipse.debug.ui.disconnect_action_context"; //$NON-NLS-1$
	}

	@Override
	public String getId() {
		return "org.eclipse.debug.ui.debugview.toolbar.disconnect"; //$NON-NLS-1$
	}

	@Override
	public String getToolTipText() {
		return ActionMessages.DisconnectAction_3;
	}

	@Override
	public ImageDescriptor getDisabledImageDescriptor() {
		return DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_DLCL_DISCONNECT);
	}

	@Override
	public ImageDescriptor getHoverImageDescriptor() {
		return DebugPluginImages.getImageDescriptor(IDebugUIConstants.IMG_LCL_DISCONNECT);
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return DebugPluginImages.getImageDescriptor(IDebugUIConstants.IMG_LCL_DISCONNECT);
	}

	@Override
	protected Class<IDisconnectHandler> getCommandType() {
		return IDisconnectHandler.class;
	}
}
