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

import java.util.Properties;

import org.eclipse.help.internal.webapp.utils.JSonHelper;
import org.xml.sax.Attributes;

public class NavParser extends ResultParser {

	private ParseElement element = null;

	public NavParser() {
		super(JSonHelper.TITLE);
	}

	@Override
	public void startElement(String uri, String lname, String name, Attributes attrs) {

		Properties properties = new Properties();
		properties.put(JSonHelper.PROPERTY_NAME, name);

		for (int i = 0; i < attrs.getLength(); i++) {
			String qname = attrs.getQName(i);
			String val = attrs.getValue(i);
			properties.put(qname, val);
		}

		ParseElement elem = new ParseElement(properties, element);
		if (element != null)
			element.addChild(elem);
		else
			items.add(elem);

		element = elem;
	}

	@Override
	public void endElement(String uri, String lname, String name) {

		if (element != null) {
			element = element.getParent();
		}
	}

	@Override
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

		ParseElement elem = items.get(0);
		if (elem != null)
			buf.append(elem.toJSON());

		buf.append(JSonHelper.END_BRACE);

		return buf.toString();
	}

}
