/*******************************************************************************
 *  Copyright (c) 2006, 2025 IBM Corporation and others.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileSystem;
import org.junit.jupiter.api.Test;

/**
 * Tests public API methods of the class EFS.
 * @see EFS
 */
public class EFSTest {
	@Test
	public void testGetLocalFileSystem() {
		IFileSystem system = EFS.getLocalFileSystem();
		assertNotNull(system);
		assertEquals("file", system.getScheme());
	}

	@Test
	public void testGetNullFileSystem() {
		IFileSystem system = EFS.getNullFileSystem();
		assertNotNull(system);
		assertEquals("null", system.getScheme());
	}
}
