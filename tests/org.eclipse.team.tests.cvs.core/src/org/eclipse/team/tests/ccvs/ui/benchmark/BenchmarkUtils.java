/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.team.tests.ccvs.ui.benchmark;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.internal.ccvs.core.CVSStatus;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;
import org.eclipse.ui.wizards.datatransfer.ZipFileStructureProvider;
import org.junit.Assert;

/**
 * Provides helpers for:
 * <ul>
 *   <li>Resource manipulation</li>
 *   <li>Diff trees</li>
 *   <li>UI automation</li>
 *   <li>Parallel development simulation</li>
 * </ul>
 * 
 * Note: This class is referenced from the VCM 1.0 performance tests.
 */
public class BenchmarkUtils {
	/*** RESOURCE MANIPULATION SUPPORT ***/
	
	/**
	 * Gets a handle for a project of a given name.
	 * @param name the project name
	 * @return the project handle
	 */
	public static IProject getProject(String name) {
		return ResourcesPlugin.getWorkspace().getRoot().getProject(name);
	}
	
	/**
	 * Creates a new project.
	 * @param name the project name
	 * @return the project handle
	 */
	public static IProject createProject(String name) throws CoreException {
		IProject project = getProject(name);
		if (!project.exists()) project.create(null);
		if (!project.isOpen()) project.open(null);
		return project;
	}

	/**
	 * Deletes a project.
	 * @param project the project
	 */
	public static void deleteProject(IProject project) throws CoreException {
		project.delete(false /*force*/, null);
	}
	
	/**
	 * Deletes a file and prunes empty containing folders.
	 * @param file the file to delete
	 */
	public static void deleteFileAndPrune(IFile file) throws CoreException {
		file.delete(false /*force*/, null);
		IContainer container = file.getParent();
		while (container != null && container instanceof IFolder &&
			isFolderEmpty((IFolder) container)) {
			deleteFolder((IFolder) container);
			container = container.getParent();
		}
	}
	
	/**
	 * Deletes a folder.
	 */
	public static void deleteFolder(IFolder folder) throws CoreException {
		try {
			folder.delete(false /*force*/, null);
		} catch (CoreException e) {
			IStatus status = e.getStatus();
			// ignore errors caused by attempting to delete folders that CVS needs to have around
			if (findStatusByCode(status, CVSStatus.FOLDER_NEEDED_FOR_FILE_DELETIONS) == null) {
				throw e;
			}
		}
	}
	
	/**
	 * Finds an IStatus instance in a multi-status by status code.
	 */
	public static IStatus findStatusByCode(IStatus status, int code) {
		if (status.getCode() == code) return status;
		IStatus[] children = status.getChildren();
		for (IStatus child : children) {
			IStatus found = findStatusByCode(child, code);
			if (found != null) return found;
		}
		return null;
	}

	/**
	 * Creates a uniquely named project.
	 * @param prefix a string prepended to the generated name
	 * @return the new project
	 */
	public static IProject createUniqueProject(String prefix) throws CoreException {
		return createProject(makeUniqueName(null, prefix, null));
	}

	/**
	 * Creates a uniquely named file in the parent folder or project with random contents.
	 * @param gen the sequence generator
	 * @param parent the parent IFolder or IProject for the new file
	 * @param meanSize the mean size of file to create (in bytes)
	 * @param variance 69% of files with be within this amount of the mean
	 * @param probBinary the probability of a new file being binary as a percentage
	 * @return the new file
	 */
	public static IFile createUniqueFile(SequenceGenerator gen, IContainer parent,
		int meanSize, int variance, int probBinary) throws IOException, CoreException {
		int fileSize;
		do {
			fileSize = (int) Math.abs(gen.nextGaussian() * variance + meanSize);
		} while (fileSize > meanSize + variance * 4); // avoid huge files
		
		IFile file;
		try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
			String fileName;
			if (gen.nextInt(100) < probBinary) {
				fileName = makeUniqueName(gen, "file", "class"); // binary
				writeRandomBytes(gen, os, fileSize);
			} else {
				fileName = makeUniqueName(gen, "file", "txt"); // text
				writeRandomText(gen, os, fileSize);
			}	
			file = parent.getFile(new Path(fileName));
			file.create(new ByteArrayInputStream(os.toByteArray()), true, new NullProgressMonitor());
		}
		return file;
	}

	/**
	 * Creates a uniquely named folder in the parent folder.
	 * @param gen the sequence generator
	 * @param parent the parent IFolder or IProject for the new folder
	 * @return the new folder
	 */
	public static IFolder createUniqueFolder(SequenceGenerator gen, IContainer parent) throws CoreException {
		IFolder folder = parent.getFolder(new Path(BenchmarkUtils.makeUniqueName(gen, "folder", null)));
		folder.create(false /*force*/, true /*local*/, null);
		return folder;
	}
	
	/**
	 * Renames a resource.
	 * The resource handle becomes invalid.
	 * @param resource the existing resource
	 * @param newName the new name for the resource
	 */
	public static void renameResource(IResource resource, String newName) throws CoreException {
		switch (resource.getType()) {
			case IResource.PROJECT: {
				IProject project = (IProject) resource;
				IProjectDescription desc = project.getDescription();
				desc.setName(newName);
				project.move(desc, false /*force*/, true /*keepHistory*/, null);
			} break;
			case IResource.FOLDER: {
				try {
					resource.move(new Path(newName), false /*force*/, null);
				} catch (CoreException e) {
					IStatus status = e.getStatus();
					// ignore errors caused by attempting to delete folders that CVS needs to have around
					if (findStatusByCode(status, CVSStatus.FOLDER_NEEDED_FOR_FILE_DELETIONS) == null) {
						throw e;
					}
				}
			} break;
			default:
				resource.move(new Path(newName), false /*force*/, null);
				break;
		}
	}

	/**
	 * Modified a resource.
	 * @param gen the sequence generator
	 * @param file the file to modify
	 */
	public static void modifyFile(SequenceGenerator gen, IFile file)
		throws IOException, CoreException {
		try (ByteArrayOutputStream os = new ByteArrayOutputStream(); InputStream is = file.getContents(true)) {
				byte[] buffer = new byte[8192];
				int rsize;
				boolean changed = false;
				while ((rsize = is.read(buffer)) != -1) {
					double gaussian;
					do {
						gaussian = gen.nextGaussian() * 0.5; // large changes are less likely than small ones
					} while (gaussian > 1.0 || gaussian < -1.0);
					int changeSize = (int) (gaussian * rsize);
					changed = changed || changeSize != 0;
					os.write(buffer, 0, changeSize < 0 ? - changeSize : rsize); // shrink file
					writeRandomText(gen, os, changeSize); // enlarge file
				}
				if (! changed) os.write('!'); // make sure we actually did change the file
				file.setContents(new ByteArrayInputStream(os.toByteArray()), false /*force*/, true /*keepHistory*/, null);
		}
	}
	
	/**
	 * Creates a unique name.
	 * Ensures that a deterministic sequence of names is generated for all files
	 * and folders within a project, though not across sessions.
	 * 
	 * @param gen the generator, or null if this name is to be globally unique
	 * @param prefix a string prepended to the generated name
	 * @param extension the file extension not including the period, null if none
	 * @return the new name
	 */
	public static String makeUniqueName(SequenceGenerator gen, String prefix, String extension) {
		StringBuilder name = new StringBuilder(prefix);
		name.append('-');
		if (gen == null) {
			name.append(SequenceGenerator.nextGloballyUniqueLong());
		} else {
			name.append(gen.nextUniqueInt());
		}
		if (extension != null) {
			name.append('.');
			name.append(extension);
		}
		return name.toString();
	}
	
	/**
	 * Imports a .zip file into a container's root folder.
	 * @param container the container
	 * @param file the path of the .zip file
	 */
	public static void importZip(IContainer container, File file)
		throws IOException, ZipException, InterruptedException, InvocationTargetException {
		ZipFile zipFile = new ZipFile(file);
		ZipFileStructureProvider provider = new ZipFileStructureProvider(zipFile);
		ImportOperation importOperation = new ImportOperation(container.getFullPath(),
			provider.getRoot(), provider, null);
		importOperation.setOverwriteResources(true); // don't ask
		importOperation.run(new NullProgressMonitor());
		Assert.assertTrue(importOperation.getStatus().isOK());
	}

	/**
	 * Writes random text to an output stream.
	 * @param gen the sequence generator
	 */
	public static void writeRandomText(SequenceGenerator gen, OutputStream os, int count) throws IOException {
		while (count-- > 0) {
			int c = gen.nextInt(99);
			os.write((c >= 95) ? '\n' : c + ' ');
		}
	}

	/**
	 * Writes random bytes to an output stream.
	 * @param gen the sequence generator
	 */
	public static void writeRandomBytes(SequenceGenerator gen, OutputStream os, int count) throws IOException {
		while (count-- > 0) {
			os.write(gen.nextInt(256));
		}
	}

	/**
	 * Creates a random folder deeply below the root folder.
	 * @param gen the sequence generator
	 * @param root the root IFolder or IProject for the operation
	 * @return the new folder
	 */
	public static IFolder createRandomDeepFolder(SequenceGenerator gen, IContainer root) throws CoreException {
		IContainer container = pickRandomDeepContainer(gen, root);
		for (;;) {
			IFolder folder = createUniqueFolder(gen, container);
			container = folder;
			// 12.5% chance of creating a nested folder
			if (gen.nextInt(8) != 0) return folder;
		}
	}
	
	/**
	 * Creates several random files deeply below the root folder.
	 * @param gen the sequence generator
	 * @param root the root IFolder or IProject for the operation
	 * @param count the number of files to create
	 * @param meanSize the mean size of file to create (in bytes)
	 * @param probBinary the probability of a new file being binary as a percentage
	 * @return the created files
	 * @throws IOException
	 * @throws CoreException
	 */
	public static IFile[] createRandomDeepFiles(SequenceGenerator gen, IContainer root, int count,
		int meanSize, int variance, int probBinary) throws IOException, CoreException  {
		IFile[] files = new IFile[count];
		while (count-- > 0) {
			files[count] = createUniqueFile(gen, pickRandomDeepContainer(gen, root), meanSize, variance, probBinary);
		}
		return files;
	}

	/**
	 * Deletes several random files deeply below the root folder.
	 * @param gen the sequence generator
	 * @param root the root IFolder or IProject for the operation
	 * @param count the number of files to delete
	 */
	public static void deleteRandomDeepFiles(SequenceGenerator gen, IContainer root, int count) throws CoreException  {
		while (count-- > 0) {
			IFile file = pickRandomDeepFile(gen, root);
			if (file == null) break;
			deleteFileAndPrune(file);
		}
	}

	/**
	 * Modifies several random files deeply below the root folder.
	 * @param gen the sequence generator
	 * @param root the root IFolder or IProject for the operation
	 * @param count the number of files to modify
	 */
	public static void modifyRandomDeepFiles(SequenceGenerator gen, IContainer root, int count)
		throws IOException, CoreException  {
		// perhaps we can add a parameter for the "magnitude" of the change
		while (count-- > 0) {
			IFile file = pickRandomDeepFile(gen, root);
			if (file == null) break;
			modifyFile(gen, file);
		}
	}
	
	/**
	 * Touches several random files deeply below the root folder.
	 * @param gen the sequence generator
	 * @param root the root IFolder or IProject for the operation
	 * @param count the number of files to touch
	 */
	public static void touchRandomDeepFiles(SequenceGenerator gen, IContainer root, int count) throws CoreException  {
		while (count-- > 0) {
			IFile file = pickRandomDeepFile(gen, root);
			if (file == null) break;
			file.touch(null);
		}
	}
	
	/**
	 * Renames several random files deeply below the root folder.
	 * @param gen the sequence generator
	 * @param root the root IFolder or IProject for the operation
	 * @param count the number of files to touch
	 */
	public static void renameRandomDeepFiles(SequenceGenerator gen, IContainer root, int count) throws CoreException  {
		while (count-- > 0) {
			IFile file = pickRandomDeepFile(gen, root);
			if (file == null) break;
			renameResource(file, makeUniqueName(gen, "file", file.getFileExtension()));
		}
	}
	
	/**
	 * Picks a random file from the parent folder or project.
	 * @param gen the sequence generator
	 * @param parent the parent IFolder or IProject for the operation
	 * @return the file that was chosen, or null if no suitable files
	 */
	public static IFile pickRandomFile(SequenceGenerator gen, IContainer parent) throws CoreException  {
		IResource[] members = filterResources(parent.members());
		for (int size = members.length; size != 0; --size) {
			int elem = gen.nextInt(size);
			if (members[elem] instanceof IFile) return (IFile) members[elem];			
			System.arraycopy(members, elem + 1, members, elem, size - elem - 1);
		}
		return null;
	}

	/**
	 * Picks a random folder from the parent folder or project.
	 * @param gen the sequence generator
	 * @param parent the parent IFolder or IProject for the operation
	 * @return the folder, or null if no suitable folders
	 */
	public static IFolder pickRandomFolder(SequenceGenerator gen, IContainer parent) throws CoreException {
		IResource[] members = filterResources(parent.members());
		for (int size = members.length; size != 0; --size) {
			int elem = gen.nextInt(size);
			if (members[elem] instanceof IFolder) return (IFolder) members[elem];
			System.arraycopy(members, elem + 1, members, elem, size - elem - 1);
		}
		return null;
	}

	/**
	 * Picks a random file deeply from the root folder or project.
	 * @param gen the sequence generator
	 * @param root the root IFolder or IProject for the operation
	 * @return the file that was chosen, or null if no suitable files
	 */
	public static IFile pickRandomDeepFile(SequenceGenerator gen, IContainer root) throws CoreException  {
		IResource[] members = filterResources(root.members());
		for (int size = members.length; size != 0; --size) {
			int elem = gen.nextInt(size);
			IResource resource = members[elem];
			if (resource instanceof IFile) return (IFile) resource;
			if (resource instanceof IFolder) {
				IFile file = pickRandomDeepFile(gen, (IFolder) resource);
				if (file != null) return file;
			}
			System.arraycopy(members, elem + 1, members, elem, size - elem - 1);
		}
		return null;
	}

	/**
	 * Picks a random folder deeply from the root folder or project.
	 * May pick the project's root container.
	 * @param gen the sequence generator
	 * @param root the root IFolder or IProject for the operation
	 * @return the container that was chosen, never null
	 */
	public static IContainer pickRandomDeepContainer(SequenceGenerator gen, IContainer root) throws CoreException {
		if (gen.nextInt(6) == 0) {
			IResource[] members = filterResources(root.members());
			for (int size = members.length; size != 0; --size) {
				int elem = gen.nextInt(size);
				IResource resource = members[elem];
				if (resource instanceof IFolder) {
					return pickRandomDeepContainer(gen, (IFolder) resource);
				}
				System.arraycopy(members, elem + 1, members, elem, size - elem - 1);
			}
		}
		Assert.assertTrue(isValidContainer(root));
		return root;
	}
	
	/**
	 * Returns true if the folder does not contain any real files.
	 */
	public static boolean isFolderEmpty(IFolder folder) throws CoreException {
		IResource[] members = folder.members();
		for (IResource member : members) {
			if (isValidFile(member) || isValidFolder(member)) return false;
		}
		return true;
	}

	/**
	 * Returns true iff file is a valid IFile (that should not be ignored).
	 */
	public static boolean isValidFile(IResource file) {
		String name = file.getName();
		return file instanceof IFile
			&& ! file.isPhantom()
			&& ! name.equals(".classpath")
			&& ! name.equals(".project")
			&& ! name.equals(".vcm_meta");
	}

	/**
	 * Returns true iff folder is a valid IFolder (that should not be ignored).
	 */
	public static boolean isValidFolder(IResource folder) {
		String name = folder.getName();
		return folder instanceof IFolder
			&& ! folder.isPhantom()
			&& ! name.equals("CVS")
			&& ! name.equals("bin");
	}

	/**
	 * Returns true iff container is a valid IFolder or IProject (that should not be ignored).
	 */
	public static boolean isValidContainer(IResource container) {
		return container instanceof IProject || isValidFolder(container);
	}
	
	/**
	 * Returns true iff resource is a valid IFile, IFolder or IProject (that should not be ignored).
	 */
	public static boolean isValidResource(IResource resource) {
		return isValidFile(resource) || isValidContainer(resource);
	}

	/**
	 * Filters and sorts an array of resources to ensure deterministic behaviour across
	 * sessions.  The general idea is to guarantee that given a known sequence of
	 * pseudo-random numbers, we will always pick the same sequence of files and
	 * folders each time we repeat the test.
	 */
	public static IResource[] filterResources(IResource[] resources) {
		List<IResource> list = new ArrayList<>(resources.length);
		for (IResource resource : resources) {
			if (isValidResource(resource)) list.add(resource);
		}
		if (list.size() != resources.length) {
			resources = list.toArray(new IResource[list.size()]);
		}
		Arrays.sort(resources, (a, b) -> a.getName().compareTo(b.getName()));
		return resources;
	}
	
	/*** DIFF SUPPORT ***/
	
	public static boolean isEmpty(IDiffContainer node) {
		if (node == null) return true;
		if (node.getKind() != 0) return false;
		IDiffElement[] children = node.getChildren();
		for (IDiffElement child : children) {
			if (!isEmpty(child)) {
				return false;
			}
		}
		return true;
	}
	public static boolean isEmpty(IDiffElement element) {
		if (element == null) return true;
		if (element.getKind() != 0) return false;
		if (element instanceof IDiffContainer) {
			IDiffElement[] children = ((DiffNode)element).getChildren();
			for (IDiffElement child : children) {
				if (!isEmpty(child)) {
					return false;
				}
			}
		}
		return true;
	}
}
