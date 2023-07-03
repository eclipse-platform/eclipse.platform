/*****************************************************************
 * Copyright (c) 2010, 2011 Texas Instruments and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Patrick Chuong (Texas Instruments) - Move debug toolbar actions to main window (Bug 332784)
 *****************************************************************/
package org.eclipse.debug.internal.ui.commands.actions;

/**
 * Disconnect action delegate.
 *
 * @since 3.7
 */
public class DisconnectCommandActionDelegate extends DebugCommandActionDelegate {

	public DisconnectCommandActionDelegate() {
		super();
		setAction(new DisconnectCommandAction());
	}
}
