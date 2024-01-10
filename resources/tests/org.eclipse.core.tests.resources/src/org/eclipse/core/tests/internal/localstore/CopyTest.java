/*******************************************************************************
 *  Copyright (c) 2000, 2012 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.core.tests.internal.localstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.removeFromFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.removeFromWorkspace;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test the copy operation.
 */
public class CopyTest {

	private static final int NUMBER_OF_PROPERTIES = 5;

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	@Test
	public void testCopyResource() throws Throwable {
		IProject project = getWorkspace().getRoot().getProject("Project");
		createInWorkspace(project);

		/* create folder and file */
		IFolder folder = project.getFolder("folder");
		IFile file = folder.getFile("file.txt");
		createInWorkspace(folder);
		createInFileSystem(folder);
		createInWorkspace(file);
		createInFileSystem(file);
		/* add some properties to file (server, local and session) */
		QualifiedName[] propNames = new QualifiedName[NUMBER_OF_PROPERTIES];
		String[] propValues = new String[NUMBER_OF_PROPERTIES];
		for (int i = 0; i < NUMBER_OF_PROPERTIES; i++) {
			propNames[i] = new QualifiedName("test", "prop" + i);
			propValues[i] = "value" + i;
			file.setPersistentProperty(propNames[i], propValues[i]);
			file.setSessionProperty(propNames[i], propValues[i]);
		}

		/* copy to absolute path */
		IResource destination = project.getFile("copy of file.txt");
		removeFromFileSystem(destination);
		file.copy(destination.getFullPath(), true, null);
		assertTrue(destination.exists());
		/* assert properties were properly copied */
		for (int i = 0; i < NUMBER_OF_PROPERTIES; i++) {
			String persistentValue = destination.getPersistentProperty(propNames[i]);
			Object sessionValue = destination.getSessionProperty(propNames[i]);
			assertThat(propValues[i]).isEqualTo(persistentValue).isNotEqualTo(sessionValue);
		}
		removeFromWorkspace(destination);
		removeFromFileSystem(destination);

		/* copy to relative path */
		IPath path = IPath.fromOSString("copy of file.txt");
		IFile destinationInFolder = folder.getFile(path);
		removeFromFileSystem(destinationInFolder);
		file.copy(path, true, null);
		assertTrue(destinationInFolder.exists());
		/* assert properties were properly copied */
		for (int i = 0; i < NUMBER_OF_PROPERTIES; i++) {
			String persistentValue = destinationInFolder.getPersistentProperty(propNames[i]);
			Object sessionValue = destinationInFolder.getSessionProperty(propNames[i]);
			assertThat(propValues[i]).isEqualTo(persistentValue).isNotEqualTo(sessionValue);
		}
		removeFromWorkspace(destinationInFolder);
		removeFromFileSystem(destinationInFolder);

		/* copy folder to destination under its hierarchy */
		IFolder destinationInSubfolder = folder.getFolder("subfolder");
		assertThrows(RuntimeException.class, () -> folder.copy(destinationInSubfolder.getFullPath(), true, null));

		/* test flag force = false */
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
		IFolder subfolder = folder.getFolder("subfolder");
		createInFileSystem(subfolder);
		IFile anotherFile = folder.getFile("new file");
		createInFileSystem(anotherFile);
		IFolder destinationFolder = project.getFolder("destination");
		CoreException exception = assertThrows(CoreException.class,
				() -> folder.copy(destinationFolder.getFullPath(), false, null));
		assertThat(exception.getStatus().getChildren()).hasSize(2);
		assertTrue(destinationFolder.exists());
		assertTrue(((IContainer) destinationFolder).getFile(IPath.fromOSString(file.getName())).exists());
		assertFalse(((IContainer) destinationFolder).getFolder(IPath.fromOSString(subfolder.getName())).exists());
		assertFalse(((IContainer) destinationFolder).getFile(IPath.fromOSString(anotherFile.getName())).exists());
		/* assert properties were properly copied */
		IResource target = ((IContainer) destinationFolder).getFile(IPath.fromOSString(file.getName()));
		for (int i = 0; i < NUMBER_OF_PROPERTIES; i++) {
			String persistentValue = target.getPersistentProperty(propNames[i]);
			Object sessionValue = target.getSessionProperty(propNames[i]);
			assertThat(propValues[i]).isEqualTo(persistentValue).isNotEqualTo(sessionValue);
		}
		removeFromWorkspace(destinationFolder);
		removeFromFileSystem(destinationFolder);

		/* copy a file that is not local but exists in the workspace */
		IFile ghostFile = project.getFile("ghost");
		ghostFile.create(null, true, null);
		removeFromFileSystem(file);
		IFile destinationFile = project.getFile("destination");
		assertThrows(CoreException.class, () -> ghostFile.copy(destinationFile.getFullPath(), true, null));
	}

}
