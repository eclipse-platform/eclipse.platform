/*******************************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ant.tests.ui.separateVM;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.ant.internal.ui.launchConfigurations.IAntLaunchConfigurationConstants;
import org.eclipse.ant.internal.ui.model.AntUIPlugin;
import org.eclipse.ant.internal.ui.model.IAntUIPreferenceConstants;
import org.eclipse.ant.tests.ui.AbstractAntUIBuildTest;
import org.eclipse.ant.tests.ui.testplugin.ConsoleLineTracker;
import org.eclipse.ant.tests.ui.testplugin.ProjectHelper;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.console.IConsoleHyperlink;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.swt.graphics.Color;

public class SeparateVMTests extends AbstractAntUIBuildTest {
		
    public SeparateVMTests(String name) {
        super(name);
    }
    
	public static Test suite() {
		return new TestSuite(SeparateVMTests.class);
	}

    /**
     * Tests launching Ant in a separate vm and getting messages
     * logged to the console.
     */
    public void testBuild() throws CoreException {
      	launch("echoingSepVM");
      	assertTrue("Incorrect number of messages logged for build. Should be 6. Was " + ConsoleLineTracker.getNumberOfMessages(), ConsoleLineTracker.getNumberOfMessages() == 6);
      	assertTrue("Incorrect last message. Should start with Total time:. Message: " + ConsoleLineTracker.getMessage(4), ConsoleLineTracker.getMessage(4).startsWith("Total time:"));
    }
    
    /**
     * Tests launching Ant in a separate vm and having an extra classpath entry designated to be availble.
     */
    public void testExtraClasspathEntries() throws CoreException {
      	launch("extensionPointSepVM");
      	assertTrue("Incorrect number of messages logged for build. Should be 8. Was " + ConsoleLineTracker.getNumberOfMessages(), ConsoleLineTracker.getNumberOfMessages() == 8);
      	assertTrue("Incorrect last message. Should start with Total time:. Message: " + ConsoleLineTracker.getMessage(6), ConsoleLineTracker.getMessage(6).startsWith("Total time:"));
    }
    
    /**
     * Tests launching Ant in a separate vm and having an extra classpath entry designated to be available.
     */
    public void testProperties() throws CoreException {
      	launch("extensionPointSepVM");
      	assertTrue("Incorrect number of messages logged for build. Should be 8. Was " + ConsoleLineTracker.getNumberOfMessages(), ConsoleLineTracker.getNumberOfMessages() == 8);
      	assertTrue("Incorrect last message. Should start with [echo] ${property.ui.testing. Message: " + ConsoleLineTracker.getMessage(3), ConsoleLineTracker.getMessage(3).trim().startsWith("[echo] ${property.ui.testing"));
      	assertTrue("Incorrect last message. Should start with [echo] hey. Message: " + ConsoleLineTracker.getMessage(4), ConsoleLineTracker.getMessage(4).trim().startsWith("[echo] hey"));
    }
    
    /**
     * Tests launching Ant in a separate vm and having an extra classpath entry designated to be available.
     */
    public void testExtensionPointTask() throws CoreException {
      	launch("extensionPointTaskSepVM");
      	assertTrue("Incorrect number of messages logged for build. Should be 6. Was " + ConsoleLineTracker.getNumberOfMessages(), ConsoleLineTracker.getNumberOfMessages() == 6);
      	assertTrue("Incorrect message. Should start with [null] Testing Ant in Eclipse with a custom task2. Message: " + ConsoleLineTracker.getMessage(2), ConsoleLineTracker.getMessage(2).trim().startsWith("[null] Testing Ant in Eclipse with a custom task2"));
    }
    
    /**
     * Tests launching Ant in a separate vm and having an extra classpath entry designated to be available.
     */
    public void testExtensionPointType() throws CoreException {
      	launch("extensionPointTypeSepVM");
      	assertTrue("Incorrect number of messages logged for build. Should be 6. Was " + ConsoleLineTracker.getNumberOfMessages(), ConsoleLineTracker.getNumberOfMessages() == 6);
      	assertTrue("Incorrect message. Should start with [echo] Ensure that an extension point defined type. Message: " + ConsoleLineTracker.getMessage(2), ConsoleLineTracker.getMessage(2).trim().startsWith("[echo] Ensure that an extension point defined type"));
    }
    
	/**
	 * Tests launching Ant in a separate vm and that the
	 * correct links are in the console doc
	 */
	public void testLinks() throws CoreException {
		launch("echoingSepVM");
		int offset= 15; //buildfile link
		IConsoleHyperlink link= getHyperlink(offset, ConsoleLineTracker.getDocument());
		assertNotNull("No hyperlink found at offset " + offset, link);
		try {
			offset= ConsoleLineTracker.getDocument().getLineOffset(2) + 10; //echo link
		} catch (BadLocationException e) {
			assertTrue("failed getting offset of line", false);
		}
		link= getHyperlink(offset, ConsoleLineTracker.getDocument());
		assertNotNull("No hyperlink found at offset " + offset, link);
	}
	
	/**
	 * Tests launching Ant in a separate vm and that the
	 * correct colors are in the console doc
	 */
	public void testColor() throws BadLocationException, CoreException {
		launch("echoingSepVM");
		int offset= 15; //buildfile
		Color color= getColorAtOffset(offset, ConsoleLineTracker.getDocument());
		assertNotNull("No color found at " + offset, color);
		assertEquals(color, AntUIPlugin.getPreferenceColor(IAntUIPreferenceConstants.CONSOLE_INFO_COLOR));
		try {
			offset= ConsoleLineTracker.getDocument().getLineOffset(2) + 10; //echo link
		} catch (BadLocationException e) {
			assertTrue("failed getting offset of line", false);
		}
		color= getColorAtOffset(offset, ConsoleLineTracker.getDocument());
		assertNotNull("No color found at " + offset, color);
		assertEquals(color, AntUIPlugin.getPreferenceColor(IAntUIPreferenceConstants.CONSOLE_WARNING_COLOR));
	}
	
	/**
	 * Tests launching Ant in a separate vm and that the
	 * correct working directory is set
	 */
	public void testWorkingDirectory() throws CoreException {
		ILaunchConfiguration config = getLaunchConfiguration("echoingSepVM");
		assertNotNull("Could not locate launch configuration for " + "echoingSepVM", config);
		ILaunchConfigurationWorkingCopy copy= config.getWorkingCopy();
		copy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY, getJavaProject().getProject().getLocation().toOSString());
		copy.setAttribute(IAntLaunchConfigurationConstants.ATTR_ANT_TARGETS, "Bug42984");
		launchAndTerminate(copy, 20000);
		ConsoleLineTracker.waitForConsole();
		assertTrue("Incorrect number of messages logged for build. Should be 6. Was " + ConsoleLineTracker.getNumberOfMessages(), ConsoleLineTracker.getNumberOfMessages() == 6);
		assertTrue("Incorrect last message. Should end with " + ProjectHelper.PROJECT_NAME + ". Message: " + ConsoleLineTracker.getMessage(2), ConsoleLineTracker.getMessage(2).endsWith(ProjectHelper.PROJECT_NAME));
	}
	
	 /**
     * Tests launching Ant in a separate vm and getting messages
     * logged to the console for project help.
     */
    public void testProjectHelp() throws CoreException {
      	launch("echoingSepVM", "-p");
      	assertTrue("Incorrect number of messages logged for build. Should be 14. Was " + ConsoleLineTracker.getNumberOfMessages(), ConsoleLineTracker.getNumberOfMessages() == 14);
      	assertTrue("Incorrect last message. Should start with echo2:. Message: " + ConsoleLineTracker.getMessage(12), ConsoleLineTracker.getMessage(12).trim().startsWith("echo2"));
    }
}