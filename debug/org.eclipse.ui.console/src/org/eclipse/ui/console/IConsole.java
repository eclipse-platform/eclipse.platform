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

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.ui.part.IPageBookViewPage;

/**
 * A console. A console is commonly used to display messages such as the output
 * streams of a system process. A console can be displayed in one or more console
 * views.
 * <p>
 * The console implementations provided by this plug-in are textual
 * (<code>TextConsole</code>, <code>MessageConsole</code> and <code>IOConsole</code>).
 * However a client can provide alternate presentations since a console implementation
 * is responsible for providing is page for the page book views in which consoles are
 * displayed.
 * </p>
 * <p>
 * Subclass <code>AbstractConsole</code> when implementing this interface.
 * </p>
 * @since 3.0
 */
public interface IConsole {

	/**
	 * Returns the name of this console.
	 *
	 * @return the name of this console
	 */
	String getName();

	/**
	 * Returns an image descriptor for this console, or <code>null</code>
	 * if none.
	 *
	 * @return an image descriptor for this console, or <code>null</code>
	 *  if none
	 */
	ImageDescriptor getImageDescriptor();

	/**
	 * Creates and returns a new page for this console. The page is displayed
	 * for this console in the console given view.
	 *
	 * @param view the view in which the page is to be created
	 * @return a page book view page representation of this console
	 */
	IPageBookViewPage createPage(IConsoleView view);

	/**
	 * Adds a listener for changes to properties of this console. Has no effect if
	 * an identical listener is already registered.
	 * <p>
	 * The changes supported by the console view are as follows:
	 * </p>
	 * <ul>
	 * <li><code>IBasicPropertyConstants.P_TEXT</code> - indicates the name of a
	 * console has changed</li>
	 * <li><code>IBasicPropertyConstants.P_IMAGE</code> - indicates the image of a
	 * console has changed</li>
	 * </ul>
	 * <p>
	 * Consoles may define additional properties as required.
	 * </p>
	 *
	 * @param listener a property change listener
	 */
	void addPropertyChangeListener(IPropertyChangeListener listener);

	/**
	 * Removes the given property listener from this console page.
	 * Has no effect if an identical listener is not already registered.
	 *
	 * @param listener a property listener
	 */
	void removePropertyChangeListener(IPropertyChangeListener listener);

	/**
	 * Returns a unique identifier for this console's type, or <code>null</code>
	 * if unspecified.
	 *
	 * @return a unique identifier for this console's type, or <code>null</code>
	 * @since 3.1
	 */
	String getType();

}
