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
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.tests.harness.FussyProgressMonitor;

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
		fail("Should not get here in FileTest.existsAndOpen");
		return false;
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
		IFile file = container.getFile(new Path(DOES_NOT_EXIST));
		nonExistingFiles.add(file);
		allFiles.add(file);

		//exists in file system only
		file = container.getFile(new Path(LOCAL_ONLY));
		localOnlyFiles.add(file);
		allFiles.add(file);

		if (existsAndOpen(container)) {

			//existing file
			file = container.getFile(new Path(EXISTING));
			existingFiles.add(file);
			allFiles.add(file);

			//exists in workspace only
			file = container.getFile(new Path(WORKSPACE_ONLY));
			workspaceOnlyFiles.add(file);
			allFiles.add(file);

			//exists in both but is out of sync
			file = container.getFile(new Path(OUT_OF_SYNC));
			outOfSyncFiles.add(file);
			allFiles.add(file);
		}
	}

	/**
	 * Returns some interesting files.  These files are created
	 * during setup.
	 */
	public IFile[] interestingFiles() {
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
	public void refreshFile(IFile file) {
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
	public void refreshFiles() {
		for (IFile file : allFiles) {
			refreshFile(file);
		}
	}

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		try {
			generateInterestingFiles();
		} catch (CoreException e) {
			fail("Failed in setup for FileTest", e);
		}
	}

	@Override
	protected void tearDown() throws Exception {
		getWorkspace().getRoot().refreshLocal(IResource.DEPTH_INFINITE, null);
		ensureDoesNotExistInWorkspace(getWorkspace().getRoot());
		super.tearDown();
	}

	public void testAppendContents() {
		IFile target = projects[0].getFile("file1");
		try {
			target.create(getContents("abc"), false, null);
		} catch (CoreException e) {
			fail("1.0", e);
		}

		try {
			target.appendContents(getContents("def"), false, false, null);
		} catch (CoreException e) {
			fail("2.0", e);
		}

		InputStream content = null;
		try {
			content = target.getContents(false);
			assertTrue("3.0", compareContent(content, getContents("abcdef")));
		} catch (CoreException e) {
			fail("3.1", e);
		}
	}

	public void testAppendContents2() {
		IFile file = projects[0].getFile("file1");
		ensureDoesNotExistInWorkspace(file);

		// If force=true, IFile is non-local, file exists in local file system:
		// make IFile local, append contents (the thinking being that this file,
		// though marked non-local, was in fact local awaiting discovery; so
		// force=true says we it is ok to make file local and proceed as per normal)
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		try {
			// setup
			file.create(null, false, monitor);
			monitor.assertUsedUp();
			assertTrue("1.0", !file.isLocal(IResource.DEPTH_ZERO));
			assertTrue("1.1", !file.getLocation().toFile().exists());
			ensureExistsInFileSystem(file);
			assertTrue("1.2", !file.isLocal(IResource.DEPTH_ZERO));
		} catch (CoreException e) {
			fail("1.3", e);
		}
		try {
			monitor.prepare();
			file.appendContents(getRandomContents(), IResource.FORCE, monitor);
			monitor.assertUsedUp();
		} catch (CoreException e) {
			fail("1.4", e);
		}
		assertTrue("1.5", file.isLocal(IResource.DEPTH_ZERO));
		assertTrue("1.6", file.getLocation().toFile().exists());
		// cleanup
		ensureDoesNotExistInWorkspace(file);

		// If force=true, IFile is non-local, file does not exist in local file system:
		// fail - file not local (this file is not local for real - cannot append
		// something to a file that you don't have)
		try {
			// setup
			monitor.prepare();
			file.create(null, false, monitor);
			monitor.assertUsedUp();
			assertTrue("2.0", !file.isLocal(IResource.DEPTH_ZERO));
			assertTrue("2.1", !file.getLocation().toFile().exists());
		} catch (CoreException e) {
			fail("2.2", e);
		}
		try {
			monitor.prepare();
			file.appendContents(getRandomContents(), IResource.FORCE, monitor);
			fail("2.3");
		} catch (CoreException e) {
			// should fail
			monitor.sanityCheck();
		}
		assertTrue("2.4", !file.isLocal(IResource.DEPTH_ZERO));
		// cleanup
		ensureDoesNotExistInWorkspace(file);

		// If force=false, IFile is non-local, file exists in local file system:
		// fail - file not local
		try {
			// setup
			monitor.prepare();
			file.create(null, false, monitor);
			monitor.assertUsedUp();
			assertTrue("3.0", !file.isLocal(IResource.DEPTH_ZERO));
			assertTrue("3.1", !file.getLocation().toFile().exists());
			ensureExistsInFileSystem(file);
			assertTrue("3.2", !file.isLocal(IResource.DEPTH_ZERO));
		} catch (CoreException e) {
			fail("3.3", e);
		}
		try {
			monitor.prepare();
			file.appendContents(getRandomContents(), IResource.NONE, monitor);
			monitor.assertUsedUp();
			fail("3.4");
		} catch (CoreException e) {
			// should fail
		}
		assertTrue("3.5", !file.isLocal(IResource.DEPTH_ZERO));
		// cleanup
		ensureDoesNotExistInWorkspace(file);

		// If force=false, IFile is non-local, file does not exist in local file system:
		// fail - file not local
		try {
			// setup
			monitor.prepare();
			file.create(null, false, monitor);
			monitor.assertUsedUp();
			assertTrue("4.0", !file.isLocal(IResource.DEPTH_ZERO));
			assertTrue("4.1", !file.getLocation().toFile().exists());
		} catch (CoreException e) {
			fail("4.2", e);
		}
		try {
			monitor.prepare();
			file.appendContents(getRandomContents(), IResource.NONE, monitor);
			fail("4.3");
		} catch (CoreException e) {
			// should fail
			monitor.sanityCheck();
		}
		assertTrue("4.4", !file.isLocal(IResource.DEPTH_ZERO));
		// cleanup
		ensureDoesNotExistInWorkspace(file);
	}

	/**
	 * Performs black box testing of the following method:
	 *     void create(InputStream, boolean, IProgressMonitor)
	 */
	public void testCreate() {
		Object[][] inputs = new Object[][] {interestingFiles(), interestingStreams(), TRUE_AND_FALSE, PROGRESS_MONITORS};
		new TestPerformer("IFileTest.testCreate") {
			@Override
			public void cleanUp(Object[] args, int count) {
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

	public void testCreateDerived() {
		IFile derived = projects[0].getFile("derived.txt");
		ensureExistsInWorkspace(projects[0], true);
		ensureDoesNotExistInWorkspace(derived);

		FussyProgressMonitor monitor = new FussyProgressMonitor();
		try {
			derived.create(getRandomContents(), IResource.DERIVED, monitor);
			monitor.assertUsedUp();
		} catch (CoreException e) {
			fail("0.99", e);
		}
		assertTrue("1.0", derived.isDerived());
		assertTrue("1.1", !derived.isTeamPrivateMember());
		try {
			monitor.prepare();
			derived.delete(false, monitor);
			monitor.assertUsedUp();
			monitor.prepare();
			derived.create(getRandomContents(), IResource.NONE, monitor);
			monitor.assertUsedUp();
		} catch (CoreException e) {
			fail("1.99", e);
		}
		assertTrue("2.0", !derived.isDerived());
		assertTrue("2.1", !derived.isTeamPrivateMember());
	}

	public void testDeltaOnCreateDerived() {
		IFile derived = projects[0].getFile("derived.txt");
		ensureExistsInWorkspace(projects[0], true);

		ResourceDeltaVerifier verifier = new ResourceDeltaVerifier();
		getWorkspace().addResourceChangeListener(verifier, IResourceChangeEvent.POST_CHANGE);

		verifier.addExpectedChange(derived, IResourceDelta.ADDED, IResource.NONE);

		try {
			FussyProgressMonitor monitor = new FussyProgressMonitor();
			derived.create(getRandomContents(), IResource.FORCE | IResource.DERIVED, monitor);
			monitor.assertUsedUp();
		} catch (CoreException e) {
			fail("1.0", e);
		}

		assertTrue("2.0", verifier.isDeltaValid());
	}

	public void testCreateDerivedTeamPrivate() {
		IFile teamPrivate = projects[0].getFile("teamPrivateDerived.txt");
		ensureExistsInWorkspace(projects[0], true);
		ensureDoesNotExistInWorkspace(teamPrivate);

		FussyProgressMonitor monitor = new FussyProgressMonitor();
		try {
			teamPrivate.create(getRandomContents(), IResource.TEAM_PRIVATE | IResource.DERIVED, monitor);
			monitor.assertUsedUp();
		} catch (CoreException e) {
			fail("0.99", e);
		}
		assertTrue("1.0", teamPrivate.isTeamPrivateMember());
		assertTrue("1.1", teamPrivate.isDerived());
		try {
			monitor.prepare();
			teamPrivate.delete(false, monitor);
			monitor.assertUsedUp();
			monitor.prepare();
			teamPrivate.create(getRandomContents(), IResource.NONE, monitor);
			monitor.assertUsedUp();
		} catch (CoreException e) {
			fail("1.99", e);
		}
		assertTrue("2.0", !teamPrivate.isTeamPrivateMember());
		assertTrue("2.1", !teamPrivate.isDerived());
	}

	public void testCreateTeamPrivate() {
		IFile teamPrivate = projects[0].getFile("teamPrivate.txt");
		ensureExistsInWorkspace(projects[0], true);
		ensureDoesNotExistInWorkspace(teamPrivate);

		FussyProgressMonitor monitor = new FussyProgressMonitor();
		try {
			teamPrivate.create(getRandomContents(), IResource.TEAM_PRIVATE, monitor);
			monitor.assertUsedUp();
		} catch (CoreException e) {
			fail("0.99", e);
		}

		assertTrue("1.0", teamPrivate.isTeamPrivateMember());
		assertTrue("1.1", !teamPrivate.isDerived());
		try {
			monitor.prepare();
			teamPrivate.delete(false, monitor);
			monitor.assertUsedUp();
			monitor.prepare();
			teamPrivate.create(getRandomContents(), IResource.NONE, monitor);
			monitor.assertUsedUp();
		} catch (CoreException e) {
			fail("1.99", e);
		}
		assertTrue("2.0", !teamPrivate.isTeamPrivateMember());
		assertTrue("2.1", !teamPrivate.isDerived());
	}

	public void testFileCreation() {
		IFile target = projects[0].getFile("file1");
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		assertTrue("1.0", !target.exists());
		try {
			monitor.prepare();
			target.create(null, true, monitor);
			monitor.assertUsedUp();
		} catch (CoreException e) {
			fail("1.1", e);
		}
		assertTrue("1.2", target.exists());

		// creation with empty content
		target = projects[0].getFile("file2");
		assertTrue("2.0", !target.exists());
		String contents = "";
		try {
			monitor.prepare();
			target.create(getContents(contents), true, monitor);
			monitor.assertUsedUp();
		} catch (CoreException e) {
			fail("2.1", e);
		}
		assertTrue("2.2", target.exists());
		InputStream stream = null;
		try {
			stream = target.getContents(false);
		} catch (CoreException e) {
			fail("2.3", e);
		}
		try {
			assertTrue("2.4", stream.available() == 0);
		} catch (IOException e) {
			fail("2.5", e);
		}
		try {
			stream.close();
		} catch (IOException e) {
			fail("2.55", e);
		}
		try {
			assertTrue("2.6", compareContent(target.getContents(false), getContents(contents)));
		} catch (CoreException e) {
			fail("2.7", e);
		}

		// creation with random content
		target = projects[0].getFile("file3");
		assertTrue("3.0", !target.exists());
		contents = getRandomString();
		try {
			monitor.prepare();
			target.create(getContents(contents), true, monitor);
			monitor.assertUsedUp();
		} catch (CoreException e) {
			fail("3.1", e);
		}
		assertTrue("3.2", target.exists());
		try {
			assertTrue("3.2", compareContent(target.getContents(false), getContents(contents)));
		} catch (CoreException e) {
			fail("3.3", e);
		}

		// try to create a file over a folder that exists
		IFolder folder = projects[0].getFolder("folder1");
		try {
			monitor.prepare();
			folder.create(true, true, monitor);
			monitor.assertUsedUp();
		} catch (CoreException e) {
			fail("4.0", e);
		}
		assertTrue("4.1", folder.exists());
		target = projects[0].getFile("folder1");
		try {
			monitor.prepare();
			target.create(null, true, monitor);
			monitor.assertUsedUp();
			fail("4.2");
		} catch (CoreException e) {
			// expected
		}
		assertTrue("4.3", folder.exists());
		assertTrue("4.4", !target.exists());

		// try to create a file under a non-existent parent
		folder = projects[0].getFolder("folder2");
		assertTrue("5.0", !folder.exists());
		target = folder.getFile("file4");
		try {
			monitor.prepare();
			target.create(null, true, monitor);
			monitor.assertUsedUp();
			fail("5.1");
		} catch (CoreException e) {
			// expected
		}
		assertTrue("5.2", !folder.exists());
		assertTrue("5.3", !target.exists());

		//create from stream that throws exceptions
		target = projects[0].getFile("file2");
		ensureDoesNotExistInWorkspace(target);
		ensureDoesNotExistInFileSystem(target);

		InputStream content = new InputStream() {
			@Override
			public int read() throws IOException {
				throw new IOException();
			}
		};
		try {
			monitor.prepare();
			target.create(content, false, monitor);
			monitor.assertUsedUp();
			fail("6.1");
		} catch (CoreException e) {
			// expected
		}
		assertDoesNotExistInWorkspace("6.2", target);
		assertDoesNotExistInFileSystem("6.3", target);

		// cleanup
		folder = projects[0].getFolder("folder1");
		try {
			monitor.prepare();
			folder.delete(false, monitor);
			monitor.assertUsedUp();
		} catch (CoreException e) {
			fail("7.0", e);
		}
		IFile file = projects[0].getFile("file1");
		try {
			monitor.prepare();
			file.delete(false, monitor);
			monitor.assertUsedUp();
		} catch (CoreException e) {
			fail("7.1", e);
		}
		file = projects[0].getFile("file2");
		try {
			monitor.prepare();
			file.delete(false, monitor);
			monitor.assertUsedUp();
		} catch (CoreException e) {
			fail("7.2", e);
		}
		file = projects[0].getFile("file3");
		try {
			monitor.prepare();
			file.delete(false, monitor);
			monitor.assertUsedUp();
		} catch (CoreException e) {
			fail("7.3", e);
		}
	}

	public void testFileCreation_Bug107188() {
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
		try {
			FussyProgressMonitor monitor = new FussyProgressMonitor();
			target.create(content, false, monitor);
			monitor.assertUsedUp();
			fail("1.0");
		} catch (OperationCanceledException e) {
			// expected
		} catch (CoreException e) {
			fail("2.0");
		}
		assertDoesNotExistInWorkspace("3.0", target);
		assertDoesNotExistInFileSystem("4.0", target);
	}

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

	public void testFileInFolderCreation() {
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		IFolder folder = projects[0].getFolder("folder1");
		try {
			folder.create(false, true, monitor);
			monitor.assertUsedUp();
		} catch (CoreException e) {
			fail("1.0", e);
		}

		IFile target = folder.getFile("file1");
		try {
			monitor.prepare();
			target.create(getRandomContents(), true, monitor);
			monitor.assertUsedUp();
			assertTrue("1.1", target.exists());
		} catch (CoreException e) {
			fail("1.2", e);
		}
	}

	public void testFileInFolderCreation1() throws Throwable {
		IFolder folder = projects[0].getFolder("folder1");
		folder.create(false, true, null);

		IFile target = folder.getFile("file1");
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		target.create(getRandomContents(), true, monitor);
		monitor.assertUsedUp();
		assertTrue("1.0", target.exists());
	}

	public void testFileInFolderCreation2() {

		IFolder folder = projects[0].getFolder("folder1");
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		try {
			folder.create(false, true, monitor);
			monitor.assertUsedUp();
		} catch (CoreException e) {
			fail("1.0", e);
		}

		IFile target = folder.getFile("file1");
		try {
			monitor.prepare();
			target.create(getRandomContents(), true, monitor);
			monitor.assertUsedUp();
			assertTrue("1.1", target.exists());
		} catch (CoreException e) {
			fail("1.2", e);
		}
	}

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

	public void testFileOverFolder() throws Throwable {
		IFolder existing = projects[0].getFolder("ExistingFolder");
		IFile target = projects[0].getFile("ExistingFolder");

		try {
			FussyProgressMonitor monitor = new FussyProgressMonitor();
			target.create(null, true, monitor);
			monitor.assertUsedUp();
			fail("Should not be able to create file over folder");
		} catch (CoreException e) {
			assertTrue("1.1", existing.exists());
			return;
		}
	}

	/**
	 * Performs black box testing of the following method:
	 *     InputStream getContents()
	 */
	public void testGetContents() {
		Object[][] inputs = new Object[][] {interestingFiles()};
		new TestPerformer("IFileTest.testGetContents") {
			@Override
			public void cleanUp(Object[] args, int count) {
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

	public void testGetContents2() throws IOException {
		IFile target = projects[0].getFile("file1");
		String testString = getRandomString();
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		try {
			target.create(null, false, null);
			target.setContents(getContents(testString), true, false, monitor);
			monitor.assertUsedUp();
		} catch (CoreException e) {
			fail("0.0", e);
		}

		ensureOutOfSync(target);

		InputStream content = null;
		try {
			try {
				content = target.getContents(false);
			} finally {
				if (content != null) {
					content.close();
				}
			}
			fail("1.0");
		} catch (CoreException e) {
			// Ok, the file is out is sync.
			assertEquals("1.1", IResourceStatus.OUT_OF_SYNC_LOCAL, e.getStatus().getCode());
		}

		try {
			try {
				content = target.getContents(true);
			} finally {
				if (content != null) {
					content.close();
				}
			}
		} catch (CoreException e) {
			fail("2.1", e);
		}

		try {
			try {
				content = target.getContents(false);
			} finally {
				if (content != null) {
					content.close();
				}
			}
			fail("3.0");
		} catch (CoreException e) {
			// Ok, the file is out is sync.
			assertEquals("3.1", IResourceStatus.OUT_OF_SYNC_LOCAL, e.getStatus().getCode());
		}
		content = new InputStream() {
			@Override
			public int read() throws IOException {
				throw new IOException();
			}
		};
		try {
			monitor.prepare();
			target.setContents(content, IResource.NONE, monitor);
			fail("4.1");
		} catch (CoreException e) {
			// expected
			monitor.sanityCheck();
		}
		assertExistsInWorkspace("4.2", target);
		assertExistsInFileSystem("4.3", target);
	}

	/**
	 * Tests creation and manipulation of file names that are reserved on some platforms.
	 */
	public void testInvalidFileNames() {
		FussyProgressMonitor monitor = new FussyProgressMonitor();
		IProject project = projects[0];

		//should not be able to create a file with invalid path on any platform
		String[] names = new String[] {"", "/"};
		for (int i = 0; i < names.length; i++) {
			try {
				project.getFile(names[i]);
				fail("0.1." + i);
			} catch (RuntimeException e) {
				//should fail
			}
		}

		//do some tests with invalid names
		names = new String[0];
		if (isWindows()) {
			//invalid windows names
			names = new String[] {"a  ", "foo::bar", "prn", "nul", "con", "aux", "clock$", "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", "com9", "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9", "AUX", "con.foo", "LPT4.txt", "*", "?", "\"", "<", ">", "|"};
		} else {
			//invalid names on non-windows platforms
			names = new String[] {};
		}
		for (String name : names) {
			monitor.prepare();
			IFile file = project.getFile(Path.fromPortableString(name));
			assertTrue("1.0 " + name, !file.exists());
			try {
				file.create(getRandomContents(), true, monitor);
				fail("1.1 " + name);
			} catch (CoreException e) {
				// expected
				monitor.sanityCheck();
			}
			assertTrue("1.2 " + name, !file.exists());
		}

		//do some tests with valid names that are *almost* invalid
		if (isWindows()) {
			//these names are valid on windows
			names = new String[] {"  a", "hello.prn.txt", "null", "con3", "foo.aux", "lpt0", "com0", "com10", "lpt10", ",", "'", ";", "clock$.class"};
		} else {
			//these names are valid on non-windows platforms
			names = new String[] {"  a", "a  ", "foo:bar", "prn", "nul", "con", "aux", "clock$", "com1", "com2", "com3", "com4", "com5", "com6", "com7", "com8", "com9", "lpt1", "lpt2", "lpt3", "lpt4", "lpt5", "lpt6", "lpt7", "lpt8", "lpt9", "con.foo", "LPT4.txt", "*", "?", "\"", "<", ">", "|", "hello.prn.txt", "null", "con3", "foo.aux", "lpt0", "com0", "com10", "lpt10", ",", "'", ";"};
		}
		for (String name : names) {
			IFile file = project.getFile(name);
			assertTrue("2.0 " + name, !file.exists());
			try {
				monitor.prepare();
				file.create(getRandomContents(), true, monitor);
				monitor.assertUsedUp();
			} catch (CoreException e) {
				fail("2.1 " + name, e);
			}
			assertTrue("2.2 " + name, file.exists());
		}
	}

	/**
	 * Performs black box testing of the following method:
	 *     void setContents(InputStream, boolean, IProgressMonitor)
	 */
	public void testSetContents1() {
		Object[][] inputs = new Object[][] {interestingFiles(), interestingStreams(), TRUE_AND_FALSE, PROGRESS_MONITORS};
		new TestPerformer("IFileTest.testSetContents1") {
			@Override
			public void cleanUp(Object[] args, int count) {
				waitForRefresh();
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

	public void testSetContents2() {
		IFile target = projects[0].getFile("file1");
		try {
			target.create(null, false, null);
		} catch (CoreException e) {
			fail("0.0", e);
		}

		String testString = getRandomString();
		try {
			FussyProgressMonitor monitor = new FussyProgressMonitor();
			target.setContents(getContents(testString), true, false, monitor);
			monitor.assertUsedUp();
		} catch (CoreException e) {
			fail("1.0", e);
		}

		InputStream content = null;
		try {
			content = target.getContents(false);
			assertTrue("get not equal set", compareContent(content, getContents(testString)));
		} catch (CoreException e) {
			fail("2.0", e);
		} finally {
			try {
				content.close();
			} catch (IOException e) {
				// ignore
			}
		}
	}

	public void testSetGetFolderPersistentProperty() throws Throwable {
		IResource target = getWorkspace().getRoot().getFile(new Path("/Project/File.txt"));
		String value = "this is a test property value";
		QualifiedName name = new QualifiedName("itp-test", "testProperty");
		// getting/setting persistent properties on non-existent resources should throw an exception
		ensureDoesNotExistInWorkspace(target);
		try {
			target.getPersistentProperty(name);
			fail("1.0");
		} catch (CoreException e) {
			//this should happen
		}
		try {
			target.setPersistentProperty(name, value);
			fail("1.1");
		} catch (CoreException e) {
			//this should happen
		}

		ensureExistsInWorkspace(target, true);
		target.setPersistentProperty(name, value);
		// see if we can get the property
		assertTrue("2.0", target.getPersistentProperty(name).equals(value));
		// see what happens if we get a non-existant property
		name = new QualifiedName("itp-test", "testNonProperty");
		assertNull("2.1", target.getPersistentProperty(name));

		//set a persistent property with null qualifier
		name = new QualifiedName(null, "foo");
		try {
			target.setPersistentProperty(name, value);
			fail("3.0");
		} catch (CoreException e) {
			//expect an exception
		}
	}
}
