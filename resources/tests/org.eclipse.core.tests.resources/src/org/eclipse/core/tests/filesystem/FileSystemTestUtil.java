/*******************************************************************************
 * Copyright (c) 2023 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package org.eclipse.core.tests.filesystem;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.tests.harness.FussyProgressMonitor;

/**
 *
 */
final class FileSystemTestUtil {

	private FileSystemTestUtil() {
	}

	static void ensureDoesNotExist(IFileStore store) throws CoreException {
		store.delete(EFS.NONE, getMonitor());
		assertTrue("store was not properly deleted: " + store, !store.fetchInfo().exists());
	}

	/**
	 * Ensures that the provided store exists, as either a file or directory.
	 */
	static void ensureExists(IFileStore store, boolean directory) throws CoreException, IOException {
		if (directory) {
			store.mkdir(EFS.NONE, getMonitor());
			final IFileInfo info = store.fetchInfo();
			assertTrue("file info for store does not exist: " + store, info.exists());
			assertTrue("created file for store is not a directory: " + store, info.isDirectory());
		} else {
			try (OutputStream out = store.openOutputStream(EFS.NONE, getMonitor())) {
				out.write(5);
			}
			final IFileInfo info = store.fetchInfo();
			assertTrue("file info for store does not exist: " + store, info.exists());
			assertTrue("created file for store is not a directory: " + store, !info.isDirectory());
		}
	}

	static IProgressMonitor getMonitor() {
		return new FussyProgressMonitor();
	}

}
