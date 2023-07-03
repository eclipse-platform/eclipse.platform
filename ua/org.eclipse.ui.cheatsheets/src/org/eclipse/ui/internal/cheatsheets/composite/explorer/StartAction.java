/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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

package org.eclipse.ui.internal.cheatsheets.composite.explorer;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.internal.cheatsheets.CheatSheetPlugin;
import org.eclipse.ui.internal.cheatsheets.Messages;
import org.eclipse.ui.internal.cheatsheets.composite.model.AbstractTask;
import org.eclipse.ui.internal.provisional.cheatsheets.ICompositeCheatSheetTask;

public class StartAction extends Action {
	private AbstractTask task;
	private static final String START_CCS_TASK_GIF = "start_ccs_task.png"; //$NON-NLS-1$

	public StartAction(ICompositeCheatSheetTask task) {
		this.task = (AbstractTask) task;
		this.setText(Messages.COMPOSITE_MENU_START);
		IPath path = CheatSheetPlugin.ICONS_PATH.append(CheatSheetPlugin.T_ELCL).append(START_CCS_TASK_GIF);
		ImageDescriptor startImage = CheatSheetPlugin.createImageDescriptor(CheatSheetPlugin.getPlugin().getBundle(), path);
		this.setImageDescriptor(startImage);
	}

	@Override
	public void run() {
		task.setState(ICompositeCheatSheetTask.IN_PROGRESS);
	}

}
