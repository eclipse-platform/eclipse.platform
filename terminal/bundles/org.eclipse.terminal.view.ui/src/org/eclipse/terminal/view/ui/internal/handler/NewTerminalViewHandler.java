/*******************************************************************************
 * Copyright (c) 2015, 2025 Wind River Systems, Inc. and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License 2.0 which accompanies this distribution, and is
 * available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Wind River Systems - initial API and implementation
 * Alexander Fedorov (ArSysOp) - further evolution
 *******************************************************************************/
package org.eclipse.terminal.view.ui.internal.handler;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.terminal.view.ui.TerminalViewId;
import org.eclipse.terminal.view.ui.internal.UIPlugin;

/**
 * New Terminal View handler implementation
 */
public class NewTerminalViewHandler extends AbstractTriggerCommandHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		try {
			UIPlugin.getConsoleManager().showConsoleView(new TerminalViewId().next());
			triggerCommand("org.eclipse.terminal.view.ui.command.launchToolbar", null); //$NON-NLS-1$
		} catch (CoreException e) {
			throw new ExecutionException(e.getStatus().getMessage(), e);
		}
		return null;
	}

}
