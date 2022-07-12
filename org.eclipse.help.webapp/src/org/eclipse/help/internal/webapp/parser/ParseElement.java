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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;

import org.eclipse.help.internal.webapp.utils.JSonHelper;

public class ParseElement {

	private Properties props;
	private ArrayList<ParseElement> children = new ArrayList<>();
	private ParseElement parent;

	public ParseElement(Properties props, ParseElement parent) {
		this.props = props;
		this.parent = parent;
	}

	public ParseElement(Properties props) {
		this(props, null);
	}

	public void updateParseElement(Properties props) {
		this.props = props;
	}

	public Properties getProps() {
		return props;
	}

	public ParseElement getParent() {
		return parent;
	}

	public String getProperty(String key) {
		return (props != null) ? props.getProperty(key) : ""; //$NON-NLS-1$
	}

	@Override
	public String toString() {
		return (props != null) ? props.toString() : ""; //$NON-NLS-1$
	}

	public void addChild(ParseElement elem) {
		children.add(elem);
	}

	public int getChildrenCount() {
		return children.size();
	}

	public String toJSON(int level) {

		StringBuilder buff = new StringBuilder();

		String space = JSonHelper.SPACE;
		for (int s = 0; s < level; s++) {
			space += JSonHelper.SPACE;
		}

		buff.append(JSonHelper.NEWLINE + space);
		buff.append(JSonHelper.BEGIN_BRACE);

		if (props != null) {
			Enumeration<?> enumObj = props.keys();
			while (enumObj.hasMoreElements()) {

				String key = (String) enumObj.nextElement();
				String val = props.getProperty(key);

				buff.append(JSonHelper.NEWLINE + space + JSonHelper.SPACE);
				buff.append(JSonHelper.getQuotes(key));
				buff.append(JSonHelper.COLON);
				val = URLEncoder.encode(val, StandardCharsets.UTF_8);
				buff.append(JSonHelper.getQuotes(val));
				buff.append(JSonHelper.COMMA);
			}
		}

		if (children.isEmpty()) {
			int len = buff.length();
			char ch = buff.charAt(len - 1);
			if (ch == ',') {
				buff.deleteCharAt(len - 1);
				buff.append(JSonHelper.NEWLINE + space);
			}

		} else {

			buff.append(JSonHelper.NEWLINE + space + JSonHelper.SPACE);
			buff.append(JSonHelper.getQuotes(JSonHelper.CHILDREN));
			buff.append(JSonHelper.COLON);
			buff.append(JSonHelper.BEGIN_BRACKET);

			for (int i = 0; i < children.size(); i++) {

				if (i > 0)
					buff.append(JSonHelper.COMMA);

				ParseElement element = children.get(i);
				buff.append(element.toJSON(level + 2));
			}

			buff.append(JSonHelper.NEWLINE + space + JSonHelper.SPACE);

			buff.append(JSonHelper.END_BRACKET);
			buff.append(JSonHelper.NEWLINE + space);
		}

		buff.append(JSonHelper.END_BRACE);

		return buff.toString();
	}

	public String toJSON() {

		StringBuilder buff = new StringBuilder();

		if (props != null) {
			Enumeration<?> enumObj = props.keys();
			while (enumObj.hasMoreElements()) {

				String key = (String) enumObj.nextElement();
				String val = props.getProperty(key);

				buff.append(JSonHelper.NEWLINE + JSonHelper.SPACE);
				buff.append(JSonHelper.getQuotes(key));
				buff.append(JSonHelper.COLON);
				val = URLEncoder.encode(val, StandardCharsets.UTF_8);
				buff.append(JSonHelper.getQuotes(val));
				buff.append(JSonHelper.COMMA);
			}
		}

		if (children.isEmpty()) {
			int len = buff.length();
			char ch = buff.charAt(len - 1);
			if (ch == ',') {
				buff.deleteCharAt(len - 1);
				buff.append(JSonHelper.NEWLINE);
			}

		} else {

			buff.append(JSonHelper.NEWLINE + JSonHelper.SPACE);
			buff.append(JSonHelper.getQuotes(JSonHelper.ITEMS));
			buff.append(JSonHelper.COLON);
			buff.append(JSonHelper.BEGIN_BRACKET);

			for (int i = 0; i < children.size(); i++) {

				if (i > 0)
					buff.append(JSonHelper.COMMA);

				ParseElement child = children.get(i);
				buff.append(child.toJSON(1));
			}

			buff.append(JSonHelper.NEWLINE + JSonHelper.SPACE);

			buff.append(JSonHelper.END_BRACKET);
			buff.append(JSonHelper.NEWLINE);
		}

		return buff.toString();
	}
}
