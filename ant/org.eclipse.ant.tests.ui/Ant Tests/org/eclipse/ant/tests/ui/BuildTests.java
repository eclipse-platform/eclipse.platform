/*******************************************************************************
 * Copyright (c) 2003, 2021 IBM Corporation and others.
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

package org.eclipse.ant.tests.ui;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.IAntUIPreferenceConstants;
import org.eclipse.ant.launching.IAntLaunchConstants;
import org.eclipse.ant.tests.ui.testplugin.ConsoleLineTracker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.console.IHyperlink;

@SuppressWarnings("restriction")
public class BuildTests extends AbstractAntUIBuildTest {

	public BuildTests(String name) {
		super(name);
	}

	/**
	 * Tests launching Ant and getting messages logged to the console.
	 */
	public void testOutput() throws CoreException {
		launch("echoing"); //$NON-NLS-1$
		assertEquals("Incorrect number of messages logged for build. Should be 8. Was " //$NON-NLS-1$
				+ ConsoleLineTracker.getNumberOfMessages(), 8, ConsoleLineTracker.getNumberOfMessages());
		String message = ConsoleLineTracker.getMessage(6);
		assertTrue("Incorrect last message. Should start with Total time:. Message: " + message, //$NON-NLS-1$
				message.startsWith("Total time:")); //$NON-NLS-1$
	}

	/**
	 * This build will fail. With verbose on you should be presented with a full
	 * stack trace. Bug 82833
	 */
	public void testVerboseStackTrace() throws Exception {
		IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode("org.eclipse.ui.monitoring"); //$NON-NLS-1$
		if (prefs != null) {
			prefs.putBoolean("monitoring_enabled", false); //$NON-NLS-1$
		}
		launch("failingTarget", "-k -verbose"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("Incorrect message", "BUILD FAILED", ConsoleLineTracker.getMessage(19)); //$NON-NLS-1$ //$NON-NLS-2$
		assertTrue("Incorrect message" + ConsoleLineTracker.getMessage(22), //$NON-NLS-1$
				ConsoleLineTracker.getMessage(22).startsWith("\tat org.apache.tools.ant.taskdefs.Zip")); //$NON-NLS-1$
	}

	/**
	 * Tests launching Ant and getting the build failed message logged to the
	 * console with associated link. Bug 42333 and 44565
	 */
	public void testBuildFailedMessage() throws CoreException, BadLocationException {
		launch("bad"); //$NON-NLS-1$
		assertEquals("Incorrect number of messages logged for build. Should be 10. Was " //$NON-NLS-1$
				+ ConsoleLineTracker.getNumberOfMessages(), 10, ConsoleLineTracker.getNumberOfMessages());
		String message = ConsoleLineTracker.getMessage(5);
		assertTrue("Incorrect last message. Should start with BUILD FAILED. Message: " + message, //$NON-NLS-1$
				message.startsWith("BUILD FAILED")); //$NON-NLS-1$
		int offset = -1;
		IDocument document = ConsoleLineTracker.getDocument();
		offset = document.getLineOffset(4) + 30; // link to buildfile that failed
		IHyperlink link = getHyperlink(offset, document);
		assertNotNull("No hyperlink found at offset " + offset + "\n" + document, link); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Tests launching Ant and that the correct links are in the console doc
	 */
	public void testLinks() throws CoreException, BadLocationException {
		launch("build"); //$NON-NLS-1$
		int offset = 25; // buildfile link
		IHyperlink link = getHyperlink(offset, ConsoleLineTracker.getDocument());
		assertNotNull("No hyperlink found at offset " + offset, link); //$NON-NLS-1$
		activateLink(link);

		offset = ConsoleLineTracker.getDocument().getLineOffset(4) + 10; // echo link
		link = getHyperlink(offset, ConsoleLineTracker.getDocument());
		assertNotNull("No hyperlink found at offset " + offset, link); //$NON-NLS-1$
		activateLink(link);
	}

	/**
	 * Tests launching Ant and that build failed presents links to the failures when
	 * multi-line.
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
	 * Tests launching Ant and that the correct colors are in the console doc
	 */
	public void testColor() throws BadLocationException, CoreException {
		launch("echoing"); //$NON-NLS-1$
		ConsoleLineTracker.waitForConsole();
		int offset = 15; // buildfile
		Color color = getColorAtOffset(offset, ConsoleLineTracker.getDocument());
		assertNotNull("No color found at " + offset, color); //$NON-NLS-1$
		assertEquals(AntUIPlugin.getPreferenceColor(IAntUIPreferenceConstants.CONSOLE_INFO_COLOR), color);
		offset = ConsoleLineTracker.getDocument().getLineOffset(4) + 10; // echo
		color = getColorAtOffset(offset, ConsoleLineTracker.getDocument());
		assertNotNull("No color found at " + offset, color); //$NON-NLS-1$
		assertEquals(AntUIPlugin.getPreferenceColor(IAntUIPreferenceConstants.CONSOLE_WARNING_COLOR), color);
	}

	/**
	 * Tests launching Ant in a separate vm and that the correct property
	 * substitions occur
	 */
	public void testPropertySubstitution() throws CoreException {
		ILaunchConfiguration config = getLaunchConfiguration("74840"); //$NON-NLS-1$
		assertNotNull("Could not locate launch configuration for " + "74840", config); //$NON-NLS-1$ //$NON-NLS-2$
		ILaunchConfigurationWorkingCopy copy = config.getWorkingCopy();
		Map<String, String> properties = new HashMap<>(1);
		properties.put("platform.location", "${workspace_loc}"); //$NON-NLS-1$ //$NON-NLS-2$
		copy.setAttribute(IAntLaunchConstants.ATTR_ANT_PROPERTIES, properties);
		copy.setAttribute(IAntLaunchConstants.ATTR_ANT_PROPERTIES, properties);
		launchAndTerminate(copy, 20000);
		ConsoleLineTracker.waitForConsole();
		assertEquals("Incorrect number of messages logged for build. Should be 8. Was " //$NON-NLS-1$
				+ ConsoleLineTracker.getNumberOfMessages(), 8, ConsoleLineTracker.getNumberOfMessages());
		assertFalse("Incorrect echo message. Should not include unsubstituted property", //$NON-NLS-1$
				ConsoleLineTracker.getMessage(4).trim().startsWith("[echo] ${workspace_loc}")); //$NON-NLS-1$
	}

	/**
	 * Tests specifying the XmlLogger as a listener (bug 80435)
	 *
	 * @throws FileNotFoundException
	 */
	public void testXmlLoggerListener() throws CoreException, FileNotFoundException {
		launch("echoing", "-listener org.apache.tools.ant.XmlLogger"); //$NON-NLS-1$ //$NON-NLS-2$
		assertEquals("Incorrect number of messages logged for build. Should be 8. Was " //$NON-NLS-1$
				+ ConsoleLineTracker.getNumberOfMessages(), 8, ConsoleLineTracker.getNumberOfMessages());
		String message = ConsoleLineTracker.getMessage(6);
		assertTrue("Incorrect last message. Should start with Total time:. Message: " + message, //$NON-NLS-1$
				message.startsWith("Total time:")); //$NON-NLS-1$
		// find the log file generated by the xml logger
		getProject().getFolder("buildfiles").refreshLocal(IResource.DEPTH_INFINITE, null); //$NON-NLS-1$
		File file = getBuildFile("log.xml"); //$NON-NLS-1$
		String content = getFileContentAsString(file);
		assertTrue("XML logging file is empty", content.length() > 0); //$NON-NLS-1$
	}

	/**
	 * Tests launching Ant and getting the build failed message logged to the
	 * console. Bug 42333.
	 */
	// public void testBuildFailedMessageDebug() throws CoreException {
	// launchInDebug("bad");
	// assertTrue("Incorrect number of messages logged for build. Should be 35. Was
	// " + ConsoleLineTracker.getNumberOfMessages(),
	// ConsoleLineTracker.getNumberOfMessages() == 35);
	// String message= ConsoleLineTracker.getMessage(33);
	// assertTrue("Incorrect last message. Should start with Build Failed:. Message:
	// " + message, message.startsWith("BUILD FAILED:"));
	// }
	//
	// /**
	// * Tests launching Ant and that the
	// * correct links are in the console doc
	// */
	// public void testLinksDebug() throws CoreException {
	// launchInDebug("echoing");
	// int offset= 0;
	// try {
	// offset= ConsoleLineTracker.getDocument().getLineOffset(2) + 15; //buildfile
	// line 3
	// } catch (BadLocationException e) {
	// assertTrue("failed getting offset of line", false);
	// }
	// IConsoleHyperlink link= getHyperlink(offset,
	// ConsoleLineTracker.getDocument());
	// assertNotNull("No hyperlink found at offset " + offset, link);
	//
	// try {
	// offset= ConsoleLineTracker.getDocument().getLineOffset(33) + 10; //echo link
	// } catch (BadLocationException e) {
	// assertTrue("failed getting offset of line", false);
	// }
	// link= getHyperlink(offset, ConsoleLineTracker.getDocument());
	// assertNotNull("No hyperlink found at offset " + offset, link);
	// }
	//
	// /**
	// * Tests launching Ant and that the
	// * correct colors are in the console doc
	// */
	// public void testColorDebug() throws BadLocationException, CoreException {
	// launchInDebug("echoing");
	// int offset= 0;
	// try {
	// offset= ConsoleLineTracker.getDocument().getLineOffset(2) + 15; //buildfile
	// line 3
	// } catch (BadLocationException e) {
	// assertTrue("failed getting offset of line", false);
	// }
	// Color color= getColorAtOffset(offset, ConsoleLineTracker.getDocument());
	// assertNotNull("No color found at " + offset, color);
	// assertEquals(AntUIPlugin.getPreferenceColor(IAntUIPreferenceConstants.CONSOLE_INFO_COLOR),
	// color);
	//
	// try {
	// offset= ConsoleLineTracker.getDocument().getLineOffset(4) + 3; //debug info
	// } catch (BadLocationException e) {
	// assertTrue("failed getting offset of line", false);
	// }
	// color= getColorAtOffset(offset, ConsoleLineTracker.getDocument());
	// assertNotNull("No color found at " + offset, color);
	// assertEquals(AntUIPlugin.getPreferenceColor(IAntUIPreferenceConstants.CONSOLE_DEBUG_COLOR),
	// color);
	//
	// try {
	// offset= ConsoleLineTracker.getDocument().getLineOffset(33) + 10; //echo line
	// 33
	// } catch (BadLocationException e) {
	// assertTrue("failed getting offset of line", false);
	// }
	// color= getColorAtOffset(offset, ConsoleLineTracker.getDocument());
	// assertNotNull("No color found at " + offset, color);
	// assertEquals(AntUIPlugin.getPreferenceColor(IAntUIPreferenceConstants.CONSOLE_WARNING_COLOR),
	// color);
	// }
}
