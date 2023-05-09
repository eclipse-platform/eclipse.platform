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

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
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
	public void testBug25658() {

		// This test is no longer valid since the error code is dependent on whether
		// or not the parent folder is marked as read-only. We need to write a different
		// test to make the file.create fail.
		Assume.assumeFalse(DISABLED);

		// We need to know whether or not we can unset the read-only flag
		// in order to perform this test.
		Assume.assumeTrue(isReadOnlySupported());

		// Don't test this on Windows
		Assume.assumeFalse(isWindows());

		IProject project = getWorkspace().getRoot().getProject("MyProject");
		IFolder folder = project.getFolder("folder");
		ensureExistsInWorkspace(new IResource[] {project, folder}, true);
		IFile file = folder.getFile("file.txt");

		try {
			folder.setReadOnly(true);
			assertTrue("0.0", folder.isReadOnly());
			try {
				file.create(getRandomContents(), true, getMonitor());
				fail("0.1");
			} catch (CoreException e) {
				assertEquals("0.2", IResourceStatus.FAILED_WRITE_LOCAL, e.getStatus().getCode());
			}
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
	public void testBug25662() {

		// We need to know whether or not we can unset the read-only flag
		// in order to perform this test.
		Assume.assumeTrue(isReadOnlySupported());

		// Only run this test on Linux for now since Windows lets you create
		// a file within a read-only folder.
		Assume.assumeTrue(isLinux());

		IProject project = getWorkspace().getRoot().getProject("MyProject");
		IFolder folder = project.getFolder("folder");
		ensureExistsInWorkspace(new IResource[] {project, folder}, true);
		IFile file = folder.getFile("file.txt");

		try {
			folder.setReadOnly(true);
			assertTrue("0.0", folder.isReadOnly());
			try {
				file.create(getRandomContents(), true, getMonitor());
				fail("0.1");
			} catch (CoreException e) {
				assertEquals("0.2", IResourceStatus.PARENT_READ_ONLY, e.getStatus().getCode());
			}
		} finally {
			folder.setReadOnly(false);
		}
	}

	/**
	 * Tests setting local timestamp of project description file
	 */
	@Test
	public void testBug43936() {
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		IFile descFile = project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		ensureExistsInWorkspace(project, true);
		assertTrue("1.0", descFile.exists());

		IProjectDescription desc = null;
		try {
			desc = project.getDescription();
		} catch (CoreException e) {
			fail("1.99", e);
		}
		//change the local file timestamp
		long newTime = System.currentTimeMillis() + 10000;
		try {
			descFile.setLocalTimeStamp(newTime);
		} catch (CoreException e1) {
			fail("2.99", e1);
		}

		assertTrue("2.0", descFile.isSynchronized(IResource.DEPTH_ZERO));

		try {
			//try setting the description -- shouldn't fail
			project.setDescription(desc, getMonitor());
		} catch (CoreException e2) {
			fail("3.99", e2);
		}
	}
}
