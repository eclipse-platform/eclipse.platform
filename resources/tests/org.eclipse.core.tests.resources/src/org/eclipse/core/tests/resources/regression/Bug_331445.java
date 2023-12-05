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

import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.tests.resources.ResourceTest;

public class Bug_331445 extends ResourceTest {
	public void testBug() throws CoreException, URISyntaxException {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject(getUniqueString());

		ensureExistsInWorkspace(project, true);

		String variableName = "a" + getUniqueString();
		String variablePath = "mem:/MyProject";
		String folderName = "MyFolder";
		String rawLinkFolderLocation = variableName + "/" + folderName;
		String linkFolderLocation = variablePath + "/" + folderName;

		project.getPathVariableManager().setURIValue(variableName, new URI(variablePath));
		IFolder folder = project.getFolder(getUniqueString());
		folder.createLink(IPath.fromOSString(rawLinkFolderLocation), IResource.ALLOW_MISSING_LOCAL, getMonitor());
		assertNull("3.0", folder.getLocation());
		assertEquals("4.0", IPath.fromOSString(rawLinkFolderLocation), folder.getRawLocation());
		assertEquals("5.0", new URI(linkFolderLocation), folder.getLocationURI());
		assertEquals("6.0", new URI(rawLinkFolderLocation), folder.getRawLocationURI());
	}
}
