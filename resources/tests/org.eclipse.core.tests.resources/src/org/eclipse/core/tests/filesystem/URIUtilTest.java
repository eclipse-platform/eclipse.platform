/*******************************************************************************
 * Copyright (c) 2007, 2025 IBM Corporation and others.
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
package org.eclipse.core.tests.filesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.net.URI;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.tests.internal.filesystem.wrapper.WrapperFileSystem;
import org.junit.Test;

/**
 * Tests API methods of the class {@link org.eclipse.core.filesystem.URIUtil}.
 */
public class URIUtilTest {
	/**
	 * Tests API method {@link org.eclipse.core.filesystem.URIUtil#equals(java.net.URI, java.net.URI)}.
	 */
	@Test
	public void testEquals() {
		if (EFS.getLocalFileSystem().isCaseSensitive()) {
			//test that case variants are not equal
			URI one = new java.io.File("c:\\temp\\test").toURI();
			URI two = new java.io.File("c:\\TEMP\\test").toURI();
			assertFalse(URIUtil.equals(one, two));
		} else {
			//test that case variants are equal
			URI one = new java.io.File("c:\\temp\\test").toURI();
			URI two = new java.io.File("c:\\TEMP\\test").toURI();
			assertTrue(URIUtil.equals(one, two));
		}

	}

	/**
	 * Tests API method {@link org.eclipse.core.filesystem.URIUtil#toURI(org.eclipse.core.runtime.IPath)}.
	 */
	@Test
	public void testPathToURI() {
		if (Platform.getOS().equals(Platform.OS_WIN32)) {
			//path with spaces
			assertEquals("/c:/temp/with spaces", URIUtil.toURI("c:\\temp\\with spaces").getSchemeSpecificPart());
		} else {
			//path with spaces
			assertEquals("/tmp/with spaces", URIUtil.toURI("/tmp/with spaces").getSchemeSpecificPart());
		}
	}

	/**
	 * Tests API method {@link org.eclipse.core.filesystem.URIUtil#toURI(String)}.
	 */
	@Test
	public void testStringToURI() {
		if (Platform.getOS().equals(Platform.OS_WIN32)) {
			assertEquals("/c:/temp/with spaces",
					URIUtil.toURI(IPath.fromOSString("c:\\temp\\with spaces")).getSchemeSpecificPart());
		} else {
			assertEquals("/tmp/with spaces",
					URIUtil.toURI(IPath.fromOSString("/tmp/with spaces")).getSchemeSpecificPart());
		}
	}

	/**
	 * Tests API method {@link org.eclipse.core.filesystem.URIUtil#toPath(java.net.URI)}.
	 */
	@Test
	public void testToPath() throws Exception {
		// Relative path
		String pathString = "test/path with/spaces to_file.txt";
		assertEquals(IPath.fromOSString(pathString), URIUtil.toPath(URIUtil.toURI(pathString, false)));
		// Absolute path
		if (Platform.getOS().equals(Platform.OS_WIN32)) {
			pathString = "c:/test/path with/spaces to_file.txt";
		} else {
			pathString = "/test/path with/spaces to_file.txt";
		}
		assertEquals(IPath.fromOSString(pathString), URIUtil.toPath(URIUtil.toURI(pathString)));
		// User defined file system
		assertEquals(IPath.fromOSString(pathString),
				URIUtil.toPath(WrapperFileSystem.getWrappedURI(URIUtil.toURI(pathString))));
	}

	/**
	 * Test API methods {@link org.eclipse.core.filesystem.URIUtil#toURI(IPath)},
	 * {@link org.eclipse.core.filesystem.URIUtil#toURI(String)} results equality
	 */
	@Test
	public void testToURIAbsolute() {
		String pathString = null;
		if (Platform.getOS().equals(Platform.OS_WIN32)) {
			pathString = "c:/test/path with/spaces to_file.txt";
		} else {
			pathString = "/test/path with/spaces to_file.txt";
		}
		IPath path = IPath.fromOSString(pathString);
		URI uri01 = URIUtil.toURI(path);
		URI uri02 = URIUtil.toURI(pathString);
		assertEquals(uri01, uri02);
	}

	/**
	 * Test API methods {@link org.eclipse.core.filesystem.URIUtil#toURI(IPath)},
	 * {@link org.eclipse.core.filesystem.URIUtil#toURI(String)} results equality
	 */
	@Test
	public void testToURIRelative() {
		String pathString = "test/path with/spaces to_file.txt";
		IPath path = IPath.fromOSString(pathString);
		URI uri01 = URIUtil.toURI(path);
		URI uri02 = URIUtil.toURI(pathString, false);
		assertEquals(uri01, uri02);
		assertFalse(uri01.isAbsolute());
		assertFalse(uri02.isAbsolute());
	}

	/**
	 * Test API methods {@link org.eclipse.core.filesystem.URIUtil#toURI(org.eclipse.core.runtime.IPath)}.
	 * {@link org.eclipse.core.filesystem.URIUtil#toPath(URI)} transformation with relative and absolute paths
	 */
	@Test
	public void testFromPathToURI() {
		//absolute path
		IPath aPath = null;
		if (Platform.getOS().equals(Platform.OS_WIN32)) {
			aPath = IPath.fromOSString("c:/test/path with spaces/to_file.txt");
		} else {
			aPath = IPath.fromOSString("/test/path with spaces/to_file.txt");
		}
		//relative path
		IPath rPath = IPath.fromOSString("relative/with spaces/path/to_file.txt");

		URI aUri = URIUtil.toURI(aPath);
		URI rUri = URIUtil.toURI(rPath);

		assertEquals(aPath.toString(), URIUtil.toPath(aUri).toString());
		assertEquals(rPath.toString(), URIUtil.toPath(rUri).toString());
	}

	@Test
	public void testBug291323_doubleDotLocationPath() {
		URI aUri = URIUtil.toURI("..");
		URI bUri = URIUtil.toURI("");
		assertEquals(URIUtil.toPath(bUri).toString(), URIUtil.toPath(aUri).toString());
	}
}
