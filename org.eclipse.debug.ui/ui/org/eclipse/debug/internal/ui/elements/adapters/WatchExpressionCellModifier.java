/*******************************************************************************
 * Copyright (c) 2010, 2013 Wind River Systems and others.
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
 *     IBM Corporation - bug fixing
 *******************************************************************************/
package org.eclipse.debug.internal.ui.elements.adapters;

import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IWatchExpression;
import org.eclipse.debug.internal.ui.DefaultLabelProvider;
import org.eclipse.jface.viewers.ICellModifier;

/**
 * Watch expressions modifier can change the expression name but not its value.
 *
 * @since 3.6
 */
@SuppressWarnings("deprecation")
public class WatchExpressionCellModifier implements ICellModifier {

	@Override
	public boolean canModify(Object element, String property) {
		if (VariableColumnPresentation.COLUMN_VARIABLE_NAME.equals(property)) {
			return element instanceof IWatchExpression;
		}
		return false;
	}

	@Override
	public Object getValue(Object element, String property) {
		if (VariableColumnPresentation.COLUMN_VARIABLE_NAME.equals(property)) {
			return DefaultLabelProvider.escapeSpecialChars( ((IWatchExpression)element).getExpressionText() );
		}
		return null;
	}

	@Override
	public void modify(Object element, String property, Object value) {
		Object oldValue = getValue(element, property);
		if (!value.equals(oldValue)) {
			if (VariableColumnPresentation.COLUMN_VARIABLE_NAME.equals(property)) {
				if (element instanceof IWatchExpression) {
					if (value instanceof String) {
						// The value column displays special characters
						// escaped, so encode the string with any special
						// characters escaped properly
						String expressionText = DefaultLabelProvider.encodeEsacpedChars((String)value);
						IWatchExpression expression = (IWatchExpression) element;
						// Bug 345974 see ExpressionManagerContentProvider.AddNewExpressionElement.modify does not allow an empty string
						if (expressionText.trim().length() > 0) {
							expression.setExpressionText(expressionText);
						} else {
							DebugPlugin.getDefault().getExpressionManager().removeExpression(expression);
						}
					}
				}
			}
		}
	}

}
