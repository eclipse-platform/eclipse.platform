/*******************************************************************************
 *  Copyright (c) 2000, 2012 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.resources;

/**
 * Workspace session tests function as follows:  Each test class looks like a typical JUnit test,
 * except the platform is shutdown and restarted after each test method.  The steps for each
 * test method are:
 *  - startup the platform
 *  - run setUp
 *  - run the test method
 *  - run tearDown
 *  - shutdown the platform
 *
 * Tests are run according to the natural order of their method names.  This is the
 * partial ordering defined by the String.compareTo() operation.  Each test method
 * must begin with the prefix "test", and have no parameters (thus overloading is
 * not supported).
 *
 * After all test methods in the class have been run, the platform location is deleted.
 * This way, each test class plays like a series of related operations on the same
 * workspace, with each operation running in a separate platform instance.
 *
 * @see org.eclipse.core.tests.session.WorkspaceSessionTestSuite
 */
public class WorkspaceSessionTest extends ResourceTest {
	protected String testName;

	/**
	 * Constructor for WorkspaceSessionTest.
	 */
	public WorkspaceSessionTest() {
		super();
	}

	/**
	 * Constructor for WorkspaceSessionTest.
	 *
	 * @param name
	 *            name of the TestCase
	 */
	public WorkspaceSessionTest(String name) {
		super(name);
	}

	@Override
	protected void tearDown() throws Exception {
		// We should not run super.tearDown() on session tests.
		// If needed, we should call it explicitly.
	}
}
