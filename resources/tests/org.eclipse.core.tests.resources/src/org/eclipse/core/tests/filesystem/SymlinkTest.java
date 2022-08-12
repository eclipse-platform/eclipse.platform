/*******************************************************************************
 * Copyright (c) 2007, 2020 Wind River Systems, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Martin Oberhuber (Wind River) - initial API and implementation
 * John Arthorne (IBM) - [172346] disable tests with problematic Platform encoding
 * Martin Oberhuber (Wind River) - [183137] liblocalfile for solaris-sparc
 * Martin Oberhuber (Wind River) - [184433] liblocalfile for Linux x86_64
 * Martin Oberhuber (Wind River) - [232426] push up createSymLink() to CoreTest
 * Martin Oberhuber (Wind River) - [331716] Symlink test failures on Windows 7
 * Sergey Prigogin (Google) - [440283] Modify symlink tests to run on Windows with or without administrator privileges
 * 							  [445805] Make sure symlink tests are green when run on Windows with administrator privileges
 *                            [458989] Add a test case for setting ATTRIBUTE_HIDDEN on a symlink on Windows
 *******************************************************************************/
package org.eclipse.core.tests.filesystem;

import java.io.OutputStream;
import org.eclipse.core.filesystem.*;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;

public class SymlinkTest extends FileSystemTest {
	/**
	 * Symbolic links on Windows behave differently compared to Unix-based systems. Symbolic links
	 * on Windows have their own set of attributes independent from the attributes of the link's
	 * target. The {@link java.io.File#exists() File.exists()} method on Windows checks for
	 * existence of the symbolic link itself, not its target.
	 */
	private static final boolean SYMLINKS_ARE_FIRST_CLASS_FILES_OR_DIRECTORIES = Platform.OS_WIN32
			.equals(Platform.getOS()) ? true : false;
	private static String specialCharName = "äöüß ÄÖÜ àÀâÂ µ²³úá"; //$NON-NLS-1$

	protected IFileStore aDir, aFile; //actual Dir, File
	protected IFileInfo iDir, iFile, ilDir, ilFile, illDir, illFile;
	protected IFileStore lDir, lFile; //symlink to Dir, File
	protected IFileStore llDir, llFile; //link to link to Dir, File

	public static IFileSystem getFileSystem() {
		try {
			return EFS.getFileSystem(EFS.SCHEME_FILE);
		} catch (CoreException e) {
			fail("getFileSystem", e);
		}
		return null;
	}

	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}

	protected void fetchFileInfos() {
		iDir = aDir.fetchInfo();
		iFile = aFile.fetchInfo();
		ilDir = lDir.fetchInfo();
		ilFile = lFile.fetchInfo();
		illDir = llDir.fetchInfo();
		illFile = llFile.fetchInfo();
	}

	public boolean haveSymlinks() {
		return isAttributeSupported(EFS.ATTRIBUTE_SYMLINK);
	}

	protected void makeLinkStructure() {
		aDir = baseStore.getChild("aDir");
		aFile = baseStore.getChild("aFile");
		lDir = baseStore.getChild("lDir");
		lFile = baseStore.getChild("lFile");
		llDir = baseStore.getChild("llDir");
		llFile = baseStore.getChild("llFile");
		ensureExists(aDir, true);
		ensureExists(aFile, false);
		mkLink(baseStore, "lDir", "aDir", true);
		mkLink(baseStore, "llDir", "lDir", true);
		mkLink(baseStore, "lFile", "aFile", false);
		mkLink(baseStore, "llFile", "lFile", false);
		fetchFileInfos();
	}

	protected void mkLink(IFileStore dir, String src, String tgt, boolean isDir) {
		try {
			createSymLink(dir.toLocalFile(EFS.NONE, getMonitor()), src, tgt, isDir);
		} catch (CoreException e) {
			fail("mkLink", e);
		}
	}

	@Override
	protected void setUp() throws Exception {
		baseStore = getFileSystem().getStore(getWorkspace().getRoot().getLocation().append("temp"));
		baseStore.mkdir(EFS.NONE, null);
	}

	@Override
	protected void tearDown() throws Exception {
		baseStore.delete(EFS.NONE, null);
	}

	public void testBrokenSymlinkAttributes() {
		// Only activate this test if testing of symbolic links is possible.
		if (!canCreateSymLinks()) {
			return;
		}
		long testStartTime = System.currentTimeMillis();
		makeLinkStructure();
		//break links by removing actual dir and file
		ensureDoesNotExist(aDir);
		ensureDoesNotExist(aFile);
		fetchFileInfos();

		assertEquals(SYMLINKS_ARE_FIRST_CLASS_FILES_OR_DIRECTORIES, ilFile.exists());
		assertFalse(ilFile.isDirectory());
		assertEquals(SYMLINKS_ARE_FIRST_CLASS_FILES_OR_DIRECTORIES, illFile.exists());
		assertFalse(illFile.isDirectory());
		assertEquals(SYMLINKS_ARE_FIRST_CLASS_FILES_OR_DIRECTORIES, ilDir.exists());
		assertEquals(SYMLINKS_ARE_FIRST_CLASS_FILES_OR_DIRECTORIES, ilDir.isDirectory());
		assertEquals(SYMLINKS_ARE_FIRST_CLASS_FILES_OR_DIRECTORIES, illDir.exists());
		assertEquals(SYMLINKS_ARE_FIRST_CLASS_FILES_OR_DIRECTORIES, illDir.isDirectory());
		if (SYMLINKS_ARE_FIRST_CLASS_FILES_OR_DIRECTORIES) {
			// Symlinks on Windows have their own modification time.
			assertTrue(ilFile.getLastModified() >= testStartTime);
			assertTrue(ilDir.getLastModified() >= testStartTime);
			assertTrue(illFile.getLastModified() >= testStartTime);
			assertTrue(illDir.getLastModified() >= testStartTime);
		} else {
			assertEquals(0, ilFile.getLastModified());
			assertEquals(0, ilDir.getLastModified());
			assertEquals(0, illFile.getLastModified());
			assertEquals(0, illDir.getLastModified());
		}
		assertEquals(0, ilFile.getLength());
		assertEquals(0, ilDir.getLength());
		assertEquals(0, illFile.getLength());
		assertEquals(0, illDir.getLength());

		assertTrue(ilFile.getAttribute(EFS.ATTRIBUTE_SYMLINK));
		assertEquals(ilFile.getStringAttribute(EFS.ATTRIBUTE_LINK_TARGET), "aFile");
		assertTrue(ilDir.getAttribute(EFS.ATTRIBUTE_SYMLINK));
		assertEquals(ilDir.getStringAttribute(EFS.ATTRIBUTE_LINK_TARGET), "aDir");
		assertTrue(illFile.getAttribute(EFS.ATTRIBUTE_SYMLINK));
		assertEquals(illFile.getStringAttribute(EFS.ATTRIBUTE_LINK_TARGET), "lFile");
		assertTrue(illDir.getAttribute(EFS.ATTRIBUTE_SYMLINK));
		assertEquals(illDir.getStringAttribute(EFS.ATTRIBUTE_LINK_TARGET), "lDir");
	}

	// Moving a broken symlink is possible.
	public void testBrokenSymlinkMove() throws Exception {
		// Only activate this test if testing of symbolic links is possible.
		if (!canCreateSymLinks()) {
			return;
		}
		makeLinkStructure();
		ensureDoesNotExist(aFile);
		ensureDoesNotExist(aDir);
		IFileInfo[] infos = baseStore.childInfos(EFS.NONE, getMonitor());
		assertEquals(infos.length, 4);

		IFileStore _llFile = baseStore.getChild("_llFile");
		IFileStore _llDir = baseStore.getChild("_llDir");
		llFile.move(_llFile, EFS.NONE, getMonitor());
		llDir.move(_llDir, EFS.NONE, getMonitor());
		infos = baseStore.childInfos(EFS.NONE, getMonitor());
		assertEquals(infos.length, 4);
		assertFalse("1.0", containsSymlink(infos, llFile.getName()));
		assertFalse("1.1", containsSymlink(infos, llDir.getName()));
		assertTrue("1.2", containsSymlink(infos, _llFile.getName()));
		assertTrue("1.3", containsSymlink(infos, _llFile.getName()));

		IFileStore _lFile = baseStore.getChild("_lFile");
		IFileStore _lDir = baseStore.getChild("_lDir");
		lFile.move(_lFile, EFS.NONE, getMonitor());
		lDir.move(_lDir, EFS.NONE, getMonitor());
		infos = baseStore.childInfos(EFS.NONE, getMonitor());
		assertEquals(infos.length, 4);
		assertFalse("1.4", containsSymlink(infos, lFile.getName()));
		assertFalse("1.5", containsSymlink(infos, lDir.getName()));
		assertTrue("1.6", containsSymlink(infos, _lFile.getName()));
		assertTrue("1.7", containsSymlink(infos, _lFile.getName()));
	}

	private boolean containsSymlink(IFileInfo[] infos, String link) {
		for (IFileInfo info : infos) {
			if (link.equals(info.getName())) {
				if (info.getAttribute(EFS.ATTRIBUTE_SYMLINK)) {
					return true;
				}
			}
		}
		return false;
	}

	// Removing a broken symlink is possible.
	public void testBrokenSymlinkRemove() throws Exception {
		// Only activate this test if testing of symbolic links is possible.
		if (!canCreateSymLinks()) {
			return;
		}
		makeLinkStructure();
		ensureDoesNotExist(aFile);
		ensureDoesNotExist(aDir);
		IFileInfo[] infos = baseStore.childInfos(EFS.NONE, getMonitor());
		assertEquals(infos.length, 4);
		llFile.delete(EFS.NONE, getMonitor());
		llDir.delete(EFS.NONE, getMonitor());
		infos = baseStore.childInfos(EFS.NONE, getMonitor());
		assertEquals(infos.length, 2);
		lFile.delete(EFS.NONE, getMonitor());
		lDir.delete(EFS.NONE, getMonitor());
		infos = baseStore.childInfos(EFS.NONE, getMonitor());
		assertEquals(infos.length, 0);
	}

	public void testRecursiveSymlink() throws Exception {
		// Only activate this test if testing of symbolic links is possible.
		if (!canCreateSymLinks()) {
			return;
		}
		mkLink(baseStore, "l1", "l2", false);
		mkLink(baseStore, "l2", "l1", false);
		IFileStore l1 = baseStore.getChild("l1");
		IFileInfo i1 = l1.fetchInfo();
		assertEquals(SYMLINKS_ARE_FIRST_CLASS_FILES_OR_DIRECTORIES, i1.exists());
		assertFalse(i1.isDirectory());

		assertTrue(i1.getAttribute(EFS.ATTRIBUTE_SYMLINK));
		assertEquals("l2", i1.getStringAttribute(EFS.ATTRIBUTE_LINK_TARGET));

		IFileInfo[] infos = baseStore.childInfos(EFS.NONE, getMonitor());
		assertEquals(infos.length, 2);
		i1.setAttribute(EFS.ATTRIBUTE_READ_ONLY, true);
		boolean exceptionThrown = false;
		try {
			l1.putInfo(i1, EFS.SET_ATTRIBUTES, getMonitor());
		} catch (CoreException ce) {
			exceptionThrown = true;
		}
		i1 = l1.fetchInfo();
		boolean fixMeFixed = false;
		if (fixMeFixed) {
			//FIXME bug: putInfo neither sets attributes nor throws an exception for broken symbolic links
			assertTrue(exceptionThrown);
			assertTrue(i1.getAttribute(EFS.ATTRIBUTE_READ_ONLY));
		}
		assertEquals(SYMLINKS_ARE_FIRST_CLASS_FILES_OR_DIRECTORIES, i1.exists());

		i1.setLastModified(12345);
		exceptionThrown = false;
		try {
			l1.putInfo(i1, EFS.SET_LAST_MODIFIED, getMonitor());
		} catch (CoreException ce) {
			exceptionThrown = true;
		}
		i1 = l1.fetchInfo();
		//FIXME bug: putInfo neither sets attributes nor throws an exception for broken symbolic links
		//assertTrue(exceptionThrown);
		//assertEquals(i1.getLastModified(), 12345);
		assertEquals(SYMLINKS_ARE_FIRST_CLASS_FILES_OR_DIRECTORIES, i1.exists());

		l1.delete(EFS.NONE, getMonitor());
		infos = baseStore.childInfos(EFS.NONE, getMonitor());
		assertEquals(infos.length, 1);
	}

	public void testSymlinkAttributes() {
		// Only activate this test if testing of symbolic links is possible.
		if (!canCreateSymLinks()) {
			return;
		}
		makeLinkStructure();
		assertFalse(iFile.getAttribute(EFS.ATTRIBUTE_SYMLINK));
		assertFalse(iDir.getAttribute(EFS.ATTRIBUTE_SYMLINK));
		//valid links
		assertTrue(ilFile.exists());
		assertFalse(ilFile.isDirectory());
		assertTrue(illFile.exists());
		assertFalse(illFile.isDirectory());
		assertTrue(ilDir.exists());
		assertTrue(ilDir.isDirectory());
		assertTrue(illDir.exists());
		assertTrue(illDir.isDirectory());
		if (SYMLINKS_ARE_FIRST_CLASS_FILES_OR_DIRECTORIES) {
			// Symlinks on Windows have their own modification time and zero size.
			assertTrue(illFile.getLastModified() >= iFile.getLastModified());
			assertEquals(0, illFile.getLength());
			assertTrue(illDir.getLastModified() >= iDir.getLastModified());
			assertEquals(0, illDir.getLength());
		} else {
			assertEquals(iFile.getLastModified(), illFile.getLastModified());
			assertEquals(iFile.getLength(), illFile.getLength());
			assertEquals(iDir.getLastModified(), illDir.getLastModified());
			assertEquals(iDir.getLength(), illDir.getLength());
		}

		assertTrue(ilFile.getAttribute(EFS.ATTRIBUTE_SYMLINK));
		assertEquals(ilFile.getStringAttribute(EFS.ATTRIBUTE_LINK_TARGET), "aFile");
		assertTrue(ilDir.getAttribute(EFS.ATTRIBUTE_SYMLINK));
		assertEquals(ilDir.getStringAttribute(EFS.ATTRIBUTE_LINK_TARGET), "aDir");
		assertTrue(illFile.getAttribute(EFS.ATTRIBUTE_SYMLINK));
		assertEquals(illFile.getStringAttribute(EFS.ATTRIBUTE_LINK_TARGET), "lFile");
		assertTrue(illDir.getAttribute(EFS.ATTRIBUTE_SYMLINK));
		assertEquals(illDir.getStringAttribute(EFS.ATTRIBUTE_LINK_TARGET), "lDir");
	}

	// Reading from a directory pointed to by a link is possible.
	public void testSymlinkDirRead() throws Exception {
		// Only activate this test if testing of symbolic links is possible.
		if (!canCreateSymLinks()) {
			return;
		}
		makeLinkStructure();
		IFileStore childDir = aDir.getChild("subDir");
		ensureExists(childDir, true);
		IFileInfo[] infos = llDir.childInfos(EFS.NONE, getMonitor());
		assertEquals(infos.length, 1);
		assertTrue(infos[0].isDirectory());
		assertFalse(infos[0].getAttribute(EFS.ATTRIBUTE_SYMLINK));
		assertNull(infos[0].getStringAttribute(EFS.ATTRIBUTE_LINK_TARGET));
		assertEquals(infos[0].getName(), "subDir");
		ensureDoesNotExist(childDir);
	}

	// Writing to symlinked dir.
	public void testSymlinkDirWrite() throws Exception {
		// Only activate this test if testing of symbolic links is possible.
		if (!canCreateSymLinks()) {
			return;
		}
		makeLinkStructure();
		IFileStore childFile = llDir.getChild("subFile");
		ensureExists(childFile, false);
		IFileInfo[] infos = aDir.childInfos(EFS.NONE, getMonitor());
		assertEquals(infos.length, 1);
		assertFalse(infos[0].isDirectory());
		assertFalse(infos[0].getAttribute(EFS.ATTRIBUTE_SYMLINK));
		assertNull(infos[0].getStringAttribute(EFS.ATTRIBUTE_LINK_TARGET));
		assertEquals(infos[0].getName(), "subFile");

		//writing to broken symlink
		ensureDoesNotExist(aDir);
		childFile = llDir.getChild("subFile");
		OutputStream out = null;
		boolean exceptionThrown = false;
		try {
			out = childFile.openOutputStream(EFS.NONE, getMonitor());
		} catch (CoreException ce) {
			exceptionThrown = true;
		}
		if (out != null) {
			out.close();
		}
		assertNull(out);
		assertTrue(exceptionThrown);
	}

	public void testSymlinkEnabled() {
		assertTrue(haveSymlinks());
	}

	/**
	 * TODO Fix this test.  See https://bugs.eclipse.org/bugs/show_bug.cgi?id=172346
	 */
	public void _testSymlinkExtendedChars() throws Exception {
		// Only activate this test if testing of symbolic links is possible.
		if (!canCreateSymLinks()) {
			return;
		}
		IFileStore childDir = baseStore.getChild(specialCharName);
		ensureExists(childDir, true);
		IFileStore childFile = baseStore.getChild("ff" + specialCharName);
		ensureExists(childFile, false);
		mkLink(baseStore, "l" + specialCharName, specialCharName, true);
		mkLink(baseStore, "lf" + specialCharName, "ff" + specialCharName, false);
		IFileInfo[] infos = baseStore.childInfos(EFS.NONE, getMonitor());
		assertEquals("0.1", infos.length, 4);
		for (IFileInfo info : infos) {
			String infoName = info.getName();
			assertTrue("1." + infoName, infoName.endsWith(specialCharName));
			assertTrue("2." + infoName, info.exists());
			if (info.getName().charAt(1) == 'f') {
				assertFalse("3." + infoName, info.isDirectory());
			} else {
				assertTrue("4." + infoName, info.isDirectory());
			}
			if (info.getName().charAt(0) == 'l') {
				assertTrue("5." + infoName, info.getAttribute(EFS.ATTRIBUTE_SYMLINK));
				assertTrue("6." + infoName, info.getStringAttribute(EFS.ATTRIBUTE_LINK_TARGET).endsWith(specialCharName));
			}
		}
	}

	public void testSymlinkPutLastModified() throws Exception {
		// Only activate this test if testing of symbolic links is possible.
		if (!canCreateSymLinks()) {
			return;
		}
		if (Platform.OS_MACOSX.equals(Platform.getOS())) {
			// flag EFS.SET_LAST_MODIFIED is set by java.io and it fails on Mac OS
			return;
		}
		//check that putInfo() "writes through" the symlink
		makeLinkStructure();
		long oldTime = iFile.getLastModified();
		long timeToSet = oldTime - 100000;
		illFile.setLastModified(timeToSet);
		llFile.putInfo(illFile, EFS.SET_ATTRIBUTES | EFS.SET_LAST_MODIFIED, getMonitor());
		iFile = aFile.fetchInfo();
		assertEquals(iFile.getLastModified(), timeToSet);

		oldTime = iDir.getLastModified();
		timeToSet = oldTime - 100000;
		illDir.setLastModified(timeToSet);
		llDir.putInfo(illDir, EFS.SET_ATTRIBUTES | EFS.SET_LAST_MODIFIED, getMonitor());
		iDir = aDir.fetchInfo();
		assertTrue(iDir.getLastModified() != oldTime);
		assertEquals(iDir.getLastModified(), timeToSet);

		// Check that link properties are maintained even through putInfo
		illFile = llFile.fetchInfo();
		illDir = llDir.fetchInfo();
		assertTrue(illFile.getAttribute(EFS.ATTRIBUTE_SYMLINK));
		assertTrue(illDir.getAttribute(EFS.ATTRIBUTE_SYMLINK));
		assertEquals(illFile.getStringAttribute(EFS.ATTRIBUTE_LINK_TARGET), "lFile");
		assertEquals(illDir.getStringAttribute(EFS.ATTRIBUTE_LINK_TARGET), "lDir");
	}

	public void testSymlinkPutReadOnly() throws Exception {
		// Only activate this test if testing of symbolic links is possible.
		if (!canCreateSymLinks()) {
			return;
		}
		//check that putInfo() "writes through" the symlink
		makeLinkStructure();
		illFile.setAttribute(EFS.ATTRIBUTE_READ_ONLY, true);
		llFile.putInfo(illFile, EFS.SET_ATTRIBUTES, getMonitor());
		iFile = aFile.fetchInfo();
		assertEquals(!SYMLINKS_ARE_FIRST_CLASS_FILES_OR_DIRECTORIES, iFile.getAttribute(EFS.ATTRIBUTE_READ_ONLY));

		illFile.setAttribute(EFS.ATTRIBUTE_READ_ONLY, false);
		llFile.putInfo(illFile, EFS.SET_ATTRIBUTES, getMonitor());
		iFile = aFile.fetchInfo();
		assertFalse(iFile.getAttribute(EFS.ATTRIBUTE_READ_ONLY));

		illDir.setAttribute(EFS.ATTRIBUTE_READ_ONLY, true);
		llDir.putInfo(illDir, EFS.SET_ATTRIBUTES, getMonitor());
		iDir = aDir.fetchInfo();
		assertEquals(!SYMLINKS_ARE_FIRST_CLASS_FILES_OR_DIRECTORIES, iDir.getAttribute(EFS.ATTRIBUTE_READ_ONLY));

		illDir.setAttribute(EFS.ATTRIBUTE_READ_ONLY, false);
		llDir.putInfo(illDir, EFS.SET_ATTRIBUTES, getMonitor());
		iDir = aDir.fetchInfo();
		assertFalse(iDir.getAttribute(EFS.ATTRIBUTE_READ_ONLY));

		// Check that link properties are maintained even through putInfo
		illFile = llFile.fetchInfo();
		illDir = llDir.fetchInfo();
		assertTrue(illFile.getAttribute(EFS.ATTRIBUTE_SYMLINK));
		assertTrue(illDir.getAttribute(EFS.ATTRIBUTE_SYMLINK));
		assertEquals(illFile.getStringAttribute(EFS.ATTRIBUTE_LINK_TARGET), "lFile");
		assertEquals(illDir.getStringAttribute(EFS.ATTRIBUTE_LINK_TARGET), "lDir");
	}

	public void testSymlinkPutExecutable() throws Exception {
		if (!isAttributeSupported(EFS.ATTRIBUTE_EXECUTABLE)) {
			return;
		}
		// Only activate this test if testing of symbolic links is possible.
		if (!canCreateSymLinks())
		 {
			return;
		// ATTRIBUTE_EXECUTABLE is not supported on Windows, so
		// SYMLINKS_ARE_FIRST_CLASS_FILES_OR_DIRECTORIES is false in this context.
		}

		//check that putInfo() "writes through" the symlink
		makeLinkStructure();
		illFile.setAttribute(EFS.ATTRIBUTE_EXECUTABLE, true);
		llFile.putInfo(illFile, EFS.SET_ATTRIBUTES, getMonitor());
		iFile = aFile.fetchInfo();
		assertTrue(iFile.getAttribute(EFS.ATTRIBUTE_EXECUTABLE));

		illDir.setAttribute(EFS.ATTRIBUTE_EXECUTABLE, false);
		llDir.putInfo(illDir, EFS.SET_ATTRIBUTES, getMonitor());
		iDir = aDir.fetchInfo();
		assertFalse(iDir.getAttribute(EFS.ATTRIBUTE_EXECUTABLE));

		// Check that link properties are maintained even through putInfo
		illFile = llFile.fetchInfo();
		illDir = llDir.fetchInfo();
		assertTrue(illFile.getAttribute(EFS.ATTRIBUTE_SYMLINK));
		assertTrue(illDir.getAttribute(EFS.ATTRIBUTE_SYMLINK));
		assertEquals(illFile.getStringAttribute(EFS.ATTRIBUTE_LINK_TARGET), "lFile");
		assertEquals(illDir.getStringAttribute(EFS.ATTRIBUTE_LINK_TARGET), "lDir");
	}

	public void testSymlinkPutHidden() throws Exception {
		if (!isAttributeSupported(EFS.ATTRIBUTE_HIDDEN)) {
			return;
		}
		// Only activate this test if testing of symbolic links is possible.
		if (!canCreateSymLinks())
		 {
			return;
		// ATTRIBUTE_HIDDEN is supported only on Windows, so
		// SYMLINKS_ARE_FIRST_CLASS_FILES_OR_DIRECTORIES is true in this context.
		}

		// Check that putInfo() applies the attribute to the symlink itself.
		makeLinkStructure();
		illFile.setAttribute(EFS.ATTRIBUTE_HIDDEN, true);
		llFile.putInfo(illFile, EFS.SET_ATTRIBUTES, getMonitor());
		illFile = llFile.fetchInfo();
		assertTrue(illFile.getAttribute(EFS.ATTRIBUTE_HIDDEN));
		iFile = aFile.fetchInfo();
		assertFalse(iFile.getAttribute(EFS.ATTRIBUTE_HIDDEN));

		illDir.setAttribute(EFS.ATTRIBUTE_HIDDEN, true);
		llDir.putInfo(illDir, EFS.SET_ATTRIBUTES, getMonitor());
		illDir = llDir.fetchInfo();
		assertTrue(illDir.getAttribute(EFS.ATTRIBUTE_HIDDEN));
		iDir = aDir.fetchInfo();
		assertFalse(iDir.getAttribute(EFS.ATTRIBUTE_HIDDEN));

		// Check that link properties are maintained even through putInfo.
		illFile = llFile.fetchInfo();
		illDir = llDir.fetchInfo();
		assertTrue(illFile.getAttribute(EFS.ATTRIBUTE_SYMLINK));
		assertTrue(illDir.getAttribute(EFS.ATTRIBUTE_SYMLINK));
		assertEquals(illFile.getStringAttribute(EFS.ATTRIBUTE_LINK_TARGET), "lFile");
		assertEquals(illDir.getStringAttribute(EFS.ATTRIBUTE_LINK_TARGET), "lDir");
	}

	// Removing a symlink keeps the link target intact.
	// Symlinks being broken due to remove are set to non-existent.
	public void testSymlinkRemove() throws Exception {
		// Only activate this test if testing of symbolic links is possible.
		if (!canCreateSymLinks()) {
			return;
		}
		makeLinkStructure();
		lFile.delete(EFS.NONE, getMonitor());
		illFile = lFile.fetchInfo();
		assertFalse(illFile.exists());
		iFile = aFile.fetchInfo();
		assertTrue(iFile.exists());

		IFileStore childFile = aDir.getChild("subFile");
		ensureExists(childFile, false);
		lDir.delete(EFS.NONE, getMonitor());
		illDir = lDir.fetchInfo();
		assertFalse(illFile.exists());
		iDir = aDir.fetchInfo();
		assertTrue(iDir.exists());
		// Check that the contents of the directory are preserved.
		IFileInfo iFileInsideDir = childFile.fetchInfo();
		assertTrue(iFileInsideDir.exists());
	}

}
