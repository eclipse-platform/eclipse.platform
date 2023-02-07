/*******************************************************************************
 * Copyright (c) 2002, 2019 IBM Corporation and others.
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
 *     Christoph Läubrich - Bug 552773 - Simplify logging in platform code base
 *******************************************************************************/
package org.eclipse.ui.internal.cheatsheets.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.cheatsheets.ICheatSheetAction;
import org.eclipse.ui.cheatsheets.ICheatSheetManager;
import org.eclipse.ui.internal.cheatsheets.CheatSheetPlugin;
import org.eclipse.ui.internal.cheatsheets.Messages;

/**
 * Action to programmatically open a perspective from a cheat sheet.
 * The perspective id must be passed to the action via param1.
 *
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 *
 */
public class OpenPerspective extends Action implements ICheatSheetAction {

	/**
	 * Create a new <code>OpenPerspective</code> action.
	 */
	public OpenPerspective() {
	}


	@Override
	public void run(String[] params, ICheatSheetManager manager) {
		try {
			if(params == null || params[0] == null) {
				return;
			}

			IWorkbench workbench = PlatformUI.getWorkbench();
			IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
			IWorkbenchPage page = window.getActivePage();

			IPerspectiveDescriptor perspective = workbench.getPerspectiveRegistry().findPerspectiveWithId(params[0]);
			page.setPerspective(perspective);
		} catch(Exception e) {
			CheatSheetPlugin.getPlugin().getLog().error(Messages.ERROR_OPENING_PERSPECTIVE, null);
		}
	}
}
