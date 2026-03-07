package org.eclipse.debug.tests.console;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.stream.Stream;

import org.eclipse.ui.internal.console.ConsoleOutputLineBreak;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class ConsoleOutputLineBreakTest {

	private static final String NL = System.lineSeparator();

	record C (int n, int m, int r, String i, String o) {}

	private static C test(int limit, String input, String output) {
		return test(limit, 1, 1, input, output);
	}

	private static C test(int limit, int chunks, String input, String output) {
		return test(limit, chunks, chunks, input, output);
	}

	private static C test(int limit, int chunks, int repeat, String input, String output) {
		return new C (limit, chunks, repeat, input, output);
	}

	private static final C[] TESTS = {
			test(10,        "========"      , "========"  ),
			test(10, 10, 8, "========"      , "=========="),
			test( 4,        "========"      , "====\n===="),
			test( 4, 10,    "========"      , "====\n===="),
			test( 4,        "====\n===="    , "====\n===="),
			test( 4, 10,    "====\n===="    , "====\n===="),

			test( 2, 10,    "=======\n=="   , "==\n==\n==\n=\n=="),
			test( 2,        "====\n===="    , "==\n==\n==\n=="   ),
			test( 2, 10,    "====\n===="    , "==\n==\n==\n=="   ),
			test( 2,        "========="     , "==\n==\n==\n==\n="),
			test( 2,        "=======\n=="   , "==\n==\n==\n=\n=="),
			test( 3,        "========="     , "===\n===\n==="    ),
			test( 3, 10,    "========="     , "===\n===\n==="    ),
			test( 3,        "========\n="   , "===\n===\n==\n="  ),

			test( 2,        "======\n======", "==\n==\n==\n==\n==\n=="),
			test( 2, 10,    "======\n======", "==\n==\n==\n==\n==\n=="),

			test( 3,  3, 1, "========\n="   , "===\n===\n==\n===\n===\n===\n===\n===\n===\n="),
			test( 4,  3, 1, "========\n="   , "====\n====\n====\n====\n=\n====\n====\n=\n="  ),
	};

	private static Stream<Arguments> tests() {
	    return Arrays.stream(TESTS).map(Arguments::of);
	}

	@ParameterizedTest
	@MethodSource("tests")
	public void test(C p) {
		String input = p.i.replaceAll("\n", NL);
		String output = p.o.replaceAll("\n", NL);
		ConsoleOutputLineBreak lineBreak = new ConsoleOutputLineBreak();
		StringBuilder c = new StringBuilder();
		for (int i = 0; i < p.m; ++i) {
			StringBuilder s = new StringBuilder(input);
			CharSequence text = lineBreak.modify(s, p.n);
			c.append(text);
		}
		String expected = repeat(output, p.r);
		assertEquals(expected, c.toString());
	}

	private static String repeat(String s, int r) {
		return (NL + s).repeat(r).substring(NL.length());
	}
}