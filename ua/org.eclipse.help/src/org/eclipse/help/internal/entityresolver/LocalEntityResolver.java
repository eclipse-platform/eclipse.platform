/*******************************************************************************
 * Copyright (c) 2008, 2015 IBM Corporation and others.
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

package org.eclipse.help.internal.entityresolver;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.help.internal.HelpPlugin;
import org.osgi.framework.Bundle;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class LocalEntityResolver implements EntityResolver {

	@Override
	public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
		int index = systemId.lastIndexOf("/"); //$NON-NLS-1$
		if (index >= 0) {
			Bundle helpBundle = HelpPlugin.getDefault().getBundle();
			String dtdPath = "dtds/internal" + systemId.substring(index); //$NON-NLS-1$
			URL dtdURL = FileLocator.find(helpBundle, IPath.fromOSString(dtdPath), null);
			if (dtdURL != null) {
				InputStream stream = dtdURL.openStream();
				if (stream != null) {
					InputSource is = new InputSource(stream);
					is.setSystemId(systemId);
					is.setPublicId(publicId);
					return is;
				}
			}
		}
		return new InputSource(new StringReader("")); //$NON-NLS-1$
	}

}
