/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
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
import org.eclipse.help.internal.webapp.utils.XMLHelper;
import org.xml.sax.Attributes;

public class SearchParser extends ResultParser {

//	private ParseElement element;
	private Properties properties;
	private String currentTag;

	public SearchParser() {
		super(JSonHelper.LABEL);
	}

	@Override
	public void startElement(String uri,
			String lname, String name, Attributes attrs) {

		currentTag = name;
		if (name.equalsIgnoreCase(XMLHelper.ELEMENT_HIT)) {

			properties = new Properties();
			properties.put(JSonHelper.PROPERTY_NAME, name);
			for (int i = 0; i < attrs.getLength(); i++) {
				String qname = attrs.getQName(i);
				String val = attrs.getValue(i);
				properties.put(qname, val);
			}

			String id = "" + items.size(); //$NON-NLS-1$
			properties.put(JSonHelper.ID, id);

		} else if (name.equalsIgnoreCase(XMLHelper.ELEMENT_CATEGORY)) {
			for (int i = 0; i < attrs.getLength(); i++) {
				String qname = attrs.getQName(i);
				String val = attrs.getValue(i);
				if (qname.equalsIgnoreCase(XMLHelper.ATTR_HREF))
					qname = XMLHelper.CATEGORY_HREF;
				properties.put(qname, val);
			}
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) {

		if (currentTag.equalsIgnoreCase(XMLHelper.ELEMENT_HIT)
				|| currentTag.equalsIgnoreCase(XMLHelper.ELEMENT_HITS))
			return;

		if (properties != null)
		{
			String content = new String(ch, start, length);

			String existing = (String) properties.get(currentTag);
			if (existing == null)
				existing = ""; //$NON-NLS-1$

			content = content.replaceAll("[\\n\\t]", "").trim(); //$NON-NLS-1$ //$NON-NLS-2$

			properties.put(currentTag, existing + content);
		}
	}

	@Override
	public void endElement(String uri, String lname, String name) {

		if (name.equalsIgnoreCase(XMLHelper.ELEMENT_HIT)) {
			ParseElement element = new ParseElement(properties);
			items.add(element);
		}
	}
}
