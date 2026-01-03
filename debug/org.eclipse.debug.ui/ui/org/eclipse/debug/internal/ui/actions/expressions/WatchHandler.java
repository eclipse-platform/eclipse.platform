/*******************************************************************************
 * Copyright (c) 2008, 2025 Wind River Systems and others.
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
 *     IBM Corporation - Improved expression creation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.actions.expressions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugException;
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
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
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
		if (selection instanceof TreeSelection treeSelection) {
			for (TreePath path : treeSelection.getPaths()) {
				if (path.getSegmentCount() > 1) {
					StringBuilder expressionString = new StringBuilder();
					for (int e = 0; e < path.getSegmentCount(); e++) {
						IVariable variable = (IVariable) path.getSegment(e);
						try {
							expressionString.append(variable.getName());
							expressionString.append("."); //$NON-NLS-1$
						} catch (DebugException e1) {
							DebugUIPlugin.log(e1);
						}
					}
					expressionString.deleteCharAt(expressionString.length() - 1);
					createWatchExpression(expressionString.toString());
				} else {
					Object element = path.getFirstSegment();
					createExpression(element);
				}
				showExpressionsView();
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
			if (element instanceof IVariable variable) {
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
		createWatchExpression(expressionString);
		showExpressionsView();
	}

	private void createWatchExpression(String expressionString) {
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
