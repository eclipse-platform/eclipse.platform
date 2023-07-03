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
 *     Sopot Cela - Bug 466829
 *******************************************************************************/
package org.eclipse.help.internal.search;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
/**
 * Represents a quoted token in user search query words
 */
public class QueryWordsExactPhrase extends QueryWordsToken {
	private List<String> words;
	public QueryWordsExactPhrase() {
		super(QueryWordsToken.EXACT_PHRASE, ""); //$NON-NLS-1$
		words = new ArrayList<>();
	}
	public void addWord(String word) {
		words.add(word);
		if (words.size() <= 1)
			value = word;
		else
			value += " " + word; //$NON-NLS-1$
	}
	public List<String> getWords() {
		return words;
	}
	/**
	 * Creates a lucene query for a field
	 */
	@Override
	public Query createLuceneQuery(String field, float boost) {
		PhraseQuery.Builder qBuilder = new PhraseQuery.Builder();
		BoostQuery boostQuery = null;
		for (String word : getWords()) {
			Term t = new Term("exact_" + field, word); //$NON-NLS-1$
			qBuilder.add(t);
			boostQuery = new BoostQuery(qBuilder.build(), boost);
		}
		return boostQuery;
	}
}
