/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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
package org.eclipse.ui.internal.cheatsheets.handlers;

import org.eclipse.core.commands.AbstractParameterValueConverter;
import org.eclipse.core.commands.ParameterValueConversionException;

/**
 * A command parameter value converter to convert between Integers and their
 * String representations for use in the open dialog commands.
 *
 * @since 3.2
 */
public class DialogIntegerValueConverter extends
		AbstractParameterValueConverter {

	@Override
	public Object convertToObject(String parameterValue)
			throws ParameterValueConversionException {

		try {
			int i = Integer.parseInt(parameterValue);
			return Integer.valueOf(i);
		} catch (NumberFormatException ex) {
			throw new ParameterValueConversionException(
					"error converting to integer: " + parameterValue); //$NON-NLS-1$
		}
	}

	@Override
	public String convertToString(Object parameterValue)
			throws ParameterValueConversionException {

		if (!(parameterValue instanceof Integer)) {
			throw new ParameterValueConversionException(
					"value for conversion must be an Integer"); //$NON-NLS-1$
		}

		Integer i = (Integer) parameterValue;
		return Integer.toString(i.intValue());
	}

}
