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

import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.PI_RESOURCES_TESTS;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createUniqueString;

import java.io.File;
import junit.framework.Test;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.internal.filesystem.FileCache;
import org.eclipse.core.internal.filesystem.local.LocalFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.tests.resources.WorkspaceSessionTest;
import org.eclipse.core.tests.session.WorkspaceSessionTestSuite;

/**
 * Test for bug 323833
 */
public class TestBug323833 extends WorkspaceSessionTest {
	public void test1() throws CoreException {
		if (!Platform.getOS().equals(Platform.OS_MACOSX)) {
			return;
		}

		IFileStore fileStore = getTempStore().getChild(createUniqueString());
		createFileInFileSystem(fileStore);

		// set EFS.ATTRIBUTE_READ_ONLY which also sets EFS.IMMUTABLE on Mac
		IFileInfo info = fileStore.fetchInfo();
		info.setAttribute(EFS.ATTRIBUTE_READ_ONLY, true);
		fileStore.putInfo(info, EFS.SET_ATTRIBUTES, createTestMonitor());

		// create a cached file
		File cachedFile = null;
		cachedFile = fileStore.toLocalFile(EFS.CACHE, createTestMonitor());

		IFileInfo cachedFileInfo = new LocalFile(cachedFile).fetchInfo();

		// check that the file in the cache has attributes set
		assertTrue("3.0", cachedFileInfo.getAttribute(EFS.ATTRIBUTE_READ_ONLY));
		assertTrue("4.0", cachedFileInfo.getAttribute(EFS.ATTRIBUTE_IMMUTABLE));
	}

	public void test2() throws CoreException {
		if (!Platform.getOS().equals(Platform.OS_MACOSX)) {
			return;
		}

		FileCache.getCache();
	}

	public static Test suite() {
		return new WorkspaceSessionTestSuite(PI_RESOURCES_TESTS, TestBug323833.class);
	}
}
