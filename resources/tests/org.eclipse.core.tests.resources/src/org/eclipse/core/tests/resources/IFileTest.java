/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.junit.Assert.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Platform.OS;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.tests.harness.FussyProgressMonitor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class IFileTest extends ResourceTest {
	//name of files according to sync category
	public static final String DOES_NOT_EXIST = "DoesNotExistFile";

	public static final String EXISTING = "ExistingFile";
	public static final String LOCAL_ONLY = "LocalOnlyFile";
	public static final String OUT_OF_SYNC = "OutOfSyncFile";
	//	protected static final IProgressMonitor[] PROGRESS_MONITORS = new IProgressMonitor[] {new FussyProgressMonitor(), new CancelingProgressMonitor(), null};
	protected static final IProgressMonitor[] PROGRESS_MONITORS = new IProgressMonitor[] {new FussyProgressMonitor(), null};
	protected static final Boolean[] TRUE_AND_FALSE = new Boolean[] {Boolean.TRUE, Boolean.FALSE};
	public static final String WORKSPACE_ONLY = "WorkspaceOnlyFile";

	ArrayList<IFile> allFiles = new ArrayList<>();
	ArrayList<IFile> existingFiles = new ArrayList<>();
	ArrayList<IFile> localOnlyFiles = new ArrayList<>();
	ArrayList<IFile> nonExistingFiles = new ArrayList<>();
	ArrayList<IFile> outOfSyncFiles = new ArrayList<>();

	IProject[] projects = null;
	ArrayList<IFile> workspaceOnlyFiles = new ArrayList<>();

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
				folder.create(true, true, getMonitor());
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
		IFile file = container.getFile(IPath.fromOSString(DOES_NOT_EXIST));
		nonExistingFiles.add(file);
		allFiles.add(file);

		//exists in file system only
		file = container.getFile(IPath.fromOSString(LOCAL_ONLY));
		localOnlyFiles.add(file);
		allFiles.add(file);

		if (existsAndOpen(container)) {

			//existing file
			file = container.getFile(IPath.fromOSString(EXISTING));
			existingFiles.add(file);
			allFiles.add(file);

			//exists in workspace only
			file = container.getFile(IPath.fromOSString(WORKSPACE_ONLY));
			workspaceOnlyFiles.add(file);
			allFiles.add(file);

			//exists in both but is out of sync
			file = container.getFile(IPath.fromOSString(OUT_OF_SYNC));
			outOfSyncFiles.add(file);
			allFiles.add(file);
		}
	}

	/**
	 * Returns some interesting files.  These files are created
	 * during setup.
	 */
	public IFile[] interestingFiles() throws CoreException {
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
			openProject.create(getMonitor());
			openProject.open(getMonitor());
			projects[0] = openProject;

			//closed project
			IProject closedProject = getWorkspace().getRoot().getProject("ClosedProject");
			closedProject.create(getMonitor());
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
		streams.add(getRandomContents());

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
		return file.getName().equals(OUT_OF_SYNC) || file.getName().equals(WORKSPACE_ONLY);
	}

	/**
	 * Makes sure file requirements are met (out of sync, workspace only, etc).
	 */
	public void refreshFile(IFile file) throws CoreException {
		if (file.getName().equals(LOCAL_ONLY)) {
			ensureDoesNotExistInWorkspace(file);
			//project must exist to access file system store.
			if (file.getProject().exists()) {
				ensureExistsInFileSystem(file);
			}
			return;
		}
		if (file.getName().equals(WORKSPACE_ONLY)) {
			ensureExistsInWorkspace(file, true);
			ensureDoesNotExistInFileSystem(file);
			return;
		}
		if (file.getName().equals(DOES_NOT_EXIST)) {
			ensureDoesNotExistInWorkspace(file);
			//project must exist to access file system store.
			if (file.getProject().exists()) {
				ensureDoesNotExistInFileSystem(file);
			}
			return;
		}
		if (file.getName().equals(EXISTING)) {
			ensureExistsInWorkspace(file, true);
			return;
		}
		if (file.getName().equals(OUT_OF_SYNC)) {
			ensureExistsInWorkspace(file, true);
			ensureOutOfSync(file);
			return;
		}
	}

	/**
	 * Makes sure file requirements are met (out of sync, workspace only, etc).
	 */
	public void refreshFiles() throws CoreException {
		for (IFile file : allFiles) {
			refreshFile(file);
		}
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		generateInterestingFiles();
	}

	@Override
	protected void tearDown() throws Exception {
		getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, null);
		super.tearDown();
	}

	@Test
	public void testAppendContents() throws Exception {
		IFile target = projects[0].getFile("file1");
		target.create(getContents("abc"), false, null);
		target.appendContents(getContents("def"), false, false, null);

		try (InputStream content = target.getContents(false)) {
			assertTrue("3.0", compareContent(content, getContents("abcdef")));
		}
	}

	@Test
	public void testAppendContents2() throws CoreException {
		IFile file = projects[0].getFile("file1");
		ensureDoesNotExistInWorkspace(file);

		// If force=true, IFile is non-local, file exists in local file system:
		// make IFile local, append contents (the thinking being that this file,
		// though marked non-local, was in fact local awaiting discovery; so
		// force=true says we it is ok to make file local and proceed as per normal)
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		// setup
		file.create(null, false, monitor);
		monitor.assertUsedUp();
		assertTrue("1.0", !file.isLocal(IResource.DEPTH_ZERO));
		assertTrue("1.1", !file.getLocation().toFile().exists());
		ensureExistsInFileSystem(file);
		assertTrue("1.2", !file.isLocal(IResource.DEPTH_ZERO));

		monitor.prepare();
		file.appendContents(getRandomContents(), IResource.FORCE, monitor);
		monitor.assertUsedUp();

		assertTrue("1.5", file.isLocal(IResource.DEPTH_ZERO));
		assertTrue("1.6", file.getLocation().toFile().exists());
		// cleanup
		ensureDoesNotExistInWorkspace(file);

		// If force=true, IFile is non-local, file does not exist in local file system:
		// fail - file not local (this file is not local for real - cannot append
		// something to a file that you don't have)
		// setup
		monitor.prepare();
		file.create(null, false, monitor);
		monitor.assertUsedUp();
		assertTrue("2.0", !file.isLocal(IResource.DEPTH_ZERO));
		assertTrue("2.1", !file.getLocation().toFile().exists());

		monitor.prepare();
		assertThrows(CoreException.class, () -> file.appendContents(getRandomContents(), IResource.FORCE, monitor));
		monitor.sanityCheck();
		assertTrue("2.4", !file.isLocal(IResource.DEPTH_ZERO));
		// cleanup
		ensureDoesNotExistInWorkspace(file);

		// If force=false, IFile is non-local, file exists in local file system:
		// fail - file not local
		// setup
		monitor.prepare();
		file.create(null, false, monitor);
		monitor.assertUsedUp();
		assertTrue("3.0", !file.isLocal(IResource.DEPTH_ZERO));
		assertTrue("3.1", !file.getLocation().toFile().exists());
		ensureExistsInFileSystem(file);
		assertTrue("3.2", !file.isLocal(IResource.DEPTH_ZERO));

		monitor.prepare();
		assertThrows(CoreException.class, () -> file.appendContents(getRandomContents(), IResource.NONE, monitor));
		monitor.assertUsedUp();
		assertTrue("3.5", !file.isLocal(IResource.DEPTH_ZERO));
		// cleanup
		ensureDoesNotExistInWorkspace(file);

		// If force=false, IFile is non-local, file does not exist in local file system:
		// fail - file not local
		// setup
		monitor.prepare();
		file.create(null, false, monitor);
		monitor.assertUsedUp();
		assertTrue("4.0", !file.isLocal(IResource.DEPTH_ZERO));
		assertTrue("4.1", !file.getLocation().toFile().exists());

		monitor.prepare();
		assertThrows(CoreException.class, () -> file.appendContents(getRandomContents(), IResource.NONE, monitor));
		monitor.sanityCheck();
		assertTrue("4.4", !file.isLocal(IResource.DEPTH_ZERO));
		// cleanup
		ensureDoesNotExistInWorkspace(file);
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
			public void cleanUp(Object[] args, int count) throws CoreException {
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
		ensureExistsInWorkspace(projects[0], true);
		ensureDoesNotExistInWorkspace(derived);

		FussyProgressMonitor monitor = new FussyProgressMonitor();
		derived.create(getRandomContents(), IResource.DERIVED, monitor);
		monitor.assertUsedUp();
		assertTrue("1.0", derived.isDerived());
		assertTrue("1.1", !derived.isTeamPrivateMember());

		monitor.prepare();
		derived.delete(false, monitor);
		monitor.assertUsedUp();
		monitor.prepare();
		derived.create(getRandomContents(), IResource.NONE, monitor);
		monitor.assertUsedUp();
		assertTrue("2.0", !derived.isDerived());
		assertTrue("2.1", !derived.isTeamPrivateMember());
	}

	@Test
	public void testDeltaOnCreateDerived() throws CoreException {
		IFile derived = projects[0].getFile("derived.txt");
		ensureExistsInWorkspace(projects[0], true);

		ResourceDeltaVerifier verifier = new ResourceDeltaVerifier();
		getWorkspace().addResourceChangeListener(verifier, IResourceChangeEvent.POST_CHANGE);

		verifier.addExpectedChange(derived, IResourceDelta.ADDED, IResource.NONE);

		FussyProgressMonitor monitor = new FussyProgressMonitor();
		derived.create(getRandomContents(), IResource.FORCE | IResource.DERIVED, monitor);
		monitor.assertUsedUp();

		assertTrue("2.0", verifier.isDeltaValid());
	}

	@Test
	public void testCreateDerivedTeamPrivate() throws CoreException {
		IFile teamPrivate = projects[0].getFile("teamPrivateDerived.txt");
		ensureExistsInWorkspace(projects[0], true);
		ensureDoesNotExistInWorkspace(teamPrivate);

		FussyProgressMonitor monitor = new FussyProgressMonitor();
		teamPrivate.create(getRandomContents(), IResource.TEAM_PRIVATE | IResource.DERIVED, monitor);
		monitor.assertUsedUp();

		assertTrue("1.0", teamPrivate.isTeamPrivateMember());
		assertTrue("1.1", teamPrivate.isDerived());

		monitor.prepare();
		teamPrivate.delete(false, monitor);
		monitor.assertUsedUp();
		monitor.prepare();
		teamPrivate.create(getRandomContents(), IResource.NONE, monitor);
		monitor.assertUsedUp();
		assertTrue("2.0", !teamPrivate.isTeamPrivateMember());
		assertTrue("2.1", !teamPrivate.isDerived());
	}

	@Test
	public void testCreateTeamPrivate() throws CoreException {
		IFile teamPrivate = projects[0].getFile("teamPrivate.txt");
		ensureExistsInWorkspace(projects[0], true);
		ensureDoesNotExistInWorkspace(teamPrivate);

		FussyProgressMonitor monitor = new FussyProgressMonitor();
		teamPrivate.create(getRandomContents(), IResource.TEAM_PRIVATE, monitor);
		monitor.assertUsedUp();
		assertTrue("1.0", teamPrivate.isTeamPrivateMember());
		assertTrue("1.1", !teamPrivate.isDerived());

		monitor.prepare();
		teamPrivate.delete(false, monitor);
		monitor.assertUsedUp();
		monitor.prepare();
		teamPrivate.create(getRandomContents(), IResource.NONE, monitor);
		monitor.assertUsedUp();
		assertTrue("2.0", !teamPrivate.isTeamPrivateMember());
		assertTrue("2.1", !teamPrivate.isDerived());
	}

	@Test
	public void testFileCreation() throws Exception {
		IFile fileWithoutInput = projects[0].getFile("file1");
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		assertTrue("1.0", !fileWithoutInput.exists());
		monitor.prepare();
		fileWithoutInput.create(null, true, monitor);
		monitor.assertUsedUp();
		assertTrue("1.2", fileWithoutInput.exists());

		// creation with empty content
		IFile emptyFile = projects[0].getFile("file2");
		assertTrue("2.0", !emptyFile.exists());
		String contents = "";
		monitor.prepare();
		emptyFile.create(getContents(contents), true, monitor);
		monitor.assertUsedUp();
		assertTrue("2.2", emptyFile.exists());
		try (InputStream stream = emptyFile.getContents(false)) {
			assertTrue("2.4", stream.available() == 0);
		}
		assertTrue("2.6", compareContent(emptyFile.getContents(false), getContents(contents)));

		// creation with random content
		IFile fileWithRandomContent = projects[0].getFile("file3");
		assertTrue("3.0", !fileWithRandomContent.exists());
		contents = getRandomString();
		monitor.prepare();
		fileWithRandomContent.create(getContents(contents), true, monitor);
		monitor.assertUsedUp();
		assertTrue("3.2", fileWithRandomContent.exists());
		assertTrue("3.2", compareContent(fileWithRandomContent.getContents(false), getContents(contents)));

		// try to create a file over a folder that exists
		IFolder folder = projects[0].getFolder("folder1");
		monitor.prepare();
		folder.create(true, true, monitor);
		monitor.assertUsedUp();
		assertTrue("4.1", folder.exists());

		IFile fileOnFolder = projects[0].getFile("folder1");
		monitor.prepare();
		assertThrows(CoreException.class, () -> fileOnFolder.create(null, true, monitor));
		monitor.assertUsedUp();
		assertTrue("4.3", folder.exists());
		assertTrue("4.4", !fileOnFolder.exists());

		// try to create a file under a non-existent parent
		folder = projects[0].getFolder("folder2");
		assertTrue("5.0", !folder.exists());
		IFile fileUnderNonExistentParent = folder.getFile("file4");
		monitor.prepare();
		assertThrows(CoreException.class, () -> fileUnderNonExistentParent.create(null, true, monitor));
		monitor.assertUsedUp();
		assertTrue("5.2", !folder.exists());
		assertTrue("5.3", !fileUnderNonExistentParent.exists());

		//create from stream that throws exceptions
		IFile fileFromStream = projects[0].getFile("file2");
		ensureDoesNotExistInWorkspace(fileFromStream);
		ensureDoesNotExistInFileSystem(fileFromStream);

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
		ensureDoesNotExistInWorkspace(target);
		ensureDoesNotExistInFileSystem(target);

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
		assertTrue("1.0", target.exists());
		monitor.prepare();
		target.delete(true, monitor);
		monitor.assertUsedUp();
		assertTrue("1.1", !target.exists());
	}

	@Test
	public void testFileEmptyDeletion() throws Throwable {
		IFile target = projects[0].getFile("file1");
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		target.create(getContents(""), true, monitor);
		monitor.assertUsedUp();
		assertTrue("1.0", target.exists());
		monitor.prepare();
		target.delete(true, monitor);
		monitor.assertUsedUp();
		assertTrue("1.1", !target.exists());
	}

	@Test
	public void testFileInFolderCreation() throws CoreException {
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		IFolder folder = projects[0].getFolder("folder1");
		folder.create(false, true, monitor);
		monitor.assertUsedUp();

		IFile target = folder.getFile("file1");
		monitor.prepare();
		target.create(getRandomContents(), true, monitor);
		monitor.assertUsedUp();
		assertTrue("1.1", target.exists());
	}

	@Test
	public void testFileInFolderCreation1() throws Throwable {
		IFolder folder = projects[0].getFolder("folder1");
		folder.create(false, true, null);

		IFile target = folder.getFile("file1");
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		target.create(getRandomContents(), true, monitor);
		monitor.assertUsedUp();
		assertTrue("1.0", target.exists());
	}

	@Test
	public void testFileInFolderCreation2() throws CoreException {
		IFolder folder = projects[0].getFolder("folder1");
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		folder.create(false, true, monitor);
		monitor.assertUsedUp();

		IFile target = folder.getFile("file1");
		monitor.prepare();
		target.create(getRandomContents(), true, monitor);
		monitor.assertUsedUp();
		assertTrue("1.1", target.exists());
	}

	@Test
	public void testFileMove() throws Throwable {
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		IFile target = projects[0].getFile("file1");
		target.create(getRandomContents(), true, monitor);
		monitor.assertUsedUp();

		IFile destination = projects[0].getFile("file2");
		monitor.prepare();
		target.move(destination.getFullPath(), true, monitor);
		monitor.assertUsedUp();

		assertTrue("1.0", destination.exists());
		assertTrue("1.1", !target.exists());
	}

	@Test
	public void testFileOverFolder() throws Throwable {
		IFolder existing = projects[0].getFolder("ExistingFolder");
		IFile target = projects[0].getFile("ExistingFolder");

		FussyProgressMonitor monitor = new FussyProgressMonitor();
		assertThrows(CoreException.class, () -> target.create(null, true, monitor));
		monitor.assertUsedUp();
		assertTrue("1.1", existing.exists());
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
			public void cleanUp(Object[] args, int count) throws CoreException {
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
		String testString = getRandomString();
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		target.create(null, false, null);
		target.setContents(getContents(testString), true, false, monitor);
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
			assertTrue("1.0 " + name, !file.exists());
			assertThrows(CoreException.class, () -> file.create(getRandomContents(), true, monitor));
			monitor.sanityCheck();
			assertTrue("1.2 " + name, !file.exists());
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
			assertTrue("2.0 " + name, !file.exists());
			monitor.prepare();
			file.create(getRandomContents(), true, monitor);
			monitor.assertUsedUp();
			assertTrue("2.2 " + name, file.exists());
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
			public void cleanUp(Object[] args, int count) throws CoreException {
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

		String testString = getRandomString();
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		target.setContents(getContents(testString), true, false, monitor);
		monitor.assertUsedUp();

		try (InputStream content = target.getContents(false)) {
			assertTrue("get not equal set", compareContent(content, getContents(testString)));
		}
	}

	@Test
	public void testSetGetFolderPersistentProperty() throws Throwable {
		IResource target = getWorkspace().getRoot().getFile(IPath.fromOSString("/Project/File.txt"));
		String value = "this is a test property value";
		QualifiedName name = new QualifiedName("itp-test", "testProperty");
		// getting/setting persistent properties on non-existent resources should throw an exception
		ensureDoesNotExistInWorkspace(target);
		assertThrows(CoreException.class, () -> target.getPersistentProperty(name));
		assertThrows(CoreException.class, () -> target.setPersistentProperty(name, value));

		ensureExistsInWorkspace(target, true);
		target.setPersistentProperty(name, value);
		// see if we can get the property
		assertTrue("2.0", target.getPersistentProperty(name).equals(value));
		// see what happens if we get a non-existant property
		QualifiedName nonExistentPropertyName = new QualifiedName("itp-test", "testNonProperty");
		assertNull("2.1", target.getPersistentProperty(nonExistentPropertyName));

		//set a persistent property with null qualifier
		QualifiedName nullQualifierName = new QualifiedName(null, "foo");
		assertThrows(CoreException.class, () -> target.setPersistentProperty(nullQualifierName, value));
	}
}
