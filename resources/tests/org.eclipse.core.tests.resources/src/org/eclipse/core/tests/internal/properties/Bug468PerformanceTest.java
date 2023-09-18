/*******************************************************************************
 * Copyright (c) ETAS GmbH 2023, all rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     ETAS GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.internal.properties;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import junit.framework.TestCase;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

//Test case for GitHub Issue 468
public class Bug468PerformanceTest extends TestCase {

	private static final String TO_BE_DELETED_FILE_NAME_PREFIX = "file_";

	private static final String TEMP_FOLDER_NAME = "temp";

	private IProject project;

	/**
	 * Creates project with below folder and file structure.
	 * <ol>
	 * <li>Project contains 1 single temp folder.</li>
	 * <li>'temp' folder contains 10 folders and 6000 files as direct children.</li>
	 * <li>each folder under 'temp' contains 10 folders (called as GrandChildren)
	 * and a single file.</li>
	 * <li>each GrandChildren, inturn contains another 10 folders (3rd level folder)
	 * and a single file.</li>
	 * <li>each 3rd level folder contains a single file.
	 * <li>
	 * </ol>
	 * A dummy property is created for all the files to ensure that the folder
	 * structure are created in the workspace .metadata area.
	 *
	 * @throws Exception
	 *             If anything goes wrong during the set up.
	 */
	@Override
	@Before
	protected void setUp() throws Exception {

		super.setUp();
		ResourcesPlugin.getWorkspace().run((ICoreRunnable) monitor -> {
			createTestProject();
			List<IFolder> childFolders = new ArrayList<>();
			List<IFolder> grandChildFolders = new ArrayList<>();
			IFolder tempFolder = Bug468PerformanceTest.this.project.getFolder(TEMP_FOLDER_NAME);
			tempFolder.create(true, true, new NullProgressMonitor());
			// 'temp' folder contains 10 folder and 6000 files as direct children.
			for (int fileIndex = 0; fileIndex < 6000; fileIndex++) {
				createFile(tempFolder, TO_BE_DELETED_FILE_NAME_PREFIX + fileIndex);
			}
			for (int childIndex = 0; childIndex < 10; childIndex++) {
				IFolder childFolder = createFolder(tempFolder, "temp_child_" + childIndex);
				// each child contains 10 folders and a single file.
				createTempFile(childFolder);
				childFolders.add(childFolder);
			}
			for (int grandChildIndex = 0; grandChildIndex < 10; grandChildIndex++) {
				IFolder grandChildFolder = createFolder(childFolders.get(grandChildIndex),
						"temp_grandChild_" + grandChildIndex);
				// each GrandChildren, intern contains another 10 folders and a single file.
				createTempFile(grandChildFolder);
				grandChildFolders.add(grandChildFolder);

			}
			for (int thirdLevelIndex = 0; thirdLevelIndex < 10; thirdLevelIndex++) {
				IFolder thirdLvLChildFolder = createFolder(grandChildFolders.get(thirdLevelIndex),
						"temp_3rdLvlChild_" + thirdLevelIndex);
				// each 3rd level folder contains a single file.
				createTempFile(thirdLvLChildFolder);
			}
		}, this.project, IWorkspace.AVOID_UPDATE, new NullProgressMonitor());
	}

	private void createTestProject() throws CoreException {
		Bug468PerformanceTest.this.project = ResourcesPlugin.getWorkspace().getRoot()
				.getProject(getName() + "_TestProject");
		Bug468PerformanceTest.this.project.create(new NullProgressMonitor());
		Bug468PerformanceTest.this.project.open(new NullProgressMonitor());
	}

	private IFolder createFolder(final IFolder parent, final String folderName) throws CoreException {
		IFolder childFolder = parent.getFolder(folderName);
		childFolder.create(true, true, new NullProgressMonitor());
		return childFolder;
	}

	private IFile createFile(final IFolder parent, final String fileName) throws CoreException {
		IFile file = parent.getFile(fileName);
		InputStream source = new ByteArrayInputStream(file.getName().getBytes());
		file.create(source, true, new NullProgressMonitor());
		file.setPersistentProperty(new QualifiedName(this.getClass().getName(), file.getName()), file.getName());
		return file;
	}

	private IFile createTempFile(final IFolder parent) throws CoreException {
		return createFile(parent, "tempFile");
	}

	/**
	 * Deletes the project
	 *
	 * @throws Exception
	 *             if any exception happens during the deleting of the project.
	 */
	@Override
	@After
	protected void tearDown() throws Exception {
		this.project.delete(true, new NullProgressMonitor());
		super.tearDown();
	}

	/*
	 * Test the timings for file deletion
	 */
	// For 3 tries, the time was around 18000 ms to 25000 ms in windows 10 machine,
	// so, set a limit of 1 minute.
	@Test
	public void test() throws CoreException {
		IFolder tempFolder = this.project.getFolder(TEMP_FOLDER_NAME);
		long[] timeTakenForDeletingFiles = new long[1];

		ResourcesPlugin.getWorkspace().run((ICoreRunnable) monitor -> {
			long startTime = System.currentTimeMillis();

			for (int fileIndex = 0; fileIndex < 6000; fileIndex++) {
				IFile fileToBeDeleted = tempFolder.getFile(TO_BE_DELETED_FILE_NAME_PREFIX + fileIndex);
				fileToBeDeleted.delete(true, new NullProgressMonitor());
			}

			long endTime = System.currentTimeMillis();
			timeTakenForDeletingFiles[0] = endTime - startTime;
		}, this.project, IWorkspace.AVOID_UPDATE, new NullProgressMonitor());

		long maxTime = TimeUnit.MILLISECONDS.convert(1, TimeUnit.MINUTES);
		assertTrue("The expected min time(ms): " + 0 + ", actual time(ms): " + timeTakenForDeletingFiles[0],
				0 <= timeTakenForDeletingFiles[0]);
		assertTrue("The expected max time(ms): " + maxTime + ", actual time(ms): " + timeTakenForDeletingFiles[0],
				timeTakenForDeletingFiles[0] <= maxTime);
	}
}
