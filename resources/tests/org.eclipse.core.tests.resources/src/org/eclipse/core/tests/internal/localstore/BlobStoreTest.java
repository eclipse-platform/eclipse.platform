/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
package org.eclipse.core.tests.internal.localstore;

import static org.junit.Assert.assertThrows;

import java.io.InputStream;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.internal.localstore.BlobStore;
import org.eclipse.core.internal.utils.UniversalUniqueIdentifier;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

//
public class BlobStoreTest extends LocalStoreTest {

	public void testConstructor() throws CoreException {
		/* build scenario */
		IFileStore root = createStore();

		/* null location */
		assertThrows(RuntimeException.class, () -> new BlobStore(null, 0));

		/* nonexistent location */
		assertThrows(RuntimeException.class, () -> new BlobStore(
						EFS.getLocalFileSystem().getStore(IPath.fromOSString("../this/path/should/not/be/a/folder")),
						128));

		/* invalid limit values */
		assertThrows(RuntimeException.class, () -> new BlobStore(root, 0));

		assertThrows(RuntimeException.class, () -> new BlobStore(root, -1));

		assertThrows(RuntimeException.class, () -> new BlobStore(root, 35));

		assertThrows(RuntimeException.class, () -> new BlobStore(root, 512));
	}

	private IFileStore createStore() throws CoreException {
		IFileStore root = getTempStore();
		root.mkdir(EFS.NONE, null);
		IFileInfo info = root.fetchInfo();
		assertTrue("createStore.1", info.exists());
		assertTrue("createStore.2", info.isDirectory());
		return root;
	}

	public void testDeleteBlob() throws CoreException {
		/* initialize common objects */
		IFileStore root = createStore();
		BlobStore store = new BlobStore(root, 64);

		/* delete blob that does not exist */
		UniversalUniqueIdentifier uuid = new UniversalUniqueIdentifier();
		assertTrue(!store.fileFor(uuid).fetchInfo().exists());
		store.deleteBlob(uuid);
		assertTrue(!store.fileFor(uuid).fetchInfo().exists());

		/* delete existing blob */
		IFileStore target = root.getChild("target");
		createFile(target, "bla bla bla");
		uuid = store.addBlob(target, true);
		assertTrue(store.fileFor(uuid).fetchInfo().exists());
		store.deleteBlob(uuid);
		assertFalse(store.fileFor(uuid).fetchInfo().exists());
	}

	public void testGetBlob() throws CoreException {
		/* initialize common objects */
		IFileStore root = createStore();
		BlobStore store = new BlobStore(root, 64);

		/* null UUID */
		assertThrows(RuntimeException.class, () -> store.getBlob(null));

		/* get existing blob */
		IFileStore target = root.getChild("target");
		UniversalUniqueIdentifier uuid = null;
		String content = "nothing important........tnatropmi gnihton";
		createFile(target, content);
		uuid = store.addBlob(target, true);
		InputStream input = store.getBlob(uuid);
		assertTrue(compareContent(getContents(content), input));
	}

	public void testSetBlob() throws CoreException {
		/* initialize common objects */
		IFileStore root = createStore();
		BlobStore store = new BlobStore(root, 64);

		/* normal conditions */
		IFileStore target = root.getChild("target");
		UniversalUniqueIdentifier uuid = null;
		String content = "nothing important........tnatropmi gnihton";
		createFile(target, content);
		uuid = store.addBlob(target, true);
		InputStream input = store.getBlob(uuid);
		assertTrue(compareContent(getContents(content), input));
	}
}
