/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.team.core.subscribers;
import org.eclipse.core.resources.IResource;

/**
 * A change event that describes a change in a resource that is or was
 * supervised by a subscriber.
 * <p>
 *
 * @see ISubscriberChangeListener
 * @since 3.0
 * @noimplement Clients are not intended to implement. Instead subclass
 *              {@link SubscriberChangeEvent}.
 */
public interface ISubscriberChangeEvent {
	/*====================================================================
	 * Constants defining the kinds of team changes to resources:
	 *====================================================================*/
	/**
	 * Delta kind constant indicating that the resource has not been changed in any way
	 * @see org.eclipse.core.resources.IResourceDelta#getKind()
	 */
	public static final int NO_CHANGE = 0;
	/**
	 * Delta kind constant (bit mask) indicating that the synchronization state of a resource has changed.
	 * @see #getFlags
	 */
	public static final int SYNC_CHANGED = 0x1;
	/**
	 * Delta kind constant (bit mask) indicating that a team provider has been configured on the resource.
	 * @see  #getFlags
	 */
	public static final int ROOT_ADDED = 0x2;
	/**
	 * Delta kind constant (bit mask) indicating that a team provider has been de-configured on the resource.
	 * @see #getFlags
	 */
	public static final int ROOT_REMOVED = 0x4;

	/**
	 * Return the flags that describe the type of change. The returned value should
	 * be ANDed with the change type flags to determine whether the change event is
	 * of a particular type. For example,
	 *
	 * <pre>
	 * if (event.getFlags() &amp; ISubscriberChangeEvent.SYNC_CHANGED) {
	 * 	// the sync info for the resource has changed
	 * }
	 * </pre>
	 *
	 * @return the flags that describe the type of change
	 */
	public abstract int getFlags();

	/**
	 * Return the resource whose state with
	 * respect to the subscriber has changed.
	 * @return the resource whose state with
	 * respect to the subscriber has changed
	 */
	public abstract IResource getResource();

	/**
	 * Return the subscriber to which this change event applies.
	 * @return the subscriber to which this change event applies
	 */
	public abstract Subscriber getSubscriber();
}
