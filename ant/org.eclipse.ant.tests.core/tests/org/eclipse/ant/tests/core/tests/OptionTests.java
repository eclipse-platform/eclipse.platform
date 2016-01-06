/*******************************************************************************
 *  Copyright (c) 2000, 2016 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.tests.core.tests;

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

public class OptionTests extends AbstractAntTest {

	protected static final String UNKNOWN_ARG = "Unknown argument: "; //$NON-NLS-1$
	protected static final String START_OF_HELP = "ant [options] [target [target2 [target3] ...]]"; //$NON-NLS-1$
	protected static final String VERSION = "Apache Ant(TM) version 1.9.6"; //$NON-NLS-1$
	protected static final String PLUGIN_VERSION = "org.apache.ant_1.9.6"; //$NON-NLS-1$

	public OptionTests(String name) {
		super(name);
	}

	/**
	 * Tests the "-help" option
	 */
	public void testHelp() throws CoreException {
		run("TestForEcho.xml", new String[] { "-help" }); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("incorrect message number logged", 34, AntTestChecker.getDefault().getMessagesLoggedCount()); //$NON-NLS-1$
		assertTrue("Help is incorrect", getLastMessageLogged() != null && AntTestChecker.getDefault().getMessages().get(0).startsWith(START_OF_HELP)); //$NON-NLS-1$
	}

	/**
	 * Tests the "-h" option (help)
	 */
	public void testMinusH() throws CoreException {
		run("TestForEcho.xml", new String[] { "-h" }); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("incorrect message number logged", 34, AntTestChecker.getDefault().getMessagesLoggedCount()); //$NON-NLS-1$
		assertTrue("Help is incorrect", getLastMessageLogged() != null && AntTestChecker.getDefault().getMessages().get(0).startsWith(START_OF_HELP)); //$NON-NLS-1$
	}

	/**
	 * Tests the "-version" option
	 */
	public void testVersion() throws CoreException {
		run("TestForEcho.xml", new String[] { "-version" }); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("One message should have been logged", AntTestChecker.getDefault().getMessagesLoggedCount() == 1); //$NON-NLS-1$
		assertTrue("Version is incorrect", getLastMessageLogged().startsWith(VERSION)); //$NON-NLS-1$
	}

	/**
	 * Tests the "-projecthelp" option
	 */
	public void testProjecthelp() throws CoreException {
		run("TestForEcho.xml", new String[] { "-projecthelp" }); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("Incorrect number of messages", 4, AntTestChecker.getDefault().getMessagesLoggedCount()); //$NON-NLS-1$
		assertTrue("Project help is incorrect", getLastMessageLogged().startsWith("Default target:")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests the "-p" option (project help)
	 */
	public void testMinusP() throws CoreException {
		run("TestForEcho.xml", new String[] { "-p" }); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("Incorrect number of messages", 4, AntTestChecker.getDefault().getMessagesLoggedCount()); //$NON-NLS-1$
		assertTrue("Project help is incorrect", getLastMessageLogged().startsWith("Default target:")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests the "-projecthelp" option when it will not show as much (quite mode)
	 */
	public void testProjecthelpQuiet() throws CoreException {
		run("TestForEcho.xml", new String[] { "-projecthelp", "-q" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertEquals(1, AntTestChecker.getDefault().getMessagesLoggedCount());
	}

	/**
	 * Tests the "-listener" option with a listener that is not an instance of BuildListener
	 */
	public void testListenerBad() {
		try {
			run("TestForEcho.xml", new String[] { "-listener", "java.lang.String" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		catch (CoreException ce) {
			String msg = ce.getMessage();
			assertTrue("Message incorrect!: " //$NON-NLS-1$
					+ msg, msg.equals("java.lang.String which was specified to be a build listener is not an instance of org.apache.tools.ant.BuildListener.")); //$NON-NLS-1$
			return;
		}
		assertTrue("A core exception should have occurred wrappering a class cast exception", false); //$NON-NLS-1$
	}

	/**
	 * Tests passing an unrecognized argument
	 */
	public void testUnknownArg() throws CoreException {
		run("TestForEcho.xml", new String[] { "-listenr" }); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Unrecognized option message should have been logged before successful build", //$NON-NLS-1$
		(AntTestChecker.getDefault().getMessagesLoggedCount() == 6) && (getLoggedMessage(5).startsWith(UNKNOWN_ARG))
				&& (getLastMessageLogged().startsWith(BUILD_SUCCESSFUL)));
	}

	/**
	 * Tests specifying the -logfile with no arg
	 */
	public void testLogFileWithNoArg() {
		try {
			run("TestForEcho.xml", new String[] { "-logfile" }); //$NON-NLS-1$ //$NON-NLS-2$
		}
		catch (CoreException ce) {
			return;
		}
		assertTrue("You must specify a log file when using the -log argument", false); //$NON-NLS-1$
	}

	/**
	 * Tests specifying the -logfile
	 */
	public void testLogFile() throws CoreException, IOException {
		run("TestForEcho.xml", new String[] { "-logfile", "TestLogFile.txt" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		IFile file = checkFileExists("TestLogFile.txt"); //$NON-NLS-1$

		try (InputStream stream = file.getContents(); InputStreamReader in = new InputStreamReader(new BufferedInputStream(stream))) {
			StringBuffer buffer = new StringBuffer();
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
	public void testLoggerWithNoArg() {
		try {
			run("TestForEcho.xml", new String[] { "-logger" }); //$NON-NLS-1$ //$NON-NLS-2$
		}
		catch (CoreException ce) {
			return;
		}
		assertTrue("You must specify a classname when using the -logger argument", false); //$NON-NLS-1$
	}

	/**
	 * Tests the "-logger" option with a logger that is not an instance of BuildLogger
	 */
	public void testLoggerBad() {
		try {
			run("TestForEcho.xml", new String[] { "-logger", "java.lang.String" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		catch (CoreException ce) {
			return;
		}
		assertTrue("A core exception should have occurred wrappering a class cast exception", false); //$NON-NLS-1$
	}

	/**
	 * Tests the "-logger" option with two loggers specified...only one is allowed
	 */
	public void testTwoLoggers() {
		try {
			run("TestForEcho.xml", new String[] { "-logger", "java.lang.String", "-q", "-logger", "java.lang.String" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$
		}
		catch (CoreException ce) {
			return;
		}
		assertTrue("As only one logger can be specified", false); //$NON-NLS-1$
	}

	/**
	 * Tests specifying the -listener with no arg
	 */
	public void testListenerWithNoArg() {
		try {
			run("TestForEcho.xml", new String[] { "-listener" }); //$NON-NLS-1$ //$NON-NLS-2$
		}
		catch (CoreException ce) {
			return;
		}
		assertTrue("You must specify a listeners when using the -listener argument ", false); //$NON-NLS-1$
	}

	/**
	 * Tests specifying the -listener with a class that will not be found
	 */
	public void testListenerClassNotFound() {
		try {
			run("TestForEcho.xml", new String[] { "-listener", "TestBuildListener" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		catch (CoreException e) {
			String message = e.getStatus().getException().getMessage();
			assertTrue("Should be ClassNotFoundException", "java.lang.ClassNotFoundException: TestBuildListener".equals(message)); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		}
		assertTrue("A CoreException should have occurred as the listener class will not be found", false); //$NON-NLS-1$

	}

	/**
	 * Tests specifying the -listener option
	 */
	public void testListener() throws CoreException {
		run("TestForEcho.xml", new String[] { "-listener", ANT_TEST_BUILD_LISTENER }); //$NON-NLS-1$ //$NON-NLS-2$
		assertSuccessful();
		assertTrue("A listener should have been added named: " //$NON-NLS-1$
				+ ANT_TEST_BUILD_LISTENER, ANT_TEST_BUILD_LISTENER.equals(AntTestChecker.getDefault().getLastListener()));
	}

	/**
	 * Tests specifying the XmlLogger as a listener (bug 80435)
	 */
	public void testXmlLoggerListener() throws CoreException {
		run("TestForEcho.xml", new String[] { "-listener", "org.apache.tools.ant.XmlLogger" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertSuccessful();

		// find the log file generated by the xml logger
		IFile file = checkFileExists("log.xml"); //$NON-NLS-1$
		InputStream stream = file.getContents();
		try {
			assertTrue(stream.available() != 0);
		}
		catch (IOException e) {
			assertTrue(false);
		}
	}

	/**
	 * Tests specifying the -listener option multiple times...which is allowed
	 */
	public void testListenerMultiple() throws CoreException {
		run("TestForEcho.xml", new String[] { "-listener", ANT_TEST_BUILD_LISTENER, "-listener", ANT_TEST_BUILD_LISTENER }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertSuccessful();
		assertTrue("A listener should have been added named: " //$NON-NLS-1$
				+ ANT_TEST_BUILD_LISTENER, ANT_TEST_BUILD_LISTENER.equals(AntTestChecker.getDefault().getLastListener()));
		assertTrue("Two listeners should have been added", AntTestChecker.getDefault().getListeners().size() == 2); //$NON-NLS-1$
	}

	/**
	 * Tests specifying the -listener option multiple times, with one missing the arg
	 */
	public void testListenerMultipleWithBad() {
		try {
			run("TestForEcho.xml", new String[] { "-listener", ANT_TEST_BUILD_LISTENER, "-q", "-listener", "-verbose" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}
		catch (CoreException e) {
			// You must specify a listener for all -listener arguments
			return;
		}
		assertTrue("You must specify a listener for all -listener arguments ", false); //$NON-NLS-1$
	}

	/**
	 * Tests specifying the -buildfile with no arg
	 */
	public void testBuildFileWithNoArg() {
		try {
			run("TestForEcho.xml", new String[] { "-buildfile" }); //$NON-NLS-1$ //$NON-NLS-2$
		}
		catch (CoreException ce) {
			// You must specify a buildfile when using the -buildfile argument
			return;
		}
		assertTrue("You must specify a buildfile when using the -buildfile argument", false); //$NON-NLS-1$
	}

	/**
	 * Tests specifying the -buildfile
	 */
	public void testBuildFile() throws CoreException {
		String buildFileName = getProject().getFolder("buildfiles").getFile("echoing.xml").getLocation().toFile().getAbsolutePath(); //$NON-NLS-1$ //$NON-NLS-2$
		run("TestForEcho.xml", new String[] { "-buildfile", buildFileName }, false, "buildfiles"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

		assertTrue("Should have been 1 tasks, was: " //$NON-NLS-1$
				+ AntTestChecker.getDefault().getTaskStartedCount(), AntTestChecker.getDefault().getTaskStartedCount() == 1);
	}

	/**
	 * Tests specifying a target at the command line that does not exist.
	 * 
	 * @since 3.6 this will not fail - the default target will be run instead
	 * @since 3.8 this will fail as there are no more known targets
	 */
	public void testSpecifyBadTargetAsArg() throws CoreException {
		run("TestForEcho.xml", new String[] { "echo2" }, false); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Should be an unknown target message", AntTestChecker.getDefault().getLoggedMessage(1).indexOf("Unknown target") >= 0); //$NON-NLS-1$//$NON-NLS-2$
		assertTrue("Should be an unknown target message", AntTestChecker.getDefault().getLoggedMessage(1).indexOf("echo2") >= 0); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Should be a no known target message", AntTestChecker.getDefault().getLoggedMessage(0).indexOf("No known target specified.") >= 0); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("Should not have run any targets", 0, AntTestChecker.getDefault().getTargetsStartedCount()); //$NON-NLS-1$
	}

	/**
	 * Tests specifying both a non-existent target and an existent target in the command line
	 * 
	 */
	public void testSpecifyBothBadAndGoodTargetsAsArg() throws CoreException {
		run("TestForEcho.xml", new String[] { "echo2", "Test for Echo" }, false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("Should be an unknown target message", AntTestChecker.getDefault().getLoggedMessage(5).indexOf("Unknown target") >= 0); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Should be an unknown target message", AntTestChecker.getDefault().getLoggedMessage(5).indexOf("echo2") >= 0); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("Should have run the Test for Echo target", 5, AntTestChecker.getDefault().getTargetsStartedCount()); //$NON-NLS-1$
	}

	/**
	 * Tests specifying a target at the command line
	 */
	public void testSpecifyTargetAsArg() throws CoreException {
		run("echoing.xml", new String[] { "echo3" }, false); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("3 messages should have been logged; was " //$NON-NLS-1$
				+ AntTestChecker.getDefault().getMessagesLoggedCount(), AntTestChecker.getDefault().getMessagesLoggedCount() == 3);
		assertSuccessful();
	}

	/**
	 * Tests specifying a target at the command line with other options
	 */
	public void testSpecifyTargetAsArgWithOtherOptions() throws CoreException {
		run("echoing.xml", new String[] { "-logfile", "TestLogFile.txt", "echo3" }, false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		assertTrue("4 messages should have been logged; was " //$NON-NLS-1$
				+ AntTestChecker.getDefault().getMessagesLoggedCount(), AntTestChecker.getDefault().getMessagesLoggedCount() == 4);
		List<String> messages = AntTestChecker.getDefault().getMessages();
		// ensure that echo3 target executed and only that target
		assertTrue("echo3 target not executed", messages.get(2).equals("echo3")); //$NON-NLS-1$ //$NON-NLS-2$
		assertSuccessful();
	}

	/**
	 * Tests specifying targets at the command line with other options
	 */
	public void testSpecifyTargetsAsArgWithOtherOptions() throws CoreException {
		run("echoing.xml", new String[] { "-logfile", "TestLogFile.txt", "echo2", "echo3" }, false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		assertTrue("5 messages should have been logged; was " //$NON-NLS-1$
				+ AntTestChecker.getDefault().getMessagesLoggedCount(), AntTestChecker.getDefault().getMessagesLoggedCount() == 5);
		List<String> messages = AntTestChecker.getDefault().getMessages();
		// ensure that echo2 target executed
		assertTrue("echo2 target not executed", messages.get(2).equals("echo2")); //$NON-NLS-1$ //$NON-NLS-2$
		assertSuccessful();
	}

	/**
	 * Tests specifying a target at the command line and quiet reporting
	 */
	public void testSpecifyTargetAsArgAndQuiet() throws CoreException {
		run("echoing.xml", new String[] { "-logfile", "TestLogFile.txt", "echo3", "-quiet" }, false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		assertTrue("2 messages should have been logged; was " //$NON-NLS-1$
				+ AntTestChecker.getDefault().getMessagesLoggedCount(), AntTestChecker.getDefault().getMessagesLoggedCount() == 2);
	}

	/**
	 * Tests properties using "-D"
	 */
	public void testMinusD() throws CoreException {
		run("echoing.xml", new String[] { "-DAntTests=testing", "-Declipse.is.cool=true" }, false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertSuccessful();
		assertTrue("eclipse.is.cool should have been set as true", "true".equals(AntTestChecker.getDefault().getUserProperty("eclipse.is.cool"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("AntTests should have a value of testing", "testing".equals(AntTestChecker.getDefault().getUserProperty("AntTests"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertNull("my.name was not set and should be null", AntTestChecker.getDefault().getUserProperty("my.name")); //$NON-NLS-1$ //$NON-NLS-2$

	}

	/**
	 * Tests properties using "-D" and "-d" to specify debug
	 */
	public void testMinusDMinusd() throws CoreException {
		run("echoing.xml", new String[] { "-d", "-DAntTests=testing", "-Declipse.is.cool=true" }, false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		assertSuccessful();
		assertTrue("eclipse.is.cool should have been set as true", "true".equals(AntTestChecker.getDefault().getUserProperty("eclipse.is.cool"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("AntTests should have a value of testing", "testing".equals(AntTestChecker.getDefault().getUserProperty("AntTests"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertNull("my.name was not set and should be null", AntTestChecker.getDefault().getUserProperty("my.name")); //$NON-NLS-1$ //$NON-NLS-2$

	}

	public void testMinusDAndGlobalProperties() throws CoreException {
		run("echoing.xml", new String[] { "-DAntTests=testing", "-Declipse.is.cool=true" }, false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertSuccessful();
		assertTrue("eclipse.running should have been set as true", "true".equals(AntTestChecker.getDefault().getUserProperty("eclipse.running"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertNotNull("eclipse.home should have been set", AntTestChecker.getDefault().getUserProperty("eclipse.home")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests specifying a property such as "-D=emptyStringIsMyName Bug 37007
	 */
	public void testMinusDEmpty() throws CoreException {
		run("echoing.xml", new String[] { "-D=emptyStringIsMyName", "-Declipse.is.cool=true" }, false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertSuccessful();
		assertTrue("eclipse.is.cool should have been set as true", "true".equals(AntTestChecker.getDefault().getUserProperty("eclipse.is.cool"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("\"\" should have a value of emptyStringIsMyName", "emptyStringIsMyName".equals(AntTestChecker.getDefault().getUserProperty(""))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertNull("my.name was not set and should be null", AntTestChecker.getDefault().getUserProperty("my.name")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests specifying properties that contain spaces Bug 37094
	 */
	public void testMinusDWithSpaces() throws CoreException {
		run("echoing.xml", new String[] { "-DAntTests= testing", "-Declipse.is.cool=    true" }, false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertSuccessful();
		assertTrue("eclipse.is.cool should have been set as true", "true".equals(AntTestChecker.getDefault().getUserProperty("eclipse.is.cool"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("AntTests should have a value of testing", "testing".equals(AntTestChecker.getDefault().getUserProperty("AntTests"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertNull("my.name was not set and should be null", AntTestChecker.getDefault().getUserProperty("my.name")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests specifying properties when the user has incorrectly specified "-Debug" Bug 40935
	 */
	public void testPropertiesWithMinusDebug() throws CoreException {
		run("echoing.xml", new String[] { "-Debug", "-DAntTests= testing", "-Declipse.is.cool=    true" }, false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		assertTrue("\"-Debug\" should be flagged as an unknown argument", "Unknown argument: -Debug".equals(AntTestChecker.getDefault().getMessages().get(0))); //$NON-NLS-1$ //$NON-NLS-2$
		assertSuccessful();
		assertTrue("eclipse.is.cool should have been set as true", "true".equals(AntTestChecker.getDefault().getUserProperty("eclipse.is.cool"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("AntTests should have a value of testing", "testing".equals(AntTestChecker.getDefault().getUserProperty("AntTests"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertNull("my.name was not set and should be null", AntTestChecker.getDefault().getUserProperty("my.name")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests when the user has incorrectly specified "-Debug" Bug 40935
	 */
	public void testMinusDebug() throws CoreException {
		run("echoing.xml", new String[] { "-Debug" }); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("\"-Debug\" should be flagged as an unknown argument", "Unknown argument: -Debug".equals(AntTestChecker.getDefault().getMessages().get(0))); //$NON-NLS-1$ //$NON-NLS-2$
		assertSuccessful();
	}

	public void testPropertyFileWithNoArg() {
		try {
			run("TestForEcho.xml", new String[] { "-propertyfile" }); //$NON-NLS-1$ //$NON-NLS-2$
		}
		catch (CoreException ce) {
			String msg = AntTestChecker.getDefault().getMessages().get(0);
			assertTrue("Message incorrect!: " + msg, msg.equals("You must specify a property filename when using the -propertyfile argument")); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		}
		assertTrue("You must specify a property filename when using the -propertyfile argument", false); //$NON-NLS-1$
	}

	/**
	 * A build should succeed when a property file is not found. The error is reported and the build continues.
	 */
	public void testPropertyFileFileNotFound() throws CoreException {

		run("TestForEcho.xml", new String[] { "-propertyfile", "qq.txt" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertSuccessful();
		String msg = AntTestChecker.getDefault().getMessages().get(0);
		assertTrue("Message incorrect!: " + msg, msg.startsWith("Could not load property file:")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testPropertyFile() throws CoreException {
		run("TestForEcho.xml", new String[] { "-propertyfile", getPropertyFileName() }); //$NON-NLS-1$ //$NON-NLS-2$
		assertSuccessful();
		assertTrue("eclipse.is.cool should have been set as Yep", "Yep".equals(AntTestChecker.getDefault().getUserProperty("eclipse.is.cool"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("AntTests should have a value of testing", "testing from properties file".equals(AntTestChecker.getDefault().getUserProperty("AntTests"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertNull("my.name was not set and should be null", AntTestChecker.getDefault().getUserProperty("my.name")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testPropertyFileWithMinusDTakingPrecedence() throws CoreException {
		run("echoing.xml", new String[] { "-propertyfile", getPropertyFileName(), "-DAntTests=testing", "-Declipse.is.cool=true" }, false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		assertSuccessful();
		assertTrue("eclipse.is.cool should have been set as true", "true".equals(AntTestChecker.getDefault().getUserProperty("eclipse.is.cool"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertTrue("AntTests should have a value of testing", "testing".equals(AntTestChecker.getDefault().getUserProperty("AntTests"))); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertNull("my.name was not set and should be null", AntTestChecker.getDefault().getUserProperty("my.name")); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void testInputHandlerWithNoArg() {
		try {
			run("TestForEcho.xml", new String[] { "-inputhandler" }); //$NON-NLS-1$ //$NON-NLS-2$
		}
		catch (CoreException ce) {
			String msg = ce.getMessage();
			assertTrue("Message incorrect!: " + msg, msg.equals("You must specify a classname when using the -inputhandler argument")); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		}
		assertTrue("You must specify a classname when using the -inputhandler argument", false); //$NON-NLS-1$
	}

	/**
	 * Tests the "-inputhandler" option with two handlers specified...only one is allowed
	 */
	public void testInputHandlerMultiple() {
		try {
			run("TestForEcho.xml", new String[] { "-inputhandler", "org.apache.tools.ant.input.DefaultInputHandler", "-q", "-inputhandler", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
					"org.apache.tools.ant.input.DefaultInputHandler" }); //$NON-NLS-1$
		}
		catch (CoreException ce) {
			String msg = ce.getMessage();
			assertTrue("Message incorrect!: " + msg, msg.equals("Only one input handler class may be specified.")); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		}
		assertTrue("As only one input handler can be specified", false); //$NON-NLS-1$
	}

	/**
	 * Tests the "-inputhandler" option with a input handler that is not an instance of InputHandler
	 */
	public void testInputHandlerBad() {
		try {
			run("TestForEcho.xml", new String[] { "-inputhandler", "java.lang.StringBuffer" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		catch (CoreException ce) {
			String msg = ce.getMessage();
			assertTrue("Message incorrect!: " //$NON-NLS-1$
					+ msg, msg.equals("The specified input handler class java.lang.StringBuffer does not implement the org.apache.tools.ant.input.InputHandler interface")); //$NON-NLS-1$
			return;
		}
		assertTrue("Incorrect inputhandler", false); //$NON-NLS-1$
	}

	/**
	 * Tests the "-inputhandler" option with a input handler that is not a defined class
	 */
	public void testInputHandlerBad2() {
		try {
			run("TestForEcho.xml", new String[] { "-inputhandler", "ja.lang.StringBuffer" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		catch (CoreException ce) {
			String msg = ce.getMessage();
			assertTrue("Message incorrect!: " + msg, msg.startsWith("Unable to instantiate specified input handler class ja.lang.StringBuffer")); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		}
		assertTrue("Incorrect inputhandler", false); //$NON-NLS-1$
	}

	/**
	 * Tests the "-inputhandler" option with a test input handler and the -noinput option
	 */
	public void testInputHandlerWithMinusNoInput() {
		try {
			run("input.xml", new String[] { "-inputhandler", "org.eclipse.ant.tests.core.support.inputHandlers.AntTestInputHandler", "-noinput" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		}
		catch (CoreException ce) {
			assertTrue("Message incorrect: " //$NON-NLS-1$
					+ ce.getMessage(), ce.getMessage().endsWith("Unable to respond to input request likely as a result of specifying the -noinput command")); //$NON-NLS-1$
			return;
		}

		assertTrue("Build should have failed", false); //$NON-NLS-1$
	}

	/**
	 * Tests the -noinput option with the default input handler
	 */
	public void testMinusNoInput() {
		try {
			run("input.xml", new String[] { "-noinput" }); //$NON-NLS-1$ //$NON-NLS-2$
		}
		catch (CoreException ce) {
			assertTrue("Message incorrect: " + ce.getMessage(), ce.getMessage().endsWith("Failed to read input from Console.")); //$NON-NLS-1$ //$NON-NLS-2$
			return;
		}

		assertTrue("Build should have failed", false); //$NON-NLS-1$
	}

	/**
	 * Tests the "-inputhandler" option with a test input handler Order after the noinput tests so that we test we are resetting the system property
	 */
	public void testInputHandler() throws CoreException {

		run("input.xml", new String[] { "-inputhandler", "org.eclipse.ant.tests.core.support.inputHandlers.AntTestInputHandler" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		assertSuccessful();
		String msg = AntTestChecker.getDefault().getMessages().get(1);
		assertTrue("Message incorrect: " + msg, msg.equals("testing handling input requests")); //$NON-NLS-1$ //$NON-NLS-2$

	}

	/**
	 * Tests the "-diagnostics" option with no ANT_HOME set bug 25693
	 */
	public void testDiagnosticsWithNoAntHome() throws CoreException {
		try {
			AntCorePlugin.getPlugin().getPreferences().setAntHome(null);
			run("input.xml", new String[] { "-diagnostics" }); //$NON-NLS-1$ //$NON-NLS-2$

			String msg = AntTestChecker.getDefault().getMessages().get(0);
			assertTrue("Message incorrect: " + msg, msg.equals("------- Ant diagnostics report -------")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		finally {
			restorePreferenceDefaults();
		}
	}

	/**
	 * Tests the "-diagnostics" option with ANT_HOME set bug 25693
	 */
	public void testDiagnostics() throws CoreException {

		try {
			run("input.xml", new String[] { "-diagnostics" }); //$NON-NLS-1$ //$NON-NLS-2$
		}
		finally {
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
	public void testMinusQuiet() throws CoreException {
		run("TestForEcho.xml", new String[] { "-quiet" }); //$NON-NLS-1$ //$NON-NLS-2$
		assertSuccessful();
	}

	/**
	 * Tests the "-keep-going" option
	 */
	public void testMinusKeepGoing() {
		try {
			run("failingTarget.xml", new String[] { "-keep-going" }, false); //$NON-NLS-1$ //$NON-NLS-2$
		}
		catch (CoreException be) {
			assertTrue("4 messages should have been logged; was " //$NON-NLS-1$
					+ AntTestChecker.getDefault().getMessagesLoggedCount(), AntTestChecker.getDefault().getMessagesLoggedCount() == 4);
			assertTrue("Incorrect message:" //$NON-NLS-1$
					+ AntTestChecker.getDefault().getLoggedMessage(1), "Still echo on failure".equals(AntTestChecker.getDefault().getLoggedMessage(1))); //$NON-NLS-1$
			return;
		}

		assertTrue("The build should have failed", false); //$NON-NLS-1$
	}

	/**
	 * Tests the "-k" option
	 */
	public void testMinusK() {
		try {
			run("failingTarget.xml", new String[] { "-k" }, false); //$NON-NLS-1$ //$NON-NLS-2$
		}
		catch (CoreException be) {
			assertTrue("4 messages should have been logged; was " //$NON-NLS-1$
					+ AntTestChecker.getDefault().getMessagesLoggedCount(), AntTestChecker.getDefault().getMessagesLoggedCount() == 4);
			assertTrue("Incorrect message:" //$NON-NLS-1$
					+ AntTestChecker.getDefault().getLoggedMessage(1), "Still echo on failure".equals(AntTestChecker.getDefault().getLoggedMessage(1))); //$NON-NLS-1$
			return;
		}

		assertTrue("The build should have failed", false); //$NON-NLS-1$
	}
}