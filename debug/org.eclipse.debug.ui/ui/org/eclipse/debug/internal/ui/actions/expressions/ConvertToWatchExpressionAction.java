/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.actions.expressions;


import java.util.Iterator;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IExpressionManager;
import org.eclipse.debug.core.model.IExpression;
import org.eclipse.debug.core.model.IWatchExpression;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * Convert one or more expressions to the equivalent watch expressions.
 * Refresh and re-evaluate the expressions if possible.
 */
public class ConvertToWatchExpressionAction extends WatchExpressionAction {

	/**
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	@Override
	public void run(IAction action) {
		IStructuredSelection selection= getCurrentSelection();
		IExpressionManager expressionManager= DebugPlugin.getDefault().getExpressionManager();
		for (Iterator<?> iter = selection.iterator(); iter.hasNext();) {
			IExpression expression= (IExpression) iter.next();
			// create the new watch expression
			IWatchExpression watchExpression= expressionManager.newWatchExpression(expression.getExpressionText());
			expressionManager.removeExpression(expression);
			expressionManager.addExpression(watchExpression);
			// refresh and re-evaluate
			watchExpression.setExpressionContext(getContext());
		}
	}

}
