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

import java.net.URL;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.variables.IDynamicVariable;
import org.eclipse.core.variables.IDynamicVariableResolver;

/**
 * Resolves common system variables. The current set of variables
 * supported (referenced as an argument to this variable) are:
 * <ul>
 * <li>ARCH - value of <code>BootLoader.getOSArch()</code></li>
 * <li>ECLIPSE_HOME - location of the Eclipse installation</li>
 * <li>NL - value of <code>BootLoader.getNL()</code></li>
 * <li>OS - value of <code>BootLoader.getOS()</code></li>
 * <li>WS - value of <code>BootLoader.getWS()</code></li>
 * </ul>
 * @since 3.0
 */
public class SystemVariableResolver implements IDynamicVariableResolver {
	@Override
	public String resolveValue(IDynamicVariable variable, String argument) throws CoreException {
		if ("ARCH".equals(argument)) { //$NON-NLS-1$
			return Platform.getOSArch();
		} else if ("ECLIPSE_HOME".equals(argument)) { //$NON-NLS-1$
			URL installURL = Platform.getInstallLocation().getURL();
			IPath ppath = IPath.fromOSString(installURL.getFile()).removeTrailingSeparator();
			return getCorrectPath(ppath.toOSString());
		} else if ("NL".equals(argument)) { //$NON-NLS-1$
			return Platform.getNL();
		} else if ("OS".equals(argument)) { //$NON-NLS-1$
			return Platform.getOS();
		} else if ("WS".equals(argument)) { //$NON-NLS-1$
			return Platform.getWS();
		}
		return null;
	}

	private static String getCorrectPath(String path) {
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < path.length(); i++) {
			char c = path.charAt(i);
			if (Platform.getOS().equals("win32")) { //$NON-NLS-1$
				if (i == 0 && c == '/') {
					continue;
				}
			}
			// Some VMs may return %20 instead of a space
			if (c == '%' && i + 2 < path.length()) {
				char c1 = path.charAt(i + 1);
				char c2 = path.charAt(i + 2);
				if (c1 == '2' && c2 == '0') {
					i += 2;
					buf.append(" "); //$NON-NLS-1$
					continue;
				}
			}
			buf.append(c);
		}
		return buf.toString();
	}
}
