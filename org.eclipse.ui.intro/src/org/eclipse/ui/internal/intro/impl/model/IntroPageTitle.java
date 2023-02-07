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
 * An intro element that represents a page title.
 */
public class IntroPageTitle extends IntroText {

	protected static final String TAG_TITLE = "title"; //$NON-NLS-1$

	IntroPageTitle(Element element, Bundle bundle) {
		super(element, bundle);
	}

	/**
	 * @return Returns the Title text.
	 */
	public String getTitle() {
		return getText();
	}

	@Override
	public int getType() {
		return AbstractIntroElement.PAGE_TITLE;
	}

}
