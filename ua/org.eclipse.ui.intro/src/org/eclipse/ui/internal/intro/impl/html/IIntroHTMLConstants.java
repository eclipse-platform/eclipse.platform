/*******************************************************************************
 * Copyright (c) 2004, 2009 IBM Corporation and others.
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
package org.eclipse.ui.internal.intro.impl.html;

public interface IIntroHTMLConstants {

	/* Constants required for creating html element tags */
	String LT = "<"; //$NON-NLS-1$
	String GT = ">"; //$NON-NLS-1$
	String FORWARD_SLASH = "/"; //$NON-NLS-1$
	String QUOTE = "\""; //$NON-NLS-1$
	String EQUALS = "="; //$NON-NLS-1$
	String HYPHEN = "-"; //$NON-NLS-1$
	String SPACE = " "; //$NON-NLS-1$
	String NEW_LINE = "\n"; //$NON-NLS-1$
	String TAB = "\t"; //$NON-NLS-1$
	String SMALL_TAB = "    "; //$NON-NLS-1$

	/* HTML element names */
	String ELEMENT_HTML = "HTML"; //$NON-NLS-1$
	String ELEMENT_HEAD = "HEAD"; //$NON-NLS-1$
	String ELEMENT_BASE = "BASE"; //$NON-NLS-1$
	String ELEMENT_BODY = "BODY"; //$NON-NLS-1$
	String ELEMENT_TITLE = "TITLE"; //$NON-NLS-1$
	String ELEMENT_LINK = "LINK"; //$NON-NLS-1$
	String ELEMENT_DIV = "DIV"; //$NON-NLS-1$
	String ELEMENT_SPAN = "SPAN"; //$NON-NLS-1$
	String ELEMENT_ANCHOR = "A"; //$NON-NLS-1$
	String ELEMENT_OBJECT = "OBJECT"; //$NON-NLS-1$
	String ELEMENT_IMG = "IMG"; //$NON-NLS-1$
	String ELEMENT_H1 = "H1"; //$NON-NLS-1$
	String ELEMENT_H2 = "H2"; //$NON-NLS-1$
	String ELEMENT_H3 = "H3"; //$NON-NLS-1$
	String ELEMENT_H4 = "H4"; //$NON-NLS-1$
	String ELEMENT_HR = "HR"; //$NON-NLS-1$
	String ELEMENT_PARAGRAPH = "P"; //$NON-NLS-1$
	String ELEMENT_STYLE = "STYLE"; //$NON-NLS-1$
	String ELEMENT_TABLE = "TABLE"; //$NON-NLS-1$
	String ELEMENT_TR = "TR"; //$NON-NLS-1$
	String ELEMENT_TD = "TD"; //$NON-NLS-1$
	String ELEMENT_IFrame = "iFrame"; //$NON-NLS-1$
	String ELEMENT_META = "meta"; //$NON-NLS-1$

	/* HTML attribute names */
	String ATTRIBUTE_ID = "id"; //$NON-NLS-1$
	String ATTRIBUTE_CLASS = "class"; //$NON-NLS-1$
	String ATTRIBUTE_STYLE = "style"; //$NON-NLS-1$
	String ATTRIBUTE_HREF = "href"; //$NON-NLS-1$
	String ATTRIBUTE_RELATIONSHIP = "rel"; //$NON-NLS-1$
	String ATTRIBUTE_SRC = "src"; //$NON-NLS-1$
	String ATTRIBUTE_TYPE = "type"; //$NON-NLS-1$
	String ATTRIBUTE_DATA = "data"; //$NON-NLS-1$
	String ATTRIBUTE_ALT = "alt"; //$NON-NLS-1$
	String ATTRIBUTE_TITLE = "title"; //$NON-NLS-1$
	String ATTRIBUTE_FRAMEBORDER = "frameborder"; //$NON-NLS-1$
	String ATTRIBUTE_SCROLLING = "scrolling"; //$NON-NLS-1$
	String ATTRIBUTE_CONTENT = "content"; //$NON-NLS-1$
	String ATTRIBUTE_HTTP_EQUIV = "http-equiv"; //$NON-NLS-1$

	/* HTML attribute values */
	String LINK_REL = "stylesheet"; //$NON-NLS-1$
	String LINK_STYLE = "text/css"; //$NON-NLS-1$
	String OBJECT_TYPE = "text/html"; //$NON-NLS-1$
	String TYPE_HTML_UTF_8 = "text/html; charset=UTF-8"; //$NON-NLS-1$
	String CONTENT_TYPE = "Content-Type"; //$NON-NLS-1$

	String DIV_ID_PAGE = "page"; //$NON-NLS-1$
	String DIV_CLASS_INLINE_HTML = "inline-html"; //$NON-NLS-1$
	String DIV_CLASS_PROVIDED_CONTENT = "provided-content"; //$NON-NLS-1$

	String ANCHOR_CLASS_LINK = "link"; //$NON-NLS-1$
	String IMAGE_SRC_BLANK = "icons/blank.gif"; //$NON-NLS-1$
	String IMAGE_CLASS_BG = "background-image"; //$NON-NLS-1$
	String LINK_EXTRA_DIV = "link-extra-div"; //$NON-NLS-1$
	String SPAN_CLASS_DIV_LABEL = "div-label"; //$NON-NLS-1$
	String SPAN_CLASS_LINK_LABEL = "link-label"; //$NON-NLS-1$
	String SPAN_CLASS_TEXT = "text"; //$NON-NLS-1$

	/* HTML style */
	String STYLE_HTML = "HTML, BODY, IMG { border: 0px; }"; //$NON-NLS-1$
}
