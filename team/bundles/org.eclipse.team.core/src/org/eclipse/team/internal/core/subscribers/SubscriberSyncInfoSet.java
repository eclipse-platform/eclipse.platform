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
import org.eclipse.team.core.synchronize.ISyncInfoSetChangeListener;
import org.eclipse.team.core.synchronize.SyncInfoTree;
import org.eclipse.team.internal.core.Policy;

/**
 * The <code>SubscriberSyncInfoSet</code> is a <code>SyncInfoSet</code> that provides the ability to add,
 * remove and change <code>SyncInfo</code> and fires change event notifications to registered listeners.
 * It also provides the ability
 * to batch changes in a single change notification as well as optimizations for sync info retrieval.
 *
 * This class uses synchronized methods and synchronized blocks to protect internal data structures during both access
 * and modify operations and uses an <code>ILock</code> to make modification operations thread-safe. The events
 * are fired while this lock is held so clients responding to these events should not obtain their own internal locks
 * while processing change events.
 *
 * TODO: Override modification methods to enforce use with handler
 *
 */
public class SubscriberSyncInfoSet extends SyncInfoTree {

	protected SubscriberEventHandler handler;

	public SubscriberSyncInfoSet(SubscriberEventHandler handler) {
		this.handler = handler;
	}

	@Override
	public void connect(ISyncInfoSetChangeListener listener, IProgressMonitor monitor) {
		if (handler == null) {
			super.connect(listener, monitor);
		} else {
			connect(listener);
		}
	}

	/**
	 * Variation of connect that does not need progress and does not throw an exception.
	 * Progress is provided by the background event handler and errors are passed through
	 * the chain to the view.
	 * @param listener
	 */
	public void connect(final ISyncInfoSetChangeListener listener) {
		if (handler == null) {
			// Should only use this connect if the set has a handler
			throw new UnsupportedOperationException();
		} else {
			handler.run(monitor -> {
				try {
					beginInput();
					monitor.beginTask(null, 100);
					removeSyncSetChangedListener(listener);
					addSyncSetChangedListener(listener);
					listener.syncInfoSetReset(SubscriberSyncInfoSet.this, Policy.subMonitorFor(monitor, 95));
				} finally {
					endInput(Policy.subMonitorFor(monitor, 5));
					monitor.done();
				}
			}, true /* high priority */);
		}
	}
}
