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
 *******************************************************************************/
package org.eclipse.ui.internal.cheatsheets.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.cheatsheets.dialogs.CheatSheetCategoryBasedSelectionDialog;
import org.eclipse.ui.internal.cheatsheets.registry.CheatSheetCollectionElement;
import org.eclipse.ui.internal.cheatsheets.registry.CheatSheetRegistryReader;

/**
 * Action to programmatically open the CheatSheet selection dialog.
 *
 * <p>
 * This class may be instantiated; it is not intended to be subclassed.
 * </p>
 *
 */
public class CheatSheetCategoryBasedSelectionAction extends Action {

	/**
	 * Create a new <code>CheatSheetCategoryBasedSelectionAction</code> action.
	 */
	public CheatSheetCategoryBasedSelectionAction() {
	}

	/**
	 * Constructor for CheatSheetCategoryBasedSelectionAction.
	 * @param text
	 */
	public CheatSheetCategoryBasedSelectionAction(String text) {
		super(text);
	}

	/**
	 * Constructor for CheatSheetCategoryBasedSelectionAction.
	 * @param text
	 * @param image
	 */
	public CheatSheetCategoryBasedSelectionAction(String text, ImageDescriptor image) {
		super(text, image);
	}

	@Override
	public void run() {
		CheatSheetCollectionElement cheatSheets = CheatSheetRegistryReader.getInstance().getCheatSheets();

		CheatSheetCategoryBasedSelectionDialog dialog = new CheatSheetCategoryBasedSelectionDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), cheatSheets);

		if(dialog.open() != Window.OK || !dialog.getStatus().isOK()) {
			notifyResult(false);
			return;
		}

		notifyResult(true);
	}
}

