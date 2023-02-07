/*******************************************************************************
 * Copyright (c) 2005, 2020 Intel Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Intel Corporation - initial API and implementation
 *     IBM Corporation - 122967 [Help] Remote help system
 *                       163558 Dynamic content support for all UA
 *                       165168 [Help] Better control of how help content is arranged and ordered
 *     George Suaridze <suag@1c.ru> (1C-Soft LLC) - Bug 560168
 *******************************************************************************/
package org.eclipse.help.internal.index;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.AbstractIndexProvider;
import org.eclipse.help.IIndex;
import org.eclipse.help.IIndexContribution;
import org.eclipse.help.internal.HelpData;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.UAElementFactory;

public class IndexManager {

	private static final String EXTENSION_POINT_ID_INDEX = HelpPlugin.PLUGIN_ID + ".index"; //$NON-NLS-1$
	private static final String ELEMENT_NAME_INDEX_PROVIDER = "indexProvider"; //$NON-NLS-1$
	private static final String ATTRIBUTE_NAME_CLASS = "class"; //$NON-NLS-1$

	private Map<String, IIndexContribution[]> indexContributionsByLocale = new HashMap<>();
	private Map<String, Index> indexesByLocale = new HashMap<>();
	private AbstractIndexProvider[] indexProviders;

	public synchronized IIndex getIndex(String locale) {
		Index index = indexesByLocale.get(locale);
		if (index == null) {
			HelpPlugin.getTocManager().getTocs(locale);  // Ensure Tocs and index not built simultaneously
			long start = System.currentTimeMillis();
			if (HelpPlugin.DEBUG_INDEX) {
				System.out.println("Start to update keyword index for locale " + locale); //$NON-NLS-1$
			}
			List<IndexContribution> contributions = new ArrayList<>(Arrays.asList(readIndexContributions(locale)));
			filterIndexContributions(contributions);
			IndexAssembler assembler = new IndexAssembler();
			index = assembler.assemble(contributions, locale);
			indexesByLocale.put(locale, index);
			long stop = System.currentTimeMillis();
			if (HelpPlugin.DEBUG_INDEX) {
				System.out.println("Milliseconds to update keyword index for locale " + locale +  " = " + (stop - start)); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return index;
	}

	/*
	 * Returns all index contributions for the given locale, from all
	 * providers.
	 */
	public synchronized IIndexContribution[] getIndexContributions(String locale) {
		IIndexContribution[] contributions = indexContributionsByLocale.get(locale);
		if (contributions == null) {
			contributions = readIndexContributions(locale);
			indexContributionsByLocale.put(locale, contributions);
		}
		return contributions;
	}

	private IndexContribution[] readIndexContributions(String locale) {
		IndexContribution[] cached;
		List<IndexContribution> contributions = new ArrayList<>();
		AbstractIndexProvider[] providers = getIndexProviders();
		for (int i=0;i<providers.length;++i) {
			IIndexContribution[] contrib;
			try {
				contrib = providers[i].getIndexContributions(locale);
				// check for nulls and root element
				for (int j = 0; j < contrib.length; ++j) {
					if (contrib[j] == null) {
						String msg = "Help keyword index provider \"" + providers[i].getClass().getName() + "\" returned a null contribution (skipping)"; //$NON-NLS-1$ //$NON-NLS-2$
						Platform.getLog(getClass()).error(msg);
					} else if (contrib[j].getIndex() == null) {
						String msg = "Help keyword index provider \"" + providers[i].getClass().getName() + "\" returned a contribution with a null root element (expected a \"" + Index.NAME + "\" element; skipping)"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						Platform.getLog(getClass()).error(msg);
					} else {
						IndexContribution contribution = new IndexContribution();
						contribution.setId(contrib[j].getId());
						contribution.setLocale(contrib[j].getLocale());
						IIndex index = contrib[j].getIndex();
						contribution.setIndex(index instanceof Index ? (Index) index
								: (Index) UAElementFactory.newElement(index));
						contributions.add(contribution);
					}
				}
			} catch (Throwable t) {
				// log, and skip the offending provider
				String msg = "Error getting help keyword index data from provider: " + providers[i].getClass().getName() + " (skipping provider)"; //$NON-NLS-1$ //$NON-NLS-2$
				Platform.getLog(getClass()).error(msg, t);
				continue;
			}
		}
		cached = contributions.toArray(new IndexContribution[contributions.size()]);
		return cached;
	}

	/*
	 * Clears all cached contributions, forcing the manager to query the
	 * providers again next time a request is made.
	 */
	public void clearCache() {
		indexContributionsByLocale.clear();
		indexesByLocale.clear();
	}

	/*
	 * Internal hook for unit testing.
	 */
	public AbstractIndexProvider[] getIndexProviders() {
		if (indexProviders == null) {
			List<AbstractIndexProvider> providers = new ArrayList<>();
			IExtensionRegistry registry = Platform.getExtensionRegistry();
			IConfigurationElement[] elements = registry.getConfigurationElementsFor(EXTENSION_POINT_ID_INDEX);
			for (int i=0;i<elements.length;++i) {
				IConfigurationElement elem = elements[i];
				if (elem.getName().equals(ELEMENT_NAME_INDEX_PROVIDER)) {
					try {
						AbstractIndexProvider provider = (AbstractIndexProvider)elem.createExecutableExtension(ATTRIBUTE_NAME_CLASS);
						providers.add(provider);
					}
					catch (CoreException e) {
						// log and skip
						String msg = "Error instantiating help keyword index provider class \"" + elem.getAttribute(ATTRIBUTE_NAME_CLASS) + '"'; //$NON-NLS-1$
						Platform.getLog(getClass()).error(msg, e);
					}
				}
			}
			indexProviders = providers.toArray(new AbstractIndexProvider[providers.size()]);
		}
		return indexProviders;
	}

	/*
	 * Returns whether or not the index has been completely loaded for the
	 * given locale yet or not.
	 */
	public boolean isIndexLoaded(String locale) {
		return indexesByLocale.get(locale) != null;
	}

	/*
	 * Internal hook for unit testing.
	 */
	public void setIndexProviders(AbstractIndexProvider[] indexProviders) {
		this.indexProviders = indexProviders;
	}

	/*
	 * Filters the given contributions according to product preferences. If
	 * either the contribution's id or its category's id is listed in the
	 * ignoredIndexes, filter the contribution.
	 */
	private void filterIndexContributions(List<IndexContribution> unfiltered) {
		Set<String> indexesToFilter = getIgnoredIndexContributions();
		ListIterator<IndexContribution> iter = unfiltered.listIterator();
		while (iter.hasNext()) {
			IIndexContribution contribution = iter.next();
			if (indexesToFilter.contains(contribution.getId())) {
				iter.remove();
			}
		}
	}

	private Set<String> getIgnoredIndexContributions() {
		HelpData helpData = HelpData.getProductHelpData();
		if (helpData != null) {
			return helpData.getHiddenIndexes();
		} else {
			HashSet<String> ignored = new HashSet<>();
			String preferredIndexes = Platform.getPreferencesService().getString(HelpPlugin.PLUGIN_ID, HelpPlugin.IGNORED_INDEXES_KEY, "", null); //$NON-NLS-1$
			if (preferredIndexes.length() > 0) {
				StringTokenizer suggestdOrderedInfosets = new StringTokenizer(preferredIndexes, " ;,"); //$NON-NLS-1$
				while (suggestdOrderedInfosets.hasMoreTokens()) {
					ignored.add(suggestdOrderedInfosets.nextToken());
				}
			}
			return ignored;
		}
	}
}
