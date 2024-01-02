/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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

import static org.eclipse.core.tests.resources.ResourceTestUtil.createInFileSystem;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.harness.PerformanceTestRunner;
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class BenchCopyFile {

	@Rule
	public TestName testName = new TestName();

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	private static final int COUNT = 5000;

	@Test
	public void testCopyFile() throws Exception {
		IFileStore input = workspaceRule.getTempStore();
		createInFileSystem(input);
		IFileStore[] output = new IFileStore[COUNT];
		for (int i = 0; i < output.length; i++) {
			output[i] = workspaceRule.getTempStore();
		}

		new PerformanceTestRunner() {
			int rep = 0;

			@Override
			protected void test() throws CoreException {
				input.copy(output[rep], EFS.NONE, null);
				rep++;
			}
		}.run(getClass(), testName.getMethodName(), 1, COUNT);
	}

}
