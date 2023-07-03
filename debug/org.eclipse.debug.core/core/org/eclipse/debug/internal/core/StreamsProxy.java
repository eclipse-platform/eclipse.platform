/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
package org.eclipse.debug.internal.core;


import java.io.IOException;
import java.nio.charset.Charset;

import org.eclipse.debug.core.model.IBinaryStreamMonitor;
import org.eclipse.debug.core.model.IBinaryStreamsProxy;
import org.eclipse.debug.core.model.IStreamMonitor;
import org.eclipse.debug.core.model.IStreamsProxy;
import org.eclipse.debug.core.model.IStreamsProxy2;

/**
 * Standard implementation of a streams proxy for {@link IStreamsProxy},
 * {@link IStreamsProxy2} and {@link IBinaryStreamsProxy}.
 * <p>
 * Will use the same monitor instances for binary and string stream handling.
 */
public class StreamsProxy implements IBinaryStreamsProxy {
	/**
	 * The monitor for the output stream (connected to standard out of the process)
	 */
	private OutputStreamMonitor fOutputMonitor;
	/**
	 * The monitor for the error stream (connected to standard error of the process)
	 */
	private OutputStreamMonitor fErrorMonitor;
	/**
	 * The monitor for the input stream (connected to standard in of the process)
	 */
	private InputStreamMonitor fInputMonitor;
	/**
	 * Records the open/closed state of communications with
	 * the underlying streams.  Note: fClosed is initialized to
	 * <code>false</code> by default.
	 */
	private boolean fClosed;

	/**
	 * Creates a <code>StreamsProxy</code> on the streams of the given system
	 * process.
	 *
	 * @param process system process to create a streams proxy on
	 * @param charset the process's charset or <code>null</code> if default
	 * @param suffix Thread name suffix
	 */
	@SuppressWarnings("resource")
	public StreamsProxy(Process process, Charset charset, String suffix) {
		if (process == null) {
			return;
		}
		fOutputMonitor = new OutputStreamMonitor(process.getInputStream(), charset);
		fErrorMonitor = new OutputStreamMonitor(process.getErrorStream(), charset);
		fInputMonitor = new InputStreamMonitor(process.getOutputStream(), charset);
		fOutputMonitor.startMonitoring("Output Stream Monitor" + suffix); //$NON-NLS-1$
		fErrorMonitor.startMonitoring("Error Stream Monitor" + suffix); //$NON-NLS-1$
		fInputMonitor.startMonitoring("Input Stream Monitor" + suffix); //$NON-NLS-1$
	}

	/**
	 * Creates a <code>StreamsProxy</code> on the streams of the given system
	 * process.
	 *
	 * @param process system process to create a streams proxy on
	 * @param encoding the process's encoding or <code>null</code> if default
	 * @deprecated use {@link #StreamsProxy(Process, Charset)} instead
	 */
	@Deprecated
	public StreamsProxy(Process process, String encoding) {
		// This constructor was once removed in favor of the Charset variant
		// but Bug 562653 brought up a client which use this internal class via
		// reflection and breaks without this constructor. So we restored the
		// old constructor for the time being.
		this(process, Charset.forName(encoding), ""); //$NON-NLS-1$
	}

	/**
	 * Causes the proxy to close all communications between it and the
	 * underlying streams after all remaining data in the streams is read.
	 */
	public void close() {
		if (!isClosed(true)) {
			fOutputMonitor.close();
			fErrorMonitor.close();
			fInputMonitor.close();
		}
	}

	/**
	 * Returns whether the proxy is currently closed.  This method
	 * synchronizes access to the <code>fClosed</code> flag.
	 *
	 * @param setClosed If <code>true</code> this method will also set the
	 * <code>fClosed</code> flag to true.  Otherwise, the <code>fClosed</code>
	 * flag is not modified.
	 * @return Returns whether the stream proxy was already closed.
	 */
	private synchronized boolean isClosed(boolean setClosed) {
		boolean closed = fClosed;
		if (setClosed) {
			fClosed = true;
		}
		return closed;
	}

	/**
	 * Causes the proxy to close all
	 * communications between it and the
	 * underlying streams immediately.
	 * Data remaining in the streams is lost.
	 */
	public void kill() {
		synchronized (this) {
			fClosed= true;
		}
		fOutputMonitor.kill();
		fErrorMonitor.kill();
		fInputMonitor.close();
	}

	@Override
	public IStreamMonitor getErrorStreamMonitor() {
		return fErrorMonitor;
	}

	@Override
	public IStreamMonitor getOutputStreamMonitor() {
		return fOutputMonitor;
	}

	@Override
	public void write(String input) throws IOException {
		if (!isClosed(false)) {
			fInputMonitor.write(input);
		} else {
			throw new IOException();
		}
	}

	@Override
	public void closeInputStream() throws IOException {
		if (!isClosed(false)) {
			fInputMonitor.closeInputStream();
		} else {
			throw new IOException();
		}

	}

	@Override
	public IBinaryStreamMonitor getBinaryErrorStreamMonitor() {
		return fErrorMonitor;
	}

	@Override
	public IBinaryStreamMonitor getBinaryOutputStreamMonitor() {
		return fOutputMonitor;
	}

	@Override
	public void write(byte[] data, int offset, int length) throws IOException {
		if (!isClosed(false)) {
			fInputMonitor.write(data, offset, length);
		} else {
			throw new IOException();
		}
	}
}
