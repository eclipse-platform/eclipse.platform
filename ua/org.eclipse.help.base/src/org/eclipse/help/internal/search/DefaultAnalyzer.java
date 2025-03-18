/*******************************************************************************
 * Copyright (c) 2000, 2024 IBM Corporation and others.
 *
 * This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Alexander Kurtakov - Bug 460787
 *     Sopot Cela - Bug 466829
 *     George Suaridze <suag@1c.ru> (1C-Soft LLC) - Bug 560168
 *******************************************************************************/
package org.eclipse.help.internal.search;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;

/**
 * Lucene Analyzer. LowerCaseFilter-&gt;StandardTokenizer
 */
public final class DefaultAnalyzer extends Analyzer {

	/*
	 * Can't use try-with-resources because the Lucene internally reuses
	 * components. See {@link org.apache.lucene.analysis.Analyzer.ReuseStrategy}
	 */
	@SuppressWarnings("resource")
	@Override
	protected TokenStreamComponents createComponents(String fieldName) {
		Tokenizer source = new StandardTokenizer();
		LowerCaseFilter filter = new LowerCaseFilter(source);
		TokenStreamComponents components = new TokenStreamComponents(source, filter);
		return components;
	}
}
