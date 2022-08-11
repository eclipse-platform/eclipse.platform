/*******************************************************************************
 * Copyright (c) 2009, 2015 IBM Corporation and others.
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

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.tests.resources.ResourceTest;

public class Bug_264182 extends ResourceTest {

	IProject project;
	IFile dotProject;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		// create a project
		project = getWorkspace().getRoot().getProject(getUniqueString());
		try {
			project.create(new NullProgressMonitor());
			project.open(new NullProgressMonitor());
		} catch (CoreException e) {
			fail("Project creation failed", e);
		}

		// set the description read-only
		dotProject = project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		setReadOnly(dotProject, true);
	}

	@Override
	protected void tearDown() throws Exception {
		// make the description writable
		setReadOnly(dotProject, false);
		super.tearDown();
	}

	public void testBug() {
		// create a linked resource
		final IFile file = project.getFile(getUniqueString());
		IFileStore tempFileStore = getTempStore();
		createFileInFileSystem(tempFileStore);
		try {
			file.createLink(tempFileStore.toURI(), IResource.NONE, new NullProgressMonitor());
			fail("1.0");
		} catch (CoreException e) {
			// should fail updating the description
		}

		// the file should not exist in the workspace
		assertDoesNotExistInWorkspace(file);
	}
}
