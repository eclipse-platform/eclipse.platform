/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
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

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.IDynamicVariable;
import org.eclipse.core.variables.IDynamicVariableResolver;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.osgi.util.NLS;

/**
 * Resolves the <code>${workspace_loc}</code> variable. The variable resolves to the
 * location of the workspace. If an argument is provided, it is interpreted as a
 * workspace relative path to a specific resource.
 */
public final class WorkspaceResolver implements IDynamicVariableResolver {

	@Override
	public String resolveValue(IDynamicVariable variable, String argument) throws CoreException {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IResource resource;

		if (argument == null) {
			resource = root;
		} else {
			resource = root.findMember(IPath.fromOSString(argument));
		}

		if (resource != null && resource.exists()) {
			URI uri = resource.getLocationURI();

			if (uri != null) {
				File file = EFS.getStore(uri).toLocalFile(EFS.NONE, null);

				if (file != null) {
					return file.getAbsolutePath();
				}
			}
		}

		String expression = VariablesPlugin.getDefault().getStringVariableManager().generateVariableExpression(variable.getName(), argument);
		String message = NLS.bind(Messages.WorkspaceResolver_0, expression);

		throw new CoreException(new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, IResourceStatus.RESOURCE_NOT_FOUND, message, null));
	}
}
