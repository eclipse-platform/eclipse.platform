/*******************************************************************************
 *  Copyright (c) 2005, 2025 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.filesystem;

import static org.eclipse.core.tests.filesystem.FileSystemTestUtil.ensureExists;
import static org.eclipse.core.tests.filesystem.FileSystemTestUtil.getMonitor;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.tests.filesystem.FileStoreCreationExtension.FileSystemType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Black box testing of {@link IFileStore#delete(int, org.eclipse.core.runtime.IProgressMonitor)}.
 */
public class DeleteTest {
	@RegisterExtension
	public final FileStoreCreationExtension localFileStoreExtension = new FileStoreCreationExtension(
			FileSystemType.LOCAL);

	@RegisterExtension
	public final FileStoreCreationExtension inMemoryFileStoreExtension = new FileStoreCreationExtension(
			FileSystemType.IN_MEMORY);

	@Test
	public void testDeleteFile() throws Exception {
		IFileStore baseStore = inMemoryFileStoreExtension.getFileStore();
		IFileStore file = baseStore.getChild("child");
		ensureExists(file, false);

		assertTrue(file.fetchInfo().exists());
		file.delete(EFS.NONE, getMonitor());
		assertFalse(file.fetchInfo().exists());
	}

	@Test
	public void testDeleteDirectory() throws Exception {
		IFileStore baseStore = inMemoryFileStoreExtension.getFileStore();
		IFileStore dir = baseStore.getChild("child");
		ensureExists(dir, true);

		assertTrue(dir.fetchInfo().exists());
		dir.delete(EFS.NONE, getMonitor());
		assertFalse(dir.fetchInfo().exists());
	}

	@Test
	public void testDeleteReadOnlyFile() throws Exception {
		IFileStore localFileBaseStore = localFileStoreExtension.getFileStore();
		ensureExists(localFileBaseStore, true);
		IFileStore file = localFileBaseStore.getChild("child");
		ensureExists(file, false);
		assertTrue(file.fetchInfo().exists());
		ensureReadOnlyLocal(file);
		file.delete(EFS.NONE, getMonitor());
		// success: we expect that read-only files can be removed
		assertFalse(file.fetchInfo().exists());
	}

	/**
	 * Ensures that the provided store is read-only
	 */
	protected void ensureReadOnlyLocal(IFileStore store) throws Exception {
		File localFile = store.toLocalFile(0, getMonitor());
		boolean readOnly = localFile.setReadOnly();
		assertTrue(readOnly);
		assertFalse(localFile.canWrite());
	}
}
