/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
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
package org.eclipse.core.tests.resources.perf;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;

import java.util.Map;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.harness.PerformanceTestRunner;
import org.eclipse.core.tests.internal.builders.SortBuilder;
import org.eclipse.core.tests.internal.builders.TestBuilder;
import org.junit.Before;

/**
 * Automated performance tests for builders.
 */
public class BuilderPerformanceTest extends WorkspacePerformanceTest {
	private static final int PROJECT_COUNT = 100;
	private static final int REPEAT = 20;

	IProject[] otherProjects;

	/**
	 * Creates a project and fills it with contents
	 */
	void createAndPopulateProject(final IProject project, final IFolder folder, final int totalResources)
			throws CoreException {
		getWorkspace().run((IWorkspaceRunnable) monitor -> {
			IProjectDescription desc = project.getWorkspace().newProjectDescription(project.getName());
			desc.setBuildSpec(new ICommand[] { createCommand(desc, "Builder1"), createCommand(desc, "Builder2"),
					createCommand(desc, "Builder3"), createCommand(desc, "Builder4"),
					createCommand(desc, "Builder5") });
			project.create(desc, createTestMonitor());
			project.open(createTestMonitor());
			createFolder(folder, totalResources);
		}, createTestMonitor());
	}

	/**
	 * Creates and returns a new command with the SortBuilder, and the TestBuilder.BUILD_ID
	 * parameter set to the given value.
	 */
	protected ICommand createCommand(IProjectDescription description, String buildID) {
		return createCommand(description, SortBuilder.BUILDER_NAME, buildID);
	}

	/**
	 * Creates and returns a new command with the given builder name, and the TestBuilder.BUILD_ID
	 * parameter set to the given value.
	 */
	protected ICommand createCommand(IProjectDescription description, String builderName, String buildID) {
		ICommand command = description.newCommand();
		Map<String, String> args = command.getArguments();
		args.put(TestBuilder.BUILD_ID, buildID);
		command.setBuilderName(builderName);
		command.setArguments(args);
		return command;
	}

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		otherProjects = new IProject[PROJECT_COUNT];
		for (int i = 0; i < otherProjects.length; i++) {
			otherProjects[i] = getWorkspace().getRoot().getProject("Project " + i);
			IFolder folder = otherProjects[i].getFolder("Folder");
			createAndPopulateProject(otherProjects[i], folder, 100);
		}
	}

	/**
	 * Tests performing manual project-level increment builds when autobuild is on.
	 * See bug 261225 for details.
	 */
	public void testManualBuildWithAutobuildOn() throws Exception {
		PerformanceTestRunner runner = new PerformanceTestRunner() {
			IProject[] projects;

			@Override
			protected void setUp() {
				waitForBackgroundActivity();
				projects = getWorkspace().computeProjectOrder(getWorkspace().getRoot().getProjects()).projects;
			}

			@Override
			protected void tearDown() {
			}

			@Override
			protected void test() throws CoreException {
				for (int repeats = 0; repeats < REPEAT; repeats++) {
					for (IProject project : projects) {
						project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
					}
				}
			}
		};
		//this test simulates a manual build before launch with autobuild enabled
		runner.setFingerprintName("Build workspace before launch");
		runner.run(getClass(), testName.getMethodName(), REPEATS, 1);
	}
}
