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

/**
 * The default state manager for a cheat sheet. The data will be saved and restored
 * using a file in metadata whose name is derived from the id
 */

package org.eclipse.ui.internal.cheatsheets.state;

import java.util.Properties;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.internal.cheatsheets.registry.CheatSheetElement;
import org.eclipse.ui.internal.cheatsheets.views.CheatSheetManager;

/**
 * A state manager which never saves or restores state. Each cheat sheet
 * opened with this state manager will have initial state
 */

public class NoSaveStateManager implements ICheatSheetStateManager {

	private CheatSheetElement element;
	@Override
	public Properties getProperties() {
		return null;
	}

	@Override
	public CheatSheetManager getCheatSheetManager() {
		CheatSheetManager result = new CheatSheetManager(element);
		return result;
	}

	@Override
	public void setElement(CheatSheetElement element) {
		this.element = element;
	}

	@Override
	public IStatus saveState(Properties properties, CheatSheetManager manager) {
		return Status.OK_STATUS;
	}
}
