/*******************************************************************************
 * Copyright (c) 2012, 2016 IBM Corporation and others.
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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.br.BrazilianAnalyzer;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.cz.CzechAnalyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.analysis.el.GreekAnalyzer;
import org.apache.lucene.analysis.fr.FrenchAnalyzer;
import org.apache.lucene.analysis.nl.DutchAnalyzer;
import org.apache.lucene.analysis.ru.RussianAnalyzer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;

/**
 * A factory responsible for instantiating a lucene {@link Analyzer}.
 */
public class AnalyzerFactory implements IExecutableExtension{
	private String locale = null;
	public Analyzer create() {
		if (locale == null)
			return null;
		if ("pt".equals(locale)) //$NON-NLS-1$
			return new BrazilianAnalyzer();
		if ("ja".equals(locale)) //$NON-NLS-1$
			return new CJKAnalyzer();
		if ("ko".equals(locale)) //$NON-NLS-1$
			return new CJKAnalyzer();
		if ("pt".equals(locale)) //$NON-NLS-1$
			return new BrazilianAnalyzer();
		if ("cs".equals(locale)) //$NON-NLS-1$
			return new CzechAnalyzer();
		if ("de".equals(locale)) //$NON-NLS-1$
			return new GermanAnalyzer();
		if ("el".equals(locale)) //$NON-NLS-1$
			return new GreekAnalyzer();
		if ("fr".equals(locale)) //$NON-NLS-1$
			return new FrenchAnalyzer();
		if ("nl".equals(locale)) //$NON-NLS-1$
			return new DutchAnalyzer();
		if ("ru".equals(locale)) //$NON-NLS-1$
			return new RussianAnalyzer();
		//unknown language
		return null;

	}

	@Override
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data)
			throws CoreException {
		if (data instanceof String)
			locale = (String)data;
	}

}
