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

import junit.framework.Test;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.tests.resources.AutomatedResourceTests;
import org.eclipse.core.tests.resources.WorkspaceSessionTest;
import org.eclipse.core.tests.session.WorkspaceSessionTestSuite;

/**
 * Runs all the snapshot usecase tests as a single session test.
 * Each test method will run a different snapshot test.
 */
public class SnapshotTest extends WorkspaceSessionTest {

	/** activities */
	static final String COMMENT_1 = "COMMENT ONE";
	static final String COMMENT_2 = "COMMENT TWO";

	/** project names */
	static final String PROJECT_1 = "MyProject";
	static final String PROJECT_2 = "Project2";

	public static Test suite() {
		return new WorkspaceSessionTestSuite(AutomatedResourceTests.PI_RESOURCES_TESTS, SnapshotTest.class);
	}

	private boolean skipTest() {
		//skip on Mac due to unknown failure (bug 127752)
		//TODO re-enable after M5 build
		return Platform.getOS().equals(Platform.OS_MACOSX);
	}

	public void test1() {
		if (skipTest()) {
			return;
		}
		Snapshot1Test test = new Snapshot1Test();
		test.testCreateMyProject();
		test.testCreateProject2();
		test.testSnapshotWorkspace();
	}

	public void test2() {
		if (skipTest()) {
			return;
		}
		Snapshot2Test test = new Snapshot2Test();
		test.testVerifyPreviousSession();
		test.testChangeMyProject();
		test.testChangeProject2();
		test.testSnapshotWorkspace();
	}

	public void test3() {
		if (skipTest()) {
			return;
		}
		Snapshot3Test test = new Snapshot3Test();
		test.testVerifyPreviousSession();
		test.testSaveWorkspace();
	}

	public void test4() {
		if (skipTest()) {
			return;
		}
		Snapshot4Test test = new Snapshot4Test();
		test.testVerifyPreviousSession();
		test.testChangeMyProject();
		test.testChangeProject2();
	}

	public void test5() {
		if (skipTest()) {
			return;
		}
		Snapshot5Test test = new Snapshot5Test();
		test.testVerifyPreviousSession();
		test.cleanUp();
	}

}
