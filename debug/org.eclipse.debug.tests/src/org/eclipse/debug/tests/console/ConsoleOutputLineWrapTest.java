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

import org.eclipse.ui.internal.console.ConsoleOutputLineWrap;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests {@link ConsoleOutputLineWrap} handling chunks of input, breaking input
 * lines at a specific length limit.
 */
public class ConsoleOutputLineWrapTest {

	/**
	 * Parameters of a test for {@link ConsoleOutputLineWrap}.
	 *
	 * @param limit the line length limit for the test
	 * @param chunks how many times the {@code input} is repeated
	 * @param repeat how many times {@code output} is repeated in the expected
	 *            output, line breaks are inserted between concatenated
	 *            {@code output}
	 * @param input input string passed to the tested
	 *            {@link ConsoleOutputLineWrap}
	 * @param output expected output, repeated {@code repeat} times
	 * @param nl the newline character sequences
	 */
	record Parameters(int limit, int chunks, int repeat, String input, String output, String... nl) {
	}

	private static Parameters test(int limit, String input, String output, String... nl) {
		return test(limit, 1, 1, input, output, nl);
	}

	private static Parameters test(int limit, int chunks, String input, String output, String... nl) {
		return test(limit, chunks, chunks, input, output, nl);
	}

	private static Parameters test(int limit, int chunks, int repeat, String input, String output, String... nl) {
		return new Parameters(limit, chunks, repeat, input, output, nl);
	}

	private static final Parameters[] TESTS = {
			// Unix newlines
			test(4, "\n========", "\n====\n====", "\n"),

			test(10,        "========"      , "========"  , "\n"),
			test(10, 10, 8, "========"      , "==========", "\n"),
			test( 4,        "========"      , "====\n====", "\n"),
			test( 4, 10,    "========"      , "====\n====", "\n"),
			test( 4,        "====\n===="    , "====\n====", "\n"),
			test( 4, 10,    "====\n===="    , "====\n====", "\n"),

			test( 2, 10,    "=======\n=="   , "==\n==\n==\n=\n==", "\n"),
			test( 2,        "====\n===="    , "==\n==\n==\n=="   , "\n"),
			test( 2, 10,    "====\n===="    , "==\n==\n==\n=="   , "\n"),
			test( 2,        "========="     , "==\n==\n==\n==\n=", "\n"),
			test( 2,        "=======\n=="   , "==\n==\n==\n=\n==", "\n"),
			test( 3,        "========="     , "===\n===\n==="    , "\n"),
			test( 3, 10,    "========="     , "===\n===\n==="    , "\n"),
			test( 3,        "========\n="   , "===\n===\n==\n="  , "\n"),

			test( 2,        "======\n======", "==\n==\n==\n==\n==\n==", "\n"),
			test( 2, 10,    "======\n======", "==\n==\n==\n==\n==\n==", "\n"),

			test( 3,  3, 1, "========\n="   , "===\n===\n==\n===\n===\n===\n===\n===\n===\n=", "\n"),
			test( 4,  3, 1, "========\n="   , "====\n====\n====\n====\n=\n====\n====\n=\n="  , "\n"),

			// Windows newlines
			test(4, "\r\n========", "\r\n====\r\n====", "\r\n"),

			test(10,        "========"        , "========"    , "\r\n"),
			test(10, 10, 8, "========"        , "=========="  , "\r\n"),
			test( 4,        "========"        , "====\r\n====", "\r\n"),
			test( 4, 10,    "========"        , "====\r\n====", "\r\n"),
			test( 4,        "====\r\n===="    , "====\r\n====", "\r\n"),
			test( 4, 10,    "====\r\n===="    , "====\r\n====", "\r\n"),

			test( 2, 10,    "=======\r\n=="   , "==\r\n==\r\n==\r\n=\r\n==", "\r\n"),
			test( 2,        "====\r\n===="    , "==\r\n==\r\n==\r\n=="     , "\r\n"),
			test( 2, 10,    "====\r\n===="    , "==\r\n==\r\n==\r\n=="     , "\r\n"),
			test( 2,        "========="       , "==\r\n==\r\n==\r\n==\r\n=", "\r\n"),
			test( 2,        "=======\r\n=="   , "==\r\n==\r\n==\r\n=\r\n==", "\r\n"),
			test( 3,        "========="       , "===\r\n===\r\n==="        , "\r\n"),
			test( 3, 10,    "========="       , "===\r\n===\r\n==="        , "\r\n"),
			test( 3,        "========\r\n="   , "===\r\n===\r\n==\r\n="    , "\r\n"),

			test( 2,        "======\r\n======", "==\r\n==\r\n==\r\n==\r\n==\r\n==", "\r\n"),
			test( 2, 10,    "======\r\n======", "==\r\n==\r\n==\r\n==\r\n==\r\n==", "\r\n"),

			test( 3,  3, 1, "========\r\n="   , "===\r\n===\r\n==\r\n===\r\n===\r\n===\r\n===\r\n===\r\n===\r\n=", "\r\n"),
			test( 4,  3, 1, "========\r\n="   , "====\r\n====\r\n====\r\n====\r\n=\r\n====\r\n====\r\n=\r\n="    , "\r\n"),

			// multiple newlines
			test( 3,  3, 1, "========\r\n="   , "===\r\n===\r\n==\r\n===\r\n===\r\n===\r\n===\r\n===\r\n===\r\n=", "\r\n", "\r", "\n"),
			test( 4,  3, 1, "========\r\n="   , "====\r\n====\r\n====\r\n====\r\n=\r\n====\r\n====\r\n=\r\n="    , "\r\n", "\r", "\n"),

			test( 3,  3, 1, "========\r\n="   , "===\r===\r==\r\n===\r===\r===\r\n===\r===\r===\r\n=", "\r", "\n", "\r\n"),
			test( 4,  3, 1, "========\r\n="   , "====\r====\r\n====\r====\r=\r\n====\r====\r=\r\n="  , "\r", "\n", "\r\n"),
	};

	private static Stream<Arguments> tests() {
	    return Arrays.stream(TESTS).map(Arguments::of);
	}

	@ParameterizedTest
	@MethodSource("tests")
	public void test(Parameters p) {
		ConsoleOutputLineWrap lineBreak = new ConsoleOutputLineWrap(p.limit, p.nl);
		StringBuilder c = new StringBuilder();
		for (int i = 0; i < p.chunks; ++i) {
			StringBuilder s = new StringBuilder(p.input);
			CharSequence text = lineBreak.modify(s);
			c.append(text);
		}
		String expected = repeat(p.output, p.repeat, p.nl[0]);
		assertEquals(expected, c.toString());
	}

	private static String repeat(String s, int r, String nl) {
		return (nl + s).repeat(r).substring(nl.length());
	}
}
