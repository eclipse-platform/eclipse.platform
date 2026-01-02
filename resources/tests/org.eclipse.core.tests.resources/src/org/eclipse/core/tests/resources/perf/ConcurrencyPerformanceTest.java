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
 *******************************************************************************/
package org.eclipse.core.tests.resources.perf;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.harness.PerformanceTestRunner;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(WorkspaceResetExtension.class)
public class ConcurrencyPerformanceTest {

	private TestInfo testInfo;

	@BeforeEach
	void storeTestInfo(TestInfo info) {
		testInfo = info;
	}

	@Test
	public void testSimpleCalls() throws Exception {
		final IWorkspaceRunnable job = monitor -> {
			// do nothing
		};
		new PerformanceTestRunner() {
			@Override
			protected void test() throws CoreException {
				getWorkspace().run(job, null);
			}
		}.run(getClass(), testInfo.getDisplayName(), 10, 50);
	}

}
