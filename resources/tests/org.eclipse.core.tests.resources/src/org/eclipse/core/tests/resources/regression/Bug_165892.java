/*******************************************************************************
 * Copyright (c) 2006, 2012 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.resources.regression;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.tests.resources.ResourceTest;

/**
 * Tests regression of bug 165892. Copying a resource should perform a deep
 * copy of its persistent properties.  Subsequent changes to persistent
 * properties on the destination resource should not affect the properties on the
 * source resource.
 */
public class Bug_165892 extends ResourceTest {
	/**
	 * Tests copying a file
	 */
	public void testCopyFile() throws CoreException {
		IProject source = getWorkspace().getRoot().getProject("project");
		IFolder sourceFolder = source.getFolder("folder");
		IFile sourceFile = sourceFolder.getFile("source");
		IFile destinationFile = sourceFolder.getFile("destination");
		ensureExistsInWorkspace(sourceFile, true);

		final String sourceValue = "SourceValue";
		QualifiedName name = new QualifiedName("Bug_165892", "Property");
		// add a persistent property to each source resource
		sourceFile.setPersistentProperty(name, sourceValue);

		//copy the file
		sourceFile.copy(destinationFile.getFullPath(), IResource.NONE, getMonitor());

		//make sure the persistent properties were copied
		assertEquals("2.0", sourceValue, sourceFile.getPersistentProperty(name));
		assertEquals("2.1", sourceValue, destinationFile.getPersistentProperty(name));

		//change the values of the persistent property on the copied resource
		final String destinationValue = "DestinationValue";
		destinationFile.setPersistentProperty(name, destinationValue);

		//make sure the persistent property values are correct
		assertEquals("3.0", sourceValue, sourceFile.getPersistentProperty(name));
		assertEquals("3.1", destinationValue, destinationFile.getPersistentProperty(name));
	}

	/**
	 * Tests that history of source file isn't affected by a copy
	 */
	public void testCopyFileHistory() throws CoreException {
		IProject source = getWorkspace().getRoot().getProject("project");
		IFolder sourceFolder = source.getFolder("folder");
		IFile sourceFile = sourceFolder.getFile("source");
		IFile destinationFile = sourceFolder.getFile("destination");
		ensureExistsInWorkspace(sourceFile, true);

		// modify the source file so it has some history
		sourceFile.setContents(getRandomContents(), IResource.KEEP_HISTORY, getMonitor());
		// check that the source file has the expected history
		assertEquals("1.0", 1, sourceFile.getHistory(getMonitor()).length);

		//copy the file
		sourceFile.copy(destinationFile.getFullPath(), IResource.NONE, getMonitor());

		//make sure the history was copied
		assertEquals("2.0", 1, sourceFile.getHistory(getMonitor()).length);
		assertEquals("2.1", 1, destinationFile.getHistory(getMonitor()).length);

		//modify the destination to change its history
		destinationFile.setContents(getRandomContents(), IResource.KEEP_HISTORY, getMonitor());

		//make sure the history is correct
		assertEquals("2.0", 1, sourceFile.getHistory(getMonitor()).length);
		assertEquals("2.1", 2, destinationFile.getHistory(getMonitor()).length);
	}

	/**
	 * Tests copying a folder
	 */
	public void testCopyFolder() throws CoreException {
		IProject source = getWorkspace().getRoot().getProject("project");
		IFolder sourceFolder = source.getFolder("source");
		IFile sourceFile = sourceFolder.getFile("Important.txt");
		IFolder destinationFolder = source.getFolder("destination");
		IFile destinationFile = destinationFolder.getFile(sourceFile.getName());
		ensureExistsInWorkspace(sourceFile, true);

		//add a persistent property to each source resource
		final String sourceValue = "SourceValue";
		QualifiedName name = new QualifiedName("Bug_165892", "Property");
		source.setPersistentProperty(name, sourceValue);
		sourceFolder.setPersistentProperty(name, sourceValue);
		sourceFile.setPersistentProperty(name, sourceValue);

		//copy the folder
		sourceFolder.copy(destinationFolder.getFullPath(), IResource.NONE, getMonitor());

		//make sure the persistent properties were copied
		assertEquals("2.0", sourceValue, source.getPersistentProperty(name));
		assertEquals("2.1", sourceValue, sourceFolder.getPersistentProperty(name));
		assertEquals("2.2", sourceValue, sourceFile.getPersistentProperty(name));
		assertEquals("2.3", sourceValue, destinationFolder.getPersistentProperty(name));
		assertEquals("2.4", sourceValue, destinationFile.getPersistentProperty(name));

		//change the values of the persistent property on the copied resource
		final String destinationValue = "DestinationValue";
		destinationFolder.setPersistentProperty(name, destinationValue);
		destinationFile.setPersistentProperty(name, destinationValue);

		//make sure the persistent property values are correct
		assertEquals("3.0", sourceValue, source.getPersistentProperty(name));
		assertEquals("3.1", sourceValue, sourceFolder.getPersistentProperty(name));
		assertEquals("3.2", sourceValue, sourceFile.getPersistentProperty(name));
		assertEquals("3.3", destinationValue, destinationFolder.getPersistentProperty(name));
		assertEquals("3.4", destinationValue, destinationFile.getPersistentProperty(name));
	}

	/**
	 * Tests copying a project
	 */
	public void testCopyProject() throws CoreException {
		IProject source = getWorkspace().getRoot().getProject("source");
		IFolder sourceFolder = source.getFolder("folder");
		IFile sourceFile = sourceFolder.getFile("Important.txt");
		IProject destination = getWorkspace().getRoot().getProject("destination");
		IFolder destinationFolder = destination.getFolder(sourceFolder.getName());
		IFile destinationFile = destinationFolder.getFile(sourceFile.getName());
		ensureExistsInWorkspace(sourceFile, true);

		//add a persistent property to each source resource
		final String sourceValue = "SourceValue";
		QualifiedName name = new QualifiedName("Bug_165892", "Property");
		source.setPersistentProperty(name, sourceValue);
		sourceFolder.setPersistentProperty(name, sourceValue);
		sourceFile.setPersistentProperty(name, sourceValue);

		//copy the project
		source.copy(destination.getFullPath(), IResource.NONE, getMonitor());

		//make sure the persistent properties were copied
		assertEquals("2.0", sourceValue, source.getPersistentProperty(name));
		assertEquals("2.1", sourceValue, sourceFolder.getPersistentProperty(name));
		assertEquals("2.2", sourceValue, sourceFile.getPersistentProperty(name));
		assertEquals("2.3", sourceValue, destination.getPersistentProperty(name));
		assertEquals("2.4", sourceValue, destinationFolder.getPersistentProperty(name));
		assertEquals("2.5", sourceValue, destinationFile.getPersistentProperty(name));

		//change the values of the persistent property on the copied resource
		final String destinationValue = "DestinationValue";
		destination.setPersistentProperty(name, destinationValue);
		destinationFolder.setPersistentProperty(name, destinationValue);
		destinationFile.setPersistentProperty(name, destinationValue);

		//make sure the persistent property values are correct
		assertEquals("3.0", sourceValue, source.getPersistentProperty(name));
		assertEquals("3.1", sourceValue, sourceFolder.getPersistentProperty(name));
		assertEquals("3.2", sourceValue, sourceFile.getPersistentProperty(name));
		assertEquals("3.3", destinationValue, destination.getPersistentProperty(name));
		assertEquals("3.4", destinationValue, destinationFolder.getPersistentProperty(name));
		assertEquals("3.5", destinationValue, destinationFile.getPersistentProperty(name));
	}
}