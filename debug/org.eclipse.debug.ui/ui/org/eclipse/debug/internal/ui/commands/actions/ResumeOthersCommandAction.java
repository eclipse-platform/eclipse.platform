/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation.
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

import org.eclipse.debug.core.commands.IResumeOthersHandler;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.debug.ui.actions.DebugCommandAction;
import org.eclipse.jface.resource.ImageDescriptor;

/**
 * Resume Others action.
 *
 * @since 3.24
 */
public class ResumeOthersCommandAction extends DebugCommandAction{

	public ResumeOthersCommandAction() {
		setActionDefinitionId("org.eclipse.debug.ui.commands.ResumeOthers"); //$NON-NLS-1$
	}

	@Override
	public String getText() {
		return ActionMessages.ResumeOtherThreadsAction;
	}

	@Override
	public String getHelpContextId() {
		return "org.eclipse.debug.ui.resume_others_action_context"; //$NON-NLS-1$
	}

	@Override
	public String getId() {
		return "org.eclipse.debug.ui.debugview.toolbar.resumeothers"; //$NON-NLS-1$
	}

	@Override
	public String getToolTipText() {
		return ActionMessages.ResumeOtherThreadsActionToolTip;
	}

	@Override
	protected Class<IResumeOthersHandler> getCommandType() {
		return IResumeOthersHandler.class;
	}

	@Override
	public ImageDescriptor getDisabledImageDescriptor() {
		return null;
	}

	@Override
	public ImageDescriptor getHoverImageDescriptor() {
		return null;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}
}
