/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.debug.core;


/**
 * Notified when a launch configuration is created,
 * deleted, or changed.
 * <p>
 * Clients may implement this interface.
 * </p>
 * @since 2.0
 */
public interface ILaunchConfigurationListener {

	/**
	 * The given launch configuration has been created.
	 *
	 * @param configuration the newly created launch configuration
	 */
	void launchConfigurationAdded(ILaunchConfiguration configuration);

	/**
	 * The given launch configuration has changed in some way.
	 * The configuration may be a working copy.
	 *
	 * @param configuration the launch configuration that has
	 *  changed
	 */
	void launchConfigurationChanged(ILaunchConfiguration configuration);

	/**
	 * The given launch configuration has been deleted.
	 * <p>
	 * The launch configuration no longer exists. Data stored
	 * in the configuration can no longer be accessed, however
	 * handle-only attributes of the launch configuration
	 * can be retrieved.
	 * </p>
	 *
	 * @param configuration the deleted launch configuration
	 */
	void launchConfigurationRemoved(ILaunchConfiguration configuration);
}

