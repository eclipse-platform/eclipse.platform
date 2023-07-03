/*******************************************************************************
 * Copyright (c) 2007, 2017 IBM Corporation and others.
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
package org.eclipse.help.internal.webapp.servlet;

import java.io.IOException;
import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.help.IIndex;
import org.eclipse.help.IIndexEntry;
import org.eclipse.help.IIndexEntry2;
import org.eclipse.help.IIndexSee;
import org.eclipse.help.IIndexSubpath;
import org.eclipse.help.ITopic;
import org.eclipse.help.base.AbstractHelpScope;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.base.scope.ScopeUtils;
import org.eclipse.help.internal.webapp.WebappResources;
import org.eclipse.help.internal.webapp.data.ActivitiesData;
import org.eclipse.help.internal.webapp.data.RequestScope;
import org.eclipse.help.internal.webapp.data.UrlUtil;
import org.eclipse.osgi.util.NLS;

/*
 * Creates xml representing selected parts of the index
 * Parameter "start" represents the part of the index to start reading from
 * Parameter "size" indicates the number of entries to read, no size parameter
 * or a negative size parameter indicates that all entries which match the start
 * letters should be displayed.
 * Parameter "offset" represents the starting point relative to the start
 */
public class IndexFragmentServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static Map<String, String> locale2Response = new WeakHashMap<>();
	private String startParameter;
	private String sizeParameter;
	private String entryParameter;
	private String modeParameter;
	private String showAllParameter;
	private int size;
	private int entry;
	private static final String NEXT = "next"; //$NON-NLS-1$
	private static final String PREVIOUS = "previous"; //$NON-NLS-1$
	private static final String SIZE = "size"; //$NON-NLS-1$
	private static final String MODE = "mode"; //$NON-NLS-1$
	private static final String ENTRY = "entry"; //$NON-NLS-1$
	private static final String SHOW_ALL = "showAll"; //$NON-NLS-1$
	private Collator collator = Collator.getInstance();

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		// set the character-set to UTF-8 before calling resp.getWriter()
		resp.setContentType("application/xml; charset=UTF-8"); //$NON-NLS-1$
		resp.getWriter().write(processRequest(req, resp));
	}

	protected String processRequest(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String locale = UrlUtil.getLocale(req, resp);
		startParameter = req.getParameter("start"); //$NON-NLS-1$
		if (startParameter != null) {
			startParameter = startParameter.toLowerCase();
		}

		size = 30;
		sizeParameter = req.getParameter(SIZE);
		if (sizeParameter != null) {
			try {
				size = Integer.parseInt(sizeParameter);
			} catch (NumberFormatException n) {
			}
		}

		entry = -1;
		entryParameter = req.getParameter(ENTRY);
		if (entryParameter != null) {
			try {
				entry = Integer.parseInt(entryParameter);
			} catch (NumberFormatException n) {
			}
		}

		modeParameter = req.getParameter(MODE);
		showAllParameter = req.getParameter(SHOW_ALL);
		if (showAllParameter != null) {
			// Use activities data to toggle the show all state
			new ActivitiesData(this.getServletContext(), req, resp);
		}

		req.setCharacterEncoding("UTF-8"); //$NON-NLS-1$
		// Cache suppression required because the set of in scope
		// topics could change between requests

		resp.setHeader("Cache-Control","no-cache");   //$NON-NLS-1$//$NON-NLS-2$
		resp.setHeader("Pragma","no-cache");  //$NON-NLS-1$ //$NON-NLS-2$
		resp.setDateHeader ("Expires", 0); 	 //$NON-NLS-1$

		AbstractHelpScope scope = RequestScope.getScope(req, resp, false);
		Serializer serializer = new Serializer(locale, scope);
		String response = serializer.generateIndexXml();
		locale2Response.put(locale, response);

		return response;
	}

	/*
	 * Class which creates the xml file based upon the request parameters
	 */
	private class Serializer {

		private IIndex index;
		private StringBuilder buf;
		private int count = 0;
		private String locale;
		private List<Integer> entryList;
		private IIndexEntry[] entries;
		private boolean enablePrevious = true;
		private boolean enableNext = true;
		private AbstractHelpScope scope;

		public Serializer(String locale, AbstractHelpScope scope) {
			this.locale = locale;
			this.scope = scope;
			index = HelpPlugin.getIndexManager().getIndex(locale);
			buf = new StringBuilder();
		}

		/*
		 * There are three modes of generation, current page, next page and previous page.
		 * Current page returns a screenful of entries starting at the startParameter.
		 * Next page returns a screenful of entries starting after but not including the start parameter.
		 * Previous page returns a screenful of entries going back from the start parameter
		 */
		private String generateIndexXml() {

			entries = index.getEntries();
			if (entries.length == 0) {
				generateEmptyIndexMessage();
			} else {
				entryList = new ArrayList<>();
				int nextEntry = findFirstEntry(entries);
				if (PREVIOUS.equals(modeParameter)) {
					int remaining = getPreviousEntries(nextEntry, size);
					getNextEntries(nextEntry, remaining);
				} else {
					int remaining = getNextEntries(nextEntry, size);
					if (remaining == size) {
						// Generate just the last entry
						size = 1;
						getPreviousEntries(nextEntry, 1);
					}
				}
				for (Integer entryId : entryList) {
					generateEntry(entries[entryId.intValue()], 0, "e" + entryId.intValue()); //$NON-NLS-1$
				}
			}
			String header = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<tree_data enableNext = \"" //$NON-NLS-1$
				+ Boolean.toString(enableNext) + "\" enablePrevious = \"" + Boolean.toString(enablePrevious) + "\">\n"; //$NON-NLS-1$ //$NON-NLS-2$
			buf.append("</tree_data>\n"); //$NON-NLS-1$
			return header + buf.toString();
		}

		private int getCategory(String keyword) {
			if (keyword != null && keyword.length() > 0) {
				char c = keyword.charAt(0);
				if (Character.isDigit(c)) {
					return 2;
				} else if (Character.isLetter(c)) {
					return 3;
				}
				return 1;
			}
			return 4;
		}

		private int compare (String left, String right) {
			int catLeft = getCategory(left);
			int catRight = getCategory(right);
			if (catLeft != catRight) {
				return catLeft - catRight;
			} else {
				return collator.compare(left, right);
			}
		}

		private int findFirstEntry(IIndexEntry[] entries) {
			if (NEXT.equals(modeParameter)) {
				if (entry >= entries.length - 1) {
					return entries.length - 1;
				} else {
					return entry + 1;
				}
			}
			if (PREVIOUS.equals(modeParameter)) {
				if (entry <= 0) {
					return 0;
				} else {
					return entry - 1;
				}
			}
			if (startParameter == null) {
				return 0;
			}
			int nextEntry = 0;
			while (nextEntry < entries.length) {
				String keyword = entries[nextEntry].getKeyword().toLowerCase();
				if (keyword != null) {
					if (compare(startParameter, keyword) <= 0) {
						break;
					}
				}
				nextEntry++;
			}
			return nextEntry;
		}

		private int getNextEntries(int nextEntry, int remaining) {
			while (nextEntry < entries.length) {
				int entrySize = enabledEntryCount(entries[nextEntry]);
				if (remaining == size || remaining > entrySize) {
					entryList.add(Integer.valueOf(nextEntry));
					setFlags(nextEntry);
					remaining -= entrySize;
				} else {
					break;
				}
				nextEntry++;
			}
			return remaining;
		}

		private int getPreviousEntries(int nextEntry, int remaining) {
			nextEntry--;
			while (nextEntry >= 0) {
				int entrySize = enabledEntryCount(entries[nextEntry]);
				if (remaining == size || remaining > entrySize) {
					entryList.add(0, Integer.valueOf(nextEntry));

					setFlags(nextEntry);
					remaining -= entrySize;
				} else {
					break;
				}
				nextEntry--;
			}
			return remaining;
		}

		private void setFlags(int nextEntry) {
			if (nextEntry == 0) {
				enablePrevious = false;
			}
			if (nextEntry == entries.length - 1) {
				enableNext = false;
			}
		}

		private int enabledEntryCount(IIndexEntry entry) {
			if (!ScopeUtils.showInTree(entry, scope)) return 0;
			if (entry.getKeyword() == null || entry.getKeyword().length() == 0) {
				return 0;
			}
			int count = 1;
			int topicCount = enabledTopicCount(entry);

			IIndexEntry[] subentries = entry.getSubentries();
			int subentryCount = 0;
			for (IIndexEntry subentrie : subentries) {
				count += enabledEntryCount(subentrie);
			}

			int seeCount = 0;
			IIndexSee[] sees = entry instanceof IIndexEntry2 entry2 ? entry2.getSees() : new IIndexSee[0];
			for (IIndexSee see : sees) {
				if (ScopeUtils.showInTree(see, scope)) {
					seeCount++;
				}
			}

			if (topicCount + subentryCount + seeCount > 1) {
				count += topicCount;
			}
			count += subentryCount;
			count += seeCount;
			return count;
		}

		private int enabledTopicCount(IIndexEntry entry) {
			int topicCount = 0;
			ITopic[] topics = entry.getTopics();
			for (ITopic topic : topics) {
				if (scope.inScope(topic)) {
					topicCount++;
				}
			}
			return topicCount;
		}

		private void generateEmptyIndexMessage() {
			buf.append("<node"); //$NON-NLS-1$
			buf.append('\n' + "      title=\"" + XMLGenerator.xmlEscape(WebappResources.getString("IndexEmpty", UrlUtil.getLocale(locale))) + '"'); //$NON-NLS-1$ //$NON-NLS-2$
			buf.append('\n' + "      id=\"no_index\""); //$NON-NLS-1$
			buf.append(">\n"); //$NON-NLS-1$
			buf.append("</node>\n"); //$NON-NLS-1$
			enableNext = false;
			enablePrevious = false;
		}

		private void generateEntry(IIndexEntry entry, int level, String id) {
			if (!ScopeUtils.showInTree(entry, scope)) return;
			if (entry.getKeyword() != null && entry.getKeyword().length() > 0) {
				ITopic[] topics = ScopeUtils.inScopeTopics(entry.getTopics(), scope);
				IIndexEntry[] subentries = ScopeUtils.inScopeEntries(entry.getSubentries(), scope);
				IIndexSee[] sees;
				if (entry instanceof IIndexEntry2 entry2) {
					sees = entry2.getSees();
				} else {
					sees = new IIndexSee[0];
				}
				boolean multipleTopics = topics.length > 1;
				boolean singleTopic = topics.length == 1;

				buf.append("<node"); //$NON-NLS-1$
				if (entry.getKeyword() != null) {
					buf.append('\n' + "      title=\"" + XMLGenerator.xmlEscape(entry.getKeyword()) + '"'); //$NON-NLS-1$
				}

				buf.append('\n' + "      id=\"" + id + '"'); //$NON-NLS-1$

				String href;
				if (singleTopic) {
					href = UrlUtil.getHelpURL((topics[0]).getHref());
					buf.append('\n' + "      href=\"" +  //$NON-NLS-1$
						XMLGenerator.xmlEscape(href) + "\""); //$NON-NLS-1$
				}
				buf.append(">\n"); //$NON-NLS-1$

				if (multipleTopics || subentries.length > 0 || sees.length > 0) {
					if (multipleTopics) generateTopicList(entry);
					generateSubentries(entry, level + 1);
					generateSees(sees);
				}

				buf.append("</node>\n"); //$NON-NLS-1$
			}
		}

		private void generateSubentries(IIndexEntry entry, int level) {
			IIndexEntry[] subentries = entry.getSubentries();
			for (IIndexEntry subentrie : subentries) {
				generateEntry(subentrie, level, "s" + count++); //$NON-NLS-1$
			}
		}

		private void generateTopicList(IIndexEntry entry) {
			ITopic[] topics = entry.getTopics();

			for (ITopic topic : topics) {
				if (ScopeUtils.showInTree(topic, scope)) {
					//
					String label = UrlUtil.htmlEncode(topic.getLabel());
					if (label == null) {
						label = UrlUtil.htmlEncode(topic.getLabel());
					}

					buf.append("<node"); //$NON-NLS-1$
					if (entry.getKeyword() != null) {
						buf.append('\n' + "      title=\"" + label + '"'); //$NON-NLS-1$
					}

					count++;
					buf.append('\n' + "      id=\"i" + count + '"'); //$NON-NLS-1$
					String href = UrlUtil.getHelpURL(topic.getHref());
					buf.append('\n' + "      href=\"" //$NON-NLS-1$
							+ XMLGenerator.xmlEscape(href) + "\""); //$NON-NLS-1$
					buf.append(">\n"); //$NON-NLS-1$
					buf.append("</node>\n"); //$NON-NLS-1$
				}
			}
		}

		private void generateSees(IIndexSee[] sees) {
			for (IIndexSee see : sees) {
				if (ScopeUtils.showInTree(see, scope)) {
					//
					String key = see.isSeeAlso() ? "SeeAlso" : "See"; //$NON-NLS-1$ //$NON-NLS-2$
					String seePrefix = WebappResources.getString(key, UrlUtil
							.getLocale(locale));
					String seeTarget = see.getKeyword();
					IIndexSubpath[] subpathElements = see.getSubpathElements();
					for (IIndexSubpath subpathElement : subpathElements) {
						seeTarget += ", "; //$NON-NLS-1$
						seeTarget += subpathElement.getKeyword();
					}
					String label = NLS.bind(seePrefix, seeTarget);
					String encodedLabel = UrlUtil.htmlEncode(label);
					buf.append("<node"); //$NON-NLS-1$

					buf.append('\n' + "      title=\"" + encodedLabel + '"'); //$NON-NLS-1$

					count++;
					buf.append('\n' + "      id=\"i" + count + '"'); //$NON-NLS-1$
					String href = "see:" + seeTarget; //$NON-NLS-1$
					buf.append('\n' + "      href=\"" //$NON-NLS-1$
							+ XMLGenerator.xmlEscape(href) + "\""); //$NON-NLS-1$
					buf.append(">\n"); //$NON-NLS-1$
					buf.append("</node>\n"); //$NON-NLS-1$
				}
			}
		}
	}

}
