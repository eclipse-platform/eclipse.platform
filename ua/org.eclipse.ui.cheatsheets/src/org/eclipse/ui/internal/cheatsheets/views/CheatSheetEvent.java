/*******************************************************************************
 * Copyright (c) 2002, 2015 IBM Corporation and others.
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
package org.eclipse.ui.internal.cheatsheets.views;

import org.eclipse.ui.cheatsheets.ICheatSheetEvent;
import org.eclipse.ui.cheatsheets.ICheatSheetManager;

public class CheatSheetEvent implements ICheatSheetEvent {
	int type;
	String cheatsheetID;
	ICheatSheetManager csm;


	public CheatSheetEvent(int eventType, String id, ICheatSheetManager csm) {
		super();
		this.csm = csm;
		this.type = eventType;
		this.cheatsheetID = id;
	}

	/**
	 * @return
	 */
	@Override
	public int getEventType() {
		return type;
	}

	/**
	 * @return
	 */
	@Override
	public String getCheatSheetID() {
		return cheatsheetID;
	}

	@Override
	public ICheatSheetManager getCheatSheetManager() {
		return csm;
	}

}
