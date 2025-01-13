/*******************************************************************************
 * Copyright (c) 2009, 2012 IBM Corporation and others.
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
package org.eclipse.core.tests.resources.session;

import static java.util.function.Predicate.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.harness.FileSystemHelper.getTempDir;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.PI_RESOURCES_TESTS;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInputStream;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;

import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.tests.harness.session.SessionTestExtension;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests for bug 266907
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class Bug_266907 {

	private static final String PROJECT_NAME = "Project";
	private static final String FILE_NAME = "File";
	private static final String MARKER_ATTRIBUTE_NAME = "AttributeName";
	private static final String MARKER_ATTRIBUTE = "Attribute";

	@RegisterExtension
	static SessionTestExtension sessionTestExtension = SessionTestExtension.forPlugin(PI_RESOURCES_TESTS)
			.withCustomization(SessionTestExtension.createCustomWorkspace()).create();

	@Test
	@Order(1)
	public void test1CreateProjectAndDeleteProjectFile() throws Exception {
		// Ensure that no asynchronous save job restores the .project file after
		// deleting it by suspending the JobManager for this workspace session
		Job.getJobManager().suspend();

		final IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject(PROJECT_NAME);
		project.create(createTestMonitor());
		project.open(createTestMonitor());

		IFile f = project.getFile(FILE_NAME);
		f.create(createInputStream("content"), true, createTestMonitor());

		IMarker marker = f.createMarker(IMarker.BOOKMARK);
		marker.setAttribute(MARKER_ATTRIBUTE_NAME, MARKER_ATTRIBUTE);

		// remember the location of .project to delete is at the end
		Path dotProject = project.getFile(".project").getLocation().toPath();

		workspace.save(true, createTestMonitor());

		// move .project to a temp location
		Path dotProjectCopy = getTempDir().append("dotProjectCopy").toPath();
		Files.copy(dotProject, dotProjectCopy);
		Files.delete(dotProject);
	}

	@Test
	@Order(2)
	public void test2RestoreWorkspaceFile() throws Exception {
		final IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject(PROJECT_NAME);

		// the project should be closed cause .project is removed
		assertThat(project).matches(not(IProject::isAccessible), "is not accessible");

		// recreate .project
		Path dotProject = project.getFile(".project").getLocation().toPath();
		Path dotProjectCopy = getTempDir().append("dotProjectCopy").toPath();

		Files.copy(dotProjectCopy, dotProject);
		Files.delete(dotProjectCopy);

		project.open(createTestMonitor());
		assertThat(project).matches(IProject::isAccessible, "is accessible");

		IFile file = project.getFile(FILE_NAME);
		IMarker[] markers = file.findMarkers(IMarker.BOOKMARK, false, IResource.DEPTH_ZERO);
		assertThat(markers).as("number of markers").hasSize(1);

		Object attribute = markers[0].getAttribute(MARKER_ATTRIBUTE_NAME);
		assertThat(attribute).as("name of marker").isEqualTo(MARKER_ATTRIBUTE);
	}

}
