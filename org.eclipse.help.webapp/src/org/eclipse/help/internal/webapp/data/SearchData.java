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
 *     Sebastian Davids <sdavids@gmx.de> - fix for Bug 182466
 *     Holger Voormann - fix for bug 365549 (http://eclip.se/365549)
 *     Holger Voormann - fix for bug 364324 (http://eclip.se/364324)
 *     Alex Blewitt - Bug 474070
 *     George Suaridze <suag@1c.ru> (1C-Soft LLC) - Bug 560168
 *******************************************************************************/
package org.eclipse.help.internal.webapp.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.core.runtime.Platform;
import org.eclipse.help.HelpSystem;
import org.eclipse.help.IHelpResource;
import org.eclipse.help.IToc;
import org.eclipse.help.ITopic;
import org.eclipse.help.base.AbstractHelpScope;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.base.HelpBasePlugin;
import org.eclipse.help.internal.base.IHelpBaseConstants;
import org.eclipse.help.internal.search.ISearchQuery;
import org.eclipse.help.internal.search.QueryTooComplexException;
import org.eclipse.help.internal.search.SearchHit;
import org.eclipse.help.internal.search.SearchManager;
import org.eclipse.help.internal.search.SearchProgressMonitor;
import org.eclipse.help.internal.search.SearchQuery;
import org.eclipse.help.internal.search.SearchResults;
import org.eclipse.help.internal.util.ProductPreferences;
import org.eclipse.help.internal.webapp.servlet.WebappWorkingSetManager;
import org.eclipse.help.internal.workingset.AdaptableHelpResource;
import org.eclipse.help.internal.workingset.AdaptableSelectedToc;
import org.eclipse.help.internal.workingset.AdaptableSelectedTopic;
import org.eclipse.help.internal.workingset.AdaptableToc;
import org.eclipse.help.internal.workingset.AdaptableTopic;
import org.eclipse.help.internal.workingset.WorkingSet;
import org.eclipse.help.search.AbstractSearchProcessor;
import org.eclipse.help.search.ISearchEngineResult;
import org.eclipse.help.search.ISearchResult;
import org.eclipse.help.search.SearchProcessorInfo;
import org.eclipse.osgi.util.NLS;

/**
 * Helper class for searchView.jsp initialization
 */
public class SearchData extends ActivitiesData {
	private static final String SHOW_CATEGORIES = "showSearchCategories"; //$NON-NLS-1$

	private WebappWorkingSetManager wsmgr;

	// Request parameters
	private String topicHref;

	private String selectedTopicId = ""; //$NON-NLS-1$

	private String searchWord;

	private String workingSetName;

	// search results
	SearchHit[] hits;

	// percentage of indexing completion
	private int indexCompletion = 100;

	// QueryException if any
	private QueryTooComplexException queryException = null;

	// List of alternate search terms
	private List<String> altList = new ArrayList<>();

	private boolean showCategories = false;

	/**
	 * Constructs the xml data for the search results page.
	 *
	 * @param context
	 * @param request
	 */
	public SearchData(ServletContext context, HttpServletRequest request,
			HttpServletResponse response) {
		super(context, request, response);
		wsmgr = new WebappWorkingSetManager(request, response, getLocale());
		this.topicHref = request.getParameter("topic"); //$NON-NLS-1$
		if (topicHref != null && topicHref.length() == 0)
			topicHref = null;

		searchWord = request.getParameter("searchWord"); //$NON-NLS-1$
		readDisplayFlags(request, response);

		if (isScopeRequest()) {
			workingSetName = request.getParameter("workingSet"); //$NON-NLS-1$
			if ( canSaveScope() ) {
				saveWorkingSet(workingSetName);
			}
		}

		// try loading search results or get the indexing progress info.
		readSearchResults();
	}

	protected boolean canSaveScope() {
		// Scope is only saved from scopeState.jsp
		// This prevents cookies from being saved with a /advanced path
		return false;
	}

	private void readDisplayFlags(HttpServletRequest request, HttpServletResponse response) {
		String showCategoriesParam = request.getParameter(SHOW_CATEGORIES);
		if (showCategoriesParam != null) {
			showCategories = "true".equalsIgnoreCase(showCategoriesParam); //$NON-NLS-1$
			RequestScope.setFlag(request, response, SHOW_CATEGORIES, showCategories);
		} else {
			showCategories = RequestScope.getFlag(request, SHOW_CATEGORIES);
		}
	}

	public void readSearchResults() {

		// try loading search results or get the indexing progress info.
		if (isSearchRequest() && !isScopeRequest()) {

			altList.clear();

			AbstractSearchProcessor processors[] = SearchManager.getSearchProcessors();
			for (AbstractSearchProcessor processor : processors) {
				SearchProcessorInfo result =
					processor.preSearch(searchWord);
				if (result!=null)
				{
					String alternates[] = result.getAlternateTerms();
					if (alternates!=null)
					{
						for (String alternate : alternates) {
							String div =
									"<div><a target=\"_self\" href=\"./searchView.jsp?searchWord="+alternate+"\">"+ //$NON-NLS-1$ //$NON-NLS-2$
									alternate+
									"</a></div>"; //$NON-NLS-1$

							if (!altList.contains(div))
								altList.add(div);
						}
					}
					String query = result.getQuery();
					if (query!=null)
						searchWord = query;
				}
			}
			altList.sort(null);

			loadSearchResults();
			if (queryException != null) {
				return;
			}
			if (!isProgressRequest()) {
				for (int i = 0; i < hits.length; i++) {
					// the following assume topic numbering as in searchView.jsp
					if (hits[i].getHref().equals(topicHref)) {
						selectedTopicId = "a" + i; //$NON-NLS-1$
						break;
					}
				}

				ISearchResult results[] = SearchManager.convertHitsToResults(hits);
				boolean reset= false;
				for (AbstractSearchProcessor processor : processors) {
					ISearchResult tmp[] = processor.postSearch(searchWord,results);
					if (tmp!=null)
					{
						reset = true;
						results = tmp;
					}
				}
				if (reset)
					hits = SearchManager.convertResultsToHits(results);
				if (isShowCategories())
					primallySortByCategory(hits);
			}
		}
	}

	/**
	 * Returns true when there is a search request
	 *
	 * @return boolean
	 */
	public boolean isSearchRequest() {
		String searchWordParam = request.getParameter("searchWord"); //$NON-NLS-1$
		return (searchWordParam != null && searchWordParam.length() > 0);
	}

	/**
	 * Return indexed completion percentage
	 */
	public boolean isProgressRequest() {
		return (hits == null && indexCompletion != 100);
	}

	/**
	 * Returns true when there is a request to change the scope (working set)
	 */
	public boolean isScopeRequest() {
		return (request.getParameter("workingSet") != null); //$NON-NLS-1$
	}

	protected boolean isQuickSearch() {
		return request.getParameterValues("quickSearch") != null; //$NON-NLS-1$
	}

	public boolean isSelectedTopicQuickSearchRequest() {
		String quickSearchType = request.getParameter("quickSearchType"); //$NON-NLS-1$
		return (null != quickSearchType && "QuickSearchTopic".equalsIgnoreCase(quickSearchType)); //$NON-NLS-1$
	}

	public String getCategoryLabel(int i) {
		IHelpResource cat = hits[i].getCategory();
		if (cat != null) {
			return cat.getLabel();
		}
		return null;
	}

	public String getCategoryHref(int i) {
		IHelpResource cat = hits[i].getCategory();
		if (cat != null) {
			String tocHref = cat.getHref();
			IToc[] tocs = HelpSystem.getTocs();
			for (int j=0;j<tocs.length;++j) {
				if (tocHref.equals(tocs[j].getHref())) {
					ITopic topic = tocs[j].getTopic(null);
					String topicHref = topic.getHref();
					if (topicHref != null) {
						return UrlUtil.getHelpURL(topicHref);
					}
					return "../nav/" + j; //$NON-NLS-1$
				}
			}
		}
		return null;
	}

	/**
	 * Return the number of links
	 *
	 * @return int
	 */
	public int getResultsCount() {
		return hits.length;
	}

	public SearchHit[] getResults() {
		return hits;
	}

	public String getSelectedTopicId() {
		return selectedTopicId;
	}

	public String getTopicHref(int i) {
		return UrlUtil.getHelpURL(hits[i].getHref());
	}

	public String getTopicLabel(int i) {
		return UrlUtil.htmlEncode(hits[i].getLabel());
	}

	public String getTopicTocLabel(int i) {
		if (hits[i].getToc() != null)
			return UrlUtil.htmlEncode(hits[i].getToc().getLabel());
		return ""; //$NON-NLS-1$
	}

	public String getTopicDescription(int i) {
		String description = hits[i].getDescription();
		if (description != null) {
			return UrlUtil.htmlEncode(description);
		}
		return ""; //$NON-NLS-1$
	}

	/**
	 * @param i
	 * @return true of result belong to an enabled TOC
	 */
	public boolean isEnabled(int i) {
		String href = hits[i].getHref();
		return HelpBasePlugin.getActivitySupport().isEnabledTopic(href,
				getLocale());
	}

	/**
	 * Returns whether or not the ith hit is a potential hit. This means
	 * it may not be an actual hit (i.e. it found something in a filtered
	 * section of the document).
	 *
	 * @param i the index of the hit to check
	 * @return whether or not the hit is a potential hit
	 */
	public boolean isPotentialHit(int i) {
		return ((getMode() != MODE_INFOCENTER) && hits[i].isPotentialHit());
	}

	public boolean isShowCategories() {
		return showCategories;
	}

	public boolean isShowLocations() {
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
				for (int i=0;i<cookies.length;++i) {
					if ("showLocations".equals(cookies[i].getName())) { //$NON-NLS-1$
						return String.valueOf(true).equals(cookies[i].getValue());
					}
				}
		}
		// get default from preferences
		return Platform.getPreferencesService().getBoolean
				(HelpBasePlugin.PLUGIN_ID, IHelpBaseConstants.P_KEY_SHOW_SEARCH_LOCATION, true, null);
	}

	public boolean isShowDescriptions() {
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
				for (Cookie cookie : cookies) {
					if ("showDescriptions".equals(cookie.getName())) { //$NON-NLS-1$
						return String.valueOf(true).equals(cookie.getValue());
					}
				}
		}
		// get default from preferences
		return Platform.getPreferencesService().getBoolean
				(HelpBasePlugin.PLUGIN_ID, IHelpBaseConstants.P_KEY_SHOW_SEARCH_DESCRIPTION, true, null);
	}

	/**
	 * Return indexed completion percentage
	 */
	public String getIndexedPercentage() {
		return String.valueOf(indexCompletion);
	}

	/**
	 * Returns the search query
	 */
	public String getSearchWord() {
		if (searchWord == null)
			return ""; //$NON-NLS-1$
		return searchWord;
	}

	/**
	 * Returns the list of selected TOC's
	 */
	public String[] getSelectedTocs() {
		String[] books = request.getParameterValues("scope"); //$NON-NLS-1$
		if (books == null) {
			// select all books
			TocData tocData = new TocData(context, request, response);
			books = new String[tocData.getTocCount()];
			for (int i = 0; i < books.length; i++)
				books[i] = tocData.getTocHref(i);
		}
		return books;
	}

	/**
	 * Returns true if book is within a search scope
	 */
	public boolean isTocSelected(int toc) {
		TocData tocData = new TocData(context, request, response);
		String href = tocData.getTocHref(toc);
		String[] books = request.getParameterValues("scope"); //$NON-NLS-1$
		if (books == null)
			return false;
		for (String book : books) {
			if (book.equals(href)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the working set selected. This is used to display the working set
	 * name in the search banner.
	 *
	 * @return String
	 */
	public String getScope() {
		if (workingSetName != null && workingSetName.length() != 0)
			return workingSetName;

		if (isSearchRequest()) {
			workingSetName = request.getParameter("scope"); //$NON-NLS-1$
			// if we have already set the working set, then use it.
			if (workingSetName == null) {
				workingSetName = request.getParameter("workingSet"); //$NON-NLS-1$
			}
		} else {
			workingSetName = wsmgr.getCurrentWorkingSet();
		}

		if (workingSetName == null || workingSetName.length() == 0
				|| getMode() == RequestData.MODE_INFOCENTER
				&& wsmgr.getWorkingSet(workingSetName) == null)
			workingSetName = ServletResources.getString("All", request); //$NON-NLS-1$
		return workingSetName;
	}

	private void saveWorkingSet(String workingSet) {
		String lastWS = wsmgr.getCurrentWorkingSet();
		if (workingSet != null && !workingSet.equals(lastWS)) {
			wsmgr.setCurrentWorkingSet(workingSet);
		} else if (workingSet == null && lastWS != null && lastWS.length() > 0) {
			wsmgr.setCurrentWorkingSet(""); //$NON-NLS-1$
		}
	}

	/**
	 * Call the search engine, and get results or the percentage of indexed
	 * documents.
	 */
	private void loadSearchResults() {
		try {
			SearchProgressMonitor pm = SearchProgressMonitor
					.getProgressMonitor(getLocale());
			if (pm.isDone()) {
				this.indexCompletion = 100;
				SearchResults results = createHitCollector();
				BaseHelpSystem.getSearchManager().search(createSearchQuery(),
						results, pm);
				hits = results.getSearchHits();
				if (hits == null) {
					Platform.getLog(getClass()).warn("No search results returned.  Help index is in use."); //$NON-NLS-1$
				}
				return;
			}
			// progress
			indexCompletion = pm.getPercentage();
			if (indexCompletion >= 100) {
				// 38573 We do not have results, so index cannot be 100
				indexCompletion = 100 - 1;
			}
			return;
		} catch (QueryTooComplexException qe) {
			queryException = qe;
		} catch (Exception e) {
			this.indexCompletion = 0;
		}

	}

	private ISearchQuery createSearchQuery() {
		String fieldSearchStr = request.getParameter("fieldSearch"); //$NON-NLS-1$
		boolean fieldSearch = fieldSearchStr != null ? Boolean.parseBoolean(
				fieldSearchStr) : false;
		return new SearchQuery(searchWord == null ? "" : searchWord, fieldSearch, new ArrayList<>(), //$NON-NLS-1$
				getLocale());
	}

	private SearchResults createHitCollector() {
		WorkingSet[] workingSets;
		boolean isSearchSelectedAndChildren = false;
		if (request.getParameterValues("scopedSearch") != null) { //$NON-NLS-1$
			// scopes are books (advanced search)
			workingSets = createTempWorkingSets();
		} else if (isQuickSearch()) {
			// scopes is just the selected toc or topic
			if(isSelectedTopicQuickSearchRequest()){
				workingSets = createQuickSearchWorkingSetOnSelectedTopic();
			} else{  // scopes is a toc or topic and its children
				workingSets = createQuickSearchWorkingSet();
				isSearchSelectedAndChildren = true;
			}
		} else {
			// scopes are working set names
			workingSets = getWorkingSets();
		}

		AbstractHelpScope filter = RequestScope.getScope(request, response, !isSearchSelectedAndChildren);
		int maxHits = 500;
		String maxHitsStr = request.getParameter("maxHits"); //$NON-NLS-1$
		if (maxHitsStr != null) {
			try {
				int clientmaxHits = Integer.parseInt(maxHitsStr);
				if (0 < clientmaxHits && clientmaxHits < 500) {
					maxHits = clientmaxHits;
				}
			} catch (NumberFormatException nfe) {
			}
		}
		return new SearchResultFilter(workingSets, maxHits, getLocale(), filter, isQuickSearch());
	}

	/**
	 * @return WorkingSet[] or null
	 */
	private WorkingSet[] getWorkingSets() {
		String[] scopes = request.getParameterValues("scope"); //$NON-NLS-1$
		if (scopes == null) {
			return null;
		}
		// confirm working set exists and use it
		ArrayList<WorkingSet> workingSetCol = new ArrayList<>(scopes.length);
		for (String scope : scopes) {
			WorkingSet ws = wsmgr.getWorkingSet(scope);
			if (ws != null) {
				workingSetCol.add(ws);
			}
		}
		if (workingSetCol.isEmpty()) {
			return null;
		}
		return workingSetCol
				.toArray(new WorkingSet[workingSetCol.size()]);
	}

	/**
	 * @return WorkingSet[] or null
	 */
	private WorkingSet[] createTempWorkingSets() {
		String[] scopes = request.getParameterValues("scope"); //$NON-NLS-1$
		if (scopes == null) {
			// it is possible that filtering is used, but all books are
			// deselected
			return new WorkingSet[0];
		}
		if (scopes.length == HelpPlugin.getTocManager().getTocs(getLocale()).length) {
			// do not filter if all books are selected
			return null;
		}
		// create working set from books
		ArrayList<AdaptableToc> tocs = new ArrayList<>(scopes.length);
		for (String scope : scopes) {
			AdaptableToc toc = wsmgr.getAdaptableToc(scope);
			if (toc != null) {
				tocs.add(toc);
			}
		}
		AdaptableToc[] adaptableTocs = tocs
				.toArray(new AdaptableToc[tocs.size()]);
		WorkingSet[] workingSets = new WorkingSet[1];
		workingSets[0] = wsmgr.createWorkingSet("temp", adaptableTocs); //$NON-NLS-1$
		return workingSets;
	}

	/**
	 * @return WorkingSet[] consisting of a single toc or topic or null
	 */
	private WorkingSet[] createQuickSearchWorkingSet() {
		WorkingSet[] workingSets = new WorkingSet[1];
		TocData tocData = new TocData(context, request, response);
		int selectedToc = tocData.getSelectedToc();
		if (selectedToc < 0) {
			return new WorkingSet[0];
		}
		IToc toc = tocData.getTocs()[selectedToc];
		ITopic[] topics = tocData.getTopicPathFromRootPath(toc);
		List<AdaptableHelpResource> resources = new ArrayList<>();
		AdaptableToc adaptableToc = new AdaptableToc(toc);
		if (topics != null) {
			ITopic selectedTopic = topics[topics.length - 1];
			AdaptableTopic adaptableTopic = new AdaptableTopic(selectedTopic);
			resources.add(adaptableTopic);
			adaptableTopic.setParent(adaptableToc);
		} else {
			resources.add(adaptableToc);
		}
		workingSets[0] = new WorkingSet("quickSearch", resources); //$NON-NLS-1$
		return workingSets;
	}

	/**
	 * @return WorkingSet[] consisting of a single selected toc or topic or null
	 */
	private WorkingSet[] createQuickSearchWorkingSetOnSelectedTopic() {
		WorkingSet[] workingSets = new WorkingSet[1];
		TocData tocData = new TocData(context, request, response);
		int selectedToc = tocData.getSelectedToc();
		if (selectedToc < 0) {
			return new WorkingSet[0];
		}
		IToc toc = tocData.getTocs()[selectedToc];
		ITopic[] topics = tocData.getTopicPathFromRootPath(toc);
		List<AdaptableHelpResource> resources = new ArrayList<>();
		AdaptableSelectedToc adaptableSelectedToc = new AdaptableSelectedToc(toc);
		if (topics != null) {
			ITopic selectedTopic = topics[topics.length - 1];
			AdaptableSelectedTopic adaptableSelectedTopic = new AdaptableSelectedTopic(selectedTopic);
			resources.add(adaptableSelectedTopic);
			adaptableSelectedTopic.setParent(adaptableSelectedToc);
		} else {
			resources.add(adaptableSelectedToc);
		}
		workingSets[0] = new WorkingSet("quickSearch", resources); //$NON-NLS-1$
		return workingSets;
	}

	public String getQueryExceptionMessage() {
		if (queryException == null) {
			return null;
		}
		return ServletResources.getString("searchTooComplex", request); //$NON-NLS-1$
	}

	public boolean isScopeActive() {
		return ! getScope().equals(ServletResources.getString("All", request)); //$NON-NLS-1$
	}

	public String getNotFoundMessage() {
		String scope = getScope();
		if (scope.equals(ServletResources.getString("All", request))) { //$NON-NLS-1$
			return ServletResources.getString("Nothing_found", request); //$NON-NLS-1$
		} else {
			return NLS.bind(ServletResources.getString("Nothing_found_in_scope", request), scope); //$NON-NLS-1$
		}
	}

	public String getScopeActiveMessage() {
		String scope = getScope();
		return NLS.bind(ServletResources.getString("activeScope", request), scope); //$NON-NLS-1$
	}

	public String getMatchesInScopeMessage() {
		String scope = getScope();
		return NLS.bind(ServletResources.getString("matchesInScope", request), "" + getResultsCount(), scope); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public String getPreProcessorResults()
	{
		if (altList==null || altList.isEmpty())
			return ""; //$NON-NLS-1$

		StringBuilder result = new StringBuilder();

		result.append(ServletResources.getString("AlternateSearchQueries", request)); //$NON-NLS-1$
		result.append("<ul>"); //$NON-NLS-1$
		for (int a=0;a<altList.size();a++)
			result.append("<li>"+altList.get(a)+"</li>"); //$NON-NLS-1$ //$NON-NLS-2$
		result.append("</ul>"); //$NON-NLS-1$

		return result.toString();
	}

	/*
	 * Filters out results that help doesn't know how to open (i.e. those hits
	 * that implement ISearchEngineResult2 and canOpen() returns true.
	 */
	private static class SearchResultFilter extends SearchResults {
		public SearchResultFilter(WorkingSet[] workingSets, int maxHits, String locale,
				AbstractHelpScope filter, boolean isQuickSearch) {
			super(workingSets, maxHits, locale, isQuickSearch);
			setFilter(filter);
		}
		@Override
		public void addHits(List<SearchHit> hits, String highlightTerms) {
			List<SearchHit> filtered = new ArrayList<>();
			Iterator<SearchHit> iter = hits.iterator();
			while (iter.hasNext()) {
				SearchHit obj = iter.next();
				if (!(obj != null && obj.canOpen())) {
					filtered.add(obj);
				}
			}
			super.addHits(filtered, highlightTerms);
		}
	}

	/**
	 * Sorts the given {@link ISearchEngineResult} array alphabetically (case
	 * insensitive) by category label but keeps the order within each category.
	 * Results without a category or of a category without a label or with an
	 * empty label are sorted to the end ({@code "Category Label" < "" < null}).
	 *
	 * @param toSort the {@link ISearchEngineResult} array to sort; must not
	 *               contain {@code null} elements
	 */
	private static void primallySortByCategory(ISearchEngineResult[] toSort) {

		// compute TOC ordering
		Map<String, String> nameIdMap = new HashMap<>();
		List<String> itemsToOrder = new ArrayList<>();
		for (int i = 0; i < toSort.length; i++) {
			IHelpResource cat = toSort[i].getCategory();
			if (cat == null) continue;
			String href = cat.getHref();
			String label = cat.getLabel();
			if (href != null && label != null) {
				nameIdMap.put(href, label);
				if (!itemsToOrder.contains(href)) {
					itemsToOrder.add(href);
				}
			}
		}
		List<String> order = ProductPreferences.getTocOrder(itemsToOrder, nameIdMap);
		final Map<Object, Integer> catOrder = new HashMap<>();
		for (int i = 0; i < order.size(); i++) {
			catOrder.put(order.get(i), Integer.valueOf(i));
		}

		Arrays.sort(toSort, (hit1, hit2) -> {
			IHelpResource c1 = hit1.getCategory();
			IHelpResource c2 = hit2.getCategory();
			if (c1 == null && c2 == null) return 0;
			if (c1 == null) return 1;
			if (c2 == null) return -1;

			// ordering after TOC ordering
			if (catOrder.containsKey(c1.getHref()) && catOrder.containsKey(c2.getHref()))
				return catOrder.get(c1.getHref()).intValue() - catOrder.get(c2.getHref()).intValue();

			// alphabetical ordering by category label
			String l1 = c1.getLabel();
			String l2 = c2.getLabel();
			if (l1 == null && l2 == null) return 0;
			if (l1 == null) return 1;
			if (l2 == null) return -1;
			if (l1.length() == 0 && l2.length() == 0) return 0;
			if (l1.length() == 0) return 1;
			if (l2.length() == 0) return -1;
			return l1.compareToIgnoreCase(l2);
		});
	}

}
