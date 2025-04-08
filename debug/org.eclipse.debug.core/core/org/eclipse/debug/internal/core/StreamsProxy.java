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
import java.io.InputStream;
import java.io.OutputStream;
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
	private final OutputStreamMonitor fOutputMonitor;
	/**
	 * The monitor for the error stream (connected to standard error of the process)
	 */
	private final OutputStreamMonitor fErrorMonitor;
	/**
	 * The monitor for the input stream (connected to standard in of the process)
	 */
	private final InputStreamMonitor fInputMonitor;
	/**
	 * Records the open/closed state of communications with
	 * the underlying streams.  Note: fClosed is initialized to
	 * <code>false</code> by default.
	 */
	private boolean fClosed;
	private boolean started;

	/**
	 * Creates a <code>StreamsProxy</code> on the streams of the given system
	 * process and starts the monitoring.
	 *
	 * @param process system process to create a streams proxy on
	 * @param charset the process's charset or <code>null</code> if default
	 * @param suffix Thread name suffix
	 */
	public StreamsProxy(Process process, Charset charset, String suffix) {
		this(process, charset);
		startMonitoring(suffix);
	}

	/**
	 * Creates a <code>StreamsProxy</code> on the streams of the given system
	 * process, monitoring must be started separately.
	 *
	 * @param process system process to create a streams proxy on
	 * @param charset the process's charset or <code>null</code> if default
	 */
	@SuppressWarnings("resource")
	public StreamsProxy(Process process, Charset charset) {
		if (process == null) {
			fOutputMonitor = new OutputStreamMonitor(InputStream.nullInputStream(), charset);
			fErrorMonitor = new OutputStreamMonitor(InputStream.nullInputStream(), charset);
			fInputMonitor = new InputStreamMonitor(OutputStream.nullOutputStream(), charset);
		} else {
			fOutputMonitor = new OutputStreamMonitor(process.getInputStream(), charset);
			fErrorMonitor = new OutputStreamMonitor(process.getErrorStream(), charset);
			fInputMonitor = new InputStreamMonitor(process.getOutputStream(), charset);
		}
	}

	/**
	 * Starts the monitoring of streams using the given suffix
	 *
	 * @param suffix
	 */
	public void startMonitoring(String suffix) {
		start();
		fOutputMonitor.startMonitoring("Output Stream Monitor" + suffix); //$NON-NLS-1$
		fErrorMonitor.startMonitoring("Error Stream Monitor" + suffix); //$NON-NLS-1$
		fInputMonitor.startMonitoring("Input Stream Monitor" + suffix); //$NON-NLS-1$
	}

	private synchronized void start() {
		if (started) {
			throw new IllegalStateException("Already started!"); //$NON-NLS-1$
		}
		started = true;
	}

	/**
	 * Creates a <code>StreamsProxy</code> on the streams of the given system
	 * process.
	 *
	 * @param process system process to create a streams proxy on
	 * @param encoding the process's encoding or <code>null</code> if default
	 * @deprecated use {@link #StreamsProxy(Process, Charset, String)} instead
	 */
	@Deprecated(forRemoval = true, since = "2025-06")
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
