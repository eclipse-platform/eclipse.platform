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
package org.eclipse.ui.console;

/**
 * The console manager manages registered consoles.
 * @since 3.0
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IConsoleManager {

	/**
	 * Registers the given listener for console notifications. Has
	 * no effect if an identical listener is already registered.
	 *
	 * @param listener listener to register
	 */
	void addConsoleListener(IConsoleListener listener);

	/**
	 * Unregisters the given listener for console notifications. Has
	 * no effect if an identical listener is not already registered.
	 *
	 * @param listener listener to unregister
	 */
	void removeConsoleListener(IConsoleListener listener);

	/**
	 * Adds the given consoles to the console manager. Has no effect for
	 * equivalent consoles already registered. The consoles will be added
	 * to any existing console views.
	 *
	 * @param consoles consoles to add
	 */
	void addConsoles(IConsole[] consoles);

	/**
	 * Removes the given consoles from the console manager. If the consoles are
	 * being displayed in any console views, the associated pages will be removed
	 * and disposed.
	 *
	 * @param consoles consoles to remove
	 */
	void removeConsoles(IConsole[] consoles);

	/**
	 * Returns a collection of consoles registered with the console manager.
	 *
	 * @return a collection of consoles registered with the console manager
	 */
	IConsole[] getConsoles();

	/**
	 * Opens the console view and displays given the console.
	 * If the view is already open, it is brought to the front unless
	 * the view is pinned on a console other than the given console.
	 * Has no effect if the given console is not currently registered.
	 *
	 * @param console console to display
	 */
	void showConsoleView(IConsole console);

	/**
	 * Warns that the content of the given console has changed in
	 * all console views. Has no effect if the given console is not
	 * currently registered.
	 *
	 * @param console the console that has changed
	 */
	void warnOfContentChange(IConsole console);

	/**
	 * Creates and returns a collection of new pattern match listeners enabled for
	 * the given console. The pattern match listeners are new instances, intended
	 * to be used in a new console. No methods on the participants have been
	 * called. Clients are responsible for connecting to and disconnecting from
	 * the pattern match listeners.
	 * <p>
	 * Console pattern match listeners are contributed via the
	 * <code>org.eclipse.ui.console.consolePatternMatchListeners</code> extension point.
	 * </p>
	 *
	 * @param console the console for which pattern match listeners are requested
	 * @return a collection of new pattern match listeners
	 * @see IPatternMatchListener
	 * @since 3.1
	 */
	IPatternMatchListener[] createPatternMatchListeners(IConsole console);

	/**
	 * Requests a redraw of any visible console page containing the specified console.
	 *
	 * @param console the console to be refreshed
	 * @since 3.1
	 */
	void refresh(IConsole console);

}
