/*******************************************************************************
 *  Copyright (c) 2000, 2023 IBM Corporation and others.
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
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.IFileSystem;
import org.eclipse.core.filesystem.provider.FileSystem;
import org.eclipse.core.internal.filesystem.Messages;
import org.eclipse.core.internal.filesystem.NullFileSystem;
import org.eclipse.core.internal.filesystem.local.LocalFile;
import org.eclipse.core.internal.filesystem.local.LocalFileSystem;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.tests.internal.localstore.LocalStoreTest;
import org.eclipse.osgi.util.NLS;

/**
 * Basic tests for the IFileStore API
 */
public class FileStoreTest extends LocalStoreTest {
	private IFileStore createDir(IFileStore store, boolean clear) throws CoreException {
		if (clear && store.fetchInfo().exists()) {
			store.delete(EFS.NONE, null);
		}
		store.mkdir(EFS.NONE, null);
		deleteOnTearDown(store);
		IFileInfo info = store.fetchInfo();
		assertTrue("createDir.1", info.exists());
		assertTrue("createDir.1", info.isDirectory());
		return store;
	}

	private IFileStore createDir(String string, boolean clear) throws CoreException {
		return createDir(EFS.getFileSystem(EFS.SCHEME_FILE).getStore(new Path(string)), clear);
	}

	/**
	 * Tests behavior of IFileStore#fetchInfo when underlying file system throws
	 * exceptions.
	 */
	public void testBrokenFetchInfo() {
		IFileStore broken = null;
		try {
			broken = EFS.getStore(new URI("broken://a/b/c"));
		} catch (CoreException e) {
			fail("0.98", e);
		} catch (URISyntaxException e) {
			fail("0.99", e);
		}
		// no-arg fetch info should return non-existent file
		IFileInfo info = broken.fetchInfo();
		assertTrue("1.0", !info.exists());

		// two-arg fetchInfo should throw exception
		try {
			info = broken.fetchInfo(EFS.NONE, getMonitor());
			fail("2.0");
		} catch (CoreException e) {
			// expected
		}
	}

	private IFileStore getDirFileStore(String path) throws CoreException {
		IFileStore store = EFS.getFileSystem(EFS.SCHEME_FILE).getStore(new Path(path));
		if (!store.toLocalFile(EFS.NONE, getMonitor()).exists()) {
			store.mkdir(EFS.NONE, null);
			deleteOnTearDown(store);
		}
		return store;
	}

	private IFileStore[] getFileStoresOnTwoVolumes() {
		IFileStore[] tempDirs = new IFileStore[2];

		for (int i = 99/* c */; i < 123/* z */; i++) {
			char c = (char) i;
			try {
				IFileStore store = getDirFileStore(c + ":/temp");
				IFileInfo info = store.fetchInfo();
				if (info.exists() && info.isDirectory() && !info.getAttribute(EFS.ATTRIBUTE_READ_ONLY)) {
					if (tempDirs[0] == null) {
						tempDirs[0] = store;
					} else {
						tempDirs[1] = store;
						break; // both temp dirs have been created
					}
				}
			} catch (CoreException e) {// ignore and go to next volume
				continue;
			}
		}
		return tempDirs;
	}

	/**
	 * Basically this is a test for the Windows Platform.
	 */
	public void testCopyAcrossVolumes() throws Throwable {
		IFileStore[] tempDirectories = getFileStoresOnTwoVolumes();

		/* test if we are in the adequate environment */
		if (tempDirectories == null || tempDirectories.length < 2 || tempDirectories[0] == null
				|| tempDirectories[1] == null) {
			return;
		}

		/* build scenario */
		// create source root folder
		IFileStore tempSrc = tempDirectories[0];
		/* get the destination folder */
		IFileStore tempDest = tempDirectories[1];

		// create tree
		String subfolderName = "target_" + System.currentTimeMillis();

		IFileStore target = tempSrc.getChild(subfolderName);
		createDir(target, true);
		createTree(getTree(target));

		/* c:\temp\target -> d:\temp\target */
		IFileStore destination = tempDest.getChild(subfolderName);
		deleteOnTearDown(destination);
		target.copy(destination, EFS.NONE, null);
		assertTrue("3.1", verifyTree(getTree(destination)));
		destination.delete(EFS.NONE, null);

		/* c:\temp\target -> d:\temp\copy of target */
		String copyOfSubfolderName = "copy of " + subfolderName;
		destination = tempDest.getChild(copyOfSubfolderName);
		deleteOnTearDown(destination);
		target.copy(destination, EFS.NONE, null);
		assertTrue("4.1", verifyTree(getTree(destination)));
		destination.delete(EFS.NONE, null);

		/* c:\temp\target -> d:\temp\target (but the destination is already a file) */
		destination = tempDest.getChild(subfolderName);
		deleteOnTearDown(destination);
		String anotherContent = "nothing..................gnihton";
		createFile(destination, anotherContent);
		assertTrue("5.1", !destination.fetchInfo().isDirectory());
		try {
			target.copy(destination, EFS.NONE, null);
			fail("5.2");
		} catch (CoreException e) {
			// should fail
		}
		assertTrue("5.3", !verifyTree(getTree(destination)));
		destination.delete(EFS.NONE, null);

		/* c:\temp\target -> d:\temp\target (but the destination is already a folder */
		destination = tempDest.getChild(subfolderName);
		createDir(destination, true);
		target.copy(destination, EFS.NONE, null);
		assertTrue("6.2", verifyTree(getTree(destination)));
		destination.delete(EFS.NONE, null);
	}

	public void testCopyDirectory() throws Throwable {
		/* build scenario */
		IFileStore temp = EFS.getFileSystem(EFS.SCHEME_FILE)
				.getStore(getWorkspace().getRoot().getLocation().append("temp"));
		deleteOnTearDown(temp);
		temp.mkdir(EFS.NONE, null);
		assertTrue("1.1", temp.fetchInfo().isDirectory());
		// create tree
		IFileStore target = temp.getChild("target");
		target.delete(EFS.NONE, null);
		createTree(getTree(target));

		/* temp\target -> temp\copy of target */
		IFileStore copyOfTarget = temp.getChild("copy of target");
		target.copy(copyOfTarget, EFS.NONE, null);
		assertTrue("2.1", verifyTree(getTree(copyOfTarget)));
	}

	public void testCopyDirectoryParentMissing() throws Throwable {
		IFileStore parent = getTempStore();
		IFileStore child = parent.getChild("child");
		IFileStore existing = getTempStore();
		createFile(existing, getRandomString());
		// try to copy when parent of destination does not exist
		try {
			existing.copy(child, EFS.NONE, getMonitor());
			fail("1.0");
		} catch (CoreException e) {
			// should fail
		}
		// destination should not exist
		assertTrue("1.1", !child.fetchInfo().exists());
	}

	public void testCaseInsensitive() throws Throwable {
		IFileStore temp = createDir(getWorkspace().getRoot().getLocation().append("temp").toString(), true);
		boolean isCaseSensitive = temp.getFileSystem().isCaseSensitive();
		if (isCaseSensitive) {
			System.out.println("Skipping copy test on caseSensitive System");
			return;
		}
		// create a file
		String content = "this is just a simple content \n to a simple file \n to test a 'simple' copy";
		IFileStore fileWithSmallName = temp.getChild("filename");
		fileWithSmallName.delete(EFS.NONE, null);
		createFile(fileWithSmallName, content);
		System.out.println(fileWithSmallName.fetchInfo().getName());
		assertTrue("1.3", fileWithSmallName.fetchInfo().exists());
		assertTrue("1.4", compareContent(getContents(content), fileWithSmallName.openInputStream(EFS.NONE, null)));

		IFileStore fileWithOtherName = temp.getChild("FILENAME");
		System.out.println(fileWithOtherName.fetchInfo().getName());
		// file content is already the same for both Cases:
		assertTrue("2.0", compareContent(getContents(content), fileWithOtherName.openInputStream(EFS.NONE, null)));
		fileWithSmallName.copy(fileWithOtherName, IResource.DEPTH_INFINITE, null); // a NOP Operation
		// file content is still the same for both Cases:
		assertTrue("2.1", compareContent(getContents(content), fileWithOtherName.openInputStream(EFS.NONE, null)));
		assertTrue("3.0", fileWithOtherName.fetchInfo().exists());
		assertTrue("3.1", fileWithSmallName.fetchInfo().exists());
		fileWithOtherName.delete(EFS.NONE, null);
		assertFalse("3.2", fileWithOtherName.fetchInfo().exists());
		assertFalse("3.3", fileWithSmallName.fetchInfo().exists());
		try {
			fileWithSmallName.move(fileWithOtherName, EFS.NONE, null);
			fail("4.0");
		} catch (CoreException e) {
			String message = NLS.bind(Messages.couldNotMove, fileWithSmallName.toString());
			assertEquals(message, e.getMessage());
		}
	}

	public void testCopyFile() throws Throwable {
		/* build scenario */
		IFileStore temp = createDir(getWorkspace().getRoot().getLocation().append("temp").toString(), true);
		// create target
		String content = "this is just a simple content \n to a simple file \n to test a 'simple' copy";
		IFileStore target = temp.getChild("target");
		target.delete(EFS.NONE, null);
		createFile(target, content);
		assertTrue("1.3", target.fetchInfo().exists());
		assertTrue("1.4", compareContent(getContents(content), target.openInputStream(EFS.NONE, null)));

		/* temp\target -> temp\copy of target */
		IFileStore copyOfTarget = temp.getChild("copy of target");
		target.copy(copyOfTarget, IResource.DEPTH_INFINITE, null);
		assertTrue("2.1", compareContent(getContents(content), copyOfTarget.openInputStream(EFS.NONE, null)));
		copyOfTarget.delete(EFS.NONE, null);

		// We need to know whether or not we can unset the read-only flag
		// in order to perform this part of the test.
		if (isReadOnlySupported()) {
			/* make source read-only and try the copy temp\target -> temp\copy of target */
			copyOfTarget = temp.getChild("copy of target");
			setReadOnly(target, true);

			target.copy(copyOfTarget, IResource.DEPTH_INFINITE, null);
			assertTrue("3.1", compareContent(getContents(content), copyOfTarget.openInputStream(EFS.NONE, null)));
			// reset read only flag for cleanup
			setReadOnly(copyOfTarget, false);
			copyOfTarget.delete(EFS.NONE, null);
			// reset the read only flag for cleanup
			setReadOnly(target, false);
			target.delete(EFS.NONE, null);
		}

		/* copy a big file to test progress monitor */
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 1000; i++) {
			sb.append("asdjhasldhaslkfjhasldkfjhasdlkfjhasdlfkjhasdflkjhsdaf");
		}
		IFileStore bigFile = temp.getChild("bigFile");
		createFile(bigFile, sb.toString());
		assertTrue("7.1", bigFile.fetchInfo().exists());
		assertTrue("7.2", compareContent(getContents(sb.toString()), bigFile.openInputStream(EFS.NONE, null)));
		IFileStore destination = temp.getChild("copy of bigFile");
		// IProgressMonitor monitor = new LoggingProgressMonitor(System.out);
		IProgressMonitor monitor = getMonitor();
		bigFile.copy(destination, EFS.NONE, monitor);
		assertTrue("7.3", compareContent(getContents(sb.toString()), destination.openInputStream(EFS.NONE, null)));
		destination.delete(EFS.NONE, null);
	}

	/**
	 * Basically this is a test for the Windows Platform.
	 */
	public void testCopyFileAcrossVolumes() throws Throwable {
		IFileStore[] tempDirectories = getFileStoresOnTwoVolumes();

		/* test if we are in the adequate environment */
		if (tempDirectories == null || tempDirectories.length < 2 || tempDirectories[0] == null
				|| tempDirectories[1] == null) {
			return;
		}

		/* build scenario */
		/* get the source folder */
		IFileStore tempSrc = tempDirectories[0];
		/* get the destination folder */
		IFileStore tempDest = tempDirectories[1];
		// create target
		String content = "this is just a simple content \n to a simple file \n to test a 'simple' copy";
		String subfolderName = "target_" + System.currentTimeMillis();

		IFileStore target = tempSrc.getChild(subfolderName);
		target.delete(EFS.NONE, null);
		createFile(target, content);
		deleteOnTearDown(target);
		assertTrue("1.3", target.fetchInfo().exists());
		assertTrue("1.4", compareContent(getContents(content), target.openInputStream(EFS.NONE, null)));

		/* c:\temp\target -> d:\temp\target */
		IFileStore destination = tempDest.getChild(subfolderName);
		deleteOnTearDown(destination);
		target.copy(destination, IResource.DEPTH_INFINITE, null);
		assertTrue("3.1", compareContent(getContents(content), destination.openInputStream(EFS.NONE, null)));
		destination.delete(EFS.NONE, null);

		/* c:\temp\target -> d:\temp\copy of target */
		String copyOfSubfoldername = "copy of " + subfolderName;
		destination = tempDest.getChild(copyOfSubfoldername);
		deleteOnTearDown(destination);
		target.copy(destination, IResource.DEPTH_INFINITE, null);
		assertTrue("4.1", compareContent(getContents(content), destination.openInputStream(EFS.NONE, null)));
		destination.delete(EFS.NONE, null);

		/* c:\temp\target -> d:\temp\target (but the destination is already a file */
		destination = tempDest.getChild(subfolderName);
		deleteOnTearDown(destination);
		String anotherContent = "nothing..................gnihton";
		createFile(destination, anotherContent);
		assertTrue("5.1", !destination.fetchInfo().isDirectory());
		target.copy(destination, IResource.DEPTH_INFINITE, null);
		assertTrue("5.2", compareContent(getContents(content), destination.openInputStream(EFS.NONE, null)));
		destination.delete(EFS.NONE, null);

		/* c:\temp\target -> d:\temp\target (but the destination is already a folder */
		destination = tempDest.getChild(subfolderName);
		createDir(destination, true);
		assertTrue("6.1", destination.fetchInfo().isDirectory());
		boolean ok = false;
		try {
			target.copy(destination, EFS.NONE, null);
		} catch (CoreException e) {
			/* test if the input stream inside the copy method was closed */
			target.delete(EFS.NONE, null);
			createFile(target, content);
			ok = true;
		}
		assertTrue("6.2", ok);
		assertTrue("6.3", destination.fetchInfo().isDirectory());
		destination.delete(EFS.NONE, null);
	}

	public void testGetLength() throws Exception {
		// evaluate test environment
		IPath root = getWorkspace().getRoot().getLocation().append("" + new Date().getTime());
		IFileStore temp = createDir(root.toString(), true);
		// create common objects
		IFileStore target = temp.getChild("target");

		// test non-existent file
		assertEquals("1.0", EFS.NONE, target.fetchInfo().getLength());

		// create empty file
		target.openOutputStream(EFS.NONE, null).close();
		assertEquals("1.0", 0, target.fetchInfo().getLength());

		try ( // add a byte
				OutputStream out = target.openOutputStream(EFS.NONE, null)) {
			out.write(5);
		}
		assertEquals("1.0", 1, target.fetchInfo().getLength());
	}

	public void testGetStat() throws CoreException {
		/* evaluate test environment */
		IPath root = getWorkspace().getRoot().getLocation().append("" + new Date().getTime());
		IFileStore temp = createDir(root.toString(), true);

		/* create common objects */
		IFileStore target = temp.getChild("target");
		long stat;

		/* test stat with an non-existing file */
		stat = target.fetchInfo().getLastModified();
		assertEquals("1.0", EFS.NONE, stat);

		/* test stat with an existing folder */
		createDir(target, true);
		stat = target.fetchInfo().getLastModified();
		assertTrue("2.0", EFS.NONE != stat);
	}

	public void testMove() throws Throwable {
		/* build scenario */
		IFileStore tempC = createDir(getWorkspace().getRoot().getLocation().append("temp").toString(), true);
		// create target file
		IFileStore target = tempC.getChild("target");
		String content = "just a content.....tnetnoc a tsuj";
		createFile(target, content);
		assertTrue("1.3", target.fetchInfo().exists());
		// create target tree
		IFileStore tree = tempC.getChild("tree");
		createDir(tree, true);
		createTree(getTree(tree));

		/* rename file */
		IFileStore destination = tempC.getChild("destination");
		target.move(destination, EFS.NONE, null);
		assertTrue("2.1", !destination.fetchInfo().isDirectory());
		assertTrue("2.2", !target.fetchInfo().exists());
		destination.move(target, EFS.NONE, null);
		assertTrue("2.3", !target.fetchInfo().isDirectory());
		assertTrue("2.4", !destination.fetchInfo().exists());

		/* rename file (but destination is already a file) */
		String anotherContent = "another content";
		createFile(destination, anotherContent);
		boolean ok = false;
		try {
			target.move(destination, EFS.NONE, null);
		} catch (CoreException e) {
			ok = true;
		}
		assertTrue("3.1", ok);
		assertTrue("3.2", !target.fetchInfo().isDirectory());
		destination.delete(EFS.NONE, null);
		assertTrue("3.3", !destination.fetchInfo().exists());

		/* rename file (but destination is already a folder) */
		createDir(destination, true);
		try {
			target.move(destination, EFS.NONE, null);
		} catch (CoreException e) {
			ok = true;
		}
		assertTrue("4.1", ok);
		assertTrue("4.2", !target.fetchInfo().isDirectory());
		destination.delete(EFS.NONE, null);
		assertTrue("4.3", !destination.fetchInfo().exists());

		/* rename folder */
		destination = tempC.getChild("destination");
		tree.move(destination, EFS.NONE, null);
		assertTrue("6.1", verifyTree(getTree(destination)));
		assertTrue("6.2", !tree.fetchInfo().exists());
		destination.move(tree, EFS.NONE, null);
		assertTrue("6.3", verifyTree(getTree(tree)));
		assertTrue("6.4", !destination.fetchInfo().exists());
	}

	public void testMoveAcrossVolumes() throws Throwable {
		IFileStore[] tempDirectories = getFileStoresOnTwoVolumes();

		/* test if we are in the adequate environment */
		if (tempDirectories == null || tempDirectories.length < 2 || tempDirectories[0] == null
				|| tempDirectories[1] == null) {
			return;
		}

		/* build scenario */
		/* get the source folder */
		IFileStore tempSrc = tempDirectories[0];
		/* get the destination folder */
		IFileStore tempDest = tempDirectories[1];
		// create target file
		String subfolderName = "target_" + System.currentTimeMillis();

		IFileStore target = tempSrc.getChild(subfolderName);
		deleteOnTearDown(target);
		String content = "just a content.....tnetnoc a tsuj";
		createFile(target, content);
		assertTrue("1.3", target.fetchInfo().exists());
		// create target tree
		IFileStore tree = tempSrc.getChild("tree");
		createDir(tree, true);
		createTree(getTree(tree));

		/* move file across volumes */
		IFileStore destination = tempDest.getChild(subfolderName);
		deleteOnTearDown(destination);
		target.move(destination, EFS.NONE, null);
		assertTrue("5.1", !destination.fetchInfo().isDirectory());
		assertTrue("5.2", !target.fetchInfo().exists());
		destination.move(target, EFS.NONE, null);
		assertTrue("5.3", !target.fetchInfo().isDirectory());
		assertTrue("5.4", !destination.fetchInfo().exists());

		/* move folder across volumes */
		destination = tempDest.getChild(subfolderName);
		deleteOnTearDown(destination);
		tree.move(destination, EFS.NONE, null);
		assertTrue("9.1", verifyTree(getTree(destination)));
		assertTrue("9.2", !tree.fetchInfo().exists());
		destination.move(tree, EFS.NONE, null);
		assertTrue("9.3", verifyTree(getTree(tree)));
		assertTrue("9.4", !destination.fetchInfo().exists());
	}

	public void testMoveDirectoryParentMissing() throws Throwable {
		IFileStore parent = getTempStore();
		IFileStore child = parent.getChild("child");
		IFileStore existing = getTempStore();
		createFile(existing, getRandomString());
		// try to move when parent of destination does not exist
		try {
			existing.move(child, EFS.NONE, getMonitor());
			fail("1.0");
		} catch (CoreException e) {
			// should fail
		}
		// destination should not exist
		assertTrue("1.1", !child.fetchInfo().exists());
	}

	/**
	 * Tests public API method
	 * {@link IFileStore#putInfo(IFileInfo, int, IProgressMonitor)}.
	 */
	public void testPutInfo() {
		IFileStore nonExisting = getTempStore();

		// assert that modifying a non-existing store fails
		IFileInfo info = nonExisting.fetchInfo();
		info.setLastModified(System.currentTimeMillis());
		try {
			nonExisting.putInfo(info, EFS.SET_LAST_MODIFIED, getMonitor());
			fail("1.0");
		} catch (CoreException e) {
			// expected
		}
		info = nonExisting.fetchInfo();
		info.setAttribute(EFS.ATTRIBUTE_READ_ONLY, false);
		try {
			nonExisting.putInfo(info, EFS.SET_ATTRIBUTES, getMonitor());
			fail("1.1");
		} catch (CoreException e) {
			// expected
		}
	}

	public void testReadOnly() throws CoreException {
		testAttribute(EFS.ATTRIBUTE_READ_ONLY);
	}

	public void testPermissionsEnabled() {
		String os = Platform.getOS();
		if (Platform.OS_LINUX.equals(os) || Platform.OS_MACOSX.equals(os)) {
			assertTrue(isAttributeSupported(EFS.ATTRIBUTE_OWNER_READ));
			assertTrue(isAttributeSupported(EFS.ATTRIBUTE_OWNER_WRITE));
			assertTrue(isAttributeSupported(EFS.ATTRIBUTE_OWNER_EXECUTE));
			assertTrue(isAttributeSupported(EFS.ATTRIBUTE_GROUP_READ));
			assertTrue(isAttributeSupported(EFS.ATTRIBUTE_GROUP_WRITE));
			assertTrue(isAttributeSupported(EFS.ATTRIBUTE_GROUP_EXECUTE));
			assertTrue(isAttributeSupported(EFS.ATTRIBUTE_OTHER_READ));
			assertTrue(isAttributeSupported(EFS.ATTRIBUTE_OTHER_WRITE));
			assertTrue(isAttributeSupported(EFS.ATTRIBUTE_OTHER_EXECUTE));
		} else {
			assertFalse(isAttributeSupported(EFS.ATTRIBUTE_OWNER_READ));
			assertFalse(isAttributeSupported(EFS.ATTRIBUTE_OWNER_WRITE));
			assertFalse(isAttributeSupported(EFS.ATTRIBUTE_OWNER_EXECUTE));
			assertFalse(isAttributeSupported(EFS.ATTRIBUTE_GROUP_READ));
			assertFalse(isAttributeSupported(EFS.ATTRIBUTE_GROUP_WRITE));
			assertFalse(isAttributeSupported(EFS.ATTRIBUTE_GROUP_EXECUTE));
			assertFalse(isAttributeSupported(EFS.ATTRIBUTE_OTHER_READ));
			assertFalse(isAttributeSupported(EFS.ATTRIBUTE_OTHER_WRITE));
			assertFalse(isAttributeSupported(EFS.ATTRIBUTE_OTHER_EXECUTE));
		}
	}

	public void testPermissions() throws CoreException {
		testAttribute(EFS.ATTRIBUTE_OWNER_READ);
		testAttribute(EFS.ATTRIBUTE_OWNER_WRITE);
		testAttribute(EFS.ATTRIBUTE_OWNER_EXECUTE);
		testAttribute(EFS.ATTRIBUTE_GROUP_READ);
		testAttribute(EFS.ATTRIBUTE_GROUP_WRITE);
		testAttribute(EFS.ATTRIBUTE_GROUP_EXECUTE);
		testAttribute(EFS.ATTRIBUTE_OTHER_READ);
		testAttribute(EFS.ATTRIBUTE_OTHER_WRITE);
		testAttribute(EFS.ATTRIBUTE_OTHER_EXECUTE);
	}

	private void testAttribute(int attribute) throws CoreException {
		if (!isAttributeSupported(attribute)) {
			return;
		}

		IPath root = getWorkspace().getRoot().getLocation().append("" + new Date().getTime());
		IFileStore targetFolder = createDir(root.toString(), true);
		try {
			IFileStore targetFile = targetFolder.getChild("targetFile");
			createFileInFileSystem(targetFile);

			// file
			boolean init = targetFile.fetchInfo().getAttribute(attribute);
			setAttribute(targetFile, attribute, !init);
			assertTrue("1.2", targetFile.fetchInfo().getAttribute(attribute) != init);
			setAttribute(targetFile, attribute, init);
			assertTrue("1.4", targetFile.fetchInfo().getAttribute(attribute) == init);

			// folder
			init = targetFolder.fetchInfo().getAttribute(attribute);
			setAttribute(targetFolder, attribute, !init);
			assertTrue("2.2", targetFolder.fetchInfo().getAttribute(attribute) != init);
			setAttribute(targetFolder, attribute, init);
			assertTrue("2.4", targetFolder.fetchInfo().getAttribute(attribute) == init);

		} finally {
			/* remove trash */
			targetFolder.delete(EFS.NONE, null);
		}
	}

	public void testGetFileStore() throws Exception {
		// create files
		File file = getTempDir().append("test.txt").toFile();
		file.createNewFile();
		assertTrue("1.0", file.exists());

		IFileStore tempStore = createDir(getTempDir().append("temp").toString(), true);
		createDir(getTempDir().append("temp/temp2").toString(), true);

		file = getTempDir().append("temp/temp2/test.txt").toFile();
		file.createNewFile();
		assertTrue("2.0", file.exists());

		// check the parent reference
		IPath relativePath = new Path("../test.txt");

		IFileStore relativeStore = tempStore.getFileStore(relativePath);
		assertNotNull("3.0", relativeStore);
		IFileInfo info = relativeStore.fetchInfo();
		assertNotNull("4.0", info);
		assertTrue("5.0", info.exists());

		// check the parent and self reference
		relativePath = new Path(".././test.txt");

		relativeStore = tempStore.getFileStore(relativePath);
		assertNotNull("6.0", relativeStore);
		info = relativeStore.fetchInfo();
		assertNotNull("7.0", info);
		assertTrue("8.0", info.exists());

		// check the a path with no parent and self references
		relativePath = new Path("temp2/test.txt");

		relativeStore = tempStore.getFileStore(relativePath);
		assertNotNull("9.0", relativeStore);
		info = relativeStore.fetchInfo();
		assertNotNull("10.0", info);
		assertTrue("11.0", info.exists());
	}

	public void testSortOrder() {
		IFileSystem nullfs = NullFileSystem.getInstance();
		if (nullfs == null) {
			nullfs = new NullFileSystem();
			((FileSystem) nullfs).initialize(EFS.SCHEME_NULL);
		}
		IFileStore nabc = nullfs.getStore(new Path("/a/b/c"));
		IFileStore nabd = nullfs.getStore(new Path("/a/b/d"));
		assertEquals("1.0", -1, nabc.compareTo(nabd));
		assertEquals("1.1", 0, nabc.compareTo(nabc));
		assertEquals("1.2", 1, nabd.compareTo(nabc));
		IFileSystem lfs = LocalFileSystem.getInstance();
		IFileStore labc = lfs.getStore(new Path("/a/b/c"));
		IFileStore labd = lfs.getStore(new Path("/a/b/d"));
		assertEquals("2.0", -1, labc.compareTo(labd));
		assertEquals("2.1", 0, labc.compareTo(labc));
		assertEquals("2.2", 1, labd.compareTo(labc));
		int schemeCompare = nullfs.getScheme().compareTo(lfs.getScheme());
		assertEquals("3.0", schemeCompare, nabd.compareTo(labc));
		assertEquals("3.1", schemeCompare, nabc.compareTo(labd));
		assertEquals("3.2", -schemeCompare, labd.compareTo(nabc));
		assertEquals("3.3", -schemeCompare, labc.compareTo(nabd));
		assertEquals("4.0", 1, labc.compareTo(null));
		assertEquals("4.1", 1, nabc.compareTo(null));
	}

	public void testSortOrderPaths() {
		IFileSystem lfs = LocalFileSystem.getInstance();
		boolean isWindows = java.io.File.separatorChar == '\\';
		String prefix = isWindows ? "/D:" : "";
		List<String> paths = List.of( //
				"/a", //
				"/a/", //
				"/a/b", //
				"/a/./c", //
				"/a/e/../c", //
				"/a/d", //
				"/aa", //
				"/b").stream().map(s -> prefix + s).toList();
		List<String> pathsTrimmed = paths.stream().map(s -> s //
				.replaceAll("/$", "") // remove trailing slashes
				.replaceAll("/[^/]+/\\.\\./", "/") // collapse /a/../ to /
				.replaceAll("/\\./", "/") // collapse /./ to /
		).toList();
		paths = new ArrayList<>(paths); // to get a mutable copy for shuffling
		Collections.shuffle(paths);
		// Test with new Path(string).getStore()
		Stream<IFileStore> pathStores = paths.stream().map(Path::new).map(lfs::getStore);
		List<String> sortedPathStores = pathStores.sorted(IFileStore::compareTo).map(IFileStore::toURI)
				.map(URI::getPath).toList();
		assertEquals("1.0 ", pathsTrimmed, sortedPathStores);
		// Test with new LocalFile(new File(string)))
		Stream<IFileStore> localFileStores = paths.stream().map(File::new).map(LocalFile::new);
		List<String> sortedLocalFileStores = localFileStores.sorted(IFileStore::compareTo).map(IFileStore::toURI)
				.map(URI::getPath).toList();
		assertEquals("2.0 ", pathsTrimmed, sortedLocalFileStores);
	}
}
