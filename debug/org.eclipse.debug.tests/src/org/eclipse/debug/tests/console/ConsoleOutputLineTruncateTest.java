/*******************************************************************************
 * Copyright (c) 2026 Simeon Andreev and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Simeon Andreev - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.tests.console;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.stream.Stream;

import org.eclipse.ui.internal.console.ConsoleOutputLineTruncate;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests {@link ConsoleOutputLineTruncate} handling chunks of input, breaking
 * input lines at a specific length limit.
 */
public class ConsoleOutputLineTruncateTest {

	/**
	 * Parameters of a test for {@link ConsoleOutputLineTruncate}.
	 *
	 * @param limit the line length limit for the test
	 * @param chunks how many times the {@code input} is repeated
	 * @param repeat how many times {@code output} is repeated in the expected
	 *            output, line breaks are inserted between concatenated
	 *            {@code output}
	 * @param input input string passed to the tested
	 *            {@link ConsoleOutputLineTruncate}
	 * @param output expected output, repeated {@code repeat} times
	 * @param nl the newline character sequences
	 */
	record Parameters(String name, int limit, int chunks, int repeat, String input, String output, String... nl) {
	}

	private static Parameters test(String name, int limit, String input, String output, String... nl) {
		return test(name, limit, 1, 1, input, output, nl);
	}

	private static Parameters test(String name, int limit, int chunks, int repeat, String input, String output, String... nl) {
		return new Parameters(name, limit, chunks, repeat, input, output, nl);
	}

	private static final Parameters[] TESTS = {
			// Unix newlines
			test("unix_nl_01", 4, "\n========", "\n==== ...\n", "\n"),
			test("unix_nl_02", 10, "========", "========", "\n"),
			test("unix_nl_03", 10, 10, 1, "========", "========== ...\n", "\n"),
			test("unix_nl_04", 4, "========", "==== ...\n", "\n"),
			test("unix_nl_05", 4, 10, 1, "========", "==== ...\n", "\n"),
			test("unix_nl_06", 4, "====\n====", "====\n====", "\n"),
			test("unix_nl_07", 4, 5, 1, "====\n====", "====\n==== ...\n==== ...\n==== ...\n==== ...\n====", "\n"),
			test("unix_nl_08", 2, 5, 1, "=======\n==", "== ...\n== ...\n== ...\n== ...\n== ...\n==", "\n"),
			test("unix_nl_09", 2, "====\n====", "== ...\n== ...\n", "\n"),
			test("unix_nl_10", 2, 5, 1, "====\n====", "== ...\n== ...\n== ...\n== ...\n== ...\n== ...\n", "\n"),
			test("unix_nl_11", 2, "=========", "== ...\n", "\n"),
			test("unix_nl_12", 2, "=======\n==", "== ...\n==", "\n"),
			test("unix_nl_13", 3, "=========", "=== ...\n", "\n"),
			test("unix_nl_14", 3, 5, 1, "=========", "=== ...\n", "\n"),
			test("unix_nl_15", 3, "========\n=", "=== ...\n=", "\n"),
			test("unix_nl_16", 2, "======\n======", "== ...\n== ...\n", "\n"),
			test("unix_nl_17", 2, 5, 1, "======\n======", "== ...\n== ...\n== ...\n== ...\n== ...\n== ...\n", "\n"),
			test("unix_nl_18", 3, 3, 1, "========\n=", "=== ...\n=== ...\n=== ...\n=", "\n"),
			test("unix_nl_19", 4, 3, 1, "========\n=", "==== ...\n==== ...\n==== ...\n=", "\n"),

			// Windows newlines
			test("win_nl_01", 4, "\r\n========", "\r\n==== ...\r\n", "\r\n"),
			test("win_nl_02", 10, "========", "========", "\r\n"),
			test("win_nl_03", 10, 10, 1, "========", "========== ...\r\n", "\r\n"),
			test("win_nl_04", 4, "========", "==== ...\r\n", "\r\n"),
			test("win_nl_05", 4, 10, 1, "========", "==== ...\r\n", "\r\n"),
			test("win_nl_06", 4, "====\r\n====", "====\r\n====", "\r\n"),
			test("win_nl_07", 4, 5, 1, "====\r\n====", "====\r\n==== ...\r\n==== ...\r\n==== ...\r\n==== ...\r\n====", "\r\n"),
			test("win_nl_08", 2, 5, 1, "=======\r\n==", "== ...\r\n== ...\r\n== ...\r\n== ...\r\n== ...\r\n==", "\r\n"),
			test("win_nl_09", 2, "====\r\n====", "== ...\r\n== ...\r\n", "\r\n"),
			test("win_nl_10", 2, 5, 1, "====\r\n====", "== ...\r\n== ...\r\n== ...\r\n== ...\r\n== ...\r\n== ...\r\n", "\r\n"),
			test("win_nl_11", 2, "=========", "== ...\r\n", "\r\n"),
			test("win_nl_12", 2, "=======\r\n==", "== ...\r\n==", "\r\n"),
			test("win_nl_13", 3, "=========", "=== ...\r\n", "\r\n"),
			test("win_nl_14", 3, 5, 1, "=========", "=== ...\r\n", "\r\n"),
			test("win_nl_15", 3, "========\r\n=", "=== ...\r\n=", "\r\n"),
			test("win_nl_16", 2, "======\r\n======", "== ...\r\n== ...\r\n", "\r\n"),
			test("win_nl_17", 2, 5, 1, "======\r\n======", "== ...\r\n== ...\r\n== ...\r\n== ...\r\n== ...\r\n== ...\r\n", "\r\n"),
			test("win_nl_18", 3, 3, 1, "========\r\n=", "=== ...\r\n=== ...\r\n=== ...\r\n=", "\r\n"),
			test("win_nl_19", 4, 3, 1, "========\r\n=", "==== ...\r\n==== ...\r\n==== ...\r\n=", "\r\n"),

			// multiple newlines
			test("multi_nl_1", 3, 3, 1, "========\r\n=", "=== ...\r\n=== ...\r\n=== ...\r\n=", "\r\n", "\r", "\n"),
			test("multi_nl_2", 4, 3, 1, "========\r\n=", "==== ...\r\n==== ...\r\n==== ...\r\n=", "\r\n", "\r", "\n"),
			test("multi_nl_3", 3, 3, 1, "========\r\n=", "=== ...\r\n=== ...\r\n=== ...\r\n=", "\r", "\n", "\r\n"),
			test("multi_nl_4", 4, 3, 1, "========\r\n=", "==== ...\r\n==== ...\r\n==== ...\r\n=", "\r", "\n", "\r\n"),

			// surrogate pairs (emoji 😀 = \uD83D\uDE00, 2 chars) -- truncation must not split a surrogate pair
			// truncation point falls on the low surrogate: limit=3, "ab😀c" (5 chars) -> back up before emoji
			test("surrogate_pairs_1",  3, "ab\uD83D\uDE00c"           , "ab ...\n"     , "\n"),
			// truncation point falls on the low surrogate: limit=4, "abc😀d" (6 chars)
			test("surrogate_pairs_2",  4, "abc\uD83D\uDE00d"          , "abc ...\n"    , "\n"),
			// truncation with newline in chunk: first line truncated before emoji, second line untouched
			test("surrogate_pairs_3",  3, "ab\uD83D\uDE00\n=="         , "ab ...\n=="   , "\n"),
			// multi-chunk: second chunk (continuation of overlong line) starts with a low surrogate and is deleted entirely
			test("surrogate_pairs_4",  2, 2, 1, "ab\uD83D\uDE00"       , "ab ...\n"     , "\n"),
	};

	private static Stream<Arguments> tests() {
		return Arrays.stream(TESTS).map(ConsoleOutputLineTruncateTest::named);
	}

	@ParameterizedTest
	@MethodSource("tests")
	public void test(Parameters p) {
		ConsoleOutputLineTruncate truncate = new ConsoleOutputLineTruncate(p.limit, p.nl);
		StringBuilder c = new StringBuilder();
		for (int i = 0; i < p.chunks; ++i) {
			StringBuilder s = new StringBuilder(p.input);
			CharSequence text = truncate.modify(s);
			c.append(text);
		}
		String expected = repeat(p.output, p.repeat, p.nl[0]);
		assertEquals(expected, c.toString());
	}

	private static String repeat(String s, int r, String nl) {
		return (nl + s).repeat(r).substring(nl.length());
	}

	private static Arguments named(Parameters p) {
		return Arguments.of(Named.of(p.name, p));
	}
}
