/*******************************************************************************
 * Copyright (c) 2006, 2018 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Michael Scharf (Wind River) - initial API and implementation
 * Martin Oberhuber (Wind River) - fixed copyright headers and beautified
 * Michael Scharf (Wind River) - [262996] get rid of TerminalState.OPENED
 *******************************************************************************/
package org.eclipse.terminal.connector;

/**
 * Represent the sate of a terminal connection.
 */
public enum TerminalState {
	/**
	 * The terminal is not connected.
	 */
	CLOSED("CLOSED"), //$NON-NLS-1$

	/**
	 * The terminal is about to connect.
	 */
	CONNECTING("CONNECTING..."), //$NON-NLS-1$

	/**
	 * The terminal is connected.
	 */
	CONNECTED("CONNECTED"); //$NON-NLS-1$

	private final String fState;

	private TerminalState(String state) {
		fState = state;
	}

	@Override
	public String toString() {
		return fState;
	}
}
