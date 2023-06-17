/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.ant.tests.core.testplugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.*;

/**
 * Helper methods to set up an IProject.
 */
public class ProjectHelper {

	public static final IPath TEST_BUILDFILES_DIR = IPath.fromOSString("testbuildfiles"); //$NON-NLS-1$
	public static final IPath TEST_RESOURCES_DIR = IPath.fromOSString("testresources"); //$NON-NLS-1$
	public static final IPath TEST_LIB_DIR = IPath.fromOSString("testlib"); //$NON-NLS-1$

	/**
	 * Creates a IProject.
	 */
	public static IProject createProject(String projectName) throws CoreException {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject(projectName);
		if (!project.exists()) {
			project.create(null);
		} else {
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
		}

		if (!project.isOpen()) {
			project.open(null);
		}

		return project;
	}

	/**
	 * Removes an IProject.
	 */
	public static void delete(IProject project) throws CoreException {
		project.delete(true, true, null);
	}

	/**
	 * Adds a folder to an IProject.
	 */
	public static IFolder addFolder(IProject project, String containerName) throws CoreException {

		IFolder folder = project.getFolder(containerName);
		if (!folder.exists()) {
			folder.create(false, true, null);
		}

		return folder;

	}

	public static void importFilesFromDirectory(File rootDir, IPath destPath, IProgressMonitor monitor) throws InvocationTargetException, IOException {
		IImportStructureProvider structureProvider = FileSystemStructureProvider.INSTANCE;
		List<File> files = new ArrayList<>(100);
		addFiles(rootDir, files);
		try {
			ImportOperation op = new ImportOperation(destPath, rootDir, structureProvider, new ImportOverwriteQuery(), files);
			op.setCreateContainerStructure(false);
			op.run(monitor);
		}
		catch (InterruptedException e) {
			// should not happen
		}
	}

	private static void addFiles(File dir, List<File> collection) throws IOException {
		List<File> subDirs = new ArrayList<>(2);
		for (File file : dir.listFiles()) {
			if (file.isFile()) {
				collection.add(file);
			} else if (file.isDirectory()) {
				subDirs.add(file);
			}
		}
		Iterator<File> iter = subDirs.iterator();
		while (iter.hasNext()) {
			File subDir = iter.next();
			addFiles(subDir, collection);
		}
	}

	private static class ImportOverwriteQuery implements IOverwriteQuery {
		@Override
		public String queryOverwrite(String file) {
			return ALL;
		}
	}

	public static final String PROJECT_NAME = "AntTests"; //$NON-NLS-1$
	public static final String BUILDFILES_FOLDER = "buildfiles"; //$NON-NLS-1$
	public static final String RESOURCES_FOLDER = "resources"; //$NON-NLS-1$
	public static final String LIB_FOLDER = "lib"; //$NON-NLS-1$
}
