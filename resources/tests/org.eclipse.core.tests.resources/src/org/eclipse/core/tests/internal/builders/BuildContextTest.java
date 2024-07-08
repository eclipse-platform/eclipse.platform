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
package org.eclipse.core.tests.internal.builders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.setAutoBuilding;
import static org.eclipse.core.tests.resources.ResourceTestUtil.updateProjectDescription;
import static org.eclipse.core.tests.resources.ResourceTestUtil.waitForBuild;

import org.eclipse.core.internal.events.BuildContext;
import org.eclipse.core.internal.resources.BuildConfiguration;
import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IBuildContext;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * These tests exercise the build context functionality that tells a builder in what context
 * it was called.
 */
@ExtendWith(WorkspaceResetExtension.class)
public class BuildContextTest {
	private IProject project0;
	private IProject project1;
	private IProject project2;
	private static final String variant0 = "Variant0";
	private static final String variant1 = "Variant1";

	@BeforeEach
	public void setUp() throws Exception {
		// Create resources
		IWorkspaceRoot root = getWorkspace().getRoot();
		project0 = root.getProject("BuildContextTests_p0");
		project1 = root.getProject("BuildContextTests_p1");
		project2 = root.getProject("BuildContextTests_p2");
		IResource[] resources = {project0, project1, project2};
		createInWorkspace(resources);
		setAutoBuilding(false);
		setupProject(project0);
		setupProject(project1);
		setupProject(project2);
	}

	/**
	 * Helper method to configure a project with a build command and several buildConfigs.
	 */
	private void setupProject(IProject project) throws CoreException {
		updateProjectDescription(project).addingCommand(ContextBuilder.BUILDER_NAME).withTestBuilderId("Build0")
				.withBuildingSetting(IncrementalProjectBuilder.AUTO_BUILD, true)
				.withBuildingSetting(IncrementalProjectBuilder.FULL_BUILD, true)
				.withBuildingSetting(IncrementalProjectBuilder.INCREMENTAL_BUILD, true)
				.withBuildingSetting(IncrementalProjectBuilder.CLEAN_BUILD, true).apply();

		IProjectDescription desc = project.getDescription();
		desc.setBuildConfigs(new String[] {variant0, variant1});
		project.setDescription(desc, createTestMonitor());
	}

	/**
	 * Change the active build configuration on the project returning the new active build configuration
	 */
	private IBuildConfiguration changeActiveBuildConfig(IProject project) throws CoreException {
		IBuildConfiguration[] configs = project.getBuildConfigs();
		IBuildConfiguration active = project.getActiveBuildConfig();
		IProjectDescription desc = project.getDescription();
		for (IBuildConfiguration config : configs) {
			if (!config.equals(active)) {
				desc.setActiveBuildConfig(config.getName());
				project.setDescription(desc, createTestMonitor());
				return config;
			}
		}
		throw new IllegalStateException(
				"No build config other than the active one could be found for project: " + project);
	}

	/**
	 * p0 --&gt; p1 --&gt; p2
	 */
	private void setupSimpleReferences() throws CoreException {
		setReferences(project0.getActiveBuildConfig(), new IBuildConfiguration[] {project1.getActiveBuildConfig()});
		setReferences(project1.getActiveBuildConfig(), new IBuildConfiguration[] {project2.getActiveBuildConfig()});
		setReferences(project2.getActiveBuildConfig(), new IBuildConfiguration[] {});
	}

	/**
	 * Helper method to set the references for a project.
	 */
	private void setReferences(IBuildConfiguration variant, IBuildConfiguration[] refs) throws CoreException {
		IProjectDescription desc = variant.getProject().getDescription();
		desc.setBuildConfigReferences(variant.getName(), refs);
		variant.getProject().setDescription(desc, createTestMonitor());
	}

	/**
	 * Setup a reference graph, then test the build context for for each project involved
	 * in the 'build'.
	 */
	@Test
	public void testBuildContext() {
		// Create reference graph
		IBuildConfiguration p0v0 = getWorkspace().newBuildConfig(project0.getName(), variant0);
		IBuildConfiguration p0v1 = getWorkspace().newBuildConfig(project0.getName(), variant1);
		IBuildConfiguration p1v0 = getWorkspace().newBuildConfig(project1.getName(), variant0);

		// Create build order
		final IBuildConfiguration[] buildOrder = new IBuildConfiguration[] {p0v0, p0v1, p1v0};

		IBuildContext context;

		context = new BuildContext(p0v0, new IBuildConfiguration[] {p0v0, p1v0}, buildOrder);
		assertThat(context.getAllReferencedBuildConfigs()).isEmpty();
		assertThat(context.getAllReferencingBuildConfigs()).containsExactly(p0v1, p1v0);
		assertThat(context.getRequestedConfigs()).containsExactly(p0v0, p1v0);

		context = new BuildContext(p0v1, buildOrder, buildOrder);
		assertThat(context.getAllReferencedBuildConfigs()).containsExactly(p0v0);
		assertThat(context.getAllReferencingBuildConfigs()).containsExactly(p1v0);

		context = new BuildContext(p1v0, buildOrder, buildOrder);
		assertThat(context.getAllReferencedBuildConfigs()).containsExactly(p0v0, p0v1);
		assertThat(context.getAllReferencingBuildConfigs()).isEmpty();

		// And it works with no build context too
		context = new BuildContext(p1v0);
		assertThat(context.getAllReferencedBuildConfigs()).isEmpty();
		assertThat(context.getAllReferencingBuildConfigs()).isEmpty();
	}

	@Test
	public void testSingleProjectBuild() throws CoreException {
		setAutoBuilding(true);

		setupSimpleReferences();
		ContextBuilder.clearStats();
		project0.build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());
		ContextBuilder.assertValid();

		IBuildContext context = ContextBuilder.getContext(project0.getActiveBuildConfig());
		assertThat(context.getAllReferencedBuildConfigs()).isEmpty();
		assertThat(context.getAllReferencingBuildConfigs()).isEmpty();

		// Change the active build configuration will cause the project to be rebuilt
		ContextBuilder.clearStats();
		IBuildConfiguration newActive = changeActiveBuildConfig(project0);
		waitForBuild();
		ContextBuilder.assertValid();

		context = ContextBuilder.getContext(newActive);
		assertThat(context.getAllReferencedBuildConfigs()).isEmpty();
	}

	/**
	 * Tests building a single project with and without references
	 */
	@Test
	public void testWorkspaceBuildProject() throws CoreException {
		setupSimpleReferences();
		ContextBuilder.clearStats();

		// Build project and resolve references
		getWorkspace().build(new IBuildConfiguration[] {project0.getActiveBuildConfig()}, IncrementalProjectBuilder.FULL_BUILD, true, createTestMonitor());
		ContextBuilder.assertValid();

		IBuildContext context = ContextBuilder.getContext(project0.getActiveBuildConfig());
		assertThat(context.getAllReferencedBuildConfigs()).containsExactly(project2.getActiveBuildConfig(),
				project1.getActiveBuildConfig());
		assertThat(context.getAllReferencingBuildConfigs()).isEmpty();

		context = ContextBuilder.getBuilder(project1.getActiveBuildConfig()).contextForLastBuild;
		assertThat(context.getAllReferencedBuildConfigs()).containsExactly(project2.getActiveBuildConfig());
		assertThat(context.getAllReferencingBuildConfigs()).containsExactly(project0.getActiveBuildConfig());

		context = ContextBuilder.getBuilder(project2.getActiveBuildConfig()).contextForLastBuild;
		assertThat(context.getAllReferencedBuildConfigs()).isEmpty();
		assertThat(context.getAllReferencingBuildConfigs()).containsExactly(project1.getActiveBuildConfig(),
				project0.getActiveBuildConfig());

		// Build just project0
		ContextBuilder.clearStats();
		getWorkspace().build(new IBuildConfiguration[] {project0.getActiveBuildConfig()}, IncrementalProjectBuilder.FULL_BUILD, false, createTestMonitor());
		ContextBuilder.assertValid();

		context = ContextBuilder.getContext(project0.getActiveBuildConfig());
		assertThat(context.getAllReferencedBuildConfigs()).isEmpty();
		assertThat(context.getAllReferencingBuildConfigs()).isEmpty();
	}

	/**
	 * Builds a couple configurations, including references
	 */
	@Test
	public void testWorkspaceBuildProjects() throws CoreException {
		setupSimpleReferences();
		ContextBuilder.clearStats();
		// build project0 & project2 ; project1 will end up being built too.
		getWorkspace().build(new IBuildConfiguration[] {project0.getActiveBuildConfig(), project2.getActiveBuildConfig()}, IncrementalProjectBuilder.FULL_BUILD, true, createTestMonitor());
		ContextBuilder.assertValid();

		IBuildContext context = ContextBuilder.getContext(project0.getActiveBuildConfig());
		assertThat(context.getAllReferencedBuildConfigs()).containsExactly(project2.getActiveBuildConfig(), project1.getActiveBuildConfig());
		assertThat(context.getAllReferencingBuildConfigs()).isEmpty();

		context = ContextBuilder.getBuilder(project1.getActiveBuildConfig()).contextForLastBuild;
		assertThat(context.getAllReferencedBuildConfigs()).containsExactly(project2.getActiveBuildConfig());
		assertThat(context.getAllReferencingBuildConfigs()).containsExactly(project0.getActiveBuildConfig());

		context = ContextBuilder.getBuilder(project2.getActiveBuildConfig()).contextForLastBuild;
		assertThat(context.getAllReferencedBuildConfigs()).isEmpty();
		assertThat(context.getAllReferencingBuildConfigs()).containsExactly(project1.getActiveBuildConfig(), project0.getActiveBuildConfig());
	}

	/**
	 * Sets references to the 'active' project build configuration
	 */
	@Test
	public void testReferenceActiveVariant() throws CoreException {
		setReferences(project0.getActiveBuildConfig(), new IBuildConfiguration[] {getWorkspace().newBuildConfig(project1.getName(), null)});
		setReferences(project1.getActiveBuildConfig(), new IBuildConfiguration[] {getWorkspace().newBuildConfig(project2.getName(), null)});
		setReferences(project2.getActiveBuildConfig(), new IBuildConfiguration[] {});

		ContextBuilder.clearStats();
		getWorkspace().build(new IBuildConfiguration[] {project0.getActiveBuildConfig()}, IncrementalProjectBuilder.FULL_BUILD, true, createTestMonitor());
		ContextBuilder.assertValid();

		IBuildContext context = ContextBuilder.getContext(project0.getActiveBuildConfig());
		assertThat(context.getAllReferencedBuildConfigs()).containsExactly(project2.getActiveBuildConfig(),
				project1.getActiveBuildConfig());
		assertThat(context.getAllReferencingBuildConfigs()).isEmpty();

		context = ContextBuilder.getBuilder(project1.getActiveBuildConfig()).contextForLastBuild;
		assertThat(context.getAllReferencedBuildConfigs()).containsExactly(project2.getActiveBuildConfig());
		assertThat(context.getAllReferencingBuildConfigs()).containsExactly(project0.getActiveBuildConfig());

		context = ContextBuilder.getBuilder(project2.getActiveBuildConfig()).contextForLastBuild;
		assertThat(context.getAllReferencedBuildConfigs()).isEmpty();
		assertThat(context.getAllReferencingBuildConfigs()).containsExactly(project1.getActiveBuildConfig(),
				project0.getActiveBuildConfig());
	}

	/**
	 * Attempts to build a project that references the active variant of another project,
	 * and the same variant directly. This should only result in one referenced variant being built.
	 */
	@Test
	public void testReferenceVariantTwice() throws CoreException {
		IBuildConfiguration ref1 = new BuildConfiguration(project1, null);
		IBuildConfiguration ref2 = new BuildConfiguration(project1, project1.getActiveBuildConfig().getName());
		setReferences(project0.getActiveBuildConfig(), new IBuildConfiguration[] {ref1, ref2});
		setReferences(project1.getActiveBuildConfig(), new IBuildConfiguration[] {});

		ContextBuilder.clearStats();
		getWorkspace().build(new IBuildConfiguration[] {project0.getActiveBuildConfig()}, IncrementalProjectBuilder.FULL_BUILD, true, createTestMonitor());
		ContextBuilder.assertValid();

		IBuildContext context = ContextBuilder.getContext(project0.getActiveBuildConfig());
		assertThat(context.getAllReferencedBuildConfigs()).containsExactly(project1.getActiveBuildConfig());
		assertThat(context.getAllReferencingBuildConfigs()).isEmpty();
		assertThat(context.getRequestedConfigs()).containsExactly(project0.getActiveBuildConfig());

		context = ContextBuilder.getBuilder(project1.getActiveBuildConfig()).contextForLastBuild;
		assertThat(context.getAllReferencedBuildConfigs()).isEmpty();
		assertThat(context.getAllReferencingBuildConfigs()).containsExactly(project0.getActiveBuildConfig());

		// Change the active configuration of project1, and test that two configurations are built
		ContextBuilder.clearStats();
		IBuildConfiguration project1PreviousActive = project1.getActiveBuildConfig();
		IBuildConfiguration project1NewActive = changeActiveBuildConfig(project1);
		getWorkspace().build(new IBuildConfiguration[] {project0.getActiveBuildConfig()}, IncrementalProjectBuilder.FULL_BUILD, true, createTestMonitor());
		ContextBuilder.assertValid();

		context = ContextBuilder.getContext(project0.getActiveBuildConfig());
		assertThat(context.getAllReferencedBuildConfigs()).containsExactly(project1PreviousActive, project1NewActive);
		assertThat(context.getAllReferencingBuildConfigs()).isEmpty();
		assertThat(context.getRequestedConfigs()).containsExactly(project0.getActiveBuildConfig());
	}
}
