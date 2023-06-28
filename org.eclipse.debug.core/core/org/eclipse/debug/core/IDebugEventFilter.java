/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
package org.eclipse.debug.core;


/**
 * An event filter allows clients to intercept debug events.
 * Event filters are registered with the debug plug-in.
 * <p>
 * Clients may implement this interface.
 * </p>
 * @see DebugPlugin
 * @since 2.0
 */
public interface IDebugEventFilter {

	/**
	 * Filters the given set of debug events, and returns the set of debug
	 * events that should be fired to registered listeners - <code>null</code>
	 * or an empty collection if no debug events should be fired.
	 * <p>
	 * When multiple event filters are registered, events are passed through
	 * all filters. That is, the events returned from the first filter are
	 * passed through the second filter, and so on.
	 * </p>
	 * @param events set of debug events to filter
	 * @return  the set of debug events to fire
	 */
	DebugEvent[] filterDebugEvents(DebugEvent[] events);
}
