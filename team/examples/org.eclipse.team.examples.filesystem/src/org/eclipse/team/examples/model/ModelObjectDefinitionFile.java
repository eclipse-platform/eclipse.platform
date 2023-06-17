/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.examples.model;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.examples.filesystem.FileSystemPlugin;

public class ModelObjectDefinitionFile extends ModelFile {

	public static final String MODEL_OBJECT_DEFINITION_FILE_EXTENSION = "mod";

	public static boolean isModFile(IResource resource) {
		if (resource instanceof IFile) {
			String fileExtension = resource.getFileExtension();
			if (fileExtension != null)
				return fileExtension.equals(MODEL_OBJECT_DEFINITION_FILE_EXTENSION);
		}
		return false;
	}

	public static IResource[] getReferencedResources(String projectName, IStorage storage) throws CoreException {
		if (storage == null)
			return new IResource[0];
		List<IResource> result = new ArrayList<>();
		String[] filePaths = readLines(storage);
		for (String path : filePaths) {
			IFile file = getFile(projectName, path);
			if (file != null
					&& file.getFileExtension() != null
					&& file.getFileExtension().equals(ModelObjectElementFile.MODEL_OBJECT_ELEMENTFILE_EXTENSION)) {
				result.add(file);
			}
		}
		return result.toArray(new IResource[result.size()]);
	}

	public ModelObjectDefinitionFile(IFile file) {
		super(file);
	}

	@Override
	public ModelObject[] getChildren() throws CoreException {
		return getModelObjectElementFiles();
	}

	public ModelObjectElementFile[] getModelObjectElementFiles() throws CoreException {
		List<ModelObjectElementFile> result = new ArrayList<>();
		String[] filePaths = readLines((IFile)getResource());
		for (String path : filePaths) {
			IFile file = getFile(getResource().getProject().getName(), path);
			if (file != null) {
				ModelObjectElementFile moeFile = getMoeFile(file);
				if (moeFile != null)
					result.add(moeFile);
			}
		}
		return result.toArray(new ModelObjectElementFile[result.size()]);
	}

	private static String[] readLines(IStorage file) throws CoreException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(file.getContents()));
		String line = null;
		List<String> result = new ArrayList<>();
		try {
			while ((line = reader.readLine()) != null) {
				result.add(line.trim());
			}
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, FileSystemPlugin.ID, 0,
					NLS.bind("Error reading from file {0}", file.getFullPath()), e));
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				// Ignore
			}
		}
		return result.toArray(new String[result.size()]);
	}

	private void writeLines(String[] strings) throws CoreException {
		StringBuilder buffer = new StringBuilder();
		for (String string : strings) {
			buffer.append(string);
			buffer.append("\n");
		}
		((IFile)getResource()).setContents(new ByteArrayInputStream(buffer.toString().getBytes()), false, true, null);
	}

	private ModelObjectElementFile getMoeFile(IFile file) {
		if (ModelObjectElementFile.isMoeFile(file)) {
			return new ModelObjectElementFile(this, file);
		}
		return null;
	}

	private static IFile getFile(String projectName, String path) {
		if (path.length() == 0)
			return null;
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IStatus status = workspace.validatePath("/" + projectName + "/" + path, IResource.FILE);
		if (status.isOK()) {
			IProject project = workspace.getRoot().getProject(projectName);
			return project.getFile(IPath.fromOSString(path));
		}
		FileSystemPlugin.log(status);
		return null;
	}

	public void addMoe(IFile file) throws CoreException {
		((IFile)getResource()).appendContents(new ByteArrayInputStream(("\n" + file.getProjectRelativePath()).getBytes()), false, true, null);
	}

	public void remove(ModelObjectElementFile file) throws CoreException {
		ModelObjectElementFile[] files = getModelObjectElementFiles();
		List<String> paths = new ArrayList<>();
		for (ModelObjectElementFile child : files) {
			if (!child.equals(file)) {
				paths.add(child.getResource().getProjectRelativePath().toString());
			}
		}
		writeLines(paths.toArray(new String[paths.size()]));
	}

	@Override
	public void delete() throws CoreException {
		ModelObjectElementFile[] files = getModelObjectElementFiles();
		super.delete();
		for (ModelObjectElementFile file : files) {
			file.getResource().delete(false, null);
		}
	}

	public void setElements(IResource[] resources) throws CoreException {
		List<String> paths = new ArrayList<>();
		for (IResource resource : resources) {
			paths.add(resource.getProjectRelativePath().toString());
		}
		writeLines(paths.toArray(new String[paths.size()]));
	}

	public boolean hasMoe(IFile file) throws CoreException {
		ModelObjectElementFile[] files = getModelObjectElementFiles();
		for (ModelObjectElementFile child : files) {
			if (child.getResource().equals(file))
				return true;
		}
		return false;
	}

}
