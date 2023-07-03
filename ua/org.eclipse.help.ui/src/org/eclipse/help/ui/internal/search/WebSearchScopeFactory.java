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
 *******************************************************************************/
package org.eclipse.help.ui.internal.search;

import java.util.Dictionary;

import org.eclipse.help.internal.search.WebSearch;
import org.eclipse.help.search.ISearchScope;
import org.eclipse.help.ui.ISearchScopeFactory;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Factory for creating scope objects for the generic web search engine
 */
public class WebSearchScopeFactory implements ISearchScopeFactory {
	public final static String P_URL = "url"; //$NON-NLS-1$

	@Override
	public ISearchScope createSearchScope(IPreferenceStore store, String engineId,
			Dictionary<String, Object> parameters) {
		String urlTemplate = getProperty(store, engineId, parameters);
		return new WebSearch.Scope(urlTemplate);
	}

	private String getProperty(IPreferenceStore store, String engineId,
			Dictionary<String, Object> parameters) {
		// try the store first
		String value = store.getString(engineId+"."+P_URL); //$NON-NLS-1$
		if (value!=null && value.length()>0) return value;
		// try the parameters
		return (String)parameters.get(P_URL);
	}
}
