/*******************************************************************************
 * Copyright (c) 2005, 2018 IBM Corporation and others.
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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.eclipse.compare.internal.Utilities;
import org.eclipse.compare.internal.core.patch.FilePatch2;
import org.eclipse.compare.internal.core.patch.FileDiffResult;
import org.eclipse.compare.internal.core.patch.Hunk;
import org.eclipse.compare.internal.patch.Patcher;
import org.eclipse.compare.patch.ApplyPatchOperation;
import org.eclipse.compare.patch.IFilePatch;
import org.eclipse.compare.patch.IFilePatchResult;
import org.eclipse.compare.patch.PatchConfiguration;
import org.eclipse.compare.patch.WorkspacePatcherUI;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

public class FileDiffResultTest extends WorkspaceTest {

	public FileDiffResultTest() {
		super();
	}

	public FileDiffResultTest(String name) {
		super(name);
	}

	private static final String PATCH_FILE = "patchfile";

	private static final String NEW_FILENAME = "newfile";

	private static final String NEW_FILE_CONTENT = "Hi There";

	private IProgressMonitor nullProgressMonitor = new NullProgressMonitor();

	private PatchConfiguration patchConfiguration = new PatchConfiguration();

	/**
	 * Tests applying a patch which creates a new file in a project. The file
	 * doesn't exist in the project.
	 *
	 * @throws CoreException
	 */
	public void testPatchAddsNewFile() throws CoreException {
		IProject project = createProject("FileDiffResultTest",
				new String[] { "oldfile" });

		try {
			// create the patch file
			IFile file = project.getFile(PATCH_FILE);
			file.create(new ByteArrayInputStream(createPatchAddingFile(project,
					NEW_FILENAME, false /* the file doesn't exist */)
					.getBytes()), true, null);

			assertFalse(project.getFile(NEW_FILENAME).exists());

			IFilePatch[] filePatch = ApplyPatchOperation.parsePatch(file);
			assertNotNull(filePatch);
			assertEquals(1, filePatch.length);

			IFilePatchResult filePatchResult = filePatch[0].apply((IStorage)null,
					patchConfiguration, nullProgressMonitor);
			assertTrue(filePatchResult.hasMatches());
			assertEquals(0, filePatchResult.getRejects().length);
			assertEquals("", getStringFromStream(filePatchResult
					.getOriginalContents()));
			assertEquals(NEW_FILE_CONTENT, getStringFromStream(filePatchResult
					.getPatchedContents()));

		} catch (IOException e) {
			fail();
		}
	}

	/**
	 * Tests applying a patch which creates a new file in a project. The file
	 * already exists in the project.
	 *
	 * @throws CoreException
	 */
	public void testPatchAddsExistingFileWithSameContents()
			throws CoreException {
		IProject project = createProject("FileDiffResultTest",
				new String[] { NEW_FILENAME });

		try {
			// create the patch file
			IFile file = project.getFile(PATCH_FILE);
			file.create(new ByteArrayInputStream(createPatchAddingFile(project,
					NEW_FILENAME, true).getBytes()), true, null);

			assertTrue(project.getFile(NEW_FILENAME).exists());

			IFilePatch[] filePatch = ApplyPatchOperation.parsePatch(file);
			assertNotNull(filePatch);
			assertEquals(1, filePatch.length);

			IFilePatchResult filePatchResult = filePatch[0].apply(project
					.getFile(NEW_FILENAME), patchConfiguration,
					nullProgressMonitor);

			assertFalse(filePatchResult.hasMatches());
			assertEquals(1, filePatchResult.getRejects().length);

			assertNotNull(filePatchResult.getOriginalContents());
			assertNotNull(filePatchResult.getPatchedContents());

			assertEquals(new FileInputStream(project.getFile(NEW_FILENAME)
					.getLocation().toFile()), filePatchResult
					.getOriginalContents());
			assertEquals(filePatchResult.getOriginalContents(), filePatchResult
					.getPatchedContents());

		} catch (IOException e) {
			fail();
		}

	}

	/**
	 * Tests applying a patch which creates a new file in a project. The file
	 * already exists in the project, but has different contents.
	 *
	 * @throws CoreException
	 */
	public void testPatchAddsExistingFileWithDifferentContents()
			throws CoreException {
		IProject project = createProject("FileDiffResultTest",
				new String[] { NEW_FILENAME });

		project.getFile(NEW_FILENAME).setContents(
				new ByteArrayInputStream("I'm a different content".getBytes()),
				IResource.NONE, null);

		try {
			// create the patch file
			IFile file = project.getFile(PATCH_FILE);
			file.create(new ByteArrayInputStream(createPatchAddingFile(project,
					NEW_FILENAME, false).getBytes()), true, null);

			assertTrue(project.getFile(NEW_FILENAME).exists());

			IFilePatch[] filePatch = ApplyPatchOperation.parsePatch(file);
			assertNotNull(filePatch);
			assertEquals(1, filePatch.length);

			IFilePatchResult filePatchResult = filePatch[0].apply(project
					.getFile(NEW_FILENAME), patchConfiguration,
					nullProgressMonitor);
			assertFalse(filePatchResult.hasMatches());
			assertEquals(1, filePatchResult.getRejects().length);

			assertNotNull(filePatchResult.getOriginalContents());
			assertNotNull(filePatchResult.getPatchedContents());

			assertEquals(new FileInputStream(project.getFile(NEW_FILENAME)
					.getLocation().toFile()), filePatchResult
					.getOriginalContents());
			assertEquals("I'm a different content",
					getStringFromStream(filePatchResult.getOriginalContents()));
			assertEquals(filePatchResult.getOriginalContents(), filePatchResult
					.getPatchedContents());

		} catch (IOException e) {
			fail();
		}

	}

	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=185379
	public void testFileDiffResultWithNullPath() {
		MyFileDiff myFileDiff = new MyFileDiff();
		FileDiffResult fileDiffResult = new FileDiffResult(myFileDiff,
				patchConfiguration);
		try {
			fileDiffResult.calculateFuzz(new ArrayList<>(), nullProgressMonitor);
		} catch (NullPointerException e) {
			fail();
		}
	}

	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=187365
	public void testExcludePartOfNonWorkspacePatch() {
		Patcher patcher = new Patcher();
		MyFileDiff myFileDiff = new MyFileDiff();
		try {
			patcher.setEnabled(myFileDiff, false);
		} catch (NullPointerException e) {
			fail();
		}
	}

	// utility methods

	/**
	 * A mock FileDiff class.
	 */
	private class MyFileDiff extends FilePatch2 {
		protected MyFileDiff() {
			super(null, 0, null, 0);
			add(Hunk.createHunk(this, new int[] { 0, 0 }, new int[] { 0, 0 },
					new ArrayList<>(), false, false, false));
		}
	}

	/**
	 * @param project
	 *            The project for which the patch is prepared.
	 * @param filename
	 *            Filename of the file to be added by the patch.
	 * @param sameContents
	 *            Should the file added by the patch has the same content as the
	 *            existing one. Enter <code>false</code>, if the file doesn't
	 *            exist.
	 * @return Content of the patch.
	 * @throws IOException
	 * @throws CoreException
	 */
	private String createPatchAddingFile(IProject project, String filename,
			boolean sameContents) throws IOException, CoreException {
		StringBuilder sb = new StringBuilder();
		sb.append(WorkspacePatcherUI.getWorkspacePatchHeader() + "\n");
		sb.append(WorkspacePatcherUI.getWorkspacePatchProjectHeader(project)
				+ "\n");
		sb.append("Index: " + filename + "\n");
		sb
				.append("===================================================================\n");
		sb.append("RCS file: " + filename + "\n");
		sb.append("diff -N " + filename + "\n");
		sb.append("--- /dev/null	1 Jan 1970 00:00:00 -0000\n");
		sb.append("+++ " + filename + "	1 Jan 1970 00:00:00 -0000\n");
		sb.append("@@ -0,0 +1,1 @@\n");
		if (sameContents) {
			sb.append("+" + getStringFromIFile(project.getFile(filename)));
		} else {
			sb.append("+" + NEW_FILE_CONTENT);
		}

		return sb.toString();
	}

	/**
	 * Return string read from an input stream.
	 *
	 * @param in
	 *            Input stream.
	 * @return String read from the stream.
	 * @throws IOException
	 */
	private static String getStringFromStream(InputStream in)
			throws IOException {
		return Utilities.readString(in, ResourcesPlugin.getEncoding());
	}

	/**
	 * Returns content of a file.
	 *
	 * @param file
	 *            A file.
	 * @return Content of the file.
	 * @throws IOException
	 * @throws CoreException
	 */
	private static String getStringFromIFile(IFile file) throws IOException,
			CoreException {
		return getStringFromStream(new BufferedInputStream(file.getContents()));
	}

	/**
	 * Check if two input streams are equal. They can't be null, all bytes need
	 * to be the same, and they need to have same length.
	 *
	 * @param inputStream1
	 *            First stream to check.
	 * @param inputStream2
	 *            Second stream to check.
	 * @throws IOException
	 */
	private static void assertEquals(InputStream inputStream1,
			InputStream inputStream2) throws IOException {

		assertNotNull(inputStream1);
		assertNotNull(inputStream2);

		int byte1, byte2;
		do {
			byte1 = inputStream1.read();
			byte2 = inputStream2.read();
			// compare every byte of the streams
			assertEquals(byte1, byte2);
		} while (byte1 != -1 || byte2 != -1);

		// the end of the streams should be reached at the same time
		assertEquals(-1, byte1);
		assertEquals(-1, byte2);

		inputStream1.close();
		inputStream2.close();
	}
}
