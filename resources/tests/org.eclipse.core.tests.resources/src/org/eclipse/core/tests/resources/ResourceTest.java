/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *******************************************************************************/
package org.eclipse.core.tests.resources;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.tests.harness.CoreTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

/**
 * Superclass for tests that use the Eclipse Platform workspace.
 */
public abstract class ResourceTest extends CoreTest {

	/**
	 * For retrieving the test name when executing test class with JUnit 4.
	 */
	@Rule
	public final TestName testName = new TestName();

	@Rule
	public final WorkspaceTestRule workspaceTestRule = new WorkspaceTestRule();

	/**
	 * Need a zero argument constructor to satisfy the test harness.
	 * This constructor should not do any real work nor should it be
	 * called by user code.
	 */
	public ResourceTest() {
		super(null);
	}

	/**
	 * Creates a new ResourceTest
	 *
	 * @param name
	 *            name of the TestCase
	 */
	public ResourceTest(String name) {
		super(name);
	}

	@Override
	public String getName() {
		String name = super.getName();
		// Ensure that in case this test class is executed with JUnit 4 the test name
		// will not be null but retrieved via a TestName rule.
		if (name == null) {
			name = testName.getMethodName();
		}
		return name;
	}

	/**
	 * Bridge method to be able to run subclasses with JUnit4 as well as with
	 * JUnit3.
	 *
	 * @throws Exception
	 *             comes from {@link #setUp()}
	 */
	@Before
	public final void before() throws Exception {
		setUp();
	}

	/**
	 * Bridge method to be able to run subclasses with JUnit4 as well as with
	 * JUnit3.
	 *
	 * @throws Exception
	 *             comes from {@link #tearDown()}
	 */
	@After
	public final void after() throws Exception {
		tearDown();
	}

	/**
	 * Returns a FileStore instance backed by storage in a temporary location.
	 * The returned store will not exist, but will belong to an existing parent.
	 * The tearDown method in this class will ensure the location is deleted after
	 * the test is completed.
	 */
	protected IFileStore getTempStore() {
		return workspaceTestRule.getTempStore();
	}

	/**
	 * Ensures that the file system location associated with the corresponding path
	 * is deleted during test tear down.
	 */
	protected void deleteOnTearDown(IPath path) {
		workspaceTestRule.deleteOnTearDown(path);
	}

	/**
	 * Ensures that the given store is deleted during test tear down.
	 */
	protected void deleteOnTearDown(IFileStore store) {
		workspaceTestRule.deleteOnTearDown(store);
	}

	/**
	 * The environment should be set-up in the main method.
	 */
	@Override
	protected void setUp() throws Exception {
		workspaceTestRule.setTestName(getName());
		workspaceTestRule.before();
	}

	@Override
	protected void tearDown() throws Exception {
		boolean wasSuspended = resumeJobManagerIfNecessary();
		workspaceTestRule.after();
		assertFalse("This test stopped the JobManager, which could have affected other tests.", //
				wasSuspended);
	}

	private boolean resumeJobManagerIfNecessary() {
		if (Job.getJobManager().isSuspended()) {
			Job.getJobManager().resume();
			return true;
		}

		return false;
	}

}
