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

import org.eclipse.debug.core.commands.IResumeHandler;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.debug.ui.actions.DebugCommandAction;
import org.eclipse.jface.resource.ImageDescriptor;

/**
 * Resume action.
 *
 * @since 3.3
 */
public class ResumeCommandAction extends DebugCommandAction{

	public ResumeCommandAction() {
		setActionDefinitionId("org.eclipse.debug.ui.commands.Resume"); //$NON-NLS-1$
	}

	@Override
	public String getText() {
		return ActionMessages.ResumeAction_0;
	}

	@Override
	public String getHelpContextId() {
		return "org.eclipse.debug.ui.resume_action_context"; //$NON-NLS-1$
	}

	@Override
	public String getId() {
		return "org.eclipse.debug.ui.debugview.toolbar.resume"; //$NON-NLS-1$
	}

	@Override
	public String getToolTipText() {
		return ActionMessages.ResumeAction_3;
	}

	@Override
	public ImageDescriptor getDisabledImageDescriptor() {
		return DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_DLCL_RESUME);
	}

	@Override
	public ImageDescriptor getHoverImageDescriptor() {
		return DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_RESUME);
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return DebugPluginImages.getImageDescriptor(IInternalDebugUIConstants.IMG_ELCL_RESUME);
	}

	@Override
	protected Class<IResumeHandler> getCommandType() {
		return IResumeHandler.class;
	}
}
