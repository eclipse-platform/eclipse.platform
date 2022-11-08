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

import static org.junit.Assert.assertArrayEquals;

import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.resources.IWorkspace.ProjectOrder;
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

	private static final IProject[] EMPTY_PROJECTS = new IProject[0];
	private static final IBuildConfiguration[] EMPTY_BUILD_CONFIGURATIONS = new IBuildConfiguration[0];

	private IProject project0;
	private IProject project1;
	private IProject project2;

	@Override
	public void setUp() throws Exception {
		project0 = getWorkspace().getRoot().getProject(PROJECT_0_NAME);
		project1 = getWorkspace().getRoot().getProject("ProjectDynamicReferencesTest_p1");
		project2 = getWorkspace().getRoot().getProject("ProjectDynamicReferencesTest_p2");
		ensureExistsInWorkspace(new IProject[] { project0, project1, project2 }, true);
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
		project0.delete(true, null);
		project1.delete(true, null);
		project2.delete(true, null);
	}

	public void testReferencedProjects() throws CoreException {
		assertArrayEquals("Project0 must not have referenced projects", EMPTY_PROJECTS,
				project0.getReferencedProjects());
		assertArrayEquals("Project1 must not have referenced projects", EMPTY_PROJECTS,
				project1.getReferencedProjects());
		assertArrayEquals("Project2 must not have referenced projects", EMPTY_PROJECTS,
				project2.getReferencedProjects());

		DynamicReferenceProvider.addReference(project0, project1);

		assertArrayEquals("Project0 must not have referenced projects", EMPTY_PROJECTS,
				project0.getReferencedProjects());
		assertArrayEquals("Project1 must not have referenced projects", EMPTY_PROJECTS,
				project1.getReferencedProjects());
		assertArrayEquals("Project2 must not have referenced projects", EMPTY_PROJECTS,
				project2.getReferencedProjects());

		clearCache();

		assertArrayEquals("Project0 must reference Project1", new IProject[] { project1 },
				project0.getReferencedProjects());
		assertArrayEquals("Project1 must not have referenced projects", EMPTY_PROJECTS,
				project1.getReferencedProjects());
		assertArrayEquals("Project2 must not have referenced projects", EMPTY_PROJECTS,
				project2.getReferencedProjects());

		DynamicReferenceProvider.addReference(project1, project2);

		assertArrayEquals("Project0 must reference Project1", new IProject[] { project1 },
				project0.getReferencedProjects());
		assertArrayEquals("Project1 must not have referenced projects", EMPTY_PROJECTS,
				project1.getReferencedProjects());
		assertArrayEquals("Project2 must not have referenced projects", EMPTY_PROJECTS,
				project2.getReferencedProjects());

		clearCache();

		assertArrayEquals("Project0 must reference Project1", new IProject[] { project1 },
				project0.getReferencedProjects());
		assertArrayEquals("Project1 must reference Project2", new IProject[] { project2 },
				project1.getReferencedProjects());
		assertArrayEquals("Project2 must not have referenced projects", EMPTY_PROJECTS,
				project2.getReferencedProjects());

		DynamicReferenceProvider.addReference(project0, project2);

		assertArrayEquals("Project0 must reference Project1", new IProject[] { project1 },
				project0.getReferencedProjects());
		assertArrayEquals("Project1 must reference Project2", new IProject[] { project2 },
				project1.getReferencedProjects());
		assertArrayEquals("Project2 must not have referenced projects", EMPTY_PROJECTS,
				project2.getReferencedProjects());

		clearCache();

		assertArrayEquals("Project0 must reference Project1 and Project2", new IProject[] { project1, project2 },
				project0.getReferencedProjects());
		assertArrayEquals("Project1 must reference Project2", new IProject[] { project2 },
				project1.getReferencedProjects());
		assertArrayEquals("Project2 must not have referenced projects", EMPTY_PROJECTS,
				project2.getReferencedProjects());
	}

	public void testReferencedBuildConfigs() throws CoreException {
		assertArrayEquals("Project0 must not have referenced projects", EMPTY_BUILD_CONFIGURATIONS,
				project0.getReferencedBuildConfigs(IBuildConfiguration.DEFAULT_CONFIG_NAME, false));
		assertArrayEquals("Project1 must not have referenced projects", EMPTY_BUILD_CONFIGURATIONS,
				project1.getReferencedBuildConfigs(IBuildConfiguration.DEFAULT_CONFIG_NAME, false));
		assertArrayEquals("Project2 must not have referenced projects", EMPTY_BUILD_CONFIGURATIONS,
				project2.getReferencedBuildConfigs(IBuildConfiguration.DEFAULT_CONFIG_NAME, false));

		DynamicReferenceProvider.addReference(project0, project1);
		DynamicReferenceProvider.addReference(project1, project2);
		DynamicReferenceProvider.addReference(project0, project2);
		clearCache();

		IBuildConfiguration buildConfigProject1 = project1.getBuildConfig(IBuildConfiguration.DEFAULT_CONFIG_NAME);
		IBuildConfiguration buildConfigProject2 = project2.getBuildConfig(IBuildConfiguration.DEFAULT_CONFIG_NAME);
		assertArrayEquals("Build configuration of Project0 must reference build configuration of project1 and project2",
				new IBuildConfiguration[] { buildConfigProject1, buildConfigProject2 },
				project0.getReferencedBuildConfigs(IBuildConfiguration.DEFAULT_CONFIG_NAME, false));
		assertArrayEquals("Build configuration of Project1 must reference build configuration of Project2",
				new IBuildConfiguration[] { buildConfigProject2 },
				project1.getReferencedBuildConfigs(IBuildConfiguration.DEFAULT_CONFIG_NAME, false));
		assertArrayEquals("Project2 must not have referenced projects", EMPTY_BUILD_CONFIGURATIONS,
				project2.getReferencedBuildConfigs(IBuildConfiguration.DEFAULT_CONFIG_NAME, false));
	}

	public void testReferencingProjects() throws CoreException {
		assertArrayEquals("Project0 must not have referencing projects", EMPTY_PROJECTS,
				project0.getReferencingProjects());
		assertArrayEquals("Project1 must not have referencing projects", EMPTY_PROJECTS,
				project1.getReferencingProjects());
		assertArrayEquals("Project2 must not have referencing projects", EMPTY_PROJECTS,
				project2.getReferencingProjects());

		DynamicReferenceProvider.addReference(project0, project1);

		assertArrayEquals("Project0 must not have referencing projects", EMPTY_PROJECTS,
				project0.getReferencingProjects());
		assertArrayEquals("Project1 must not have referencing projects", EMPTY_PROJECTS,
				project1.getReferencingProjects());
		assertArrayEquals("Project2 must not have referencing projects", EMPTY_PROJECTS,
				project2.getReferencingProjects());

		clearCache();

		assertArrayEquals("Project0 must not have referencing projects", EMPTY_PROJECTS,
				project0.getReferencingProjects());
		assertArrayEquals("Project1 must be referenced by Project0", new IProject[] { project0 },
				project1.getReferencingProjects());
		assertArrayEquals("Project2 must not have referencing projects", EMPTY_PROJECTS,
				project2.getReferencingProjects());

		DynamicReferenceProvider.addReference(project1, project2);

		assertArrayEquals("Project0 must not have referencing projects", EMPTY_PROJECTS,
				project0.getReferencingProjects());
		assertArrayEquals("Project1 must must be referenced by Project0", new IProject[] { project0 },
				project1.getReferencingProjects());
		assertArrayEquals("Project2 must not have referencing projects", EMPTY_PROJECTS,
				project2.getReferencingProjects());

		clearCache();

		assertArrayEquals("Project0 must not have referencing projects", EMPTY_PROJECTS,
				project0.getReferencingProjects());
		assertArrayEquals("Project1 must be referenced by Project0", new IProject[] { project0 },
				project1.getReferencingProjects());
		assertArrayEquals("Project2 must be referenced by Project1", new IProject[] { project1 },
				project2.getReferencingProjects());

		DynamicReferenceProvider.addReference(project0, project2);

		assertArrayEquals("Project0 must not have referencing projects", EMPTY_PROJECTS,
				project0.getReferencingProjects());
		assertArrayEquals("Project1 must be referenced by Project0", new IProject[] { project0 },
				project1.getReferencingProjects());
		assertArrayEquals("Project2 must be referenced by Project1", new IProject[] { project1 },
				project2.getReferencingProjects());

		clearCache();

		assertArrayEquals("Project0 must not have referencing projects", EMPTY_PROJECTS,
				project0.getReferencingProjects());
		assertArrayEquals("Project1 must be referenced by Project0", new IProject[] { project0 },
				project1.getReferencingProjects());
		assertArrayEquals("Project2 must be referenced by Project0 and Project1", new IProject[] { project0, project1 },
				project2.getReferencingProjects());
	}

	public void testComputeProjectOrder() throws CoreException {
		IProject[] allProjects = new IProject[] { project0, project1, project2 };

		ProjectOrder projectOrder = getWorkspace().computeProjectOrder(allProjects);

		assertArrayEquals("Build order not defined, must return projects in default order", allProjects,
				projectOrder.projects);
		assertFalse("No cycles", projectOrder.hasCycles);

		DynamicReferenceProvider.addReference(project0, project1);
		DynamicReferenceProvider.addReference(project1, project2);
		clearCache();

		projectOrder = getWorkspace().computeProjectOrder(allProjects);

		assertArrayEquals("Build order must be Project2, Project1, Project0",
				new IProject[] { project2, project1, project0 }, projectOrder.projects);
		assertFalse("No cycles", projectOrder.hasCycles);

		DynamicReferenceProvider.clear();
		DynamicReferenceProvider.addReference(project1, project0);
		DynamicReferenceProvider.addReference(project0, project2);
		clearCache();

		projectOrder = getWorkspace().computeProjectOrder(allProjects);

		assertArrayEquals("Build order must be Project2, Project0, Project1",
				new IProject[] { project2, project0, project1 }, projectOrder.projects);
		assertFalse("No cycles", projectOrder.hasCycles);
	}

	public void testBug543776() throws CoreException {
		IFile projectFile = project0.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		String projectDescription = readStringInFileSystem(projectFile);
		projectDescription = projectDescription.replace(PROJECT_0_NAME, "anotherName");
		ensureExistsInWorkspace(projectFile, projectDescription);
		project0.delete(false, true, null);
		project0.create(null);
		project0.open(null);

		assertEquals(PROJECT_0_NAME, project0.getName());
		assertEquals("anotherName", project0.getDescription().getName());

		DynamicReferenceProvider.addReference(project0, project1);
		clearCache();

		assertArrayEquals("Project0 must reference Project1", new IProject[] { project1 },
				project0.getReferencedProjects());
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
