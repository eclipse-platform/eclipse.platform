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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomContentsStream;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.junit.Assert.assertEquals;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests regression of bug 165892. Copying a resource should perform a deep
 * copy of its persistent properties.  Subsequent changes to persistent
 * properties on the destination resource should not affect the properties on the
 * source resource.
 */
public class Bug_165892 {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	/**
	 * Tests copying a file
	 */
	@Test
	public void testCopyFile() throws CoreException {
		IProject source = getWorkspace().getRoot().getProject("project");
		IFolder sourceFolder = source.getFolder("folder");
		IFile sourceFile = sourceFolder.getFile("source");
		IFile destinationFile = sourceFolder.getFile("destination");
		createInWorkspace(sourceFile);

		final String sourceValue = "SourceValue";
		QualifiedName name = new QualifiedName("Bug_165892", "Property");
		// add a persistent property to each source resource
		sourceFile.setPersistentProperty(name, sourceValue);

		//copy the file
		sourceFile.copy(destinationFile.getFullPath(), IResource.NONE, createTestMonitor());

		//make sure the persistent properties were copied
		assertEquals(sourceValue, sourceFile.getPersistentProperty(name));
		assertEquals(sourceValue, destinationFile.getPersistentProperty(name));

		//change the values of the persistent property on the copied resource
		final String destinationValue = "DestinationValue";
		destinationFile.setPersistentProperty(name, destinationValue);

		//make sure the persistent property values are correct
		assertEquals(sourceValue, sourceFile.getPersistentProperty(name));
		assertEquals(destinationValue, destinationFile.getPersistentProperty(name));
	}

	/**
	 * Tests that history of source file isn't affected by a copy
	 */
	@Test
	public void testCopyFileHistory() throws CoreException {
		IProject source = getWorkspace().getRoot().getProject("project");
		IFolder sourceFolder = source.getFolder("folder");
		IFile sourceFile = sourceFolder.getFile("source");
		IFile destinationFile = sourceFolder.getFile("destination");
		createInWorkspace(sourceFile);

		// modify the source file so it has some history
		sourceFile.setContents(createRandomContentsStream(), IResource.KEEP_HISTORY, createTestMonitor());
		// check that the source file has the expected history
		assertThat(sourceFile.getHistory(createTestMonitor())).hasSize(1);

		//copy the file
		sourceFile.copy(destinationFile.getFullPath(), IResource.NONE, createTestMonitor());

		//make sure the history was copied
		assertThat(sourceFile.getHistory(createTestMonitor())).hasSize(1);
		assertThat(destinationFile.getHistory(createTestMonitor())).hasSize(1);

		//modify the destination to change its history
		destinationFile.setContents(createRandomContentsStream(), IResource.KEEP_HISTORY, createTestMonitor());

		//make sure the history is correct
		assertThat(sourceFile.getHistory(createTestMonitor())).hasSize(1);
		assertThat(destinationFile.getHistory(createTestMonitor())).hasSize(2);
	}

	/**
	 * Tests copying a folder
	 */
	@Test
	public void testCopyFolder() throws CoreException {
		IProject source = getWorkspace().getRoot().getProject("project");
		IFolder sourceFolder = source.getFolder("source");
		IFile sourceFile = sourceFolder.getFile("Important.txt");
		IFolder destinationFolder = source.getFolder("destination");
		IFile destinationFile = destinationFolder.getFile(sourceFile.getName());
		createInWorkspace(sourceFile);

		//add a persistent property to each source resource
		final String sourceValue = "SourceValue";
		QualifiedName name = new QualifiedName("Bug_165892", "Property");
		source.setPersistentProperty(name, sourceValue);
		sourceFolder.setPersistentProperty(name, sourceValue);
		sourceFile.setPersistentProperty(name, sourceValue);

		//copy the folder
		sourceFolder.copy(destinationFolder.getFullPath(), IResource.NONE, createTestMonitor());

		//make sure the persistent properties were copied
		assertEquals(sourceValue, source.getPersistentProperty(name));
		assertEquals(sourceValue, sourceFolder.getPersistentProperty(name));
		assertEquals(sourceValue, sourceFile.getPersistentProperty(name));
		assertEquals(sourceValue, destinationFolder.getPersistentProperty(name));
		assertEquals(sourceValue, destinationFile.getPersistentProperty(name));

		//change the values of the persistent property on the copied resource
		final String destinationValue = "DestinationValue";
		destinationFolder.setPersistentProperty(name, destinationValue);
		destinationFile.setPersistentProperty(name, destinationValue);

		//make sure the persistent property values are correct
		assertEquals(sourceValue, source.getPersistentProperty(name));
		assertEquals(sourceValue, sourceFolder.getPersistentProperty(name));
		assertEquals(sourceValue, sourceFile.getPersistentProperty(name));
		assertEquals(destinationValue, destinationFolder.getPersistentProperty(name));
		assertEquals(destinationValue, destinationFile.getPersistentProperty(name));
	}

	/**
	 * Tests copying a project
	 */
	@Test
	public void testCopyProject() throws CoreException {
		IProject source = getWorkspace().getRoot().getProject("source");
		IFolder sourceFolder = source.getFolder("folder");
		IFile sourceFile = sourceFolder.getFile("Important.txt");
		IProject destination = getWorkspace().getRoot().getProject("destination");
		IFolder destinationFolder = destination.getFolder(sourceFolder.getName());
		IFile destinationFile = destinationFolder.getFile(sourceFile.getName());
		createInWorkspace(sourceFile);

		//add a persistent property to each source resource
		final String sourceValue = "SourceValue";
		QualifiedName name = new QualifiedName("Bug_165892", "Property");
		source.setPersistentProperty(name, sourceValue);
		sourceFolder.setPersistentProperty(name, sourceValue);
		sourceFile.setPersistentProperty(name, sourceValue);

		//copy the project
		source.copy(destination.getFullPath(), IResource.NONE, createTestMonitor());

		//make sure the persistent properties were copied
		assertEquals(sourceValue, source.getPersistentProperty(name));
		assertEquals(sourceValue, sourceFolder.getPersistentProperty(name));
		assertEquals(sourceValue, sourceFile.getPersistentProperty(name));
		assertEquals(sourceValue, destination.getPersistentProperty(name));
		assertEquals(sourceValue, destinationFolder.getPersistentProperty(name));
		assertEquals(sourceValue, destinationFile.getPersistentProperty(name));

		//change the values of the persistent property on the copied resource
		final String destinationValue = "DestinationValue";
		destination.setPersistentProperty(name, destinationValue);
		destinationFolder.setPersistentProperty(name, destinationValue);
		destinationFile.setPersistentProperty(name, destinationValue);

		//make sure the persistent property values are correct
		assertEquals(sourceValue, source.getPersistentProperty(name));
		assertEquals(sourceValue, sourceFolder.getPersistentProperty(name));
		assertEquals(sourceValue, sourceFile.getPersistentProperty(name));
		assertEquals(destinationValue, destination.getPersistentProperty(name));
		assertEquals(destinationValue, destinationFolder.getPersistentProperty(name));
		assertEquals(destinationValue, destinationFile.getPersistentProperty(name));
	}
}