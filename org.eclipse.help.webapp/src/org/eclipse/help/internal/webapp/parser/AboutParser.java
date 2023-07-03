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
import org.eclipse.help.internal.webapp.utils.Utils;
import org.eclipse.help.internal.webapp.utils.XMLHelper;
import org.xml.sax.Attributes;

public class AboutParser extends ResultParser {

	private ParseElement element = null;
	private String currentTag;
	private long service;

	public AboutParser(long service) {
		super(JSonHelper.TITLE);
		this.service = service;
		if (this.service == Utils.ABOUT_PLUGIN)
			this.setIdentifier(JSonHelper.PLUGIN_ID);
	}

	@Override
	public void startElement(String uri, String lname, String name, Attributes attrs) {

		currentTag = name;

		Properties properties = new Properties();
		properties.put(JSonHelper.PROPERTY_NAME, name);
		if (!((service == Utils.PREFERENCE
					&& (name.equalsIgnoreCase(XMLHelper.ELEMENT_PREFERENCES)
							|| name.equalsIgnoreCase(XMLHelper.ELEMENT_PLUGIN)))
				|| service == Utils.ABOUT_PLUGIN))
			properties.put(JSonHelper.PROPERTY_VALUE, ""); //$NON-NLS-1$

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
	public void characters(char[] ch, int start, int length) {
		if ((service == Utils.PREFERENCE
				&& (currentTag.equalsIgnoreCase(XMLHelper.ELEMENT_PREFERENCES)
					|| currentTag.equalsIgnoreCase(XMLHelper.ELEMENT_PLUGIN)))
			|| service == Utils.ABOUT_PLUGIN)
			return;

		if (element != null && !currentTag.isEmpty()) {

			Properties properties = element.getProps();
			if (properties != null) {

				String content = new String(ch, start, length);

				String existing = (String) properties.get(currentTag);
				if (existing == null)
					existing = ""; //$NON-NLS-1$

				content = content.replaceAll("[\\n\\t]", "").trim(); //$NON-NLS-1$ //$NON-NLS-2$

				properties.put(JSonHelper.PROPERTY_VALUE, existing + content);
				element.updateParseElement(properties);
			}
		}
	}

	@Override
	public void endElement(String uri, String lname, String name) {

		if (element != null) {
			element = element.getParent();
		}
		currentTag = ""; //$NON-NLS-1$
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
