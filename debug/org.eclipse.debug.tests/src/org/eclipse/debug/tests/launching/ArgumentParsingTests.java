/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
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
package org.eclipse.debug.tests.launching;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.tests.AbstractDebugTest;
import org.eclipse.osgi.service.environment.Constants;
import org.junit.Test;

/**
 * Tests {@link org.eclipse.debug.core.DebugPlugin#parseArguments(String)} and
 * {@link org.eclipse.debug.core.DebugPlugin#renderArguments(String[], int[])}.
 */
public class ArgumentParsingTests extends AbstractDebugTest {

	private void execute1Arg(String cmdLine) throws Exception {
		execute1Arg(cmdLine, cmdLine);
	}

	private void execute1Arg(String cmdLine, String argParsed) throws Exception {
		execute1Arg(cmdLine, argParsed, cmdLine);
	}

	private void execute1Arg(String cmdLine, String argParsed, String rendered) throws Exception {
		execute("a " + cmdLine + " b", new String[] { "a", argParsed, "b" }, "a " + rendered + " b"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
	}

	private static void execute(String commandLine, String[] expectedArgs) throws Exception {
		execute(commandLine, expectedArgs, commandLine);
	}

	private static void execute(String commandLine, String[] expectedArgs, String expectedRendered) throws Exception {
		String[] arguments = DebugPlugin.parseArguments(commandLine);
		assertEquals("unexpected parseArguments result;", //$NON-NLS-1$
				Arrays.asList(expectedArgs).toString(),
				Arrays.asList(arguments).toString());

		runCommandLine(commandLine, arguments);

		String rendered = DebugPlugin.renderArguments(arguments, null);
		assertEquals("unexpected renderArguments result;", expectedRendered, rendered); //$NON-NLS-1$

		if (!commandLine.equals(rendered)) {
			String[] arguments2 = DebugPlugin.parseArguments(rendered);
			assertEquals("parsing rendered command line doesn't yield original arguments;", //$NON-NLS-1$
					Arrays.asList(expectedArgs).toString(),
					Arrays.asList(arguments2).toString());
		}

		String[] splitArguments = DebugPlugin.splitArguments(commandLine);
		assertEquals(expectedArgs.length, splitArguments.length);
		String sb = String.join(" ", splitArguments); //$NON-NLS-1$
		assertEquals(commandLine, sb);
	}

	private static void runCommandLine(String commandLine, String[] arguments) throws IOException,
			URISyntaxException, CoreException {
		URL classPathUrl = ArgumentsPrinter.class.getResource("/"); //$NON-NLS-1$
		classPathUrl = FileLocator.toFileURL(classPathUrl);
		File classPathFile = URIUtil.toFile(URIUtil.toURI(classPathUrl));

		String[] execArgs= new String[arguments.length + 4];
		execArgs[0] = IPath.fromOSString(System.getProperty("java.home")).append("bin/java").toOSString(); //$NON-NLS-1$ //$NON-NLS-2$
		execArgs[1] = "-cp"; //$NON-NLS-1$
		execArgs[2]= classPathFile.getAbsolutePath();
		execArgs[3]= ArgumentsPrinter.class.getName();
		System.arraycopy(arguments, 0, execArgs, 4, arguments.length);

		ArrayList<String> resultArgs = runCommandLine(execArgs);

		assertEquals("unexpected exec result;", //$NON-NLS-1$
				Arrays.asList(arguments).toString(),
				resultArgs.toString());

		if (! Platform.getOS().equals(Constants.OS_WIN32)) {
			execArgs = new String[] { "sh", "-c", execArgs[0] + " " + execArgs[1] + " " + execArgs[2] + " " + execArgs[3] + " " + commandLine }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
			resultArgs = runCommandLine(execArgs);
			assertEquals("unexpected sh exec result;", //$NON-NLS-1$
					Arrays.asList(arguments).toString(),
					resultArgs.toString());
		}
	}

	private static ArrayList<String> runCommandLine(String[] execArgs)
			throws CoreException, IOException {
		String[] systemDependentExecArgs = quoteWindowsArgs(execArgs);
		Process process = DebugPlugin.exec(systemDependentExecArgs, null);
		BufferedReader procOut = new BufferedReader(new InputStreamReader(process.getInputStream()));

		ArrayList<String> procArgs = new ArrayList<>();
		String procArg;
		while ((procArg = procOut.readLine()) != null) {
			procArgs.add(procArg);
		}
		return procArgs;
	}

	private static String[] quoteWindowsArgs(String[] cmdLine) {
		// see https://bugs.eclipse.org/387504#c13 , workaround for http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6511002
		String[] systemDependentCmdLine = cmdLine;
		if (Platform.getOS().equals(Constants.OS_WIN32)) {
			String[] winCmdLine = new String[cmdLine.length];
			for (int i = 0; i < cmdLine.length; i++) {
				winCmdLine[i] = winQuote(cmdLine[i]);
			}
			systemDependentCmdLine = winCmdLine;
		}
		return systemDependentCmdLine;
	}


	private static boolean needsQuoting(String s) {
		int len = s.length();
		if (len == 0) {
			return true;
		}
		for (int i = 0; i < len; i++) {
			switch (s.charAt(i)) {
				case ' ': case '\t': case '\\': case '"':
					return true;
				default:
					break;
			}
		}
		return false;
	}

	private static String winQuote(String s) {
		if (! needsQuoting(s)) {
			return s;
		}
		return "\"" + s.replaceAll("([\\\\]*)\"", "$1$1\\\\\"").replaceAll("([\\\\]*)\\z", "$1$1") + "\"";
	}

	// -- tests:
	@Test
	public void testEmpty() throws Exception {
		execute("", new String[0]); //$NON-NLS-1$
	}

	@Test
	public void test1arg() throws Exception {
		execute("a", new String[] { "a" }); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void test2arg() throws Exception {
		execute("a b", new String[] { "a", "b" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	@Test
	public void test100arg() throws Exception {
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < 100; i++) {
			buf.append("a "); //$NON-NLS-1$
		}
		buf.deleteCharAt(buf.length() - 1);
		String[] args = new String[100];
		Arrays.fill(args, "a"); //$NON-NLS-1$
		execute(buf.toString(), args, buf.toString().trim());
	}

	@Test
	public void testEscape() throws Exception {
		if (Platform.getOS().equals(Constants.OS_WIN32)) {
			execute1Arg("\\1"); //$NON-NLS-1$
		} else {
			execute1Arg("\\1", "1", "1"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	@Test
	public void testRenderWindowsBackslash() throws Exception {
		String[] arguments = {
			"-Dfoo=\"abc\\def\\ghi\""
		};
		String rendered = DebugPlugin.renderArguments(arguments, null);
		if (Platform.getOS().equals(Constants.OS_WIN32)) {
			assertEquals("unexpected renderArguments result;", "-Dfoo=\\\"abc\\def\\ghi\\\"", rendered); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			assertEquals("unexpected renderArguments result;", "-Dfoo=\\\"abc\\\\def\\\\ghi\\\"", rendered); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	@Test
	public void testEscapeDoubleQuote1() throws Exception {
		execute1Arg("\\\"", "\"", "\\\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	@Test
	public void testEscapeDoubleQuote2() throws Exception {
		execute1Arg("arg=\\\"bla\\\"", "arg=\"bla\"", "arg=\\\"bla\\\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	@Test
	public void testDoubleQuoted1() throws Exception {
		execute1Arg("\"1 2\"", "1 2"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testDoubleQuoted2() throws Exception {
		execute1Arg("\"1\"", "1", "1"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	@Test
	public void testDoubleQuoted3() throws Exception {
		if (Platform.getOS().equals(Constants.OS_WIN32)) {
//			execute1Arg("\"\"", "", "\"\""); // would be correct, but ProcessImpl is buggy on Windows JDKs
			execute1Arg("\"\""); //$NON-NLS-1$
		} else {
			execute1Arg("\"\"", "", "\"\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	@Test
	public void testDoubleQuoted4() throws Exception {
		if (Platform.getOS().equals(Constants.OS_WIN32)) {
			execute1Arg("\"\"\"\"", "\"", "\\\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		} else {
			execute1Arg("\"\"\"\"", "", "\"\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	@Test
	public void testDoubleQuoted5() throws Exception {
		execute1Arg("ab\"cd\"ef\"gh\"", "abcdefgh", "abcdefgh"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	@Test
	public void testDoubleQuotedWithSpace1() throws Exception {
		if (Platform.getOS().equals(Constants.OS_WIN32)) {
			execute1Arg("\"\"\"1\"\" 2\"", "\"1\" 2", "\"\\\"1\\\" 2\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		} else {
			execute1Arg("\"\"\"1\"\" 2\"", "1 2", "\"1 2\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	@Test
	public void testDoubleQuotedWithSpace2() throws Exception {
		execute1Arg("\"\\\"1\\\" 2\"", "\"1\" 2"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testSingleQuoted1() throws Exception {
		if (Platform.getOS().equals(Constants.OS_WIN32)) {
			execute("'1 2'", new String[] { "'1", "2'" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		} else {
			execute("'1 2'", new String[] { "1 2" }, "\"1 2\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	@Test
	public void testSingleQuoted2() throws Exception {
		if (Platform.getOS().equals(Constants.OS_WIN32)) {
			execute1Arg("'1'", "'1'", "'1'"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		} else {
			execute1Arg("'1'", "1", "1"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
	}

	@Test
	public void testWindows1() throws Exception {
		execute("\"a b c\" d e", new String[] { "a b c", "d", "e" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	@Test
	public void testWindows2() throws Exception {
		if (Platform.getOS().equals(Constants.OS_WIN32)) {
			execute("\"ab\\\"c\" \"\\\\\" d", new String[] { "ab\"c", "\\", "d" }, "ab\\\"c \\ d"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		} else {
			execute("\"ab\\\"c\" \"\\\\\" d", new String[] { "ab\"c", "\\", "d" }, "ab\\\"c \\\\ d"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}
	}

	@Test
	public void testWindows3() throws Exception {
		if (Platform.getOS().equals(Constants.OS_WIN32)) {
			execute("a\\\\\\b d\"e f\"g h", new String[] { "a\\\\\\b", "de fg", "h" }, "a\\\\\\b \"de fg\" h"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		} else {
			execute("a\\\\\\b d\"e f\"g h", new String[] { "a\\b", "de fg", "h" }, "a\\\\b \"de fg\" h"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}
	}

	@Test
	public void testWindows4() throws Exception {
		execute("a\\\\\\\"b c d", new String[] { "a\\\"b", "c", "d" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	@Test
	public void testWindows5() throws Exception {
		if (Platform.getOS().equals(Constants.OS_WIN32)) {
			execute("a\\\\\\\\\"b c\" d e", new String[] { "a\\\\b c", "d", "e" }, "\"a\\\\b c\" d e"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		} else {
			execute("a\\\\\\\\\"b c\" d e", new String[] { "a\\\\b c", "d", "e" }, "\"a\\\\\\\\b c\" d e"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}
	}

	@Test
	public void testAllInOne() throws Exception {
		if (Platform.getOS().equals(Constants.OS_WIN32)) {
			execute("1 \"\" 2 \" \" 3 \\\" 4 \"a b\" 5 \\\"bla\\\" 6 \"ab\"cd 7 ef\"gh\" 8 i\"\"j 9 \"x\\\"y\\\\\" 10 z\\\\z 11 \"two-quotes:\"\"\"\"\" 12 \"g\"\"h\" 13 \"\"\"a\"\" b\"", //$NON-NLS-1$
					new String[] { "1", "\"\"", "2", " ", "3", "\"", "4", "a b", "5", "\"bla\"", "6", "abcd", "7", "efgh", "8", "ij", "9", "x\"y\\", "10", "z\\\\z", "11", "two-quotes:\"\"", "12", "g\"h", "13", "\"a\" b" }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$ //$NON-NLS-14$ //$NON-NLS-15$ //$NON-NLS-16$ //$NON-NLS-17$ //$NON-NLS-18$ //$NON-NLS-19$ //$NON-NLS-20$ //$NON-NLS-21$ //$NON-NLS-22$ //$NON-NLS-23$ //$NON-NLS-24$ //$NON-NLS-25$ //$NON-NLS-26$
					"1 \"\" 2 \" \" 3 \\\" 4 \"a b\" 5 \\\"bla\\\" 6 abcd 7 efgh 8 ij 9 x\\\"y\\ 10 z\\\\z 11 two-quotes:\\\"\\\" 12 g\\\"h 13 \"\\\"a\\\" b\""); //$NON-NLS-1$
		} else {
			execute("1 \"\" 2 \" \" 3 \\\" 4 \"a b\" 5 \\\"bla\\\" 6 \"ab\"cd 7 ef\"gh\" 8 i\"\"j 9 \"x\\\"y\\\\\" 10 z\\\\z 11 \"two-quotes:\"\"\"\"\" 12 \"g\"\"h\" 13 \"\"\"a\"\" b\"", //$NON-NLS-1$
					new String[] { "1", "", "2", " ", "3", "\"", "4", "a b", "5", "\"bla\"", "6", "abcd", "7", "efgh", "8", "ij", "9", "x\"y\\", "10", "z\\z", "11", "two-quotes:", "12", "gh", "13", "a b" }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$ //$NON-NLS-11$ //$NON-NLS-12$ //$NON-NLS-13$ //$NON-NLS-14$ //$NON-NLS-15$ //$NON-NLS-16$ //$NON-NLS-17$ //$NON-NLS-18$ //$NON-NLS-19$ //$NON-NLS-20$ //$NON-NLS-21$ //$NON-NLS-22$ //$NON-NLS-23$ //$NON-NLS-24$ //$NON-NLS-25$ //$NON-NLS-26$
					"1 \"\" 2 \" \" 3 \\\" 4 \"a b\" 5 \\\"bla\\\" 6 abcd 7 efgh 8 ij 9 x\\\"y\\\\ 10 z\\\\z 11 two-quotes: 12 gh 13 \"a b\""); //$NON-NLS-1$
		}
	}

}
