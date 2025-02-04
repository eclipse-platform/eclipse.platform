/*******************************************************************************
 * Copyright (c) 2011, 2015 IBM Corporation and others.
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
 * This class is responsible for handling any pre or post
 * search processing events, including query manipulation
 * and output to the search frame.
 *
 * @since 3.6
 */
public abstract class AbstractSearchProcessor {

	public AbstractSearchProcessor()
	{

	}

	/**
	 * This method is called before the search is performed.
	 *
	 * See {@link SearchProcessorInfo} for types of information that can be used by
	 * the search display.
	 *
	 * If a {@link SearchProcessorInfo} with an empty, non-{@code null} query
	 * ({@code ""}) is returned, no search will be executed, resulting in no search
	 * results.
	 *
	 * @return {@link SearchProcessorInfo}, or {@code null} for no changes.
	 */
	public abstract SearchProcessorInfo preSearch(String query);

	/**
	 * This method is called after the search is performed.
	 *
	 * This method can be used to return a modified result set. For example, one can
	 * change the result score of an item, add new results to the top of the list,
	 * or remove results.
	 *
	 * This method exists for backwards compatibility. Overwrite
	 * {@link #postSearch(String, String, ISearchResult[], String, IHelpResource[])}
	 * if more information like the locale, etc. is needed instead.
	 *
	 * @return The modified results, or {@code null} for no changes.
	 */
	public abstract ISearchResult[] postSearch(String query, ISearchResult[] results);

	/**
	 * This method is called after the search is performed.
	 *
	 * This method can be used to return a modified result set. For example, one can
	 * change the result score of an item, add new results to the top of the list,
	 * or remove results.
	 *
	 * @param query         The actually query that was executed (after any changes
	 *                      made by {@link #preSearch(String)}).
	 * @param originalQuery The original query before any changes made by
	 *                      {@link #preSearch(String)}.
	 * @param results       The results of the executed query.
	 * @param results       The locale.
	 * @param results       The set scopes (might be {@code null}).
	 *
	 * @return The modified results, or {@code null} for no changes.
	 *
	 * @see #postSearch(String, ISearchResult[])
	 *
	 * @since 4.5
	 */
	public ISearchResult[] postSearch(String query, String originalQuery, ISearchResult[] results, String locale,
			IHelpResource[] scopes) {
		return postSearch(query, results);
	}
}
