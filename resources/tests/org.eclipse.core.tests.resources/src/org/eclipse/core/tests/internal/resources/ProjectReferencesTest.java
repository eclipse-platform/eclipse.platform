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
		project0 = getWorkspace().getRoot().getProject("ProjectReferencesTest_p0");
		project1 = getWorkspace().getRoot().getProject("ProjectReferencesTest_p1");
		project2 = getWorkspace().getRoot().getProject("ProjectReferencesTest_p2");
		project3 = getWorkspace().getRoot().getProject("ProjectReferencesTest_p3");
		ensureExistsInWorkspace(new IProject[] {project0, project1, project2, project3}, true);
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

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();

		// clean-up resources
		project0.delete(true, null);
		project1.delete(true, null);
		project2.delete(true, null);
		project3.delete(true, null);
	}

	/**
	 * Returns a reference to the active build configuration
	 * @param project
	 * @return
	 */
	private IBuildConfiguration getRef(IProject project) {
		return new BuildConfiguration(project, null);
	}

	/**
	 * Create 2 build configurations bc0 and bc1 on each project
	 * @param project
	 * @throws CoreException
	 */
	private void setUpVariants(IProject project) throws CoreException {
		IProjectDescription desc = project.getDescription();
		desc.setBuildConfigs(new String[] {bc0, bc1});
		project.setDescription(desc, getMonitor());
	}

	public void testAddReferencesToNonExistantConfigs() throws CoreException {
		IProjectDescription desc = project0.getDescription();

		assertFalse("1.0", project0.hasBuildConfig(nonExistentBC));

		desc.setBuildConfigReferences(nonExistentBC, new IBuildConfiguration[] {project1v0});
		project0.setDescription(desc, getMonitor());

		assertFalse("2.0", project0.hasBuildConfig(nonExistentBC));

		assertEquals("3.1", new IBuildConfiguration[0], desc.getBuildConfigReferences(nonExistentBC));
		try {
			project0.getReferencedBuildConfigs(nonExistentBC, true);
			fail("3.2");
		} catch (CoreException e) {
		}
	}

	/**
	 * Tests that setting re-setting build configurations doesn't perturb the existing
	 * configuration level references.
	 *
	 * Removing a build configuration removes associated build configuration references
	 * @throws CoreException
	 */
	public void testChangingBuildConfigurations() throws CoreException {
		IProjectDescription desc = project0.getDescription();
		IBuildConfiguration[] refs = new IBuildConfiguration[] {project0v1, project1v0};
		IBuildConfiguration[] refs2 = new IBuildConfiguration[] {project1v1, project1v0};

		// Set some references
		desc.setBuildConfigReferences(project0v0.getName(), refs);
		desc.setBuildConfigReferences(project0v1.getName(), refs2);
		project0.setDescription(desc, getMonitor());

		// Check build configa
		desc = project0.getDescription();
		assertEquals("1.0", refs, desc.getBuildConfigReferences(project0v0.getName()));
		assertEquals("1.1", refs2, desc.getBuildConfigReferences(project0v1.getName()));
		// Resetting the build configs doesn't change anything
		desc.setBuildConfigs(new String[] {project0v0.getName(), project0v1.getName()});
		project0.setDescription(desc, getMonitor());

		desc = project0.getDescription();
		assertEquals("2.0", refs, desc.getBuildConfigReferences(project0v0.getName()));
		assertEquals("2.1", refs2, desc.getBuildConfigReferences(project0v1.getName()));
		// Removing a build configuration removes the references
		desc.setBuildConfigs(new String[] {project0v0.getName()});
		project0.setDescription(desc, getMonitor());

		desc = project0.getDescription();
		assertEquals("3.0", refs, desc.getBuildConfigReferences(project0v0.getName()));
		assertEquals("3.1", new IBuildConfiguration[0], desc.getBuildConfigReferences(project0v1.getName()));
		// Re-adding a build configuration doesn't make references re-appear
		desc.setBuildConfigs(new String[] {project0v0.getName()});
		project0.setDescription(desc, getMonitor());

		desc = project0.getDescription();
		assertEquals("4.0", refs, desc.getBuildConfigReferences(project0v0.getName()));
		assertEquals("4.1", new IBuildConfiguration[0], desc.getBuildConfigReferences(project0v1.getName()));
	}

	/**
	 * Tests that setting build configuration level dynamic references
	 * trumps the project level dynamic references when it comes to order.
	 * @throws CoreException
	 */
	public void testMixedProjectAndBuildConfigRefs() throws CoreException {
		// Set project variant references
		IProjectDescription desc = project0.getDescription();
		desc.setDynamicReferences(new IProject[] {project1, project3});
		project0.setDescription(desc, getMonitor());

		// Check getters
		desc = project0.getDescription();
		assertEquals("1.1", new IProject[] {project1, project3}, desc.getDynamicReferences());
		assertEquals("1.2", new IBuildConfiguration[] {}, desc.getBuildConfigReferences(project0v0.getName()));
		assertEquals("1.3", new IBuildConfiguration[] {}, desc.getBuildConfigReferences(project0v1.getName()));
		assertEquals("1.4", new IBuildConfiguration[] {project1.getActiveBuildConfig(), project3.getActiveBuildConfig()}, project0.getReferencedBuildConfigs(project0v0.getName(), false));
		assertEquals("1.5", new IBuildConfiguration[] {project1.getActiveBuildConfig(), project3.getActiveBuildConfig()}, project0.getReferencedBuildConfigs(project0v1.getName(), false));

		// Now set dynamic references on config1
		desc.setBuildConfigReferences(project0v0.getName(), new IBuildConfiguration[] {project3v1, project2v0, project1v0});
		project0.setDescription(desc, getMonitor());

		// Check references
		// This is deterministic as config0 is listed first, so we expect its config order to trump cofig1's
		desc = project0.getDescription();
		assertEquals("2.1", new IProject[] {project1, project3}, desc.getDynamicReferences());
		assertEquals("2.2", new IBuildConfiguration[] {project3v1, project2v0, project1v0}, desc.getBuildConfigReferences(project0v0.getName()));
		// Now at the project leve
		assertEquals("2.3", new IBuildConfiguration[] {project3v1, project2v0, project1v0, project3v0}, project0.getReferencedBuildConfigs(project0v0.getName(), false));
		assertEquals("2.4", new IBuildConfiguration[] {project1.getActiveBuildConfig(), project3.getActiveBuildConfig()}, project0.getReferencedBuildConfigs(project0v1.getName(), false));
	}

	public void testSetAndGetProjectReferences() throws CoreException {
		// Set project references
		IProjectDescription desc = project0.getDescription();
		desc.setReferencedProjects(new IProject[] {project3, project1});
		desc.setDynamicReferences(new IProject[] {project1, project2});
		project0.setDescription(desc, getMonitor());

		desc = project1.getDescription();
		desc.setReferencedProjects(new IProject[] {project0});
		desc.setDynamicReferences(new IProject[] {});
		project1.setDescription(desc, getMonitor());

		desc = project2.getDescription();
		desc.setReferencedProjects(new IProject[] {});
		desc.setDynamicReferences(new IProject[] {});
		project2.setDescription(desc, getMonitor());

		desc = project3.getDescription();
		desc.setReferencedProjects(new IProject[] {});
		desc.setDynamicReferences(new IProject[] {project0});
		project3.setDescription(desc, getMonitor());

		// Test getters
		desc = project0.getDescription();
		assertEquals("1.0", new IProject[] {project3, project1}, desc.getReferencedProjects());
		assertEquals("1.1", new IProject[] {project1, project2}, desc.getDynamicReferences());
		assertEquals("1.3", new IBuildConfiguration[] {}, desc.getBuildConfigReferences(bc0));

		assertEquals("2.0", new IProject[] {project3, project1, project2}, project0.getReferencedProjects());
		assertEquals("2.1", new IProject[] {project1, project3}, project0.getReferencingProjects());
		assertEquals("2.2", new IBuildConfiguration[] {project3v0, project1v0, project2v0}, project0.getReferencedBuildConfigs(project0v0.getName(), true));
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
		project0.setDescription(desc, getMonitor());

		desc = project1.getDescription();
		desc.setReferencedProjects(new IProject[] {project0});
		desc.setBuildConfigReferences(bc0, new IBuildConfiguration[] {project0v1});
		desc.setBuildConfigReferences(bc1, new IBuildConfiguration[] {});
		project1.setDescription(desc, getMonitor());

		desc = project3.getDescription();
		desc.setBuildConfigReferences(bc0, new IBuildConfiguration[] {project0v1});
		desc.setBuildConfigReferences(bc1, new IBuildConfiguration[] {});
		project3.setDescription(desc, getMonitor());

		// Check getters
		desc = project0.getDescription();
		assertEquals("1.0", new IProject[] {project1}, desc.getReferencedProjects());
		assertEquals("1.1", new IProject[] {project3}, desc.getDynamicReferences());
		assertEquals("1.3", new IBuildConfiguration[] {project2v0, project1v0}, desc.getBuildConfigReferences(bc0));
		assertEquals("1.5", new IBuildConfiguration[] {project2v0}, desc.getBuildConfigReferences(bc1));

		assertEquals("2.0", new IProject[] {project2, project1, project3}, project0.getReferencedProjects());
		assertEquals("2.1", new IProject[] {project1, project3}, project0.getReferencingProjects());
		assertEquals("2.2", new IBuildConfiguration[] {project2v0, project1v0, project3.getActiveBuildConfig()}, project0.getReferencedBuildConfigs(project0v0.getName(), true));
		assertEquals("2.3", new IBuildConfiguration[] {project2v0, project1.getActiveBuildConfig(), project3.getActiveBuildConfig()}, project0.getReferencedBuildConfigs(project0v1.getName(), true));
	}

	public void testReferencesToActiveConfigs() throws CoreException {
		IProjectDescription desc = project0.getDescription();
		desc.setBuildConfigReferences(bc0, new IBuildConfiguration[] {getRef(project1)});
		project0.setDescription(desc, getMonitor());

		assertEquals("1.0", new IBuildConfiguration[] {getRef(project1)}, desc.getBuildConfigReferences(bc0));
		assertEquals("1.1", new IBuildConfiguration[] {project1v0}, project0.getReferencedBuildConfigs(project0v0.getName(), true));
	}
}
