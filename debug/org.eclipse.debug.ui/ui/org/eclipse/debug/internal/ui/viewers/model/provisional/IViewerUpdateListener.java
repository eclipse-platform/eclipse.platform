/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
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
 * Notified of viewer updates.
 *
 * @since 3.3
 */
public interface IViewerUpdateListener {

	/**
	 * Notification that a sequence of viewer updates are starting.
	 */
	void viewerUpdatesBegin();

	/**
	 * Notification that viewer updates are complete. Corresponds to
	 * a <code>viewerUpdatesBegin()</code> notification.
	 */
	void viewerUpdatesComplete();

	/**
	 * Notification that a specific update has started within
	 * a sequence of updates.
	 *
	 * @param update update
	 */
	void updateStarted(IViewerUpdate update);

	/**
	 * Notification that a specific update has completed within a
	 * sequence of updates.
	 *
	 * @param update update
	 */
	void updateComplete(IViewerUpdate update);
}
