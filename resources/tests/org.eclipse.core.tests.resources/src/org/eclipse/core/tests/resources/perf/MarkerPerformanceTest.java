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
 *******************************************************************************/
package org.eclipse.core.tests.resources.perf;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomContentsStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.tests.harness.PerformanceTestRunner;
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

public class MarkerPerformanceTest {

	@Rule
	public TestName testName = new TestName();

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	IProject project;
	IFile file;
	IMarker[] markers;
	final int NUM_MARKERS = 5000;
	final int REPEAT = 100;

	@Test
	public void testSetAttributes1() throws Exception {
		//benchmark setting many attributes in a single operation
		final IWorkspaceRunnable runnable = monitor -> {
			//set all attributes for each marker
			for (int i = 0; i < NUM_MARKERS; i++) {
				for (int j = 0; j < REPEAT; j++) {
					markers[i].setAttribute("attrib", "hello");
				}
			}
		};
		PerformanceTestRunner runner = new PerformanceTestRunner() {
			@Override
			protected void test() throws CoreException {
				getWorkspace().run(runnable, null);
			}
		};
		runner.setFingerprintName("Set marker attributes");
		runner.run(getClass(), testName.getMethodName(), 1, 1);
	}

	@Test
	public void testSetAttributes2() throws Exception {
		//benchmark setting many attributes in a single operation
		final IWorkspaceRunnable runnable = monitor -> {
			//set one attribute per marker, repeat for all attributes
			for (int j = 0; j < REPEAT; j++) {
				for (int i = 0; i < NUM_MARKERS; i++) {
					markers[i].setAttribute("attrib", "hello");
				}
			}
		};
		new PerformanceTestRunner() {
			@Override
			protected void test() throws CoreException {
				getWorkspace().run(runnable, null);
			}
		}.run(getClass(), testName.getMethodName(), 1, 1);
	}

	@Before
	public void setUp() throws Exception {
		final IMarker[] createdMarkers = new IMarker[NUM_MARKERS];
		IWorkspaceRunnable runnable = monitor -> {
			//create resources
			project = getWorkspace().getRoot().getProject("TestProject");
			project.create(null);
			project.open(null);
			file = project.getFile(IPath.ROOT.append("file.txt"));
			file.create(createRandomContentsStream(), true, null);
			//create markers
			for (int i = 0; i < NUM_MARKERS; i++) {
				createdMarkers[i] = file.createMarker(IMarker.BOOKMARK);
			}
		};

		getWorkspace().run(runnable, null);
		markers = createdMarkers;
	}

}
