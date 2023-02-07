/*******************************************************************************
 * Copyright (c) 2007, 2016 IBM Corporation and others.
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

package org.eclipse.help.internal;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URL;
import java.net.URLConnection;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;


public class CachedEntityResolver implements EntityResolver {

	@Override
	public InputSource resolveEntity(String publicId, String systemId) {
		int index = systemId.lastIndexOf("/"); //$NON-NLS-1$
		File cachedCopy = null;
		if (index != -1) {
			cachedCopy = new File(HelpPlugin.getConfigurationDirectory(), "/DTDs"); //$NON-NLS-1$
			cachedCopy.mkdirs();
			cachedCopy = new File(cachedCopy, systemId.substring(index));
		}
		if (cachedCopy != null) {
			if (!cachedCopy.exists()) {
				try {
					URL system = new URL(systemId);
					URLConnection sc = system.openConnection();
					String inputLine;
					try (BufferedReader in = new BufferedReader(new InputStreamReader(sc.getInputStream()));
							BufferedWriter out = new BufferedWriter(new FileWriter(cachedCopy))) {
						while ((inputLine = in.readLine()) != null) {
							out.write(inputLine);
							out.newLine();
						}
						out.flush();
					}
				} catch (IOException e) {
				}
			}
			try {
				InputSource is = new InputSource(new FileReader(cachedCopy));
				is.setSystemId(systemId);
				is.setPublicId(publicId);
				return is;
			} catch (FileNotFoundException e) {}
		}
		return new InputSource(new StringReader("")); //$NON-NLS-1$
	}

}
