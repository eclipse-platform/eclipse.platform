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

public class SkipAction extends Action {

	private static final String SKIP_CCS_TASK_GIF = "skip_ccs_task.png"; //$NON-NLS-1$
	private AbstractTask task;

	public SkipAction(ICompositeCheatSheetTask task) {
		this.task = (AbstractTask) task;
		this.setText(Messages.COMPOSITE_MENU_SKIP);
		IPath ePath = CheatSheetPlugin.ICONS_PATH.append(CheatSheetPlugin.T_ELCL).append(SKIP_CCS_TASK_GIF);
		ImageDescriptor skipImage = CheatSheetPlugin.createImageDescriptor(CheatSheetPlugin.getPlugin().getBundle(), ePath);
		this.setImageDescriptor(skipImage);
	}

	@Override
	public void run() {
		task.setState(ICompositeCheatSheetTask.SKIPPED);
	}

}
