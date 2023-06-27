/*******************************************************************************
 * Copyright (c) 2006, 2020 IBM Corporation and others.
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
 *     IBM Corporation - add support for filtering of the index view
 *     George Suaridze <suag@1c.ru> (1C-Soft LLC) - Bug 560168
 *******************************************************************************/
package org.eclipse.help.internal.index;

import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.ILog;
import org.eclipse.help.IIndexEntry;
import org.eclipse.help.ITopic;
import org.eclipse.help.IUAElement;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.Topic;
import org.eclipse.help.internal.UAElement;
import org.eclipse.help.internal.dynamic.DocumentProcessor;
import org.eclipse.help.internal.dynamic.DocumentReader;
import org.eclipse.help.internal.dynamic.ExtensionHandler;
import org.eclipse.help.internal.dynamic.IncludeHandler;
import org.eclipse.help.internal.dynamic.ProcessorHandler;
import org.eclipse.help.internal.toc.HrefUtil;


/*
 * Assembles individual keyword index contributions into a complete, fully
 * sorted master index.
 */
public class IndexAssembler {

	private DocumentProcessor processor;
	private IndexComparator comparator;
	private String locale;

	/*
	 * Assembles the given index contributions into a complete, sorted index.
	 * The originals are not modified.
	 */
	public Index assemble(List<IndexContribution> contributions, String locale) {
		this.locale = locale;
		process(contributions);
		Index index = merge(contributions);
		sortAndPrune(index);
		return index;
	}

	/*
	 * Merge all index contributions into one large index, not sorted.
	 */
	private Index merge(List<IndexContribution> contributions) {
		Index index = new Index();
		Iterator<IndexContribution> iter = contributions.iterator();
		while (iter.hasNext()) {
			IndexContribution contribution = iter.next();
			mergeChildren(index, (Index)contribution.getIndex());
			contribution.setIndex(null);
		}
		return index;
	}

	/*
	 * Merges the children of nodes a and b, and stores them into a. If the two
	 * contain the same keyword, only one is kept but its children are merged,
	 * recursively. If multiple topics exist with the same href, only the
	 * first one found is kept. If multiple see elements are found with the same target
	 * only one is retained
	 */
	private void mergeChildren(UAElement a, UAElement b) {
		// create data structures for fast lookup
		Map<String, UAElement> entriesByKeyword = new HashMap<>();
		Set<String> topicHrefs = new HashSet<>();
		Set<IndexSee> seeTargets = new HashSet<>();
		IUAElement[] childrenA = a.getChildren();
		for (int i=0;i<childrenA.length;++i) {
			UAElement childA = (UAElement)childrenA[i];
			if (childA instanceof IndexEntry) {
				entriesByKeyword.put(childA.getAttribute(IndexEntry.ATTRIBUTE_KEYWORD), childA);
			}
			else if (childA instanceof Topic) {
				topicHrefs.add(childA.getAttribute(Topic.ATTRIBUTE_HREF));
			} else if (childA instanceof IndexSee) {
				seeTargets.add((IndexSee) childA);
			}
		}

		// now do the merge
		IUAElement[] childrenB = b.getChildren();
		for (int i=0;i<childrenB.length;++i) {
			UAElement childB = (UAElement)childrenB[i];
			if (childB instanceof IndexEntry) {
				String keyword = childB.getAttribute(IndexEntry.ATTRIBUTE_KEYWORD);
				if (entriesByKeyword.containsKey(keyword)) {
					// duplicate keyword; merge children
					mergeChildren(entriesByKeyword.get(keyword), childB);
				}
				else {
					// wasn't a duplicate
					a.appendChild(childB);
					entriesByKeyword.put(keyword, childB);
				}
			}
			else if (childB instanceof Topic) {
				String href = childB.getAttribute(Topic.ATTRIBUTE_HREF);
				if (!topicHrefs.contains(href)) {
					// add topic only if href doesn't exist yet
					a.appendChild(childB);
					topicHrefs.add(href);
				}
			} else if (childB instanceof IndexSee) {
				if (!seeTargets.contains((childB))) {
					// add see only if it doesn't exist yet
					a.appendChild(childB);
					seeTargets.add((IndexSee) childB);
				}
			}
		}
	}

	private void process(List<IndexContribution> contributions) {
		if (processor == null) {
			DocumentReader reader = new DocumentReader();
			processor = new DocumentProcessor(new ProcessorHandler[] {
				new NormalizeHandler(),
				new IncludeHandler(reader, locale),
				new ExtensionHandler(reader, locale),
			});
		}
		Iterator<IndexContribution> iter = contributions.iterator();
		while (iter.hasNext()) {
			IndexContribution contribution = iter.next();
			processor.process((Index)contribution.getIndex(), contribution.getId());
		}
	}

	/*
	 * Sort the given node's descendants recursively.
	 */
	private void sortAndPrune(UAElement element) {
		if (comparator == null) {
			comparator = new IndexComparator();
		}
		sortAndPrune(element, comparator);
	}

	/*
	 * Sort the given node's descendants recursively using the given
	 * Comparator. Prune out any empty entry elements. Return true if this node was
	 * not pruned
	 */
	private boolean sortAndPrune(UAElement element, IndexComparator comparator) {
		// sort children
		IUAElement[] children = element.getChildren();
		if (children.length > 1) {
			for (int i=0;i<children.length;++i) {
				element.removeChild((UAElement)children[i]);
			}
			Arrays.sort(children, comparator);
			for (int i=0;i<children.length;++i) {
				element.appendChild((UAElement)children[i]);
			}
		}
		// sort children's children
		boolean hasChildren = false;
		for (int i=0;i<children.length;++i) {
			hasChildren = hasChildren | sortAndPrune((UAElement)children[i], comparator);
		}
		if (element instanceof IIndexEntry && !hasChildren) {
			element.getParentElement().removeChild(element);
			return false;
		}
		if (element instanceof IndexSee && !isValidSeeReference((IndexSee) element)) {
			element.getParentElement().removeChild(element);
			return false;
		}
		return true;
	}

	boolean isValidSeeReference(IndexSee see) {
		UAElement ancestor = see.getParentElement();
		while (!(ancestor instanceof Index)) {
			if (ancestor == null) {
				return true;
			}
			ancestor = ancestor.getParentElement();
		}
		return ((Index)ancestor).getSeeTarget(see) != null;
	}

	/*
	 * Normalizes topic hrefs, by prepending the plug-in id to form an href.
	 * e.g. "path/myfile.html" -> "/my.plugin/path/myfile.html"
	 */
	private static class NormalizeHandler extends ProcessorHandler {
		@Override
		public short handle(UAElement element, String id) {
			if (element instanceof Topic) {
				Topic topic = (Topic)element;
				String href = topic.getHref();
				if (href != null) {
					int index = id.indexOf('/', 1);
					if (index != -1) {
						String pluginId = id.substring(1, index);
						topic.setHref(HrefUtil.normalizeHref(pluginId, href));
					}
				}
				String title = element.getAttribute("title"); //$NON-NLS-1$
				if (title != null) {
					topic.setLabel(title);
				}
			}
			return UNHANDLED;
		}
	}

	private class IndexComparator implements Comparator<IUAElement> {
		Collator collator = Collator.getInstance();
		@Override
		public int compare(IUAElement o1, IUAElement o2) {
			/*
			 * First separate the objects into different groups by type;
			 * topics first, then entries, etc. Then within each
			 * group, sort alphabetically.
			 */
			int c1 = getCategory(o1);
			int c2 = getCategory(o2);
			if (c1 == c2) {
				if (o1 instanceof IndexSee) {
					return ((IndexSee)o1).compareTo(o2);
				}
				// same type of object; compare alphabetically
				String s1 = getLabel(o1);
				String s2 = getLabel(o2);
				//return s1.compareTo(s2);
				return collator.compare(s1, s2);
			}
			else {
				// different types; compare by type
				return c1 - c2;
			}
		}

		/*
		 * Returns the category of the node. The order is:
		 * 1. topics
		 * 2. entries starting with non-alphanumeric
		 * 3. entries starting with digit
		 * 4. entries starting with alpha
		 * 5. other
		 */
		private int getCategory(IUAElement element) {
			if (element instanceof Topic) {
				return 0;
			}
			else if (element instanceof IndexEntry) {
				String keyword = ((IndexEntry)element).getKeyword();
				if (keyword != null && keyword.length() > 0) {
					char c = keyword.charAt(0);
					if (Character.isDigit(c)) {
						return 2;
					}
					else if (Character.isLetter(c)) {
						return 3;
					}
					return 1;
				}
				return 4;
			} else if (element instanceof IndexSee) {
				return 5;
			}
			else {
				return 6;
			}
		}

		/*
		 * Returns the string that will be displayed for the given object,
		 * used for sorting.
		 */
		private String getLabel(IUAElement element) {
			if (element instanceof Topic) {
				Topic topic = (Topic)element;
				if (topic.getLabel() == null) {
					ITopic topic2 = HelpPlugin.getTocManager().getTopic(topic.getHref(), locale);
					if (topic2 != null) {
						topic.setLabel(topic2.getLabel());
					}
					else {
						String msg = "Unable to look up label for help keyword index topic \"" + topic.getHref() + "\" with missing \"" + Topic.ATTRIBUTE_LABEL + "\" attribute (topic does not exist in table of contents; using href as label)"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						ILog.of(getClass()).error(msg);
						topic.setLabel(topic.getHref());
					}
				}
				return topic.getLabel();
			}
			else if (element instanceof IndexEntry) {
				return ((IndexEntry)element).getKeyword();
			}
			return null;
		}
	}
}
