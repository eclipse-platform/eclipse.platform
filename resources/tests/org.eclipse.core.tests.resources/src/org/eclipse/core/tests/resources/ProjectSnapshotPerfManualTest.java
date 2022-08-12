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
 *     Francis Lynch (Wind River) - [301563] adapted from RefreshLocalPerformanceTest
 *******************************************************************************/
package org.eclipse.core.tests.resources;

import java.io.File;
import java.net.URI;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.tests.harness.PerformanceTestRunner;

/**
 * Measure speed of a project "Import with Snapshot" operation compared
 * to a normal "Project Import" operation, as per bug 301563.
 *
 * This test is meant to be run manually, and not integrated into the
 * automated test suite. Requires large project data (thousands of folders)
 * to be available on a slow file system (bigSiteLocation). Modify
 * bigSiteLocation to suit your needs, then run-as &gt; JUnit Plug-in Test.
 */
public class ProjectSnapshotPerfManualTest extends ResourceTest {
	/** big site default volume (windows) */
	public static final String bigSiteDevice = "c:";

	/** big site initial location. Modify to suit your needs */
	public static final IPath bigSiteLocation = new Path(bigSiteDevice, "/test");

	/** settings directory name */
	private static final String DIR_NAME = ".settings";

	/** location of refresh snapshot file */
	private static final String REFRESH_SNAPSHOT_FILE_LOCATION = ".settings/resource-index.zip";

	public ProjectSnapshotPerfManualTest() {
		super();
	}

	public ProjectSnapshotPerfManualTest(String name) {
		super(name);
	}

	protected int countChildren(File root) {
		String[] children = root.list();
		if (children == null) {
			return 0;
		}
		int result = 0;
		for (String element : children) {
			File child = new File(root, element);
			if (child.isDirectory()) {
				result += countChildren(child);
			}
			result++;
		}
		return result;
	}

	/**
	 * Open a project and export a refresh snapshot. Re-open the project using
	 * the snapshot and compare the times for opening with and without the
	 * snapshot.
	 */
	public void testSnapshotImportPerformance() throws Exception {
		// test if the test can be done in this machine
		if (!bigSiteLocation.toFile().isDirectory()) {
			return;
		}

		// create common objects
		final IProject project = getWorkspace().getRoot().getProject("MyTestProject");
		IProjectDescription description = getWorkspace().newProjectDescription(project.getName());
		description.setLocation(bigSiteLocation);

		// report the number of files to be refreshed
		int numberOfFiles = countChildren(bigSiteLocation.toFile());
		System.out.println("Number of local resources: " + numberOfFiles);

		// create the project from the location
		project.create(description, null);

		// open the project, timing the initial refresh
		new PerformanceTestRunner() {
			@Override
			protected void test() {
				try {
					project.open(null);
				} catch (CoreException e) {
					fail("Original open", e);
				}
			}
		}.run(new ProjectSnapshotPerfManualTest("Original open"), 1, 1);

		// dump the snapshot refresh info
		ensureExistsInWorkspace(project.getFolder(DIR_NAME), true);
		IPath projPath = project.getLocation();
		projPath = projPath.append(REFRESH_SNAPSHOT_FILE_LOCATION);
		final URI snapshotLocation = org.eclipse.core.filesystem.URIUtil.toURI(projPath);
		project.saveSnapshot(IProject.SNAPSHOT_TREE, snapshotLocation, null);

		// close and delete project but leave contents
		project.close(null);
		project.delete(false, false, null);

		// open the project and import refresh snapshot
		project.create(description, null);
		new PerformanceTestRunner() {
			@Override
			protected void test() {
				try {
					project.loadSnapshot(IProject.SNAPSHOT_TREE, snapshotLocation, null);
					project.open(IResource.NONE, null);
				} catch (CoreException e) {
					fail("Snapshot open", e);
				}
			}
		}.run(new ProjectSnapshotPerfManualTest("Snapshot open"), 1, 1);

		// now refresh the project, verifying zero resource delta
		// (except for the creation of .settings/resource-index.zip)
		final ResourceDeltaVerifier[] verifier = new ResourceDeltaVerifier[1];
		new PerformanceTestRunner() {
			@Override
			protected void test() {
				try {
					verifier[0] = new ResourceDeltaVerifier();
					ResourcesPlugin.getWorkspace().addResourceChangeListener(verifier[0]);
					verifier[0].reset();
					IFolder settings = project.getFolder(DIR_NAME);
					IFile snapshot = project.getFile(REFRESH_SNAPSHOT_FILE_LOCATION);
					verifier[0].addExpectedChange(settings, IResourceDelta.CHANGED, 0);
					verifier[0].addExpectedChange(snapshot, IResourceDelta.ADDED, 0);
					project.refreshLocal(IResource.DEPTH_INFINITE, null);
				} catch (CoreException e) {
					fail("Forced refresh only", e);
				}
			}
		}.run(new ProjectSnapshotPerfManualTest("Forced refresh only"), 1, 1);
		verifier[0].verifyDelta(null);
		assertTrue("1.0 " + verifier[0].getMessage(), verifier[0].isDeltaValid());

		// close and delete project but leave contents
		project.close(null);
		project.delete(false, false, null);
		IPath snapshotFile = bigSiteLocation.append(REFRESH_SNAPSHOT_FILE_LOCATION);
		snapshotFile.toFile().delete();

		// open the project again with standard refresh
		project.create(description, null);
		new PerformanceTestRunner() {
			@Override
			protected void test() {
				try {
					project.open(null);
				} catch (CoreException e) {
					fail("Second refresh open", e);
				}
			}
		}.run(new ProjectSnapshotPerfManualTest("Second refresh open"), 1, 1);

		// delete project to avoid getting content deleted in tearDown()
		project.close(null);
		project.delete(false, false, null);
	}
}
