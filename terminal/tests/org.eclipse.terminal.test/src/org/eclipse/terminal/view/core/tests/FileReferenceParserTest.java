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
import org.eclipse.terminal.view.core.internal.FileReferenceParser;
import org.junit.jupiter.api.Test;

class FileReferenceParserTest {

	@Test
	void testPlainPath() {
		FileReference ref = FileReferenceParser.parse("src/Test.java");
		assertEquals("src/Test.java", ref.path());
		assertEquals(-1, ref.line());
		assertEquals(-1, ref.column());
		assertFalse(ref.hasLine());
		assertFalse(ref.hasColumn());
	}

	@Test
	void testPathWithLine() {
		FileReference ref = FileReferenceParser.parse("src/Test.java:1");
		assertEquals("src/Test.java", ref.path());
		assertEquals(1, ref.line());
		assertEquals(-1, ref.column());
		assertTrue(ref.hasLine());
		assertFalse(ref.hasColumn());
	}

	@Test
	void testPathWithLineAndTrailingColon() {
		FileReference ref = FileReferenceParser.parse("src/Test.java:42:");
		assertEquals("src/Test.java", ref.path());
		assertEquals(42, ref.line());
		assertEquals(-1, ref.column());
	}

	@Test
	void testPathWithLineAndColumn() {
		FileReference ref = FileReferenceParser.parse("src/Test.java:42:7");
		assertEquals("src/Test.java", ref.path());
		assertEquals(42, ref.line());
		assertEquals(7, ref.column());
		assertTrue(ref.hasColumn());
	}

	@Test
	void testPathWithLineAndColumnAndTrailingColon() {
		FileReference ref = FileReferenceParser.parse("src/Test.java:42:7:");
		assertEquals("src/Test.java", ref.path());
		assertEquals(42, ref.line());
		assertEquals(7, ref.column());
	}

	@Test
	void testPathWithLineAndTrailingTextAfterSpace() {
		FileReference ref = FileReferenceParser.parse("src/Test.java:42:  private Prix prix;");
		assertEquals("src/Test.java:42:  private Prix prix;", ref.path());
		assertEquals(-1, ref.line());
	}

	@Test
	void testPathWithLineColumnAndTrailingText() {
		FileReference ref = FileReferenceParser.parse("src/Test.java:42:7: message");
		assertEquals("src/Test.java:42:7: message", ref.path());
		assertEquals(-1, ref.line());
	}

	@Test
	void testPathWithSpaces() {
		FileReference ref = FileReferenceParser.parse("src/main/My File.java:12");
		assertEquals("src/main/My File.java", ref.path());
		assertEquals(12, ref.line());
	}

	@Test
	void testUnixAbsolutePath() {
		FileReference ref = FileReferenceParser.parse("/home/user/project/Test.java:12");
		assertEquals("/home/user/project/Test.java", ref.path());
		assertEquals(12, ref.line());
	}

	@Test
	void testWindowsAbsolutePath() {
		FileReference ref = FileReferenceParser.parse("C:\\work\\project\\Test.java:12");
		assertEquals("C:\\work\\project\\Test.java", ref.path());
		assertEquals(12, ref.line());
	}

	@Test
	void testWindowsAbsolutePathWithColumn() {
		FileReference ref = FileReferenceParser.parse("C:\\work\\project\\Test.java:12:8");
		assertEquals("C:\\work\\project\\Test.java", ref.path());
		assertEquals(12, ref.line());
		assertEquals(8, ref.column());
	}

	@Test
	void testWindowsRelativePath() {
		FileReference ref = FileReferenceParser.parse("..\\module\\Test.java:15");
		assertEquals("..\\module\\Test.java", ref.path());
		assertEquals(15, ref.line());
	}

	@Test
	void testWindowsForwardSlash() {
		FileReference ref = FileReferenceParser.parse("C:/work/project/Test.java:12");
		assertEquals("C:/work/project/Test.java", ref.path());
		assertEquals(12, ref.line());
	}

	@Test
	void testLineZeroIsNotLine() {
		FileReference ref = FileReferenceParser.parse("src/Test.java:0");
		assertEquals("src/Test.java:0", ref.path());
		assertEquals(-1, ref.line());
		assertFalse(ref.hasLine());
	}

	@Test
	void testColumnZeroIsNotColumn() {
		FileReference ref = FileReferenceParser.parse("src/Test.java:42:0");
		assertEquals("src/Test.java:42:0", ref.path());
		assertEquals(-1, ref.line());
		assertEquals(-1, ref.column());
	}

	@Test
	void testNonNumericSuffix() {
		FileReference ref = FileReferenceParser.parse("src/Test.java:abc");
		assertEquals("src/Test.java:abc", ref.path());
		assertEquals(-1, ref.line());
	}

	@Test
	void testUrlLikeText() {
		FileReference ref = FileReferenceParser.parse("http://localhost:8080/path");
		assertEquals("http://localhost:8080/path", ref.path());
		assertEquals(-1, ref.line());
		assertEquals(-1, ref.column());
	}

	@Test
	void testTimeLikeText() {
		FileReference ref = FileReferenceParser.parse("12:30:45");
		assertEquals("12:30:45", ref.path());
		assertEquals(-1, ref.line());
		assertEquals(-1, ref.column());
	}

	@Test
	void testMavenCoordinates() {
		FileReference ref = FileReferenceParser.parse("groupId:artifactId:version");
		assertEquals("groupId:artifactId:version", ref.path());
		assertEquals(-1, ref.line());
	}

	@Test
	void testNullInput() {
		FileReference ref = FileReferenceParser.parse(null);
		assertEquals(null, ref.path());
		assertEquals(-1, ref.line());
		assertFalse(ref.hasLine());
	}

	@Test
	void testEmptyInput() {
		FileReference ref = FileReferenceParser.parse("");
		assertEquals("", ref.path());
		assertEquals(-1, ref.line());
		assertFalse(ref.hasLine());
	}

	@Test
	void testGitGrepOutput() {
		FileReference ref = FileReferenceParser
				.parse("ecore/app/src/main/java/com/example/CombinaisonTarifaire.java:42:");
		assertEquals("ecore/app/src/main/java/com/example/CombinaisonTarifaire.java", ref.path());
		assertEquals(42, ref.line());
		assertEquals(-1, ref.column());
	}

	@Test
	void testGitGrepOutputWithColumn() {
		FileReference ref = FileReferenceParser
				.parse("ecore/app/src/main/java/com/example/MyClass.java:42:17");
		assertEquals("ecore/app/src/main/java/com/example/MyClass.java", ref.path());
		assertEquals(42, ref.line());
		assertEquals(17, ref.column());
	}

	@Test
	void testMultipleColonsInPath() {
		FileReference ref = FileReferenceParser.parse("/path/with:colon/File.java:42");
		assertEquals("/path/with:colon/File.java", ref.path());
		assertEquals(42, ref.line());
	}

	@Test
	void testJustFileNameWithLine() {
		FileReference ref = FileReferenceParser.parse("MyClass.java:42");
		assertEquals("MyClass.java", ref.path());
		assertEquals(42, ref.line());
	}

	@Test
	void testNonFilePathLikeMot123() {
		FileReference ref = FileReferenceParser.parse("mot:123");
		assertEquals("mot", ref.path());
		assertEquals(123, ref.line());
	}
}
