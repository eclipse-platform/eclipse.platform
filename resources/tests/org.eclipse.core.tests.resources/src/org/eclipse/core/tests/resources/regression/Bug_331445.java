/*******************************************************************************
 *  Copyright (c) 2010, 2012 IBM Corporation and others.
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

import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createUniqueString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(WorkspaceResetExtension.class)
public class Bug_331445 {

	@Test
	public void testBug() throws CoreException, URISyntaxException {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject(createUniqueString());

		createInWorkspace(project);

		String variableName = "a" + createUniqueString();
		String variablePath = "mem:/MyProject";
		String folderName = "MyFolder";
		String rawLinkFolderLocation = variableName + "/" + folderName;
		String linkFolderLocation = variablePath + "/" + folderName;

		project.getPathVariableManager().setURIValue(variableName, new URI(variablePath));
		IFolder folder = project.getFolder(createUniqueString());
		folder.createLink(IPath.fromOSString(rawLinkFolderLocation), IResource.ALLOW_MISSING_LOCAL, createTestMonitor());
		assertNull(folder.getLocation());
		assertEquals(IPath.fromOSString(rawLinkFolderLocation), folder.getRawLocation());
		assertEquals(new URI(linkFolderLocation), folder.getLocationURI());
		assertEquals(new URI(rawLinkFolderLocation), folder.getRawLocationURI());
	}

}
