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
 * A launches listener is notified of launches as they
 * are added and removed from the launch manager. Also,
 * when a process or debug target is added to a launch,
 * listeners are notified of a change.
 * <p>
 * This interface is analogous to {@link ILaunchListener}, except
 * notifications are batched to include more than one launch object
 * when possible.
 * </p>
 * <p>
 * Clients may implement this interface.
 * </p>
 * <p>
 * Clients may also implement the extension interface {@link ILaunchesListener2} for
 * notification of more than one launch being terminated.
 * </p>
 * @see org.eclipse.debug.core.ILaunch
 * @see org.eclipse.debug.core.ILaunchManager
 * @see ILaunchListener
 * @see ILaunchesListener2
 * @since 2.1
 */
public interface ILaunchesListener {
	/**
	 * Notifies this listener that the specified
	 * launches have been removed.
	 *
	 * @param launches the removed launch objects
	 */
	void launchesRemoved(ILaunch[] launches);
	/**
	 * Notifies this listener that the specified launches
	 * have been added.
	 *
	 * @param launches the newly added launch objects
	 */
	void launchesAdded(ILaunch[] launches);
	/**
	 * Notifies this listener that the specified launches
	 * have changed. For example, a process or debug target
	 * has been added to a launch.
	 *
	 * @param launches the changed launch object
	 */
	void launchesChanged(ILaunch[] launches);
}
