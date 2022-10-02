/*******************************************************************************
 *  Copyright (c) 2000, 2022 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Alexander Kurtakov <akurtako@redhat.com> - Bug 459343
 *******************************************************************************/
package org.eclipse.core.tests.resources;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

import java.io.File;
import java.util.*;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.*;
import org.eclipse.core.tests.harness.CancelingProgressMonitor;
import org.eclipse.core.tests.harness.FussyProgressMonitor;

public class IResourceTest extends ResourceTest {
	protected static final Boolean[] FALSE_AND_TRUE = { Boolean.FALSE, Boolean.TRUE };
	protected static IPath[] interestingPaths;
	protected static IResource[] interestingResources;
	protected static Set<IResource> nonExistingResources = new HashSet<>();
	protected static final IProgressMonitor[] PROGRESS_MONITORS = { new FussyProgressMonitor(),
			new CancelingProgressMonitor(), null };

	/**
	 * Resource exists in both file system and workspace, but has been changed
	 * in the file system since the last sync. This only applies to files.
	 */
	protected static final int S_CHANGED = 3;

	/**
	 * Resource does not exist in file system or workspace. */
	protected static final int S_DOES_NOT_EXIST = 4;

	/**
	 * Resource is a file in the workspace, but has been converted to a folder
	 * in the file system.
	 */
	protected static final int S_FILE_TO_FOLDER = 6;

	/**
	 * Resource exists in the file system only. It has been added to the
	 * file system manually since the last local refresh.
	 */
	protected static final int S_FILESYSTEM_ONLY = 1;

	/**
	 * Resource is a folder in the workspace, but has been converted to a file
	 * in the file system.
	 */
	protected static final int S_FOLDER_TO_FILE = 5;

	/**
	 * Resource exists in the file system and workspace, and is in sync */
	protected static final int S_UNCHANGED = 2;

	/**
	 * Resource only exists in the workspace. It has been deleted from the
	 * file system manually
	 */
	protected static final int S_WORKSPACE_ONLY = 0;
	protected static final Boolean[] TRUE_AND_FALSE = { Boolean.TRUE, Boolean.FALSE };
	protected static Set<IResource> unsynchronizedResources = new HashSet<>();

	/* the delta verifier */
	ResourceDeltaVerifier verifier;

	/**
	 * Get all files and directories in given directory recursive.
	 *
	 * @param dir
	 *            the directory to start with
	 * @return set of files and directories in given directory and sub-directories
	 */
	protected static Set<File> getAllFilesForDirectory(File dir) {
		Set<File> result = new HashSet<>(50);
		String[] members = dir.list();
		if (members != null) {
			for (String member2 : members) {
				File member = new File(dir, member2);
				result.add(member);
				if (member.isDirectory()) {
					result.addAll(getAllFilesForDirectory(member));
				}
			}
		}
		return result;
	}

	/**
	 * Get all files and directories in given resource recursive.
	 *
	 * @param resource
	 *            the resource to start with. Resource can be a file in which case
	 *            the result will only contain the file.
	 * @param considerUnsyncLocalFiles
	 *            if <code>true</code> force reading from filesystem
	 * @return set of files and directories under given resource
	 */
	protected static Set<File> getAllFilesForResource(IResource resource, boolean considerUnsyncLocalFiles)
			throws CoreException {
		Set<File> result = new HashSet<>(50);
		if (resource.getLocation() != null && (resource.getType() != IResource.PROJECT || ((IProject) resource).isOpen())) {
			java.io.File file = resource.getLocation().toFile();
			if (considerUnsyncLocalFiles) {
				if (file.exists()) {
					result.add(file);
					if (file.isDirectory()) {
						result.addAll(getAllFilesForDirectory(file));
					}
				}
			} else {
				if (resource.exists()) {
					result.add(file);
					if (resource.getType() != IResource.FILE) {
						IContainer container = (IContainer) resource;
						IResource[] children = container.members();
						for (IResource member : children) {
							result.addAll(getAllFilesForResource(member, considerUnsyncLocalFiles));
						}
					}
				}
			}
		}
		return result;
	}

	/**
	 * @return Set
	 * @param resource IResource
	 */
	protected static Set<IResource> getAllResourcesForResource(IResource resource) throws CoreException {
		Set<IResource> result = new HashSet<>(50);
		if (resource.exists()) {
			result.add(resource);
			if (resource.getType() != IResource.FILE && resource.isAccessible()) {
				IContainer container = (IContainer) resource;
				IResource[] children = container.members();
				for (IResource member : children) {
					result.addAll(getAllResourcesForResource(member));
				}
			}
		}
		return result;
	}

	/**
	 * Returns interesting resources for refresh local / sync tests. */
	protected IResource[] buildInterestingResources() {
		IProject emptyProject = getWorkspace().getRoot().getProject("EmptyProject");
		IProject fullProject = getWorkspace().getRoot().getProject("FullProject");
		//resource pattern is: empty file, empty folder, full folder, repeat
		// with full folder
		IResource[] resources = buildResources(fullProject, new String[] {"1", "2/", "3/", "3/1", "3/2/"});

		IResource[] result = new IResource[resources.length + 3];
		result[0] = getWorkspace().getRoot();
		result[1] = emptyProject;
		result[2] = fullProject;
		System.arraycopy(resources, 0, result, 3, resources.length);
		ensureExistsInWorkspace(result, true);
		return result;
	}

	private IResource[] buildSampleResources(IContainer root) {
		// do not change the example resources unless you change references to
		// specific indices in setUp()
		IResource[] result = buildResources(root, new String[] {"1/", "1/1/", "1/1/1/", "1/1/1/1", "1/1/2/", "1/1/2/1/", "1/1/2/2/", "1/1/2/3/", "1/2/", "1/2/1", "1/2/2", "1/2/3/", "1/2/3/1", "1/2/3/2", "1/2/3/3", "1/2/3/4", "2", "2"});
		ensureExistsInWorkspace(result, true);
		result[result.length - 1] = root.getFolder(new Path("2/"));
		nonExistingResources.add(result[result.length - 1]);

		IResource[] deleted = buildResources(root, new String[] {"1/1/2/1/", "1/2/3/1"});
		ensureDoesNotExistInWorkspace(deleted);
		nonExistingResources.addAll(Arrays.asList(deleted));
		//out of sync
		IResource[] unsynchronized = buildResources(root, new String[] {"1/2/3/3"});
		ensureOutOfSync((IFile) unsynchronized[0]);
		unsynchronizedResources.add(unsynchronized[0]);

		//file system only
		unsynchronized = buildResources(root, new String[] {"1/1/2/2/1"});
		ensureExistsInFileSystem(unsynchronized);
		unsynchronizedResources.add(unsynchronized[0]);
		return result;
	}

	/**
	 * Checks that the after state is as expected.
	 * @param receiver the resource that was the receiver of the refreshLocal
	 * call
	 * @param target the resource that was out of sync
	 */
	protected boolean checkAfterState(IResource receiver, IResource target, int state, int depth) {
		assertTrue(verifier.getMessage(), verifier.isDeltaValid());
		switch (state) {
			case S_FILESYSTEM_ONLY :
				assertExistsInFileSystem(target);
				//if receiver was a parent, then refreshLocal
				//will have added the target
				if (hasParent(target, receiver, depth) || target.equals(receiver)) {
					assertExistsInWorkspace(target);
				} else {
					assertDoesNotExistInWorkspace(target);
				}
				break;
			case S_UNCHANGED :
			case S_CHANGED :
				assertExistsInWorkspace(target);
				assertExistsInFileSystem(target);
				break;
			case S_WORKSPACE_ONLY :
				assertDoesNotExistInFileSystem(target);
				//if receiver was a parent, then refreshLocal
				//will have deleted the target
				if (hasParent(target, receiver, depth) || target.equals(receiver)) {
					assertDoesNotExistInWorkspace(target);
				} else {
					assertExistsInWorkspace(target);
				}
				break;
			case S_DOES_NOT_EXIST :
				assertDoesNotExistInWorkspace(target);
				assertDoesNotExistInFileSystem(target);
				break;
			case S_FOLDER_TO_FILE :
				break;
			case S_FILE_TO_FOLDER :
				break;
		}
		return true;
	}

	public void cleanUpAfterRefreshTest(Object[] args) {
		IResource receiver = (IResource) args[0];
		IResource target = (IResource) args[1];
		int state = ((Integer) args[2]).intValue();
		int depth = ((Integer) args[3]).intValue();
		if (!makesSense(receiver, target, state, depth)) {
			return;
		}
		try {
			getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, null);
		} catch (CoreException e) {
			fail("Exception tearing down in cleanUpAfterRefreshTest", e);
		}
		//target may have changed gender
		IResource changedTarget = getWorkspace().getRoot().findMember(target.getFullPath());
		if (changedTarget != null && changedTarget.getType() != target.getType()) {
			ensureDoesNotExistInWorkspace(changedTarget);
		}
		ensureExistsInWorkspace(interestingResources, true);
	}

	/**
	 * Returns an array of all projects in the given resource array.
	 */
	protected IProject[] getProjects(IResource[] resources) {
		ArrayList<IProject> list = new ArrayList<>();
		for (IResource resource : resources) {
			if (resource.getType() == IResource.PROJECT) {
				list.add((IProject) resource);
			}
		}
		return list.toArray(new IProject[list.size()]);
	}

	/**
	 * Returns true if resource1 has parent resource2, in range of the given
	 * depth. This is basically asking if refreshLocal on resource2 with depth
	 * "depth" will hit resource1.
	 */
	protected boolean hasParent(IResource resource1, IResource resource2, int depth) {
		if (depth == IResource.DEPTH_ZERO) {
			return false;
		}
		if (depth == IResource.DEPTH_ONE) {
			return resource2.equals(resource1.getParent());
		}
		IResource parent = resource1.getParent();
		while (parent != null) {
			if (parent.equals(resource2)) {
				return true;
			}
			parent = parent.getParent();
		}
		return false;
	}

	/**
	 * Returns interesting resource states. */
	protected Integer[] interestingDepths() {
		return new Integer[] {Integer.valueOf(IResource.DEPTH_ZERO), Integer.valueOf(IResource.DEPTH_ONE), Integer.valueOf(IResource.DEPTH_INFINITE)};
	}

	/**
	 * Returns interesting resource states. */
	protected Integer[] interestingStates() {
		return new Integer[] { Integer.valueOf(S_WORKSPACE_ONLY), Integer.valueOf(S_FILESYSTEM_ONLY),
				Integer.valueOf(S_UNCHANGED), Integer.valueOf(S_CHANGED), Integer.valueOf(S_DOES_NOT_EXIST),
				//		Integer.valueOf(S_FOLDER_TO_FILE),
				//		Integer.valueOf(S_FILE_TO_FOLDER),
		};
	}

	protected boolean isFile(IResource r) {
		return r.getType() == IResource.FILE;
	}

	protected boolean isFolder(IResource r) {
		return r.getType() == IResource.FOLDER;
	}

	protected boolean isProject(IResource r) {
		return r.getType() == IResource.PROJECT;
	}

	/**
	 * Returns true if this combination of arguments makes sense. */
	protected boolean makesSense(IResource receiver, IResource target, int state, int depth) {
		/* don't allow projects or the root as targets */
		if (target.getType() == IResource.PROJECT || target.getType() == IResource.ROOT) {
			return false;
		}

		/* target cannot be a parent of receiver */
		if (hasParent(receiver, target, IResource.DEPTH_INFINITE)) {
			return false;
		}

		/* target can only take certain forms for some states */
		switch (state) {
			case S_WORKSPACE_ONLY :
				return true;
			case S_FILESYSTEM_ONLY :
				return true;
			case S_UNCHANGED :
				return true;
			case S_CHANGED :
				return isFile(target);
			case S_DOES_NOT_EXIST :
				return true;
			case S_FOLDER_TO_FILE :
				return isFolder(target);
			case S_FILE_TO_FOLDER :
				return isFile(target);
		}

		return true;
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		IWorkspaceDescription description = getWorkspace().getDescription();
		description.setAutoBuilding(false);
		getWorkspace().setDescription(description);

		try {
			// open project
			IProject openProject = getWorkspace().getRoot().getProject("openProject");
			openProject.create(null);
			openProject.open(null);
			IResource[] resourcesInOpenProject = buildSampleResources(openProject);

			// closed project
			IProject closedProject = getWorkspace().getRoot().getProject("ClosedProject");
			closedProject.create(null);
			closedProject.open(null);
			IResource[] resourcesInClosedProject = buildSampleResources(closedProject);
			closedProject.close(null);

			// non-existant project
			IProject nonExistingProject = getWorkspace().getRoot().getProject("nonExistingProject");
			nonExistingProject.create(null);
			nonExistingProject.open(null);
			nonExistingProject.delete(true, null);

			ArrayList<IResource> resources = new ArrayList<>();
			resources.add(openProject);
			for (IResource element : resourcesInOpenProject) {
				resources.add(element);
			}

			resources.add(closedProject);
			for (IResource element : resourcesInClosedProject) {
				resources.add(element);
				nonExistingResources.add(element);
			}

			resources.add(nonExistingProject);
			nonExistingResources.add(nonExistingProject);

			interestingResources = new IResource[resources.size()];
			resources.toArray(interestingResources);

			String[] interestingPathnames = { "1/", "1/1/", "1/1/1/", "1/1/1/1", "1/1/2/1/", "1/1/2/2/", "1/1/2/3/",
					"1/2/", "1/2/1", "1/2/2", "1/2/3/", "1/2/3/1", "1/2/3/2", "1/2/3/3", "1/2/3/4", "2", "2/1", "2/2",
					"2/3", "2/4", "2/1/", "2/2/", "2/3/", "2/4/", ".." };
			interestingPaths = new IPath[interestingPathnames.length];
			for (int i = 0; i < interestingPathnames.length; i++) {
				interestingPaths[i] = new Path(interestingPathnames[i]);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
	}

	/**
	 * Sets up the workspace and file system for this test. */
	protected void setupBeforeState(IResource receiver, IResource target, int state, int depth, boolean addVerifier) {
		// Wait for any outstanding refresh to finish
		try {
			Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_REFRESH, getMonitor());
		} catch (InterruptedException e) {
			fail("interrupted unexpectedly");
		}

		if (addVerifier) {
			/* install the verifier */
			if (verifier == null) {
				verifier = new ResourceDeltaVerifier();
				getWorkspace().addResourceChangeListener(verifier);
			}
		}

		/* the target's parents must exist */
		ensureExistsInWorkspace(target.getParent(), true);
		switch (state) {
			case S_WORKSPACE_ONLY :
				ensureExistsInWorkspace(target, true);
				ensureDoesNotExistInFileSystem(target);
				if (addVerifier) {
					verifier.reset();
					// we only get a delta if the receiver of refreshLocal
					// is a parent of the changed resource, or they're the same
					// resource.
					if (hasParent(target, receiver, depth) || target.equals(receiver)) {
						verifier.addExpectedDeletion(target);
					}
				}
				break;
			case S_FILESYSTEM_ONLY :
				ensureDoesNotExistInWorkspace(target);
				ensureExistsInFileSystem(target);
				if (addVerifier) {
					verifier.reset();
					// we only get a delta if the receiver of refreshLocal
					// is a parent of the changed resource, or they're the same
					// resource.
					if (hasParent(target, receiver, depth) || target.equals(receiver)) {
						verifier.addExpectedChange(target, IResourceDelta.ADDED, 0);
					}
				}
				break;
			case S_UNCHANGED :
				ensureExistsInWorkspace(target, true);
				if (addVerifier) {
					verifier.reset();
				}
				break;
			case S_CHANGED :
				ensureExistsInWorkspace(target, true);
				touchInFilesystem(target);
				if (addVerifier) {
					verifier.reset();
					// we only get a delta if the receiver of refreshLocal
					// is a parent of the changed resource, or they're the same
					// resource.
					if (hasParent(target, receiver, depth) || target.equals(receiver)) {
						verifier.addExpectedChange(target, IResourceDelta.CHANGED, IResourceDelta.CONTENT);
					}
				}
				break;
			case S_DOES_NOT_EXIST :
				ensureDoesNotExistInWorkspace(target);
				ensureDoesNotExistInFileSystem(target);
				if (addVerifier) {
					verifier.reset();
				}
				break;
			case S_FOLDER_TO_FILE :
				ensureExistsInWorkspace(target, true);
				ensureDoesNotExistInFileSystem(target);
				ensureExistsInFileSystem(target);
				if (addVerifier) {
					verifier.reset();
					// we only get a delta if the receiver of refreshLocal
					// is a parent of the changed resource, or they're the same
					// resource.
					if (hasParent(target, receiver, depth) || target.equals(receiver)) {
						verifier.addExpectedChange(target, IResourceDelta.CHANGED, IResourceDelta.REPLACED | IResourceDelta.TYPE | IResourceDelta.CONTENT);
					}
				}
				break;
			case S_FILE_TO_FOLDER :
				ensureExistsInWorkspace(target, true);
				ensureDoesNotExistInFileSystem(target);
				target.getLocation().toFile().mkdirs();
				if (addVerifier) {
					verifier.reset();
					// we only get a delta if the receiver of refreshLocal
					// is a parent of the changed resource, or they're the same
					// resource.
					if (hasParent(target, receiver, depth) || target.equals(receiver)) {
						verifier.addExpectedChange(target, IResourceDelta.CHANGED, IResourceDelta.REPLACED | IResourceDelta.TYPE | IResourceDelta.CONTENT);
					}
				}
				break;
		}
	}

	@Override
	protected void tearDown() throws Exception {
		if (verifier != null) {
			getWorkspace().removeResourceChangeListener(verifier);
		}
		getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, null);
		ensureDoesNotExistInWorkspace(getWorkspace().getRoot());
		interestingPaths = null;
		interestingResources = null;
		super.tearDown();
	}

	/**
	 * Performs black box testing of the following method: void
	 * accept(IResourceVisitor)
	 */
	public void testAccept2() {
		class LoggingResourceVisitor implements IResourceVisitor {
			Vector<IResource> visitedResources = new Vector<>();

			void clear() {
				visitedResources.removeAllElements();
			}

			void recordVisit(IResource r) {
				visitedResources.addElement(r);
			}

			@Override
			public boolean visit(IResource r) {
				throw new RuntimeException("this class is abstract");
			}
		}

		final LoggingResourceVisitor deepVisitor = new LoggingResourceVisitor() {
			@Override
			public boolean visit(IResource r) {
				recordVisit(r);
				return true;
			}
		};

		final LoggingResourceVisitor shallowVisitor = new LoggingResourceVisitor() {
			@Override
			public boolean visit(IResource r) {
				recordVisit(r);
				return false;
			}
		};

		LoggingResourceVisitor[] interestingVisitors = new LoggingResourceVisitor[] {shallowVisitor, deepVisitor};
		Object[][] inputs = { interestingResources, interestingVisitors, TRUE_AND_FALSE, };
		new TestPerformer("IResourceTest.testAccept2") {

			@Override
			public Object[] interestingOldState(Object[] args) {
				return null;
			}

			@Override
			public Object invokeMethod(Object[] args, int count) throws Exception {
				IResource resource = (IResource) args[0];
				IResourceVisitor visitor = (IResourceVisitor) args[1];
				Boolean includePhantoms = (Boolean) args[2];
				resource.accept(visitor, IResource.DEPTH_INFINITE, includePhantoms.booleanValue());
				return null;
			}

			@Override
			public boolean shouldFail(Object[] args, int count) {
				deepVisitor.clear();
				shallowVisitor.clear();
				IResource resource = (IResource) args[0];
				return nonExistingResources.contains(resource) || !resource.isAccessible();
			}

			@Override
			public boolean wasSuccess(Object[] args, Object result, Object[] oldState) {
				IResource resource = (IResource) args[0];
				LoggingResourceVisitor visitor = (LoggingResourceVisitor) args[1];
				//Boolean includePhantoms = (Boolean) args[2];
				Vector<IResource> visitedResources = visitor.visitedResources;
				if (visitor == shallowVisitor) {
					return visitedResources.size() == 1 && visitedResources.elementAt(0).equals(resource);
				} else if (visitor == deepVisitor) {
					if (resource.getType() == IResource.FILE) {
						return visitedResources.size() == 1 && visitedResources.elementAt(0).equals(resource);
					}
					IContainer container = (IContainer) resource;
					int memberCount = 0;
					try {
						memberCount = memberCount + container.members().length;
					} catch (CoreException ex) {
						return false;
					}
					return visitedResources.size() >= memberCount + 1 && visitedResources.elementAt(0).equals(resource);
				} else {
					return false;
				}
			}
		}.performTest(inputs);
	}

	public void testAcceptDoNotCheckExistence() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject(getUniqueString());
		IFolder a = project.getFolder("a");
		ensureExistsInWorkspace(project, true);

		// pass DEPTH_ONE to avoid using proxy visitor
		assertThrows(CoreException.class, () -> a.accept((IResourceVisitor) resource -> {
			// we should not get that far if the resource does not exist
			fail("1.0");
			return true;
		}, IResource.DEPTH_ONE, IResource.NONE));

		assertThrows(CoreException.class, () -> a.accept(proxy -> {
			// we should not get that far if the resource does not exist
			fail("2.0");
			return true;
		}, IResource.NONE));

		// pass DEPTH_ONE to avoid using proxy visitor
		// if we don't check for existence, then no exception should be thrown
		a.accept((IResourceVisitor) resource -> {
			// we should not get that far if the resource does not exist
			fail("3.0");
			return true;
		}, IResource.DEPTH_ONE, IContainer.DO_NOT_CHECK_EXISTENCE);

		// if we don't check for existence, then no exception should be thrown
		a.accept(proxy -> {
			// we should not get that far if the resource does not exist
			fail("4.0");
			return true;
		}, IContainer.DO_NOT_CHECK_EXISTENCE);
	}

	public void testAcceptProxyVisitorWithDepth() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject(getUniqueString());
		IFolder a = project.getFolder("a");
		IFile a1 = a.getFile("a1.txt");
		IFile a2 = a.getFile("a2.txt");
		IFolder b = a.getFolder("b");
		IFile b1 = b.getFile("b1.txt");
		IFile b2 = b.getFile("b2.txt");
		IFolder c = b.getFolder("c");
		IFile c1 = c.getFile("c1.txt");
		IFile c2 = c.getFile("c2.txt");
		final Set<IResource> toVisit = new HashSet<>();
		final int[] toVisitCount = { 0 };

		IResourceProxyVisitor visitor = proxy -> {
			toVisit.remove(proxy.requestResource());
			toVisitCount[0]--;
			return true;
		};

		ensureExistsInWorkspace(new IResource[] {project, a, a1, a2, b, b1, b2, c, c1, c2}, true);

		toVisit.addAll(Arrays.asList(new IResource[] {a}));
		toVisitCount[0] = 1;
		a.accept(visitor, IResource.DEPTH_ZERO, IResource.NONE);
		assertTrue("1.0", toVisit.isEmpty());
		assertEquals("1.1", 0, toVisitCount[0]);

		toVisit.addAll(Arrays.asList(a, a1, a2, b));
		toVisitCount[0] = 4;
		a.accept(visitor, IResource.DEPTH_ONE, IResource.NONE);
		assertTrue("2.0", toVisit.isEmpty());
		assertEquals("2.1", 0, toVisitCount[0]);

		toVisit.addAll(Arrays.asList(a, a1, a2, b, b1, b2, c, c1, c2));
		toVisitCount[0] = 9;
		a.accept(visitor, IResource.DEPTH_INFINITE, IResource.NONE);
		assertTrue("3.0", toVisit.isEmpty());
		assertEquals("3.1", 0, toVisitCount[0]);
	}

	/**
	 * This method tests the IResource.refreshLocal() operation */
	public void testAddLocalProject() throws CoreException {
		/**
		 * Add a project in the file system, but not in the workspace */

		IProject project1 = getWorkspace().getRoot().getProject("Project");
		project1.create(getMonitor());
		project1.open(getMonitor());

		IProject project2 = getWorkspace().getRoot().getProject("NewProject");

		IPath projectPath = project1.getLocation().removeLastSegments(1).append("NewProject");
		try {
			projectPath.toFile().mkdirs();

			project1.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
			project2.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
			assertTrue("1.1", project1.exists());
			assertTrue("1.2", project1.isSynchronized(IResource.DEPTH_INFINITE));
			assertFalse("1.3", project2.exists());
			assertTrue("1.4", project2.isSynchronized(IResource.DEPTH_INFINITE));
		} finally {
			Workspace.clear(projectPath.toFile());
		}
	}

	/**
	 * Tests various resource constants. */
	public void testConstants() {

		// IResource constants (all have fixed values)
		assertEquals("1.0", 0, IResource.NONE);

		assertEquals("2.1", 0x1, IResource.FILE);
		assertEquals("2.2", 0x2, IResource.FOLDER);
		assertEquals("2.3", 0x4, IResource.PROJECT);
		assertEquals("2.4", 0x8, IResource.ROOT);

		assertEquals("3.1", 0, IResource.DEPTH_ZERO);
		assertEquals("3.2", 1, IResource.DEPTH_ONE);
		assertEquals("3.1", 2, IResource.DEPTH_INFINITE);

		assertEquals("4.1", -1, IResource.NULL_STAMP);

		assertEquals("5.1", 0x1, IResource.FORCE);
		assertEquals("5.2", 0x2, IResource.KEEP_HISTORY);
		assertEquals("5.3", 0x4, IResource.ALWAYS_DELETE_PROJECT_CONTENT);
		assertEquals("5.4", 0x8, IResource.NEVER_DELETE_PROJECT_CONTENT);

		// IContainer constants (all have fixed values)
		assertEquals("6.1", 0x1, IContainer.INCLUDE_PHANTOMS);
		assertEquals("6.2", 0x2, IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS);
		assertEquals("6.2", 0x8, IContainer.INCLUDE_HIDDEN);
	}

	/**
	 * Performs black box testing of the following method: void copy(IPath,
	 * boolean, IProgressMonitor)
	 */
	public void testCopy() {
		//add markers to all resources ... markers should not be copied
		try {
			getWorkspace().getRoot().accept(resource -> {
				if (resource.isAccessible()) {
					resource.createMarker(IMarker.TASK);
				}
				return true;
			});
		} catch (CoreException e) {
			fail("1.0", e);
		}

		Object[][] inputs = new Object[][] {interestingResources, interestingPaths, TRUE_AND_FALSE, PROGRESS_MONITORS};
		new TestPerformer("IResourceTest.testCopy") {

			@Override
			public Object[] interestingOldState(Object[] args) {
				return null;
			}

			@Override
			public Object invokeMethod(Object[] args, int count) throws Exception {
				IResource resource = (IResource) args[0];
				IPath destination = (IPath) args[1];
				Boolean force = (Boolean) args[2];
				IProgressMonitor monitor = (IProgressMonitor) args[3];
				if (monitor instanceof FussyProgressMonitor) {
					((FussyProgressMonitor) monitor).prepare();
				}
				resource.copy(destination, force.booleanValue(), monitor);
				if (monitor instanceof FussyProgressMonitor) {
					((FussyProgressMonitor) monitor).sanityCheck();
				}
				return null;
			}

			@Override
			public boolean shouldFail(Object[] args, int count) {
				IResource resource = (IResource) args[0];
				IPath destination = (IPath) args[1];
				//Boolean force = (Boolean) args[2];
				if (!resource.isAccessible()) {
					return true;
				}
				if (isProject(resource) && destination.segmentCount() > 1 && !getWorkspace().validatePath(destination.toString(), IResource.FOLDER).isOK()) {
					return true;
				}
				java.io.File destinationParent = destination.isAbsolute() ? destination.removeLastSegments(1).toFile() : resource.getLocation().removeLastSegments(1).append(destination.removeLastSegments(1)).toFile();
				java.io.File destinationFile = destination.isAbsolute() ? destination.toFile() : resource.getLocation().removeLastSegments(1).append(destination).removeTrailingSeparator().toFile();
				return !destinationParent.exists() || !destinationParent.isDirectory() || destinationFile.exists() || destinationFile.toString().startsWith(resource.getLocation().toFile().toString());
			}

			@Override
			public boolean wasSuccess(Object[] args, Object result, Object[] oldState) throws CoreException {
				IResource source = (IResource) args[0];
				IPath destination = (IPath) args[1];
				//ensure the destination exists
				//"Relative paths are considered to be relative to the
				// container of the resource being copied."
				IPath path = destination.isAbsolute() ? destination : source.getParent().getFullPath().append(destination);
				IResource copy = getWorkspace().getRoot().findMember(path);
				if (copy == null) {
					return false;
				}
				if (!copy.exists()) {
					return false;
				}
				//markers are never copied, so ensure copy has none
				if (copy.findMarkers(IMarker.TASK, true, IResource.DEPTH_INFINITE).length > 0) {
					return false;
				}
				return true;
			}
		}.performTest(inputs);
	}

	/**
	 * Performs black box testing of the following method: void delete(boolean,
	 * IProgressMonitor)
	 */
	public void testDelete() {
		IProgressMonitor[] monitors = new IProgressMonitor[] {new FussyProgressMonitor(), null};
		Object[][] inputs = { FALSE_AND_TRUE, monitors, interestingResources };
		final String CANCELED = "canceled";
		new TestPerformer("IResourceTest.testDelete") {

			@Override
			public Object[] interestingOldState(Object[] args) throws Exception {
				Boolean force = (Boolean) args[0];
				IResource resource = (IResource) args[2];
				return new Object[] {Boolean.valueOf(resource.isAccessible()), getAllFilesForResource(resource, force.booleanValue()), getAllResourcesForResource(resource)};
			}

			@Override
			public Object invokeMethod(Object[] args, int count) throws Exception {
				Boolean force = (Boolean) args[0];
				IProgressMonitor monitor = (IProgressMonitor) args[1];
				IResource resource = (IResource) args[2];
				if (monitor instanceof FussyProgressMonitor) {
					((FussyProgressMonitor) monitor).prepare();
				}
				try {
					resource.delete(force.booleanValue(), monitor);
				} catch (OperationCanceledException e) {
					return CANCELED;
				}
				if (monitor instanceof FussyProgressMonitor) {
					((FussyProgressMonitor) monitor).sanityCheck();
				}
				return null;
			}

			@Override
			public boolean shouldFail(Object[] args, int count) {
				Boolean force = (Boolean) args[0];
				IProgressMonitor monitor = (IProgressMonitor) args[1];
				IResource resource = (IResource) args[2];
				if (monitor instanceof CancelingProgressMonitor) {
					return false;
				}
				if (force.booleanValue() || !resource.exists()) {
					return false;
				}
				if (resource.getType() == IResource.PROJECT) {
					IProject project = (IProject) resource;
					try {
						if (!project.isOpen()) {
							return false;
						}
						IResource[] children = project.members();
						for (IResource member : children) {
							if (shouldFail(new Object[] {args[0], args[1], member}, count)) {
								return true;
							}
						}
					} catch (CoreException ex) {
						ex.printStackTrace();
						throw new RuntimeException("there is a problem in the testing method 'shouldFail'");
					}
					return false;
				}
				final boolean[] hasUnsynchronizedResources = new boolean[] {false};
				try {
					resource.accept(new IResourceVisitor() {
						@Override
						public boolean visit(IResource toVisit) throws CoreException {
							File target = toVisit.getLocation().toFile();
							if (target.exists() != toVisit.exists()) {
								hasUnsynchronizedResources[0] = true;
								return false;
							}
							if (target.isFile() != (toVisit.getType() == IResource.FILE)) {
								hasUnsynchronizedResources[0] = true;
								return false;
							}
							if (unsynchronizedResources.contains(toVisit)) {
								hasUnsynchronizedResources[0] = true;
								return false;
							}
							if (target.isFile()) {
								return false;
							}
							String[] list = target.list();
							if (list == null) {
								return true;
							}
							IContainer container = (IContainer) toVisit;
							for (String element : list) {
								File file = new File(target, element);
								IResource child = file.isFile() ? (IResource) container.getFile(new Path(element)) : container.getFolder(new Path(element));
								if (!child.exists()) {
									visit(child);
								}
							}
							return true;
						}
					});
				} catch (Exception ex) {
					ex.printStackTrace();
					throw new RuntimeException("there is a problem in the testing method 'shouldFail'");
				}
				return hasUnsynchronizedResources[0];
			}

			@Override
			public boolean wasSuccess(Object[] args, Object result, Object[] oldState) throws Exception {
				Boolean force = (Boolean) args[0];
				IProgressMonitor monitor = (IProgressMonitor) args[1];
				IResource resource = (IResource) args[2];
				if (result == CANCELED) {
					return monitor instanceof CancelingProgressMonitor;
				}
				//oldState[0] : was resource accessible before the invocation?
				//oldState[1] : all files that should have been deleted from
				// the file system
				//oldState[2] : all resources that should have been deleted
				// from the workspace
				if (resource.getType() != IResource.PROJECT && ((Boolean) oldState[0]).booleanValue()) {
					// check the parent's members, deleted resource should not
					// be a member
					IResource[] children = ((IContainer) getWorkspace().getRoot().findMember(resource.getFullPath().removeLastSegments(1))).members();
					for (IResource element : children) {
						if (resource == element) {
							return false;
						}
					}
				}
				if (!getAllFilesForResource(resource, force.booleanValue()).isEmpty()) {
					return false;
				}
				@SuppressWarnings("unchecked")
				Set<File> oldFiles = (Set<File>) oldState[1];
				for (File oldFile : oldFiles) {
					if (oldFile.exists()) {
						return false;
					}
				}
				@SuppressWarnings("unchecked")
				Set<IResource> oldResources = (Set<IResource>) oldState[2];
				for (IResource oldResource : oldResources) {
					if (oldResource.exists() || getWorkspace().getRoot().findMember(oldResource.getFullPath()) != null) {
						return false;
					}
				}
				return true;
			}
		}.performTest(inputs);
	}

	/**
	 * Performs black box testing of the following methods: isDerived() and
	 * setDerived(boolean, IProgressMonitor)
	 */
	public void testDerived() {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject("Project");
		IFolder folder = project.getFolder("folder");
		IFile file = folder.getFile("target");
		try {
			project.create(getMonitor());
			project.open(getMonitor());
			folder.create(true, true, getMonitor());
			file.create(getRandomContents(), true, getMonitor());

		} catch (CoreException e) {
			fail("1.0", e);
		}

		verifier = new ResourceDeltaVerifier();
		getWorkspace().addResourceChangeListener(verifier, IResourceChangeEvent.POST_CHANGE);

		// all resources have independent derived flag; all non-derived by
		// default; check each type
		try {

			// root - cannot be marked as derived
			assertFalse("2.1.1", root.isDerived());
			assertFalse("2.1.2", project.isDerived());
			assertFalse("2.1.3", folder.isDerived());
			assertFalse("2.1.4", file.isDerived());

			root.setDerived(true, new NullProgressMonitor());
			assertFalse("2.2.1", root.isDerived());
			assertFalse("2.2.2", project.isDerived());
			assertFalse("2.2.3", folder.isDerived());
			assertFalse("2.2.4", file.isDerived());
			assertTrue("2.2.5" + verifier.getMessage(), verifier.isDeltaValid());
			verifier.reset();

			root.setDerived(false, new NullProgressMonitor());
			assertFalse("2.3.1", root.isDerived());
			assertFalse("2.3.2", project.isDerived());
			assertFalse("2.3.3", folder.isDerived());
			assertFalse("2.3.4", file.isDerived());
			assertTrue("2.3.5" + verifier.getMessage(), verifier.isDeltaValid());
			verifier.reset();

			// project - cannot be marked as derived
			project.setDerived(true, new NullProgressMonitor());
			assertFalse("3.1.1", root.isDerived());
			assertFalse("3.1.2", project.isDerived());
			assertFalse("3.1.3", folder.isDerived());
			assertFalse("3.1.4", file.isDerived());
			assertTrue("3.1.5" + verifier.getMessage(), verifier.isDeltaValid());
			verifier.reset();

			project.setDerived(false, new NullProgressMonitor());
			assertFalse("3.2.1", root.isDerived());
			assertFalse("3.2.2", project.isDerived());
			assertFalse("3.2.3", folder.isDerived());
			assertFalse("3.2.4", file.isDerived());
			assertTrue("3.2.5" + verifier.getMessage(), verifier.isDeltaValid());
			verifier.reset();

			// folder
			verifier.addExpectedChange(folder, IResourceDelta.CHANGED, IResourceDelta.DERIVED_CHANGED);
			folder.setDerived(true, new NullProgressMonitor());
			assertFalse("4.1.1", root.isDerived());
			assertFalse("4.1.2", project.isDerived());
			assertTrue("4.1.3", folder.isDerived());
			assertFalse("4.1.4", file.isDerived());
			assertTrue("4.1.5" + verifier.getMessage(), verifier.isDeltaValid());
			verifier.reset();

			verifier.addExpectedChange(folder, IResourceDelta.CHANGED, IResourceDelta.DERIVED_CHANGED);
			folder.setDerived(false, new NullProgressMonitor());
			assertFalse("4.2.1", root.isDerived());
			assertFalse("4.2.2", project.isDerived());
			assertFalse("4.2.3", folder.isDerived());
			assertFalse("4.2.4", file.isDerived());
			assertTrue("4.2.5" + verifier.getMessage(), verifier.isDeltaValid());
			verifier.reset();

			// file
			verifier.addExpectedChange(file, IResourceDelta.CHANGED, IResourceDelta.DERIVED_CHANGED);
			file.setDerived(true, new NullProgressMonitor());
			assertFalse("5.1.1", root.isDerived());
			assertFalse("5.1.2", project.isDerived());
			assertFalse("5.1.3", folder.isDerived());
			assertTrue("5.1.4", file.isDerived());
			assertTrue("5.1.5" + verifier.getMessage(), verifier.isDeltaValid());
			verifier.reset();

			verifier.addExpectedChange(file, IResourceDelta.CHANGED, IResourceDelta.DERIVED_CHANGED);
			file.setDerived(false, new NullProgressMonitor());
			assertFalse("5.2.1", root.isDerived());
			assertFalse("5.2.2", project.isDerived());
			assertFalse("5.2.3", folder.isDerived());
			assertFalse("5.2.4", file.isDerived());
			assertTrue("5.2.5" + verifier.getMessage(), verifier.isDeltaValid());
			verifier.reset();

		} catch (CoreException e) {
			fail("6.0", e);
		}

		/* remove trash */
		try {
			project.delete(true, getMonitor());
		} catch (CoreException e) {
			fail("7.0", e);
		}

		// isDerived should return false when resource does not exist
		assertFalse("8.1", project.isDerived());
		assertFalse("8.2", folder.isDerived());
		assertFalse("8.3", file.isDerived());

		// setDerived should fail when resource does not exist
		assertThrows(CoreException.class, () -> project.setDerived(false, new NullProgressMonitor()));
		assertThrows(CoreException.class, () -> folder.setDerived(false, new NullProgressMonitor()));
		assertThrows(CoreException.class, () -> file.setDerived(false, new NullProgressMonitor()));
	}

	/**
	 * Performs black box testing of the following methods: isDerived() and
	 * setDerived(boolean)
	 */
	public void testDeprecatedDerived() {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject("Project");
		IFolder folder = project.getFolder("folder");
		IFile file = folder.getFile("target");
		try {
			project.create(getMonitor());
			project.open(getMonitor());
			folder.create(true, true, getMonitor());
			file.create(getRandomContents(), true, getMonitor());

		} catch (CoreException e) {
			fail("1.0", e);
		}

		// all resources have independent derived flag; all non-derived by
		// default; check each type
		try {

			// root - cannot be marked as derived
			assertFalse("2.1.1", root.isDerived());
			assertFalse("2.1.2", project.isDerived());
			assertFalse("2.1.3", folder.isDerived());
			assertFalse("2.1.4", file.isDerived());
			root.setDerived(true);
			assertFalse("2.2.1", root.isDerived());
			assertFalse("2.2.2", project.isDerived());
			assertFalse("2.2.3", folder.isDerived());
			assertFalse("2.2.4", file.isDerived());
			root.setDerived(false);
			assertFalse("2.3.1", root.isDerived());
			assertFalse("2.3.2", project.isDerived());
			assertFalse("2.3.3", folder.isDerived());
			assertFalse("2.3.4", file.isDerived());

			// project - cannot be marked as derived
			project.setDerived(true);
			assertFalse("3.1.1", root.isDerived());
			assertFalse("3.1.2", project.isDerived());
			assertFalse("3.1.3", folder.isDerived());
			assertFalse("3.1.4", file.isDerived());
			project.setDerived(false);
			assertFalse("3.2.1", root.isDerived());
			assertFalse("3.2.2", project.isDerived());
			assertFalse("3.2.3", folder.isDerived());
			assertFalse("3.2.4", file.isDerived());

			// folder
			folder.setDerived(true);
			assertFalse("4.1.1", root.isDerived());
			assertFalse("4.1.2", project.isDerived());
			assertTrue("4.1.3", folder.isDerived());
			assertFalse("4.1.4", file.isDerived());
			folder.setDerived(false);
			assertFalse("4.2.1", root.isDerived());
			assertFalse("4.2.2", project.isDerived());
			assertFalse("4.2.3", folder.isDerived());
			assertFalse("4.2.4", file.isDerived());

			// file
			file.setDerived(true);
			assertFalse("5.1.1", root.isDerived());
			assertFalse("5.1.2", project.isDerived());
			assertFalse("5.1.3", folder.isDerived());
			assertTrue("5.1.4", file.isDerived());
			file.setDerived(false);
			assertFalse("5.2.1", root.isDerived());
			assertFalse("5.2.2", project.isDerived());
			assertFalse("5.2.3", folder.isDerived());
			assertFalse("5.2.4", file.isDerived());

		} catch (CoreException e) {
			fail("6.0", e);
		}

		/* remove trash */
		try {
			project.delete(true, getMonitor());
		} catch (CoreException e) {
			fail("7.0", e);
		}

		// isDerived should return false when resource does not exist
		assertFalse("8.1", project.isDerived());
		assertFalse("8.2", folder.isDerived());
		assertFalse("8.3", file.isDerived());

		// setDerived should fail when resource does not exist
		assertThrows(CoreException.class, () -> project.setDerived(false));
		assertThrows(CoreException.class, () -> folder.setDerived(false));
		assertThrows(CoreException.class, () -> file.setDerived(false));
	}

	/**
	 * Test the isDerived() and isDerived(int) methods
	 */
	public void testDerivedUsingAncestors() {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject(getUniqueString());
		IFolder folder = project.getFolder("folder");
		IFile file1 = folder.getFile("file1.txt");
		IFile file2 = folder.getFile("file2.txt");
		IResource[] resources = { project, folder, file1, file2 };

		// create the resources
		ensureExistsInWorkspace(resources, true);

		// initial values should be false
		for (IResource resource2 : resources) {
			IResource resource = resource2;
			assertFalse("1.0: " + resource.getFullPath(), resource.isDerived());
		}

		// now set the root as derived
		try {
			root.setDerived(true, new NullProgressMonitor());
		} catch (CoreException e) {
			fail("2.0: " + root.getFullPath(), e);
		}

		// we can't mark the root as derived, so none of its children should be derived
		assertFalse("2.1: " + root.getFullPath(), root.isDerived(IResource.CHECK_ANCESTORS));
		assertFalse("2.2: " + project.getFullPath(), project.isDerived(IResource.CHECK_ANCESTORS));
		assertFalse("2.3: " + folder.getFullPath(), folder.isDerived(IResource.CHECK_ANCESTORS));
		assertFalse("2.4: " + file1.getFullPath(), file1.isDerived(IResource.CHECK_ANCESTORS));
		assertFalse("2.5: " + file2.getFullPath(), file2.isDerived(IResource.CHECK_ANCESTORS));

		// now set the project as derived
		try {
			project.setDerived(true, new NullProgressMonitor());
		} catch (CoreException e) {
			fail("3.0: " + project.getFullPath(), e);
		}

		// we can't mark a project as derived, so none of its children should be derived
		// even when CHECK_ANCESTORS is used
		assertFalse("3.0: " + project.getFullPath(), project.isDerived(IResource.CHECK_ANCESTORS));
		assertFalse("3.1: " + folder.getFullPath(), folder.isDerived(IResource.CHECK_ANCESTORS));
		assertFalse("3.2: " + file1.getFullPath(), file1.isDerived(IResource.CHECK_ANCESTORS));
		assertFalse("3.3: " + file2.getFullPath(), file2.isDerived(IResource.CHECK_ANCESTORS));

		// now set the folder as derived
		try {
			folder.setDerived(true, new NullProgressMonitor());
		} catch (CoreException e) {
			fail("4.0: " + folder.getFullPath(), e);
		}

		// first check if isDerived() returns valid values
		assertTrue("4.1: " + folder.getFullPath(), folder.isDerived());
		assertFalse("4.2: " + file1.getFullPath(), file1.isDerived());
		assertFalse("4.3: " + file2.getFullPath(), file2.isDerived());

		// check if isDerived(IResource.CHECK_ANCESTORS) returns valid values
		assertTrue("4.4: " + folder.getFullPath(), folder.isDerived(IResource.CHECK_ANCESTORS));
		assertTrue("4.5: " + file1.getFullPath(), file1.isDerived(IResource.CHECK_ANCESTORS));
		assertTrue("4.6: " + file2.getFullPath(), file2.isDerived(IResource.CHECK_ANCESTORS));

		// clear the values
		try {
			folder.setDerived(false, new NullProgressMonitor());
		} catch (CoreException e) {
			fail("6.0: " + folder.getFullPath(), e);
		}

		// values should be false again
		for (IResource resource2 : resources) {
			assertFalse("7.0: " + resource2.getFullPath(), resource2.isDerived());
		}
	}

	/**
	 * Performs black box testing of the following method: boolean
	 * equals(Object)
	 */
	public void testEquals() {
		Object[][] inputs = { interestingResources, interestingResources };
		new TestPerformer("IResourceTest.testEquals") {

			@Override
			public Object[] interestingOldState(Object[] args) throws Exception {
				return null;
			}

			@Override
			public Object invokeMethod(Object[] args, int count) throws Exception {
				IResource resource0 = (IResource) args[0];
				IResource resource1 = (IResource) args[1];
				return resource0.equals(resource1) ? Boolean.TRUE : Boolean.FALSE;
			}

			@Override
			public boolean shouldFail(Object[] args, int count) {
				return false;
			}

			@Override
			public boolean wasSuccess(Object[] args, Object result, Object[] oldState) throws Exception {
				IResource resource0 = (IResource) args[0];
				IResource resource1 = (IResource) args[1];
				boolean booleanResult = ((Boolean) result).booleanValue();
				boolean expectedResult = resource0.getFullPath().equals(resource1.getFullPath()) && resource0.getType() == resource1.getType() && resource0.getWorkspace().equals(resource1.getWorkspace());
				if (booleanResult) {
					assertEquals("hashCode should be equal if equals returns true", resource0.hashCode(),
							resource1.hashCode());
				}
				return booleanResult == expectedResult;
			}
		}.performTest(inputs);
	}

	/**
	 * Performs black box testing of the following method: boolean exists() */
	public void testExists() {
		Object[][] inputs = { interestingResources };
		new TestPerformer("IResourceTest.testExists") {

			@Override
			public Object[] interestingOldState(Object[] args) throws Exception {
				return null;
			}

			@Override
			public Object invokeMethod(Object[] args, int count) throws Exception {
				IResource resource = (IResource) args[0];
				return resource.exists() ? Boolean.TRUE : Boolean.FALSE;
			}

			@Override
			public boolean shouldFail(Object[] args, int count) {
				return false;
			}

			@Override
			public boolean wasSuccess(Object[] args, Object result, Object[] oldState) throws Exception {
				boolean booleanResult = ((Boolean) result).booleanValue();
				IResource resource = (IResource) args[0];
				return booleanResult != nonExistingResources.contains(resource);
			}
		}.performTest(inputs);
	}

	/**
	 * Performs black box testing of the following method: IPath getLocation() */
	public void testGetLocation() {
		Object[][] inputs = { interestingResources };
		new TestPerformer("IResourceTest.testGetLocation") {

			@Override
			public Object[] interestingOldState(Object[] args) {
				return null;
			}

			@Override
			public Object invokeMethod(Object[] args, int count) throws Exception {
				IResource resource = (IResource) args[0];
				return resource.getLocation();
			}

			@Override
			public boolean shouldFail(Object[] args, int count) {
				return false;
			}

			@Override
			public boolean wasSuccess(Object[] args, Object result, Object[] oldState) {
				IResource resource = (IResource) args[0];
				IPath resultPath = (IPath) result;
				if (resource.getType() == IResource.PROJECT) {
					if (!resource.exists()) {
						return resultPath == null;
					}
					return resultPath != null;
				}
				if (!resource.getProject().exists()) {
					return resultPath == null;
				}
				return resultPath != null;
			}
		}.performTest(inputs);
	}

	public void testGetModificationStamp() {
		// cleanup auto-created resources
		try {
			getWorkspace().getRoot().delete(true, getMonitor());
		} catch (CoreException e) {
			fail("0.0", e);
		}

		// setup
		IResource[] resources = buildResources(getWorkspace().getRoot(), new String[] {"/1/", "/1/1", "/1/2", "/1/3", "/2/", "/2/1"});
		final Map<IPath, Long> table = new HashMap<>(resources.length);

		for (IResource resource : resources) {
			if (resource.getType() != IResource.ROOT) {
				assertEquals("1.0." + resource.getFullPath(), IResource.NULL_STAMP, resource.getModificationStamp());
			}
		}

		// create the project(s). the resources should still have null
		// modification stamp
		IProject[] projects = getProjects(resources);
		IProject project;
		for (IProject project2 : projects) {
			project = project2;
			try {
				project.create(getMonitor());
			} catch (CoreException e) {
				fail("2.0." + project.getFullPath(), e);
			}
			assertEquals("2.1." + project.getFullPath(), IResource.NULL_STAMP, project.getModificationStamp());
		}

		// open the project(s) and create the resources. none should have a
		// null stamp anymore.
		for (IProject project2 : projects) {
			project = project2;
			assertEquals("3.1." + project.getFullPath(), IResource.NULL_STAMP, project.getModificationStamp());
			try {
				project.open(getMonitor());
			} catch (CoreException e) {
				fail("3.2", e);
			}
			assertNotEquals("3.3." + project.getFullPath(), IResource.NULL_STAMP, project.getModificationStamp());
			// cache the value for later use
			table.put(project.getFullPath(), Long.valueOf(project.getModificationStamp()));
		}
		for (IResource resource : resources) {
			if (resource.getType() != IResource.PROJECT) {
				assertEquals("3.4." + resource.getFullPath(), IResource.NULL_STAMP, resource.getModificationStamp());
				ensureExistsInWorkspace(resource, true);
				assertNotEquals("3.5." + resource.getFullPath(), IResource.NULL_STAMP, resource.getModificationStamp());
				// cache the value for later use
				table.put(resource.getFullPath(), Long.valueOf(resource.getModificationStamp()));
			}
		}

		// close the projects. now all resources should have a null stamp again
		for (IProject project2 : projects) {
			project = project2;
			try {
				project.close(getMonitor());
			} catch (CoreException e) {
				fail("4.0." + project.getFullPath(), e);
			}
		}
		for (IResource resource : resources) {
			if (resource.getType() != IResource.ROOT) {
				assertEquals("4.1." + resource.getFullPath(), IResource.NULL_STAMP, resource.getModificationStamp());
			}
		}

		// re-open the projects. all resources should have the same stamps
		for (IProject project2 : projects) {
			project = project2;
			try {
				project.open(getMonitor());
			} catch (CoreException e) {
				fail("5.0." + project.getFullPath(), e);
			}
		}
		for (IResource resource : resources) {
			if (resource.getType() != IResource.PROJECT) {
				Object v = table.get(resource.getFullPath());
				assertNotNull("5.1." + resource.getFullPath(), v);
				long old = ((Long) v).longValue();
				assertEquals("5.2." + resource.getFullPath(), old, resource.getModificationStamp());
			}
		}

		// touch all the resources. this will update the modification stamp
		final Map<IPath, Long> tempTable = new HashMap<>(resources.length);
		for (IResource resource : resources) {
			if (resource.getType() != IResource.ROOT) {
				try {
					resource.touch(getMonitor());
				} catch (CoreException e) {
					fail("6.2", e);
				}
				long stamp = resource.getModificationStamp();
				Object v = table.get(resource.getFullPath());
				assertNotNull("6.0." + resource.getFullPath(), v);
				long old = ((Long) v).longValue();
				assertNotEquals("6.1." + resource.getFullPath(), old, stamp);
				// cache for next time
				tempTable.put(resource.getFullPath(), Long.valueOf(stamp));
			}
		}
		table.clear();
		table.putAll(tempTable);

		// mark all resources as non-local. all non-local resources have a null
		// stamp
		try {
			getWorkspace().getRoot().setLocal(false, IResource.DEPTH_INFINITE, getMonitor());
		} catch (CoreException e) {
			fail("7.1", e);
		}
		IResourceVisitor visitor = resource -> {
			//projects and root are always local
			if (resource.getType() == IResource.ROOT || resource.getType() == IResource.PROJECT) {
				assertNotEquals("7.2" + resource.getFullPath(), IResource.NULL_STAMP, resource.getModificationStamp());
			} else {
				assertEquals("7.3." + resource.getFullPath(), IResource.NULL_STAMP, resource.getModificationStamp());
			}
			return true;
		};
		try {
			getWorkspace().getRoot().accept(visitor, IResource.DEPTH_INFINITE, false);
		} catch (CoreException e) {
			fail("7.4", e);
		}

		// mark all resources as local. none should have a null stamp and it
		// should be different than
		// the last one
		try {
			getWorkspace().getRoot().setLocal(true, IResource.DEPTH_INFINITE, getMonitor());
		} catch (CoreException e) {
			fail("8.1", e);
		}
		tempTable.clear();
		for (IResource resource : resources) {
			if (resource.getType() != IResource.ROOT) {
				long stamp = resource.getModificationStamp();
				assertNotEquals("8.2." + resource.getFullPath(), IResource.NULL_STAMP, stamp);
				Object v = table.get(resource.getFullPath());
				assertNotNull("8.3." + resource.getFullPath(), v);
				long old = ((Long) v).longValue();
				assertNotEquals("8.4." + resource.getFullPath(), IResource.NULL_STAMP, old);
				tempTable.put(resource.getFullPath(), Long.valueOf(stamp));
			}
		}
		table.clear();
		table.putAll(tempTable);
		//set local on resources that are already local, this should not
		// affect the modification stamp
		try {
			getWorkspace().getRoot().setLocal(true, IResource.DEPTH_INFINITE, getMonitor());
		} catch (CoreException e) {
			fail("9.1", e);
		}
		for (IResource resource : resources) {
			if (resource.getType() != IResource.ROOT) {
				long newStamp = resource.getModificationStamp();
				assertNotEquals("9.2." + resource.getFullPath(), IResource.NULL_STAMP, newStamp);
				Object v = table.get(resource.getFullPath());
				assertNotNull("9.3." + resource.getFullPath(), v);
				long oldStamp = ((Long) v).longValue();
				assertEquals("9.4." + resource.getFullPath(), oldStamp, newStamp);
			}
		}

		// delete all the resources so we can start over.
		try {
			getWorkspace().getRoot().delete(true, getMonitor());
		} catch (CoreException e) {
			fail("10.0", e);
		}

		// none of the resources exist yet so all the modification stamps
		// should be null
		for (IResource resource : resources) {
			if (resource.getType() != IResource.ROOT) {
				assertEquals("10.1" + resource.getFullPath(), IResource.NULL_STAMP, resource.getModificationStamp());
			}
		}

		// create all the resources (non-local) and ensure all stamps are null
		ensureExistsInWorkspace(resources, false);
		for (IResource resource : resources) {
			switch (resource.getType()) {
				case IResource.ROOT :
					break;
				case IResource.PROJECT :
					assertNotEquals("11.1." + resource.getFullPath(), IResource.NULL_STAMP,
							resource.getModificationStamp());
					break;
				default :
					assertEquals("11.2." + resource.getFullPath(), IResource.NULL_STAMP, resource.getModificationStamp());
					break;
			}
		}
		// now make all resources local and re-check stamps
		try {
			getWorkspace().getRoot().setLocal(true, IResource.DEPTH_INFINITE, getMonitor());
		} catch (CoreException e) {
			fail("12.0", e);
		}
		visitor = resource -> {
			if (resource.getType() != IResource.ROOT) {
				assertNotEquals("12.1." + resource.getFullPath(), IResource.NULL_STAMP,
						resource.getModificationStamp());
			}
			return true;
		};
		try {
			getWorkspace().getRoot().accept(visitor, IResource.DEPTH_INFINITE, false);
		} catch (CoreException e) {
			fail("12.2", e);
		}
	}

	/**
	 * Tests that, having replaced a file, the modification stamp
	 * has changed.
	 */
	public void testGetModificationStampAfterReplace() throws Exception {
		final IFile file = getWorkspace().getRoot().getFile(new Path("/project/f"));

		create(file, true);
		long modificationStamp = file.getModificationStamp();
		assertNotEquals("1.1", modificationStamp, IResource.NULL_STAMP);

		// Remove and re-create the file in a workspace operation
		getWorkspace().run((IWorkspaceRunnable) monitor -> {
			file.delete(false, getMonitor());
			create(file, true);
		}, getMonitor());

		assertNotEquals("1.0", modificationStamp, file.getModificationStamp());
	}

	/**
	 * Performs black box testing of the following method: IPath
	 * getRawLocation()
	 */
	public void testGetRawLocation() {
		IProject project = getWorkspace().getRoot().getProject("Project");
		IFolder topFolder = project.getFolder("TopFolder");
		IFile topFile = project.getFile("TopFile");
		IFile deepFile = topFolder.getFile("DeepFile");
		IResource[] allResources = { project, topFolder, topFile, deepFile };

		//non existing project
		assertNull("2.0", project.getRawLocation());

		//resources in non-existing project
		assertNull("2.1", topFolder.getRawLocation());
		assertNull("2.2", topFile.getRawLocation());
		assertNull("2.3", deepFile.getRawLocation());

		ensureExistsInWorkspace(allResources, true);
		//open project
		assertNull("2.0", project.getRawLocation());
		//resources in open project
		final IPath workspaceLocation = getWorkspace().getRoot().getLocation();
		assertEquals("2.1", workspaceLocation.append(topFolder.getFullPath()), topFolder.getRawLocation());
		assertEquals("2.2", workspaceLocation.append(topFile.getFullPath()), topFile.getRawLocation());
		assertEquals("2.3", workspaceLocation.append(deepFile.getFullPath()), deepFile.getRawLocation());

		try {
			project.close(getMonitor());
		} catch (CoreException e) {
			fail("1.99", e);
		}
		//closed project
		assertNull("3.0", project.getRawLocation());
		//resource in closed project
		assertEquals("3.1", workspaceLocation.append(topFolder.getFullPath()), topFolder.getRawLocation());
		assertEquals("3.2", workspaceLocation.append(topFile.getFullPath()), topFile.getRawLocation());
		assertEquals("3.3", workspaceLocation.append(deepFile.getFullPath()), deepFile.getRawLocation());

		IPath projectLocation = getRandomLocation();
		IPath folderLocation = getRandomLocation();
		IPath fileLocation = getRandomLocation();
		IPath variableLocation = getRandomLocation();
		final String variableName = "IResourceTest_VariableName";
		IPathVariableManager varMan = getWorkspace().getPathVariableManager();
		try {
			varMan.setValue(variableName, variableLocation);
			project.open(getMonitor());
			IProjectDescription description = project.getDescription();
			description.setLocation(projectLocation);
			project.move(description, IResource.NONE, getMonitor());

			//open project not in default location
			assertEquals("4.0", projectLocation, project.getRawLocation());
			//resource in open project not in default location
			assertEquals("4.1", projectLocation.append(topFolder.getProjectRelativePath()), topFolder.getRawLocation());
			assertEquals("4.2", projectLocation.append(topFile.getProjectRelativePath()), topFile.getRawLocation());
			assertEquals("4.3", projectLocation.append(deepFile.getProjectRelativePath()), deepFile.getRawLocation());

			project.close(getMonitor());

			//closed project not in default location
			assertEquals("5.0", projectLocation, project.getRawLocation());
			//resource in closed project not in default location
			assertEquals("5.1", projectLocation.append(topFolder.getProjectRelativePath()), topFolder.getRawLocation());
			assertEquals("5.2", projectLocation.append(topFile.getProjectRelativePath()), topFile.getRawLocation());
			assertEquals("5.3", projectLocation.append(deepFile.getProjectRelativePath()), deepFile.getRawLocation());

			project.open(getMonitor());
			ensureDoesNotExistInWorkspace(topFolder);
			ensureDoesNotExistInWorkspace(topFile);
			createFileInFileSystem(EFS.getFileSystem(EFS.SCHEME_FILE).getStore(fileLocation));
			folderLocation.toFile().mkdirs();
			topFolder.createLink(folderLocation, IResource.NONE, getMonitor());
			topFile.createLink(fileLocation, IResource.NONE, getMonitor());
			ensureExistsInWorkspace(deepFile, true);

			//linked file
			assertEquals("6.0", fileLocation, topFile.getRawLocation());
			//linked folder
			assertEquals("6.1", folderLocation, topFolder.getRawLocation());
			//resource below linked folder
			assertEquals("6.2", folderLocation.append(deepFile.getName()), deepFile.getRawLocation());

			project.close(getMonitor());

			//linked file in closed project (should default to project
			// location)
			assertEquals("7.0", projectLocation.append(topFile.getProjectRelativePath()), topFile.getRawLocation());
			//linked folder in closed project
			assertEquals("7.1", projectLocation.append(topFolder.getProjectRelativePath()), topFolder.getRawLocation());
			//resource below linked folder in closed project
			assertEquals("7.3", projectLocation.append(deepFile.getProjectRelativePath()), deepFile.getRawLocation());

			project.open(getMonitor());
			IPath variableFolderLocation = new Path(variableName).append("/VarFolderName");
			IPath variableFileLocation = new Path(variableName).append("/VarFileName");
			ensureDoesNotExistInWorkspace(topFolder);
			ensureDoesNotExistInWorkspace(topFile);
			createFileInFileSystem(EFS.getFileSystem(EFS.SCHEME_FILE).getStore(varMan.resolvePath(variableFileLocation)));
			varMan.resolvePath(variableFolderLocation).toFile().mkdirs();
			topFolder.createLink(variableFolderLocation, IResource.NONE, getMonitor());
			topFile.createLink(variableFileLocation, IResource.NONE, getMonitor());
			ensureExistsInWorkspace(deepFile, true);

			//linked file with variable
			assertEquals("8.0", variableFileLocation, topFile.getRawLocation());
			//linked folder with variable
			assertEquals("8.1", variableFolderLocation, topFolder.getRawLocation());
			//resource below linked folder with variable
			assertEquals("8.3", varMan.resolvePath(variableFolderLocation).append(deepFile.getName()), deepFile.getRawLocation());

			project.close(getMonitor());

			//linked file in closed project with variable
			assertEquals("9.0", projectLocation.append(topFile.getProjectRelativePath()), topFile.getRawLocation());
			//linked folder in closed project with variable
			assertEquals("9.1", projectLocation.append(topFolder.getProjectRelativePath()), topFolder.getRawLocation());
			//resource below linked folder in closed project with variable
			assertEquals("9.3", projectLocation.append(deepFile.getProjectRelativePath()), deepFile.getRawLocation());
		} catch (CoreException e) {
			fail("99.99", e);
		} finally {
			try {
				getWorkspace().getRoot().delete(IResource.FORCE | IResource.ALWAYS_DELETE_PROJECT_CONTENT, getMonitor());
				varMan.setValue(variableName, null);
			} catch (CoreException e) {
			}
			Workspace.clear(projectLocation.toFile());
			Workspace.clear(folderLocation.toFile());
			Workspace.clear(fileLocation.toFile());
			Workspace.clear(variableLocation.toFile());
		}
	}

	public void testIsConflicting() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("Project");
		IFolder a = project.getFolder("a");
		IFolder b = project.getFolder("b");

		ensureExistsInWorkspace(new IResource[] { project, a, b }, true);

		ISchedulingRule multi = MultiRule.combine(a, b);

		assertEquals(false, a.isConflicting(b));
		assertEquals(false, b.isConflicting(a));

		assertEquals(true, a.isConflicting(multi));
		assertEquals(true, multi.isConflicting(a));

		assertEquals(true, b.isConflicting(multi));
		assertEquals(true, multi.isConflicting(b));

		project.delete(true, getMonitor());
	}

	public void testIsConflicting2() throws CoreException {
		final IProject project = getWorkspace().getRoot().getProject("Project");
		ensureExistsInWorkspace(project, true);

		ISchedulingRule wrapper = new ISchedulingRule() {
			@Override
			public boolean isConflicting(ISchedulingRule rule) {
				return this == rule || project.isConflicting(rule);
			}

			@Override
			public boolean contains(ISchedulingRule rule) {
				return this == rule || project.contains(rule);
			}
		};
		ISchedulingRule multi = MultiRule.combine(wrapper, new ISchedulingRule() {
			@Override
			public boolean isConflicting(ISchedulingRule rule) {
				return this == rule;
			}

			@Override
			public boolean contains(ISchedulingRule rule) {
				return this == rule;
			}
		});

		assertEquals(false, project.isConflicting(wrapper));
		assertEquals(false, project.isConflicting(multi));
		assertEquals(true, wrapper.isConflicting(project));
		assertEquals(true, multi.isConflicting(project));

		project.delete(true, getMonitor());
	}

	/**
	 * This method tests the IResource.isSynchronized() operation */
	public void testIsSynchronized() {
		//don't need auto-created resources
		try {
			getWorkspace().getRoot().delete(true, true, getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}

		interestingResources = buildInterestingResources();
		Object[][] inputs = { interestingResources, interestingResources, interestingStates(), interestingDepths() };
		new TestPerformer("IResourceTest.testRefreshLocal") {

			@Override
			public void cleanUp(Object[] args, int count) {
				cleanUpAfterRefreshTest(args);
			}

			@Override
			public Object invokeMethod(Object[] args, int count) {
				IResource receiver = (IResource) args[0];
				IResource target = (IResource) args[1];
				int state = ((Integer) args[2]).intValue();
				int depth = ((Integer) args[3]).intValue();
				if (!makesSense(receiver, target, state, depth)) {
					return null;
				}
				setupBeforeState(receiver, target, state, depth, false);
				boolean result = receiver.isSynchronized(depth);
				return result ? Boolean.TRUE : Boolean.FALSE;
			}

			@Override
			public boolean shouldFail(Object[] args, int count) {
				return false;
			}

			@Override
			public boolean wasSuccess(Object[] args, Object result, Object[] oldState) {
				if (result == null)
				 {
					return true; //combination didn't make sense
				}
				boolean bResult = ((Boolean) result).booleanValue();
				IResource receiver = (IResource) args[0];
				IResource target = (IResource) args[1];
				int state = ((Integer) args[2]).intValue();
				int depth = ((Integer) args[3]).intValue();

				//only !synchronized if target is same as or child of receiver
				if (!(receiver.equals(target) || hasParent(target, receiver, depth))) {
					return bResult;
				}
				switch (state) {
					case S_UNCHANGED :
					case S_DOES_NOT_EXIST :
						//these cases correspond to being in sync
						return bResult;
					case S_WORKSPACE_ONLY :
					case S_FILESYSTEM_ONLY :
					case S_CHANGED :
					case S_FOLDER_TO_FILE :
					case S_FILE_TO_FOLDER :
						//these cases correspond to being out of sync
						return !bResult;
					default :
						//shouldn't be possible
						return false;
				}
			}
		}.performTest(inputs);
	}

	/**
	 * Performs black box testing of the following method: void move(IPath,
	 * boolean, IProgressMonitor)
	 */
	public void testMove() {
		Object[][] inputs = { interestingResources, interestingPaths, TRUE_AND_FALSE, PROGRESS_MONITORS };
		new TestPerformer("IResourceTest.testMove") {

			@Override
			public Object[] interestingOldState(Object[] args) {
				return null;
			}

			@Override
			public Object invokeMethod(Object[] args, int count) throws Exception {
				IResource resource = (IResource) args[0];
				IPath destination = (IPath) args[1];
				Boolean force = (Boolean) args[2];
				IProgressMonitor monitor = (IProgressMonitor) args[3];
				if (monitor instanceof FussyProgressMonitor) {
					((FussyProgressMonitor) monitor).prepare();
				}
				resource.move(destination, force.booleanValue(), monitor);
				if (monitor instanceof FussyProgressMonitor) {
					((FussyProgressMonitor) monitor).sanityCheck();
				}
				return null;
			}

			@Override
			public boolean shouldFail(Object[] args, int count) {
				IResource resource = (IResource) args[0];
				IPath destination = (IPath) args[1];
				//			Boolean force = (Boolean) args[2];
				if (!resource.isAccessible()) {
					return true;
				}
				if (isProject(resource)) {
					if (destination.isAbsolute() ? destination.segmentCount() != 2 : destination.segmentCount() != 1) {
						return true;
					}
					return !getWorkspace().validateName(destination.segment(0), IResource.PROJECT).isOK();
				}
				File destinationParent = destination.isAbsolute() ? destination.removeLastSegments(1).toFile() : resource.getLocation().removeLastSegments(1).append(destination.removeLastSegments(1)).toFile();
				File destinationFile = destination.isAbsolute() ? destination.toFile() : resource.getLocation().removeLastSegments(1).append(destination).removeTrailingSeparator().toFile();
				return !destinationParent.exists() || !destinationParent.isDirectory() || destinationFile.exists() || destinationFile.toString().startsWith(resource.getLocation().toFile().toString());
			}

			@Override
			public boolean wasSuccess(Object[] args, Object result, Object[] oldState) {
				return true;
			}
		}.performTest(inputs);
	}

	public void testMultiCreation() {

		final IProject project = getWorkspace().getRoot().getProject("bar");
		final IResource[] resources = buildResources(project, new String[] {"a/", "a/b"});
		// create the project. Have to do this outside the resource operation
		// to ensure that things are setup properly (e.g., add the delta
		// listener)
		try {
			project.create(null);
			project.open(null);
		} catch (CoreException e) {
			fail("1.2", e);
		}
		assertExistsInWorkspace("1.3", project);
		// define an operation which will create a bunch of resources including
		// a project.
		for (IResource resource : resources) {
			try {
				switch (resource.getType()) {
					case IResource.FILE :
						((IFile) resource).create(null, false, getMonitor());
						break;
					case IResource.FOLDER :
						((IFolder) resource).create(false, true, getMonitor());
						break;
					case IResource.PROJECT :
						((IProject) resource).create(getMonitor());
						break;
				}
			} catch (CoreException e) {
				fail("1.4: " + resource.getFullPath(), e);
			}
		}
		assertExistsInWorkspace("1.5", resources);
		try {
			project.delete(true, false, getMonitor());
		} catch (CoreException e) {
			fail("2.0", e);
		}
	}

	/**
	 * Test that opening an closing a project does not affect the description
	 * file.
	 */
	public void testProjectDescriptionFileModification() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("P1");
		IFile file = project.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		project.create(null);
		project.open(null);
		long stamp = file.getModificationStamp();
		project.close(null);
		project.open(null);
		assertEquals(stamp, file.getModificationStamp());
	}

	/**
	 * Tests IResource#getPersistentProperties and IResource#getSessionProperties
	 */
	public void testProperties() throws CoreException {
		QualifiedName qn1 = new QualifiedName("package", "property1");
		QualifiedName qn2 = new QualifiedName("package", "property2");

		IProject project = getWorkspace().getRoot().getProject("P1");
		IProject project2 = getWorkspace().getRoot().getProject("P2");
		project.create(null);
		project.open(null);
		project.setPersistentProperty(qn1, "value1");
		project.setPersistentProperty(qn2, "value2");
		project.setSessionProperty(qn1, "value1");
		project.setSessionProperty(qn2, "value2");

		assertEquals("value1", project.getPersistentProperty(qn1));
		assertEquals("value2", project.getPersistentProperty(qn2));
		assertEquals("value1", project.getSessionProperty(qn1));
		assertEquals("value2", project.getSessionProperty(qn2));

		Map<QualifiedName, ?> props = project.getPersistentProperties();
		assertEquals(2, props.size());
		assertEquals("value1", props.get(qn1));
		assertEquals("value2", props.get(qn2));

		props = project.getSessionProperties();
		// Don't check the size, because other plugins (like team) may add
		// a property depending on if they are present or not
		assertEquals("value1", props.get(qn1));
		assertEquals("value2", props.get(qn2));

		project.setPersistentProperty(qn1, null);
		project.setSessionProperty(qn1, null);

		props = project.getPersistentProperties();
		assertEquals(1, props.size());
		assertNull(props.get(qn1));
		assertEquals("value2", props.get(qn2));

		props = project.getSessionProperties();
		assertNull(props.get(qn1));
		assertEquals("value2", props.get(qn2));

		// Copy
		project.copy(project2.getFullPath(), true, null);

		// Persistent properties go with the copy
		props = project2.getPersistentProperties();
		assertEquals(1, props.size());
		assertNull(props.get(qn1));
		assertEquals("value2", props.get(qn2));

		// Session properties don't
		props = project2.getSessionProperties();
		// Don't check size (see above)
		assertNull(props.get(qn1));
		assertNull(props.get(qn2));

		// Test persistence
		project.close(null);
		project.open(null);

		// Make sure they are really persistent
		props = project.getPersistentProperties();
		assertEquals(1, props.size());
		assertNull(props.get(qn1));
		assertEquals("value2", props.get(qn2));

		// Make sure they don't persist
		props = project.getSessionProperties();
		// Don't check size (see above)
		assertNull(props.get(qn1));
		assertNull(props.get(qn2));

	}

	/**
	 * Tests IResource.isReadOnly and setReadOnly
	 * @deprecated This test is for deprecated API
	 */
	@Deprecated
	public void testReadOnly() {
		// We need to know whether or not we can unset the read-only flag
		// in order to perform this test.
		if (!isReadOnlySupported()) {
			return;
		}
		IProject project = getWorkspace().getRoot().getProject(getUniqueString());
		IFile file = project.getFile("target");
		try {
			project.create(getMonitor());
			project.open(getMonitor());
			file.create(getRandomContents(), true, getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}

		// file
		assertFalse("1.0", file.isReadOnly());
		file.setReadOnly(true);
		assertTrue("1.2", file.isReadOnly());
		file.setReadOnly(false);
		assertFalse("1.4", file.isReadOnly());

		// folder
		assertFalse("2.0", project.isReadOnly());
		project.setReadOnly(true);
		assertTrue("2.2", project.isReadOnly());
		project.setReadOnly(false);
		assertFalse("2.4", project.isReadOnly());

		/* remove trash */
		try {
			project.delete(true, getMonitor());
		} catch (CoreException e) {
			fail("3.0", e);
		}
	}

	/**
	 * This method tests the IResource.refreshLocal() operation */
	public void testRefreshLocal() {
		//don't need auto-created resources
		try {
			getWorkspace().getRoot().delete(true, true, getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}

		interestingResources = buildInterestingResources();
		Object[][] inputs = { interestingResources, interestingResources, interestingStates(), interestingDepths() };
		new TestPerformer("IResourceTest.testRefreshLocal") {

			@Override
			public void cleanUp(Object[] args, int count) {
				cleanUpAfterRefreshTest(args);
			}

			@Override
			public Object invokeMethod(Object[] args, int count) throws CoreException {
				IResource receiver = (IResource) args[0];
				IResource target = (IResource) args[1];
				int state = ((Integer) args[2]).intValue();
				int depth = ((Integer) args[3]).intValue();
				if (!makesSense(receiver, target, state, depth)) {
					return null;
				}
				setupBeforeState(receiver, target, state, depth, true);
				receiver.refreshLocal(depth, getMonitor());
				return Boolean.TRUE;
			}

			@Override
			public boolean shouldFail(Object[] args, int count) {
				return false;
			}

			@Override
			public boolean wasSuccess(Object[] args, Object result, Object[] oldState) {
				if (result == null)
				 {
					return true; //permutation didn't make sense
				}
				IResource receiver = (IResource) args[0];
				IResource target = (IResource) args[1];
				int state = ((Integer) args[2]).intValue();
				int depth = ((Integer) args[3]).intValue();
				return checkAfterState(receiver, target, state, depth);
			}
		}.performTest(inputs);
	}

	public void testRefreshLocalWithDepth() {
		IProject project = getWorkspace().getRoot().getProject("Project");
		IFolder folder = project.getFolder("Folder");
		try {
			project.create(getMonitor());
			project.open(getMonitor());
			folder.create(true, true, getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}

		String[] hierarchy = {"Folder/", "Folder/Folder/", "Folder/Folder/Folder/", "Folder/Folder/Folder/Folder/"};
		IResource[] resources = buildResources(folder, hierarchy);
		ensureExistsInFileSystem(resources);
		assertDoesNotExistInWorkspace("3.0", resources);

		try {
			folder.refreshLocal(IResource.DEPTH_ONE, getMonitor());
		} catch (CoreException e) {
			fail("4.0", e);
		}

		assertExistsInWorkspace("5.0", folder.getFolder("Folder"));
		assertDoesNotExistInWorkspace("5.1", folder.getFolder("Folder/Folder"));
	}

	/**
	 * This method tests the IResource.refreshLocal() operation */
	public void testRefreshWithMissingParent() throws CoreException {
		/**
		 * Add a folder and file to the file system. Call refreshLocal on the
		 * file, when neither of them exist in the workspace.
		 */
		IProject project1 = getWorkspace().getRoot().getProject("Project");
		project1.create(getMonitor());
		project1.open(getMonitor());

		IFolder folder = project1.getFolder("Folder");
		IFile file = folder.getFile("File");

		ensureExistsInFileSystem(file);

		file.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
	}

	/**
	 * This method tests the IResource.revertModificationStamp() operation */
	public void testRevertModificationStamp() {
		//revert all existing resources
		try {
			getWorkspace().getRoot().accept(resource -> {
				if (!resource.isAccessible()) {
					return false;
				}
				long oldStamp = resource.getModificationStamp();
				resource.touch(null);
				long newStamp = resource.getModificationStamp();
				if (resource.getType() == IResource.ROOT) {
					assertEquals("1.0." + resource.getFullPath(), oldStamp, newStamp);
				} else {
					assertNotEquals("1.0." + resource.getFullPath(), oldStamp, newStamp);
				}
				resource.revertModificationStamp(oldStamp);
				assertEquals("1.1." + resource.getFullPath(), oldStamp, resource.getModificationStamp());
				return true;
			});
		} catch (CoreException e) {
			fail("1.99", e);
		}
		//illegal values
		IResource[] resources = buildInterestingResources();
		long[] illegal = { -1, -10, -100 };
		for (IResource resource : resources) {
			if (!resource.isAccessible()) {
				continue;
			}
			for (long element : illegal) {
				assertThrows(RuntimeException.class, () -> resource.revertModificationStamp(element));
			}
		}
		//should fail for non-existent resources
		try {
			getWorkspace().getRoot().delete(IResource.ALWAYS_DELETE_PROJECT_CONTENT, getMonitor());
		} catch (CoreException e) {
			fail("3.99", e);
		}
		for (IResource resource : resources) {
			try {
				resource.revertModificationStamp(1);
				if (resource.getType() != IResource.ROOT) {
					fail("4." + resource.getFullPath());
				}
			} catch (CoreException e) {
				//should fail except for root
				if (resource.getType() == IResource.ROOT) {
					fail("4.99");
				}
			}
		}
	}

	/**
	 * This method tests the IResource.setLocalTimeStamp() operation */
	public void testSetLocalTimeStamp() {
		//don't need auto-created resources
		try {
			getWorkspace().getRoot().delete(true, true, getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}

		interestingResources = buildInterestingResources();
		Long[] interestingTimes = { Long.valueOf(-1), Long.valueOf(System.currentTimeMillis() - 1000),
				Long.valueOf(System.currentTimeMillis() - 100), Long.valueOf(System.currentTimeMillis()),
				Long.valueOf(Integer.MAX_VALUE * 512L) };
		Object[][] inputs = { interestingResources, interestingTimes };
		new TestPerformer("IResourceTest.testRefreshLocal") {

			@Override
			public void cleanUp(Object[] args, int count) {
			}

			@Override
			public Object invokeMethod(Object[] args, int count) throws CoreException {
				IResource receiver = (IResource) args[0];
				long time = ((Long) args[1]).longValue();
				long actual = receiver.setLocalTimeStamp(time);
				return Long.valueOf(actual);
			}

			@Override
			public boolean shouldFail(Object[] args, int count) {
				long time = ((Long) args[1]).longValue();
				return time < 0;
			}

			@Override
			public boolean wasSuccess(Object[] args, Object result, Object[] oldState) {
				IResource receiver = (IResource) args[0];
				if (receiver.getType() == IResource.ROOT) {
					return true;
				}
				long time = ((Long) args[1]).longValue();
				long actual = ((Long) result).longValue();
				if (actual != receiver.getLocalTimeStamp()) {
					return false;
				}
				if (Math.abs(actual - time) > 2000) {
					return false;
				}
				return true;
			}
		}.performTest(inputs);
	}

	/**
	 * Performs black box testing of the following methods:
	 * isTeamPrivateMember() and setTeamPrivateMember(boolean)
	 */
	public void testTeamPrivateMember() {
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject("Project");
		IFolder folder = project.getFolder("folder");
		IFile file = folder.getFile("target");
		try {
			project.create(getMonitor());
			project.open(getMonitor());
			folder.create(true, true, getMonitor());
			file.create(getRandomContents(), true, getMonitor());

		} catch (CoreException e) {
			fail("1.0", e);
		}

		// all resources have independent team private member flag
		// all non-TPM by default; check each type
		try {

			// root - cannot be made team private member
			assertFalse("2.1.1", root.isTeamPrivateMember());
			assertFalse("2.1.2", project.isTeamPrivateMember());
			assertFalse("2.1.3", folder.isTeamPrivateMember());
			assertFalse("2.1.4", file.isTeamPrivateMember());
			root.setTeamPrivateMember(true);
			assertFalse("2.2.1", root.isTeamPrivateMember());
			assertFalse("2.2.2", project.isTeamPrivateMember());
			assertFalse("2.2.3", folder.isTeamPrivateMember());
			assertFalse("2.2.4", file.isTeamPrivateMember());
			root.setTeamPrivateMember(false);
			assertFalse("2.3.1", root.isTeamPrivateMember());
			assertFalse("2.3.2", project.isTeamPrivateMember());
			assertFalse("2.3.3", folder.isTeamPrivateMember());
			assertFalse("2.3.4", file.isTeamPrivateMember());

			// project - cannot be made team private member
			project.setTeamPrivateMember(true);
			assertFalse("3.1.1", root.isTeamPrivateMember());
			assertFalse("3.1.2", project.isTeamPrivateMember());
			assertFalse("3.1.3", folder.isTeamPrivateMember());
			assertFalse("3.1.4", file.isTeamPrivateMember());
			project.setTeamPrivateMember(false);
			assertFalse("3.2.1", root.isTeamPrivateMember());
			assertFalse("3.2.2", project.isTeamPrivateMember());
			assertFalse("3.2.3", folder.isTeamPrivateMember());
			assertFalse("3.2.4", file.isTeamPrivateMember());

			// folder
			folder.setTeamPrivateMember(true);
			assertFalse("4.1.1", root.isTeamPrivateMember());
			assertFalse("4.1.2", project.isTeamPrivateMember());
			assertTrue("4.1.3", folder.isTeamPrivateMember());
			assertFalse("4.1.4", file.isTeamPrivateMember());
			folder.setTeamPrivateMember(false);
			assertFalse("4.2.1", root.isTeamPrivateMember());
			assertFalse("4.2.2", project.isTeamPrivateMember());
			assertFalse("4.2.3", folder.isTeamPrivateMember());
			assertFalse("4.2.4", file.isTeamPrivateMember());

			// file
			file.setTeamPrivateMember(true);
			assertFalse("5.1.1", root.isTeamPrivateMember());
			assertFalse("5.1.2", project.isTeamPrivateMember());
			assertFalse("5.1.3", folder.isTeamPrivateMember());
			assertTrue("5.1.4", file.isTeamPrivateMember());
			file.setTeamPrivateMember(false);
			assertFalse("5.2.1", root.isTeamPrivateMember());
			assertFalse("5.2.2", project.isTeamPrivateMember());
			assertFalse("5.2.3", folder.isTeamPrivateMember());
			assertFalse("5.2.4", file.isTeamPrivateMember());

		} catch (CoreException e) {
			fail("6.0", e);
		}

		/* remove trash */
		try {
			project.delete(true, getMonitor());
		} catch (CoreException e) {
			fail("7.0", e);
		}

		// isTeamPrivateMember should return false when resource does not exist
		assertFalse("8.1", project.isTeamPrivateMember());
		assertFalse("8.2", folder.isTeamPrivateMember());
		assertFalse("8.3", file.isTeamPrivateMember());

		// setTeamPrivateMember should fail when resource does not exist
		assertThrows(CoreException.class, () -> project.setTeamPrivateMember(false));
		assertThrows(CoreException.class, () -> folder.setTeamPrivateMember(false));
		assertThrows(CoreException.class, () -> file.setTeamPrivateMember(false));
	}

	// https://bugs.eclipse.org/461838
	public void testAcceptProxyVisitorAlphabetic() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("P");
		IFolder settings = project.getFolder(".settings");
		IFile prefs = settings.getFile("org.eclipse.core.resources.prefs");
		IFolder a = project.getFolder("a");
		IFile a1 = a.getFile("a1.txt");
		IFile a2 = a.getFile("a2.txt");
		IFolder b = a.getFolder("b");
		IFile b1 = b.getFile("b1.txt");
		IFile b2 = b.getFile("B2.txt");

		ensureExistsInWorkspace(new IResource[] {project, settings, prefs, a, a1, a2, b, b1, b2}, true);

		final List<IResource> actualOrder = new ArrayList<>();
		IResourceProxyVisitor visitor = proxy -> {
			actualOrder.add(proxy.requestResource());
			return true;
		};

		project.accept(visitor, IResource.DEPTH_INFINITE, IResource.NONE);

		List<IResource> expectedOrder = Arrays.asList(project, project.getFile(".project"),  settings, prefs, a, a1, a2, b, b2, b1);
		assertEquals("1.0", expectedOrder.toString(), actualOrder.toString());
	}
}
