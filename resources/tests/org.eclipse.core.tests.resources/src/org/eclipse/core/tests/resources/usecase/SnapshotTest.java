/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
package org.eclipse.core.tests.resources.usecase;

import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.PI_RESOURCES_TESTS;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.harness.session.SessionTestExtension;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Runs all the snapshot usecase tests as a single session test.
 * Each test method will run a different snapshot test.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SnapshotTest {

	/** activities */
	static final String COMMENT_1 = "COMMENT ONE";
	static final String COMMENT_2 = "COMMENT TWO";

	/** project names */
	static final String PROJECT_1 = "MyProject";
	static final String PROJECT_2 = "Project2";

	@RegisterExtension
	static SessionTestExtension sessionTestExtension = SessionTestExtension.forPlugin(PI_RESOURCES_TESTS)
			.withCustomization(SessionTestExtension.createCustomWorkspace()).create();

	@Test
	@Order(1)
	public void test1() throws CoreException {
		Snapshot1Test test = new Snapshot1Test();
		test.testCreateMyProject();
		test.testCreateProject2();
		test.testSnapshotWorkspace();
	}

	@Test
	@Order(2)
	public void test2() throws CoreException {
		Snapshot2Test test = new Snapshot2Test();
		test.testVerifyPreviousSession();
		test.testChangeMyProject();
		test.testChangeProject2();
		test.testSnapshotWorkspace();
	}

	@Test
	@Order(3)
	public void test3() throws CoreException {
		Snapshot3Test test = new Snapshot3Test();
		test.testVerifyPreviousSession();
		test.testSaveWorkspace();
	}

	@Test
	@Order(4)
	public void test4() throws CoreException {
		Snapshot4Test test = new Snapshot4Test();
		test.testVerifyPreviousSession();
		test.testChangeMyProject();
		test.testChangeProject2();
	}

	@Test
	@Order(5)
	public void test5() throws CoreException {
		Snapshot5Test test = new Snapshot5Test();
		test.testVerifyPreviousSession();
	}

}
