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
import org.eclipse.jface.window.Window;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.cheatsheets.CheatSheetPlugin;
import org.eclipse.ui.internal.cheatsheets.Messages;
import org.eclipse.ui.internal.cheatsheets.composite.model.AbstractTask;
import org.eclipse.ui.internal.cheatsheets.composite.model.CompositeCheatSheetModel;
import org.eclipse.ui.internal.cheatsheets.composite.model.TaskStateUtilities;
import org.eclipse.ui.internal.provisional.cheatsheets.ICompositeCheatSheetTask;

/**
 * Action to reset a single task and its children
 */

public class ResetTaskAction extends Action {
	private final AbstractTask task;

	public ResetTaskAction(ICompositeCheatSheetTask task) {
		this.task = (AbstractTask) task;
		this.setText(Messages.COMPOSITE_MENU_RESET);
		IPath path = CheatSheetPlugin.ICONS_PATH.append(CheatSheetPlugin.T_ELCL).append("return_to_start.png");//$NON-NLS-1$
		ImageDescriptor restartImage = CheatSheetPlugin.createImageDescriptor(CheatSheetPlugin.getPlugin().getBundle(), path);
		this.setImageDescriptor(restartImage);
	}

	@Override
	public void run() {
		AbstractTask[] restartTasks = TaskStateUtilities.getRestartTasks(task);
		if (restartTasks.length == 0) return;
		TreeLabelProvider labelProvider = new TreeLabelProvider();
		ConfirmRestartDialog dlg = new ConfirmRestartDialog(
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), restartTasks, labelProvider);
		dlg.open();
		labelProvider.dispose();
		if (dlg.getReturnCode() == Window.OK) {
			CompositeCheatSheetModel model = (CompositeCheatSheetModel) restartTasks[0].getCompositeCheatSheet();
			model.resetTasks(restartTasks);
		}
	}

}
