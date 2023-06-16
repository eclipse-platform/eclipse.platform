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
package org.eclipse.debug.internal.core.variables;

import java.io.File;
import java.net.URI;
import java.text.MessageFormat;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.IDynamicVariable;
import org.eclipse.core.variables.IDynamicVariableResolver;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.DebugPlugin;

/**
 * Common function of variable resolvers.
 * Moved to debug core in 3.5, existed in debug.iu since 3.0.
 *
 * @since 3.5
 */
public class ResourceResolver implements IDynamicVariableResolver {

	@Override
	public String resolveValue(IDynamicVariable variable, String argument) throws CoreException {
		IResource resource = null;
		if (argument == null) {
			resource = getSelectedResource(variable);
		} else {
			resource = getWorkspaceRoot().findMember(IPath.fromOSString(argument));
		}
		if (resource != null && resource.exists()) {
			resource = translateSelectedResource(resource);
			if (resource != null && resource.exists()) {
				return translateToValue(resource, variable);
			}
		}
		abort(MessageFormat.format(Messages.ResourceResolver_0, new Object[] { getReferenceExpression(variable, argument) }), null);
		return null;
	}

	/**
	 * Returns the resource applicable to this resolver, relative to the selected
	 * resource. This method is called when no argument is present in a variable
	 * expression. For, example, this method might return the project for the
	 * selected resource.
	 *
	 * @param resource selected resource
	 * @return resource applicable to this variable resolver
	 */
	protected IResource translateSelectedResource(IResource resource) {
		return resource;
	}

	/**
	 * Returns the workspace root
	 *
	 * @return workspace root
	 */
	protected IWorkspaceRoot getWorkspaceRoot() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}

	/**
	 * Returns an expression used to reference the given variable and optional argument.
	 * For example, <code>${var_name:arg}</code>.
	 *
	 * @param variable referenced variable
	 * @param argument referenced argument or <code>null</code>
	 * @return variable reference expression
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

	/**
	 * Throws an exception with the given message and underlying exception.
	 *
	 * @param message exception message
	 * @param exception underlying exception or <code>null</code>
	 * @throws CoreException if a problem occurs
	 */
	protected void abort(String message, Throwable exception) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugPlugin.INTERNAL_ERROR, message, exception));
	}

	/**
	 * Returns the selected resource. Uses the ${selected_resource_path} variable
	 * to determine the selected resource. This variable is provided by the debug.ui
	 * plug-in. Selected resource resolution is only available when the debug.ui
	 * plug-in is present.
	 *
	 * @param variable variable referencing a resource
	 * @return selected resource
	 * @throws CoreException if there is no selection
	 */
	protected IResource getSelectedResource(IDynamicVariable variable) throws CoreException {
		IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
		try {
			String pathString = manager.performStringSubstitution("${selected_resource_path}"); //$NON-NLS-1$
			return ResourcesPlugin.getWorkspace().getRoot().findMember(IPath.fromOSString(pathString));
		} catch (CoreException e) {
			// unable to resolve a selection
		}
		abort(MessageFormat.format(Messages.ResourceResolver_1, new Object[] { getReferenceExpression(variable, null) }), null);
		return null;
	}

	/**
	 * Translates the given resource into a value for this variable resolver.
	 *
	 * @param resource the resource applicable to this resolver's variable
	 * @param variable the variable being resolved
	 * @return variable value
	 * @throws CoreException if the variable name is not recognized
	 */
	protected String translateToValue(IResource resource, IDynamicVariable variable) throws CoreException {
		String name = variable.getName();
		IPath path = null;
		URI uri = null;
		if (name.endsWith("_loc")) { //$NON-NLS-1$
			uri = resource.getLocationURI();
			if(uri != null) {
				File file = EFS.getStore(uri).toLocalFile(0, null);
				if(file != null) {
					return file.getAbsolutePath();
				}
			}
		} else if (name.endsWith("_path")) { //$NON-NLS-1$
			path = resource.getFullPath();
			if(path != null) {
				return path.toOSString();
			}
		} else if (name.endsWith("_name")) { //$NON-NLS-1$
			return resource.getName();
		}
		abort(MessageFormat.format(Messages.ResourceResolver_2, new Object[] { getReferenceExpression(variable, null) }), null);
		return null;
	}

}
