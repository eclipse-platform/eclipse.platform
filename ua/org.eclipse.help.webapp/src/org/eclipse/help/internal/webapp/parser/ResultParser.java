/*******************************************************************************
 * Copyright (c) 2011, 2017 IBM Corporation and others.
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
package org.eclipse.help.internal.webapp.parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import org.eclipse.help.internal.webapp.utils.JSonHelper;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ResultParser extends DefaultHandler {

	protected String id;
	protected String label;
	protected ArrayList<ParseElement> items = new ArrayList<>(); //parser populates the items arrayList with parsed data.

	public ResultParser(String label) {
		this(label, JSonHelper.ID);
	}

	public ResultParser(String label, String id) {
		this.label = label;
		this.id = id;
	}

	public void parse(InputStream in) throws ParserConfigurationException, SAXException, IOException {
		@SuppressWarnings("restriction")
		SAXParser parser = org.eclipse.core.internal.runtime.XmlProcessorFactory.createSAXParserWithErrorOnDOCTYPE();
		parser.parse(in, this);
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public void setIdentifier(String id) {
		this.id = id;
	}

	public ArrayList<ParseElement> getItems()
	{
		return items;
	}

	@Override
	public String toString()
	{
		return items.toString();
	}

	public String toJSON() {

		StringBuilder buf = new StringBuilder();

		buf.append(JSonHelper.BEGIN_BRACE);
		buf.append(JSonHelper.NEWLINE + JSonHelper.SPACE);

		buf.append(JSonHelper.getQuotes(JSonHelper.IDENTIFIER));
		buf.append(JSonHelper.COLON);
		buf.append(JSonHelper.getQuotes(id));
		buf.append(JSonHelper.COMMA);

		buf.append(JSonHelper.NEWLINE + JSonHelper.SPACE);
		buf.append(JSonHelper.getQuotes(JSonHelper.LABEL));
		buf.append(JSonHelper.COLON);
		buf.append(JSonHelper.getQuotes(label));
		buf.append(JSonHelper.COMMA);

		buf.append(JSonHelper.NEWLINE + JSonHelper.SPACE);
		buf.append(JSonHelper.getQuotes(JSonHelper.ITEMS));
		buf.append(JSonHelper.COLON);
		buf.append(JSonHelper.BEGIN_BRACKET);

		for (int i = 0; i < items.size(); i++) {

			if (i > 0)
				buf.append(JSonHelper.COMMA);

			ParseElement element = items.get(i);
			buf.append(element.toJSON(1));
		}

		if (!items.isEmpty())
			buf.append(JSonHelper.NEWLINE + JSonHelper.SPACE);

		buf.append(JSonHelper.END_BRACKET);
		buf.append(JSonHelper.NEWLINE);
		buf.append(JSonHelper.END_BRACE);

		return buf.toString();
	}
}
