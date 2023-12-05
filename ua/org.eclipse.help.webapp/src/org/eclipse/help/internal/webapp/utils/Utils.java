/*******************************************************************************
 * Copyright (c) 2011, 2020 IBM Corporation and others.
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
package org.eclipse.help.internal.webapp.utils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.eclipse.help.internal.base.util.ProxyUtil;

public class Utils {

	public static final String SERVICE_CONTEXT 		= "/vs/service"; //$NON-NLS-1$

	public static final String RETURN_TYPE 			= "returnType"; //$NON-NLS-1$
	public static final String NO_CATEGORY 			= "noCategory"; //$NON-NLS-1$

	// returnType values: xml (default) | json
	public static final String XML 					= "xml"; //$NON-NLS-1$
	public static final String JSON 				= "json"; //$NON-NLS-1$
	public static final String HTML 				= "html"; //$NON-NLS-1$

	// Constants for About service
	public static final long AGENT			= 1L;
	public static final long PREFERENCE		= 2L;
	public static final long ABOUT_PLUGIN	= 3L;

	public static String readString(URL url) throws IOException {
		try (InputStream is = ProxyUtil.getStream(url)) {
			if (is != null) {
				return new String(is.readAllBytes(), StandardCharsets.UTF_8);
			} else {
				return ""; //$NON-NLS-1$
			}
		}
	}

	public static void transferContent(InputStream inputStream, OutputStream out)
			throws IOException {
		try {
			// Prepare the input stream for reading
			BufferedInputStream dataStream = new BufferedInputStream(
					inputStream);

			// Create a fixed sized buffer for reading.
			// We could create one with the size of available data...
			byte[] buffer = new byte[4096];
			int len = 0;
			while (true) {
				len = dataStream.read(buffer); // Read file into the byte array
				if (len == -1)
					break;
				out.write(buffer, 0, len);
			}
		} catch (Exception e) {
		}
	}

	public static String updateResponse(String response) {
		response = removeString(response, "advanced/synchWithToc.js"); //$NON-NLS-1$
		response = removeString(response, "index.jsp"); //$NON-NLS-1$
		return response;
	}

	private static String removeString(String response, String remove) {
		StringBuilder buff = new StringBuilder(response);
		int index = buff.indexOf(remove);
		if (index > -1) {
			int start = buff.lastIndexOf("<script", index); //$NON-NLS-1$
			int end = buff.indexOf("</script>", index); //$NON-NLS-1$
			if (start > -1 && end > -1 && start < end) {
				buff.delete(start, end + "</script>".length()); //$NON-NLS-1$
			}
		}
		return buff.toString();
	}

}
