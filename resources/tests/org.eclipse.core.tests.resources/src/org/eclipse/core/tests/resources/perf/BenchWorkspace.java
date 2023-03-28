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
 *******************************************************************************/
package org.eclipse.core.tests.resources.perf;

import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.harness.PerformanceTestRunner;
import org.eclipse.core.tests.resources.ResourceTest;

public class BenchWorkspace extends ResourceTest {
	private static final int FILES_PER_FOLDER = 20;
	private static final int NUM_FOLDERS = 400;//must be multiple of 10
	IProject project;

	/**
	 * Creates the given number of problem markers on each resource in the workspace.
	 * @param i
	 */
	private void addProblems(final int problemCount) {
		IWorkspaceRunnable runnable = monitor -> getWorkspace().getRoot().accept(resource -> {
			for (int i = 0; i < problemCount; i++) {
				IMarker marker = resource.createMarker(IMarker.PROBLEM);
				marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
			}
			return true;
		});
		try {
			getWorkspace().run(runnable, null);
		} catch (CoreException e) {
			fail("1.0", e);
		}
	}

	@Override
	public String[] defineHierarchy() {
		//define a hierarchy with NUM_FOLDERS folders, NUM_FILES files.
		String[] names = new String[NUM_FOLDERS * (FILES_PER_FOLDER + 1)];
		//create the folders
		final int MAX_DEPTH = 10;
		final int MAX_SPAN = NUM_FOLDERS / MAX_DEPTH;
		int i = 0;
		for (int depth = 0; depth < MAX_DEPTH; depth++) {
			for (int span = 0; span < MAX_SPAN; span++) {
				if (depth == 0) {
					names[i] = "TestProject/" + span + "/";
				} else {
					names[i] = names[i - MAX_SPAN] + span + "/";
				}
				i++;
			}
		}
		//create files for each folder
		for (int folder = 0; folder < NUM_FOLDERS; folder++) {
			for (int file = 0; file < FILES_PER_FOLDER; file++) {
				names[i++] = names[folder] + "file" + file;
			}
		}
		return names;
	}

	int findMaxProblemSeverity(IWorkspaceRoot root) {
		class ResourceVisitor implements IResourceVisitor {
			int maxSeverity = -1;

			@Override
			public boolean visit(IResource resource) throws CoreException {
				IMarker[] markers = resource.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_ZERO);
				for (IMarker marker : markers) {
					int severity = marker.getAttribute(IMarker.SEVERITY, -1);
					if (severity > maxSeverity) {
						maxSeverity = severity;
					}
				}
				return true;
			}
		}
		ResourceVisitor visitor = new ResourceVisitor();
		try {
			root.accept(visitor);
		} catch (CoreException e) {
			fail("4.99", e);
			return -1;
		}
		return visitor.maxSeverity;
	}

	int findMaxProblemSeverity2(IWorkspaceRoot root) {
		try {
			return root.findMaxProblemSeverity(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
		} catch (CoreException e) {
			fail("4.99", e);
			return -1;
		}
	}

	/**
	 * @see ResourceTest#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();

		IWorkspaceRunnable runnable = monitor -> {
			//create resources
			project = getWorkspace().getRoot().getProject("TestProject");
			project.create(null);
			project.open(null);
			IResource[] resources = buildResources(project, defineHierarchy());
			ensureExistsInWorkspace(resources, true);
		};
		try {
			getWorkspace().run(runnable, null);
		} catch (CoreException e) {
			fail("1.0", e);
		}
	}

	public void testCountResources() {
		final Workspace workspace = (Workspace) getWorkspace();
		final IWorkspaceRoot root = workspace.getRoot();
		new PerformanceTestRunner() {
			@Override
			protected void setUp() throws CoreException {
				super.setUp();
				waitForBackgroundActivity();
			}

			@Override
			protected void test() {
				workspace.countResources(root.getFullPath(), IResource.DEPTH_INFINITE, true);
			}
		}.run(this, 10, 100);
	}

	/**
	 * Waits until background activity settles down before running a performance test.
	 *
	 */
	public void waitForBackgroundActivity() {
		waitForRefresh();
		waitForBuild();
	}

	public void testCountResourcesDuringOperation() {
		final Workspace workspace = (Workspace) getWorkspace();
		IWorkspaceRunnable runnable = monitor -> {
			//touch all files
			workspace.getRoot().accept(resource -> {
				resource.touch(null);
				return true;
			});
			new PerformanceTestRunner() {
				@Override
				protected void test() {
					workspace.countResources(project.getFullPath(), IResource.DEPTH_INFINITE, true);
				}
			}.run(BenchWorkspace.this, 10, 10);
		};
		try {
			workspace.run(runnable, getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}
	}

	/**
	 * Tests computing max marker severity
	 */
	public void testFindMaxProblemSeverity() {
		addProblems(10);
		final Workspace workspace = (Workspace) getWorkspace();
		final IWorkspaceRoot root = workspace.getRoot();
		new PerformanceTestRunner() {
			@Override
			protected void test() {
				findMaxProblemSeverity2(root);
			}
		}.run(this, 10, 100);
	}
}
