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
package org.eclipse.help.internal.search;

import java.util.List;

/**
 * Search hit collector. The search engine adds hits to it.
 */
public interface ISearchHitCollector {

	/**
	 * Adds hits to the result.
	 *
	 * @param hits the List of raw hits
	 */
	public void addHits(List<SearchHit> hits, String wordsSearched);

	/**
	 * An exception occurred in the search. Implementing subclasses should either
	 * rethrow the exception or save a local copy and test for it later.
	 * @param exception
	 * @throws QueryTooComplexException
	 */
	public void addQTCException(QueryTooComplexException exception) throws QueryTooComplexException;
}

