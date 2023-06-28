/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
 * Enhancements to the launches listener interface. Provides notification
 * when one or more launches terminate.
 * <p>
 * Clients implementing launches listener may implement
 * this interface.
 * </p>
 * @since 3.0
 */
public interface ILaunchesListener2 extends ILaunchesListener {

	/**
	 * Notification that the given launches have terminated.
	 *
	 * @param launches the launches that have terminated
	 */
	void launchesTerminated(ILaunch[] launches);
}
