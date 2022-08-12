/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.team.internal.core.streams;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Updates a progress monitor as bytes are read from the input stream.
 * Also starts a background thread to provide responsive cancellation on read().
 *
 * Supports resuming partially completed operations after an InterruptedIOException
 * if the underlying stream does.  Check the bytesTransferred field to determine how
 * much of the operation completed; conversely, at what point to resume.
 */
public abstract class ProgressMonitorInputStream extends FilterInputStream {
	private IProgressMonitor monitor;
	private int updateIncrement;
	private long bytesTotal;
	private long bytesRead = 0;
	private long lastUpdate = -1;
	private long nextUpdate = 0;

	/**
	 * Creates a progress monitoring input stream.
	 * @param in the underlying input stream
	 * @param bytesTotal the number of bytes to read in total (passed to updateMonitor())
	 * @param updateIncrement the number of bytes read between updates
	 * @param monitor the progress monitor
	 */
	public ProgressMonitorInputStream(InputStream in, long bytesTotal, int updateIncrement, IProgressMonitor monitor) {
		super(in);
		this.bytesTotal = bytesTotal;
		this.updateIncrement = updateIncrement;
		this.monitor = monitor;
		update(true);
	}

	protected abstract void updateMonitor(long bytesRead, long size, IProgressMonitor monitor);

	/**
	 * Wraps the underlying stream's method.
	 * Updates the progress monitor to the final number of bytes read.
	 * @throws IOException if an i/o error occurs
	 */
	@Override
	public void close() throws IOException {
		try {
			in.close();
		} finally {
			update(true);
		}
	}

	/**
	 * Wraps the underlying stream's method.
	 * Updates the progress monitor if the next update increment has been reached.
	 * @throws InterruptedIOException if the operation was interrupted before all of the
	 *         bytes specified have been skipped, bytesTransferred will be zero
	 * @throws IOException if an i/o error occurs
	 */
	@Override
	public int read() throws IOException {
		int b = in.read();
		if (b != -1) {
			bytesRead += 1;
			update(false);
		}
		return b;
	}

	/**
	 * Wraps the underlying stream's method.
	 * Updates the progress monitor if the next update increment has been reached.
	 * @throws InterruptedIOException if the operation was interrupted before all of the
	 *         bytes specified have been skipped, bytesTransferred may be non-zero
	 * @throws IOException if an i/o error occurs
	 */
	@Override
	public int read(byte[] buffer, int offset, int length) throws IOException {
		try {
			int count = in.read(buffer, offset, length);
			if (count != -1) {
				bytesRead += count;
				update(false);
			}
			return count;
		} catch (InterruptedIOException e) {
			bytesRead += e.bytesTransferred;
			update(false);
			throw e;
		}
	}

	/**
	 * Wraps the underlying stream's method.
	 * Updates the progress monitor if the next update increment has been reached.
	 * @throws InterruptedIOException if the operation was interrupted before all of the
	 *         bytes specified have been skipped, bytesTransferred may be non-zero
	 * @throws IOException if an i/o error occurs
	 */
	@Override
	public long skip(long amount) throws IOException {
		try {
			long count = in.skip(amount);
			bytesRead += count;
			update(false);
			return count;
		} catch (InterruptedIOException e) {
			bytesRead += e.bytesTransferred;
			update(false);
			throw e;
		}
	}

	/**
	 * Mark is not supported by the wrapper even if the underlying stream does, returns false.
	 */
	@Override
	public boolean markSupported() {
		return false;
	}

	private void update(boolean now) {
		if (bytesRead >= nextUpdate || now) {
			nextUpdate = bytesRead - (bytesRead % updateIncrement);
			if (nextUpdate != lastUpdate) updateMonitor(nextUpdate, bytesTotal, monitor);
			lastUpdate = nextUpdate;
			nextUpdate += updateIncrement;
		}
	}
}
