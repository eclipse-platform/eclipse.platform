/*******************************************************************************
 *  Copyright (c) 2004, 2021 IBM Corporation and others.
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

package org.eclipse.ant.tests.ui.separateVM;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.IAntUIConstants;
import org.eclipse.ant.internal.ui.IAntUIPreferenceConstants;
import org.eclipse.ant.launching.IAntLaunchConstants;
import org.eclipse.ant.tests.ui.AbstractAntUIBuildTest;
import org.eclipse.ant.tests.ui.debug.TestAgainException;
import org.eclipse.ant.tests.ui.testplugin.ConsoleLineTracker;
import org.eclipse.ant.tests.ui.testplugin.ProjectHelper;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.console.IHyperlink;

@SuppressWarnings("restriction")
public class SeparateVMTests extends AbstractAntUIBuildTest {

	protected static final String PLUGIN_VERSION;

	static {
		PLUGIN_VERSION = Platform.getBundle("org.apache.ant").getVersion().toString(); //$NON-NLS-1$
	}

	public SeparateVMTests(String name) {
		super(name);
	}

	/**
	 * Checks that the expected line count has been reached and if not dump out what
	 * was tracked to System.err
	 *
	 * @param expectedLines
	 * @since 3.8.200
	 */
	void assertLines(int expectedLines) {
		ConsoleLineTracker.waitForConsole();
		if (ConsoleLineTracker.getNumberOfMessages() != expectedLines) {
			List<String> lines = ConsoleLineTracker.getAllMessages();
			System.out.println("Failed line count from " + getName() + ", tracked lines: "); //$NON-NLS-1$ //$NON-NLS-2$
			for (String string : lines) {
				System.out.println('\t' + string);
			}
			throw new TestAgainException("Test again - Incorrect number of messages logged for build - should be " //$NON-NLS-1$
					+ expectedLines + " but was " //$NON-NLS-1$
					+ ConsoleLineTracker.getNumberOfMessages());
		}
	}

	/**
	 * Tests launching Ant in a separate VM and getting messages logged to the
	 * console.
	 */
	public void testBuild() throws CoreException {
		launch("echoingSepVM"); //$NON-NLS-1$
		assertLines(6);
		assertTrue("Incorrect last message. Should start with Total time:. Message: " //$NON-NLS-1$
				+ ConsoleLineTracker.getMessage(4), ConsoleLineTracker.getMessage(4).startsWith("Total time:")); //$NON-NLS-1$
	}

	/**
	 * Tests launching Ant in a separate VM and having an extra classpath entry
	 * designated to be available.
	 */
	public void testExtraClasspathEntries() throws CoreException {
		launch("extensionPointSepVM"); //$NON-NLS-1$
		assertLines(8);
		assertTrue("Incorrect last message. Should start with Total time:. Message: " //$NON-NLS-1$
				+ ConsoleLineTracker.getMessage(6), ConsoleLineTracker.getMessage(6).startsWith("Total time:")); //$NON-NLS-1$
	}

	/**
	 * Tests launching Ant in a separate VM and having a property designated to be
	 * available.
	 */
	public void testProperties() throws CoreException {
		launch("extensionPointSepVM"); //$NON-NLS-1$
		assertLines(8);
		assertTrue("Incorrect last message. Should start with [echo] ${property.ui.testing. Message: " //$NON-NLS-1$
				+ ConsoleLineTracker.getMessage(3),
				ConsoleLineTracker.getMessage(3).trim().startsWith("[echo] ${property.ui.testing")); //$NON-NLS-1$
		assertTrue("Incorrect last message. Should start with [echo] hey. Message: " //$NON-NLS-1$
				+ ConsoleLineTracker.getMessage(4), ConsoleLineTracker.getMessage(4).trim().startsWith("[echo] hey")); //$NON-NLS-1$
	}

	/**
	 * Tests launching Ant in a separate VM and having an task designated to be
	 * available.
	 */
	public void testExtensionPointTask() throws CoreException {
		launch("extensionPointTaskSepVM"); //$NON-NLS-1$
		assertLines(7);
		assertTrue("Incorrect message. Should start with [null] Testing Ant in Eclipse with a custom task2. Message: " //$NON-NLS-1$
				+ ConsoleLineTracker.getMessage(2),
				ConsoleLineTracker.getMessage(2).trim()
				.startsWith("[null] Testing Ant in Eclipse with a custom task2")); //$NON-NLS-1$
		assertTrue("Incorrect message. Should start with [null] Testing Ant in Eclipse with a custom task2. Message: " //$NON-NLS-1$
				+ ConsoleLineTracker.getMessage(3),
				ConsoleLineTracker.getMessage(3).trim()
				.startsWith("[null] Testing Ant in Eclipse with a custom task2")); //$NON-NLS-1$
	}

	/**
	 * Tests launching Ant in a separate VM and having a type designated to be
	 * available.
	 */
	public void testExtensionPointType() throws CoreException {
		launch("extensionPointTypeSepVM"); //$NON-NLS-1$
		assertLines(6);
		assertTrue("Incorrect message. Should start with [echo] Ensure that an extension point defined type. Message: " //$NON-NLS-1$
				+ ConsoleLineTracker.getMessage(2),
				ConsoleLineTracker.getMessage(2).trim()
				.startsWith("[echo] Ensure that an extension point defined type")); //$NON-NLS-1$
	}

	/**
	 * Tests launching Ant in a separate VM and that the correct links are in the
	 * console doc
	 */
	public void testLinks() throws CoreException, BadLocationException {
		launch("echoingSepVM"); //$NON-NLS-1$
		int offset = 15; // buildfile link
		IHyperlink link = getHyperlink(offset, ConsoleLineTracker.getDocument());
		assertNotNull("No hyperlink found at offset " + offset, link); //$NON-NLS-1$
		offset = ConsoleLineTracker.getDocument().getLineOffset(2) + 10; // echo link
		link = getHyperlink(offset, ConsoleLineTracker.getDocument());
		assertNotNull("No hyperlink found at offset " + offset, link); //$NON-NLS-1$
	}

	/**
	 * Tests launching Ant and that build failed presents links to the failures
	 */
	public void testBuildFailedLinks() throws CoreException, BadLocationException {
		launch("102282"); //$NON-NLS-1$
		IDocument document = ConsoleLineTracker.getDocument();
		int offset = document.getLineOffset(9) + 10; // second line of build failed link
		IHyperlink link = getHyperlink(offset, document);
		assertNotNull("No hyperlink found at offset " + offset + "\n" + document, link); //$NON-NLS-1$ //$NON-NLS-2$
		activateLink(link);
	}

	/**
	 * Tests launching Ant in a separate VM and that the correct colors are in the
	 * console doc
	 */
	public void testColor() throws BadLocationException, CoreException {
		launch("echoingSepVM"); //$NON-NLS-1$
		int offset = 15; // buildfile
		Color color = getColorAtOffset(offset, ConsoleLineTracker.getDocument());
		assertNotNull("No color found at " + offset, color); //$NON-NLS-1$
		assertEquals(color, AntUIPlugin.getPreferenceColor(IAntUIPreferenceConstants.CONSOLE_INFO_COLOR));
		offset = ConsoleLineTracker.getDocument().getLineOffset(2) + 10; // echo link
		color = getColorAtOffset(offset, ConsoleLineTracker.getDocument());
		assertNotNull("No color found at " + offset, color); //$NON-NLS-1$
		assertEquals(color, AntUIPlugin.getPreferenceColor(IAntUIPreferenceConstants.CONSOLE_WARNING_COLOR));
	}

	/**
	 * Tests launching Ant in a separate VM and that the correct working directory
	 * is set
	 */
	public void testWorkingDirectory() throws CoreException {
		ILaunchConfiguration config = getLaunchConfiguration("echoingSepVM"); //$NON-NLS-1$
		assertNotNull("Could not locate launch configuration for " + "echoingSepVM", config); //$NON-NLS-1$ //$NON-NLS-2$
		ILaunchConfigurationWorkingCopy copy = config.getWorkingCopy();
		copy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY,
				getJavaProject().getProject().getLocation().toOSString());
		copy.setAttribute(IAntLaunchConstants.ATTR_ANT_TARGETS, "Bug42984"); //$NON-NLS-1$
		launchAndTerminate(copy, 20000);
		ConsoleLineTracker.waitForConsole();
		assertLines(6);
		assertTrue("Incorrect last message. Should end with " + ProjectHelper.PROJECT_NAME + ". Message: " //$NON-NLS-1$ //$NON-NLS-2$
				+ ConsoleLineTracker.getMessage(2),
				ConsoleLineTracker.getMessage(2).endsWith(ProjectHelper.PROJECT_NAME));
	}

	/**
	 * Tests launching Ant in a separate VM and that the correct property
	 * substitutions occur
	 */
	public void testPropertySubstitution() throws CoreException {
		ILaunchConfiguration config = getLaunchConfiguration("74840SepVM"); //$NON-NLS-1$
		assertNotNull("Could not locate launch configuration for " + "74840SepVM", config); //$NON-NLS-1$ //$NON-NLS-2$
		ILaunchConfigurationWorkingCopy copy = config.getWorkingCopy();
		Map<String, String> properties = new HashMap<>(1);
		properties.put("platform.location", "${workspace_loc}"); //$NON-NLS-1$ //$NON-NLS-2$
		copy.setAttribute(IAntLaunchConstants.ATTR_ANT_PROPERTIES, properties);
		launchAndTerminate(copy, 20000);
		ConsoleLineTracker.waitForConsole();
		assertLines(6);
		assertFalse("Incorrect echo message. Should not include unsubstituted property ", //$NON-NLS-1$
				ConsoleLineTracker.getMessage(2).trim().startsWith("[echo] ${workspace_loc}")); //$NON-NLS-1$
	}

	/**
	 * Tests launching Ant in a separate VM and getting messages logged to the
	 * console for project help.
	 */
	public void testProjectHelp() throws CoreException {
		launch("echoingSepVM", "-p"); //$NON-NLS-1$ //$NON-NLS-2$
		assertLines(14);
		assertTrue("Incorrect last message. Should start with echo2:. Message: " //$NON-NLS-1$
				+ ConsoleLineTracker.getMessage(12), ConsoleLineTracker.getMessage(12).trim().startsWith("echo2")); //$NON-NLS-1$
	}

	/**
	 * Tests specifying the XmlLogger as a listener (bug 80435)
	 *
	 * @throws FileNotFoundException
	 */
	public void testXmlLoggerListener() throws CoreException, FileNotFoundException {
		launch("echoingSepVM", "-listener org.apache.tools.ant.XmlLogger"); //$NON-NLS-1$ //$NON-NLS-2$
		assertLines(6);
		assertTrue("Incorrect last message. Should start with Total time:. Message: " //$NON-NLS-1$
				+ ConsoleLineTracker.getMessage(4), ConsoleLineTracker.getMessage(4).startsWith("Total time:")); //$NON-NLS-1$

		// find the log file generated by the XML logger
		getProject().refreshLocal(IResource.DEPTH_INFINITE, null);
		IFile iFile = getProject().getFolder("buildfiles").getFile("log.xml"); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Could not find log file named: log.xml", iFile.exists()); //$NON-NLS-1$
		File file = iFile.getLocation().toFile();
		String content = getFileContentAsString(file);
		assertTrue("XML logging file is empty", content.length() > 0); //$NON-NLS-1$
	}

	/**
	 * Tests launching Ant in a separate VM and that the Environment variable
	 * ANT_HOME is set from the Ant home set for the build and ant.home is set as a
	 * property. Bug 75729
	 */
	public void testAntHome() throws CoreException {
		launch("environmentVar"); //$NON-NLS-1$
		assertLines(6);
		String message = ConsoleLineTracker.getMessage(1);
		assertTrue("Incorrect message. Should end with org.apache.ant [" + message + "]", checkAntHomeMessage(message)); //$NON-NLS-1$ //$NON-NLS-2$
		message = ConsoleLineTracker.getMessage(2);
		assertTrue("Incorrect message. Should end with org.apache.ant. Message: " + message, //$NON-NLS-1$
				checkAntHomeMessage(message));
	}

	private boolean checkAntHomeMessage(String message) {
		String msg = message;
		if (msg.endsWith("org.apache.ant")) { //$NON-NLS-1$
			return true;
		}

		if (msg.endsWith(PLUGIN_VERSION)) {
			return true;
		}

		int index = msg.lastIndexOf('.');
		if (index > 0) {
			msg = msg.substring(0, index);
		}
		return msg.endsWith(PLUGIN_VERSION);
	}

	public void testFailInputHandler() throws CoreException {
		ILaunchConfiguration config = getLaunchConfiguration("echoingSepVM"); //$NON-NLS-1$
		assertNotNull("Could not locate launch configuration for " + "echoingSepVM", config); //$NON-NLS-1$ //$NON-NLS-2$
		ILaunchConfigurationWorkingCopy copy = config.getWorkingCopy();
		copy.setAttribute(IAntUIConstants.SET_INPUTHANDLER, false);
		launch(copy);
		String message = ConsoleLineTracker.getMessage(1);
		assertNotNull("There must be a message", message); //$NON-NLS-1$
		assertTrue("Incorrect message. Should start with Message:. Message: " + message, message.startsWith("echo1")); //$NON-NLS-1$ //$NON-NLS-2$
	}
}
