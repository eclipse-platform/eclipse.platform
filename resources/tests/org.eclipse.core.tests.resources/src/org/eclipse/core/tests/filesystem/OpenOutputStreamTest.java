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

import java.io.*;
import org.eclipse.core.filesystem.*;
import org.eclipse.core.runtime.CoreException;

public class OpenOutputStreamTest extends FileSystemTest {
	public void testAppend() {
		IFileStore file = baseStore.getChild("file");
		ensureDoesNotExist(file);

		final int BYTE_ONE = 1;
		final int BYTE_TWO = 2;
		final int EOF = -1;

		try (OutputStream out = file.openOutputStream(EFS.APPEND, getMonitor())){
			out.write(BYTE_ONE);
		} catch (CoreException e) {
			fail("1.99", e);
		} catch (IOException e) {
			fail("2.99", e);
		}
		//append some more content
		try (OutputStream out = file.openOutputStream(EFS.APPEND, getMonitor())) {
			out.write(BYTE_TWO);
		} catch (CoreException e) {
			fail("3.99", e);
		} catch (IOException e) {
			fail("4.99", e);
		}
		//file should contain two bytes
		try (InputStream in = file.openInputStream(EFS.NONE, getMonitor())) {
			assertEquals("1.0", BYTE_ONE, in.read());
			assertEquals("1.1", BYTE_TWO, in.read());
			assertEquals("1.2", EOF, in.read());
		} catch (CoreException | IOException e) {
			fail("4.99", e);
		}

	}

	public void testParentExists() {
		IFileStore file = baseStore.getChild("file");
		ensureDoesNotExist(file);

		try (OutputStream out = file.openOutputStream(EFS.NONE, getMonitor())) {
			out.write(1);
		} catch (CoreException e) {
			fail("1.99", e);
		} catch (IOException e) {
			fail("2.99", e);
		}
		final IFileInfo info = file.fetchInfo();
		assertExists("1.0", file);
		assertTrue("1.1", !info.isDirectory());
		assertEquals("1.2", file.getName(), info.getName());
	}

	public void testParentNotExists() {
		IFileStore dir = baseStore.getChild("dir");
		IFileStore file = dir.getChild("file");
		ensureDoesNotExist(dir);

		try {
			file.openOutputStream(EFS.NONE, getMonitor());
			fail("1.0");
		} catch (CoreException e) {
			//should fail
		}
		final IFileInfo info = file.fetchInfo();
		assertTrue("1.1", !info.exists());
		assertTrue("1.2", !info.isDirectory());
	}
}
