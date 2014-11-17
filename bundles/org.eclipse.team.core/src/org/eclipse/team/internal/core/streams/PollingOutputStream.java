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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.team.internal.core.Messages;
import org.eclipse.team.internal.core.Policy;

/**
 * Polls a progress monitor periodically and handles timeouts over extended durations.
 * For this class to be effective, a high numAttempts should be specified, and the
 * underlying stream should time out frequently on writes (every second or so).
 *
 * Supports resuming partially completed operations after an InterruptedIOException
 * if the underlying stream does.  Check the bytesTransferred field to determine how
 * much of the operation completed; conversely, at what point to resume.
 */
public class PollingOutputStream extends FilterOutputStream {
	private int numAttempts;
	private IProgressMonitor monitor;
	private boolean cancellable;
	
	/**
	 * Creates a new polling output stream.
	 * @param out the underlying output stream
	 * @param numAttempts the number of attempts before issuing an InterruptedIOException,
	 *           if 0, retries indefinitely until canceled
	 * @param monitor the progress monitor to be polled for cancellation
	 */
	public PollingOutputStream(OutputStream out, int numAttempts, IProgressMonitor monitor) {
		super(out);
		this.numAttempts = numAttempts;
		this.monitor = monitor;
		this.cancellable = true;
	}
	
	/**
	 * Wraps the underlying stream's method.
	 * @throws OperationCanceledException if the progress monitor is canceled
	 * @throws InterruptedIOException if the underlying operation times out numAttempts times
	 *         and no data was sent, bytesTransferred will be zero
	 * @throws IOException if an i/o error occurs
	 */
	public void write(int b) throws IOException {
		int attempts = 0;
		for (;;) {
			if (checkCancellation()) throw new OperationCanceledException();
			try {
				out.write(b);
				return;
			} catch (InterruptedIOException e) {
				if (++attempts == numAttempts)
					throw new InterruptedIOException(Messages.PollingOutputStream_writeTimeout); 
				if (Policy.DEBUG_STREAMS) System.out.println("write retry=" + attempts); //$NON-NLS-1$
			}
		}
	}
	
	/**
	 * Wraps the underlying stream's method.
	 * @throws OperationCanceledException if the progress monitor is canceled
	 * @throws InterruptedIOException if the underlying operation times out numAttempts times,
	 *         bytesTransferred will reflect the number of bytes sent
	 * @throws IOException if an i/o error occurs
	 */
	public void write(byte[] buffer, int off, int len) throws IOException {
		int count = 0;
		int attempts = 0;
		for (;;) {
			if (checkCancellation()) throw new OperationCanceledException();
			try {
				out.write(buffer, off, len);
				return;
			} catch (InterruptedIOException e) {
				int amount = e.bytesTransferred;
				if (amount != 0) { // keep partial transfer
					len -= amount;
					if (len <= 0) return;
					off += amount;
					count += amount;
					attempts = 0; // made some progress, don't time out quite yet
				}
				if (++attempts == numAttempts) {
					e = new InterruptedIOException(Messages.PollingOutputStream_writeTimeout); 
					e.bytesTransferred = count;
					throw e;
				}
				if (Policy.DEBUG_STREAMS) System.out.println("write retry=" + attempts); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Wraps the underlying stream's method.
	 * @throws OperationCanceledException if the progress monitor is canceled
	 * @throws InterruptedIOException if the underlying operation times out numAttempts times,
	 *         bytesTransferred will reflect the number of bytes sent
	 * @throws IOException if an i/o error occurs
	 */
	public void flush() throws IOException {
		int count = 0;
		int attempts = 0;
		for (;;) {
			if (checkCancellation()) throw new OperationCanceledException();
			try {
				out.flush();
				return;
			} catch (InterruptedIOException e) {
				int amount = e.bytesTransferred;
				if (amount != 0) { // keep partial transfer
					count += amount;
					attempts = 0; // made some progress, don't time out quite yet
				}
				if (++attempts == numAttempts) {
					e = new InterruptedIOException(Messages.PollingOutputStream_writeTimeout); 
					e.bytesTransferred = count;
					throw e;
				}
				if (Policy.DEBUG_STREAMS) System.out.println("write retry=" + attempts); //$NON-NLS-1$
			}
		}
	}
	
	/**
	 * Calls flush() then close() on the underlying stream.
	 * @throws OperationCanceledException if the progress monitor is canceled
	 * @throws InterruptedIOException if the underlying operation times out numAttempts times,
	 *         bytesTransferred will reflect the number of bytes sent during the flush()
	 * @throws IOException if an i/o error occurs
	 */
	public void close() throws IOException {
		int attempts = numAttempts - 1; // fail fast if flush() does times out
 		try {
 			out.flush();
			attempts = 0;
 		} finally {
 			boolean stop = false;
			while (!stop) {
				try {
					out.close();
					stop = true;
				} catch (InterruptedIOException e) {
					if (checkCancellation()) throw new OperationCanceledException();
					if (++attempts == numAttempts)
						throw new InterruptedIOException(Messages.PollingOutputStream_closeTimeout); 
					if (Policy.DEBUG_STREAMS) System.out.println("close retry=" + attempts); //$NON-NLS-1$
				}
			}
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
