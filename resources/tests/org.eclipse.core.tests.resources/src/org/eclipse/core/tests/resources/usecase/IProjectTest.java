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

import static java.util.function.Predicate.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.usecase.IResourceTestUtil.PROJECT;
import static org.eclipse.core.tests.resources.usecase.IResourceTestUtil.Q_NAME_SESSION;
import static org.eclipse.core.tests.resources.usecase.IResourceTestUtil.STRING_VALUE;
import static org.eclipse.core.tests.resources.usecase.IResourceTestUtil.commonFailureTestsForResource;
import static org.eclipse.core.tests.resources.usecase.IResourceTestUtil.isLocal;
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
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class IProjectTest {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	public static String LOCAL_LOCATION_PATH_STRING_0;

	@Before
	public void setUp() {
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
		assertThat(proj).matches(not(IProject::isOpen), "is not open");

		// addMapping
		assertThrows(Exception.class,
				() -> proj.getDescription().setLocation(IPath.fromOSString(LOCAL_LOCATION_PATH_STRING_0)));

		assertThat(wb.getRoot().getProjects()).isEmpty();
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
	@Test
	public void testProject() throws CoreException {
		IWorkspace workspace = getWorkspace();
		IProgressMonitor monitor = null;

		// Create a project handle.
		IProject proj = workspace.getRoot().getProject(PROJECT);

		// Inspection methods with meaningful results invoked on a handle for a nonexistent project
		assertThat(proj).matches(not(IProject::exists), "does not exist");
		assertThat(proj.getWorkspace()).isEqualTo(workspace);
		assertThat(proj.getType()).isEqualTo(IResource.PROJECT);
		assertThat(proj.getFullPath()).isEqualTo(IPath.fromOSString("/" + PROJECT));
		assertThat(proj.getName()).isEqualTo(PROJECT);
		assertThat(proj).matches(it -> !workspace.getRoot().exists(it.getFullPath()), "is not contained in workspace");
		assertThat(workspace.getRoot().findMember(proj.getFullPath())).isNull();
		assertThat(proj.getParent()).isEqualTo(workspace.getRoot());
		// Legal question inherited from IResource: returns the receiver.
		assertThat(proj.getProject()).isEqualTo(proj);
		assertThat(proj.getProjectRelativePath()).isEqualTo(IPath.fromOSString(""));

		// Check that there are no projects.
		assertThat(workspace.getRoot().getProjects()).isEmpty();

		// These tests produce failure because the project does not exist yet.
		nonexistentProjectFailureTests(proj);

		// Create the project.
		proj.create(monitor);

		// Check that the project is get-able from the containers but still not open
		assertThat(proj).matches(IProject::exists, "exists")
				.matches(it -> workspace.getRoot().exists(it.getFullPath()), "is contained in workspace")
				.matches(it -> workspace.getRoot().findMember(it.getFullPath()).exists(),
						"is found existing in workspace")
				.isEqualTo(workspace.getRoot().findMember(proj.getFullPath()))
				.matches(not(IProject::isOpen), "is open");

		// These tests produce failure because the project has not been opened yet.
		unopenedProjectFailureTests(proj);

		// Open project
		proj.open(monitor);
		assertThat(proj).matches(IProject::isOpen, "is open");
		assertThat(proj.getLocation()).isNotNull();

		/* Properties */

		// Session Property
		assertThat(proj.getSessionProperty(Q_NAME_SESSION)).isNull();
		proj.setSessionProperty(Q_NAME_SESSION, STRING_VALUE);
		assertThat(proj.getSessionProperty(Q_NAME_SESSION)).isEqualTo(STRING_VALUE);
		proj.setSessionProperty(Q_NAME_SESSION, null);
		assertThat(proj.getSessionProperty(Q_NAME_SESSION)).isNull();

		// Project buildspec
		IProjectDescription desc = proj.getDescription();

		assertThat(desc.getBuildSpec()).isEmpty();
		ICommand command = desc.newCommand();
		command.setBuilderName("org.eclipse.core.tests.buildername");
		ICommand[] commands = new ICommand[] {command};
		Hashtable<String, String> arguments = new Hashtable<>(2);
		arguments.put("param0", "arg0");
		command.setArguments(arguments);
		// Add buildspec to project
		desc.setBuildSpec(commands);

		// Compare project buildspecs
		assertThat(commands).isEqualTo(desc.getBuildSpec());

		// IResource.isLocal(int)
		assertThat(proj).matches(it -> isLocal(it, IResource.DEPTH_ZERO), "is locally available")
				// No kids, but it should still answer yes.
				.matches(it -> isLocal(it, IResource.DEPTH_ONE), "is locally available with direct children")
				.matches(it -> isLocal(it, IResource.DEPTH_INFINITE), "is locally available with all children");

		// Close project
		proj.close(monitor);
		// The project is no longer open but still exists
		assertThat(proj).matches(not(IProject::isOpen), "is not open") //
				.matches(IProject::exists, "exists")
				.matches(it -> workspace.getRoot().exists(it.getFullPath()), "is contained in workspace")
				.isEqualTo(workspace.getRoot().findMember(proj.getFullPath()));

		// These tests produce failure because the project is now closed.
		unopenedProjectFailureTests(proj);

		// Delete the project
		proj.delete(true, true, monitor);

		// The project no longer exists.
		assertThat(proj).matches(not(IProject::exists), "does not exist");
		assertThat(workspace.getRoot().getProjects()).isEmpty();
		assertThat(workspace.getRoot().findMember(proj.getFullPath())).isNull();
		// These tests produce failure because the project no longer exists.
		nonexistentProjectFailureTests(proj);
		assertThat(proj).matches(it -> !workspace.getRoot().exists(it.getFullPath()), "is not contained in workspace");
	}

	/**
	 * Tests failure on get/set methods invoked on a unopened project.
	 * Get methods either throw an exception or return null (abnormally).
	 * Set methods throw an exception.
	 */
	protected void unopenedProjectFailureTests(IProject proj) {
		commonFailureTests(proj, true);
	}

	protected void unopenedSolutionFailureTests(IProject proj, IWorkspace workspace) {
		// Try to create the project without the solution being open.
		assertThrows(CoreException.class, () -> proj.create(null));
		assertThat(proj).matches(not(IProject::exists), "does not exist");
		assertThat(proj).matches(it -> workspace.getRoot().exists(it.getFullPath()), "is contained in workspace");
	}

}
