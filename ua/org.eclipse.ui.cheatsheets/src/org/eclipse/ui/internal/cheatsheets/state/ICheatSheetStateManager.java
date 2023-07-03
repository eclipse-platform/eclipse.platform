/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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

package org.eclipse.ui.internal.cheatsheets.state;

import java.util.Properties;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.ui.internal.cheatsheets.registry.CheatSheetElement;
import org.eclipse.ui.internal.cheatsheets.views.CheatSheetManager;

/**
 * This interface is an abstraction of the state saving functions for a cheat sheet.
 * Depending on how a cheat sheet is opened the state may be opened from
 * a memento passed in, a file whose name is determined from the id of the
 * cheat sheet, or in the case of a tray dialog it is passed using an object.
 */

public interface ICheatSheetStateManager {

	/**
	 * Will be called before any of the functions to get or save state
	 * @param element
	 */
	public void setElement(CheatSheetElement element);

	/**
	 * Load the properties from the state file/memento/memory
	 */
	public Properties getProperties();

	/**
	 * Load the cheat sheet manager from state file/memento/memory
	 */
	public CheatSheetManager getCheatSheetManager();

	/**
	 * Save the properties and the cheat sheet manager
	 * @param properties
	 * @param manager
	 * @return
	 */
	public IStatus saveState(Properties properties, CheatSheetManager manager);

}
