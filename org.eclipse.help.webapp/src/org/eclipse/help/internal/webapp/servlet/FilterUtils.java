/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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

package org.eclipse.help.internal.webapp.servlet;

import javax.servlet.http.HttpServletRequest;

public class FilterUtils {

	public static String getRelativePathPrefix(HttpServletRequest req) {
		// append "../" to get to the webapp
		StringBuilder result = new StringBuilder(""); //$NON-NLS-1$
		String path = req.getPathInfo();
		if (path != null) {
			for (int i; 0 <= (i = path.indexOf('/')); path = path
					.substring(i + 1)) {
				result.append("../"); //$NON-NLS-1$
			}
		}
		return result.toString();
	}

}
