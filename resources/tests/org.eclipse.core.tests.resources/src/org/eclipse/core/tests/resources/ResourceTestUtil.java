/*******************************************************************************
 * Copyright (c) 2023 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package org.eclipse.core.tests.resources;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.internal.resources.Resource;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.tests.harness.FileSystemHelper;
import org.eclipse.core.tests.harness.FussyProgressMonitor;

/**
 * Utilities for resource tests.
 */
public final class ResourceTestUtil {
	private ResourceTestUtil() {
	}

	public static IProgressMonitor getMonitor() {
		return new FussyProgressMonitor();
	}

	/**
	 * Return an input stream with some random text to use as contents for a file
	 * resource.
	 */
	public static InputStream getRandomContents() {
		return new ByteArrayInputStream(getRandomString().getBytes());
	}

	/**
	 * Return String with some random text to use as contents for a file resource.
	 */
	public static String getRandomString() {
		switch ((int) Math.round(Math.random() * 10)) {
		case 0:
			return "este e' o meu conteudo (portuguese)";
		case 1:
			return "ho ho ho";
		case 2:
			return "I'll be back";
		case 3:
			return "don't worry, be happy";
		case 4:
			return "there is no imagination for more sentences";
		case 5:
			return "Alexandre Bilodeau, Canada's first home gold. 14/02/2010";
		case 6:
			return "foo";
		case 7:
			return "bar";
		case 8:
			return "foobar";
		case 9:
			return "case 9";
		default:
			return "these are my contents";
		}
	}

	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}

	/**
	 * Assert whether or not the given resource exists in the workspace resource
	 * info tree.
	 */
	public static void assertExistsInWorkspace(IResource resource) {
		assertExistsInWorkspace("", resource); //$NON-NLS-1$
	}

	/**
	 * Assert that each element of the resource array exists in the workspace
	 * resource info tree.
	 */
	public static void assertExistsInWorkspace(IResource[] resources) {
		assertExistsInWorkspace("", resources);
	}

	/**
	 * Assert whether or not the given resource exists in the workspace resource
	 * info tree.
	 */
	public static void assertExistsInWorkspace(String message, IResource resource) {
		String formatted = message == null || message.isEmpty() ? "" : message + " ";
		assertTrue(formatted + resource.getFullPath() + " unexpectedly does not exist in the workspace",
				existsInWorkspace(resource));
	}


	/**
	 * Assert that each element of the resource array exists in the workspace
	 * resource info tree.
	 */
	public static void assertExistsInWorkspace(String message, IResource[] resources) {
		for (IResource resource : resources) {
			assertExistsInWorkspace(message, resource);
		}
	}

	private static boolean existsInWorkspace(IResource resource) {
		class CheckIfResourceExistsJob extends Job {
			private final AtomicBoolean resourceExists = new AtomicBoolean(false);

			public CheckIfResourceExistsJob() {
				super("Checking whether resource exists: " + resource);
			}

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}

				IResource target = getWorkspace().getRoot().findMember(resource.getFullPath(), false);
				boolean existsInWorkspace = target != null && target.getType() == resource.getType();
				resourceExists.set(existsInWorkspace);

				return Status.OK_STATUS;
			}

			boolean resourceExists() {
				return resourceExists.get();
			}
		}

		IWorkspace workspace = getWorkspace();
		ISchedulingRule modifyWorkspaceRule = workspace.getRuleFactory().modifyRule(workspace.getRoot());

		CheckIfResourceExistsJob checkIfResourceExistsJob = new CheckIfResourceExistsJob();
		checkIfResourceExistsJob.setRule(modifyWorkspaceRule);
		checkIfResourceExistsJob.schedule();
		try {
			checkIfResourceExistsJob.join(30_000, getMonitor());
		} catch (OperationCanceledException | InterruptedException e) {
			throw new IllegalStateException("failed when joining resource-existence-checking job", e);
		}
		return checkIfResourceExistsJob.resourceExists();
	}

	/**
	 * Assert that the given resource does not exist in the workspace resource info
	 * tree.
	 */
	public static void assertDoesNotExistInWorkspace(IResource resource) {
		assertDoesNotExistInWorkspace("", resource); //$NON-NLS-1$
	}

	/**
	 * Assert that each element of the resource array does not exist in the
	 * workspace resource info tree.
	 */
	public static void assertDoesNotExistInWorkspace(IResource[] resources) {
		assertDoesNotExistInWorkspace("", resources); //$NON-NLS-1$
	}

	/**
	 * Assert that the given resource does not exist in the workspace resource info
	 * tree.
	 */
	public static void assertDoesNotExistInWorkspace(String message, IResource resource) {
		String formatted = message == null || message.isEmpty() ? "" : message + " ";
		assertFalse(formatted + resource.getFullPath() + " unexpectedly exists in the workspace",
				existsInWorkspace(resource));
	}

	/**
	 * Assert that each element of the resource array does not exist in the
	 * workspace resource info tree.
	 */
	public static void assertDoesNotExistInWorkspace(String message, IResource[] resources) {
		for (IResource resource : resources) {
			assertDoesNotExistInWorkspace(message, resource);
		}
	}

	/**
	 * Assert whether or not the given resource exists in the local store. Use the
	 * resource manager to ensure that we have a correct Path -&gt; File mapping.
	 */
	public static void assertExistsInFileSystem(IResource resource) {
		assertExistsInFileSystem("", resource); //$NON-NLS-1$
	}


	/**
	 * Assert that each element in the resource array exists in the local store.
	 */
	public static void assertExistsInFileSystem(IResource[] resources) {
		assertExistsInFileSystem("", resources); //$NON-NLS-1$
	}

	/**
	 * Assert whether or not the given resource exists in the local store. Use the
	 * resource manager to ensure that we have a correct Path -&gt; File mapping.
	 */
	public static void assertExistsInFileSystem(String message, IResource resource) {
		String formatted = message == null || message.isEmpty() ? "" : message + " ";
		assertTrue(formatted + resource.getFullPath() + " unexpectedly does not exist in the file system",
				existsInFileSystem(resource));
	}

	/**
	 * Assert that each element in the resource array exists in the local store.
	 */
	public static void assertExistsInFileSystem(String message, IResource[] resources) {
		for (IResource resource : resources) {
			assertExistsInFileSystem(message, resource);
		}
	}

	private static boolean existsInFileSystem(IResource resource) {
		IPath path = resource.getLocation();
		if (path == null) {
			path = computeDefaultLocation(resource);
		}
		return path.toFile().exists();
	}

	private static IPath computeDefaultLocation(IResource target) {
		switch (target.getType()) {
		case IResource.ROOT:
			return Platform.getLocation();
		case IResource.PROJECT:
			return Platform.getLocation().append(target.getFullPath());
		default:
			IPath location = computeDefaultLocation(target.getProject());
			location = location.append(target.getFullPath().removeFirstSegments(1));
			return location;
		}
	}

	/**
	 * Assert that the given resource does not exist in the local store.
	 */
	public static void assertDoesNotExistInFileSystem(IResource resource) {
		assertDoesNotExistInFileSystem("", resource); //$NON-NLS-1$
	}

	/**
	 * Assert that each element of the resource array does not exist in the
	 * local store.
	 */
	public static void assertDoesNotExistInFileSystem(IResource[] resources) {
		assertDoesNotExistInFileSystem("", resources); //$NON-NLS-1$
	}

	/**
	 * Assert that the given resource does not exist in the local store.
	 */
	public static void assertDoesNotExistInFileSystem(String message, IResource resource) {
		String formatted = message == null || message.isEmpty() ? "" : message + " ";
		assertFalse(formatted + resource.getFullPath() + " unexpectedly exists in the file system",
				existsInFileSystem(resource));
	}

	/**
	 * Assert that each element of the resource array does not exist in the local
	 * store.
	 */
	public static void assertDoesNotExistInFileSystem(String message, IResource[] resources) {
		for (IResource resource : resources) {
			assertDoesNotExistInFileSystem(message, resource);
		}
	}

	public static void create(final IResource resource, boolean local) throws CoreException {
		if (resource == null || resource.exists()) {
			return;
		}
		if (!resource.getParent().exists()) {
			create(resource.getParent(), local);
		}
		switch (resource.getType()) {
		case IResource.FILE:
			((IFile) resource).create(local ? new ByteArrayInputStream(new byte[0]) : null, true, getMonitor());
			break;
		case IResource.FOLDER:
			((IFolder) resource).create(true, local, getMonitor());
			break;
		case IResource.PROJECT:
			((IProject) resource).create(getMonitor());
			((IProject) resource).open(getMonitor());
			break;
		}
	}

	/**
	 * Create the given file in the file system.
	 */
	public static void createFile(File file, InputStream contents) throws IOException {
		file.getParentFile().mkdirs();
		try (FileOutputStream output = new FileOutputStream(file)) {
			transferData(contents, output);
		}
	}

	/**
	 * Create the given file in the local store.
	 */
	public static void createFile(IFileStore file) throws CoreException {
		createFile(file, getRandomContents());
	}

	/**
	 * Create the given file in the local store.
	 */
	public static void createFile(IFileStore file, InputStream contents) throws CoreException {
		file.getParent().mkdir(EFS.NONE, null);
		try (OutputStream output = file.openOutputStream(EFS.NONE, null)) {
			transferData(contents, output);
		} catch (IOException e) {
			throw new IllegalStateException("failed creating file in file system", e);
		}
	}

	/**
	 * Copy the data from the input stream to the output stream. Close both streams
	 * when finished.
	 */
	private static void transferData(InputStream input, OutputStream output) throws IOException {
		try (input; output) {
			int c = 0;
			while ((c = input.read()) != -1) {
				output.write(c);
			}
		}
	}

	/**
	 * Create the given file in the file system.
	 */
	public static void createFile(IPath path) throws CoreException {
		createFile(path, getRandomContents());
	}

	/**
	 * Create the given file in the file system.
	 */
	public static void createFile(IPath path, InputStream contents) throws CoreException {
		try {
			createFile(path.toFile(), contents);
		} catch (IOException e) {
			throw new IllegalStateException("failed creating file in file system", e);
		}
	}

	/**
	 * Create the given file in the workspace resource info tree.
	 */
	public static void ensureExistsInWorkspace(final IFile resource, final InputStream contents) throws CoreException {
		if (resource == null) {
			return;
		}
		IWorkspaceRunnable body;
		if (resource.exists()) {
			body = monitor -> resource.setContents(contents, true, false, null);
		} else {
			body = monitor -> {
				create(resource.getParent(), true);
				resource.create(contents, true, null);
			};
		}
		getWorkspace().run(body, null);
	}

	/**
	 * Create the given file in the workspace resource info tree.
	 */
	public static void ensureExistsInWorkspace(IFile resource, String contents) throws CoreException {
		ensureExistsInWorkspace(resource, new ByteArrayInputStream(contents.getBytes()));
	}

	/**
	 * Create the given resource in the workspace resource info tree.
	 */
	public static void ensureExistsInWorkspace(final IResource resource, final boolean local) throws CoreException {
		IWorkspaceRunnable body = monitor -> create(resource, local);
		getWorkspace().run(body, null);
	}

	/**
	 * Create each element of the resource array in the workspace resource info
	 * tree.
	 */
	public static void ensureExistsInWorkspace(final IResource[] resources, final boolean local) throws CoreException {
		IWorkspaceRunnable body = monitor -> {
			for (IResource resource : resources) {
				create(resource, local);
			}
		};
		getWorkspace().run(body, null);
	}

	/**
	 * Delete the given resource from the workspace resource tree.
	 */
	public static void removeFromWorkspace(IResource resource) throws CoreException {
		if (resource.exists()) {
			resource.delete(IResource.FORCE | IResource.ALWAYS_DELETE_PROJECT_CONTENT, getMonitor());
		}
	}

	/**
	 * Delete each element of the resource array from the workspace resource info
	 * tree.
	 */
	public static void removeFromWorkspace(final IResource[] resources) throws CoreException {
		IWorkspaceRunnable body = monitor -> {
			for (IResource resource : resources) {
				removeFromWorkspace(resource);
			}
		};
		ResourcesPlugin.getWorkspace().run(body, null);
	}

	/**
	 * Create the given folder in the local store. Use the resource
	 * manager to ensure that we have a correct Path -&gt; File mapping.
	 */
	public static void ensureExistsInFileSystem(IResource resource) throws CoreException {
		if (resource instanceof IFile file) {
			ensureExistsInFileSystem(file);
		} else {
			((Resource) resource).getStore().mkdir(EFS.NONE, null);
		}
	}

	/**
	 * Create the given file in the local store. Use the resource manager
	 * to ensure that we have a correct Path -&gt; File mapping.
	 */
	public static void ensureExistsInFileSystem(IFile file) throws CoreException {
		createFile(((Resource) file).getStore());
	}

	/**
	 * Create the each resource of the array in the local store.
	 */
	public static void ensureExistsInFileSystem(IResource[] resources) throws CoreException {
		for (IResource resource : resources) {
			ensureExistsInFileSystem(resource);
		}
	}

	public static void removeFromFileSystem(File file) {
		FileSystemHelper.clear(file);
	}

	/**
	 * Delete the given resource from the local store. Use the resource manager to
	 * ensure that we have a correct Path -&gt; File mapping.
	 */
	public static void removeFromFileSystem(IResource resource) {
		IPath path = resource.getLocation();
		if (path != null) {
			removeFromFileSystem(path.toFile());
		}
	}

	/**
	 * Delete the resources in the array from the local store.
	 */
	public static void removeFromFileSystem(IResource[] resources) {
		for (IResource resource : resources) {
			removeFromFileSystem(resource);
		}
	}

	/**
	 * Convenience method to copy contents from one stream to another.
	 */
	public static void transferStreams(InputStream source, OutputStream destination, String path) throws IOException {
		try (source; destination) {
			source.transferTo(destination);
		}
	}

}
