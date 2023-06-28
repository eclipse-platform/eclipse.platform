/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.ui.internal.console;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleView;

/**
 * Shows a specific console in the console view
 */
public class ShowConsoleAction extends Action {

	private IConsole fConsole;
	private IConsoleView fView;

	@Override
	public void run() {
		showConsole(fConsole, fView);
	}

	/**
	 * Shows the given console in the given console view.
	 *
	 * @param console the console to show
	 * @param consoleView the console view
	 */
	public static void showConsole(IConsole console, IConsoleView consoleView) {
		if (!console.equals(consoleView.getConsole())) {
			boolean pinned = consoleView.isPinned();
			if (pinned) {
				consoleView.setPinned(false);
			}
			consoleView.display(console);
			if (pinned) {
				consoleView.setPinned(true);
			}
		}
	}

	/**
	 * Constructs an action to display the given console.
	 *
	 * @param view the console view in which the given console is contained
	 * @param console the console
	 */
	public ShowConsoleAction(IConsoleView view, IConsole console) {
		super(console.getName(), AS_RADIO_BUTTON);
		fConsole = console;
		fView = view;
		setImageDescriptor(console.getImageDescriptor());
	}
}
