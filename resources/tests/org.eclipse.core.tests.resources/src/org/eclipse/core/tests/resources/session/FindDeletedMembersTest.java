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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.PI_RESOURCES_TESTS;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomContentsStream;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.harness.session.SessionTestExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Copies the tests from HistoryStoreTest#testFindDeleted, phrased
 * as a session test.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FindDeletedMembersTest {

	@RegisterExtension
	static SessionTestExtension sessionTestExtension = SessionTestExtension.forPlugin(PI_RESOURCES_TESTS)
			.withCustomization(SessionTestExtension.createCustomWorkspace()).create();

	//common objects
	private IWorkspaceRoot root;
	private IProject project;
	private IFile pfile;
	private IFile folderAsFile;
	private IFolder folder;
	private IFile file;
	private IFile file1;
	private IFile file2;
	private IFolder folder2;
	private IFile file3;

	@BeforeEach
	public void setUp() throws Exception {
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

	private void saveWorkspace() throws CoreException {
		getWorkspace().save(true, createTestMonitor());
	}

	@Test
	@Order(1)
	public void test1() throws Exception {
		project.create(createTestMonitor());
		project.open(createTestMonitor());

		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_ONE, createTestMonitor())).isEmpty();

		// test that a deleted file can be found
		// create and delete a file
		pfile.create(createRandomContentsStream(), true, createTestMonitor());
		pfile.delete(true, true, createTestMonitor());

		saveWorkspace();
	}

	@Test
	@Order(2)
	public void test2() throws Exception {
		// the deleted file should show up as a deleted member of project
		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_ONE, createTestMonitor()))
				.containsExactly(pfile);
		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, createTestMonitor()))
				.containsExactly(pfile);
		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, createTestMonitor())).isEmpty();

		// the deleted file should show up as a deleted member of workspace root
		assertThat(root.findDeletedMembersWithHistory(IResource.DEPTH_ONE, createTestMonitor())).isEmpty();
		assertThat(root.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, createTestMonitor()))
				.containsExactly(pfile);
		assertThat(root.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, createTestMonitor())).isEmpty();

		// recreate the file
		pfile.create(createRandomContentsStream(), true, createTestMonitor());

		saveWorkspace();
	}

	@Test
	@Order(3)
	public void test3() throws Exception {
		// the deleted file should no longer show up as a deleted member of project
		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_ONE, createTestMonitor())).isEmpty();
		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, createTestMonitor())).isEmpty();
		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, createTestMonitor())).isEmpty();

		// the deleted file should no longer show up as a deleted member of ws root
		assertThat(root.findDeletedMembersWithHistory(IResource.DEPTH_ONE, createTestMonitor())).isEmpty();
		assertThat(root.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, createTestMonitor())).isEmpty();
		assertThat(root.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, createTestMonitor())).isEmpty();

		// scrub the project
		project.delete(true, createTestMonitor());
		project.create(createTestMonitor());
		project.open(createTestMonitor());

		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_ONE, createTestMonitor())).isEmpty();

		// test folder
		// create and delete a file in a folder
		folder.create(true, true, createTestMonitor());
		file.create(createRandomContentsStream(), true, createTestMonitor());
		file.delete(true, true, createTestMonitor());

		saveWorkspace();
	}

	@Test
	@Order(4)
	public void test4() throws Exception {
		// the deleted file should show up as a deleted member
		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_ONE, createTestMonitor())).isEmpty();
		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, createTestMonitor()))
				.containsExactly(file);
		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, createTestMonitor())).isEmpty();

		// recreate the file
		file.create(createRandomContentsStream(), true, createTestMonitor());

		// the recreated file should no longer show up as a deleted member
		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_ONE, createTestMonitor())).isEmpty();
		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, createTestMonitor())).isEmpty();
		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, createTestMonitor())).isEmpty();

		// deleting the folder should bring it back into history
		folder.delete(true, true, createTestMonitor());

		saveWorkspace();
	}

	@Test
	@Order(5)
	public void test5() throws Exception {
		// the deleted file should show up as a deleted member of project
		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_ONE, createTestMonitor())).isEmpty();
		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, createTestMonitor()))
				.containsExactly(file);
		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, createTestMonitor())).isEmpty();

		// create and delete a file where the folder was
		folderAsFile.create(createRandomContentsStream(), true, createTestMonitor());
		folderAsFile.delete(true, true, createTestMonitor());
		folder.create(true, true, createTestMonitor());

		// the deleted file should show up as a deleted member of folder
		assertThat(folder.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, createTestMonitor()))
				.containsExactly(folderAsFile);
		assertThat(folder.findDeletedMembersWithHistory(IResource.DEPTH_ONE, createTestMonitor()))
				.containsExactlyInAnyOrder(file, folderAsFile);
		assertThat(folder.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, createTestMonitor()))
				.containsExactlyInAnyOrder(file, folderAsFile);

		// scrub the project
		project.delete(true, createTestMonitor());
		project.create(createTestMonitor());
		project.open(createTestMonitor());

		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_ONE, createTestMonitor())).isEmpty();

		// test a bunch of deletes
		// create and delete a file in a folder
		folder.create(true, true, createTestMonitor());
		folder2.create(true, true, createTestMonitor());
		file1.create(createRandomContentsStream(), true, createTestMonitor());
		file2.create(createRandomContentsStream(), true, createTestMonitor());
		file3.create(createRandomContentsStream(), true, createTestMonitor());
		folder.delete(true, true, createTestMonitor());

		saveWorkspace();
	}

	@Test
	@Order(6)
	public void test6() throws Exception {
		// under root
		assertThat(root.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, createTestMonitor())).isEmpty();
		assertThat(root.findDeletedMembersWithHistory(IResource.DEPTH_ONE, createTestMonitor())).isEmpty();
		assertThat(root.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, createTestMonitor()))
				.containsExactlyInAnyOrder(file1, file2, file3);

		// under project
		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, createTestMonitor())).isEmpty();
		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_ONE, createTestMonitor())).isEmpty();
		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, createTestMonitor()))
				.containsExactlyInAnyOrder(file1, file2, file3);

		// under folder
		assertThat(folder.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, createTestMonitor())).isEmpty();
		assertThat(folder.findDeletedMembersWithHistory(IResource.DEPTH_ONE, createTestMonitor())).hasSize(2);
		assertThat(folder.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, createTestMonitor())).hasSize(3);

		// under folder2
		assertThat(folder2.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, createTestMonitor())).isEmpty();
		assertThat(folder2.findDeletedMembersWithHistory(IResource.DEPTH_ONE, createTestMonitor())).hasSize(1);
		assertThat(folder2.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, createTestMonitor())).hasSize(1);

		project.delete(true, createTestMonitor());

		saveWorkspace();
	}

	@Test
	@Order(7)
	public void test7() throws Exception {
		// once the project is gone, so is all the history for that project
		// under root
		assertThat(root.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, createTestMonitor())).isEmpty();
		assertThat(root.findDeletedMembersWithHistory(IResource.DEPTH_ONE, createTestMonitor())).isEmpty();
		assertThat(root.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, createTestMonitor())).isEmpty();

		// under project
		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, createTestMonitor())).isEmpty();
		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_ONE, createTestMonitor())).isEmpty();
		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, createTestMonitor())).isEmpty();

		// under folder
		assertThat(folder.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, createTestMonitor())).isEmpty();
		assertThat(folder.findDeletedMembersWithHistory(IResource.DEPTH_ONE, createTestMonitor())).isEmpty();
		assertThat(folder.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, createTestMonitor())).isEmpty();

		// under folder2
		assertThat(folder2.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, createTestMonitor())).isEmpty();
		assertThat(folder2.findDeletedMembersWithHistory(IResource.DEPTH_ONE, createTestMonitor())).isEmpty();
		assertThat(folder2.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, createTestMonitor())).isEmpty();

		saveWorkspace();
	}

}
