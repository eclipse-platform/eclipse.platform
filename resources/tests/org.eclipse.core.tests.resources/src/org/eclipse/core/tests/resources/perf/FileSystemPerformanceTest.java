/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
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

import java.io.IOException;
import java.util.Random;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.harness.PerformanceTestRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

/**
 * Automated performance tests for file system operations.
 */
public class FileSystemPerformanceTest {

	@Rule
	public TestName testName = new TestName();

	private static final String chars = "abcdefghijklmnopqrstuvwxyz";
	private static final int FILE_COUNT = 100;
	private static final int DIR_COUNT = 25;

	private static final int OUTER = 4;
	private static final int INNER = 1;

	private final Random random = new Random();
	private IFileStore baseStore;

	public String createString(int length) {
		StringBuilder buf = new StringBuilder(length);
		//fill the string with random characters up to the desired length
		for (int i = 0; i < length; i++) {
			buf.append(chars.charAt(random.nextInt(chars.length())));
		}
		return buf.toString();
	}

	void createStructure() throws CoreException, IOException {
		baseStore = EFS.getLocalFileSystem().getStore(getRandomLocation(getTempDir()));
		baseStore.mkdir(EFS.NONE, null);
		for (int i = 0; i < DIR_COUNT; i++) {
			IFileStore dir = baseStore.getChild(createString(8));
			dir.mkdir(EFS.SHALLOW, null);
			for (int j = 0; j < FILE_COUNT; j++) {
				IFileStore file = dir.getChild(createString(16));
				createInFileSystem(file);
			}
		}
	}

	void setAttributesOnTree() throws CoreException {
		IFileStore[] dirs = baseStore.childStores(EFS.NONE, null);
		for (IFileStore dir : dirs) {
			IFileStore[] files = dir.childStores(EFS.NONE, null);
			for (IFileStore file : files) {
				IFileInfo fileInfo = file.fetchInfo();
				boolean clear = fileInfo.getAttribute(EFS.ATTRIBUTE_READ_ONLY);
				fileInfo.setAttribute(EFS.ATTRIBUTE_EXECUTABLE | EFS.ATTRIBUTE_READ_ONLY, !clear);
				file.putInfo(fileInfo, EFS.SET_ATTRIBUTES, null);
			}
		}
	}

	@Test
	public void testPutFileInfo() throws Exception {
		createStructure();
		PerformanceTestRunner runner = new PerformanceTestRunner() {
			@Override
			protected void test() throws CoreException {
				setAttributesOnTree();
			}
		};
		runner.run(getClass(), testName.getMethodName(), OUTER, INNER);
		baseStore.delete(EFS.NONE, null);
	}

}
