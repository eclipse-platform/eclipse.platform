/*******************************************************************************
 * Copyright (c) 2000, 2025 IBM Corporation and others.
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
 *     Alexander Kurtakov <akurtako@redhat.com> - Bug 459343
 *     Sergey Prigogin (Google) - [462440] IFile#getContents methods should specify the status codes for its exceptions
 *******************************************************************************/
package org.eclipse.core.tests.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertDoesNotExistInFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertDoesNotExistInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertExistsInFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertExistsInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInputStream;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomContentsStream;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomString;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.ensureOutOfSync;
import static org.eclipse.core.tests.resources.ResourceTestUtil.removeFromFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.removeFromWorkspace;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.core.internal.resources.ResourceException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Platform.OS;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.tests.harness.FussyProgressMonitor;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(WorkspaceResetExtension.class)
public class IFileTest {

	//name of files according to sync category
	public static final String DOES_NOT_EXIST = "DoesNotExistFile";

	public static final String EXISTING = "ExistingFile";
	public static final String LOCAL_ONLY = "LocalOnlyFile";
	public static final String OUT_OF_SYNC = "OutOfSyncFile";
	//	protected static final IProgressMonitor[] PROGRESS_MONITORS = new IProgressMonitor[] {new FussyProgressMonitor(), new CancelingProgressMonitor(), null};
	protected static final IProgressMonitor[] PROGRESS_MONITORS = new IProgressMonitor[] {new FussyProgressMonitor(), null};
	protected static final Boolean[] TRUE_AND_FALSE = new Boolean[] {Boolean.TRUE, Boolean.FALSE};
	public static final String WORKSPACE_ONLY = "WorkspaceOnlyFile";
	public static final String EXISTING_HIDDEN = ".ExistingFileHidden";
	public static final String LOCAL_ONLY_HIDDEN = ".LocalOnlyFileHidden";
	public static final String WORKSPACE_ONLY_HIDDEN = ".WorkspaceOnlyFileHidden";

	ArrayList<IFile> allFiles = new ArrayList<>();

	IProject[] projects = null;

	/**
	 * Returns true if the given container exists, and is open
	 * if applicable.
	 */
	public boolean existsAndOpen(IContainer container) {
		if (!container.exists()) {
			return false;
		}
		if (container instanceof IFolder) {
			return true;
		}
		if (container instanceof IProject project) {
			return project.isOpen();
		}
		throw new IllegalArgumentException("Unhandled container type: " + container);
	}

	/**
	 * This method creates the necessary resources
	 * for the FileTests.  The interesting files are
	 * placed in ArrayLists that are members of the class.
	 */
	protected void generateInterestingFiles() throws CoreException {
		IProject[] interestingProjects = interestingProjects();
		for (IProject project : interestingProjects) {
			//file in project
			generateInterestingFiles(project);

			//file in non-existent folder
			generateInterestingFiles(project.getFolder("NonExistentFolder"));

			//file in existent folder
			if (project.exists() && project.isOpen()) {
				IFolder folder = project.getFolder("ExistingFolder");
				folder.create(true, true, createTestMonitor());
				generateInterestingFiles(folder);
			}
		}
	}

	/**
	 * Creates some interesting files in the specified container.
	 * Adds these files to the appropriate member ArrayLists.
	 * Conditions on these files (out of sync, workspace only, etc)
	 * will be ensured by refreshFiles
	 */
	public void generateInterestingFiles(IContainer container) {
		//non-existent file
		allFiles.add(container.getFile(IPath.fromOSString(DOES_NOT_EXIST)));

		//exists in file system only
		allFiles.add(container.getFile(IPath.fromOSString(LOCAL_ONLY)));
		allFiles.add(container.getFile(IPath.fromOSString(LOCAL_ONLY_HIDDEN))); // FileNotFoundException on testCreate()

		if (existsAndOpen(container)) {

			//existing file
			allFiles.add(container.getFile(IPath.fromOSString(EXISTING)));
			allFiles.add(container.getFile(IPath.fromOSString(EXISTING_HIDDEN)));

			//exists in workspace only
			allFiles.add(container.getFile(IPath.fromOSString(WORKSPACE_ONLY)));
			allFiles.add(container.getFile(IPath.fromOSString(WORKSPACE_ONLY_HIDDEN)));

			//exists in both but is out of sync
			allFiles.add(container.getFile(IPath.fromOSString(OUT_OF_SYNC)));
		}
	}

	/**
	 * Returns some interesting files.  These files are created
	 * during setup.
	 */
	public IFile[] interestingFiles() throws Exception {
		refreshFiles();
		IFile[] result = new IFile[allFiles.size()];
		allFiles.toArray(result);
		return result;
	}

	/**
	 * Creates and returns some interesting projects
	 */
	public IProject[] interestingProjects() throws CoreException {
		if (projects == null) {
			projects = new IProject[3];

			//open project
			IProject openProject = getWorkspace().getRoot().getProject("OpenProject");
			openProject.create(createTestMonitor());
			openProject.open(createTestMonitor());
			projects[0] = openProject;

			//closed project
			IProject closedProject = getWorkspace().getRoot().getProject("ClosedProject");
			closedProject.create(createTestMonitor());
			projects[1] = closedProject;

			//non-existent project
			projects[2] = getWorkspace().getRoot().getProject("NonExistentProject");
		}

		return projects;
	}

	/**
	 * Returns some interesting input streams
	 */
	public InputStream[] interestingStreams() {
		ArrayList<InputStream> streams = new ArrayList<>();

		//empty stream
		ByteArrayInputStream bis = new ByteArrayInputStream(new byte[0]);
		streams.add(bis);

		// random content
		streams.add(createRandomContentsStream());

		//large stream
		bis = new ByteArrayInputStream(new byte[10000]);
		streams.add(bis);

		InputStream[] results = new InputStream[streams.size()];
		streams.toArray(results);
		return results;
	}

	/**
	 * Returns true if the given file is out of sync from the
	 * local file system.  The file must exist in the workspace.
	 */
	public boolean outOfSync(IFile file) {
		return file.getName().equals(OUT_OF_SYNC) || file.getName().equals(WORKSPACE_ONLY)
				|| file.getName().equals(WORKSPACE_ONLY_HIDDEN);
	}

	/**
	 * Makes sure file requirements are met (out of sync, workspace only, etc).
	 */
	public void refreshFile(IFile file) throws CoreException, IOException {
		if (file.getName().equals(LOCAL_ONLY)) {
			removeFromWorkspace(file);
			//project must exist to access file system store.
			if (file.getProject().exists()) {
				createInFileSystem(file);
			}
			return;
		}
		if (file.getName().equals(LOCAL_ONLY_HIDDEN)) {
			removeFromWorkspace(file);
			// project must exist to access file system store.
			if (file.getProject().exists()) {
				file.getRawLocation().toFile().delete();
				createInFileSystem(file);
				if (Platform.OS.isWindows()) {
					Files.setAttribute(file.getRawLocation().toPath(), "dos:hidden", Boolean.TRUE);
				}
			}
			return;
		}
		if (file.getName().equals(WORKSPACE_ONLY)) {
			createInWorkspace(file);
			removeFromFileSystem(file);
			return;
		}
		if (file.getName().equals(WORKSPACE_ONLY_HIDDEN)) {
			file.getRawLocation().toFile().delete();
			createInFileSystem(file);
			if (Platform.OS.isWindows()) {
				Files.setAttribute(file.getRawLocation().toPath(), "dos:hidden", Boolean.TRUE);
			}
			file.refreshLocal(1, null);
			file.getRawLocation().toFile().delete();
			return;
		}
		if (file.getName().equals(DOES_NOT_EXIST)) {
			removeFromWorkspace(file);
			//project must exist to access file system store.
			if (file.getProject().exists()) {
				removeFromFileSystem(file);
			}
			return;
		}
		if (file.getName().equals(EXISTING)) {
			createInWorkspace(file);
			return;
		}
		if (file.getName().equals(EXISTING_HIDDEN)) {
			createInWorkspace(file);
			if (Platform.OS.isWindows()) {
				Files.setAttribute(file.getRawLocation().toPath(), "dos:hidden", Boolean.TRUE);
			}
			file.refreshLocal(1, null);
			return;
		}
		if (file.getName().equals(OUT_OF_SYNC)) {
			createInWorkspace(file);
			ensureOutOfSync(file);
			return;
		}
	}

	/**
	 * Makes sure file requirements are met (out of sync, workspace only, etc).
	 */
	public void refreshFiles() throws CoreException, IOException {
		for (IFile file : allFiles) {
			refreshFile(file);
		}
	}

	@BeforeEach
	public void setUp() throws Exception {
		generateInterestingFiles();
	}

	@Test
	public void testAppendContents() throws Exception {
		IFile target = projects[0].getFile("file1");
		target.create(createInputStream("abc"), false, null);
		assertEquals("abc", target.readString());
		target.appendContents(createInputStream("def"), false, false, null);
		assertEquals("abcdef", target.readString());
	}

	@SuppressWarnings("deprecation") // org.eclipse.core.resources.IResource.isLocal(int)
	@Test
	public void testAppendContents2() throws Exception {
		IFile file = projects[0].getFile("file1");
		removeFromWorkspace(file);

		// If force=true, IFile is non-local, file exists in local file system:
		// make IFile local, append contents (the thinking being that this file,
		// though marked non-local, was in fact local awaiting discovery; so
		// force=true says we it is ok to make file local and proceed as per normal)
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		// setup
		file.create(null, false, monitor);
		monitor.assertUsedUp();
		assertFalse(file.isLocal(IResource.DEPTH_ZERO));
		assertFalse(file.getLocation().toFile().exists());
		createInFileSystem(file);
		assertFalse(file.isLocal(IResource.DEPTH_ZERO));

		monitor.prepare();
		file.appendContents(createRandomContentsStream(), IResource.FORCE, monitor);
		monitor.assertUsedUp();

		assertTrue(file.isLocal(IResource.DEPTH_ZERO));
		assertTrue(file.getLocation().toFile().exists());
		// cleanup
		removeFromWorkspace(file);

		// If force=true, IFile is non-local, file does not exist in local file system:
		// fail - file not local (this file is not local for real - cannot append
		// something to a file that you don't have)
		// setup
		monitor.prepare();
		file.create(null, false, monitor);
		monitor.assertUsedUp();
		assertFalse(file.isLocal(IResource.DEPTH_ZERO));
		assertFalse(file.getLocation().toFile().exists());

		monitor.prepare();
		assertThrows(CoreException.class, () -> file.appendContents(createRandomContentsStream(), IResource.FORCE, monitor));
		monitor.sanityCheck();
		assertFalse(file.isLocal(IResource.DEPTH_ZERO));
		// cleanup
		removeFromWorkspace(file);

		// If force=false, IFile is non-local, file exists in local file system:
		// fail - file not local
		// setup
		monitor.prepare();
		file.create(null, false, monitor);
		monitor.assertUsedUp();
		assertFalse(file.isLocal(IResource.DEPTH_ZERO));
		assertFalse(file.getLocation().toFile().exists());
		createInFileSystem(file);
		assertFalse(file.isLocal(IResource.DEPTH_ZERO));

		monitor.prepare();
		assertThrows(CoreException.class, () -> file.appendContents(createRandomContentsStream(), IResource.NONE, monitor));
		monitor.assertUsedUp();
		assertFalse(file.isLocal(IResource.DEPTH_ZERO));
		// cleanup
		removeFromWorkspace(file);

		// If force=false, IFile is non-local, file does not exist in local file system:
		// fail - file not local
		// setup
		monitor.prepare();
		file.create(null, false, monitor);
		monitor.assertUsedUp();
		assertFalse(file.isLocal(IResource.DEPTH_ZERO));
		assertFalse(file.getLocation().toFile().exists());

		monitor.prepare();
		assertThrows(CoreException.class, () -> file.appendContents(createRandomContentsStream(), IResource.NONE, monitor));
		monitor.sanityCheck();
		assertFalse(file.isLocal(IResource.DEPTH_ZERO));
		// cleanup
		removeFromWorkspace(file);
	}

	/**
	 * Performs black box testing of the following method:
	 *     void create(InputStream, boolean, IProgressMonitor)
	 */
	@Test
	public void testCreate() throws Exception {
		Object[][] inputs = new Object[][] {interestingFiles(), interestingStreams(), TRUE_AND_FALSE, PROGRESS_MONITORS};
		new TestPerformer("IFileTest.testCreate") {
			@Override
			public void cleanUp(Object[] args, int count) throws Exception {
				IFile file = (IFile) args[0];
				refreshFile(file);
			}

			@Override
			public Object[] interestingOldState(Object[] args) throws Exception {
				return null;
			}

			@Override
			public Object invokeMethod(Object[] args, int count) throws Exception {
				IFile file = (IFile) args[0];
				InputStream stream = (InputStream) args[1];
				boolean force = ((Boolean) args[2]).booleanValue();
				IProgressMonitor monitor = (IProgressMonitor) args[3];
				if (monitor instanceof FussyProgressMonitor fussy) {
					fussy.prepare();
				}
				file.create(stream, force, monitor);
				if (monitor instanceof FussyProgressMonitor fussy) {
					fussy.sanityCheck();
				}
				return null;
			}

			@Override
			public boolean shouldFail(Object[] args, int count) {
				IFile file = (IFile) args[0];
				IPath fileLocation = file.getLocation();
				boolean force = ((Boolean) args[2]).booleanValue();
				boolean fileExistsInWS = file.exists();
				boolean fileExistsInFS = fileLocation != null && fileLocation.toFile().exists();

				// parent must be accessible
				if (!file.getParent().isAccessible()) {
					return true;
				}

				// should never fail if force is true
				if (force && !fileExistsInWS) {
					return false;
				}

				// file must not exist in WS or on filesystem.
				return fileExistsInWS || fileExistsInFS;
			}

			@Override
			public boolean wasSuccess(Object[] args, Object result, Object[] oldState) throws Exception {
				IFile file = (IFile) args[0];
				return file.exists();
			}
		}.performTest(inputs);
	}

	@Test
	public void testCreateDerived() throws CoreException {
		IFile derived = projects[0].getFile("derived.txt");
		createInWorkspace(projects[0]);
		removeFromWorkspace(derived);

		FussyProgressMonitor monitor = new FussyProgressMonitor();
		derived.create(createRandomContentsStream(), IResource.DERIVED, monitor);
		monitor.assertUsedUp();
		assertTrue(derived.isDerived());
		assertFalse(derived.isTeamPrivateMember());

		monitor.prepare();
		derived.delete(false, monitor);
		monitor.assertUsedUp();
		monitor.prepare();
		derived.create(createRandomContentsStream(), IResource.NONE, monitor);
		monitor.assertUsedUp();
		assertFalse(derived.isDerived());
		assertFalse(derived.isTeamPrivateMember());
	}
	@Test
	public void testCreateBytes() throws CoreException {
		IFile derived = projects[0].getFile("derived.txt");
		createInWorkspace(projects[0]);
		removeFromWorkspace(derived);

		FussyProgressMonitor monitor = new FussyProgressMonitor();
		derived.create("derived".getBytes(), false, true, monitor);
		monitor.assertUsedUp();
		assertTrue(derived.isDerived());
		assertFalse(derived.isTeamPrivateMember());
		assertEquals("derived", derived.readString());

		monitor.prepare();
		derived.delete(false, monitor);
		monitor.assertUsedUp();
		monitor.prepare();
		derived.create("notDerived".getBytes(), false, false, monitor);
		monitor.assertUsedUp();
		assertFalse(derived.isDerived());
		assertFalse(derived.isTeamPrivateMember());
		assertEquals("notDerived", derived.readString());

		IFolder subFolder = projects[0].getFolder("subFolder");
		subFolder.create(true, true, null);
		subFolder.getRawLocation().toFile().delete();

		IFile orphan = subFolder.getFile("myParentDoesNotExist.txt");
		monitor.prepare();
		orphan.write("parentDoesNotExistInFileSystemButInWorkspace".getBytes(), true, false, false, monitor);
		monitor.assertUsedUp();
		assertEquals("parentDoesNotExistInFileSystemButInWorkspace", orphan.readString());

		monitor.prepare();
		orphan.getParent().delete(true, null);
		// if the parent is deleted in workspace Exception is expected:
		try {
			orphan.write("parentDoesNotExist - not even in workspace".getBytes(), true, false, false, monitor);
			fail("should not be reached");
		} catch (ResourceException expected) {
			monitor.assertUsedUp();
			assertFalse(orphan.exists());
		}
	}

	@Test
	public void testWrite() throws CoreException {
		/* set local history policies */
		IWorkspaceDescription description = getWorkspace().getDescription();
		description.setMaxFileStates(4);
		getWorkspace().setDescription(description);

		IFile derived = projects[0].getFile("derived.txt");
		createInWorkspace(projects[0]);
		removeFromWorkspace(derived);
		for (int i = 0; i < 16; i++) {
			boolean setDerived = i % 2 == 0;
			boolean deleteBefore = (i >> 1) % 2 == 0;
			boolean keepHistory = (i >> 2) % 2 == 0;
			boolean oldDerived1 = false;
			if (deleteBefore) {
				derived.delete(false, null);
			} else {
				oldDerived1 = derived.isDerived();
			}
			assertEquals(!deleteBefore, derived.exists());
			FussyProgressMonitor monitor = new FussyProgressMonitor();
			AtomicInteger changeCount = new AtomicInteger();
			ResourcesPlugin.getWorkspace().addResourceChangeListener(event -> changeCount.incrementAndGet());
			derived.write(("updateOrCreate" + i).getBytes(), false, setDerived, keepHistory, monitor);
			assertEquals(1, changeCount.get(), "not atomic");
			monitor.assertUsedUp();
			if (deleteBefore) {
				assertEquals(setDerived, derived.isDerived());
			} else {
				assertEquals(oldDerived1 || setDerived, derived.isDerived());
			}
			assertFalse(derived.isTeamPrivateMember());
			assertTrue(derived.exists());

			IFileState[] history1 = derived.getHistory(null);
			changeCount.set(0);
			derived.write(("update" + i).getBytes(), false, false, keepHistory, null);
			boolean oldDerived2 = derived.isDerived();
			assertEquals(oldDerived2, derived.isDerived());
			assertEquals(1, changeCount.get(), "not atomic");
			IFileState[] history2 = derived.getHistory(null);
			assertEquals((keepHistory && !oldDerived2) ? 1 : 0, history2.length - history1.length);
		}
	}

	// @Test // does not test anything but only measures the performance benefit
	public void _testWritePerformanceBatch_() throws CoreException {
		createInWorkspace(projects[0]);
		Map<IFile, byte[]> fileMap2 = new HashMap<>();
		Map<IFile, byte[]> fileMap1 = new HashMap<>();
		for (int i = 0; i < 1000; i++) {
			IFile file = projects[0].getFile("My" + i + ".class");
			removeFromWorkspace(file);
			((i % 2 == 0) ? fileMap1 : fileMap2).put(file, ("smallFileContent" + i).getBytes());
		}
		{
			long n0 = System.nanoTime();
			ExecutorService executorService = Executors.newWorkStealingPool();
			ResourcesPlugin.getWorkspace().write(fileMap1, false, true, false, null, executorService);
			executorService.shutdownNow();
			long n1 = System.nanoTime();
			System.out.println("parallel write took:" + (n1 - n0) / 1_000_000 + "ms"); // ~ 250ms with 6 cores
		}
		{
			long n0 = System.nanoTime();
			for (Entry<IFile, byte[]> e : fileMap2.entrySet()) {
				e.getKey().write(e.getValue(), false, true, false, null);
			}
			long n1 = System.nanoTime();
			System.out.println("sequential write took:" + (n1 - n0) / 1_000_000 + "ms"); // ~ 1500ms
		}
	}

	@Test
	public void testWrites() throws CoreException {
		ExecutorService executorService = Executors.newWorkStealingPool();
		IWorkspaceDescription description = getWorkspace().getDescription();
		description.setMaxFileStates(4);
		getWorkspace().setDescription(description);

		IFile derived = projects[0].getFile("derived.txt");
		IFile anyOther = projects[0].getFile("anyOther.txt");
		createInWorkspace(projects[0]);
		removeFromWorkspace(derived);
		removeFromWorkspace(anyOther);
		for (int i = 0; i < 16; i++) {
			boolean setDerived = i % 2 == 0;
			boolean deleteBefore = (i >> 1) % 2 == 0;
			boolean keepHistory = (i >> 2) % 2 == 0;
			boolean oldDerived1 = false;
			if (deleteBefore) {
				derived.delete(false, null);
				anyOther.delete(false, null);
			} else {
				oldDerived1 = derived.isDerived();
			}
			assertEquals(!deleteBefore, derived.exists());
			FussyProgressMonitor monitor = new FussyProgressMonitor();
			AtomicInteger changeCount = new AtomicInteger();
			ResourcesPlugin.getWorkspace().addResourceChangeListener(event -> changeCount.incrementAndGet());
			String derivedContent = "updateOrCreate" + i;
			String otherContent = "other" + i;
			ResourcesPlugin.getWorkspace().write(
					Map.of(derived, derivedContent.getBytes(), anyOther, otherContent.getBytes()), false, setDerived,
					keepHistory, monitor, executorService);
			assertEquals(derivedContent, new String(derived.readAllBytes()));
			assertEquals(otherContent, new String(anyOther.readAllBytes()));
			monitor.assertUsedUp();
			if (deleteBefore) {
				assertEquals(setDerived, derived.isDerived());
			} else {
				assertEquals(oldDerived1 || setDerived, derived.isDerived());
			}
			assertFalse(derived.isTeamPrivateMember());
			assertTrue(derived.exists());

			IFileState[] history1 = derived.getHistory(null);
			changeCount.set(0);
			derivedContent = "update" + i;
			otherContent = "dude" + i;
			ResourcesPlugin.getWorkspace().write(
					Map.of(derived, derivedContent.getBytes(), anyOther, otherContent.getBytes()), false, false,
					keepHistory,
					null, executorService);
			assertEquals(derivedContent, new String(derived.readAllBytes()));
			assertEquals(otherContent, new String(anyOther.readAllBytes()));
			boolean oldDerived2 = derived.isDerived();
			assertEquals(oldDerived2, derived.isDerived());
			IFileState[] history2 = derived.getHistory(null);
			assertEquals((keepHistory && !oldDerived2) ? 1 : 0, history2.length - history1.length);
		}
		executorService.shutdown();
	}
	@Test
	public void testWriteRule() throws CoreException {
		IFile resource = projects[0].getFile("derived.txt");
		createInWorkspace(projects[0]);
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		resource.delete(false, null);
		AtomicInteger changeCount = new AtomicInteger();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(event -> changeCount.incrementAndGet());
		workspace.run(pm -> {
			resource.write(("create").getBytes(), false, false, false, null);
		}, workspace.getRuleFactory().createRule(resource), IWorkspace.AVOID_UPDATE, null);
		assertTrue(resource.exists());
		assertEquals(1, changeCount.get(), "not atomic");
		// test that modifyRule can be used for IFile.write() if the file already exits:
		changeCount.set(0);
		workspace.run(pm -> {
			resource.write(("replace").getBytes(), false, false, false, null);
		}, workspace.getRuleFactory().modifyRule(resource), IWorkspace.AVOID_UPDATE, null);
		assertTrue(resource.exists());
		assertEquals(1, changeCount.get(), "not atomic");
	}

	@Test
	public void testDeltaOnCreateDerived() throws CoreException {
		IFile derived = projects[0].getFile("derived.txt");
		createInWorkspace(projects[0]);

		ResourceDeltaVerifier verifier = new ResourceDeltaVerifier();
		getWorkspace().addResourceChangeListener(verifier, IResourceChangeEvent.POST_CHANGE);

		verifier.addExpectedChange(derived, IResourceDelta.ADDED, IResource.NONE);

		FussyProgressMonitor monitor = new FussyProgressMonitor();
		derived.create(createRandomContentsStream(), IResource.FORCE | IResource.DERIVED, monitor);
		monitor.assertUsedUp();

		assertTrue(verifier.isDeltaValid());
	}

	@Test
	public void testCreateDerivedTeamPrivate() throws CoreException {
		IFile teamPrivate = projects[0].getFile("teamPrivateDerived.txt");
		createInWorkspace(projects[0]);
		removeFromWorkspace(teamPrivate);

		FussyProgressMonitor monitor = new FussyProgressMonitor();
		teamPrivate.create(createRandomContentsStream(), IResource.TEAM_PRIVATE | IResource.DERIVED, monitor);
		monitor.assertUsedUp();

		assertTrue(teamPrivate.isTeamPrivateMember());
		assertTrue(teamPrivate.isDerived());

		monitor.prepare();
		teamPrivate.delete(false, monitor);
		monitor.assertUsedUp();
		monitor.prepare();
		teamPrivate.create(createRandomContentsStream(), IResource.NONE, monitor);
		monitor.assertUsedUp();
		assertFalse(teamPrivate.isTeamPrivateMember());
		assertFalse(teamPrivate.isDerived());
	}

	@Test
	public void testCreateTeamPrivate() throws CoreException {
		IFile teamPrivate = projects[0].getFile("teamPrivate.txt");
		createInWorkspace(projects[0]);
		removeFromWorkspace(teamPrivate);

		FussyProgressMonitor monitor = new FussyProgressMonitor();
		teamPrivate.create(createRandomContentsStream(), IResource.TEAM_PRIVATE, monitor);
		monitor.assertUsedUp();
		assertTrue(teamPrivate.isTeamPrivateMember());
		assertFalse(teamPrivate.isDerived());

		monitor.prepare();
		teamPrivate.delete(false, monitor);
		monitor.assertUsedUp();
		monitor.prepare();
		teamPrivate.create(createRandomContentsStream(), IResource.NONE, monitor);
		monitor.assertUsedUp();
		assertFalse(teamPrivate.isTeamPrivateMember());
		assertFalse(teamPrivate.isDerived());
	}

	@Test
	public void testFileCreation() throws Exception {
		IFile fileWithoutInput = projects[0].getFile("file1");
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		assertFalse(fileWithoutInput.exists());
		monitor.prepare();
		fileWithoutInput.create(null, true, monitor);
		monitor.assertUsedUp();
		assertTrue(fileWithoutInput.exists());

		// creation with empty content
		IFile emptyFile = projects[0].getFile("file2");
		assertFalse(emptyFile.exists());
		String contents = "";
		monitor.prepare();
		emptyFile.create(createInputStream(contents), true, monitor);
		monitor.assertUsedUp();
		assertTrue(emptyFile.exists());
		try (InputStream stream = emptyFile.getContents(false)) {
			assertEquals(0, stream.available());
			assertThat(stream).hasContent(contents);

		}

		// creation with random content
		IFile fileWithRandomContent = projects[0].getFile("file3");
		assertFalse(fileWithRandomContent.exists());
		contents = createRandomString();
		monitor.prepare();
		fileWithRandomContent.create(createInputStream(contents), true, monitor);
		monitor.assertUsedUp();
		assertTrue(fileWithRandomContent.exists());
		try (InputStream fileInput = fileWithRandomContent.getContents(false)) {
			assertThat(fileInput).hasContent(contents);
		}

		// try to create a file over a folder that exists
		IFolder folder = projects[0].getFolder("folder1");
		monitor.prepare();
		folder.create(true, true, monitor);
		monitor.assertUsedUp();
		assertTrue(folder.exists());

		IFile fileOnFolder = projects[0].getFile("folder1");
		monitor.prepare();
		assertThrows(CoreException.class, () -> fileOnFolder.create(null, true, monitor));
		monitor.assertUsedUp();
		assertTrue(folder.exists());
		assertFalse(fileOnFolder.exists());

		// try to create a file under a non-existent parent
		folder = projects[0].getFolder("folder2");
		assertFalse(folder.exists());
		IFile fileUnderNonExistentParent = folder.getFile("file4");
		monitor.prepare();
		assertThrows(CoreException.class, () -> fileUnderNonExistentParent.create(null, true, monitor));
		monitor.assertUsedUp();
		assertFalse(folder.exists());
		assertFalse(fileUnderNonExistentParent.exists());

		//create from stream that throws exceptions
		IFile fileFromStream = projects[0].getFile("file2");
		removeFromWorkspace(fileFromStream);
		removeFromFileSystem(fileFromStream);

		InputStream content = new InputStream() {
			@Override
			public int read() throws IOException {
				throw new IOException();
			}
		};
		monitor.prepare();
		assertThrows(CoreException.class, () -> fileFromStream.create(content, false, monitor));
		monitor.assertUsedUp();
		assertDoesNotExistInWorkspace(fileFromStream);
		assertDoesNotExistInFileSystem(fileFromStream);

		// cleanup
		folder = projects[0].getFolder("folder1");
		monitor.prepare();
		folder.delete(false, monitor);
		monitor.assertUsedUp();

		IFile file = projects[0].getFile("file1");
		monitor.prepare();
		file.delete(false, monitor);
		monitor.assertUsedUp();

		file = projects[0].getFile("file2");
		monitor.prepare();
		file.delete(false, monitor);
		monitor.assertUsedUp();

		file = projects[0].getFile("file3");
		monitor.prepare();
		file.delete(false, monitor);
		monitor.assertUsedUp();
	}

	@Test
	public void testFileCreation_Bug107188() throws CoreException {
		//create from stream that is canceled
		IFile target = projects[0].getFile("file1");
		removeFromWorkspace(target);
		removeFromFileSystem(target);

		InputStream content = new InputStream() {
			@Override
			public int read() {
				throw new OperationCanceledException();
			}
		};
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		assertThrows(OperationCanceledException.class, () -> target.create(content, false, monitor));
		monitor.assertUsedUp();
		assertDoesNotExistInWorkspace(target);
		assertDoesNotExistInFileSystem(target);
	}

	@Test
	public void testFileDeletion() throws Throwable {
		IFile target = projects[0].getFile("file1");
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		target.create(null, true, monitor);
		monitor.assertUsedUp();
		assertTrue(target.exists());
		monitor.prepare();
		target.delete(true, monitor);
		monitor.assertUsedUp();
		assertFalse(target.exists());
	}

	@Test
	public void testFileEmptyDeletion() throws Throwable {
		IFile target = projects[0].getFile("file1");
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		target.create(createInputStream(""), true, monitor);
		monitor.assertUsedUp();
		assertTrue(target.exists());
		monitor.prepare();
		target.delete(true, monitor);
		monitor.assertUsedUp();
		assertFalse(target.exists());
	}

	@Test
	public void testFileInFolderCreation() throws CoreException {
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		IFolder folder = projects[0].getFolder("folder1");
		folder.create(false, true, monitor);
		monitor.assertUsedUp();

		IFile target = folder.getFile("file1");
		monitor.prepare();
		target.create(createRandomContentsStream(), true, monitor);
		monitor.assertUsedUp();
		assertTrue(target.exists());
	}

	@Test
	public void testFileInFolderCreation1() throws Throwable {
		IFolder folder = projects[0].getFolder("folder1");
		folder.create(false, true, null);

		IFile target = folder.getFile("file1");
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		target.create(createRandomContentsStream(), true, monitor);
		monitor.assertUsedUp();
		assertTrue(target.exists());
	}

	@Test
	public void testFileInFolderCreation2() throws CoreException {
		IFolder folder = projects[0].getFolder("folder1");
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		folder.create(false, true, monitor);
		monitor.assertUsedUp();

		IFile target = folder.getFile("file1");
		monitor.prepare();
		target.create(createRandomContentsStream(), true, monitor);
		monitor.assertUsedUp();
		assertTrue(target.exists());
	}

	@Test
	public void testFileMove() throws Throwable {
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		IFile target = projects[0].getFile("file1");
		target.create(createRandomContentsStream(), true, monitor);
		monitor.assertUsedUp();

		IFile destination = projects[0].getFile("file2");
		monitor.prepare();
		target.move(destination.getFullPath(), true, monitor);
		monitor.assertUsedUp();

		assertTrue(destination.exists());
		assertFalse(target.exists());
	}

	@Test
	public void testFileOverFolder() throws Throwable {
		IFolder existing = projects[0].getFolder("ExistingFolder");
		IFile target = projects[0].getFile("ExistingFolder");

		FussyProgressMonitor monitor = new FussyProgressMonitor();
		assertThrows(CoreException.class, () -> target.create(null, true, monitor));
		monitor.assertUsedUp();
		assertTrue(existing.exists());
	}

	/**
	 * Performs black box testing of the following method:
	 *     InputStream getContents()
	 */
	@Test
	public void testGetContents() throws Exception {
		Object[][] inputs = new Object[][] {interestingFiles()};
		new TestPerformer("IFileTest.testGetContents") {
			@Override
			public void cleanUp(Object[] args, int count) throws Exception {
				IFile file = (IFile) args[0];
				refreshFile(file);
			}

			@Override
			public Object[] interestingOldState(Object[] args) throws Exception {
				return null;
			}

			@Override
			public Object invokeMethod(Object[] args, int count) throws Exception {
				IFile file = (IFile) args[0];
				return file.getContents(false);
			}

			@Override
			public boolean shouldFail(Object[] args, int count) {
				IFile file = (IFile) args[0];

				//file must exist
				if (!file.exists()) {
					return true;
				}

				//file must be in sync
				if (outOfSync(file)) {
					return true;
				}
				return false;
			}

			@Override
			public boolean wasSuccess(Object[] args, Object result, Object[] oldState) throws Exception {
				IFile file = (IFile) args[0];
				boolean returnVal;
				try (InputStream contents = (InputStream) result) {
					returnVal = file.exists() && contents != null;
				}
				return returnVal;
			}
		}.performTest(inputs);
	}

	@Test
	public void testGetContents2() throws IOException, CoreException {
		IFile target = projects[0].getFile("file1");
		String testString = createRandomString();
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		target.create(null, false, null);
		target.setContents(createInputStream(testString), true, false, monitor);
		monitor.assertUsedUp();
		ensureOutOfSync(target);

		CoreException firstException = assertThrows(CoreException.class, () -> {
			try (InputStream content = target.getContents(false)) {
			}
		});
		assertEquals(IResourceStatus.OUT_OF_SYNC_LOCAL, firstException.getStatus().getCode());

		try (InputStream content = target.getContents(true)) {
		}

		CoreException secondException = assertThrows(CoreException.class, () -> {
			try (InputStream content = target.getContents(false)) {
			}
		});
		assertEquals(IResourceStatus.OUT_OF_SYNC_LOCAL, secondException.getStatus().getCode());
		InputStream content = new InputStream() {
			@Override
			public int read() throws IOException {
				throw new IOException();
			}
		};
		try (content) {
			monitor.prepare();
			assertThrows(CoreException.class, () -> target.setContents(content, IResource.NONE, monitor));
			monitor.sanityCheck();
		}
		assertExistsInWorkspace(target);
		assertExistsInFileSystem(target);
	}

	/**
	 * Tests creation and manipulation of file names that are reserved on some platforms.
	 */
	@Test
	public void testInvalidFileNames() throws CoreException {
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		IProject project = projects[0];

		//should not be able to create a file with invalid path on any platform
		String[] names = new String[] {"", "/"};
		for (String name : names) {
			assertThrows(RuntimeException.class, () -> project.getFile(name));
		}

		//do some tests with invalid names
		names = new String[0];
		if (OS.isWindows()) {
			//invalid windows names
			names = new String[] {"a  ", "foo::bar", "prn", "nul", "con", "aux", "clock$", "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", "com9", "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9", "AUX", "con.foo", "LPT4.txt", "*", "?", "\"", "<", ">", "|"};
		} else {
			//invalid names on non-windows platforms
			names = new String[] {};
		}
		for (String name : names) {
			monitor.prepare();
			IFile file = project.getFile(IPath.fromPortableString(name));
			assertFalse(file.exists(), name);
			assertThrows(CoreException.class, () -> file.create(createRandomContentsStream(), true, monitor));
			monitor.sanityCheck();
			assertFalse(file.exists(), name);
		}

		//do some tests with valid names that are *almost* invalid
		if (OS.isWindows()) {
			//these names are valid on windows
			names = new String[] {"  a", "hello.prn.txt", "null", "con3", "foo.aux", "lpt0", "com0", "com10", "lpt10", ",", "'", ";", "clock$.class"};
		} else {
			//these names are valid on non-windows platforms
			names = new String[] {"  a", "a  ", "foo:bar", "prn", "nul", "con", "aux", "clock$", "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", "com9", "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9", "con.foo", "LPT4.txt", "*", "?", "\"", "<", ">", "|", "hello.prn.txt", "null", "con3", "foo.aux", "lpt0", "com0", "com10", "lpt10", ",", "'", ";"};
		}
		for (String name : names) {
			IFile file = project.getFile(name);
			assertFalse(file.exists(), name + " shouldn't exist");
			monitor.prepare();
			file.create(createRandomContentsStream(), true, monitor);
			monitor.assertUsedUp();
			assertTrue(file.exists(), name + " should exist");
		}
	}

	/**
	 * Performs black box testing of the following method:
	 *     void setContents(InputStream, boolean, IProgressMonitor)
	 */
	@Test
	public void testSetContents1() throws Exception {
		Object[][] inputs = new Object[][] {interestingFiles(), interestingStreams(), TRUE_AND_FALSE, PROGRESS_MONITORS};
		new TestPerformer("IFileTest.testSetContents1") {
			@Override
			public void cleanUp(Object[] args, int count) throws Exception {
				IFile file = (IFile) args[0];
				refreshFile(file);
			}

			@Override
			public Object[] interestingOldState(Object[] args) throws Exception {
				return null;
			}

			@Override
			public Object invokeMethod(Object[] args, int count) throws Exception {
				IFile file = (IFile) args[0];
				InputStream stream = (InputStream) args[1];
				boolean force = ((Boolean) args[2]).booleanValue();
				IProgressMonitor monitor = (IProgressMonitor) args[3];
				if (monitor instanceof FussyProgressMonitor fussy) {
					fussy.prepare();
				}
				file.setContents(stream, force, false, monitor);
				if (monitor instanceof FussyProgressMonitor fussy) {
					fussy.sanityCheck();
				}
				return null;
			}

			@Override
			public boolean shouldFail(Object[] args, int count) {
				IFile file = (IFile) args[0];
				boolean force = ((Boolean) args[2]).booleanValue();

				//file must exist
				if (!file.exists()) {
					return true;
				}

				//file must be in sync if force is false
				if (!force && outOfSync(file)) {
					return true;
				}
				return false;
			}

			@Override
			public boolean wasSuccess(Object[] args, Object result, Object[] oldState) throws Exception {
				IFile file = (IFile) args[0];
				return file.exists();
			}
		}.performTest(inputs);
	}

	@Test
	public void testSetContents2() throws IOException, CoreException {
		IFile target = projects[0].getFile("file1");
		target.create(null, false, null);

		String testString = createRandomString();
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		target.setContents(createInputStream(testString), true, false, monitor);
		monitor.assertUsedUp();

		try (InputStream content = target.getContents(false)) {
			assertThat(content).hasContent(testString);
		}
	}

	@Test
	public void testCreateByteArray() throws IOException, CoreException {
		IFile target = projects[0].getFile("file1");
		String testString = createRandomString();
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		target.create(testString.getBytes(), true, false, monitor);
		monitor.assertUsedUp();
		assertEquals(testString, target.readString());
	}

	@Test
	public void testReadNBytes() throws IOException, CoreException {
		IFile file = projects[0].getFile("smallfile");
		byte[] bytes = "1234".getBytes(StandardCharsets.US_ASCII);
		file.write(bytes, false, false, false, null);
		assertThrows(IllegalArgumentException.class, () -> file.readNBytes(-1));
		byte[] nBytes0 = file.readNBytes(0);
		assertEquals(0, nBytes0.length);
		byte[] nBytes1 = file.readNBytes(1);
		assertEquals(1, nBytes1.length);
		assertEquals('1', nBytes1[0]);
		byte[] nBytes4 = file.readNBytes(4);
		assertEquals(4, nBytes4.length);
		byte[] nBytes5 = file.readNBytes(5);
		assertEquals(4, nBytes5.length);
		byte[] nBytesMax = file.readNBytes(Integer.MAX_VALUE);
		assertEquals(4, nBytesMax.length);
		assertArrayEquals(bytes, nBytesMax);

		IFile largefile = projects[0].getFile("largefile");
		byte[] largeContent = new byte[50_000_000]; // only 50 MB to prevent OutOfMemoryError on jenkins
		largefile.write(largeContent, false, false, false, null);
		byte[] largeBytes = largefile.readNBytes(Integer.MAX_VALUE);
		assertEquals(largeContent.length, largeBytes.length);
		byte[] largeBytes1 = largefile.readNBytes(1);
		assertEquals(1, largeBytes1.length);
	}

	@Test
	public void testReadAll() throws IOException, CoreException {
		List<Charset> charsets = List.of(StandardCharsets.ISO_8859_1, StandardCharsets.UTF_8, StandardCharsets.UTF_16BE,
				StandardCharsets.UTF_16LE);
		List<String> fileTypes = List.of(".txt", "");
		String BOM = "\uFEFF";
		Charset defaultCharset = Charset.forName(projects[0].getDefaultCharset());
		for (String fileType : fileTypes) {
			for (Charset charset : charsets) {
				for (int bomCount = 0; bomCount <= (charset.name().contains("UTF") ? 1 : 0); bomCount++) {
					IFile target = projects[0].getFile("file1" + charset.name() + "_" + bomCount + fileType);
					target.create(null, false, null);
					String testString = "hallo";
					boolean setCharset = bomCount == 0;
					if (setCharset) {
						target.setCharset(charset.name(), null);
					}
					FussyProgressMonitor monitor = new FussyProgressMonitor();
					byte[] content = ((bomCount == 1 ? BOM : "") + testString).getBytes(charset);
					target.setContents(content, true, false, monitor);
					monitor.assertUsedUp();
					byte[] allBytes = target.readAllBytes();
					assertArrayEquals(content, allBytes, target.getName());
					char[] allChars = target.readAllChars();
					String readString = target.readString();
					String expected;
					if (!setCharset && fileType.isEmpty() && charset != defaultCharset) {
						// BOM present but ignored for unknown filetype
						expected = new String(content, defaultCharset);
					} else {
						// ".txt" files autodetect charset by BOM if present
						expected = testString;
					}
					assertArrayEquals(expected.toCharArray(), allChars, target.getName());
					assertEquals(expected, readString, target.getName());
				}
			}
		}
	}

	@Test
	public void testSetGetFolderPersistentProperty() throws Throwable {
		IResource target = getWorkspace().getRoot().getFile(IPath.fromOSString("/Project/File.txt"));
		String value = "this is a test property value";
		QualifiedName name = new QualifiedName("itp-test", "testProperty");
		// getting/setting persistent properties on non-existent resources should throw an exception
		removeFromWorkspace(target);
		assertThrows(CoreException.class, () -> target.getPersistentProperty(name));
		assertThrows(CoreException.class, () -> target.setPersistentProperty(name, value));

		createInWorkspace(target);
		target.setPersistentProperty(name, value);
		// see if we can get the property
		assertTrue(target.getPersistentProperty(name).equals(value));
		// see what happens if we get a non-existant property
		QualifiedName nonExistentPropertyName = new QualifiedName("itp-test", "testNonProperty");
		assertNull(target.getPersistentProperty(nonExistentPropertyName));

		//set a persistent property with null qualifier
		QualifiedName nullQualifierName = new QualifiedName(null, "foo");
		assertThrows(CoreException.class, () -> target.setPersistentProperty(nullQualifierName, value));
	}

}
