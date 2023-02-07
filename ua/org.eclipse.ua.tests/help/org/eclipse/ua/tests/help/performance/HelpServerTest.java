/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
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

package org.eclipse.ua.tests.help.performance;

import org.eclipse.test.performance.Dimension;
import org.eclipse.test.performance.PerformanceTestCase;
import org.eclipse.ua.tests.help.util.LoadServletUtil;

/**
 * Test the performance of the help server without launching the Help UI
 */

public class HelpServerTest extends PerformanceTestCase {

	@Override
	protected void tearDown() throws Exception {
		LoadServletUtil.stopServer();
		super.tearDown();
	}

	public void testServletRead100x() throws Exception {
		tagAsSummary("Servlet Read", Dimension.ELAPSED_PROCESS);
		LoadServletUtil.startServer();
		// run the tests
		for (int i=0; i < 100; ++i) {
			boolean warmup = i < 2;
			if (!warmup) {
				startMeasuring();
			}

			for (int j = 0; j <= 100; j++) {
				LoadServletUtil.readLoadServlet(200);
			}

			if (!warmup) {
				stopMeasuring();
			}
		}

		commitMeasurements();
		assertPerformance();
	}

	public void testStartServer() throws Exception {
		tagAsSummary("Start Server", Dimension.ELAPSED_PROCESS);

		// run the tests
		for (int i=0; i < 25; ++i) {
			boolean warmup = i < 2;
			LoadServletUtil.stopServer();
			if (!warmup) {
				startMeasuring();
			}

			LoadServletUtil.startServer();

			if (!warmup) {
				stopMeasuring();
			}
		}

		commitMeasurements();
		assertPerformance();
	}

}
