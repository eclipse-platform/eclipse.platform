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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertArrayEquals;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.eclipse.compare.internal.core.patch.Hunk;
import org.eclipse.compare.internal.core.patch.LineReader;
import org.eclipse.compare.internal.patch.Utilities;
import org.eclipse.compare.patch.ApplyPatchOperation;
import org.eclipse.compare.patch.IFilePatch;
import org.eclipse.compare.patch.IFilePatch2;
import org.eclipse.compare.patch.IFilePatchResult;
import org.eclipse.compare.patch.IHunk;
import org.eclipse.compare.patch.PatchBuilder;
import org.eclipse.compare.patch.PatchConfiguration;
import org.eclipse.compare.tests.PatchUtils.StringStorage;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Test;

public class PatchBuilderTest {
	@Test
	public void testModifyHunks() throws CoreException, IOException {
		IStorage patchStorage = new StringStorage("patch_modifyHunks.txt");
		IStorage contextStorage = new StringStorage("context.txt");
		IFilePatch[] patches = ApplyPatchOperation.parsePatch(patchStorage);
		assertThat(patches).hasSize(1);
		IHunk[] hunksBefore = patches[0].getHunks();
		assertThat(hunksBefore).hasSize(5);

		String[] lines = new String[] { " [d]", "+[d1]", "+[d2]", "+[d3]", "+[d4]", " [e]" };
		String lineDelimiter = getLineDelimiter(patchStorage);
		addLineDelimiters(lines, lineDelimiter);
		IHunk[] toAdd = new IHunk[] { PatchBuilder.createHunk(3, lines) };
		IFilePatch2 filePatch = PatchBuilder.addHunks(patches[0], toAdd);

		IHunk[] toRemove = new IHunk[] { hunksBefore[0], hunksBefore[2] };
		filePatch = PatchBuilder.removeHunks(filePatch, toRemove);

		IHunk[] hunksAfter = filePatch.getHunks();
		assertThat(hunksAfter).hasSize(4);
		assertThat(((Hunk) hunksAfter[0]).getStart(false)).isEqualTo(3);
		assertThat(((Hunk) hunksAfter[0]).getStart(true)).isEqualTo(3);
		assertThat(((Hunk) hunksAfter[1]).getStart(false)).isEqualTo(7);
		assertThat(((Hunk) hunksAfter[1]).getStart(true)).isEqualTo(11);
		assertThat(((Hunk) hunksAfter[2]).getStart(false)).isEqualTo(18);
		assertThat(((Hunk) hunksAfter[2]).getStart(true)).isEqualTo(22);
		assertThat(((Hunk) hunksAfter[3]).getStart(false)).isEqualTo(28);
		assertThat(((Hunk) hunksAfter[3]).getStart(true)).isEqualTo(33);

		IFilePatchResult result = filePatch.apply(Utilities.getReaderCreator(contextStorage), new PatchConfiguration(),
				new NullProgressMonitor());

		IHunk[] rejects = result.getRejects();
		assertThat(rejects).hasSize(1);

		InputStream actual = result.getPatchedContents();

		LineReader lr = new LineReader(PatchUtils.getReader("exp_modifyHunks.txt"));
		List<String> inLines = lr.readLines();
		String expected = LineReader.createString(false, inLines);

		assertThat(PatchUtils.asString(actual)).isEqualTo(expected);
	}

	@Test
	public void testAddHunks() throws CoreException, IOException {
		IStorage patchStorage = new StringStorage("patch_addHunks.txt");
		IStorage contextStorage = new StringStorage("context_full.txt");
		IFilePatch[] patches = ApplyPatchOperation.parsePatch(patchStorage);
		assertThat(patches).hasSize(1);
		IHunk[] hunksBefore = patches[0].getHunks();
		assertThat(hunksBefore).hasSize(3);

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
		assertThat(hunksAfter).hasSize(5);
		assertThat(((Hunk) hunksAfter[0]).getStart(false)).isEqualTo(0);
		assertThat(((Hunk) hunksAfter[0]).getStart(true)).isEqualTo(0);
		assertThat(((Hunk) hunksAfter[1]).getStart(false)).isEqualTo(3);
		assertThat(((Hunk) hunksAfter[1]).getStart(true)).isEqualTo(5);
		assertThat(((Hunk) hunksAfter[2]).getStart(false)).isEqualTo(19);
		assertThat(((Hunk) hunksAfter[2]).getStart(true)).isEqualTo(25);
		assertThat(((Hunk) hunksAfter[3]).getStart(false)).isEqualTo(36);
		assertThat(((Hunk) hunksAfter[3]).getStart(true)).isEqualTo(40);
		assertThat(((Hunk) hunksAfter[4]).getStart(false)).isEqualTo(46);
		assertThat(((Hunk) hunksAfter[4]).getStart(true)).isEqualTo(51);

		IFilePatchResult result = filePatch.apply(Utilities.getReaderCreator(contextStorage), new PatchConfiguration(),
				new NullProgressMonitor());

		IHunk[] rejects = result.getRejects();
		assertThat(rejects).isEmpty();

		InputStream actual = result.getPatchedContents();

		LineReader lr = new LineReader(PatchUtils.getReader("exp_addHunks.txt"));
		List<String> inLines = lr.readLines();
		String expected = LineReader.createString(false, inLines);

		assertThat(PatchUtils.asString(actual)).isEqualTo(expected);
	}

	@Test
	public void testRemoveHunks() throws CoreException, IOException {
		IStorage patchStorage = new StringStorage("patch_removeHunks.txt");
		IStorage contextStorage = new StringStorage("context_full.txt");
		IFilePatch[] patches = ApplyPatchOperation.parsePatch(patchStorage);
		assertThat(patches).hasSize(1);
		IHunk[] hunksBefore = patches[0].getHunks();
		assertThat(hunksBefore).hasSize(5);

		IHunk[] toRemove = new IHunk[] { hunksBefore[0], hunksBefore[1] };
		IFilePatch2 filePatch = PatchBuilder.removeHunks(patches[0], toRemove);

		IHunk[] hunksAfter = filePatch.getHunks();
		assertThat(hunksAfter).hasSize(3);
		assertThat(((Hunk) hunksAfter[0]).getStart(false)).isEqualTo(19);
		assertThat(((Hunk) hunksAfter[0]).getStart(true)).isEqualTo(19);
		assertThat(((Hunk) hunksAfter[1]).getStart(false)).isEqualTo(29);
		assertThat(((Hunk) hunksAfter[1]).getStart(true)).isEqualTo(27);
		assertThat(((Hunk) hunksAfter[2]).getStart(false)).isEqualTo(46);
		assertThat(((Hunk) hunksAfter[2]).getStart(true)).isEqualTo(43);

		IFilePatchResult result = filePatch.apply(Utilities.getReaderCreator(contextStorage), new PatchConfiguration(),
				new NullProgressMonitor());

		IHunk[] rejects = result.getRejects();
		assertThat(rejects).isEmpty();

		InputStream actual = result.getPatchedContents();

		LineReader lr = new LineReader(PatchUtils.getReader("exp_removeHunks.txt"));
		List<String> inLines = lr.readLines();
		String expected = LineReader.createString(false, inLines);

		assertThat(PatchUtils.asString(actual)).isEqualTo(expected);
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

		IFilePatch2 filePatch = PatchBuilder.createFilePatch(IPath.fromOSString(""), IFilePatch2.DATE_UNKNOWN, IPath.fromOSString(""),
				IFilePatch2.DATE_UNKNOWN, hunks);

		assertThat(filePatch.getHunks()).containsExactly(hunk0, hunk1);

		IFilePatchResult result = filePatch.apply(Utilities.getReaderCreator(contextStorage), new PatchConfiguration(),
				new NullProgressMonitor());

		InputStream actual = result.getPatchedContents();

		LineReader lr = new LineReader(PatchUtils.getReader("exp_createFilePatch.txt"));
		List<String> inLines = lr.readLines();
		String expected = LineReader.createString(false, inLines);

		assertThat(PatchUtils.asString(actual)).isEqualTo(expected);
	}

	@Test
	public void testCreateHunk0() throws CoreException, IOException {
		IStorage patch = new StringStorage("patch_createHunk0.txt");
		IFilePatch[] filePatches = ApplyPatchOperation.parsePatch(patch);
		assertThat(filePatches).hasSize(1);
		assertThat(filePatches[0].getHunks()).hasSize(1);

		String[] lines = new String[] { "+[a1]", "+[a2]", "+[a3]", " [a]", " [b]", "-[c]", " [d]", " [e]", " [f]" };
		String lineDelimiter = getLineDelimiter(patch);
		addLineDelimiters(lines, lineDelimiter);
		Hunk hunk = (Hunk) PatchBuilder.createHunk(0, lines);
		String[] actual = hunk.getUnifiedLines();
		assertThat(actual).isNotSameAs(lines).containsExactly(lines);

		assertHunkEquals(hunk, (Hunk) filePatches[0].getHunks()[0]);
	}

	@Test
	public void testCreateHunk1() throws CoreException, IOException {
		IStorage patch = new StringStorage("patch_createHunk1.txt");
		IFilePatch[] filePatches = ApplyPatchOperation.parsePatch(patch);
		assertThat(filePatches).hasSize(1);
		assertThat(filePatches[0].getHunks()).hasSize(1);

		String[] lines = new String[] { " [a]", " [b]", "-[c]", " [d]", "-[e]", " [f]", " [g]", " [h]", "+[h1]", " [i]",
				" [j]", "+[j1]", "+[j2]", " [k]", " [l]", " [m]" };
		String lineDelimiter = getLineDelimiter(patch);
		addLineDelimiters(lines, lineDelimiter);
		Hunk hunk = (Hunk) PatchBuilder.createHunk(0, lines);
		String[] actual = hunk.getUnifiedLines();
		assertThat(actual).isNotSameAs(lines).containsExactly(lines);

		assertHunkEquals(hunk, (Hunk) filePatches[0].getHunks()[0]);
	}

	@Test
	public void testCreateHunk2() throws CoreException, IOException {
		IStorage patch = new StringStorage("patch_createHunk2.txt");
		IFilePatch[] filePatches = ApplyPatchOperation.parsePatch(patch);
		assertThat(filePatches).hasSize(1);
		assertThat(filePatches[0].getHunks()).hasSize(1);

		String[] lines = new String[] { "+[aa]", "+[bb]", "+[cc]" };
		String lineDelimiter = getLineDelimiter(patch);
		addLineDelimiters(lines, lineDelimiter);
		Hunk hunk = (Hunk) PatchBuilder.createHunk(0, lines);
		String[] actual = hunk.getUnifiedLines();
		assertThat(actual).isNotSameAs(lines).containsExactly(lines);

		assertHunkEquals(hunk, (Hunk) filePatches[0].getHunks()[0]);
	}

	@Test
	public void testCreateHunk3() throws CoreException, IOException {
		IStorage patch = new StringStorage("patch_createHunk3.txt");
		IFilePatch[] filePatches = ApplyPatchOperation.parsePatch(patch);
		assertThat(filePatches).hasSize(1);
		assertThat(filePatches[0].getHunks()).hasSize(1);

		String[] lines = new String[] { "-[aa]", "-[bb]", "-[cc]", "-[dd]" };
		String lineDelimiter = getLineDelimiter(patch);
		addLineDelimiters(lines, lineDelimiter);
		Hunk hunk = (Hunk) PatchBuilder.createHunk(0, lines);
		String[] actual = hunk.getUnifiedLines();
		assertThat(actual).isNotSameAs(lines).containsExactly(lines);

		assertHunkEquals(hunk, (Hunk) filePatches[0].getHunks()[0]);
	}

	@Test
	public void testReadNotValidPatch() throws CoreException, IOException {
		IStorage patch = new StringStorage("not_a_patch.txt");
		IFilePatch[] filePatches = ApplyPatchOperation.parsePatch(patch);
		assertArrayEquals(new IFilePatch[0], filePatches);
	}

	private void assertHunkEquals(Hunk h1, Hunk h2) {
		String[] l1 = h1.getLines();
		String[] l2 = h2.getLines();
		assertThat(l1).containsExactly(l2);
		assertThat(h1.getStart(false)).isEqualTo(h2.getStart(false));
		assertThat(h1.getStart(true)).isEqualTo(h2.getStart(true));
		assertThat(h1.getLength(false)).isEqualTo(h2.getLength(false));
		assertThat(h1.getLength(true)).isEqualTo(h2.getLength(true));
		assertThat(h1.getHunkType(false)).isEqualTo(h2.getHunkType(false));
		assertThat(h1.getHunkType(true)).isEqualTo( h2.getHunkType(true));
	}

	private String getLineDelimiter(IStorage storage) throws CoreException, IOException {
		try (InputStream in = storage.getContents()) {
			int ch;
			while ((ch = in.read()) != -1) {
				if (ch == '\r') {
					return "\r\n";
				}
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
