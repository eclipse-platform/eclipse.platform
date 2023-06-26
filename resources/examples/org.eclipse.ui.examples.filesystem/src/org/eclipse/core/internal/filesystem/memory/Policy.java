/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package org.eclipse.core.internal.filesystem.memory;

import org.eclipse.core.runtime.*;

/**
 * 
 */
public class Policy {
	public static final String PI_FILESYSTEM_EXAMPLE = "org.eclipse.ui.examples.filesystem"; //$NON-NLS-1$

	public static void error(String message) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, PI_FILESYSTEM_EXAMPLE, 1, message, null));
	}

	private Policy() {
		super();
	}

	public static void log(IStatus status) {
		ILog.of(Platform.getBundle(PI_FILESYSTEM_EXAMPLE)).log(status);
	}

	public static IStatus createStatus(Throwable t) {
		if (t instanceof CoreException)
			return ((CoreException) t).getStatus();
		return new Status(IStatus.ERROR, PI_FILESYSTEM_EXAMPLE, 1, "Internal Error: " + t.getMessage(), t);
	}

}
