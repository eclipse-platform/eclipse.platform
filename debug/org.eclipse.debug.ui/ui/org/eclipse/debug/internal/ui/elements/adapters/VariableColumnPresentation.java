/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
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

import org.eclipse.debug.internal.ui.viewers.provisional.AbstractColumnPresentation;
import org.eclipse.debug.ui.IDebugUIConstants;

/**
 * Columns for Java variables.
 *
 * @since 3.2
 */
public class VariableColumnPresentation extends AbstractColumnPresentation {

	/**
	 * Constant identifier for the default variable column presentation.
	 * @deprecated Replaced by {@link IDebugUIConstants#COLUMN_PRESENTATION_ID_VARIABLE}
	 */
	@Deprecated
	public final static String DEFAULT_VARIABLE_COLUMN_PRESENTATION = IDebugUIConstants.COLUMN_PRESENTATION_ID_VARIABLE;

	/**
	 * Default column identifiers
	 * @deprecated Replaced by {@link IDebugUIConstants#COLUMN_ID_VARIABLE_NAME}
	 */
	@Deprecated
	public final static String COLUMN_VARIABLE_NAME = IDebugUIConstants.COLUMN_ID_VARIABLE_NAME;
	/**
	 * @deprecated Replaced by {@link IDebugUIConstants#COLUMN_ID_VARIABLE_TYPE}
	 */
	@Deprecated
	public final static String COLUMN_VARIABLE_TYPE = IDebugUIConstants.COLUMN_ID_VARIABLE_TYPE;
	/**
	 * @deprecated Replaced by {@link IDebugUIConstants#COLUMN_ID_VARIABLE_VALUE}
	 */
	@Deprecated
	public final static String COLUMN_VARIABLE_VALUE = IDebugUIConstants.COLUMN_ID_VARIABLE_VALUE;
	/**
	 * @deprecated Replaced by {@link IDebugUIConstants#COLUMN_ID_VARIABLE_VALUE_TYPE}
	 */
	@Deprecated
	public final static String COLUMN_VALUE_TYPE = IDebugUIConstants.COLUMN_ID_VARIABLE_VALUE_TYPE;

	private static final String[] ALL_COLUMNS = new String[]{IDebugUIConstants.COLUMN_ID_VARIABLE_NAME,
		IDebugUIConstants.COLUMN_ID_VARIABLE_TYPE, IDebugUIConstants.COLUMN_ID_VARIABLE_VALUE, IDebugUIConstants.COLUMN_ID_VARIABLE_VALUE_TYPE};
	private static final String[] INITIAL_COLUMNS = new String[]{IDebugUIConstants.COLUMN_ID_VARIABLE_NAME,
		IDebugUIConstants.COLUMN_ID_VARIABLE_VALUE};

	@Override
	public String[] getAvailableColumns() {
		return ALL_COLUMNS;
	}

	@Override
	public String getHeader(String id) {
		if (IDebugUIConstants.COLUMN_ID_VARIABLE_TYPE.equals(id)) {
			return Messages.VariableColumnPresentation_0;
		}
		if (IDebugUIConstants.COLUMN_ID_VARIABLE_NAME.equals(id)) {
			return Messages.VariableColumnPresentation_1;
		}
		if (IDebugUIConstants.COLUMN_ID_VARIABLE_VALUE.equals(id)) {
			return Messages.VariableColumnPresentation_2;
		}
		if (IDebugUIConstants.COLUMN_ID_VARIABLE_VALUE_TYPE.equals(id)) {
			return Messages.VariableColumnPresentation_3;
		}
		return null;
	}

	@Override
	public String getId() {
		return IDebugUIConstants.COLUMN_PRESENTATION_ID_VARIABLE;
	}

	@Override
	public String[] getInitialColumns() {
		return INITIAL_COLUMNS;
	}

	@Override
	public boolean isOptional() {
		return true;
	}

}
