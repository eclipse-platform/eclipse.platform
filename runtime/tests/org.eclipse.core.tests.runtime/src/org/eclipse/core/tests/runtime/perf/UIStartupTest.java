/*******************************************************************************
 * Copyright (c) 2004, 2018 IBM Corporation and others.
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
package org.eclipse.core.tests.runtime.perf;

import org.eclipse.test.performance.*;
import org.junit.Test;

public class UIStartupTest {

	@Test
	public void testUIApplicationStartup() {
		PerformanceMeter meter = Performance.getDefault()
				.createPerformanceMeter(getClass().getName() + '.' + UIStartupTest.class.getName());
		try {
			meter.stop();
			Performance performance = Performance.getDefault();
			performance.tagAsGlobalSummary(meter, "Core UI Startup", Dimension.ELAPSED_PROCESS);
			meter.commit();
			performance.assertPerformanceInRelativeBand(meter, Dimension.ELAPSED_PROCESS, -50, 5);
		} finally {
			meter.dispose();
		}
	}
}
