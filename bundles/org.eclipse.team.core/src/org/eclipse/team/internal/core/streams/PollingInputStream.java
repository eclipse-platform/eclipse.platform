/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.team.internal.core.*;
import org.eclipse.team.internal.core.Policy;
import org.eclipse.team.internal.core.TeamPlugin;

/**
 * Polls a progress monitor periodically and handles timeouts over extended durations.
 * For this class to be effective, a high numAttempts should be specified, and the
 * underlying stream should time out frequently on reads (every second or so).
 *
 * Supports resuming partially completed operations after an InterruptedIOException
 * if the underlying stream does.  Check the bytesTransferred field to determine how
 * much of the operation completed; conversely, at what point to resume.
 */
public class PollingInputStream extends FilterInputStream {
	private int numAttempts;
	private IProgressMonitor monitor;
	private boolean cancellable;
	
	/**
	 * Creates a new polling input stream.
	 * @param in the underlying input stream
	 * @param numAttempts the number of attempts before issuing an InterruptedIOException,
	 *        if 0, retries indefinitely until canceled
	 * @param monitor the progress monitor to be polled for cancellation
	 */
	public PollingInputStream(InputStream in, int numAttempts, IProgressMonitor monitor) {
		super(in);
		this.numAttempts = numAttempts;
		this.monitor = monitor;
		this.cancellable = true;
	}
	
	/**
	 * Wraps the underlying stream's method.
	 * It may be important to wait for an input stream to be closed because it
	 * holds an implicit lock on a system resource (such as a file) while it is
	 * open.  Closing a stream may take time if the underlying stream is still
	 * servicing a previous request.
	 * @throws OperationCanceledException if the progress monitor is canceled
	 * @throws InterruptedIOException if the underlying operation times out numAttempts times
	 */
	public void close() throws InterruptedIOException {
		int attempts = 0;
		try {
			readPendingInput();
		} catch (IOException e) {
			// We shouldn't get an exception when we're getting the available input.
			// If we do, just log it so we can close.
			TeamPlugin.log(IStatus.ERROR, e.getMessage(), e);
		} finally {
			boolean stop = false;
			while (!stop) {
				try {
					if (in != null)
						in.close();
					stop = true;
				} catch (InterruptedIOException e) {
					if (checkCancellation()) throw new OperationCanceledException();
					if (++attempts == numAttempts)
						throw new InterruptedIOException(Messages.PollingInputStream_closeTimeout); 
					if (Policy.DEBUG_STREAMS) System.out.println("close retry=" + attempts); //$NON-NLS-1$
				} catch (IOException e) {
					// ignore it - see https://bugs.eclipse.org/bugs/show_bug.cgi?id=203423#c10
				}
			}
		}
	}
	
	/**
	 * Wraps the underlying stream's method.
	 * @return the next byte of data, or -1 if the end of the stream is reached.
	 * @throws OperationCanceledException if the progress monitor is canceled
	 * @throws InterruptedIOException if the underlying operation times out numAttempts times
	 *         and no data was received, bytesTransferred will be zero
	 * @throws IOException if an i/o error occurs
	 */
	public int read() throws IOException {
		int attempts = 0;
		for (;;) {
			if (checkCancellation()) throw new OperationCanceledException();
			try {
				return in.read();
			} catch (InterruptedIOException e) {
				if (++attempts == numAttempts)
					throw new InterruptedIOException(Messages.PollingInputStream_readTimeout); 
				if (Policy.DEBUG_STREAMS) System.out.println("read retry=" + attempts); //$NON-NLS-1$
			}
		}
	}
	
	/**
	 * Wraps the underlying stream's method.
	 * @param buffer - the buffer into which the data is read.
	 * @param off - the start offset of the data.
	 * @param len - the maximum number of bytes read.
	 * @return the total number of bytes read into the buffer, or -1 if there is no more data because the end of the stream has been reached.
	 * @throws OperationCanceledException if the progress monitor is canceled
	 * @throws InterruptedIOException if the underlying operation times out numAttempts times
	 *         and no data was received, bytesTransferred will be zero
	 * @throws IOException if an i/o error occurs
	 */
	public int read(byte[] buffer, int off, int len) throws IOException {
		int attempts = 0;
		for (;;) {
			if (checkCancellation()) throw new OperationCanceledException();
			try {
				return in.read(buffer, off, len);
			} catch (InterruptedIOException e) {
				if (e.bytesTransferred != 0) return e.bytesTransferred; // keep partial transfer
				if (++attempts == numAttempts)
					throw new InterruptedIOException(Messages.PollingInputStream_readTimeout); 
				if (Policy.DEBUG_STREAMS) System.out.println("read retry=" + attempts); //$NON-NLS-1$
			}
		}
	}
	
	/**
	 * Wraps the underlying stream's method.
	 * @param count - the number of bytes to be skipped.
	 * @return the actual number of bytes skipped.
	 * @throws OperationCanceledException if the progress monitor is canceled
	 * @throws InterruptedIOException if the underlying operation times out numAttempts times
	 *         and no data was received, bytesTransferred will be zero
	 * @throws IOException if an i/o error occurs
	 */
	public long skip(long count) throws IOException {
		int attempts = 0;
		for (;;) {
			if (checkCancellation()) throw new OperationCanceledException();
			try {
				return in.skip(count);
			} catch (InterruptedIOException e) {
				if (e.bytesTransferred != 0) return e.bytesTransferred; // keep partial transfer
				if (++attempts == numAttempts)
					throw new InterruptedIOException(Messages.PollingInputStream_readTimeout); 
				if (Policy.DEBUG_STREAMS) System.out.println("read retry=" + attempts); //$NON-NLS-1$
			}
		}
	}
	
	/**
	 * Reads any pending input from the input stream so that
	 * the stream can savely be closed.
	 */
	protected void readPendingInput() throws IOException {
		byte[] buffer= new byte[2048];
		while (true) {
			int available = in.available();
			if (available < 1) break;
			if (available > buffer.length) available = buffer.length;
			if (in.read(buffer, 0, available) < 1) break;
		}	
	}
		
	/**
	 * Called to set whether cancellation will be checked by this stream. Turning cancellation checking
	 * off can be very useful for protecting critical portions of a protocol that shouldn't be interrupted. 
	 * For example, it is often necessary to protect login sequences.
	 * @param cancellable a flag controlling whether this stream will check for cancellation.
	 */
	public void setIsCancellable(boolean cancellable) {
		this.cancellable = cancellable;
	}

	/**
	 * Checked whether the monitor for this stream has been cancelled. If the cancellable
	 * flag is <code>false</code> then the monitor is never cancelled. 
	 * @return <code>true</code> if the monitor has been cancelled and <code>false</code>
	 * otherwise.
	 */	
	private boolean checkCancellation() {
		if(cancellable) {
			return monitor.isCanceled();
		} else {
			return false;
		}
	}
}
