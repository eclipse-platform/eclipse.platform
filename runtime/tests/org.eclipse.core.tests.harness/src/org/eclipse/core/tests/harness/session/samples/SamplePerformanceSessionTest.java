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

import org.eclipse.core.tests.harness.session.PerformanceSessionTest;
import org.eclipse.core.tests.harness.session.SessionTestExtension;
import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.Performance;
import org.eclipse.test.performance.PerformanceMeter;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Examples demonstrating the behavior of performance session tests using the
 * JUnit 5 platform. When executed, a warmup session is performed and five
 * subsequent actual sessions with startup measurements will be performed.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SamplePerformanceSessionTest {
	@RegisterExtension
	SessionTestExtension sessionTestExtension = SessionTestExtension.forPlugin(PI_HARNESS).create();

	@Test
	@Order(0)
	public void warmup() {
		actualTest(true);
	}

	@PerformanceSessionTest(repetitions = 5)
	@Order(1)
	public void runMeasurements() {
		actualTest(false);
	}

	private void actualTest(boolean warmup) {
		PerformanceMeter meter = Performance.getDefault().createPerformanceMeter(getClass().getName() + ".startup");
		try {
			meter.stop();
			if (!warmup) {
				meter.commit();
			}
			Performance.getDefault().assertPerformanceInRelativeBand(meter, Dimension.ELAPSED_PROCESS, -50, 5);
		} finally {
			meter.dispose();
		}
	}

}
