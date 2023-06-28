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

package org.eclipse.debug.internal.core;

import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.IDynamicVariable;
import org.eclipse.core.variables.IDynamicVariableResolver;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.osgi.service.environment.Constants;

/**
 * Resolves the value of environment variables.
 */
public class EnvironmentVariableResolver implements IDynamicVariableResolver {

	@Override
	public String resolveValue(IDynamicVariable variable, String argument) throws CoreException {
		if (argument == null) {
			throw new CoreException(new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), IStatus.ERROR, DebugCoreMessages.EnvironmentVariableResolver_0, null));
		}
		Map<String, String> map = DebugPlugin.getDefault().getLaunchManager().getNativeEnvironmentCasePreserved();
		String value = map.get(argument);
		if (value == null && Platform.getOS().equals(Constants.OS_WIN32)) {
			// On Win32, env variables are case insensitive, so we search the map
			// for matches manually.
			for (Entry<String, String> entry : map.entrySet()) {
				String key = entry.getKey();
				if (key.equalsIgnoreCase(argument)) {
					return entry.getValue();
				}
			}
		}
		return value;
	}

}
