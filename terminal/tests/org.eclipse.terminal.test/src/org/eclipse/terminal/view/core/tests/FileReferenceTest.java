/*******************************************************************************
 * Copyright (c) 2024 Fabrizio Iannetti and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.terminal.view.core.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.terminal.view.core.internal.FileReference;
import org.junit.jupiter.api.Test;

class FileReferenceTest {

	@Test
	void testDefaultValues() {
		FileReference ref = new FileReference("/path/to/file.java", -1, -1);
		assertEquals("/path/to/file.java", ref.path());
		assertEquals(-1, ref.line());
		assertEquals(-1, ref.column());
		assertFalse(ref.hasLine());
		assertFalse(ref.hasColumn());
	}

	@Test
	void testWithLineOnly() {
		FileReference ref = new FileReference("/path/to/file.java", 42, -1);
		assertTrue(ref.hasLine());
		assertFalse(ref.hasColumn());
	}

	@Test
	void testWithLineAndColumn() {
		FileReference ref = new FileReference("/path/to/file.java", 42, 7);
		assertTrue(ref.hasLine());
		assertTrue(ref.hasColumn());
	}

	@Test
	void testLineZeroIsAbsent() {
		FileReference ref = new FileReference("file.java", 0, -1);
		assertFalse(ref.hasLine());
	}

	@Test
	void testColumnZeroIsAbsent() {
		FileReference ref = new FileReference("file.java", 42, 0);
		assertTrue(ref.hasLine());
		assertFalse(ref.hasColumn());
	}
}
