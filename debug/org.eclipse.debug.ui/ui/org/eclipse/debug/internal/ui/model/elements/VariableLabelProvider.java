/*******************************************************************************
 *  Copyright (c) 2006, 2012 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.model.elements;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.DefaultLabelProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IPresentationContext;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.RGB;

/**
 * Provides context sensitive labels for debug variables.
 *
 * @since 3.3
 */
public class VariableLabelProvider extends DebugElementLabelProvider {

	@Override
	protected RGB getBackground(TreePath elementPath, IPresentationContext presentationContext, String columnId) throws CoreException {
		Object element = elementPath.getLastSegment();
		if (columnId != null) {
			if (element instanceof IVariable) {
				IVariable variable = (IVariable) element;
				if (variable.hasValueChanged()) {
					return DebugUIPlugin.getPreferenceColor(IDebugUIConstants.PREF_CHANGED_VALUE_BACKGROUND).getRGB();
				}
			}
		}
		return super.getBackground(elementPath, presentationContext, columnId);
	}

	@Override
	protected RGB getForeground(TreePath elementPath, IPresentationContext presentationContext, String columnId) throws CoreException {
		Object element = elementPath.getLastSegment();
		if (columnId == null) {
			if (element instanceof IVariable) {
				IVariable variable = (IVariable) element;
				if (variable.hasValueChanged()) {
					return DebugUIPlugin.getPreferenceColor(IDebugUIConstants.PREF_CHANGED_DEBUG_ELEMENT_COLOR).getRGB();
				}
			}
		}
		return super.getForeground(elementPath, presentationContext, columnId);
	}

	@Override
	protected ImageDescriptor getImageDescriptor(TreePath elementPath, IPresentationContext presentationContext, String columnId) throws CoreException {
		if (columnId == null || IDebugUIConstants.COLUMN_ID_VARIABLE_NAME.equals(columnId)) {
			return super.getImageDescriptor(elementPath, presentationContext, columnId);
		}
		return null;
	}

	@Override
	protected FontData getFontData(TreePath elementPath, IPresentationContext presentationContext, String columnId)	throws CoreException {
		return JFaceResources.getFontDescriptor(IDebugUIConstants.PREF_VARIABLE_TEXT_FONT).getFontData()[0];
	}

	@Override
	protected String getLabel(TreePath elementPath, IPresentationContext context, String columnId) throws CoreException {
		if (columnId == null) {
			return super.getLabel(elementPath, context, columnId);
		} else {
			IVariable variable = (IVariable) elementPath.getLastSegment();
			IValue value = variable.getValue();
			return getColumnText(variable, value, context, columnId);
		}
	}

	/**
	 * Returns text for a specific columns for the variable/value.
	 *
	 * @param variable variable to retrieve text for
	 * @param value the value associated with the variable
	 * @param context presentation context specifying how to display the text
	 * @param columnId the column to get the text for
	 * @return the label text
	 * @throws CoreException Error while retrieving data from model.
	 */
	protected String getColumnText(IVariable variable, IValue value, IPresentationContext context, String columnId) throws CoreException {
		if (IDebugUIConstants.COLUMN_ID_VARIABLE_NAME.equals(columnId)) {
			return getVariableName(variable, context);
		} else if (IDebugUIConstants.COLUMN_ID_VARIABLE_TYPE.equals(columnId)) {
			return getVariableTypeName(variable, context);
		} else if (IDebugUIConstants.COLUMN_ID_VARIABLE_VALUE.equals(columnId)) {
			return getValueText(variable, value, context);
		} else if (IDebugUIConstants.COLUMN_ID_VARIABLE_VALUE_TYPE.equals(columnId)) {
			return getValueTypeName(variable, value, context);
		}
		return null;
	}

	/**
	 * Returns the name of the given variable to display in <code>COLUMN_VARIABLE_NAME</code>.
	 *
	 * @param variable Variable to get the name for.
	 * @param context View context.
	 * @return variable name
	 * @throws CoreException Error while retrieving data from model.
	 */
	protected String getVariableName(IVariable variable, IPresentationContext context) throws CoreException {
		return variable.getName();
	}

	/**
	 * Returns the type name of the given variable to display in <code>COLUMN_VARIABLE_TYPE</code>.
	 *
	 * @param variable Variable to get the type for.
	 * @param context View context.
	 * @return variable type name
	 * @throws CoreException Error while retrieving data from model.
	 */
	protected String getVariableTypeName(IVariable variable, IPresentationContext context) throws CoreException {
		return variable.getReferenceTypeName();
	}

	/**
	 * Returns the label for the given value's type to display in <code>COLUMN_VARIABLE_VALUE</code>
	 *
	 * @param variable Variable to get the value type for.
	 * @param value Variable value to get type label for.
	 * @param context View context.
	 * @return value label
	 * @throws CoreException Error while retrieving data from model.
	 */
	protected String getValueTypeName(IVariable variable, IValue value, IPresentationContext context) throws CoreException {
		return value.getReferenceTypeName();
	}

	/**
	 * Returns the label for the given value to display in <code>COLUMN_VALUE_TYPE</code>
	 *
	 * @param variable Variable to get the value for.
	 * @param value Variable value to get value label for.
	 * @param context View context.
	 * @return value label
	 * @throws CoreException Error while retrieving data from model.
	 */
	protected String getValueText(IVariable variable, IValue value, IPresentationContext context) throws CoreException {
		return escapeSpecialChars(value.getValueString());
	}

	/**
	 * Escapes special characters using the default label provider
	 *
	 * @param label the text to escape
	 * @return the string with special characters escaped
	 */
	protected String escapeSpecialChars(String label) {
		return DefaultLabelProvider.escapeSpecialChars(label);
	}
}
