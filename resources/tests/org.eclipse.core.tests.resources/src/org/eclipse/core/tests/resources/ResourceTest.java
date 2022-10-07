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

import java.io.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import org.eclipse.core.filesystem.*;
import org.eclipse.core.internal.resources.*;
import org.eclipse.core.internal.utils.FileUtil;
import org.eclipse.core.internal.utils.UniversalUniqueIdentifier;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.tests.harness.CoreTest;
import org.eclipse.core.tests.harness.FileSystemHelper;

/**
 * Superclass for tests that use the Eclipse Platform workspace.
 */
public abstract class ResourceTest extends CoreTest {
	//nature that installs and runs a builder (regression test for bug 29116)
	protected static final String NATURE_29116 = "org.eclipse.core.tests.resources.nature29116";

	//cycle1 requires: cycle2
	protected static final String NATURE_CYCLE1 = "org.eclipse.core.tests.resources.cycle1";
	//cycle2 requires: cycle3
	protected static final String NATURE_CYCLE2 = "org.eclipse.core.tests.resources.cycle2";

	//constants for nature ids

	//cycle3 requires: cycle1
	protected static final String NATURE_CYCLE3 = "org.eclipse.core.tests.resources.cycle3";
	//earthNature, one-of: stateSet
	protected static final String NATURE_EARTH = "org.eclipse.core.tests.resources.earthNature";
	//invalidNature
	protected static final String NATURE_INVALID = "org.eclipse.core.tests.resources.invalidNature";
	//missing nature
	protected static final String NATURE_MISSING = "no.such.nature.Missing";
	//missing pre-req nature
	protected static final String NATURE_MISSING_PREREQ = "org.eclipse.core.tests.resources.missingPrerequisiteNature";
	//mudNature, requires: waterNature, earthNature, one-of: otherSet
	protected static final String NATURE_MUD = "org.eclipse.core.tests.resources.mudNature";
	//simpleNature
	protected static final String NATURE_SIMPLE = "org.eclipse.core.tests.resources.simpleNature";
	//nature for regression tests of bug 127562
	protected static final String NATURE_127562 = "org.eclipse.core.tests.resources.bug127562Nature";
	//snowNature, requires: waterNature, one-of: otherSet
	protected static final String NATURE_SNOW = "org.eclipse.core.tests.resources.snowNature";
	//waterNature, one-of: stateSet
	protected static final String NATURE_WATER = "org.eclipse.core.tests.resources.waterNature";
	public static final String PI_RESOURCES_TESTS = "org.eclipse.core.tests.resources"; //$NON-NLS-1$
	protected static final String SET_OTHER = "org.eclipse.core.tests.resources.otherSet";
	//constants for nature sets
	protected static final String SET_STATE = "org.eclipse.core.tests.resources.stateSet";
	/**
	 * Number of received log messages with severity error while running a single
	 * test method.
	 */
	private final Queue<IStatus> loggedErrors = new ConcurrentLinkedQueue<>();

	/** Listener to count error messages while testing. */
	private final ILogListener errorLogListener = (IStatus status, String plugin) -> {
		if (status.matches(IStatus.ERROR)) {
			loggedErrors.add(status);
		}
	};

	/**
	 * Set of FileStore instances that must be deleted when the
	 * test is complete
	 * @see #getTempStore
	 */
	private final Set<IFileStore> storesToDelete = new HashSet<>();

	public static IWorkspace getWorkspace() {
		return ResourcesPlugin.getWorkspace();
	}

	/**
	 * Returns whether the file system in which the provided resource
	 * is stored is case sensitive. This succeeds whether or not the resource
	 * exists.
	 */
	protected static boolean isCaseSensitive(IResource resource) {
		return ((Resource) resource).getStore().getFileSystem().isCaseSensitive();
	}

	/**
	 * Returns whether the current platform is windows.
	 * @return <code>true</code> if this platform is windows, and
	 * <code>false</code> otherwise.
	 */
	protected static boolean isWindows() {
		return Platform.getOS().equals(Platform.OS_WIN32);
	}

	/**
	 * Convenience method to copy contents from one stream to another.
	 */
	public static void transferStreams(InputStream source, OutputStream destination, String path) {
		try {
			source.transferTo(destination);
		}  catch (IOException e) {
			fail("Failed to write during transferStreams", e);
		}finally {
			FileUtil.safeClose(source);
			FileUtil.safeClose(destination);
		}
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

	/**
	 * Assert that the given resource does not exist in the local store.
	 */
	public void assertDoesNotExistInFileSystem(IResource resource) {
		assertDoesNotExistInFileSystem("", resource); //$NON-NLS-1$
	}

	/**
	 * Assert that each element of the resource array does not exist in the
	 * local store.
	 */
	public void assertDoesNotExistInFileSystem(IResource[] resources) {
		assertDoesNotExistInFileSystem("", resources); //$NON-NLS-1$
	}

	/**
	 * Assert that the given resource does not exist in the local store.
	 */
	public void assertDoesNotExistInFileSystem(String message, IResource resource) {
		if (existsInFileSystem(resource)) {
			String formatted = message == null ? "" : message + " ";
			fail(formatted + resource.getFullPath() + " unexpectedly exists in the file system");
		}
	}

	/**
	 * Assert that each element of the resource array does not exist in the
	 * local store.
	 */
	public void assertDoesNotExistInFileSystem(String message, IResource[] resources) {
		for (IResource resource : resources) {
			assertDoesNotExistInFileSystem(message, resource);
		}
	}

	/**
	 * Assert that the given resource does not exist in the workspace
	 * resource info tree.
	 */
	public void assertDoesNotExistInWorkspace(IResource resource) {
		assertDoesNotExistInWorkspace("", resource); //$NON-NLS-1$
	}

	/**
	 * Assert that each element of the resource array does not exist
	 * in the workspace resource info tree.
	 */
	public void assertDoesNotExistInWorkspace(IResource[] resources) {
		assertDoesNotExistInWorkspace("", resources); //$NON-NLS-1$
	}

	/**
	 * Assert that the given resource does not exist in the workspace
	 * resource info tree.
	 */
	public void assertDoesNotExistInWorkspace(String message, IResource resource) {
		if (existsInWorkspace(resource, false)) {
			String formatted = message == null ? "" : message + " ";
			fail(formatted + resource.getFullPath().toString() + " unexpectedly exists in the workspace");
		}
	}

	/**
	 * Assert that each element of the resource array does not exist
	 * in the workspace resource info tree.
	 */
	public void assertDoesNotExistInWorkspace(String message, IResource[] resources) {
		for (IResource resource : resources) {
			assertDoesNotExistInWorkspace(message, resource);
		}
	}

	/**
	 * Assert whether or not the given resource exists in the local
	 * store. Use the resource manager to ensure that we have a
	 * correct Path -&gt; File mapping.
	 */
	public void assertExistsInFileSystem(IResource resource) {
		assertExistsInFileSystem("", resource); //$NON-NLS-1$
	}

	/**
	 * Assert that each element in the resource array  exists in the local store.
	 */
	public void assertExistsInFileSystem(IResource[] resources) {
		assertExistsInFileSystem("", resources); //$NON-NLS-1$
	}

	/**
	 * Assert whether or not the given resource exists in the local
	 * store. Use the resource manager to ensure that we have a
	 * correct Path -&gt; File mapping.
	 */
	public void assertExistsInFileSystem(String message, IResource resource) {
		if (!existsInFileSystem(resource)) {
			String formatted = message == null ? "" : message + " ";
			fail(formatted + resource.getFullPath() + " unexpectedly does not exist in the file system");
		}
	}

	/**
	 * Assert that each element in the resource array  exists in the local store.
	 */
	public void assertExistsInFileSystem(String message, IResource[] resources) {
		for (IResource resource : resources) {
			assertExistsInFileSystem(message, resource);
		}
	}

	/**
	 * Assert whether or not the given resource exists in the workspace
	 * resource info tree.
	 */
	public void assertExistsInWorkspace(IResource resource) {
		assertExistsInWorkspace("", resource, false); //$NON-NLS-1$
	}

	/**
	 * Assert whether or not the given resource exists in the workspace
	 * resource info tree.
	 */
	public void assertExistsInWorkspace(IResource resource, boolean phantom) {
		assertExistsInWorkspace("", resource, phantom); //$NON-NLS-1$
	}

	/**
	 * Assert that each element of the resource array exists in the
	 * workspace resource info tree.
	 */
	public void assertExistsInWorkspace(IResource[] resources) {
		assertExistsInWorkspace("", resources, false); //$NON-NLS-1$
	}

	/**
	 * Assert that each element of the resource array exists in the
	 * workspace resource info tree.
	 */
	public void assertExistsInWorkspace(IResource[] resources, boolean phantom) {
		assertExistsInWorkspace("", resources, phantom); //$NON-NLS-1$
	}

	/**
	 * Assert whether or not the given resource exists in the workspace
	 * resource info tree.
	 */
	public void assertExistsInWorkspace(String message, IResource resource) {
		assertExistsInWorkspace(message, resource, false);
	}

	/**
	 * Assert whether or not the given resource exists in the workspace
	 * resource info tree.
	 */
	public void assertExistsInWorkspace(String message, IResource resource, boolean phantom) {
		if (!existsInWorkspace(resource, phantom)) {
			String formatted = message == null ? "" : message + " ";
			fail(formatted + resource.getFullPath().toString() + " unexpectedly does not exist in the workspace");
		}
	}

	/**
	 * Assert that each element of the resource array exists in the
	 * workspace resource info tree.
	 */
	public void assertExistsInWorkspace(String message, IResource[] resources) {
		for (IResource resource : resources) {
			assertExistsInWorkspace(message, resource, false);
		}
	}

	/**
	 * Assert that each element of the resource array exists in the
	 * workspace resource info tree.
	 */
	public void assertExistsInWorkspace(String message, IResource[] resources, boolean phantom) {
		for (IResource resource : resources) {
			assertExistsInWorkspace(message, resource, phantom);
		}
	}

	/**
	 * Return a collection of resources the hierarchy defined by defineHeirarchy().
	 */
	public IResource[] buildResources() {
		return buildResources(getWorkspace().getRoot(), defineHierarchy());
	}

	/**
	 * Return a collection of resources for the given hierarchy at
	 * the given root.
	 */
	public IResource[] buildResources(IContainer root, String[] hierarchy) {
		IResource[] result = new IResource[hierarchy.length];
		for (int i = 0; i < hierarchy.length; i++) {
			IPath path = new Path(hierarchy[i]);
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

	protected void cleanup() throws CoreException {
		final IFileStore[] toDelete = storesToDelete.toArray(new IFileStore[0]);
		storesToDelete.clear();
		getWorkspace().run((IWorkspaceRunnable) monitor -> {
			getWorkspace().getRoot().delete(IResource.FORCE | IResource.ALWAYS_DELETE_PROJECT_CONTENT, getMonitor());
			//clear stores in workspace runnable to avoid interaction with resource jobs
			for (IFileStore element : toDelete) {
				clear(element);
			}
		}, null);
		getWorkspace().save(true, null);
		//don't leak builder jobs, since they may affect subsequent tests
		waitForBuild();
	}

	protected void clear(IFileStore store) {
		try {
			store.delete(EFS.NONE, null);
		} catch (CoreException e) {
			fail("IResourceTest#clear.99", e);
		}
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

	private IPath computeDefaultLocation(IResource target) {
		switch (target.getType()) {
			case IResource.ROOT :
				return Platform.getLocation();
			case IResource.PROJECT :
				return Platform.getLocation().append(target.getFullPath());
			default :
				IPath location = computeDefaultLocation(target.getProject());
				location = location.append(target.getFullPath().removeFirstSegments(1));
				return location;
		}
	}

	protected void create(final IResource resource, boolean local) throws CoreException {
		if (resource == null || resource.exists()) {
			return;
		}
		if (!resource.getParent().exists()) {
			create(resource.getParent(), local);
		}
		switch (resource.getType()) {
			case IResource.FILE :
				((IFile) resource).create(local ? new ByteArrayInputStream(new byte[0]) : null, true, getMonitor());
				break;
			case IResource.FOLDER :
				((IFolder) resource).create(true, local, getMonitor());
				break;
			case IResource.PROJECT :
				((IProject) resource).create(getMonitor());
				((IProject) resource).open(getMonitor());
				break;
		}
	}

	/**
	 * Create the given file in the local store.
	 */
	public void createFileInFileSystem(IFileStore file) {
		createFileInFileSystem(file, getRandomContents());
	}

	/**
	 * Create the given file in the local store.
	 */
	public void createFileInFileSystem(IFileStore file, InputStream contents) {
		OutputStream output = null;
		try {
			file.getParent().mkdir(EFS.NONE, null);
			output = file.openOutputStream(EFS.NONE, null);
			transferData(contents, output);
		} catch (CoreException e) {
			fail("ResourceTest#createFileInFileSystem.2", e);
		} finally {
			assertClose(output);
		}
	}

	/**
	 * Create the given file in the file system.
	 */
	public void createFileInFileSystem(IPath path) {
		createFileInFileSystem(path, getRandomContents());
	}

	/**
	 * Create the given file in the file system.
	 */
	public void createFileInFileSystem(IPath path, InputStream contents) {
		try {
			createFileInFileSystem(path.toFile(), contents);
		} catch (IOException e) {
			fail("ResourceTest#createFileInFileSystem", e);
		}
	}

	public IResource[] createHierarchy() {
		IResource[] result = buildResources();
		ensureExistsInWorkspace(result, true);
		return result;
	}

	/**
	 * Returns a collection of string paths describing the standard
	 * resource hierarchy for this test.  In the string forms, folders are
	 * represented as having trailing separators ('/').  All other resources
	 * are files.  It is generally assumed that this hierarchy will be
	 * inserted under some project structure.
	 * For example,
	 * <pre>
	 *    return new String[] {"/", "/1/", "/1/1", "/1/2", "/1/3", "/2/", "/2/1"};
	 * </pre>
	 */
	public String[] defineHierarchy() {
		return new String[0];
	}

	/**
	 * Delete the given resource from the local store. Use the resource
	 * manager to ensure that we have a correct Path -&gt; File mapping.
	 */
	public void ensureDoesNotExistInFileSystem(IResource resource) {
		IPath path = resource.getLocation();
		if (path != null) {
			ensureDoesNotExistInFileSystem(path.toFile());
		}
	}

	/**
	 * Delete the resources in the array from the local store.
	 */
	public void ensureDoesNotExistInFileSystem(IResource[] resources) {
		for (IResource resource : resources) {
			ensureDoesNotExistInFileSystem(resource);
		}
	}

	/**
	 * Delete the given resource from the workspace resource tree.
	 */
	public void ensureDoesNotExistInWorkspace(IResource resource) {
		try {
			if (resource.exists()) {
				resource.delete(true, null);
			}
		} catch (CoreException e) {
			fail("#ensureDoesNotExistInWorkspace(IResource): " + resource.getFullPath(), e);
		}
	}

	/**
	 * Delete each element of the resource array from the workspace
	 * resource info tree.
	 */
	public void ensureDoesNotExistInWorkspace(final IResource[] resources) {
		IWorkspaceRunnable body = monitor -> {
			for (IResource resource : resources) {
				ensureDoesNotExistInWorkspace(resource);
			}
		};
		try {
			getWorkspace().run(body, null);
		} catch (CoreException e) {
			fail("#ensureDoesNotExistInWorkspace(IResource[])", e);
		}
	}

	/**
	 * Create the given file in the local store. Use the resource manager
	 * to ensure that we have a correct Path -&gt; File mapping.
	 */
	public void ensureExistsInFileSystem(IFile file) {
		createFileInFileSystem(((Resource) file).getStore());
	}

	/**
	 * Create the given folder in the local store. Use the resource
	 * manager to ensure that we have a correct Path -&gt; File mapping.
	 */
	public void ensureExistsInFileSystem(IResource resource) {
		if (resource instanceof IFile) {
			ensureExistsInFileSystem((IFile) resource);
		} else {
			try {
				((Resource) resource).getStore().mkdir(EFS.NONE, null);
			} catch (CoreException e) {
				fail("ensureExistsInFileSystem.1", e);
			}
		}
	}

	/**
	 * Create the each resource of the array in the local store.
	 */
	public void ensureExistsInFileSystem(IResource[] resources) {
		for (IResource resource : resources) {
			ensureExistsInFileSystem(resource);
		}
	}

	/**
	 * Create the given file in the workspace resource info tree.
	 */
	public void ensureExistsInWorkspace(final IFile resource, final InputStream contents) {
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
		try {
			getWorkspace().run(body, null);
		} catch (CoreException e) {
			fail("#ensureExistsInWorkspace(IFile, InputStream): " + resource.getFullPath(), e);
		}
	}

	/**
	 * Create the given file in the workspace resource info tree.
	 */
	public void ensureExistsInWorkspace(IFile resource, String contents) {
		ensureExistsInWorkspace(resource, new ByteArrayInputStream(contents.getBytes()));
	}

	/**
	 * Create the given resource in the workspace resource info tree.
	 */
	public void ensureExistsInWorkspace(final IResource resource, final boolean local) {
		IWorkspaceRunnable body = monitor -> create(resource, local);
		try {
			getWorkspace().run(body, null);
		} catch (CoreException e) {
			fail("#ensureExistsInWorkspace(IResource): " + resource.getFullPath(), e);
		}
	}

	/**
	 * Create each element of the resource array in the workspace resource
	 * info tree.
	 */
	public void ensureExistsInWorkspace(final IResource[] resources, final boolean local) {
		IWorkspaceRunnable body = monitor -> {
			for (IResource resource : resources) {
				create(resource, local);
			}
		};
		try {
			getWorkspace().run(body, null);
		} catch (CoreException e) {
			fail("#ensureExistsInWorkspace(IResource[])", e);
		}
	}

	/**
	 * Modifies the passed in IFile in the file system so that it is out of sync
	 * with the workspace.
	 */
	public void ensureOutOfSync(final IFile file) {
		modifyInFileSystem(file);
		touchInFilesystem(file);
		assertTrue("File not out of sync: " + file.getLocation().toOSString(), file.getLocation().toFile().lastModified() != file.getLocalTimeStamp());
	}

	/**
	 * Touch (but don't modify) the resource in the filesystem so that it's modification stamp is newer than
	 * the cached value in the Workspace.
	 */
	public void touchInFilesystem(IResource resource) {
		// Ensure the resource exists in the filesystem
		IPath location = resource.getLocation();
		if (!location.toFile().exists()) {
			ensureExistsInFileSystem(resource);
		}
		// Manually check that the core.resource time-stamp is out-of-sync
		// with the java.io.File last modified. #isSynchronized() will schedule
		// out-of-sync resources for refresh, so we don't use that here.
		for (int count = 0; count < 3000 && isInSync(resource); count++) {
			FileTime now = FileTime.fromMillis(resource.getLocalTimeStamp() + 1000);
			try {
				Files.setLastModifiedTime(location.toFile().toPath(), now);
			} catch (IOException e) {
				fail("#touchInFilesystem(IResource)", e);
			}
			if (count > 1) {
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
					// ignore
				}
			}
		}
		assertTrue("File not out of sync: " + location.toOSString(), getLastModifiedTime(location) != resource.getLocalTimeStamp());
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

	private boolean existsInFileSystem(IResource resource) {
		IPath path = resource.getLocation();
		if (path == null) {
			path = computeDefaultLocation(resource);
		}
		return path.toFile().exists();
	}

	boolean existsInWorkspace(IResource resource, boolean phantom) {
		class CheckIfResourceExistsJob extends Job {

			private final AtomicBoolean resourceExists = new AtomicBoolean(false);

			public CheckIfResourceExistsJob() {
				super("Test " + ResourceTest.this.getName() + " checking whether resource exists: " + resource);
			}

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				if (monitor.isCanceled()) {
					return Status.CANCEL_STATUS;
				}

				IResource target = getWorkspace().getRoot().findMember(resource.getFullPath(), phantom);
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
		} catch (Exception e) {
			fail("Joining job to check whether resource exists in workspace failed with exception", e);
		}

		return checkIfResourceExistsJob.resourceExists();
	}

	/**
	 * Returns the unqualified class name of the receiver (i.e. without the package prefix).
	 */
	protected String getClassName() {
		String fullClassName = getClass().getName();
		return fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
	}

	/**
	 * Returns invalid sets of natures
	 */
	protected String[][] getInvalidNatureSets() {
		return new String[][] {{NATURE_SNOW}, //missing water pre-req
				{NATURE_WATER, NATURE_EARTH}, //duplicates from state-set
				{NATURE_WATER, NATURE_MUD}, //missing earth pre-req
				{NATURE_WATER, NATURE_EARTH, NATURE_MUD}, //duplicates from state-set
				{NATURE_SIMPLE, NATURE_SNOW, NATURE_WATER, NATURE_MUD}, //duplicates from other-set, missing pre-req
				{NATURE_MISSING}, //doesn't exist
				{NATURE_SIMPLE, NATURE_MISSING}, //missing doesn't exist
				{NATURE_MISSING_PREREQ}, //requires nature that doesn't exist
				{NATURE_SIMPLE, NATURE_MISSING_PREREQ}, //requires nature that doesn't exist
				{NATURE_CYCLE1}, //missing pre-req
				{NATURE_CYCLE2, NATURE_CYCLE3}, //missing pre-req
				{NATURE_CYCLE1, NATURE_SIMPLE, NATURE_CYCLE2, NATURE_CYCLE3}, //cycle
		};
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
		storesToDelete.add(store);
		return store;
	}

	/**
	 * Ensures that the file system location associated with the corresponding path is deleted during test tear down.
	 */
	protected void deleteOnTearDown(IPath path) {
		storesToDelete.add(EFS.getLocalFileSystem().getStore(path));

	}

	@Override
	public String getUniqueString() {
		return new UniversalUniqueIdentifier().toString();
	}

	/**
	 * Returns valid sets of natures
	 */
	protected String[][] getValidNatureSets() {
		return new String[][] {{}, {NATURE_SIMPLE}, {NATURE_SNOW, NATURE_WATER}, {NATURE_EARTH}, {NATURE_WATER, NATURE_SIMPLE, NATURE_SNOW},};
	}

	/**
	 * Checks whether the local file system supports accessing and modifying
	 * the given attribute.
	 */
	protected boolean isAttributeSupported(int attribute) {
		return (EFS.getLocalFileSystem().attributes() & attribute) != 0;
	}

	/**
	 * Checks whether the local file system supports accessing and modifying
	 * the read-only flag.
	 */
	protected boolean isReadOnlySupported() {
		return isAttributeSupported(EFS.ATTRIBUTE_READ_ONLY);
	}

	/**
	 * Modifies the content of the given file in the file system by appending an
	 * 'f'.
	 *
	 * @param file
	 *            the file system file to extend
	 */
	protected void modifyInFileSystem(IFile file) {
		String m = getClassName() + ".modifyInFileSystem(IFile): ";
		String newContent = readStringInFileSystem(file) + "f";
		IPath location = file.getLocation();
		if (location == null) {
			fail("0.1 - null location for file: " + file);
			return;
		}
		java.io.File osFile = location.toFile();
		try (FileOutputStream os = new FileOutputStream(osFile)) {
			os.write(newContent.getBytes("UTF8"));
		} catch (IOException e) {
			fail(m + "0.0", e);
		}
	}

	/**
	 * Modifies the content of the given file in the workspace by appending a 'w'.
	 *
	 * @param file
	 *            the workspace file to extend
	 */
	protected void modifyInWorkspace(IFile file) throws CoreException {
		String newContent = readStringInWorkspace(file) + "w";
		ByteArrayInputStream is = new ByteArrayInputStream(newContent.getBytes(StandardCharsets.UTF_8));
		file.setContents(is, false, false, null);
	}

	/**
	 * Returns the content of the given file in the file system as a byte array.
	 *
	 * @param file
	 *            file system file to read
	 */
	protected byte[] readBytesInFileSystem(IFile file) {
		String m = getClassName() + ".readBytesInFileSystem(IFile): ";
		try {
			IPath location = file.getLocation();
			if (location == null) {
				fail("0.1 - null location for file: " + file);
				return null;
			}
			java.io.File osFile = location.toFile();
			FileInputStream is = new FileInputStream(osFile);
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			transferData(is, os);
			return os.toByteArray();
		} catch (IOException e) {
			fail(m + "0.0", e);
		}
		return null;
	}

	/**
	 * Returns the content of the given file in the workspace as a byte array.
	 *
	 * @param file
	 *            workspace file to read
	 */
	protected byte[] readBytesInWorkspace(IFile file) {
		String m = getClassName() + ".readBytesInWorkspace(IFile): ";
		try {
			InputStream is = file.getContents(false);
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			transferData(is, os);
			return os.toByteArray();
		} catch (CoreException e) {
			fail(m + "0.0", e);
		}
		return null;
	}

	/**
	 * Returns the content of the given file in the file system as a String (UTF8).
	 *
	 * @param file
	 *            file system file to read
	 */
	protected String readStringInFileSystem(IFile file) {
		return new String(readBytesInFileSystem(file), StandardCharsets.UTF_8);
	}

	/**
	 * Returns the content of the given file in the workspace as a String (UTF8).
	 *
	 * @param file
	 *            workspace file to read
	 */
	protected String readStringInWorkspace(IFile file) {
		return new String(readBytesInWorkspace(file), StandardCharsets.UTF_8);
	}

	protected void setAttribute(IFileStore target, int attribute, boolean value) {
		assertTrue("setAttribute.1", isAttributeSupported(attribute));
		IFileInfo fileInfo = target.fetchInfo();
		fileInfo.setAttribute(attribute, value);
		try {
			target.putInfo(fileInfo, EFS.SET_ATTRIBUTES, null);
		} catch (CoreException e) {
			fail("ResourceTest#setAttribute", e);
		}
	}

	protected void setReadOnly(IFileStore target, boolean value) {
		assertTrue("setReadOnly.1", isReadOnlySupported());
		IFileInfo fileInfo = target.fetchInfo();
		fileInfo.setAttribute(EFS.ATTRIBUTE_READ_ONLY, value);
		try {
			target.putInfo(fileInfo, EFS.SET_ATTRIBUTES, null);
		} catch (CoreException e) {
			fail("ResourceTest#setReadOnly", e);
		}
	}

	protected void setReadOnly(IResource target, boolean value) {
		ResourceAttributes attributes = target.getResourceAttributes();
		assertNotNull("setReadOnly for null attributes", attributes);
		attributes.setReadOnly(value);
		try {
			target.setResourceAttributes(attributes);
		} catch (CoreException e) {
			fail("ResourceTest#setReadOnly", e);
		}
	}

	/**
	 * The environment should be set-up in the main method.
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		TestUtil.log(IStatus.INFO, getName(), "setUp");
		FreezeMonitor.expectCompletionInAMinute();
		assertNotNull("Workspace was not setup", getWorkspace());
		loggedErrors.clear();
		Platform.addLogListener(errorLogListener);
	}

	@Override
	protected void tearDown() throws Exception {
		Platform.removeLogListener(errorLogListener);
		TestUtil.log(IStatus.INFO, getName(), "tearDown");
		// Ensure everything is in a clean state for next one.
		// Session tests should overwrite it.
		cleanup();
		super.tearDown();
		FreezeMonitor.done();
	}

	protected void assertNoErrorsLogged() {
		List<IStatus> errorlist = new ArrayList<>();
		loggedErrors.removeIf(errorlist::add);
		errorlist.forEach(status -> {
			if (status.getException() != null) {
				throw new AssertionError("Test logged exception", status.getException());
			}
		});
		assertTrue(
				"Test logged errors: "
						+ errorlist.stream().map(IStatus::toString).collect(Collectors.joining(", ")),
				errorlist.isEmpty());
	}

	/**
	 * Blocks the calling thread until autobuild completes.
	 */
	protected void waitForBuild() {
		((Workspace) getWorkspace()).getBuildManager().waitForAutoBuild();
	}

	/**
	 * Blocks the calling thread until autobuild completes.
	 */
	protected void waitForRefresh() {
		try {
			Job.getJobManager().wakeUp(ResourcesPlugin.FAMILY_AUTO_REFRESH);
			Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_REFRESH, null);
		} catch (OperationCanceledException | InterruptedException e) {
			//ignore
		}
	}

	public String[] findAvailableDevices() {
		String[] devices = new String[2];
		for (int i = 97/*a*/; i < 123/*z*/; i++) {
			char c = (char) i;
			java.io.File rootFile = new java.io.File(c + ":\\");
			if (rootFile.exists() && rootFile.canWrite()) {
				//sometimes canWrite can return true but we are still not allowed to create a file - see bug 379284.
				File probe = new File(rootFile, getUniqueString());
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

	protected void waitForEncodingRelatedJobs() {
		TestUtil.waitForJobs(getName(), 10, 5_000, ValidateProjectEncoding.class);
		TestUtil.waitForJobs(getName(), 10, 5_000, CharsetDeltaJob.FAMILY_CHARSET_DELTA);
	}

	protected void waitForContentDescriptionUpdate() {
		TestUtil.waitForJobs(getName(), 10, 5_000, ContentDescriptionManager.FAMILY_DESCRIPTION_CACHE_FLUSH);
	}
}
