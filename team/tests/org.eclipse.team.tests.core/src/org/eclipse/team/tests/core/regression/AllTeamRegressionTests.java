/*******************************************************************************
 * Copyright (c) 2008, 2010 IBM Corporation and others.
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
package org.eclipse.team.tests.core.regression;

import junit.framework.*;

public class AllTeamRegressionTests extends TestCase {
	public AllTeamRegressionTests() {
		super(null);
	}

	public AllTeamRegressionTests(String name) {
		super(name);
	}

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTeamRegressionTests.class.getName());
		suite.addTest(Bug_217673.suite());
		suite.addTest(DoNotRemoveTest.suite());
		return suite;
	}
}
