/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.model.IWatchExpression;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

/**
 *
 */
public class EnableWatchExpressionAction implements IViewActionDelegate, IActionDelegate2, IDebugEventSetListener {

	private ISelection fSelection;
	private IAction fAction;
	protected boolean fEnable= true;

	@Override
	public void init(IViewPart view) {
		DebugPlugin.getDefault().addDebugEventListener(this);
	}

	@Override
	public void run(IAction action) {
		if (fSelection instanceof IStructuredSelection) {
			Iterator<?> iter = ((IStructuredSelection) fSelection).iterator();
			IWatchExpression expression;
			while (iter.hasNext()) {
				expression= getWatchExpression(iter.next());
				if (expression != null) {
					expression.setEnabled(fEnable);
					fireWatchExpressionChanged(expression);
				}
			}
		} else if (fSelection instanceof IWatchExpression) {
			IWatchExpression expression= ((IWatchExpression) fSelection);
			expression.setEnabled(fEnable);
			fireWatchExpressionChanged(expression);
		}
	}

	/**
	 * Fires a {@link DebugEvent} for the given watch expression
	 * @param expression the expression that has changed
	 */
	private void fireWatchExpressionChanged(IWatchExpression expression) {
		DebugPlugin.getDefault().fireDebugEventSet(new DebugEvent[] {new DebugEvent(expression, DebugEvent.CHANGE)});
	}

	@Override
	public void selectionChanged(IAction action, ISelection selection) {
		fSelection= selection;
		if (fSelection instanceof IStructuredSelection) {
			boolean enabled= false;
			Iterator<?> iter = ((IStructuredSelection) selection).iterator();
			while (iter.hasNext()) {
				IWatchExpression expression = getWatchExpression(iter.next());
				if (expression != null && expression.isEnabled() != fEnable) {
					enabled= true;
					break;
				}
			}
			action.setEnabled(enabled);
		} else if (fSelection instanceof IWatchExpression) {
			action.setEnabled(((IWatchExpression) fSelection).isEnabled() != fEnable);
		} else {
			action.setEnabled(false);
		}
	}

	private IWatchExpression getWatchExpression(Object element) {
		if (element instanceof IWatchExpression) {
			return (IWatchExpression)element;
		} else if (element instanceof IAdaptable) {
			return ((IAdaptable)element).getAdapter(IWatchExpression.class);
		}
		return null;
	}

	@Override
	public void dispose() {
		DebugPlugin.getDefault().removeDebugEventListener(this);
	}

	@Override
	public void init(IAction action) {
		fAction = action;
	}

	@Override
	public void runWithEvent(IAction action, Event event) {
		run(action);
	}

	@Override
	public void handleDebugEvents(DebugEvent[] events) {
		for (DebugEvent event : events) {
			if (event.getSource() instanceof IWatchExpression) {
				if (event.getKind() == DebugEvent.CHANGE) {
					selectionChanged(fAction, fSelection);
				}
			}
		}

	}
}
