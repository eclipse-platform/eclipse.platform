/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others.
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
package org.eclipse.core.tests.session.samples;

import junit.framework.*;
import org.eclipse.core.tests.harness.CoreTest;
import org.eclipse.core.tests.session.SessionTestSuite;
import org.eclipse.test.performance.*;

public class SampleSessionTest extends TestCase {
	public SampleSessionTest(String methodName) {
		super(methodName);
	}

	public void testBasic1() {
		// Everything is fine...
	}

	public void testBasic2() {
		fail("Breaking the test " + System.currentTimeMillis());
	}

	public void testBasic3() {
		throw new RuntimeException("Will break the test as well " + System.currentTimeMillis());
	}

	public void testApplicationStartup() {
		PerformanceMeter meter = Performance.getDefault().createPerformanceMeter(getClass().getName() + ".testPerformance");
		try {
			meter.stop();
			meter.commit();
			Performance.getDefault().assertPerformanceInRelativeBand(meter, Dimension.ELAPSED_PROCESS, -50, 5);
		} finally {
			meter.dispose();
		}
	}

	public static Test suite() {
		return new SessionTestSuite(CoreTest.PI_HARNESS, SampleSessionTest.class);
	}

}
