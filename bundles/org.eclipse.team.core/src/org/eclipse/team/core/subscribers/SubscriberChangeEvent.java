/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.core.subscribers;

import org.eclipse.core.resources.IResource;

/**
 * A concrete implementation of <code>ISubscriberChangeEvent</code> that can
 * be used by clients.
 *
 * @see ISubscriberChangeEvent
 * @see Subscriber
 *
 * @since 3.0
 */
public class SubscriberChangeEvent implements ISubscriberChangeEvent {

	private Subscriber subscriber;
	private int flags;
	private IResource resource;

	/**
	 * Create a change event with the given flags for the given subscriber and resource.
	 * @param subscriber the subscriber to which the state change applies
	 * @param flags the flags that describe the change
	 * @param resource the resource whose state has change
	 */
	public SubscriberChangeEvent(Subscriber subscriber, int flags, IResource resource) {
		this.subscriber = subscriber;
		this.flags = flags;
		this.resource = resource;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.ISubscriberChangeEvent#getFlags()
	 */
	public int getFlags() {
		return flags;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.ISubscriberChangeEvent#getResource()
	 */
	public IResource getResource() {
		return resource;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.ISubscriberChangeEvent#getSubscriber()
	 */
	public Subscriber getSubscriber() {
		return subscriber;
	}

	/**
	 * Returns an array of deltas for the resources with <code>ISubscriberChangeEvent.SYNC_CHANGED</code>
	 * as the flag.
	 * @param subscriber the subscriber
	 * @param resources the resources whose sync info has changed
	 * @return an array of change events
	 */
	public static SubscriberChangeEvent[] asSyncChangedDeltas(Subscriber subscriber, IResource[] resources) {
		SubscriberChangeEvent[] deltas = new SubscriberChangeEvent[resources.length];
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			deltas[i] = new SubscriberChangeEvent(subscriber, ISubscriberChangeEvent.SYNC_CHANGED, resource);
		}
		return deltas;
	}
}
