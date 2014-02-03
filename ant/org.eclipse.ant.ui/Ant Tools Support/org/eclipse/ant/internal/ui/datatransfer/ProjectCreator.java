/*******************************************************************************
 * Copyright (c) 2004, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.datatransfer;

import java.io.File;
import com.ibm.icu.text.MessageFormat;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Javac;
import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.JavaRuntime;

public class ProjectCreator {
		
	public IJavaProject createJavaProjectFromJavacNode(String projectName, Javac javacTask, IProgressMonitor monitor) throws CoreException {
		try {
			IJavaProject javaProject = createJavaProject(projectName, monitor);
			
			File destDir= javacTask.getDestdir();
			String destDirName= destDir == null ? null : destDir.getName();
			org.apache.tools.ant.types.Path sourceDirs= javacTask.getSrcdir();
			createSourceDirectories(destDir, destDirName, sourceDirs, javaProject, monitor);
			
			// add rt.jar
			addVariableEntry(javaProject, new Path(JavaRuntime.JRELIB_VARIABLE), new Path(JavaRuntime.JRESRC_VARIABLE), new Path(JavaRuntime.JRESRCROOT_VARIABLE), monitor);
			
			setClasspath(javacTask, javaProject, monitor);
			
			javaProject.getProject().refreshLocal(IResource.DEPTH_INFINITE, monitor);
			return javaProject;
		} catch (BuildException be) {
			IStatus status= new Status(IStatus.ERROR, AntUIPlugin.PI_ANTUI, IStatus.OK, be.getLocalizedMessage(), be);
			throw new CoreException(status);
		}
	}
	
	private void setClasspath(Javac javacTask, IJavaProject javaProject, IProgressMonitor monitor) throws CoreException {
		try {
			org.apache.tools.ant.types.Path classpath= javacTask.getClasspath();
			if (classpath == null) {
				return;
			}
			String[] classpaths= classpath.list();
			for (int i = 0; i < classpaths.length; i++) {
				String cp = classpaths[i];
				File classpathEntry= new File(cp);
				if (classpathEntry.isFile()) {
					addLibrary(javaProject, new Path(classpathEntry.getAbsolutePath()), monitor);
				} else {
					addContainer(javaProject, new Path(classpathEntry.getAbsolutePath()), monitor);
				}
			}
		} catch (BuildException be) {
			IStatus status= new Status(IStatus.ERROR, AntUIPlugin.PI_ANTUI, IStatus.OK, MessageFormat.format(DataTransferMessages.ProjectCreator_0, new String[]{be.getLocalizedMessage()}), null);
			throw new CoreException(status);
		}
	}

	private void createSourceDirectories(File destDir, String destDirName, org.apache.tools.ant.types.Path sourceDirs, IJavaProject javaProject, IProgressMonitor monitor) throws CoreException {
		String[] sourceDirectories= sourceDirs.list();
		for (int i = 0; i < sourceDirectories.length; i++) {
			String srcDir = sourceDirectories[i];
			File srcDirectory= new File(srcDir);
			String srcDirectoryName= srcDirectory.getName();
			String destDirPath= destDir == null ? srcDir : destDir.getAbsolutePath();
			if (destDirName == null) {
			    destDirName= srcDirectoryName;
			}
			addSourceContainer(javaProject, srcDirectoryName, srcDir, destDirName, destDirPath, monitor);
		}
	}

	private IJavaProject createJavaProject(String projectName, IProgressMonitor monitor) throws CoreException {
		IWorkspaceRoot root= ResourcesPlugin.getWorkspace().getRoot();
		IProject project= root.getProject(projectName);
		if (!project.exists()) {
			project.create(monitor);
		} else {
			project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
		}
		
		if (!project.isOpen()) {
			project.open(monitor);
		}
		
		if (!project.hasNature(JavaCore.NATURE_ID)) {
			addNatureToProject(project, JavaCore.NATURE_ID, monitor);
		}
		
		IJavaProject jproject= JavaCore.create(project);
		
		jproject.setRawClasspath(new IClasspathEntry[0], monitor);
		
		return jproject;	
	}
	
	private void addNatureToProject(IProject proj, String natureId, IProgressMonitor monitor) throws CoreException {
		IProjectDescription description = proj.getDescription();
		String[] prevNatures= description.getNatureIds();
		String[] newNatures= new String[prevNatures.length + 1];
		System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
		newNatures[prevNatures.length]= natureId;
		description.setNatureIds(newNatures);
		proj.setDescription(description, monitor);
	}
	
	/**
	 * Adds a source container to a IJavaProject.
	 */		
	private void addSourceContainer(IJavaProject jproject, String srcName, String srcPath, String outputName, String outputPath, IProgressMonitor monitor) throws CoreException {
		IProject project= jproject.getProject();
		IContainer container= null;
		if (srcName == null || srcName.length() == 0) {
			container= project;
		} else {
			IFolder folder= project.getFolder(srcName);
			if (!folder.exists()) {
				folder.createLink(new Path(srcPath), IResource.ALLOW_MISSING_LOCAL, monitor);
			}
			container= folder;
		}
		IPackageFragmentRoot root= jproject.getPackageFragmentRoot(container);

		IPath output= null;
		if (outputName!= null) {
			IFolder outputFolder = project.getFolder(outputName);
			if (!outputFolder.exists()) {
			    outputFolder.createLink(new Path(outputPath), IResource.ALLOW_MISSING_LOCAL, monitor);
			}
			output= outputFolder.getFullPath();
		}
				
		IClasspathEntry cpe= JavaCore.newSourceEntry(root.getPath(), new IPath[0], output);
		
		addToClasspath(jproject, cpe, monitor);		
	}	
	
	private void addToClasspath(IJavaProject jproject, IClasspathEntry cpe, IProgressMonitor monitor) throws JavaModelException {
		IClasspathEntry[] oldEntries= jproject.getRawClasspath();
		for (int i= 0; i < oldEntries.length; i++) {
			if (oldEntries[i].equals(cpe)) {
				return;
			}
		}
		int nEntries= oldEntries.length;
		IClasspathEntry[] newEntries= new IClasspathEntry[nEntries + 1];
		System.arraycopy(oldEntries, 0, newEntries, 0, nEntries);
		newEntries[nEntries]= cpe;
		jproject.setRawClasspath(newEntries, monitor);
	}
	
	/**
	 * Adds a variable entry with source attachment to a IJavaProject if the path can be resolved.
	 */			
	private void addVariableEntry(IJavaProject jproject, IPath path, IPath sourceAttachPath, IPath sourceAttachRoot, IProgressMonitor monitor) throws JavaModelException {
		IClasspathEntry cpe= JavaCore.newVariableEntry(path, sourceAttachPath, sourceAttachRoot);
		addToClasspath(jproject, cpe, monitor);
	}
	
	/**
	 * Adds a library entry to an IJavaProject.
	 */			
	private void addLibrary(IJavaProject jproject, IPath path, IProgressMonitor monitor) throws JavaModelException {
		IClasspathEntry cpe= JavaCore.newLibraryEntry(path, null, null);
		addToClasspath(jproject, cpe, monitor);
	}
	
	/**
	 * Adds a container entry to an IJavaProject.
	 */			
	private void addContainer(IJavaProject jproject, IPath path, IProgressMonitor monitor) throws CoreException {
		IClasspathEntry cpe= JavaCore.newContainerEntry(path);
		addToClasspath(jproject, cpe, monitor);
		IProject project= jproject.getProject();
		IFolder folder= project.getFolder(path.lastSegment());
		if (!folder.exists()) {
			folder.createLink(path, IResource.ALLOW_MISSING_LOCAL, monitor);
		}
	}
}
