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

/**
 * This class is responsible for handling any pre or post
 * search processing events, including query manipulation
 * and output to the search frame.
 *
 * @since 3.6
 *
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
	 * @return <code>SearchProcessorInfo</code>, or <code>null</code> for no changes.
	 */
	public abstract SearchProcessorInfo preSearch(String query);

	/**
	 * This method is called after the search is performed.
	 *
	 * Results are stored as an array of ISearchResult containing
	 * all available data.
	 *
	 * This method can be used to return a modified result set.  For example, one can change the
	 * result score of an item, add new results to the top of the list, or remove results.
	 *
	 * @return <code>{@link ISearchResult}[]</code>, or <code>null</code> for no changes.
	 */
	public abstract ISearchResult[] postSearch(String query, ISearchResult[] results);
}
