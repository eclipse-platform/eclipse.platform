/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation.
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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IExpression;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.core.model.IWatchExpression;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;

/*
 * Associates the current stack frame with this expression, allowing its evaluation
 * result to remain accessible across different debug contexts.
 */
public class PinWatchContextAction implements IViewActionDelegate, IDebugEventSetListener, IActionDelegate2 {

	/**
	 * Finds the currently selected context in the UI.
	 *
	 * @return the current debug context
	 */
	protected IDebugElement getContext() {
		IAdaptable object = DebugUITools.getDebugContext();
		IDebugElement context = null;
		if (object instanceof IDebugElement iDebugElement) {
			context = iDebugElement;
		} else if (object instanceof ILaunch iLaunch) {
			context = iLaunch.getDebugTarget();
		}
		return context;
	}

	protected IStructuredSelection getCurrentSelection() {
		IWorkbenchPage page = DebugUIPlugin.getActiveWorkbenchWindow().getActivePage();
		if (page != null) {
			ISelection selection = page.getSelection();
			if (selection instanceof IStructuredSelection sel) {
				return sel;
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
			if (iter.next() instanceof IWatchExpression expression) {
				if (expression.getPinnedContext() != null) {
					expression.removePinnedContext();
					expression.setExpressionContext(context);
					action.setText(ActionMessages.ExpressionsPinContext);
				} else {
					expression.setPinnedContext(context);
					action.setText(ActionMessages.ExpressionsRemovePin);
				}
				if (expression.isEnabled()) {
					expression.evaluate();
				}
			}
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {

		IDebugElement debugElement = getContext();
		if (debugElement == null) {
			action.setEnabled(false);
			return;
		} else {
			action.setEnabled(true);
		}
		if (getCurrentSelection() == null) {
			return;
		}
		for (Object select : getCurrentSelection()) {
			if (select instanceof IWatchExpression expression) {
				if (expression.getPinnedContext() != null) {
					action.setText(ActionMessages.ExpressionsRemovePin);
				} else {
					action.setText(ActionMessages.ExpressionsPinContext);
				}
			} else {
				action.setEnabled(false);
			}

		}

	}

	@Override
	public void handleDebugEvents(DebugEvent[] events) {
		for (DebugEvent event : events) {
			if (event.getSource() instanceof IThread thread && thread.isTerminated()) {
				for (IExpression exp : DebugPlugin.getDefault().getExpressionManager().getExpressions()) {
					if (exp instanceof IWatchExpression expression && expression.getPinnedContext() != null) {
						if (expression.getPinnedContext() instanceof IStackFrame frame && frame.isTerminated()) {
							expression.removePinnedContext();
							expression.setExpressionContext(getContext());
						}
					}
				}

			}
		}

	}

	@Override
	public void init(IViewPart view) {
		DebugPlugin.getDefault().addDebugEventListener(this);

	}

	@Override
	public void init(IAction action) {
	}

	@Override
	public void dispose() {
		DebugPlugin.getDefault().removeDebugEventListener(this);
	}

	@Override
	public void runWithEvent(IAction action, Event event) {
		run(action);
	}

}
