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
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *******************************************************************************/
package org.eclipse.core.tests.resources;

import static java.io.InputStream.nullInputStream;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createUniqueString;
import static org.eclipse.core.tests.resources.ResourceTestUtil.waitForBuild;
import static org.eclipse.core.tests.resources.ResourceTestUtil.waitForRefresh;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertArrayEquals;

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
import java.util.HashSet;
import java.util.Set;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.internal.resources.Resource;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.tests.harness.CoreTest;
import org.eclipse.core.tests.harness.FileSystemHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

/**
 * Superclass for tests that use the Eclipse Platform workspace.
 */
public abstract class ResourceTest extends CoreTest {

	/**
	 * For retrieving the test name when executing test class with JUnit 4.
	 */
	@Rule
	public final TestName testName = new TestName();

	/**
	 * Set of FileStore instances that must be deleted when the
	 * test is complete
	 * @see #getTempStore
	 */
	private final Set<IFileStore> storesToDelete = new HashSet<>();

	private IWorkspaceDescription storedWorkspaceDescription;

	private final void storeWorkspaceDescription() {
		this.storedWorkspaceDescription = getWorkspace().getDescription();
	}

	private final void restoreWorkspaceDescription() throws CoreException {
		if (storedWorkspaceDescription != null) {
			getWorkspace().setDescription(storedWorkspaceDescription);
		}
		storedWorkspaceDescription = null;
	}

	/**
	 * Need a zero argument constructor to satisfy the test harness.
	 * This constructor should not do any real work nor should it be
	 * called by user code.
	 */
	public ResourceTest() {
		super(null);
	}

	/**
	 * Creates a new ResourceTest
	 *
	 * @param name
	 *            name of the TestCase
	 */
	public ResourceTest(String name) {
		super(name);
	}

	@Override
	public String getName() {
		String name = super.getName();
		// Ensure that in case this test class is executed with JUnit 4 the test name
		// will not be null but retrieved via a TestName rule.
		if (name == null) {
			name = testName.getMethodName();
		}
		return name;
	}

	/**
	 * Bridge method to be able to run subclasses with JUnit4 as well as with
	 * JUnit3.
	 *
	 * @throws Exception
	 *             comes from {@link #setUp()}
	 */
	@Before
	public final void before() throws Exception {
		setUp();
	}

	/**
	 * Bridge method to be able to run subclasses with JUnit4 as well as with
	 * JUnit3.
	 *
	 * @throws Exception
	 *             comes from {@link #tearDown()}
	 */
	@After
	public final void after() throws Exception {
		tearDown();
	}

	/**
	 * Return a collection of resources for the given hierarchy at
	 * the given root.
	 */
	public IResource[] buildResources(IContainer root, String[] hierarchy) throws CoreException {
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

	private void cleanup() throws CoreException {
		// Wait for any build job that may still be executed
		waitForBuild();
		final IFileStore[] toDelete = storesToDelete.toArray(new IFileStore[0]);
		storesToDelete.clear();
		getWorkspace().run((IWorkspaceRunnable) monitor -> {
			getWorkspace().getRoot().delete(true, true, createTestMonitor());
			//clear stores in workspace runnable to avoid interaction with resource jobs
			for (IFileStore store : toDelete) {
				store.delete(EFS.NONE, null);
			}
		}, null);
		getWorkspace().save(true, null);
		// don't leak builder jobs, since they may affect subsequent tests
		waitForBuild();
		assertWorkspaceFolderEmpty();
	}

	private void assertWorkspaceFolderEmpty() {
		final String metadataDirectoryName = ".metadata";
		File workspaceLocation = getWorkspace().getRoot().getLocation().toFile();
		File[] remainingFilesInWorkspace = workspaceLocation
				.listFiles(file -> !file.getName().equals(metadataDirectoryName));
		assertArrayEquals("There are unexpected contents in the workspace folder", new File[0],
				remainingFilesInWorkspace);
	}

	/**
	 * Returns a boolean value indicating whether or not the contents
	 * of the given streams are considered to be equal. Closes both input streams.
	 */
	public boolean compareContent(InputStream a, InputStream b) {
		int c, d;
		if (a == null && b == null) {
			return true;
		}
		try {
			if (a == null || b == null) {
				return false;
			}
			while ((c = a.read()) == (d = b.read()) && (c != -1 && d != -1)) {
				//body not needed
			}
			return (c == -1 && d == -1);
		} catch (IOException e) {
			return false;
		} finally {
			assertClose(a);
			assertClose(b);
		}
	}

	/**
	 * Create the given file and its parents in the local store with random
	 * contents.
	 */
	public void createFileInFileSystem(IFileStore file) throws CoreException, IOException {
		file.getParent().mkdir(EFS.NONE, null);
		try (InputStream input = getRandomContents(); OutputStream output = file.openOutputStream(EFS.NONE, null)) {
			input.transferTo(output);
		}
	}

	/**
	 * Create the given file and its parents in the file system with random
	 * contents.
	 */
	public void createFileInFileSystem(IPath path) throws CoreException, IOException {
		path.toFile().getParentFile().mkdirs();
		try (InputStream input = getRandomContents(); OutputStream output = new FileOutputStream(path.toFile())) {
			input.transferTo(output);
		}
	}

	/**
	 * Create the given file or folder in the local store. Use the resource manager
	 * to ensure that we have a correct Path -&gt; File mapping.
	 */
	public void createInFileSystem(IResource resource) throws CoreException, IOException {
		if (resource instanceof IFile file) {
			createFileInFileSystem(((Resource) file).getStore());
		} else {
			((Resource) resource).getStore().mkdir(EFS.NONE, null);
		}
	}

	/**
	 * Delete the given resource from the local store. Use the resource manager to
	 * ensure that we have a correct Path -&gt; File mapping.
	 */
	public void ensureDoesNotExistInFileSystem(IResource resource) {
		IPath path = resource.getLocation();
		if (path != null) {
			ensureDoesNotExistInFileSystem(path.toFile());
		}
	}

	/**
	 * Delete the given resource from the workspace resource tree.
	 */
	public void ensureDoesNotExistInWorkspace(IResource resource) throws CoreException {
		if (resource.exists()) {
			resource.delete(IResource.FORCE | IResource.ALWAYS_DELETE_PROJECT_CONTENT, createTestMonitor());
		}
	}

	/**
	 * Delete each element of the resource array from the workspace
	 * resource info tree.
	 */
	public void ensureDoesNotExistInWorkspace(final IResource[] resources) throws CoreException {
		IWorkspaceRunnable body = monitor -> {
			for (IResource resource : resources) {
				ensureDoesNotExistInWorkspace(resource);
			}
		};
		getWorkspace().run(body, null);
	}

	/**
	 * Create the given file in the workspace resource info tree.
	 */
	public void createInWorkspace(IFile resource, String contents) throws CoreException {
		InputStream contentStream = getContents(contents);
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
	public void createInWorkspace(final IResource resource) throws CoreException {
		IWorkspaceRunnable body = monitor -> createInWorkspace(resource, monitor);
		getWorkspace().run(body, createTestMonitor());
	}

	/**
	 * Create each element of the resource array and all their parents in the
	 * workspace resource info tree.
	 */
	public void createInWorkspace(final IResource[] resources) throws CoreException {
		IWorkspaceRunnable body = monitor -> {
			for (IResource resource : resources) {
				createInWorkspace(resource, monitor);
			}
		};
		getWorkspace().run(body, createTestMonitor());
	}

	private void createInWorkspace(final IResource resource, IProgressMonitor monitor) throws CoreException {
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
	 * Modifies the passed in IFile in the file system so that it is out of sync
	 * with the workspace.
	 */
	public void ensureOutOfSync(final IFile file) throws CoreException, IOException {
		modifyInFileSystem(file);
		waitForRefresh();
		touchInFilesystem(file);
		assertThat("file not out of sync: " + file.getLocation().toOSString(), file.getLocalTimeStamp(),
				not(is(file.getLocation().toFile().lastModified())));
	}

	private void modifyInFileSystem(IFile file) throws FileNotFoundException, IOException {
		String originalContent = readStringInFileSystem(file);
		String newContent = originalContent + "f";
		try (FileOutputStream outputStream = new FileOutputStream(file.getLocation().toFile())) {
			outputStream.write(newContent.getBytes("UTF8"));
		}
	}

	/**
	 * Returns the content of the given file in the file system as a String (UTF8).
	 *
	 * @param file
	 *            file system file to read
	 */
	protected String readStringInFileSystem(IFile file) throws IOException {
		IPath location = file.getLocation();
		assertNotNull("location was null for file: " + file, location);
		try (FileInputStream inputStream = new FileInputStream(location.toFile())) {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			inputStream.transferTo(outputStream);
			return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
		}
	}

	/**
	 * Touch (but don't modify) the resource in the filesystem so that it's modification stamp is newer than
	 * the cached value in the Workspace.
	 */
	public void touchInFilesystem(IResource resource) throws CoreException, IOException {
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

	private boolean isInSync(IResource resource) {
		IPath location = resource.getLocation();
		long localTimeStamp = resource.getLocalTimeStamp();
		return getLastModifiedTime(location) == localTimeStamp || location.toFile().lastModified() == localTimeStamp;
	}

	private long getLastModifiedTime(IPath fileLocation) {
		IFileInfo fileInfo = EFS.getLocalFileSystem().getStore(fileLocation).fetchInfo();
		return fileInfo.getLastModified();
	}

	protected String getLineSeparatorFromFile(IFile file) {
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
	 * Returns a FileStore instance backed by storage in a temporary location.
	 * The returned store will not exist, but will belong to an existing parent.
	 * The tearDown method in this class will ensure the location is deleted after
	 * the test is completed.
	 */
	protected IFileStore getTempStore() {
		IFileStore store = EFS.getLocalFileSystem().getStore(FileSystemHelper.getRandomLocation(getTempDir()));
		deleteOnTearDown(store);
		return store;
	}

	/**
	 * Ensures that the file system location associated with the corresponding path
	 * is deleted during test tear down.
	 */
	protected void deleteOnTearDown(IPath path) {
		storesToDelete.add(EFS.getLocalFileSystem().getStore(path));
	}

	/**
	 * Ensures that the given store is deleted during test tear down.
	 */
	protected void deleteOnTearDown(IFileStore store) {
		storesToDelete.add(store);

	}

	/**
	 * The environment should be set-up in the main method.
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		// Wait for any pending refresh operation, in particular from startup
		waitForRefresh();
		TestUtil.log(IStatus.INFO, getName(), "setUp");
		FreezeMonitor.expectCompletionInAMinute();
		assertNotNull("Workspace was not setup", getWorkspace());
		storeWorkspaceDescription();
	}

	@Override
	protected void tearDown() throws Exception {
		boolean wasSuspended = resumeJobManagerIfNecessary();
		TestUtil.log(IStatus.INFO, getName(), "tearDown");
		// Ensure everything is in a clean state for next one.
		// Session tests should overwrite it.
		restoreWorkspaceDescription();
		cleanup();
		super.tearDown();
		FreezeMonitor.done();
		assertFalse("This test stopped the JobManager, which could have affected other tests.", //
				wasSuspended);
	}

	private boolean resumeJobManagerIfNecessary() {
		if (Job.getJobManager().isSuspended()) {
			Job.getJobManager().resume();
			return true;
		}

		return false;
	}

	/**
	 * Enables or disables workspace autobuild. Waits for the build to be finished,
	 * even if the autobuild value did not change and a previous build is still running.
	 */
	protected void setAutoBuilding(boolean enabled) throws CoreException {
		IWorkspace workspace = getWorkspace();
		if (workspace.isAutoBuilding() != enabled) {
			IWorkspaceDescription description = workspace.getDescription();
			description.setAutoBuilding(enabled);
			workspace.setDescription(description);
		}
		waitForBuild();
	}

	public String[] findAvailableDevices() {
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
