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

import org.eclipse.core.resources.*;
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
	public void testCopyFile() {
		IProject source = getWorkspace().getRoot().getProject("project");
		IFolder sourceFolder = source.getFolder("folder");
		IFile sourceFile = sourceFolder.getFile("source");
		IFile destinationFile = sourceFolder.getFile("destination");
		ensureExistsInWorkspace(sourceFile, true);

		final String sourceValue = "SourceValue";
		QualifiedName name = new QualifiedName("Bug_165892", "Property");
		try {
			//add a persistent property to each source resource
			sourceFile.setPersistentProperty(name, sourceValue);
		} catch (CoreException e) {
			fail("0.99", e);
		}

		//copy the file
		try {
			sourceFile.copy(destinationFile.getFullPath(), IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("1.99", e);
		}

		//make sure the persistent properties were copied
		try {
			assertEquals("2.0", sourceValue, sourceFile.getPersistentProperty(name));
			assertEquals("2.1", sourceValue, destinationFile.getPersistentProperty(name));
		} catch (CoreException e) {
			fail("2.98", e);
		}

		//change the values of the persistent property on the copied resource
		final String destinationValue = "DestinationValue";
		try {
			destinationFile.setPersistentProperty(name, destinationValue);
		} catch (CoreException e) {
			fail("2.99", e);
		}

		//make sure the persistent property values are correct
		try {
			assertEquals("3.0", sourceValue, sourceFile.getPersistentProperty(name));
			assertEquals("3.1", destinationValue, destinationFile.getPersistentProperty(name));
		} catch (CoreException e) {
			fail("3.99", e);
		}
	}

	/**
	 * Tests that history of source file isn't affected by a copy
	 */
	public void testCopyFileHistory() {
		IProject source = getWorkspace().getRoot().getProject("project");
		IFolder sourceFolder = source.getFolder("folder");
		IFile sourceFile = sourceFolder.getFile("source");
		IFile destinationFile = sourceFolder.getFile("destination");
		ensureExistsInWorkspace(sourceFile, true);

		try {
			//modify the source file so it has some history
			sourceFile.setContents(getRandomContents(), IResource.KEEP_HISTORY, getMonitor());
			//check that the source file has the expected history
			assertEquals("1.0", 1, sourceFile.getHistory(getMonitor()).length);
		} catch (CoreException e) {
			fail("0.99", e);
		}

		//copy the file
		try {
			sourceFile.copy(destinationFile.getFullPath(), IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("1.99", e);
		}

		//make sure the history was copied
		try {
			assertEquals("2.0", 1, sourceFile.getHistory(getMonitor()).length);
			assertEquals("2.1", 1, destinationFile.getHistory(getMonitor()).length);
		} catch (CoreException e) {
			fail("2.98", e);
		}

		//modify the destination to change its history
		try {
			destinationFile.setContents(getRandomContents(), IResource.KEEP_HISTORY, getMonitor());
		} catch (CoreException e) {
			fail("2.99", e);
		}

		//make sure the history is correct
		try {
			assertEquals("2.0", 1, sourceFile.getHistory(getMonitor()).length);
			assertEquals("2.1", 2, destinationFile.getHistory(getMonitor()).length);
		} catch (CoreException e) {
			fail("3.99", e);
		}
	}

	/**
	 * Tests copying a folder
	 */
	public void testCopyFolder() {
		IProject source = getWorkspace().getRoot().getProject("project");
		IFolder sourceFolder = source.getFolder("source");
		IFile sourceFile = sourceFolder.getFile("Important.txt");
		IFolder destinationFolder = source.getFolder("destination");
		IFile destinationFile = destinationFolder.getFile(sourceFile.getName());
		ensureExistsInWorkspace(sourceFile, true);

		//add a persistent property to each source resource
		final String sourceValue = "SourceValue";
		QualifiedName name = new QualifiedName("Bug_165892", "Property");
		try {
			source.setPersistentProperty(name, sourceValue);
			sourceFolder.setPersistentProperty(name, sourceValue);
			sourceFile.setPersistentProperty(name, sourceValue);
		} catch (CoreException e) {
			fail("0.99", e);
		}

		//copy the folder
		try {
			sourceFolder.copy(destinationFolder.getFullPath(), IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("1.99", e);
		}

		//make sure the persistent properties were copied
		try {
			assertEquals("2.0", sourceValue, source.getPersistentProperty(name));
			assertEquals("2.1", sourceValue, sourceFolder.getPersistentProperty(name));
			assertEquals("2.2", sourceValue, sourceFile.getPersistentProperty(name));
			assertEquals("2.3", sourceValue, destinationFolder.getPersistentProperty(name));
			assertEquals("2.4", sourceValue, destinationFile.getPersistentProperty(name));
		} catch (CoreException e) {
			fail("2.98", e);
		}

		//change the values of the persistent property on the copied resource
		final String destinationValue = "DestinationValue";
		try {
			destinationFolder.setPersistentProperty(name, destinationValue);
			destinationFile.setPersistentProperty(name, destinationValue);
		} catch (CoreException e) {
			fail("2.99", e);
		}

		//make sure the persistent property values are correct
		try {
			assertEquals("3.0", sourceValue, source.getPersistentProperty(name));
			assertEquals("3.1", sourceValue, sourceFolder.getPersistentProperty(name));
			assertEquals("3.2", sourceValue, sourceFile.getPersistentProperty(name));
			assertEquals("3.3", destinationValue, destinationFolder.getPersistentProperty(name));
			assertEquals("3.4", destinationValue, destinationFile.getPersistentProperty(name));
		} catch (CoreException e) {
			fail("3.99", e);
		}

	}

	/**
	 * Tests copying a project
	 */
	public void testCopyProject() {
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
		try {
			source.setPersistentProperty(name, sourceValue);
			sourceFolder.setPersistentProperty(name, sourceValue);
			sourceFile.setPersistentProperty(name, sourceValue);
		} catch (CoreException e) {
			fail("0.99", e);
		}

		//copy the project
		try {
			source.copy(destination.getFullPath(), IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("1.99", e);
		}

		//make sure the persistent properties were copied
		try {
			assertEquals("2.0", sourceValue, source.getPersistentProperty(name));
			assertEquals("2.1", sourceValue, sourceFolder.getPersistentProperty(name));
			assertEquals("2.2", sourceValue, sourceFile.getPersistentProperty(name));
			assertEquals("2.3", sourceValue, destination.getPersistentProperty(name));
			assertEquals("2.4", sourceValue, destinationFolder.getPersistentProperty(name));
			assertEquals("2.5", sourceValue, destinationFile.getPersistentProperty(name));
		} catch (CoreException e) {
			fail("2.98", e);
		}

		//change the values of the persistent property on the copied resource
		final String destinationValue = "DestinationValue";
		try {
			destination.setPersistentProperty(name, destinationValue);
			destinationFolder.setPersistentProperty(name, destinationValue);
			destinationFile.setPersistentProperty(name, destinationValue);
		} catch (CoreException e) {
			fail("2.99", e);
		}

		//make sure the persistent property values are correct
		try {
			assertEquals("3.0", sourceValue, source.getPersistentProperty(name));
			assertEquals("3.1", sourceValue, sourceFolder.getPersistentProperty(name));
			assertEquals("3.2", sourceValue, sourceFile.getPersistentProperty(name));
			assertEquals("3.3", destinationValue, destination.getPersistentProperty(name));
			assertEquals("3.4", destinationValue, destinationFolder.getPersistentProperty(name));
			assertEquals("3.5", destinationValue, destinationFile.getPersistentProperty(name));
		} catch (CoreException e) {
			fail("3.99", e);
		}
	}
}