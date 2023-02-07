/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.help.search;

import org.eclipse.help.IHelpResource;

/**
 * A search result created by the help search engine. Engines that have direct
 * access to the search servers are expected to provide one search result object
 * per each hit. Engines that can only compose a search query that returns a
 * document with the results must create one search result object whose URL is
 * the actual query. This URL will then be presented in the Web browser.
 * <p>
 * This interface is inteded to be implemented by clients.
 *
 * @since 3.1
 */
public interface ISearchEngineResult {
	/**
	 * Returns the label of the search result to use in the UI.
	 *
	 * @return the search result label
	 */
	String getLabel();

	/**
	 * Returns the short description of the search result. If coming from an
	 * individual search hit, this description is typically composed of the
	 * document fragment in the vicinity of the searched expression, or a
	 * combination of several document fragments. Other search engines may
	 * return a couple of sentencies at the beninning of the document.
	 *
	 * @return a short description, or <code>null</code> if not available.
	 */
	String getDescription();

	/**
	 * Returns a category this search result belongs to. Engines can use this
	 * object to denote the origin of the hit in the search domain. The usage of
	 * <code>IHelpResource</code> allows search result UI to create a link to
	 * the category.
	 *
	 * @return a hit category or <code>null</code> if not available.
	 */
	IHelpResource getCategory();

	/**
	 * Returns the URL (as a string) associated with this help resource.
	 *
	 * @return the URL (as a string) associated with the resource
	 *         <p>
	 *         Valid URL of a help resource is:
	 *         </p>
	 *         <ul>
	 *         <li>a <em>/pluginID/path/to/resource</em>, where
	 *         <ul>
	 *         <li><em>pluginID</em> is the unique identifier of the plugin
	 *         containing the help resource,
	 *         <li><em>path/to/document</em> is the help resource path, relative
	 *         to the plugin directory.
	 *         </ul>
	 *         For example. <em>/myplugin/mytoc.xml</em> or
	 *         <em>/myplugin/references/myclass.html</em> are vaild.
	 *         <li>string representation of URI to an external document. In this
	 *         case, all special characters have to be enoded such that the URI
	 *         is appropriate to be opened with a web browser.
	 *         <em>http://eclipse.org/documents/my%20file.html</em> and
	 *         <em>jar:file:/c:/my%20sources/src.zip!/mypackage/MyClass.html</em>
	 *         are examples of valid URIs.
	 *         </ul>
	 */
	String getHref();

	/**
	 * Returns a float number in the range between 0 and 1 that can be used to
	 * sort the hits by relevance (1 being the perfect result). The number can
	 * be interpreted as the probability of a match in the given topic.
	 *
	 * @return the score of this hit between 0.0 and 1.0
	 */
	float getScore();

	/**
	 * Tests if the result link must be shown in an external web browser/Help
	 * window, or it can also be shown embedded. Contributors should force
	 * external window only if the link points at a web page that cannot be
	 * displayed in a narrow view without distorsion. Text-only document
	 * typically reflows well to fit the narrow view. Documents with tables
	 * and/or images normally do not fit without showing the horizontal scroll
	 * bar.
	 * <p>
	 * Note that returning <code>false</code> does not prevent the link to be
	 * opened in the external window. Instead, it indicates that the link should
	 * be opened using the current user preferences, which may always force
	 * external window for URL rendering.
	 *
	 * @return <code>true</code> if external window must be used,
	 *         <code>false</code> if the link opening mode can be chosen by
	 *         the help system.
	 */
	boolean getForceExternalWindow();

	/**
	 * Converts a relative href into absolute according to the search engine
	 * base URL.
	 *
	 * @param href
	 *            the relative href to convert into absolute
	 * @param frames
	 *            if <code>true</code>, the resulting href can contain frames
	 *            in a format suitable for presentation in a standalone web
	 *            browser. Otherwise, the resulting href should be without
	 *            frames and suitable for presentation in the embedded web
	 *            browser inside the workbench.
	 * @return the absolute href that can be used to navigate to the reference
	 *         as-is
	 */
	String toAbsoluteHref(String href, boolean frames);
}
