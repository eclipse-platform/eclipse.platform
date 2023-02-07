/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
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

package org.eclipse.ua.tests.doc.internal.linkchecker;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletRequest;

import org.eclipse.help.internal.webapp.servlet.FilterHTMLHeadOutputStream;
import org.eclipse.help.internal.webapp.servlet.FilterUtils;
import org.eclipse.help.webapp.IFilter;

/**
 * This class inserts a CSSs for narrow and disabled CSSs when called from the
 * dynamic help view.
 */
public class AddScriptFilter implements IFilter {
	@Override
	public OutputStream filter(HttpServletRequest req, OutputStream out) {
		String relativePath = FilterUtils.getRelativePathPrefix(req);
		StringBuilder script = new StringBuilder();
		script.append("\n<script type=\"text/javascript\" src=\"");
		script.append(relativePath);
		script.append("content/org.eclipse.ua.tests.doc/checkdoc.js\"> </script>"); //$NON-NLS-1$

		return new FilterHTMLHeadOutputStream(out, script.toString().getBytes(StandardCharsets.US_ASCII));
	}

}
