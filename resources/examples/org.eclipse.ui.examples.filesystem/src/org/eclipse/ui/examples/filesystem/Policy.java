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
package org.eclipse.ui.examples.filesystem;

import org.eclipse.core.runtime.*;
import org.osgi.framework.Bundle;

/**
 * 
 */
public class Policy {
	private static Bundle bundle;
	public static final String PI_FILESYSTEM_EXAMPLE = "org.eclipse.ui.examples.filesystem"; //$NON-NLS-1$

	private static Bundle getBundle() {
		if (bundle == null)
			bundle = Platform.getBundle(PI_FILESYSTEM_EXAMPLE);
		return bundle;
	}

	public static void log(IStatus status) {
		Platform.getLog(getBundle()).log(status);
	}

	public static void log(String message, IStatus status) {
		final ILog log = Platform.getLog(getBundle());
		log.log(new Status(status.getSeverity(), PI_FILESYSTEM_EXAMPLE, 1, message, null));
		log.log(status);
	}

	public static void log(String message, Throwable t) {
		final ILog log = Platform.getLog(getBundle());
		log.log(new Status(IStatus.ERROR, PI_FILESYSTEM_EXAMPLE, 1, message, t));
	}
}
