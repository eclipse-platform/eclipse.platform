/*******************************************************************************
 * Copyright (c)2005 IBM Corporation and others.
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
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.actions.AbstractDebugActionDelegate;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.PlatformUI;

/**
 * This class provides the aciton event for both the context menu in breakpoints view
 * and the drop down menu in the breakpoints view.
 * <p>
 * The action simply calls the wizard to import breakpoints.
 * </p>
 *
 *  @see WizardImportBreakpoints
 *  @see WizardImportBreakpointsPage
 *
 *  @since 3.2
 */
public class ImportBreakpoints extends AbstractDebugActionDelegate {

	/**
	 * Opens import wizard
	 *
	 * @param action IAction the action
	 */
	@Override
	public void run(IAction action) {
		WizardImportBreakpoints wiz = new WizardImportBreakpoints();
		wiz.init(PlatformUI.getWorkbench(), null);
		WizardDialog wizdialog = new WizardDialog(DebugUIPlugin.getShell(), wiz);
		wizdialog.setBlockOnOpen(true);
		wizdialog.open();
	}

	@Override
	protected void doAction(Object element) throws DebugException {}

	@Override
	protected void update(IAction action, ISelection s) {
		super.update(action, s);
		getAction().setEnabled(true);
	}

	@Override
	public void runWithEvent(IAction action, Event event) {
		run(action);
	}
}
