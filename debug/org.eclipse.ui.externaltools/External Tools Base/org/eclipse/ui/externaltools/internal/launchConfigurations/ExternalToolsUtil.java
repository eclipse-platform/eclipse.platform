/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
 *     Keith Seitz (keiths@redhat.com) - Bug 27243 (environment variables contribution)
 *     dakshinamurthy.karra@gmail.com - bug 165371
 *******************************************************************************/
package org.eclipse.ui.externaltools.internal.launchConfigurations;


import org.eclipse.core.externaltools.internal.launchConfigurations.ExternalToolsCoreUtil;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.ui.externaltools.internal.model.ExternalToolsPlugin;

/**
 * Utilities for external tool launch configurations.
 * <p>
 * This class it not intended to be instantiated.
 * </p>
 */
public class ExternalToolsUtil {

	/**
	 * Throws a core exception with an error status object built from
	 * the given message, lower level exception, and error code.
	 *
	 * @param message the status message
	 * @param exception lower level exception associated with the
	 *  error, or <code>null</code> if none
	 * @param code error code
	 */
	protected static void abort(String message, Throwable exception, int code) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, ExternalToolsPlugin.PLUGIN_ID, code, message, exception));
	}

	/**
	 * Expands and returns the location attribute of the given launch
	 * configuration. The location is
	 * verified to point to an existing file, in the local file system.
	 *
	 * @param configuration launch configuration
	 * @return an absolute path to a file in the local file system
	 * @throws CoreException if unable to retrieve the associated launch
	 * configuration attribute, if unable to resolve any variables, or if the
	 * resolved location does not point to an existing file in the local file
	 * system
	 */
	public static IPath getLocation(ILaunchConfiguration configuration) throws CoreException {
		return ExternalToolsCoreUtil.getLocation(configuration);
	}

	/**
	 * Returns a boolean specifying whether or not output should be captured for
	 * the given configuration
	 *
	 * @param configuration the configuration from which the value will be
	 * extracted
	 * @return boolean specifying whether or not output should be captured
	 * @throws CoreException if unable to access the associated attribute
	 */
	public static boolean getCaptureOutput(ILaunchConfiguration configuration) throws CoreException {
		return ExternalToolsCoreUtil.getCaptureOutput(configuration);
	}

	/**
	 * Expands and returns the working directory attribute of the given launch
	 * configuration. Returns <code>null</code> if a working directory is not
	 * specified. If specified, the working is verified to point to an existing
	 * directory in the local file system.
	 *
	 * @param configuration launch configuration
	 * @return an absolute path to a directory in the local file system, or
	 * <code>null</code> if unspecified
	 * @throws CoreException if unable to retrieve the associated launch
	 * configuration attribute, if unable to resolve any variables, or if the
	 * resolved location does not point to an existing directory in the local
	 * file system
	 */
	public static IPath getWorkingDirectory(ILaunchConfiguration configuration) throws CoreException {
		return ExternalToolsCoreUtil.getWorkingDirectory(configuration);
	}

	/**
	 * Expands and returns the arguments attribute of the given launch
	 * configuration. Returns <code>null</code> if arguments are not specified.
	 *
	 * @param configuration launch configuration
	 * @return an array of resolved arguments, or <code>null</code> if
	 * unspecified
	 * @throws CoreException if unable to retrieve the associated launch
	 * configuration attribute, or if unable to resolve any variables
	 */
	public static String[] getArguments(ILaunchConfiguration configuration) throws CoreException {
		return ExternalToolsCoreUtil.getArguments(configuration);
	}

	/**
	 * Returns whether the given launch configuration is enabled. This property
	 * is intended only to apply to external tool builder configurations and
	 * determines whether the project builder will launch the configuration
	 * when it builds.
	 *
	 * @param configuration the configuration for which the enabled state should
	 * 		be determined.
	 * @return whether the given configuration is enabled to be run when a build occurs.
	 * @throws CoreException if unable to access the associated attribute
	 */
	public static boolean isBuilderEnabled(ILaunchConfiguration configuration) throws CoreException {
		return ExternalToolsCoreUtil.isBuilderEnabled(configuration);
	}

	/**
	 * Returns the collection of resources for the build scope as specified by the given launch configuration.
	 *
	 * @param configuration launch configuration
	 * @throws CoreException if an exception occurs while retrieving the resources
	 */
	public static IResource[] getResourcesForBuildScope(ILaunchConfiguration configuration) throws CoreException {
		return ExternalToolsCoreUtil.getResourcesForBuildScope(configuration);
	}

	/**
	 * Parses the argument text into an array of individual
	 * strings using the space character as the delimiter.
	 * An individual argument containing spaces must have a
	 * double quote (") at the start and end. Two double
	 * quotes together is taken to mean an embedded double
	 * quote in the argument text.
	 *
	 * @param arguments the arguments as one string
	 * @return the array of arguments
	 */
	public static String[] parseStringIntoList(String arguments) {
		return ExternalToolsCoreUtil.parseStringIntoList(arguments);
	}

}
