/*******************************************************************************
 *  Copyright (c) 2026 Andrey Loskutov <loskutov@gmx.de> and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.debug.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class ConsoleExtension implements BeforeEachCallback, AfterEachCallback {

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		IConsole[] consoles = getConsoleManager().getConsoles();
		assertEquals(0, consoles.length, "Should have no consoles before test, but found: " + Arrays.toString(consoles));
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		removeAllConsoles();
	}

	private static void removeAllConsoles() {
		IConsoleManager consoleManager = getConsoleManager();
		consoleManager.removeConsoles(consoleManager.getConsoles());
		IConsole[] consoles = consoleManager.getConsoles();
		assertEquals(0, consoles.length, "Should have no consoles after test, but found: " + Arrays.toString(consoles));
	}

	private static IConsoleManager getConsoleManager() {
		return ConsolePlugin.getDefault().getConsoleManager();
	}

}
