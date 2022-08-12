/*******************************************************************************
 *  Copyright (c) 2006, 2012 IBM Corporation and others.
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

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileSystem;

/**
 * Tests public API methods of the class EFS.
 * @see EFS
 */
public class EFSTest extends FileSystemTest {
	public void testGetLocalFileSystem() {
		IFileSystem system = EFS.getLocalFileSystem();
		assertNotNull("1.0", system);
		assertEquals("1.1", "file", system.getScheme());
	}

	public void testGetNullFileSystem() {
		IFileSystem system = EFS.getNullFileSystem();
		assertNotNull("1.0", system);
		assertEquals("1.1", "null", system.getScheme());
	}
}
