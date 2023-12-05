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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IDynamicReferenceProvider;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace.ProjectOrder;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.tests.resources.ResourceTest;

/**
 * Test project dynamic references provided by extension point
 * <code>org.eclipse.core.resources.builders</code> and dynamicReference
 * {@link IDynamicReferenceProvider}
 */
public class ProjectDynamicReferencesTest extends ResourceTest {
	private static final String PROJECT_0_NAME = "ProjectDynamicReferencesTest_p0";

	private IProject project0;
	private IProject project1;
	private IProject project2;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		project0 = getWorkspace().getRoot().getProject(PROJECT_0_NAME);
		project1 = getWorkspace().getRoot().getProject("ProjectDynamicReferencesTest_p1");
		project2 = getWorkspace().getRoot().getProject("ProjectDynamicReferencesTest_p2");
		ensureExistsInWorkspace(new IProject[] { project0, project1, project2 });
		addBuilder(project0);
		addBuilder(project1);
		addBuilder(project2);
	}

	private static void addBuilder(IProject project) throws CoreException {
		IProjectDescription description = project.getDescription();
		ICommand command = description.newCommand();
		command.setBuilderName(Builder.NAME);
		description.setBuildSpec(new ICommand[] {command});
		project.setDescription(description, null);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		DynamicReferenceProvider.clear();
	}

	public void testReferencedProjects() throws CoreException {
		assertThat("Project0 must not have referenced projects", project0.getReferencedProjects(), emptyArray());
		assertThat("Project1 must not have referenced projects", project1.getReferencedProjects(), emptyArray());
		assertThat("Project2 must not have referenced projects", project2.getReferencedProjects(), emptyArray());

		DynamicReferenceProvider.addReference(project0, project1);

		assertThat("Project0 must not have referenced projects", project0.getReferencedProjects(), emptyArray());
		assertThat("Project1 must not have referenced projects", project1.getReferencedProjects(), emptyArray());
		assertThat("Project2 must not have referenced projects", project2.getReferencedProjects(), emptyArray());

		clearCache();

		assertThat("Project0 must reference Project1", project0.getReferencedProjects(), arrayContaining(project1));
		assertThat("Project1 must not have referenced projects", project1.getReferencedProjects(), emptyArray());
		assertThat("Project2 must not have referenced projects", project2.getReferencedProjects(), emptyArray());

		DynamicReferenceProvider.addReference(project1, project2);

		assertThat("Project0 must reference Project1", project0.getReferencedProjects(), arrayContaining(project1));
		assertThat("Project1 must not have referenced projects", project1.getReferencedProjects(), emptyArray());
		assertThat("Project2 must not have referenced projects", project2.getReferencedProjects(), emptyArray());

		clearCache();

		assertThat("Project0 must reference Project1", project0.getReferencedProjects(), arrayContaining(project1));
		assertThat("Project1 must reference Project2", project1.getReferencedProjects(), arrayContaining(project2));
		assertThat("Project2 must not have referenced projects", project2.getReferencedProjects(), emptyArray());

		DynamicReferenceProvider.addReference(project0, project2);

		assertThat("Project0 must reference Project1", project0.getReferencedProjects(), arrayContaining(project1));
		assertThat("Project1 must reference Project2", project1.getReferencedProjects(), arrayContaining(project2));
		assertThat("Project2 must not have referenced projects", project2.getReferencedProjects(), emptyArray());

		clearCache();

		assertThat("Project0 must reference Project1 and Project2", project0.getReferencedProjects(),
				arrayContaining(project1, project2));
		assertThat("Project1 must reference Project2", project1.getReferencedProjects(), arrayContaining(project2));
		assertThat("Project2 must not have referenced projects", project2.getReferencedProjects(), emptyArray());
	}

	public void testReferencedBuildConfigs() throws CoreException {
		assertThat("Project0 must not have referenced projects",
				project0.getReferencedBuildConfigs(IBuildConfiguration.DEFAULT_CONFIG_NAME, false), emptyArray());
		assertThat("Project1 must not have referenced projects",
				project1.getReferencedBuildConfigs(IBuildConfiguration.DEFAULT_CONFIG_NAME, false), emptyArray());
		assertThat("Project2 must not have referenced projects",
				project2.getReferencedBuildConfigs(IBuildConfiguration.DEFAULT_CONFIG_NAME, false), emptyArray());

		DynamicReferenceProvider.addReference(project0, project1);
		DynamicReferenceProvider.addReference(project1, project2);
		DynamicReferenceProvider.addReference(project0, project2);
		clearCache();

		IBuildConfiguration buildConfigProject1 = project1.getBuildConfig(IBuildConfiguration.DEFAULT_CONFIG_NAME);
		IBuildConfiguration buildConfigProject2 = project2.getBuildConfig(IBuildConfiguration.DEFAULT_CONFIG_NAME);
		assertThat("Build configuration of Project0 must reference build configuration of project1 and project2",
				project0.getReferencedBuildConfigs(IBuildConfiguration.DEFAULT_CONFIG_NAME, false),
				arrayContaining(buildConfigProject1, buildConfigProject2));
		assertThat("Build configuration of Project1 must reference build configuration of Project2",
				project1.getReferencedBuildConfigs(IBuildConfiguration.DEFAULT_CONFIG_NAME, false),
				arrayContaining(buildConfigProject2));
		assertThat("Project2 must not have referenced projects",
				project2.getReferencedBuildConfigs(IBuildConfiguration.DEFAULT_CONFIG_NAME, false), emptyArray());
	}

	public void testReferencingProjects() throws CoreException {
		assertThat("Project0 must not have referencing projects", project0.getReferencingProjects(), emptyArray());
		assertThat("Project1 must not have referencing projects", project1.getReferencingProjects(), emptyArray());
		assertThat("Project2 must not have referencing projects", project2.getReferencingProjects(), emptyArray());

		DynamicReferenceProvider.addReference(project0, project1);

		assertThat("Project0 must not have referencing projects", project0.getReferencingProjects(), emptyArray());
		assertThat("Project1 must not have referencing projects", project1.getReferencingProjects(), emptyArray());
		assertThat("Project2 must not have referencing projects", project2.getReferencingProjects(), emptyArray());

		clearCache();

		assertThat("Project0 must not have referencing projects", project0.getReferencingProjects(), emptyArray());
		assertThat("Project1 must be referenced by Project0", project1.getReferencingProjects(),
				arrayContaining(project0));
		assertThat("Project2 must not have referencing projects", project2.getReferencingProjects(), emptyArray());

		DynamicReferenceProvider.addReference(project1, project2);

		assertThat("Project0 must not have referencing projects", project0.getReferencingProjects(), emptyArray());
		assertThat("Project1 must must be referenced by Project0", project1.getReferencingProjects(),
				arrayContaining(project0));
		assertThat("Project2 must not have referencing projects", project2.getReferencingProjects(), emptyArray());

		clearCache();

		assertThat("Project0 must not have referencing projects", project0.getReferencingProjects(), emptyArray());
		assertThat("Project1 must be referenced by Project0", project1.getReferencingProjects(),
				arrayContaining(project0));
		assertThat("Project2 must be referenced by Project1", project2.getReferencingProjects(),
				arrayContaining(project1));

		DynamicReferenceProvider.addReference(project0, project2);

		assertThat("Project0 must not have referencing projects", project0.getReferencingProjects(), emptyArray());
		assertThat("Project1 must be referenced by Project0", project1.getReferencingProjects(),
				arrayContaining(project0));
		assertThat("Project2 must be referenced by Project1", project2.getReferencingProjects(),
				arrayContaining(project1));

		clearCache();

		assertThat("Project0 must not have referencing projects", project0.getReferencingProjects(), emptyArray());
		assertThat("Project1 must be referenced by Project0", project1.getReferencingProjects(),
				arrayContaining(project0));
		assertThat("Project2 must be referenced by Project0 and Project1", project2.getReferencingProjects(),
				arrayContaining(project0, project1));
	}

	public void testComputeProjectOrder() throws CoreException {
		IProject[] allProjects = new IProject[] { project0, project1, project2 };

		ProjectOrder projectOrder = getWorkspace().computeProjectOrder(allProjects);

		assertThat("Build order not defined, must return projects in default order", projectOrder.projects,
				is(allProjects));
		assertThat("Project order should not have cycles: " + projectOrder, !projectOrder.hasCycles);

		DynamicReferenceProvider.addReference(project0, project1);
		DynamicReferenceProvider.addReference(project1, project2);
		clearCache();

		projectOrder = getWorkspace().computeProjectOrder(allProjects);

		assertThat("Build order must be Project2, Project1, Project0", projectOrder.projects,
				arrayContaining(project2, project1, project0));
		assertThat("Project order should not have cycles: " + projectOrder, !projectOrder.hasCycles);

		DynamicReferenceProvider.clear();
		DynamicReferenceProvider.addReference(project1, project0);
		DynamicReferenceProvider.addReference(project0, project2);
		clearCache();

		projectOrder = getWorkspace().computeProjectOrder(allProjects);

		assertThat("Build order must be Project2, Project0, Project1",
				projectOrder.projects, arrayContaining(project2, project0, project1));
		assertThat("Project order should not have cycles: " + projectOrder, !projectOrder.hasCycles);
	}

	public void testBug543776() throws Exception {
		IFile projectFile = project0.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		String projectDescription = readStringInFileSystem(projectFile);
		projectDescription = projectDescription.replace(PROJECT_0_NAME, "anotherName");
		ensureExistsInWorkspace(projectFile, projectDescription);
		project0.delete(false, true, null);
		project0.create(null);
		project0.open(null);

		assertThat(project0.getName(), is(PROJECT_0_NAME));
		assertThat(project0.getDescription().getName(), is("anotherName"));

		DynamicReferenceProvider.addReference(project0, project1);
		clearCache();

		assertThat("Project0 must reference Project1", project0.getReferencedProjects(),
				arrayContaining(project1));
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
