/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ant.tests.ui;

import org.eclipse.ant.internal.ui.model.AntUIPlugin;
import org.eclipse.ant.internal.ui.model.IAntUIPreferenceConstants;
import org.eclipse.ant.tests.ui.testplugin.ConsoleLineTracker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.ui.console.IConsoleHyperlink;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.swt.graphics.Color;


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
	 * Tests launching Ant and getting the build failed message
	 * logged to the console with associated link.
	 * Bug 42333 and 44565
	 */
	public void testBuildFailedMessage() throws CoreException {
		launch("bad");
		assertTrue("Incorrect number of messages logged for build. Should be 7. Was " + ConsoleLineTracker.getNumberOfMessages(), ConsoleLineTracker.getNumberOfMessages() == 7);
		String message= ConsoleLineTracker.getMessage(4);
		assertTrue("Incorrect last message. Should start with Build Failed:. Message: " + message, message.startsWith("BUILD FAILED:"));
		int offset= -1;
		try {
			offset= ConsoleLineTracker.getDocument().getLineOffset(4) + 30; //link to buildfile that failed
		} catch (BadLocationException e) {
			assertTrue("failed getting offset of line", false);
		}
		IConsoleHyperlink link= getHyperlink(offset, ConsoleLineTracker.getDocument());
		assertNotNull("No hyperlink found at offset " + offset, link);
	}
  
  /**
	 * Tests launching Ant and that the
	 * correct links are in the console doc
	 */
	public void testLinks() throws CoreException {
		launch("build");
		int offset= 25; //buildfile link
		IConsoleHyperlink link= getHyperlink(offset, ConsoleLineTracker.getDocument());
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