/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.tests.core;


import java.io.File;

import org.eclipse.ant.tests.core.testplugin.AntTestPlugin;
import org.eclipse.ant.tests.core.testplugin.ProjectHelper;
import org.eclipse.core.resources.*;

/**
 * Test to close the workbench, since debug tests do not run in the UI
 * thread.
 */
public class ProjectCreationDecorator extends AbstractAntTest {
	
	public ProjectCreationDecorator(String name) {
		super(name);
	}
	
	public void testProjectCreation() throws Exception {
		// delete any pre-existing project
		IProject pro = ResourcesPlugin.getWorkspace().getRoot().getProject("AntTests");
		if (pro.exists()) {
			pro.delete(true, true, null);
		}
		// create project and import buildfiles and support files
		project = ProjectHelper.createProject("AntTests", "bin");
		IFolder folder = ProjectHelper.addFolder(project, "buildfiles");
		File root = AntTestPlugin.getDefault().getFileInPlugin(ProjectHelper.TEST_BUILDFILES_DIR);
		ProjectHelper.importFilesFromDirectory(root, folder.getFullPath(), null);
		
		folder = ProjectHelper.addFolder(project, "resources");
		root = AntTestPlugin.getDefault().getFileInPlugin(ProjectHelper.TEST_RESOURCES_DIR);
		ProjectHelper.importFilesFromDirectory(root, folder.getFullPath(), null);
		
		folder = ProjectHelper.addFolder(project, "lib");
		root = AntTestPlugin.getDefault().getFileInPlugin(ProjectHelper.TEST_LIB_DIR);
		ProjectHelper.importFilesFromDirectory(root, folder.getFullPath(), null);
	}
}
