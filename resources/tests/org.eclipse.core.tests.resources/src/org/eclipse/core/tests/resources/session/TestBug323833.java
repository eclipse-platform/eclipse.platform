/*******************************************************************************
 * Copyright (c) 2010, 2024 SAP AG and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     SAP AG - initial API and implementation
 *     IBM Corporation - ongoing development
 *******************************************************************************/
package org.eclipse.core.tests.resources.session;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.PI_RESOURCES_TESTS;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.internal.filesystem.FileCache;
import org.eclipse.core.internal.filesystem.local.LocalFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform.OS;
import org.eclipse.core.tests.harness.session.CustomSessionWorkspace;
import org.eclipse.core.tests.harness.session.ExecuteInHost;
import org.eclipse.core.tests.harness.session.SessionTestExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Test for bug 323833
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestBug323833 {
	private static final String READONLY_FILE_NAME = "test";

	private static CustomSessionWorkspace sessionWorkspace = SessionTestExtension.createCustomWorkspace();

	@RegisterExtension
	static SessionTestExtension sessionTestExtension = SessionTestExtension.forPlugin(PI_RESOURCES_TESTS)
			.withCustomization(sessionWorkspace).create();

	@AfterAll
	@ExecuteInHost
	public static void restoreFileWriabilityForCleanup() throws CoreException, IOException {
		Path workspaceDirectory = sessionWorkspace.getWorkspaceDirectory();
		for (int i = 1; i <= 2; i++) {
			workspaceDirectory.resolve(READONLY_FILE_NAME + i).toFile().setWritable(true, false);
		}
	}

	@Test
	@Order(1)
	void test1_smallFile() throws Exception {
		test1(10, READONLY_FILE_NAME + 1);
	}

	@Test
	@Order(2)
	void test1_largeFile() throws Exception {
		test1(LocalFile.LARGE_FILE_SIZE_THRESHOLD + 10, READONLY_FILE_NAME + 2);
	}

	private void test1(int fileSize, String filename) throws Exception {
		if (!OS.isMac()) {
			return;
		}

		IPath workspaceRootLocation = getWorkspace().getRoot().getLocation();
		IFileStore fileStore = EFS.getLocalFileSystem().getStore(workspaceRootLocation).getChild(filename);
		createInFileSystem(fileStore, fileSize);

		// set EFS.ATTRIBUTE_READ_ONLY which also sets EFS.IMMUTABLE on Mac
		IFileInfo info = fileStore.fetchInfo();
		info.setAttribute(EFS.ATTRIBUTE_READ_ONLY, true);
		fileStore.putInfo(info, EFS.SET_ATTRIBUTES, createTestMonitor());

		// create a cached file
		File cachedFile = fileStore.toLocalFile(EFS.CACHE, createTestMonitor());
		IFileInfo cachedFileInfo = new LocalFile(cachedFile).fetchInfo();

		// check that the file in the cache has attributes set
		assertTrue(cachedFileInfo.getAttribute(EFS.ATTRIBUTE_READ_ONLY));
		assertTrue(cachedFileInfo.getAttribute(EFS.ATTRIBUTE_IMMUTABLE));
	}

	@Test
	@Order(3)
	public void test2() throws CoreException {
		if (!OS.isMac()) {
			return;
		}

		FileCache.getCache();
	}

}
