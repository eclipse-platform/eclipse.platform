/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
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
package org.eclipse.ui.intro.config;

import org.w3c.dom.Element;

/**
 * A content provider for dynamic XHTML Intro content. When an XHTML intro page
 * is parsed and a contentProvider element is detected, it is used to create
 * dynamic XHTML content in the page.
 *
 * @since 3.1
 */
public interface IIntroXHTMLContentProvider extends IIntroContentProvider {

	/**
	 * Create XHTML content in the provided parent DOM Element. A typical usage
	 * for this method would be: <br>
	 *
	 * <br>
	 * <code>
	 * &lt;contentProvider id=&quot;contentProviderId&quot;
	 * class=&quot;xx.yy.IntroContentProvider&quot;
	 * pluginId=&quot;xx.yy.id&quot;/&gt; <br></code> <br>
	 *
	 * A parent DOM Element will be passed to allow for adding dynamic content
	 * by manipulating the Java XML DOM for the XHTML file. A div is created
	 * with an id equal to the id specified in the contentProvider element, and
	 * is passed as the parent. In the above example, the DOM element
	 * representing a div with id=myContentProviderDivId would be the parent
	 * passed.
	 *
	 * @param id
	 *            the unique identifier of the content element. The same content
	 *            provider class can be reused for several elements and the id
	 *            can be used to tell them apart.
	 *
	 * @param parent
	 *            the parent xml Element where dynamic content will be added as
	 *            children.
	 *
	 */
	public void createContent(String id, Element parent);



}
