/*******************************************************************************
 *  Copyright (c) 2025 Vector Informatik GmbH and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.ant.tests.ui.testplugin;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.IAntUIPreferenceConstants;
import org.eclipse.ant.tests.ui.debug.TestAgainException;
import org.eclipse.core.externaltools.internal.IExternalToolConstants;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.IHyperlink;
import org.eclipse.ui.internal.console.ConsoleHyperlinkPosition;
import org.eclipse.ui.internal.console.IOConsolePartition;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

@SuppressWarnings("restriction")
public final class AntUITestUtil {

	private AntUITestUtil() {
	}

	/**
	 * Returns the launch configuration for the given build file
	 *
	 * @param buildFileName
	 *            build file to launch
	 */
	public static ILaunchConfiguration getLaunchConfiguration(String buildFileName) {
		IFile file = getJavaProject().getProject().getFolder("launchConfigurations").getFile(buildFileName + ".launch"); //$NON-NLS-1$ //$NON-NLS-2$
		ILaunchConfiguration config = getLaunchManager().getLaunchConfiguration(file);
		assertTrue("Could not find launch configuration for " + buildFileName, config.exists()); //$NON-NLS-1$
		return config;
	}

	/**
	 * Launches the given configuration and waits for an event. Returns the source of the event. If the event is not received, the launch is
	 * terminated and an exception is thrown.
	 *
	 * @param configuration
	 *            the configuration to launch
	 * @param waiter
	 *            the event waiter to use
	 * @return Object the source of the event
	 */
	public static Object launchAndWait(ILaunchConfiguration configuration, DebugEventWaiter waiter)
			throws CoreException {
		ILaunch launch = configuration.launch(ILaunchManager.RUN_MODE, null);
		Object suspendee = waiter.waitForEvent();
		if (suspendee == null) {
			try {
				launch.terminate();
			}
			catch (CoreException e) {
				e.printStackTrace();
			}
			throw new TestAgainException("Retest - Program did not suspend launching: " + configuration.getName()); //$NON-NLS-1$
		}
		boolean terminated = launch.isTerminated();
		assertTrue("launch did not terminate", terminated); //$NON-NLS-1$
		if (terminated && !ConsoleLineTracker.isClosed()) {
			ConsoleLineTracker.waitForConsole();
		}
		assertTrue("Console is not closed", ConsoleLineTracker.isClosed()); //$NON-NLS-1$
		return suspendee;
	}

	/**
	 * Launches the given configuration and waits for the terminated event or the length of the given timeout, whichever comes first
	 */
	public static void launchAndTerminate(ILaunchConfiguration config, int timeout) throws CoreException {
		DebugEventWaiter waiter = new DebugElementKindEventWaiter(DebugEvent.TERMINATE, IProcess.class);
		waiter.setTimeout(timeout);

		Object terminatee = launchAndWait(config, waiter);
		assertTrue("terminatee is not an IProcess", terminatee instanceof IProcess); //$NON-NLS-1$
		IProcess process = (IProcess) terminatee;
		boolean terminated = process.isTerminated();
		assertTrue("process is not terminated", terminated); //$NON-NLS-1$
	}

	/**
	 * Launches the launch configuration Waits for all of the lines to be appended
	 * to the console.
	 *
	 * @param config the config to execute
	 */
	public static void launch(ILaunchConfiguration config) throws CoreException {
		launchAndTerminate(config, 20000);
	}

	/**
	 * Asserts that the testing project has been setup in the test workspace
	 *
	 * @throws Exception
	 *
	 * @since 3.5
	 */
	public static void assertProject() throws Exception {
		IProject pro = ResourcesPlugin.getWorkspace().getRoot().getProject(ProjectHelper.PROJECT_NAME);
		if (!pro.exists()) {
			// create project and import build files and support files
			IProject project = ProjectHelper.createProject(ProjectHelper.PROJECT_NAME);
			IFolder folder = ProjectHelper.addFolder(project, "buildfiles"); //$NON-NLS-1$
			ProjectHelper.addFolder(project, "launchConfigurations"); //$NON-NLS-1$
			File root = getFileInPlugin(ProjectHelper.TEST_BUILDFILES_DIR);
			ProjectHelper.importFilesFromDirectory(root, folder.getFullPath(), null);

			ProjectHelper.createLaunchConfigurationForBoth("echoing"); //$NON-NLS-1$
			ProjectHelper.createLaunchConfigurationForBoth("102282"); //$NON-NLS-1$
			ProjectHelper.createLaunchConfigurationForBoth("74840"); //$NON-NLS-1$
			ProjectHelper.createLaunchConfigurationForBoth("failingTarget"); //$NON-NLS-1$
			ProjectHelper.createLaunchConfiguration("build"); //$NON-NLS-1$
			ProjectHelper.createLaunchConfiguration("bad"); //$NON-NLS-1$
			ProjectHelper.createLaunchConfiguration("importRequiringUserProp"); //$NON-NLS-1$
			ProjectHelper.createLaunchConfigurationForSeparateVM("echoPropertiesSepVM", "echoProperties"); //$NON-NLS-1$ //$NON-NLS-2$
			ProjectHelper.createLaunchConfigurationForSeparateVM("extensionPointSepVM", null); //$NON-NLS-1$
			ProjectHelper.createLaunchConfigurationForSeparateVM("extensionPointTaskSepVM", null); //$NON-NLS-1$
			ProjectHelper.createLaunchConfigurationForSeparateVM("extensionPointTypeSepVM", null); //$NON-NLS-1$
			ProjectHelper.createLaunchConfigurationForSeparateVM("input", null); //$NON-NLS-1$
			ProjectHelper.createLaunchConfigurationForSeparateVM("environmentVar", null); //$NON-NLS-1$

			ProjectHelper.createLaunchConfigurationForBoth("breakpoints"); //$NON-NLS-1$
			ProjectHelper.createLaunchConfigurationForBoth("debugAntCall"); //$NON-NLS-1$
			ProjectHelper.createLaunchConfigurationForBoth("96022"); //$NON-NLS-1$
			ProjectHelper.createLaunchConfigurationForBoth("macrodef"); //$NON-NLS-1$
			ProjectHelper.createLaunchConfigurationForBoth("85769"); //$NON-NLS-1$

			ProjectHelper.createLaunchConfiguration("big", ProjectHelper.PROJECT_NAME + "/buildfiles/performance/build.xml"); //$NON-NLS-1$ //$NON-NLS-2$

			// do not show the Ant build failed error dialog
			AntUIPlugin.getDefault().getPreferenceStore().setValue(IAntUIPreferenceConstants.ANT_ERROR_DIALOG, false);
		}
	}

	private static File getFileInPlugin(IPath path) {
		try {
			Bundle bundle = FrameworkUtil.getBundle(AntUITestUtil.class);
			URL installURL = bundle.getEntry("/" + path.toString()); //$NON-NLS-1$
			URL localURL = FileLocator.toFileURL(installURL);
			return new File(localURL.getFile());
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * Returns the 'AntUITests' project.
	 *
	 * @return the test project
	 */
	public static IJavaProject getJavaProject() {
		return JavaCore.create(getProject());
	}

	/**
	 * Returns the launch manager
	 *
	 * @return launch manager
	 */
	public static ILaunchManager getLaunchManager() {
		return DebugPlugin.getDefault().getLaunchManager();
	}

	/**
	 * Returns the 'AntUITests' project.
	 *
	 * @return the test project
	 */
	public static IProject getProject() {
		return ResourcesPlugin.getWorkspace().getRoot().getProject(ProjectHelper.PROJECT_NAME);
	}

	/**
	 * Launches the Ant build with the build file name (no extension).
	 *
	 * @param buildFileName
	 *            the build file
	 * @param arguments
	 *            the ant arguments
	 */
	public static void launch(String buildFileName, String arguments) throws CoreException {
		ILaunchConfiguration config = getLaunchConfiguration(buildFileName);
		assertNotNull("Could not locate launch configuration for " + buildFileName, config); //$NON-NLS-1$
		ILaunchConfigurationWorkingCopy copy = config.getWorkingCopy();
		copy.setAttribute(IExternalToolConstants.ATTR_TOOL_ARGUMENTS, arguments);
		launchAndTerminate(copy, 20000);
	}

	/**
	 * Launches the Ant build with the build file name (no extension).
	 *
	 * @param buildFileName
	 *            the ant build file name
	 */
	public static void launch(String buildFileName) throws CoreException {
		ILaunchConfiguration config = getLaunchConfiguration(buildFileName);
		assertNotNull("Could not locate launch configuration for " + buildFileName, config); //$NON-NLS-1$
		launchAndTerminate(config, 20000);
	}

	/**
	 * Returns the {@link IFile} for the given build file name
	 *
	 * @return the associated {@link IFile} for the given build file name
	 */
	public static IFile getIFile(String buildFileName) {
		return getProject().getFolder("buildfiles").getFile(buildFileName); //$NON-NLS-1$
	}

	/**
	 * Returns the {@link File} for the given build file name
	 *
	 * @return the {@link File} for the given build file name
	 */
	public static File getBuildFile(String buildFileName) {
		IFile file = getIFile(buildFileName);
		assertTrue("Could not find build file named: " + buildFileName, file.exists()); //$NON-NLS-1$
		return file.getLocation().toFile();
	}

	/**
	 * Returns the contents of the given {@link BufferedReader} as a {@link String}
	 *
	 * @return the contents of the given {@link BufferedReader} as a {@link String}
	 */
	public static String getReaderContentAsString(BufferedReader bufferedReader) {
		StringBuilder result = new StringBuilder();
		try {
			String line = bufferedReader.readLine();

			while (line != null) {
				if (result.length() != 0) {
					result.append(System.lineSeparator());
				}
				result.append(line);
				line = bufferedReader.readLine();
			}
		}
		catch (IOException e) {
			AntUIPlugin.log(e);
			return null;
		}

		return result.toString();
	}

	/**
	 * Returns the {@link Color} at the given offset on the given document, or
	 * <code>null</code> if there is no {@link Color} at that offset on the
	 * document.
	 *
	 * @return the {@link Color} at the given offset on the given document or
	 *         <code>null</code>
	 */
	public static Color getColorAtOffset(int offset, IDocument document) throws BadLocationException {
		if (document != null) {
			IDocumentPartitioner partitioner = document.getDocumentPartitioner();
			if (partitioner != null) {
				ITypedRegion[] regions = partitioner.computePartitioning(offset,
						document.getLineInformationOfOffset(offset).getLength());
				if (regions.length > 0) {
					IOConsolePartition partition = (IOConsolePartition) regions[0];
					return partition.getColor();
				}
			}
		}
		return null;
	}

	/**
	 * Returns the {@link IHyperlink} at the given offset on the given document, or
	 * <code>null</code> if there is no {@link IHyperlink} at that offset on the
	 * document.
	 *
	 * @return the {@link IHyperlink} at the given offset on the given document or
	 *         <code>null</code>
	 */
	public static IHyperlink getHyperlink(int offset, IDocument doc) {
		if (offset >= 0 && doc != null) {
			Position[] positions = null;
			try {
				positions = doc.getPositions(ConsoleHyperlinkPosition.HYPER_LINK_CATEGORY);
			} catch (BadPositionCategoryException ex) {
				// no links have been added
				return null;
			}
			for (Position position : positions) {
				if (offset >= position.getOffset() && offset <= (position.getOffset() + position.getLength())) {
					return ((ConsoleHyperlinkPosition) position).getHyperLink();
				}
			}
		}
		return null;
	}

	public static void activateLink(final IHyperlink link) {
		Display.getDefault().asyncExec(() -> link.linkActivated());
	}
}
