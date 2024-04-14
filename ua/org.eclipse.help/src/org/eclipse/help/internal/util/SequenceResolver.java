/*******************************************************************************
 * Copyright (c) 2006, 2016 IBM Corporation and others.
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
package org.eclipse.help.internal.util;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

/*
 * Provides an algorithm for determining a recommended sequence of items that
 * satisfies a primary sequence and as many secondary sequences as possible.
 *
 * For example, this is used to determine the display order of books on the tocs
 * based on the active product's preferred order as well as all other products'
 * preferred orders.
 */
public class SequenceResolver<T> {

	private static record ListWithIterator<T>(List<T> list, ListIterator<T> iterator) {
		public ListWithIterator(List<T> list) {
			this(list, list.listIterator());
		}
	}

	private ListWithIterator<T> primaryList;
	private List<ListWithIterator<T>> secondaryLists;
	private Set<T> processedItems;

	/*
	 * Merges the given primary and secondary orderings such that all ordering
	 * conditions from the primary are satisfied, and as many secondary ordering
	 * conditions as reasonably possible are also satisfied. An ordering condition
	 * is a pair of adjacent items in any ordering.
	 *
	 * For example:
	 *
	 *    primary =             {b, d, f}
	 *    secondary[0] =        {c, d, e}
	 *    secondary[1] =        {a, b, c}
	 *    -------------------------------
	 *    result =     {a, b, c, d, e, f}
	 *
	 * The algorithm works in iterations, where at each iteration we determine
	 * the next element in the recommended sequence. We maintain a pointer for
	 * each ordering to keep track of what we've already ordered.
	 *
	 * To determine the next item, we locate the current element in each ordering.
	 * These are the candidates for the next item as the next item can only be
	 * one of these.
	 *
	 * The top candidates are selected from the list of candidates, where a top
	 * candidate is one that has the lowest rank (there can be many). Rank is
	 * determined by how many other candidates appear before that candidate
	 * item in all the orderings. That is, we find out how many items
	 * the orderings list before each candidate.
	 *
	 * If a candidate has no other candidates listed before it, it will be
	 * the next item. If there are many, the first one is selected. If one of
	 * the top candidates is from the primary ordering (can only be one), it is
	 * automatically selected.
	 *
	 * Using the top rank ensures that if there are conflicts, and that
	 * as many orderings as possible are satisfied. For example, if most orderings
	 * want x before y, but a few want the opposite, x will be placed before y.
	 */
	public List<T> getSequence(List<T> primary, List<List<T>> secondary) {
		prepareDataStructures(primary, secondary);
		List<T> order = new ArrayList<>();
		T item;
		while ((item = getNextItem()) != null) {
			processedItems.add(item);
			advanceIterator(primaryList.iterator);
			for (ListWithIterator<T> secondaryList : secondaryLists) {
				advanceIterator(secondaryList.iterator);
			}
			order.add(item);
		}
		return order;
	}

	/*
	 * Create the data structures necessary for later operations.
	 */
	private void prepareDataStructures(List<T> primary, List<List<T>> secondary) {
		primaryList = new ListWithIterator<>(primary);
		secondaryLists = secondary.stream().map(ListWithIterator::new).collect(toList());
		processedItems = new HashSet<>();
	}

	/*
	 * Determine the next item in the sequence based on the top
	 * candidate items.
	 */
	private T getNextItem() {
		Candidate<T>[] candidates = getTopCandidates();
		switch(candidates.length) {
		case 0:
			return null;
		case 1:
			return candidates[0].item;
		default:
			for (int i=0;i<candidates.length;++i) {
				if (candidates[i].isPrimary) {
					return candidates[i].item;
				}
			}
			return candidates[0].item;
		}
	}

	/*
	 * Retrieves the top candidates from all the available next item candidates.
	 * These are the candidates that have the lowest rank
	 */
	private Candidate<T>[] getTopCandidates() {
		Candidate<T>[] candidates = getEligibleCandidates();
		rankCandidates(candidates);
		if (candidates.length > 0) {
			int topRank = candidates[0].rank;
			for (int i=1;i<candidates.length;++i) {
				if (candidates[i].rank < topRank) {
					topRank = candidates[i].rank;
				}
			}
			List<Candidate<T>> topCandidates = new ArrayList<>();
			for (int i=0;i<candidates.length;++i) {
				if (candidates[i].rank == topRank) {
					topCandidates.add(candidates[i]);
				}
			}
			return toCandidatesArray(topCandidates);
		}
		return candidates;
	}

	/*
	 * Returns all eligible candidates. A candidate is eligible if it does not
	 * conflict with the primary candidate. That is, if the primary candidate's list
	 * has that candidate after the primary candidate, it is contradicting the primary
	 * sequence and is not eligible.
	 */
	private Candidate<T>[] getEligibleCandidates() {
		Candidate<T>[] allCandidates = getAllCandidates();
		Candidate<T> primary = null;
		for (int i=0;i<allCandidates.length;++i) {
			if (allCandidates[i].isPrimary) {
				primary = allCandidates[i];
				break;
			}
		}
		// if we have no primary candidate then they're all eligible
		if (primary != null) {
			List<Candidate<T>> eligibleCandidates = new ArrayList<>(allCandidates.length);
			// primary candidate is always eligible
			eligibleCandidates.add(primary);
			Set<T> primarySet = Collections.singleton(primary.item);
			for (int i=0;i<allCandidates.length;++i) {
				Candidate<T> c = allCandidates[i];
				if (c != primary) {
					// does it contradict the primary sequence? if not, it is eligible
					if (countPrecedingItems(c.item, primary.src, primarySet) == 0) {
						eligibleCandidates.add(c);
					}
				}
			}
			return toCandidatesArray(eligibleCandidates);
		}
		return allCandidates;
	}

	/*
	 * Retrieve all the candidates for the next item in sequence, with
	 * no duplicates.
	 */
	private Candidate<T>[] getAllCandidates() {
		List<Candidate<T>> candidates = new ArrayList<>();
		T item = getNextItem(primaryList.iterator);
		if (item != null) {
			Candidate<T> c = new Candidate<>();
			c.item = item;
			c.isPrimary = true;
			c.src = primaryList.list;
			candidates.add(c);
		}
		for (ListWithIterator<T> secondary : secondaryLists) {
			item = getNextItem(secondary.iterator);
			if (item != null) {
				Candidate<T> c = new Candidate<>();
				c.item = item;
				c.isPrimary = false;
				c.src = secondary.list;
				if (!candidates.contains(c)) {
					candidates.add(c);
				}
			}
		}
		return toCandidatesArray(candidates);
	}

	/** Helper function as we cannot create arrays of parameterized types */
	@SuppressWarnings("unchecked")
	private Candidate<T>[] toCandidatesArray(List<Candidate<T>> candidates) {
		return candidates.toArray(new Candidate[candidates.size()]);
	}

	/*
	 * Assign a rank to each of the given candidates. Rank is determined by
	 * how many preceding candidates appear before that candidate in the orderings.
	 * This essentially means how far back this item should be in the final
	 * sequence.
	 */
	private void rankCandidates(Candidate<T>[] candidates) {
		// for quick lookup
		Set<T> candidateItems = new HashSet<>();
		for (int i=0;i<candidates.length;++i) {
			candidateItems.add(candidates[i].item);
		}
		for (int i=0;i<candidates.length;++i) {
			Candidate<T> c = candidates[i];
			for (int j=0;j<candidates.length;++j) {
				c.rank += countPrecedingItems(c.item, candidates[j].src, candidateItems);
			}
		}
	}

	/*
	 * Counts the number of elements from the given set that come before
	 * the given item in the given list.
	 */
	private int countPrecedingItems(Object item, List<?> list, Set<?> set) {
		int count = 0;
		Iterator<?> iter = list.iterator();
		while (iter.hasNext()) {
			Object next = iter.next();
			if (next.equals(item)) {
				return count;
			}
			if (set.contains(next)) {
				++count;
			}
		}
		return 0;
	}

	/*
	 * Returns the next item available to this iterator, without moving it
	 * forward.
	 */
	private T getNextItem(ListIterator<T> iter) {
		if (iter.hasNext()) {
			T next = iter.next();
			iter.previous();
			return next;
		}
		return null;
	}

	/*
	 * Advances the given iterator to the next item in its
	 * sequence that we haven't yet processed.
	 */
	private void advanceIterator(ListIterator<T> iter) {
		while (iter.hasNext()) {
			T item = iter.next();
			if (!processedItems.contains(item)) {
				iter.previous();
				break;
			}
		}
	}

	/*
	 * A candidate item; one that could potentially be the next one in the final
	 * sequence.
	 */
	private static class Candidate<T> {
		public T item;
		public boolean isPrimary;
		public int rank;
		public List<?> src;

		@Override
		public boolean equals(Object obj) {
			return item.equals(obj);
		}

		@Override
		public int hashCode() {
			return item.hashCode();
		}
	}
}
