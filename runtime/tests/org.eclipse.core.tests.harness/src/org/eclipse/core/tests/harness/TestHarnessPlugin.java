/*******************************************************************************
 * Copyright (c) 2023 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package org.eclipse.core.tests.harness;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;

public final class TestHarnessPlugin {
	/**
	 * ID of this plug-in
	 */
	public static final String PI_HARNESS = "org.eclipse.core.tests.harness";

	private TestHarnessPlugin() {
	}

	/**
	 * Logs the given status via {@link ILog} for the ID of this plug-in
	 * {@link #PI_HARNESS}.
	 */
	public static void log(IStatus status) {
		ILog.of(Platform.getBundle(PI_HARNESS)).log(status);
	}

}
