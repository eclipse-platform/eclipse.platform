/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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

package org.eclipse.ua.tests.help.util;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;

import org.eclipse.ui.internal.cheatsheets.CheatSheetPlugin;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class DocumentCreator {

	public static Document createDocument(String input) throws SAXException, IOException {
		StringReader reader = new StringReader(input);
		InputSource source = new InputSource(reader);

		DocumentBuilder documentBuilder = CheatSheetPlugin.getPlugin()
					.getDocumentBuilder();

		return documentBuilder.parse(source);
	}

}
