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

import java.io.ByteArrayInputStream;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.ResourceTest;

public class Bug_006708 extends ResourceTest {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		deleteProject("bug_6708");
		deleteProject("bug_6708_2");
	}

	static void deleteProject(String name) throws CoreException {
		IProject p = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
		p.delete(true, null);
	}

	public void testBug() throws CoreException {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject sourceProj = root.getProject("bug_6708");
		assertFalse("Project bug_6708 exists already", sourceProj.exists());
		sourceProj.create(null);
		sourceProj.open(null);
		IFile source = sourceProj.getFile("Source.txt");
		source.create(new ByteArrayInputStream("abcdef".getBytes()), false, null);

		IProject destProj = root.getProject("bug_6708_2");
		assertFalse("Project bug_6708_2 exists already", destProj.exists());
		destProj.create(null);
		destProj.open(null);
		IFile dest = destProj.getFile("Dest.txt");

		source.copy(dest.getFullPath(), false, null);
		dest.setContents(new ByteArrayInputStream("ghijkl".getBytes()), false, true, null);
	}
}
