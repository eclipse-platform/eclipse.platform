/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.compare.examples.structurecreator;

import java.io.*;
import java.util.*;

import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;


public class Util {

	private static final String RESOURCE_BUNDLE= "org.eclipse.compare.examples.structurecreator.CompareExampleMessages"; //$NON-NLS-1$

	private static ResourceBundle fgResourceBundle= ResourceBundle.getBundle(RESOURCE_BUNDLE);

	public static String getString(String key) {
		try {
			return fgResourceBundle.getString(key);
		} catch (MissingResourceException e) {
			return "!" + key + "!";	//$NON-NLS-2$ //$NON-NLS-1$
		}
	}

	/*
	 * Reads the contents of the given input stream into a string.
	 * The function assumes that the input stream uses the platform's default encoding
	 * (<code>ResourcesPlugin.getEncoding()</code>).
	 * Returns null if an error occurred.
	 */
	private static String readString(InputStream is, String encoding) {
		if (is == null)
			return null;
		try (BufferedReader reader=new BufferedReader(new InputStreamReader(is, encoding)))
		{
			StringBuilder buffer= new StringBuilder();
			char[] part= new char[2048];
			int read= 0;

			while ((read= reader.read(part)) != -1)
				buffer.append(part, 0, read);
			
			return buffer.toString();
			
		} catch (IOException ex) {
			// silently ignored
		} 
		return null;
	}

	static String readString(IStreamContentAccessor sa) throws CoreException {
		InputStream is= sa.getContents();
		String encoding= null;
		if (sa instanceof IEncodedStreamContentAccessor)
			encoding= ((IEncodedStreamContentAccessor)sa).getCharset();
		if (encoding == null)
			encoding= ResourcesPlugin.getEncoding();
		return readString(is, encoding);
	}
}
