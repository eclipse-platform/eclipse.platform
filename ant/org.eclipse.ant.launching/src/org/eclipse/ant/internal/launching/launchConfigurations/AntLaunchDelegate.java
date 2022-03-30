/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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
 *     Juan A. Hernandez - bug 89926
 *     dakshinamurthy.karra@gmail.com - bug 165371
 *     Ericsson AB, Hamdan Msheik - Bug 389564
 *******************************************************************************/
package org.eclipse.ant.internal.launching.launchConfigurations;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.tools.ant.ProjectHelper;
import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.ant.core.AntCorePreferences;
import org.eclipse.ant.core.AntRunner;
import org.eclipse.ant.core.Property;
import org.eclipse.ant.core.Task;
import org.eclipse.ant.core.Type;
import org.eclipse.ant.internal.core.AbstractEclipseBuildLogger;
import org.eclipse.ant.internal.launching.AntLaunch;
import org.eclipse.ant.internal.launching.AntLaunching;
import org.eclipse.ant.internal.launching.AntLaunchingUtil;
import org.eclipse.ant.internal.launching.debug.IAntDebugConstants;
import org.eclipse.ant.internal.launching.debug.model.RemoteAntDebugBuildListener;
import org.eclipse.ant.launching.IAntLaunchConstants;
import org.eclipse.core.externaltools.internal.IExternalToolConstants;
import org.eclipse.core.externaltools.internal.launchConfigurations.BackgroundResourceRefresher;
import org.eclipse.core.externaltools.internal.launchConfigurations.ExternalToolsCoreUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.RefreshUtil;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.LaunchConfigurationDelegate;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.AbstractVMInstall;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.SocketUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.namespace.HostNamespace;
import org.osgi.framework.wiring.BundleWire;
import org.osgi.framework.wiring.BundleWiring;

/**
 * Launch delegate for Ant builds
 */
public class AntLaunchDelegate extends LaunchConfigurationDelegate {

	private static final String ANT_LOGGER_CLASS = "org.eclipse.ant.internal.launching.runtime.logger.AntProcessBuildLogger"; //$NON-NLS-1$
	private static final String ANT_DEBUG_LOGGER_CLASS = "org.eclipse.ant.internal.launching.runtime.logger.AntProcessDebugBuildLogger"; //$NON-NLS-1$
	private static final String NULL_LOGGER_CLASS = "org.eclipse.ant.internal.launching.runtime.logger.NullBuildLogger"; //$NON-NLS-1$
	private static final String REMOTE_ANT_LOGGER_CLASS = "org.eclipse.ant.internal.launching.remote.logger.RemoteAntBuildLogger"; //$NON-NLS-1$
	private static final String REMOTE_ANT_DEBUG_LOGGER_CLASS = "org.eclipse.ant.internal.launching.remote.logger.RemoteAntDebugBuildLogger"; //$NON-NLS-1$
	private static final String BASE_DIR_PREFIX = "-Dbasedir="; //$NON-NLS-1$
	private static final String INPUT_HANDLER_CLASS = "org.eclipse.ant.internal.ui.antsupport.inputhandler.AntInputHandler"; //$NON-NLS-1$
	private static final String REMOTE_INPUT_HANDLER_CLASS = "org.eclipse.ant.internal.ui.antsupport.inputhandler.ProxyInputHandler"; //$NON-NLS-1$

	/**
	 * String attribute identifying the build scope for a launch configuration. <code>null</code> indicates the default workspace build.
	 *
	 * Note: this attribute was used with the old 'AntBuildTab' which has been replaced by the 'ExternalToolsBuildTab'. The 'ExternalToolsBuildTab'
	 * uses a different attribute key, so use the external tools attribute when present: IExternalToolConstants.ATTR_BUILD_SCOPE
	 */
	private static final String ATTR_BUILD_SCOPE = AntLaunching.getUniqueIdentifier() + ".ATTR_BUILD_SCOPE"; //$NON-NLS-1$

	/**
	 * Attribute identifier specifying whether referenced projects should be considered when computing the projects to build. Default value is
	 * <code>true</code>.
	 *
	 * Note: this attribute was used with the old 'AntBuildTab' which has been replaced by the 'ExternalToolsBuildTab'. The 'ExternalToolsBuildTab'
	 * uses a different attribute key, so use the external tools attribute when present: IExternalToolConstants.ATTR_INCLUDE_REFERENCED_PROJECTS
	 */
	private static final String ATTR_INCLUDE_REFERENCED_PROJECTS = AntLaunching.getUniqueIdentifier() + ".ATTR_INCLUDE_REFERENCED_PROJECTS"; //$NON-NLS-1$

	private static String fgSWTLibraryLocation;

	private String fMode;
	ILaunchManager launchManager;

	private boolean fUserSpecifiedLogger = false;

	@Override
	public void launch(ILaunchConfiguration configuration, String mode, ILaunch launch, IProgressMonitor monitor) throws CoreException {
		String path = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH, new String("")); //$NON-NLS-1$
		String vmver = null;
		if (!path.isEmpty()) {
			IPath jrePath = Path.fromPortableString(path);
			IVMInstall vm = JavaRuntime.getVMInstall(jrePath);
			if (vm instanceof AbstractVMInstall) {
				AbstractVMInstall install = (AbstractVMInstall) vm;
				vmver = install.getJavaVersion();
				// versionToJdkLevel only handles 3 char versions = 1.5, 1.6, 1.9, etc
				if (vmver.length() > 3) {
					vmver = vmver.substring(0, 3);
				}
				// int ver = (int) (CompilerOptions.versionToJdkLevel(vmver) >>> 16);
				if (JavaCore.compareJavaVersions(vmver, JavaCore.VERSION_11) < 0) {
					boolean useDefault = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_DEFAULT_CLASSPATH, true);
					if (useDefault) {
						IStatus status = new Status(IStatus.ERROR, AntLaunching.PLUGIN_ID, 1, AntLaunchConfigurationMessages.AntLaunchDelegate_Launching__0__2, null);
						throw new CoreException(status);
					}
				}
			}
		}
		if (vmver == null) {
			IVMInstall vm = JavaRuntime.getDefaultVMInstall();
			if (vm instanceof AbstractVMInstall) {
				AbstractVMInstall install = (AbstractVMInstall) vm;
				vmver = install.getJavaVersion();
			}

		}
		if (monitor.isCanceled()) {
			return;
		}
		fUserSpecifiedLogger = false;
		fMode = mode;
		launchManager = DebugPlugin.getDefault().getLaunchManager();

		// migrate the config to the new classpath format if required
		AntLaunchingUtil.migrateToNewClasspathFormat(configuration);

		boolean isSeparateJRE = AntLaunchingUtil.isSeparateJREAntBuild(configuration);

		if (AntLaunchingUtil.isLaunchInBackground(configuration)) {
			monitor.beginTask(MessageFormat.format(AntLaunchConfigurationMessages.AntLaunchDelegate_Launching__0__1, new Object[] {
					configuration.getName() }), 10);
		} else {
			monitor.beginTask(MessageFormat.format(AntLaunchConfigurationMessages.AntLaunchDelegate_Running__0__2, new Object[] {
					configuration.getName() }), 100);
		}

		// resolve location
		IPath location = ExternalToolsCoreUtil.getLocation(configuration);
		monitor.worked(1);

		if (monitor.isCanceled()) {
			return;
		}

		if (!isSeparateJRE && AntRunner.isBuildRunning()) {
			IStatus status = new Status(IStatus.ERROR, AntLaunching.PLUGIN_ID, 1, MessageFormat.format(AntLaunchConfigurationMessages.AntLaunchDelegate_Build_In_Progress, new Object[] {
					location.toOSString() }), null);
			throw new CoreException(status);
		}

		// resolve working directory
		IPath workingDirectory = ExternalToolsCoreUtil.getWorkingDirectory(configuration);
		String basedir = null;
		if (workingDirectory != null) {
			basedir = workingDirectory.toOSString();
		}
		monitor.worked(1);

		if (monitor.isCanceled()) {
			return;
		}

		// link the process to its build logger via a timestamp
		long timeStamp = System.currentTimeMillis();
		String idStamp = Long.toString(timeStamp);
		StringBuffer idProperty = new StringBuffer("-D"); //$NON-NLS-1$
		idProperty.append(AbstractEclipseBuildLogger.ANT_PROCESS_ID);
		idProperty.append('=');
		idProperty.append(idStamp);

		// resolve arguments
		String[] arguments = ExternalToolsCoreUtil.getArguments(configuration);

		Map<String, String> userProperties = AntLaunchingUtil.getProperties(configuration);
		if (userProperties != null) {// create a copy so as to not affect the
			// configuration with transient
			// properties
			userProperties = new HashMap<>(userProperties);
		}
		String[] propertyFiles = AntLaunchingUtil.getPropertyFiles(configuration);
		String[] targets = AntLaunchingUtil.getTargetNames(configuration);
		URL[] customClasspath = AntLaunchingUtil.getCustomClasspath(configuration);
		String antHome = AntLaunchingUtil.getAntHome(configuration);

		boolean setInputHandler = true;
		try {
			// check if set specify inputhandler
			setInputHandler = configuration.getAttribute(AntLaunching.SET_INPUTHANDLER, true);
		}
		catch (CoreException ce) {
			AntLaunching.log(ce);
		}

		AntRunner runner = null;
		if (!isSeparateJRE) {
			runner = configureAntRunner(configuration, location, basedir, idProperty, arguments, userProperties, propertyFiles, targets, customClasspath, antHome, setInputHandler);
		}

		monitor.worked(1);

		if (monitor.isCanceled()) {
			return;
		}
		boolean captureOutput = ExternalToolsCoreUtil.getCaptureOutput(configuration);
		int port = -1;
		int requestPort = -1;
		if (isSeparateJRE && captureOutput) {
			if (userProperties == null) {
				userProperties = new HashMap<>();
			}
			port = SocketUtil.findFreePort();
			userProperties.put(AbstractEclipseBuildLogger.ANT_PROCESS_ID, idStamp);
			userProperties.put("eclipse.connect.port", Integer.toString(port)); //$NON-NLS-1$
			if (fMode.equals(ILaunchManager.DEBUG_MODE)) {
				requestPort = SocketUtil.findFreePort();
				userProperties.put("eclipse.connect.request_port", Integer.toString(requestPort)); //$NON-NLS-1$
			}
		}

		StringBuffer commandLine = generateCommandLine(location, arguments, userProperties, propertyFiles, targets, antHome, basedir, isSeparateJRE, captureOutput, setInputHandler, vmver);

		if (isSeparateJRE) {
			monitor.beginTask(MessageFormat.format(AntLaunchConfigurationMessages.AntLaunchDelegate_Launching__0__1, new Object[] {
					configuration.getName() }), 10);
			runInSeparateVM(configuration, launch, monitor, idStamp, antHome, port, requestPort, commandLine, captureOutput, setInputHandler);
		} else {
			runInSameVM(configuration, launch, monitor, location, idStamp, runner, commandLine);
		}

		monitor.done();
	}

	private void runInSameVM(ILaunchConfiguration configuration, ILaunch launch, IProgressMonitor monitor, IPath location, String idStamp, AntRunner runner, StringBuffer commandLine) throws CoreException {
		Map<String, String> attributes = new HashMap<>(2);
		attributes.put(IProcess.ATTR_PROCESS_TYPE, IAntLaunchConstants.ID_ANT_PROCESS_TYPE);
		attributes.put(AbstractEclipseBuildLogger.ANT_PROCESS_ID, idStamp);

		final AntProcess process = new AntProcess(location.toOSString(), launch, attributes);
		setProcessAttributes(process, idStamp, commandLine);
		boolean debug = fMode.equals(ILaunchManager.DEBUG_MODE);
		if (debug || AntLaunchingUtil.isLaunchInBackground(configuration)) {
			final AntRunner finalRunner = runner;
			Runnable r = () -> {
				try {
					finalRunner.run(process);
				}
				catch (CoreException e) {
					handleException(e, AntLaunchConfigurationMessages.AntLaunchDelegate_Failure);
				}
				process.terminated();
			};
			Thread background = new Thread(r);
			background.setDaemon(true);
			background.start();
			monitor.worked(1);
			// refresh resources after process finishes
			if (configuration.getAttribute(RefreshUtil.ATTR_REFRESH_SCOPE, (String) null) != null) {
				BackgroundResourceRefresher refresher = new BackgroundResourceRefresher(configuration, process);
				refresher.startBackgroundRefresh();
			}
		} else {
			// execute the build
			try {
				process.setProgressMonitor(monitor);
				runner.run(monitor);
			}
			catch (CoreException e) {
				process.terminated();
				monitor.done();
				handleException(e, AntLaunchConfigurationMessages.AntLaunchDelegate_23);
				return;
			}
			process.terminated();

			// refresh resources
			RefreshUtil.refreshResources(configuration, monitor);
		}
	}

	private AntRunner configureAntRunner(ILaunchConfiguration configuration, IPath location, String baseDir, StringBuffer idProperty, String[] arguments, Map<String, String> userProperties, String[] propertyFiles, String[] targets, URL[] customClasspath, String antHome, boolean setInputHandler) throws CoreException {
		int argLength = 1; // at least one user property - timestamp
		if (arguments != null) {
			argLength += arguments.length;
		}
		if (baseDir != null && baseDir.length() > 0) {
			argLength++;
		}
		String[] runnerArgs = new String[argLength];
		if (arguments != null) {
			System.arraycopy(arguments, 0, runnerArgs, 0, arguments.length);
		}
		if (baseDir != null && baseDir.length() > 0) {
			runnerArgs[runnerArgs.length - 2] = BASE_DIR_PREFIX + baseDir;
		}
		runnerArgs[runnerArgs.length - 1] = idProperty.toString();

		AntRunner runner = new AntRunner();
		runner.setBuildFileLocation(location.toOSString());
		boolean captureOutput = ExternalToolsCoreUtil.getCaptureOutput(configuration);
		if (captureOutput) {
			if (fMode.equals(ILaunchManager.DEBUG_MODE)) {
				runner.addBuildLogger(ANT_DEBUG_LOGGER_CLASS);
			} else {
				runner.addBuildLogger(ANT_LOGGER_CLASS);
			}
		} else {
			runner.addBuildLogger(NULL_LOGGER_CLASS);
		}
		if (setInputHandler) {
			runner.setInputHandler(INPUT_HANDLER_CLASS);
		} else {
			runner.setInputHandler(""); //$NON-NLS-1$
		}
		runner.setArguments(runnerArgs);
		if (userProperties != null) {
			runner.addUserProperties(userProperties);
		}

		if (propertyFiles != null) {
			runner.setPropertyFiles(propertyFiles);
		}

		if (targets != null) {
			runner.setExecutionTargets(targets);
		}

		if (customClasspath != null) {
			runner.setCustomClasspath(customClasspath);
		}

		if (antHome != null) {
			runner.setAntHome(antHome);
		}
		return runner;
	}

	private void handleException(final CoreException e, final String title) {
		AntLaunching.log(title, e);
	}

	private void setProcessAttributes(IProcess process, String idStamp, StringBuffer commandLine) {
		// link the process to the Eclipse build logger via a timestamp
		if (!fUserSpecifiedLogger) {
			process.setAttribute(AbstractEclipseBuildLogger.ANT_PROCESS_ID, idStamp);
		}

		// create "fake" command line for the process
		if (commandLine != null) {
			process.setAttribute(IProcess.ATTR_CMDLINE, commandLine.toString());
		}
	}

	private StringBuffer generateCommandLine(IPath location, String[] arguments, Map<String, String> userProperties, String[] propertyFiles, String[] targets, String antHome, String basedir, boolean separateVM, boolean captureOutput, boolean setInputHandler, String vmver) {
		StringBuffer commandLine = new StringBuffer();

		if (!separateVM) {
			commandLine.append("ant"); //$NON-NLS-1$
		}

		if (arguments != null) {
			commandLine.append(' ');
			commandLine.append(DebugPlugin.renderArguments(arguments, null));
		}

		AntCorePreferences prefs = AntCorePlugin.getPlugin().getPreferences();
		if (propertyFiles == null) { // global
			for (String path : prefs.getCustomPropertyFiles()) {
				commandLine.append(" -propertyfile \""); //$NON-NLS-1$
				commandLine.append(path);
				commandLine.append('\"');
			}
		} else {// "local" configuration
			for (String path : propertyFiles) {
				commandLine.append(" -propertyfile \""); //$NON-NLS-1$
				commandLine.append(path);
				commandLine.append('\"');
			}
		}
		// "local" configuration
		if (userProperties != null) {
			Iterator<String> keys = userProperties.keySet().iterator();
			String key;
			while (keys.hasNext()) {
				key = keys.next();
				appendProperty(commandLine, key, userProperties.get(key));
			}
		}

		// global
		List<Property> properties = null;
		if (!separateVM) {
			properties = prefs.getProperties();
		} else {
			properties = prefs.getRemoteAntProperties();
		}

		// if we have user properties this means that the user has chosen to
		// override the global properties
		// if in a separate VM and have only two (or three if debug) user
		// properties these are really only Eclipse generated properties
		// and the user is still using the global properties
		int numberOfEclipseProperties = 2;
		if (userProperties != null && userProperties.get("eclipse.connect.request_port") != null) { //$NON-NLS-1$
			numberOfEclipseProperties = 3; // debug mode
		}
		boolean useGlobalProperties = userProperties == null || (separateVM && userProperties.size() == numberOfEclipseProperties);
		if (useGlobalProperties) {
			for (Property property : properties) {
				String key = property.getName();
				String value = property.getValue(false);
				if (value != null) {
					appendProperty(commandLine, key, value);
				}
			}
		}

		if (basedir != null && basedir.length() > 0) {
			appendProperty(commandLine, "basedir", basedir); //$NON-NLS-1$
		}

		if (antHome != null) {
			commandLine.append(" \"-Dant.home="); //$NON-NLS-1$
			commandLine.append(antHome);
			commandLine.append('\"');
		}
		if (vmver != null && JavaCore.compareJavaVersions(vmver, JavaCore.VERSION_17) >= 0
				&& commandLine.indexOf("-Djava.security.manager=allow") == -1) { //$NON-NLS-1$
			commandLine.append(" \"-Djava.security.manager=allow\""); //$NON-NLS-1$
		}

		if (separateVM) {
			if (commandLine.indexOf("-logger") == -1) { //$NON-NLS-1$
				if (captureOutput) {
					commandLine.append(" -logger "); //$NON-NLS-1$
					if (fMode.equals(ILaunchManager.DEBUG_MODE)) {
						commandLine.append(REMOTE_ANT_DEBUG_LOGGER_CLASS);
					} else {
						commandLine.append(REMOTE_ANT_LOGGER_CLASS);
					}
				}
			} else {
				fUserSpecifiedLogger = true;
			}
			if (commandLine.indexOf("-inputhandler") == -1 && setInputHandler) { //$NON-NLS-1$
				commandLine.append(" -inputhandler "); //$NON-NLS-1$
				commandLine.append(REMOTE_INPUT_HANDLER_CLASS);
			}
		} else {
			if (commandLine.indexOf("-inputhandler") == -1 && setInputHandler) { //$NON-NLS-1$
				commandLine.append(" -inputhandler "); //$NON-NLS-1$
				commandLine.append(INPUT_HANDLER_CLASS);
			}
			if (commandLine.indexOf("-logger") == -1) { //$NON-NLS-1$
				commandLine.append(" -logger "); //$NON-NLS-1$
				if (fMode.equals(ILaunchManager.DEBUG_MODE)) {
					commandLine.append(ANT_DEBUG_LOGGER_CLASS);
				} else if (captureOutput) {
					commandLine.append(ANT_LOGGER_CLASS);
				} else {
					commandLine.append(NULL_LOGGER_CLASS);
				}
			}
		}

		if (separateVM) {
			appendTaskAndTypes(prefs, commandLine);
		}
		commandLine.append(" -buildfile \""); //$NON-NLS-1$
		commandLine.append(location.toOSString());
		commandLine.append('\"');

		if (targets != null) {
			for (String target : targets) {
				commandLine.append(" \""); //$NON-NLS-1$
				commandLine.append(target);
				commandLine.append('\"');
			}
		}
		return commandLine;
	}

	private void appendTaskAndTypes(AntCorePreferences prefs, StringBuffer commandLine) {
		List<Task> tasks = prefs.getRemoteTasks();
		for (Task task : tasks) {
			commandLine.append(" -eclipseTask "); //$NON-NLS-1$
			String name = ProjectHelper.genComponentName(task.getURI(), task.getTaskName());
			commandLine.append(name);
			commandLine.append(',');
			commandLine.append(task.getClassName());
		}

		List<Type> types = prefs.getRemoteTypes();
		for (Type type : types) {
			commandLine.append(" -eclipseType "); //$NON-NLS-1$
			String name = ProjectHelper.genComponentName(type.getURI(), type.getTypeName());
			commandLine.append(name);
			commandLine.append(',');
			commandLine.append(type.getClassName());
		}
	}

	private void appendProperty(StringBuffer commandLine, String name, String value) {
		commandLine.append(" \"-D"); //$NON-NLS-1$
		commandLine.append(name);
		commandLine.append('=');
		commandLine.append(value);
		if (value.length() > 0 && value.charAt(value.length() - 1) == File.separatorChar) {
			commandLine.append(File.separatorChar);
		}
		commandLine.append("\""); //$NON-NLS-1$
	}

	private void runInSeparateVM(ILaunchConfiguration configuration, ILaunch launch, IProgressMonitor monitor, String idStamp, String antHome, int port, int requestPort, StringBuffer commandLine, boolean captureOutput, boolean setInputHandler) throws CoreException {
		boolean debug = fMode.equals(ILaunchManager.DEBUG_MODE);
		if (captureOutput) {
			String encoding = DebugPlugin.getDefault().getLaunchManager().getEncoding(configuration);
			if (debug) {
				RemoteAntDebugBuildListener listener = new RemoteAntDebugBuildListener(launch, encoding);
				if (requestPort != -1) {
					listener.startListening(port, requestPort);
				}
			} else if (!fUserSpecifiedLogger) {
				RemoteAntBuildListener client = new RemoteAntBuildListener(launch, encoding);
				if (port != -1) {
					client.startListening(port);
				}
			}
		}

		ILaunchConfigurationWorkingCopy copy = configuration.getWorkingCopy();
		setDefaultWorkingDirectory(copy);
		copy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS, commandLine.toString());
		copy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, IAntLaunchConstants.MAIN_TYPE_NAME);
		StringBuffer vmArgs = generateVMArguments(copy, setInputHandler, antHome);
		copy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, vmArgs.toString());
		copy.setAttribute(ILaunchManager.ATTR_PRIVATE, true);
		if (copy.getAttribute(IAntLaunchConstants.ATTR_DEFAULT_VM_INSTALL, false)) {
			setDefaultVM(configuration, copy);
		}

		copy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_USE_START_ON_FIRST_THREAD, false);

		if (debug) { // do not allow launch in foreground bug 83254
			copy.setAttribute(IExternalToolConstants.ATTR_LAUNCH_IN_BACKGROUND, true);
		}

		// set the ANT_HOME environment variable
		if (antHome != null) {
			Map<String, String> vars = copy.getAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, new HashMap<>(1));
			vars.put("ANT_HOME", antHome); //$NON-NLS-1$
			copy.setAttribute(ILaunchManager.ATTR_ENVIRONMENT_VARIABLES, vars);
		}
		// copy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS,
		// "-Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000");
		IProgressMonitor subMonitor = SubMonitor.convert(monitor, 10);
		AntJavaLaunchDelegate delegate = new AntJavaLaunchDelegate();
		delegate.preLaunchCheck(copy, ILaunchManager.RUN_MODE, subMonitor);
		delegate.launch(copy, ILaunchManager.RUN_MODE, launch, subMonitor);
		final IProcess[] processes = launch.getProcesses();
		for (IProcess process : processes) {
			setProcessAttributes(process, idStamp, null);
		}

		if (AntLaunchingUtil.isLaunchInBackground(copy)) {
			// refresh resources after process finishes
			if (configuration.getAttribute(RefreshUtil.ATTR_REFRESH_SCOPE, (String) null) != null) {
				BackgroundResourceRefresher refresher = new BackgroundResourceRefresher(configuration, processes[0]);
				refresher.startBackgroundRefresh();
			}
		} else {
			final AtomicBoolean terminated = new AtomicBoolean(false);
			IDebugEventSetListener listener = events -> {
				for (DebugEvent event : events) {
					for (IProcess process : processes) {
						if (event.getSource() == process && event.getKind() == DebugEvent.TERMINATE) {
							terminated.set(true);
							break;
						}
					}
				}
			};
			DebugPlugin.getDefault().addDebugEventListener(listener);
			terminated.compareAndSet(false, launch.isTerminated());
			monitor.subTask(AntLaunchConfigurationMessages.AntLaunchDelegate_28);
			while (!monitor.isCanceled() && !terminated.get()) {
				try {
					Thread.sleep(50);
				}
				catch (InterruptedException e) {
					// do nothing
				}
			}
			DebugPlugin.getDefault().removeDebugEventListener(listener);
			if (!monitor.isCanceled()) {
				// refresh resources
				RefreshUtil.refreshResources(configuration, monitor);
			}
		}
	}

	@SuppressWarnings("deprecation")
	private void setDefaultVM(ILaunchConfiguration configuration, ILaunchConfigurationWorkingCopy copy) {
		try {
			JavaRuntime.getJavaProject(configuration);
			// remove the vm name, install type and jre container path for the
			// Java launching concept of default VM
			copy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_NAME, (String) null);
			copy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_TYPE, (String) null);
			copy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_JRE_CONTAINER_PATH, (String) null);
		}
		catch (CoreException ce) {
			// not in a Java project
			IVMInstall defaultVMInstall = JavaRuntime.getDefaultVMInstall();
			copy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_NAME, defaultVMInstall.getName());
			copy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_INSTALL_TYPE, defaultVMInstall.getVMInstallType().getId());
		}
	}

	private StringBuffer generateVMArguments(ILaunchConfiguration config, boolean setInputHandler, String antHome) {
		StringBuffer vmArgs = new StringBuffer();
		try {
			String configArgs = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, (String) null);
			if (configArgs != null) {
				vmArgs.append(configArgs);
				vmArgs.append(' ');
			}
		}
		catch (CoreException e) {
			// do nothing
		}

		if (antHome != null) {
			vmArgs.append("-Dant.home=\""); //$NON-NLS-1$
			vmArgs.append(antHome);
			vmArgs.append("\" "); //$NON-NLS-1$

			File antLibDir = new File(antHome, "lib"); //$NON-NLS-1$
			vmArgs.append("-Dant.library.dir=\""); //$NON-NLS-1$
			vmArgs.append(antLibDir.getAbsolutePath());
			vmArgs.append('\"');
		}
		if (setInputHandler) {
			String swtLocation = getSWTLibraryLocation();
			if (swtLocation != null) {
				vmArgs.append(" -Djava.library.path=\""); //$NON-NLS-1$
				String javaLibPath = System.getProperty("java.library.path"); //$NON-NLS-1$
				javaLibPath = stripUnescapedQuotes(javaLibPath);
				if (javaLibPath != null) {
					vmArgs.append(javaLibPath);
					if (vmArgs.charAt(vmArgs.length() - 1) != File.pathSeparatorChar) {
						vmArgs.append(File.pathSeparatorChar);
					}
				}
				vmArgs.append(swtLocation);
				vmArgs.append('"');
			}
		}
		return vmArgs;
	}

	private String stripUnescapedQuotes(String javaLibPath) {
		StringBuilder buf = new StringBuilder(javaLibPath.length());
		for (int i = 0; i < javaLibPath.length(); i++) {
			char c = javaLibPath.charAt(i);
			switch (c) {
				case '"':
					if (i != 0 && javaLibPath.charAt(i - 1) == '\\') {
						buf.append(c);
					}
					break;
				default:
					buf.append(c);
					break;
			}
		}
		return buf.toString();
	}

	@Override
	protected IProject[] getBuildOrder(ILaunchConfiguration configuration, String mode) throws CoreException {
		String scopeKey = ATTR_BUILD_SCOPE;
		String refKey = ATTR_INCLUDE_REFERENCED_PROJECTS;
		if (configuration.hasAttribute(IExternalToolConstants.ATTR_BUILD_SCOPE)
				|| configuration.hasAttribute(IExternalToolConstants.ATTR_INCLUDE_REFERENCED_PROJECTS)) {
			// use new attributes when present - see bug 282581
			scopeKey = IExternalToolConstants.ATTR_BUILD_SCOPE;
			refKey = IExternalToolConstants.ATTR_INCLUDE_REFERENCED_PROJECTS;
		}
		IProject[] projects = ExternalToolsCoreUtil.getBuildProjects(configuration, scopeKey);
		if (projects == null) {
			return null;
		}
		boolean isRef = ExternalToolsCoreUtil.isIncludeReferencedProjects(configuration, refKey);
		if (isRef) {
			return computeReferencedBuildOrder(projects);
		}
		return computeBuildOrder(projects);
	}

	private String getSWTLibraryLocation() {
		if (fgSWTLibraryLocation == null) {
			Bundle bundle = Platform.getBundle("org.eclipse.swt"); //$NON-NLS-1$
			BundleWiring wiring = bundle.adapt(BundleWiring.class);
			List<BundleWire> fragmentWires = wiring == null ? Collections.<BundleWire> emptyList()
					: wiring.getProvidedWires(HostNamespace.HOST_NAMESPACE);
			if (fragmentWires.isEmpty()) {
				return null;
			}
			Bundle fragBundle = fragmentWires.get(0).getRequirer().getBundle();
			try {
				URL url = FileLocator.toFileURL(fragBundle.getEntry("/")); //$NON-NLS-1$
				try {
					IPath path = new Path(URIUtil.toURL(URIUtil.toURI(url)).getPath());
					path = path.removeTrailingSeparator();
					fgSWTLibraryLocation = path.toOSString();
				}
				catch (URISyntaxException e) {
					AntLaunching.log(e);
				}
			}
			catch (IOException e) {
				// do nothing
			}
		}
		return fgSWTLibraryLocation;
	}

	@Override
	protected IBreakpoint[] getBreakpoints(ILaunchConfiguration configuration) {
		IBreakpointManager breakpointManager = DebugPlugin.getDefault().getBreakpointManager();
		if (!breakpointManager.isEnabled()) {
			// no need to check breakpoints individually.
			return null;
		}
		return breakpointManager.getBreakpoints(IAntDebugConstants.ID_ANT_DEBUG_MODEL);
	}

	@Override
	protected boolean saveBeforeLaunch(ILaunchConfiguration configuration, String mode, IProgressMonitor monitor) throws CoreException {
		if (IExternalToolConstants.ID_EXTERNAL_TOOLS_BUILDER_LAUNCH_CATEGORY.equals(configuration.getType().getCategory())) {
			// don't prompt for builders
			return true;
		}
		return super.saveBeforeLaunch(configuration, mode, monitor);
	}

	/**
	 * Sets the default working directory to be the parent folder of the buildfile if the user has not explicitly set the working directory.
	 */
	private void setDefaultWorkingDirectory(ILaunchConfigurationWorkingCopy copy) {
		try {
			String wd = copy.getAttribute(IJavaLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY, (String) null);
			if (wd == null) {
				wd = ExternalToolsCoreUtil.getLocation(copy).removeLastSegments(1).toOSString();
				copy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_WORKING_DIRECTORY, wd);
			}
		}
		catch (CoreException e) {
			AntLaunching.log(e.getStatus());
		}
	}

	@Override
	public ILaunch getLaunch(ILaunchConfiguration configuration, String mode) throws CoreException {
		return new AntLaunch(configuration, mode, null);
	}
}
