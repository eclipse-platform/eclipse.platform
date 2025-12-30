/*******************************************************************************
 * Copyright (c) 2025 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.core.tests.harness.session.samples;

import static org.eclipse.core.tests.harness.TestHarnessPlugin.PI_HARNESS;

import java.util.Date;
import org.eclipse.core.tests.harness.session.SessionTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Example demonstrating the a session test executed as UI application using the
 * JUnit 5 platform.
 */
public class SampleUISessionTest {
	@RegisterExtension
	final SessionTestExtension sessionTestExtension = SessionTestExtension.forPlugin(PI_HARNESS)
			.withApplicationId(SessionTestExtension.UI_TEST_APPLICATION).create();

	@Test
	public void testApplicationStartup() {
		message("Running startup test");
	}

	/**
	 * Print a debug message to the console. Pre-pend the message with the current
	 * date and the name of the current thread.
	 */
	public static void message(String message) {
		StringBuilder buffer = new StringBuilder();
		buffer.append(new Date(System.currentTimeMillis()));
		buffer.append(" - ["); //$NON-NLS-1$
		buffer.append(Thread.currentThread().getName());
		buffer.append("] "); //$NON-NLS-1$
		buffer.append(message);
		System.out.println(buffer.toString());
	}

}
