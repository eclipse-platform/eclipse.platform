/*******************************************************************************
 *  Copyright (c) 2000, 2017 IBM Corporation and others.
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
 *  Martin Oberhuber (Wind River) - [306573] Add tests for import from snapshot
 *******************************************************************************/
package org.eclipse.core.tests.resources.perf;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.Random;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.tests.harness.PerformanceTestRunner;
import org.eclipse.core.tests.resources.ResourceTest;

/**
 * Basic performance calculations for standard workspace operations.
 */
public class WorkspacePerformanceTest extends ResourceTest {
	private static final String chars = "abcdefghijklmnopqrstuvwxyz";
	static final int REPEATS = 5;
	private static final int TREE_WIDTH = 10;
	private static final int DEFAULT_TOTAL_RESOURCES = 10000;

	private final Random random = new Random();
	IFolder testFolder;
	IProject testProject;

	IFolder copyFolder() {
		IFolder destination = testProject.getFolder("CopyDestination");
		try {
			testFolder.copy(destination.getFullPath(), IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("Failed to copy project in performance test", e);
		}
		return destination;
	}

	/**
	 * Creates a project and fills it with contents
	 */
	void createAndPopulateProject(final int totalResources) {
		try {
			getWorkspace().run((IWorkspaceRunnable) monitor -> {
				testProject.create(getMonitor());
				testProject.open(getMonitor());
				createFolder(testFolder, totalResources);
			}, getMonitor());
		} catch (CoreException e) {
			fail("Failed to create project in performance test", e);
		}
	}

	private byte[] createBytes(int length) {
		byte[] bytes = new byte[length];
		random.nextBytes(bytes);
		return bytes;
	}

	/**
	 * Creates and returns a folder with lots of contents
	 */
	IFolder createFolder(IFolder topFolder, int totalResources) throws CoreException {
		topFolder.create(IResource.NONE, true, getMonitor());
		//tree depth is log of total resource count with the width as the log base
		int depth = (int) (Math.log(totalResources) / Math.log(TREE_WIDTH));
		recursiveCreateChildren(topFolder, depth - 1);
		return topFolder;
	}

	private String createString(int length) {
		StringBuilder buf = new StringBuilder(length);
		//fill the string with random characters up to the desired length
		for (int i = 0; i < length; i++) {
			buf.append(chars.charAt(random.nextInt(chars.length())));
		}
		return buf.toString();
	}

	/**
	 * Deletes the test project without deleting content, and then recreates
	 * the project without discovering content on disk.  This sets us up
	 * for benchmarking performance of refresh local.
	 */
	void deleteAndRecreateProject() throws CoreException {
		//delete without deleting contents
		testProject.delete(IResource.NEVER_DELETE_PROJECT_CONTENT, null);
		//recreate project but don't discover content
		testProject.create(null);
		testProject.open(IResource.NONE, null);
	}

	IFolder moveFolder() {
		IFolder destination = testFolder.getProject().getFolder("MoveDestination");
		try {
			testFolder.move(destination.getFullPath(), IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("Failed to move folder during performance test", e);
		}
		return destination;
	}

	/**
	 * Create children of the given folder, and recurse to the given depth
	 */
	private void recursiveCreateChildren(IFolder parentFolder, int depth) throws CoreException {
		//create TREE_WIDTH files
		for (int i = 0; i < TREE_WIDTH; i++) {
			IFile file = parentFolder.getFile(createString(10));
			file.create(new ByteArrayInputStream(createBytes(5000)), IResource.NONE, getMonitor());
		}
		if (depth <= 0) {
			return;
		}
		//create TREE_WIDTH folders
		for (int i = 0; i < TREE_WIDTH; i++) {
			IFolder folder = parentFolder.getFolder(createString(6));
			folder.create(IResource.NONE, true, getMonitor());
			recursiveCreateChildren(folder, depth - 1);
		}
	}

	@Override
	protected void setUp() throws Exception {
		testProject = getWorkspace().getRoot().getProject("Project");
		testFolder = testProject.getFolder("TopFolder");
	}

	/**
	 * Benchmark test of creating a project and populating it with folders and files.
	 */
	public void testCreateResources() {
		PerformanceTestRunner runner = new PerformanceTestRunner() {
			@Override
			protected void setUp() {
				waitForBackgroundActivity();
			}

			@Override
			protected void tearDown() throws CoreException {
				testProject.delete(IResource.FORCE, null);
			}

			@Override
			protected void test() {
				createAndPopulateProject(DEFAULT_TOTAL_RESOURCES);
			}
		};
		runner.run(this, REPEATS, 1);
	}

	public void testDeleteProject() {
		//create the project contents
		PerformanceTestRunner runner = new PerformanceTestRunner() {
			@Override
			protected void setUp() {
				createAndPopulateProject(DEFAULT_TOTAL_RESOURCES);
				waitForBackgroundActivity();
			}

			@Override
			protected void test() {
				try {
					testProject.delete(IResource.NONE, null);
				} catch (CoreException e) {
					fail("Failed to delete project during performance test", e);
				}
			}
		};
		runner.run(this, REPEATS, 1);
	}

	public void testFolderCopy() {
		//create the project contents
		new PerformanceTestRunner() {
			@Override
			protected void setUp() {
				createAndPopulateProject(DEFAULT_TOTAL_RESOURCES);
				waitForBackgroundActivity();
			}

			@Override
			protected void tearDown() throws CoreException {
				testProject.delete(IResource.FORCE, null);
			}

			@Override
			protected void test() {
				copyFolder();
			}
		}.run(this, REPEATS, 1);
	}

	public void testFolderMove() {
		//create the project contents
		new PerformanceTestRunner() {
			@Override
			protected void setUp() {
				createAndPopulateProject(DEFAULT_TOTAL_RESOURCES);
				waitForBackgroundActivity();
			}

			@Override
			protected void tearDown() throws CoreException {
				testProject.delete(IResource.FORCE, null);
			}

			@Override
			protected void test() {
				moveFolder();
			}
		}.run(this, REPEATS, 1);
	}

	public void testRefreshProject() {
		PerformanceTestRunner runner = new PerformanceTestRunner() {
			@Override
			protected void setUp() throws CoreException {
				createAndPopulateProject(50000);
				deleteAndRecreateProject();
				waitForBackgroundActivity();
			}

			@Override
			protected void tearDown() throws CoreException {
				testProject.delete(IResource.FORCE, null);
			}

			@Override
			protected void test() {
				try {
					testProject.refreshLocal(IResource.DEPTH_INFINITE, null);
				} catch (CoreException e) {
					fail("Failed to refresh during testRefreshProject", e);
				}
			}
		};
		runner.setFingerprintName("Refresh Project");
		runner.run(this, REPEATS, 1);
	}

	public void testCloseOpenProject() {
		// 8 minutes total test time, 400 msec test execution time (*3 inner loops)
		new PerformanceTestRunner() {
			@Override
			protected void setUp() {
				createAndPopulateProject(50000);
				waitForBackgroundActivity();
			}

			@Override
			protected void tearDown() throws CoreException {
				testProject.delete(IResource.FORCE, null);
			}

			@Override
			protected void test() {
				try {
					testProject.close(null);
					testProject.open(null);
				} catch (CoreException e) {
					fail("Failed to close/open during testCloseOpenProject", e);
				}
			}
		}.run(this, REPEATS, 3);
	}

	public void testLoadSnapshot() {
		// 2 minutes total test time, 528 msec test execution time
		IProject snapProject = getWorkspace().getRoot().getProject("SnapProject");
		ensureExistsInWorkspace(snapProject, true);
		final URI snapshotLocation = snapProject.getFile("snapshot.zip").getLocationURI();
		createAndPopulateProject(50000);
		waitForBackgroundActivity();
		try {
			testProject.saveSnapshot(IProject.SNAPSHOT_TREE, snapshotLocation, null);
			testProject.delete(IResource.FORCE, null);
		} catch (CoreException e) {
			fail("Failed to create snapshot during testLoadSnapshot");
		}
		waitForBackgroundActivity();
		new PerformanceTestRunner() {
			@Override
			protected void setUp() {
			}

			@Override
			protected void tearDown() throws CoreException {
				testProject.delete(IResource.FORCE, null);
			}

			@Override
			protected void test() {
				try {
					testProject.create(null);
					testProject.loadSnapshot(IProject.SNAPSHOT_TREE, snapshotLocation, null);
					testProject.open(null);
				} catch (CoreException e) {
					fail("Failed to load snapshot during testLoadSnapshot", e);
				}
			}
		}.run(this, REPEATS, 1);
	}

	/**
	 * Waits until background activity settles down before running a performance test.
	 *
	 */
	public void waitForBackgroundActivity() {
		waitForSnapshot();
		waitForRefresh();
		waitForBuild();
	}

	/**
	 * Wait for snapshot to complete by running and joining a workspace modification job.
	 * This job will get queued to run behind any scheduled snapshot job.
	 */
	private void waitForSnapshot() {
		Job wait = new Job("Wait") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				return Status.OK_STATUS;
			}
		};
		wait.setRule(getWorkspace().getRoot());
		wait.schedule();
		try {
			wait.join();
		} catch (InterruptedException e) {
			//ignore interruption
		}
	}
}
