/*******************************************************************************
 *  Copyright (c) 2004, 2017 IBM Corporation and others.
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
package org.eclipse.core.tests.resources.perf;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomContentsStream;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomString;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createUniqueString;
import static org.eclipse.core.tests.resources.ResourceTestUtil.removeFromWorkspace;

import org.eclipse.core.internal.localstore.IHistoryStore;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.tests.harness.PerformanceTestRunner;
import org.eclipse.core.tests.internal.localstore.HistoryStoreTest;
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

/**
 * Contains a set of use case-oriented performance tests for the local history.
 *
 * @since 3.1
 */
public class LocalHistoryPerformanceTest {

	@Rule
	public TestName testName = new TestName();

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	void cleanHistory() {
		((Workspace) getWorkspace()).getFileSystemManager().getHistoryStore().clean(createTestMonitor());
	}

	/**
	 * Creates a tree of resources containing history.
	 */
	void createTree(IFolder base, final int filesPerFolder, final int statesPerFile) throws CoreException {
		final IFolder[] folders = new IFolder[5];
		folders[0] = base.getFolder("folder1");
		folders[1] = base.getFolder("folder2");
		folders[2] = folders[0].getFolder("folder3");
		folders[3] = folders[2].getFolder("folder4");
		folders[4] = folders[3].getFolder("folder5");
		final IWorkspace workspace = getWorkspace();
		workspace.run((IWorkspaceRunnable) monitor -> {
			createInWorkspace(folders);
			for (IFolder folder : folders) {
				for (int j = 0; j < filesPerFolder; j++) {
					IFile file = folder.getFile("file" + j);
					createInWorkspace(file, createRandomString());
					for (int k = 0; k < statesPerFile; k++) {
						file.setContents(createRandomContentsStream(), IResource.KEEP_HISTORY, createTestMonitor());
					}
				}
			}
		}, workspace.getRuleFactory().modifyRule(workspace.getRoot()), IWorkspace.AVOID_UPDATE, createTestMonitor());
	}

	IWorkspaceDescription setMaxFileStates(int maxFileStates) throws CoreException {
		IWorkspaceDescription currentDescription = getWorkspace().getDescription();
		IWorkspaceDescription testDescription = getWorkspace().getDescription();
		testDescription.setMaxFileStates(maxFileStates);
		getWorkspace().setDescription(testDescription);
		return currentDescription;
	}

	@After
	public void tearDown() throws Exception {
		HistoryStoreTest.wipeHistoryStore(createTestMonitor());
	}

	@Test
	public void testAddState() throws Exception {
		setMaxFileStates(100);
		final IFile file = getWorkspace().getRoot().getProject("proj1").getFile("file.txt");
		new PerformanceTestRunner() {

			@Override
			protected void setUp() throws CoreException {
				createInWorkspace(file, createRandomString());
			}

			@Override
			protected void tearDown() throws CoreException {
				file.clearHistory(createTestMonitor());
				file.delete(IResource.FORCE, createTestMonitor());
			}

			@Override
			protected void test() throws CoreException {
				file.setContents(createRandomContentsStream(), IResource.KEEP_HISTORY, createTestMonitor());
			}
		}.run(getClass(), testName.getMethodName(), 10, 30);
	}

	@Test
	public void testBug28603() throws Exception {
		final IProject project = getWorkspace().getRoot().getProject("myproject");
		final IFolder folder1 = project.getFolder("myfolder1");
		final IFolder folder2 = project.getFolder("myfolder2");
		final IFile file1 = folder1.getFile("myfile.txt");
		final IFile file2 = folder2.getFile(file1.getName());

		new PerformanceTestRunner() {

			@Override
			protected void setUp() throws CoreException {
				createInWorkspace(new IResource[] {project, folder1, folder2});
				file1.create(createRandomContentsStream(), IResource.FORCE, createTestMonitor());
				file1.setContents(createRandomContentsStream(), IResource.FORCE | IResource.KEEP_HISTORY,
						createTestMonitor());
				file1.setContents(createRandomContentsStream(), IResource.FORCE | IResource.KEEP_HISTORY,
						createTestMonitor());
				file1.setContents(createRandomContentsStream(), IResource.FORCE | IResource.KEEP_HISTORY,
						createTestMonitor());
			}

			@Override
			protected void tearDown() throws CoreException {
				removeFromWorkspace(getWorkspace().getRoot());
				IHistoryStore store = ((Workspace) getWorkspace()).getFileSystemManager().getHistoryStore();
				// Remove all the entries from the history store index. Note that
				// this does not cause the history store states to be removed.
				store.remove(IPath.ROOT, createTestMonitor());
				// Now make sure all the states are really removed.
				store.removeGarbage();
			}

			@Override
			protected void test() throws CoreException {
				file1.move(file2.getFullPath(), true, true, createTestMonitor());
				file2.move(file1.getFullPath(), true, true, createTestMonitor());
			}
		}.run(getClass(), testName.getMethodName(), 10, 5);
	}

	private void testClearHistory(final int filesPerFolder, final int statesPerFile) throws Exception {
		IProject project = getWorkspace().getRoot().getProject("proj1");
		final IFolder base = project.getFolder("base");
		removeFromWorkspace(base);
		new PerformanceTestRunner() {
			private IWorkspaceDescription original;

			@Override
			protected void setUp() throws CoreException {
				original = setMaxFileStates(1);
				// make sure we start with no garbage
				cleanHistory();
				// create our own garbage
				createTree(base, filesPerFolder, statesPerFile);
				removeFromWorkspace(base);
			}

			@Override
			protected void tearDown() throws CoreException {
				if (original != null) {
					getWorkspace().setDescription(original);
				}
			}

			@Override
			protected void test() throws CoreException {
				base.clearHistory(createTestMonitor());
			}
		}.run(getClass(), testName.getMethodName(), 4, 3);
	}

	@Test
	public void testClearHistory100x4() throws Exception {
		testClearHistory(100, 4);
	}

	@Test
	public void testClearHistory20x20() throws Exception {
		testClearHistory(20, 20);
	}

	@Test
	public void testClearHistory4x100() throws Exception {
		testClearHistory(4, 100);
	}

	private void testCopyHistory(int filesPerFolder, int statesPerFile) throws Exception {
		IProject project = getWorkspace().getRoot().getProject("proj1");
		IFolder base = project.getFolder("base");
		createTree(base, filesPerFolder, statesPerFile);
		// need a final reference so the inner class can see it
		final IProject[] tmpProject = new IProject[] {project};
		new PerformanceTestRunner() {
			@Override
			protected void test() throws CoreException {
				String newProjectName = createUniqueString();
				IProject newProject = getWorkspace().getRoot().getProject(newProjectName);
				tmpProject[0].copy(newProject.getFullPath(), true, createTestMonitor());
				tmpProject[0] = newProject;
			}
		}.run(getClass(), testName.getMethodName(), 10, 1);
	}

	@Test
	public void testCopyHistory100x4() throws Exception {
		testCopyHistory(100, 4);
	}

	@Test
	public void testCopyHistory20x20() throws Exception {
		testCopyHistory(20, 20);
	}

	@Test
	public void testCopyHistory4x100() throws Exception {
		testCopyHistory(4, 100);
	}

	private void testGetDeletedMembers(int filesPerFolder, int statesPerFile) throws Exception {
		IProject project = getWorkspace().getRoot().getProject("proj1");
		IFolder base = project.getFolder("base");
		createTree(base, filesPerFolder, statesPerFile);
		removeFromWorkspace(base);
		// need a final reference so the inner class can see it
		final IProject tmpProject = project;
		new PerformanceTestRunner() {
			@Override
			protected void test() throws CoreException {
				tmpProject.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, createTestMonitor());
			}
		}.run(getClass(), testName.getMethodName(), 2, 5);
	}

	@Test
	public void testGetDeletedMembers100x4() throws Exception {
		testGetDeletedMembers(100, 4);
	}

	@Test
	public void testGetDeletedMembers20x20() throws Exception {
		testGetDeletedMembers(20, 20);
	}

	@Test
	public void testGetDeletedMembers4x100() throws Exception {
		testGetDeletedMembers(4, 100);
	}

	@Test
	public void testGetHistory() throws Exception {
		IProject project = getWorkspace().getRoot().getProject("proj1");
		final IFile file = project.getFile("file.txt");
		createInWorkspace(file, createRandomString());
		for (int i = 0; i < 100; i++) {
			file.setContents(createRandomContentsStream(), IResource.KEEP_HISTORY, createTestMonitor());
		}
		new PerformanceTestRunner() {
			@Override
			protected void test() throws CoreException {
				file.getHistory(createTestMonitor());
			}
		}.run(getClass(), testName.getMethodName(), 1, 150);
	}

	private void testHistoryCleanUp(final int filesPerFolder, final int statesPerFile) throws Exception {
		IProject project = getWorkspace().getRoot().getProject("proj1");
		final IFolder base = project.getFolder("base");
		removeFromWorkspace(base);
		new PerformanceTestRunner() {
			private IWorkspaceDescription original;

			@Override
			protected void setUp() throws CoreException {
				original = setMaxFileStates(1);
				// make sure we start with no garbage
				cleanHistory();
				// create our own garbage
				createTree(base, filesPerFolder, statesPerFile);
				removeFromWorkspace(base);
			}

			@Override
			protected void tearDown() throws CoreException {
				if (original != null) {
					getWorkspace().setDescription(original);
				}
			}

			@Override
			protected void test() {
				cleanHistory();
			}

		}.run(getClass(), testName.getMethodName(), 5, 1);
	}

	@Test
	public void testHistoryCleanUp100x4() throws Exception {
		testHistoryCleanUp(100, 4);
	}

	@Test
	public void testHistoryCleanUp20x20() throws Exception {
		testHistoryCleanUp(20, 20);
	}

}
