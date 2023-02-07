/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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

import org.eclipse.help.internal.UAElement;
import org.eclipse.help.internal.dynamic.ProcessorHandler;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/*
 * Converts the charset in XHTML meta tag to UTF-8. This is the encoding
 * output by the XMLProcessor, and we need the charset in the meta tags
 * to match, otherwise browsers will be confused.
 * Also ensure that all <script> and <div> elements have a child,
 */
public class XHTMLCharsetHandler extends ProcessorHandler {

	private static final String ELEMENT_META = "meta"; //$NON-NLS-1$
	private static final String ATTRIBUTE_CONTENT = "content"; //$NON-NLS-1$
	private static final String PREFIX_CHARSET = "text/html; charset="; //$NON-NLS-1$
	private static final String ENCODING_UTF8 = "UTF-8"; //$NON-NLS-1$

	@Override
	public short handle(UAElement element, String id) {
		if (ELEMENT_META.equals(element.getElementName())) {
			String content = element.getAttribute(ATTRIBUTE_CONTENT);
			if (content != null && content.startsWith(PREFIX_CHARSET)) {
				element.setAttribute(ATTRIBUTE_CONTENT, PREFIX_CHARSET + ENCODING_UTF8);
				return HANDLED_CONTINUE;
			}
		}
		if (endTagRequired(element)) {
			Element domElement = element.getElement();
			if (domElement.getFirstChild() == null) {
				Document document = domElement.getOwnerDocument();
				Comment child = document.createComment(" "); //$NON-NLS-1$
				domElement.appendChild(child);
			}
		}
		return UNHANDLED;
	}

	/*
	 * Returns true if this element requires the end tag to be separate from the
	 * start tag to render correctly in browsers.
	 * i.e. generate <a></a> rather than <a/>
	 */
	private boolean endTagRequired(UAElement element) {
		String elementName = element.getElementName();
		if ("a".equalsIgnoreCase(elementName)) return true; //$NON-NLS-1$
		if ("p".equalsIgnoreCase(elementName)) return true; //$NON-NLS-1$
		if ("div".equalsIgnoreCase(elementName)) return true; //$NON-NLS-1$
		if ("script".equalsIgnoreCase(elementName)) return true; //$NON-NLS-1$
		if ("textarea".equalsIgnoreCase(elementName)) return true; //$NON-NLS-1$
		if ("iframe".equalsIgnoreCase(elementName)) return true; //$NON-NLS-1$
		if ("title".equalsIgnoreCase(elementName)) return true; //$NON-NLS-1$
		return false;
	}
}
