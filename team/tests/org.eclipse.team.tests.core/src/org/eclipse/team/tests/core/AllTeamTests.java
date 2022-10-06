/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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
package org.eclipse.team.tests.core;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.tests.resources.ResourceTest;
import org.eclipse.team.tests.core.regression.AllTeamRegressionTests;

public class AllTeamTests extends ResourceTest {

	/**
	 * Constructor for CVSClientTest.
	 */
	public AllTeamTests() {
		super();
	}

	/**
	 * Constructor for CVSClientTest.
	 * @param name
	 */
	public AllTeamTests(String name) {
		super(name);
	}

	public static Test suite() {
		TestSuite suite = new TestSuite();
		suite.addTest(RepositoryProviderTests.suite());
		suite.addTest(StreamTests.suite());
		suite.addTest(StorageMergerTests.suite());
		suite.addTest(AllTeamRegressionTests.suite());
		return suite;
	}
}

