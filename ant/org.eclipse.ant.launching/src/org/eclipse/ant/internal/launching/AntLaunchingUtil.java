/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
package org.eclipse.ant.internal.launching;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.util.FileUtils;
import org.eclipse.ant.internal.core.IAntCoreConstants;
import org.eclipse.ant.internal.launching.launchConfigurations.AntHomeClasspathEntry;
import org.eclipse.ant.internal.launching.launchConfigurations.AntProcess;
import org.eclipse.ant.internal.launching.launchConfigurations.RemoteAntRuntimeProcess;
import org.eclipse.ant.launching.IAntLaunchConstants;
import org.eclipse.core.externaltools.internal.IExternalToolConstants;
import org.eclipse.core.externaltools.internal.model.ExternalToolBuilder;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry2;
import org.eclipse.jdt.launching.JavaRuntime;

/**
 * General utility class dealing with Ant build files
 */
public final class AntLaunchingUtil {
	public static final String ATTRIBUTE_SEPARATOR = ","; //$NON-NLS-1$
	public static final char ANT_CLASSPATH_DELIMITER = '*';
	public static final String ANT_HOME_CLASSPATH_PLACEHOLDER = "G"; //$NON-NLS-1$
	public static final String ANT_GLOBAL_USER_CLASSPATH_PLACEHOLDER = "UG"; //$NON-NLS-1$

	/**
	 * No instances allowed
	 */
	private AntLaunchingUtil() {
		super();
	}

	/**
	 * Returns a single-string of the strings for storage.
	 *
	 * @param strings
	 *            the array of strings
	 * @return a single-string representation of the strings or <code>null</code> if the array is empty.
	 */
	public static String combineStrings(String[] strings) {
		if (strings.length == 0)
			return null;

		if (strings.length == 1)
			return strings[0];

		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < strings.length - 1; i++) {
			buf.append(strings[i]);
			buf.append(ATTRIBUTE_SEPARATOR);
		}
		buf.append(strings[strings.length - 1]);
		return buf.toString();
	}

	/**
	 * Returns an array of targets to be run, or <code>null</code> if none are specified (indicating the default target or implicit target should be
	 * run).
	 *
	 * @param configuration
	 *            launch configuration
	 * @return array of target names, or <code>null</code>
	 * @throws CoreException
	 *             if unable to access the associated attribute
	 */
	public static String[] getTargetNames(ILaunchConfiguration configuration) throws CoreException {
		String attribute = null;
		if (IAntLaunchConstants.ID_ANT_BUILDER_LAUNCH_CONFIGURATION_TYPE.equals(configuration.getType().getIdentifier())) {
			attribute = getTargetNamesForAntBuilder(configuration);
		}
		if (attribute == null) {
			attribute = configuration.getAttribute(IAntLaunchConstants.ATTR_ANT_TARGETS, (String) null);
			if (attribute == null) {
				return null;
			}
		}

		return AntLaunchingUtil.parseRunTargets(attribute);
	}

	private static String getTargetNamesForAntBuilder(ILaunchConfiguration configuration) throws CoreException {
		String buildType = ExternalToolBuilder.getBuildType();
		String targets = null;
		if (IExternalToolConstants.BUILD_TYPE_AUTO.equals(buildType)) {
			targets = configuration.getAttribute(IAntLaunchConstants.ATTR_ANT_AUTO_TARGETS, (String) null);
		} else if (IExternalToolConstants.BUILD_TYPE_CLEAN.equals(buildType)) {
			targets = configuration.getAttribute(IAntLaunchConstants.ATTR_ANT_CLEAN_TARGETS, (String) null);
		} else if (IExternalToolConstants.BUILD_TYPE_FULL.equals(buildType)) {
			targets = configuration.getAttribute(IAntLaunchConstants.ATTR_ANT_AFTER_CLEAN_TARGETS, (String) null);
		} else if (IExternalToolConstants.BUILD_TYPE_INCREMENTAL.equals(buildType)) {
			targets = configuration.getAttribute(IAntLaunchConstants.ATTR_ANT_MANUAL_TARGETS, (String) null);
		}

		return targets;
	}

	/**
	 * Returns a map of properties to be defined for the build, or <code>null</code> if none are specified (indicating no additional properties
	 * specified for the build).
	 *
	 * @param configuration
	 *            launch configuration
	 * @return map of properties (name --&gt; value), or <code>null</code>
	 * @throws CoreException
	 *             if unable to access the associated attribute
	 */
	public static Map<String, String> getProperties(ILaunchConfiguration configuration) throws CoreException {
		// TODO PLATFORM DEBUG 1.5 API
		Map<String, String> map = configuration.getAttribute(IAntLaunchConstants.ATTR_ANT_PROPERTIES, (Map<String, String>) null);
		return map;
	}

	/**
	 * Returns a String specifying the Ant home to use for the build.
	 *
	 * @param configuration
	 *            launch configuration
	 * @return String specifying Ant home to use or <code>null</code>
	 * @throws CoreException
	 *             if unable to access the associated attribute
	 */
	public static String getAntHome(ILaunchConfiguration configuration) throws CoreException {
		IRuntimeClasspathEntry[] entries = JavaRuntime.computeUnresolvedRuntimeClasspath(configuration);
		for (IRuntimeClasspathEntry entry : entries) {
			if (entry.getType() == IRuntimeClasspathEntry.OTHER) {
				IRuntimeClasspathEntry2 entry2 = (IRuntimeClasspathEntry2) entry;
				if (entry2.getTypeId().equals(AntHomeClasspathEntry.TYPE_ID)) {
					return ((AntHomeClasspathEntry) entry2).getAntHome();
				}
			}
		}
		return null;
	}

	/**
	 * Returns an array of property files to be used for the build, or <code>null</code> if none are specified (indicating no additional property
	 * files specified for the build).
	 *
	 * @param configuration
	 *            launch configuration
	 * @return array of property file names, or <code>null</code>
	 * @throws CoreException
	 *             if unable to access the associated attribute
	 */
	public static String[] getPropertyFiles(ILaunchConfiguration configuration) throws CoreException {
		String attribute = configuration.getAttribute(IAntLaunchConstants.ATTR_ANT_PROPERTY_FILES, (String) null);
		if (attribute == null) {
			return null;
		}
		String[] propertyFiles = AntLaunchingUtil.parseString(attribute, ","); //$NON-NLS-1$
		for (int i = 0; i < propertyFiles.length; i++) {
			String propertyFile = propertyFiles[i];
			propertyFile = expandVariableString(propertyFile, AntCoreModelMessages.AntUtil_6);
			propertyFiles[i] = propertyFile;
		}
		return propertyFiles;
	}

	/**
	 * Returns the list of URLs that define the custom classpath for the Ant build, or <code>null</code> if the global classpath is to be used.
	 *
	 * @param config
	 *            launch configuration
	 * @return a list of <code>URL</code>
	 *
	 * @throws CoreException
	 *             if file does not exist, IO problems, or invalid format.
	 */
	public static URL[] getCustomClasspath(ILaunchConfiguration config) throws CoreException {
		boolean useDefault = config.getAttribute(IJavaLaunchConfigurationConstants.ATTR_DEFAULT_CLASSPATH, true);
		if (useDefault) {
			return null;
		}
		IRuntimeClasspathEntry[] unresolved = JavaRuntime.computeUnresolvedRuntimeClasspath(config);
		// don't consider bootpath entries
		List<IRuntimeClasspathEntry> userEntries = new ArrayList<>(unresolved.length);
		for (IRuntimeClasspathEntry entry : unresolved) {
			if (entry.getClasspathProperty() == IRuntimeClasspathEntry.USER_CLASSES) {
				userEntries.add(entry);
			}
		}
		IRuntimeClasspathEntry[] entries = JavaRuntime.resolveRuntimeClasspath(userEntries.toArray(new IRuntimeClasspathEntry[userEntries.size()]), config);
		URL[] urls = new URL[entries.length];
		for (int i = 0; i < entries.length; i++) {
			IRuntimeClasspathEntry entry = entries[i];
			try {
				urls[i] = new URL(IAntCoreConstants.FILE_PROTOCOL + entry.getLocation());
			}
			catch (MalformedURLException e) {
				throw new CoreException(new Status(IStatus.ERROR, AntLaunching.getUniqueIdentifier(), AntLaunching.INTERNAL_ERROR, AntCoreModelMessages.AntUtil_7, e));
			}
		}
		return urls;
	}

	private static String expandVariableString(String variableString, String invalidMessage) throws CoreException {
		String expandedString = VariablesPlugin.getDefault().getStringVariableManager().performStringSubstitution(variableString);
		if (expandedString == null || expandedString.length() == 0) {
			String msg = MessageFormat.format(invalidMessage, new Object[] { variableString });
			throw new CoreException(new Status(IStatus.ERROR, AntLaunching.PLUGIN_ID, 0, msg, null));
		}
		return expandedString;
	}

	/**
	 * Returns the list of target names to run
	 *
	 * @param extraAttibuteValue
	 *            the external tool's extra attribute value for the run targets key.
	 * @return a list of target names
	 */
	public static String[] parseRunTargets(String extraAttibuteValue) {
		return parseString(extraAttibuteValue, ATTRIBUTE_SEPARATOR);
	}

	/**
	 * Returns the list of Strings that were delimiter separated.
	 *
	 * @param delimString
	 *            the String to be tokenized based on the delimiter
	 * @return a list of Strings
	 */
	public static String[] parseString(String delimString, String delim) {
		if (delimString == null) {
			return new String[0];
		}

		// Need to handle case where separator character is
		// actually part of the target name!
		StringTokenizer tokenizer = new StringTokenizer(delimString, delim);
		String[] results = new String[tokenizer.countTokens()];
		for (int i = 0; i < results.length; i++) {
			results[i] = tokenizer.nextToken().trim();
		}

		return results;
	}

	/**
	 * Returns an IFile with the given fully qualified path (relative to the workspace root). The returned IFile may or may not exist.
	 */
	public static IFile getFile(String fullPath) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		return root.getFile(IPath.fromOSString(fullPath));
	}

	/**
	 * Returns the workspace file associated with the given path in the local file system, or <code>null</code> if none. If the path happens to be a
	 * relative path, then the path is interpreted as relative to the specified parent file.
	 *
	 * Attempts to handle linked files; the first found linked file with the correct path is returned.
	 *
	 * @param path
	 * @param buildFileParent
	 * @return file or <code>null</code>
	 * @see org.eclipse.core.resources.IWorkspaceRoot#findFilesForLocation(IPath)
	 */
	public static IFile getFileForLocation(String path, File buildFileParent) {
		if (path == null) {
			return null;
		}
		IPath filePath = IPath.fromOSString(path);
		IFile file = null;
		URI location = filePath.makeAbsolute().toFile().toURI();
		IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(location);
		if (files.length > 0) {
			file = files[0];
		}
		if (file == null) {
			// relative path
			File relativeFile = null;
			try {
				// this call is ok if buildFileParent is null
				relativeFile = FileUtils.getFileUtils().resolveFile(buildFileParent, path);
				filePath = IPath.fromOSString(relativeFile.getAbsolutePath());
				location = filePath.makeAbsolute().toFile().toURI();
				files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(location);
				if (files.length > 0) {
					file = files[0];
				} else {
					return null;
				}
			}
			catch (BuildException be) {
				return null;
			}
		}

		if (file.exists()) {
			return file;
		}
		File ioFile = file.getLocation().toFile();
		if (ioFile.exists()) {// needs to handle case insensitivity on WINOS
			files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(ioFile.toURI());
			if (files.length > 0) {
				return files[0];
			}
		}

		return null;
	}

	/**
	 * Migrates the classpath in the given configuration from the old format to the new format. The old format is not preserved. Instead, the default
	 * classpath will be used. However, ANT_HOME settings are preserved.
	 *
	 * @param configuration
	 *            a configuration to migrate
	 * @throws CoreException
	 *             if unable to migrate
	 * @since 3.0
	 */
	@SuppressWarnings({ "restriction", "deprecation" })
	public static void migrateToNewClasspathFormat(ILaunchConfiguration configuration) throws CoreException {
		String oldClasspath = configuration.getAttribute(AntLaunching.ATTR_ANT_CUSTOM_CLASSPATH, (String) null);
		String oldAntHome = configuration.getAttribute(AntLaunching.ATTR_ANT_HOME, (String) null);
		String provider = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH_PROVIDER, (String) null);
		if (oldClasspath != null || oldAntHome != null || provider == null) {
			ILaunchConfigurationWorkingCopy workingCopy = null;
			if (configuration.isWorkingCopy()) {
				workingCopy = (ILaunchConfigurationWorkingCopy) configuration;
			} else {
				workingCopy = configuration.getWorkingCopy();
			}
			workingCopy.setAttribute(AntLaunching.ATTR_ANT_CUSTOM_CLASSPATH, (String) null);
			workingCopy.setAttribute(AntLaunching.ATTR_ANT_HOME, (String) null);
			workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH_PROVIDER, "org.eclipse.ant.ui.AntClasspathProvider"); //$NON-NLS-1$
			workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_DEFAULT_CLASSPATH, true);
			if (oldAntHome != null) {
				IRuntimeClasspathEntry[] entries = JavaRuntime.computeUnresolvedRuntimeClasspath(workingCopy);
				List<String> mementos = new ArrayList<>(entries.length);
				for (IRuntimeClasspathEntry entry : entries) {
					if (entry.getType() == IRuntimeClasspathEntry.OTHER) {
						IRuntimeClasspathEntry2 entry2 = (IRuntimeClasspathEntry2) entry;
						if (entry2.getTypeId().equals(AntHomeClasspathEntry.TYPE_ID)) {
							AntHomeClasspathEntry homeEntry = new AntHomeClasspathEntry(oldAntHome);
							mementos.add(homeEntry.getMemento());
							continue;
						}
					}
					mementos.add(entry.getMemento());
				}
				workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_DEFAULT_CLASSPATH, false);
				workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH, mementos);
			}
			workingCopy.doSave();
		}
	}

	public static boolean isSeparateJREAntBuild(ILaunchConfiguration configuration) {
		boolean separateJRE = true;
		try {
			// always null for same JRE
			separateJRE = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, (String) null) != null;
		}
		catch (CoreException e) {
			AntLaunching.log(AntCoreModelMessages.AntUtil_2, e);
		}

		return separateJRE;
	}

	public static void linkBuildFailedMessage(String message, IProcess process) {
		String fileName = null;
		String lineNumber = ""; //$NON-NLS-1$
		int fileStart = 0;
		int index = message.indexOf("xml"); //$NON-NLS-1$
		if (index > 0) {
			int numberStart = index + 4;
			int numberEnd = message.indexOf(':', numberStart);
			int fileEnd = index + 3;
			if (numberStart > 0 && fileEnd > 0) {
				fileName = message.substring(fileStart, fileEnd).trim();
				if (numberEnd > 0) {
					lineNumber = message.substring(numberStart, numberEnd).trim();
				}
			}
		}

		if (fileName != null) {
			int num = -1;
			try {
				num = Integer.parseInt(lineNumber);
			}
			catch (NumberFormatException e) {
				// do nothing
			}
			URI location = IPath.fromOSString(fileName).makeAbsolute().toFile().toURI();
			IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(location);
			IFile file = null;
			if (files.length > 0) {
				file = files[0];
			}
			if (file != null && file.exists()) {
				if (process != null) {
					ILaunch launch = null;
					if (process instanceof RemoteAntRuntimeProcess) {
						launch = ((RemoteAntRuntimeProcess) process).getLaunch();
					} else if (process instanceof AntProcess) {
						launch = ((AntProcess) process).getLaunch();
					}
					if (launch != null) {
						((AntLaunch) launch).addLinkDescriptor(message, fileName, num, 0, message.length());
					}
				}
			}
		}
	}

	/**
	 * Returns whether the given configuration should be launched in the background. When unspecified, the default value for an Ant launch
	 * configuration is <code>true</code>.
	 *
	 * @param configuration
	 *            the configuration
	 * @return whether the configuration is configured to launch in the background
	 */
	public static boolean isLaunchInBackground(ILaunchConfiguration configuration) {
		boolean launchInBackground = true;
		try {
			launchInBackground = configuration.getAttribute(IExternalToolConstants.ATTR_LAUNCH_IN_BACKGROUND, true);
		}
		catch (CoreException ce) {
			AntLaunching.log(ce);
		}
		return launchInBackground;
	}
}