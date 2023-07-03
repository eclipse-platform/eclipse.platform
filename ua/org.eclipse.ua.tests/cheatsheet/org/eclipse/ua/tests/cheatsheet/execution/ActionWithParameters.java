/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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

package org.eclipse.ua.tests.cheatsheet.execution;


import org.junit.Assert;

import org.eclipse.jface.action.Action;

import org.eclipse.ui.cheatsheets.ICheatSheetAction;
import org.eclipse.ui.cheatsheets.ICheatSheetManager;

/**
 * Action which saves the parameters it was passed. Used for testing
 * parameter passing to action.
 */
public class ActionWithParameters extends Action implements ICheatSheetAction {

	/**
	 * Should never be called
	 */
	@Override
	public void run() {
		Assert.fail("Should not call this version of run");
	}

	@Override
	public void run(String[] params, ICheatSheetManager manager) {
		ActionEnvironment.setParams(params);
		ActionEnvironment.actionCompleted();
	}

}
