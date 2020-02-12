/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
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
package org.eclipse.compare.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Field;

import org.eclipse.compare.internal.core.patch.FilePatch2;
import org.eclipse.compare.internal.patch.WorkspacePatcher;
import org.eclipse.compare.patch.IHunk;
import org.junit.Test;

public class PatchLinesTest {

	// unified diff format
	@Test
	public void test_196847() throws Exception {
		int[] lines = parsePatch("196847/stuff_patch.txt");
		assertEquals(6, lines[0]);
		assertEquals(5, lines[1]);
	}

	@Test
	public void test_deletion_autofuzz() throws Exception {
		int[] lines = parsePatch("deletion_autofuzz/patch.txt");
		assertEquals(0, lines[0]);
		assertEquals(1, lines[1]);
	}

	@Test
	public void test_patch_addition() throws Exception {
		int[] lines = parsePatch("patch_addition.txt");
		assertEquals(9, lines[0]);
		assertEquals(0, lines[1]);
	}

	@Test
	public void test_patch_context1() throws Exception {
		int[] lines = parsePatch("patch_context1.txt");
		assertEquals(4, lines[0]);
		assertEquals(2, lines[1]);
	}

	@Test
	public void test_patch_context3() throws Exception {
		int[] lines = parsePatch("patch_context3.txt");
		assertEquals(4, lines[0]);
		assertEquals(2, lines[1]);
	}

	@Test
	public void test_patch_workspacePatchAddition() throws Exception {
		int[] lines = parsePatch("patch_workspacePatchAddition.txt");
		assertEquals(31, lines[0]);
		assertEquals(0, lines[1]);
	}

	@Test
	public void test_patch_workspacePatchDelete() throws Exception {
		int[] lines = parsePatch("patch_workspacePatchDelete.txt");
		assertEquals(29, lines[0]);
		assertEquals(46, lines[1]);
	}

	@Test
	public void test_patch_workspacePatchMod() throws Exception {
		int[] lines = parsePatch("patch_workspacePatchMod.txt");
		assertEquals(33, lines[0]);
		assertEquals(8, lines[1]);
	}

	// context diff format
	@Test
	public void test_patch_addition_context() throws Exception {
		int[] lines = parsePatch("patch_addition_context.txt");
		assertEquals(9, lines[0]);
		assertEquals(0, lines[1]);
	}

	@Test
	public void test_patch_oneline_context() throws Exception {
		int[] lines = parsePatch("patch_oneline_context.txt");
		assertEquals(1, lines[0]);
		assertEquals(0, lines[1]);
	}

	@Test
	public void test_patch_context3_context() throws Exception {
		int[] lines = parsePatch("patch_context3_context.txt");
		assertEquals(4, lines[0]);
		assertEquals(2, lines[1]);
	}

	private int[] parsePatch(String patch) {
		WorkspacePatcher patcher = new WorkspacePatcher();
		try {
			patcher.parse(getReader(patch));
			patcher.countLines();
		} catch (IOException e) {
			e.printStackTrace();
		}

		int[] result = new int[] { 0, 0 };
		int hunksLengthSum = 0;
		int fileDiffSum = 0;

		FilePatch2[] diffs = patcher.getDiffs();
		for (FilePatch2 fileDiff : diffs) {
			IHunk[] hunks = fileDiff.getHunks();
			for (IHunk hunk : hunks) {
				hunksLengthSum += getNewLength(hunk);
				hunksLengthSum -= getOldLength(hunk);
			}
			result[0] += fileDiff.getAddedLines();
			result[1] += fileDiff.getRemovedLines();
			fileDiffSum += fileDiff.getAddedLines();
			fileDiffSum -= fileDiff.getRemovedLines();
		}

		assertEquals(hunksLengthSum, fileDiffSum);
		return result;
	}

	private BufferedReader getReader(String name) {
		return PatchUtils.getReader(name);
	}

	private int getNewLength(IHunk hunk) {
		Class<?> cls = hunk.getClass();
		try {
			Field fld = cls.getDeclaredField("fNewLength");
			fld.setAccessible(true);
			return fld.getInt(hunk);
		} catch (SecurityException e) {
			fail(e.getMessage());
		} catch (NoSuchFieldException e) {
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			fail(e.getMessage());
		} catch (IllegalAccessException e) {
			fail(e.getMessage());
		}
		fail();
		return -1;
	}

	private int getOldLength(IHunk hunk) {
		Class<?> cls = hunk.getClass();
		try {
			Field fld = cls.getDeclaredField("fOldLength");
			fld.setAccessible(true);
			return fld.getInt(hunk);
		} catch (SecurityException e) {
			fail(e.getMessage());
		} catch (NoSuchFieldException e) {
			fail(e.getMessage());
		} catch (IllegalArgumentException e) {
			fail(e.getMessage());
		} catch (IllegalAccessException e) {
			fail(e.getMessage());
		}
		fail();
		return -1;
	}
}
