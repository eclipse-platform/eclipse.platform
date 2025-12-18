/*******************************************************************************
 * Copyright (c) 2014, 2018 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.terminal.view.ui.internal.handler;

import jakarta.inject.Inject;

import org.eclipse.core.commands.ParameterizedCommand;
import org.eclipse.e4.core.commands.ECommandService;
import org.eclipse.e4.core.commands.EHandlerService;
import org.eclipse.e4.core.di.annotations.Execute;

/**
 * Quick access handler implementation.
 */
public class QuickAccessHandler {

	@Inject
	private ECommandService commandService;

	@Inject
	private EHandlerService handlerService;

	@Execute
	public void execute() {
		ParameterizedCommand command = commandService.createCommand("org.eclipse.ui.window.quickAccess", null); //$NON-NLS-1$
		if (command != null) {
			handlerService.executeHandler(command);
		}
	}

}
