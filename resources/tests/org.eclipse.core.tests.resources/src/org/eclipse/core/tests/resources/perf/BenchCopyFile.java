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

import static org.eclipse.core.tests.harness.FileSystemHelper.getRandomLocation;
import static org.eclipse.core.tests.harness.FileSystemHelper.getTempDir;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInFileSystem;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.harness.PerformanceTestRunner;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(WorkspaceResetExtension.class)
public class BenchCopyFile {

	private static final int COUNT = 5000;

	private TestInfo testInfo;
	private IFileStore fileStore;

	@BeforeEach
	void setUp(TestInfo info) {
		testInfo = info;
		fileStore = EFS.getLocalFileSystem().getStore(getRandomLocation(getTempDir()));
	}

	@AfterEach
	void tearDown() throws CoreException {
		fileStore.delete(EFS.NONE, null);
	}

	@Test
	public void testCopyFile() throws Exception {
		createInFileSystem(fileStore);
		IFileStore[] output = new IFileStore[COUNT];
		for (int i = 0; i < output.length; i++) {
			output[i] = fileStore;
		}

		new PerformanceTestRunner() {
			int rep = 0;

			@Override
			protected void test() throws CoreException {
				fileStore.copy(output[rep], EFS.NONE, null);
				rep++;
			}
		}.run(getClass(), testInfo.getDisplayName(), 1, COUNT);
	}

}
