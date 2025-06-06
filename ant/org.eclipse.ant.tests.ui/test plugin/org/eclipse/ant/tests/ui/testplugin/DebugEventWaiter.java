/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.ant.tests.ui.testplugin;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;

/**
 * The <code>DebugEventWaiter</code> is to wait for a specific kind of debug event.
 * <p>
 * When a <code>DebugEventWaiter</code> is created, it registers itself with the <code>DebugPlugin</code> as an <code>IDebugEventSetListener</code>.
 * <p>
 * NOTE: <code>DebugEventWaiter</code> objects are intended for one time use only!
 */
public class DebugEventWaiter implements IDebugEventSetListener {
	/**
	 * The kind of event the waiter is waiting for
	 */
	protected int fEventType;

	/**
	 * The number of milliseconds the waiter will wait before timing out.
	 */
	protected long fTimeout;

	/**
	 * The <code>IDebugModelManager</code> this waiter is listening to.
	 */
	protected DebugPlugin fDebugPlugin;

	/**
	 * The <code>DebugEvent</code> received.
	 */
	protected DebugEvent fEvent;

	/**
	 * The event set that was accepted
	 */
	protected DebugEvent[] fEventSet;

	/**
	 * The default timeout value if none is given (5000).
	 */
	public static final long DEFAULT_TIMEOUT = 5000;

	/**
	 * Creates a new <code>DebugEventWaiter</code> which waits for events of a kind <code>eventType</code>. The wait method will wait the default
	 * timeout value.
	 */
	public DebugEventWaiter(int eventType) {
		fDebugPlugin = DebugPlugin.getDefault();
		fEventType = eventType;
		fTimeout = DEFAULT_TIMEOUT;

		fDebugPlugin.addDebugEventListener(this);
	}

	/**
	 * Answers true if the <code>DebugEvent</code> is acceptable.
	 */
	public boolean accept(DebugEvent event) {
		return event.getKind() == fEventType && event.getDetail() != DebugEvent.EVALUATION_IMPLICIT;
	}

	/**
	 * Answers the event name associated with the given flag.
	 */
	public String getEventName(int flag) {
		switch (flag) {
			case DebugEvent.CREATE:
				return "Create"; //$NON-NLS-1$
			case DebugEvent.TERMINATE:
				return "Terminate"; //$NON-NLS-1$
			case DebugEvent.RESUME:
				return "Resume"; //$NON-NLS-1$
			case DebugEvent.SUSPEND:
				return "Suspend"; //$NON-NLS-1$
			default:
				return "UNKNOWN"; //$NON-NLS-1$
		}
	}

	/**
	 * Handles debug events.
	 *
	 * @see IDebugEventSetListener
	 * @see #accept(DebugEvent)
	 */
	@Override
	public synchronized void handleDebugEvents(DebugEvent[] events) {
		// printReceived(events);
		for (DebugEvent event : events) {
			if (accept(event)) {
				fEvent = event;
				fEventSet = events;
				unregister();
				notifyAll();
				return;
			}
		}
	}

	/**
	 * Prints a message indicating which event was received.
	 */
	protected void printReceived(DebugEvent[] events) {
		for (DebugEvent event : events) {
			System.out.println(this + " got " + event); //$NON-NLS-1$
		}
	}

	/**
	 * Sets the number of milliseconds to wait for this callback
	 */
	public void setTimeout(long milliseconds) {
		fTimeout = milliseconds;
	}

	/**
	 * Unregisters this waiter as a listener
	 */
	public void unregister() {
		fDebugPlugin.removeDebugEventListener(this);
	}

	/**
	 * Returns the source of the accepted event, or <code>null</code> if no event was accepted.
	 */
	public synchronized Object waitForEvent() {
		if (fEvent == null) {
			try {
				wait(fTimeout);
			}
			catch (InterruptedException ie) {
				System.err.println("Interrupted waiting for event"); //$NON-NLS-1$
			}
		}
		unregister();
		if (fEvent == null) {
			return null;
		}
		return fEvent.getSource();
	}

	/**
	 * Returns the accepted event, if any.
	 */
	public DebugEvent getEvent() {
		return fEvent;
	}

	/**
	 * Returns the accepted event set, if any.
	 */
	public DebugEvent[] getEventSet() {
		return fEventSet;
	}
}
