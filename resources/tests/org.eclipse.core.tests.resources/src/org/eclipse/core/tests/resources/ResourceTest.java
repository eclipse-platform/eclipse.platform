/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *******************************************************************************/
package org.eclipse.core.tests.resources;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.waitForBuild;
import static org.eclipse.core.tests.resources.ResourceTestUtil.waitForRefresh;
import static org.junit.Assert.assertArrayEquals;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.tests.harness.CoreTest;
import org.eclipse.core.tests.harness.FileSystemHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

/**
 * Superclass for tests that use the Eclipse Platform workspace.
 */
public abstract class ResourceTest extends CoreTest {

	/**
	 * For retrieving the test name when executing test class with JUnit 4.
	 */
	@Rule
	public final TestName testName = new TestName();

	/**
	 * Set of FileStore instances that must be deleted when the
	 * test is complete
	 * @see #getTempStore
	 */
	private final Set<IFileStore> storesToDelete = new HashSet<>();

	private IWorkspaceDescription storedWorkspaceDescription;

	private final void storeWorkspaceDescription() {
		this.storedWorkspaceDescription = getWorkspace().getDescription();
	}

	private final void restoreWorkspaceDescription() throws CoreException {
		if (storedWorkspaceDescription != null) {
			getWorkspace().setDescription(storedWorkspaceDescription);
		}
		storedWorkspaceDescription = null;
	}

	/**
	 * Need a zero argument constructor to satisfy the test harness.
	 * This constructor should not do any real work nor should it be
	 * called by user code.
	 */
	public ResourceTest() {
		super(null);
	}

	/**
	 * Creates a new ResourceTest
	 *
	 * @param name
	 *            name of the TestCase
	 */
	public ResourceTest(String name) {
		super(name);
	}

	@Override
	public String getName() {
		String name = super.getName();
		// Ensure that in case this test class is executed with JUnit 4 the test name
		// will not be null but retrieved via a TestName rule.
		if (name == null) {
			name = testName.getMethodName();
		}
		return name;
	}

	/**
	 * Bridge method to be able to run subclasses with JUnit4 as well as with
	 * JUnit3.
	 *
	 * @throws Exception
	 *             comes from {@link #setUp()}
	 */
	@Before
	public final void before() throws Exception {
		setUp();
	}

	/**
	 * Bridge method to be able to run subclasses with JUnit4 as well as with
	 * JUnit3.
	 *
	 * @throws Exception
	 *             comes from {@link #tearDown()}
	 */
	@After
	public final void after() throws Exception {
		tearDown();
	}

	private void cleanup() throws CoreException {
		// Wait for any build job that may still be executed
		waitForBuild();
		final IFileStore[] toDelete = storesToDelete.toArray(new IFileStore[0]);
		storesToDelete.clear();
		getWorkspace().run((IWorkspaceRunnable) monitor -> {
			getWorkspace().getRoot().delete(true, true, createTestMonitor());
			//clear stores in workspace runnable to avoid interaction with resource jobs
			for (IFileStore store : toDelete) {
				store.delete(EFS.NONE, null);
			}
		}, null);
		getWorkspace().save(true, null);
		// don't leak builder jobs, since they may affect subsequent tests
		waitForBuild();
		assertWorkspaceFolderEmpty();
	}

	private void assertWorkspaceFolderEmpty() {
		final String metadataDirectoryName = ".metadata";
		File workspaceLocation = getWorkspace().getRoot().getLocation().toFile();
		File[] remainingFilesInWorkspace = workspaceLocation
				.listFiles(file -> !file.getName().equals(metadataDirectoryName));
		assertArrayEquals("There are unexpected contents in the workspace folder", new File[0],
				remainingFilesInWorkspace);
	}

	/**
	 * Returns a FileStore instance backed by storage in a temporary location.
	 * The returned store will not exist, but will belong to an existing parent.
	 * The tearDown method in this class will ensure the location is deleted after
	 * the test is completed.
	 */
	protected IFileStore getTempStore() {
		IFileStore store = EFS.getLocalFileSystem().getStore(FileSystemHelper.getRandomLocation(getTempDir()));
		deleteOnTearDown(store);
		return store;
	}

	/**
	 * Ensures that the file system location associated with the corresponding path
	 * is deleted during test tear down.
	 */
	protected void deleteOnTearDown(IPath path) {
		storesToDelete.add(EFS.getLocalFileSystem().getStore(path));
	}

	/**
	 * Ensures that the given store is deleted during test tear down.
	 */
	protected void deleteOnTearDown(IFileStore store) {
		storesToDelete.add(store);

	}

	/**
	 * The environment should be set-up in the main method.
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		// Wait for any pending refresh operation, in particular from startup
		waitForRefresh();
		TestUtil.log(IStatus.INFO, getName(), "setUp");
		FreezeMonitor.expectCompletionInAMinute();
		assertNotNull("Workspace was not setup", getWorkspace());
		storeWorkspaceDescription();
	}

	@Override
	protected void tearDown() throws Exception {
		boolean wasSuspended = resumeJobManagerIfNecessary();
		TestUtil.log(IStatus.INFO, getName(), "tearDown");
		// Ensure everything is in a clean state for next one.
		// Session tests should overwrite it.
		restoreWorkspaceDescription();
		cleanup();
		super.tearDown();
		FreezeMonitor.done();
		assertFalse("This test stopped the JobManager, which could have affected other tests.", //
				wasSuspended);
	}

	private boolean resumeJobManagerIfNecessary() {
		if (Job.getJobManager().isSuspended()) {
			Job.getJobManager().resume();
			return true;
		}

		return false;
	}

}
