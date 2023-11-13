/*******************************************************************************
 * Copyright (c) 2004, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *     Alexander Kurtakov <akurtako@redhat.com> - Bug 459343
 *******************************************************************************/
package org.eclipse.core.tests.resources;

import static org.junit.Assert.assertThrows;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.internal.events.BuildCommand;
import org.eclipse.core.internal.resources.Project;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.internal.builders.CustomTriggerBuilder;

/**
 * Tests protocol of IProjectDescription and other specified behavior
 * that relates to the project description.
 */
public class IProjectDescriptionTest extends ResourceTest {

	public void testDescriptionConstant() {
		assertEquals("1.0", ".project", IProjectDescription.DESCRIPTION_FILE_NAME);
	}

	/**
	 * Tests that setting the build spec preserves any instantiated builder.
	 */
	public void testBuildSpecBuilder() throws Exception {
		Project project = (Project) getWorkspace().getRoot().getProject("ProjectTBSB");
		ensureExistsInWorkspace(project, true);
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
		IFile descriptionFile = project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		assertTrue("1.0", descriptionFile.exists());

		// Add a builder to the build command.
		IProjectDescription desc = project.getDescription();
		ICommand command = desc.newCommand();
		command.setBuilderName(CustomTriggerBuilder.BUILDER_NAME);
		desc.setBuildSpec(new ICommand[] {command});
		project.setDescription(desc, null);

		project.build(IncrementalProjectBuilder.FULL_BUILD, null);

		// Get a non-cloned version of the project desc build spec, and check for the builder
		assertTrue("2.0", ((BuildCommand) project.internalGetDescription().getBuildSpec(false)[0]).getBuilders() != null);

		// Now reset the build command. The builder shouldn't disappear.
		desc = project.getDescription();
		desc.setBuildSpec(new ICommand[] {command});
		project.setDescription(desc, null);

		// builder should still be there
		assertTrue("3.0", ((BuildCommand) project.internalGetDescription().getBuildSpec(false)[0]).getBuilders() != null);
	}

	/**
	 * Tests that the description file is not dirtied if the description has not actually
	 * changed.
	 */
	public void testDirtyDescription() throws Exception {
		IProject project = getWorkspace().getRoot().getProject("Project");
		IProject target1 = getWorkspace().getRoot().getProject("target1");
		IProject target2 = getWorkspace().getRoot().getProject("target2");
		ensureExistsInWorkspace(project, true);
		IFile descriptionFile = project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		assertTrue("1.0", descriptionFile.exists());

		long timestamp = descriptionFile.getLocalTimeStamp();

		// wait a bit to ensure that timestamp granularity does not
		// spoil our test
		Thread.sleep(1000);

		IProjectDescription description = project.getDescription();
		description.setBuildSpec(description.getBuildSpec());
		description.setComment(description.getComment());
		description.setDynamicReferences(description.getDynamicReferences());
		description.setLocation(description.getLocation());
		description.setName(description.getName());
		description.setNatureIds(description.getNatureIds());
		description.setReferencedProjects(description.getReferencedProjects());
		project.setDescription(description, IResource.NONE, null);

		//the timestamp should be the same
		assertEquals("2.0", timestamp, descriptionFile.getLocalTimeStamp());

		//adding a dynamic reference should not dirty the file
		description = project.getDescription();
		description.setDynamicReferences(new IProject[] { target1, target2 });
		project.setDescription(description, IResource.NONE, null);

		assertEquals("2.1", timestamp, descriptionFile.getLocalTimeStamp());
	}

	/**
	 * Tests that the description file is dirtied if the description has actually
	 * changed. This is a regression test for bug 64128.
	 */
	public void testDirtyBuildSpec() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("Project");
		IFile projectDescription = project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		ensureExistsInWorkspace(project, true);
		String key = "key";
		String value1 = "value1";
		String value2 = "value2";

		IProjectDescription description = project.getDescription();
		ICommand newCommand = description.newCommand();
		Map<String, String> args = new HashMap<>();
		args.put(key, value1);
		newCommand.setArguments(args);
		description.setBuildSpec(new ICommand[] { newCommand });
		project.setDescription(description, IResource.NONE, null);

		//changing a build command argument should dirty the description file
		long modificationStamp = projectDescription.getModificationStamp();
		description = project.getDescription();
		ICommand command = description.getBuildSpec()[0];
		args = command.getArguments();
		args.put(key, value2);
		command.setArguments(args);
		description.setBuildSpec(new ICommand[] { command });
		project.setDescription(description, IResource.NONE, null);

		assertTrue("3.0", modificationStamp != projectDescription.getModificationStamp());
	}

	public void testDynamicProjectReferences() throws CoreException {
		IProject target1 = getWorkspace().getRoot().getProject("target1");
		IProject target2 = getWorkspace().getRoot().getProject("target2");
		ensureExistsInWorkspace(target1, true);
		ensureExistsInWorkspace(target2, true);

		IProject project = getWorkspace().getRoot().getProject("project");
		ensureExistsInWorkspace(project, true);

		IProjectDescription description = project.getDescription();
		description.setReferencedProjects(new IProject[] {target1});
		description.setDynamicReferences(new IProject[] {target2});
		project.setDescription(description, getMonitor());
		IProject[] refs = project.getReferencedProjects();
		assertEquals("2.1", 2, refs.length);
		assertEquals("2.2", target1, refs[0]);
		assertEquals("2.3", target2, refs[1]);
		assertEquals("2.4", 1, target1.getReferencingProjects().length);
		assertEquals("2.5", 1, target2.getReferencingProjects().length);

		//get references for a non-existent project
		assertThrows(CoreException.class,
				() -> getWorkspace().getRoot().getProject("DoesNotExist").getReferencedProjects());
	}

	/**
	 * Tests IProjectDescription project references
	 */
	public void testProjectReferences() throws CoreException {
		IProject target = getWorkspace().getRoot().getProject("Project1");
		ensureExistsInWorkspace(target, true);

		IProject project = getWorkspace().getRoot().getProject("Project2");
		ensureExistsInWorkspace(project, true);

		project.open(getMonitor());
		IProjectDescription description = project.getDescription();
		description.setReferencedProjects(new IProject[] {target});
		project.setDescription(description, getMonitor());
		assertEquals("2.1", 1, target.getReferencingProjects().length);

		//get references for a non-existent project
		assertThrows(CoreException.class,
				() -> getWorkspace().getRoot().getProject("DoesNotExist").getReferencedProjects());

		//get referencing projects for a non-existent project
		IProject[] refs = getWorkspace().getRoot().getProject("DoesNotExist2").getReferencingProjects();
		assertEquals("4.0", 0, refs.length);
	}
}
