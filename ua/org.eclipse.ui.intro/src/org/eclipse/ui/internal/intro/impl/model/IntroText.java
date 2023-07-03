/*******************************************************************************
 * Copyright (c) 2004, 2016 IBM Corporation and others.
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

package org.eclipse.ui.internal.intro.impl.model;

import org.osgi.framework.Bundle;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * An intro text element.
 */
public class IntroText extends AbstractBaseIntroElement {

	protected static final String TAG_TEXT = "text"; //$NON-NLS-1$

	private String text;
	/**
	 * boolean flag which is true if the text element contains CData content.
	 * which means we would have to model it as formatted text.
	 */
	private boolean isFormatted = false;

	IntroText(Element element, Bundle bundle) {
		super(element, bundle);
		Node textNode = element.getFirstChild();
		if (textNode == null)
			return;
		if (textNode.getNodeType() == Node.TEXT_NODE
				|| textNode.getNodeType() == Node.CDATA_SECTION_NODE) {
			// we may have a text or a CDATA nodes.
			text = textNode.getNodeValue();
			isFormatted = checkIfFormatted();
		}
	}

	/**
	 * @return Returns the text description.
	 */
	public String getText() {
		IntroModelRoot root = getModelRoot();
		if (root!=null)
			return root.resolveVariables(text);
		return text;
	}

	@Override
	public int getType() {
		return AbstractIntroElement.TEXT;
	}

	/**
	 * @return true if the content of this text element has any "&lt;" which makes
	 *         it formatted.
	 */
	public boolean checkIfFormatted() {
		if (text == null)
			return false;
		int i = text.indexOf('<');
		return i == -1 ? false : true;
	}


	/**
	 * @return Returns the isFormatted.
	 */
	public boolean isFormatted() {
		return isFormatted;
	}
}
