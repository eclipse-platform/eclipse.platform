/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.viewers.model.provisional;


/**
 * Listener for viewer state updates.
 *
 * @since 3.6
 */
public interface IStateUpdateListener {

	/**
	 * Notification that a sequence of state saving updates are starting.
	 *
	 * @param input Input object for the state operation.
	 */
	void stateSaveUpdatesBegin(Object input);

	/**
	 * Notification that viewer updates are complete. Corresponds to
	 * a <code>viewerUpdatesBegin()</code> notification.
	 *
	 * @param input Input object for the state operation.
	 */
	void stateSaveUpdatesComplete(Object input);

	/**
	 * Notification that a sequence of viewer updates are starting.
	 *
	 * @param input Input object for the state operation.
	 */
	void stateRestoreUpdatesBegin(Object input);

	/**
	 * Notification that viewer updates are complete. Corresponds to
	 * a <code>viewerUpdatesBegin()</code> notification.
	 *
	 * @param input Input object for the state operation.
	 */
	void stateRestoreUpdatesComplete(Object input);

	/**
	 * Notification that a specific update has started within
	 * a sequence of updates.
	 *
	 * @param input Input object for the state operation.
	 * @param update update
	 */
	void stateUpdateStarted(Object input, IViewerUpdate update);

	/**
	 * Notification that a specific update has completed within a
	 * sequence of updates.
	 *
	 * @param input Input object for the state operation.
	 * @param update update
	 */
	void stateUpdateComplete(Object input, IViewerUpdate update);
}
