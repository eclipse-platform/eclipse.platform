/*******************************************************************************
 * Copyright (c) 2006, 2019 IBM Corporation and others.
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

import java.util.Hashtable;
import java.util.Properties;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.ui.internal.cheatsheets.data.CheatSheetSaveHelper;
import org.eclipse.ui.internal.cheatsheets.data.IParserTags;
import org.eclipse.ui.internal.cheatsheets.registry.CheatSheetElement;
import org.eclipse.ui.internal.cheatsheets.views.CheatSheetManager;

/**
 * The default state manager saves the state in an xml file whose name is the
 * same as the id of the cheatsheet.
 */

public class DefaultStateManager implements ICheatSheetStateManager {

	private CheatSheetSaveHelper saveHelper = new CheatSheetSaveHelper();
	private Properties props;
	private CheatSheetElement element;
	private boolean propertiesRead = false;

	@Override
	public Properties getProperties() {
		if (!propertiesRead) {
			props = saveHelper.loadState(element.getID());
			propertiesRead = true;
		}
		return props;
	}

	@SuppressWarnings("unchecked")
	@Override
	public CheatSheetManager getCheatSheetManager() {
		CheatSheetManager result = new CheatSheetManager(element);
		if (getProperties() != null) {
			result.setData((Hashtable<String, String>) getProperties().get(IParserTags.MANAGERDATA));
		}
		return result;
	}

	@Override
	public void setElement(CheatSheetElement element) {
		this.element = element;
	}

	@Override
	public IStatus saveState(Properties properties, CheatSheetManager manager) {
		return saveHelper.saveState(properties, manager);
	}
}
