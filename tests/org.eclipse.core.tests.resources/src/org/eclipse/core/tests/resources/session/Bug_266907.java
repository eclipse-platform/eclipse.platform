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

import java.io.*;
import junit.framework.Test;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
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

	public void test1stSession() {
		final IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject(PROJECT_NAME);
		try {
			project.create(getMonitor());
			project.open(getMonitor());
		} catch (CoreException e1) {
			fail("1.0", e1);
		}

		IFile f = project.getFile(FILE_NAME);
		try {
			f.create(getContents("content"), true, getMonitor());
		} catch (CoreException e1) {
			fail("2.0", e1);
		}

		try {
			IMarker marker = f.createMarker(IMarker.BOOKMARK);
			marker.setAttribute(MARKER_ATTRIBUTE_NAME, MARKER_ATTRIBUTE);
		} catch (CoreException e2) {
			fail("3.0", e2);
		}

		// remember the location of .project to delete is at the end
		File dotProject = project.getFile(".project").getLocation().toFile();

		try {
			workspace.save(true, getMonitor());
		} catch (CoreException e) {
			fail("4.0", e);
		}

		// move .project to a temp location
		File dotProjectCopy = getTempDir().append("dotProjectCopy").toFile();
		try {
			dotProjectCopy.createNewFile();
			transferStreams(new FileInputStream(dotProject), new FileOutputStream(dotProjectCopy), null);
			dotProject.delete();
		} catch (FileNotFoundException e) {
			fail("5.0", e);
		} catch (IOException e) {
			fail("5.1", e);
		}
	}

	public void test2ndSession() {
		final IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject(PROJECT_NAME);

		// the project should be closed cause .project is removed
		assertTrue("1.0", !project.isAccessible());

		// recreate .project
		File dotProject = project.getFile(".project").getLocation().toFile();
		File dotProjectCopy = getTempDir().append("dotProjectCopy").toFile();
		try {
			dotProject.createNewFile();
			transferStreams(new FileInputStream(dotProjectCopy), new FileOutputStream(dotProject), null);
			dotProjectCopy.delete();
		} catch (IOException e1) {
			fail("2.0", e1);
		}

		try {
			project.open(getMonitor());
		} catch (CoreException e) {
			fail("3.0", e);
		}

		assertTrue("4.0", project.isAccessible());

		IFile file = project.getFile(FILE_NAME);
		IMarker[] markers = null;
		try {
			markers = file.findMarkers(IMarker.BOOKMARK, false, IResource.DEPTH_ZERO);
		} catch (CoreException e) {
			fail("5.0", e);
		}

		assertNotNull("6.0", markers);
		assertEquals("6.1", markers.length, 1);

		Object attribute = null;
		try {
			attribute = markers[0].getAttribute(MARKER_ATTRIBUTE_NAME);
		} catch (CoreException e) {
			fail("7.0", e);
		}
		assertEquals("8.0", attribute, MARKER_ATTRIBUTE);
	}
}
