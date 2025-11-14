/*******************************************************************************
 *  Copyright (c) 2000, 2024 IBM Corporation and others.
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

import static org.eclipse.core.tests.internal.localstore.LocalStoreTestUtil.createTree;
import static org.eclipse.core.tests.internal.localstore.LocalStoreTestUtil.getTree;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomString;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.getFileStore;
import static org.eclipse.core.tests.resources.ResourceTestUtil.isAttributeSupported;
import static org.eclipse.core.tests.resources.ResourceTestUtil.isReadOnlySupported;
import static org.eclipse.core.tests.resources.ResourceTestUtil.setReadOnly;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.eclipse.osgi.util.NLS;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Basic tests for the IFileStore API
 */
@ExtendWith(WorkspaceResetExtension.class)
public class FileStoreTest {

	private static final Random RANDOM = new Random();

	private IFileStore createDir(IFileStore store, boolean clear) throws CoreException {
		if (clear && store.fetchInfo().exists()) {
			store.delete(EFS.NONE, null);
		}
		store.mkdir(EFS.NONE, null);
		IFileInfo info = store.fetchInfo();
		assertTrue(info.exists());
		assertTrue(info.isDirectory());
		return store;
	}

	@TempDir
	static Path tempDirectory;
	private static final AtomicInteger PATH_INDEX = new AtomicInteger();

	private static Path randomUniqueNotExistingPath() {
		return tempDirectory.resolve(System.currentTimeMillis() + "-" + PATH_INDEX.incrementAndGet());
	}

	private static IFileStore randomUniqueNotExistingFileStore() throws IOException {
		return getFileStore(randomUniqueNotExistingPath());
	}

	private void createFile(IFileStore target, String content) throws CoreException {
		createFile(target, content.getBytes());
	}

	private void createFile(IFileStore target, byte[] content) throws CoreException {
		target.write(content, EFS.NONE, null);
	}

	/**
	 * Tests behavior of IFileStore#fetchInfo when underlying file system throws
	 * exceptions.
	 */
	@Test
	public void testBrokenFetchInfo() throws Exception {
		IFileStore broken = EFS.getStore(new URI("broken://a/b/c"));
		// no-arg fetch info should return non-existent file
		IFileInfo info = broken.fetchInfo();
		assertTrue(!info.exists(), "file info does not exist");

		// two-arg fetchInfo should throw exception
		assertThrows(CoreException.class, () -> broken.fetchInfo(EFS.NONE, createTestMonitor()));
	}

	private IFileStore getDirFileStore(String path) throws CoreException {
		IFileStore store = EFS.getLocalFileSystem().getStore(IPath.fromOSString(path));
		if (!store.toLocalFile(EFS.NONE, createTestMonitor()).exists()) {
			store.mkdir(EFS.NONE, null);
		}
		return store;
	}

	private IFileStore[] getFileStoresOnTwoVolumes() {
		IFileStore[] tempDirs = new IFileStore[2];
		for (char c = 'c'; c <= 'z'; c++) {
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
	@ParameterizedTest(name = "File size {0} bytes")
	@ValueSource(ints = { 10, LocalFile.LARGE_FILE_SIZE_THRESHOLD + 10 })
	public void testCopyAcrossVolumes(int fileSize) throws Throwable {
		IFileStore[] tempDirectories = getFileStoresOnTwoVolumes();

		/* test if we are in the adequate environment */
		assumeFalse("only executable if at least two volumes are present", tempDirectories == null
				|| tempDirectories.length < 2 || tempDirectories[0] == null || tempDirectories[1] == null);

		/* build scenario */
		// create source root folder
		IFileStore tempSrc = tempDirectories[0];
		/* get the destination folder */
		IFileStore tempDest = tempDirectories[1];

		// create tree
		String subfolderName = "target_" + System.currentTimeMillis();

		IFileStore target = tempSrc.getChild(subfolderName);
		createDir(target, true);
		createTree(getTree(target), fileSize);

		/* c:\temp\target -> d:\temp\target */
		IFileStore destination = tempDest.getChild(subfolderName);
		target.copy(destination, EFS.NONE, null);
		assertTrue(verifyTree(getTree(destination)));
		destination.delete(EFS.NONE, null);

		/* c:\temp\target -> d:\temp\copy of target */
		String copyOfSubfolderName = "copy of " + subfolderName;
		destination = tempDest.getChild(copyOfSubfolderName);
		target.copy(destination, EFS.NONE, null);
		assertTrue(verifyTree(getTree(destination)));
		destination.delete(EFS.NONE, null);

		/* c:\temp\target -> d:\temp\target (but the destination is already a file) */
		destination = tempDest.getChild(subfolderName);
		String anotherContent = "nothing..................gnihton";
		createFile(destination, anotherContent);
		assertTrue(!destination.fetchInfo().isDirectory());
		final IFileStore immutableDestination = destination;
		assertThrows(CoreException.class, () -> target.copy(immutableDestination, EFS.NONE, null));
		assertTrue(!verifyTree(getTree(destination)));
		destination.delete(EFS.NONE, null);

		/* c:\temp\target -> d:\temp\target (but the destination is already a folder */
		destination = tempDest.getChild(subfolderName);
		createDir(destination, true);
		target.copy(destination, EFS.NONE, null);
		assertTrue(verifyTree(getTree(destination)));
		destination.delete(EFS.NONE, null);
	}

	@Test
	public void testCopyDirectory() throws Throwable {
		/* build scenario */
		IFileStore temp = randomUniqueNotExistingFileStore();
		temp.mkdir(EFS.NONE, null);
		assertTrue(temp.fetchInfo().isDirectory());
		// create tree
		IFileStore target = temp.getChild("target");
		target.delete(EFS.NONE, null);
		createTree(getTree(target));

		/* temp\target -> temp\copy of target */
		IFileStore copyOfTarget = temp.getChild("copy of target");
		target.copy(copyOfTarget, EFS.NONE, null);
		assertTrue(verifyTree(getTree(copyOfTarget)));
	}

	@Test
	public void testCopyDirectoryParentMissing() throws Throwable {
		IFileStore parent = randomUniqueNotExistingFileStore();
		IFileStore child = parent.getChild("child");
		IFileStore existing = randomUniqueNotExistingFileStore();
		createFile(existing, createRandomString());
		// try to copy when parent of destination does not exist
		assertThrows(CoreException.class, () -> existing.copy(child, EFS.NONE, createTestMonitor()));
		// destination should not exist
		assertTrue(!child.fetchInfo().exists());
	}

	@ParameterizedTest(name = "File size {0} bytes")
	@ValueSource(ints = { 10, LocalFile.LARGE_FILE_SIZE_THRESHOLD + 10 })
	public void testCaseInsensitive(int fileSize) throws Throwable {
	IFileStore temp = createDir(randomUniqueNotExistingFileStore(), true);
		boolean isCaseSensitive = temp.getFileSystem().isCaseSensitive();
		assumeFalse("only relevant for platforms with case-sensitive file system", isCaseSensitive);

		byte[] content = new byte[fileSize];
		RANDOM.nextBytes(content);

		// create a file
		IFileStore fileWithSmallName = temp.getChild("filename");
		fileWithSmallName.delete(EFS.NONE, null);
		createFile(fileWithSmallName, content);
		System.out.println(fileWithSmallName.fetchInfo().getName());
		assertTrue(fileWithSmallName.fetchInfo().exists());
		assertTrue( Arrays.equals(content, fileWithSmallName.readAllBytes(EFS.NONE, null)));

		IFileStore fileWithOtherName = temp.getChild("FILENAME");
		System.out.println(fileWithOtherName.fetchInfo().getName());
		// file content is already the same for both Cases:
		assertTrue(Arrays.equals(content, fileWithOtherName.readAllBytes(EFS.NONE, null)));
		fileWithSmallName.copy(fileWithOtherName, IResource.DEPTH_INFINITE, null); // a NOP Operation
		// file content is still the same for both Cases:
		assertTrue(Arrays.equals(content, fileWithOtherName.readAllBytes(EFS.NONE, null)));
		assertTrue(fileWithOtherName.fetchInfo().exists());
		assertTrue(fileWithSmallName.fetchInfo().exists());
		fileWithOtherName.delete(EFS.NONE, null);
		assertFalse(fileWithOtherName.fetchInfo().exists());
		assertFalse(fileWithSmallName.fetchInfo().exists());
		CoreException exception = assertThrows(CoreException.class,
				() -> fileWithSmallName.move(fileWithOtherName, EFS.NONE, null));
		String message = NLS.bind(Messages.couldNotMove, fileWithSmallName.toString());
		assertEquals(message, exception.getMessage());
	}

	@ParameterizedTest(name = "File size {0} bytes")
	@ValueSource(ints = { 10, LocalFile.LARGE_FILE_SIZE_THRESHOLD + 10 })
	public void testCopyFile(int fileSize) throws Throwable {
		/* build scenario */
		IFileStore temp = createDir(randomUniqueNotExistingFileStore(), true);
		byte[] content = new byte[fileSize];
		RANDOM.nextBytes(content);

		// create target
		IFileStore target = temp.getChild("target");
		target.delete(EFS.NONE, null);
		createFile(target, content);
		assertTrue(target.fetchInfo().exists());
		assertTrue(Arrays.equals(content, target.readAllBytes(EFS.NONE, null)));

		/* temp\target -> temp\copy of target */
		IFileStore copyOfTarget = temp.getChild("copy of target");
		target.copy(copyOfTarget, IResource.DEPTH_INFINITE, null);
		assertTrue(Arrays.equals(content, copyOfTarget.readAllBytes(EFS.NONE, null)));
		copyOfTarget.delete(EFS.NONE, null);

		// We need to know whether or not we can unset the read-only flag
		// in order to perform this part of the test.
		if (isReadOnlySupported()) {
			/* make source read-only and try the copy temp\target -> temp\copy of target */
			copyOfTarget = temp.getChild("copy of target");
			setReadOnly(target, true);

			target.copy(copyOfTarget, IResource.DEPTH_INFINITE, null);
			assertTrue(Arrays.equals(content, copyOfTarget.readAllBytes(EFS.NONE, null)));
			// reset read only flag for cleanup
			setReadOnly(copyOfTarget, false);
			copyOfTarget.delete(EFS.NONE, null);
			// reset the read only flag for cleanup
			setReadOnly(target, false);
			target.delete(EFS.NONE, null);
		}
	}

	/**
	 * Basically this is a test for the Windows Platform.
	 */
	@ParameterizedTest(name = "File size {0} bytes")
	@ValueSource(ints = { 10, LocalFile.LARGE_FILE_SIZE_THRESHOLD + 10 })
	public void testCopyFileAcrossVolumes(int fileSize) throws Throwable {
		IFileStore[] tempDirectories = getFileStoresOnTwoVolumes();

		/* test if we are in the adequate environment */
		assumeFalse("only executable if at least two volumes are present", tempDirectories == null
				|| tempDirectories.length < 2 || tempDirectories[0] == null || tempDirectories[1] == null);

		/* build scenario */
		/* get the source folder */
		IFileStore tempSrc = tempDirectories[0];
		/* get the destination folder */
		IFileStore tempDest = tempDirectories[1];
		// create target
		byte[] content = new byte[fileSize];
		RANDOM.nextBytes(content);
		String subfolderName = "target_" + System.currentTimeMillis();

		IFileStore target = tempSrc.getChild(subfolderName);
		target.delete(EFS.NONE, null);
		createFile(target, content);
		assertTrue(target.fetchInfo().exists());
		assertTrue(Arrays.equals(content, target.readAllBytes(EFS.NONE, null)));

		/* c:\temp\target -> d:\temp\target */
		IFileStore destination = tempDest.getChild(subfolderName);
		target.copy(destination, IResource.DEPTH_INFINITE, null);
		assertTrue(Arrays.equals(content, destination.readAllBytes(EFS.NONE, null)));
		destination.delete(EFS.NONE, null);

		/* c:\temp\target -> d:\temp\copy of target */
		String copyOfSubfoldername = "copy of " + subfolderName;
		destination = tempDest.getChild(copyOfSubfoldername);
		target.copy(destination, IResource.DEPTH_INFINITE, null);
		assertTrue(Arrays.equals(content, destination.readAllBytes(EFS.NONE, null)));
		destination.delete(EFS.NONE, null);

		/* c:\temp\target -> d:\temp\target (but the destination is already a file */
		destination = tempDest.getChild(subfolderName);
		String anotherContent = "nothing..................gnihton";
		createFile(destination, anotherContent);
		assertTrue(!destination.fetchInfo().isDirectory());
		target.copy(destination, IResource.DEPTH_INFINITE, null);
		assertTrue(Arrays.equals(content, destination.readAllBytes(EFS.NONE, null)));
		destination.delete(EFS.NONE, null);

		/* c:\temp\target -> d:\temp\target (but the destination is already a folder */
		destination = tempDest.getChild(subfolderName);
		createDir(destination, true);
		assertTrue(destination.fetchInfo().isDirectory());
		final IFileStore immutableDestination = destination;
		assertThrows(CoreException.class, () -> target.copy(immutableDestination, EFS.NONE, null));
		/* test if the input stream inside the copy method was closed */
		target.delete(EFS.NONE, null);
		createFile(target, content);
		assertTrue(destination.fetchInfo().isDirectory());
		destination.delete(EFS.NONE, null);
	}

	@Test
	public void testFileAttributeCopyForSmallFiles() throws CoreException, IOException {
		Path root = Files.createDirectories(randomUniqueNotExistingPath());
		Path sourceFile = root.resolve("source.txt");
		Files.writeString(sourceFile, "This is a test file.");
		Path targetFile = root.resolve("target.txt");

		IFileStore sourceStore = EFS.getLocalFileSystem().getStore(IPath.fromPath(sourceFile));
		IFileStore targetStore = new OnCloseWritingFileStore(targetFile);

		sourceStore.copy(targetStore, EFS.NONE, new NullProgressMonitor());

		String sourceContent = Files.readString(sourceFile);
		String targetContent = Files.readString(targetFile);
		assertEquals(sourceContent, targetContent);
	}

	@Test
	public void testGetLength() throws Exception {
		// evaluate test environment
		IFileStore temp = createDir(randomUniqueNotExistingFileStore(), true);
		// create common objects
		IFileStore target = temp.getChild("target");

		// test non-existent file
		assertEquals(EFS.NONE, target.fetchInfo().getLength());

		// create empty file
		target.openOutputStream(EFS.NONE, null).close();
		assertEquals(0, target.fetchInfo().getLength());

		try ( // add a byte
				OutputStream out = target.openOutputStream(EFS.NONE, null)) {
			out.write(5);
		}
		assertEquals(1, target.fetchInfo().getLength());
	}

	@Test
	public void testGetStat() throws CoreException, IOException {
		/* evaluate test environment */
		IFileStore temp = createDir(randomUniqueNotExistingFileStore(), true);

		/* create common objects */
		IFileStore target = temp.getChild("target");
		long stat;

		/* test stat with an non-existing file */
		stat = target.fetchInfo().getLastModified();
		assertEquals(EFS.NONE, stat);

		/* test stat with an existing folder */
		createDir(target, true);
		stat = target.fetchInfo().getLastModified();
		assertTrue(EFS.NONE != stat);
	}

	@Test
	public void testDelete() throws Throwable {
		IFileStore tempC = createDir(randomUniqueNotExistingFileStore(), true);
		System.out.println("Threadcount=" + Runtime.getRuntime().availableProcessors());
		{
			IFileStore singleDirectory = tempC.getChild("tree");
			createDir(singleDirectory, true);
			List<IFileStore> treeItems = new ArrayList<>();
			for (int i = 0; i < 2300; i++) {
				treeItems.add(singleDirectory.getChild("file" + i));
			}

			createTree(treeItems.toArray(IFileStore[]::new));
			assertTrue(singleDirectory.fetchInfo().exists());
			long n0 = System.nanoTime();
			singleDirectory.delete(EFS.NONE, null);
			long n1 = System.nanoTime();
			System.out.println("delete singleDirectory took ms:" + (n1 - n0) / 1_000_000);
			assertFalse(singleDirectory.fetchInfo().exists());
		}

		{
			IFileStore binaryTree = tempC.getChild("tree");
			createDir(binaryTree, true);
			createChildDirs(binaryTree, 2, 10);
			assertTrue(binaryTree.fetchInfo().exists());
			long n0 = System.nanoTime();
			binaryTree.delete(EFS.NONE, null);
			long n1 = System.nanoTime();
			System.out.println("delete binaryTree took ms:" + (n1 - n0) / 1_000_000);
			assertFalse(binaryTree.fetchInfo().exists());
		}
	}

	private void createChildDirs(IFileStore parent, int count, int depth) throws CoreException {
		if (depth <= 0) {
			return;
		}
		for (int i = 0; i < count; i++) {
			IFileStore child = parent.getChild("dir-" + depth + "-" + i);
			child.mkdir(EFS.NONE, null);
			createChildDirs(child, count, depth - 1);
		}
	}

	@Test
	public void testMove() throws Throwable {
		/* build scenario */
		IFileStore tempC = createDir(randomUniqueNotExistingFileStore(), true);
		// create target file
		IFileStore target = tempC.getChild("target");
		String content = "just a content.....tnetnoc a tsuj";
		createFile(target, content);
		assertTrue(target.fetchInfo().exists());
		// create target tree
		IFileStore tree = tempC.getChild("tree");
		createDir(tree, true);
		createTree(getTree(tree));

		/* rename file */
		IFileStore destination = tempC.getChild("destination");
		target.move(destination, EFS.NONE, null);
		assertTrue(!destination.fetchInfo().isDirectory());
		assertTrue(!target.fetchInfo().exists());
		destination.move(target, EFS.NONE, null);
		assertTrue(!target.fetchInfo().isDirectory());
		assertTrue(!destination.fetchInfo().exists());

		/* rename file (but destination is already a file) */
		String anotherContent = "another content";
		createFile(destination, anotherContent);
		final IFileStore immutableFileDestination = destination;
		assertThrows(CoreException.class, () -> target.move(immutableFileDestination, EFS.NONE, null));
		assertTrue(!target.fetchInfo().isDirectory());
		destination.delete(EFS.NONE, null);
		assertTrue(!destination.fetchInfo().exists());

		/* rename file (but destination is already a folder) */
		createDir(destination, true);
		final IFileStore immutableFolderDestination = destination;
		assertThrows(CoreException.class, () -> target.move(immutableFolderDestination, EFS.NONE, null));
		assertTrue(!target.fetchInfo().isDirectory());
		destination.delete(EFS.NONE, null);
		assertTrue(!destination.fetchInfo().exists());

		/* rename folder */
		destination = tempC.getChild("destination");
		tree.move(destination, EFS.NONE, null);
		assertTrue(verifyTree(getTree(destination)));
		assertTrue(!tree.fetchInfo().exists());
		destination.move(tree, EFS.NONE, null);
		assertTrue(verifyTree(getTree(tree)));
		assertTrue(!destination.fetchInfo().exists());
	}

	@Test
	public void testMoveAcrossVolumes() throws Throwable {
		IFileStore[] tempDirectories = getFileStoresOnTwoVolumes();

		/* test if we are in the adequate environment */
		assumeFalse("only executable if at least two volumes are present", tempDirectories == null
				|| tempDirectories.length < 2 || tempDirectories[0] == null || tempDirectories[1] == null);

		/* build scenario */
		/* get the source folder */
		IFileStore tempSrc = tempDirectories[0];
		/* get the destination folder */
		IFileStore tempDest = tempDirectories[1];
		// create target file
		String subfolderName = "target_" + System.currentTimeMillis();

		IFileStore target = tempSrc.getChild(subfolderName);
		String content = "just a content.....tnetnoc a tsuj";
		createFile(target, content);
		assertTrue(target.fetchInfo().exists());
		// create target tree
		IFileStore tree = tempSrc.getChild("tree");
		createDir(tree, true);
		createTree(getTree(tree));

		/* move file across volumes */
		IFileStore destination = tempDest.getChild(subfolderName);
		target.move(destination, EFS.NONE, null);
		assertTrue(!destination.fetchInfo().isDirectory());
		assertTrue(!target.fetchInfo().exists());
		destination.move(target, EFS.NONE, null);
		assertTrue(!target.fetchInfo().isDirectory());
		assertTrue(!destination.fetchInfo().exists());

		/* move folder across volumes */
		destination = tempDest.getChild(subfolderName);
		tree.move(destination, EFS.NONE, null);
		assertTrue(verifyTree(getTree(destination)));
		assertTrue(!tree.fetchInfo().exists());
		destination.move(tree, EFS.NONE, null);
		assertTrue(verifyTree(getTree(tree)));
		assertTrue(!destination.fetchInfo().exists());
	}

	@Test
	public void testMoveDirectoryParentMissing() throws Throwable {
		IFileStore parent = randomUniqueNotExistingFileStore();
		IFileStore child = parent.getChild("child");
		IFileStore existing = randomUniqueNotExistingFileStore();
		createFile(existing, createRandomString());
		// try to move when parent of destination does not exist
		assertThrows(CoreException.class, () -> existing.move(child, EFS.NONE, createTestMonitor()));
		// destination should not exist
		assertTrue(!child.fetchInfo().exists());
	}

	/**
	 * Tests public API method
	 * {@link IFileStore#putInfo(IFileInfo, int, IProgressMonitor)}.
	 */
	@Test
	public void testPutInfo() throws IOException {
		IFileStore nonExisting = randomUniqueNotExistingFileStore();

		// assert that modifying a non-existing store fails
		IFileInfo info = nonExisting.fetchInfo();
		info.setLastModified(System.currentTimeMillis());
		assertThrows(CoreException.class, () -> nonExisting.putInfo(info, EFS.SET_LAST_MODIFIED, createTestMonitor()));
		IFileInfo refetchedInfo = nonExisting.fetchInfo();
		info.setAttribute(EFS.ATTRIBUTE_READ_ONLY, false);
		assertThrows(CoreException.class, () -> nonExisting.putInfo(refetchedInfo, EFS.SET_ATTRIBUTES, createTestMonitor()));
	}

	@Test
	public void testReadOnly() throws Exception {
		testAttribute(EFS.ATTRIBUTE_READ_ONLY);
	}

	@Test
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

	@Test
	public void testPermissions() throws Exception {
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

	private void testAttribute(int attribute) throws Exception {
		assumeTrue("only relevant for platforms supporting attribute: " + attribute, isAttributeSupported(attribute));

		IFileStore targetFolder = createDir(randomUniqueNotExistingFileStore(), true);
		IFileStore targetFile = targetFolder.getChild("targetFile");
		createInFileSystem(targetFile);

		// file
		boolean init = targetFile.fetchInfo().getAttribute(attribute);
		setAttribute(targetFile, attribute, !init);
		assertTrue(targetFile.fetchInfo().getAttribute(attribute) != init);
		setAttribute(targetFile, attribute, init);
		assertTrue(targetFile.fetchInfo().getAttribute(attribute) == init);

		// folder
		init = targetFolder.fetchInfo().getAttribute(attribute);
		setAttribute(targetFolder, attribute, !init);
		assertTrue(targetFolder.fetchInfo().getAttribute(attribute) != init);
		setAttribute(targetFolder, attribute, init);
		assertTrue(targetFolder.fetchInfo().getAttribute(attribute) == init);
	}

	private void setAttribute(IFileStore target, int attribute, boolean value) throws CoreException {
		assertTrue(isAttributeSupported(attribute));
		IFileInfo fileInfo = target.fetchInfo();
		fileInfo.setAttribute(attribute, value);
		target.putInfo(fileInfo, EFS.SET_ATTRIBUTES, null);
	}

	@Test
	public void testGetFileStore() throws Exception {
		// create files
		Path root = Files.createDirectories(randomUniqueNotExistingPath());
		File file = root.resolve("test.txt").toFile();
		file.createNewFile();
		assertTrue(file.exists());

		IFileStore tempStore = createDir(getFileStore(root.resolve("temp")), true);
		createDir(getFileStore(root.resolve("temp/temp2")), true);

		file = root.resolve("temp/temp2/test.txt").toFile();
		file.createNewFile();
		assertTrue(file.exists());

		// check the parent reference
		IPath relativePath = IPath.fromOSString("../test.txt");

		IFileStore relativeStore = tempStore.getFileStore(relativePath);
		assertNotNull(relativeStore);
		IFileInfo info = relativeStore.fetchInfo();
		assertNotNull(info);
		assertTrue(info.exists());

		// check the parent and self reference
		relativePath = IPath.fromOSString(".././test.txt");

		relativeStore = tempStore.getFileStore(relativePath);
		assertNotNull(relativeStore);
		info = relativeStore.fetchInfo();
		assertNotNull(info);
		assertTrue(info.exists());

		// check the a path with no parent and self references
		relativePath = IPath.fromOSString("temp2/test.txt");

		relativeStore = tempStore.getFileStore(relativePath);
		assertNotNull(relativeStore);
		info = relativeStore.fetchInfo();
		assertNotNull(info);
		assertTrue(info.exists());
	}

	@Test
	public void testSortOrder() {
		IFileSystem nullfs = NullFileSystem.getInstance();
		if (nullfs == null) {
			nullfs = new NullFileSystem();
			((FileSystem) nullfs).initialize(EFS.SCHEME_NULL);
		}
		IFileStore nabc = nullfs.getStore(IPath.fromOSString("/a/b/c"));
		IFileStore nabd = nullfs.getStore(IPath.fromOSString("/a/b/d"));
		assertEquals(-1, nabc.compareTo(nabd));
		assertEquals(0, nabc.compareTo(nabc));
		assertEquals(1, nabd.compareTo(nabc));
		IFileSystem lfs = LocalFileSystem.getInstance();
		IFileStore labc = lfs.getStore(IPath.fromOSString("/a/b/c"));
		IFileStore labd = lfs.getStore(IPath.fromOSString("/a/b/d"));
		assertEquals(-1, labc.compareTo(labd));
		assertEquals(0, labc.compareTo(labc));
		assertEquals(1, labd.compareTo(labc));
		int schemeCompare = nullfs.getScheme().compareTo(lfs.getScheme());
		assertEquals(schemeCompare, nabd.compareTo(labc));
		assertEquals(schemeCompare, nabc.compareTo(labd));
		assertEquals(-schemeCompare, labd.compareTo(nabc));
		assertEquals(-schemeCompare, labc.compareTo(nabd));
		assertEquals(1, labc.compareTo(null));
		assertEquals(1, nabc.compareTo(null));
	}

	@Test
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
				.replace("/./", "/") // collapse /./ to /
		).toList();
		paths = new ArrayList<>(paths); // to get a mutable copy for shuffling
		Collections.shuffle(paths);
		// Test with IPath.fromOSString(string).getStore()
		Stream<IFileStore> pathStores = paths.stream().map(IPath::fromOSString).map(lfs::getStore);
		List<String> sortedPathStores = pathStores.sorted(IFileStore::compareTo).map(IFileStore::toURI)
				.map(URI::getPath).toList();
		assertEquals(pathsTrimmed, sortedPathStores);
		// Test with new LocalFile(new File(string)))
		Stream<IFileStore> localFileStores = paths.stream().map(File::new).map(LocalFile::new);
		List<String> sortedLocalFileStores = localFileStores.sorted(IFileStore::compareTo).map(IFileStore::toURI)
				.map(URI::getPath).toList();
		assertEquals(pathsTrimmed, sortedLocalFileStores);
	}

	private boolean verifyNode(IFileStore node) {
		char type = node.getName().charAt(0);
		// if the name starts with d it must be a directory
		return (type == 'd') == node.fetchInfo().isDirectory();
	}

	private boolean verifyTree(IFileStore[] tree) {
		for (IFileStore t : tree) {
			if (!verifyNode(t)) {
				return false;
			}
		}
		return true;
	}

}
