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

import java.io.File;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;

/**
 * Black box testing of {@link IFileStore#delete(int, org.eclipse.core.runtime.IProgressMonitor)}.
 */
public class DeleteTest extends FileSystemTest {

	public void testDeleteFile() {
		IFileStore file = baseStore.getChild("child");
		ensureExists(file, false);

		assertTrue("1.0", file.fetchInfo().exists());
		try {
			file.delete(EFS.NONE, getMonitor());
		} catch (CoreException e) {
			fail("1.99", e);
		}
		assertTrue("1.1", !file.fetchInfo().exists());
	}

	public void testDeleteDirectory() {
		IFileStore dir = baseStore.getChild("child");
		ensureExists(dir, true);

		assertTrue("1.0", dir.fetchInfo().exists());
		try {
			dir.delete(EFS.NONE, getMonitor());
		} catch (CoreException e) {
			fail("1.99", e);
		}
		assertTrue("1.1", !dir.fetchInfo().exists());
	}

	public void testDeleteReadOnlyFile() throws Exception {
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
