/*******************************************************************************
 * Copyright (c) 2023 Eclipse Platform, Security Group and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Eclipse Platform - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.pki;

public class DebugLogger {
	public static boolean enableDebugLogging = false;

	public static void setEnableDebugLogging(boolean enableDebugLogging) {
		DebugLogger.enableDebugLogging = enableDebugLogging;
	}

	public static void printDebug(String message) {
		if (enableDebugLogging || checkForFlag()) {
			System.out.println(message);
		}
	}

	private static boolean checkForFlag() {
		String debugFlag = System.getProperty("debug.verbose"); //$NON-NLS-1$
		if (debugFlag != null) {
			debugFlag = debugFlag.trim().toLowerCase();
			if (debugFlag.equals("true")) { //$NON-NLS-1$
				DebugLogger.setEnableDebugLogging(true);
				return true;
			}
		}
		return false;
	}
}
