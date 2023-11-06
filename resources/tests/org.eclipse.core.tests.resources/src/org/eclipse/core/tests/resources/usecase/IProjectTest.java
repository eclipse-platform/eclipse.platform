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
 *     Alexander Kurtakov <akurtako@redhat.com> - Bug 459343
 *******************************************************************************/
package org.eclipse.core.tests.resources.usecase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThrows;

import java.util.Hashtable;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;

public class IProjectTest extends IResourceTest {
	public static String LOCAL_LOCATION_PATH_STRING_0;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		LOCAL_LOCATION_PATH_STRING_0 = getWorkspace().getRoot().getLocation().append("temp/location0").toOSString();
	}

	/**
	 * Tests failure on get/set methods invoked on a nonexistent or unopened project.
	 * Get methods either throw an exception or return null (abnormally).
	 * Set methods throw an exception.
	 */
	protected void commonFailureTests(IProject proj, boolean created) {
		// Tests for failure in get/set methods in IResource.
		commonFailureTestsForResource(proj, created);
		// Description
		assertThrows(CoreException.class, () -> proj.getDescription());
	}

	/**
	 * Tests failure on get/set methods invoked on a nonexistent project.
	 * Get methods either throw an exception or return null (abnormally).
	 * Set methods throw an exception.
	 */
	protected void nonexistentProjectFailureTests(IProject proj) {
		commonFailureTests(proj, false);
		IProgressMonitor monitor = null;

		IWorkspace wb = getWorkspace();

		// Try to open a non-created project.
		assertThrows(CoreException.class, () -> proj.open(monitor));
		assertThat("project is unexpectedly open: " + proj, !proj.isOpen());

		// addMapping
		assertThrows(Exception.class,
				() -> proj.getDescription().setLocation(IPath.fromOSString(LOCAL_LOCATION_PATH_STRING_0)));

		assertThat(wb.getRoot().getProjects(), arrayWithSize(0));
	}

	/**
	 * Create a solution in an open workspace. Don't open the solution.
	 * Construct a project handle &amp; check its initial state.
	 * Try creating the project in the unopened solution.
	 * Set/get comment &amp; owner after creating the project but before opening. Is this proper?
	 * Open the solution.
	 * Create the project &amp; check its state further.
	 * Delete the project.
	 * Close the workspace.
	 *
	 * TBD:
	 *
	 * Test that deletion of open project works.
	 * Test that session properties are lost when wb is closed or containing soln is closed or deleted or the
	 * receiving project is closed/deleted.
	 * Test deleting a project that doesn't exist.
	 * Test deleting a project the is in a closed solution.
	 * Test that deleting a project recursively deletes all children.
	 * Test closing a project that doesn't exist.
	 * Test closing a project the is in a closed solution.
	 * Test getDataLocation(Plugin)
	 * Test ensureLocal(...)
	 * Test IResource API
	 * Test IFolder API
	 */
	public void testProject() throws CoreException {
		IWorkspace wb = getWorkspace();
		IProgressMonitor monitor = null;

		// Create a project handle.
		IProject proj = wb.getRoot().getProject(PROJECT);

		// Inspection methods with meaningful results invoked on a handle for a nonexistent project
		assertThat("project exists unexpectedly: " + proj, !proj.exists());
		assertThat(proj.getWorkspace(), is(wb));
		assertThat(proj.getType(), is(IResource.PROJECT));
		assertThat(proj.getFullPath(), is(IPath.fromOSString("/" + PROJECT)));
		assertThat(proj.getName(), is(PROJECT));
		assertThat("project at path '" + proj.getFullPath() + "' unexpectedly exists in workspace",
				!wb.getRoot().exists(proj.getFullPath()));
		assertThat(wb.getRoot().findMember(proj.getFullPath()), is(nullValue()));
		assertThat(proj.getParent(), is(wb.getRoot()));
		// Legal question inherited from IResource: returns the receiver.
		assertThat(proj.getProject(), is(proj));
		assertThat(proj.getProjectRelativePath(), is(IPath.fromOSString("")));

		// Check that there are no projects.
		assertThat(wb.getRoot().getProjects(), arrayWithSize(0));

		// These tests produce failure because the project does not exist yet.
		nonexistentProjectFailureTests(proj);

		// Create the project.
		proj.create(monitor);

		// Check that the project is get-able from the containers.
		assertThat("project does not exist: " + proj, proj.exists());
		assertThat("project with name '" + proj.getName() + "' does not exist in workspace",
				wb.getRoot().findMember(proj.getName()).exists());
		assertTrue("project at path '" + proj.getFullPath() + "' does not exist in workspace",
				wb.getRoot().exists(proj.getFullPath()));
		// But it is still not open.
		assertThat("project is unexpectedly open: " + proj, !proj.isOpen());
		assertThat(wb.getRoot().findMember(proj.getFullPath()), is(proj));

		// These tests produce failure because the project has not been opened yet.
		unopenedProjectFailureTests(proj);

		// Open project
		proj.open(monitor);
		assertThat("project is not open: " + proj, proj.isOpen());
		assertThat(proj.getLocation(), not(is(nullValue())));

		/* Properties */

		// Session Property
		assertThat(proj.getSessionProperty(Q_NAME_SESSION), is(nullValue()));
		proj.setSessionProperty(Q_NAME_SESSION, STRING_VALUE);
		assertThat(proj.getSessionProperty(Q_NAME_SESSION), is(STRING_VALUE));
		proj.setSessionProperty(Q_NAME_SESSION, null);
		assertThat(proj.getSessionProperty(Q_NAME_SESSION), is(nullValue()));

		// Project buildspec
		IProjectDescription desc = proj.getDescription();

		assertThat(desc.getBuildSpec(), arrayWithSize(0));
		ICommand command = desc.newCommand();
		command.setBuilderName("org.eclipse.core.tests.buildername");
		ICommand[] commands = new ICommand[] {command};
		Hashtable<String, String> arguments = new Hashtable<>(2);
		arguments.put("param0", "arg0");
		command.setArguments(arguments);
		// Add buildspec to project
		desc.setBuildSpec(commands);

		// Compare project buildspecs
		assertThat(commands, is(desc.getBuildSpec()));

		// IResource.isLocal(int)
		assertThat("project is not available locally: " + proj, isLocal(proj, IResource.DEPTH_ZERO));
		// No kids, but it should still answer yes.
		assertThat("project and its direct children are not available locally: " + proj,
				isLocal(proj, IResource.DEPTH_ONE));
		assertThat("project and all its children are not available locally: " + proj,
				isLocal(proj, IResource.DEPTH_INFINITE));

		// Close project
		proj.close(monitor);
		// The project is no longer open
		assertThat("project has not been closed: " + proj, !proj.isOpen());
		// But it still exists.
		assertThat("project does not exist: " + proj, proj.exists());
		assertThat(wb.getRoot().findMember(proj.getFullPath()), is(proj));
		assertTrue("project at path '" + proj.getFullPath() + "' does not exist in workspace",
				wb.getRoot().exists(proj.getFullPath()));

		// These tests produce failure because the project is now closed.
		unopenedProjectFailureTests(proj);

		// Delete the project
		proj.delete(true, true, monitor);

		// The project no longer exists.
		assertThat("project unexpectedly exists: " + proj, !proj.exists());
		assertThat(wb.getRoot().getProjects(), arrayWithSize(0));
		assertThat(wb.getRoot().findMember(proj.getFullPath()), is(nullValue()));
		// These tests produce failure because the project no longer exists.
		nonexistentProjectFailureTests(proj);
		assertTrue("project at path '" + proj.getFullPath() + "' unexpectedly exists in workspace",
				!wb.getRoot().exists(proj.getFullPath()));
	}

	/**
	 * Tests failure on get/set methods invoked on a unopened project.
	 * Get methods either throw an exception or return null (abnormally).
	 * Set methods throw an exception.
	 */
	protected void unopenedProjectFailureTests(IProject proj) {
		commonFailureTests(proj, true);
	}

	protected void unopenedSolutionFailureTests(IProject proj, IWorkspace wb) {
		// Try to create the project without the solution being open.
		assertThrows(CoreException.class, () -> proj.create(null));
		assertThat("project unexpectedly exists: " + proj, !proj.exists());
		assertTrue("project at path '" + proj.getFullPath() + "' unexpectedly exists in workspace",
				wb.getRoot().exists(proj.getFullPath()));
	}
}
