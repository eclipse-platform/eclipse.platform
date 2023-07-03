/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
 *     Alexander Kurtakov - Bug 460787
 *     Sopot Cela - Bug 466829
 *******************************************************************************/
package org.eclipse.help.internal.search;

import java.util.HashSet;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;

/**
 * Lucene Analyzer for English.
 * LowerCaseAndDigitsTokenizer-&gt;StopFilter-&gt;PorterStemFilter
 */
public final class Analyzer_en extends Analyzer {
	/**
	 * Constructor for Analyzer_en.
	 */
	public Analyzer_en() {
		super();
	}

	/*
	 * Can't use try-with-resources because the Lucene internally reuses
	 * components. See {@link org.apache.lucene.analysis.Analyzer.ReuseStrategy}
	 */
	@SuppressWarnings("resource")
	@Override
	protected TokenStreamComponents createComponents(String fieldName) {
		final Tokenizer source = new CharAndDigitsTokenizer();
		TokenStream result = new StopFilter(source, new CharArraySet(getStopWords(), false));
		result = new LowerCaseFilter(result);
		result = new PorterStemFilter(result);
		return new TokenStreamComponents(source, result);
	}

	private Set<String> stopWords;

	private Set<String> getStopWords() {
		if ( stopWords == null ) {
			stopWords = new HashSet<>();
			for (String element : STOP_WORDS) {
				stopWords.add(element);
			}
		}
		return stopWords;
	}

	/**
	 * Array of English stop words. Differs from StandardAnalyzer's default stop
	 * words by not having "for", "if", and "this" that are java keywords.
	 */
	private final static String[] STOP_WORDS = {"a", //$NON-NLS-1$
			"and", //$NON-NLS-1$
			"are", //$NON-NLS-1$
			"as", //$NON-NLS-1$
			"at", //$NON-NLS-1$
			"be", //$NON-NLS-1$
			"but", //$NON-NLS-1$
			"by", //$NON-NLS-1$
			"in", //$NON-NLS-1$
			"into", //$NON-NLS-1$
			"is", //$NON-NLS-1$
			"it", //$NON-NLS-1$
			"no", //$NON-NLS-1$
			"not", //$NON-NLS-1$
			"of", //$NON-NLS-1$
			"on", //$NON-NLS-1$
			"or", //$NON-NLS-1$
			"s", //$NON-NLS-1$
			"such", //$NON-NLS-1$
			"t", //$NON-NLS-1$
			"that", //$NON-NLS-1$
			"the", //$NON-NLS-1$
			"their", //$NON-NLS-1$
			"then", //$NON-NLS-1$
			"there", //$NON-NLS-1$
			"these", //$NON-NLS-1$
			"they", //$NON-NLS-1$
			"to", //$NON-NLS-1$
			"was", //$NON-NLS-1$
			"will", //$NON-NLS-1$
			"with"}; //$NON-NLS-1$

}
