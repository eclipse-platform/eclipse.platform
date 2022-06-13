/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
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
package org.eclipse.e4.core.internal.di.shared;


/**
 * This class attempt to use OSGi logging service to log a error. If OSGi is not
 * available, it falls back on writing to the error stream.
 */
public class CoreLogger {

	static public void logError(String msg, Throwable e) {
		// Different VMs have different degrees of "laziness" for the class loading.
		// To make sure that VM won't try to load OSGi-specific helper class before getting into
		// this try-catch block, the fully qualified name is used (removing entry for
		// the utility class from the import list).
		try {
			org.eclipse.e4.core.internal.di.osgi.LogHelper.logError(msg, e);
		} catch (NoClassDefFoundError noClass) {
			baseLog(msg, e);
		}
	}

	static private void baseLog(String msg, Throwable e) {
		if (msg != null)
			System.err.println(msg);
		if (e != null)
			e.printStackTrace();
	}

}
