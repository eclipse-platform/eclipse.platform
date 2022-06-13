/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
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
package org.eclipse.e4.core.internal.contexts;

import java.util.Objects;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.contexts.RunAndTrack;

/**
 * An event describing a change to an {@link IEclipseContext}. The following types of events are
 * currently defined:
 * <ul>
 * <li>An event indicating that the listener has just been registered with a context.</li>
 * <li>An after-the-fact report that a context value has been changed (either a new value or a
 * change to an existing value.</li>
 * <li>An after-the-fact report that a context value has been removed.</li>
 * <li>A report that a context is about to be disposed.</li>
 * </ul>
 * <p>
 * In order to handle additional event types that may be introduced in future releases, clients
 * should not write code that presumes the set of event types is closed.
 * </p>
 *
 * @see RunAndTrack
 */
public final class ContextChangeEvent {
	/**
	 * A change event type (value "0"), indicating that the listener receiving this event has just
	 * been registered with a context.
	 *
	 * @see IEclipseContext#runAndTrack(RunAndTrack)
	 */
	public static final int INITIAL = 0;

	/**
	 * A change event type (value "1"), indicating that a context value has been added.
	 */
	public static final int ADDED = 1;

	/**
	 * A change event type (value "2"), indicating that a context value has been removed.
	 */
	public static final int REMOVED = 2;

	/**
	 * A change event type (value "3") indicating that the context is being disposed. The context is
	 * still valid at the time of this event.
	 */
	public static final int DISPOSE = 3;

	/**
	 * A change event type (value "4"), indicating that a context has been un-injected from the
	 * object.
	 */
	public static final int UNINJECTED = 4;

	/**
	 * A change event type (value "5"), indicating that a context needs to process scheduled
	 * updates.
	 */
	public static final int UPDATE = 5;

	/**
	 * A change event type (value "6"), indicating that stored computations depending
	 * on a given name might need to be updated.
	 */
	public static final int RECALC = 6;

	private final Object[] args;
	private final IEclipseContext context;
	private final int eventType;
	private final String key;

	private final Object oldValue;

	/**
	 * Creates a new context event.
	 *
	 * @param context
	 * @param eventType
	 * @param args
	 * @param name
	 */
	ContextChangeEvent(IEclipseContext context, int eventType, Object[] args, String name, Object oldValue) {
		this.context = context;
		this.key = name;
		this.eventType = eventType;
		this.args = args;
		this.oldValue = oldValue;
	}

	/**
	 * Returns the arguments that were supplied when the listener was registered.
	 *
	 * @see IEclipseContext#runAndTrack(RunAndTrack)
	 * @return the arguments that were supplied when the listener was registered.
	 */
	public Object[] getArguments() {
		return args;
	}

	/**
	 * Returns the context where the change occurred.
	 *
	 * @return the context where the change occurred
	 */
	public IEclipseContext getContext() {
		return context;
	}

	/**
	 * Returns the type of content change that occurred.
	 *
	 * @return the type of content change that occurred.
	 */
	public int getEventType() {
		return eventType;
	}

	/**
	 * Returns the name of the context value that changed, or <code>null</code> if not applicable if
	 * this event type.
	 *
	 * @return The name of the changed context value, or <code>null</code>
	 */
	public String getName() {
		return key;
	}

	public Object getOldValue() {
		return oldValue;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		// if ((eventType == DISPOSE) || (eventType == UNINJECTED))
		// result = prime * result + ((context == null) ? 0 : context.hashCode());
		result = prime * result + eventType;
		return prime * result + Objects.hashCode(key);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ContextChangeEvent other = (ContextChangeEvent) obj;

		// if ((eventType == DISPOSE) || (eventType == UNINJECTED)) {
		// if (context == null) {
		// if (other.context != null)
		// return false;
		// } else if (!context.equals(other.context))
		// return false;
		// }
		return this.eventType == other.eventType && Objects.equals(this.key, other.key);
	}

}
