/*******************************************************************************
 * Copyright (c) 2008, 2015 Wind River Systems and others.
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
package org.eclipse.debug.internal.ui.actions.expressions;

import java.util.Iterator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.core.model.IWatchExpression;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.actions.IWatchExpressionFactoryAdapter;
import org.eclipse.debug.ui.actions.IWatchExpressionFactoryAdapter2;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Handler for creating a watch expression.
 *
 * @since 3.4
 */
public class WatchHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		if (selection instanceof IStructuredSelection) {
			Iterator<?> iter = ((IStructuredSelection)selection).iterator();
			while (iter.hasNext()) {
				Object element = iter.next();
				createExpression(element);
			}
		}
		return null;
	}


	private void showExpressionsView() {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IViewPart part = page.findView(IDebugUIConstants.ID_EXPRESSION_VIEW);
		if (part == null) {
			try {
				page.showView(IDebugUIConstants.ID_EXPRESSION_VIEW);
			} catch (PartInitException e) {
			}
		} else {
			page.bringToTop(part);
		}

	}

	private void createExpression(Object element) {
		String expressionString;
		try {
			if (element instanceof IVariable) {
				IVariable variable = (IVariable)element;
				IWatchExpressionFactoryAdapter factory = getFactory(variable);
				expressionString = variable.getName();
				if (factory != null) {
					expressionString = factory.createWatchExpression(variable);
				}
			} else {
				IWatchExpressionFactoryAdapter2 factory2 = getFactory2(element);
				if (factory2 != null) {
					expressionString = factory2.createWatchExpression(element);
				} else {
					// Action should not have been enabled
					return;
				}
			}
		} catch (CoreException e) {
			DebugUIPlugin.errorDialog(DebugUIPlugin.getShell(), ActionMessages.WatchAction_0, ActionMessages.WatchAction_1, e); //
			return;
		}

		IWatchExpression expression;
			expression = DebugPlugin.getDefault().getExpressionManager().newWatchExpression(expressionString);
		DebugPlugin.getDefault().getExpressionManager().addExpression(expression);
		IAdaptable object = DebugUITools.getDebugContext();
		IDebugElement context = null;
		if (object instanceof IDebugElement) {
			context = (IDebugElement) object;
		} else if (object instanceof ILaunch) {
			context = ((ILaunch) object).getDebugTarget();
		}
		expression.setExpressionContext(context);
		showExpressionsView();
	}


	/**
	 * Returns the factory adapter for the given variable or <code>null</code> if none.
	 *
	 * @param variable the variable to get the factory for
	 * @return factory or <code>null</code>
	 */
	static IWatchExpressionFactoryAdapter getFactory(IVariable variable) {
		return variable.getAdapter(IWatchExpressionFactoryAdapter.class);
	}

	/**
	 * Returns the factory adapter for the given variable or <code>null</code> if none.
	 *
	 * @param element the element to try and adapt
	 * @return factory or <code>null</code>
	 */
	static IWatchExpressionFactoryAdapter2 getFactory2(Object element) {
		if (element instanceof IAdaptable) {
			return ((IAdaptable)element).getAdapter(IWatchExpressionFactoryAdapter2.class);
		}
		return null;
	}

}
