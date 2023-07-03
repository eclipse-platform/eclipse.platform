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

/**
 * An extension of the search engine interface that provides for opening of the results. Engines
 * that return hits that cannot be opened into the web browser should implement this interface and
 * also indicate that the search results should be opened by the engine using
 * {@link ISearchEngineResult2#canOpen()}.
 *
 * @since 3.2
 *
 */

public interface ISearchEngine2 extends ISearchEngine {

	/**
	 * Opens the search result using the engine-specific identifier. When performing the search
	 * operation, search engine is responsible for tagging the results with unique identifiers that
	 * fully reference the results and are not transient. These identifiers can be later used to
	 * open the results by delegating the operation to the engine, or to bookmark the results for
	 * future opening.
	 *
	 * @param id
	 *            The engine-specific identifier provided by the search result
	 * @return <code>true</code> if the operation was successful, or <code>false</code> if the
	 *         help system should try to open the hit using the provided href.
	 */
	boolean open(String id);
}
