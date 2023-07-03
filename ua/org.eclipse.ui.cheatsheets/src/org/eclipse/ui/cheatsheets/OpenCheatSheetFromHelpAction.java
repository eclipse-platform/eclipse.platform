/*******************************************************************************
 * Copyright (c) 2004, 2017 IBM Corporation and others.
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
package org.eclipse.ui.cheatsheets;

import org.eclipse.help.ILiveHelpAction;
import org.eclipse.swt.widgets.Display;

/**
 * Live help action for launching a cheat sheet from a help book.
 * <p>
 * The initialization string passed to {@link #setInitializationString(String)}
 * is the id of a cheat sheet contributed to the <code>cheatsheetContent</code>
 * extension point.
 * </p>
 *
 * @since 3.0
 */
public final class OpenCheatSheetFromHelpAction implements ILiveHelpAction {

	/**
	 * Cheat sheet id; null until initialized.
	 */
	private String cheatsheetID = null;

	/**
	 * Creates a new live help action.
	 */
	public OpenCheatSheetFromHelpAction() {
		super();
	}

	/*
	 * This method is called by the eclipse framework.  The initialization string must be the id of a
	 * registered cheat sheet in order for the action to work.
	 */
	@Override
	public void setInitializationString(String data) {
		cheatsheetID = data;
	}

	@Override
	public void run() {
		// Active help does not run on the UI thread, so we must use syncExec
		Display.getDefault().syncExec(() -> new OpenCheatSheetAction(cheatsheetID).run());
	}
}
