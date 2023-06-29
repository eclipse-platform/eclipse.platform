/*******************************************************************************
 * Copyright (c) 2020 Paul Pazderski and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Paul Pazderski - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.core.model;

import org.eclipse.debug.core.IBinaryStreamListener;

/**
 * A variant of {@link IStreamMonitor} which does not touch the received content
 * and pass it as bytes instead of strings.
 * <p>
 * A stream monitor manages the contents of the stream a process is writing to,
 * and notifies registered listeners of changes in the stream.
 * </p>
 * <p>
 * Clients may implement this interface. Generally, a client that provides an
 * implementation of the {@link IBinaryStreamsProxy} interface must also provide
 * an implementation of this interface.
 * </p>
 *
 * @see org.eclipse.debug.core.model.IStreamsProxy
 * @see org.eclipse.debug.core.model.IFlushableStreamMonitor
 * @since 3.16
 */
public interface IBinaryStreamMonitor extends IFlushableStreamMonitor {
	/**
	 * Adds the given listener to this stream monitor's registered listeners.
	 * Has no effect if an identical listener is already registered.
	 *
	 * @param listener the listener to add
	 */
	void addBinaryListener(IBinaryStreamListener listener);

	/**
	 * Returns the entire current contents of the stream. An empty array is
	 * returned if the stream is empty.
	 * <p>
	 * Note: the current content is influenced by the buffering mechanism.
	 * </p>
	 *
	 * @return the stream contents as array
	 * @see #isBuffered()
	 * @see #flushContents()
	 */
	byte[] getData();

	/**
	 * Removes the given listener from this stream monitor's registered listeners.
	 * Has no effect if the listener is not already registered.
	 *
	 * @param listener the listener to remove
	 */
	void removeBinaryListener(IBinaryStreamListener listener);
}
