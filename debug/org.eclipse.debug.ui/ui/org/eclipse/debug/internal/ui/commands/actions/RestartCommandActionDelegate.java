/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *******************************************************************************/

package org.eclipse.debug.internal.ui.commands.actions;

import org.eclipse.debug.ui.actions.DebugCommandAction;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/**
 * Restart action delegate.
 *
 * @since 3.6
 */
public class RestartCommandActionDelegate implements IWorkbenchWindowActionDelegate, IActionDelegate2 {

	private DebugCommandAction fDebugAction = new RestartCommandAction();

	@Override
	public void dispose() {
		fDebugAction.dispose();
	}

	@Override
	public void init(IWorkbenchWindow window) {
		fDebugAction.init(window);
	}

	@Override
	public void run(IAction action) {
		fDebugAction.run();
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		// do nothing
	}

	@Override
	public void init(IAction action) {
		fDebugAction.setActionProxy(action);

	}

	@Override
	public void runWithEvent(IAction action, Event event) {
		run(action);
	}
}
