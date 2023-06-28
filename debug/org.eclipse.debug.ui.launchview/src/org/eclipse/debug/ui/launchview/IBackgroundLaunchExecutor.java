/*******************************************************************************
 * Copyright (c) 2021 SSI Schaefer IT Solutions GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SSI Schaefer IT Solutions GmbH
 *******************************************************************************/
package org.eclipse.debug.ui.launchview;

import java.io.File;

import org.eclipse.debug.core.ILaunchConfiguration;

/**
 * Allows to execute a specified launch configuration in the background, i.e.
 * non-blocking.
 * <p>
 * The launch configuration is started as a background job.
 *
 * @since 1.0.2
 */
public interface IBackgroundLaunchExecutor {

	/**
	 * Starts a launch configuration. The return value is only valid if wait is
	 * <code>true</code>. Otherwise the launch is not awaited and the method
	 * returns immediately.
	 *
	 * @param launchConf the launch configuration
	 * @param mode the launch mode to use.
	 * @param build whether to perform a build before launch
	 * @param wait whether to wait for completion
	 * @param logFile an optional {@link File} to write console output to. May
	 *            be <code>null</code>.
	 * @return process exit value if wait is <code>true</code>, always 0 if wait
	 *         is <code>false</code>. -1 in case waiting was interrupted.
	 */
	int launchProcess(ILaunchConfiguration launchConf, String mode, boolean build, boolean wait, File logFile);

}