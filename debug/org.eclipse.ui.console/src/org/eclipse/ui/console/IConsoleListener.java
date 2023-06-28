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
package org.eclipse.ui.console;

/**
 * A console listener is notified when consoles are added or removed from
 * the console manager.
 * <p>
 * Clients may implement this interface.
 * </p>
 * @since 3.0
 */
public interface IConsoleListener {

	/**
	 * Notification the given consoles have been added to the console
	 * manager.
	 *
	 * @param consoles added consoles
	 */
	void consolesAdded(IConsole[] consoles);

	/**
	 * Notification the given consoles have been removed from the
	 * console manager.
	 *
	 * @param consoles removed consoles
	 */
	void consolesRemoved(IConsole[] consoles);

}
