/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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

package org.eclipse.ui.internal.cheatsheets;

import org.eclipse.core.commands.ParameterType;
import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.core.commands.common.CommandException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.internal.cheatsheets.data.CheatSheetCommand;
import org.eclipse.ui.internal.cheatsheets.views.CheatSheetManager;

/**
 * Execute a command defined in a cheatsheet
 */

public class CommandRunner {

	private ICommandService getCommandService() {
		IWorkbench wb =	PlatformUI.getWorkbench();
		if (wb != null) {
			ICommandService serviceObject = wb.getAdapter(ICommandService.class);
			if (serviceObject != null) {
				return serviceObject;
			}
		}
		return null;
	}

	private IHandlerService getHandlerService() {
		IWorkbench wb =	PlatformUI.getWorkbench();
		if (wb != null) {
			IHandlerService serviceObject = wb.getAdapter(IHandlerService.class);
			if (serviceObject != null) {
				return serviceObject;
			}
		}
		return null;
	}

	/**
	 * Attempt to execute a command
	 * @param command a CheatSheetCommand created by the parser
	 * @param csm
	 * @return OK_STATUS if the command completes withour error, otherwise
	 * an error status
	 */
	public IStatus executeCommand(CheatSheetCommand command, CheatSheetManager csm) {
		ICommandService commandService = getCommandService();
		IHandlerService handlerService = getHandlerService();
		if (commandService == null || handlerService == null) {
			return new Status
			(IStatus.ERROR,
			ICheatSheetResource.CHEAT_SHEET_PLUGIN_ID, 0,
			Messages.ERROR_COMMAND_SERVICE_UNAVAILABLE, null);
		}

		ParameterizedCommand selectedCommand;
		Object result;
		String rawSerialization = command.getSerialization();
		try {
			String substitutedSerialization = csm.performVariableSubstitution(rawSerialization);
			selectedCommand = commandService.deserialize(substitutedSerialization);
			result = handlerService.executeCommand(selectedCommand, null);


			String returnsAttribute = command.getReturns();
			if ((returnsAttribute != null) && (result != null)) {
				ParameterType returnType = selectedCommand.getCommand().getReturnType();
				if ((returnType != null && (returnType.getValueConverter() != null))) {
					String resultString = returnType.getValueConverter().convertToString(result);
					csm.setDataQualified(returnsAttribute, resultString);
				}
				else {
					if (result instanceof String) {
						csm.setDataQualified(returnsAttribute, (String)result);
					}
				}
			}

		} catch (NotDefinedException e) {
			String message = NLS.bind(Messages.ERROR_COMMAND_ID_NOT_FOUND, (new Object[] {rawSerialization}));
			return new Status(IStatus.ERROR, ICheatSheetResource.CHEAT_SHEET_PLUGIN_ID, IStatus.OK, message, e);
		} catch (CommandException e) {
			return commandFailureStatus(e);
		} catch (Exception e) {
			return commandFailureStatus(e);
		}

		return Status.OK_STATUS;
	}

	private IStatus commandFailureStatus(Exception exception) {
		return new Status
		(IStatus.ERROR,
		ICheatSheetResource.CHEAT_SHEET_PLUGIN_ID, 0,
		Messages.ERROR_COMMAND_ERROR_STATUS, exception);
	}

}
