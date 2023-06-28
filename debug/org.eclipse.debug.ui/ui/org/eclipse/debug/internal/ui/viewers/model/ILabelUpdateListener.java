/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.viewers.model;

import org.eclipse.debug.internal.ui.viewers.model.provisional.ILabelUpdate;

/**
 * Notified of label updates.
 *
 * @since 3.3
 */
public interface ILabelUpdateListener {

	/**
	 * Notification that a sequence of viewer updates are starting.
	 */
	void labelUpdatesBegin();

	/**
	 * Notification that viewer updates are complete. Corresponds to
	 * a <code>viewerUpdatesBegin()</code> notification.
	 */
	void labelUpdatesComplete();

	/**
	 * Notification that a specific update has started within
	 * a sequence of updates.
	 *
	 * @param update update
	 */
	void labelUpdateStarted(ILabelUpdate update);

	/**
	 * Notification that a specific update has completed within a
	 * sequence of updates.
	 *
	 * @param update update
	 */
	void labelUpdateComplete(ILabelUpdate update);
}
