/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.commands.actions;

import org.eclipse.debug.internal.ui.views.launch.LaunchView;


/**
 * Default handler for terminate and re-launch. See bug 300810.
 */
public class TerminateAllHandler extends DebugActionHandler {

	public TerminateAllHandler() {
		super(LaunchView.TERMINATE_ALL);
	}


}
