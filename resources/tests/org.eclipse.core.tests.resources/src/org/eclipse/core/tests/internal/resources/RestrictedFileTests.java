/*******************************************************************************
 * Copyright (c) 2026 Simeon Andreev and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Simeon Andreev - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.internal.resources;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(WorkspaceResetExtension.class)
public class RestrictedFileTests {

	@Test
	public void testRestrictedFile() throws Exception {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		IProject project = root.getProject(RestrictedFileTests.class.getSimpleName());
		try {
			project.create(null);
			project.open(null);

			IFile file1 = project.getFile("test1.txt");
			IFile file2 = project.getFile("test2.txt");

			try {
				file1.isContentRestricted();
				fail("Should not work on not existing files");
			} catch (CoreException e) {
				// expected, file should not exist
			}

			file1.create("line 1".getBytes(), IResource.FORCE, null);
			file2.create("line 1".getBytes(), IResource.FORCE, null);

			assertFalse(file1.isContentRestricted(), "Expected file to not be restricted");
			assertFalse(file2.isContentRestricted(), "Expected file to not be restricted");

			file1.setContentRestricted(true);
			assertTrue(file1.isContentRestricted(), "Expected file to be restricted");
			assertFalse(file2.isContentRestricted(), "Expected file to not be restricted");

			file1 = project.getFile("test1.txt");
			file2 = project.getFile("test2.txt");
			assertTrue(file1.isContentRestricted(), "Expected file to be restricted");
			assertFalse(file2.isContentRestricted(), "Expected file to not be restricted");
		} finally {
			project.delete(true, null);
		}
	}
}
