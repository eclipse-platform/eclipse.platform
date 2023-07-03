/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.debug.ui;

import org.eclipse.jface.operation.IRunnableContext;


 /**
 * A launch configuration dialog is used to edit and launch
 * launch configurations. It contains a launch configuration
 * tab group.
 * @see ILaunchConfigurationTabGroup
 * @see ILaunchConfigurationTab
 * @since 2.0
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */

public interface ILaunchConfigurationDialog extends IRunnableContext {

	/**
	 * Return value from <code>open()</code> method of a
	 * launch configuration dialog when a launch completed
	 * successfully with a single click (without opening a
	 * launch configuration dialog).
	 *
	 * @deprecated the launch dialog no longer supports launching without
	 * 	opening - this constant will never be returned by the dialog
	 */
	@Deprecated int LAUNCHED_BEFORE_OPENING = 2;

	/**
	 * Adjusts the enable state of this dialog's buttons
	 * to reflect the state of the active tab group.
	 * <p>
	 * This may be called by to force a button state
	 * update.
	 * </p>
	 */
	void updateButtons();

	/**
	 * Updates the message (or error message) shown in the message line to
	 * reflect the state of the currently active tab in this launch
	 * configuration dialog.
	 * <p>
	 * This method may be called to force a message
	 * update.
	 * </p>
	 */
	void updateMessage();

	/**
	 * Sets the contents of the name field to the given name.
	 *
	 * @param name new name value
	 */
	void setName(String name);

	/**
	 * Returns a unique launch configuration name, using the given name
	 * as a seed.
	 *
	 * @param name seed from which to generate a new unique name
	 * @return the new unique launch configuration name
	 */
	String generateName(String name);

	/**
	 * Returns the tabs currently being displayed, or
	 * <code>null</code> if none.
	 *
	 * @return currently displayed tabs, or <code>null</code>
	 */
	ILaunchConfigurationTab[] getTabs();

	/**
	 * Returns the currently active <code>ILaunchConfigurationTab</code>
	 * being displayed, or <code>null</code> if there is none.
	 *
	 * @return currently active <code>ILaunchConfigurationTab</code>, or <code>null</code>.
	 */
	ILaunchConfigurationTab getActiveTab();

	/**
	 * Returns the mode in which this dialog was opened -
	 * run or debug.
	 *
	 * @return one of <code>RUN_MODE</code> or <code>DEBUG_MODE</code> defined in <code>ILaunchManager</code>
	 * @see org.eclipse.debug.core.ILaunchManager
	 */
	String getMode();

	/**
	 * Sets the displayed tab to the given tab. Has no effect if the specified
	 * tab is not one of the tabs being displayed in the dialog currently.
	 *
	 * @param tab the tab to display/activate
	 * @since 2.1
	 */
	void setActiveTab(ILaunchConfigurationTab tab);

	/**
	 * Sets the displayed tab to the tab with the given index. Has no effect if
	 * the specified index is not within the limits of the tabs returned by
	 * <code>getTabs()</code>.
	 *
	 * @param index the index of the tab to display
	 * @since 2.1
	 */
	void setActiveTab(int index);

}
