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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.is;

import org.eclipse.core.internal.resources.BuildConfiguration;
import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.ResourceTest;

/**
 * Tests configuration project build configuration on the project description
 */
public class ProjectBuildConfigsTest extends ResourceTest {

	private IProject project;
	private static final String variantId0 = "Variant0";
	private static final String variantId1 = "Variant1";
	private static final String variantId2 = "Variant2";
	private IBuildConfiguration variant0;
	private IBuildConfiguration variant1;
	private IBuildConfiguration variant2;
	private IBuildConfiguration defaultVariant;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		project = getWorkspace().getRoot().getProject("ProjectBuildConfigsTest_Project");
		ensureExistsInWorkspace(new IProject[] {project}, true);
		variant0 = new BuildConfiguration(project, variantId0);
		variant1 = new BuildConfiguration(project, variantId1);
		variant2 = new BuildConfiguration(project, variantId2);
		defaultVariant = new BuildConfiguration(project, IBuildConfiguration.DEFAULT_CONFIG_NAME);
	}

	public void testBasics() throws CoreException {
		IProjectDescription desc = project.getDescription();
		String[] configs = new String[] {variantId0, variantId1};
		desc.setBuildConfigs(configs);
		project.setDescription(desc, getMonitor());

		assertThat(project.getBuildConfigs(), arrayContaining(variant0, variant1));
		assertThat(project.getBuildConfig(variantId0), is(variant0));
		assertThat(project.getBuildConfig(variantId1), is(variant1));

		// Build configuration names don't contribute to equality
		assertThat("project '" + project + "' is missing build config: " + variant0,
				project.hasBuildConfig(variant0.getName()));
		assertThat("project '" + project + "' is missing build config: " + variant1,
				project.hasBuildConfig(variant1.getName()));
		assertThat("project '" + project + "' unexpectedly has build config: " + variant2,
				!project.hasBuildConfig(variant2.getName()));

		assertThat(project.getActiveBuildConfig(), is(variant0));
		desc = project.getDescription();
		desc.setActiveBuildConfig(variantId1);
		project.setDescription(desc, getMonitor());
		assertThat(project.getActiveBuildConfig(), is(variant1));
		// test that setting the variant to an invalid id has no effect
		desc.setActiveBuildConfig(variantId2);
		assertThat(project.getActiveBuildConfig(), is(variant1));

		IBuildConfiguration variant = project.getBuildConfigs()[0];
		assertThat(variant.getProject(), is(project));
		assertThat(variant.getName(), is(variantId0));
	}

	public void testDuplicates() throws CoreException {
		IProjectDescription desc = project.getDescription();
		desc.setBuildConfigs(new String[] {variantId0, variantId1, variantId0});
		project.setDescription(desc, getMonitor());
		assertThat(project.getBuildConfigs(), arrayContaining(variant0, variant1));
	}

	public void testDefaultVariant() throws CoreException {
		IProjectDescription desc = project.getDescription();
		desc.setBuildConfigs(new String[] {});
		project.setDescription(desc, getMonitor());

		assertThat(project.getBuildConfigs(), arrayContaining(defaultVariant));
		assertThat("project '" + project + "' is missing build config: " + defaultVariant,
				project.hasBuildConfig(defaultVariant.getName()));

		assertThat(project.getActiveBuildConfig(), is(defaultVariant));
		desc = project.getDescription();
		desc.setActiveBuildConfig(IBuildConfiguration.DEFAULT_CONFIG_NAME);
		project.setDescription(desc, getMonitor());
		assertThat(project.getActiveBuildConfig(), is(defaultVariant));
	}

	public void testRemoveActiveVariant() throws CoreException {
		IProjectDescription desc = project.getDescription();
		desc.setBuildConfigs(new String[0]);
		desc.setBuildConfigs(new String[] {variant0.getName(), variant1.getName()});
		project.setDescription(desc, getMonitor());
		assertThat(project.getActiveBuildConfig(), is(variant0));
		desc.setBuildConfigs(new String[] {variant0.getName(), variant2.getName()});
		project.setDescription(desc, getMonitor());
		assertThat(project.getActiveBuildConfig(), is(variant0));
		desc = project.getDescription();
		desc.setActiveBuildConfig(variantId2);
		project.setDescription(desc, getMonitor());
		desc.setBuildConfigs(new String[] {variant0.getName(), variant1.getName()});
		project.setDescription(desc, getMonitor());
		assertThat(project.getActiveBuildConfig(), is(variant0));
	}

	/**
	 * Tests that build configuration references are correct after moving a project
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
		assertThat("project does not exist: " + newProject, newProject.exists());

		IBuildConfiguration[] newConfigs = newProject.getBuildConfigs();
		for (int i = 0; i < configs.length; i++) {
			assertThat("unexpected project at index " + i, newConfigs[i].getProject(), is(newProject));
			assertThat("unexpected project name at index " + i, newConfigs[i].getName(), is(configs[i].getName()));
		}
	}
}
