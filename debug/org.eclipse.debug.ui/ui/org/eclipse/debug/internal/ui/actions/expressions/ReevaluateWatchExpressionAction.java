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
 *     Wind River Systems - integration with non-standard debug models (Bug 209883)
 *******************************************************************************/
package org.eclipse.debug.internal.ui.actions.expressions;


import java.util.Iterator;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IWatchExpression;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Ask to re-evaluate one or more watch expressions in the context of the
 * currently selected thread.
 */
public class ReevaluateWatchExpressionAction implements IObjectActionDelegate {

	@Override
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
	}

	/**
	 * Finds the currently selected context in the UI.
	 * @return the current debug context
	 */
	protected IDebugElement getContext() {
		IAdaptable object = DebugUITools.getDebugContext();
		IDebugElement context = null;
		if (object instanceof IDebugElement) {
			context = (IDebugElement) object;
		} else if (object instanceof ILaunch) {
			context = ((ILaunch) object).getDebugTarget();
		}
		return context;
	}

	protected IStructuredSelection getCurrentSelection() {
		IWorkbenchPage page = DebugUIPlugin.getActiveWorkbenchWindow().getActivePage();
		if (page != null) {
			ISelection selection = page.getSelection();
			if (selection instanceof IStructuredSelection) {
				return (IStructuredSelection) selection;
			}
		}
		return null;
	}

	/**
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	@Override
	public void run(IAction action) {
		IDebugElement context = getContext();
		for (Iterator<?> iter = getCurrentSelection().iterator(); iter.hasNext();) {
			IWatchExpression expression= (IWatchExpression) iter.next();
			expression.setExpressionContext(context);
			if (!expression.isEnabled()) {
				// Force a reevaluation
				expression.evaluate();
			}
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		IDebugElement debugElement = getContext();
		if (debugElement == null) {
			action.setEnabled(false);
		} else {
			action.setEnabled(true);
		}
	}

}
