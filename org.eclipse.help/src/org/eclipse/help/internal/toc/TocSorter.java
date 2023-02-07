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
 *     George Suaridze <suag@1c.ru> (1C-Soft LLC) - Bug 560168
 *******************************************************************************/

package org.eclipse.help.internal.toc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Platform;
import org.eclipse.help.ITocContribution;
import org.eclipse.help.internal.util.ProductPreferences;

public class TocSorter {

	/*
	 * A category of tocs. A category has an id and a list of contained
	 * tocs.
	 */
	private static class TocCategory extends ArrayList<ITocContribution> {

		private static final long serialVersionUID = 1L;

		/**
		 * Constructs a new empty TOC category with the given id.
		 *
		 * @param id the category's id
		 */
		public TocCategory(String id) {
		}

	}

	/*
	 * Orders the given toc contributions by category and product preference.
	 */
	public ITocContribution[] orderTocContributions(ITocContribution[] unorderedTocs) {
		// first categorize the TOCs
		List<String> itemsToOrder = new ArrayList<>();
		Map<String, Object> categorized = categorizeTocs(Arrays.asList(unorderedTocs), itemsToOrder);
		Map<String, String> nameIdMap = createNameIdMap(categorized);

		// order them
		List<String> orderedItems = ProductPreferences.getTocOrder(itemsToOrder, nameIdMap);

		// replace with actual TocContribution or category
		List<Object> actualItems = substituteValues(orderedItems, categorized);

		// expand the categories
		List<ITocContribution> expandedItems = expandCategories(actualItems);
		return expandedItems.toArray(new ITocContribution[orderedItems.size()]);
	}

	// Create a mapping from an id to a label that can be sorted
	private Map<String, String> createNameIdMap(Map<String, Object> categorized) {
		Map<String, String> map = new HashMap<>();
		for (Iterator<String> iter = categorized.keySet().iterator(); iter.hasNext();) {
			String key = iter.next();
			Object value = categorized.get(key);
			ITocContribution toc;
			if (value instanceof TocCategory) {
				TocCategory category = (TocCategory)value;
				toc = category.get(0);
			} else {
				toc = (ITocContribution)value;
			}
			map.put(key, toc.getToc().getLabel());
		}
		return map;
	}

	/*
	 * Categorizes the given toc contributions into categories, or individual
	 * toc contributions if no category (treated as a category of one). Returns
	 * mapping from category id/toc id to category/toc. Order of categories/
	 * tocs is returned via tocOrder.
	 */
	private Map<String, Object> categorizeTocs(List<ITocContribution> tocs, List<String> tocOrder) {
		Map<String, Object> categorized = new HashMap<>();
		Iterator<ITocContribution> iter = tocs.iterator();
		while (iter.hasNext()) {
			ITocContribution toc = iter.next();
			String categoryId;
			try {
				categoryId = toc.getCategoryId();
			}
			catch (Throwable t) {
				// log and skip
				String msg = "Error retrieving categoryId from " + ITocContribution.class.getName() + ": " + toc.getClass().getName(); //$NON-NLS-1$ //$NON-NLS-2$
				Platform.getLog(getClass()).error(msg, t);
				continue;
			}
			if (categoryId != null && categoryId.length() > 0) {
				// it has a category, add it to the appropriate TocCategory
				TocCategory category = (TocCategory)categorized.get(categoryId);
				if (category == null) {
					// create categories as needed
					category = new TocCategory(categoryId);
					categorized.put(categoryId, category);
					tocOrder.add(categoryId);
					category.add(toc);
				} else {
					// Add in alphabetic sequence
					String tocLabel = toc.getToc().getLabel();
					boolean done = false;
					for (int next = 0; next < category.size() && !done; next++ ) {
						String nextName = category.get(next).getToc().getLabel();
						if (tocLabel.compareToIgnoreCase(nextName) < 0) {
							done = true;
							category.add(next, toc);
						}
					}
					if (!done) {
						category.add(toc);
					}
				}
			}
			else {
				// doesn't have a category; insert the TOC directly
				String id;
				try {
					id = toc.getId();
				}
				catch (Throwable t) {
					// log and skip
					String msg = "Error retrieving id from " + ITocContribution.class.getName() + ": " + toc.getClass().getName(); //$NON-NLS-1$ //$NON-NLS-2$
					Platform.getLog(getClass()).error(msg, t);
					continue;
				}
				categorized.put(id, toc);
				tocOrder.add(id);
			}
		}
		return categorized;
	}

	/*
	 * Expands all categories in the given list to actual toc contributions
	 * organized by category.
	 */
	private List<ITocContribution> expandCategories(List<Object> entries) {
		List<ITocContribution> expanded = new ArrayList<>();
		for (Object entry : entries) {
			if (entry instanceof ITocContribution) {
				expanded.add((ITocContribution) entry);
			}
			else if (entry instanceof TocCategory) {
				expanded.addAll((TocCategory)entry);
			}
		}
		return expanded;
	}

	/*
	 * Substitutes each item with it's corresponding mapping from the map.
	 * Original List is not modified.
	 */
	private static List<Object> substituteValues(List<String> items, Map<String, Object> map) {
		if (items != null && map != null) {
			List<Object> result = new ArrayList<>(items.size());
			for (String key : items) {
				Object value = map.get(key);
				if (value != null) {
					result.add(value);
				}
			}
			return result;
		}
		return null;
	}

}
