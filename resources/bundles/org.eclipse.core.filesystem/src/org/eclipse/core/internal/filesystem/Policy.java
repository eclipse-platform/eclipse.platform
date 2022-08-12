/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
package org.eclipse.core.internal.filesystem;

import java.io.*;
import java.util.Date;
import org.eclipse.core.internal.runtime.RuntimeLog;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;

/**
 * Grab bag of utility methods for the file system plugin
 */
public class Policy {
	public static final String PI_FILE_SYSTEM = "org.eclipse.core.filesystem"; //$NON-NLS-1$

	/**
	 * Print a debug message to the console.
	 * Pre-pend the message with the current date and the name of the current thread.
	 */
	public static void debug(String message) {
		StringBuilder buffer = new StringBuilder();
		buffer.append(new Date(System.currentTimeMillis()));
		buffer.append(" - ["); //$NON-NLS-1$
		buffer.append(Thread.currentThread().getName());
		buffer.append("] "); //$NON-NLS-1$
		buffer.append(message);
		System.out.println(buffer.toString());
	}

	public static void error(int code, String message) throws CoreException {
		error(code, message, null);
	}

	public static void error(int code, String message, Throwable exception) throws CoreException {
		int severity = code == 0 ? 0 : 1 << (code % 100 / 33);
		throw new CoreException(new Status(severity, PI_FILE_SYSTEM, code, message, exception));
	}

	public static void log(int severity, String message, Throwable t) {
		if (message == null)
			message = ""; //$NON-NLS-1$
		RuntimeLog.log(new Status(severity, PI_FILE_SYSTEM, 1, message, t));
	}

	/**
	 * Closes a stream and ignores any resulting exception.
	 */
	public static void safeClose(InputStream in) {
		try {
			if (in != null)
				in.close();
		} catch (IOException e) {
			//ignore
		}
	}

	/**
	 * Closes a stream and ignores any resulting exception.
	 */
	public static void safeClose(OutputStream out) {
		try {
			if (out != null)
				out.close();
		} catch (IOException e) {
			//ignore
		}
	}
}
