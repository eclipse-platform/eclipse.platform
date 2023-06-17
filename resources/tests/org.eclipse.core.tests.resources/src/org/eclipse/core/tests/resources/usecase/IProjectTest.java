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

import java.util.Arrays;
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
		// Prefix to assertion messages.
		String method = "commonFailureTests(IProject," + (created ? "CREATED" : "NONEXISTENT") + "): ";

		// Tests for failure in get/set methods in IResource.
		commonFailureTestsForResource(proj, created);

		// Description
		try {
			proj.getDescription();
			fail(method + "4.1");
		} catch (CoreException e) {
			// expected
		}
	}

	/**
	 * Tests failure on get/set methods invoked on a nonexistent project.
	 * Get methods either throw an exception or return null (abnormally).
	 * Set methods throw an exception.
	 */
	protected void nonexistentProjectFailureTests(IProject proj) {
		String method = "nonexistentProjectFailureTests(IProject,ISolution,IWorkspace): ";
		commonFailureTests(proj, false);
		IProgressMonitor monitor = null;

		IWorkspace wb = getWorkspace();

		// Try to open a non-created project.
		try {
			proj.open(monitor);
			fail(method + "0.0");
		} catch (CoreException e) {
			// expected
		}
		assertTrue(method + "0.1", !proj.isOpen());

		// addMapping
		try {
			// Project must exist.
			proj.getDescription().setLocation(IPath.fromOSString(LOCAL_LOCATION_PATH_STRING_0));
			fail(method + "1");
		} catch (Exception e) {
			// expected
		}

		assertTrue(method + "2.1", wb.getRoot().getProjects().length == 0);
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
	public void testProject() {
		IWorkspace wb = getWorkspace();
		IProgressMonitor monitor = null;

		// Create a project handle.
		IProject proj = wb.getRoot().getProject(PROJECT);

		// Inspection methods with meaningful results invoked on a handle for a nonexistent project
		assertTrue("1.1", !proj.exists());
		assertTrue("1.2", proj.getWorkspace().equals(wb));
		assertTrue("1.4", proj.getType() == IResource.PROJECT);
		assertTrue("1.5", proj.getFullPath().equals(IPath.fromOSString("/" + PROJECT)));
		assertTrue("1.6", proj.getName().equals(PROJECT));
		assertTrue("1.9", !wb.getRoot().exists(proj.getFullPath()));
		assertTrue("1.11", wb.getRoot().findMember(proj.getFullPath()) == null);
		assertTrue("1.12", proj.getParent().equals(wb.getRoot()));
		// Legal question inherited from IResource: returns the receiver.
		assertTrue("1.13", proj.getProject().equals(proj));
		assertTrue("1.14", proj.getProjectRelativePath().equals(IPath.fromOSString("")));

		// Check that there are no projects.
		assertTrue("6.1", wb.getRoot().getProjects().length == 0);

		// These tests produce failure because the project does not exist yet.
		nonexistentProjectFailureTests(proj);

		// Create the project.
		try {
			proj.create(monitor);
		} catch (CoreException e) {
			fail("8");
		}

		// Check that the project is get-able from the containers.
		assertTrue("9.0", proj.exists());
		assertTrue("9.1", wb.getRoot().findMember(proj.getName()).exists());
		assertTrue("9.3", wb.getRoot().exists(proj.getFullPath()));
		// But it is still not open.
		assertTrue("9.4", !proj.isOpen());
		assertTrue("9.5", wb.getRoot().findMember(proj.getFullPath()).equals(proj));

		// These tests produce failure because the project has not been opened yet.
		unopenedProjectFailureTests(proj);

		// Open project
		try {
			proj.open(monitor);
		} catch (CoreException e) {
			fail("11.0", e);
		}
		assertTrue("11.1", proj.isOpen());
		assertTrue("11.2", proj.getLocation() != null);

		/* Properties */

		// Session Property
		try {
			assertTrue("12.0", proj.getSessionProperty(Q_NAME_SESSION) == null);
		} catch (CoreException e) {
			fail("12.1");
		}
		try {
			proj.setSessionProperty(Q_NAME_SESSION, STRING_VALUE);
		} catch (CoreException e) {
			fail("12.2");
		}
		try {
			assertTrue("12.3", proj.getSessionProperty(Q_NAME_SESSION).equals(STRING_VALUE));
		} catch (CoreException e) {
			fail("12.4");
		}
		try {
			proj.setSessionProperty(Q_NAME_SESSION, null);
		} catch (CoreException e) {
			fail("12.5");
		}
		try {
			assertTrue("12.6", proj.getSessionProperty(Q_NAME_SESSION) == null);
		} catch (CoreException e) {
			fail("12.7");
		}

		// Project buildspec
		IProjectDescription desc = null;
		try {
			desc = proj.getDescription();
		} catch (CoreException e) {
			fail("14.0");
		}

		assertTrue("15.1", desc.getBuildSpec().length == 0);
		ICommand command = desc.newCommand();
		command.setBuilderName("org.eclipse.core.tests.buildername");
		ICommand[] commands = new ICommand[] {command};
		Hashtable<String, String> arguments = new Hashtable<>(2);
		arguments.put("param0", "arg0");
		command.setArguments(arguments);
		// Add buildspec to project
		desc.setBuildSpec(commands);

		// Compare project buildspecs
		assertTrue("15.5", Arrays.deepEquals(desc.getBuildSpec(), commands));

		// IResource.isLocal(int)
		assertTrue("18.0", proj.isLocal(IResource.DEPTH_ZERO));
		assertTrue("18.1", proj.isLocal(IResource.DEPTH_ONE));
		assertTrue("18.2", proj.isLocal(IResource.DEPTH_INFINITE));

		// Close project
		try {
			proj.close(monitor);
		} catch (CoreException e) {
			fail("19.0", e);
		}
		// The project is no longer open
		assertTrue("19.1", !proj.isOpen());
		// But it still exists.
		assertTrue("19.2", proj.exists());
		assertTrue("19.5", wb.getRoot().findMember(proj.getFullPath()).equals(proj));
		assertTrue("19.6", wb.getRoot().exists(proj.getFullPath()));

		// These tests produce failure because the project is now closed.
		unopenedProjectFailureTests(proj);

		// Delete the project
		try {
			proj.delete(true, true, monitor);
		} catch (CoreException e) {
			fail("20.0", e);
		}

		// The project no longer exists.
		assertTrue("20.1", !proj.exists());
		assertTrue("20.2", wb.getRoot().getProjects().length == 0);
		assertTrue("20.4", wb.getRoot().findMember(proj.getFullPath()) == null);
		// These tests produce failure because the project no longer exists.
		nonexistentProjectFailureTests(proj);
		assertTrue("20.5", !wb.getRoot().exists(proj.getFullPath()));
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
		String method = "unopenedSolutionFailureTests(IProject,IWorkspace): ";
		IProgressMonitor monitor = null;

		// Try to create the project without the solution being open.
		try {
			proj.create(monitor);
			fail(method + "1");
		} catch (CoreException e) {
			// expected
		}
		assertTrue(method + "2", !proj.exists());
		assertTrue(method + "3", !wb.getRoot().exists(proj.getFullPath()));
	}
}
