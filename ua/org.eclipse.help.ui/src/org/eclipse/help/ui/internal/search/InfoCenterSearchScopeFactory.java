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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.StringTokenizer;

import org.eclipse.help.internal.search.InfoCenter;
import org.eclipse.help.search.ISearchScope;
import org.eclipse.help.ui.ISearchScopeFactory;
import org.eclipse.jface.preference.IPreferenceStore;

/**
 * Creates the scope for local search using the help working sets
 */
public class InfoCenterSearchScopeFactory implements ISearchScopeFactory {
	public static final String P_URL = "url"; //$NON-NLS-1$
	public static final String P_SEARCH_SELECTED = "searchSelected"; //$NON-NLS-1$
	public static final String P_TOCS = "tocs"; //$NON-NLS-1$
	public static final String TOC_SEPARATOR = ";"; //$NON-NLS-1$

	@Override
	public ISearchScope createSearchScope(IPreferenceStore store, String engineId,
			Dictionary<String, Object> parameters) {
		String url = getProperty(P_URL, store, engineId, parameters);
		String ssvalue = getProperty(P_SEARCH_SELECTED, store, engineId, parameters);
		boolean searchSelected = ssvalue!=null && ssvalue.equalsIgnoreCase("true"); //$NON-NLS-1$
		String [] tocs=null;
		if (searchSelected) {
			String tvalue = getProperty(P_TOCS, store, engineId, parameters);
			if (tvalue!=null && tvalue.length()>0) {
				StringTokenizer stok = new StringTokenizer(tvalue, TOC_SEPARATOR);
				ArrayList<String> list = new ArrayList<>();
				while (stok.hasMoreTokens()) {
					String toc = stok.nextToken();
					list.add(toc);
				}
				if (list.size()>0)
					tocs = list.toArray(new String[list.size()]);
			}
		}
		return new InfoCenter.Scope(url, searchSelected, tocs);
	}

	private String getProperty(String key, IPreferenceStore store, String engineId,
			Dictionary<String, Object> parameters) {
		// try the store first
		String value = store.getString(engineId+"."+key); //$NON-NLS-1$
		if (value!=null && value.length()>0) return value;
		// try the parameters
		return (String) parameters.get(key);
	}
}
