/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.team.internal.core.subscribers;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.ITeamStatus;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.Subscriber;

/**
 * Records resource synchronization changes from a Team subscriber. The actual changes
 * are calculated via the SubscriberEventHandler and stored in this input.
 */
public class SyncSetInputFromSubscriber extends SyncSetInput  {

	private Subscriber subscriber;

	public SyncSetInputFromSubscriber(Subscriber subscriber, SubscriberEventHandler handler) {
		super(handler);
		this.subscriber = subscriber;
	}

	@Override
	public void disconnect() {
	}

	public Subscriber getSubscriber() {
		return subscriber;
	}

	@Override
	protected void fetchInput(IProgressMonitor monitor) throws TeamException {
		// don't calculate changes. The SubscriberEventHandler will fetch the
		// input in a job and update this sync set when the changes are
		// calculated.
	}

	/**
	 * Handle an error that occurred while populating the receiver's set.
	 * The <code>ITeamStatus</code> includes the resource for which the
	 * error occurred.
	 * This error is propogated to any set listeners.
	 * @param status the error status
	 */
	public void handleError(ITeamStatus  status) {
		getSyncSet().addError(status);
	}
}
