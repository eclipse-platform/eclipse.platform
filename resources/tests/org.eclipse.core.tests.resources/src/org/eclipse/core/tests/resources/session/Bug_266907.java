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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import junit.framework.Test;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.tests.resources.WorkspaceSessionTest;
import org.eclipse.core.tests.session.WorkspaceSessionTestSuite;

/**
 * Tests for bug 266907
 */
public class Bug_266907 extends WorkspaceSessionTest {

	private static final String PROJECT_NAME = "Project";
	private static final String FILE_NAME = "File";
	private static final String MARKER_ATTRIBUTE_NAME = "AttributeName";
	private static final String MARKER_ATTRIBUTE = "Attribute";

	public static Test suite() {
		return new WorkspaceSessionTestSuite(PI_RESOURCES_TESTS, Bug_266907.class);
	}

	public void test1CreateProjectAndDeleteProjectFile() throws Exception {
		final IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject(PROJECT_NAME);
		project.create(getMonitor());
		project.open(getMonitor());

		IFile f = project.getFile(FILE_NAME);
		f.create(getContents("content"), true, getMonitor());

		IMarker marker = f.createMarker(IMarker.BOOKMARK);
		marker.setAttribute(MARKER_ATTRIBUTE_NAME, MARKER_ATTRIBUTE);

		// remember the location of .project to delete is at the end
		File dotProject = project.getFile(".project").getLocation().toFile();

		workspace.save(true, getMonitor());

		// move .project to a temp location
		File dotProjectCopy = getTempDir().append("dotProjectCopy").toFile();
		dotProjectCopy.createNewFile();
		transferStreams(new FileInputStream(dotProject), new FileOutputStream(dotProjectCopy), null);
		dotProject.delete();
	}

	public void test2RestoreWorkspaceFile() throws Exception {
		final IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject(PROJECT_NAME);

		// the project should be closed cause .project is removed
		assertThat("project should not be accessible", project.isAccessible(), is(false));

		// recreate .project
		File dotProject = project.getFile(".project").getLocation().toFile();
		File dotProjectCopy = getTempDir().append("dotProjectCopy").toFile();

		dotProject.createNewFile();
		transferStreams(new FileInputStream(dotProjectCopy), new FileOutputStream(dotProject), null);
		dotProjectCopy.delete();

		project.open(getMonitor());
		assertThat("project should be accessible", project.isAccessible(), is(true));

		IFile file = project.getFile(FILE_NAME);
		IMarker[] markers = file.findMarkers(IMarker.BOOKMARK, false, IResource.DEPTH_ZERO);
		assertThat("unexpected number of markers in test project", markers.length, is(1));

		Object attribute = markers[0].getAttribute(MARKER_ATTRIBUTE_NAME);
		assertThat("unexpected name of marker", attribute, is(MARKER_ATTRIBUTE));
	}

}
