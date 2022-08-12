/*******************************************************************************
 * Copyright (c) 2004, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 473427
 *******************************************************************************/
package org.eclipse.core.internal.utils;

import java.util.HashMap;

/**
 * A string pool is used for sharing strings in a way that eliminates duplicate
 * equal strings.  A string pool instance can be maintained over a long period
 * of time, or used as a temporary structure during a string sharing pass over
 * a data structure.
 * <p>
 * This class is not intended to be subclassed by clients.
 * </p>
 *
 * @see IStringPoolParticipant
 * @since 3.1
 */
public final class StringPool {
	private int savings;
	private final HashMap<String, String> map = new HashMap<>();

	/**
	 * Creates a new string pool.
	 */
	public StringPool() {
		super();
	}

	/**
	 * Adds a <code>String</code> to the pool.  Returns a <code>String</code>
	 * that is equal to the argument but that is unique within this pool.
	 * @param string The string to add to the pool
	 * @return A string that is equal to the argument.
	 */
	public String add(String string) {
		if (string == null)
			return string;
		String result = map.putIfAbsent(string, string);
		if (result != null) {
			if (result != string) {
				// XXX that number is wrong since String implementation changed to LATIN1
				// encoding, also interned String may have become externed:
				savings += 44 + 2 * string.length();
			}
			return result;
		}
		return string;
	}

	/**
	 * Returns an estimate of the size in bytes that was saved by sharing strings in
	 * the pool.  In particular, this returns the size of all strings that were added to the
	 * pool after an equal string had already been added.  This value can be used
	 * to estimate the effectiveness of a string sharing operation, in order to
	 * determine if or when it should be performed again.
	 *
	 * In some cases this does not precisely represent the number of bytes that
	 * were saved.  For example, say the pool already contains string S1.  Now
	 * string S2, which is equal to S1 but not identical, is added to the pool five
	 * times. This method will return the size of string S2 multiplied by the
	 * number of times it was added, even though the actual savings in this case
	 * is only the size of a single copy of S2.
	 */
	public int getSavedStringCount() {
		return savings;
	}
}
