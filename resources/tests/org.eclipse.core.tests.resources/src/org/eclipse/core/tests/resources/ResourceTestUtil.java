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

import static java.io.InputStream.nullInputStream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.internal.resources.CharsetDeltaJob;
import org.eclipse.core.internal.resources.Resource;
import org.eclipse.core.internal.resources.ValidateProjectEncoding;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.internal.utils.UniversalUniqueIdentifier;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRunnable;
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
import org.eclipse.core.tests.harness.FileSystemHelper;
import org.eclipse.core.tests.harness.FussyProgressMonitor;
import org.eclipse.core.tests.internal.builders.TestBuilder;

/**
 * Utilities for resource tests.
 */
public final class ResourceTestUtil {
	private static final Random RANDOM = new Random();

	private ResourceTestUtil() {
	}

	public static IProgressMonitor createTestMonitor() {
		return new FussyProgressMonitor();
	}

	public static String createUniqueString() {
		return new UniversalUniqueIdentifier().toString();
	}

	/**
	 * Return an input stream with some the specified text to use as contents for a
	 * file resource.
	 */
	public static InputStream createInputStream(String text) {
		return new ByteArrayInputStream(text.getBytes());
	}

	/**
	 * Return String with some random text to use as contents for a file resource.
	 */
	public static String createRandomString() {
		return RANDOM.nextLong() + " " + RANDOM.nextLong();
	}

	/**
	 * Return an input stream with some random text to use as contents for a file
	 * resource.
	 */
	public static InputStream createRandomContentsStream() {
		return createInputStream(createRandomString());
	}

	/**
	 * Create the given file and its parents in the local store with random
	 * contents.
	 */
	public static void createInFileSystem(IFileStore file) throws CoreException, IOException {
		file.getParent().mkdir(EFS.NONE, null);
		try (InputStream input = createRandomContentsStream();
				OutputStream output = file.openOutputStream(EFS.NONE, null)) {
			input.transferTo(output);
		}
	}

	/**
	 * Create a file and its parents at the given path in the file system with
	 * random contents.
	 */
	public static void createInFileSystem(IPath path) throws CoreException, IOException {
		path.toFile().getParentFile().mkdirs();
		try (InputStream input = createRandomContentsStream();
				OutputStream output = new FileOutputStream(path.toFile())) {
			input.transferTo(output);
		}
	}

	/**
	 * Create the given file or folder in the local store. Use the resource manager
	 * to ensure that we have a correct Path -&gt; File mapping.
	 */
	public static void createInFileSystem(IResource resource) throws CoreException, IOException {
		if (resource instanceof IFile file) {
			createInFileSystem(((Resource) file).getStore());
		} else {
			((Resource) resource).getStore().mkdir(EFS.NONE, null);
		}
	}

	/**
	 * Create the given file in the workspace resource info tree.
	 */
	public static void createInWorkspace(IFile resource, String contents) throws CoreException {
		InputStream contentStream = createInputStream(contents);
		if (resource == null) {
			return;
		}
		IWorkspaceRunnable body;
		if (resource.exists()) {
			body = monitor -> resource.setContents(contentStream, true, false, null);
		} else {
			body = monitor -> {
				createInWorkspace(resource.getParent(), monitor);
				resource.create(contentStream, true, null);
			};
		}
		getWorkspace().run(body, createTestMonitor());
	}

	/**
	 * Create the given resource and all its parents in the workspace resource info
	 * tree.
	 */
	public static void createInWorkspace(final IResource resource) throws CoreException {
		IWorkspaceRunnable body = monitor -> createInWorkspace(resource, monitor);
		getWorkspace().run(body, createTestMonitor());
	}

	/**
	 * Create each element of the resource array and all their parents in the
	 * workspace resource info tree.
	 */
	public static void createInWorkspace(final IResource[] resources) throws CoreException {
		IWorkspaceRunnable body = monitor -> {
			for (IResource resource : resources) {
				createInWorkspace(resource, monitor);
			}
		};
		getWorkspace().run(body, createTestMonitor());
	}

	private static void createInWorkspace(final IResource resource, IProgressMonitor monitor) throws CoreException {
		if (resource == null || resource.exists()) {
			return;
		}
		if (!resource.getParent().exists()) {
			createInWorkspace(resource.getParent(), monitor);
		}
		switch (resource.getType()) {
		case IResource.FILE:
			((IFile) resource).create(nullInputStream(), true, monitor);
			break;
		case IResource.FOLDER:
			((IFolder) resource).create(true, true, monitor);
			break;
		case IResource.PROJECT:
			((IProject) resource).create(monitor);
			((IProject) resource).open(monitor);
			break;
		}
	}

	/**
	 * Delete the given file in the file system.
	 */
	public static void removeFromFileSystem(java.io.File file) {
		FileSystemHelper.clear(file);
	}

	/**
	 * Delete the given resource in the file system.
	 */
	public static void removeFromFileSystem(IResource resource) {
		IPath path = resource.getLocation();
		if (path != null) {
			removeFromFileSystem(path.toFile());
		}
	}

	/**
	 * Delete the given resource in the workspace resource tree. Also removes
	 * project contents in case the resource is a project and the project is
	 * currently closed.
	 */
	public static void removeFromWorkspace(IResource resource) throws CoreException {
		if (resource.exists()) {
			resource.delete(IResource.FORCE | IResource.ALWAYS_DELETE_PROJECT_CONTENT, createTestMonitor());
		}
	}

	/**
	 * Delete each element of the resource array in the workspace resource info
	 * tree. Also removes project contents in case a resource is a project and the
	 * project is currently closed.
	 */
	public static void removeFromWorkspace(final IResource[] resources) throws CoreException {
		IWorkspaceRunnable body = monitor -> {
			for (IResource resource : resources) {
				removeFromWorkspace(resource);
			}
		};
		getWorkspace().run(body, null);
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
		assertThat(isReadOnlySupported()).withFailMessage("setting read only is not supported by local file system")
				.isTrue();
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
		assertThat(resource.getLocalTimeStamp()).as("file not out of sync: %s", location.toOSString())
				.isNotEqualTo(getLastModifiedTime(location));
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

	/**
	 * Returns a boolean value indicating whether or not the contents
	 * of the given streams are considered to be equal. Closes both input streams.
	 */
	public static boolean compareContent(InputStream a, InputStream b) throws IOException {
		int c, d;
		if (a == null && b == null) {
			return true;
		}
		try (a; b) {
			if (a == null || b == null) {
				return false;
			}
			while ((c = a.read()) == (d = b.read()) && (c != -1 && d != -1)) {
				// body not needed
			}
		}
		return (c == -1 && d == -1);
	}

	/**
	 * Enables or disables workspace autobuild. Waits for the build to be finished,
	 * even if the autobuild value did not change and a previous build is still
	 * running.
	 */
	public static void setAutoBuilding(boolean enabled) throws CoreException {
		IWorkspace workspace = getWorkspace();
		if (workspace.isAutoBuilding() != enabled) {
			IWorkspaceDescription description = workspace.getDescription();
			description.setAutoBuilding(enabled);
			workspace.setDescription(description);
		}
		waitForBuild();
	}

	/**
	 * Sets the workspace build order to just contain the given projects. With
	 * {@code projects} is null, the default build order of the workspace will be
	 * used.
	 */
	public static void setBuildOrder(IProject... projects) throws CoreException {
		IWorkspace workspace = getWorkspace();
		IWorkspaceDescription desc = workspace.getDescription();
		if (projects == null) {
			desc.setBuildOrder(null);
		} else {
			desc.setBuildOrder(Stream.of(projects).map(IProject::getName).toArray(String[]::new));
		}
		workspace.setDescription(desc);
	}

	/**
	 * Creates a builder to update the project description with new commands. After
	 * finishing the builder, @{link {@link ProjectDescriptionUpdater#apply()} has
	 * to be called.
	 */
	public static ProjectDescriptionUpdater updateProjectDescription(IProject project) throws CoreException {
		return new ProjectDescriptionUpdater(project);
	}

	public static class ProjectDescriptionUpdater {
		private IProject project;

		private IProjectDescription description;

		private List<ICommand> commands = new ArrayList<>();

		private ProjectDescriptionUpdater(IProject project) throws CoreException {
			this.project = project;
			this.description = project.getDescription();
		}

		/**
		 * Applies the project description update.
		 */
		public void apply() throws CoreException {
			description.setBuildSpec(commands.toArray(ICommand[]::new));
			project.setDescription(description, createTestMonitor());
		}

		/**
		 * Removes all existing commands.
		 */
		public ProjectDescriptionUpdater removingExistingCommands() {
			commands.clear();
			return this;
		}

		/**
		 * Adds the given command to the project description.
		 */
		public CommandBuilder addingCommand(ICommand command) {
			commands.add(command);
			return new CommandBuilder(command);
		}

		/**
		 * Adds a command with the given builder name.
		 */
		public CommandBuilder addingCommand(String builderName) {
			ICommand command = description.newCommand();
			command.setBuilderName(builderName);
			commands.add(command);
			return new CommandBuilder(command);
		}

		public class CommandBuilder {
			private final ICommand command;

			private CommandBuilder(ICommand command) {
				this.command = command;
			}

			/**
			 * Adds the given TestBuilder.BUILD_ID to the command.
			 */
			public CommandBuilder withTestBuilderId(String id) {
				return withAdditionalBuildArgument(TestBuilder.BUILD_ID, id);
			}

			/**
			 * Activates or deactivates the given build setting.
			 */
			public CommandBuilder withBuildingSetting(int kind, boolean value) {
				command.setBuilding(kind, value);
				return this;
			}

			/**
			 * Adds the given argument to the command.
			 */
			public CommandBuilder withAdditionalBuildArgument(String key, String value) {
				Map<String, String> args = command.getArguments();
				args.put(key, value);
				command.setArguments(args);
				return this;
			}

			/**
			 * Finalizes the current command and adds another command with the given builder
			 * name.
			 */
			public CommandBuilder andCommand(String builderName) {
				return ProjectDescriptionUpdater.this.addingCommand(builderName);
			}

			/**
			 * Applies the project description update.
			 */
			public void apply() throws CoreException {
				ProjectDescriptionUpdater.this.apply();
			}
		}

	}

	/**
	 * Returns the character sequence used as a line separator within the given
	 * file.
	 */
	public static String getLineSeparatorFromFile(IFile file) {
		if (file.exists()) {
			InputStream input = null;
			try {
				input = file.getContents();
				int c = input.read();
				while (c != -1 && c != '\r' && c != '\n') {
					c = input.read();
				}
				if (c == '\n')
				 {
					return "\n"; //$NON-NLS-1$
				}
				if (c == '\r') {
					if (input.read() == '\n')
					 {
						return "\r\n"; //$NON-NLS-1$
					}
					return "\r"; //$NON-NLS-1$
				}
			} catch (CoreException | IOException e) {
				// ignore
			} finally {
				try {
					input.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
		return null;
	}

	/**
	 * Returns a two-element array containing two available devices. In case there
	 * are fewer devices available, the other array entries will be null. Note that
	 * this method only works on Windows.
	 */
	public static String[] findAvailableDevices() {
		String[] devices = new String[2];
		for (int i = 97/*a*/; i < 123/*z*/; i++) {
			char c = (char) i;
			java.io.File rootFile = new java.io.File(c + ":\\");
			if (rootFile.exists() && rootFile.canWrite()) {
				//sometimes canWrite can return true but we are still not allowed to create a file - see bug 379284.
				File probe = new File(rootFile, createUniqueString());
				try {
					probe.createNewFile();
				} catch (IOException e) {
					//can't create a file here.. try another device
					continue;
				} finally {
					probe.delete();
				}
				if (devices[0] == null) {
					devices[0] = c + ":/";
				} else {
					devices[1] = c + ":/";
					break;
				}
			}
		}
		return devices;
	}

}
