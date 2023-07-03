/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others.
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

import org.eclipse.core.runtime.IConfigurationElement;
import org.osgi.framework.Bundle;
import org.w3c.dom.Element;

/**
 * An Intro Config component that has an id attribute. It is used as a base
 * class for all config elements that can take an id, and hence are valid
 * targets for includes and finds.
 *
 */
public abstract class AbstractIntroIdElement extends AbstractIntroElement {

	public static final String ATT_ID = "id"; //$NON-NLS-1$

	protected String id;

	AbstractIntroIdElement(IConfigurationElement element) {
		super(element);
		id = element.getAttribute(ATT_ID);
	}

	AbstractIntroIdElement(Element element, Bundle bundle) {
		super(element, bundle);
		id = getAttribute(element, ATT_ID);
	}

	AbstractIntroIdElement(Element element, Bundle bundle, String base) {
		super(element, bundle, base);
		id = getAttribute(element, ATT_ID);
	}

	/**
	 * @return Returns the id.
	 */
	public String getId() {
		return id;
	}
}
