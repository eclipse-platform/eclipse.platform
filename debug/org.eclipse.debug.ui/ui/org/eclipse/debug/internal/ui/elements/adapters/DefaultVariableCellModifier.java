/*******************************************************************************
 * Copyright (c) 2006, 2025 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.elements.adapters;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.DefaultLabelProvider;
import org.eclipse.debug.internal.ui.actions.variables.details.DetailPaneAssignValueAction;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.viewers.ICellModifier;

/**
 * @since 3.2
 */
public class DefaultVariableCellModifier implements ICellModifier {

	@Override
	public boolean canModify(Object element, String property) {
		if (IDebugUIConstants.COLUMN_ID_VARIABLE_VALUE.equals(property)) {
			if (element instanceof IVariable) {
				return ((IVariable) element).supportsValueModification();
			}
		}
		return false;
	}

	@Override
	public Object getValue(Object element, String property) {
		if (IDebugUIConstants.COLUMN_ID_VARIABLE_VALUE.equals(property)) {
			if (element instanceof IVariable variable) {
				try {
					return DefaultLabelProvider.escapeSpecialChars(variable.getValue().getValueString());
				} catch (DebugException e) {
					DebugUIPlugin.log(e);
				}
			}
		}
		return null;
	}

	@Override
	public void modify(Object element, String property, Object value) {
		Object oldValue = getValue(element, property);
		if (!value.equals(oldValue)) {
			if (IDebugUIConstants.COLUMN_ID_VARIABLE_VALUE.equals(property)) {
				if (element instanceof IVariable variable) {
					if (value instanceof String) {
						// The value column displays special characters escaped, so encode the string with any special characters escaped properly
						String valueExpression = DefaultLabelProvider.encodeEsacpedChars((String)value);
						DetailPaneAssignValueAction.assignValue(DebugUIPlugin.getShell(), variable, valueExpression);
					}
				}
			}
		}
	}

}
