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

	public void testConstructor() {
		/* build scenario */
		IFileStore root = createStore();

		/* null location */
		boolean ok = false;
		try {
			new BlobStore(null, 0);
		} catch (RuntimeException e) {
			ok = true;
		}
		assertTrue("1.1", ok);

		/* nonexistent location */
		ok = false;
		try {
			new BlobStore(EFS.getLocalFileSystem().getStore(IPath.fromOSString("../this/path/should/not/be/a/folder")), 128);
		} catch (RuntimeException e) {
			ok = true;
		}
		assertTrue("3.1", ok);

		/* invalid limit values */
		ok = false;
		try {
			new BlobStore(root, 0);
		} catch (RuntimeException e) {
			ok = true;
		}
		assertTrue("4.1", ok);
		ok = false;
		try {
			new BlobStore(root, -1);
		} catch (RuntimeException e) {
			ok = true;
		}
		assertTrue("4.2", ok);
		ok = false;
		try {
			new BlobStore(root, 35);
		} catch (RuntimeException e) {
			ok = true;
		}
		assertTrue("4.3", ok);
		ok = false;
		try {
			new BlobStore(root, 512);
		} catch (RuntimeException e) {
			ok = true;
		}
		assertTrue("4.4", ok);
	}

	private IFileStore createStore() {
		IFileStore root = getTempStore();
		try {
			root.mkdir(EFS.NONE, null);
		} catch (CoreException e1) {
			fail("createStore.99", e1);
		}
		IFileInfo info = root.fetchInfo();
		assertTrue("createStore.1", info.exists());
		assertTrue("createStore.2", info.isDirectory());
		return root;
	}

	public void testDeleteBlob() {
		/* initialize common objects */
		IFileStore root = createStore();
		BlobStore store = new BlobStore(root, 64);

		/* delete blob that does not exist */
		UniversalUniqueIdentifier uuid = new UniversalUniqueIdentifier();
		assertTrue("2.1", !store.fileFor(uuid).fetchInfo().exists());
		store.deleteBlob(uuid);
		assertTrue("2.2", !store.fileFor(uuid).fetchInfo().exists());

		/* delete existing blob */
		IFileStore target = root.getChild("target");
		try {
			createFile(target, "bla bla bla");
			uuid = store.addBlob(target, true);
		} catch (CoreException e) {
			fail("4.1", e);
		}
		assertTrue("4.2", store.fileFor(uuid).fetchInfo().exists());
		store.deleteBlob(uuid);
		assertTrue("4.3", !store.fileFor(uuid).fetchInfo().exists());
	}

	public void testGetBlob() {
		/* initialize common objects */
		IFileStore root = createStore();
		BlobStore store = new BlobStore(root, 64);

		/* null UUID */
		boolean ok = false;
		try {
			store.getBlob(null);
		} catch (RuntimeException e) {
			ok = true;
		} catch (CoreException e) {
			fail("2.0", e);
		}
		assertTrue("2.1", ok);

		/* get existing blob */
		IFileStore target = root.getChild("target");
		UniversalUniqueIdentifier uuid = null;
		String content = "nothing important........tnatropmi gnihton";
		try {
			createFile(target, content);
			uuid = store.addBlob(target, true);
		} catch (CoreException e) {
			fail("3.1", e);
		}
		InputStream input = null;
		try {
			input = store.getBlob(uuid);
		} catch (CoreException e) {
			fail("3.4", e);
		}
		assertTrue("4.1", compareContent(getContents(content), input));
	}

	public void testSetBlob() {
		/* initialize common objects */
		IFileStore root = createStore();
		BlobStore store = new BlobStore(root, 64);

		/* normal conditions */
		IFileStore target = root.getChild("target");
		UniversalUniqueIdentifier uuid = null;
		String content = "nothing important........tnatropmi gnihton";
		try {
			createFile(target, content);
			uuid = store.addBlob(target, true);
		} catch (CoreException e) {
			fail("2.1", e);
		}
		InputStream input = null;
		try {
			input = store.getBlob(uuid);
		} catch (CoreException e) {
			fail("2.4", e);
		}
		assertTrue("2.5", compareContent(getContents(content), input));
	}
}
