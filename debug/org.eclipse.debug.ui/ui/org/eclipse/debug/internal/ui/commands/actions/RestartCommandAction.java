/*******************************************************************************
 * Copyright (c) 2009, 2013 Wind River Systems and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *     IBM Corporation - bug fixing
 *******************************************************************************/
package org.eclipse.debug.internal.ui.commands.actions;

import org.eclipse.debug.core.commands.IRestartHandler;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.debug.ui.actions.DebugCommandAction;
import org.eclipse.jface.resource.ImageDescriptor;

/**
 * Handler for the Restart action
 *
 * @since 3.6
 */
public class RestartCommandAction extends DebugCommandAction {

	public RestartCommandAction() {
		setActionDefinitionId("org.eclipse.debug.ui.commands.Restart"); //$NON-NLS-1$
	}

	@Override
	protected Class<IRestartHandler> getCommandType() {
		return IRestartHandler.class;
	}

	@Override
	public ImageDescriptor getDisabledImageDescriptor() {
		return DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_DLCL_RESTART);
	}

	@Override
	public String getHelpContextId() {
		return "org.eclipse.debug.ui.restart_action_context"; //$NON-NLS-1$
	}

	@Override
	public ImageDescriptor getHoverImageDescriptor() {
		return DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_RESTART);
	}

	@Override
	public String getId() {
		return "org.eclipse.debug.ui.actions.Restart"; //$NON-NLS-1$
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_RESTART);
	}

	@Override
	public String getText() {
		return ActionMessages.RestartCommandAction__text;
	}

	@Override
	public String getToolTipText() {
		return ActionMessages.RestartCommandAction_tooltip;
	}


}
