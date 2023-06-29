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
package org.eclipse.core.internal.variables;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.IDynamicVariable;
import org.eclipse.core.variables.IDynamicVariableResolver;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.osgi.util.NLS;

/**
 * Dynamic variable
 */
public class DynamicVariable extends StringVariable implements IDynamicVariable {

	/**
	 * Resolver, or <code>null</code> until needed
	 */
	private IDynamicVariableResolver fResolver;

	@Override
	public String getValue(String argument) throws CoreException {
		if (!supportsArgument()) {
			// check for an argument - not supported
			if (argument != null && argument.length() > 0) {
				throw new CoreException(new Status(IStatus.ERROR, VariablesPlugin.getUniqueIdentifier(), VariablesPlugin.INTERNAL_ERROR, NLS.bind(VariablesMessages.DynamicVariable_0, new String[]{argument, getName()}), null));
			}
		}
		if (fResolver == null) {
			String name = getConfigurationElement().getAttribute("resolver"); //$NON-NLS-1$
			if (name == null) {
				throw new CoreException(new Status(IStatus.ERROR, VariablesPlugin.getUniqueIdentifier(), VariablesPlugin.INTERNAL_ERROR, NLS.bind("Contributed context variable {0} must specify a resolver.",new String[]{getName()}), null)); //$NON-NLS-1$
			}
			Object object = getConfigurationElement().createExecutableExtension("resolver"); //$NON-NLS-1$
			if (object instanceof IDynamicVariableResolver) {
				fResolver = (IDynamicVariableResolver)object;
			} else {
				throw new CoreException(new Status(IStatus.ERROR, VariablesPlugin.getUniqueIdentifier(), VariablesPlugin.INTERNAL_ERROR, NLS.bind("Contributed context variable resolver for {0} must be an instance of IContextVariableResolver.",new String[]{getName()}), null)); //$NON-NLS-1$
			}
		}
		try {
			return fResolver.resolveValue(this, argument);
		} catch (RuntimeException e) {
			throw new CoreException(new Status(IStatus.ERROR, VariablesPlugin.getUniqueIdentifier(), VariablesPlugin.INTERNAL_ERROR, NLS.bind("Error while evaluating variable {0}.",new String[]{getName()}), e)); //$NON-NLS-1$
		}
	}

	/**
	 * Constructs a new context variable.
	 *
	 * @param name variable name
	 * @param description variable description or <code>null</code>
	 * @param configurationElement configuration element
	 */
	public DynamicVariable(String name, String description, IConfigurationElement configurationElement) {
		super(name, description, configurationElement);
	}

	@Override
	public boolean supportsArgument() {
		String arg = getConfigurationElement().getAttribute("supportsArgument"); //$NON-NLS-1$
		return arg == null || Boolean.parseBoolean(arg);
	}

}
