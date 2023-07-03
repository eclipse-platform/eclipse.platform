/*******************************************************************************
 * Copyright (c) 2002, 2015 IBM Corporation and others.
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
package org.eclipse.ua.tests.cheatsheet.util;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.cheatsheets.ICheatSheetAction;
import org.eclipse.ui.cheatsheets.ICheatSheetManager;
import org.eclipse.ui.cheatsheets.OpenCheatSheetAction;

public class NavigateAction extends Action implements ICheatSheetAction {

	private static void go(String newSheet) {
		final String finalNewSheet = newSheet;
		try {
			Display.getCurrent().asyncExec( () -> {
				OpenCheatSheetAction csAction = new OpenCheatSheetAction( finalNewSheet );
				csAction.run();
			} );
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}

	@Override
	public void run( String[] params, ICheatSheetManager manager ) {
		go( params[ 0 ] );
	}
}
