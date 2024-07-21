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

import org.eclipse.core.internal.runtime.RuntimeLog;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;

/**
 * Grab bag of utility methods for the file system plugin
 */
public class Policy {
	public static final String PI_FILE_SYSTEM = "org.eclipse.core.filesystem"; //$NON-NLS-1$

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

}
