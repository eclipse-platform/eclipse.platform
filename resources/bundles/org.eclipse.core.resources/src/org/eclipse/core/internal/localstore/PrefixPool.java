/*******************************************************************************
 * Copyright (c) 2007 Wind River Systems, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Martin Oberhuber (Wind River) - initial API and implementation for [105554]
 *******************************************************************************/

package org.eclipse.core.internal.localstore;

import java.util.Arrays;

/**
 * A pool of Strings for doing prefix checks against multiple
 * candidates.
 * <p>
 * Allows to enter a list of Strings, and then perform the
 * following checks:
 * </p>
 * <ul>
 *   <li>{@link #containsAsPrefix(String)} - check whether a given
 *       String s is a prefix of any String in the pool.</li>
 *   <li>{@link #hasPrefixOf(String)} - check whether any String
 *       in the pool is a prefix of the given String s.
 * </ul>
 * The prefix pool is always kept normalized, i.e. no element of
 * the pool is a prefix of any other element in the pool. In order
 * to maintain this constraint, there are two methods for adding
 * Strings to the pool:
 * <ul>
 *   <li>{@link #insertLonger(String)} - add a String s to the pool,
 *       and remove any existing prefix of s from the pool.</li>
 *   <li>{@link #insertShorter(String)} - add a String s to the pool,
 *       and remove any existing Strings sx from the pool which
 *       contain s as prefix.</li>
 * </ul>
 * The PrefixPool grows as needed when adding Strings. Typically,
 * it is used for prefix checks on absolute paths of a tree.
 * <p>
 * This class is not thread-safe: no two threads may add or
 * check items at the same time.
 *
 * @since 3.3
 */
public class PrefixPool {
	private String[] pool;
	private int size;

	/**
	 * Constructor.
	 * @param initialCapacity the initial size of the
	 *     internal array holding the String pool. Must
	 *     be greater than 0.
	 */
	public PrefixPool(int initialCapacity) {
		if (initialCapacity <= 0)
			throw new IllegalArgumentException("Illegal Capacity: " + initialCapacity); //$NON-NLS-1$
		pool = new String[initialCapacity];
		size = 0;
	}

	/**
	 * Clears the prefix pool, allowing all items to be
	 * garbage collected. Does not change the capacity
	 * of the pool.
	 */
	public/*synchronized*/void clear() {
		Arrays.fill(pool, 0, size, null);
		size = 0;
	}

	/**
	 * Return the current size of prefix pool.
	 * @return the number of elements in the pool.
	 */
	public/*synchronized*/int size() {
		return size;
	}

	/**
	 * Ensure that there is room for at least one more element.
	 */
	private void checkCapacity() {
		if (size + 1 >= pool.length) {
			String[] newprefixList = new String[2 * pool.length];
			System.arraycopy(pool, 0, newprefixList, 0, pool.length);
			Arrays.fill(pool, null); //help the garbage collector
			pool = newprefixList;
		}
	}

	/**
	 * Insert a String s into the pool of known prefixes, removing
	 * any existing prefix of it.
	 * <p>
	 * If any existing prefix of this String is found in the pool,
	 * it is replaced by the new longer one in order to maintain
	 * the constraint of keeping the pool normalized.
	 * </p><p>
	 * If it turns out that s is actually a prefix or equal to
	 * an existing element in the pool (so it is essentially
	 * shorter), this method returns with no operation in order
	 * to maintain the constraint that the pool remains normalized.
	 * </p>
	 * @param s the String to insert.
	 */
	public/*synchronized*/void insertLonger(String s) {
		//check in reverse order since we expect some locality
		for (int i = size - 1; i >= 0; i--) {
			if (pool[i].startsWith(s)) {
				//prefix of an existing String --> no-op
				return;
			} else if (s.startsWith(pool[i])) {
				//replace, since a longer s has more prefixes than a short one
				pool[i] = s;
				return;
			}
		}
		checkCapacity();
		pool[size] = s;
		size++;
	}

	/**
	 * Insert a String s into the pool of known prefixes, removing
	 * any Strings that have s as prefix.
	 * <p>
	 * If this String is a prefix of any existing String in the pool,
	 * all elements that contain the new String as prefix are removed
	 * and return value <code>true</code> is returned.
	 * </p><p>
	 * Otherwise, the new String is added to the pool unless an
	 * equal String or e prefix of it exists there already (so
	 * it is essentially equal or longer than an existing prefix).
	 * In all these cases, <code>false</code> is returned since
	 * no prefixes were replaced.
	 * </p>
	 * @param s the String to insert.
	 * @return <code>true</code>if any longer elements have been
	 *     removed.
	 */
	public/*synchronized*/boolean insertShorter(String s) {
		boolean replaced = false;
		//check in reverse order since we expect some locality
		for (int i = size - 1; i >= 0; i--) {
			if (s.startsWith(pool[i])) {
				//longer or equal to an existing prefix - nothing to do
				return false;
			} else if (pool[i].startsWith(s)) {
				if (replaced) {
					//replaced before, so shrink the array.
					//Safe since we are iterating in reverse order.
					System.arraycopy(pool, i + 1, pool, i, size - i - 1);
					size--;
					pool[size] = null;
				} else {
					//replace, since this is a shorter s
					pool[i] = s;
					replaced = true;
				}
			}
		}
		if (!replaced) {
			//append at the end
			checkCapacity();
			pool[size] = s;
			size++;
		}
		return replaced;
	}

	/**
	 * Check if the given String s is a prefix of any of Strings
	 * in the pool.
	 * @param s a s to check for being a prefix
	 * @return <code>true</code> if the passed s is a prefix
	 *     of any of the Strings contained in the pool.
	 */
	public/*synchronized*/boolean containsAsPrefix(String s) {
		//check in reverse order since we expect some locality
		for (int i = size - 1; i >= 0; i--) {
			if (pool[i].startsWith(s)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Test if the String pool contains any one that is a prefix
	 * of the given String s.
	 * @param s the String to test
	 * @return <code>true</code> if the String pool contains a
	 *     prefix of the given String.
	 */
	public/*synchronized*/boolean hasPrefixOf(String s) {
		for (int i = size - 1; i >= 0; i--) {
			if (s.startsWith(pool[i])) {
				return true;
			}
		}
		return false;
	}

}
