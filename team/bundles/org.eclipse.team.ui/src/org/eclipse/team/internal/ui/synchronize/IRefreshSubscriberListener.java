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
package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.ui.actions.ActionFactory;

/**
 * Callback interface for clients interested in being notified about the lifecycle of
 * a subscriber refresh operation.
 * <p>
 * Clients may implement this interface.
 * </p>
 * @see IRefreshEvent
 * @since 3.0
 */
public interface IRefreshSubscriberListener {
	/**
	 * Notification that a refresh is about to start. The event contains the resources
	 * that will be refreshed.
	 *
	 * @param event an event describing the state of the refresh.
	 */
	public void refreshStarted(IRefreshEvent event);

	/**
	 * Notification that a refresh has completed. The event contains the changes
	 * found during the refresh as well as the status of the refresh.
	 *
	 * @param event the event describing the result of the refresh.
	 */
	public ActionFactory.IWorkbenchAction refreshDone(IRefreshEvent event);
}
