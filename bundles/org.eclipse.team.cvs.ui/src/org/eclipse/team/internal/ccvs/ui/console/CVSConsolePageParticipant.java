/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.console;

import org.eclipse.ui.IActionBars;
import org.eclipse.ui.console.*;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsolePageParticipant;
import org.eclipse.ui.part.IPageBookViewPage;

/**
 * Console helper that allows contributing actions to the console view when
 * the CVS console is visible. Added to the console via an extension point
 * from org.eclipse.ui.console.
 * 
 * @since 3.1
 */
public class CVSConsolePageParticipant implements IConsolePageParticipant {

	private ConsoleRemoveAction consoleRemoveAction;
	
	public void init(IPageBookViewPage page, IConsole console) {
		this.consoleRemoveAction = new ConsoleRemoveAction();
		IActionBars bars = page.getSite().getActionBars();
		bars.getToolBarManager().appendToGroup(IConsoleConstants.LAUNCH_GROUP, consoleRemoveAction);
	}

	public void dispose() {
		this.consoleRemoveAction = null;
	}

	public void activated() {
	}

	public void deactivated() {
	}

	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}
}
