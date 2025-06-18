/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.terminal.ui;

import java.util.Map;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.runtime.ILog;
import org.eclipse.tm.internal.terminal.provisional.api.ITerminalConnector;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleFactory;
import org.eclipse.ui.handlers.IHandlerService;

/**
 * A Factory that supports open new Terminal Console.
 */
public class TerminalConsoleFactory implements IConsoleFactory {

	@Override
	public void openConsole() {
		ICommandService commandService = PlatformUI.getWorkbench().getService(ICommandService.class);
		IHandlerService handlerService = PlatformUI.getWorkbench().getService(IHandlerService.class);
		if (commandService == null || handlerService == null) {
			return;
		}
		Command command = commandService.getCommand("org.eclipse.tm.terminal.view.ui.command.launchConsole");
		if (command == null) {
			return;
		}
		ParameterizedCommand parameterizedCommand = ParameterizedCommand.generateCommand(command, Map.of());
		try {
			Object result = handlerService.executeCommandInContext(parameterizedCommand, null,
					handlerService.getCurrentState());
			if (result instanceof ITerminalConnector terminalConnector) {
				TerminalConsole console = new TerminalConsole(terminalConnector);
				ConsolePlugin.getDefault().getConsoleManager().addConsoles(new IConsole[] { console });
			}
		} catch (Exception e) {
			ILog.get().error("Can't launch terminal console", e);
		}
	}

}
