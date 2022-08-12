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

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;

/**
 * Test the copy operation.
 */
public class CopyTest extends LocalStoreTest {

	public void testCopyResource() throws Throwable {
		/* create common objects */
		IProject[] testProjects = getWorkspace().getRoot().getProjects();

		/* create folder and file */
		IFolder folder = testProjects[0].getFolder("folder");
		IFile file = folder.getFile("file.txt");
		ensureExistsInWorkspace(folder, true);
		ensureExistsInFileSystem(folder);
		ensureExistsInWorkspace(file, true);
		ensureExistsInFileSystem(file);
		/* add some properties to file (server, local and session) */
		QualifiedName[] propNames = new QualifiedName[numberOfProperties];
		String[] propValues = new String[numberOfProperties];
		for (int i = 0; i < numberOfProperties; i++) {
			propNames[i] = new QualifiedName("test", "prop" + i);
			propValues[i] = "value" + i;
			file.setPersistentProperty(propNames[i], propValues[i]);
			file.setSessionProperty(propNames[i], propValues[i]);
		}

		/* copy to absolute path */
		IResource destination = testProjects[0].getFile("copy of file.txt");
		ensureDoesNotExistInFileSystem(destination);
		try {
			file.copy(destination.getFullPath(), true, null);
		} catch (CoreException e) {
			fail("1.0", e);
		}
		assertTrue("1.1", destination.exists());
		/* assert properties were properly copied */
		for (int i = 0; i < numberOfProperties; i++) {
			String persistentValue = destination.getPersistentProperty(propNames[i]);
			Object sessionValue = destination.getSessionProperty(propNames[i]);
			assertTrue("1.2", propValues[i].equals(persistentValue));
			assertTrue("1.4", !propValues[i].equals(sessionValue));
		}
		ensureDoesNotExistInWorkspace(destination);
		ensureDoesNotExistInFileSystem(destination);

		/* copy to relative path */
		IPath path = new Path("copy of file.txt");
		destination = folder.getFile(path);
		ensureDoesNotExistInFileSystem(destination);
		try {
			file.copy(path, true, null);
		} catch (CoreException e) {
			fail("2.0", e);
		}
		assertTrue("2.1", destination.exists());
		/* assert properties were properly copied */
		for (int i = 0; i < numberOfProperties; i++) {
			String persistentValue = destination.getPersistentProperty(propNames[i]);
			Object sessionValue = destination.getSessionProperty(propNames[i]);
			assertTrue("2.2", propValues[i].equals(persistentValue));
			assertTrue("2.4", !propValues[i].equals(sessionValue));
		}
		ensureDoesNotExistInWorkspace(destination);
		ensureDoesNotExistInFileSystem(destination);

		/* copy folder to destination under its hierarchy */
		destination = folder.getFolder("subfolder");
		try {
			folder.copy(destination.getFullPath(), true, null);
			fail("3.1");
		} catch (RuntimeException e) {
			// expected
		} catch (CoreException e) {
			fail("3.2", e);
		}

		/* test flag force = false */
		testProjects[0].refreshLocal(IResource.DEPTH_INFINITE, null);
		IFolder subfolder = folder.getFolder("subfolder");
		ensureExistsInFileSystem(subfolder);
		IFile anotherFile = folder.getFile("new file");
		ensureExistsInFileSystem(anotherFile);
		destination = testProjects[0].getFolder("destination");
		try {
			folder.copy(destination.getFullPath(), false, null);
			fail("4.1");
		} catch (CoreException e) {
			assertTrue("4.2", e.getStatus().getChildren().length == 2);
		}
		assertTrue("4.3", destination.exists());
		assertTrue("4.4", ((IContainer) destination).getFile(new Path(file.getName())).exists());
		assertTrue("4.5", !((IContainer) destination).getFolder(new Path(subfolder.getName())).exists());
		assertTrue("4.6", !((IContainer) destination).getFile(new Path(anotherFile.getName())).exists());
		/* assert properties were properly copied */
		IResource target = ((IContainer) destination).getFile(new Path(file.getName()));
		for (int i = 0; i < numberOfProperties; i++) {
			String persistentValue = target.getPersistentProperty(propNames[i]);
			Object sessionValue = target.getSessionProperty(propNames[i]);
			assertTrue("4.7", propValues[i].equals(persistentValue));
			assertTrue("4.9", !propValues[i].equals(sessionValue));
		}
		ensureDoesNotExistInWorkspace(destination);
		ensureDoesNotExistInFileSystem(destination);

		/* copy a file that is not local but exists in the workspace */
		file = testProjects[0].getFile("ghost");
		file.create(null, true, null);
		ensureDoesNotExistInFileSystem(file);
		destination = testProjects[0].getFile("destination");
		try {
			file.copy(destination.getFullPath(), true, null);
			fail("5.1");
		} catch (CoreException e) {
			// expected
		}
	}
}
