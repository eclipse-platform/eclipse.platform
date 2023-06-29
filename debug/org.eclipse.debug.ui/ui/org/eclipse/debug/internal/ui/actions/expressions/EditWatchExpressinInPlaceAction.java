/*******************************************************************************
 * Copyright (c) 2011, 2012 Wind River Systems and others.
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

import org.eclipse.debug.core.model.IWatchExpression;
import org.eclipse.debug.internal.ui.viewers.model.provisional.TreeModelViewer;
import org.eclipse.debug.internal.ui.views.expression.ExpressionView;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

/**
 * This action activates the cell editor in the expressions view to edit the
 * expression string.  If no expression column found or for multi-line
 * expressions, revert to the
 */
public class EditWatchExpressinInPlaceAction extends Action implements ISelectionChangedListener {

	private ExpressionView fView;
	private TreeModelViewer fViewer;
	private EditWatchExpressionAction fEditActionDelegate = new EditWatchExpressionAction();

	public EditWatchExpressinInPlaceAction(ExpressionView view) {
		fView = view;
		fViewer = (TreeModelViewer)view.getViewer();
		fEditActionDelegate.init(view);
		ISelectionProvider selectionProvider = fView.getSite().getSelectionProvider();
		selectionProvider.addSelectionChangedListener(this);
		fEditActionDelegate.selectionChanged(this, selectionProvider.getSelection());
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event) {
		IStructuredSelection selection = fEditActionDelegate.getCurrentSelection();
		setEnabled(selection != null && selection.size() == 1);
	}

	public void dispose() {
		fView.getSite().getSelectionProvider().removeSelectionChangedListener(this);
	}

	@Override
	public void run() {
		IStructuredSelection selelection = fEditActionDelegate.getCurrentSelection();

		if (selelection.size() != 1) {
			return;
		}

		// Always edit multi-line expressions in dialog.  Otherwise try to find the expression
		// column and activate cell editor there.
		int expressionColumn = getExpressionColumnIndex();
		IWatchExpression[] expressions = fEditActionDelegate.getSelectedExpressions();
		if (expressionColumn != -1 && !isWatchExpressionWithNewLine(expressions)) {
			fViewer.editElement(selelection.getFirstElement(), expressionColumn);
		} else if (expressions.length == 1) {
			fEditActionDelegate.run(this);
		}
	}

	private boolean isWatchExpressionWithNewLine(IWatchExpression[] expressions) {
		return expressions.length == 1 &&
			expressions[0].getExpressionText().indexOf('\n') != -1;
	}

	private int getExpressionColumnIndex() {
		Object[] columnProperties = fViewer.getColumnProperties();
		for (int i = 0; columnProperties != null && i < columnProperties.length; i++) {
			if (IDebugUIConstants.COLUMN_ID_VARIABLE_NAME.equals(columnProperties[i])) {
				return i;
			}
		}
		return -1;
	}
}
