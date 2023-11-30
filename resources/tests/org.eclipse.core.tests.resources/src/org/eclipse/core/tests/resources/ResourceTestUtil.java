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

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.concurrent.atomic.AtomicBoolean;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.internal.resources.CharsetDeltaJob;
import org.eclipse.core.internal.resources.ValidateProjectEncoding;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.internal.utils.UniversalUniqueIdentifier;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.tests.harness.FussyProgressMonitor;

/**
 * Utilities for resource tests.
 */
public final class ResourceTestUtil {
	private ResourceTestUtil() {
	}

	public static IProgressMonitor createTestMonitor() {
		return new FussyProgressMonitor();
	}

	public static String createUniqueString() {
		return new UniversalUniqueIdentifier().toString();
	}

	/**
	 * Assert whether or not the given resource exists in the workspace resource
	 * info tree.
	 */
	public static void assertExistsInWorkspace(IResource resource) {
		assertTrue(resource.getFullPath() + " unexpectedly does not exist in the workspace",
				existsInWorkspace(resource));
	}

	/**
	 * Assert that each element of the resource array exists in the workspace
	 * resource info tree.
	 */
	public static void assertExistsInWorkspace(IResource[] resources) {
		for (IResource resource : resources) {
			assertExistsInWorkspace(resource);
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
			checkIfResourceExistsJob.join(30_000, new NullProgressMonitor());
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
		assertFalse(resource.getFullPath() + " unexpectedly exists in the workspace", existsInWorkspace(resource));
	}

	/**
	 * Assert that each element of the resource array does not exist in the
	 * workspace resource info tree.
	 */
	public static void assertDoesNotExistInWorkspace(IResource[] resources) {
		for (IResource resource : resources) {
			assertDoesNotExistInWorkspace(resource);
		}
	}

	/**
	 * Assert whether or not the given resource exists in the local store. Use the
	 * resource manager to ensure that we have a correct Path -&gt; File mapping.
	 */
	public static void assertExistsInFileSystem(IResource resource) {
		assertTrue(resource.getFullPath() + " unexpectedly does not exist in the file system",
				existsInFileSystem(resource));
	}


	/**
	 * Assert that each element in the resource array exists in the local store.
	 */
	public static void assertExistsInFileSystem(IResource[] resources) {
		for (IResource resource : resources) {
			assertExistsInFileSystem(resource);
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
		assertFalse(resource.getFullPath() + " unexpectedly exists in the file system", existsInFileSystem(resource));
	}

	/**
	 * Assert that each element of the resource array does not exist in the
	 * local store.
	 */
	public static void assertDoesNotExistInFileSystem(IResource[] resources) {
		for (IResource resource : resources) {
			assertDoesNotExistInFileSystem(resource);
		}
	}

	/**
	 * Blocks the calling thread until autobuild completes.
	 */
	public static void waitForBuild() {
		((Workspace) getWorkspace()).getBuildManager().waitForAutoBuild();
	}

	/**
	 * Blocks the calling thread until refresh job completes.
	 */
	public static void waitForRefresh() {
		try {
			Job.getJobManager().wakeUp(ResourcesPlugin.FAMILY_AUTO_REFRESH);
			Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_REFRESH, null);
		} catch (OperationCanceledException | InterruptedException e) {
			//ignore
		}
	}

	/**
	 * Waits for at most 5 seconds for encoding-related jobs (project encoding
	 * validation and charset delta) to finish.
	 */
	public static void waitForEncodingRelatedJobs(String testName) {
		TestUtil.waitForJobs(testName, 10, 5_000, ValidateProjectEncoding.class);
		TestUtil.waitForJobs(testName, 10, 5_000, CharsetDeltaJob.FAMILY_CHARSET_DELTA);
	}

	/**
	 * Checks whether the local file system supports accessing and modifying
	 * the given attribute.
	 */
	public static boolean isAttributeSupported(int attribute) {
		return (EFS.getLocalFileSystem().attributes() & attribute) != 0;
	}

	/**
	 * Checks whether the local file system supports accessing and modifying
	 * the read-only flag.
	 */
	public static boolean isReadOnlySupported() {
		return isAttributeSupported(EFS.ATTRIBUTE_READ_ONLY);
	}

	/**
	 * Sets the read-only state of the given file store to {@code value}.
	 */
	public static void setReadOnly(IFileStore target, boolean value) throws CoreException {
		assertThat("Setting read only is not supported by local file system", isReadOnlySupported());
		IFileInfo fileInfo = target.fetchInfo();
		fileInfo.setAttribute(EFS.ATTRIBUTE_READ_ONLY, value);
		target.putInfo(fileInfo, EFS.SET_ATTRIBUTES, null);
	}

	/**
	 * Sets the read-only state of the given resource to {@code value}.
	 */
	public static void setReadOnly(IResource target, boolean value) throws CoreException {
		ResourceAttributes attributes = target.getResourceAttributes();
		assertNotNull("tried to set read only for null attributes", attributes);
		attributes.setReadOnly(value);
		target.setResourceAttributes(attributes);
	}

	/**
	 * Return a collection of resources for the given hierarchy at
	 * the given root.
	 */
	public static IResource[] buildResources(IContainer root, String[] hierarchy) throws CoreException {
		IResource[] result = new IResource[hierarchy.length];
		for (int i = 0; i < hierarchy.length; i++) {
			IPath path = IPath.fromOSString(hierarchy[i]);
			IPath fullPath = root.getFullPath().append(path);
			switch (fullPath.segmentCount()) {
				case 0 :
					result[i] = getWorkspace().getRoot();
					break;
				case 1 :
					result[i] = getWorkspace().getRoot().getProject(fullPath.segment(0));
					break;
				default :
					if (hierarchy[i].charAt(hierarchy[i].length() - 1) == IPath.SEPARATOR) {
						result[i] = root.getFolder(path);
					} else {
						result[i] = root.getFile(path);
					}
					break;
			}
		}
		return result;
	}

	/*
	 * Modifies the passed in IFile in the file system so that it is out of sync
	 * with the workspace.
	 */
	public static void ensureOutOfSync(final IFile file) throws CoreException, IOException {
		modifyInFileSystem(file);
		waitForRefresh();
		touchInFilesystem(file);
		assertThat("file not out of sync: " + file.getLocation().toOSString(), file.getLocalTimeStamp(),
				not(is(file.getLocation().toFile().lastModified())));
	}

	private static void modifyInFileSystem(IFile file) throws FileNotFoundException, IOException {
		String originalContent = readStringInFileSystem(file);
		String newContent = originalContent + "f";
		try (FileOutputStream outputStream = new FileOutputStream(file.getLocation().toFile())) {
			outputStream.write(newContent.getBytes("UTF8"));
		}
	}

	/**
	 * Returns the content of the given file in the file system as a String (UTF8).
	 */
	public static String readStringInFileSystem(IFile file) throws IOException {
		IPath location = file.getLocation();
		assertNotNull("location was null for file: " + file, location);
		try (FileInputStream inputStream = new FileInputStream(location.toFile())) {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			inputStream.transferTo(outputStream);
			return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
		}
	}

	/**
	 * Touch (but don't modify) the resource in the filesystem so that it's
	 * modification stamp is newer than the cached value in the Workspace.
	 */
	public static void touchInFilesystem(IResource resource) throws CoreException, IOException {
		IPath location = resource.getLocation();
		// Manually check that the core.resource time-stamp is out-of-sync
		// with the java.io.File last modified. #isSynchronized() will schedule
		// out-of-sync resources for refresh, so we don't use that here.
		for (int count = 0; count < 3000 && isInSync(resource); count++) {
			FileTime now = FileTime.fromMillis(resource.getLocalTimeStamp() + 1000);
			Files.setLastModifiedTime(location.toFile().toPath(), now);
			if (count > 1) {
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					// ignore
				}
			}
		}
		assertThat("File not out of sync: " + location.toOSString(), resource.getLocalTimeStamp(),
				not(is(getLastModifiedTime(location))));
	}

	private static boolean isInSync(IResource resource) {
		IPath location = resource.getLocation();
		long localTimeStamp = resource.getLocalTimeStamp();
		return getLastModifiedTime(location) == localTimeStamp || location.toFile().lastModified() == localTimeStamp;
	}

	private static long getLastModifiedTime(IPath fileLocation) {
		IFileInfo fileInfo = EFS.getLocalFileSystem().getStore(fileLocation).fetchInfo();
		return fileInfo.getLastModified();
	}

}
