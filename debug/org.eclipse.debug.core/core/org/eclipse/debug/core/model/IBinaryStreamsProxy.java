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

import java.io.IOException;

/**
 * A variant of {@link IStreamsProxy} which does not touch the proxied content
 * and pass it as bytes instead of strings.
 * <p>
 * A streams proxy acts as proxy between the streams of a process and interested
 * clients. This abstraction allows implementations of <code>IProcess</code> to
 * handle I/O related to the standard input, output, and error streams
 * associated with a process.
 * </p>
 * <p>
 * Clients implementing the <code>IProcess</code> interface for a process which
 * produce or consumes binary content should consider to implement this
 * interface instead of just {@link IStreamsProxy}.
 * </p>
 *
 * @see IProcess
 * @see IStreamsProxy
 * @since 3.16
 */
public interface IBinaryStreamsProxy extends IStreamsProxy2 {
	/**
	 * Returns a monitor for the error stream of this proxy's process, or
	 * <code>null</code> if not supported.
	 * <p>
	 * The monitor is connected to the error stream of the associated process.
	 * </p>
	 * <p>
	 * In contrast to {@link #getErrorStreamMonitor()} which will decode the
	 * stream content to strings, the {@link IBinaryStreamMonitor} will provide
	 * the raw stream data.
	 * </p>
	 *
	 * @return an error stream monitor, or <code>null</code> if none
	 */
	IBinaryStreamMonitor getBinaryErrorStreamMonitor();

	/**
	 * Returns a monitor for the output stream of this proxy's process, or
	 * <code>null</code> if not supported.
	 * <p>
	 * The monitor is connected to the output stream of the associated process.
	 * </p>
	 * <p>
	 * In contrast to {@link #getOutputStreamMonitor()} which will decode the
	 * stream content to strings, the {@link IBinaryStreamMonitor} will provide
	 * the raw stream data.
	 * </p>
	 *
	 * @return an output stream monitor, or <code>null</code> if none
	 */
	IBinaryStreamMonitor getBinaryOutputStreamMonitor();

	/**
	 * Writes the given data to the output stream connected to the standard
	 * input stream of this proxy's process.
	 *
	 * @param data the data to be written
	 * @exception IOException when an error occurs writing to the underlying
	 *                <code>OutputStream</code>.
	 */
	default void write(byte[] data) throws IOException {
		write(data, 0, data.length);
	}

	/**
	 * Writes the given data to the output stream connected to the standard
	 * input stream of this proxy's process.
	 *
	 * @param data the data to be written
	 * @param offset start offset in the data
	 * @param length number of bytes to write
	 * @exception IOException when an error occurs writing to the underlying
	 *                <code>OutputStream</code>.
	 */
	void write(byte[] data, int offset, int length) throws IOException;
}
