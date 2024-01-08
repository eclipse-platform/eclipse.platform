/*******************************************************************************
 * Copyright (c) 2024 Vector Informatik GmbH and others.
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.eclipse.core.tests.harness.session.SessionShouldError;
import org.eclipse.core.tests.harness.session.SessionTestExtension;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Examples demonstrating the behavior of session tests using the JUnit 5
 * platform. When executed, the {@code Successful} tests will succeed, while the
 * {@code Failing} ones will fail.
 */
public class SampleSessionTests {
	@RegisterExtension
	static SessionTestExtension sessionTestExtension = SessionTestExtension.forPlugin(PI_HARNESS).create();

	@Nested
	public class Successful {
		@Test
		public void asIntended() {
			assertEquals(1, 1);
		}

		@Test
		@SessionShouldError
		public void withExpectedFailure() {
			fail("fail");
		}

		@Test
		@SessionShouldError
		public void withExpectedError_viaException() {
			throw new RuntimeException("error");
		}

		@Test
		@SessionShouldError
		public void withExpectedError_viaExit() {
			System.exit(1);
		}
	}

	@Nested
	public class Failing {
		@Test
		@SessionShouldError
		public void unexpectedlySuccessful() {
			assertEquals(1, 1);
		}

		@Test
		public void withFailure() {
			fail("fail");
		}

		@Test
		public void withError_viaException() {
			throw new RuntimeException("error");
		}

		@Test
		public void withError_viaExit() {
			System.exit(1);
		}
	}

}
