/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
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

import org.eclipse.core.runtime.CoreException;

/**
 * Responsible for migrating launch configurations between different versions of Eclipse.
 * A migration delegate is contributed as an optional attribute of a
 * <code>launchConfigurationType</code> extension and is responsible for identifying
 * migration candidates and migrating launch configurations of that type.
 * <p>
 * For example, since 3.2 launch configurations may have resources mapped to them. A migration
 * delegate could assign appropriate resources to a launch configuration create in an earlier
 * version.
 * </p>
 * <p>
 * Clients may implement this interface.
 * </p>
 * @since 3.2
 */
public interface ILaunchConfigurationMigrationDelegate {

	/**
	 * Returns whether the given launch configuration requires migration.
	 *
	 * @param candidate potential migration candidate
	 * @return whether the given launch configuration requires migration
	 * @throws CoreException if an exception occurs determining the status of the
	 *  given configuration
	 */
	boolean isCandidate(ILaunchConfiguration candidate) throws CoreException;

	/**
	 * Migrates the given launch configuration to be compatible with the current tooling.
	 *
	 * @param candidate the candidate to be migrated, which can be a launch configuration
	 *  or working copy
	 * @throws CoreException if an exception occurs during migration
	 */
	void migrate(ILaunchConfiguration candidate) throws CoreException;

}
