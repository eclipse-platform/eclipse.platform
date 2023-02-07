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

/**
 * An intro config component that can have a single Text element as a child. In
 * case there is more than one text child, the text is retrieved from the first
 * text child element.
 */
public abstract class AbstractTextElement extends AbstractIntroContainer {

	AbstractTextElement(Element element, Bundle bundle) {
		super(element, bundle);
	}

	/**
	 * Retruns the intro text element embedded in this element.
	 */
	public IntroText getIntroText() {
		AbstractIntroElement[] children = getChildren();
		for (int i=0;i<children.length;++i) {
			if (children[i] instanceof IntroText) {
				return (IntroText)children[i];
			}
		}
		return null;
	}

	/**
	 * @return Returns the text of the child text of this element.
	 */
	public String getText() {
		// intro text may be null if there is no child Text element.
		IntroText text = getIntroText();
		if (text != null) {
			return text.getText();
		}
		return null;
	}

	@Override
	public int getType() {
		return AbstractIntroElement.ABSTRACT_TEXT;
	}
}
