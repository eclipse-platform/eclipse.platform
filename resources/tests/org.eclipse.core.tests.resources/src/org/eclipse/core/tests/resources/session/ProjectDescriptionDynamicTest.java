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

import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.PI_RESOURCES_TESTS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.is;

import junit.framework.Test;
import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
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
		assertThat("project is unexpectedly not accessible: " + proj, proj.isAccessible());
		assertThat(dynRefs, is(proj.getDescription().getDynamicReferences()));
		assertThat(proj.getBuildConfigs(), is(configs));
		assertThat(proj.getActiveBuildConfig(), is(configs[0]));

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
		assertThat("project is unexpectedly not accessible: " + proj, proj.isAccessible());
		assertThat(proj.getActiveBuildConfig(), is(configs[1]));
		// At description dynamic refs are what was set
		assertThat(proj.getDescription().getDynamicReferences(), is(dynRefs));
		// At project all references are union of build configuration and project references
		assertThat(proj.getReferencedProjects(), is(configRefsProjects));

		// At the description level, dynamic config references match what was set.
		assertThat(proj.getDescription().getBuildConfigReferences(configs[1].getName()), is(configRefs));
		// At the project level, references are the union of project and build configuration level references
		IBuildConfiguration[] refs = new IBuildConfiguration[] {configRefs[0], configRefs[1], configRefs[2], getRef(dynRefs[0]), getRef(dynRefs[1])};
		assertThat(proj.getReferencedBuildConfigs(configs[1].getName(), true), is(refs));
		// No other projects exist, so check references are empty if we want to filter empty projects
		assertThat(proj.getReferencedBuildConfigs(configs[1].getName(), false), emptyArray());
	}

	public static Test suite() {
		return new WorkspaceSessionTestSuite(PI_RESOURCES_TESTS, ProjectDescriptionDynamicTest.class);
	}

}
