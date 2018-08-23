/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.console;

import org.eclipse.jface.action.Action;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ui.Utils;

/**
 * Action that removed the CVS console from the console view. The console
 * can be re-added via the console view "Open Console" drop-down.
 * 
 * @since 3.1
 */
public class ConsoleRemoveAction extends Action {

	ConsoleRemoveAction() {
		Utils.initAction(this, "ConsoleRemoveAction.", Policy.getActionBundle()); //$NON-NLS-1$
	}
	
	public void run() {
		CVSConsoleFactory.closeConsole();
	}
}
