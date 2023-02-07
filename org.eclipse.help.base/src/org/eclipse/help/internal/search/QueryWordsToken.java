/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
/**
 * Represents a token in user search query words
 */
public class QueryWordsToken {
	public static final int AND = 0;
	public static final int OR = 1;
	public static final int NOT = 2;
	public static final int EXACT_PHRASE = 3;
	public static final int PHRASE = 4;
	public static final int WORD = 5;
	private static final QueryWordsToken fAND = new QueryWordsToken(AND, "AND"); //$NON-NLS-1$
	private static final QueryWordsToken fOR = new QueryWordsToken(OR, "OR"); //$NON-NLS-1$
	private static final QueryWordsToken fNOT = new QueryWordsToken(NOT, "NOT"); //$NON-NLS-1$
	public int type;
	public String value;
	protected QueryWordsToken(int type, String value) {
		this.type = type;
		this.value = value;
	}
	/**
	 * Creates a lucene query for a field
	 */
	public Query createLuceneQuery(String field, float boost) {
		Query q;
		int questionPos = value.indexOf('?');
		int starPos = value.indexOf('*');
		if (questionPos >= 0 || starPos >= 0) {
			if (questionPos == -1 && starPos == value.length() - 1) {
				Term t = new Term("exact_" + field, value.substring(0, starPos)); //$NON-NLS-1$
				q = new PrefixQuery(t);
				q = new BoostQuery(q, boost);
			} else {
				Term t = new Term("exact_" + field, value); //$NON-NLS-1$
				q = new WildcardQuery(t);
				q = new BoostQuery(q, boost);
			}
		} else {
			Term t = new Term(field, value);
			q = new TermQuery(t);
			q = new BoostQuery(q, boost);
		}
		// after updating Lucene, set boost on a Query class
		return q;
	}
	public static QueryWordsToken AND() {
		return fAND;
	}
	public static QueryWordsToken OR() {
		return fOR;
	}
	public static QueryWordsToken NOT() {
		return fNOT;
	}
	public static QueryWordsToken word(String word) {
		return new QueryWordsToken(QueryWordsToken.WORD, word);
	}
	public static QueryWordsPhrase phrase() {
		return new QueryWordsPhrase();
	}
	public static QueryWordsExactPhrase exactPhrase() {
		return new QueryWordsExactPhrase();
	}
	public static QueryWordsExactPhrase exactPhrase(String word) {
		QueryWordsExactPhrase token = new QueryWordsExactPhrase();
		token.addWord(word);
		return token;
	}
}
