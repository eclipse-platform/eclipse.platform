/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
 *     Wind River Systems - integration with non-standard debug models (Bug 209883)
 *******************************************************************************/
package org.eclipse.debug.internal.ui.actions.expressions;


import org.eclipse.debug.core.model.IWatchExpression;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;

/**
 * Open the watch expression dialog for the select watch expression.
 * Re-evaluate and refresh the watch expression is necessary.
 */
public class EditWatchExpressionAction extends WatchExpressionAction {

	/**
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	@Override
	public void run(IAction action) {
		IWatchExpression watchExpression= getSelectedExpressions()[0];
		// display the watch expression dialog for the currently selected watch expression
		new WatchExpressionDialog(DebugUIPlugin.getShell(), watchExpression, true).open();
	}

	@Override
	public void selectionChanged(IAction action, ISelection sel) {
		action.setEnabled(getSelectedExpressions().length == 1);
	}
}
