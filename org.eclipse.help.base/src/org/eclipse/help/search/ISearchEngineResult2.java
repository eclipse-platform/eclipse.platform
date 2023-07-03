/***************************************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
 *
 * This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors: IBM Corporation - initial API and implementation
 **************************************************************************************************/
package org.eclipse.help.search;

import java.net.URL;

/**
 * An extension of the search result interface that allows engines to define engine result icon for
 * each search result.
 *
 * @since 3.2
 */


public interface ISearchEngineResult2 extends ISearchEngineResult {

	/**
	 * Returns a unique identifier that can be associated with this search result. Search engines
	 * can optionally use this method to pass information on documents that are accessible via
	 * hashtables using a unique identifier. This method is typically used when the search result
	 * can open by itself.
	 *
	 * @see #canOpen()
	 * @see ISearchEngine2#open(String)
	 * @return unique identifier associated with this search result or <code>null</code> if not
	 *         available or not needed.
	 */
	String getId();

	/**
	 * Returns an optional URL of the 16x16 icon to be used to render this search result. If not
	 * provided, the icon for the engine will be used.
	 *
	 * @return the URL of the icon to be used to render this search result or <code>null</code> to
	 *         use the engine icon.
	 */
	URL getIconURL();

	/**
	 * Tests whether this result's open action should be delegated to search engine.
	 *
	 * @return <code>true</code> for engines that must open their results using non-standards
	 *         means, or <code>false</code> for opening the result by the help system using the
	 *         provided href.
	 */
	boolean canOpen();
}
