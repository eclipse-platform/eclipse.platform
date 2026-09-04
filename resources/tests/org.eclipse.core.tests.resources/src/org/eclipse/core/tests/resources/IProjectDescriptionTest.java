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

import static java.util.function.Predicate.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.harness.FileSystemHelper.getRandomLocation;
import static org.eclipse.core.tests.harness.FileSystemHelper.getTempDir;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertDoesNotExistInFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.removeFromFileSystem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.internal.events.BuildCommand;
import org.eclipse.core.internal.resources.Project;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.tests.internal.builders.CustomTriggerBuilder;
import org.eclipse.core.tests.internal.filesystem.wrapper.WrapperFileStore;
import org.eclipse.core.tests.internal.filesystem.wrapper.WrapperFileSystem;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Tests protocol of IProjectDescription and other specified behavior
 * that relates to the project description.
 */
@ExtendWith(WorkspaceResetExtension.class)
public class IProjectDescriptionTest {

	@Test
	public void testDescriptionConstant() {
		assertEquals(".project", IProjectDescription.DESCRIPTION_FILE_NAME);
	}

	/**
	 * Deleting the description file through the workspace closes the project and
	 * does not recreate the file.
	 */
	@Test
	public void testDeleteDescriptionFileClosesProject() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("Project");
		IFile descriptionFile = project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		createInWorkspace(project);

		descriptionFile.delete(IResource.NONE, createTestMonitor());

		assertThat(project).matches(IProject::exists, "exists").matches(not(IProject::isOpen), "is closed");
		getWorkspace().save(true, createTestMonitor());
		assertDoesNotExistInFileSystem(descriptionFile);
	}

	/**
	 * A refresh that finds the description file deleted closes the project and
	 * does not recreate the file. Restoring the file allows to reopen the project.
	 */
	@Test
	public void testRefreshWithDeletedDescriptionFileClosesProject() throws Exception {
		IProject project = getWorkspace().getRoot().getProject("Project");
		IFile descriptionFile = project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		IFile file = project.getFile("file.txt");
		createInWorkspace(file);
		Path backup = getTempDir().append("dotProjectBackup").toPath();
		Files.copy(descriptionFile.getLocation().toPath(), backup);
		try {
			removeFromFileSystem(descriptionFile);

			project.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());

			assertThat(project).matches(IProject::exists, "exists").matches(not(IProject::isOpen), "is closed");
			getWorkspace().save(true, createTestMonitor());
			assertDoesNotExistInFileSystem(descriptionFile);

			Files.copy(backup, descriptionFile.getLocation().toPath());
			project.open(createTestMonitor());
			assertThat(project).matches(IProject::isOpen, "is open");
			assertThat(file).matches(IResource::exists, "exists");
		} finally {
			Files.deleteIfExists(backup);
		}
	}

	/**
	 * A refresh that finds the whole project directory deleted closes the project
	 * and does not recreate the directory.
	 */
	@Test
	public void testRefreshWithDeletedProjectDirectoryClosesProject() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("Project");
		createInWorkspace(project.getFile("file.txt"));

		removeFromFileSystem(project);
		project.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());

		assertThat(project).matches(IProject::exists, "exists").matches(not(IProject::isOpen), "is closed");
		getWorkspace().save(true, createTestMonitor());
		assertDoesNotExistInFileSystem(project);
	}

	/**
	 * Closing a project does not recreate a deleted description file.
	 */
	@Test
	public void testCloseDoesNotRecreateDescriptionFile() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("Project");
		IFile descriptionFile = project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		createInWorkspace(project);

		removeFromFileSystem(descriptionFile);
		project.close(createTestMonitor());

		assertThat(project).matches(not(IProject::isOpen), "is closed");
		assertDoesNotExistInFileSystem(descriptionFile);
	}

	/**
	 * Moves by copying and deleting, and refuses to delete a file named
	 * {@link #UNDELETABLE_FILE} after deleting everything else, like a locked
	 * file on Windows.
	 */
	public static class UndeletableFileStore extends WrapperFileStore {
		static final String UNDELETABLE_FILE = "undeletable.txt";

		public UndeletableFileStore(IFileStore store) {
			super(store);
		}

		@Override
		public void move(IFileStore destination, int options, IProgressMonitor monitor) throws CoreException {
			copy(destination, options, monitor);
			delete(EFS.NONE, monitor);
		}

		@Override
		public void delete(int options, IProgressMonitor monitor) throws CoreException {
			CoreException failure = null;
			for (IFileStore child : childStores(EFS.NONE, null)) {
				try {
					child.delete(options, monitor);
				} catch (CoreException e) {
					failure = e;
				}
			}
			if (failure != null) {
				throw failure;
			}
			if (UNDELETABLE_FILE.equals(getName())) {
				throw new CoreException(Status.error("cannot delete " + this));
			}
			super.delete(options, monitor);
		}
	}

	/**
	 * A project move that copied the content and then failed to delete part of
	 * the source, including its description file, still ends up at the
	 * destination with its markers.
	 */
	@Test
	public void testMoveWithUndeletableSourceContent() throws Exception {
		IPath sourceLocation = getRandomLocation();
		IProject source = getWorkspace().getRoot().getProject("Source");
		IProjectDescription sourceDescription = getWorkspace().newProjectDescription(source.getName());
		sourceDescription.setLocationURI(WrapperFileSystem.getWrappedURI(URIUtil.toURI(sourceLocation)));
		source.create(sourceDescription, createTestMonitor());
		source.open(createTestMonitor());
		IFile file = source.getFile("file.txt");
		IFile undeletableFile = source.getFile(UndeletableFileStore.UNDELETABLE_FILE);
		createInWorkspace(new IResource[] { file, undeletableFile });
		IMarker marker = file.createMarker(IMarker.BOOKMARK);
		IProject destination = getWorkspace().getRoot().getProject("Destination");
		IProjectDescription destinationDescription = getWorkspace().newProjectDescription(destination.getName());
		WrapperFileSystem.setCustomFileStore(UndeletableFileStore.class);
		try {
			assertThrows(CoreException.class,
					() -> source.move(destinationDescription, IResource.FORCE, createTestMonitor()));

			assertFalse(sourceLocation.append(IProjectDescription.DESCRIPTION_FILE_NAME).toFile().exists());
			assertThat(source).matches(not(IProject::exists), "does not exist");
			assertThat(destination).matches(IProject::isOpen, "is open");
			IFile movedFile = destination.getFile(file.getProjectRelativePath());
			assertThat(movedFile).matches(IResource::exists, "exists");
			assertNotNull(movedFile.findMarker(marker.getId()));
			assertThat(destination.getFile(undeletableFile.getProjectRelativePath())).matches(IResource::exists,
					"exists");
		} finally {
			WrapperFileSystem.setCustomFileStore(null);
			removeFromFileSystem(sourceLocation.toFile());
		}
	}

	/**
	 * Tests that setting the build spec preserves any instantiated builder.
	 */
	@Test
	public void testBuildSpecBuilder() throws Exception {
		Project project = (Project) getWorkspace().getRoot().getProject("ProjectTBSB");
		createInWorkspace(project);
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
		IFile descriptionFile = project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		assertTrue(descriptionFile.exists());

		// Add a builder to the build command.
		IProjectDescription desc = project.getDescription();
		ICommand command = desc.newCommand();
		command.setBuilderName(CustomTriggerBuilder.BUILDER_NAME);
		desc.setBuildSpec(new ICommand[] {command});
		project.setDescription(desc, null);

		project.build(IncrementalProjectBuilder.FULL_BUILD, null);

		// Get a non-cloned version of the project desc build spec, and check for the builder
		assertNotNull(((BuildCommand) project.internalGetDescription().getBuildSpec(false)[0]).getBuilders());

		// Now reset the build command. The builder shouldn't disappear.
		desc = project.getDescription();
		desc.setBuildSpec(new ICommand[] {command});
		project.setDescription(desc, null);

		// builder should still be there
		assertNotNull(((BuildCommand) project.internalGetDescription().getBuildSpec(false)[0]).getBuilders());
	}

	/**
	 * Tests that the description file is not dirtied if the description has not actually
	 * changed.
	 */
	@Test
	public void testDirtyDescription() throws Exception {
		IProject project = getWorkspace().getRoot().getProject("Project");
		IProject target1 = getWorkspace().getRoot().getProject("target1");
		IProject target2 = getWorkspace().getRoot().getProject("target2");
		createInWorkspace(project);
		IFile descriptionFile = project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		assertTrue(descriptionFile.exists());

		long timestamp = descriptionFile.getLocalTimeStamp();

		// wait a bit to ensure that timestamp granularity does not
		// spoil our test
		Thread.sleep(1000);

		IProjectDescription description = project.getDescription();
		description.setBuildSpec(description.getBuildSpec());
		description.setComment(description.getComment());
		description.setDynamicReferences(description.getDynamicReferences());
		description.setLocationURI(description.getLocationURI());
		description.setName(description.getName());
		description.setNatureIds(description.getNatureIds());
		description.setReferencedProjects(description.getReferencedProjects());
		project.setDescription(description, IResource.NONE, null);

		//the timestamp should be the same
		assertEquals(timestamp, descriptionFile.getLocalTimeStamp());

		//adding a dynamic reference should not dirty the file
		description = project.getDescription();
		description.setDynamicReferences(new IProject[] { target1, target2 });
		project.setDescription(description, IResource.NONE, null);

		assertEquals(timestamp, descriptionFile.getLocalTimeStamp());
	}

	/**
	 * Tests that the description file is dirtied if the description has actually
	 * changed. This is a regression test for bug 64128.
	 */
	@Test
	public void testDirtyBuildSpec() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("Project");
		IFile projectDescription = project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		createInWorkspace(project);
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

		assertTrue(modificationStamp != projectDescription.getModificationStamp());
	}

	@Test
	@Deprecated // Explicitly tests deprecated API
	public void testDynamicProjectReferences() throws CoreException {
		IProject target1 = getWorkspace().getRoot().getProject("target1");
		IProject target2 = getWorkspace().getRoot().getProject("target2");
		createInWorkspace(target1);
		createInWorkspace(target2);

		IProject project = getWorkspace().getRoot().getProject("project");
		createInWorkspace(project);

		IProjectDescription description = project.getDescription();
		description.setReferencedProjects(new IProject[] {target1});
		description.setDynamicReferences(new IProject[] {target2});
		project.setDescription(description, createTestMonitor());
		IProject[] refs = project.getReferencedProjects();
		assertThat(refs).containsExactly(target1, target2);
		assertThat(target1.getReferencingProjects()).hasSize(1);
		assertThat(target2.getReferencingProjects()).hasSize(1);

		//get references for a non-existent project
		assertThrows(CoreException.class,
				() -> getWorkspace().getRoot().getProject("DoesNotExist").getReferencedProjects());
	}

	/**
	 * Tests IProjectDescription project references
	 */
	@Test
	public void testProjectReferences() throws CoreException {
		IProject target = getWorkspace().getRoot().getProject("Project1");
		createInWorkspace(target);

		IProject project = getWorkspace().getRoot().getProject("Project2");
		createInWorkspace(project);

		project.open(createTestMonitor());
		IProjectDescription description = project.getDescription();
		description.setReferencedProjects(new IProject[] {target});
		project.setDescription(description, createTestMonitor());
		assertThat(target.getReferencingProjects()).hasSize(1);

		//get references for a non-existent project
		assertThrows(CoreException.class,
				() -> getWorkspace().getRoot().getProject("DoesNotExist").getReferencedProjects());

		//get referencing projects for a non-existent project
		IProject[] refs = getWorkspace().getRoot().getProject("DoesNotExist2").getReferencingProjects();
		assertThat(refs).isEmpty();
	}

}
