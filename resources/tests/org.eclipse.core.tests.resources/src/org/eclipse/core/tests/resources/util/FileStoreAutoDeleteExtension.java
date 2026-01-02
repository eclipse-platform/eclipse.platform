/*******************************************************************************
 * Copyright (c) 2026 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.core.tests.resources.util;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.harness.FileSystemHelper.getRandomLocation;
import static org.eclipse.core.tests.harness.FileSystemHelper.getTempDir;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;

import java.util.HashSet;
import java.util.Set;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.IPath;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * A test extension that automatically deletes file stores that are either
 * retrieved from the extension itself or passed to it via one of the
 * {@code deleteOnTearDown()} methods.
 */
public class FileStoreAutoDeleteExtension implements AfterEachCallback {

	private final Set<IFileStore> storesToDelete = new HashSet<>();

	/**
	 * {@return a temporary file store backed by storage in a temporary location
	 * which will be automatically deleted when the test is completed}
	 */
	public IFileStore getTempStore() {
		IFileStore store = EFS.getLocalFileSystem().getStore(getRandomLocation(getTempDir()));
		deleteOnTearDown(store);
		return store;
	}

	/**
	 * Ensures that the store for the given path in the local file system is deleted
	 * when the test is completed.
	 *
	 * @param path
	 *            the path to the file store to delete after test execution
	 */
	public void deleteOnTearDown(IPath path) {
		storesToDelete.add(EFS.getLocalFileSystem().getStore(path));
	}

	/**
	 * Ensures that the given store is deleted when the test is completed.
	 *
	 * @param store
	 *            the store to delete after test execution
	 */
	public void deleteOnTearDown(IFileStore store) {
		storesToDelete.add(store);

	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		getWorkspace().run((IWorkspaceRunnable) monitor -> {
			getWorkspace().getRoot().delete(true, true, createTestMonitor());
			// clear stores in workspace runnable to avoid interaction with resource jobs
			for (IFileStore element : storesToDelete) {
				element.delete(EFS.NONE, null);
			}
			storesToDelete.clear();
		}, null);
	}

}
