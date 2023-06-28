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


import java.io.IOException;

/**
 * A streams proxy acts as proxy between the streams of a
 * process and interested clients. This abstraction allows
 * implementations of <code>IProcess</code> to handle I/O related
 * to the standard input, output, and error streams associated
 * with a process.
 * <p>
 * Clients implementing the <code>IProcess</code> interface must also
 * provide an implementation of this interface.
 * </p>
 * @see IProcess
 * @see IBinaryStreamsProxy
 */
public interface IStreamsProxy {
	/**
	 * Returns a monitor for the error stream of this proxy's process,
	 * or <code>null</code> if not supported.
	 * The monitor is connected to the error stream of the
	 * associated process.
	 *
	 * @return an error stream monitor, or <code>null</code> if none
	 */
	IStreamMonitor getErrorStreamMonitor();
	/**
	 * Returns a monitor for the output stream of this proxy's process,
	 * or <code>null</code> if not supported.
	 * The monitor is connected to the output stream of the
	 * associated process.
	 *
	 * @return an output stream monitor, or <code>null</code> if none
	 */
	IStreamMonitor getOutputStreamMonitor();
	/**
	 * Writes the given text to the output stream connected to the
	 * standard input stream of this proxy's process.
	 *
	 * @param input the text to be written
	 * @exception IOException when an error occurs writing to the
	 *		underlying <code>OutputStream</code>.
	 *
	 */
	void write(String input) throws IOException;
}
