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

import static java.lang.System.currentTimeMillis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomContentsStream;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.isReadOnlySupported;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform.OS;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(WorkspaceResetExtension.class)
public class IFileTest {

	/**
	 * Bug states that the error code in the CoreException which is thrown when
	 * you try to create a file in a read-only folder on Linux should be
	 * ERROR_WRITE.
	 */
	@Test
	@Disabled("This test is no longer valid since the error code is dependent on whether or not the parent folder is marked as read-only. We need to write a different test to make the file.create fail.")
	public void testBug25658() throws CoreException {
		// We need to know whether or not we can unset the read-only flag
		// in order to perform this test.
		assumeTrue("only relevant for platforms supporting read-only files", isReadOnlySupported());

		// Don't test this on Windows
		assumeFalse("not relevant on Windows", OS.isWindows());

		IProject project = getWorkspace().getRoot().getProject("MyProject");
		IFolder folder = project.getFolder("folder");
		createInWorkspace(new IResource[] {project, folder});
		IFile file = folder.getFile("file.txt");

		try {
			folder.setReadOnly(true);
			assertThat(folder).matches(IResource::isReadOnly, "is read only");
			CoreException exception = assertThrows(CoreException.class,
					() -> file.create(createRandomContentsStream(), true, createTestMonitor()));
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
		assumeTrue("only relevant for platforms supporting read-only files", isReadOnlySupported());

		// Only run this test on Linux for now since Windows lets you create
		// a file within a read-only folder.
		assumeTrue("only relevant on Linux", OS.isLinux());

		IProject project = getWorkspace().getRoot().getProject("MyProject");
		IFolder folder = project.getFolder("folder");
		createInWorkspace(new IResource[] {project, folder});
		IFile file = folder.getFile("file.txt");

		try {
			folder.setReadOnly(true);
			assertThat(folder).matches(IResource::isReadOnly, "is read only");
			CoreException exception = assertThrows(CoreException.class,
					() -> file.create(createRandomContentsStream(), true, createTestMonitor()));
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
		createInWorkspace(project);
		assertThat(descFile).matches(IResource::exists, "exists");

		IProjectDescription desc = project.getDescription();

		//change the local file timestamp
		long newTime = System.currentTimeMillis() + 10000;
		descFile.setLocalTimeStamp(newTime);

		assertThat(descFile).matches(it -> it.isSynchronized(IResource.DEPTH_ZERO), "is synchronized");

		// try setting the description -- shouldn't fail
		project.setDescription(desc, createTestMonitor());
	}

	/**
	 * Do not throw RuntimeException when accessing a deleted file
	 */
	@Test
	public void testIssue2290() throws CoreException, InterruptedException {
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		IFile subject = project.getFile("subject.txt");
		Job createDelete = Job.create("Create/delete", monitor -> {
			try {
				while (!monitor.isCanceled()) {
					createInWorkspace(subject);
					while (!monitor.isCanceled()) {
						try {
							subject.delete(true, monitor);
							break;
						} catch (CoreException e) {
							// On Windows, files opened for reading can't be deleted, try again
							if (e.getStatus().getCode() != IResourceStatus.FAILED_DELETE_LOCAL) {
								throw e;
							}
						}
					}
				}
			} catch (CoreException e) {
				return e.getStatus();
			}
			return Status.OK_STATUS;
		});
		createDelete.setPriority(Job.INTERACTIVE);

		long stop = currentTimeMillis() + 1000;
		try {
			createDelete.schedule();
			while (currentTimeMillis() < stop) {
				assertContentAccessibleOrNotFound(subject); // should not throw
			}
		} finally {
			createDelete.cancel();
			createDelete.join();
			IStatus result = createDelete.getResult();
			if (!result.isOK()) {
				throw new CoreException(result);
			}
		}
	}

	private void assertContentAccessibleOrNotFound(IFile file) {
		try (InputStream contents = file.getContents(false)) {
			contents.transferTo(OutputStream.nullOutputStream());
		} catch (IOException e) {
			throw new AssertionError(e);
		} catch (CoreException e) {
			switch (e.getStatus().getCode()) {
			case IResourceStatus.RESOURCE_NOT_LOCAL:
			case IResourceStatus.RESOURCE_NOT_FOUND:
			case IResourceStatus.FAILED_READ_LOCAL:
			case IResourceStatus.OUT_OF_SYNC_LOCAL:
				break;
			default:
				throw new AssertionError(e);
			}
		}
		try (InputStream contents = file.getContents(true)) {
			contents.transferTo(OutputStream.nullOutputStream());
		} catch (IOException e) {
			throw new AssertionError(e);
		} catch (CoreException e) {
			switch (e.getStatus().getCode()) {
			case IResourceStatus.RESOURCE_NOT_LOCAL:
			case IResourceStatus.RESOURCE_NOT_FOUND:
			case IResourceStatus.FAILED_READ_LOCAL:
				break;
			default:
				throw new AssertionError(e);
			}
		}
	}

}
