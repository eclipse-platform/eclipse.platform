/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
package org.eclipse.help.internal.protocols;
import java.io.*;
import java.net.*;
public class HelpURLStreamHandler extends URLStreamHandler {
	private static HelpURLStreamHandler instance;
	/**
	 * Constructor for URLHandler
	 */
	public HelpURLStreamHandler() {
		super();
	}

	@Override
	protected URLConnection openConnection(URL url) throws IOException {
		String protocol = url.getProtocol();
		if (protocol.equals("help")) { //$NON-NLS-1$
			return new HelpURLConnection(url);
		} else if (protocol.equals("localhelp")) { //$NON-NLS-1$
			return new HelpURLConnection(url, true);
		}
		return null;
	}

	public static URLStreamHandler getDefault() {
		if (instance == null) {
			instance = new HelpURLStreamHandler();
		}
		return instance;
	}
}
