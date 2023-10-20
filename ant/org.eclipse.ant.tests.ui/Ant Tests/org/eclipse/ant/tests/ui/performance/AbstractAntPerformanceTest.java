/*******************************************************************************
 *  Copyright (c) 2005, 2008 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.tests.ui.performance;

import org.eclipse.ant.tests.ui.testplugin.AbstractAntUITest;
import org.eclipse.test.performance.PerformanceTestCaseJunit5;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;

/**
 * Abstract class for ant performance tests, ensures the test project is created and ready in the test workspace.
 *
 * @since 3.5
 */
public abstract class AbstractAntPerformanceTest extends PerformanceTestCaseJunit5 {

	@BeforeEach
	@Override
	public void setUp(TestInfo testInfo) throws Exception {
		super.setUp(testInfo);
		AbstractAntUITest.assertProject();
	}
}
