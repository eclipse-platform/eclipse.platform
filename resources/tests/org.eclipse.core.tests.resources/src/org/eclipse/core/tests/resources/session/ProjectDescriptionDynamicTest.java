/*******************************************************************************
 * Copyright (c) 2010, 2015 Broadcom Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Broadcom Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.core.tests.resources.session;

import junit.framework.Test;
import org.eclipse.core.resources.*;
import org.eclipse.core.tests.resources.AutomatedResourceTests;
import org.eclipse.core.tests.resources.WorkspaceSessionTest;
import org.eclipse.core.tests.session.WorkspaceSessionTestSuite;

/**
 * This class tests persistence of project description build configurations
 * and project description dynamic state across workbench sessions.
 */
public class ProjectDescriptionDynamicTest extends WorkspaceSessionTest {

	IProject proj;
	String[] configNames;
	IBuildConfiguration[] configs;
	IProject[] dynRefs;
	IBuildConfiguration[] configRefs;
	IProject[] configRefsProjects;

	/**
	 * return reference to the active configuration in the project
	 */
	public IBuildConfiguration getRef(IProject project) {
		return getWorkspace().newBuildConfig(project.getName(), null);
	}

	/**
	 * return reference to the active configuration in the project
	 */
	public IBuildConfiguration getConfig(IProject project, String id) {
		return getWorkspace().newBuildConfig(project.getName(), id);
	}

	@Override
	protected void setUp() throws Exception {
		IWorkspaceRoot wr = getWorkspace().getRoot();
		// The project we're setting metadata on
		proj = wr.getProject("referencing");
		configNames = new String[] {"someConfiguration", "someConfiguration2"};
		configs = new IBuildConfiguration[] {getConfig(proj, "someConfiguration"), getConfig(proj, "someConfiguration2")};

		// The references:
		// Dynamic Project level
		dynRefs = new IProject[] {wr.getProject("ref1"), wr.getProject("ref2")};
		// Dynamic Build Configuration level -- reverse order
		configRefs = new IBuildConfiguration[] {getWorkspace().newBuildConfig("ref3", "ref3config1"), getWorkspace().newBuildConfig("ref2", "ref2config1"), getWorkspace().newBuildConfig("ref1", "ref1config1")};
		configRefsProjects = new IProject[] {wr.getProject("ref3"), wr.getProject("ref2"), wr.getProject("ref1")};
		super.setUp();
	}

	public ProjectDescriptionDynamicTest(String name) {
		super(name);
	}

	/**
	 * Create some dynamic project level references
	 */
	public void test1() throws Exception {
		// Projects to references -- needn't exist
		proj.create(getMonitor());
		proj.open(getMonitor());

		IProjectDescription desc = proj.getDescription();
		desc.setBuildConfigs(configNames);
		desc.setDynamicReferences(dynRefs);
		proj.setDescription(desc, getMonitor());

		ResourcesPlugin.getWorkspace().save(true, getMonitor());
	}

	/**
	 * Check that the following still exist:
	 *  - project build configurations
	 *  - project level references still exist
	 */
	public void test2() throws Exception {
		assertTrue("1.0", proj.isAccessible());
		assertEquals("1.1", dynRefs, proj.getDescription().getDynamicReferences());
		assertEquals("1.2", configs, proj.getBuildConfigs());
		assertEquals("1.3", configs[0], proj.getActiveBuildConfig());

		// set build configuration level dynamic references on the project
		IProjectDescription desc = proj.getDescription();
		desc.setBuildConfigReferences(configs[1].getName(), configRefs);
		// Change the active configuration
		desc.setActiveBuildConfig(configs[1].getName());
		proj.setDescription(desc, getMonitor());

		ResourcesPlugin.getWorkspace().save(true, getMonitor());
	}

	/**
	 * Check that the following still exist:
	 *  - Active configuration has changed
	 *  - Dynamic project references are correct
	 *  - Build config references are correct
	 */
	public void test3() throws Exception {
		assertTrue("2.0", proj.isAccessible());
		assertEquals("2.1", configs[1], proj.getActiveBuildConfig());
		// At description dynamic refs are what was set
		assertEquals("2.2", dynRefs, proj.getDescription().getDynamicReferences());
		// At project all references are union of build configuration and project references
		assertEquals("2.4", configRefsProjects, proj.getReferencedProjects());

		// At the description level, dynamic config references match what was set.
		assertEquals("2.5", configRefs, proj.getDescription().getBuildConfigReferences(configs[1].getName()));
		// At the project level, references are the union of project and build configuration level references
		IBuildConfiguration[] refs = new IBuildConfiguration[] {configRefs[0], configRefs[1], configRefs[2], getRef(dynRefs[0]), getRef(dynRefs[1])};
		assertEquals("2.6", refs, proj.getReferencedBuildConfigs(configs[1].getName(), true));
		// No other projects exist, so check references are empty if we want to filter empty projects
		assertEquals("2.7", new IBuildConfiguration[0], proj.getReferencedBuildConfigs(configs[1].getName(), false));
	}

	public static Test suite() {
		return new WorkspaceSessionTestSuite(AutomatedResourceTests.PI_RESOURCES_TESTS, ProjectDescriptionDynamicTest.class);
	}

}
