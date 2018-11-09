/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
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
package org.eclipse.team.tests.ccvs.ui;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.ui.SelectionPropertyTester;
import org.eclipse.team.internal.ccvs.ui.actions.BranchAction;
import org.eclipse.team.internal.ccvs.ui.actions.CommitAction;
import org.eclipse.team.internal.ccvs.ui.actions.IgnoreAction;
import org.eclipse.team.tests.ccvs.core.CVSTestSetup;
import org.eclipse.ui.IActionDelegate;

import junit.framework.Test;
import junit.framework.TestSuite;

public class SelectionPropertyTesterTest extends EnablementTest {

	/**
	 * Constructor for CVSProviderTest
	 */
	public SelectionPropertyTesterTest() {
		super();
	}

	/**
	 * Constructor for CVSProviderTest
	 */
	public SelectionPropertyTesterTest(String name) {
		super(name);
	}

	public static Test suite() {
		TestSuite suite = new TestSuite(SelectionPropertyTesterTest.class);
		return new CVSTestSetup(suite);
	}

	//
	// tests for corner cases
	//
	public void testSelectionPropertyTester_allNulls() {
		SelectionPropertyTester tester = new SelectionPropertyTester();
		assertEquals(false, tester.test(null, null, null, null));
	}

	public void testSelectionPropertyTester_nullSelection() {
		SelectionPropertyTester tester = new SelectionPropertyTester();
		assertEquals(
				false,
				tester
						.test(
								null,
								"isEnabled",
								new Object[] { "org.eclipse.team.internal.ccvs.ui.actions.CommitAction", },
								null));
	}

	public void testSelectionPropertyTester_invalidClassForSelection()
			throws TeamException, CoreException {
		SelectionPropertyTester tester = new SelectionPropertyTester();
		IActionDelegate action = new CommitAction();
		IProject project = createTestProject(action);
		List<IResource> resources = getManagedResources(project, false, false);
		assertEquals(
				false,
				tester
						.test(
								resources,
								"isEnabled",
								new Object[] { "org.eclipse.team.internal.ccvs.ui.actions.CommitAction", },
								null));
	}

	public void testSelectionPropertyTester_toManyArgs() throws TeamException,
			CoreException {
		SelectionPropertyTester tester = new SelectionPropertyTester();
		IActionDelegate action = new CommitAction();
		IProject project = createTestProject(action);
		List<IResource> resources = getManagedResources(project, false, false);
		assertEquals(
				false,
				tester
						.test(
								asSelection(resources),
								"isEnabled",
								new Object[] {
										"org.eclipse.team.internal.ccvs.ui.actions.CommitAction",
										"org.eclipse.team.internal.ccvs.ui.actions.IgnoreAction" },
								null));
	}

	public void testSelectionPropertyTester_unknownProperty()
			throws TeamException, CoreException {
		SelectionPropertyTester tester = new SelectionPropertyTester();
		IActionDelegate action = new CommitAction();
		IProject project = createTestProject(action);
		List<IResource> resources = getManagedResources(project, false, false);
		assertEquals(
				false,
				tester
						.test(
								asSelection(resources),
								"unknownProperty",
								new Object[] { "org.eclipse.team.internal.ccvs.ui.actions.CommitAction" },
								null));
	}

	// enablement for resources from a shared project

	public void testSelectionPropertyTester_commitActionManaged()
			throws CoreException, TeamException {
		SelectionPropertyTester tester = new SelectionPropertyTester();
		IActionDelegate action = new CommitAction();
		IProject project = createTestProject(action);
		List<IResource> resources = getManagedResources(project, false, false);
		boolean testResult = tester
				.test(
						asSelection(resources),
						"isEnabled",
						new Object[] { "org.eclipse.team.internal.ccvs.ui.actions.CommitAction" },
						null);
		assertEquals(true, testResult);
		assertEnablement(action, asSelection(resources), testResult);
	}

	public void testSelectionPropertyTester_ignoreActionManaged()
			throws CoreException, TeamException {
		SelectionPropertyTester tester = new SelectionPropertyTester();
		IActionDelegate action = new IgnoreAction();
		IProject project = createTestProject(action);
		List<IResource> resources = getManagedResources(project, false, false);
		boolean testResult = tester
				.test(
						asSelection(resources),
						"isEnabled",
						new Object[] { "org.eclipse.team.internal.ccvs.ui.actions.IgnoreAction" },
						null);
		assertEquals(false, testResult);
		assertEnablement(action, asSelection(resources), testResult);
	}

	public void testSelectionPropertyTester_branchActionManaged()
			throws CoreException, TeamException {
		SelectionPropertyTester tester = new SelectionPropertyTester();
		IActionDelegate action = new BranchAction();
		IProject project = createTestProject(action);
		List<IResource> resources = getManagedResources(project, false, false);
		boolean testResult = tester
				.test(
						asSelection(resources),
						"isEnabled",
						new Object[] { "org.eclipse.team.internal.ccvs.ui.actions.BranchAction" },
						null);
		assertEquals(true, testResult);
		assertEnablement(action, asSelection(resources), testResult);
	}

	public void testSelectionPropertyTester_commitActionUnmanaged()
			throws CoreException, TeamException {
		SelectionPropertyTester tester = new SelectionPropertyTester();
		IActionDelegate action = new CommitAction();
		IProject project = createTestProject(action);
		List<IResource> resources = getUnmanagedResources(project);
		boolean testResult = tester
				.test(
						asSelection(resources),
						"isEnabled",
						new Object[] { "org.eclipse.team.internal.ccvs.ui.actions.CommitAction" },
						null);
		assertEquals(true, testResult);
		assertEnablement(action, asSelection(resources), testResult);
	}

	public void testSelectionPropertyTester_ignoreActionUnmanaged()
			throws CoreException, TeamException {
		SelectionPropertyTester tester = new SelectionPropertyTester();
		IActionDelegate action = new IgnoreAction();
		IProject project = createTestProject(action);
		List<IResource> resources = getUnmanagedResources(project);
		boolean testResult = tester
				.test(
						asSelection(resources),
						"isEnabled",
						new Object[] { "org.eclipse.team.internal.ccvs.ui.actions.IgnoreAction" },
						null);
		assertEquals(true, testResult);
		assertEnablement(action, asSelection(resources), testResult);
	}

	public void testSelectionPropertyTester_branchActionUnmanaged()
			throws CoreException, TeamException {
		SelectionPropertyTester tester = new SelectionPropertyTester();
		IActionDelegate action = new BranchAction();
		IProject project = createTestProject(action);
		List<IResource> resources = getUnmanagedResources(project);
		boolean testResult = tester
				.test(
						asSelection(resources),
						"isEnabled",
						new Object[] { "org.eclipse.team.internal.ccvs.ui.actions.BranchAction" },
						null);
		assertEquals(false, testResult);
		assertEnablement(action, asSelection(resources), testResult);
	}

	// enablement for resources from an unshared project

	public void testSelectionPropertyTester_commitActionUnshared()
			throws CoreException, TeamException {
		SelectionPropertyTester tester = new SelectionPropertyTester();
		IActionDelegate action = new CommitAction();
		String actionName = getName(action);
		String[] resourcesNames = new String[] { "file.txt", "folder1/",
				"folder1/a.txt" };
		IProject project = getUniqueTestProject(actionName);
		IResource[] buildResources = buildResources(project, resourcesNames,
				true);
		List<IResource> resources = Arrays.asList(buildResources);
		boolean testResult = tester
				.test(
						asSelection(resources),
						"isEnabled",
						new Object[] { "org.eclipse.team.internal.ccvs.ui.actions.CommitAction" },
						null);
		assertEquals(false, testResult);
		assertEnablement(action, asSelection(resources), testResult);
	}

	public void testSelectionPropertyTester_ignoreActionUnshared()
			throws CoreException, TeamException {
		SelectionPropertyTester tester = new SelectionPropertyTester();
		IActionDelegate action = new IgnoreAction();
		String actionName = getName(action);
		String[] resourcesNames = new String[] { "file.txt", "folder1/",
				"folder1/a.txt" };
		IProject project = getUniqueTestProject(actionName);
		IResource[] buildResources = buildResources(project, resourcesNames,
				true);
		List<IResource> resources = Arrays.asList(buildResources);
		boolean testResult = tester
				.test(
						asSelection(resources),
						"isEnabled",
						new Object[] { "org.eclipse.team.internal.ccvs.ui.actions.IgnoreAction" },
						null);
		assertEquals(false, testResult);
		assertEnablement(action, asSelection(resources), testResult);
	}

	public void testSelectionPropertyTester_branchActionUnshared()
			throws CoreException, TeamException {
		SelectionPropertyTester tester = new SelectionPropertyTester();
		IActionDelegate action = new BranchAction();
		String actionName = getName(action);
		String[] resourcesNames = new String[] { "file.txt", "folder1/",
				"folder1/a.txt" };
		IProject project = getUniqueTestProject(actionName);
		IResource[] buildResources = buildResources(project, resourcesNames,
				true);
		List<IResource> resources = Arrays.asList(buildResources);
		boolean testResult = tester
				.test(
						asSelection(resources),
						"isEnabled",
						new Object[] { "org.eclipse.team.internal.ccvs.ui.actions.BranchAction" },
						null);
		assertEquals(false, testResult);
		assertEnablement(action, asSelection(resources), testResult);
	}

}
