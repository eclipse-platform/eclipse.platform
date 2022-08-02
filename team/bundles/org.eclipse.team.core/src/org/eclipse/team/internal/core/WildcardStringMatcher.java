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
 *******************************************************************************/
package org.eclipse.team.internal.core;

import org.eclipse.core.text.StringMatcher;

/**
 * A string pattern matcher. Supports '*' and '?' wildcards.
 */
public class WildcardStringMatcher {

	private final StringMatcher fMatcher;

	private final boolean fPathPattern;

	/**
	 * Constructs a wildcard matcher for a pattern that may contain '*' for 0 and
	 * many characters and '?' for exactly one character. Character matching is
	 * case-insensitive.
	 *
	 * Literal '*' and '?' characters must be escaped in the pattern e.g., "\*"
	 * means literal "*", etc.
	 *
	 * The escape character '\' is an escape only if followed by '*', '?', or '\'.
	 * All other occurrences are taken literally.
	 *
	 * If invoking the StringMatcher with string literals in Java, don't forget
	 * escape characters are represented by "\\".
	 *
	 * @param pattern the pattern to match text against
	 * @throws IllegalArgumentException if {@code pattern == null}
	 */
	public WildcardStringMatcher(String pattern) {
		fMatcher = new StringMatcher(pattern, true, false);
		fPathPattern = pattern.indexOf('/') != -1;
	}

	/**
	 * Determines whether the patterns contains a forward slash.
	 *
	 * @return {@code true} if the pattern contains a '/', {@code false} otherwise
	 */
	public boolean isPathPattern() {
		return fPathPattern;
	}

	/**
	 * Determines whether the given {@code text} matches the pattern.
	 *
	 * @param text String to match; must not be {@code null}
	 * @return {@code true} if the whole {@code text} matches the pattern;
	 *         {@code false} otherwise
	 * @throws IllegalArgumentException if {@code text == null}
	 */
	public boolean match(String text) {
		return fMatcher.match(text);
	}

}
