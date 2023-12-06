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
package org.eclipse.core.tests.resources.regression;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertExistsInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomString;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ISynchronizer;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.tests.resources.ResourceTest;

/**
 * When a container was moved, its children were not added to phantom space.
 */
public class Bug_029671 extends ResourceTest {

	public void testBug() throws CoreException {
		final QualifiedName partner = new QualifiedName("org.eclipse.core.tests.resources", "myTarget");
		IWorkspace workspace = getWorkspace();
		final ISynchronizer synchronizer = workspace.getSynchronizer();
		synchronizer.add(partner);

		IProject project = workspace.getRoot().getProject("MyProject");
		IFolder folder = project.getFolder("source");
		IFile file = folder.getFile("file.txt");

		createInWorkspace(file);

		try {
			// sets sync info for the folder and its children
			synchronizer.setSyncInfo(partner, folder, createRandomString().getBytes());
			synchronizer.setSyncInfo(partner, file, createRandomString().getBytes());

			IFolder targetFolder = project.getFolder("target");
			IFile targetFile = targetFolder.getFile(file.getName());

			folder.move(targetFolder.getFullPath(), false, false, createTestMonitor());
			assertTrue("3.0", folder.isPhantom());
			assertTrue("4.0", file.isPhantom());

			assertExistsInWorkspace(targetFolder);
			assertTrue("5.1", !targetFolder.isPhantom());

			assertExistsInWorkspace(targetFile);
			assertTrue("6.1", !targetFile.isPhantom());
		} finally {
			synchronizer.remove(partner);
		}
	}
}
