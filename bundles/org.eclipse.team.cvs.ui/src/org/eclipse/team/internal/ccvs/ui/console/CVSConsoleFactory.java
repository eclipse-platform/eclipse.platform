/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.console;

import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
//commented out because of Bug 75387
//import org.eclipse.team.internal.ccvs.ui.console.CVSOutputConsole.MyLifecycle;
import org.eclipse.ui.console.*;

/**
 * Console factory is used to show the console from the Console view "Open Console"
 * drop-down action. This factory is registered via the org.eclipse.ui.console.consoleFactory 
 * extension point. 
 * 
 * @since 3.1
 */
public class CVSConsoleFactory implements IConsoleFactory {

	public CVSConsoleFactory() {
	}
	
	public void openConsole() {
		showConsole();
	}
	
	public static void showConsole() {
		CVSOutputConsole console = CVSUIPlugin.getPlugin().getConsole();
		if (console != null) {
			IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
			IConsole[] existing = manager.getConsoles();
			boolean exists = false;
			for (IConsole e : existing) {
				if (console == e) {
					exists = true;
				}
			}
			if(! exists)
				manager.addConsoles(new IConsole[] {console});
			manager.showConsoleView(console);
		}
	}
	
	public static void closeConsole() {
		IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();
		CVSOutputConsole console = CVSUIPlugin.getPlugin().getConsole();
		if (console != null) {
			manager.removeConsoles(new IConsole[] {console});
			ConsolePlugin.getDefault().getConsoleManager().addConsoleListener(console.new MyLifecycle());
		}
	}
}
