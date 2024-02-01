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

import static java.util.function.Predicate.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.compareContent;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInputStream;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomContentsStream;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.eclipse.compare.internal.Utilities;
import org.eclipse.compare.internal.core.patch.FileDiffResult;
import org.eclipse.compare.internal.core.patch.FilePatch2;
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
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.junit.Rule;
import org.junit.Test;

public class FileDiffResultTest {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	private static final String PATCH_FILE = "patchfile";

	private static final String NEW_FILENAME = "newfile";

	private static final String NEW_FILE_CONTENT = "Hi There";

	private final PatchConfiguration patchConfiguration = new PatchConfiguration();

	/**
	 * Tests applying a patch which creates a new file in a project. The file
	 * doesn't exist in the project.
	 */
	@Test
	public void testPatchAddsNewFile() throws CoreException, IOException {
		IProject project = getWorkspace().getRoot().getProject("FileDiffResultTest");
		createInWorkspace(project);
		createInWorkspace(project.getFile("oldfile"));

		// create the patch file
		IFile file = project.getFile(PATCH_FILE);
		file.create(createInputStream(createPatchAddingFile(project, NEW_FILENAME, false /* the file doesn't exist */)),
				true, null);

		assertThat(project.getFile(NEW_FILENAME)).matches(not(IFile::exists), "does not exist");

		IFilePatch[] filePatch = ApplyPatchOperation.parsePatch(file);
		assertThat(filePatch).hasSize(1);

		IFilePatchResult filePatchResult = filePatch[0].apply((IStorage) null, patchConfiguration, createTestMonitor());
		assertThat(filePatchResult).matches(IFilePatchResult::hasMatches, "has matches");
		assertThat(filePatchResult.getRejects()).isEmpty();
		assertThat(getStringFromStream(filePatchResult.getOriginalContents())).isEmpty();
		assertThat(getStringFromStream(filePatchResult.getPatchedContents())).isEqualTo(NEW_FILE_CONTENT);
	}

	/**
	 * Tests applying a patch which creates a new file in a project. The file
	 * already exists in the project.
	 */
	@Test
	public void testPatchAddsExistingFileWithSameContents() throws CoreException, IOException {
		IProject project = getWorkspace().getRoot().getProject("FileDiffResultTest");
		createInWorkspace(project);
		createInWorkspace(project.getFile(NEW_FILENAME));
		project.getFile(NEW_FILENAME).setContents(createRandomContentsStream(), true, false, null);

		// create the patch file
		IFile file = project.getFile(PATCH_FILE);
		file.create(createInputStream(createPatchAddingFile(project, NEW_FILENAME, true)), true, null);

		assertThat(project.getFile(NEW_FILENAME)).matches(IFile::exists, "exists");

		IFilePatch[] filePatch = ApplyPatchOperation.parsePatch(file);
		assertThat(filePatch).hasSize(1);

		IFilePatchResult filePatchResult = filePatch[0].apply(project.getFile(NEW_FILENAME), patchConfiguration,
				createTestMonitor());

		assertThat(filePatchResult).matches(not(IFilePatchResult::hasMatches), "has no matches");
		assertThat(filePatchResult.getRejects()).hasSize(1);

		assertThat(filePatchResult.getOriginalContents()).isNotNull();
		assertThat(filePatchResult.getPatchedContents()).isNotNull();

		compareContent(new FileInputStream(project.getFile(NEW_FILENAME).getLocation().toFile()),
				filePatchResult.getOriginalContents());
		compareContent(filePatchResult.getOriginalContents(), filePatchResult.getPatchedContents());
	}

	/**
	 * Tests applying a patch which creates a new file in a project. The file
	 * already exists in the project, but has different contents.
	 */
	@Test
	public void testPatchAddsExistingFileWithDifferentContents() throws CoreException, IOException {
		IProject project = getWorkspace().getRoot().getProject("FileDiffResultTest");
		createInWorkspace(project);
		createInWorkspace(project.getFile(NEW_FILENAME));

		project.getFile(NEW_FILENAME).setContents(createInputStream("I'm a different content"), IResource.NONE, null);

		// create the patch file
		IFile file = project.getFile(PATCH_FILE);
		file.create(createInputStream(createPatchAddingFile(project, NEW_FILENAME, false)), true, null);

		assertThat(project.getFile(NEW_FILENAME)).matches(IFile::exists, "exists");

		IFilePatch[] filePatch = ApplyPatchOperation.parsePatch(file);
		assertThat(filePatch).hasSize(1);

		IFilePatchResult filePatchResult = filePatch[0].apply(project.getFile(NEW_FILENAME), patchConfiguration,
				createTestMonitor());
		assertThat(filePatchResult).matches(not(IFilePatchResult::hasMatches), "has no matches");
		assertThat(filePatchResult.getRejects()).hasSize(1);

		assertThat(filePatchResult.getOriginalContents()).isNotNull();
		assertThat(filePatchResult.getPatchedContents()).isNotNull();

		compareContent(new FileInputStream(project.getFile(NEW_FILENAME).getLocation().toFile()),
				filePatchResult.getOriginalContents());
		assertThat(getStringFromStream(filePatchResult.getOriginalContents())).isEqualTo("I'm a different content");
		compareContent(filePatchResult.getOriginalContents(), filePatchResult.getPatchedContents());
	}

	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=185379
	@Test
	public void testFileDiffResultWithNullPath() {
		MyFileDiff myFileDiff = new MyFileDiff();
		FileDiffResult fileDiffResult = new FileDiffResult(myFileDiff, patchConfiguration);
		fileDiffResult.calculateFuzz(new ArrayList<>(), null);
	}

	// https://bugs.eclipse.org/bugs/show_bug.cgi?id=187365
	@Test
	public void testExcludePartOfNonWorkspacePatch() {
		Patcher patcher = new Patcher();
		MyFileDiff myFileDiff = new MyFileDiff();
		patcher.setEnabled(myFileDiff, false);
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
	 */
	private static String getStringFromIFile(IFile file) throws IOException,
			CoreException {
		return getStringFromStream(new BufferedInputStream(file.getContents()));
	}

}
