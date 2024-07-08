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
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *******************************************************************************/
package org.eclipse.core.tests.internal.builders;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomContentsStream;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.setAutoBuilding;
import static org.eclipse.core.tests.resources.ResourceTestUtil.updateProjectDescription;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.TestPerformer;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * This class tests builds that span multiple projects.  Project builders
 * can specify what other projects they are interested in receiving deltas for,
 * and they should only be receiving deltas for exactly those projects.
 */
@ExtendWith(WorkspaceResetExtension.class)
public class MultiProjectBuildTest {
	//various resource handles
	private IProject project1;
	private IProject project2;
	private IProject project3;
	private IProject project4;
	private IFile file1;
	private IFile file2;
	private IFile file3;
	private IFile file4;

	/**
	 * Returns an array of interesting project combinations.
	 */
	private IProject[][] interestingProjects() {
		//mix things up, because requests from one run affect results in the next.
		return new IProject[][] {new IProject[] {}, new IProject[] {project3}, new IProject[] {project1}, new IProject[] {project1, project2, project3}, new IProject[] {project2}, new IProject[] {project3}, new IProject[] {project4}, new IProject[] {project1, project2}, new IProject[] {project1, project3}, new IProject[] {project3}, new IProject[] {project2, project3}, new IProject[] {project1, project2, project3}, new IProject[] {project1, project2, project4}, new IProject[] {project1}, new IProject[] {project1, project3, project4}, new IProject[] {project1, project2}, new IProject[] {project2, project3, project4}, new IProject[] {project3, project4}, new IProject[] {project1, project2, project3, project4},};
	}

	/**
	 * Modifies any files in the given projects, all in a single operation
	 */
	private void dirty(final IProject[] projects) throws CoreException {
		getWorkspace().run((IWorkspaceRunnable) monitor -> {
			for (IProject project : projects) {
				for (IResource member : project.members()) {
					if (member.getType() == IResource.FILE && !member.getName().equals(IProjectDescription.DESCRIPTION_FILE_NAME)) {
						((IFile) member).setContents(createRandomContentsStream(), true, true, null);
					}
				}
			}
			getWorkspace().build(IncrementalProjectBuilder.INCREMENTAL_BUILD, null);
		}, createTestMonitor());
	}

	/**
	 * Returns an array reversed.
	 */
	private IProject[][] reverse(IProject[][] input) {
		if (input == null) {
			return null;
		}
		int len = input.length;
		IProject[][] output = (IProject[][]) Array.newInstance(IProject[].class, len);
		for (int i = 0; i < len; i++) {
			output[len - i - 1] = input[i];
		}
		return output;
	}

	/*
	 * @see TestCase#setUp()
	 */
	@BeforeEach
	public void setUp() throws Exception {
		setAutoBuilding(true);
		IWorkspaceRoot root = getWorkspace().getRoot();
		project1 = root.getProject("Project1");
		project2 = root.getProject("Project2");
		project3 = root.getProject("Project3");
		project4 = root.getProject("Project4");
		file1 = project1.getFile("File1");
		file2 = project2.getFile("File2");
		file3 = project3.getFile("File3");
		file4 = project4.getFile("File4");
		IResource[] resources = {project1, project2, project3, project4, file1, file2, file3, file4};
		createInWorkspace(resources);
	}

	/**
	 * In this test, only project1 has a builder, but it is interested in deltas from the other projects.
	 * We vary the set of projects that are changed, and the set of projects we request deltas for.
	 */
	@Test
	public void testDeltas() throws Exception {
		//add builder and do an initial build to get the instance
		setAutoBuilding(false);
		updateProjectDescription(project1).addingCommand(DeltaVerifierBuilder.BUILDER_NAME)
				.withTestBuilderId("testbuild").apply();
		project1.build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());

		final DeltaVerifierBuilder builder = DeltaVerifierBuilder.getInstance();
		assertNotNull(builder);
		//always check deltas for all projects
		final IProject[] allProjects = new IProject[] {project1, project2, project3, project4};
		builder.checkDeltas(allProjects);

		//hold onto the set of requested projects here
		final IProject[][] previousRequest = new IProject[][] {new IProject[] {project1}};
		//hold onto projects that have been modified since the last time the builder was run.
		final HashSet<IProject> previouslyModified = new HashSet<>();
		new TestPerformer("testDeltas") {
			@Override
			public Object[] interestingOldState(Object[] args) throws Exception {
				return null;
			}

			@Override
			public Object invokeMethod(Object[] args, int count) throws Exception {
				//set requests for next build
				IProject[] requested = (IProject[]) args[0];
				IProject[] toModify = (IProject[]) args[1];
				builder.reset();
				builder.requestDeltas(requested);
				//do the build
				dirty(toModify);
				Object result = previousRequest[0];
				if (builder.wasBuilt()) {
					//if the builder ran, update previous request
					previousRequest[0] = requested;
					previouslyModified.clear();
				} else {
					previouslyModified.addAll(Arrays.asList(toModify));
				}
				return result;
			}

			@Override
			public boolean shouldFail(Object[] args, int count) {
				return false;
			}

			@Override
			public boolean wasSuccess(Object[] args, Object result, Object[] oldState) throws Exception {
				HashSet<IProject> requested = new HashSet<>(Arrays.asList((IProject[]) result));
				HashSet<IProject> modified = new HashSet<>(Arrays.asList((IProject[]) args[1]));
				modified.addAll(previouslyModified);
				HashSet<IProject> obtained = new HashSet<>();
				if (!builder.getReceivedDeltas().isEmpty()) {
					obtained.addAll(builder.getReceivedDeltas());
				}
				ArrayList<IProject> emptyDeltas = builder.getEmptyDeltas();

				//the builder's project is implicitly requested
				requested.add(builder.getProject());

				for (IProject project : allProjects) {
					boolean wasObtained = obtained.contains(project);
					boolean wasRequested = requested.contains(project);
					boolean wasModified = modified.contains(project);
					boolean wasEmpty = emptyDeltas.contains(project);
					if (wasObtained) {
						//every delta we obtained should have been requested and (modified or empty)
						if (!wasRequested || !(wasModified || wasEmpty)) {
							return false;
						}
					} else {
						//if delta was not obtained, then must be unchanged or not requested
						if (wasRequested && wasModified) {
							return false;
						}
					}
				}
				return true;
			}
		}.performTest(new Object[][] {interestingProjects(), reverse(interestingProjects())});
	}

	/**
	 * Tests a builder that requests deltas for closed and missing projects.
	 */
	@Test
	public void testRequestMissingProject() throws CoreException {
		//add builder and do an initial build to get the instance
		updateProjectDescription(project1).addingCommand(DeltaVerifierBuilder.BUILDER_NAME)
				.withTestBuilderId("testbuild").apply();
		project1.build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());

		final DeltaVerifierBuilder builder = DeltaVerifierBuilder.getInstance();
		assertNotNull(builder);
		//always check deltas for all projects
		final IProject[] allProjects = new IProject[] {project1, project2, project3, project4};
		project2.close(createTestMonitor());
		project3.delete(IResource.ALWAYS_DELETE_PROJECT_CONTENT, createTestMonitor());

		builder.checkDeltas(allProjects);

		//modify a file in project1 to force an autobuild
		file1.setContents(createRandomContentsStream(), IResource.NONE, createTestMonitor());
	}

	/**
	 * Test for Bug #5102.  Never reproduced but interesting little test, worth keeping around
	 */
	@Test
	public void testPR() throws Exception {
		//create a project with a RefreshLocalJavaFileBuilder and a SortBuilder on the classpath
		IProject project = getWorkspace().getRoot().getProject("P1");
		project.create(null);
		project.open(null);
		updateProjectDescription(project).addingCommand(RefreshLocalJavaFileBuilder.BUILDER_NAME)
				.andCommand(SortBuilder.BUILDER_NAME).apply();

		//do a full build
		project.build(IncrementalProjectBuilder.FULL_BUILD, null);

		//do an incremental build by creating a file
		IFile file = project.getFile("Foo");
		file.create(createRandomContentsStream(), true, createTestMonitor());
	}

}
