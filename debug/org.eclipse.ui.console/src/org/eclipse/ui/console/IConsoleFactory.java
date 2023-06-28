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
 * A console factory extension is responsible for opening a console in the
 * console view. Extensions appear on a menu in the console view, and their
 * <code>openConsole</code> method is called when the action is invoked.
 * Implementations may choose to open a new console or activate an existing
 * console. The extension point used to contribute a console factory is
 * <code>org.eclipse.ui.console.consoleFactories</code>.
 * <p>
 * Following is an example console factory extension.
 * </p>
 *
 * <pre>
 * &lt;extension point="org.eclipse.ui.console.consoleFactories"&gt;
 *   &lt;consoleFactory
 *      label="Command Console"
 *      icon="icons\cmd_console.gif"
 *      class="com.example.CommandConsoleFactory"&gt;
 *   &lt;/consoleFactory&gt;
 * &lt;/extension&gt;
 * </pre>
 * <p>
 * An action appears in the console view's 'Open Console' drop-down menu with
 * the corresponding <code>label</code> and optional <code>icon</code>. When the
 * action is invoked, the specified <code>class</code> is instantiated and
 * called to open a console, via the method <code>openConsole()</code>.
 * </p>
 * <p>
 * Clients providing console factory extensions are intended to implement this
 * interface.
 * </p>
 *
 * @since 3.1
 */
public interface IConsoleFactory {
	/**
	 * Opens a console in the console view. Implementations may create a new
	 * console or activate an existing console.
	 */
	void openConsole();

}
