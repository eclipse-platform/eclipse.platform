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

import org.eclipse.core.internal.events.BuildContext;
import org.eclipse.core.internal.resources.BuildConfiguration;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;

/**
 * These tests exercise the build context functionality that tells a builder in what context
 * it was called.
 */
public class BuildContextTest extends AbstractBuilderTest {

	private IProject project0;
	private IProject project1;
	private IProject project2;
	private static final String variant0 = "Variant0";
	private static final String variant1 = "Variant1";

	public BuildContextTest(String name) {
		super(name);
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		// Create resources
		IWorkspaceRoot root = getWorkspace().getRoot();
		project0 = root.getProject("BuildContextTests_p0");
		project1 = root.getProject("BuildContextTests_p1");
		project2 = root.getProject("BuildContextTests_p2");
		IResource[] resources = {project0, project1, project2};
		ensureExistsInWorkspace(resources, true);
		setAutoBuilding(false);
		setupProject(project0);
		setupProject(project1);
		setupProject(project2);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();

		// Cleanup
		project0.delete(true, null);
		project1.delete(true, null);
		project2.delete(true, null);
	}

	/**
	 * Helper method to configure a project with a build command and several buildConfigs.
	 */
	private void setupProject(IProject project) throws CoreException {
		IProjectDescription desc = project.getDescription();

		// Add build command
		ICommand command = createCommand(desc, ContextBuilder.BUILDER_NAME, "Build0");
		command.setBuilding(IncrementalProjectBuilder.AUTO_BUILD, true);
		command.setBuilding(IncrementalProjectBuilder.FULL_BUILD, true);
		command.setBuilding(IncrementalProjectBuilder.INCREMENTAL_BUILD, true);
		command.setBuilding(IncrementalProjectBuilder.CLEAN_BUILD, true);
		desc.setBuildSpec(new ICommand[] {command});

		// Create buildConfigs
		desc.setBuildConfigs(new String[] {variant0, variant1});

		project.setDescription(desc, getMonitor());
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
				project.setDescription(desc, getMonitor());
				return config;
			}
		}
		assertTrue(false);
		return null;
	}

	/**
	 * p0 --&gt; p1 --&gt; p2
	 * @throws CoreException
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
		variant.getProject().setDescription(desc, getMonitor());
	}

	/**
	 * Setup a reference graph, then test the build context for for each project involved
	 * in the 'build'.
	 */
	public void testBuildContext() {
		// Create reference graph
		IBuildConfiguration p0v0 = getWorkspace().newBuildConfig(project0.getName(), variant0);
		IBuildConfiguration p0v1 = getWorkspace().newBuildConfig(project0.getName(), variant1);
		IBuildConfiguration p1v0 = getWorkspace().newBuildConfig(project1.getName(), variant0);

		// Create build order
		final IBuildConfiguration[] buildOrder = new IBuildConfiguration[] {p0v0, p0v1, p1v0};

		IBuildContext context;

		context = new BuildContext(p0v0, new IBuildConfiguration[] {p0v0, p1v0}, buildOrder);
		assertEquals("1.0", new IBuildConfiguration[] {}, context.getAllReferencedBuildConfigs());
		assertEquals("1.1", new IBuildConfiguration[] {p0v1, p1v0}, context.getAllReferencingBuildConfigs());
		assertEquals("1.2", new IBuildConfiguration[] {p0v0, p1v0}, context.getRequestedConfigs());

		context = new BuildContext(p0v1, buildOrder, buildOrder);
		assertEquals("2.0", new IBuildConfiguration[] {p0v0}, context.getAllReferencedBuildConfigs());
		assertEquals("2.1", new IBuildConfiguration[] {p1v0}, context.getAllReferencingBuildConfigs());

		context = new BuildContext(p1v0, buildOrder, buildOrder);
		assertEquals("3.0", new IBuildConfiguration[] {p0v0, p0v1}, context.getAllReferencedBuildConfigs());
		assertEquals("3.1", new IBuildConfiguration[] {}, context.getAllReferencingBuildConfigs());

		// And it works with no build context too
		context = new BuildContext(p1v0);
		assertEquals("4.0", new IBuildConfiguration[] {}, context.getAllReferencedBuildConfigs());
		assertEquals("4.1", new IBuildConfiguration[] {}, context.getAllReferencingBuildConfigs());
	}

	public void testSingleProjectBuild() throws CoreException {
		setAutoBuilding(true);

		setupSimpleReferences();
		ContextBuilder.clearStats();
		project0.build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());
		assertTrue("1.0", ContextBuilder.checkValid());

		IBuildContext context = ContextBuilder.getContext(project0.getActiveBuildConfig());
		assertEquals("2.0", 0, context.getAllReferencedBuildConfigs().length);
		assertEquals("2.1", 0, context.getAllReferencingBuildConfigs().length);

		// Change the active build configuration will cause the project to be rebuilt
		ContextBuilder.clearStats();
		IBuildConfiguration newActive = changeActiveBuildConfig(project0);
		waitForBuild();
		assertTrue("3.0", ContextBuilder.checkValid());

		context = ContextBuilder.getContext(newActive);
		assertEquals("3.1", 0, context.getAllReferencedBuildConfigs().length);
	}

	/**
	 * Tests building a single project with and without references
	 * @throws CoreException
	 */
	public void testWorkspaceBuildProject() throws CoreException {
		setupSimpleReferences();
		ContextBuilder.clearStats();

		// Build project and resolve references
		getWorkspace().build(new IBuildConfiguration[] {project0.getActiveBuildConfig()}, IncrementalProjectBuilder.FULL_BUILD, true, getMonitor());
		assertTrue("1.0", ContextBuilder.checkValid());

		IBuildContext context = ContextBuilder.getContext(project0.getActiveBuildConfig());
		assertEquals("2.0", new IBuildConfiguration[] {project2.getActiveBuildConfig(), project1.getActiveBuildConfig()}, context.getAllReferencedBuildConfigs());
		assertEquals("2.1", 0, context.getAllReferencingBuildConfigs().length);

		context = ContextBuilder.getBuilder(project1.getActiveBuildConfig()).contextForLastBuild;
		assertEquals("3.0", new IBuildConfiguration[] {project2.getActiveBuildConfig()}, context.getAllReferencedBuildConfigs());
		assertEquals("3.1", new IBuildConfiguration[] {project0.getActiveBuildConfig()}, context.getAllReferencingBuildConfigs());

		context = ContextBuilder.getBuilder(project2.getActiveBuildConfig()).contextForLastBuild;
		assertEquals("4.0", 0, context.getAllReferencedBuildConfigs().length);
		assertEquals("4.1", new IBuildConfiguration[] {project1.getActiveBuildConfig(), project0.getActiveBuildConfig()}, context.getAllReferencingBuildConfigs());

		// Build just project0
		ContextBuilder.clearStats();
		getWorkspace().build(new IBuildConfiguration[] {project0.getActiveBuildConfig()}, IncrementalProjectBuilder.FULL_BUILD, false, getMonitor());
		assertTrue("5.0", ContextBuilder.checkValid());

		context = ContextBuilder.getContext(project0.getActiveBuildConfig());
		assertTrue("5.1", context.getAllReferencedBuildConfigs().length == 0);
		assertTrue("5.2", context.getAllReferencingBuildConfigs().length == 0);
	}

	/**
	 * Builds a couple configurations, including references
	 * @throws CoreException
	 */
	public void testWorkspaceBuildProjects() throws CoreException {
		setupSimpleReferences();
		ContextBuilder.clearStats();
		// build project0 & project2 ; project1 will end up being built too.
		getWorkspace().build(new IBuildConfiguration[] {project0.getActiveBuildConfig(), project2.getActiveBuildConfig()}, IncrementalProjectBuilder.FULL_BUILD, true, getMonitor());
		assertTrue("1.0", ContextBuilder.checkValid());

		IBuildContext context = ContextBuilder.getContext(project0.getActiveBuildConfig());
		assertEquals("2.0", new IBuildConfiguration[] {project2.getActiveBuildConfig(), project1.getActiveBuildConfig()}, context.getAllReferencedBuildConfigs());
		assertEquals("2.1", 0, context.getAllReferencingBuildConfigs().length);

		context = ContextBuilder.getBuilder(project1.getActiveBuildConfig()).contextForLastBuild;
		assertEquals("3.0", new IBuildConfiguration[] {project2.getActiveBuildConfig()}, context.getAllReferencedBuildConfigs());
		assertEquals("3.1", new IBuildConfiguration[] {project0.getActiveBuildConfig()}, context.getAllReferencingBuildConfigs());

		context = ContextBuilder.getBuilder(project2.getActiveBuildConfig()).contextForLastBuild;
		assertEquals("4.0", 0, context.getAllReferencedBuildConfigs().length);
		assertEquals("4.1", new IBuildConfiguration[] {project1.getActiveBuildConfig(), project0.getActiveBuildConfig()}, context.getAllReferencingBuildConfigs());
	}

	/**
	 * Sets references to the 'active' project build configuration
	 * @throws CoreException
	 */
	public void testReferenceActiveVariant() throws CoreException {
		setReferences(project0.getActiveBuildConfig(), new IBuildConfiguration[] {getWorkspace().newBuildConfig(project1.getName(), null)});
		setReferences(project1.getActiveBuildConfig(), new IBuildConfiguration[] {getWorkspace().newBuildConfig(project2.getName(), null)});
		setReferences(project2.getActiveBuildConfig(), new IBuildConfiguration[] {});

		ContextBuilder.clearStats();
		getWorkspace().build(new IBuildConfiguration[] {project0.getActiveBuildConfig()}, IncrementalProjectBuilder.FULL_BUILD, true, getMonitor());
		assertTrue("1.0", ContextBuilder.checkValid());

		IBuildContext context = ContextBuilder.getContext(project0.getActiveBuildConfig());
		assertEquals("2.0", new IBuildConfiguration[] {project2.getActiveBuildConfig(), project1.getActiveBuildConfig()}, context.getAllReferencedBuildConfigs());
		assertEquals("2.1", 0, context.getAllReferencingBuildConfigs().length);

		context = ContextBuilder.getBuilder(project1.getActiveBuildConfig()).contextForLastBuild;
		assertEquals("3.0", new IBuildConfiguration[] {project2.getActiveBuildConfig()}, context.getAllReferencedBuildConfigs());
		assertEquals("3.1", new IBuildConfiguration[] {project0.getActiveBuildConfig()}, context.getAllReferencingBuildConfigs());

		context = ContextBuilder.getBuilder(project2.getActiveBuildConfig()).contextForLastBuild;
		assertEquals("4.0", 0, context.getAllReferencedBuildConfigs().length);
		assertEquals("4.1", new IBuildConfiguration[] {project1.getActiveBuildConfig(), project0.getActiveBuildConfig()}, context.getAllReferencingBuildConfigs());
	}

	/**
	 * Attempts to build a project that references the active variant of another project,
	 * and the same variant directly. This should only result in one referenced variant being built.
	 */
	public void testReferenceVariantTwice() throws CoreException {
		IBuildConfiguration ref1 = new BuildConfiguration(project1, null);
		IBuildConfiguration ref2 = new BuildConfiguration(project1, project1.getActiveBuildConfig().getName());
		setReferences(project0.getActiveBuildConfig(), new IBuildConfiguration[] {ref1, ref2});
		setReferences(project1.getActiveBuildConfig(), new IBuildConfiguration[] {});

		ContextBuilder.clearStats();
		getWorkspace().build(new IBuildConfiguration[] {project0.getActiveBuildConfig()}, IncrementalProjectBuilder.FULL_BUILD, true, getMonitor());
		assertTrue("1.0", ContextBuilder.checkValid());

		IBuildContext context = ContextBuilder.getContext(project0.getActiveBuildConfig());
		assertEquals("2.0", new IBuildConfiguration[] {project1.getActiveBuildConfig()}, context.getAllReferencedBuildConfigs());
		assertEquals("2.1", 0, context.getAllReferencingBuildConfigs().length);
		assertEquals("2.2", new IBuildConfiguration[] {project0.getActiveBuildConfig()}, context.getRequestedConfigs());

		context = ContextBuilder.getBuilder(project1.getActiveBuildConfig()).contextForLastBuild;
		assertEquals("3.0", 0, context.getAllReferencedBuildConfigs().length);
		assertEquals("3.1", new IBuildConfiguration[] {project0.getActiveBuildConfig()}, context.getAllReferencingBuildConfigs());

		// Change the active configuration of project1, and test that two configurations are built
		ContextBuilder.clearStats();
		IBuildConfiguration project1PreviousActive = project1.getActiveBuildConfig();
		IBuildConfiguration project1NewActive = changeActiveBuildConfig(project1);
		getWorkspace().build(new IBuildConfiguration[] {project0.getActiveBuildConfig()}, IncrementalProjectBuilder.FULL_BUILD, true, getMonitor());
		assertTrue("4.0", ContextBuilder.checkValid());

		context = ContextBuilder.getContext(project0.getActiveBuildConfig());
		assertEquals("4.1", new IBuildConfiguration[] {project1PreviousActive, project1NewActive}, context.getAllReferencedBuildConfigs());
		assertEquals("4.2", 0, context.getAllReferencingBuildConfigs().length);
		assertEquals("4.3", new IBuildConfiguration[] {project0.getActiveBuildConfig()}, context.getRequestedConfigs());
	}
}
