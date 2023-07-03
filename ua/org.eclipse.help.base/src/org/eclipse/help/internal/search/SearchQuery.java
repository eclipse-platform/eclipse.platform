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

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.core.runtime.*;

/**
 * An implementation of ISearchQuery.
 */
public class SearchQuery implements ISearchQuery {
	Collection<String> fieldNames;
	boolean fieldSearch;
	String locale;
	String searchWord;
	public SearchQuery() {
		this("", false, new ArrayList<>(), Platform.getNL()); //$NON-NLS-1$
	}
	public SearchQuery(String searchWord, boolean fieldSearch,
			Collection<String> fieldNames, String locale) {
		this.searchWord = searchWord;
		this.fieldSearch = fieldSearch;
		this.fieldNames = fieldNames;
		this.locale = locale;
	}
	/**
	 * Returns the fieldNames.
	 *
	 * @return Collection
	 */
	@Override
	public Collection<String> getFieldNames() {
		return fieldNames;
	}

	/**
	 * Returns the fieldSearch.
	 *
	 * @return boolean
	 */
	@Override
	public boolean isFieldSearch() {
		return fieldSearch;
	}

	/**
	 * Returns the locale.
	 *
	 * @return String
	 */
	@Override
	public String getLocale() {
		return locale;
	}

	/**
	 * Returns the searchWord.
	 *
	 * @return String
	 */
	@Override
	public String getSearchWord() {
		return searchWord;
	}

	/**
	 * Sets the fieldNames.
	 *
	 * @param fieldNames
	 *            The fieldNames to set
	 */
	public void setFieldNames(Collection<String> fieldNames) {
		this.fieldNames = fieldNames;
	}

	/**
	 * Sets the fieldSearch.
	 *
	 * @param fieldSearch
	 *            The fieldSearch to set
	 */
	public void setFieldSearch(boolean fieldSearch) {
		this.fieldSearch = fieldSearch;
	}

	/**
	 * Sets the locale.
	 *
	 * @param locale
	 *            The locale to set
	 */
	public void setLocale(String locale) {
		this.locale = locale;
	}

	/**
	 * Sets the searchWord.
	 *
	 * @param searchWord
	 *            The searchWord to set
	 */
	public void setSearchWord(String searchWord) {
		this.searchWord = searchWord;
	}

}
