/*******************************************************************************
 * Copyright (c) 2025 Holger Voormann and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.ua.tests.help.search;

import org.eclipse.help.IHelpResource;
import org.eclipse.help.search.AbstractSearchProcessor;
import org.eclipse.help.search.ISearchResult;
import org.eclipse.help.search.SearchProcessorInfo;

public class WrappedSearchProcessor extends AbstractSearchProcessor {

	public static final String QUERY = "SearchProcessorTest";

	private static volatile AbstractSearchProcessor wrapped = null;

	public static synchronized AutoCloseable set(AbstractSearchProcessor searchProcessor) {
		wrapped = searchProcessor;
		return () -> wrapped = null;
	}

	private static boolean isNotSearchProcessorTest(String query) {
		return wrapped == null || (query != null && !query.contains(QUERY));
	}

	@Override
	public SearchProcessorInfo preSearch(String query) {
		if (isNotSearchProcessorTest(query))
			return null;
		synchronized (WrappedSearchProcessor.class) {
			if (isNotSearchProcessorTest(query))
				return null;
			return wrapped.preSearch(query);
		}
	}

	@Override
	public ISearchResult[] postSearch(String query, ISearchResult[] results) {
		if (wrapped == null)
			return null;
		synchronized (WrappedSearchProcessor.class) {
			if (wrapped == null)
				return null;
			return wrapped.postSearch(query, results);
		}
	}

	@Override
	public ISearchResult[] postSearch(String query, String originalQuery, ISearchResult[] results, String locale,
			IHelpResource[] scopes) {
		if (isNotSearchProcessorTest(originalQuery))
			return null;
		synchronized (WrappedSearchProcessor.class) {
			if (isNotSearchProcessorTest(originalQuery))
				return null;
			return wrapped.postSearch(query, originalQuery, results, locale, scopes);
		}
	}

}
