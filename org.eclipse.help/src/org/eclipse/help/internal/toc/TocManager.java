/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
 *     James Blackburn - fix for Bug 230842
 *     George Suaridze <suag@1c.ru> (1C-Soft LLC) - Bug 560168
 *******************************************************************************/
package org.eclipse.help.internal.toc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.AbstractTocProvider;
import org.eclipse.help.IToc;
import org.eclipse.help.ITocContribution;
import org.eclipse.help.ITopic;
import org.eclipse.help.internal.HelpData;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.Topic;
import org.eclipse.help.internal.UAElement;
import org.eclipse.help.internal.UAElementFactory;

/*
 * Manages toc contributions (TocContribution) supplied by the various toc
 * providers (AbstractTocProvider).
 */
public class TocManager {

	private static final String EXTENSION_POINT_ID_TOC = HelpPlugin.PLUGIN_ID + ".toc"; //$NON-NLS-1$
	private static final String ELEMENT_NAME_TOC_PROVIDER = "tocProvider"; //$NON-NLS-1$
	private static final String ATTRIBUTE_NAME_CLASS = "class"; //$NON-NLS-1$

	private AbstractTocProvider[] tocProviders;
	// There are two sets of TOC contributions, one is used for Toc Assembly and is modified from the original
	// The other is used by the TocServlet and is unprocessed, i.e. anchors are not replaced with the contributions
	private Map<String, TocContribution[]> tocContributionsByLocale = new HashMap<>();
	private Map<String, TocContribution[]> tocContributionsForTocByLocale = new HashMap<>();
	private Map<String, Toc[]> tocsByLocale = new HashMap<>();
	private Map<String, Toc> tocsById = new HashMap<>();
	private Map<String, Toc> tocsByTopic;

	/*
	 * Returns all toc entries (complete books) for the given locale.
	 */
	public synchronized Toc[] getTocs(String locale) {
		Toc[] tocs = tocsByLocale.get(locale);
		if (tocs == null) {
			long start = System.currentTimeMillis();
			if (HelpPlugin.DEBUG_TOC) {
				System.out.println("Start to build toc for locale " + locale); //$NON-NLS-1$
			}
			Set<String> tocsToFilter = getIgnoredTocContributions();
			TocContribution[] raw = getRootTocContributions(locale, tocsToFilter);
			TocContribution[] filtered = filterTocContributions(raw, tocsToFilter);
			ITocContribution[] ordered = new TocSorter().orderTocContributions(filtered);
			List<Toc> orderedTocs = new ArrayList<>(ordered.length);
			for (int i=0;i<ordered.length;++i) {
				try {
					Toc toc = (Toc)ordered[i].getToc();
					orderedTocs.add(toc);
					tocsById.put(ordered[i].getId(), toc);
				}
				catch (Throwable t) {
					// log and skip
					String msg = "Error getting " + Toc.class.getName() + " from " + ITocContribution.class.getName() + ": " + ordered[i]; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					ILog.of(getClass()).error(msg, t);
				}
			}
			tocs = orderedTocs.toArray(new Toc[orderedTocs.size()]);
			TopicSorter topicSorter = new TopicSorter();
			for (int i = 0; i < tocs.length; i++) {
				topicSorter.sortChildren(tocs[i]);
			}
			tocsByLocale.put(locale, tocs);
			long stop = System.currentTimeMillis();
			if (HelpPlugin.DEBUG_TOC) {
				System.out.println("Milliseconds to update toc for locale " + locale +  " = " + (stop - start)); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
		return tocs;
	}

	/*
	 * Returns the toc whose toc contribution has the given id, for the
	 * given locale.
	 */
	public synchronized Toc getToc(String id, String locale) {
		getTocs(locale);
		return tocsById.get(id);
	}

	public synchronized Toc getOwningToc(String href) {
		if (tocsByTopic == null) {
			tocsByTopic = new HashMap<>();
			Toc[] tocs = HelpPlugin.getTocManager().getTocs(Platform.getNL());
			for (int i=0;i<tocs.length;++i) {
				ITocContribution contribution = tocs[i].getTocContribution();
				String[] extraDocuments = contribution.getExtraDocuments();
				for (int j=0;j<extraDocuments.length;++j) {
					tocsByTopic.put(extraDocuments[j], tocs[i]);
				}
			}
		}
		return tocsByTopic.get(href);
	}

	public synchronized ITopic getTopic(String href, String locale) {
		Toc[] tocs = HelpPlugin.getTocManager().getTocs(locale);
		for (int i=0;i<tocs.length;++i) {
			ITopic topic = tocs[i].getTopic(href);
			if (topic != null) {
				return topic;
			}
		}
		int index = href.indexOf('#');
		if (index != -1) {
			return getTopic(href.substring(0, index), locale);
		}
		return null;
	}

	public synchronized int[] getTopicPath(String href, String locale) {
		ITopic topic = getTopic(href, locale);
		try {
			if (topic != null && topic instanceof UAElement) {
				List<Integer> path = new ArrayList<>();
				UAElement element = (UAElement) topic;
				while (!(element instanceof Toc)) {
					UAElement parent = element.getParentElement();
					path.add(Integer.valueOf(indexOf(parent, (Topic)element)));
					element = parent;
				}
				Toc[] tocs = getTocs(locale);
				for (int i=0;i<tocs.length;++i) {
					if (tocs[i] == element) {
						path.add(Integer.valueOf(i));
						int[] array = new int[path.size()];
						for (int j=0;j<array.length;++j) {
							array[j] = path.get(array.length - 1 - j).intValue();
						}
						return array;
					}
				}
			}
		} catch (Exception e) {
			return null;
		}
		// no path; not in toc
		return null;
	}

	/*
	 * Returns the zero-based index at which the child topic is located under
	 * the parent topic/toc.
	 */
	private int indexOf(UAElement parent, Topic child) {
		ITopic[] children;
		if (parent instanceof Topic) {
			children = ((Topic)parent).getSubtopics();
		}
		else if (parent instanceof Toc) {
			children = ((Toc)parent).getTopics();
		}
		else {
			return -1;
		}
		for (int i=0;i<children.length;++i) {
			if (children[i] == child) {
				return i;
			}
		}
		return -1;
	}

	/*
	 * Returns all toc contributions for the given locale, from all toc
	 * providers.
	 */
	public TocContribution[] getTocContributions(String locale) {
		return getAndCacheTocContributions(locale, tocContributionsByLocale);
	}

	private TocContribution[] getTocContributionsForToc(String locale) {
		return getAndCacheTocContributions(locale, tocContributionsForTocByLocale);
	}

	private synchronized TocContribution[] getAndCacheTocContributions(String locale,
			Map<String, TocContribution[]> contributionsByLocale) {
		TocContribution[] cached = contributionsByLocale.get(locale);
		if (cached == null) {
			HashMap<String, TocContribution> contributions = new HashMap<>();
			AbstractTocProvider[] providers = getTocProviders();
			for (int i=0;i<providers.length;++i) {
				ITocContribution[] contrib;
				try {
					contrib = providers[i].getTocContributions(locale);
					for (int j=0;j<contrib.length;++j) {
						TocContribution contribution = new TocContribution();
						contribution.setCategoryId(contrib[j].getCategoryId());
						contribution.setContributorId(contrib[j].getContributorId());
						contribution.setExtraDocuments(contrib[j].getExtraDocuments());
						contribution.setId(contrib[j].getId());
						contribution.setLocale(contrib[j].getLocale());
						contribution.setPrimary(contrib[j].isPrimary());
						IToc toc = contrib[j].getToc();
						Toc t = toc instanceof Toc ? (Toc)toc : (Toc)UAElementFactory.newElement(toc);
						t.setLinkTo(contrib[j].getLinkTo());
						contribution.setToc(t);
						if(!contributions.containsKey(contrib[j].getId()))
							contributions.put(contrib[j].getId(), contribution);
					}
				}
				catch (Throwable t) {
					// log, and skip the offending provider
					String msg = "Error getting help table of contents data from provider: " + providers[i].getClass().getName() + " (skipping provider)"; //$NON-NLS-1$ //$NON-NLS-2$
					ILog.of(getClass()).error(msg, t);
					continue;
				}

			}
			cached = contributions.values().toArray(new TocContribution[contributions.size()]);
			contributionsByLocale.put(locale, cached);
		}
		return cached;
	}

	/*
	 * Clears all cached contributions, forcing the manager to query the
	 * providers again next time a request is made.
	 */
	public void clearCache() {
		tocContributionsByLocale.clear();
		tocContributionsForTocByLocale.clear();
		tocsByLocale.clear();
		tocsById.clear();
		tocsByTopic = null;
		tocProviders=null;
	}

	/*
	 * Internal hook for unit testing.
	 */
	public AbstractTocProvider[] getTocProviders() {
		if (tocProviders == null) {
			List<AbstractTocProvider> providers = new ArrayList<>();
			IExtensionRegistry registry = Platform.getExtensionRegistry();
			IConfigurationElement[] elements = registry.getConfigurationElementsFor(EXTENSION_POINT_ID_TOC);
			for (int i=0;i<elements.length;++i) {
				IConfigurationElement elem = elements[i];
				if (elem.getName().equals(ELEMENT_NAME_TOC_PROVIDER)) {
					try {
						AbstractTocProvider provider = (AbstractTocProvider)elem.createExecutableExtension(ATTRIBUTE_NAME_CLASS);
						providers.add(provider);
					}
					catch (CoreException e) {
						// log and skip
						String msg = "Error instantiating help table of contents provider class \"" + elem.getAttribute(ATTRIBUTE_NAME_CLASS) + '"'; //$NON-NLS-1$
						ILog.of(getClass()).error(msg, e);
					}
				}
			}
			providers.sort(new TocProviderComparator());
			tocProviders = providers.toArray(new AbstractTocProvider[providers.size()]);
		}
		return tocProviders;
	}

	/*
	 * Internal hook for unit testing.
	 */
	public void setTocProviders(AbstractTocProvider[] tocProviders) {
		this.tocProviders = tocProviders;
	}

	/*
	 * Filters the given contributions according to product preferences. If
	 * either the contribution's id or its category's id is listed in the
	 * ignoredTocs, filter the contribution.
	 */
	private TocContribution[] filterTocContributions(TocContribution[] unfiltered, Set<String> tocsToFilter) {
		List<TocContribution> filtered = new ArrayList<>();
		for (int i=0;i<unfiltered.length;++i) {
			if (!tocsToFilter.contains(unfiltered[i].getId()) &&
					!tocsToFilter.contains(unfiltered[i].getCategoryId())) {
				filtered.add(unfiltered[i]);
			}
		}
		return filtered.toArray(new TocContribution[filtered.size()]);
	}

	private TocContribution[] getRootTocContributions(String locale, Set<String> tocsToFilter) {
		TocContribution[] contributions = getTocContributionsForToc(locale);
		List<TocContribution> unassembled = new ArrayList<>(Arrays.asList(contributions));
		TocAssembler assembler = new TocAssembler(tocsToFilter);
		List<TocContribution> assembled = assembler.assemble(unassembled);
		return assembled.toArray(new TocContribution[assembled.size()]);
	}

	private Set<String> getIgnoredTocContributions() {
		HelpData helpData = HelpData.getProductHelpData();
		if (helpData != null) {
			return helpData.getHiddenTocs();
		}
		else {
			HashSet<String> ignored = new HashSet<>();
			String preferredTocs = Platform.getPreferencesService().getString(HelpPlugin.PLUGIN_ID, HelpPlugin.IGNORED_TOCS_KEY, "", null); //$NON-NLS-1$
			if (preferredTocs.length() > 0) {
				StringTokenizer suggestdOrderedInfosets = new StringTokenizer(preferredTocs, " ;,"); //$NON-NLS-1$
				while (suggestdOrderedInfosets.hasMoreTokens()) {
					ignored.add(suggestdOrderedInfosets.nextToken());
				}
			}
			return ignored;
		}
	}

	/*
	 * Returns whether or not the toc for the given locale has been completely
	 * loaded yet or not.
	 */
	public boolean isTocLoaded(String locale) {
		return tocsByLocale.get(locale) != null;
	}

}
