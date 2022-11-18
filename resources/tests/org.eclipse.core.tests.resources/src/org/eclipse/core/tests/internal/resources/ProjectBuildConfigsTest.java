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
 *******************************************************************************/
package org.eclipse.core.tests.internal.resources;

import org.eclipse.core.internal.resources.BuildConfiguration;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.ResourceTest;

/**
 * Tests configuration project build configuration on the project description
 */
public class ProjectBuildConfigsTest extends ResourceTest {

	private IProject project;
	private final String variantId0 = "Variant0";
	private final String variantId1 = "Variant1";
	private final String variantId2 = "Variant2";
	private IBuildConfiguration variant0;
	private IBuildConfiguration variant1;
	private IBuildConfiguration variant2;
	private IBuildConfiguration defaultVariant;

	@Override
	public void setUp() throws Exception {
		project = getWorkspace().getRoot().getProject("ProjectBuildConfigsTest_Project");
		ensureExistsInWorkspace(new IProject[] {project}, true);
		variant0 = new BuildConfiguration(project, variantId0);
		variant1 = new BuildConfiguration(project, variantId1);
		variant2 = new BuildConfiguration(project, variantId2);
		defaultVariant = new BuildConfiguration(project, IBuildConfiguration.DEFAULT_CONFIG_NAME);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		project.delete(true, null);
	}

	public void testBasics() throws CoreException {
		IProjectDescription desc = project.getDescription();
		String[] configs = new String[] {variantId0, variantId1};
		desc.setBuildConfigs(configs);
		project.setDescription(desc, getMonitor());

		assertEquals("1.0", new IBuildConfiguration[] {variant0, variant1}, project.getBuildConfigs());
		assertEquals("1.1", variant0, project.getBuildConfig(variantId0));
		assertEquals("1.2", variant1, project.getBuildConfig(variantId1));

		// Build configuration names don't contribute to equality
		assertTrue("2.0", project.hasBuildConfig(variant0.getName()));
		assertTrue("2.1", project.hasBuildConfig(variant1.getName()));
		assertFalse("2.2", project.hasBuildConfig(variant2.getName()));

		assertEquals("3.0", variant0, project.getActiveBuildConfig());
		desc = project.getDescription();
		desc.setActiveBuildConfig(variantId1);
		project.setDescription(desc, getMonitor());
		assertEquals("3.1", variant1, project.getActiveBuildConfig());
		// test that setting the variant to an invalid id has no effect
		desc.setActiveBuildConfig(variantId2);
		assertEquals("3.2", variant1, project.getActiveBuildConfig());

		IBuildConfiguration variant = project.getBuildConfigs()[0];
		assertEquals("4.0", project, variant.getProject());
		assertEquals("4.1", variantId0, variant.getName());
	}

	public void testDuplicates() throws CoreException {
		IProjectDescription desc = project.getDescription();
		desc.setBuildConfigs(new String[] {variantId0, variantId1, variantId0});
		project.setDescription(desc, getMonitor());
		assertEquals("1.0", new IBuildConfiguration[] {variant0, variant1}, project.getBuildConfigs());
	}

	public void testDefaultVariant() throws CoreException {
		IProjectDescription desc = project.getDescription();
		desc.setBuildConfigs(new String[] {});
		project.setDescription(desc, getMonitor());

		assertEquals("1.0", new IBuildConfiguration[] {defaultVariant}, project.getBuildConfigs());
		assertTrue("1.1", project.hasBuildConfig(defaultVariant.getName()));

		assertEquals("2.0", defaultVariant, project.getActiveBuildConfig());
		desc = project.getDescription();
		desc.setActiveBuildConfig(IBuildConfiguration.DEFAULT_CONFIG_NAME);
		project.setDescription(desc, getMonitor());
		assertEquals("2.1", defaultVariant, project.getActiveBuildConfig());
	}

	public void testRemoveActiveVariant() throws CoreException {
		IProjectDescription desc = project.getDescription();
		desc.setBuildConfigs(new String[0]);
		desc.setBuildConfigs(new String[] {variant0.getName(), variant1.getName()});
		project.setDescription(desc, getMonitor());
		assertEquals("1.0", variant0, project.getActiveBuildConfig());
		desc.setBuildConfigs(new String[] {variant0.getName(), variant2.getName()});
		project.setDescription(desc, getMonitor());
		assertEquals("2.0", variant0, project.getActiveBuildConfig());
		desc = project.getDescription();
		desc.setActiveBuildConfig(variantId2);
		project.setDescription(desc, getMonitor());
		desc.setBuildConfigs(new String[] {variant0.getName(), variant1.getName()});
		project.setDescription(desc, getMonitor());
		assertEquals("3.0", variant0, project.getActiveBuildConfig());
	}

	/**
	 * Tests that build configuration references are correct after moving a project
	 * @throws CoreException
	 */
	public void testProjectMove() throws CoreException {
		IProjectDescription desc = project.getDescription();
		IBuildConfiguration[] configs = new IBuildConfiguration[] {variant0, variant1};
		desc.setBuildConfigs(new String[] {configs[0].getName(), configs[1].getName()});
		project.setDescription(desc, getMonitor());

		// Move the project. The build configurations should point at the new project
		String newProjectName = "projectMoved";
		desc = project.getDescription();
		desc.setName(newProjectName);
		project.move(desc, false, getMonitor());

		IProject newProject = getWorkspace().getRoot().getProject(newProjectName);
		assertTrue("1.0", newProject.exists());

		IBuildConfiguration[] newConfigs = newProject.getBuildConfigs();
		for (int i = 0; i < configs.length; i++) {
			assertEquals("2." + i * 3, newProject, newConfigs[i].getProject());
			assertEquals("2." + i * 3 + 1, configs[i].getName(), newConfigs[i].getName());
		}
	}
}
