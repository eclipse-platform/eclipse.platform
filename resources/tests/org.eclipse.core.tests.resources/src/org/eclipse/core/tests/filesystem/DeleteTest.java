/*******************************************************************************
 *  Copyright (c) 2005, 2012 IBM Corporation and others.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.tests.filesystem.FileStoreCreationRule.FileSystemType;
import org.junit.Rule;
import org.junit.Test;

/**
 * Black box testing of {@link IFileStore#delete(int, org.eclipse.core.runtime.IProgressMonitor)}.
 */
public class DeleteTest {
	@Rule
	public final FileStoreCreationRule localFileStoreRule = new FileStoreCreationRule(FileSystemType.LOCAL);

	@Rule
	public final FileStoreCreationRule inMemoryFileStoreRule = new FileStoreCreationRule(FileSystemType.IN_MEMORY);

	@Test
	public void testDeleteFile() throws Exception {
		IFileStore baseStore = inMemoryFileStoreRule.getFileStore();
		IFileStore file = baseStore.getChild("child");
		ensureExists(file, false);

		assertTrue("1.0", file.fetchInfo().exists());
		file.delete(EFS.NONE, getMonitor());
		assertTrue("1.1", !file.fetchInfo().exists());
	}

	@Test
	public void testDeleteDirectory() throws Exception {
		IFileStore baseStore = inMemoryFileStoreRule.getFileStore();
		IFileStore dir = baseStore.getChild("child");
		ensureExists(dir, true);

		assertTrue("1.0", dir.fetchInfo().exists());
		dir.delete(EFS.NONE, getMonitor());
		assertTrue("1.1", !dir.fetchInfo().exists());
	}

	@Test
	public void testDeleteReadOnlyFile() throws Exception {
		IFileStore localFileBaseStore = localFileStoreRule.getFileStore();
		ensureExists(localFileBaseStore, true);
		IFileStore file = localFileBaseStore.getChild("child");
		ensureExists(file, false);
		assertTrue("1.0", file.fetchInfo().exists());
		ensureReadOnlyLocal(file);
		file.delete(EFS.NONE, getMonitor());
		// success: we expect that read-only files can be removed
		assertTrue("1.1", !file.fetchInfo().exists());
	}

	/**
	 * Ensures that the provided store is read-only
	 */
	protected void ensureReadOnlyLocal(IFileStore store) throws Exception {
		File localFile = store.toLocalFile(0, getMonitor());
		boolean readOnly = localFile.setReadOnly();
		assertTrue("1.0", readOnly);
		assertFalse("1.1", localFile.canWrite());
	}
}
