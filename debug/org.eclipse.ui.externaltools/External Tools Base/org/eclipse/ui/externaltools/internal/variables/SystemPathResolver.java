/*******************************************************************************
 * Copyright (c) 2005, 2013 IBM Corporation and others.
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
 *     Wieant (wieant@tasking.com) - Bug 138007
 *******************************************************************************/
package org.eclipse.ui.externaltools.internal.variables;

import java.io.File;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.externaltools.internal.IExternalToolConstants;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.IDynamicVariable;
import org.eclipse.core.variables.IDynamicVariableResolver;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.ui.externaltools.internal.model.ExternalToolsPlugin;

public class SystemPathResolver implements IDynamicVariableResolver {

	@Override
	public String resolveValue(IDynamicVariable variable, String argument) throws CoreException {
		if (argument == null) {
			throw new CoreException(new Status(IStatus.ERROR,  ExternalToolsPlugin.PLUGIN_ID, IExternalToolConstants.ERR_INTERNAL_ERROR, VariableMessages.SystemPathResolver_0, null));
		}
		Map<String, String> map = DebugPlugin.getDefault().getLaunchManager().getNativeEnvironment();
		String path= map.get("PATH"); //$NON-NLS-1$
		if (path == null) {
			return argument;
		}
		// On MS Windows the PATHEXT environment variable defines which file extensions
		// mark files that are executable (e.g. .EXE, .COM, .BAT)
		String pathext = map.get("PATHEXT"); //$NON-NLS-1$
		StringTokenizer tokenizer= new StringTokenizer(path, File.pathSeparator);
		while (tokenizer.hasMoreTokens()) {
			String pathElement= tokenizer.nextToken();
			File pathElementFile= new File(pathElement);
			if (pathElementFile.isDirectory()) {
				File toolFile= new File(pathElementFile, argument);
				if (toolFile.exists()) {
					return toolFile.getAbsolutePath();
				}
				if ( pathext != null ) {
					StringTokenizer pathextTokenizer = new StringTokenizer(pathext, File.pathSeparator);
					while (pathextTokenizer.hasMoreTokens()) {
						String pathextElement = pathextTokenizer.nextToken();
						toolFile = new File(pathElementFile, argument + pathextElement);
						if (toolFile.exists()) {
							return toolFile.getAbsolutePath();
						}
					}
				}
			}
		}
		return argument;
	}
}
