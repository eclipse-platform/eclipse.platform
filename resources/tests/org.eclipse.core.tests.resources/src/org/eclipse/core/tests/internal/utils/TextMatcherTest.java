/*******************************************************************************
 * Copyright (c) 2020 Thomas Wolf<thomas.wolf@paranor.ch> and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.core.tests.internal.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.internal.utils.TextMatcher;
import org.junit.Test;

/**
 * Tests for {@link TextMatcher}.
 */
public class TextMatcherTest {

	@Test
	public void testEmpty() {
		assertTrue(new TextMatcher("", false, false).match(""));
		assertFalse(new TextMatcher("", false, false).match("foo"));
		assertFalse(new TextMatcher("", false, false).match("foo bar baz"));
		assertTrue(new TextMatcher("", false, true).match(""));
		assertFalse(new TextMatcher("", false, true).match("foo"));
		assertFalse(new TextMatcher("", false, true).match("foo bar baz"));
	}

	@Test
	public void testSuffixes() {
		assertFalse(new TextMatcher("fo*ar", false, false).match("foobar_123"));
		assertFalse(new TextMatcher("fo*ar", false, false).match("foobar_baz"));
	}

	@Test
	public void testChinese() {
		assertTrue(new TextMatcher("喜欢", false, false).match("我 喜欢 吃 苹果。"));
		// This test would work only if word-splitting used the ICU BreakIterator.
		// "Words" are as shown above.
		// assertTrue(new TextMatcher("喜欢", false, false).match("我喜欢吃苹果。"));
	}

	@Test
	public void testSingleWords() {
		assertTrue(new TextMatcher("huhn", false, false).match("hahn henne hühner küken huhn"));
		assertTrue(new TextMatcher("h?hner", false, false).match("hahn henne hühner küken huhn"));
		assertTrue(new TextMatcher("h*hner", false, false).match("hahn henne hühner küken huhn"));
		assertTrue(new TextMatcher("hühner", false, false).match("hahn henne hühner küken huhn"));
		// Full pattern must match word fully
		assertFalse(new TextMatcher("h?hner", false, false).match("hahn henne hühnerhof küken huhn"));
		assertFalse(new TextMatcher("h*hner", false, false).match("hahn henne hühnerhof küken huhn"));
		assertFalse(new TextMatcher("hühner", false, false).match("hahn henne hühnerhof küken huhn"));

		assertTrue(new TextMatcher("huhn", false, true).match("hahn henne hühner küken huhn"));
		assertFalse(new TextMatcher("h?hner", false, true).match("hahn henne hühner küken huhn"));
		assertFalse(new TextMatcher("h*hner", false, true).match("hahn henne hühner küken huhn"));
		assertTrue(new TextMatcher("hühner", false, true).match("hahn henne hühner küken huhn"));
		// Full pattern must match word fully
		assertFalse(new TextMatcher("h?hner", false, true).match("hahn henne hühnerhof küken huhn"));
		assertFalse(new TextMatcher("h*hner", false, true).match("hahn henne hühnerhof küken huhn"));
		assertFalse(new TextMatcher("hühner", false, true).match("hahn henne hühnerhof küken huhn"));

		// Bug 570390: Pattern starting/ending with whitespace should still match
		assertTrue(new TextMatcher("hahn ", false, false).match("hahn henne hühnerhof küken huhn"));
		assertTrue(new TextMatcher("huhn ", false, false).match("hahn henne hühnerhof küken huhn"));
		assertTrue(new TextMatcher(" hahn", false, false).match("hahn henne hühnerhof küken huhn"));
		assertTrue(new TextMatcher(" huhn", false, false).match("hahn henne hühnerhof küken huhn"));
	}

	@Test
	public void testMultipleWords() {
		assertTrue(new TextMatcher("huhn h?hner", false, false).match("hahn henne hühner küken huhn"));
		assertTrue(new TextMatcher("huhn h?hner", false, false).match("hahn henne hühnerhof küken huhn"));
		assertFalse(new TextMatcher("huhn h?hner", false, true).match("hahn henne hühner küken huhn"));
		assertFalse(new TextMatcher("huhn h?hner", false, true).match("hahn henne hühnerhof küken huhn"));
		assertTrue(new TextMatcher("huhn h*hner", false, false).match("hahn henne hühner küken huhn"));
		assertTrue(new TextMatcher("huhn h*hner", false, false).match("hahn henne hühnerhof küken huhn"));
		assertFalse(new TextMatcher("huhn h*hner", false, true).match("hahn henne hühner küken huhn"));
		assertFalse(new TextMatcher("huhn h*hner", false, true).match("hahn henne hühnerhof küken huhn"));
		assertTrue(new TextMatcher("huhn hühner", false, false).match("hahn henne hühner küken huhn"));
		assertTrue(new TextMatcher("huhn hühner", false, false).match("hahn henne hühnerhof küken huhn"));
		assertTrue(new TextMatcher("huhn hühner", false, true).match("hahn henne hühner küken huhn"));
		assertTrue(new TextMatcher("huhn hühner", false, true).match("hahn henne hühnerhof küken huhn"));

		// Bug 570390: Pattern starting/ending with whitespace should still match
		assertTrue(new TextMatcher("huhn hahn ", false, false).match("hahn henne hühnerhof küken huhn"));
		assertTrue(new TextMatcher("hahn huhn ", false, false).match("hahn henne hühnerhof küken huhn"));
		assertTrue(new TextMatcher(" huhn hahn", false, false).match("hahn henne hühnerhof küken huhn"));
		assertTrue(new TextMatcher(" hahn huhn", false, false).match("hahn henne hühnerhof küken huhn"));
	}

	@Test
	public void testCaseInsensitivity() {
		assertTrue(new TextMatcher("Huhn HÜHNER", true, false).match("hahn henne hühner küken huhn"));
		assertTrue(new TextMatcher("Huhn HÜHNER", true, false).match("hahn henne hühnerhof küken huhn"));
		assertTrue(new TextMatcher("Huhn HÜHNER", true, true).match("hahn henne hühner küken huhn"));
		assertTrue(new TextMatcher("Huhn HÜHNER", true, true).match("hahn henne hühnerhof küken huhn"));
		assertTrue(new TextMatcher("HüHnEr", true, false).match("hahn henne hühner küken huhn"));
		assertFalse(new TextMatcher("HüHnEr", true, false).match("hahn henne hühnerhof küken huhn"));
		assertTrue(new TextMatcher("HüHnEr", true, true).match("hahn henne hühner küken huhn"));
		assertFalse(new TextMatcher("HüHnEr", true, true).match("hahn henne hühnerhof küken huhn"));
	}
}
