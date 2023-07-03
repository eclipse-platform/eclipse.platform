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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.help.internal.base.util.ProxyUtil;
import org.eclipse.help.internal.webapp.utils.JSonHelper;
import org.eclipse.help.internal.webapp.utils.XMLHelper;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class TocFragmentParser extends ResultParser {

	private Properties properties;
	private String parentId = ""; //$NON-NLS-1$
	private int tagLevel = -1;
	private int level = 0;

	public TocFragmentParser() {
		super(JSonHelper.TITLE);
	}

	public void parse(URL tocURL, int level)
		throws ParserConfigurationException, SAXException, IOException
	{
		parse(ProxyUtil.getStream(tocURL), level);
	}

	public void parse(InputStream in, int level)
		throws ParserConfigurationException, SAXException, IOException
	{
		this.level = level;
		super.parse(in);
	}

	@Override
	public void startElement(String uri,
			String lname, String name, Attributes attrs) {

		if (name.equalsIgnoreCase(XMLHelper.ELEMENT_NODE)
				|| name.equalsIgnoreCase(XMLHelper.ELEMENT_NUMERIC_PATH)) {

			tagLevel++;

			if (tagLevel == level) {
				properties = new Properties();
				if (name.equalsIgnoreCase(XMLHelper.ELEMENT_NUMERIC_PATH)) {
					setLabel(""); //$NON-NLS-1$
					properties.put(JSonHelper.PROPERTY_NAME, JSonHelper.NUMERIC_PATH);
				}
				else
					properties.put(JSonHelper.PROPERTY_NAME, JSonHelper.TOPIC);

				for (int i = 0; i < attrs.getLength(); i++) {
					String qname = attrs.getQName(i);
					String val = attrs.getValue(i);
					if (qname.equals(XMLHelper.ATTR_ID)) {
						if (parentId.length() > 0)
							val = parentId + '$' + val;
					}
					properties.put(qname, val);
				}
			} else if (parentId.length() <= 0) {
				for (int i = 0; i < attrs.getLength(); i++) {
					if (attrs.getQName(i).equals(XMLHelper.ATTR_ID))
						parentId = attrs.getValue(i);
				}
			}
		}
	}

	@Override
	public void endElement(String uri, String lname, String name) {

		if (name.equalsIgnoreCase(XMLHelper.ELEMENT_NODE)
				|| name.equalsIgnoreCase(XMLHelper.ELEMENT_NUMERIC_PATH))
		{
			if (tagLevel == level && properties != null ) {

				if (name.equalsIgnoreCase(XMLHelper.ELEMENT_NUMERIC_PATH)) {
					properties.setProperty("id", "0"); //$NON-NLS-1$ //$NON-NLS-2$
					properties.setProperty("type", "topic"); //$NON-NLS-1$ //$NON-NLS-2$
				}
				else
					properties.setProperty("type", "toc"); //$NON-NLS-1$ //$NON-NLS-2$

				ParseElement element = new ParseElement(properties);
				items.add(element);

				properties = null;
			}

			tagLevel--;
		}
	}
}
