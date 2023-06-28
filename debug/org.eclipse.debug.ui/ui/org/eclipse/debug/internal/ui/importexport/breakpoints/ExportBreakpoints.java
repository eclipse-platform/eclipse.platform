/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
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

package org.eclipse.debug.internal.ui.importexport.breakpoints;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.actions.AbstractDebugActionDelegate;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.PlatformUI;

/**
 * <p>
 * This class provides the action event for both the context menu in breakpoints
 * view and the drop down menu in the breakpoints view.
 * </p>
 * <p>
 * The action simply calls the wizard to export breakpoints.
 * </p>
 *
 * @see WizardExportBreakpoints
 * @see WizardExportBreakpointsPage
 *
 * @since 3.2
 */
public class ExportBreakpoints extends AbstractDebugActionDelegate {

	/**
	 * This method actually performs the execution of the action event
	 *
	 * @param action IAction the action
	 */
	@Override
	public void run(IAction action) {
		WizardExportBreakpoints wiz = new WizardExportBreakpoints();
		wiz.init(PlatformUI.getWorkbench(), getSelection());
		WizardDialog wizdialog = new WizardDialog(DebugUIPlugin.getShell(), wiz);
		wizdialog.setBlockOnOpen(true);
		wizdialog.open();
	}

	@Override
	protected void update(IAction action, ISelection s) {
		super.update(action, s);
		getAction().setEnabled(DebugPlugin.getDefault().getBreakpointManager().hasBreakpoints());
	}

	@Override
	protected void doAction(Object element) throws DebugException {}

	@Override
	public void runWithEvent(IAction action, Event event) {
		run(action);
	}
}
