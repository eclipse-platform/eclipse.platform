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

import org.eclipse.core.runtime.IStatus;

/**
 * A collector for the search hits (asynchronously) returned by the help search
 * participants.
 * <p>
 * This interface is intended to be implemented by clients and passed to the
 * search engine instance.
 *
 * @since 3.1
 */
public interface ISearchEngineResultCollector {
	/**
	 * Accepts a new search result object.
	 *
	 * @param searchResult
	 *            the new search result
	 */
	void accept(ISearchEngineResult searchResult);

	/**
	 * Accepts an array of new search results.
	 *
	 * @param searchResults
	 *            an array of search result objects
	 */
	void accept(ISearchEngineResult[] searchResults);

	/**
	 * Notifies the collector that an error has occured in the search engine.
	 * The kinds of errors that are reported this way are not abnormal problems
	 * or internal errors. Unexpected problems should be left to the job manager
	 * to handle by throwing a <code>CoreException</code>. Use this method to
	 * report errors that are expected to occur from time to time (e.g., server
	 * down, server timeout, incorrect URL etc.).
	 *
	 * @param status
	 *            the reported error status
	 */
	void error(IStatus status);
}
