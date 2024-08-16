/*******************************************************************************
 *  Copyright (c) 2000, 2021 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.tests.core.tests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.ant.tests.core.AbstractAntTest;
import org.eclipse.ant.tests.core.testplugin.AntTestChecker;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Version;

public class OptionTests extends AbstractAntTest {

	protected static final String UNKNOWN_ARG = "Unknown argument: "; //$NON-NLS-1$
	protected static final String START_OF_HELP = "ant [options] [target [target2 [target3] ...]]"; //$NON-NLS-1$
	protected static final String VERSION;
	protected static final String PLUGIN_VERSION;

	static {
		Version antVersion = Platform.getBundle("org.apache.ant").getVersion(); //$NON-NLS-1$
		VERSION = "Apache Ant(TM) version " + antVersion.getMajor() + '.' + antVersion.getMinor() + '.' //$NON-NLS-1$
				+ antVersion.getMicro();
		PLUGIN_VERSION = antVersion.toString();
	}

	/**
	 * Tests the "-help" option
	 */
	@Test
	public void testHelp() throws CoreException {
		run("TestForEcho.xml", new String[] { "-help" }); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(36, AntTestChecker.getDefault().getMessagesLoggedCount());
		assertTrue(getLastMessageLogged() != null
						&& AntTestChecker.getDefault().getMessages().get(0).startsWith(START_OF_HELP),
				"Help is incorrect"); //$NON-NLS-1$
	}

	/**
	 * Tests the "-h" option (help)
	 */
	@Test
	public void testMinusH() throws CoreException {
		run("TestForEcho.xml", new String[] { "-h" }); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(36, AntTestChecker.getDefault().getMessagesLoggedCount());
		assertTrue(
				getLastMessageLogged() != null
						&& AntTestChecker.getDefault().getMessages().get(0).startsWith(START_OF_HELP),
				"Help is incorrect"); //$NON-NLS-1$
	}

	/**
	 * Tests the "-version" option
	 */
	@Test
	public void testVersion() throws CoreException {
		run("TestForEcho.xml", new String[] { "-version" }); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(1, AntTestChecker.getDefault().getMessagesLoggedCount());
		assertThat(getLastMessageLogged()).startsWith(VERSION);
	}

	/**
	 * Tests the "-projecthelp" option
	 */
	@Test
	public void testProjecthelp() throws CoreException {
		run("TestForEcho.xml", new String[] { "-projecthelp" }); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(4, AntTestChecker.getDefault().getMessagesLoggedCount());
		assertThat(getLastMessageLogged()).startsWith("Default target:"); //$NON-NLS-1$
	}

	/**
	 * Tests the "-p" option (project help)
	 */
	@Test
	public void testMinusP() throws CoreException {
		run("TestForEcho.xml", new String[] { "-p" }); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(4, AntTestChecker.getDefault().getMessagesLoggedCount());
		assertThat(getLastMessageLogged()).startsWith("Default target:"); //$NON-NLS-1$
	}

	/**
	 * Tests the "-projecthelp" option when it will not show as much (quite mode)
	 */
	@Test
	public void testProjecthelpQuiet() throws CoreException {
		run("TestForEcho.xml", new String[] { "-projecthelp", "-q" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals(1, AntTestChecker.getDefault().getMessagesLoggedCount());
	}

	/**
	 * Tests the "-listener" option with a listener that is not an instance of
	 * BuildListener
	 */
	@Test
	public void testListenerBad() {
		CoreException ce = assertThrows(CoreException.class,
				() -> run("TestForEcho.xml", new String[] { "-listener", "java.lang.String" }), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				"A core exception should have occurred wrappering a class cast exception"); //$NON-NLS-1$
		String msg = ce.getMessage();
		assertEquals(
				"java.lang.String which was specified to be a build listener is not an instance of org.apache.tools.ant.BuildListener.", //$NON-NLS-1$
				msg);
	}

	/**
	 * Tests passing an unrecognized argument
	 */
	@Test
	public void testUnknownArg() throws CoreException {
		run("TestForEcho.xml", new String[] { "-listenr" }); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue(
				AntTestChecker.getDefault().getMessagesLoggedCount() == 6 && getLoggedMessage(5).startsWith(UNKNOWN_ARG)
						&& getLastMessageLogged().startsWith(BUILD_SUCCESSFUL),
				"Unrecognized option message should have been logged before successful build"); //$NON-NLS-1$
	}

	/**
	 * Tests specifying the -logfile with no arg
	 */
	@Test
	public void testLogFileWithNoArg() {
		assertThrows(CoreException.class, () -> run("TestForEcho.xml", new String[] { "-logfile" }), //$NON-NLS-1$ //$NON-NLS-2$
				"You must specify a log file when using the -log argument"); //$NON-NLS-1$
	}

	/**
	 * Tests specifying the -logfile
	 */
	@Test
	public void testLogFile() throws CoreException, IOException {
		run("TestForEcho.xml", new String[] { "-logfile", "TestLogFile.txt" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		IFile file = checkFileExists("TestLogFile.txt"); //$NON-NLS-1$

		try (InputStream stream = file.getContents();
				InputStreamReader in = new InputStreamReader(new BufferedInputStream(stream))) {
			StringBuilder buffer = new StringBuilder();
			char[] readBuffer = new char[2048];
			int n = in.read(readBuffer);
			while (n > 0) {
				buffer.append(readBuffer, 0, n);
				n = in.read(readBuffer);
			}
			assertThat(buffer.toString()).startsWith("Buildfile"); //$NON-NLS-1$
		}

	}

	/**
	 * Tests specifying the -logger with no arg
	 */
	@Test
	public void testLoggerWithNoArg() {
		assertThrows(CoreException.class, () -> run("TestForEcho.xml", new String[] { "-logger" }), //$NON-NLS-1$ //$NON-NLS-2$
				"You must specify a classname when using the -logger argument"); //$NON-NLS-1$
	}

	/**
	 * Tests the "-logger" option with a logger that is not an instance of
	 * BuildLogger
	 */
	@Test
	public void testLoggerBad() {
		assertThrows(CoreException.class, () -> run("TestForEcho.xml", new String[] { "-logger", "java.lang.String" }), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				"A core exception should have occurred wrappering a class cast exception"); //$NON-NLS-1$
	}

	/**
	 * Tests the "-logger" option with two loggers specified...only one is allowed
	 */
	@Test
	public void testTwoLoggers() {
		assertThrows(CoreException.class, () -> run("TestForEcho.xml", //$NON-NLS-1$
				new String[] { "-logger", "java.lang.String", "-q", "-logger", "java.lang.String" }), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
				"As only one logger can be specified"); //$NON-NLS-1$
	}

	/**
	 * Tests specifying the -listener with no arg
	 */
	@Test
	public void testListenerWithNoArg() {
		assertThrows(CoreException.class, () -> run("TestForEcho.xml", new String[] { "-listener" }), //$NON-NLS-1$ //$NON-NLS-2$
				"You must specify a listeners when using the -listener argument "); //$NON-NLS-1$
	}

	/**
	 * Tests specifying the -listener with a class that will not be found
	 */
	@Test
	public void testListenerClassNotFound() {
		CoreException e = assertThrows(CoreException.class,
				() -> run("TestForEcho.xml", new String[] { "-listener", "TestBuildListener" }), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				"A CoreException should have occurred as the listener class will not be found"); //$NON-NLS-1$
		String message = e.getStatus().getException().getMessage();
		assertEquals("java.lang.ClassNotFoundException: TestBuildListener", message); //$NON-NLS-1$

	}

	/**
	 * Tests specifying the -listener option
	 */
	@Test
	public void testListener() throws CoreException {
		run("TestForEcho.xml", new String[] { "-listener", ANT_TEST_BUILD_LISTENER }); //$NON-NLS-1$ //$NON-NLS-2$
		assertSuccessful();
		assertEquals(ANT_TEST_BUILD_LISTENER, AntTestChecker.getDefault().getLastListener());
	}

	/**
	 * Tests specifying the XmlLogger as a listener (bug 80435)
	 */
	@Test
	public void testXmlLoggerListener() throws CoreException, IOException {
		run("TestForEcho.xml", new String[] { "-listener", "org.apache.tools.ant.XmlLogger" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertSuccessful();

		// find the log file generated by the xml logger
		IFile file = checkFileExists("log.xml"); //$NON-NLS-1$
		InputStream stream = file.getContents();
		assertNotEquals(0, stream.available());
	}

	/**
	 * Tests specifying the -listener option multiple times...which is allowed
	 */
	@Test
	public void testListenerMultiple() throws CoreException {
		run("TestForEcho.xml", //$NON-NLS-1$
				new String[] { "-listener", ANT_TEST_BUILD_LISTENER, "-listener", ANT_TEST_BUILD_LISTENER }); //$NON-NLS-1$ //$NON-NLS-2$
		assertSuccessful();
		assertEquals(ANT_TEST_BUILD_LISTENER, AntTestChecker.getDefault().getLastListener());
		assertThat(AntTestChecker.getDefault().getListeners()).hasSize(2);
	}

	/**
	 * Tests specifying the -listener option multiple times, with one missing the
	 * arg
	 */
	@Test
	public void testListenerMultipleWithBad() {
		assertThrows(CoreException.class, () -> run("TestForEcho.xml", //$NON-NLS-1$
				new String[] { "-listener", ANT_TEST_BUILD_LISTENER, "-q", "-listener", "-verbose" }), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				"You must specify a listener for all -listener arguments"); //$NON-NLS-1$
	}

	/**
	 * Tests specifying the -buildfile with no arg
	 */
	@Test
	public void testBuildFileWithNoArg() {
		assertThrows(CoreException.class, () -> run("TestForEcho.xml", new String[] { "-buildfile" }), //$NON-NLS-1$ //$NON-NLS-2$
				"You must specify a buildfile when using the -buildfile argument"); //$NON-NLS-1$
	}

	/**
	 * Tests specifying the -buildfile
	 */
	@Test
	public void testBuildFile() throws CoreException {
		String buildFileName = getProject().getFolder("buildfiles").getFile("echoing.xml").getLocation().toFile() //$NON-NLS-1$ //$NON-NLS-2$
				.getAbsolutePath();
		run("TestForEcho.xml", new String[] { "-buildfile", buildFileName }, false, "buildfiles"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		assertEquals(1, AntTestChecker.getDefault().getTaskStartedCount());
	}

	/**
	 * Tests specifying a target at the command line that does not exist.
	 *
	 * @since 3.6 this will not fail - the default target will be run instead
	 * @since 3.8 this will fail as there are no more known targets
	 */
	@Test
	public void testSpecifyBadTargetAsArg() throws CoreException {
		run("TestForEcho.xml", new String[] { "echo2" }, false); //$NON-NLS-1$ //$NON-NLS-2$
		assertThat(AntTestChecker.getDefault().getLoggedMessage(1)).as("Should be an unknown target message") //$NON-NLS-1$
				.contains("Unknown target"); //$NON-NLS-1$
		assertThat(AntTestChecker.getDefault().getLoggedMessage(1)).as("Should be an unknown target message") //$NON-NLS-1$
				.contains("echo2"); //$NON-NLS-1$
		assertThat(AntTestChecker.getDefault().getLoggedMessage(0)).as("Should be a no known target message") //$NON-NLS-1$
				.contains("No known target specified."); //$NON-NLS-1$
		assertEquals(0, AntTestChecker.getDefault().getTargetsStartedCount(), "Should not have run any targets"); //$NON-NLS-1$
	}

	/**
	 * Tests specifying both a non-existent target and an existent target in the
	 * command line
	 */
	@Test
	public void testSpecifyBothBadAndGoodTargetsAsArg() throws CoreException {
		run("TestForEcho.xml", new String[] { "echo2", "Test for Echo" }, false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertThat(AntTestChecker.getDefault().getLoggedMessage(5)).as("Should be an unknown target message") //$NON-NLS-1$
				.contains("Unknown target"); //$NON-NLS-1$
		assertThat(AntTestChecker.getDefault().getLoggedMessage(5)).as("Should be an unknown target message") //$NON-NLS-1$
				.contains("echo2"); //$NON-NLS-1$
		assertEquals(5, AntTestChecker.getDefault().getTargetsStartedCount(),
				"Should have run the Test for Echo target"); //$NON-NLS-1$
	}

	/**
	 * Tests specifying a target at the command line
	 */
	@Test
	public void testSpecifyTargetAsArg() throws CoreException {
		run("echoing.xml", new String[] { "echo3" }, false); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals(3, AntTestChecker.getDefault().getMessagesLoggedCount());
		assertSuccessful();
	}

	/**
	 * Tests specifying a target at the command line with other options
	 */
	@Test
	public void testSpecifyTargetAsArgWithOtherOptions() throws CoreException {
		run("echoing.xml", new String[] { "-logfile", "TestLogFile.txt", "echo3" }, false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		assertEquals(4, AntTestChecker.getDefault().getMessagesLoggedCount());
		List<String> messages = AntTestChecker.getDefault().getMessages();
		// ensure that echo3 target executed and only that target
		assertEquals("echo3", messages.get(2), "echo3 target not executed"); //$NON-NLS-1$ //$NON-NLS-2$
		assertSuccessful();
	}

	/**
	 * Tests specifying targets at the command line with other options
	 */
	@Test
	public void testSpecifyTargetsAsArgWithOtherOptions() throws CoreException {
		run("echoing.xml", new String[] { "-logfile", "TestLogFile.txt", "echo2", "echo3" }, false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		assertEquals(5, AntTestChecker.getDefault().getMessagesLoggedCount());
		List<String> messages = AntTestChecker.getDefault().getMessages();
		// ensure that echo2 target executed
		assertEquals("echo2", messages.get(2), "echo2 target not executed"); //$NON-NLS-1$ //$NON-NLS-2$
		assertSuccessful();
	}

	/**
	 * Tests specifying a target at the command line and quiet reporting
	 */
	@Test
	public void testSpecifyTargetAsArgAndQuiet() throws CoreException {
		run("echoing.xml", new String[] { "-logfile", "TestLogFile.txt", "echo3", "-quiet" }, false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		assertEquals(2, AntTestChecker.getDefault().getMessagesLoggedCount());
	}

	/**
	 * Tests properties using "-D"
	 */
	@Test
	public void testMinusD() throws CoreException {
		run("echoing.xml", new String[] { "-DAntTests=testing", "-Declipse.is.cool=true" }, false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertSuccessful();
		assertEquals("true", AntTestChecker.getDefault().getUserProperty("eclipse.is.cool")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("testing", AntTestChecker.getDefault().getUserProperty("AntTests")); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull(AntTestChecker.getDefault().getUserProperty("my.name"), "my.name was not set and should be null"); //$NON-NLS-1$ //$NON-NLS-2$

	}

	/**
	 * Tests properties using "-D" and "-d" to specify debug
	 */
	@Test
	public void testMinusDMinusd() throws CoreException {
		run("echoing.xml", new String[] { "-d", "-DAntTests=testing", "-Declipse.is.cool=true" }, false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		assertSuccessful();
		assertEquals("true", AntTestChecker.getDefault().getUserProperty("eclipse.is.cool")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("testing", AntTestChecker.getDefault().getUserProperty("AntTests")); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull(AntTestChecker.getDefault().getUserProperty("my.name"), "my.name was not set and should be null"); //$NON-NLS-1$ //$NON-NLS-2$

	}

	@Test
	public void testMinusDAndGlobalProperties() throws CoreException {
		run("echoing.xml", new String[] { "-DAntTests=testing", "-Declipse.is.cool=true" }, false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertSuccessful();
		assertEquals("true", AntTestChecker.getDefault().getUserProperty("eclipse.running")); //$NON-NLS-1$ //$NON-NLS-2$
		assertNotNull("eclipse.home should have been set", AntTestChecker.getDefault().getUserProperty("eclipse.home")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests specifying a property such as "-D=emptyStringIsMyName Bug 37007
	 */
	@Test
	public void testMinusDEmpty() throws CoreException {
		run("echoing.xml", new String[] { "-D=emptyStringIsMyName", "-Declipse.is.cool=true" }, false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertSuccessful();
		assertEquals("true", AntTestChecker.getDefault().getUserProperty("eclipse.is.cool")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("emptyStringIsMyName", AntTestChecker.getDefault().getUserProperty("")); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull(AntTestChecker.getDefault().getUserProperty("my.name"), "my.name was not set and should be null"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests specifying properties that contain spaces Bug 37094
	 */
	@Test
	public void testMinusDWithSpaces() throws CoreException {
		run("echoing.xml", new String[] { "-DAntTests= testing", "-Declipse.is.cool=    true" }, false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertSuccessful();
		assertEquals("true", AntTestChecker.getDefault().getUserProperty("eclipse.is.cool")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("testing", AntTestChecker.getDefault().getUserProperty("AntTests")); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull(AntTestChecker.getDefault().getUserProperty("my.name"), "my.name was not set and should be null"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests specifying properties when the user has incorrectly specified "-Debug"
	 * Bug 40935
	 */
	@Test
	public void testPropertiesWithMinusDebug() throws CoreException {
		run("echoing.xml", new String[] { "-Debug", "-DAntTests= testing", "-Declipse.is.cool=    true" }, false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		assertEquals("Unknown argument: -Debug", AntTestChecker.getDefault().getMessages().get(0)); //$NON-NLS-1$
		assertSuccessful();
		assertEquals("true", AntTestChecker.getDefault().getUserProperty("eclipse.is.cool")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("testing", AntTestChecker.getDefault().getUserProperty("AntTests")); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull(AntTestChecker.getDefault().getUserProperty("my.name"), "my.name was not set and should be null"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests when the user has incorrectly specified "-Debug" Bug 40935
	 */
	@Test
	public void testMinusDebug() throws CoreException {
		run("echoing.xml", new String[] { "-Debug" }); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("Unknown argument: -Debug", AntTestChecker.getDefault().getMessages().get(0)); //$NON-NLS-1$
		assertSuccessful();
	}

	@Test
	public void testPropertyFileWithNoArg() {
		assertThrows(CoreException.class, () -> run("TestForEcho.xml", new String[] { "-propertyfile" }), //$NON-NLS-1$ //$NON-NLS-2$
				"You must specify a property filename when using the -propertyfile argument"); //$NON-NLS-1$
		String msg = AntTestChecker.getDefault().getMessages().get(0);
		assertEquals("You must specify a property filename when using the -propertyfile argument", msg); //$NON-NLS-1$
	}

	/**
	 * A build should succeed when a property file is not found. The error is
	 * reported and the build continues.
	 */
	@Test
	public void testPropertyFileFileNotFound() throws CoreException {

		run("TestForEcho.xml", new String[] { "-propertyfile", "qq.txt" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertSuccessful();
		String msg = AntTestChecker.getDefault().getMessages().get(0);
		assertThat(msg).startsWith("Could not load property file:"); //$NON-NLS-1$
	}

	@Test
	public void testPropertyFile() throws CoreException {
		run("TestForEcho.xml", new String[] { "-propertyfile", getPropertyFileName() }); //$NON-NLS-1$ //$NON-NLS-2$
		assertSuccessful();
		assertEquals("Yep", AntTestChecker.getDefault().getUserProperty("eclipse.is.cool")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("testing from properties file", AntTestChecker.getDefault().getUserProperty("AntTests")); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull(AntTestChecker.getDefault().getUserProperty("my.name"), "my.name was not set and should be null"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testPropertyFileWithMinusDTakingPrecedence() throws CoreException {
		run("echoing.xml", //$NON-NLS-1$
				new String[] { "-propertyfile", getPropertyFileName(), "-DAntTests=testing", "-Declipse.is.cool=true" }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				false);
		assertSuccessful();
		assertEquals("true", AntTestChecker.getDefault().getUserProperty("eclipse.is.cool")); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("testing", AntTestChecker.getDefault().getUserProperty("AntTests")); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull(AntTestChecker.getDefault().getUserProperty("my.name"), "my.name was not set and should be null"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testInputHandlerWithNoArg() {
		CoreException ce = assertThrows(CoreException.class,
				() -> run("TestForEcho.xml", new String[] { "-inputhandler" }), //$NON-NLS-1$ //$NON-NLS-2$
				"You must specify a classname when using the -inputhandler argument"); //$NON-NLS-1$
		String msg = ce.getMessage();
		assertEquals("You must specify a classname when using the -inputhandler argument", msg); //$NON-NLS-1$
	}

	/**
	 * Tests the "-inputhandler" option with two handlers specified...only one is
	 * allowed
	 */
	@Test
	public void testInputHandlerMultiple() {
		CoreException ce = assertThrows(CoreException.class, () -> run("TestForEcho.xml", //$NON-NLS-1$
				new String[] { "-inputhandler", "org.apache.tools.ant.input.DefaultInputHandler", "-q", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						"-inputhandler", //$NON-NLS-1$
						"org.apache.tools.ant.input.DefaultInputHandler" }), //$NON-NLS-1$
				"As only one input handler can be specified"); //$NON-NLS-1$
		String msg = ce.getMessage();
		assertEquals("Only one input handler class may be specified.", msg); //$NON-NLS-1$
	}

	/**
	 * Tests the "-inputhandler" option with a input handler that is not an instance
	 * of InputHandler
	 */
	@Test
	public void testInputHandlerBad() {
		CoreException ce = assertThrows(CoreException.class,
				() -> run("TestForEcho.xml", new String[] { "-inputhandler", "java.lang.StringBuffer" }), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				"Incorrect inputhandler"); //$NON-NLS-1$
		String msg = ce.getMessage();
		assertEquals(
				"The specified input handler class java.lang.StringBuffer does not implement the org.apache.tools.ant.input.InputHandler interface", //$NON-NLS-1$
				msg);
	}

	/**
	 * Tests the "-inputhandler" option with a input handler that is not a defined
	 * class
	 */
	@Test
	public void testInputHandlerBad2() {
		CoreException ce = assertThrows(CoreException.class,
				() -> run("TestForEcho.xml", new String[] { "-inputhandler", "ja.lang.StringBuffer" }), //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				"Incorrect inputhandler"); //$NON-NLS-1$
		String msg = ce.getMessage();
		assertThat(msg).startsWith("Unable to instantiate specified input handler class ja.lang.StringBuffer"); //$NON-NLS-1$
	}

	/**
	 * Tests the "-inputhandler" option with a test input handler and the -noinput
	 * option
	 */
	@Test
	public void testInputHandlerWithMinusNoInput() {
		CoreException ce = assertThrows(CoreException.class, () -> run("input.xml", new String[] { "-inputhandler", //$NON-NLS-1$ //$NON-NLS-2$
				"org.eclipse.ant.tests.core.support.inputHandlers.AntTestInputHandler", "-noinput" }), //$NON-NLS-1$ //$NON-NLS-2$
				"Build should have failed"); //$NON-NLS-1$
		assertThat(ce.getMessage())
				.endsWith("Unable to respond to input request likely as a result of specifying the -noinput command"); //$NON-NLS-1$
	}

	/**
	 * Tests the -noinput option with the default input handler
	 */
	@Test
	public void testMinusNoInput() {
		CoreException ce = assertThrows(CoreException.class, () -> run("input.xml", new String[] { "-noinput" }), //$NON-NLS-1$ //$NON-NLS-2$
				"Build should have failed"); //$NON-NLS-1$
		assertThat(ce.getMessage()).endsWith("Failed to read input from Console."); //$NON-NLS-1$
	}

	/**
	 * Tests the "-inputhandler" option with a test input handler Order after the
	 * noinput tests so that we test we are resetting the system property
	 */
	@Test
	public void testInputHandler() throws CoreException {
		run("input.xml", new String[] { "-inputhandler", //$NON-NLS-1$ //$NON-NLS-2$
				"org.eclipse.ant.tests.core.support.inputHandlers.AntTestInputHandler" }); //$NON-NLS-1$
		assertSuccessful();
		String msg = AntTestChecker.getDefault().getMessages().get(1);
		assertEquals("testing handling input requests", msg); //$NON-NLS-1$

	}

	/**
	 * Tests the "-diagnostics" option with no ANT_HOME set bug 25693
	 */
	@Test
	public void testDiagnosticsWithNoAntHome() throws CoreException {
		try {
			AntCorePlugin.getPlugin().getPreferences().setAntHome(null);
			run("input.xml", new String[] { "-diagnostics" }); //$NON-NLS-1$ //$NON-NLS-2$

			String msg = AntTestChecker.getDefault().getMessages().get(0);
			assertEquals("------- Ant diagnostics report -------", msg); //$NON-NLS-1$
		} finally {
			restorePreferenceDefaults();
		}
	}

	/**
	 * Tests the "-diagnostics" option with ANT_HOME set bug 25693
	 */
	@Test
	public void testDiagnostics() throws CoreException {

		try {
			run("input.xml", new String[] { "-diagnostics" }); //$NON-NLS-1$ //$NON-NLS-2$
		} finally {
			restorePreferenceDefaults();
		}
		// we are looking for the ant.home entry
		List<String> messages = AntTestChecker.getDefault().getMessages();
		String msg = messages.get(17);
		// msg depends on whether self hosting testing or build testing
		assertAntHomeMessage(msg);
	}

	private void assertAntHomeMessage(String message) {
		assertThat(message).satisfiesAnyOf( //
				it -> assertThat(it).endsWith("org.apache.ant"), //$NON-NLS-1$
				it -> assertThat(it).endsWith(PLUGIN_VERSION), it -> {
					// org.apache.ant_1.7.1.v200704241635
					int index = it.lastIndexOf('.');
					if (index > 0) {
						it = it.substring(0, index);
					}
					assertThat(it).endsWith(PLUGIN_VERSION);

				});
	}

	/**
	 * Tests the "-quiet" still reports build successful bug 34488
	 */
	@Test
	public void testMinusQuiet() throws CoreException {
		run("TestForEcho.xml", new String[] { "-quiet" }); //$NON-NLS-1$ //$NON-NLS-2$
		assertSuccessful();
	}

	/**
	 * Tests the "-keep-going" option
	 */
	@Test
	public void testMinusKeepGoing() {
		assertThrows(CoreException.class, () -> run("failingTarget.xml", new String[] { "-keep-going" }, false), //$NON-NLS-1$ //$NON-NLS-2$
				"The build should have failed"); //$NON-NLS-1$
		assertEquals(4, AntTestChecker.getDefault().getMessagesLoggedCount());
		assertEquals("Still echo on failure", AntTestChecker.getDefault().getLoggedMessage(1)); //$NON-NLS-1$
	}

	/**
	 * Tests the "-k" option
	 */
	@Test
	public void testMinusK() {
		assertThrows(CoreException.class, () -> run("failingTarget.xml", new String[] { "-k" }, false), //$NON-NLS-1$ //$NON-NLS-2$
				"The build should have failed"); //$NON-NLS-1$
		assertEquals(4, AntTestChecker.getDefault().getMessagesLoggedCount());
		assertEquals("Still echo on failure", AntTestChecker.getDefault().getLoggedMessage(1)); //$NON-NLS-1$
	}
}