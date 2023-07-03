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
 * An Intro page.
 */
public class IntroPage extends AbstractIntroPage {

	/**
	 * An Intro Part page.
	 */
	IntroPage(Element element, Bundle bundle, String base) {
		super(element, bundle, base);
	}

	@Override
	public int getType() {
		return AbstractIntroElement.PAGE;
	}

}
