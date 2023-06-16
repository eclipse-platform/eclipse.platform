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
 *     Bjorn Freeman-Benson - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.variables;

import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.variables.IDynamicVariable;
import org.eclipse.core.variables.IDynamicVariableResolver;
import org.eclipse.osgi.service.datalocation.Location;

/**
 * Resolver for ${eclipse_home}
 *
 * @since 3.2
 */
public class EclipseHomeVariableResolver implements IDynamicVariableResolver {

	@Override
	public String resolveValue(IDynamicVariable variable, String argument) throws CoreException {
		Location installLocation = Platform.getInstallLocation();
		if (installLocation != null) {
			URL url = installLocation.getURL();
			if (url != null) {

				// Try to convert the URL to an OS string, to be consistent with
				// how other variables, like ${workspace_loc} resolve. See
				// ResourceResolver.translateToValue(). [bugzilla 263535]
				String file = url.getFile();
				IPath path = IPath.fromOSString(file);
				String osstr = path.toOSString();
				if (osstr.length() != 0) {
					return osstr;
				}

				if (file.length() != 0) {
					return file;
				}
			}
		}
		return null;
	}

}
