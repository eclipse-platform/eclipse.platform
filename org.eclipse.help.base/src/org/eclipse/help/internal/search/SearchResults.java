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
 *     Yaroslav Nikolaiko <nikolaiko.yaroslav@gmail.com> - [webapp][base] Bugs related to Search Scope for filtering content in The Eclipse platform's help infocenter - http://bugs.eclipse.org/441407
 *******************************************************************************/
package org.eclipse.help.internal.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.help.IToc;
import org.eclipse.help.ITopic;
import org.eclipse.help.base.AbstractHelpScope;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.Topic;
import org.eclipse.help.internal.base.scope.CriteriaHelpScope;
import org.eclipse.help.internal.criteria.CriterionResource;
import org.eclipse.help.internal.util.URLCoder;
import org.eclipse.help.internal.workingset.AdaptableHelpResource;
import org.eclipse.help.internal.workingset.AdaptableSelectedToc;
import org.eclipse.help.internal.workingset.AdaptableSelectedTopic;
import org.eclipse.help.internal.workingset.AdaptableToc;
import org.eclipse.help.internal.workingset.AdaptableTopic;
import org.eclipse.help.internal.workingset.WorkingSet;

/**
 * Search result collector. Performs filtering and collects hits into an array
 * of SearchHit
 */
public class SearchResults implements ISearchHitCollector {
	// Collection of AdaptableHelpResource[]
	private ArrayList<AdaptableHelpResource> scopes;
	private int maxHits;
	private String locale;
	private AbstractHelpScope filter;
	private CriteriaHelpScope criteriaScope;
	protected SearchHit[] searchHits = new SearchHit[0];
	private QueryTooComplexException searchException = null;
	private boolean isQuickSearch;

	public SearchResults(WorkingSet[] workingSets, int maxHits, String locale) {
		this(workingSets, maxHits, locale, false);
	}

	/**
	 * Constructor
	 *
	 * @param workingSets
	 *            working sets or null if no filtering
	 */
	public SearchResults(WorkingSet[] workingSets, int maxHits, String locale, boolean isQuickSearch) {
		this.maxHits = maxHits;
		this.locale = locale;
		this.scopes = getScopes(workingSets);
		this.criteriaScope = new CriteriaHelpScope(getCriteriaScopes(workingSets));
		this.isQuickSearch = isQuickSearch;
	}

	public void setFilter(AbstractHelpScope filter) {
		this.filter = filter;
	}

	@Override
	public void addHits(List<SearchHit> hits, String highlightTerms) {
		String urlEncodedWords = URLCoder.encode(highlightTerms);
		List<SearchHit> searchHitList = new ArrayList<>();
		float scoreScale = 1.0f;
		boolean scoreScaleSet = false;

		Iterator<SearchHit> iter = hits.iterator();
		for (int filteredHits = 0; filteredHits < maxHits && iter.hasNext(); ) {
			SearchHit rawHit = iter.next();
			String href = rawHit.getHref();
			IToc toc = null; // the TOC containing the topic
			AdaptableHelpResource scope = null;
			// the scope for the topic, if any
			if (scopes == null) {
				toc = getTocForTopic(href, locale);
				if (toc == null && !rawHit.canOpen()) {
					continue;
				}
			} else {
				scope = getScopeForTopic(href);
				if (scope == null) {
					// topic outside of scope
					continue;
				} else if ((scope instanceof AdaptableToc) || (scope instanceof AdaptableSelectedToc)) {
					toc = scope.getAdapter(IToc.class);
				} else if((scope instanceof AdaptableTopic) || (scope instanceof AdaptableSelectedTopic)){ // scope is AdaptableTopic or AdaptableSelectedTopic
					toc = scope.getParent().getAdapter(IToc.class);
				}
			}

			// adjust score
			float score = rawHit.getScore();
			if (!scoreScaleSet) {
				if (score > 0) {
					scoreScale = 0.99f / score;
					score = 1;
				}
				scoreScaleSet = true;
			} else {
				score = score * scoreScale + 0.01f;
			}

			// Set the document label
			String label = rawHit.getLabel();
			if ("".equals(label) && toc != null) { //$NON-NLS-1$
				ITopic t;
				if (scope != null) {
					t = scope.getTopic(href);
				} else {
					t = toc.getTopic(href);
				}
				if (t != null) {
					label = t.getLabel();
				}
			}
			if (label == null || "".equals(label)) { //$NON-NLS-1$
				label = href;
			}

			// Set document href
			if (urlEncodedWords.length() > 0) {
				href += "?resultof=" + urlEncodedWords; //$NON-NLS-1$
			}
			filteredHits ++;
			searchHitList.add(new SearchHit(href, label, rawHit.getSummary(), score, toc, rawHit.getRawId(), rawHit.getParticipantId(), rawHit.isPotentialHit()));
		}
		searchHits = searchHitList
				.toArray(new SearchHit[searchHitList.size()]);

	}

	public void setHits(SearchHit hits[])
	{
		searchHits = hits;
	}
	/**
	 * Finds a topic within a scope
	 */
	private AdaptableHelpResource getScopeForTopic(String href) {
		boolean enabled = HelpPlugin.getCriteriaManager().isCriteriaEnabled();
		for (AdaptableHelpResource scope : scopes) {
			ITopic inScopeTopic = scope.getTopic(href);
			if (inScopeTopic != null){
				if (filter == null || filter.inScope(inScopeTopic)) {
					if(!enabled || (enabled && criteriaScope.inScope(inScopeTopic))){
						return scope;
					}
				}
			}

			// add root toc's extradir topics to search scope
			if (!isQuickSearch) {
				IToc tocRoot = getTocForScope(scope, locale);
				if (tocRoot != null) {
					IToc toc = HelpPlugin.getTocManager().getOwningToc(href);
					if (toc != null) {
						String owningTocHref = toc.getHref();
						if (owningTocHref == tocRoot.getHref()) {
							Topic extradirTopic = new Topic();
							extradirTopic.setHref(href);
							if (filter == null || filter.inScope(extradirTopic)) {
								if(!enabled || (enabled && criteriaScope.inScope(inScopeTopic))){
									return scope;
								}
							}
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Finds a scope in a toc
	 */
	private IToc getTocForScope(AdaptableHelpResource scope, String locale) {
		if (scope == null) {
			return null;
		}
		String href = scope.getHref();
		IToc toc=scope.getAdapter(IToc.class);
		if (toc != null){
			href=toc.getTopic(null).getHref();
		}

		if (href != null && href.length() > 0) {
			return getTocForTopic(href, locale);
		} else {
			AdaptableHelpResource[] childrenScopes = scope.getChildren();
			if (childrenScopes != null) {
				for (AdaptableHelpResource childrenScope : childrenScopes) {
					// To find the target toc recursively because scope.getHref
					// may be null.
					toc = getTocForScope(childrenScope, locale);
					if (toc != null)
						return toc;
				}
			}
		}
		return null;
	}

	/**
	 * Finds a topic in a toc or within a scope if specified
	 */
	private IToc getTocForTopic(String href, String locale) {
		IToc[] tocs = HelpPlugin.getTocManager().getTocs(locale);
		boolean foundInToc = false;
		for (IToc nextToc : tocs) {
			ITopic topic = nextToc.getTopic(href);
			if (topic != null) {
				foundInToc = true;
				if (filter == null || filter.inScope(topic)) {
					return nextToc;
				}
			}
			// Test for href attached to Toc element
			topic = nextToc.getTopic(null);
			if (topic != null && href != null && href.equals(topic.getHref())) {
				if (filter == null || filter.inScope(topic)) {
					return nextToc;
				}
			}
		}
		if (!foundInToc) {
			// test to pick up files in extradirs
			IToc toc = HelpPlugin.getTocManager().getOwningToc(href);
			if (toc != null) {
				foundInToc = true;
				if (filter == null || filter.inScope(toc)) {
					return toc;
				}
			}
		}
		return null;
	}

	/**
	 * Gets the searchHits.
	 *
	 * @return Returns a SearchHit[]
	 */
	public SearchHit[] getSearchHits() {
		return searchHits;
	}

	public QueryTooComplexException getException() {
		return searchException;
	}

	/**
	 * Returns a collection of adaptable help resources that are roots for
	 * filtering.
	 *
	 * @return Collection
	 */
	private ArrayList<AdaptableHelpResource> getScopes(WorkingSet[] wSets) {
		if (wSets == null)
			return null;

		scopes = new ArrayList<>(wSets.length);
		for (int w = 0; w < wSets.length; w++) {
			AdaptableHelpResource[] elements = wSets[w].getElements();
			Collections.addAll(scopes, elements);
		}
		return scopes;
	}

	private ArrayList<CriterionResource> getCriteriaScopes(WorkingSet[] wSets){
		if (wSets == null)
			return null;

		ArrayList<CriterionResource> criteriaScopes = new ArrayList<>(wSets.length);
		for (WorkingSet wSet : wSets) {
			CriterionResource[] elements = wSet.getCriteria();
			Collections.addAll(criteriaScopes, elements);
		}
		return criteriaScopes;
	}

	@Override
	public void addQTCException(QueryTooComplexException exception) throws QueryTooComplexException {
		this.searchException = exception;
	}
}
