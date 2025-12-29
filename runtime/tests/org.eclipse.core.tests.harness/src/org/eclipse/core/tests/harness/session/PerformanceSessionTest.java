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
package org.eclipse.core.tests.harness.session;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Test;

/**
 * A specialization of the {@link Test} annotation for marking a repeated
 * performance session test. When executed with a {@link SessionTestExtension},
 * the specified number of repeated sessions will be executed. If of them gets
 * the properties {@code eclipse.perf.dbloc} and {@code eclipse.perf.config}
 * passed from the host. The last session also gets the
 * {@code eclipse.perf.assertAgainst} property passed from the host to allow
 * execution of performance assertions.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Test
public @interface PerformanceSessionTest {
	/**
	 * {@return the number of repeated sessions to execute}
	 */
	int repetitions() default 1;
}
