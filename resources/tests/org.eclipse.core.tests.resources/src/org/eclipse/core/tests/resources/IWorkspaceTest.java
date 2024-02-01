/*******************************************************************************
 *  Copyright (c) 2000, 2022 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Alexander Kurtakov <akurtako@redhat.com> - Bug 459343
 *******************************************************************************/
package org.eclipse.core.tests.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.harness.FileSystemHelper.getRandomLocation;
import static org.eclipse.core.tests.harness.FileSystemHelper.getTempDir;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.NATURE_CYCLE1;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.NATURE_CYCLE2;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.NATURE_CYCLE3;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.NATURE_EARTH;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.NATURE_INVALID;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.NATURE_MUD;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.NATURE_SIMPLE;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.NATURE_SNOW;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.NATURE_WATER;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.SET_OTHER;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.SET_STATE;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.getInvalidNatureSets;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.getValidNatureSets;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertDoesNotExistInFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertDoesNotExistInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertExistsInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.buildResources;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomContentsStream;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.ensureOutOfSync;
import static org.eclipse.core.tests.resources.ResourceTestUtil.isReadOnlySupported;
import static org.eclipse.core.tests.resources.ResourceTestUtil.removeFromFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.removeFromWorkspace;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.core.internal.resources.TestingSupport;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFilterMatcherDescriptor;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNatureDescriptor;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Platform.OS;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

public class IWorkspaceTest {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	private IResource[] buildResourceHierarchy() throws CoreException {
		return buildResources(getWorkspace().getRoot(),
				new String[] { "/", "/Project/", "/Project/Folder/", "/Project/Folder/File", });
	}

	private void ensureResourceHierarchyExist() throws CoreException {
		createInWorkspace(buildResourceHierarchy());
	}

	/**
	 * Returns the nature descriptor with the given Id, or null if not found
	 */
	protected IProjectNatureDescriptor findNature(IProjectNatureDescriptor[] descriptors, String id) {
		for (IProjectNatureDescriptor descriptor : descriptors) {
			if (descriptor.getNatureId().equals(id)) {
				return descriptor;
			}
		}
		return null;
	}

	/**
	 * Tests handling of runnables that throw OperationCanceledException.
	 */
	@Test
	public void testCancelRunnable() {
		assertThrows(OperationCanceledException.class, () -> getWorkspace().run((IWorkspaceRunnable) monitor -> {
			throw new OperationCanceledException();
		}, createTestMonitor()));
	}

	/**
	 * Performs black box testing of the following method:
	 * 		IStatus copy([IResource, IPath, boolean, IProgressMonitor)
	 * See also testMultiCopy()
	 */
	@Test
	public void testCopy() throws Exception {
		IResource[] resources = buildResourceHierarchy();
		IProject project = (IProject) resources[1];
		IFolder folder = (IFolder) resources[2];
		IFile file = (IFile) resources[3];
		IFile file2 = folder.getFile("File2");
		IFile file3 = folder.getFile("File3");
		IFolder folder2 = project.getFolder("Folder2");
		IFolder folderCopy = folder2.getFolder("Folder");
		IFile fileCopy = folder2.getFile("File");
		IFile file2Copy = folder2.getFile("File2");

		/********** FAILURE CASES ***********/

		// project not open
		assertThrows(CoreException.class,
				() -> getWorkspace().copy(new IResource[] { file }, folder.getFullPath(), false, createTestMonitor()));

		ensureResourceHierarchyExist();

		//copy to bogus destination
		assertThrows(CoreException.class, () -> getWorkspace().copy(new IResource[] { file },
				folder2.getFullPath().append("figment"), false, createTestMonitor()));

		//copy to non-existent destination
		assertThrows(CoreException.class,
				() -> getWorkspace().copy(new IResource[] { file }, folder2.getFullPath(), false, createTestMonitor()));

		//create the destination
		folder2.create(false, true, createTestMonitor());

		//source file doesn't exist
		assertThrows(CoreException.class,
				() -> getWorkspace().copy(new IResource[] { file2 }, folder2.getFullPath(), false, createTestMonitor()));

		//some source files don't exist
		assertThrows(CoreException.class,
				() -> getWorkspace().copy(new IResource[] { file, file2 }, folder2.getFullPath(), false, createTestMonitor()));

		//make sure the first copy worked
		assertTrue("1.5", fileCopy.exists());
		fileCopy.delete(true, createTestMonitor());

		// create the files
		IFile projectFile = project.getFile("ProjectPhile");
		file2.create(createRandomContentsStream(), false, createTestMonitor());
		file3.create(createRandomContentsStream(), false, createTestMonitor());
		projectFile.create(createRandomContentsStream(), false, createTestMonitor());

		//source files aren't siblings
		assertThrows(CoreException.class, () -> getWorkspace().copy(new IResource[] { file, projectFile },
				folder2.getFullPath(), false, createTestMonitor()));

		//source files contains duplicates
		assertThrows(CoreException.class, () -> getWorkspace().copy(new IResource[] { file, file2, file },
				folder2.getFullPath(), false, createTestMonitor()));

		//source can't be prefix of destination
		assertThrows(CoreException.class, () -> {
			IFolder folder3 = folder2.getFolder("Folder3");
			folder3.create(false, true, createTestMonitor());
			getWorkspace().copy(new IResource[] { folder2 }, folder3.getFullPath(), false, createTestMonitor());
		});

		//target exists
		assertThrows(CoreException.class, () -> {
			file2Copy.create(createRandomContentsStream(), false, createTestMonitor());
			getWorkspace().copy(new IResource[] { file, file2 }, folder2.getFullPath(), false, createTestMonitor());
		});
		removeFromWorkspace(file2Copy);
		removeFromFileSystem(file2Copy);

		//make sure the first copy worked
		fileCopy = folder2.getFile("File");
		assertTrue("2.2", fileCopy.exists());
		fileCopy.delete(true, createTestMonitor());

		//resource out of sync with filesystem
		ensureOutOfSync(file);
		assertThrows(CoreException.class,
				() -> getWorkspace().copy(new IResource[] { file }, folder2.getFullPath(), false, createTestMonitor()));

		// make sure "file" is in sync.
		file.refreshLocal(IResource.DEPTH_ZERO, null);
		/********** NON FAILURE CASES ***********/

		//empty resource list
		getWorkspace().copy(new IResource[] {}, folder2.getFullPath(), false, createTestMonitor());

		//copy single file
		getWorkspace().copy(new IResource[] { file }, folder2.getFullPath(), false, createTestMonitor());
		assertTrue("3.2", fileCopy.exists());
		removeFromWorkspace(fileCopy);
		removeFromFileSystem(fileCopy);

		//copy two files
		getWorkspace().copy(new IResource[] { file, file2 }, folder2.getFullPath(), false, createTestMonitor());
		assertTrue("3.4", fileCopy.exists());
		assertTrue("3.5", file2Copy.exists());
		removeFromWorkspace(fileCopy);
		removeFromWorkspace(file2Copy);
		removeFromFileSystem(fileCopy);
		removeFromFileSystem(file2Copy);

		//copy a folder
		getWorkspace().copy(new IResource[] { folder }, folder2.getFullPath(), false, createTestMonitor());
		assertTrue("3.7", folderCopy.exists());
		assertThat(folderCopy.members()).hasSizeGreaterThan(0);
		removeFromWorkspace(folderCopy);
		removeFromFileSystem(folderCopy);
	}

	/**
	 * Performs black box testing of the following method:
	 * 		IStatus delete([IResource, boolean, IProgressMonitor)
	 */
	@Test
	public void testDelete() throws CoreException {
		IResource[] resources = buildResourceHierarchy();
		IProject project = (IProject) resources[1];
		IFolder folder = (IFolder) resources[2];
		IFile file = (IFile) resources[3];

		//delete non-existent resources
		assertTrue(getWorkspace().delete(new IResource[] {project, folder, file}, false, createTestMonitor()).isOK());
		assertTrue(getWorkspace().delete(new IResource[] {file}, false, createTestMonitor()).isOK());
		assertTrue(getWorkspace().delete(new IResource[] {}, false, createTestMonitor()).isOK());
		ensureResourceHierarchyExist();

		//delete existing resources
		resources = new IResource[] {file, project, folder};
		assertTrue(getWorkspace().delete(resources, false, createTestMonitor()).isOK());
		//	assertDoesNotExistInFileSystem(resources);
		assertDoesNotExistInWorkspace(resources);
		ensureResourceHierarchyExist();
		resources = new IResource[] {file};
		assertTrue(getWorkspace().delete(resources, false, createTestMonitor()).isOK());
		assertDoesNotExistInFileSystem(resources);
		assertDoesNotExistInWorkspace(resources);
		file.create(createRandomContentsStream(), false, createTestMonitor());
		resources = new IResource[] {};
		assertTrue(getWorkspace().delete(resources, false, createTestMonitor()).isOK());
		assertDoesNotExistInFileSystem(resources);
		assertDoesNotExistInWorkspace(resources);
		ensureResourceHierarchyExist();

		//delete a combination of existing and non-existent resources
		IProject fakeProject = getWorkspace().getRoot().getProject("pigment");
		IFolder fakeFolder = fakeProject.getFolder("ligament");
		resources = new IResource[] {file, folder, fakeFolder, project, fakeProject};
		assertTrue(getWorkspace().delete(resources, false, createTestMonitor()).isOK());
		//	assertDoesNotExistInFileSystem(resources);
		assertDoesNotExistInWorkspace(resources);
		ensureResourceHierarchyExist();
		resources = new IResource[] {fakeProject, file};
		assertTrue(getWorkspace().delete(resources, false, createTestMonitor()).isOK());
		assertDoesNotExistInFileSystem(resources);
		assertDoesNotExistInWorkspace(resources);
		file.create(createRandomContentsStream(), false, createTestMonitor());
		resources = new IResource[] {fakeProject};
		assertTrue(getWorkspace().delete(resources, false, createTestMonitor()).isOK());
		//	assertDoesNotExistInFileSystem(resources);
		assertDoesNotExistInWorkspace(resources);
		ensureResourceHierarchyExist();
	}

	/**
	 * Performs black box testing of the following method:
	 * 	{@link IWorkspace#forgetSavedTree(String)}.
	 */
	@Test
	public void testForgetSavedTree() {
		// according to javadoc spec, null means forget all plugin trees
		getWorkspace().forgetSavedTree(null);
	}

	/**
	 * Performs black box testing of the following method:
	 *     IProjectNatureDescriptor[] getNatureDescriptors()
	 */
	@Test
	public void testGetNatureDescriptors() {
		//NOTE: see static fields for description of available test natures
		IProjectNatureDescriptor[] descriptors = getWorkspace().getNatureDescriptors();

		IProjectNatureDescriptor current = findNature(descriptors, NATURE_SIMPLE);
		assertNotNull("2.0", current);
		assertEquals("2.1", NATURE_SIMPLE, current.getNatureId());
		assertEquals("2.2", "Simple", current.getLabel());
		assertThat(current.getRequiredNatureIds()).isEmpty();
		assertThat(current.getNatureSetIds()).isEmpty();

		current = findNature(descriptors, NATURE_SNOW);
		assertNotNull("3.0", current);
		assertEquals("3.1", NATURE_SNOW, current.getNatureId());
		assertEquals("3.2", "Snow", current.getLabel());
		String[] required = current.getRequiredNatureIds();
		assertThat(required).containsExactly(NATURE_WATER);
		String[] sets = current.getNatureSetIds();
		assertThat(sets).containsExactly(SET_OTHER);

		current = findNature(descriptors, NATURE_WATER);
		assertNotNull("4.0", current);
		assertEquals("4.1", NATURE_WATER, current.getNatureId());
		assertEquals("4.2", "Water", current.getLabel());
		required = current.getRequiredNatureIds();
		assertThat(required).isEmpty();
		sets = current.getNatureSetIds();
		assertThat(sets).containsExactly(SET_STATE);

		current = findNature(descriptors, NATURE_EARTH);
		assertNotNull("5.0", current);
		assertEquals("5.1", NATURE_EARTH, current.getNatureId());
		assertEquals("5.2", "Earth", current.getLabel());
		required = current.getRequiredNatureIds();
		assertThat(required).isEmpty();
		sets = current.getNatureSetIds();
		assertThat(sets).containsExactly(SET_STATE);

		current = findNature(descriptors, NATURE_MUD);
		assertNotNull("6.0", current);
		assertEquals("6.1", NATURE_MUD, current.getNatureId());
		assertEquals("6.2", "Mud", current.getLabel());
		required = current.getRequiredNatureIds();
		//water and earth are required for mud
		assertThat(required).containsExactlyInAnyOrder(NATURE_WATER, NATURE_EARTH);
		sets = current.getNatureSetIds();
		assertThat(sets).containsExactly(SET_OTHER);

		current = findNature(descriptors, NATURE_INVALID);
		assertNotNull("7.0", current);
		assertEquals("7.1", NATURE_INVALID, current.getNatureId());
		assertEquals("7.2", "", current.getLabel());
		required = current.getRequiredNatureIds();
		assertThat(required).isEmpty();
		sets = current.getNatureSetIds();
		assertThat(sets).isEmpty();

		current = findNature(descriptors, NATURE_CYCLE1);
		assertNotNull("8.0", current);
		assertEquals("8.1", NATURE_CYCLE1, current.getNatureId());
		assertEquals("8.2", "Cycle1", current.getLabel());
		required = current.getRequiredNatureIds();
		assertThat(required).containsExactly(NATURE_CYCLE2);
		sets = current.getNatureSetIds();
		assertThat(sets).isEmpty();

		current = findNature(descriptors, NATURE_CYCLE2);
		assertNotNull("5.0", current);
		assertEquals("9.1", NATURE_CYCLE2, current.getNatureId());
		assertEquals("9.2", "Cycle2", current.getLabel());
		required = current.getRequiredNatureIds();
		assertThat(required).containsExactly(NATURE_CYCLE3);
		sets = current.getNatureSetIds();
		assertThat(sets).isEmpty();

		current = findNature(descriptors, NATURE_CYCLE3);
		assertNotNull("10.0", current);
		assertEquals("10.1", NATURE_CYCLE3, current.getNatureId());
		assertEquals("10.2", "Cycle3", current.getLabel());
		required = current.getRequiredNatureIds();
		assertThat(required).containsExactly(NATURE_CYCLE1);
		sets = current.getNatureSetIds();
		assertThat(sets).isEmpty();
	}

	/**
	 * Performs black box testing of the following method:
	 *     IProjectNatureDescriptor getNatureDescriptor(String)
	 */
	@Test
	public void testGetNatureDescriptor() {
		//NOTE: see static fields for description of available test natures
		IWorkspace ws = getWorkspace();

		IProjectNatureDescriptor current = ws.getNatureDescriptor(NATURE_SIMPLE);
		assertNotNull("2.0", current);
		assertEquals("2.1", NATURE_SIMPLE, current.getNatureId());
		assertEquals("2.2", "Simple", current.getLabel());
		assertThat(current.getRequiredNatureIds()).isEmpty();
		assertThat(current.getNatureSetIds()).isEmpty();

		current = ws.getNatureDescriptor(NATURE_SNOW);
		assertNotNull("3.0", current);
		assertEquals("3.1", NATURE_SNOW, current.getNatureId());
		assertEquals("3.2", "Snow", current.getLabel());
		String[] required = current.getRequiredNatureIds();
		assertThat(required).containsExactly(NATURE_WATER);
		String[] sets = current.getNatureSetIds();
		assertThat(sets).containsExactly(SET_OTHER);

		current = ws.getNatureDescriptor(NATURE_WATER);
		assertNotNull("4.0", current);
		assertEquals("4.1", NATURE_WATER, current.getNatureId());
		assertEquals("4.2", "Water", current.getLabel());
		required = current.getRequiredNatureIds();
		assertThat(required).isEmpty();
		sets = current.getNatureSetIds();
		assertThat(sets).containsExactly(SET_STATE);

		current = ws.getNatureDescriptor(NATURE_EARTH);
		assertNotNull("5.0", current);
		assertEquals("5.1", NATURE_EARTH, current.getNatureId());
		assertEquals("5.2", "Earth", current.getLabel());
		required = current.getRequiredNatureIds();
		assertThat(required).isEmpty();
		sets = current.getNatureSetIds();
		assertThat(sets).containsExactly(SET_STATE);

		current = ws.getNatureDescriptor(NATURE_MUD);
		assertNotNull("6.0", current);
		assertEquals("6.1", NATURE_MUD, current.getNatureId());
		assertEquals("6.2", "Mud", current.getLabel());
		required = current.getRequiredNatureIds();
		assertThat(required).containsExactlyInAnyOrder(NATURE_WATER, NATURE_EARTH);
		sets = current.getNatureSetIds();
		assertThat(sets).containsExactly(SET_OTHER);

		current = ws.getNatureDescriptor(NATURE_INVALID);
		assertNotNull("7.0", current);
		assertEquals("7.1", NATURE_INVALID, current.getNatureId());
		assertEquals("7.2", "", current.getLabel());
		required = current.getRequiredNatureIds();
		assertThat(required).isEmpty();
		sets = current.getNatureSetIds();
		assertThat(sets).isEmpty();

		current = ws.getNatureDescriptor(NATURE_CYCLE1);
		assertNotNull("8.0", current);
		assertEquals("8.1", NATURE_CYCLE1, current.getNatureId());
		assertEquals("8.2", "Cycle1", current.getLabel());
		required = current.getRequiredNatureIds();
		assertThat(required).containsExactly(NATURE_CYCLE2);
		sets = current.getNatureSetIds();
		assertThat(sets).isEmpty();

		current = ws.getNatureDescriptor(NATURE_CYCLE2);
		assertNotNull("5.0", current);
		assertEquals("9.1", NATURE_CYCLE2, current.getNatureId());
		assertEquals("9.2", "Cycle2", current.getLabel());
		required = current.getRequiredNatureIds();
		assertThat(required).containsExactly(NATURE_CYCLE3);
		sets = current.getNatureSetIds();
		assertThat(sets).isEmpty();

		current = ws.getNatureDescriptor(NATURE_CYCLE3);
		assertNotNull("10.0", current);
		assertEquals("10.1", NATURE_CYCLE3, current.getNatureId());
		assertEquals("10.2", "Cycle3", current.getLabel());
		required = current.getRequiredNatureIds();
		assertThat(required).containsExactly(NATURE_CYCLE1);
		sets = current.getNatureSetIds();
		assertThat(sets).isEmpty();
	}

	/**
	 * Performs black box testing of the following method:
	 *     IStatus move([IResource, IPath, boolean, IProgressMonitor)
	 */
	@Test
	public void testMove() throws CoreException {
		/* create folders and files */
		IProject project = getWorkspace().getRoot().getProject("Project");
		IFolder folder = project.getFolder("folder");
		IFile file = project.getFile("file.txt");
		IFile anotherFile = project.getFile("anotherFile.txt");
		IFile oneMoreFile = project.getFile("oneMoreFile.txt");
		createInWorkspace(new IResource[] {project, folder, file, anotherFile, oneMoreFile});

		/* normal case */
		IResource[] resources = {file, anotherFile, oneMoreFile};
		getWorkspace().move(resources, folder.getFullPath(), true, createTestMonitor());
		assertFalse("1.1", file.exists());
		assertFalse("1.2", anotherFile.exists());
		assertFalse("1.3", oneMoreFile.exists());
		assertTrue("1.4", folder.getFile(file.getName()).exists());
		assertTrue("1.5", folder.getFile(anotherFile.getName()).exists());
		assertTrue("1.6", folder.getFile(oneMoreFile.getName()).exists());

		/* test duplicates */
		resources = new IResource[] {folder.getFile(file.getName()), folder.getFile(anotherFile.getName()), folder.getFile(oneMoreFile.getName()), folder.getFile(oneMoreFile.getName())};
		IStatus status = getWorkspace().move(resources, project.getFullPath(), true, createTestMonitor());
		assertTrue("2.1", status.isOK());
		assertTrue("2.3", file.exists());
		assertTrue("2.4", anotherFile.exists());
		assertTrue("2.5", oneMoreFile.exists());
		assertFalse("2.6", folder.getFile(file.getName()).exists());
		assertFalse("2.7", folder.getFile(anotherFile.getName()).exists());
		assertFalse("2.8", folder.getFile(oneMoreFile.getName()).exists());

		/* test no simblings */
		IResource[] resources2 = new IResource[] { file, anotherFile, oneMoreFile, project };
		CoreException ex = assertThrows(CoreException.class,
				() -> getWorkspace().move(resources2, folder.getFullPath(), true, createTestMonitor()));
		assertFalse("3.1", ex.getStatus().isOK());
		assertThat(ex.getStatus().getChildren()).hasSize(1);
		assertFalse("3.3", file.exists());
		assertFalse("3.4", anotherFile.exists());
		assertFalse("3.5", oneMoreFile.exists());
		assertTrue("3.6", folder.getFile(file.getName()).exists());
		assertTrue("3.7", folder.getFile(anotherFile.getName()).exists());
		assertTrue("3.8", folder.getFile(oneMoreFile.getName()).exists());

		/* inexisting resource */
		IResource[] resources3 = new IResource[] { folder.getFile(file.getName()),
				folder.getFile(anotherFile.getName()), folder.getFile("inexisting"),
				folder.getFile(oneMoreFile.getName()) };
		CoreException ex2 = assertThrows(CoreException.class,
				() -> getWorkspace().move(resources3, project.getFullPath(), true, createTestMonitor()));
		assertFalse("4.1", ex2.getStatus().isOK());
		assertTrue("4.3", file.exists());
		assertTrue("4.4", anotherFile.exists());
		assertTrue("4.5", oneMoreFile.exists());
		assertFalse("4.6", folder.getFile(file.getName()).exists());
		assertFalse("4.7", folder.getFile(anotherFile.getName()).exists());
		assertFalse("4.8", folder.getFile(oneMoreFile.getName()).exists());
	}

	/**
	 * Another test method for IWorkspace.copy().  See also testCopy
	 */
	@Test
	public void testMultiCopy() throws Exception {
		/* create common objects */
		IResource[] resources = buildResourceHierarchy();
		IProject project = (IProject) resources[1];
		IFolder folder = (IFolder) resources[2];

		/* create folder and file */
		createInWorkspace(folder);
		createInFileSystem(folder);
		IFile file1 = project.getFile("file.txt");
		createInWorkspace(file1);
		createInFileSystem(file1);
		IFile anotherFile = project.getFile("anotherFile.txt");
		createInWorkspace(anotherFile);
		createInFileSystem(anotherFile);
		IFile oneMoreFile = project.getFile("oneMoreFile.txt");
		createInWorkspace(oneMoreFile);
		createInFileSystem(oneMoreFile);

		/* normal case */
		resources = new IResource[] {file1, anotherFile, oneMoreFile};
		getWorkspace().copy(resources, folder.getFullPath(), true, createTestMonitor());
		assertTrue("1.1", file1.exists());
		assertTrue("1.2", anotherFile.exists());
		assertTrue("1.3", oneMoreFile.exists());
		assertTrue("1.4", folder.getFile(file1.getName()).exists());
		assertTrue("1.5", folder.getFile(anotherFile.getName()).exists());
		assertTrue("1.6", folder.getFile(oneMoreFile.getName()).exists());
		removeFromWorkspace(folder.getFile(file1.getName()));
		removeFromWorkspace(folder.getFile(anotherFile.getName()));
		removeFromWorkspace(folder.getFile(oneMoreFile.getName()));
		removeFromFileSystem(folder.getFile(file1.getName()));
		removeFromFileSystem(folder.getFile(anotherFile.getName()));
		removeFromFileSystem(folder.getFile(oneMoreFile.getName()));

		/* test duplicates */
		resources = new IResource[] {file1, anotherFile, oneMoreFile, file1};
		getWorkspace().copy(resources, folder.getFullPath(), true, createTestMonitor());
		assertTrue("2.2", file1.exists());
		assertTrue("2.3", anotherFile.exists());
		assertTrue("2.4", oneMoreFile.exists());
		assertTrue("2.5", folder.getFile(file1.getName()).exists());
		assertTrue("2.6", folder.getFile(anotherFile.getName()).exists());
		assertTrue("2.7", folder.getFile(oneMoreFile.getName()).exists());
		removeFromWorkspace(folder.getFile(file1.getName()));
		removeFromWorkspace(folder.getFile(anotherFile.getName()));
		removeFromWorkspace(folder.getFile(oneMoreFile.getName()));
		removeFromFileSystem(folder.getFile(file1.getName()));
		removeFromFileSystem(folder.getFile(anotherFile.getName()));
		removeFromFileSystem(folder.getFile(oneMoreFile.getName()));

		/* test no siblings */
		IResource[] resources2 = new IResource[] { file1, anotherFile, oneMoreFile, project };
		CoreException e = assertThrows(CoreException.class,
				() -> getWorkspace().copy(resources2, folder.getFullPath(), true, createTestMonitor()));
		IStatus status = e.getStatus();
		assertFalse("3.1", status.isOK());
		assertThat(status.getChildren()).hasSize(1);
		assertTrue("3.3", file1.exists());
		assertTrue("3.4", anotherFile.exists());
		assertTrue("3.5", oneMoreFile.exists());
		assertTrue("3.6", folder.getFile(file1.getName()).exists());
		assertTrue("3.7", folder.getFile(anotherFile.getName()).exists());
		assertTrue("3.8", folder.getFile(oneMoreFile.getName()).exists());
		removeFromWorkspace(folder.getFile(file1.getName()));
		removeFromWorkspace(folder.getFile(anotherFile.getName()));
		removeFromWorkspace(folder.getFile(oneMoreFile.getName()));
		removeFromFileSystem(folder.getFile(file1.getName()));
		removeFromFileSystem(folder.getFile(anotherFile.getName()));
		removeFromFileSystem(folder.getFile(oneMoreFile.getName()));

		/* inexisting resource */
		IResource[] resources3 = new IResource[] { file1, anotherFile, project.getFile("inexisting"), oneMoreFile };
		CoreException ex = assertThrows(CoreException.class,
				() -> getWorkspace().copy(resources3, folder.getFullPath(), true, createTestMonitor()));
		status = ex.getStatus();
		assertFalse("4.1", status.isOK());
		assertTrue("4.2", file1.exists());
		assertTrue("4.3", anotherFile.exists());
		assertTrue("4.4", oneMoreFile.exists());
		assertTrue("4.5", folder.getFile(file1.getName()).exists());
		assertTrue("4.6", folder.getFile(anotherFile.getName()).exists());
		assertTrue("4.7 Fails because of 1FVFOOQ", folder.getFile(oneMoreFile.getName()).exists());

		/* copy projects should not be allowed */
		IResource destination = getWorkspace().getRoot().getProject("destination");
		CoreException ex2 = assertThrows(CoreException.class,
				() -> getWorkspace().copy(new IResource[] { project }, destination.getFullPath(), true, createTestMonitor()));
		status = ex2.getStatus();
		assertFalse("5.1", status.isOK());
		assertThat(status.getChildren()).hasSize(1);
	}

	@Test
	public void testMultiCreation() throws Throwable {
		final IProject project = getWorkspace().getRoot().getProject("bar");
		final IResource[] resources = buildResources(project, new String[] {"a/", "a/b"});
		IWorkspaceRunnable body = monitor -> {
			project.create(null);
			project.open(null);
			// define an operation which will create a bunch of resources including a project.
			for (IResource resource : resources) {
				switch (resource.getType()) {
					case IResource.FILE :
						((IFile) resource).create(null, false, createTestMonitor());
						break;
					case IResource.FOLDER :
						((IFolder) resource).create(false, true, createTestMonitor());
						break;
					case IResource.PROJECT :
						((IProject) resource).create(createTestMonitor());
						break;
				}
			}
		};
		getWorkspace().run(body, createTestMonitor());
		assertExistsInWorkspace(project);
		assertExistsInWorkspace(resources);
	}

	@Test
	public void testMultiDeletion() throws Throwable {
		IProject project = getWorkspace().getRoot().getProject("testProject");
		IResource[] before = buildResources(project, new String[] {"c/", "c/b/", "c/x", "c/b/y", "c/b/z"});
		createInWorkspace(before);
		//
		assertExistsInWorkspace(before);
		getWorkspace().delete(before, true, createTestMonitor());
		assertDoesNotExistInWorkspace(before);
	}

	/**
	 * Test thread safety of the API method IWorkspace.setDescription.
	 */
	@Test
	public void testMultiSetDescription() throws CoreException {
		final int THREAD_COUNT = 2;
		final CoreException[] errorPointer = new CoreException[1];
		Thread[] threads = new Thread[THREAD_COUNT];
		for (int i = 0; i < THREAD_COUNT; i++) {
			threads[i] = new Thread((Runnable) () -> {
				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				IWorkspaceDescription description = workspace.getDescription();
				for (int j = 0; j < 100; j++) {
					description.setAutoBuilding(false);
					try {
						workspace.setDescription(description);
					} catch (CoreException e1) {
						errorPointer[0] = e1;
						return;
					}
					description.setAutoBuilding(true);
					try {
						workspace.setDescription(description);
					} catch (CoreException e2) {
						errorPointer[0] = e2;
						return;
					}
				}
			}, "Autobuild " + i);
			threads[i].start();
		}
		for (Thread thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
			}
		}
		if (errorPointer[0] != null) {
			throw errorPointer[0];
		}
	}

	/**
	 * Test API method IWorkspace.setDescription.
	 */
	@Test
	public void testSave() throws CoreException {
		// ensure save returns a warning if a project's .project file is deleted.
		IProject project = getWorkspace().getRoot().getProject("Broken");
		createInWorkspace(project);
		// wait for snapshot before modifying file
		TestingSupport.waitForSnapshot();
		IFile descriptionFile = project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		descriptionFile.delete(IResource.NONE, null);
		IStatus result = getWorkspace().save(true, createTestMonitor());
		assertEquals("1.0", IStatus.WARNING, result.getSeverity());
	}

	/**
	 * Performs black box testing of the following method:
	 *     String[] sortNatureSet(String[])
	 */
	@Test
	public void testSortNatureSet() {
		//NOTE: see static fields for description of available test natures
		IWorkspace ws = getWorkspace();

		//invalid sets shouldn't fail
		String[][] invalid = getInvalidNatureSets();
		for (String[] element : invalid) {
			String[] sorted = ws.sortNatureSet(element);
			//set may grow if it contained duplicates
			assertThat(sorted).hasSizeLessThanOrEqualTo(element.length);
		}
		String[] sorted = ws.sortNatureSet(new String[] {});
		assertThat(sorted).isEmpty();

		sorted = ws.sortNatureSet(new String[] {NATURE_SIMPLE});
		assertThat(sorted).containsExactly(NATURE_SIMPLE);

		sorted = ws.sortNatureSet(new String[] {NATURE_SNOW, NATURE_WATER});
		assertThat(sorted).containsExactly(NATURE_WATER, NATURE_SNOW);

		sorted = ws.sortNatureSet(new String[] {NATURE_WATER, NATURE_SIMPLE, NATURE_SNOW});
		assertThat(sorted).satisfiesAnyOf(order -> assertThat(order).containsExactly(NATURE_WATER, NATURE_SNOW, NATURE_SIMPLE),
				order -> assertThat(order).containsExactly(NATURE_WATER, NATURE_SIMPLE, NATURE_SNOW),
				order -> assertThat(order).containsExactly(NATURE_SIMPLE, NATURE_WATER, NATURE_SNOW));
	}

	@Test
	public void testValidateEdit() throws CoreException {
		// We need to know whether or not we can unset the read-only flag
		// in order to perform this test.
		if (!isReadOnlySupported()) {
			return;
		}
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		IFile file = project.getFile("myfile.txt");
		createInWorkspace(new IResource[] {project, file});
		IStatus result = getWorkspace().validateEdit(new IFile[] {file}, null);
		assertTrue("1.0", result.isOK());
		file.setReadOnly(true);
		result = getWorkspace().validateEdit(new IFile[] {file}, null);
		assertEquals("1.1", IStatus.ERROR, result.getSeverity());
		//	assertEquals("1.2", IResourceStatus.READ_ONLY_LOCAL, result.getCode());
		// remove the read-only status so test cleanup will work ok
		file.setReadOnly(false);
	}

	@Test
	public void testValidateLinkLocation() {
		// TODO
		// see also: some tests in LinkedResourceWithPathVariableTest
	}

	/**
	 * Performs black box testing of the following method:
	 *     IStatus validateName(String, int)
	 */
	@Test
	public void testValidateName() {
		/* normal name */
		assertTrue("1.1", getWorkspace().validateName("abcdef", IResource.FILE).isOK());
		/* invalid characters (windows only) */
		if (OS.isWindows()) {
			assertFalse("2.1", getWorkspace().validateName("dsa:sf", IResource.FILE).isOK());
			assertFalse("2.2", getWorkspace().validateName("*dsasf", IResource.FILE).isOK());
			assertFalse("2.3", getWorkspace().validateName("?dsasf", IResource.FILE).isOK());
			assertFalse("2.4", getWorkspace().validateName("\"dsasf", IResource.FILE).isOK());
			assertFalse("2.5", getWorkspace().validateName("<dsasf", IResource.FILE).isOK());
			assertFalse("2.6", getWorkspace().validateName(">dsasf", IResource.FILE).isOK());
			assertFalse("2.7", getWorkspace().validateName("|dsasf", IResource.FILE).isOK());
			assertFalse("2.8", getWorkspace().validateName("\"dsasf", IResource.FILE).isOK());
			assertFalse("2.10", getWorkspace().validateName("\\dsasf", IResource.FILE).isOK());
			assertFalse("2.11", getWorkspace().validateName("...", IResource.PROJECT).isOK());
			assertFalse("2.12", getWorkspace().validateName("foo.", IResource.FILE).isOK());
			for (int i = 0; i <= 31; i++) {
				assertFalse("2.13 Windows should NOT accept character #" + i,
						getWorkspace().validateName("anything" + ((char) i) + "something", IResource.FILE).isOK());
			}
			assertTrue("2.14 Windows should accept character #" + 32,
					getWorkspace().validateName("anything" + ((char) 32) + "something", IResource.FILE).isOK());
			assertFalse("2.15 Windows should NOT accept space at the end",
					getWorkspace().validateName("foo ", IResource.FILE).isOK());
			assertTrue("2.16 Windows should accept space in the middle",
					getWorkspace().validateName("fo o", IResource.FILE).isOK());
			assertTrue("2.17 Windows should accept space in at the beginning",
					getWorkspace().validateName(" foo", IResource.FILE).isOK());
		} else {
			//trailing dots are ok on other platforms
			assertTrue("3.3", getWorkspace().validateName("...", IResource.FILE).isOK());
			assertTrue("3.4", getWorkspace().validateName("....", IResource.PROJECT).isOK());
			assertTrue("3.7", getWorkspace().validateName("abc.", IResource.FILE).isOK());
			for (int i = 1; i <= 32; i++) {
				assertTrue("3.8 Unix-style filesystems should accept character #" + i,
						getWorkspace().validateName("anything" + ((char) i) + "something", IResource.FILE).isOK());
			}
			assertTrue("3.9 Unix-style filesystems should accept space at the end",
					getWorkspace().validateName("foo ", IResource.FILE).isOK());
		}
		/* invalid characters on all platforms */
		assertFalse("2.9", getWorkspace().validateName("/dsasf", IResource.FILE).isOK());
		assertFalse("2.9", getWorkspace().validateName("", IResource.FILE).isOK());

		/* dots */
		assertFalse("3.1", getWorkspace().validateName(".", IResource.FILE).isOK());
		assertFalse("3.2", getWorkspace().validateName("..", IResource.FILE).isOK());
		assertTrue("3.3", getWorkspace().validateName("...z", IResource.FILE).isOK());
		assertTrue("3.4", getWorkspace().validateName("....z", IResource.FILE).isOK());
		assertTrue("3.5", getWorkspace().validateName("....abc", IResource.FILE).isOK());
		assertTrue("3.6", getWorkspace().validateName("abc....def", IResource.FILE).isOK());
		assertTrue("3.7", getWorkspace().validateName("abc.d...z", IResource.FILE).isOK());
	}

	/**
	 * Performs black box testing of the following method:
	 *     IStatus validateNatureSet(String[])
	 */
	@Test
	public void testValidateNatureSet() {
		//NOTE: see static fields for description of available test natures
		IWorkspace ws = getWorkspace();

		String[][] invalid = getInvalidNatureSets();
		for (int i = 0; i < invalid.length; i++) {
			IStatus result = ws.validateNatureSet(invalid[i]);
			assertFalse("invalid (severity): " + i, result.isOK());
			assertNotEquals("invalid (code): " + i, IStatus.OK, result.getCode());
		}
		String[][] valid = getValidNatureSets();
		for (int i = 0; i < valid.length; i++) {
			IStatus result = ws.validateNatureSet(valid[i]);
			assertTrue("valid (severity): " + i, result.isOK());
			assertEquals("valid (code): " + i, IStatus.OK, result.getCode());
		}
	}

	/**
	 * Performs black box testing of the following method:
	 *     IStatus validatePath(String, int)
	 */
	@Test
	public void testValidatePath() {
		/* normal path */
		assertTrue("1.1", getWorkspace().validatePath("/one/two/three/four/", IResource.FILE | IResource.FOLDER).isOK());

		/* invalid characters (windows only) */
		final boolean WINDOWS = OS.isWindows();
		if (WINDOWS) {
			assertFalse("2.1", (getWorkspace().validatePath("\\dsa:sf", IResource.FILE).isOK()));
			assertFalse("2.2", (getWorkspace().validatePath("/abc/*dsasf", IResource.FILE).isOK()));
			assertFalse("2.3", (getWorkspace().validatePath("/abc/?dsasf", IResource.FILE).isOK()));
			assertFalse("2.4", (getWorkspace().validatePath("/abc/\"dsasf", IResource.FILE).isOK()));
			assertFalse("2.5", (getWorkspace().validatePath("/abc/<dsasf", IResource.FILE).isOK()));
			assertFalse("2.6", (getWorkspace().validatePath("/abc/>dsasf", IResource.FILE).isOK()));
			assertFalse("2.7", (getWorkspace().validatePath("/abc/|dsasf", IResource.FILE).isOK()));
			assertFalse("2.8", (getWorkspace().validatePath("/abc/\"dsasf", IResource.FILE).isOK()));

			assertFalse("5.2", (getWorkspace().validatePath("\\", IResource.FILE).isOK()));
			assertFalse("5.4", (getWorkspace().validatePath("device:/abc/123", IResource.FILE).isOK()));

			//trailing dots in segments names not allowed on Windows
			assertFalse("3.1", getWorkspace().validatePath("/abc/.../defghi", IResource.FILE).isOK());
			assertFalse("3.2", getWorkspace().validatePath("/abc/..../defghi", IResource.FILE).isOK());
			assertFalse("3.3", getWorkspace().validatePath("/abc/def..../ghi", IResource.FILE).isOK());
		} else {
			assertTrue("3.1", getWorkspace().validatePath("/abc/.../defghi", IResource.FILE).isOK());
			assertTrue("3.2", getWorkspace().validatePath("/abc/..../defghi", IResource.FILE).isOK());
			assertTrue("3.3", getWorkspace().validatePath("/abc/def..../ghi", IResource.FILE).isOK());
		}

		/* dots */
		assertTrue("3.4", getWorkspace().validatePath("/abc/../ghi/j", IResource.FILE).isOK());
		assertTrue("3.5", getWorkspace().validatePath("/abc/....def/ghi", IResource.FILE).isOK());
		assertTrue("3.6", getWorkspace().validatePath("/abc/def....ghi/jkl", IResource.FILE).isOK());

		/* test hiding incorrect characters using .. and device separator : */
		assertTrue("4.1", getWorkspace().validatePath("/abc/.?./../def/as", IResource.FILE).isOK());
		assertTrue("4.2", getWorkspace().validatePath("/abc/;*?\"'/../def/safd", IResource.FILE).isOK());
		assertTrue("4.3", getWorkspace().validatePath("/abc;*?\"':/def/asdf/sadf", IResource.FILE).isOK() != WINDOWS);

		/* other invalid paths */
		assertFalse("5.1", (getWorkspace().validatePath("/", IResource.FILE).isOK()));
		assertFalse("5.3", (getWorkspace().validatePath("", IResource.FILE).isOK()));

		/* test types / segments */
		assertTrue("6.6", getWorkspace().validatePath("/asf", IResource.PROJECT).isOK());
		assertFalse("6.7", (getWorkspace().validatePath("/asf", IResource.FILE).isOK()));
		// note this is value for a file OR project (note the logical OR)
		assertTrue("6.8", getWorkspace().validatePath("/asf", IResource.PROJECT | IResource.FILE).isOK());
		assertTrue("6.10", getWorkspace().validatePath("/project/.metadata", IResource.FILE).isOK());
		// FIXME: Should this be valid?
		assertTrue("6.11", getWorkspace().validatePath("/.metadata/project", IResource.FILE).isOK());
	}

	/**
	 * Performs black box testing of the following method:
	 *     IStatus validateProjectLocation(IProject, IPath)
	 */
	@Test
	public void testValidateProjectLocation() throws CoreException {
		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject("Project");

		/* normal path */
		assertTrue("1.1", workspace.validateProjectLocation(project, IPath.fromOSString("/one/two/three/four/")).isOK());

		/* invalid characters (windows only) */
		final boolean WINDOWS = OS.isWindows();
		if (WINDOWS) {
			assertFalse("2.1", workspace.validateProjectLocation(project, IPath.fromOSString("d:\\dsa:sf")).isOK());
			assertFalse("2.2", workspace.validateProjectLocation(project, IPath.fromOSString("/abc/*dsasf")).isOK());
			assertFalse("2.3", workspace.validateProjectLocation(project, IPath.fromOSString("/abc/?dsasf")).isOK());
			assertFalse("2.4", workspace.validateProjectLocation(project, IPath.fromOSString("/abc/\"dsasf")).isOK());
			assertFalse("2.5", workspace.validateProjectLocation(project, IPath.fromOSString("/abc/<dsasf")).isOK());
			assertFalse("2.6", workspace.validateProjectLocation(project, IPath.fromOSString("/abc/>dsasf")).isOK());
			assertFalse("2.7", workspace.validateProjectLocation(project, IPath.fromOSString("/abc/|dsasf")).isOK());
			assertFalse("2.8", workspace.validateProjectLocation(project, IPath.fromOSString("/abc/\"dsasf")).isOK());

			//trailing dots invalid on Windows
			assertFalse("3.1", workspace.validateProjectLocation(project, IPath.fromOSString("/abc/.../defghi")).isOK());
			assertFalse("3.2", workspace.validateProjectLocation(project, IPath.fromOSString("/abc/..../defghi")).isOK());
			assertFalse("3.3", workspace.validateProjectLocation(project, IPath.fromOSString("/abc/def..../ghi")).isOK());
		} else {
			assertTrue("3.1", workspace.validateProjectLocation(project, IPath.fromOSString("/abc/.../defghi")).isOK());
			assertTrue("3.2", workspace.validateProjectLocation(project, IPath.fromOSString("/abc/..../defghi")).isOK());
			assertTrue("3.3", workspace.validateProjectLocation(project, IPath.fromOSString("/abc/def..../ghi")).isOK());
		}

		/* dots */
		assertTrue("3.4", workspace.validateProjectLocation(project, IPath.fromOSString("/abc/....def/ghi")).isOK());
		assertTrue("3.5", workspace.validateProjectLocation(project, IPath.fromOSString("/abc/def....ghi/jkl")).isOK());

		/* test hiding incorrect characters using .. and device separator : */
		assertTrue("4.1", workspace.validateProjectLocation(project, IPath.fromOSString("/abc/.?./../def/as")).isOK());
		assertTrue("4.2", workspace.validateProjectLocation(project, IPath.fromOSString("/abc/;*?\"'/../def/safd")).isOK());
		assertFalse("4.3",
				(workspace.validateProjectLocation(project, IPath.fromOSString("c:/abc;*?\"':/def/asdf/sadf")).isOK()));

		// cannot overlap the platform directory
		IPath platformLocation = Platform.getLocation();
		assertFalse("5.1",
				(workspace.validateProjectLocation(project, new Path(platformLocation.getDevice(), "/")).isOK()));
		assertFalse("5.2",
				(workspace.validateProjectLocation(project, new Path(platformLocation.getDevice(), "\\")).isOK()));
		assertFalse("5.3",
				(workspace.validateProjectLocation(project, new Path(platformLocation.getDevice(), "")).isOK()));
		assertFalse("5.4", (workspace.validateProjectLocation(project, platformLocation).isOK()));
		assertFalse("5.5", (workspace.validateProjectLocation(project, platformLocation.append("foo")).isOK()));

		//can overlap platform directory on another device
		IPath anotherDevice = platformLocation.setDevice("u:");
		assertTrue("6.1", workspace.validateProjectLocation(project, new Path("u:", "/")).isOK());
		if (WINDOWS) {
			assertTrue("6.2", workspace.validateProjectLocation(project, new Path("u:", "\\")).isOK());
		}
		assertTrue("6.4", workspace.validateProjectLocation(project, anotherDevice).isOK());
		assertTrue("6.5", workspace.validateProjectLocation(project, anotherDevice.append("foo")).isOK());

		//cannot be a relative path
		assertFalse("7.1", workspace.validateProjectLocation(project, new Path("u:", "")).isOK());
		assertFalse("7.2", workspace.validateProjectLocation(project, IPath.fromOSString("c:")).isOK());
		assertFalse("7.3", workspace.validateProjectLocation(project, IPath.fromOSString("c:foo")).isOK());
		assertFalse("7.4", workspace.validateProjectLocation(project, IPath.fromOSString("foo/bar")).isOK());
		assertFalse("7.5", workspace.validateProjectLocation(project, IPath.fromOSString("c:foo/bar")).isOK());

		//may be relative to an existing path variable
		final String PATH_VAR_NAME = "FOOVAR";
		final IPath PATH_VAR_VALUE = getRandomLocation();
		try {
			IPath varPath = IPath.fromOSString(PATH_VAR_NAME);
			workspace.getPathVariableManager().setValue(PATH_VAR_NAME, PATH_VAR_VALUE);
			assertTrue("8.1", workspace.validateProjectLocation(project, varPath).isOK());
			assertTrue("8.2", workspace.validateProjectLocation(project, varPath.append("test")).isOK());
			assertTrue("8.3", workspace.validateProjectLocation(project, varPath.append("test/ing")).isOK());
		} finally {
			workspace.getPathVariableManager().setValue(PATH_VAR_NAME, null);
		}

		//cannot overlap with another project's location
		IPath openProjectLocation = getTempDir().append("OpenProject");
		IProject open = workspace.getRoot().getProject("OpenProject");
		IProjectDescription openDesc = workspace.newProjectDescription(open.getName());
		openDesc.setLocation(openProjectLocation);
		IPath closedProjectLocation = getTempDir().append("ClosedProject");
		IProject closed = workspace.getRoot().getProject("ClosedProject");
		IProjectDescription closedDesc = workspace.newProjectDescription(closed.getName());
		closedDesc.setLocation(closedProjectLocation);
		open.create(openDesc, null);
		open.open(null);
		closed.create(closedDesc, null);
		IPath linkLocation = getRandomLocation();
		try {
			//indirect test: setting the project description may validate location, which shouldn't complain
			IProjectDescription desc = open.getDescription();
			desc.setReferencedProjects(new IProject[] {project});
			open.setDescription(desc, IResource.FORCE, createTestMonitor());

			assertFalse("9.1", workspace.validateProjectLocation(project, openProjectLocation).isOK());
			assertFalse("9.2", workspace.validateProjectLocation(project, closedProjectLocation).isOK());

			//for an existing project, it cannot overlap itself, but its own location is valid
			assertTrue("9.3", workspace.validateProjectLocation(open, openProjectLocation).isOK());
			assertFalse("9.4", workspace.validateProjectLocation(open, openProjectLocation.append("sub")).isOK());

			//an existing project cannot overlap the location of any linked resource in that project
			linkLocation.toFile().mkdirs();
			assertTrue("10.1", workspace.validateProjectLocation(open, linkLocation).isOK());
			IFolder link = open.getFolder("link");
			link.createLink(linkLocation, IResource.NONE, createTestMonitor());
			assertFalse("10.2", workspace.validateProjectLocation(open, linkLocation).isOK());
			assertFalse("10.3", workspace.validateProjectLocation(open, linkLocation.append("sub")).isOK());

			//however another project can overlap an existing link location
			assertTrue("10.4", workspace.validateProjectLocation(project, linkLocation).isOK());

			// A new project cannot overlap the default locations of other projects, but its own location is valid
			IPath defaultProjectLocation = workspace.getRoot().getLocation();
			assertTrue("11.1", workspace.validateProjectLocation(project, defaultProjectLocation.append(project.getName())).isOK());
			assertFalse("11.2",
					workspace.validateProjectLocation(project, defaultProjectLocation.append("foo")).isOK());
		} finally {
			Workspace.clear(linkLocation.toFile());
			//make sure we clean up project directories
			try {
				open.delete(IResource.ALWAYS_DELETE_PROJECT_CONTENT, createTestMonitor());
				open.delete(IResource.ALWAYS_DELETE_PROJECT_CONTENT, createTestMonitor());
			} catch (CoreException e) {
			}
			removeFromFileSystem(openProjectLocation.toFile());
			removeFromFileSystem(closedProjectLocation.toFile());
		}

		// cannot overlap .metadata folder from the current workspace
		assertFalse("12.1", (workspace.validateProjectLocation(project,
				platformLocation.addTrailingSeparator().append(".metadata"))).isOK());

		IProject metadataProject = workspace.getRoot().getProject(".metadata");
		assertFalse("12.2", (workspace.validateProjectLocation(metadataProject, null)).isOK());

		// FIXME: Should this be valid?
		assertTrue("23.1", workspace.validateProjectLocation(project, IPath.fromOSString("/asf")).isOK());
		assertTrue("23.2", workspace.validateProjectLocation(project, IPath.fromOSString("/project/.metadata")).isOK());
		// FIXME: Should this be valid?
		assertTrue("23.3", workspace.validateProjectLocation(project, IPath.fromOSString("/.metadata/project")).isOK());
	}

	/**
	 * Performs black box testing of the following method:
	 *     IStatus validateProjectLocationURI(IProject, URI)
	 */
	@Test
	public void testValidateProjectLocationURI() throws URISyntaxException {
		IWorkspace workspace = getWorkspace();
		IProject project = workspace.getRoot().getProject("Project");
		// URI with no scheme
		URI uri = new URI("eferfsdfwer");
		assertFalse("1.0", workspace.validateProjectLocationURI(project, uri).isOK());
		// URI with unknown scheme
		uri = new URI("blorts://foo.com?bad");
		assertFalse("1.1", workspace.validateProjectLocationURI(project, uri).isOK());
	}

	@Test
	public void testWorkspaceService() {
		final BundleContext context = FrameworkUtil.getBundle(IWorkspaceTest.class).getBundleContext();
		ServiceReference<IWorkspace> ref = context.getServiceReference(IWorkspace.class);
		assertNotNull("1.0", ref);
		IWorkspace ws = context.getService(ref);
		assertNotNull("1.1", ws);
	}

	@Test
	public void testGetFilterMatcherDescriptor() {
		IFilterMatcherDescriptor descriptor = getWorkspace().getFilterMatcherDescriptor("");
		assertNull("1.0", descriptor);
		descriptor = getWorkspace().getFilterMatcherDescriptor("org.eclipse.core.resources.regexFilterMatcher");
		assertNotNull("1.1", descriptor);
	}

}
