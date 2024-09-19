/*******************************************************************************
 * Copyright (c) 2014 Google Inc and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Sergey Prigogin - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.internal.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.internal.utils.FileUtil;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.tests.harness.FileSystemHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link FileUtil} class.
 */
public class FileUtilTest {
	private IPath baseTestDir;

	@BeforeEach
	public void setUp() throws Exception {
		baseTestDir = FileSystemHelper.getRandomLocation();
		baseTestDir.toFile().mkdirs();
	}

	@AfterEach
	public void tearDown() throws Exception {
		FileSystemHelper.clear(baseTestDir.toFile());
	}

	@Test
	public void testRealPath() throws Exception {
		IPath realPath = baseTestDir.append("Test.TXT");
		realPath.toFile().createNewFile();
		IPath testPath;
		if (EFS.getLocalFileSystem().isCaseSensitive()) {
			testPath = realPath;
		} else {
			testPath = IPath.fromOSString(realPath.toOSString().toLowerCase());
		}
		assertEquals(realPath, FileUtil.realPath(testPath));
	}

	@Test
	public void testRealPathOfNonexistingFile() throws Exception {
		IPath realPath = baseTestDir.append("ExistingDir");
		realPath.toFile().mkdirs();
		IPath testPath;
		if (EFS.getLocalFileSystem().isCaseSensitive()) {
			testPath = realPath;
		} else {
			testPath = IPath.fromOSString(realPath.toOSString().toLowerCase());
		}
		String suffix = "NonexistingDir/NonexistingFile.txt";
		assertEquals(realPath.append(suffix), FileUtil.realPath(testPath.append(suffix)));
	}

	@Test
	public void testRealURI() throws Exception {
		IPath realPath = baseTestDir.append("Test.TXT");
		realPath.toFile().createNewFile();
		IPath testPath;
		if (EFS.getLocalFileSystem().isCaseSensitive()) {
			testPath = realPath;
		} else {
			testPath = IPath.fromOSString(realPath.toOSString().toLowerCase());
		}
		URI realURI = URIUtil.toURI(realPath);
		URI testURI = URIUtil.toURI(testPath);
		assertEquals(realURI, FileUtil.realURI(testURI));
	}
}