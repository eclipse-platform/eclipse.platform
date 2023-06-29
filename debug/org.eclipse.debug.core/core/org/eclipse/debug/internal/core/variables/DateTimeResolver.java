/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
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
package org.eclipse.debug.internal.core.variables;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.IDynamicVariable;
import org.eclipse.core.variables.IDynamicVariableResolver;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.osgi.util.NLS;

/**
 * Resolver for the <code>current_date</code> dynamic variable for launch configurations. The optional argument
 * must be a string pattern for {@link SimpleDateFormat}.  Default pattern is <code>yyyyMMdd_HHmm</code>.
 *
 * @since 3.8
 */
public class DateTimeResolver implements IDynamicVariableResolver {

	@Override
	public String resolveValue(IDynamicVariable variable, String argument) throws CoreException {
		SimpleDateFormat format = null;
		if (argument != null && argument.trim().length() > 0){
			try {
				format = new SimpleDateFormat(argument);
			} catch (IllegalArgumentException e){
				DebugPlugin.log(new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), NLS.bind(Messages.DateTimeResolver_ProblemWithDateArgument, argument), e));
			}
		}

		if (format == null){
			format = new SimpleDateFormat("yyyyMMdd_HHmm"); //$NON-NLS-1$
		}

		return format.format(new Date());
	}

}
