/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.ui.externaltools.internal.variables;

import org.eclipse.core.externaltools.internal.IExternalToolConstants;
import org.eclipse.core.externaltools.internal.model.ExternalToolBuilder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.IDynamicVariable;
import org.eclipse.core.variables.IDynamicVariableResolver;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.externaltools.internal.model.ExternalToolsPlugin;


public class BuildProjectResolver implements IDynamicVariableResolver {

	@Override
	public String resolveValue(IDynamicVariable variable, String argument) throws CoreException {
		IResource resource= ExternalToolBuilder.getBuildProject();
		if (argument != null && resource != null) {
			resource = ((IProject) resource).findMember(IPath.fromOSString(argument));
		}
		if (resource != null && resource.exists()) {
			return resource.getLocation().toOSString();
		}
		abort(NLS.bind(VariableMessages.BuildProjectResolver_3, new String[]{getReferenceExpression(variable, argument)}), null);
		return null;
	}

	/**
	 * Throws an exception with the given message and underlying exception.
	 *
	 * @param message exception message
	 * @param exception underlying exception or <code>null</code>
	 * @throws CoreException
	 */
	protected void abort(String message, Throwable exception) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, ExternalToolsPlugin.PLUGIN_ID, IExternalToolConstants.ERR_INTERNAL_ERROR, message, exception));
	}

	/**
	 * Returns an expression used to reference the given variable and optional argument.
	 * For example, <code>${var_name:arg}</code>.
	 *
	 * @param variable referenced variable
	 * @param argument referenced argument or <code>null</code>
	 * @return vraiable reference expression
	 */
	protected String getReferenceExpression(IDynamicVariable variable, String argument) {
		StringBuilder reference = new StringBuilder();
		reference.append("${"); //$NON-NLS-1$
		reference.append(variable.getName());
		if (argument != null) {
			reference.append(":"); //$NON-NLS-1$
			reference.append(argument);
		}
		reference.append("}"); //$NON-NLS-1$
		return reference.toString();
	}
}
