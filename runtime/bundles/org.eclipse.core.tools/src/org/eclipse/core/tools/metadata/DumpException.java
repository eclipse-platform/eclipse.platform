/*******************************************************************************
 * Copyright (c) 2002, 2006 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tools.metadata;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 A high-level exception class to be used by <code>IDumper</code>s. It is
 intended to be used as a wrapper for low-level exceptions. A method for
 accessing the low-level exception that caused this
 <code>DumpException</code> is provided.
 */
public class DumpException extends Exception {

	private static final long serialVersionUID = 1L;
	/**
	 * The low-level exception that caused this <code>DumpException</code>. May be
	 * null.
	 */
	private Throwable cause;

	/**
	 * Constructs a <code>DumpException</code> with the provided message.
	 *
	 * @param msg the message
	 */
	public DumpException(String msg) {
		super(msg);
	}

	/**
	 * Constructs a <code>DumpException</code> with the provided message and cause.
	 *
	 * @param msg the message
	 * @param cause the exception that caused this <code>DumpException</code>
	 */
	public DumpException(String msg, Throwable cause) {
		super(msg);
		this.cause = cause;
	}

	/**
	 * Returns the cause for this <code>DumpException</code>. May be null.
	 *
	 * @return the cause for this <code>DumpException</code>.
	 */
	@Override
	public Throwable getCause() {
		return cause;
	}

	/**
	 * Returns this exception message (including the cause message, if there is a
	 * cause exception).
	 *
	 * @return the error message string
	 * @see java.lang.Throwable#getMessage()
	 */
	@Override
	public String getMessage() {
		return super.getMessage() + ((cause != null) ? (" caused by " + cause) : ""); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Prints this <code>DumpException</code> message and its backtrace to the
	 * specified print stream.
	 *
	 * @param output the <code>java.io.PrintStream</code> object where to print
	 * @see java.lang.Throwable#printStackTrace(java.io.PrintStream)
	 */
	@Override
	public void printStackTrace(PrintStream output) {
		super.printStackTrace(output);
		if (cause == null)
			return;
		output.println("*** Caused by:"); //$NON-NLS-1$
		cause.printStackTrace(output);
	}

	/**
	 * Prints this <code>DumpException</code> message and its backtrace to the
	 * specified print writer.
	 *
	 * @param output the <code>java.io.PrintWriter</code> object where to print
	 * @see java.lang.Throwable#printStackTrace(java.io.PrintWriter)
	 */
	@Override
	public void printStackTrace(PrintWriter output) {
		super.printStackTrace(output);
		if (cause == null)
			return;
		output.println("*** Caused by:"); //$NON-NLS-1$
		cause.printStackTrace(output);
	}
}
