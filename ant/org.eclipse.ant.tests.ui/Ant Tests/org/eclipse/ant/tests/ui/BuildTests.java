/*******************************************************************************
 * Copyright (c) 2003, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ant.tests.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.IAntUIPreferenceConstants;
import org.eclipse.ant.internal.ui.launchConfigurations.IAntLaunchConfigurationConstants;
import org.eclipse.ant.tests.ui.testplugin.ConsoleLineTracker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.console.IHyperlink;


public class BuildTests extends AbstractAntUIBuildTest {

	public BuildTests(String name) {
		super(name);
	}
	
  /**
   * Tests launching Ant and getting messages
   * logged to the console.
   */
  public void testOutput() throws CoreException {
	  launch("echoing");
	  assertTrue("Incorrect number of messages logged for build. Should be 8. Was " + ConsoleLineTracker.getNumberOfMessages(), ConsoleLineTracker.getNumberOfMessages() == 8);
	  String message= ConsoleLineTracker.getMessage(6);
	  assertTrue("Incorrect last message. Should start with Total time:. Message: " + message, message.startsWith("Total time:"));
  }
  
  /**
   * This build will fail. With verbose on you should be presented with a full 
   * stack trace. Bug 82833
   */
  public void testVerboseStackTrace() throws CoreException {      
	  launch("failingTarget", "-k -verbose");
      assertEquals("Incorrect message", "BUILD FAILED", ConsoleLineTracker.getMessage(17));
      assertTrue("Incorrect message" + ConsoleLineTracker.getMessage(20), ConsoleLineTracker.getMessage(20).startsWith("\tat org.apache.tools.ant.taskdefs.Zip"));
  }
  
  /**
	 * Tests launching Ant and getting the build failed message
	 * logged to the console with associated link.
	 * Bug 42333 and 44565
	 */
	public void testBuildFailedMessage() throws CoreException {
		launch("bad");
		assertTrue("Incorrect number of messages logged for build. Should be 10. Was " + ConsoleLineTracker.getNumberOfMessages(), ConsoleLineTracker.getNumberOfMessages() == 10);
		String message= ConsoleLineTracker.getMessage(5);
		assertTrue("Incorrect last message. Should start with BUILD FAILED. Message: " + message, message.startsWith("BUILD FAILED"));
		int offset= -1;
		try {
			offset= ConsoleLineTracker.getDocument().getLineOffset(4) + 30; //link to buildfile that failed
		} catch (BadLocationException e) {
			assertTrue("failed getting offset of line", false);
		}
		IHyperlink link= getHyperlink(offset, ConsoleLineTracker.getDocument());
		assertNotNull("No hyperlink found at offset " + offset, link);
	}
  
  /**
	 * Tests launching Ant and that the
	 * correct links are in the console doc
	 */
	public void testLinks() throws CoreException {
		launch("build");
		int offset= 25; //buildfile link
		IHyperlink link= getHyperlink(offset, ConsoleLineTracker.getDocument());
		assertNotNull("No hyperlink found at offset " + offset, link);
		activateLink(link);
		
		try {
			offset= ConsoleLineTracker.getDocument().getLineOffset(4) + 10; //echo link
		} catch (BadLocationException e) {
			assertTrue("failed getting offset of line", false);
		}
		link= getHyperlink(offset, ConsoleLineTracker.getDocument());
		assertNotNull("No hyperlink found at offset " + offset, link);
		activateLink(link);
	}
	
	 /**
	 * Tests launching Ant and that build failed presents links to the failures when multi-line.
	 */
	public void testBuildFailedLinks() throws CoreException {
		launch("102282");
		try {
			int offset= ConsoleLineTracker.getDocument().getLineOffset(9) + 10; //second line of build failed link
			IHyperlink link= getHyperlink(offset, ConsoleLineTracker.getDocument());
			assertNotNull("No hyperlink found at offset " + offset, link);
			activateLink(link);
			
		} catch (BadLocationException e) {
			assertTrue("failed getting offset of line", false);
		}
	}
	
	/**
	 * Tests launching Ant and that the
	 * correct colors are in the console doc
	 */
	public void testColor() throws BadLocationException, CoreException {		
		launch("echoing");
		int offset= 15; //buildfile
		Color color= getColorAtOffset(offset, ConsoleLineTracker.getDocument());
		assertNotNull("No color found at " + offset, color);
		assertEquals(AntUIPlugin.getPreferenceColor(IAntUIPreferenceConstants.CONSOLE_INFO_COLOR), color);
		try {
			offset= ConsoleLineTracker.getDocument().getLineOffset(4) + 10; //echo
		} catch (BadLocationException e) {
			assertTrue("failed getting offset of line", false);
		}
		color= getColorAtOffset(offset, ConsoleLineTracker.getDocument());
		assertNotNull("No color found at " + offset, color);
		assertEquals(AntUIPlugin.getPreferenceColor(IAntUIPreferenceConstants.CONSOLE_WARNING_COLOR), color);
	}
	
	/**
	 * Tests launching Ant in a separate vm and that the
	 * correct property substitions occur
	 */
	public void testPropertySubstitution() throws CoreException {
		ILaunchConfiguration config = getLaunchConfiguration("74840");
		assertNotNull("Could not locate launch configuration for " + "74840", config);
		ILaunchConfigurationWorkingCopy copy= config.getWorkingCopy();
		Map properties= new HashMap(1);
		properties.put("platform.location", "${workspace_loc}");
		copy.setAttribute(IAntLaunchConfigurationConstants.ATTR_ANT_PROPERTIES, properties);
		copy.setAttribute(IAntLaunchConfigurationConstants.ATTR_ANT_PROPERTIES, properties);
		launchAndTerminate(copy, 20000);
		ConsoleLineTracker.waitForConsole();
		assertTrue("Incorrect number of messages logged for build. Should be 8. Was " + ConsoleLineTracker.getNumberOfMessages(), ConsoleLineTracker.getNumberOfMessages() == 8);
		assertTrue("Incorrect echo message. Should not include unsubstituted property", !ConsoleLineTracker.getMessage(4).trim().startsWith("[echo] ${workspace_loc}"));
	}
	
	 /**
	 * Tests specifying the XmlLogger as a listener (bug 80435)
     * @throws FileNotFoundException 
	 */
	public void testXmlLoggerListener() throws CoreException, FileNotFoundException {
		launch("echoing", "-listener org.apache.tools.ant.XmlLogger");
		assertTrue("Incorrect number of messages logged for build. Should be 8. Was " + ConsoleLineTracker.getNumberOfMessages(), ConsoleLineTracker.getNumberOfMessages() == 8);
		String message= ConsoleLineTracker.getMessage(6);
		assertTrue("Incorrect last message. Should start with Total time:. Message: " + message, message.startsWith("Total time:"));
		//find the log file generated by the xml logger
		getProject().getFolder("buildfiles").refreshLocal(IResource.DEPTH_INFINITE, null);
		File file= getBuildFile("log.xml");
		String content= getFileContentAsString(file);
		assertTrue("XML logging file is empty", content.length() > 0);
	}
	
	/**
	 * Tests launching Ant and getting the build failed message
	 * logged to the console.
	 * Bug 42333.
	 */
//	public void testBuildFailedMessageDebug() throws CoreException {
//		launchInDebug("bad");
//		assertTrue("Incorrect number of messages logged for build. Should be 35. Was " + ConsoleLineTracker.getNumberOfMessages(), ConsoleLineTracker.getNumberOfMessages() == 35);
//		String message= ConsoleLineTracker.getMessage(33);
//		assertTrue("Incorrect last message. Should start with Build Failed:. Message: " + message, message.startsWith("BUILD FAILED:"));
//		}
//  
//	  /**
//	 * Tests launching Ant and that the
//	 * correct links are in the console doc
//	 */
//	public void testLinksDebug() throws CoreException {
//		launchInDebug("echoing");
//		int offset= 0; 
//		try {
//			offset= ConsoleLineTracker.getDocument().getLineOffset(2) + 15; //buildfile line 3
//		} catch (BadLocationException e) {
//			assertTrue("failed getting offset of line", false);
//		}
//		IConsoleHyperlink link= getHyperlink(offset, ConsoleLineTracker.getDocument());
//		assertNotNull("No hyperlink found at offset " + offset, link);
//	
//		try {
//			offset= ConsoleLineTracker.getDocument().getLineOffset(33) + 10; //echo link
//		} catch (BadLocationException e) {
//			assertTrue("failed getting offset of line", false);
//		}
//		link= getHyperlink(offset, ConsoleLineTracker.getDocument());
//		assertNotNull("No hyperlink found at offset " + offset, link);
//	}
//
//	/**
//	 * Tests launching Ant and that the
//	 * correct colors are in the console doc
//	 */
//	public void testColorDebug() throws BadLocationException, CoreException {
//		launchInDebug("echoing");
//		int offset= 0; 
//		try {
//			offset= ConsoleLineTracker.getDocument().getLineOffset(2) + 15; //buildfile line 3
//		} catch (BadLocationException e) {
//			assertTrue("failed getting offset of line", false);
//		}
//		Color color= getColorAtOffset(offset, ConsoleLineTracker.getDocument());
//		assertNotNull("No color found at " + offset, color);
//		assertEquals(AntUIPlugin.getPreferenceColor(IAntUIPreferenceConstants.CONSOLE_INFO_COLOR), color);
//		
//		try {
//			offset= ConsoleLineTracker.getDocument().getLineOffset(4) + 3; //debug info
//		} catch (BadLocationException e) {
//			assertTrue("failed getting offset of line", false);
//		}
//		color= getColorAtOffset(offset, ConsoleLineTracker.getDocument());
//		assertNotNull("No color found at " + offset, color);
//		assertEquals(AntUIPlugin.getPreferenceColor(IAntUIPreferenceConstants.CONSOLE_DEBUG_COLOR), color);
//		
//		try {
//			offset= ConsoleLineTracker.getDocument().getLineOffset(33) + 10; //echo line 33
//		} catch (BadLocationException e) {
//			assertTrue("failed getting offset of line", false);
//		}
//		color= getColorAtOffset(offset, ConsoleLineTracker.getDocument());
//		assertNotNull("No color found at " + offset, color);
//		assertEquals(AntUIPlugin.getPreferenceColor(IAntUIPreferenceConstants.CONSOLE_WARNING_COLOR), color);
//	}
}
