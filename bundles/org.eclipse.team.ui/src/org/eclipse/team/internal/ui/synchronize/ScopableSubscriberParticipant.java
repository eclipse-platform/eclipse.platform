/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.ui.synchronize.*;

/**
 * Subscriber participant that supports filtering using scopes.
 */
public abstract class ScopableSubscriberParticipant extends SubscriberParticipant {

	/**
	 * No arg constructor used to create workspace scope and for
	 * creation of persisted participant after startup
	 */
	public ScopableSubscriberParticipant() {
	}

	public ScopableSubscriberParticipant(ISynchronizeScope scope) {
		super(scope);
	}


	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.synchronize.subscriber.SubscriberParticipant#setSubscriber(org.eclipse.team.core.subscribers.Subscriber)
	 */
	@Override
	protected void setSubscriber(Subscriber subscriber) {
		super.setSubscriber(subscriber);
		try {
			ISynchronizeParticipantDescriptor descriptor = getDescriptor();
			setInitializationData(descriptor);
		} catch (CoreException e) {
			TeamUIPlugin.log(e);
		}
		if (getSecondaryId() == null) {
			setSecondaryId(Long.toString(System.currentTimeMillis()));
		}
	}

	/**
	 * Return the descriptor for this participant
	 * @return the descriptor for this participant
	 */
	protected abstract ISynchronizeParticipantDescriptor getDescriptor();

}
