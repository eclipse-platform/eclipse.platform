/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *     Alexander Kurtakov <akurtako@redhat.com> - Bug 459343
 *******************************************************************************/
package org.eclipse.core.tests.resources.session;

import java.util.Arrays;
import java.util.List;
import junit.framework.Test;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.AutomatedResourceTests;
import org.eclipse.core.tests.resources.WorkspaceSessionTest;
import org.eclipse.core.tests.session.WorkspaceSessionTestSuite;

/**
 * Copies the tests from HistoryStoreTest#testFindDeleted, phrased
 * as a session test.
 */
public class FindDeletedMembersTest extends WorkspaceSessionTest {
	//common objects
	protected IWorkspaceRoot root;
	protected IProject project;
	protected IFile pfile;
	protected IFile folderAsFile;
	protected IFolder folder;
	protected IFile file;
	protected IFile file1;
	protected IFile file2;
	protected IFolder folder2;
	protected IFile file3;

	@Override
	protected void setUp() throws Exception {
		root = getWorkspace().getRoot();
		project = root.getProject("MyProject");
		pfile = project.getFile("file.txt");
		folder = project.getFolder("folder");
		file = folder.getFile("file.txt");
		folderAsFile = project.getFile(folder.getProjectRelativePath());
		file1 = folder.getFile("file1.txt");
		file2 = folder.getFile("file2.txt");
		folder2 = folder.getFolder("folder2");
		file3 = folder2.getFile("file3.txt");

	}

	@Override
	protected void tearDown() throws Exception {
		getWorkspace().save(true, getMonitor());
	}

	public void test1() {
		try {
			project.create(getMonitor());
			project.open(getMonitor());

			IFile[] df = project.findDeletedMembersWithHistory(IResource.DEPTH_ONE, getMonitor());
			assertEquals("0.1", 0, df.length);
		} catch (CoreException e) {
			fail("0.0", e);
		}

		// test that a deleted file can be found
		try {
			// create and delete a file
			pfile.create(getRandomContents(), true, getMonitor());
			pfile.delete(true, true, getMonitor());
		} catch (CoreException e) {
			fail("0.99", e);
		}
	}

	public void test2() {
		try {
			// the deleted file should show up as a deleted member of project
			IFile[] df = project.findDeletedMembersWithHistory(IResource.DEPTH_ONE, getMonitor());
			assertEquals("0.1", 1, df.length);
			assertEquals("0.2", pfile, df[0]);

			df = project.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, getMonitor());
			assertEquals("0.3", 1, df.length);
			assertEquals("0.4", pfile, df[0]);

			df = project.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, getMonitor());
			assertEquals("0.5", 0, df.length);

			// the deleted file should show up as a deleted member of workspace root
			df = root.findDeletedMembersWithHistory(IResource.DEPTH_ONE, getMonitor());
			assertEquals("0.5.1", 0, df.length);

			df = root.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, getMonitor());
			assertEquals("0.5.2", 1, df.length);
			assertEquals("0.5.3", pfile, df[0]);

			df = root.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, getMonitor());
			assertEquals("0.5.4", 0, df.length);

			// recreate the file
			pfile.create(getRandomContents(), true, getMonitor());
		} catch (CoreException e) {
			fail("0.99", e);
		}
	}

	public void test3() {
		try {
			// the deleted file should no longer show up as a deleted member of project
			IFile[] df = project.findDeletedMembersWithHistory(IResource.DEPTH_ONE, getMonitor());
			assertEquals("0.6", 0, df.length);

			df = project.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, getMonitor());
			assertEquals("0.7", 0, df.length);

			df = project.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, getMonitor());
			assertEquals("0.8", 0, df.length);

			// the deleted file should no longer show up as a deleted member of ws root
			df = root.findDeletedMembersWithHistory(IResource.DEPTH_ONE, getMonitor());
			assertEquals("0.8.1", 0, df.length);

			df = root.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, getMonitor());
			assertEquals("0.8.2", 0, df.length);

			df = root.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, getMonitor());
			assertEquals("0.8.3", 0, df.length);

		} catch (CoreException e) {
			fail("0.99", e);
		}

		// scrub the project
		try {
			project.delete(true, getMonitor());
			project.create(getMonitor());
			project.open(getMonitor());

			IFile[] df = project.findDeletedMembersWithHistory(IResource.DEPTH_ONE, getMonitor());
			assertEquals("0.9", 0, df.length);
		} catch (CoreException e) {
			fail("0.10", e);
		}

		// test folder
		try {
			// create and delete a file in a folder
			folder.create(true, true, getMonitor());
			file.create(getRandomContents(), true, getMonitor());
			file.delete(true, true, getMonitor());
		} catch (CoreException e) {
			fail("1.99", e);
		}
	}

	public void test4() {
		try {
			// the deleted file should show up as a deleted member
			IFile[] df = project.findDeletedMembersWithHistory(IResource.DEPTH_ONE, getMonitor());
			assertEquals("1.1", 0, df.length);

			df = project.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, getMonitor());
			assertEquals("1.2", 1, df.length);
			assertEquals("1.3", file, df[0]);

			df = project.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, getMonitor());
			assertEquals("1.4", 0, df.length);

			// recreate the file
			file.create(getRandomContents(), true, getMonitor());

			// the recreated file should no longer show up as a deleted member
			df = project.findDeletedMembersWithHistory(IResource.DEPTH_ONE, getMonitor());
			assertEquals("1.5", 0, df.length);

			df = project.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, getMonitor());
			assertEquals("1.6", 0, df.length);

			df = project.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, getMonitor());
			assertEquals("1.7", 0, df.length);

			// deleting the folder should bring it back into history
			folder.delete(true, true, getMonitor());
		} catch (CoreException e) {
			fail("1.99", e);
		}
	}

	public void test5() {
		try {
			// the deleted file should show up as a deleted member of project
			IFile[] df = project.findDeletedMembersWithHistory(IResource.DEPTH_ONE, getMonitor());
			assertEquals("1.8", 0, df.length);

			df = project.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, getMonitor());
			assertEquals("1.9", 1, df.length);
			assertEquals("1.10", file, df[0]);

			df = project.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, getMonitor());
			assertEquals("1.11", 0, df.length);

			// create and delete a file where the folder was
			folderAsFile.create(getRandomContents(), true, getMonitor());
			folderAsFile.delete(true, true, getMonitor());
			folder.create(true, true, getMonitor());

			// the deleted file should show up as a deleted member of folder
			df = folder.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, getMonitor());
			assertEquals("1.12", 1, df.length);
			assertEquals("1.13", folderAsFile, df[0]);

			df = folder.findDeletedMembersWithHistory(IResource.DEPTH_ONE, getMonitor());
			assertEquals("1.14", 2, df.length);
			List<IFile> dfList = Arrays.asList(df);
			assertTrue("1.15", dfList.contains(file));
			assertTrue("1.16", dfList.contains(folderAsFile));

			df = folder.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, getMonitor());
			assertEquals("1.17", 2, df.length);
			dfList = Arrays.asList(df);
			assertTrue("1.18", dfList.contains(file));
			assertTrue("1.19", dfList.contains(folderAsFile));

		} catch (CoreException e) {
			fail("1.00", e);
		}

		// scrub the project
		try {
			project.delete(true, getMonitor());
			project.create(getMonitor());
			project.open(getMonitor());

			IFile[] df = project.findDeletedMembersWithHistory(IResource.DEPTH_ONE, getMonitor());
			assertEquals("1.50", 0, df.length);
		} catch (CoreException e) {
			fail("1.51", e);
		}

		// test a bunch of deletes
		try {
			// create and delete a file in a folder
			folder.create(true, true, getMonitor());
			folder2.create(true, true, getMonitor());
			file1.create(getRandomContents(), true, getMonitor());
			file2.create(getRandomContents(), true, getMonitor());
			file3.create(getRandomContents(), true, getMonitor());
			folder.delete(true, true, getMonitor());
		} catch (CoreException e) {
			fail("3.99", e);
		}
	}

	public void test6() {
		try {
			// under root
			IFile[] df = root.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, getMonitor());
			assertEquals("3.1", 0, df.length);

			df = root.findDeletedMembersWithHistory(IResource.DEPTH_ONE, getMonitor());
			assertEquals("3.2", 0, df.length);

			df = root.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, getMonitor());
			assertEquals("3.3", 3, df.length);
			List<IFile> dfList = Arrays.asList(df);
			assertTrue("3.3.1", dfList.contains(file1));
			assertTrue("3.3.2", dfList.contains(file2));
			assertTrue("3.3.3", dfList.contains(file3));

			// under project
			df = project.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, getMonitor());
			assertEquals("3.4", 0, df.length);

			df = project.findDeletedMembersWithHistory(IResource.DEPTH_ONE, getMonitor());
			assertEquals("3.5", 0, df.length);

			df = project.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, getMonitor());
			assertEquals("3.6", 3, df.length);
			dfList = Arrays.asList(df);
			assertTrue("3.6.1", dfList.contains(file1));
			assertTrue("3.6.2", dfList.contains(file2));
			assertTrue("3.6.3", dfList.contains(file3));

			// under folder
			df = folder.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, getMonitor());
			assertEquals("3.7", 0, df.length);

			df = folder.findDeletedMembersWithHistory(IResource.DEPTH_ONE, getMonitor());
			assertEquals("3.8", 2, df.length);

			df = folder.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, getMonitor());
			assertEquals("3.9", 3, df.length);

			// under folder2
			df = folder2.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, getMonitor());
			assertEquals("3.10", 0, df.length);

			df = folder2.findDeletedMembersWithHistory(IResource.DEPTH_ONE, getMonitor());
			assertEquals("3.11", 1, df.length);

			df = folder2.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, getMonitor());
			assertEquals("3.12", 1, df.length);

		} catch (CoreException e) {
			fail("3.00", e);
		}

		try {
			project.delete(true, getMonitor());
		} catch (CoreException e) {
			fail("3.5", e);
		}
	}

	public void test7() {
		// once the project is gone, so is all the history for that project
		try {
			// under root
			IFile[] df = root.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, getMonitor());
			assertEquals("4.1", 0, df.length);

			df = root.findDeletedMembersWithHistory(IResource.DEPTH_ONE, getMonitor());
			assertEquals("4.2", 0, df.length);

			df = root.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, getMonitor());
			assertEquals("4.3", 0, df.length);

			// under project
			df = project.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, getMonitor());
			assertEquals("4.4", 0, df.length);

			df = project.findDeletedMembersWithHistory(IResource.DEPTH_ONE, getMonitor());
			assertEquals("4.5", 0, df.length);

			df = project.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, getMonitor());
			assertEquals("4.6", 0, df.length);

			// under folder
			df = folder.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, getMonitor());
			assertEquals("4.7", 0, df.length);

			df = folder.findDeletedMembersWithHistory(IResource.DEPTH_ONE, getMonitor());
			assertEquals("4.8", 0, df.length);

			df = folder.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, getMonitor());
			assertEquals("4.9", 0, df.length);

			// under folder2
			df = folder2.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, getMonitor());
			assertEquals("4.10", 0, df.length);

			df = folder2.findDeletedMembersWithHistory(IResource.DEPTH_ONE, getMonitor());
			assertEquals("4.11", 0, df.length);

			df = folder2.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, getMonitor());
			assertEquals("4.12", 0, df.length);

		} catch (CoreException e) {
			fail("4.00", e);
		}
	}

	public static Test suite() {
		return new WorkspaceSessionTestSuite(AutomatedResourceTests.PI_RESOURCES_TESTS, FindDeletedMembersTest.class);
	}
}
