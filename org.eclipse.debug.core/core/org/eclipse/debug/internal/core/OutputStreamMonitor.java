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
 *     Paul Pazderski  - Bug 545769: fixed rare UTF-8 character corruption bug
 *     Paul Pazderski  - Bug 558463: add handling of raw stream content instead of strings
 *******************************************************************************/
package org.eclipse.debug.internal.core;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBinaryStreamListener;
import org.eclipse.debug.core.IStreamListener;
import org.eclipse.debug.core.model.IBinaryStreamMonitor;

/**
 * Monitors the output stream of a system process and notifies listeners of
 * additions to the stream.
 * <p>
 * The output stream monitor reads system out (or err) via and input stream.
 */
public class OutputStreamMonitor implements IBinaryStreamMonitor {
	/**
	 * The size of the read buffer.
	 */
	private static final int BUFFER_SIZE = 8192;

	/**
	 * The stream being monitored (connected system out or err).
	 */
	private InputStream fStream;

	/**
	 * A collection of listeners interested in decoded content.
	 */
	private ListenerList<IStreamListener> fListeners = new ListenerList<>();

	/**
	 * A collection of listeners interested in the raw content.
	 */
	private ListenerList<IBinaryStreamListener> fBinaryListeners = new ListenerList<>();

	/**
	 * The buffered stream content since last flush. Value of <code>null</code>
	 * indicates disabled buffering.
	 *
	 * @see #isBuffered()
	 */
	private ByteArrayOutputStream fContents;

	/**
	 * Decoder used for the buffered content. This is required to keep the state
	 * of an incomplete character.
	 */
	private StreamDecoder fBufferedDecoder;
	private String fCachedDecodedContents;

	/**
	 * The thread which reads from the stream
	 */
	private Thread fThread;

	/**
	 * Whether or not this monitor has been killed. When the monitor is killed,
	 * it stops reading from the stream immediately.
	 */
	private boolean fKilled = false;

	private Charset fCharset;

	private StreamDecoder fDecoder;

	private final AtomicBoolean fDone;

	/**
	 * Creates an output stream monitor on the given stream (connected to system
	 * out or err).
	 *
	 * @param stream input stream to read from
	 * @param charset stream charset or <code>null</code> for system default;
	 *            unused if only the binary interface is used
	 */
	public OutputStreamMonitor(InputStream stream, Charset charset) {
		// java.lang.ProcessImpl returns a buffered stream anyway
		fStream = stream instanceof BufferedInputStream ? stream : new BufferedInputStream(stream);
		fCharset = charset;
		fDecoder = new StreamDecoder(charset == null ? Charset.defaultCharset() : charset);
		fDone = new AtomicBoolean(false);
		setBuffered(true);
	}

	/**
	 * Creates an output stream monitor on the given stream (connected to system
	 * out or err).
	 *
	 * @param stream input stream to read from
	 * @param encoding stream encoding or <code>null</code> for system default
	 * @deprecated use {@link #OutputStreamMonitor(InputStream, Charset)}
	 *             instead
	 */
	@Deprecated
	public OutputStreamMonitor(InputStream stream, String encoding) {
		this(stream, Charset.forName(encoding));
	}

	@Override
	public synchronized void addListener(IStreamListener listener) {
		fListeners.add(listener);
	}

	@Override
	public synchronized void addBinaryListener(IBinaryStreamListener listener) {
		fBinaryListeners.add(listener);
	}

	/**
	 * Causes the monitor to close all communications between it and the
	 * underlying stream by waiting for the thread to terminate.
	 */
	protected void close() {
		Thread thread = null;

		synchronized (this) {
			thread = fThread;
			fThread = null;
		}

		if (thread != null) {
			try {
				thread.join();
			} catch (InterruptedException ie) {
			}

			fListeners.clear();
			fBinaryListeners.clear();
		}
	}

	/**
	 * Notifies the listeners that content has been appended to the stream. Will
	 * notify both, binary and text listeners.
	 *
	 * @param data that has been appended; not <code>null</code>
	 * @param offset start of valid data
	 * @param length number of valid bytes
	 */
	private void fireStreamAppended(final byte[] data, int offset, int length) {
		if (!fListeners.isEmpty()) {
			String text = fDecoder.decode(data, offset, length);
			for (final IStreamListener listener : fListeners) {
				SafeRunner.run(new ISafeRunnable() {
					@Override
					public void run() throws Exception {
						listener.streamAppended(text, OutputStreamMonitor.this);
					}

					@Override
					public void handleException(Throwable exception) {
						DebugPlugin.log(exception);
					}
				});
			}
		}
		if (!fBinaryListeners.isEmpty()) {
			final byte[] validData;
			if (offset > 0 || length < data.length) {
				validData = new byte[length];
				System.arraycopy(data, offset, validData, 0, length);
			} else {
				validData = data;
			}
			for (final IBinaryStreamListener listener : fBinaryListeners) {
				SafeRunner.run(new ISafeRunnable() {
					@Override
					public void run() throws Exception {
						listener.streamAppended(validData, OutputStreamMonitor.this);
					}

					@Override
					public void handleException(Throwable exception) {
						DebugPlugin.log(exception);
					}
				});
			}
		}
	}

	@Override
	public synchronized String getContents() {
		if (!isBuffered()) {
			return ""; //$NON-NLS-1$
		}
		if (fCachedDecodedContents != null) {
			return fCachedDecodedContents;
		}
		byte[] data = getData();
		fCachedDecodedContents = fBufferedDecoder.decode(data, 0, data.length);
		return fCachedDecodedContents;
	}

	@Override
	public synchronized byte[] getData() {
		return isBuffered() ? fContents.toByteArray() : new byte[0];
	}

	private void read() {
		try {
			internalRead();
		} finally {
			fDone.set(true);
		}
	}

	/**
	 * Continually reads from the stream.
	 * <p>
	 * This method, along with the {@link #startMonitoring(String)} method is
	 * used to allow {@link OutputStreamMonitor} to implement {@link Runnable}
	 * without publicly exposing a {@link Runnable#run()} method.
	 */
	private void internalRead() {
		long lastSleep = System.currentTimeMillis();
		long currentTime = lastSleep;
		byte[] buffer = new byte[BUFFER_SIZE];
		int read = 0;
		try {
			while (read >= 0) {
				try {
					if (fKilled) {
						break;
					}
					read = fStream.read(buffer);
					if (read > 0) {
						synchronized (this) {
							if (isBuffered()) {
								fCachedDecodedContents = null;
								fContents.write(buffer, 0, read);
							}
							fireStreamAppended(buffer, 0, read);
						}
					}
				} catch (IOException ioe) {
					if (!fKilled) {
						DebugPlugin.log(ioe);
					}
					return;
				} catch (NullPointerException e) {
					// killing the stream monitor while reading can cause an NPE
					// when reading from the stream
					if (!fKilled && fThread != null) {
						DebugPlugin.log(e);
					}
					return;
				}

				currentTime = System.currentTimeMillis();
				if (currentTime - lastSleep > 1000) {
					lastSleep = currentTime;
					Thread.yield();// just give up CPU to maintain UI
									// responsiveness.
				}
			}
		} finally {
			try {
				fStream.close();
			} catch (IOException e) {
				DebugPlugin.log(e);
			}
		}
	}

	protected void kill() {
		fKilled = true;
	}

	@Override
	public synchronized void removeListener(IStreamListener listener) {
		fListeners.remove(listener);
	}

	@Override
	public synchronized void removeBinaryListener(IBinaryStreamListener listener) {
		fBinaryListeners.remove(listener);
	}

	/**
	 * Starts a thread which reads from the stream
	 *
	 * @param name Thread name
	 */
	protected void startMonitoring(String name) {
		synchronized (this) {
			if (fThread == null) {
				fDone.set(false);
				fThread = new Thread((Runnable) this::read, name);
				fThread.setDaemon(true);
				fThread.setPriority(Thread.MIN_PRIORITY);
				fThread.start();
			}
		}
	}

	@Override
	public synchronized void setBuffered(boolean buffer) {
		if (isBuffered() != buffer) {
			fCachedDecodedContents = null;
			if (buffer) {
				fContents = new ByteArrayOutputStream();
				fBufferedDecoder = new StreamDecoder(fCharset == null ? Charset.defaultCharset() : fCharset);
			} else {
				fContents = null;
				fBufferedDecoder = null;
			}
		}
	}

	@Override
	public synchronized void flushContents() {
		if (isBuffered()) {
			fCachedDecodedContents = null;
			fContents.reset();
		}
	}

	@Override
	public synchronized boolean isBuffered() {
		return fContents != null;
	}

	/**
	 * @return {@code true} if reading the underlying stream is done.
	 *         {@code false} if reading the stream has not started or is not
	 *         done.
	 */
	public boolean isReadingDone() {
		return fDone.get();
	}
}
