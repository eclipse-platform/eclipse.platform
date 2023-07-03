/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *     George Suaridze <suag@1c.ru> (1C-Soft LLC) - Bug 560168
 *******************************************************************************/
package org.eclipse.help.internal.search;

import org.apache.lucene.analysis.*;
import org.eclipse.core.runtime.*;
import org.eclipse.help.internal.base.*;
import org.osgi.framework.*;

/**
 * Text Analyzer Descriptor. Encapsulates Lucene Analyzer
 */
public class AnalyzerDescriptor {

	private Analyzer luceneAnalyzer;

	private String id;

	private String lang;

	/**
	 * Constructor
	 */
	public AnalyzerDescriptor(String locale) {

		// try creating the analyzer for the specified locale (usually
		// lang_country)
		this.luceneAnalyzer = createAnalyzer(locale);

		// 	try creating configured analyzer for the language only
		if (this.luceneAnalyzer == null) {
			String language = null;
			if (locale.length() > 2) {
				language = locale.substring(0, 2);
				this.luceneAnalyzer = createAnalyzer(language);
			}
		}

		// if all fails, create default analyzer
		if (this.luceneAnalyzer == null) {
			this.id = HelpBasePlugin.PLUGIN_ID
					+ "#" //$NON-NLS-1$
					+ HelpBasePlugin.getDefault().getBundle().getHeaders().get(Constants.BUNDLE_VERSION)
					+ "?locale=" + locale; //$NON-NLS-1$
			this.luceneAnalyzer = new DefaultAnalyzer(locale);
			this.lang = locale;
		}
	}

	/**
	 * Gets the analyzer.
	 *
	 * @return Returns a Analyzer
	 */
	public Analyzer getAnalyzer() {
		return new SmartAnalyzer(lang, luceneAnalyzer);
	}

	/**
	 * Gets the id.
	 *
	 * @return Returns a String
	 */
	public String getId() {
		return id;
	}

	/**
	 * Gets the language for the analyzer
	 *
	 * @return Returns a String
	 */
	public String getLang() {
		return lang;
	}

	public String getAnalyzerClassName() {
		return luceneAnalyzer.getClass().getName();
	}

	/**
	 * Creates analyzer for a locale, if it is configured in the
	 * org.eclipse.help.luceneAnalyzer extension point. The identifier of the
	 * analyzer and locale and lang are also set.
	 *
	 * @return Analyzer or null if no analyzer is configured for given locale.
	 */
	private Analyzer createAnalyzer(String locale) {
		// find extension point
		IConfigurationElement configElements[] = Platform.getExtensionRegistry().getConfigurationElementsFor(
				HelpBasePlugin.PLUGIN_ID, "luceneAnalyzer"); //$NON-NLS-1$
		for (IConfigurationElement configElement : configElements) {
			if (!configElement.getName().equals("analyzer")) //$NON-NLS-1$
				continue;
			String analyzerLocale = configElement.getAttribute("locale"); //$NON-NLS-1$
			if (analyzerLocale == null || !analyzerLocale.equals(locale))
				continue;
			try {
				Object analyzer = configElement.createExecutableExtension("class"); //$NON-NLS-1$
				if (analyzer instanceof AnalyzerFactory)
					this.luceneAnalyzer = ((AnalyzerFactory) analyzer).create();
				else if (analyzer instanceof Analyzer)
					this.luceneAnalyzer = (Analyzer) analyzer;
				else
					continue;
				String pluginId = configElement.getContributor().getName();
				String pluginVersion = Platform.getBundle(pluginId).getHeaders()
						.get(Constants.BUNDLE_VERSION);
				this.id = pluginId + "#" + pluginVersion + "?locale=" + locale; //$NON-NLS-1$ //$NON-NLS-2$
				this.lang = locale;
				if (HelpBasePlugin.PLUGIN_ID.equals(pluginId)) {
					// The analyzer is contributed by help plugin.
					// Continue in case there is another analyzer for the
					// same locale
					// let another analyzer take precendence over one from
					// help
				} else {
					// the analyzer does not come from help
					return this.luceneAnalyzer;
				}
			} catch (CoreException ce) {
				ILog.of(getClass()).error(
						"Exception occurred creating text analyzer " //$NON-NLS-1$
								+ configElement.getAttribute("class") + " for " + locale + " locale.", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						ce);
			}
		}

		return this.luceneAnalyzer;
	}

	/**
	 * Checks whether analyzer is compatible with a given analyzer. The ID has
	 * the form [plugin_id]#[plugin_version]?locale=[locale], for example:
	 * org.eclipse.help.base#3.1.0?locale=ru
	 *
	 * @param analyzerId
	 *            id of analyzer used in the past by the index; id has a form:
	 *            [plugin.id]#[version]?locale=[locale]
	 * @return true when it is known that given analyzer is compatible with this
	 *         analyzer
	 */
	public boolean isCompatible(String analyzerId) {
		if (analyzerId != null) {
			// parse the id
			int numberSignIndex = analyzerId.indexOf('#');
			int questionMarkIndex = analyzerId.indexOf('?', numberSignIndex);
			String pluginId = analyzerId.substring(0, numberSignIndex);
			String version = analyzerId.substring(numberSignIndex + 1, questionMarkIndex);
			String locale = analyzerId.substring(questionMarkIndex + 1 + "locale=".length()); //$NON-NLS-1$

			// plugin compatible?
			// must both be org.eclipse.help.base
			String thisPluginId = id.substring(0, id.indexOf('#'));
			if (!HelpBasePlugin.PLUGIN_ID.equals(pluginId) || !HelpBasePlugin.PLUGIN_ID.equals(thisPluginId)) {
				return false;
			}

			// version compatible?
			// must both be >= 3.1
			Version vA = getVersion(id);
			Version vB = new Version(version);
			Version v3_1 = new Version(3, 1, 0);
			if (vA.compareTo(v3_1) < 0 && vB.compareTo(v3_1) < 0) {
				return false;
			}

			// locale compatible?
			// first part must be equal (first two chars)
			if (!lang.substring(0, 2).equals(locale.substring(0, 2))) {
				return false;
			}
			return true;
		}
		return false;
	}

	private Version getVersion(String id) {
		int idStart = id.indexOf('#');
		int idStop = id.indexOf('?');
		String value = idStop == -1 ? id.substring(idStart + 1) : id.substring(idStart + 1, idStop);
		return new Version(value);
	}
}
