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
import static org.eclipse.core.tests.resources.ResourceTestUtil.readStringInFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.updateProjectDescription;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IDynamicReferenceProvider;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace.ProjectOrder;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test project dynamic references provided by extension point
 * <code>org.eclipse.core.resources.builders</code> and dynamicReference
 * {@link IDynamicReferenceProvider}
 */
public class ProjectDynamicReferencesTest {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	private static final String PROJECT_0_NAME = "ProjectDynamicReferencesTest_p0";

	private IProject project0;
	private IProject project1;
	private IProject project2;

	@Before
	public void setUp() throws Exception {
		project0 = getWorkspace().getRoot().getProject(PROJECT_0_NAME);
		project1 = getWorkspace().getRoot().getProject("ProjectDynamicReferencesTest_p1");
		project2 = getWorkspace().getRoot().getProject("ProjectDynamicReferencesTest_p2");
		createInWorkspace(new IProject[] { project0, project1, project2 });
		updateProjectDescription(project0).addingCommand(Builder.NAME).apply();
		updateProjectDescription(project1).addingCommand(Builder.NAME).apply();
		updateProjectDescription(project2).addingCommand(Builder.NAME).apply();
	}

	@After
	public void tearDown() throws Exception {
		DynamicReferenceProvider.clear();
	}

	@Test
	public void testReferencedProjects() throws CoreException {
		assertThat(project0.getReferencedProjects()).as("referenced projects of Project0").isEmpty();
		assertThat(project1.getReferencedProjects()).as("referenced projects of Project1").isEmpty();
		assertThat(project2.getReferencedProjects()).as("referenced projects of Project2").isEmpty();

		DynamicReferenceProvider.addReference(project0, project1);

		assertThat(project0.getReferencedProjects()).as("referenced projects of Project0").isEmpty();
		assertThat(project1.getReferencedProjects()).as("referenced projects of Project1").isEmpty();
		assertThat(project2.getReferencedProjects()).as("referenced projects of Project2").isEmpty();

		clearCache();

		assertThat(project0.getReferencedProjects()).as("referenced projects of Project0").containsExactly(project1);
		assertThat(project1.getReferencedProjects()).as("referenced projects of Project1").isEmpty();
		assertThat(project2.getReferencedProjects()).as("referenced projects of Project2").isEmpty();

		DynamicReferenceProvider.addReference(project1, project2);

		assertThat(project0.getReferencedProjects()).as("referenced projects of Project0").containsExactly(project1);
		assertThat(project1.getReferencedProjects()).as("referenced projects of Project1").isEmpty();
		assertThat(project2.getReferencedProjects()).as("referenced projects of Project2").isEmpty();

		clearCache();

		assertThat(project0.getReferencedProjects()).as("referenced projects of Project0").containsExactly(project1);
		assertThat(project1.getReferencedProjects()).as("referenced projects of Project1").containsExactly(project2);
		assertThat(project2.getReferencedProjects()).as("referenced projects of Project2").isEmpty();

		DynamicReferenceProvider.addReference(project0, project2);

		assertThat(project0.getReferencedProjects()).as("referenced projects of Project0").containsExactly(project1);
		assertThat(project1.getReferencedProjects()).as("referenced projects of Project1").containsExactly(project2);
		assertThat(project2.getReferencedProjects()).as("referenced projects of Project2").isEmpty();

		clearCache();

		assertThat(project0.getReferencedProjects()).as("referenced projects of Project0").containsExactly(project1,
				project2);
		assertThat(project1.getReferencedProjects()).as("referenced projects of Project1").containsExactly(project2);
		assertThat(project2.getReferencedProjects()).as("referenced projects of Project2").isEmpty();
	}

	@Test
	public void testReferencedBuildConfigs() throws CoreException {
		assertThat(project0.getReferencedBuildConfigs(IBuildConfiguration.DEFAULT_CONFIG_NAME, false))
				.as("referenced build configs of Project0").isEmpty();
		assertThat(project1.getReferencedBuildConfigs(IBuildConfiguration.DEFAULT_CONFIG_NAME, false))
				.as("referenced build configs of Project1").isEmpty();
		assertThat(project2.getReferencedBuildConfigs(IBuildConfiguration.DEFAULT_CONFIG_NAME, false))
				.as("referenced build configs of Project2").isEmpty();

		DynamicReferenceProvider.addReference(project0, project1);
		DynamicReferenceProvider.addReference(project1, project2);
		DynamicReferenceProvider.addReference(project0, project2);
		clearCache();

		IBuildConfiguration buildConfigProject1 = project1.getBuildConfig(IBuildConfiguration.DEFAULT_CONFIG_NAME);
		IBuildConfiguration buildConfigProject2 = project2.getBuildConfig(IBuildConfiguration.DEFAULT_CONFIG_NAME);
		assertThat(project0.getReferencedBuildConfigs(IBuildConfiguration.DEFAULT_CONFIG_NAME, false))
				.as("referenced build configs of Project0")
				.containsExactly(buildConfigProject1, buildConfigProject2);
		assertThat(project1.getReferencedBuildConfigs(IBuildConfiguration.DEFAULT_CONFIG_NAME, false))
				.as("referenced build configs of Project1")
				.containsExactly(buildConfigProject2);
		assertThat(project2.getReferencedBuildConfigs(IBuildConfiguration.DEFAULT_CONFIG_NAME, false))
				.as("referenced build configs of Project2").isEmpty();
	}

	@Test
	public void testReferencingProjects() throws CoreException {
		assertThat(project0.getReferencingProjects()).as("referencing projects of Project0").isEmpty();
		assertThat(project1.getReferencingProjects()).as("referencing projects of Project1").isEmpty();
		assertThat(project2.getReferencingProjects()).as("referencing projects of Project2").isEmpty();

		DynamicReferenceProvider.addReference(project0, project1);

		assertThat(project0.getReferencingProjects()).as("referencing projects of Project0").isEmpty();
		assertThat(project1.getReferencingProjects()).as("referencing projects of Project1").isEmpty();
		assertThat(project2.getReferencingProjects()).as("referencing projects of Project2").isEmpty();

		clearCache();

		assertThat(project0.getReferencingProjects()).as("referencing projects of Project0").isEmpty();
		assertThat(project1.getReferencingProjects()).as("referencing projects of Project1").containsExactly(project0);
		assertThat(project2.getReferencingProjects()).as("referencing projects of Project2").isEmpty();

		DynamicReferenceProvider.addReference(project1, project2);

		assertThat(project0.getReferencingProjects()).as("referencing projects of Project0").isEmpty();
		assertThat(project1.getReferencingProjects()).as("referencing projects of Project1").containsExactly(project0);
		assertThat(project2.getReferencingProjects()).as("referencing projects of Project2").isEmpty();

		clearCache();

		assertThat(project0.getReferencingProjects()).as("referencing projects of Project0").isEmpty();
		assertThat(project1.getReferencingProjects()).as("referencing projects of Project2").containsExactly(project0);
		assertThat(project2.getReferencingProjects()).as("referencing projects of Project2").containsExactly(project1);

		DynamicReferenceProvider.addReference(project0, project2);

		assertThat(project0.getReferencingProjects()).as("referencing projects of Project0").isEmpty();
		assertThat(project1.getReferencingProjects()).as("referencing projects of Project1").containsExactly(project0);
		assertThat(project2.getReferencingProjects()).as("referencing projects of Project2").containsExactly(project1);

		clearCache();

		assertThat(project0.getReferencingProjects()).as("referencing projects of Project0").isEmpty();
		assertThat(project1.getReferencingProjects()).as("referencing projects of Project1").containsExactly(project0);
		assertThat(project2.getReferencingProjects()).as("referencing projects of Project2").containsExactly(project0,
				project1);
	}

	@Test
	public void testComputeProjectOrder() throws CoreException {
		IProject[] allProjects = new IProject[] { project0, project1, project2 };

		ProjectOrder projectOrder = getWorkspace().computeProjectOrder(allProjects);

		// Build order not defined, must return projects in default order
		assertThat(projectOrder.projects).as("build order").isEqualTo(allProjects);
		assertThat(projectOrder).matches(it -> !it.hasCycles, "does not have cycles");

		DynamicReferenceProvider.addReference(project0, project1);
		DynamicReferenceProvider.addReference(project1, project2);
		clearCache();

		projectOrder = getWorkspace().computeProjectOrder(allProjects);

		assertThat(projectOrder.projects).as("build order").containsExactly(project2, project1, project0);
		assertThat(projectOrder).matches(it -> !it.hasCycles, "does not have cycles");

		DynamicReferenceProvider.clear();
		DynamicReferenceProvider.addReference(project1, project0);
		DynamicReferenceProvider.addReference(project0, project2);
		clearCache();

		projectOrder = getWorkspace().computeProjectOrder(allProjects);

		assertThat(projectOrder.projects).as("build order").containsExactly(project2, project0, project1);
		assertThat(projectOrder).matches(it -> !it.hasCycles, "does not have cycles");
	}

	@Test
	public void testBug543776() throws Exception {
		IFile projectFile = project0.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		String projectDescription = readStringInFileSystem(projectFile);
		projectDescription = projectDescription.replace(PROJECT_0_NAME, "anotherName");
		createInWorkspace(projectFile, projectDescription);
		project0.delete(false, true, null);
		project0.create(null);
		project0.open(null);

		assertThat(project0.getName()).isEqualTo(PROJECT_0_NAME);
		assertThat(project0.getDescription().getName()).isEqualTo("anotherName");

		DynamicReferenceProvider.addReference(project0, project1);
		clearCache();

		assertThat(project0.getReferencedProjects()).as("referenced projects of Project0").containsExactly(project1);
	}

	private void clearCache() {
		project0.clearCachedDynamicReferences();
		project1.clearCachedDynamicReferences();
		project2.clearCachedDynamicReferences();
	}

	public static final class Builder extends IncrementalProjectBuilder {

		public static final String NAME = "org.eclipse.core.tests.resources.dynamicProjectReferenceBuilder";

		@Override
		protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
			return null;
		}

	}

	public static final class DynamicReferenceProvider implements IDynamicReferenceProvider
	{
		private static final Map<IProject, List<IProject>> dependentProjects = new HashMap<>();

		@Override
		public List<IProject> getDependentProjects(IBuildConfiguration buildConfiguration) throws CoreException {
			IProject project = buildConfiguration.getProject();
			List<IProject> depProjects = dependentProjects.get(project);
			if (depProjects != null) {
				return depProjects;
			}
			return Collections.emptyList();
		}

		public static void addReference(IProject project, IProject dependentProject) {
			List<IProject> depProjects = dependentProjects.computeIfAbsent(project, proj -> new ArrayList<>());
			depProjects.add(dependentProject);
		}

		public static void clear() {
			dependentProjects.clear();
		}
	}
}
