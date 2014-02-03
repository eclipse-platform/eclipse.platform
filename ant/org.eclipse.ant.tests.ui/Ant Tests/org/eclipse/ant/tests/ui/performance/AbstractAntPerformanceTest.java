/*******************************************************************************
 *  Copyright (c) 2005, 2012 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.tests.ui.performance;

import org.eclipse.ant.tests.ui.testplugin.AbstractAntUITest;
import org.eclipse.test.performance.PerformanceTestCase;

/**
 * Abstract class for ant performance tests, ensures the test project is created 
 * and ready in the test workspace.
 * 
 * @since 3.5
 */
public abstract class AbstractAntPerformanceTest extends PerformanceTestCase {

	/* (non-Javadoc)
	 * @see org.eclipse.test.performance.PerformanceTestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		AbstractAntUITest.assertProject();
	}
}
