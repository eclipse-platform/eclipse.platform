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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;

import org.eclipse.core.internal.resources.BuildConfiguration;
import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests configuration project build configuration on the project description
 */
public class ProjectBuildConfigsTest {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	private IProject project;
	private static final String variantId0 = "Variant0";
	private static final String variantId1 = "Variant1";
	private static final String variantId2 = "Variant2";
	private IBuildConfiguration variant0;
	private IBuildConfiguration variant1;
	private IBuildConfiguration variant2;
	private IBuildConfiguration defaultVariant;

	@Before
	public void setUp() throws Exception {
		project = getWorkspace().getRoot().getProject("ProjectBuildConfigsTest_Project");
		createInWorkspace(new IProject[] {project});
		variant0 = new BuildConfiguration(project, variantId0);
		variant1 = new BuildConfiguration(project, variantId1);
		variant2 = new BuildConfiguration(project, variantId2);
		defaultVariant = new BuildConfiguration(project, IBuildConfiguration.DEFAULT_CONFIG_NAME);
	}

	@Test
	public void testBasics() throws CoreException {
		IProjectDescription desc = project.getDescription();
		String[] configs = new String[] {variantId0, variantId1};
		desc.setBuildConfigs(configs);
		project.setDescription(desc, createTestMonitor());

		assertThat(project.getBuildConfigs()).containsExactly(variant0, variant1);
		assertThat(project.getBuildConfig(variantId0)).isEqualTo(variant0);
		assertThat(project.getBuildConfig(variantId1)).isEqualTo(variant1);

		// Build configuration names don't contribute to equality
		assertThat(project.hasBuildConfig(variant0.getName()))
				.withFailMessage("project '%s' is missing build config: %s", project, variant0).isTrue();
		assertThat(project.hasBuildConfig(variant1.getName()))
				.withFailMessage("project '%s' is missing build config: %s", project, variant1).isTrue();
		assertThat(project.hasBuildConfig(variant2.getName()))
				.withFailMessage("project '%s' unexpectedly has build config: %s", project, variant2).isFalse();

		assertThat(project.getActiveBuildConfig()).isEqualTo(variant0);
		desc = project.getDescription();
		desc.setActiveBuildConfig(variantId1);
		project.setDescription(desc, createTestMonitor());
		assertThat(project.getActiveBuildConfig()).isEqualTo(variant1);
		// test that setting the variant to an invalid id has no effect
		desc.setActiveBuildConfig(variantId2);
		assertThat(project.getActiveBuildConfig()).isEqualTo(variant1);

		IBuildConfiguration variant = project.getBuildConfigs()[0];
		assertThat(variant.getProject()).isEqualTo(project);
		assertThat(variant.getName()).isEqualTo(variantId0);
	}

	@Test
	public void testDuplicates() throws CoreException {
		IProjectDescription desc = project.getDescription();
		desc.setBuildConfigs(new String[] {variantId0, variantId1, variantId0});
		project.setDescription(desc, createTestMonitor());
		assertThat(project.getBuildConfigs()).containsExactly(variant0, variant1);
	}

	@Test
	public void testDefaultVariant() throws CoreException {
		IProjectDescription desc = project.getDescription();
		desc.setBuildConfigs(new String[] {});
		project.setDescription(desc, createTestMonitor());

		assertThat(project.getBuildConfigs()).containsExactly(defaultVariant);
		assertThat(project.hasBuildConfig(defaultVariant.getName()))
				.withFailMessage("project '%s' is missing build config: %s", project, defaultVariant).isTrue();

		assertThat(project.getActiveBuildConfig()).isEqualTo(defaultVariant);
		desc = project.getDescription();
		desc.setActiveBuildConfig(IBuildConfiguration.DEFAULT_CONFIG_NAME);
		project.setDescription(desc, createTestMonitor());
		assertThat(project.getActiveBuildConfig()).isEqualTo(defaultVariant);
	}

	@Test
	public void testRemoveActiveVariant() throws CoreException {
		IProjectDescription desc = project.getDescription();
		desc.setBuildConfigs(new String[0]);
		desc.setBuildConfigs(new String[] {variant0.getName(), variant1.getName()});
		project.setDescription(desc, createTestMonitor());
		assertThat(project.getActiveBuildConfig()).isEqualTo(variant0);
		desc.setBuildConfigs(new String[] {variant0.getName(), variant2.getName()});
		project.setDescription(desc, createTestMonitor());
		assertThat(project.getActiveBuildConfig()).isEqualTo(variant0);
		desc = project.getDescription();
		desc.setActiveBuildConfig(variantId2);
		project.setDescription(desc, createTestMonitor());
		desc.setBuildConfigs(new String[] {variant0.getName(), variant1.getName()});
		project.setDescription(desc, createTestMonitor());
		assertThat(project.getActiveBuildConfig()).isEqualTo(variant0);
	}

	/**
	 * Tests that build configuration references are correct after moving a project
	 */
	@Test
	public void testProjectMove() throws CoreException {
		IProjectDescription desc = project.getDescription();
		IBuildConfiguration[] configs = new IBuildConfiguration[] {variant0, variant1};
		desc.setBuildConfigs(new String[] {configs[0].getName(), configs[1].getName()});
		project.setDescription(desc, createTestMonitor());

		// Move the project. The build configurations should point at the new project
		String newProjectName = "projectMoved";
		desc = project.getDescription();
		desc.setName(newProjectName);
		project.move(desc, false, createTestMonitor());

		IProject newProject = getWorkspace().getRoot().getProject(newProjectName);
		assertThat(newProject).matches(IProject::exists, "exists");

		IBuildConfiguration[] newConfigs = newProject.getBuildConfigs();
		for (int i = 0; i < configs.length; i++) {
			assertThat(newConfigs[i].getProject()).as("project at index %s", i).isEqualTo(newProject);
			assertThat(newConfigs[i].getName()).as("project name at index %s", i).isEqualTo(configs[i].getName());
		}
	}

}
