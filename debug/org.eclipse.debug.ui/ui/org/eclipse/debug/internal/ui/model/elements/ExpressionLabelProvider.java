/*******************************************************************************
 * Copyright (c) 2006, 2014 IBM Corporation and others.
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
 *     Wind Rvier Systems - added support for columns (bug 235646)
 *     Ralf M Petter <ralf.petter@gmail.com> - (bug 470536)
 *******************************************************************************/
package org.eclipse.debug.internal.ui.model.elements;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IErrorReportingExpression;
import org.eclipse.debug.core.model.IExpression;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IWatchExpression;
import org.eclipse.debug.internal.ui.DebugUIMessages;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.swt.graphics.RGB;

/**
 * @since 3.3
 */
public class ExpressionLabelProvider extends VariableLabelProvider {

	@Override
	protected RGB getForeground(TreePath elementPath, IPresentationContext presentationContext, String columnId) throws CoreException {
		Object element = elementPath.getLastSegment();
		if (element instanceof IErrorReportingExpression) {
			IErrorReportingExpression expression = (IErrorReportingExpression) element;
			if (expression.hasErrors()) {
				if (columnId == null || columnId.equals(IDebugUIConstants.COLUMN_ID_VARIABLE_VALUE)) {
					return new RGB(255, 0, 0);
				}
			}
		}
		return super.getForeground(elementPath, presentationContext, columnId);
	}

	@Override
protected String getLabel(TreePath elementPath, IPresentationContext context, String columnId) throws CoreException {
		if (columnId == null) {
			return super.getLabel(elementPath, context, columnId);
		} else {
			IExpression expression = (IExpression) elementPath.getLastSegment();
			IValue value = expression.getValue();
			return getColumnText(expression, value, context, columnId);
		}
	}

	/**
	 * Returns text for a specific columns for the expression/value.
	 *
	 * @param expression expression to retrieve text for
	 * @param value the value associated with the variable
	 * @param context presentation context specifying how to display the text
	 * @param columnId the column to get the text for
	 * @return the label text
	 * @throws CoreException Error while retrieving data from model.
	 *
	 * @since 3.6
	 */
	private String getColumnText(IExpression expression, IValue value, IPresentationContext context, String columnId) throws CoreException {
		if (IDebugUIConstants.COLUMN_ID_VARIABLE_NAME.equals(columnId)) {
			return getExpressionName(expression, context);
		} else if (IDebugUIConstants.COLUMN_ID_VARIABLE_VALUE.equals(columnId)) {
			return getExpressionValueText(expression, value, context);
		} else if (IDebugUIConstants.COLUMN_ID_VARIABLE_TYPE.equals(columnId) ||
				IDebugUIConstants.COLUMN_ID_VARIABLE_VALUE_TYPE.equals(columnId))
		{
			if (value != null) {
				return getValueTypeName(null, value, context);
			}
		}
		return null;
	}

	/**
	 * Returns the expression's text to show in the view's name column.
	 *
	 * @param expression expression to retrieve text for
	 * @param context presentation context specifying how to display the text
	 * @return Returns the expression's text to show in the view's name column.
	 * @exception CoreException in an error occurs
	 * @since 3.6
	 */
	protected String getExpressionName(IExpression expression, IPresentationContext context) throws CoreException {
		if (expression instanceof IWatchExpression) {
			return getWatchExpressionName((IWatchExpression) expression, context);
		}
		return expression.getExpressionText();
	}

	/**
	 * Returns the watch expression's text to show in the view's name column.
	 *
	 * @param expression the expression
	 * @param context associated presentation context
	 * @return Returns the watch expression's text to show in the view's name column.
	 * @since 3.6
	 */
	private String getWatchExpressionName(IWatchExpression expression, IPresentationContext context) {
		StringBuilder result= new StringBuilder();

		String snippet = expression.getExpressionText().trim();
		StringBuilder snippetBuffer = new StringBuilder();
		if (snippet.length() > 254) {
			snippetBuffer.append(snippet.substring(0, 127));
			snippetBuffer.append(DebugUIMessages.DefaultLabelProvider_0);
			snippetBuffer.append(snippet.substring(snippet.length() - 127));
		} else {
			snippetBuffer.append(snippet);
		}
		snippet = snippetBuffer.toString().replaceAll("[\n\r\t]+", " ");  //$NON-NLS-1$//$NON-NLS-2$

		result.append('"');
		result.append(snippet);
		result.append('"');

		return result.toString();
	}

	/**
	 * Returns the expression's value, or a message to show in the value column,
	 * if the value is not available.
	 *
	 * @param expression expression to retrieve text for
	 * @param value the value associated with the variable
	 * @param context presentation context specifying how to display the text
	 * @return string representing the expression's value
	 * @throws CoreException Error while retrieving data from model.
	 *
	 * @since 3.6
	 */
	protected String getExpressionValueText(IExpression expression, IValue value, IPresentationContext context) throws CoreException {
		if (expression instanceof IWatchExpression) {
			IWatchExpression watchExpression = (IWatchExpression)expression;
			StringBuilder result = new StringBuilder();

			if (watchExpression.isPending() && value == null) {
				result.append(DebugUIMessages.DefaultLabelProvider_12);
			} else if (watchExpression.hasErrors()) {
				result.append(DebugUIMessages.DefaultLabelProvider_13);
			} else if (value != null) {
				result.append( getValueText(null, value, context) );
			}
			if (!watchExpression.isEnabled()) {
				result.append(DebugUIMessages.DefaultLabelProvider_15);
			}

			return result.toString();
		}

		if (value != null) {
			return getValueText(null, value, context);
		}
		return null;
	}

}
