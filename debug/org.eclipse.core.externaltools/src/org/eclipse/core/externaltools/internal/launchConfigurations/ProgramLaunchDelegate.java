/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
 *     Keith Seitz (keiths@redhat.com) - environment variables contribution (Bug 27243)
 *     dakshinamurthy.karra@gmail.com - bug 165371
 *******************************************************************************/
package org.eclipse.core.externaltools.internal.launchConfigurations;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.externaltools.internal.IExternalToolConstants;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.RefreshUtil;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.osgi.util.NLS;

/**
 * Launch delegate for a program.
 */
public class ProgramLaunchDelegate extends LaunchConfigurationDelegate {

	/**
	 * Launch configuration attribute - a boolean value indicating whether a
	 * configuration should be launched in the background. Default value is <code>true</code>.
	 * <p>
	 * This constant is defined in org.eclipse.debug.ui, but has to be copied here to support
	 * headless launching.
	 * </p>
	 */
	private static final String ATTR_LAUNCH_IN_BACKGROUND = "org.eclipse.debug.ui.ATTR_LAUNCH_IN_BACKGROUND"; //$NON-NLS-1$

	/**
	 * @see org.eclipse.debug.core.model.ILaunchConfigurationDelegate#launch(org.eclipse.debug.core.ILaunchConfiguration,
	 *      java.lang.String, org.eclipse.debug.core.ILaunch,
	 *      org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void launch(ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {

		if (monitor.isCanceled()) {
			return;
		}

		// resolve location
		IPath location = ExternalToolsCoreUtil.getLocation(configuration);

		if (monitor.isCanceled()) {
			return;
		}

		// resolve working directory
		IPath workingDirectory = ExternalToolsCoreUtil
				.getWorkingDirectory(configuration);

		if (monitor.isCanceled()) {
			return;
		}

		String[] cmdLine = buildCommandLine(configuration, location);

		if (monitor.isCanceled()) {
			return;
		}

		File workingDir = null;
		if (workingDirectory != null) {
			workingDir = workingDirectory.toFile();
		}

		if (monitor.isCanceled()) {
			return;
		}

		String[] envp = DebugPlugin.getDefault().getLaunchManager()
				.getEnvironment(configuration);

		if (monitor.isCanceled()) {
			return;
		}

		boolean mergeOutput = configuration.getAttribute(DebugPlugin.ATTR_MERGE_OUTPUT, false);
		Process p = DebugPlugin.exec(cmdLine, workingDir, envp, mergeOutput);
		IProcess process = null;

		// add process type to process attributes
		Map<String, String> processAttributes = new HashMap<>();
		String programName = location.lastSegment();
		String extension = location.getFileExtension();
		if (extension != null) {
			programName = programName.substring(0, programName.length()
					- (extension.length() + 1));
		}
		programName = programName.toLowerCase();
		processAttributes.put(IProcess.ATTR_PROCESS_TYPE, programName);

		if (p != null) {
			monitor.beginTask(NLS.bind(
					ExternalToolsProgramMessages.ProgramLaunchDelegate_3,
					new String[] { configuration.getName() }),
					IProgressMonitor.UNKNOWN);
			String label = getProcessLabel(location, p);
			process = DebugPlugin.newProcess(launch, p, label, processAttributes);
		}
		if (p == null || process == null) {
			if (p != null) {
				p.destroy();
			}
			throw new CoreException(new Status(IStatus.ERROR,
					IExternalToolConstants.PLUGIN_ID,
					IExternalToolConstants.ERR_INTERNAL_ERROR,
					ExternalToolsProgramMessages.ProgramLaunchDelegate_4, null));
		}
		process.setAttribute(IProcess.ATTR_CMDLINE, generateCommandLine(cmdLine));
		process.setAttribute(DebugPlugin.ATTR_LAUNCH_TIMESTAMP, Long.toString(System.currentTimeMillis()));

		if (configuration.getAttribute(ATTR_LAUNCH_IN_BACKGROUND, true)) {
			// refresh resources after process finishes
			String scope = configuration.getAttribute(RefreshUtil.ATTR_REFRESH_SCOPE, (String)null);
			if (scope != null) {
				BackgroundResourceRefresher refresher = new BackgroundResourceRefresher(configuration, process);
				refresher.startBackgroundRefresh();
			}
		} else {
			// wait for process to exit
			while (!process.isTerminated()) {
				try {
					if (monitor.isCanceled()) {
						process.terminate();
						break;
					}
					Thread.sleep(50);
				} catch (InterruptedException e) {
				}
			}

			// refresh resources
			RefreshUtil.refreshResources(configuration, monitor);
		}
	}

	private String getProcessLabel(IPath location, Process p) {
		String label = location.toOSString();
		try {
			label += " " + NLS.bind(ExternalToolsProgramMessages.ProgramLaunchDelegate_5, new Object[] { //$NON-NLS-1$
					p.pid() });
		} catch (UnsupportedOperationException e) {
			// ignore, pid() is not implemented in this JVM
		}
		return label;
	}

	private String[] buildCommandLine(ILaunchConfiguration configuration, IPath location) throws CoreException {
		// resolve arguments
		String[] arguments = ExternalToolsCoreUtil.getArguments(configuration);

		int cmdLineLength = 1;
		if (arguments != null) {
			cmdLineLength += arguments.length;
		}
		String[] cmdLine = new String[cmdLineLength];
		cmdLine[0] = location.toOSString();
		if (arguments != null) {
			System.arraycopy(arguments, 0, cmdLine, 1, arguments.length);
		}
		return cmdLine;
	}

	private String generateCommandLine(String[] commandLine) {
		if (commandLine.length < 1) {
			return IExternalToolConstants.EMPTY_STRING;
		}
		StringBuilder buf = new StringBuilder();
		for (String c : commandLine) {
			if (buf.length() > 0) {
				buf.append(' ');
			}
			StringBuilder command = new StringBuilder();
			boolean containsSpace = false;
			for (char character : c.toCharArray()) {
				if (character == '\"') {
					command.append('\\');
				} else if (character == ' ') {
					containsSpace = true;
				}
				command.append(character);
			}
			if (containsSpace) {
				buf.append('\"');
				buf.append(command);
				buf.append('\"');
			} else {
				buf.append(command);
			}
		}
		return buf.toString();
	}

	@Override
	protected IProject[] getBuildOrder(ILaunchConfiguration configuration,
			String mode) throws CoreException {
		IProject[] projects = ExternalToolsCoreUtil.getBuildProjects(
				configuration, null);
		if (projects == null) {
			return null;
		}
		boolean isRef = ExternalToolsCoreUtil.isIncludeReferencedProjects(
				configuration, null);
		if (isRef) {
			return computeReferencedBuildOrder(projects);
		}
		return computeBuildOrder(projects);
	}

	@Override
	protected boolean saveBeforeLaunch(ILaunchConfiguration configuration,
			String mode, IProgressMonitor monitor) throws CoreException {
		if (IExternalToolConstants.ID_EXTERNAL_TOOLS_BUILDER_LAUNCH_CATEGORY
				.equals(configuration.getType().getCategory())) {
			// don't prompt for builders
			return true;
		}
		return super.saveBeforeLaunch(configuration, mode, monitor);
	}

	@Override
	public String showCommandLine(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		IPath location = ExternalToolsCoreUtil.getLocation(configuration);
		String[] cmd = buildCommandLine(configuration, location);
		String cmdLine = generateCommandLine(cmd);
		return cmdLine;
	}
}
