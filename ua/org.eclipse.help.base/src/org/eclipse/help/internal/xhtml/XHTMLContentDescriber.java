/*******************************************************************************
 * Copyright (c) 2006, 2019 IBM Corporation and others.
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
package org.eclipse.help.internal.xhtml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescriber;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.help.internal.search.ASCIIReader;

/**
 * A content describer for XHTML.
 */
public class XHTMLContentDescriber implements IContentDescriber {

	private static final String XHTML_DTD_PREFIX = "http://www.w3.org/TR/xhtml"; //$NON-NLS-1$

	public static final int BUFFER_SIZE = 4096;

	@Override
	public int describe(InputStream contents, IContentDescription description) throws IOException {
		try (Reader reader = new ASCIIReader(contents, BUFFER_SIZE)) {
			char[] chars = new char[BUFFER_SIZE];
			reader.read(chars);
			String str = new String(chars);
			return (str.contains(XHTML_DTD_PREFIX)) ? VALID : INVALID;
		}
		catch (Exception e) {
			return INDETERMINATE;
		}
	}

	@Override
	public QualifiedName[] getSupportedOptions() {
		return new QualifiedName[0];
	}
}
