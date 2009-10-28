/*******************************************************************************
 * Copyright (c) 2004, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.ant.tests.ui.separateVM;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.IAntUIPreferenceConstants;
import org.eclipse.ant.internal.ui.launchConfigurations.IAntLaunchConfigurationConstants;
import org.eclipse.ant.tests.ui.AbstractAntUIBuildTest;
import org.eclipse.ant.tests.ui.testplugin.ConsoleLineTracker;
import org.eclipse.ant.tests.ui.testplugin.ProjectHelper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.console.IHyperlink;

public class SeparateVMTests extends AbstractAntUIBuildTest {
	
	protected static final String PLUGIN_VERSION= "org.apache.ant_1.7.0";
		
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
     * Tests launching Ant in a separate vm and having an extra classpath entry designated to be available.
     */
    public void testExtraClasspathEntries() throws CoreException {
      	launch("extensionPointSepVM");
      	assertTrue("Incorrect number of messages logged for build. Should be 8. Was " + ConsoleLineTracker.getNumberOfMessages(), ConsoleLineTracker.getNumberOfMessages() == 8);
      	assertTrue("Incorrect last message. Should start with Total time:. Message: " + ConsoleLineTracker.getMessage(6), ConsoleLineTracker.getMessage(6).startsWith("Total time:"));
    }
    
    /**
     * Tests launching Ant in a separate vm and having a property designated to be available.
     */
    public void testProperties() throws CoreException {
      	launch("extensionPointSepVM");
      	assertTrue("Incorrect number of messages logged for build. Should be 8. Was " + ConsoleLineTracker.getNumberOfMessages(), ConsoleLineTracker.getNumberOfMessages() == 8);
      	assertTrue("Incorrect last message. Should start with [echo] ${property.ui.testing. Message: " + ConsoleLineTracker.getMessage(3), ConsoleLineTracker.getMessage(3).trim().startsWith("[echo] ${property.ui.testing"));
      	assertTrue("Incorrect last message. Should start with [echo] hey. Message: " + ConsoleLineTracker.getMessage(4), ConsoleLineTracker.getMessage(4).trim().startsWith("[echo] hey"));
    }
    
    /**
     * Tests launching Ant in a separate vm and having an task designated to be available.
     */
    public void testExtensionPointTask() throws CoreException {
      	launch("extensionPointTaskSepVM");
      	assertTrue("Incorrect number of messages logged for build. Should be 7. Was " + ConsoleLineTracker.getNumberOfMessages(), ConsoleLineTracker.getNumberOfMessages() == 7);
      	assertTrue("Incorrect message. Should start with [null] Testing Ant in Eclipse with a custom task2. Message: " + ConsoleLineTracker.getMessage(2), ConsoleLineTracker.getMessage(2).trim().startsWith("[null] Testing Ant in Eclipse with a custom task2"));
      	assertTrue("Incorrect message. Should start with [null] Testing Ant in Eclipse with a custom task2. Message: " + ConsoleLineTracker.getMessage(3), ConsoleLineTracker.getMessage(3).trim().startsWith("[null] Testing Ant in Eclipse with a custom task2"));
    }
    
    /**
     * Tests launching Ant in a separate vm and having a type designated to be available.
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
		IHyperlink link= getHyperlink(offset, ConsoleLineTracker.getDocument());
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
	 * Tests launching Ant and that build failed presents links to the failures
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
	 * Tests launching Ant in a separate vm and that the
	 * correct property substitions occur
	 */
	public void testPropertySubstitution() throws CoreException {
		ILaunchConfiguration config = getLaunchConfiguration("74840SepVM");
		assertNotNull("Could not locate launch configuration for " + "74840SepVM", config);
		ILaunchConfigurationWorkingCopy copy= config.getWorkingCopy();
		Map properties= new HashMap(1);
		properties.put("platform.location", "${workspace_loc}");
		copy.setAttribute(IAntLaunchConfigurationConstants.ATTR_ANT_PROPERTIES, properties);
		launchAndTerminate(copy, 20000);
		ConsoleLineTracker.waitForConsole();
		assertTrue("Incorrect number of messages logged for build. Should be 6. Was " + ConsoleLineTracker.getNumberOfMessages(), ConsoleLineTracker.getNumberOfMessages() == 6);
		assertTrue("Incorrect echo message. Should not include unsubstituted property ", !ConsoleLineTracker.getMessage(2).trim().startsWith("[echo] ${workspace_loc}"));
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
 
    /**
	 * Tests specifying the XmlLogger as a listener (bug 80435)
     * @throws FileNotFoundException 
	 */
	public void testXmlLoggerListener() throws CoreException, FileNotFoundException {
		launch("echoingSepVM", "-listener org.apache.tools.ant.XmlLogger");
		assertTrue("Incorrect number of messages logged for build. Should be 6. Was " + ConsoleLineTracker.getNumberOfMessages(), ConsoleLineTracker.getNumberOfMessages() == 6);
      	assertTrue("Incorrect last message. Should start with Total time:. Message: " + ConsoleLineTracker.getMessage(4), ConsoleLineTracker.getMessage(4).startsWith("Total time:"));
		
		//find the log file generated by the xml logger
		getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
		IFile iFile= getProject().getFolder("buildfiles").getFile("log.xml");	
		assertTrue("Could not find log file named: log.xml" , iFile.exists());
		File file= iFile.getLocation().toFile();
		String content= getFileContentAsString(file);
		assertTrue("XML logging file is empty", content.length() > 0);
	}
	
	 /**
     * Tests launching Ant in a separate vm and that the Environment variable
     * ANT_HOME is set from the Ant home set for the build and ant.home is set as a property.
     * Bug 75729
     */
    public void testAntHome() throws CoreException {
      	launch("environmentVar");
      	assertTrue("Incorrect number of messages logged for build. Should be 6. Was " + ConsoleLineTracker.getNumberOfMessages(), ConsoleLineTracker.getNumberOfMessages() == 6);
      	String message= ConsoleLineTracker.getMessage(1);
      	assertTrue("Incorrect message. Should end with org.apache.ant. Message: " + message, checkAntHomeMessage(message));
      	message= ConsoleLineTracker.getMessage(2);
		assertTrue("Incorrect message. Should end with org.apache.ant. Message: " + message, checkAntHomeMessage(message));
		
    }

	private boolean checkAntHomeMessage(String message) {
		if (message.endsWith("org.apache.ant")) {
			return true;
		}
		
		int index = message.lastIndexOf(PLUGIN_VERSION);
		if (index == -1) {
			return false;
		}
		//org.apache.ant_1.7.0.v200704241635
		int result = message.length() - (index + PLUGIN_VERSION.length());
		return  result == 14;
	}
}
