/*******************************************************************************
 *  Copyright (c) 2000, 2011 IBM Corporation and others.
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
	
	protected static final String UNKNOWN_ARG = "Unknown argument: ";
	protected static final String START_OF_HELP = "ant [options] [target [target2 [target3] ...]]";
	protected static final String VERSION = "Apache Ant(TM) version 1.8.2 compiled on December 20 2010";
	protected static final String PLUGIN_VERSION = "org.apache.ant_1.8.2";
	 
	public OptionTests(String name) {
		super(name);
	}
	
	/**
	 * Tests the "-help" option
	 */
	public void testHelp() throws CoreException {
		run("TestForEcho.xml", new String[]{"-help"});
		assertEquals("incorrect message number logged", 35, AntTestChecker.getDefault().getMessagesLoggedCount());
		assertTrue("Help is incorrect", getLastMessageLogged() != null && ((String) AntTestChecker.getDefault().getMessages().get(0)).startsWith(START_OF_HELP));
	}
	
	/**
	 * Tests the "-h" option (help)
	 */
	public void testMinusH() throws CoreException {
		run("TestForEcho.xml", new String[]{"-h"});
		assertEquals("incorrect message number logged", 35, AntTestChecker.getDefault().getMessagesLoggedCount());
		assertTrue("Help is incorrect", getLastMessageLogged() != null && ((String) AntTestChecker.getDefault().getMessages().get(0)).startsWith(START_OF_HELP));
	}
	
	/**
	 * Tests the "-version" option
	 */
	public void testVersion() throws CoreException {
		run("TestForEcho.xml", new String[]{"-version"});
		assertTrue("One message should have been logged", AntTestChecker.getDefault().getMessagesLoggedCount() == 1);
		assertEquals("Version is incorrect: ", VERSION, getLastMessageLogged());
	}
	
	/**
	 * Tests the "-projecthelp" option
	 */
	public void testProjecthelp() throws CoreException {
		run("TestForEcho.xml", new String[]{"-projecthelp"});
		assertEquals("Incorrect number of messages", 4, AntTestChecker.getDefault().getMessagesLoggedCount());
		assertTrue("Project help is incorrect", getLastMessageLogged().startsWith("Default target:"));
	}
	
	/**
	 * Tests the "-p" option (project help)
	 */
	public void testMinusP() throws CoreException {
		run("TestForEcho.xml", new String[]{"-p"});
		assertEquals("Incorrect number of messages", 4, AntTestChecker.getDefault().getMessagesLoggedCount());
		assertTrue("Project help is incorrect", getLastMessageLogged().startsWith("Default target:"));
	}
	
	/**
	 * Tests the "-projecthelp" option when it will not show as much (quite mode)
	 */
	public void testProjecthelpQuiet() throws CoreException {
		run("TestForEcho.xml", new String[]{"-projecthelp", "-q"});
		assertEquals(1, AntTestChecker.getDefault().getMessagesLoggedCount());
	}
	
	/**
	 * Tests the "-listener" option with a listener that is not an instance of BuildListener
	 */
	public void testListenerBad() {
		try {
			run("TestForEcho.xml", new String[]{"-listener", "java.lang.String"});
		} catch (CoreException ce) {
			String msg= ce.getMessage();
			assertTrue("Message incorrect!: " + msg, msg.equals("java.lang.String which was specified to be a build listener is not an instance of org.apache.tools.ant.BuildListener."));
			return;
		}
		assertTrue("A core exception should have occurred wrappering a class cast exception", false);
	}
	
	/**
	 * Tests passing an unrecognized argument
	 */
	public void testUnknownArg() throws CoreException {	
		run("TestForEcho.xml", new String[]{"-listenr"});
        assertTrue("Unrecognized option message should have been logged before successful build",
					(AntTestChecker.getDefault().getMessagesLoggedCount() == 6)
					 && (getLoggedMessage(5).startsWith(UNKNOWN_ARG))
                     && (getLastMessageLogged().startsWith(BUILD_SUCCESSFUL)));
	}
	
	/**
	 * Tests specifying the -logfile with no arg
	 */
	public void testLogFileWithNoArg() {
		try {
			run("TestForEcho.xml", new String[]{"-logfile"});
		} catch (CoreException ce) {
			return;
		}
		assertTrue("You must specify a log file when using the -log argument", false);
	}
	
	/**
	 * Tests specifying the -logfile
	 */
	public void testLogFile() throws CoreException, IOException {
		run("TestForEcho.xml", new String[]{"-logfile", "TestLogFile.txt"});
		IFile file= checkFileExists("TestLogFile.txt");
		InputStream stream =file.getContents();
		
		InputStreamReader in= null;
		try {		
			in= new InputStreamReader(new BufferedInputStream(stream));
			StringBuffer buffer= new StringBuffer();
			char[] readBuffer= new char[2048];
			int n= in.read(readBuffer);
			while (n > 0) {
				buffer.append(readBuffer, 0, n);
				n= in.read(readBuffer);
			}
			assertTrue("File should have started with Buildfile", buffer.toString().startsWith("Buildfile"));
		} finally {
            if (in != null) {
                in.close();
            }
			stream.close();
		}
		
	}
	
	/**
	 * Tests specifying the -logger with no arg
	 */
	public void testLoggerWithNoArg() {
		try {
			run("TestForEcho.xml", new String[]{"-logger"});
		} catch (CoreException ce) {
			return;
		}
		assertTrue("You must specify a classname when using the -logger argument", false);
	}
	
	/**
	 * Tests the "-logger" option with a logger that is not an instance of BuildLogger
	 */
	public void testLoggerBad() {
		try {
			run("TestForEcho.xml", new String[]{"-logger", "java.lang.String"});
		} catch (CoreException ce) {
			return;
		}
		assertTrue("A core exception should have occurred wrappering a class cast exception", false);
	}
	
	/**
	 * Tests the "-logger" option with two loggers specified...only one is allowed
	 */
	public void testTwoLoggers() {
		try {
			run("TestForEcho.xml", new String[]{"-logger", "java.lang.String", "-q", "-logger", "java.lang.String"});
		} catch (CoreException ce) {
			return;
		}
		assertTrue("As only one logger can be specified", false);
	}
	
	/**
	 * Tests specifying the -listener with no arg
	 */
	public void testListenerWithNoArg() {
		try {
			run("TestForEcho.xml", new String[]{"-listener"});
		} catch (CoreException ce) {
			return;
		}
		assertTrue("You must specify a listeners when using the -listener argument ", false);
	}
	
	/**
	 * Tests specifying the -listener with a class that will not be found
	 */
	public void testListenerClassNotFound() {
		try {
			run("TestForEcho.xml", new String[]{"-listener", "TestBuildListener"});
		} catch (CoreException e) {
			String message= e.getStatus().getException().getMessage();
			assertTrue("Should be ClassNotFoundException", "java.lang.ClassNotFoundException: TestBuildListener".equals(message));
			return;
		}
		assertTrue("A CoreException should have occurred as the listener class will not be found", false);
		
	}
	
	/**
	 * Tests specifying the -listener option
	 */
	public void testListener() throws CoreException {
		run("TestForEcho.xml", new String[]{"-listener", ANT_TEST_BUILD_LISTENER});
		assertSuccessful();
		assertTrue("A listener should have been added named: " + ANT_TEST_BUILD_LISTENER, ANT_TEST_BUILD_LISTENER.equals(AntTestChecker.getDefault().getLastListener()));
	}
	
	/**
	 * Tests specifying the XmlLogger as a listener (bug 80435)
	 */
	public void testXmlLoggerListener() throws CoreException {
		run("TestForEcho.xml", new String[]{"-listener", "org.apache.tools.ant.XmlLogger"});
		assertSuccessful();
		
		//find the log file generated by the xml logger
		IFile file= checkFileExists("log.xml");
		InputStream stream= file.getContents();
		try {
			assertTrue(stream.available() != 0);
		} catch (IOException e) {
			assertTrue(false);
		}
	}
	
	/**
	 * Tests specifying the -listener option multiple times...which is allowed
	 */
	public void testListenerMultiple() throws CoreException {
		run("TestForEcho.xml", new String[]{"-listener", ANT_TEST_BUILD_LISTENER, "-listener", ANT_TEST_BUILD_LISTENER});
		assertSuccessful();
		assertTrue("A listener should have been added named: " + ANT_TEST_BUILD_LISTENER, ANT_TEST_BUILD_LISTENER.equals(AntTestChecker.getDefault().getLastListener()));
		assertTrue("Two listeners should have been added", AntTestChecker.getDefault().getListeners().size() == 2);
	}
	
	/**
	 * Tests specifying the -listener option multiple times, with one missing the arg
	 */
	public void testListenerMultipleWithBad() {
		try {
			run("TestForEcho.xml", new String[]{"-listener", ANT_TEST_BUILD_LISTENER, "-q", "-listener", "-verbose"});
		} catch(CoreException e) {
			//You must specify a listener for all -listener arguments
			return;
		}
		assertTrue("You must specify a listener for all -listener arguments ", false);
	}
	
	/**
	 * Tests specifying the -buildfile with no arg
	 */
	public void testBuildFileWithNoArg() {
		try {
			run("TestForEcho.xml", new String[]{"-buildfile"});
		} catch (CoreException ce) {
			//You must specify a buildfile when using the -buildfile argument
			return;
		}
		assertTrue("You must specify a buildfile when using the -buildfile argument", false);
	}
	
	/**
	 * Tests specifying the -buildfile
	 */
	public void testBuildFile() throws CoreException {
		String buildFileName= getProject().getFolder("buildfiles").getFile("echoing.xml").getLocation().toFile().getAbsolutePath();
		run("TestForEcho.xml", new String[]{"-buildfile", buildFileName}, false, "buildfiles");
		
		assertTrue("Should have been 1 tasks, was: " + AntTestChecker.getDefault().getTaskStartedCount(), AntTestChecker.getDefault().getTaskStartedCount() == 1);
	}
	
	/**
	 * Tests specifying a target at the command line that does not exist.
	 * 
	 * @since 3.6  this will not fail - the default target will be run instead
	 * @since 3.7.1  this will fail as there are no more known targets
	 */
	public void testSpecifyBadTargetAsArg() throws CoreException {
		run("TestForEcho.xml", new String[]{"echo2"}, false);
		assertTrue("Should be an unknown target message", AntTestChecker.getDefault().getLoggedMessage(1).indexOf("Unknown target") >= 0);
		assertTrue("Should be an unknown target message", AntTestChecker.getDefault().getLoggedMessage(1).indexOf("echo2") >= 0);
		assertTrue("Should be a no known target message", AntTestChecker.getDefault().getLoggedMessage(0).indexOf("No known target specified.") >= 0);
		assertEquals("Should not have run any targets", 0, AntTestChecker.getDefault().getTargetsStartedCount());
	}
	
	/**
	 * Tests specifying both a non-existent target and an existent target in the command line 
	 * 
	 */
	public void testSpecifyBothBadAndGoodTargetsAsArg() throws CoreException {
		run("TestForEcho.xml", new String[]{"echo2", "Test for Echo"}, false);
		assertTrue("Should be an unknown target message", AntTestChecker.getDefault().getLoggedMessage(5).indexOf("Unknown target") >= 0);
		assertTrue("Should be an unknown target message", AntTestChecker.getDefault().getLoggedMessage(5).indexOf("echo2") >= 0);
		assertEquals("Should have run the Test for Echo target", 5, AntTestChecker.getDefault().getTargetsStartedCount());
	}
	
	/**
	 * Tests specifying a target at the command line
	 */
	public void testSpecifyTargetAsArg() throws CoreException {
		run("echoing.xml", new String[]{"echo3"}, false);
		assertTrue("3 messages should have been logged; was " + AntTestChecker.getDefault().getMessagesLoggedCount(), AntTestChecker.getDefault().getMessagesLoggedCount() == 3);
		assertSuccessful();
	}
	
	/**
	 * Tests specifying a target at the command line with other options
	 */
	public void testSpecifyTargetAsArgWithOtherOptions() throws CoreException {
		run("echoing.xml", new String[]{"-logfile", "TestLogFile.txt", "echo3"}, false);
		assertTrue("4 messages should have been logged; was " + AntTestChecker.getDefault().getMessagesLoggedCount(), AntTestChecker.getDefault().getMessagesLoggedCount() == 4);
		List messages= AntTestChecker.getDefault().getMessages();
		//ensure that echo3 target executed and only that target
		assertTrue("echo3 target not executed", messages.get(2).equals("echo3"));
		assertSuccessful();
	}
	
	/**
	 * Tests specifying targets at the command line with other options
	 */
	public void testSpecifyTargetsAsArgWithOtherOptions() throws CoreException {
		run("echoing.xml", new String[]{"-logfile", "TestLogFile.txt", "echo2", "echo3"}, false);
		assertTrue("5 messages should have been logged; was " + AntTestChecker.getDefault().getMessagesLoggedCount(), AntTestChecker.getDefault().getMessagesLoggedCount() == 5);
		List messages= AntTestChecker.getDefault().getMessages();
		//ensure that echo2 target executed
		assertTrue("echo2 target not executed", messages.get(2).equals("echo2"));
		assertSuccessful();
	}
	
	/**
	 * Tests specifying a target at the command line and quiet reporting
	 */
	public void testSpecifyTargetAsArgAndQuiet() throws CoreException {
		run("echoing.xml", new String[]{"-logfile", "TestLogFile.txt", "echo3", "-quiet"}, false);
		assertTrue("2 messages should have been logged; was " + AntTestChecker.getDefault().getMessagesLoggedCount(), AntTestChecker.getDefault().getMessagesLoggedCount() == 2);
	}
	
	/**
	 * Tests properties using "-D"
	 */
	public void testMinusD() throws CoreException {
		run("echoing.xml", new String[]{"-DAntTests=testing", "-Declipse.is.cool=true"}, false);
		assertSuccessful();
		assertTrue("eclipse.is.cool should have been set as true", "true".equals(AntTestChecker.getDefault().getUserProperty("eclipse.is.cool")));
		assertTrue("AntTests should have a value of testing", "testing".equals(AntTestChecker.getDefault().getUserProperty("AntTests")));
		assertNull("my.name was not set and should be null", AntTestChecker.getDefault().getUserProperty("my.name"));
		
	}
	
	/**
	 * Tests properties using "-D" and "-d" to specify debug
	 */
	public void testMinusDMinusd() throws CoreException {
		run("echoing.xml", new String[]{"-d", "-DAntTests=testing", "-Declipse.is.cool=true"}, false);
		assertSuccessful();
		assertTrue("eclipse.is.cool should have been set as true", "true".equals(AntTestChecker.getDefault().getUserProperty("eclipse.is.cool")));
		assertTrue("AntTests should have a value of testing", "testing".equals(AntTestChecker.getDefault().getUserProperty("AntTests")));
		assertNull("my.name was not set and should be null", AntTestChecker.getDefault().getUserProperty("my.name"));
	
	}
	
	public void testMinusDAndGlobalProperties() throws CoreException {
		run("echoing.xml", new String[]{"-DAntTests=testing", "-Declipse.is.cool=true"}, false);
		assertSuccessful();
		assertTrue("eclipse.running should have been set as true", "true".equals(AntTestChecker.getDefault().getUserProperty("eclipse.running")));
		assertNotNull("eclipse.home should have been set", AntTestChecker.getDefault().getUserProperty("eclipse.home"));
	}
	
	/**
	 * Tests specifying a property such as "-D=emptyStringIsMyName
	 * Bug 37007
	 */
	public void testMinusDEmpty() throws CoreException {
		run("echoing.xml", new String[]{"-D=emptyStringIsMyName", "-Declipse.is.cool=true"}, false);
		assertSuccessful();
		assertTrue("eclipse.is.cool should have been set as true", "true".equals(AntTestChecker.getDefault().getUserProperty("eclipse.is.cool")));
		assertTrue("\"\" should have a value of emptyStringIsMyName", "emptyStringIsMyName".equals(AntTestChecker.getDefault().getUserProperty("")));
		assertNull("my.name was not set and should be null", AntTestChecker.getDefault().getUserProperty("my.name"));
	}
	
	/**
	 * Tests specifying properties that contain spaces
	 * Bug 37094
	 */
	public void testMinusDWithSpaces() throws CoreException {
		run("echoing.xml", new String[]{"-DAntTests= testing", "-Declipse.is.cool=    true"}, false);
		assertSuccessful();
		assertTrue("eclipse.is.cool should have been set as true", "true".equals(AntTestChecker.getDefault().getUserProperty("eclipse.is.cool")));
		assertTrue("AntTests should have a value of testing", "testing".equals(AntTestChecker.getDefault().getUserProperty("AntTests")));
		assertNull("my.name was not set and should be null", AntTestChecker.getDefault().getUserProperty("my.name"));
	}
	
	/**
	 * Tests specifying properties when the user has incorrectly specified "-Debug"
	 * Bug 40935
	 */
	public void testPropertiesWithMinusDebug() throws CoreException {
		run("echoing.xml", new String[]{"-Debug", "-DAntTests= testing", "-Declipse.is.cool=    true"}, false);
		assertTrue("\"-Debug\" should be flagged as an unknown argument", "Unknown argument: -Debug".equals(AntTestChecker.getDefault().getMessages().get(0)));
		assertSuccessful();
		assertTrue("eclipse.is.cool should have been set as true", "true".equals(AntTestChecker.getDefault().getUserProperty("eclipse.is.cool")));
		assertTrue("AntTests should have a value of testing", "testing".equals(AntTestChecker.getDefault().getUserProperty("AntTests")));
		assertNull("my.name was not set and should be null", AntTestChecker.getDefault().getUserProperty("my.name"));
	}
	
	/**
	 * Tests when the user has incorrectly specified "-Debug"
	 * Bug 40935
	 */
	public void testMinusDebug() throws CoreException {
		run("echoing.xml", new String[]{"-Debug"});
		assertTrue("\"-Debug\" should be flagged as an unknown argument", "Unknown argument: -Debug".equals(AntTestChecker.getDefault().getMessages().get(0)));
		assertSuccessful();
	}
	
	public void testPropertyFileWithNoArg() {
		try {
			run("TestForEcho.xml", new String[]{"-propertyfile"});
		} catch (CoreException ce) {
			String msg= (String)AntTestChecker.getDefault().getMessages().get(0);
			assertTrue("Message incorrect!: " + msg, msg.equals("You must specify a property filename when using the -propertyfile argument"));
			return;
		}
		assertTrue("You must specify a property filename when using the -propertyfile argument", false);
	}
	
	/**
	 * A build should succeed when a property file is not found.
	 * The error is reported and the build continues.
	 */
	public void testPropertyFileFileNotFound() throws CoreException {
		
		run("TestForEcho.xml", new String[]{"-propertyfile", "qq.txt"});
		assertSuccessful();
		String msg= (String)AntTestChecker.getDefault().getMessages().get(0);
		assertTrue("Message incorrect!: " + msg, msg.startsWith("Could not load property file:"));
	}
	
	public void testPropertyFile() throws CoreException {
		run("TestForEcho.xml", new String[]{"-propertyfile", getPropertyFileName()});
		assertSuccessful();
		assertTrue("eclipse.is.cool should have been set as Yep", "Yep".equals(AntTestChecker.getDefault().getUserProperty("eclipse.is.cool")));
		assertTrue("AntTests should have a value of testing", "testing from properties file".equals(AntTestChecker.getDefault().getUserProperty("AntTests")));
		assertNull("my.name was not set and should be null", AntTestChecker.getDefault().getUserProperty("my.name"));
	}
	
	public void testPropertyFileWithMinusDTakingPrecedence() throws CoreException {
		run("echoing.xml", new String[]{"-propertyfile", getPropertyFileName(), "-DAntTests=testing", "-Declipse.is.cool=true"}, false);
		assertSuccessful();
		assertTrue("eclipse.is.cool should have been set as true", "true".equals(AntTestChecker.getDefault().getUserProperty("eclipse.is.cool")));
		assertTrue("AntTests should have a value of testing", "testing".equals(AntTestChecker.getDefault().getUserProperty("AntTests")));
		assertNull("my.name was not set and should be null", AntTestChecker.getDefault().getUserProperty("my.name"));
	}
	
	public void testInputHandlerWithNoArg() {
		try {
			run("TestForEcho.xml", new String[]{"-inputhandler"});
		} catch (CoreException ce) {
			String msg= ce.getMessage();
			assertTrue("Message incorrect!: " + msg, msg.equals("You must specify a classname when using the -inputhandler argument"));
			return;
		}
		assertTrue("You must specify a classname when using the -inputhandler argument", false);
	}
	
	/**
	 * Tests the "-inputhandler" option with two handlers specified...only one is allowed
	 */
	public void testInputHandlerMultiple() {
		try {
			run("TestForEcho.xml", new String[]{"-inputhandler", "org.apache.tools.ant.input.DefaultInputHandler", "-q", "-inputhandler", "org.apache.tools.ant.input.DefaultInputHandler"});
		} catch (CoreException ce) {
			String msg= ce.getMessage();
			assertTrue("Message incorrect!: " + msg, msg.equals("Only one input handler class may be specified."));
			return;
		}
		assertTrue("As only one input handler can be specified", false);
	}
	
	/**
	 * Tests the "-inputhandler" option with a input handler that is not an instance of InputHandler
	 */
	public void testInputHandlerBad() {
		try {
			run("TestForEcho.xml", new String[]{"-inputhandler", "java.lang.StringBuffer"});
		} catch (CoreException ce) {
			String msg= ce.getMessage();
			assertTrue("Message incorrect!: " + msg, msg.equals("The specified input handler class java.lang.StringBuffer does not implement the org.apache.tools.ant.input.InputHandler interface"));
			return;
		}
		assertTrue("Incorrect inputhandler", false);
	}
	
	/**
	 * Tests the "-inputhandler" option with a input handler that is not a defined class
	 */
	public void testInputHandlerBad2() {
		try {
			run("TestForEcho.xml", new String[]{"-inputhandler", "ja.lang.StringBuffer"});
		} catch (CoreException ce) {
			String msg= ce.getMessage();
			assertTrue("Message incorrect!: " + msg, msg.startsWith("Unable to instantiate specified input handler class ja.lang.StringBuffer"));
			return;
		}
		assertTrue("Incorrect inputhandler", false);
	}
	
	/**
	 * Tests the "-inputhandler" option with a test input handler and the -noinput option
	 */
	public void testInputHandlerWithMinusNoInput() {
		try {
			run("input.xml", new String[]{"-inputhandler", "org.eclipse.ant.tests.core.support.inputHandlers.AntTestInputHandler", "-noinput"});
		} catch (CoreException ce) {
			assertTrue("Message incorrect: " + ce.getMessage(), ce.getMessage().endsWith("Unable to respond to input request likely as a result of specifying the -noinput command"));
			return;
		}
		
		assertTrue("Build should have failed", false);
	}
	
	/**
	 * Tests the -noinput option with the default input handler
	 */
	public void testMinusNoInput() {
		try {
			run("input.xml", new String[]{"-noinput"});
		} catch (CoreException ce) {
			assertTrue("Message incorrect: " + ce.getMessage(), ce.getMessage().endsWith("Failed to read input from Console."));
			return;
		}
	
		assertTrue("Build should have failed", false);
	}
	
	/**
	 * Tests the "-inputhandler" option with a test input handler
	 * Order after the noinput tests so that we test we are resetting the system property
	 */
	public void testInputHandler() throws CoreException {
		
		run("input.xml", new String[]{"-inputhandler", "org.eclipse.ant.tests.core.support.inputHandlers.AntTestInputHandler"});
		assertSuccessful();
		String msg= (String)AntTestChecker.getDefault().getMessages().get(1);
		assertTrue("Message incorrect: " + msg, msg.equals("testing handling input requests"));
		
	}
	
	/**
	 * Tests the "-diagnostics" option with no ANT_HOME set
	 * bug 25693
	 */
	public void testDiagnosticsWithNoAntHome() throws CoreException {
		try {
			AntCorePlugin.getPlugin().getPreferences().setAntHome(null);
			run("input.xml", new String[]{"-diagnostics"});
		
			String msg= (String)AntTestChecker.getDefault().getMessages().get(0);
			assertTrue("Message incorrect: " + msg, msg.equals("------- Ant diagnostics report -------"));
		} finally {
			restorePreferenceDefaults();
		}
	}
		
	/**
	 * Tests the "-diagnostics" option with ANT_HOME set
	 * bug 25693
	 */
	public void testDiagnostics() throws CoreException {
	
		try {
			run("input.xml", new String[]{"-diagnostics"});
		} finally {
			restorePreferenceDefaults();
		}
		//we are looking for the ant.home entry
		List messages = AntTestChecker.getDefault().getMessages();
		String msg = (String)messages.get(17);
		//msg depends on whether self hosting testing or build testing
		assertTrue("Message incorrect: " + msg, checkAntHomeMessage(msg));
	}
	
	private boolean checkAntHomeMessage(String message) {
		String msg = message;
		if (msg.endsWith("org.apache.ant")) {
			return true;
		}
		
		if (msg.endsWith(PLUGIN_VERSION)) {
			return true;
		}
		
		//org.apache.ant_1.7.1.v200704241635
		int index = msg.lastIndexOf('.');
		if (index > 0) {
			msg = msg.substring(0, index);
		}
		return msg.endsWith(PLUGIN_VERSION);
	}
	
	/**
	 * Tests the "-quiet" still reports build successful
	 * bug 34488
	 */
	public void testMinusQuiet() throws CoreException {
		run("TestForEcho.xml", new String[]{"-quiet"});
		assertSuccessful();	
	}

	/**
	 * Tests the "-keep-going" option
	 */
	public void testMinusKeepGoing() {
		try {
			run("failingTarget.xml", new String[]{"-keep-going"}, false);
		} catch (CoreException be) {
			assertTrue("4 messages should have been logged; was " + AntTestChecker.getDefault().getMessagesLoggedCount(), AntTestChecker.getDefault().getMessagesLoggedCount() == 4);
			assertTrue("Incorrect message:"  + AntTestChecker.getDefault().getLoggedMessage(1), "Still echo on failure".equals(AntTestChecker.getDefault().getLoggedMessage(1)));
			return;
		}
		
		assertTrue("The build should have failed", false);
	}
	
	/**
	 * Tests the "-k" option
	 */
	public void testMinusK() {
		try {
			run("failingTarget.xml", new String[]{"-k"}, false);
		} catch (CoreException be) {
			assertTrue("4 messages should have been logged; was " + AntTestChecker.getDefault().getMessagesLoggedCount(), AntTestChecker.getDefault().getMessagesLoggedCount() == 4);
			assertTrue("Incorrect message:"  + AntTestChecker.getDefault().getLoggedMessage(1), "Still echo on failure".equals(AntTestChecker.getDefault().getLoggedMessage(1)));
			return;
		}
		
		assertTrue("The build should have failed", false);
	}
}