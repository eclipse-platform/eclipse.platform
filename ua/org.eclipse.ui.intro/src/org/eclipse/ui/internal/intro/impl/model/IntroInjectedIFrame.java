/*******************************************************************************
 * Copyright (c) 2005, 2016 IBM Corporation and others.
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
 * An Intro IFrame element that has been injected at runtime to embed a url. It
 * is placed as the only child of a target div to display the content of a url.
 * The URL can be any valid URL.
 */
public class IntroInjectedIFrame extends AbstractIntroIdElement {

	private String url;

	IntroInjectedIFrame(Element element, Bundle bundle) {
		super(element, bundle);
	}

	public void setIFrameURL(String url) {
		this.url = url;
	}

	public String getIFrameURL() {
		return this.url;
	}


	@Override
	public int getType() {
		return AbstractIntroElement.INJECTED_IFRAME;
	}


}
