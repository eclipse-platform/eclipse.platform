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

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.ResourceTest;

/**
 * Tests copying a file to a linked folder that does not exist on disk
 */
public class Bug_126104 extends ResourceTest {

	public void testBug() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("p1");
		IFile source = project.getFile("source");
		createInWorkspace(source);
		IFolder link = project.getFolder("link");
		IFileStore location = getTempStore();
		link.createLink(location.toURI(), IResource.ALLOW_MISSING_LOCAL, createTestMonitor());
		IFile destination = link.getFile(source.getName());
		source.copy(destination.getFullPath(), IResource.NONE, createTestMonitor());
		assertTrue("1.0", destination.exists());

		//try the same thing with move
		ensureDoesNotExistInWorkspace(destination);
		location.delete(EFS.NONE, createTestMonitor());
		source.move(destination.getFullPath(), IResource.NONE, createTestMonitor());
		assertTrue("3.0", !source.exists());
		assertTrue("3.1", destination.exists());
	}

}
