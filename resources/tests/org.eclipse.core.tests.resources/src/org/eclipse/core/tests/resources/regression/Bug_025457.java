/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
package org.eclipse.core.tests.resources.regression;

import static java.util.function.Predicate.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomString;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.isReadOnlySupported;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.InputStream;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform.OS;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests regression of bug 25457.  In this case, attempting to move a project
 * that is only a case change, where the move fails due to another handle being
 * open on a file in the hierarchy, would cause deletion of the source.
 *
 * Note: this is similar to Bug_32076, which deals with failure to move in
 * the non case-change scenario.
 */
@ExtendWith(WorkspaceResetExtension.class)
public class Bug_025457 {

	@Test
	public void testFile() throws Exception {
		assumeTrue(OS.isWindows(), "only relevant on Windows");

		IProject source = getWorkspace().getRoot().getProject("project");
		IFile sourceFile = source.getFile("file.txt");
		IFile destFile = source.getFile("File.txt");
		createInWorkspace(source);
		final String content = createRandomString();
		createInWorkspace(sourceFile, content);

		//open a stream in the source to cause the rename to fail
		try (InputStream stream = sourceFile.getContents()) {
			//try to rename the file (should fail)
			assertThrows(CoreException.class,
					() -> sourceFile.move(destFile.getFullPath(), IResource.NONE, createTestMonitor()));
		}
		//ensure source still exists and has same content
		assertThat(source).matches(IResource::exists, "exists");
		assertThat(sourceFile).matches(IResource::exists, "exists");
		assertEquals(content, sourceFile.readString());
		//ensure destination file does not exist
		assertThat(destFile).matches(not(IResource::exists), "not exists");
	}

	@Test
	public void testFolder() throws IOException, CoreException {
		//native code must also be present so move can detect the case change
		assumeTrue(OS.isWindows() && isReadOnlySupported(), "only relevant on Windows");

		IProject source = getWorkspace().getRoot().getProject("SourceProject");
		IFolder sourceFolder = source.getFolder("folder");
		IFile sourceFile = sourceFolder.getFile("Important.txt");
		IFolder destFolder = source.getFolder("Folder");
		IFile destFile = destFolder.getFile("Important.txt");
		createInWorkspace(source);
		createInWorkspace(sourceFolder);
		createInWorkspace(sourceFile);

		//open a stream in the source to cause the rename to fail
		try (InputStream stream = sourceFile.getContents()) {
			//try to rename the project (should fail)
			assertThrows(CoreException.class,
					() -> sourceFolder.move(destFolder.getFullPath(), IResource.NONE, createTestMonitor()));
			//ensure source still exists
			assertThat(source).matches(IResource::exists, "exists");
			assertThat(sourceFolder).matches(IResource::exists, "exists");
			assertThat(sourceFile).matches(IResource::exists, "exists");

			//ensure destination does not exist
			assertThat(destFolder).matches(not(IResource::exists), "not exists");
			assertThat(destFile).matches(not(IResource::exists), "not exists");
		}
	}

	@Test
	public void testProject() throws IOException, CoreException {
		assumeTrue(OS.isWindows(), "only relevant on Windows");

		IProject source = getWorkspace().getRoot().getProject("project");
		IProject destination = getWorkspace().getRoot().getProject("Project");
		IFile sourceFile = source.getFile("Important.txt");
		IFile destFile = destination.getFile("Important.txt");
		createInWorkspace(source);
		createInWorkspace(sourceFile);

		//open a stream in the source to cause the rename to fail
		try (InputStream stream = sourceFile.getContents()) {
			//try to rename the project (should fail)
			assertThrows(CoreException.class,
					() -> source.move(destination.getFullPath(), IResource.NONE, createTestMonitor()));

			//ensure source does not exist
			assertThat(source).matches(not(IResource::exists), "not exists");
			assertThat(sourceFile).matches(not(IResource::exists), "not exists");

			//ensure destination does not exist
			assertThat(destination).matches(IResource::exists, "exists");
			assertThat(destFile).matches(IResource::exists, "exists");
		}
	}
}
