/*******************************************************************************
 *  Copyright (c) 2018 Simeon Andreev and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Simeon Andreev - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.resources.regression;

import static org.junit.Assert.assertNotEquals;

import java.io.ByteArrayInputStream;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.internal.filesystem.local.LocalFileNativesManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.tests.resources.ResourceTest;


/**
 * Test for Bug 530868: millisecond resolution of file timestamps with native
 * provider.
 */
public class Bug_530868 extends ResourceTest {

	private IProject testProject;
	private IFile testFile;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		testProject = getWorkspace().getRoot().getProject(Bug_530868.class + "TestProject");
		testProject.create(getMonitor());
		testProject.open(getMonitor());
		testFile = testProject.getFile(getName());

	}

	/**
	 * Create a file several times and check that we see different modification
	 * timestamps.
	 */
	public void testMillisecondResolution() throws Exception {
		try {
			assertTrue("can only run if native provider can be enabled", LocalFileNativesManager.setUsingNative(true));
			if (Platform.OS_MACOSX.equals(Platform.getOS())) {
				// Mac still has no milliseconds resolution
				return;
			}
			/*
			 * Run 3 times in case we have seconds resolution due to a bug, but by chance we
			 * happened to modify the file in-between two seconds.
			 */
			long timestamp1 = modifyTestFileAndFetchTimestamp("some contents 1");
			Thread.sleep(50);
			long timestamp2 = modifyTestFileAndFetchTimestamp("some contents 2");
			Thread.sleep(50);
			long timestamp3 = modifyTestFileAndFetchTimestamp("some contents 3");

			String failMessage = "expected different timestamps for modifications in quick succession";
			assertNotEquals(failMessage, timestamp1, timestamp2);
			assertNotEquals(failMessage, timestamp2, timestamp3);
		} finally {
			LocalFileNativesManager.reset();
		}
	}

	private long modifyTestFileAndFetchTimestamp(String contents) throws Exception {
		setTestFileContents(contents);
		long timestamp = getLastModificationTimestamp();
		return timestamp;
	}

	private void setTestFileContents(String contents) throws Exception {
		ByteArrayInputStream contentsStream = new ByteArrayInputStream(String.valueOf(contents).getBytes());
		if (testFile.exists()) {
			testFile.delete(true, getMonitor());
		}
		testFile.create(contentsStream, true, getMonitor());
	}

	private long getLastModificationTimestamp() {
		IPath testFileLocation = testFile.getLocation();
		String filePath = testFileLocation.toOSString();
		FileInfo testFileInfo = LocalFileNativesManager.fetchFileInfo(filePath);
		return testFileInfo.getLastModified();
	}
}
