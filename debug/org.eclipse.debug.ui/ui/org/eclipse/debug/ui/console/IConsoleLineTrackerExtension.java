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
package org.eclipse.debug.ui.console;

/**
 * An extension to the console line tracker interface that console line
 * trackers may implement to be notified when output from the console is complete.
 * <p>
 * Clients may implement this interface.
 * </p>
 * @since 3.0
 */
public interface IConsoleLineTrackerExtension extends IConsoleLineTracker {

	/**
	 * Notification that all output streams connected to the console have been
	 * closed. No more lines will be appended after this method is called.
	 */
	void consoleClosed();

}
