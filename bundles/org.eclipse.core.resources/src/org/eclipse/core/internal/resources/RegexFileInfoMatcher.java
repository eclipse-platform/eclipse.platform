/*******************************************************************************
 * Copyright (c) 2008, 2014 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Serge Beauchamp (Freescale Semiconductor) - initial API and implementation
 *     IBM Corporation - ongoing development
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import java.util.regex.*;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.resources.*;
import org.eclipse.core.resources.filtermatchers.AbstractFileInfoMatcher;
import org.eclipse.core.runtime.*;

/**
 * A Filter provider for Java Regular expression supported by
 * java.util.regex.Pattern.
 */
public class RegexFileInfoMatcher extends AbstractFileInfoMatcher {

	Pattern pattern = null;

	public RegexFileInfoMatcher() {
		// nothing to do
	}

	@Override
	public boolean matches(IContainer parent, IFileInfo fileInfo) {
		if (pattern != null) {
			Matcher m = pattern.matcher(fileInfo.getName());
			return m.matches();
		}
		return false;
	}

	@Override
	public void initialize(IProject project, Object arguments) throws CoreException {
		if (arguments != null) {
			try {
				pattern = Pattern.compile((String) arguments);
			} catch (PatternSyntaxException e) {
				throw new CoreException(new Status(IStatus.ERROR, ResourcesPlugin.PI_RESOURCES, Platform.PLUGIN_ERROR, e.getMessage(), e));
			}
		}
	}
}