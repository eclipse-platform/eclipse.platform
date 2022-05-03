/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
		Platform.getLog(Platform.getBundle(PI_FILESYSTEM_EXAMPLE)).log(status);
	}

	public static IStatus createStatus(Throwable t) {
		if (t instanceof CoreException)
			return ((CoreException) t).getStatus();
		return new Status(IStatus.ERROR, PI_FILESYSTEM_EXAMPLE, 1, "Internal Error: " + t.getMessage(), t);
	}

}
