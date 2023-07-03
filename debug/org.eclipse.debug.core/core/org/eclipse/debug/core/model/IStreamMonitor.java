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
package org.eclipse.debug.core.model;


import org.eclipse.debug.core.IStreamListener;

/**
 * A stream monitor manages the contents of the stream a process
 * is writing to, and notifies registered listeners of changes in
 * the stream.
 * <p>
 * Clients may implement this interface. Generally, a client that
 * provides an implementation of the <code>IStreamsProxy</code>
 * interface must also provide an implementation of this interface.
 * </p>
 * @see org.eclipse.debug.core.model.IStreamsProxy
 * @see org.eclipse.debug.core.model.IFlushableStreamMonitor
 * @see org.eclipse.debug.core.model.IBinaryStreamMonitor
 */
public interface IStreamMonitor {
	/**
	 * Adds the given listener to this stream monitor's registered listeners.
	 * Has no effect if an identical listener is already registered.
	 *
	 * @param listener the listener to add
	 */
	void addListener(IStreamListener listener);
	/**
	 * Returns the entire current contents of the stream. An empty
	 * String is returned if the stream is empty.
	 *
	 * @return the stream contents as a <code>String</code>
	 */
	String getContents();
	/**
	 * Removes the given listener from this stream monitor's registered listeners.
	 * Has no effect if the listener is not already registered.
	 *
	 * @param listener the listener to remove
	 */
	void removeListener(IStreamListener listener);
}
