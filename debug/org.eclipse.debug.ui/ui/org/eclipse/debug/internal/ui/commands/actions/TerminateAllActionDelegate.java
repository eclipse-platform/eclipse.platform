/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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

package org.eclipse.debug.internal.ui.commands.actions;

import org.eclipse.debug.ui.actions.DebugCommandAction;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

/**
 * Terminate action delegate.
 */
public class TerminateAllActionDelegate implements IWorkbenchWindowActionDelegate, IActionDelegate2, IViewActionDelegate {

	private DebugCommandAction fConsoleAction = new TerminateAllAction();

	@Override
	public void dispose() {
		fConsoleAction.dispose();
	}

	@Override
	public void init(IWorkbenchWindow window) {
		fConsoleAction.init(window);
	}

	@Override
	public void run(IAction action) {
		fConsoleAction.run();
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		// do nothing
	}

	@Override
	public void init(IAction action) {
		fConsoleAction.setActionProxy(action);

	}

	@Override
	public void runWithEvent(IAction action, Event event) {
		run(action);
	}

	@Override
	public void init(IViewPart view) {
		fConsoleAction.init(view);
	}
}