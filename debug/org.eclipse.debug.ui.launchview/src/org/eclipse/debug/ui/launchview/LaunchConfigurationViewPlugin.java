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

import org.eclipse.debug.ui.launchview.internal.launcher.StandaloneLaunchConfigExecutor;

/**
 * Static Helper which provides access to internal implementations of externally
 * available API.
 *
 * @since 1.0.2
 */
public class LaunchConfigurationViewPlugin {

	/**
	 * @return an {@link IBackgroundLaunchExecutor} which can be used to launch
	 *         launch configurations as background jobs.
	 */
	public static IBackgroundLaunchExecutor getExecutor() {
		return new StandaloneLaunchConfigExecutor();
	}

}
