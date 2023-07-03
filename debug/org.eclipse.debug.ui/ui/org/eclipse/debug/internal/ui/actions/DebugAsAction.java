/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.actions;


import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.actions.LaunchShortcutsAction;

/**
 * "Debug As" action in the top level "Run" menu.
 */
public class DebugAsAction extends LaunchShortcutsAction {

	public DebugAsAction() {
		super(IDebugUIConstants.ID_DEBUG_LAUNCH_GROUP);
	}

}
