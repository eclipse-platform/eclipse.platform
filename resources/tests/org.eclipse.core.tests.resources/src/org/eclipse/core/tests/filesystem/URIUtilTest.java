/*******************************************************************************
 * Copyright (c) 2007, 2012 IBM Corporation and others.
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

import java.net.URI;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.tests.internal.filesystem.wrapper.WrapperFileSystem;

/**
 * Tests API methods of the class {@link org.eclipse.core.filesystem.URIUtil}.
 */
public class URIUtilTest extends FileSystemTest {
	/**
	 * Tests API method {@link org.eclipse.core.filesystem.URIUtil#equals(java.net.URI, java.net.URI)}.
	 */
	public void testEquals() {
		if (EFS.getLocalFileSystem().isCaseSensitive()) {
			//test that case variants are not equal
			URI one = new java.io.File("c:\\temp\\test").toURI();
			URI two = new java.io.File("c:\\TEMP\\test").toURI();
			assertTrue("1.0", !URIUtil.equals(one, two));
		} else {
			//test that case variants are equal
			URI one = new java.io.File("c:\\temp\\test").toURI();
			URI two = new java.io.File("c:\\TEMP\\test").toURI();
			assertTrue("1.0", URIUtil.equals(one, two));
		}

	}

	/**
	 * Tests API method {@link org.eclipse.core.filesystem.URIUtil#toURI(org.eclipse.core.runtime.IPath)}.
	 */
	public void testPathToURI() {
		if (Platform.getOS().equals(Platform.OS_WIN32)) {
			//path with spaces
			assertEquals("1.0", "/c:/temp/with spaces", URIUtil.toURI("c:\\temp\\with spaces").getSchemeSpecificPart());
		} else {
			//path with spaces
			assertEquals("2.0", "/tmp/with spaces", URIUtil.toURI("/tmp/with spaces").getSchemeSpecificPart());
		}
	}

	/**
	 * Tests API method {@link org.eclipse.core.filesystem.URIUtil#toURI(String)}.
	 */
	public void testStringToURI() {
		if (Platform.getOS().equals(Platform.OS_WIN32)) {
			assertEquals("1.0", "/c:/temp/with spaces", URIUtil.toURI(IPath.fromOSString("c:\\temp\\with spaces")).getSchemeSpecificPart());
		} else {
			assertEquals("1.0", "/tmp/with spaces", URIUtil.toURI(IPath.fromOSString("/tmp/with spaces")).getSchemeSpecificPart());
		}
	}

	/**
	 * Tests API method {@link org.eclipse.core.filesystem.URIUtil#toPath(java.net.URI)}.
	 */
	public void testToPath() throws Exception {
		// Relative path
		String pathString = "test/path with/spaces to_file.txt";
		assertEquals("1.0", IPath.fromOSString(pathString), URIUtil.toPath(URIUtil.toURI(pathString, false)));
		// Absolute path
		if (Platform.getOS().equals(Platform.OS_WIN32)) {
			pathString = "c:/test/path with/spaces to_file.txt";
		} else {
			pathString = "/test/path with/spaces to_file.txt";
		}
		assertEquals("2.0", IPath.fromOSString(pathString), URIUtil.toPath(URIUtil.toURI(pathString)));
		// User defined file system
		assertEquals("3.0", IPath.fromOSString(pathString), URIUtil.toPath(WrapperFileSystem.getWrappedURI(URIUtil.toURI(pathString))));
	}

	/**
	 * Test API methods {@link org.eclipse.core.filesystem.URIUtil#toURI(IPath)},
	 * {@link org.eclipse.core.filesystem.URIUtil#toURI(String)} results equality
	 */
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
		assertEquals("1.0", uri01, uri02);
	}

	/**
	 * Test API methods {@link org.eclipse.core.filesystem.URIUtil#toURI(IPath)},
	 * {@link org.eclipse.core.filesystem.URIUtil#toURI(String)} results equality
	 */
	public void testToURIRelative() {
		String pathString = "test/path with/spaces to_file.txt";
		IPath path = IPath.fromOSString(pathString);
		URI uri01 = URIUtil.toURI(path);
		URI uri02 = URIUtil.toURI(pathString, false);
		assertEquals("1.0", uri01, uri02);
		assertTrue("1.1", !uri01.isAbsolute());
		assertTrue("1.2", !uri02.isAbsolute());
	}

	/**
	 * Test API methods {@link org.eclipse.core.filesystem.URIUtil#toURI(org.eclipse.core.runtime.IPath)}.
	 * {@link org.eclipse.core.filesystem.URIUtil#toPath(URI)} transformation with relative and absolute paths
	 */
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

		assertEquals("1.0", aPath.toString(), URIUtil.toPath(aUri).toString());
		assertEquals("2.0", rPath.toString(), URIUtil.toPath(rUri).toString());
	}

	public void testBug291323_doubleDotLocationPath() {
		URI aUri = URIUtil.toURI("..");
		URI bUri = URIUtil.toURI("");
		assertEquals("1.0", URIUtil.toPath(bUri).toString(), URIUtil.toPath(aUri).toString());
	}
}
