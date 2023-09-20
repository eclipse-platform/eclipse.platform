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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

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
import org.junit.Test;
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
		assertEquals("incorrect message number logged", 36, AntTestChecker.getDefault().getMessagesLoggedCount()); //$NON-NLS-1$
		assertTrue("Help is incorrect", getLastMessageLogged() != null //$NON-NLS-1$
				&& AntTestChecker.getDefault().getMessages().get(0).startsWith(START_OF_HELP));
	}

	/**
	 * Tests the "-h" option (help)
	 */
	@Test
	public void testMinusH() throws CoreException {
		run("TestForEcho.xml", new String[] { "-h" }); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("incorrect message number logged", 36, AntTestChecker.getDefault().getMessagesLoggedCount()); //$NON-NLS-1$
		assertTrue("Help is incorrect", getLastMessageLogged() != null //$NON-NLS-1$
				&& AntTestChecker.getDefault().getMessages().get(0).startsWith(START_OF_HELP));
	}

	/**
	 * Tests the "-version" option
	 */
	@Test
	public void testVersion() throws CoreException {
		run("TestForEcho.xml", new String[] { "-version" }); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("One message should have been logged", 1, AntTestChecker.getDefault().getMessagesLoggedCount()); //$NON-NLS-1$
		assertTrue("Version is incorrect", getLastMessageLogged().startsWith(VERSION)); //$NON-NLS-1$
	}

	/**
	 * Tests the "-projecthelp" option
	 */
	@Test
	public void testProjecthelp() throws CoreException {
		run("TestForEcho.xml", new String[] { "-projecthelp" }); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("Incorrect number of messages", 4, AntTestChecker.getDefault().getMessagesLoggedCount()); //$NON-NLS-1$
		assertTrue("Project help is incorrect", getLastMessageLogged().startsWith("Default target:")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests the "-p" option (project help)
	 */
	@Test
	public void testMinusP() throws CoreException {
		run("TestForEcho.xml", new String[] { "-p" }); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("Incorrect number of messages", 4, AntTestChecker.getDefault().getMessagesLoggedCount()); //$NON-NLS-1$
		assertTrue("Project help is incorrect", getLastMessageLogged().startsWith("Default target:")); //$NON-NLS-1$ //$NON-NLS-2$
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
		CoreException ce = assertThrows("A core exception should have occurred wrappering a class cast exception", //$NON-NLS-1$
				CoreException.class, () -> run("TestForEcho.xml", new String[] { "-listener", "java.lang.String" })); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		String msg = ce.getMessage();
		assertTrue("Message incorrect!: " //$NON-NLS-1$
				+ msg,
				msg.equals(
						"java.lang.String which was specified to be a build listener is not an instance of org.apache.tools.ant.BuildListener.")); //$NON-NLS-1$
	}

	/**
	 * Tests passing an unrecognized argument
	 */
	@Test
	public void testUnknownArg() throws CoreException {
		run("TestForEcho.xml", new String[] { "-listenr" }); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Unrecognized option message should have been logged before successful build", //$NON-NLS-1$
				(AntTestChecker.getDefault().getMessagesLoggedCount() == 6)
						&& (getLoggedMessage(5).startsWith(UNKNOWN_ARG))
						&& (getLastMessageLogged().startsWith(BUILD_SUCCESSFUL)));
	}

	/**
	 * Tests specifying the -logfile with no arg
	 */
	@Test
	public void testLogFileWithNoArg() {
		assertThrows("You must specify a log file when using the -log argument", CoreException.class, //$NON-NLS-1$
				() -> run("TestForEcho.xml", new String[] { "-logfile" })); //$NON-NLS-1$ //$NON-NLS-2$
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
			assertTrue("File should have started with Buildfile", buffer.toString().startsWith("Buildfile")); //$NON-NLS-1$ //$NON-NLS-2$
		}

	}

	/**
	 * Tests specifying the -logger with no arg
	 */
	@Test
	public void testLoggerWithNoArg() {
		assertThrows("You must specify a classname when using the -logger argument", CoreException.class, //$NON-NLS-1$
				() -> run("TestForEcho.xml", new String[] { "-logger" })); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests the "-logger" option with a logger that is not an instance of
	 * BuildLogger
	 */
	@Test
	public void testLoggerBad() {
		assertThrows("A core exception should have occurred wrappering a class cast exception", CoreException.class, //$NON-NLS-1$
				() -> run("TestForEcho.xml", new String[] { "-logger", "java.lang.String" })); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	/**
	 * Tests the "-logger" option with two loggers specified...only one is allowed
	 */
	@Test
	public void testTwoLoggers() {
		assertThrows("As only one logger can be specified", CoreException.class, () -> run("TestForEcho.xml", //$NON-NLS-1$ //$NON-NLS-2$
				new String[] { "-logger", "java.lang.String", "-q", "-logger", "java.lang.String" })); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	}

	/**
	 * Tests specifying the -listener with no arg
	 */
	@Test
	public void testListenerWithNoArg() {
		assertThrows("You must specify a listeners when using the -listener argument ", CoreException.class, //$NON-NLS-1$
				() -> run("TestForEcho.xml", new String[] { "-listener" })); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests specifying the -listener with a class that will not be found
	 */
	@Test
	public void testListenerClassNotFound() {
		CoreException e = assertThrows("A CoreException should have occurred as the listener class will not be found", //$NON-NLS-1$
				CoreException.class, () -> run("TestForEcho.xml", new String[] { "-listener", "TestBuildListener" })); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		String message = e.getStatus().getException().getMessage();
		assertTrue("Should be ClassNotFoundException", //$NON-NLS-1$
				"java.lang.ClassNotFoundException: TestBuildListener".equals(message)); //$NON-NLS-1$

	}

	/**
	 * Tests specifying the -listener option
	 */
	@Test
	public void testListener() throws CoreException {
		run("TestForEcho.xml", new String[] { "-listener", ANT_TEST_BUILD_LISTENER }); //$NON-NLS-1$ //$NON-NLS-2$
		assertSuccessful();
		assertTrue("A listener should have been added named: " //$NON-NLS-1$
				+ ANT_TEST_BUILD_LISTENER,
				ANT_TEST_BUILD_LISTENER.equals(AntTestChecker.getDefault().getLastListener()));
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
		assertTrue("A listener should have been added named: " //$NON-NLS-1$
				+ ANT_TEST_BUILD_LISTENER,
				ANT_TEST_BUILD_LISTENER.equals(AntTestChecker.getDefault().getLastListener()));
		assertEquals("Two listeners should have been added", 2, AntTestChecker.getDefault().getListeners().size()); //$NON-NLS-1$
	}

	/**
	 * Tests specifying the -listener option multiple times, with one missing the
	 * arg
	 */
	@Test
	public void testListenerMultipleWithBad() {
		assertThrows("You must specify a listener for all -listener arguments ", CoreException.class, //$NON-NLS-1$
				() -> run("TestForEcho.xml", //$NON-NLS-1$
						new String[] { "-listener", ANT_TEST_BUILD_LISTENER, "-q", "-listener", "-verbose" })); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	/**
	 * Tests specifying the -buildfile with no arg
	 */
	@Test
	public void testBuildFileWithNoArg() {
		assertThrows("You must specify a buildfile when using the -buildfile argument", CoreException.class, //$NON-NLS-1$
				() -> run("TestForEcho.xml", new String[] { "-buildfile" })); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests specifying the -buildfile
	 */
	@Test
	public void testBuildFile() throws CoreException {
		String buildFileName = getProject().getFolder("buildfiles").getFile("echoing.xml").getLocation().toFile() //$NON-NLS-1$ //$NON-NLS-2$
				.getAbsolutePath();
		run("TestForEcho.xml", new String[] { "-buildfile", buildFileName }, false, "buildfiles"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		assertEquals("Should have been 1 tasks, was: " //$NON-NLS-1$
				+ AntTestChecker.getDefault().getTaskStartedCount(), 1,
				AntTestChecker.getDefault().getTaskStartedCount());
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
		assertTrue("Should be an unknown target message", //$NON-NLS-1$
				AntTestChecker.getDefault().getLoggedMessage(1).contains("Unknown target")); //$NON-NLS-1$
		assertTrue("Should be an unknown target message", //$NON-NLS-1$
				AntTestChecker.getDefault().getLoggedMessage(1).contains("echo2")); //$NON-NLS-1$
		assertTrue("Should be a no known target message", //$NON-NLS-1$
				AntTestChecker.getDefault().getLoggedMessage(0).contains("No known target specified.")); //$NON-NLS-1$
		assertEquals("Should not have run any targets", 0, AntTestChecker.getDefault().getTargetsStartedCount()); //$NON-NLS-1$
	}

	/**
	 * Tests specifying both a non-existent target and an existent target in the
	 * command line
	 * 
	 */
	@Test
	public void testSpecifyBothBadAndGoodTargetsAsArg() throws CoreException {
		run("TestForEcho.xml", new String[] { "echo2", "Test for Echo" }, false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("Should be an unknown target message", //$NON-NLS-1$
				AntTestChecker.getDefault().getLoggedMessage(5).contains("Unknown target")); //$NON-NLS-1$
		assertTrue("Should be an unknown target message", //$NON-NLS-1$
				AntTestChecker.getDefault().getLoggedMessage(5).contains("echo2")); //$NON-NLS-1$
		assertEquals("Should have run the Test for Echo target", 5, //$NON-NLS-1$
				AntTestChecker.getDefault().getTargetsStartedCount());
	}

	/**
	 * Tests specifying a target at the command line
	 */
	@Test
	public void testSpecifyTargetAsArg() throws CoreException {
		run("echoing.xml", new String[] { "echo3" }, false); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("3 messages should have been logged; was " //$NON-NLS-1$
				+ AntTestChecker.getDefault().getMessagesLoggedCount(), 3,
				AntTestChecker.getDefault().getMessagesLoggedCount());
		assertSuccessful();
	}

	/**
	 * Tests specifying a target at the command line with other options
	 */
	@Test
	public void testSpecifyTargetAsArgWithOtherOptions() throws CoreException {
		run("echoing.xml", new String[] { "-logfile", "TestLogFile.txt", "echo3" }, false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		assertEquals("4 messages should have been logged; was " //$NON-NLS-1$
				+ AntTestChecker.getDefault().getMessagesLoggedCount(), 4,
				AntTestChecker.getDefault().getMessagesLoggedCount());
		List<String> messages = AntTestChecker.getDefault().getMessages();
		// ensure that echo3 target executed and only that target
		assertTrue("echo3 target not executed", messages.get(2).equals("echo3")); //$NON-NLS-1$ //$NON-NLS-2$
		assertSuccessful();
	}

	/**
	 * Tests specifying targets at the command line with other options
	 */
	@Test
	public void testSpecifyTargetsAsArgWithOtherOptions() throws CoreException {
		run("echoing.xml", new String[] { "-logfile", "TestLogFile.txt", "echo2", "echo3" }, false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		assertEquals("5 messages should have been logged; was " //$NON-NLS-1$
				+ AntTestChecker.getDefault().getMessagesLoggedCount(), 5,
				AntTestChecker.getDefault().getMessagesLoggedCount());
		List<String> messages = AntTestChecker.getDefault().getMessages();
		// ensure that echo2 target executed
		assertTrue("echo2 target not executed", messages.get(2).equals("echo2")); //$NON-NLS-1$ //$NON-NLS-2$
		assertSuccessful();
	}

	/**
	 * Tests specifying a target at the command line and quiet reporting
	 */
	@Test
	public void testSpecifyTargetAsArgAndQuiet() throws CoreException {
		run("echoing.xml", new String[] { "-logfile", "TestLogFile.txt", "echo3", "-quiet" }, false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		assertEquals("2 messages should have been logged; was " //$NON-NLS-1$
				+ AntTestChecker.getDefault().getMessagesLoggedCount(), 2,
				AntTestChecker.getDefault().getMessagesLoggedCount());
	}

	/**
	 * Tests properties using "-D"
	 */
	@Test
	public void testMinusD() throws CoreException {
		run("echoing.xml", new String[] { "-DAntTests=testing", "-Declipse.is.cool=true" }, false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertSuccessful();
		assertTrue("eclipse.is.cool should have been set as true", //$NON-NLS-1$
				"true".equals(AntTestChecker.getDefault().getUserProperty("eclipse.is.cool"))); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("AntTests should have a value of testing", //$NON-NLS-1$
				"testing".equals(AntTestChecker.getDefault().getUserProperty("AntTests"))); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("my.name was not set and should be null", AntTestChecker.getDefault().getUserProperty("my.name")); //$NON-NLS-1$ //$NON-NLS-2$

	}

	/**
	 * Tests properties using "-D" and "-d" to specify debug
	 */
	@Test
	public void testMinusDMinusd() throws CoreException {
		run("echoing.xml", new String[] { "-d", "-DAntTests=testing", "-Declipse.is.cool=true" }, false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		assertSuccessful();
		assertTrue("eclipse.is.cool should have been set as true", //$NON-NLS-1$
				"true".equals(AntTestChecker.getDefault().getUserProperty("eclipse.is.cool"))); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("AntTests should have a value of testing", //$NON-NLS-1$
				"testing".equals(AntTestChecker.getDefault().getUserProperty("AntTests"))); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("my.name was not set and should be null", AntTestChecker.getDefault().getUserProperty("my.name")); //$NON-NLS-1$ //$NON-NLS-2$

	}

	@Test
	public void testMinusDAndGlobalProperties() throws CoreException {
		run("echoing.xml", new String[] { "-DAntTests=testing", "-Declipse.is.cool=true" }, false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertSuccessful();
		assertTrue("eclipse.running should have been set as true", //$NON-NLS-1$
				"true".equals(AntTestChecker.getDefault().getUserProperty("eclipse.running"))); //$NON-NLS-1$ //$NON-NLS-2$
		assertNotNull("eclipse.home should have been set", AntTestChecker.getDefault().getUserProperty("eclipse.home")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests specifying a property such as "-D=emptyStringIsMyName Bug 37007
	 */
	@Test
	public void testMinusDEmpty() throws CoreException {
		run("echoing.xml", new String[] { "-D=emptyStringIsMyName", "-Declipse.is.cool=true" }, false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertSuccessful();
		assertTrue("eclipse.is.cool should have been set as true", //$NON-NLS-1$
				"true".equals(AntTestChecker.getDefault().getUserProperty("eclipse.is.cool"))); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("\"\" should have a value of emptyStringIsMyName", //$NON-NLS-1$
				"emptyStringIsMyName".equals(AntTestChecker.getDefault().getUserProperty(""))); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("my.name was not set and should be null", AntTestChecker.getDefault().getUserProperty("my.name")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests specifying properties that contain spaces Bug 37094
	 */
	@Test
	public void testMinusDWithSpaces() throws CoreException {
		run("echoing.xml", new String[] { "-DAntTests= testing", "-Declipse.is.cool=    true" }, false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertSuccessful();
		assertTrue("eclipse.is.cool should have been set as true", //$NON-NLS-1$
				"true".equals(AntTestChecker.getDefault().getUserProperty("eclipse.is.cool"))); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("AntTests should have a value of testing", //$NON-NLS-1$
				"testing".equals(AntTestChecker.getDefault().getUserProperty("AntTests"))); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("my.name was not set and should be null", AntTestChecker.getDefault().getUserProperty("my.name")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests specifying properties when the user has incorrectly specified "-Debug"
	 * Bug 40935
	 */
	@Test
	public void testPropertiesWithMinusDebug() throws CoreException {
		run("echoing.xml", new String[] { "-Debug", "-DAntTests= testing", "-Declipse.is.cool=    true" }, false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		assertTrue("\"-Debug\" should be flagged as an unknown argument", //$NON-NLS-1$
				"Unknown argument: -Debug".equals(AntTestChecker.getDefault().getMessages().get(0))); //$NON-NLS-1$
		assertSuccessful();
		assertTrue("eclipse.is.cool should have been set as true", //$NON-NLS-1$
				"true".equals(AntTestChecker.getDefault().getUserProperty("eclipse.is.cool"))); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("AntTests should have a value of testing", //$NON-NLS-1$
				"testing".equals(AntTestChecker.getDefault().getUserProperty("AntTests"))); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("my.name was not set and should be null", AntTestChecker.getDefault().getUserProperty("my.name")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests when the user has incorrectly specified "-Debug" Bug 40935
	 */
	@Test
	public void testMinusDebug() throws CoreException {
		run("echoing.xml", new String[] { "-Debug" }); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("\"-Debug\" should be flagged as an unknown argument", //$NON-NLS-1$
				"Unknown argument: -Debug".equals(AntTestChecker.getDefault().getMessages().get(0))); //$NON-NLS-1$
		assertSuccessful();
	}

	@Test
	public void testPropertyFileWithNoArg() {
		assertThrows("You must specify a property filename when using the -propertyfile argument", CoreException.class, //$NON-NLS-1$
				() -> run("TestForEcho.xml", new String[] { "-propertyfile" })); //$NON-NLS-1$ //$NON-NLS-2$
		String msg = AntTestChecker.getDefault().getMessages().get(0);
		assertTrue("Message incorrect!: " + msg, //$NON-NLS-1$
				msg.equals("You must specify a property filename when using the -propertyfile argument")); //$NON-NLS-1$
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
		assertTrue("Message incorrect!: " + msg, msg.startsWith("Could not load property file:")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testPropertyFile() throws CoreException {
		run("TestForEcho.xml", new String[] { "-propertyfile", getPropertyFileName() }); //$NON-NLS-1$ //$NON-NLS-2$
		assertSuccessful();
		assertTrue("eclipse.is.cool should have been set as Yep", //$NON-NLS-1$
				"Yep".equals(AntTestChecker.getDefault().getUserProperty("eclipse.is.cool"))); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("AntTests should have a value of testing", //$NON-NLS-1$
				"testing from properties file".equals(AntTestChecker.getDefault().getUserProperty("AntTests"))); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("my.name was not set and should be null", AntTestChecker.getDefault().getUserProperty("my.name")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testPropertyFileWithMinusDTakingPrecedence() throws CoreException {
		run("echoing.xml", //$NON-NLS-1$
				new String[] { "-propertyfile", getPropertyFileName(), "-DAntTests=testing", "-Declipse.is.cool=true" }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				false);
		assertSuccessful();
		assertTrue("eclipse.is.cool should have been set as true", //$NON-NLS-1$
				"true".equals(AntTestChecker.getDefault().getUserProperty("eclipse.is.cool"))); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("AntTests should have a value of testing", //$NON-NLS-1$
				"testing".equals(AntTestChecker.getDefault().getUserProperty("AntTests"))); //$NON-NLS-1$ //$NON-NLS-2$
		assertNull("my.name was not set and should be null", AntTestChecker.getDefault().getUserProperty("my.name")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testInputHandlerWithNoArg() {
		CoreException ce = assertThrows("You must specify a classname when using the -inputhandler argument", //$NON-NLS-1$
				CoreException.class, () -> run("TestForEcho.xml", new String[] { "-inputhandler" })); //$NON-NLS-1$ //$NON-NLS-2$
		String msg = ce.getMessage();
		assertTrue("Message incorrect!: " + msg, //$NON-NLS-1$
				msg.equals("You must specify a classname when using the -inputhandler argument")); //$NON-NLS-1$
	}

	/**
	 * Tests the "-inputhandler" option with two handlers specified...only one is
	 * allowed
	 */
	@Test
	public void testInputHandlerMultiple() {
		CoreException ce = assertThrows("As only one input handler can be specified", CoreException.class, //$NON-NLS-1$
				() -> run("TestForEcho.xml", //$NON-NLS-1$
						new String[] { "-inputhandler", "org.apache.tools.ant.input.DefaultInputHandler", "-q", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								"-inputhandler", //$NON-NLS-1$
								"org.apache.tools.ant.input.DefaultInputHandler" })); //$NON-NLS-1$
		String msg = ce.getMessage();
		assertTrue("Message incorrect!: " + msg, msg.equals("Only one input handler class may be specified.")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests the "-inputhandler" option with a input handler that is not an instance
	 * of InputHandler
	 */
	@Test
	public void testInputHandlerBad() {
		CoreException ce = assertThrows("Incorrect inputhandler", CoreException.class, //$NON-NLS-1$
				() -> run("TestForEcho.xml", new String[] { "-inputhandler", "java.lang.StringBuffer" })); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		String msg = ce.getMessage();
		assertTrue("Message incorrect!: " //$NON-NLS-1$
				+ msg,
				msg.equals(
						"The specified input handler class java.lang.StringBuffer does not implement the org.apache.tools.ant.input.InputHandler interface")); //$NON-NLS-1$
	}

	/**
	 * Tests the "-inputhandler" option with a input handler that is not a defined
	 * class
	 */
	@Test
	public void testInputHandlerBad2() {
		CoreException ce = assertThrows("Incorrect inputhandler", CoreException.class, //$NON-NLS-1$
				() -> run("TestForEcho.xml", new String[] { "-inputhandler", "ja.lang.StringBuffer" })); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		String msg = ce.getMessage();
		assertTrue("Message incorrect!: " + msg, //$NON-NLS-1$
				msg.startsWith("Unable to instantiate specified input handler class ja.lang.StringBuffer")); //$NON-NLS-1$
	}

	/**
	 * Tests the "-inputhandler" option with a test input handler and the -noinput
	 * option
	 */
	@Test
	public void testInputHandlerWithMinusNoInput() {
		CoreException ce = assertThrows("Build should have failed", CoreException.class, //$NON-NLS-1$
				() -> run("input.xml", new String[] { "-inputhandler", //$NON-NLS-1$ //$NON-NLS-2$
						"org.eclipse.ant.tests.core.support.inputHandlers.AntTestInputHandler", "-noinput" })); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Message incorrect: " //$NON-NLS-1$
				+ ce.getMessage(),
				ce.getMessage().endsWith(
						"Unable to respond to input request likely as a result of specifying the -noinput command")); //$NON-NLS-1$
	}

	/**
	 * Tests the -noinput option with the default input handler
	 */
	@Test
	public void testMinusNoInput() {
		CoreException ce = assertThrows("Build should have failed", CoreException.class, //$NON-NLS-1$
				() -> run("input.xml", new String[] { "-noinput" })); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Message incorrect: " + ce.getMessage(), //$NON-NLS-1$
				ce.getMessage().endsWith("Failed to read input from Console.")); //$NON-NLS-1$
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
		assertTrue("Message incorrect: " + msg, msg.equals("testing handling input requests")); //$NON-NLS-1$ //$NON-NLS-2$

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
			assertTrue("Message incorrect: " + msg, msg.equals("------- Ant diagnostics report -------")); //$NON-NLS-1$ //$NON-NLS-2$
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
		assertTrue("Message incorrect: " + msg, checkAntHomeMessage(msg)); //$NON-NLS-1$
	}

	private boolean checkAntHomeMessage(String message) {
		String msg = message;
		if (msg.endsWith("org.apache.ant")) { //$NON-NLS-1$
			return true;
		}

		if (msg.endsWith(PLUGIN_VERSION)) {
			return true;
		}

		// org.apache.ant_1.7.1.v200704241635
		int index = msg.lastIndexOf('.');
		if (index > 0) {
			msg = msg.substring(0, index);
		}
		return msg.endsWith(PLUGIN_VERSION);
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
		assertThrows("The build should have failed", CoreException.class, //$NON-NLS-1$
				() -> run("failingTarget.xml", new String[] { "-keep-going" }, false)); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("4 messages should have been logged; was " //$NON-NLS-1$
				+ AntTestChecker.getDefault().getMessagesLoggedCount(), 4,
				AntTestChecker.getDefault().getMessagesLoggedCount());
		assertTrue("Incorrect message:" //$NON-NLS-1$
				+ AntTestChecker.getDefault().getLoggedMessage(1),
				"Still echo on failure".equals(AntTestChecker.getDefault().getLoggedMessage(1))); //$NON-NLS-1$
	}

	/**
	 * Tests the "-k" option
	 */
	@Test
	public void testMinusK() {
		assertThrows("The build should have failed", CoreException.class, //$NON-NLS-1$
				() -> run("failingTarget.xml", new String[] { "-k" }, false)); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("4 messages should have been logged; was " //$NON-NLS-1$
				+ AntTestChecker.getDefault().getMessagesLoggedCount(), 4,
				AntTestChecker.getDefault().getMessagesLoggedCount());
		assertTrue("Incorrect message:" //$NON-NLS-1$
				+ AntTestChecker.getDefault().getLoggedMessage(1),
				"Still echo on failure".equals(AntTestChecker.getDefault().getLoggedMessage(1))); //$NON-NLS-1$
	}
}