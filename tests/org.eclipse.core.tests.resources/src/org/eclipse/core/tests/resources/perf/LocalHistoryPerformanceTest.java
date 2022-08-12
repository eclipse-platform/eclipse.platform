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

import org.eclipse.core.internal.localstore.IHistoryStore;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.tests.harness.PerformanceTestRunner;
import org.eclipse.core.tests.internal.localstore.HistoryStoreTest;
import org.eclipse.core.tests.resources.ResourceTest;

/**
 * Contains a set of use case-oriented performance tests for the local history.
 *
 * @since 3.1
 */
public class LocalHistoryPerformanceTest extends ResourceTest {

	private IWorkspaceDescription original;

	void cleanHistory() {
		((Workspace) getWorkspace()).getFileSystemManager().getHistoryStore().clean(getMonitor());
	}

	/**
	 * Creates a tree of resources containing history.
	 */
	void createTree(IFolder base, final int filesPerFolder, final int statesPerFile) {
		final IFolder[] folders = new IFolder[5];
		folders[0] = base.getFolder("folder1");
		folders[1] = base.getFolder("folder2");
		folders[2] = folders[0].getFolder("folder3");
		folders[3] = folders[2].getFolder("folder4");
		folders[4] = folders[3].getFolder("folder5");
		final IWorkspace workspace = getWorkspace();
		try {
			workspace.run((IWorkspaceRunnable) monitor -> {
				ensureExistsInWorkspace(folders, true);
				for (IFolder folder : folders) {
					for (int j = 0; j < filesPerFolder; j++) {
						IFile file = folder.getFile("file" + j);
						ensureExistsInWorkspace(file, getRandomContents());
						try {
							for (int k = 0; k < statesPerFile; k++) {
								file.setContents(getRandomContents(), IResource.KEEP_HISTORY, getMonitor());
							}
						} catch (CoreException ce) {
							fail("0.5", ce);
						}
					}
				}
			}, workspace.getRuleFactory().modifyRule(workspace.getRoot()), IWorkspace.AVOID_UPDATE, getMonitor());
		} catch (CoreException e) {
			fail("#createTree at : " + base.getFullPath(), e);
		}
	}

	IWorkspaceDescription setMaxFileStates(String failureMessage, int maxFileStates) {
		IWorkspaceDescription currentDescription = getWorkspace().getDescription();
		IWorkspaceDescription testDescription = getWorkspace().getDescription();
		testDescription.setMaxFileStates(maxFileStates);
		try {
			getWorkspace().setDescription(testDescription);
		} catch (CoreException e) {
			fail(failureMessage, e);
		}
		return currentDescription;
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		original = getWorkspace().getDescription();
	}

	@Override
	protected void tearDown() throws Exception {
		getWorkspace().setDescription(original);
		super.tearDown();
		HistoryStoreTest.wipeHistoryStore(getMonitor());
	}

	public void testAddState() {
		setMaxFileStates("0.01", 100);
		final IFile file = getWorkspace().getRoot().getProject("proj1").getFile("file.txt");
		new PerformanceTestRunner() {

			@Override
			protected void setUp() {
				ensureExistsInWorkspace(file, getRandomContents());
			}

			@Override
			protected void tearDown() {
				try {
					file.clearHistory(getMonitor());
					file.delete(IResource.FORCE, getMonitor());
				} catch (CoreException e) {
					fail("1.0", e);
				}
			}

			@Override
			protected void test() {
				try {
					file.setContents(getRandomContents(), IResource.KEEP_HISTORY, getMonitor());
				} catch (CoreException e) {
					fail("", e);
				}
			}
		}.run(LocalHistoryPerformanceTest.this, 10, 30);
	}

	public void testBug28603() {
		final IProject project = getWorkspace().getRoot().getProject("myproject");
		final IFolder folder1 = project.getFolder("myfolder1");
		final IFolder folder2 = project.getFolder("myfolder2");
		final IFile file1 = folder1.getFile("myfile.txt");
		final IFile file2 = folder2.getFile(file1.getName());

		new PerformanceTestRunner() {

			@Override
			protected void setUp() {
				ensureExistsInWorkspace(new IResource[] {project, folder1, folder2}, true);
				try {
					file1.create(getRandomContents(), IResource.FORCE, getMonitor());
					file1.setContents(getRandomContents(), IResource.FORCE | IResource.KEEP_HISTORY, getMonitor());
					file1.setContents(getRandomContents(), IResource.FORCE | IResource.KEEP_HISTORY, getMonitor());
					file1.setContents(getRandomContents(), IResource.FORCE | IResource.KEEP_HISTORY, getMonitor());
				} catch (CoreException e) {
					fail("0.0", e);
				}
			}

			@Override
			protected void tearDown() {
				try {
					ensureDoesNotExistInWorkspace(getWorkspace().getRoot());
					IHistoryStore store = ((Workspace) getWorkspace()).getFileSystemManager().getHistoryStore();
					// Remove all the entries from the history store index.  Note that
					// this does not cause the history store states to be removed.
					store.remove(Path.ROOT, getMonitor());
					// Now make sure all the states are really removed.
					store.removeGarbage();
				} catch (Exception e) {
					fail("2.0", e);
				}
			}

			@Override
			protected void test() {
				try {
					file1.move(file2.getFullPath(), true, true, getMonitor());
					file2.move(file1.getFullPath(), true, true, getMonitor());
				} catch (CoreException e) {
					fail("1.0", e);
				}
			}
		}.run(this, 10, 5);
	}

	private void testClearHistory(final int filesPerFolder, final int statesPerFile) {
		IProject project = getWorkspace().getRoot().getProject("proj1");
		final IFolder base = project.getFolder("base");
		ensureDoesNotExistInWorkspace(base);
		new PerformanceTestRunner() {
			private IWorkspaceDescription original;

			@Override
			protected void setUp() {
				original = setMaxFileStates("0.1", 1);
				// make sure we start with no garbage
				cleanHistory();
				// create our own garbage
				createTree(base, filesPerFolder, statesPerFile);
				ensureDoesNotExistInWorkspace(base);
			}

			@Override
			protected void tearDown() throws CoreException {
				if (original != null) {
					getWorkspace().setDescription(original);
				}
			}

			@Override
			protected void test() {
				try {
					base.clearHistory(getMonitor());
				} catch (CoreException e) {
					fail("", e);
				}
			}
		}.run(this, 4, 3);
	}

	public void testClearHistory100x4() {
		testClearHistory(100, 4);
	}

	public void testClearHistory20x20() {
		testClearHistory(20, 20);
	}

	public void testClearHistory4x100() {
		testClearHistory(4, 100);
	}

	private void testCopyHistory(int filesPerFolder, int statesPerFile) {
		IProject project = getWorkspace().getRoot().getProject("proj1");
		IFolder base = project.getFolder("base");
		createTree(base, filesPerFolder, statesPerFile);
		// need a final reference so the inner class can see it
		final IProject[] tmpProject = new IProject[] {project};
		new PerformanceTestRunner() {
			@Override
			protected void test() {
				try {
					String newProjectName = getUniqueString();
					IProject newProject = getWorkspace().getRoot().getProject(newProjectName);
					tmpProject[0].copy(newProject.getFullPath(), true, getMonitor());
					tmpProject[0] = newProject;
				} catch (CoreException e) {
					fail("", e);
				}
			}
		}.run(this, 10, 1);
	}

	public void testCopyHistory100x4() {
		testCopyHistory(100, 4);
	}

	public void testCopyHistory20x20() {
		testCopyHistory(20, 20);
	}

	public void testCopyHistory4x100() {
		testCopyHistory(4, 100);
	}

	private void testGetDeletedMembers(int filesPerFolder, int statesPerFile) {
		IProject project = getWorkspace().getRoot().getProject("proj1");
		IFolder base = project.getFolder("base");
		createTree(base, filesPerFolder, statesPerFile);
		ensureDoesNotExistInWorkspace(base);
		// need a final reference so the inner class can see it
		final IProject tmpProject = project;
		new PerformanceTestRunner() {
			@Override
			protected void test() {
				try {
					tmpProject.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, getMonitor());
				} catch (CoreException e) {
					fail("", e);
				}
			}
		}.run(this, 2, 5);
	}

	public void testGetDeletedMembers100x4() {
		testGetDeletedMembers(100, 4);
	}

	public void testGetDeletedMembers20x20() {
		testGetDeletedMembers(20, 20);
	}

	public void testGetDeletedMembers4x100() {
		testGetDeletedMembers(4, 100);
	}

	public void testGetHistory() {
		IProject project = getWorkspace().getRoot().getProject("proj1");
		final IFile file = project.getFile("file.txt");
		ensureExistsInWorkspace(file, getRandomContents());
		try {
			for (int i = 0; i < 100; i++) {
				file.setContents(getRandomContents(), IResource.KEEP_HISTORY, getMonitor());
			}
		} catch (CoreException ce) {
			fail("0.5", ce);
		}
		new PerformanceTestRunner() {
			@Override
			protected void test() {
				try {
					file.getHistory(getMonitor());
				} catch (CoreException e) {
					fail("", e);
				}
			}
		}.run(this, 1, 150);
	}

	private void testHistoryCleanUp(final int filesPerFolder, final int statesPerFile) {
		IProject project = getWorkspace().getRoot().getProject("proj1");
		final IFolder base = project.getFolder("base");
		ensureDoesNotExistInWorkspace(base);
		new PerformanceTestRunner() {
			private IWorkspaceDescription original;

			@Override
			protected void setUp() {
				original = setMaxFileStates("0.1", 1);
				// make sure we start with no garbage
				cleanHistory();
				// create our own garbage
				createTree(base, filesPerFolder, statesPerFile);
				ensureDoesNotExistInWorkspace(base);
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

		}.run(this, 5, 1);
	}

	public void testHistoryCleanUp100x4() {
		testHistoryCleanUp(100, 4);
	}

	public void testHistoryCleanUp20x20() {
		testHistoryCleanUp(20, 20);
	}
}
