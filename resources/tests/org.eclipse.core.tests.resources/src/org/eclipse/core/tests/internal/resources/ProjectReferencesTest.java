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

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;

import org.eclipse.core.internal.resources.BuildConfiguration;
import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.ResourceTest;

/**
 * Test project variant references
 */
public class ProjectReferencesTest extends ResourceTest {

	private IProject project0;
	private IProject project1;
	private IProject project2;
	private IProject project3;
	private IBuildConfiguration project0v0;
	private IBuildConfiguration project0v1;
	private IBuildConfiguration project1v0;
	private IBuildConfiguration project1v1;
	private IBuildConfiguration project2v0;
	private IBuildConfiguration project3v0;
	private IBuildConfiguration project3v1;
	private static final String bc0 = "Variant0";
	private static final String bc1 = "Variant1";
	private static final String nonExistentBC = "foo";

	@Override
	public void setUp() throws Exception {
		super.setUp();
		project0 = getWorkspace().getRoot().getProject("ProjectReferencesTest_p0");
		project1 = getWorkspace().getRoot().getProject("ProjectReferencesTest_p1");
		project2 = getWorkspace().getRoot().getProject("ProjectReferencesTest_p2");
		project3 = getWorkspace().getRoot().getProject("ProjectReferencesTest_p3");
		createInWorkspace(new IProject[] {project0, project1, project2, project3});
		setUpVariants(project0);
		setUpVariants(project1);
		setUpVariants(project2);
		setUpVariants(project3);
		project0v0 = new BuildConfiguration(project0, bc0);
		project0v1 = new BuildConfiguration(project0, bc1);
		project1v0 = new BuildConfiguration(project1, bc0);
		project1v1 = new BuildConfiguration(project1, bc1);
		project2v0 = new BuildConfiguration(project2, bc0);
		project3v0 = new BuildConfiguration(project3, bc0);
		project3v1 = new BuildConfiguration(project3, bc1);
	}

	/**
	 * Returns a reference to the active build configuration
	 */
	private IBuildConfiguration getRef(IProject project) {
		return new BuildConfiguration(project, null);
	}

	/**
	 * Create 2 build configurations bc0 and bc1 on each project
	 */
	private void setUpVariants(IProject project) throws CoreException {
		IProjectDescription desc = project.getDescription();
		desc.setBuildConfigs(new String[] {bc0, bc1});
		project.setDescription(desc, createTestMonitor());
	}

	public void testAddReferencesToNonExistantConfigs() throws CoreException {
		IProjectDescription desc = project0.getDescription();

		assertThat("project '" + project0 + "' has unexpected build config: " + nonExistentBC,
				!project0.hasBuildConfig(nonExistentBC));

		desc.setBuildConfigReferences(nonExistentBC, new IBuildConfiguration[] {project1v0});
		project0.setDescription(desc, createTestMonitor());

		assertThat("project '" + project0 + "' has unexpected build config: " + nonExistentBC,
				!project0.hasBuildConfig(nonExistentBC));

		assertThat(desc.getBuildConfigReferences(nonExistentBC), emptyArray());
		assertThrows(CoreException.class, () -> project0.getReferencedBuildConfigs(nonExistentBC, true));
	}

	/**
	 * Tests that setting re-setting build configurations doesn't perturb the existing
	 * configuration level references.
	 *
	 * Removing a build configuration removes associated build configuration references
	 */
	public void testChangingBuildConfigurations() throws CoreException {
		IProjectDescription desc = project0.getDescription();
		IBuildConfiguration[] refs = new IBuildConfiguration[] {project0v1, project1v0};
		IBuildConfiguration[] refs2 = new IBuildConfiguration[] {project1v1, project1v0};

		// Set some references
		desc.setBuildConfigReferences(project0v0.getName(), refs);
		desc.setBuildConfigReferences(project0v1.getName(), refs2);
		project0.setDescription(desc, createTestMonitor());

		// Check build configa
		desc = project0.getDescription();
		assertThat(desc.getBuildConfigReferences(project0v0.getName()), is(refs));
		assertThat(desc.getBuildConfigReferences(project0v1.getName()), is(refs2));
		// Resetting the build configs doesn't change anything
		desc.setBuildConfigs(new String[] {project0v0.getName(), project0v1.getName()});
		project0.setDescription(desc, createTestMonitor());

		desc = project0.getDescription();
		assertThat(desc.getBuildConfigReferences(project0v0.getName()), is(refs));
		assertThat(desc.getBuildConfigReferences(project0v1.getName()), is(refs2));
		// Removing a build configuration removes the references
		desc.setBuildConfigs(new String[] {project0v0.getName()});
		project0.setDescription(desc, createTestMonitor());

		desc = project0.getDescription();
		assertThat(desc.getBuildConfigReferences(project0v0.getName()), is(refs));
		assertThat(desc.getBuildConfigReferences(project0v1.getName()), emptyArray());
		// Re-adding a build configuration doesn't make references re-appear
		desc.setBuildConfigs(new String[] {project0v0.getName()});
		project0.setDescription(desc, createTestMonitor());

		desc = project0.getDescription();
		assertThat(desc.getBuildConfigReferences(project0v0.getName()), is(refs));
		assertThat(desc.getBuildConfigReferences(project0v1.getName()), emptyArray());
	}

	/**
	 * Tests that setting build configuration level dynamic references
	 * trumps the project level dynamic references when it comes to order.
	 */
	public void testMixedProjectAndBuildConfigRefs() throws CoreException {
		// Set project variant references
		IProjectDescription desc = project0.getDescription();
		desc.setDynamicReferences(new IProject[] {project1, project3});
		project0.setDescription(desc, createTestMonitor());

		// Check getters
		desc = project0.getDescription();
		assertThat(desc.getDynamicReferences(), arrayContaining(project1, project3));
		assertThat(desc.getBuildConfigReferences(project0v0.getName()), emptyArray());
		assertThat(desc.getBuildConfigReferences(project0v1.getName()), emptyArray());
		assertThat(project0.getReferencedBuildConfigs(project0v0.getName(), false),
				arrayContaining(project1.getActiveBuildConfig(), project3.getActiveBuildConfig()));
		assertThat(project0.getReferencedBuildConfigs(project0v1.getName(), false),
				arrayContaining(project1.getActiveBuildConfig(), project3.getActiveBuildConfig()));

		// Now set dynamic references on config1
		desc.setBuildConfigReferences(project0v0.getName(), new IBuildConfiguration[] {project3v1, project2v0, project1v0});
		project0.setDescription(desc, createTestMonitor());

		// Check references
		// This is deterministic as config0 is listed first, so we expect its config order to trump cofig1's
		desc = project0.getDescription();
		assertThat(desc.getDynamicReferences(), arrayContaining(project1, project3));
		assertThat(desc.getBuildConfigReferences(project0v0.getName()),
				arrayContaining(project3v1, project2v0, project1v0));
		// Now at the project leve
		assertThat(project0.getReferencedBuildConfigs(project0v0.getName(), false),
				arrayContaining(project3v1, project2v0, project1v0, project3v0));
		assertThat(project0.getReferencedBuildConfigs(project0v1.getName(), false),
				arrayContaining(project1.getActiveBuildConfig(), project3.getActiveBuildConfig()));
	}

	public void testSetAndGetProjectReferences() throws CoreException {
		// Set project references
		IProjectDescription desc = project0.getDescription();
		desc.setReferencedProjects(new IProject[] {project3, project1});
		desc.setDynamicReferences(new IProject[] {project1, project2});
		project0.setDescription(desc, createTestMonitor());

		desc = project1.getDescription();
		desc.setReferencedProjects(new IProject[] {project0});
		desc.setDynamicReferences(new IProject[] {});
		project1.setDescription(desc, createTestMonitor());

		desc = project2.getDescription();
		desc.setReferencedProjects(new IProject[] {});
		desc.setDynamicReferences(new IProject[] {});
		project2.setDescription(desc, createTestMonitor());

		desc = project3.getDescription();
		desc.setReferencedProjects(new IProject[] {});
		desc.setDynamicReferences(new IProject[] {project0});
		project3.setDescription(desc, createTestMonitor());

		// Test getters
		desc = project0.getDescription();
		assertThat(desc.getReferencedProjects(), arrayContaining(project3, project1));
		assertThat(desc.getDynamicReferences(), arrayContaining(project1, project2));
		assertThat(desc.getBuildConfigReferences(bc0), emptyArray());

		assertThat(project0.getReferencedProjects(), arrayContaining(project3, project1, project2));
		assertThat(project0.getReferencingProjects(), arrayContaining(project1, project3));
		assertThat(project0.getReferencedBuildConfigs(project0v0.getName(), true),
				arrayContaining(project3v0, project1v0, project2v0));
	}

	public void testSetAndGetProjectConfigReferences() throws CoreException {
		// Set project variant references
		IProjectDescription desc = project0.getDescription();
		// 1 static reference
		desc.setReferencedProjects(new IProject[] {project1});
		// 1 dynamic project-level reference
		desc.setDynamicReferences(new IProject[] {project3});
		// config level references
		desc.setBuildConfigReferences(bc0, new IBuildConfiguration[] {project2v0, project1v0});
		desc.setBuildConfigReferences(bc1, new IBuildConfiguration[] {project2v0});
		project0.setDescription(desc, createTestMonitor());

		desc = project1.getDescription();
		desc.setReferencedProjects(new IProject[] {project0});
		desc.setBuildConfigReferences(bc0, new IBuildConfiguration[] {project0v1});
		desc.setBuildConfigReferences(bc1, new IBuildConfiguration[] {});
		project1.setDescription(desc, createTestMonitor());

		desc = project3.getDescription();
		desc.setBuildConfigReferences(bc0, new IBuildConfiguration[] {project0v1});
		desc.setBuildConfigReferences(bc1, new IBuildConfiguration[] {});
		project3.setDescription(desc, createTestMonitor());

		// Check getters
		desc = project0.getDescription();
		assertThat(desc.getReferencedProjects(), arrayContaining(project1));
		assertThat(desc.getDynamicReferences(), arrayContaining(project3));
		assertThat(desc.getBuildConfigReferences(bc0), arrayContaining(project2v0, project1v0));
		assertThat(desc.getBuildConfigReferences(bc1), arrayContaining(project2v0));

		assertThat(project0.getReferencedProjects(), arrayContaining(project2, project1, project3));
		assertThat(project0.getReferencingProjects(), arrayContaining(project1, project3));
		assertThat(project0.getReferencedBuildConfigs(project0v0.getName(), true),
				arrayContaining(project2v0, project1v0, project3.getActiveBuildConfig()));
		assertThat(project0.getReferencedBuildConfigs(project0v1.getName(), true), arrayContaining(
				project2v0, project1.getActiveBuildConfig(), project3.getActiveBuildConfig()));
	}

	public void testReferencesToActiveConfigs() throws CoreException {
		IProjectDescription desc = project0.getDescription();
		desc.setBuildConfigReferences(bc0, new IBuildConfiguration[] {getRef(project1)});
		project0.setDescription(desc, createTestMonitor());

		assertThat(desc.getBuildConfigReferences(bc0), arrayContaining(getRef(project1)));
		assertThat(project0.getReferencedBuildConfigs(project0v0.getName(), true),
				arrayContaining(project1v0));
	}
}
