/*******************************************************************************
 * Copyright (c) 2026 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.core.tests.filesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.internal.filesystem.local.LocalFileNativesManager;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link org.eclipse.core.internal.filesystem.local.NativeHandler}
 * implementations.
 */
public class NativeHandlerTest {

	@TempDir
	Path tempDirectory;

	@ParameterizedTest
	@ValueSource(ints = { EFS.NONE, EFS.IGNORE_NAME_CASE })
	public void testFetchFileInfoOfNonExistingFileHasName(int options) {
		String path = tempDirectory.resolve("doesNotExist.txt").toString();
		IFileInfo info = LocalFileNativesManager.fetchFileInfo(path, options);

		assertFalse(info.exists());
		assertEquals("doesNotExist.txt", info.getName());
	}

	@ParameterizedTest
	@ValueSource(ints = { EFS.NONE, EFS.IGNORE_NAME_CASE })
	public void testFetchFileInfoInNonExistingParentHasName(int options) {
		String path = tempDirectory.resolve("doesNotExist").resolve("doesNotExist.txt").toString();
		IFileInfo info = LocalFileNativesManager.fetchFileInfo(path, options);

		assertFalse(info.exists());
		assertEquals("doesNotExist.txt", info.getName());
	}

	/**
	 * https://github.com/eclipse-platform/eclipse.platform/issues/3024
	 */
	@RepeatedTest(value = 5, failureThreshold = 1)
	void testListDirectoryAndGetFileInfosNeverReturnsNamelessEntry() throws Exception {
		List<Path> files = new ArrayList<>();
		// Fill directory with files
		for (int i = 0; i < 2000; i++) {
			files.add(Files.createFile(tempDirectory.resolve("file" + i + ".tmp")));
		}
		// Delete files concurrently
		Thread deleter = new Thread(() -> files.forEach(f -> f.toFile().delete()));
		deleter.start();
		try {
			// list directory while deletion is running
			while (deleter.isAlive()) {
				IFileInfo[] infos = LocalFileNativesManager.listDirectoryAndGetFileInfos(tempDirectory.toString());
				for (IFileInfo info : infos) {
					assertFalse(info.getName().isEmpty(), "entry with empty name: " + info);
				}
			}
		} finally {
			deleter.join();
		}
	}

}
