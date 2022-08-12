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

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.tests.resources.ResourceTest;

/**
 * When a container was moved, its children were not added to phantom space.
 *
 */
public class Bug_029671 extends ResourceTest {

	public void testBug() {
		final QualifiedName partner = new QualifiedName("org.eclipse.core.tests.resources", "myTarget");
		IWorkspace workspace = getWorkspace();
		final ISynchronizer synchronizer = workspace.getSynchronizer();
		synchronizer.add(partner);

		IProject project = workspace.getRoot().getProject("MyProject");
		IFolder folder = project.getFolder("source");
		IFile file = folder.getFile("file.txt");

		ensureExistsInWorkspace(file, true);

		try {
			// sets sync info for the folder and its children
			try {
				synchronizer.setSyncInfo(partner, folder, getRandomString().getBytes());
				synchronizer.setSyncInfo(partner, file, getRandomString().getBytes());
			} catch (CoreException ce) {
				fail("1.0", ce);
			}

			IFolder targetFolder = project.getFolder("target");
			IFile targetFile = targetFolder.getFile(file.getName());

			try {
				folder.move(targetFolder.getFullPath(), false, false, getMonitor());
			} catch (CoreException e) {
				fail("2.0", e);
			}
			assertTrue("3.0", folder.isPhantom());
			assertTrue("4.0", file.isPhantom());

			assertExistsInWorkspace("5.0", targetFolder);
			assertTrue("5.1", !targetFolder.isPhantom());

			assertExistsInWorkspace("6.0", targetFile);
			assertTrue("6.1", !targetFile.isPhantom());
		} finally {
			synchronizer.remove(partner);
		}
	}
}
