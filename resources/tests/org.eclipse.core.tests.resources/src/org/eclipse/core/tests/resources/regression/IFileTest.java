/*******************************************************************************
 *  Copyright (c) 2000, 2012 IBM Corporation and others.
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
package org.eclipse.core.tests.resources.regression;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.junit.Assert.assertThrows;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform.OS;
import org.eclipse.core.tests.resources.ResourceTest;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class IFileTest extends ResourceTest {
	private final boolean DISABLED = true;

	/**
	 * Bug states that the error code in the CoreException which is thrown when
	 * you try to create a file in a read-only folder on Linux should be
	 * ERROR_WRITE.
	 */
	@Test
	public void testBug25658() throws CoreException {

		// This test is no longer valid since the error code is dependent on whether
		// or not the parent folder is marked as read-only. We need to write a different
		// test to make the file.create fail.
		Assume.assumeFalse(DISABLED);

		// We need to know whether or not we can unset the read-only flag
		// in order to perform this test.
		Assume.assumeTrue(isReadOnlySupported());

		// Don't test this on Windows
		Assume.assumeFalse(OS.isWindows());

		IProject project = getWorkspace().getRoot().getProject("MyProject");
		IFolder folder = project.getFolder("folder");
		ensureExistsInWorkspace(new IResource[] {project, folder}, true);
		IFile file = folder.getFile("file.txt");

		try {
			folder.setReadOnly(true);
			assertTrue(folder.isReadOnly());
			CoreException exception = assertThrows(CoreException.class,
					() -> file.create(getRandomContents(), true, createTestMonitor()));
			assertEquals(IResourceStatus.FAILED_WRITE_LOCAL, exception.getStatus().getCode());
		} finally {
			folder.setReadOnly(false);
		}
	}

	/**
	 * Bug requests that if a failed file write occurs on Linux that we check the immediate
	 * parent to see if it is read-only so we can return a better error code and message
	 * to the user.
	 */
	@Test
	public void testBug25662() throws CoreException {

		// We need to know whether or not we can unset the read-only flag
		// in order to perform this test.
		Assume.assumeTrue(isReadOnlySupported());

		// Only run this test on Linux for now since Windows lets you create
		// a file within a read-only folder.
		Assume.assumeTrue(OS.isLinux());

		IProject project = getWorkspace().getRoot().getProject("MyProject");
		IFolder folder = project.getFolder("folder");
		ensureExistsInWorkspace(new IResource[] {project, folder}, true);
		IFile file = folder.getFile("file.txt");

		try {
			folder.setReadOnly(true);
			assertTrue(folder.isReadOnly());
			CoreException exception = assertThrows(CoreException.class,
					() -> file.create(getRandomContents(), true, createTestMonitor()));
			assertEquals(IResourceStatus.PARENT_READ_ONLY, exception.getStatus().getCode());
		} finally {
			folder.setReadOnly(false);
		}
	}

	/**
	 * Tests setting local timestamp of project description file
	 */
	@Test
	public void testBug43936() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		IFile descFile = project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		ensureExistsInWorkspace(project, true);
		assertTrue("1.0", descFile.exists());

		IProjectDescription desc = project.getDescription();

		//change the local file timestamp
		long newTime = System.currentTimeMillis() + 10000;
		descFile.setLocalTimeStamp(newTime);

		assertTrue("2.0", descFile.isSynchronized(IResource.DEPTH_ZERO));

		// try setting the description -- shouldn't fail
		project.setDescription(desc, createTestMonitor());
	}
}
