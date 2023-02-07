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
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.internal.cheatsheets.registry.CheatSheetElement;
import org.eclipse.ui.internal.cheatsheets.views.CheatSheetManager;

/**
 * A state manager used to pass cheat sheet state to and from a tray dialog.
 * It does not save any data to a file, it just acts as a conduit between
 * two different CheatSheetViewers
 */

public class TrayStateManager implements ICheatSheetStateManager {

	private Properties properties;
	private CheatSheetManager manager;

	@Override
	public Properties getProperties() {
		return properties;
	}

	@Override
	public void setElement(CheatSheetElement element) {
		// element not used
	}

	@Override
	public CheatSheetManager getCheatSheetManager() {
		return manager;
	}

	/**
	 * Save the properties and cheat sheet manager locally so they can be passed
	 * to and from a cheat sheet viewer in a tray dialog.
	 */
	@Override
	public IStatus saveState(Properties properties, CheatSheetManager manager) {
		this.properties = properties;
		this.manager = manager;
		return Status.OK_STATUS;
	}

}
