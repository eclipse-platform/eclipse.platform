/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.views.launch;

import org.eclipse.debug.internal.ui.commands.actions.DebugActionHandler;

/**
 * Handler for terminate and remove. See bug 290219.
 *
 * @since 3.6
 */
public class TerminateAndRemoveHandler extends DebugActionHandler {

	public TerminateAndRemoveHandler() {
		super(LaunchView.TERMINATE_AND_REMOVE);
	}
}
