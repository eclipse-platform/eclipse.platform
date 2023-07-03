/*******************************************************************************
 *  Copyright (c) 2004, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.cheatsheets;

/**
 * Event in the life cycle of a cheat sheet.
 * <p>
 * Events over the life time of a running cheat sheet follow this pattern:
 * </p>
 *
 * <pre>
 * opened { started | restored } { restarted | completed }* closed
 * </pre>
 *
 * @see CheatSheetListener
 * @since 3.0
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface ICheatSheetEvent {

	/**
	 * Event type constant (value {@value}) indicating that
	 * the cheat sheet was opened. This is the first event
	 * in the life of a running cheat sheet.
	 */
	public static final int CHEATSHEET_OPENED = 0;

	/**
	 * Event type constant (value {@value}) indicating that
	 * the cheat sheet was closed. This is the last event
	 * in the life of a running cheat sheet.
	 */
	public static final int CHEATSHEET_CLOSED = 1;

	/**
	 * Event type constant (value {@value}) indicating that
	 * the cheat sheet was started.
	 */
	public static final int CHEATSHEET_STARTED = 2;

	/**
	 * Event type constant (value {@value}) indicating that
	 * the cheat sheet was restarted.
	 */
	public static final int CHEATSHEET_RESTARTED = 3;

	/**
	 * Event type constant (value {@value}) indicating that
	 * the cheat sheet has been completed.
	 */
	public static final int CHEATSHEET_COMPLETED = 4;

	/**
	 * Event type constant (value {@value}) indicating that
	 * the cheat sheet has been restored.
	 */
	public static final int CHEATSHEET_RESTORED = 5;

	/**
	 * Returns the type of this cheat sheet event.
	 *
	 * @return the event type code; one of the event type constants
	 * declared on this class
	 */
	public int getEventType();

	/**
	 * Returns the id of the cheat sheet that generated this event.
	 *
	 * @return the cheat sheet id
	 */
	public String getCheatSheetID();

	/**
	 * Returns the cheat sheet manager responsible for executing
	 * the cheat sheet.
	 *
	 * @return the cheat sheet manager
	 */
	public ICheatSheetManager getCheatSheetManager();
}
