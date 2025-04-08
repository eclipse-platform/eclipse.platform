/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.core;

import java.io.File;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.runtime.CoreException;

/**
 * A {@link ExecFactory} can be used to control how Eclipse forks a new
 * {@link Process}.
 *
 * @since 3.23
 */
public interface ExecFactory {

	/**
	 * Executes the given command with the provided working directory and
	 * environment
	 *
	 * @param cmdLine the commandline to execute
	 * @param workingDirectory an optional working directory to be used
	 *            otherwise the process factory must use its default
	 * @param environment the environment to use, if empty the process factory
	 *            must use its defaults
	 * @param mergeOutput <code>true</code> if standard error and standard out
	 *            should be merged
	 * @return an {@link Optional} describing the new process created, if an
	 *         empty {@link Optional} is returned it is assumed that this
	 *         factory is not capable of executing the provided command with the
	 *         requested settings
	 * @throws CoreException if the factory is capable of execution but the
	 *             creation of a process itself has failed
	 */
	Optional<Process> exec(String[] cmdLine, Optional<File> workingDirectory, Optional<Map<String, String>> environment, boolean mergeOutput) throws CoreException;

}
