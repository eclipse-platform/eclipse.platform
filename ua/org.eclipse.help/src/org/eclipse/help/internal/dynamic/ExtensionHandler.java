/*******************************************************************************
 * Copyright (c) 2006, 2016 IBM Corporation and others.
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
package org.eclipse.help.internal.dynamic;

import org.eclipse.help.IContentExtension;
import org.eclipse.help.internal.Anchor;
import org.eclipse.help.internal.UAElement;
import org.eclipse.help.internal.UAElementFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/*
 * The handler responsible for processing content extensions (contributions
 * into anchors and element replacements).
 */
public class ExtensionHandler extends ProcessorHandler {

	private static final String ATTRIBUTE_ID = "id"; //$NON-NLS-1$

	private ExtensionResolver resolver;
	private DocumentReader reader;
	private String locale;

	/*
	 * This handler must know the locale since it's pulling content
	 * in from other documents.
	 */
	public ExtensionHandler(DocumentReader reader, String locale) {
		this.reader = reader;
		this.locale = locale;
	}

	@Override
	public short handle(UAElement element, String path) {
		if (element instanceof Anchor) {
			return handleExtension(element, path, IContentExtension.CONTRIBUTION);
		}
		if (element.getAttribute(ATTRIBUTE_ID) != null) {
			return handleExtension(element, path, IContentExtension.REPLACEMENT);
		}
		return UNHANDLED;
	}

	/*
	 * Handle the given extension-related node. It is either an anchor
	 * or an element with an id that could potentially be replaced.
	 */
	private short handleExtension(UAElement uaElement, String path, int type) {
		String id = uaElement.getAttribute(ATTRIBUTE_ID);
		if (id != null && id.length() > 0) {
			if (resolver == null) {
				resolver = new ExtensionResolver(getProcessor(), reader, locale);
			}
			// get the nodes to insert/replace with
			Node[] nodes = resolver.resolveExtension(path + '#' + id, type);
			if (nodes != null && nodes.length > 0) {
				Element domElement = uaElement.getElement();
				UAElement parent = uaElement.getParentElement();
				for (int i=0;i<nodes.length;++i) {
					if (nodes[i].getNodeType() == Node.ELEMENT_NODE) {
						// ensure elements are typed
						parent.insertBefore(UAElementFactory.newElement((Element)nodes[i]), uaElement);
					}
					else {
						// text nodes are not typed
						Node node = domElement.getOwnerDocument().importNode(nodes[i], true);
						parent.getElement().insertBefore(node, domElement);
					}
				}
				parent.removeChild(uaElement);
				return HANDLED_SKIP;
			}
		}
		// always remove anchors, even invalid ones
		if (type == IContentExtension.CONTRIBUTION) {
			uaElement.getParentElement().removeChild(uaElement);
			return HANDLED_SKIP;
		}
		// it was an element with id, but no replacements
		return UNHANDLED;
	}
}
