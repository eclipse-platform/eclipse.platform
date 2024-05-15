/*******************************************************************************
 * Copyright (c) 2010, 2012 SAP AG and others.
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
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.internal.filesystem.FileCache;
import org.eclipse.core.internal.filesystem.local.LocalFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform.OS;
import org.eclipse.core.tests.harness.session.CustomSessionWorkspace;
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
	public static void restoreFileWriabilityForCleanup() throws CoreException, IOException {
		sessionWorkspace.getWorkspaceDirectory().resolve(READONLY_FILE_NAME).toFile().setWritable(true, false);
	}

	@Test
	@Order(1)
	public void test1() throws Exception {
		if (!OS.isMac()) {
			return;
		}

		IFileStore fileStore = EFS.getLocalFileSystem().getStore(getWorkspace().getRoot().getLocation())
				.getChild(READONLY_FILE_NAME);
		createInFileSystem(fileStore);

		// set EFS.ATTRIBUTE_READ_ONLY which also sets EFS.IMMUTABLE on Mac
		IFileInfo info = fileStore.fetchInfo();
		info.setAttribute(EFS.ATTRIBUTE_READ_ONLY, true);
		fileStore.putInfo(info, EFS.SET_ATTRIBUTES, createTestMonitor());

		// create a cached file
		File cachedFile = null;
		cachedFile = fileStore.toLocalFile(EFS.CACHE, createTestMonitor());

		IFileInfo cachedFileInfo = new LocalFile(cachedFile).fetchInfo();

		// check that the file in the cache has attributes set
		assertTrue(cachedFileInfo.getAttribute(EFS.ATTRIBUTE_READ_ONLY));
		assertTrue(cachedFileInfo.getAttribute(EFS.ATTRIBUTE_IMMUTABLE));
	}

	@Test
	@Order(2)
	public void test2() throws CoreException {
		if (!OS.isMac()) {
			return;
		}

		FileCache.getCache();
	}

}
