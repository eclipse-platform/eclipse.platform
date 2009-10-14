/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.tests.ui.testplugin;


import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.FileSystemStructureProvider;
import org.eclipse.ui.wizards.datatransfer.IImportStructureProvider;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;

/**
 * Helper methods to set up an IProject.
 */
public class ProjectHelper {
	
	public static final IPath TEST_BUILDFILES_DIR= new Path("testbuildfiles");
	public static final IPath TEST_RESOURCES_DIR= new Path("testresources");	
	public static final IPath TEST_LIB_DIR= new Path("testlib");
	
	public static final String PROJECT_NAME= "Ant UI Tests";
	
	/**
	 * Creates a IProject.
	 */	
	public static IProject createProject(String projectName) throws CoreException {
		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		IProject project= root.getProject(projectName);
		if (!project.exists()) {
			project.create(null);
		} else {
			project.refreshLocal(IResource.DEPTH_INFINITE, null);
		}
		
		if (!project.isOpen()) {
			project.open(null);
		}
		
		if (!project.hasNature(JavaCore.NATURE_ID)) {
			addNatureToProject(project, JavaCore.NATURE_ID, null);
		}
		
		return project;
	}
	
	private static void addNatureToProject(IProject proj, String natureId, IProgressMonitor monitor) throws CoreException {
		IProjectDescription description = proj.getDescription();
		String[] prevNatures= description.getNatureIds();
		String[] newNatures= new String[prevNatures.length + 1];
		System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
		newNatures[prevNatures.length]= natureId;
		description.setNatureIds(newNatures);
		proj.setDescription(description, monitor);
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
		
			IFolder folder= project.getFolder(containerName);
			if (!folder.exists()) {
				folder.create(false, true, null);
			}
		
		return folder;
		
	}
	
	public static void importFilesFromDirectory(File rootDir, IPath destPath, IProgressMonitor monitor) throws InvocationTargetException, IOException {		
		IImportStructureProvider structureProvider = FileSystemStructureProvider.INSTANCE;
		List files = new ArrayList(100);
		addFiles(rootDir, files);
		try {
			ImportOperation op= new ImportOperation(destPath, rootDir, structureProvider, new ImportOverwriteQuery(), files);
			op.setCreateContainerStructure(false);
			op.run(monitor);
		} catch (InterruptedException e) {
			// should not happen
		}
	}	
	
	private static void addFiles(File dir, List collection) throws IOException {
		File[] files = dir.listFiles();
		List subDirs = new ArrayList(2);
		for (int i = 0; i < files.length; i++) {
			if (files[i].isFile()) {
				collection.add(files[i]);
			} else if (files[i].isDirectory()) {
				subDirs.add(files[i]);
			}
		}
		Iterator iter = subDirs.iterator();
		while (iter.hasNext()) {
			File subDir = (File)iter.next();
			addFiles(subDir, collection);
		}
	}
	
	private static class ImportOverwriteQuery implements IOverwriteQuery {
		public String queryOverwrite(String file) {
			return ALL;
		}	
	}			
}
