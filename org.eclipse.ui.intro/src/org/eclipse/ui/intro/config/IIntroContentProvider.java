/*******************************************************************************
 * Copyright (c) 2004, 2007 IBM Corporation and others.
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

import java.io.PrintWriter;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * A content provider for dynamic intro content. It is initialized with the
 * content provider site because a typical content provider would need to update
 * its contents dynamically at runtime. And so, the site can be informed of a
 * need to redraw its content through a call to its reflow(..) method.
 * <p>
 * The life cycle of an IIntroContentProvider is as follows:</p>
 * <ul>
 * <li>a content provider is defined in the Intro content markup file (ie:
 * introContent.xml file) as follows:
 *
 * <p>
 * &lt;contentProvider id=&quot;roles&quot;
 * class=&quot;x.y.z.ContentProvider&quot;&gt; <br>
 * &lt;text&gt;Some alternate text for dynamic content&lt;/text&gt; <br>
 * &lt;/contentProvider&gt;
 * </p>
 *
 * This defines the content provider as part of the intro content for that page.
 * </li>
 * <li>the content provider class is created when the page that contains this
 * provider is loaded. It is only created once in the life cycle of an open
 * Intro view, ie: the class instance is cached and only disposed of when the
 * intro view is closed.
 * <li>init() is called to initialize the instance of the class on load of the
 * page. This is where the provider site can be cached for later reuse.</li>
 * <li>createContent() is then called to give the content provider a chance to
 * generate the dynamic content. This is when the dynamic content can be cached
 * for later reuse when the same page is shown again.</li>
 * <li>it is important to note that there is a difference between when the
 * createContent() is called in the case of SWT and HTML presentations. The HTML
 * presentation is dynamic html generated on the fly each time an Intro page
 * needs to be displayed. SWT presentation on the other hand, is based on SWT
 * forms widgets, and for performance, each page is only created once and cached
 * in a pageBook for re-use. With this in mind, createContent() is called every
 * time a page is shown in the case of HTML presentation. Whereas it is only
 * called once in the case of the SWT presentation. This is why createContent()
 * should not be used as a means of refreshing content. Content should only be
 * created once during this method call, and cached for later re-use. If a
 * refresh is needed, then reflow() should be called on the contentProviderSite
 * when the appropriate event happens.
 * <li>finally, when the intro view is closed, dispose will be called on the
 * content provider to give it a chance to dispose of any resources.</li></ul>
 *
 * @since 3.0.1
 */
public interface IIntroContentProvider {
	/**
	 * Initializes the content provider. An IIntroContentProviderSite is passed,
	 * which will be called on to recompute or layout the content when the
	 * content becomes stale.
	 *
	 * @param site
	 *            the site of this IIntroContentProvider
	 */
	public void init(IIntroContentProviderSite site);


	/**
	 * Creates HTML content in the provided PrintWriter. This content will be
	 * included in the generated HTML page when embedded HTML widget is used to
	 * render intro content.
	 *
	 * @param id
	 *            the unique identifier of the content element. The same content
	 *            provider class can be reused for several elements and the id
	 *            can be used to tell them apart.
	 * @param out
	 *            the output print writer to generate HTML content into
	 */
	public void createContent(String id, PrintWriter out);

	/**
	 * Creates SWT content in the provided Composite. This method is called when
	 * Eclipse Forms are used to render intro content.
	 *
	 * @param id
	 *            the unique identifier of the content element
	 * @param parent
	 *            the parent composite that should be used when creating SWT
	 *            widgets
	 * @param toolkit
	 *            the form toolkit that should be used when creating new widgets
	 */
	public void createContent(String id, Composite parent, FormToolkit toolkit);


	/**
	 * Dispose of the ContentProvider. This will only be called when the Intro
	 * view is closed. In other words, the content provider will not be disposed
	 * of until the last possible minute. This gives the implementor the chance
	 * to cache content and avoid regenerating content on every page switch.
	 */
	public void dispose();

}
