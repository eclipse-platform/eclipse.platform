/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
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
package org.eclipse.core.internal.resources;

import java.io.PrintStream;
import java.io.PrintWriter;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.runtime.*;

/**
 * A checked exception representing a failure.
 * <p>
 * Resource exceptions contain a status object describing the cause of the
 * exception, and optionally the path of the resource where the failure
 * occurred.
 * </p>
 *
 * @see IStatus
 */
public class ResourceException extends CoreException {
	/**
	 * All serializable objects should have a stable serialVersionUID
	 */
	private static final long serialVersionUID = 1L;

	public ResourceException(int code, IPath path, String message, Throwable exception) {
		super(new ResourceStatus(code, path, message, provideStackTrace(message, exception)));
	}

	private static Throwable provideStackTrace(String message, Throwable exception) {
		return exception != null ? exception : new Exception(message);
	}

	/**
	 * Constructs a new exception with the given status object.
	 *
	 * @param status the status object to be associated with this exception
	 * @see IStatus
	 */
	public ResourceException(IStatus status) {
		super(status);
	}

	/**
	 * Prints a stack trace out for the exception, and
	 * any nested exception that it may have embedded in
	 * its Status object.
	 */
	@Override
	public void printStackTrace() {
		printStackTrace(System.err);
	}

	/**
	 * Prints a stack trace out for the exception, and
	 * any nested exception that it may have embedded in
	 * its Status object.
	 */
	@Override
	public void printStackTrace(PrintStream output) {
		synchronized (output) {
			IStatus status = getStatus();
			if (status.getException() != null) {
				String path = "()"; //$NON-NLS-1$
				if (status instanceof IResourceStatus)
					path = "(" + ((IResourceStatus) status).getPath() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
				output.print(getClass().getName() + path + "[" + status.getCode() + "]: "); //$NON-NLS-1$ //$NON-NLS-2$
				status.getException().printStackTrace(output);
			} else
				super.printStackTrace(output);
		}
	}

	/**
	 * Prints a stack trace out for the exception, and
	 * any nested exception that it may have embedded in
	 * its Status object.
	 */
	@Override
	public void printStackTrace(PrintWriter output) {
		synchronized (output) {
			IStatus status = getStatus();
			if (status.getException() != null) {
				String path = "()"; //$NON-NLS-1$
				if (status instanceof IResourceStatus)
					path = "(" + ((IResourceStatus) status).getPath() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
				output.print(getClass().getName() + path + "[" + status.getCode() + "]: "); //$NON-NLS-1$ //$NON-NLS-2$
				status.getException().printStackTrace(output);
			} else
				super.printStackTrace(output);
		}
	}

}
