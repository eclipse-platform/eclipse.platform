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

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.harness.PerformanceTestRunner;
import org.eclipse.core.tests.resources.ResourceTest;

public class BenchMiscWorkspace extends ResourceTest {
	/**
	 * Benchmarks performing many empty operations.
	 */
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
			protected void test() {
				try {
					ws.run(noop, null);
				} catch (CoreException e) {
					fail("0.0", e);
				}
			}
		}.run(this, 10, 100000);
	}

	public void testGetProject() {
		new PerformanceTestRunner() {
			@Override
			protected void test() {
				IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
				for (int i = 0; i < 2000; i++) {
					root.getProject(Integer.toString(i));
				}
			}
		}.run(this, 10, 1000);

	}
}
