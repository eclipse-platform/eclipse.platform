/*******************************************************************************
 * Copyright (c) 2011, 2019 IBM Corporation and others.
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
package org.eclipse.help.internal.base;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.eclipse.help.internal.HelpPlugin.IHelpProvider;
import org.eclipse.help.internal.base.util.ProxyUtil;
import org.eclipse.help.internal.protocols.HelpURLStreamHandler;

/*
 * Provides help document content via the internal application server.
 * The org.eclipse.help plugin should not make any assumptions about
 * where the content comes from.
 */
public class HelpProvider implements IHelpProvider {

	@Override
	public InputStream getHelpContent(String href, String locale) {
		try {
			URL helpURL = new URL("help", //$NON-NLS-1$
					null, -1, href + "?lang=" + locale, HelpURLStreamHandler.getDefault()); //$NON-NLS-1$
			return ProxyUtil.getStream(helpURL);
		} catch (IOException ioe) {
			return null;
		}
	}
}
