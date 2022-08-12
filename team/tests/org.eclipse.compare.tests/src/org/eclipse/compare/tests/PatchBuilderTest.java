/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.eclipse.compare.internal.core.patch.Hunk;
import org.eclipse.compare.internal.core.patch.LineReader;
import org.eclipse.compare.internal.patch.Utilities;
import org.eclipse.compare.patch.*;
import org.eclipse.compare.tests.PatchUtils.StringStorage;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.*;
import org.junit.Assert;
import org.junit.Test;

public class PatchBuilderTest {
	@Test
	public void testModifyHunks() throws CoreException, IOException {
		IStorage patchStorage = new StringStorage("patch_modifyHunks.txt");
		IStorage contextStorage = new StringStorage("context.txt");
		IFilePatch[] patches = ApplyPatchOperation.parsePatch(patchStorage);
		assertEquals(1, patches.length);
		IHunk[] hunksBefore = patches[0].getHunks();
		assertEquals(5, hunksBefore.length);

		String[] lines = new String[] { " [d]", "+[d1]", "+[d2]", "+[d3]", "+[d4]", " [e]" };
		String lineDelimiter = getLineDelimiter(patchStorage);
		addLineDelimiters(lines, lineDelimiter);
		IHunk[] toAdd = new IHunk[] { PatchBuilder.createHunk(3, lines) };
		IFilePatch2 filePatch = PatchBuilder.addHunks(patches[0], toAdd);

		IHunk[] toRemove = new IHunk[] { hunksBefore[0], hunksBefore[2] };
		filePatch = PatchBuilder.removeHunks(filePatch, toRemove);

		IHunk[] hunksAfter = filePatch.getHunks();
		assertEquals(4, hunksAfter.length);
		assertEquals(3, ((Hunk) hunksAfter[0]).getStart(false));
		assertEquals(3, ((Hunk) hunksAfter[0]).getStart(true));
		assertEquals(7, ((Hunk) hunksAfter[1]).getStart(false));
		assertEquals(11, ((Hunk) hunksAfter[1]).getStart(true));
		assertEquals(18, ((Hunk) hunksAfter[2]).getStart(false));
		assertEquals(22, ((Hunk) hunksAfter[2]).getStart(true));
		assertEquals(28, ((Hunk) hunksAfter[3]).getStart(false));
		assertEquals(33, ((Hunk) hunksAfter[3]).getStart(true));

		IFilePatchResult result = filePatch.apply(Utilities.getReaderCreator(contextStorage), new PatchConfiguration(),
				new NullProgressMonitor());

		IHunk[] rejects = result.getRejects();
		assertEquals(1, rejects.length);

		InputStream actual = result.getPatchedContents();

		LineReader lr = new LineReader(PatchUtils.getReader("exp_modifyHunks.txt"));
		List<String> inLines = lr.readLines();
		String expected = LineReader.createString(false, inLines);

		assertEquals(expected, PatchUtils.asString(actual));
	}

	@Test
	public void testAddHunks() throws CoreException, IOException {
		IStorage patchStorage = new StringStorage("patch_addHunks.txt");
		IStorage contextStorage = new StringStorage("context_full.txt");
		IFilePatch[] patches = ApplyPatchOperation.parsePatch(patchStorage);
		assertEquals(1, patches.length);
		IHunk[] hunksBefore = patches[0].getHunks();
		assertEquals(3, hunksBefore.length);

		String[] lines0 = new String[] { " [d]", "+[d1]", "+[d2]", "+[d3]", "+[d4]", " [e]" };
		String lineDelimiter = getLineDelimiter(patchStorage);
		addLineDelimiters(lines0, lineDelimiter);
		IHunk hunk0 = PatchBuilder.createHunk(3, lines0);

		String[] lines1 = new String[] { " [K]", " [L]", "-[M]", " [N]", "+[N1]", "+[N2]", " [O]", " [P]" };
		addLineDelimiters(lines1, lineDelimiter);
		IHunk hunk1 = PatchBuilder.createHunk(36, lines1);

		IHunk[] toAdd = new IHunk[] { hunk0, hunk1 };
		IFilePatch2 filePatch = PatchBuilder.addHunks(patches[0], toAdd);

		IHunk[] hunksAfter = filePatch.getHunks();
		assertEquals(5, hunksAfter.length);
		assertEquals(0, ((Hunk) hunksAfter[0]).getStart(false));
		assertEquals(0, ((Hunk) hunksAfter[0]).getStart(true));
		assertEquals(3, ((Hunk) hunksAfter[1]).getStart(false));
		assertEquals(5, ((Hunk) hunksAfter[1]).getStart(true));
		assertEquals(19, ((Hunk) hunksAfter[2]).getStart(false));
		assertEquals(25, ((Hunk) hunksAfter[2]).getStart(true));
		assertEquals(36, ((Hunk) hunksAfter[3]).getStart(false));
		assertEquals(40, ((Hunk) hunksAfter[3]).getStart(true));
		assertEquals(46, ((Hunk) hunksAfter[4]).getStart(false));
		assertEquals(51, ((Hunk) hunksAfter[4]).getStart(true));

		IFilePatchResult result = filePatch.apply(Utilities.getReaderCreator(contextStorage), new PatchConfiguration(),
				new NullProgressMonitor());

		IHunk[] rejects = result.getRejects();
		assertEquals(0, rejects.length);

		InputStream actual = result.getPatchedContents();

		LineReader lr = new LineReader(PatchUtils.getReader("exp_addHunks.txt"));
		List<String> inLines = lr.readLines();
		String expected = LineReader.createString(false, inLines);

		assertEquals(expected, PatchUtils.asString(actual));
	}

	@Test
	public void testRemoveHunks() throws CoreException, IOException {
		IStorage patchStorage = new StringStorage("patch_removeHunks.txt");
		IStorage contextStorage = new StringStorage("context_full.txt");
		IFilePatch[] patches = ApplyPatchOperation.parsePatch(patchStorage);
		assertEquals(1, patches.length);
		IHunk[] hunksBefore = patches[0].getHunks();
		assertEquals(5, hunksBefore.length);

		IHunk[] toRemove = new IHunk[] { hunksBefore[0], hunksBefore[1] };
		IFilePatch2 filePatch = PatchBuilder.removeHunks(patches[0], toRemove);

		IHunk[] hunksAfter = filePatch.getHunks();
		assertEquals(3, hunksAfter.length);
		assertEquals(19, ((Hunk) hunksAfter[0]).getStart(false));
		assertEquals(19, ((Hunk) hunksAfter[0]).getStart(true));
		assertEquals(29, ((Hunk) hunksAfter[1]).getStart(false));
		assertEquals(27, ((Hunk) hunksAfter[1]).getStart(true));
		assertEquals(46, ((Hunk) hunksAfter[2]).getStart(false));
		assertEquals(43, ((Hunk) hunksAfter[2]).getStart(true));

		IFilePatchResult result = filePatch.apply(Utilities.getReaderCreator(contextStorage), new PatchConfiguration(),
				new NullProgressMonitor());

		IHunk[] rejects = result.getRejects();
		assertEquals(0, rejects.length);

		InputStream actual = result.getPatchedContents();

		LineReader lr = new LineReader(PatchUtils.getReader("exp_removeHunks.txt"));
		List<String> inLines = lr.readLines();
		String expected = LineReader.createString(false, inLines);

		assertEquals(expected, PatchUtils.asString(actual));
	}

	@Test
	public void testCreateFilePatch() throws CoreException, IOException {
		IStorage contextStorage = new StringStorage("context.txt");

		String[] lines0 = new String[] { "+[a1]", "+[a2]", "+[a3]", " [a]" };
		String lineDelimiter = getLineDelimiter(contextStorage);
		addLineDelimiters(lines0, lineDelimiter);
		Hunk hunk0 = (Hunk) PatchBuilder.createHunk(0, lines0);

		String[] lines1 = new String[] { " [b]", " [c]", "-[d]", "-[e]", " [f]", " [g]", " [h]", "+[h1]", " [i]",
				" [j]", "+[j1]", "+[j2]", " [k]", " [l]" };
		addLineDelimiters(lines1, lineDelimiter);
		Hunk hunk1 = (Hunk) PatchBuilder.createHunk(1, lines1);

		IHunk[] hunks = new IHunk[] { hunk1, hunk0 };

		IFilePatch2 filePatch = PatchBuilder.createFilePatch(new Path(""), IFilePatch2.DATE_UNKNOWN, new Path(""),
				IFilePatch2.DATE_UNKNOWN, hunks);

		assertEquals(2, filePatch.getHunks().length);
		assertEquals(hunk0, filePatch.getHunks()[0]);
		assertEquals(hunk1, filePatch.getHunks()[1]);

		IFilePatchResult result = filePatch.apply(Utilities.getReaderCreator(contextStorage), new PatchConfiguration(),
				new NullProgressMonitor());

		InputStream actual = result.getPatchedContents();

		LineReader lr = new LineReader(PatchUtils.getReader("exp_createFilePatch.txt"));
		List<String> inLines = lr.readLines();
		String expected = LineReader.createString(false, inLines);

		assertEquals(expected, PatchUtils.asString(actual));
	}

	@Test
	public void testCreateHunk0() throws CoreException, IOException {
		IStorage patch = new StringStorage("patch_createHunk0.txt");
		IFilePatch[] filePatches = ApplyPatchOperation.parsePatch(patch);
		assertEquals(1, filePatches.length);
		assertEquals(1, filePatches[0].getHunks().length);

		String[] lines = new String[] { "+[a1]", "+[a2]", "+[a3]", " [a]", " [b]", "-[c]", " [d]", " [e]", " [f]" };
		String lineDelimiter = getLineDelimiter(patch);
		addLineDelimiters(lines, lineDelimiter);
		Hunk hunk = (Hunk) PatchBuilder.createHunk(0, lines);
		String[] actual = hunk.getUnifiedLines();
		assertTrue(lines != actual);
		Assert.assertArrayEquals(lines, actual);

		assertHunkEquals(hunk, (Hunk) filePatches[0].getHunks()[0]);
	}

	@Test
	public void testCreateHunk1() throws CoreException, IOException {
		IStorage patch = new StringStorage("patch_createHunk1.txt");
		IFilePatch[] filePatches = ApplyPatchOperation.parsePatch(patch);
		assertEquals(1, filePatches.length);
		assertEquals(1, filePatches[0].getHunks().length);

		String[] lines = new String[] { " [a]", " [b]", "-[c]", " [d]", "-[e]", " [f]", " [g]", " [h]", "+[h1]", " [i]",
				" [j]", "+[j1]", "+[j2]", " [k]", " [l]", " [m]" };
		String lineDelimiter = getLineDelimiter(patch);
		addLineDelimiters(lines, lineDelimiter);
		Hunk hunk = (Hunk) PatchBuilder.createHunk(0, lines);
		String[] actual = hunk.getUnifiedLines();
		assertTrue(lines != actual);
		Assert.assertArrayEquals(lines, actual);

		assertHunkEquals(hunk, (Hunk) filePatches[0].getHunks()[0]);
	}

	@Test
	public void testCreateHunk2() throws CoreException, IOException {
		IStorage patch = new StringStorage("patch_createHunk2.txt");
		IFilePatch[] filePatches = ApplyPatchOperation.parsePatch(patch);
		assertEquals(1, filePatches.length);
		assertEquals(1, filePatches[0].getHunks().length);

		String[] lines = new String[] { "+[aa]", "+[bb]", "+[cc]" };
		String lineDelimiter = getLineDelimiter(patch);
		addLineDelimiters(lines, lineDelimiter);
		Hunk hunk = (Hunk) PatchBuilder.createHunk(0, lines);
		String[] actual = hunk.getUnifiedLines();
		assertTrue(lines != actual);
		Assert.assertArrayEquals(lines, actual);

		assertHunkEquals(hunk, (Hunk) filePatches[0].getHunks()[0]);
	}

	@Test
	public void testCreateHunk3() throws CoreException, IOException {
		IStorage patch = new StringStorage("patch_createHunk3.txt");
		IFilePatch[] filePatches = ApplyPatchOperation.parsePatch(patch);
		assertEquals(1, filePatches.length);
		assertEquals(1, filePatches[0].getHunks().length);

		String[] lines = new String[] { "-[aa]", "-[bb]", "-[cc]", "-[dd]" };
		String lineDelimiter = getLineDelimiter(patch);
		addLineDelimiters(lines, lineDelimiter);
		Hunk hunk = (Hunk) PatchBuilder.createHunk(0, lines);
		String[] actual = hunk.getUnifiedLines();
		assertTrue(lines != actual);
		Assert.assertArrayEquals(lines, actual);

		assertHunkEquals(hunk, (Hunk) filePatches[0].getHunks()[0]);
	}

	private void assertHunkEquals(Hunk h1, Hunk h2) {
		String[] l1 = h1.getLines();
		String[] l2 = h2.getLines();
		assertEquals(l1.length, l2.length);
		for (int i = 0; i < l1.length; i++) {
			assertFalse(l1[i] == null && l2[i] != null);
			assertEquals(l1[i], (l2[i]));
		}
		assertEquals(h1.getStart(false), h2.getStart(false));
		assertEquals(h1.getStart(true), h2.getStart(true));
		assertEquals(h1.getLength(false), h2.getLength(false));
		assertEquals(h1.getLength(true), h2.getLength(true));
		assertEquals(h1.getHunkType(false), h2.getHunkType(false));
		assertEquals(h1.getHunkType(true), h2.getHunkType(true));
	}

	private String getLineDelimiter(IStorage storage) throws CoreException, IOException {
		InputStream in = null;
		try {
			in = storage.getContents();
			int ch;
			while ((ch = in.read()) != -1) {
				if (ch == '\r') {
					return "\r\n";
				}
			}
		} finally {
			if (in != null) {
				in.close();
			}
		}
		return "\n";
	}

	private void addLineDelimiters(String[] lines, String lineDelimiter) {
		for (int i = 0; i < lines.length; i++) {
			lines[i] = lines[i] + lineDelimiter;
		}
	}

}
