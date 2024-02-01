/*******************************************************************************
 *  Copyright (c) 2005, 2019 IBM Corporation and others.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.tests.filesystem.FileSystemTestUtil.ensureDoesNotExist;
import static org.eclipse.core.tests.filesystem.FileSystemTestUtil.getMonitor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.filesystem.FileStoreCreationRule.FileSystemType;
import org.junit.Rule;
import org.junit.Test;

public class OpenOutputStreamTest {
	@Rule
	public final FileStoreCreationRule fileStoreRule = new FileStoreCreationRule(FileSystemType.IN_MEMORY);

	@Test
	public void testAppend() throws Exception {
		IFileStore baseStore = fileStoreRule.getFileStore();
		IFileStore file = baseStore.getChild("file");
		ensureDoesNotExist(file);

		final int BYTE_ONE = 1;
		final int BYTE_TWO = 2;
		final int EOF = -1;

		try (OutputStream out = file.openOutputStream(EFS.APPEND, getMonitor())){
			out.write(BYTE_ONE);
		}
		//append some more content
		try (OutputStream out = file.openOutputStream(EFS.APPEND, getMonitor())) {
			out.write(BYTE_TWO);
		}
		//file should contain two bytes
		try (InputStream in = file.openInputStream(EFS.NONE, getMonitor())) {
			assertEquals("1.0", BYTE_ONE, in.read());
			assertEquals("1.1", BYTE_TWO, in.read());
			assertEquals("1.2", EOF, in.read());
		}
	}

	@Test
	public void testParentExists() throws Exception {
		IFileStore baseStore = fileStoreRule.getFileStore();
		IFileStore file = baseStore.getChild("file");
		ensureDoesNotExist(file);

		try (OutputStream out = file.openOutputStream(EFS.NONE, getMonitor())) {
			out.write(1);
		}
		final IFileInfo info = file.fetchInfo();
		assertExists(file);
		assertTrue("1.1", !info.isDirectory());
		assertEquals("1.2", file.getName(), info.getName());
	}

	private static void assertExists(IFileStore store) throws CoreException {
		IFileInfo info = store.fetchInfo();
		assertTrue("store has no file info: " + store, info.exists());
		// check that the parent knows about it
		IFileInfo[] children = store.getParent().childInfos(EFS.NONE, getMonitor());
		List<String> childrenNames = Stream.of(children).map(IFileInfo::getName).collect(Collectors.toList());
		assertThat(childrenNames).contains(store.getName());
	}

	@Test
	public void testParentNotExists() throws CoreException {
		IFileStore baseStore = fileStoreRule.getFileStore();
		IFileStore dir = baseStore.getChild("dir");
		IFileStore file = dir.getChild("file");
		ensureDoesNotExist(dir);

		assertThrows(CoreException.class, () -> {
			file.openOutputStream(EFS.NONE, getMonitor());
			fail("1.0");
		});
		final IFileInfo info = file.fetchInfo();
		assertTrue("1.1", !info.exists());
		assertTrue("1.2", !info.isDirectory());
	}
}
