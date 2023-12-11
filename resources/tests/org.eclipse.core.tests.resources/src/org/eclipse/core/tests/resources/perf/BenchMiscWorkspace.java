/*******************************************************************************
 *  Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.core.tests.resources.perf;

import static org.eclipse.core.tests.resources.ResourceTestUtil.waitForBuild;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.harness.PerformanceTestRunner;
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class BenchMiscWorkspace {

	@Rule
	public TestName testName = new TestName();

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	/**
	 * Benchmarks performing many empty operations.
	 */
	@Test
	public void testNoOp() throws Exception {
		final IWorkspace ws = ResourcesPlugin.getWorkspace();
		final IWorkspaceRunnable noop = monitor -> {
		};
		//run a first operation to make sure no other jobs are running before starting timer
		ws.run(noop, null);
		waitForBuild();
		//now start the test
		new PerformanceTestRunner() {
			@Override
			protected void test() throws CoreException {
				ws.run(noop, null);
			}
		}.run(getClass(), testName.getMethodName(), 10, 100000);
	}

	@Test
	public void testGetProject() throws CoreException {
		new PerformanceTestRunner() {
			@Override
			protected void test() {
				IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
				for (int i = 0; i < 2000; i++) {
					root.getProject(Integer.toString(i));
				}
			}
		}.run(getClass(), testName.getMethodName(), 10, 1000);
	}

}
