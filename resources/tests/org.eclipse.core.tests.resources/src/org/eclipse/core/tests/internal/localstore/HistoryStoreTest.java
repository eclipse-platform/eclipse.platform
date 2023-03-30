/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.core.tests.internal.localstore;

import java.io.*;
import java.util.*;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.internal.localstore.IHistoryStore;
import org.eclipse.core.internal.resources.*;
import org.eclipse.core.internal.utils.UniversalUniqueIdentifier;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.tests.resources.ResourceTest;

/**
 * This class defines all tests for the HistoryStore Class.
 */

public class HistoryStoreTest extends ResourceTest {

	static class LogListenerVerifier implements ILogListener {
		List<Integer> actual = new ArrayList<>();
		List<Integer> expected = new ArrayList<>();

		void addExpected(int statusCode) {
			expected.add(Integer.valueOf(statusCode));
		}

		String dump() {
			StringBuilder buffer = new StringBuilder();
			buffer.append("Expected:\n");
			for (Integer integer : expected) {
				buffer.append("\t" + integer + "\n");
			}
			buffer.append("Actual:\n");
			for (Integer integer : actual) {
				buffer.append("\t" + integer + "\n");
			}
			return buffer.toString();
		}

		@Override
		public void logging(IStatus status, String plugin) {
			actual.add(Integer.valueOf(status.getCode()));
		}

		void reset() {
			expected = new ArrayList<>();
			actual = new ArrayList<>();
		}

		void verify() throws VerificationFailedException {
			String message;
			if (expected.size() != actual.size()) {
				message = "Expected size: " + expected.size() + " does not equal actual size: " + actual.size() + "\n";
				message += dump();
				throw new VerificationFailedException(message);
			}
			for (Integer status : expected) {
				if (!actual.contains(status)) {
					message = "Expected and actual results differ.\n";
					message += dump();
					throw new VerificationFailedException(message);
				}
			}
		}
	}

	static class VerificationFailedException extends Exception {
		/**
		 * All serializable objects should have a stable serialVersionUID
		 */
		private static final long serialVersionUID = 1L;

		VerificationFailedException(String message) {
			super(message);
		}
	}

	public static void assertEquals(String tag, IFileState expected, IFileState actual) {
		assertEquals(tag + " path differs", expected.getFullPath(), actual.getFullPath());
		assertEquals(tag + " timestamp differs", expected.getModificationTime(), actual.getModificationTime());
		assertEquals(tag + " uuid differs", ((FileState) expected).getUUID(), ((FileState) actual).getUUID());
	}

	/*
	 * This little helper method makes sure that the history store is
	 * completely clean after it is invoked.  If a history store entry or
	 * a file is left, it may become part of the history for another file in
	 * another test (if this file has the same name).
	 */
	public static void wipeHistoryStore(IProgressMonitor monitor) {
		IHistoryStore store = ((Workspace) getWorkspace()).getFileSystemManager().getHistoryStore();
		// Remove all the entries from the history store index.  Note that
		// this does not cause the history store states to be removed.
		store.remove(Path.ROOT, monitor);
		// Now make sure all the states are really removed.
		store.removeGarbage();
	}

	private int numBytes(InputStream input) {
		int i = 0;
		int c = -1;
		try {
			c = input.read();
			while (c != -1) {
				i++;
				c = input.read();
			}
		} catch (IOException e) {
			i = 0;
		}
		if (c != -1) {
			i = 0;
		}
		return i;
	}

	public IWorkspaceDescription setMaxFileStates(int maxFileStates) throws CoreException {
		IWorkspaceDescription currentDescription = getWorkspace().getDescription();
		IWorkspaceDescription newDescription = getWorkspace().getDescription();
		newDescription.setMaxFileStates(maxFileStates);
		getWorkspace().setDescription(newDescription);
		return currentDescription;
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		wipeHistoryStore(getMonitor());
	}

	/**
	 * Test the various policies in place to ensure that the history store
	 * does not grow to unmanageable size.  The policies currently in place
	 * include:
	 * - store only a maximum number of states for each file
	 * - do not store files greater than some stated size
	 * - consider history store information stale after some specified period
	 *   of time and discard stale data
	 *
	 * History store states are always stored in order from the newest state to
	 * the oldest state.  This will be tested as well
	 *
	 * Scenario:
	 *   1. Create project					AddStateAndPoliciesProject
	 *   2. Create file	(file.txt)			random contents
	 *   3. Set policy information in the workspace description as follows:
	 * 			- don't store states older than 1 day
	 * 			- keep a maximum of 5 states per file
	 * 			- file states must be less than 1 Mb
	 *   4. Make 8 modifications to file.txt (causing 8 states to be created)
	 *   5. Ensure only 5 states were kept.
	 *   6. Ensure states are in order from newest to oldest.
	 *   7. Set policy such that file states must be no greater than 7 bytes.
	 *   8. Create a new file file1.txt
	 *   9. Add 10 bytes of data to this file.
	 *  10. Check each of the states for this file and ensure they are not
	 *      greater than 7 bytes.
	 *  11. Revert to policy in #3
	 *  12. Make sure we still have 5 states for file.txt (the first file we
	 *      worked with)
	 *  13. Change the policy so that data older than 10 seconds is stale.
	 *  14. Wait 12 seconds (make it longer than 10 seconds to ensure we don't
	 *      encounter granularity issues).
	 *  15. Check file states.  There should be none left.
	 */
	public void testAddStateAndPolicies() {
		/* Create common objects. */
		IProject project = getWorkspace().getRoot().getProject("Project");
		IFile file = project.getFile("file.txt");
		try {
			project.create(getMonitor());
			project.open(getMonitor());
			file.create(getRandomContents(), true, getMonitor());
		} catch (CoreException e) {
			fail("0.0", e);
		}

		/* set local history policies */
		IWorkspaceDescription description = getWorkspace().getDescription();
		// longevity set to 1 day
		description.setFileStateLongevity(1000 * 3600 * 24);
		// keep a max of 5 file states
		description.setMaxFileStates(5);
		// max size of file = 1 Mb
		description.setMaxFileStateSize(1024 * 1024);
		try {
			getWorkspace().setDescription(description);
		} catch (CoreException e) {
			fail("0.1", e);
		}

		/* test max file states */
		for (int i = 0; i < 8; i++) {
			try {
				ensureOutOfSync(file);
				file.refreshLocal(IResource.DEPTH_ZERO, getMonitor());
				file.setContents(getRandomContents(), true, true, getMonitor());
			} catch (CoreException e) {
				fail("1.0", e);
			}
		}
		IFileState[] states = null;
		try {
			states = file.getHistory(getMonitor());
		} catch (CoreException e) {
			fail("1.01", e);
		}
		// Make sure we have 8 states as we haven't trimmed yet.
		assertEquals("1.02", 8, states.length);

		IFileState[] oldStates = states;

		try {
			getWorkspace().save(true, null);
			states = file.getHistory(getMonitor());
		} catch (CoreException e) {
			fail("1.1", e);
		}
		// We added 8 states.  Make sure we only have 5 (the max).
		assertEquals("1.2", description.getMaxFileStates(), states.length);

		// assert that states are in the correct order (newer ones first)
		long lastModified = states[0].getModificationTime();
		for (int i = 1; i < states.length; i++) {
			assertTrue("1.3." + i, lastModified >= states[i].getModificationTime());
			lastModified = states[i].getModificationTime();
		}

		// assert that the most recent states were preserved
		for (int i = 0; i < states.length; i++) {
			assertEquals("1.4." + i, oldStates[i], states[i]);
		}

		/* test max file state size */
		description.setMaxFileStates(15);
		// max size of file = 7 bytes
		description.setMaxFileStateSize(7);
		try {
			getWorkspace().setDescription(description);
		} catch (CoreException e) {
			fail("2.0.0", e);
		}
		file = project.getFile("file1.txt");
		try {
			file.create(new ByteArrayInputStream(new byte[0]), true, getMonitor());
		} catch (CoreException e) {
			fail("2.0", e);
		}
		// Add 10 bytes to exceed the max file state size.
		for (int i = 0; i < 10; i++) {
			try {
				file.appendContents(getContents("a"), true, true, getMonitor());
			} catch (CoreException e) {
				fail("2.1", e);
			}
		}
		try {
			getWorkspace().save(true, null);
			states = file.getHistory(getMonitor());
			// #states = size + 1 for the 0 byte length file to begin with.
			for (int i = 0; i < states.length; i++) {
				int bytesRead = numBytes(states[i].getContents());
				assertTrue("2.2." + i, bytesRead <= description.getMaxFileStateSize());
			}
		} catch (CoreException e) {
			fail("2.3", e);
		}

		/* test max file longevity */
		// use the file of the first test
		file = project.getFile("file.txt");
		// 1 day
		description.setFileStateLongevity(1000 * 3600 * 24);
		description.setMaxFileStates(5);
		// 1 Mb
		description.setMaxFileStateSize(1024 * 1024);
		// the description should be the same as the first test
		try {
			getWorkspace().setDescription(description);
		} catch (CoreException e) {
			fail("3.0", e);
		}
		try {
			states = file.getHistory(getMonitor());
			// Make sure we have 5 states for file file.txt
			assertEquals("3.1", description.getMaxFileStates(), states.length);
		} catch (CoreException e) {
			fail("3.2", e);
		}
		// change policies
		// 10 seconds
		description.setFileStateLongevity(1000 * 10);
		// 1 Mb
		description.setMaxFileStateSize(1024 * 1024);
		try {
			getWorkspace().setDescription(description);
		} catch (CoreException e) {
			fail("3.3", e);
		}
		try {
			// sleep for more than 10 seconds (the granularity varies on
			// some machines so we will sleep for 12 seconds)
			Thread.sleep(1000 * 12);
		} catch (InterruptedException e) {
			fail("3.4", e);
		}
		try {
			getWorkspace().save(true, null);
			states = file.getHistory(getMonitor());
			// The 5 states for file.txt should have exceeded their longevity
			// and been removed.  Make sure we have 0 states left.
			assertEquals("3.5", 0, states.length);
		} catch (CoreException e) {
			fail("3.6", e);
		}
	}

	public void testBug28238() {
		// paths to mimic files in the workspace
		IProject project = getWorkspace().getRoot().getProject("myproject28238");
		IFolder folder = project.getFolder("myfolder");
		IFolder destinationFolder = project.getFolder("myfolder2");
		IFile file = folder.getFile("myfile.txt");
		IFile destinationFile = destinationFolder.getFile(file.getName());

		IHistoryStore store = ((Resource) getWorkspace().getRoot()).getLocalManager().getHistoryStore();

		// location of the data on disk
		IFileStore fileStore = getTempStore();
		createFileInFileSystem(fileStore);
		assertEquals("1.0" + " file already has state", 0, store.getStates(file.getFullPath(), getMonitor()).length);

		// add the data to the history store
		FileInfo fileInfo = new FileInfo(file.getName());
		fileInfo.setLastModified(System.currentTimeMillis());
		store.addState(file.getFullPath(), fileStore, fileInfo, true);
		IFileState[] states = store.getStates(file.getFullPath(), getMonitor());
		assertEquals("2.0", 1, states.length);

		// copy the data
		store.copyHistory(folder, destinationFolder, true);

		states = store.getStates(destinationFile.getFullPath(), getMonitor());
		assertEquals("3.0", 1, states.length);
	}

	public void testBug28603() {
		// paths to mimic files in the workspace
		IProject project = getWorkspace().getRoot().getProject("myproject28603");
		IFolder folder1 = project.getFolder("myfolder1");
		IFolder folder2 = project.getFolder("myfolder2");
		IFile file1 = folder1.getFile("myfile.txt");
		IFile file2 = folder2.getFile(file1.getName());

		// directly deletes history files if project did already existed:
		ensureExistsInWorkspace(new IResource[] {project, folder1, folder2}, true);
		try {
			file1.create(getRandomContents(), IResource.FORCE, getMonitor());
			file1.setContents(getRandomContents(), IResource.FORCE | IResource.KEEP_HISTORY, getMonitor());
			file1.setContents(getRandomContents(), IResource.FORCE | IResource.KEEP_HISTORY, getMonitor());
			file1.setContents(getRandomContents(), IResource.FORCE | IResource.KEEP_HISTORY, getMonitor());
			setMaxFileStates(50);
		} catch (CoreException e) {
			fail("0.0", e);
		}

		int maxStates = ResourcesPlugin.getWorkspace().getDescription().getMaxFileStates();

		IFileState[] states = null;
		try {
			states = file1.getHistory(getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}
		assertEquals("1.1", 3, states.length);
		int currentStates = 3;
		try {
			assertEquals("1.2" + " file2 already has history", 0, file2.getHistory(getMonitor()).length);
		} catch (CoreException e) {
			fail("1.3", e);
		}
		for (int i = 0; i < maxStates + 10; i++) {
			try {
				states = file1.getHistory(getMonitor());
			} catch (CoreException e) {
				fail("2.0." + i, e);
			}
			assertEquals("2.1." + i + " file1 states", currentStates, states.length);
			try {
				file1.move(file2.getFullPath(), true, true, getMonitor());
			} catch (CoreException e) {
				fail("2.2." + i, e);
			}

			try {
				states = file2.getHistory(getMonitor());
			} catch (CoreException e) {
				fail("2.3." + i, e);
			}
			currentStates = currentStates < maxStates ? currentStates + 1 : maxStates;
			assertEquals("2.4." + i + " file2 states", currentStates, states.length);
			try {
				file2.move(file1.getFullPath(), true, true, getMonitor());
			} catch (CoreException e) {
				fail("2.5." + i, e);
			}
			try {
				states = file1.getHistory(getMonitor());
			} catch (CoreException e) {
				fail("2.6." + i, e);
			}
			currentStates = currentStates < maxStates ? currentStates + 1 : maxStates;
			assertEquals("2.7." + i + " file1 states", currentStates, states.length);
		}
	}

	/*
	 * Test the functionality in store.clean() which is called to ensure
	 * the history store to ensure that the history store does not grow to
	 * unmanageable size.  The policies currently in place include:
	 * - store only a maximum number of states for each file
	 * - do not store files greater than some stated size
	 * - consider history store information stale after some specified period
	 *   of time and discard stale data
	 *
	 */
	public void testClean() {
		/* Create common objects. */
		IProject project = getWorkspace().getRoot().getProject("ProjectClean");
		IFile file = project.getFile("file.txt");
		try {
			project.create(getMonitor());
			project.open(getMonitor());
			file.create(getRandomContents(), true, getMonitor());
		} catch (CoreException e) {
			fail("0.0", e);
		}
		IHistoryStore store = ((Workspace) getWorkspace()).getFileSystemManager().getHistoryStore();
		// get another copy for changes
		IWorkspaceDescription description = getWorkspace().getDescription();

		/* test max file states */
		// 1 day
		description.setFileStateLongevity(1000 * 3600 * 24);
		// 500 states per file max.
		description.setMaxFileStates(500);
		// 1Mb max size
		description.setMaxFileStateSize(1024 * 1024);
		try {
			getWorkspace().setDescription(description);
		} catch (CoreException e) {
			fail("0.1", e);
		}

		// Set up 8 file states for this file when 500 are allowed
		for (int i = 0; i < 8; i++) {
			try {
				ensureOutOfSync(file);
				file.refreshLocal(IResource.DEPTH_ZERO, getMonitor());
				//			try {
				//				Thread.sleep(5000); // necessary because of lastmodified granularity in some file systems
				//			} catch (InterruptedException e) {
				//			}
				file.setContents(getRandomContents(), true, true, getMonitor());
			} catch (CoreException e) {
				fail("1.0", e);
			}
		}
		// All 8 states should exist.
		long oldLastModTimes[] = new long[8];
		try {
			IFileState[] states = file.getHistory(getMonitor());
			assertEquals("1.1", 8, states.length);
			for (int i = 0; i < 8; i++) {
				oldLastModTimes[i] = states[i].getModificationTime();
			}
		} catch (CoreException e) {
			fail("1.2", e);
		}

		// Set max. number of file states to be 3
		description.setMaxFileStates(3);
		try {
			getWorkspace().setDescription(description);
		} catch (CoreException e) {
			fail("2.0", e);
		}
		// Run 'clean' - should cause 5 of 8 states to be removed
		store.clean(getMonitor());
		// Restore max. number of states/file to 500
		description.setMaxFileStates(500);
		try {
			getWorkspace().setDescription(description);
		} catch (CoreException e) {
			fail("2.2", e);
		}

		// Check to ensure only 3 states remain.  Make sure these are the 3
		// newer states.
		try {
			IFileState[] states = file.getHistory(getMonitor());
			assertEquals("2.3", 3, states.length);
			// assert that states are in the correct order (newer ones first)
			long lastModified = states[0].getModificationTime();
			for (int i = 1; i < states.length; i++) {
				assertTrue("2.4." + i, lastModified >= states[i].getModificationTime());
				lastModified = states[i].getModificationTime();
			}
			// Make sure we kept the 3 newer states.
			for (int i = 0; i < states.length; i++) {
				assertTrue("2.5." + i, oldLastModTimes[i] == states[i].getModificationTime());
			}
		} catch (CoreException e) {
			fail("2.6", e);
		}

		/* test max file longevity */
		file = project.getFile("file.txt"); // use the file of the first test
		description.setFileStateLongevity(1000 * 3600 * 24); // 1 day
		description.setMaxFileStates(500);
		description.setMaxFileStateSize(1024 * 1024); // 1 Mb
		// the description should be the same as the first test
		try {
			getWorkspace().setDescription(description);
		} catch (CoreException e) {
			fail("3.0", e);
		}
		try {
			IFileState[] states = file.getHistory(getMonitor());
			assertEquals("3.1", 3, states.length);
		} catch (CoreException e) {
			fail("3.2", e);
		}
		// change policies
		// 10 seconds
		description.setFileStateLongevity(1000 * 10);
		// 1 Mb
		description.setMaxFileStateSize(1024 * 1024);
		try {
			getWorkspace().setDescription(description);
		} catch (CoreException e) {
			fail("4.0", e);
		}
		try {
			// sleep for 12 seconds (must exceed 10 seconds).  This should
			// cause all 3 states for file.txt to be considered stale.
			Thread.sleep(1000 * 12);
		} catch (InterruptedException e) {
			fail("4.1", e);
		}
		store.clean(getMonitor());
		// change policies - restore to original values
		// 1 day
		description.setFileStateLongevity(1000 * 3600 * 24);
		// 1 Mb
		description.setMaxFileStateSize(1024 * 1024);
		try {
			getWorkspace().setDescription(description);
		} catch (CoreException e) {
			fail("5.0", e);
		}
		// Ensure we have no state information left.  It should have been
		// considered stale.
		try {
			IFileState[] states = file.getHistory(getMonitor());
			assertEquals("5.1", 0, states.length);
		} catch (CoreException e) {
			fail("5.2", e);
		}
	}

	/**
	 * Copy case for History Store of folder when the local history is being
	 * copied.
	 *
	 * Scenario:
	 *   1. Create folder (folder1)
	 *   2. Create file						"content 1"
	 *   3. Set new content					"content 2"
	 *   4. Set new content					"content 3"
	 *   5. Copy folder
	 *   6. Set new content	to moved file	"content 4"
	 *   7. Set new content to moved file	"content 5"
	 *
	 * The original file should have two states available.
	 * But the copied file should have 4 states as it retains the states from
	 * before the copy took place as well.
	 */
	public void testCopyFolder() {
		String[] contents = {"content1", "content2", "content3", "content4", "content5"};
		// create common objects
		IProject project = getWorkspace().getRoot().getProject("CopyFolderProject");
		try {
			project.create(getMonitor());
			project.open(getMonitor());
		} catch (CoreException e) {
			fail("0.0", e);
		}

		IFile file = project.getFile("file1.txt");

		IFolder folder = project.getFolder("folder1");
		IFolder folder2 = project.getFolder("folder2");
		file = folder.getFile("file1.txt");
		try {
			// Setup folder1 and file1.txt with some local history
			folder.create(true, true, getMonitor());
			file.create(getContents(contents[0]), true, getMonitor());
			file.setContents(getContents(contents[1]), true, true, getMonitor());
			file.setContents(getContents(contents[2]), true, true, getMonitor());
			IFileState[] states = file.getHistory(getMonitor());
			assertEquals("1.0", 2, states.length);
			assertTrue("1.1", compareContent(getContents(contents[1]), states[0].getContents()));
			assertTrue("1.2", compareContent(getContents(contents[0]), states[1].getContents()));

			// Now do the move
			folder.copy(folder2.getFullPath(), true, getMonitor());

			// Check to make sure the file has been copied
			IFile file2 = folder2.getFile("file1.txt");
			assertTrue("1.3", file2.getFullPath().toString().endsWith("folder2/file1.txt"));

			// Give the new (copied file) some new contents
			file2.setContents(getContents(contents[3]), true, true, getMonitor());
			file2.setContents(getContents(contents[4]), true, true, getMonitor());

			// Check the local history of both files
			states = file.getHistory(getMonitor());
			assertEquals("2.0", 2, states.length);
			assertTrue("2.1", compareContent(getContents(contents[1]), states[0].getContents()));
			assertTrue("2.2", compareContent(getContents(contents[0]), states[1].getContents()));
			states = file2.getHistory(getMonitor());
			assertEquals("2.3", 4, states.length);
			assertTrue("2.4", compareContent(getContents(contents[3]), states[0].getContents()));
			assertTrue("2.5", compareContent(getContents(contents[2]), states[1].getContents()));
			assertTrue("2.6", compareContent(getContents(contents[1]), states[2].getContents()));
			assertTrue("2.7", compareContent(getContents(contents[0]), states[3].getContents()));
		} catch (CoreException e) {
			fail("2.8", e);
		}

		try {
			project.delete(true, getMonitor());
		} catch (CoreException e) {
			fail("3.0", e);
		}
	}

	/*
	 * This test is designed to exercise the public API method
	 * HistoryStore.copyHistory().  The following tests will be performed:
	 * - give a null source path
	 * - give a null destination path
	 * - give the same path for source and destination
	 * - give an invalid source path but a valid destination path
	 * - give an invalid destination path but a valid source path
	 */
	public void testCopyHistoryFile() {
		// Create a project, folder and file so we have some history store
		// Should have a project that appears as follows:
		// - project name TestCopyHistoryProject
		// - has one folder called folder1
		// - folder1 has one file called file1.txt
		// - file1.txt was created with initial data "content1"
		// - change data in file1.txt to be "content2"
		// - change data in file1.txt to be "content3"
		// As a result of the above, there should be 2 history store states for
		// file1.txt (one with "contents1" and the other with "contents2".

		String[] contents = {"content0", "content1", "content2", "content3", "content4"};
		// create common objects
		IProject project = getWorkspace().getRoot().getProject("TestCopyHistoryProject");
		ensureExistsInWorkspace(project, true);

		IFolder folder = project.getFolder("folder1");
		IFile file = folder.getFile("file1.txt");
		IFile file2 = folder.getFile("file2.txt");
		try {
			// Setup folder1 and file1.txt with some local history
			folder.create(true, true, getMonitor());
			file.create(getContents(contents[0]), true, getMonitor());
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				fail("0.0", e);
			}
			file.setContents(getContents(contents[1]), true, true, getMonitor());
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				fail("0.1", e);
			}
			file.setContents(getContents(contents[2]), true, true, getMonitor());
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				fail("0.2", e);
			}
			IFileState[] states = file.getHistory(getMonitor());
			assertEquals("1.0", 2, states.length);
			assertTrue("1.1", compareContent(getContents(contents[1]), states[0].getContents()));
			assertTrue("1.2", compareContent(getContents(contents[0]), states[1].getContents()));
			file2.create(getContents(contents[3]), true, getMonitor());
			file2.setContents(getContents(contents[4]), true, true, getMonitor());
		} catch (CoreException e) {
			fail("1.3", e);
		}

		// Run some tests with illegal arguments
		LogListenerVerifier verifier = new LogListenerVerifier();
		ILog log = ResourcesPlugin.getPlugin().getLog();
		log.addLogListener(verifier);

		// Test with null source and/or destination
		IHistoryStore store = ((Resource) file).getLocalManager().getHistoryStore();
		verifier.addExpected(IResourceStatus.INTERNAL_ERROR);
		store.copyHistory(null, null, false);
		try {
			verifier.verify();
		} catch (VerificationFailedException e) {
			fail("1.4 ", e);
		}
		verifier.reset();

		verifier.addExpected(IResourceStatus.INTERNAL_ERROR);
		store.copyHistory(null, file2, false);
		try {
			verifier.verify();
		} catch (VerificationFailedException e) {
			fail("1.5 ", e);
		}
		verifier.reset();

		verifier.addExpected(IResourceStatus.INTERNAL_ERROR);
		store.copyHistory(file, null, false);
		try {
			verifier.verify();
		} catch (VerificationFailedException e) {
			fail("1.6 ", e);
		}
		verifier.reset();

		// Try to copy the history store stuff to the same location
		verifier.addExpected(IResourceStatus.INTERNAL_ERROR);
		store.copyHistory(file, file, false);
		try {
			verifier.verify();
		} catch (VerificationFailedException e) {
			fail("1.7 ", e);
		}
		verifier.reset();

		// Remember to remove the log listener now that we are done
		// testing illegal arguments.
		log.removeLogListener(verifier);

		// Test a valid copy of a file
		store.copyHistory(file, file2, false);
		IFileState[] states = null;
		try {
			states = file2.getHistory(getMonitor());
		} catch (CoreException e) {
			fail("2.3");
		}
		assertEquals("2.4", 3, states.length);
		try {
			assertTrue("2.5", compareContent(getContents(contents[3]), states[0].getContents()));
			assertTrue("2.6", compareContent(getContents(contents[1]), states[1].getContents()));
			assertTrue("2.7", compareContent(getContents(contents[0]), states[2].getContents()));
		} catch (CoreException e) {
			fail("2.8");
		}
	}

	public void testCopyHistoryFolder() {
		String[] contents = {"content0", "content1", "content2", "content3", "content4"};
		// create common objects
		IProject project = getWorkspace().getRoot().getProject("TestCopyHistoryProject");
		ensureExistsInWorkspace(project, true);

		IFolder folder = project.getFolder("folder1");
		IFolder folder2 = project.getFolder("folder2");
		IFile file = folder.getFile("file1.txt");
		IFile file2 = folder2.getFile("file1.txt");
		try {
			// Setup folder1 and file1.txt with some local history
			folder.create(true, true, getMonitor());
			file.create(getContents(contents[0]), true, getMonitor());
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				fail("0.0", e);
			}
			file.setContents(getContents(contents[1]), true, true, getMonitor());
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				fail("0.1", e);
			}
			file.setContents(getContents(contents[2]), true, true, getMonitor());
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				fail("0.2", e);
			}
			IFileState[] states = file.getHistory(getMonitor());
			assertEquals("1.0", 2, states.length);
			assertTrue("1.1", compareContent(getContents(contents[1]), states[0].getContents()));
			assertTrue("1.2", compareContent(getContents(contents[0]), states[1].getContents()));
			folder2.create(true, true, getMonitor());
			file2.create(getContents(contents[3]), true, getMonitor());
			file2.setContents(getContents(contents[4]), true, true, getMonitor());
		} catch (CoreException e) {
			fail("1.9", e);
		}

		// Test a valid copy of a folder
		IHistoryStore store = ((Resource) file).getLocalManager().getHistoryStore();
		store.copyHistory(folder, folder2, false);
		IFileState[] states = null;
		try {
			states = file2.getHistory(getMonitor());
		} catch (CoreException e) {
			fail("2.3");
		}
		assertEquals("2.4", 3, states.length);
		try {
			assertTrue("2.5", compareContent(getContents(contents[3]), states[0].getContents()));
			assertTrue("2.6", compareContent(getContents(contents[1]), states[1].getContents()));
			assertTrue("2.7", compareContent(getContents(contents[0]), states[2].getContents()));
		} catch (CoreException e) {
			fail("2.8");
		}
	}

	public void testCopyHistoryProject() {
		String[] contents = {"content0", "content1", "content2", "content3", "content4"};
		// create common objects
		IProject project = getWorkspace().getRoot().getProject("TestCopyHistoryProject");
		IProject project2 = getWorkspace().getRoot().getProject("TestCopyHistoryProject2");
		ensureExistsInWorkspace(new IResource[] {project, project2}, true);

		IFolder folder = project.getFolder("folder1");
		IFolder folder2 = project2.getFolder("folder1");
		IFile file = folder.getFile("file1.txt");
		IFile file2 = folder2.getFile("file1.txt");
		try {
			// Setup folder1 and file1.txt with some local history
			folder.create(true, true, getMonitor());
			file.create(getContents(contents[0]), true, getMonitor());
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				fail("0.0", e);
			}
			file.setContents(getContents(contents[1]), true, true, getMonitor());
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				fail("0.1", e);
			}
			file.setContents(getContents(contents[2]), true, true, getMonitor());
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				fail("0.2", e);
			}
			IFileState[] states = file.getHistory(getMonitor());
			assertEquals("1.0", 2, states.length);
			assertTrue("1.1", compareContent(getContents(contents[1]), states[0].getContents()));
			assertTrue("1.2", compareContent(getContents(contents[0]), states[1].getContents()));
			folder2.create(true, true, getMonitor());
			file2.create(getContents(contents[3]), true, getMonitor());
			file2.setContents(getContents(contents[4]), true, true, getMonitor());
		} catch (CoreException e) {
			fail("1.9", e);
		}

		// Test a valid copy of a folder
		IHistoryStore store = ((Resource) file).getLocalManager().getHistoryStore();
		store.copyHistory(project, project2, false);
		IFileState[] states = null;
		try {
			states = file2.getHistory(getMonitor());
		} catch (CoreException e) {
			fail("2.3");
		}
		assertEquals("2.4", 3, states.length);
		try {
			assertTrue("2.5", compareContent(getContents(contents[3]), states[0].getContents()));
			assertTrue("2.6", compareContent(getContents(contents[1]), states[1].getContents()));
			assertTrue("2.7", compareContent(getContents(contents[0]), states[2].getContents()));
		} catch (CoreException e) {
			fail("2.8");
		}
	}

	public void testDelete() {
		// create common objects
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		try {
			project.create(getMonitor());
			project.open(getMonitor());
		} catch (CoreException e) {
			fail("0.0", e);
		}

		// test file
		IFile file = project.getFile("file.txt");
		try {
			file.create(getRandomContents(), true, getMonitor());
			file.setContents(getRandomContents(), true, true, getMonitor());
			file.setContents(getRandomContents(), true, true, getMonitor());

			// Check to see that there are only 2 states before the deletion
			IFileState[] states = file.getHistory(getMonitor());
			assertEquals("1.0", 2, states.length);

			// Delete the file.  This should add a state to the history store.
			file.delete(true, true, getMonitor());
			states = file.getHistory(getMonitor());
			assertEquals("1.1", 3, states.length);

			// Re-create the file.  This should not affect the history store.
			file.create(getRandomContents(), true, getMonitor());
			states = file.getHistory(getMonitor());
			assertEquals("1.2", 3, states.length);
		} catch (CoreException e) {
			fail("1.20", e);
		}

		// test folder
		IFolder folder = project.getFolder("folder");
		// Make sure this has a different name as the history store information
		// for the first 'file.txt' is likely still around.
		file = folder.getFile("file2.txt");
		try {
			folder.create(true, true, getMonitor());
			file.create(getRandomContents(), true, getMonitor());
			file.setContents(getRandomContents(), true, true, getMonitor());
			file.setContents(getRandomContents(), true, true, getMonitor());

			// There should only be 2 history store entries.
			IFileState[] states = file.getHistory(getMonitor());
			assertEquals("2.0", 2, states.length);

			// Delete the folder.  This should cause one more history store entry.
			folder.delete(true, true, getMonitor());
			states = file.getHistory(getMonitor());
			assertEquals("2.1", 3, states.length);

			// Re-create the folder.  There should be no new history store entries.
			folder.create(true, true, getMonitor());
			file.create(getRandomContents(), true, getMonitor());
			states = file.getHistory(getMonitor());
			assertEquals("2.2", 3, states.length);
		} catch (CoreException e) {
			fail("2.99", e);
		}

		try {
			project.delete(true, getMonitor());
		} catch (CoreException e) {
			fail("20.0", e);
		}
	}

	/**
	 * Test for existence of file states in the HistoryStore.
	 */
	public void testExists() throws Throwable {

		/* Create common objects. */
		IProject project = getWorkspace().getRoot().getProject("Project");
		IFile file = project.getFile("removeAllStatesFile.txt");
		try {
			project.create(getMonitor());
			project.open(getMonitor());
			file.create(getRandomContents(), true, getMonitor());
		} catch (CoreException e) {
			fail("0.0", e);
		}

		// Constant for the number of states we will create
		final int ITERATIONS = 20;

		/* Add multiple states for one file location. */
		for (int i = 0; i < ITERATIONS; i++) {
			try {
				file.setContents(getRandomContents(), true, true, getMonitor());
			} catch (CoreException e) {
				fail("3.0." + i, e);
			}
		}

		/* Valid Case: Test retrieved values. */
		IFileState[] states = null;
		try {
			states = file.getHistory(getMonitor());
		} catch (CoreException e) {
			fail("5.0", e);
		}
		// Make sure we have ITERATIONS number of states
		assertEquals("5.1", ITERATIONS, states.length);
		// Make sure that each of these states really exists in the filesystem.
		for (int i = 0; i < states.length; i++) {
			assertTrue("5.2." + i, states[i].exists());
		}
	}

	public void testFindDeleted() {
		// create common objects
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject("MyProject");
		try {
			project.create(getMonitor());
			project.open(getMonitor());

			IFile[] df = project.findDeletedMembersWithHistory(IResource.DEPTH_ONE, getMonitor());
			assertEquals("0.1", 0, df.length);
		} catch (CoreException e) {
			fail("0.0", e);
		}

		// test that a deleted file can be found
		IFile pfile = project.getFile("findDeletedFile.txt");
		try {
			// create and delete a file
			pfile.create(getRandomContents(), true, getMonitor());
			pfile.delete(true, true, getMonitor());

			// the deleted file should show up as a deleted member of project
			IFile[] df = project.findDeletedMembersWithHistory(IResource.DEPTH_ONE, getMonitor());
			assertEquals("0.1", 1, df.length);
			assertEquals("0.2", pfile, df[0]);

			df = project.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, getMonitor());
			assertEquals("0.3", 1, df.length);
			assertEquals("0.4", pfile, df[0]);

			df = project.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, getMonitor());
			assertEquals("0.5", 0, df.length);

			// the deleted file should show up as a deleted member of workspace root
			df = root.findDeletedMembersWithHistory(IResource.DEPTH_ONE, getMonitor());
			assertEquals("0.5.1", 0, df.length);

			df = root.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, getMonitor());
			assertEquals("0.5.2", 1, df.length);
			assertEquals("0.5.3", pfile, df[0]);

			df = root.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, getMonitor());
			assertEquals("0.5.4", 0, df.length);

			// recreate the file
			pfile.create(getRandomContents(), true, getMonitor());

			// the deleted file should no longer show up as a deleted member of project
			df = project.findDeletedMembersWithHistory(IResource.DEPTH_ONE, getMonitor());
			assertEquals("0.6", 0, df.length);

			df = project.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, getMonitor());
			assertEquals("0.7", 0, df.length);

			df = project.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, getMonitor());
			assertEquals("0.8", 0, df.length);

			// the deleted file should no longer show up as a deleted member of ws root
			df = root.findDeletedMembersWithHistory(IResource.DEPTH_ONE, getMonitor());
			assertEquals("0.8.1", 0, df.length);

			df = root.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, getMonitor());
			assertEquals("0.8.2", 0, df.length);

			df = root.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, getMonitor());
			assertEquals("0.8.3", 0, df.length);

		} catch (CoreException e) {
			fail("0.00", e);
		}

		// scrub the project
		try {
			project.delete(true, getMonitor());
			project.create(getMonitor());
			project.open(getMonitor());

			IFile[] df = project.findDeletedMembersWithHistory(IResource.DEPTH_ONE, getMonitor());
			assertEquals("0.9", 0, df.length);
		} catch (CoreException e) {
			fail("0.10", e);
		}

		// test folder
		IFolder folder = project.getFolder("folder");
		IFile file = folder.getFile("filex.txt");
		IFile folderAsFile = project.getFile(folder.getProjectRelativePath());
		try {
			// create and delete a file in a folder
			folder.create(true, true, getMonitor());
			file.create(getRandomContents(), true, getMonitor());
			file.delete(true, true, getMonitor());

			// the deleted file should show up as a deleted member
			IFile[] df = project.findDeletedMembersWithHistory(IResource.DEPTH_ONE, getMonitor());
			assertEquals("1.1", 0, df.length);

			df = project.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, getMonitor());
			assertEquals("1.2", 1, df.length);
			assertEquals("1.3", file, df[0]);

			df = project.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, getMonitor());
			assertEquals("1.4", 0, df.length);

			// recreate the file
			file.create(getRandomContents(), true, getMonitor());

			// the deleted file should no longer show up as a deleted member
			df = project.findDeletedMembersWithHistory(IResource.DEPTH_ONE, getMonitor());
			assertEquals("1.5", 0, df.length);

			df = project.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, getMonitor());
			assertEquals("1.6", 0, df.length);

			df = project.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, getMonitor());
			assertEquals("1.7", 0, df.length);

			// deleting the folder should bring it back
			folder.delete(true, true, getMonitor());

			// the deleted file should show up as a deleted member of project
			df = project.findDeletedMembersWithHistory(IResource.DEPTH_ONE, getMonitor());
			assertEquals("1.8", 0, df.length);

			df = project.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, getMonitor());
			assertEquals("1.9", 1, df.length);
			assertEquals("1.10", file, df[0]);

			df = project.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, getMonitor());
			assertEquals("1.11", 0, df.length);

			// create and delete a file where the folder was
			folderAsFile.create(getRandomContents(), true, getMonitor());
			folderAsFile.delete(true, true, getMonitor());
			folder.create(true, true, getMonitor());

			// the deleted file should show up as a deleted member of folder
			df = folder.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, getMonitor());
			assertEquals("1.12", 1, df.length);
			assertEquals("1.13", folderAsFile, df[0]);

			df = folder.findDeletedMembersWithHistory(IResource.DEPTH_ONE, getMonitor());
			assertEquals("1.14", 2, df.length);
			List<IFile> dfList = Arrays.asList(df);
			assertTrue("1.15", dfList.contains(file));
			assertTrue("1.16", dfList.contains(folderAsFile));

			df = folder.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, getMonitor());
			assertEquals("1.17", 2, df.length);
			dfList = Arrays.asList(df);
			assertTrue("1.18", dfList.contains(file));
			assertTrue("1.19", dfList.contains(folderAsFile));

		} catch (CoreException e) {
			fail("1.00", e);
		}

		// scrub the project
		try {
			project.delete(true, getMonitor());
			project.create(getMonitor());
			project.open(getMonitor());

			IFile[] df = project.findDeletedMembersWithHistory(IResource.DEPTH_ONE, getMonitor());
			assertEquals("1.50", 0, df.length);
		} catch (CoreException e) {
			fail("1.51", e);
		}

		// test a bunch of deletes
		folder = project.getFolder("folder");
		IFile file1 = folder.getFile("file1.txt");
		IFile file2 = folder.getFile("file2.txt");
		IFolder folder2 = folder.getFolder("folder2");
		IFile file3 = folder2.getFile("file3.txt");
		try {
			// create and delete a file in a folder
			folder.create(true, true, getMonitor());
			folder2.create(true, true, getMonitor());
			file1.create(getRandomContents(), true, getMonitor());
			file2.create(getRandomContents(), true, getMonitor());
			file3.create(getRandomContents(), true, getMonitor());
			folder.delete(true, true, getMonitor());

			// under root
			IFile[] df = root.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, getMonitor());
			assertEquals("3.1", 0, df.length);

			df = root.findDeletedMembersWithHistory(IResource.DEPTH_ONE, getMonitor());
			assertEquals("3.2", 0, df.length);

			df = root.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, getMonitor());
			assertEquals("3.3", 3, df.length);
			List<IFile> dfList = Arrays.asList(df);
			assertTrue("3.3.1", dfList.contains(file1));
			assertTrue("3.3.2", dfList.contains(file2));
			assertTrue("3.3.3", dfList.contains(file3));

			// under project
			df = project.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, getMonitor());
			assertEquals("3.4", 0, df.length);

			df = project.findDeletedMembersWithHistory(IResource.DEPTH_ONE, getMonitor());
			assertEquals("3.5", 0, df.length);

			df = project.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, getMonitor());
			assertEquals("3.6", 3, df.length);
			dfList = Arrays.asList(df);
			assertTrue("3.6.1", dfList.contains(file1));
			assertTrue("3.6.2", dfList.contains(file2));
			assertTrue("3.6.3", dfList.contains(file3));

			// under folder
			df = folder.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, getMonitor());
			assertEquals("3.7", 0, df.length);

			df = folder.findDeletedMembersWithHistory(IResource.DEPTH_ONE, getMonitor());
			assertEquals("3.8", 2, df.length);

			df = folder.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, getMonitor());
			assertEquals("3.9", 3, df.length);

			// under folder2
			df = folder2.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, getMonitor());
			assertEquals("3.10", 0, df.length);

			df = folder2.findDeletedMembersWithHistory(IResource.DEPTH_ONE, getMonitor());
			assertEquals("3.11", 1, df.length);

			df = folder2.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, getMonitor());
			assertEquals("3.12", 1, df.length);

		} catch (CoreException e) {
			fail("3.00", e);
		}

		try {
			project.delete(true, getMonitor());
		} catch (CoreException e) {
			fail("3.5", e);
		}

		// once the project is gone, so is all the history for that project
		try {
			// under root
			IFile[] df = root.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, getMonitor());
			assertEquals("4.1", 0, df.length);

			df = root.findDeletedMembersWithHistory(IResource.DEPTH_ONE, getMonitor());
			assertEquals("4.2", 0, df.length);

			df = root.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, getMonitor());
			assertEquals("4.3", 0, df.length);

			// under project
			df = project.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, getMonitor());
			assertEquals("4.4", 0, df.length);

			df = project.findDeletedMembersWithHistory(IResource.DEPTH_ONE, getMonitor());
			assertEquals("4.5", 0, df.length);

			df = project.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, getMonitor());
			assertEquals("4.6", 0, df.length);

			// under folder
			df = folder.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, getMonitor());
			assertEquals("4.7", 0, df.length);

			df = folder.findDeletedMembersWithHistory(IResource.DEPTH_ONE, getMonitor());
			assertEquals("4.8", 0, df.length);

			df = folder.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, getMonitor());
			assertEquals("4.9", 0, df.length);

			// under folder2
			df = folder2.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, getMonitor());
			assertEquals("4.10", 0, df.length);

			df = folder2.findDeletedMembersWithHistory(IResource.DEPTH_ONE, getMonitor());
			assertEquals("4.11", 0, df.length);

			df = folder2.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, getMonitor());
			assertEquals("4.12", 0, df.length);

		} catch (CoreException e) {
			fail("4.00", e);
		}
	}

	/**
	 * Test for retrieving contents of files with states logged in the HistoryStore.
	 */
	public void testGetContents() throws Throwable {

		final int ITERATIONS = 20;

		/* Create common objects. */
		IProject project = getWorkspace().getRoot().getProject("Project");
		try {
			project.create(getMonitor());
			project.open(getMonitor());
		} catch (CoreException e) {
			fail("0.0", e);
		}

		/* Create files. */
		IFile file = project.getFile("getContentsFile.txt");
		String contents = "This file has some contents in testGetContents.";
		ensureExistsInWorkspace(file, contents);

		IFile secondValidFile = project.getFile("secondGetContentsFile.txt");
		contents = "A file with some other contents in testGetContents.";
		ensureExistsInWorkspace(secondValidFile, contents);

		IHistoryStore historyStore = ((Workspace) getWorkspace()).getFileSystemManager().getHistoryStore();

		/* Simulated date -- Increment once for each edition added. */
		long myLong = 0L;

		/* Add multiple editions for one file location. */
		for (int i = 0; i < ITERATIONS; i++, myLong++) {
			FileInfo fileInfo = new FileInfo(file.getName());
			fileInfo.setLastModified(myLong);
			historyStore.addState(file.getFullPath(), ((Resource) file).getStore(), fileInfo, true);
			try {
				contents = "This file has some contents in testGetContents.";
				InputStream is = new ByteArrayInputStream(contents.getBytes());
				createFileInFileSystem(file.getLocation(), is);
				file.refreshLocal(IResource.DEPTH_INFINITE, null);
			} catch (CoreException e) {
				fail("1.1." + i, e);
			}
		}

		/* Add multiple editions for second file location. */
		for (int i = 0; i < ITERATIONS; i++, myLong++) {
			FileInfo fileInfo = new FileInfo(file.getName());
			fileInfo.setLastModified(myLong);
			historyStore.addState(secondValidFile.getFullPath(), ((Resource) secondValidFile).getStore(), fileInfo, true);
			try {
				contents = "A file with some other contents in testGetContents.";
				InputStream is = new ByteArrayInputStream(contents.getBytes());
				createFileInFileSystem(secondValidFile.getLocation(), is);
				secondValidFile.refreshLocal(IResource.DEPTH_INFINITE, null);
			} catch (CoreException e) {
				fail("2.1." + i, e);
			}
		}

		/* Ensure contents of file and retrieved resource are identical.
		 Does not check timestamps. Timestamp checks are performed in a separate test. */
		DataInputStream inFile = null;
		DataInputStream inContents = null;
		IFileState[] stateArray = null;
		stateArray = historyStore.getStates(file.getFullPath(), getMonitor());
		for (int i = 0; i < stateArray.length; i++, myLong++) {
			inFile = new DataInputStream(file.getContents(false));
			try {
				inContents = new DataInputStream(historyStore.getContents(stateArray[i]));
			} catch (CoreException e) {
				fail("3.1." + i, e);
			}
			if (!compareContent(inFile, inContents)) {
				fail("3.2." + i + " No match, files are not identical.");
			}
		}

		stateArray = historyStore.getStates(secondValidFile.getFullPath(), getMonitor());
		for (int i = 0; i < stateArray.length; i++, myLong++) {
			inFile = new DataInputStream(secondValidFile.getContents(false));
			try {
				inContents = new DataInputStream(historyStore.getContents(stateArray[i]));
			} catch (CoreException e) {
				fail("4.1." + i, e);
			}
			if (!compareContent(inFile, inContents)) {
				fail("4.2." + i + " No match, files are not identical.");
			}
		}

		/* Test getting an invalid file state. */
		for (int i = 0; i < ITERATIONS; i++) {
			// Create bogus FileState using invalid uuid.
			try {
				InputStream in = historyStore.getContents(new FileState(historyStore, Path.ROOT, myLong, new UniversalUniqueIdentifier()));
				in.close();
				fail("6." + i + " Edition should be invalid.");
			} catch (CoreException e) {
				// expected
			}
		}

		/* Test verification using null file state. */
		for (int i = 0; i < ITERATIONS; i++) {
			try {
				historyStore.getContents(null);
				fail("7." + i + " Null edition should be invalid.");
			} catch (RuntimeException e) {
				// expected
			}
		}
	}

	public void testModifiedStamp() {
		/* Initialize common objects. */
		IProject project = getWorkspace().getRoot().getProject("Project");
		try {
			project.create(getMonitor());
			project.open(getMonitor());
		} catch (CoreException e) {
			fail("0.0", e);
		}
		IFile file = project.getFile("file");
		try {
			file.create(getRandomContents(), true, getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}
		IFileState[] history = null;
		try {
			history = file.getHistory(getMonitor());
		} catch (CoreException e) {
			fail("1.1", e);
		}
		// no history yet
		assertEquals("1.2", 0, history.length);
		// save the file's current time stamp - it will be remembered in the file state
		long fileTimeStamp = file.getLocalTimeStamp();
		try {
			file.setContents(getRandomContents(), true, true, getMonitor());
		} catch (CoreException e) {
			fail("2.0", e);
		}
		try {
			history = file.getHistory(getMonitor());
		} catch (CoreException e) {
			fail("2.1", e);
		}
		// one state in the history
		assertEquals("2.2", 1, history.length);
		// the timestamp in the state should match the previous file's timestamp
		assertEquals("3.0", fileTimeStamp, history[0].getModificationTime());
	}

	/**
	 * Move case for History Store of folder when the local history is being
	 * copied.
	 *
	 * Scenario:
	 *   1. Create folder (folder1)
	 *   2. Create file						"content 1"
	 *   3. Set new content					"content 2"
	 *   4. Set new content					"content 3"
	 *   5. Move folder
	 *   6. Set new content	to moved file	"content 4"
	 *   7. Set new content to moved file	"content 5"
	 *
	 * The original file should have two states available.
	 * But the moved file should have 4 states as it retains the states from
	 * before the move took place as well.
	 */
	public void testMoveFolder() {
		String[] contents = {"content1", "content2", "content3", "content4", "content5"};
		// create common objects
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		try {
			project.create(getMonitor());
			project.open(getMonitor());
		} catch (CoreException e) {
			fail("0.0", e);
		}

		IFile file = project.getFile("file1.txt");

		IFolder folder = project.getFolder("folder1");
		IFolder folder2 = project.getFolder("folder2");
		file = folder.getFile("file1.txt");
		try {
			// Setup folder1 and file1.txt with some local history
			folder.create(true, true, getMonitor());
			file.create(getContents(contents[0]), true, getMonitor());
			file.setContents(getContents(contents[1]), true, true, getMonitor());
			file.setContents(getContents(contents[2]), true, true, getMonitor());
			IFileState[] states = file.getHistory(getMonitor());
			assertEquals("1.0", 2, states.length);
			assertTrue("1.1", compareContent(getContents(contents[1]), states[0].getContents()));
			assertTrue("1.2", compareContent(getContents(contents[0]), states[1].getContents()));

			// Now do the move
			folder.move(folder2.getFullPath(), true, getMonitor());

			// Check to make sure the file has been moved
			IFile file2 = folder2.getFile("file1.txt");
			assertTrue("1.3", file2.getFullPath().toString().endsWith("folder2/file1.txt"));

			// Give the new (moved file) some new contents
			file2.setContents(getContents(contents[3]), true, true, getMonitor());
			file2.setContents(getContents(contents[4]), true, true, getMonitor());

			// Check the local history of both files
			states = file.getHistory(getMonitor());
			assertEquals("2.0", 2, states.length);
			assertTrue("2.1", compareContent(getContents(contents[1]), states[0].getContents()));
			assertTrue("2.2", compareContent(getContents(contents[0]), states[1].getContents()));
			states = file2.getHistory(getMonitor());
			assertEquals("2.3", 4, states.length);
			assertTrue("2.4", compareContent(getContents(contents[3]), states[0].getContents()));
			assertTrue("2.5", compareContent(getContents(contents[2]), states[1].getContents()));
			assertTrue("2.6", compareContent(getContents(contents[1]), states[2].getContents()));
			assertTrue("2.7", compareContent(getContents(contents[0]), states[3].getContents()));
		} catch (CoreException e) {
			fail("2.8", e);
		}

		try {
			project.delete(true, getMonitor());
		} catch (CoreException e) {
			fail("3.0", e);
		}
	}

	/**
	 * Move case for History Store of project.  Note that local history is
	 * NOT copied for a project move.
	 *
	 * Scenario:
	 *   1. Create folder (folder1)
	 *   2. Create file						"content 1"
	 *   2. Set new content					"content 2"
	 *   3. Set new content					"content 3"
	 *   4. Copy folder
	 *   5. Set new content	to moved file	"content 4"
	 *   6. Set new content to moved file	"content 5"
	 *
	 * The original file should have two states available.
	 * But the copied file should have 4 states as it retains the states from
	 * before the copy took place as well.
	 */
	public void testMoveProject() {
		String[] contents = {"content1", "content2", "content3", "content4", "content5"};
		// create common objects
		IProject project = getWorkspace().getRoot().getProject("MoveProjectProject");
		IProject project2 = getWorkspace().getRoot().getProject("SecondMoveProjectProject");
		try {
			project.create(getMonitor());
			project.open(getMonitor());
		} catch (CoreException e) {
			fail("0.0", e);
		}

		IFile file = project.getFile("file1.txt");

		IFolder folder = project.getFolder("folder1");
		file = folder.getFile("file1.txt");
		try {
			// Setup folder1 and file1.txt with some local history
			folder.create(true, true, getMonitor());
			file.create(getContents(contents[0]), true, getMonitor());
			file.setContents(getContents(contents[1]), true, true, getMonitor());
			file.setContents(getContents(contents[2]), true, true, getMonitor());
			IFileState[] states = file.getHistory(getMonitor());
			assertEquals("1.0", 2, states.length);
			assertTrue("1.1", compareContent(getContents(contents[1]), states[0].getContents()));
			assertTrue("1.2", compareContent(getContents(contents[0]), states[1].getContents()));

			// Now do the move
			project.move(new Path("SecondMoveProjectProject"), true, getMonitor());

			// Check to make sure the file has been moved
			IFile file2 = project2.getFile("folder1/file1.txt");
			assertTrue("1.3", file2.getFullPath().toString().endsWith("SecondMoveProjectProject/folder1/file1.txt"));

			// Give the new (copied file) some new contents
			file2.setContents(getContents(contents[3]), true, true, getMonitor());
			file2.setContents(getContents(contents[4]), true, true, getMonitor());

			// Check the local history of both files
			states = file.getHistory(getMonitor());

			// original file should not remember history when project is moved
			assertEquals("2.0", 0, states.length);
			states = file2.getHistory(getMonitor());
			assertEquals("2.3", 4, states.length);
			assertTrue("2.4", compareContent(getContents(contents[3]), states[0].getContents()));
			assertTrue("2.5", compareContent(getContents(contents[2]), states[1].getContents()));
			assertTrue("2.6", compareContent(getContents(contents[1]), states[2].getContents()));
			assertTrue("2.7", compareContent(getContents(contents[0]), states[3].getContents()));
		} catch (CoreException e) {
			fail("2.9", e);
		}

		try {
			project.delete(true, getMonitor());
		} catch (CoreException e) {
			fail("3.0", e);
		}
	}

	public void testRemoveAll() {

		/* Create common objects. */
		IProject project = getWorkspace().getRoot().getProject("Project");
		IFile file = project.getFile("removeAllStatesFile.txt");
		try {
			project.create(getMonitor());
			project.open(getMonitor());
			file.create(getRandomContents(), true, getMonitor());
		} catch (CoreException e) {
			fail("0.0", e);
		}
		final int ITERATIONS = 20;

		/* test remove in a file */
		for (int i = 0; i < ITERATIONS; i++) {
			try {
				file.setContents(getRandomContents(), true, true, getMonitor());
			} catch (CoreException e) {
				fail("3.0." + i, e);
			}
		}

		/* Valid Case: Ensure correct number of states available. */
		IFileState[] states = null;
		try {
			states = file.getHistory(getMonitor());
		} catch (CoreException e) {
			fail("4.0", e);
		}
		assertEquals("4.1", ITERATIONS, states.length);

		/* Remove all states, and verify that no states remain. */
		try {
			file.clearHistory(getMonitor());
			states = file.getHistory(getMonitor());
			assertEquals("5.0", 0, states.length);
		} catch (CoreException e) {
			fail("5.1", e);
		}

		/* test remove in a folder -- make sure it does not affect other resources' states*/
		IFolder folder = project.getFolder("folder");
		IFile anotherOne = folder.getFile("anotherOne");
		try {
			folder.create(true, true, getMonitor());
			anotherOne.create(getRandomContents(), true, getMonitor());
		} catch (CoreException e) {
			fail("6.0", e);
		}
		for (int i = 0; i < ITERATIONS; i++) {
			try {
				file.setContents(getRandomContents(), true, true, getMonitor());
				anotherOne.setContents(getRandomContents(), true, true, getMonitor());
			} catch (CoreException e) {
				fail("6.1." + i, e);
			}
		}

		try {
			states = file.getHistory(getMonitor());
			assertEquals("6.2", ITERATIONS, states.length);
			states = anotherOne.getHistory(getMonitor());
			assertEquals("6.3", ITERATIONS, states.length);
		} catch (CoreException e) {
			fail("6.4", e);
		}

		/* Remove all states, and verify that no states remain. */
		try {
			project.clearHistory(getMonitor());
			states = file.getHistory(getMonitor());
			assertEquals("7.0", 0, states.length);
			states = anotherOne.getHistory(getMonitor());
			assertEquals("7.1", 0, states.length);
		} catch (CoreException e) {
			fail("7.2", e);
		}

		/* test remove in a folder -- make sure it does not affect other resources' states*/
		IFile aaa = project.getFile("aaa");
		IFolder bbb = project.getFolder("bbb");
		anotherOne = bbb.getFile("anotherOne");
		IFile ccc = project.getFile("ccc");
		try {
			bbb.create(true, true, getMonitor());
			anotherOne.create(getRandomContents(), true, getMonitor());
			aaa.create(getRandomContents(), true, getMonitor());
			ccc.create(getRandomContents(), true, getMonitor());
		} catch (CoreException e) {
			fail("8.0", e);
		}
		for (int i = 0; i < ITERATIONS; i++) {
			try {
				anotherOne.setContents(getRandomContents(), true, true, getMonitor());
				aaa.setContents(getRandomContents(), true, true, getMonitor());
				ccc.setContents(getRandomContents(), true, true, getMonitor());
			} catch (CoreException e) {
				fail("8.1." + i, e);
			}
		}

		try {
			states = anotherOne.getHistory(getMonitor());
			assertEquals("8.3", ITERATIONS, states.length);
			states = aaa.getHistory(getMonitor());
			assertEquals("8.4", ITERATIONS, states.length);
			states = ccc.getHistory(getMonitor());
			assertEquals("8.5", ITERATIONS, states.length);
		} catch (CoreException e) {
			fail("8.6", e);
		}

		/* Remove all states, and verify that no states remain. aaa and ccc should not be affected. */
		try {
			bbb.clearHistory(getMonitor());
			states = anotherOne.getHistory(getMonitor());
			assertEquals("9.1", 0, states.length);
			states = aaa.getHistory(getMonitor());
			assertEquals("9.2", ITERATIONS, states.length);
			states = ccc.getHistory(getMonitor());
			assertEquals("9.3", ITERATIONS, states.length);
		} catch (CoreException e) {
			fail("9.4", e);
		}
	}

	/**
	 * Simple copy case for History Store when the local history is being
	 * copied.
	 *
	 * Scenario:
	 *   1. Create file						"content 1"
	 *   2. Set new content					"content 2"
	 *   3. Set new content					"content 3"
	 *   4. Move file
	 *   5. Set new content	to copied file	"content 4"
	 *   6. Set new content to copied file	"content 5"
	 *
	 * The original file should have two states available.
	 * But the copied file should have 4 states as it retains the states from
	 * before the copy took place as well.
	 */
	public void testSimpleCopy() {

		/* Initialize common objects. */
		IProject project = getWorkspace().getRoot().getProject("SimpleCopyProject");
		try {
			project.create(getMonitor());
			project.open(getMonitor());
		} catch (CoreException e) {
			fail("0.0", e);
		}
		String[] contents = {"content1", "content2", "content3", "content4", "content5"};
		IFile file = project.getFile("simpleCopyFileWithHistoryCopy");
		IFile copyFile = project.getFile("copyOfSimpleCopyFileWithHistoryCopy");

		/* Create first file. */
		try {
			file.create(getContents(contents[0]), true, null);
		} catch (CoreException e) {
			fail("1.2", e);
		}

		/* Set new contents on first file. Should add two entries to the history store. */
		try {
			file.setContents(getContents(contents[1]), true, true, null);
			file.setContents(getContents(contents[2]), true, true, null);
		} catch (CoreException e) {
			fail("2.0", e);
		}

		/* Copy first file to the second. Second file should have no history. */
		try {
			file.copy(copyFile.getFullPath(), true, null);
		} catch (CoreException e) {
			fail("3.0", e);
		}

		/* Check history for both files. */
		try {
			IFileState[] states = file.getHistory(null);
			assertEquals("4.0", 2, states.length);
			states = copyFile.getHistory(null);
			assertEquals("4.1", 2, states.length);
		} catch (CoreException e) {
			fail("4.2", e);
		}

		/* Set new contents on second file. Should add two entries to the history store. */
		try {
			copyFile.setContents(getContents(contents[3]), true, true, null);
			copyFile.setContents(getContents(contents[4]), true, true, null);
		} catch (CoreException e) {
			fail("5.0", e);
		}

		/* Check history for both files. */
		try {
			// Check log for original file.
			IFileState[] states = file.getHistory(null);
			assertEquals("6.0", 2, states.length);
			assertTrue("6.1", compareContent(getContents(contents[1]), states[0].getContents()));
			assertTrue("6.2", compareContent(getContents(contents[0]), states[1].getContents()));

			// Check log for copy.
			states = copyFile.getHistory(null);
			assertEquals("6.3", 4, states.length);
			assertTrue("6.4", compareContent(getContents(contents[3]), states[0].getContents()));
			assertTrue("6.5", compareContent(getContents(contents[2]), states[1].getContents()));
			assertTrue("6.6", compareContent(getContents(contents[1]), states[2].getContents()));
			assertTrue("6.7", compareContent(getContents(contents[0]), states[3].getContents()));

		} catch (CoreException e) {
			fail("6.8", e);
		}
	}

	/**
	 * Simple move case for History Store when the local history is being
	 * copied.
	 *
	 * Scenario:
	 *   1. Create file						"content 1"
	 *   2. Set new content					"content 2"
	 *   3. Set new content					"content 3"
	 *   4. Move file
	 *   5. Set new content	to moved file	"content 4"
	 *   6. Set new content to moved file	"content 5"
	 *
	 * The original file should have two states available.
	 * But the moved file should have 4 states as it retains the states from
	 * before the move took place as well.
	 */
	public void testSimpleMove() {

		/* Initialize common objects. */
		IProject project = getWorkspace().getRoot().getProject("SimpleMoveProject");
		try {
			project.create(getMonitor());
			project.open(getMonitor());
		} catch (CoreException e) {
			fail("0.0", e);
		}
		String[] contents = {"content1", "content2", "content3", "content4", "content5"};
		IFile file = project.getFile("simpleMoveFileWithCopy");
		IFile moveFile = project.getFile("copyOfSimpleMoveFileWithCopy");

		/* Create first file. */
		try {
			file.create(getContents(contents[0]), true, null);
		} catch (CoreException e) {
			fail("1.2", e);
		}

		/* Set new contents on source file. Should add two entries to the history store. */
		try {
			file.setContents(getContents(contents[1]), true, true, null);
			file.setContents(getContents(contents[2]), true, true, null);
		} catch (CoreException e) {
			fail("2.0", e);
		}

		/* Move source file to second location.
		 * Moved files should have the history of the original file.
		 */
		try {
			file.move(moveFile.getFullPath(), true, null);
		} catch (CoreException e) {
			fail("3.0", e);
		}

		/* Check history for both files. */
		try {
			IFileState[] states = file.getHistory(null);
			assertEquals("4.0", 2, states.length);
			states = moveFile.getHistory(null);
			assertEquals("4.1", 2, states.length);
		} catch (CoreException e) {
			fail("4.2", e);
		}

		/* Set new contents on moved file. Should add two entries to the history store. */
		try {
			moveFile.setContents(getContents(contents[3]), true, true, null);
			moveFile.setContents(getContents(contents[4]), true, true, null);
		} catch (CoreException e) {
			fail("5.0", e);
		}

		/* Check history for both files. */
		try {
			// Check log for original file.
			IFileState[] states = file.getHistory(null);
			assertEquals("6.0", 2, states.length);
			assertTrue("6.1", compareContent(getContents(contents[1]), states[0].getContents()));
			assertTrue("6.2", compareContent(getContents(contents[0]), states[1].getContents()));

			// Check log for moved file.
			states = moveFile.getHistory(null);
			assertEquals("6.3", 4, states.length);
			assertTrue("6.4", compareContent(getContents(contents[3]), states[0].getContents()));
			assertTrue("6.5", compareContent(getContents(contents[2]), states[1].getContents()));
			assertTrue("6.6", compareContent(getContents(contents[1]), states[2].getContents()));
			assertTrue("6.7", compareContent(getContents(contents[0]), states[3].getContents()));

		} catch (CoreException e) {
			fail("6.8", e);
		}
	}

	/**
	 * Simple use case for History Store.
	 *
	 * Scenario:									   # Editions
	 *   1. Create file					"content 1"			0
	 *   2. Set new content				"content 2"			1
	 *   3. Set new content				"content 3"			2
	 *   4. Delete file										3
	 *   5. Roll back to first version  "content 1"			3
	 *   6. Set new content				"content 2"			4
	 *   7. Roll back to third version  "content 3"			5
	 */
	public void testSimpleUse() {

		/* Initialize common objects. */
		IProject project = getWorkspace().getRoot().getProject("Project");
		try {
			project.create(getMonitor());
			project.open(getMonitor());
		} catch (CoreException e) {
			fail("0.0", e);
		}
		String[] contents = {"content1", "content2", "content3"};
		IFile file = project.getFile("file");

		/* Create the file. */
		try {
			file.create(getContents(contents[0]), true, getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}

		/* Set new contents on the file. Should add two entries to the store. */
		try {
			for (int i = 0; i < 2; i++) {
				file.setContents(getContents(contents[i + 1]), true, true, getMonitor());
			}
		} catch (CoreException e) {
			fail("2.0", e);
		}

		/* Ensure two entries are available for the file, and that content matches. */
		try {
			IFileState[] states = file.getHistory(getMonitor());
			assertEquals("3.0", 2, states.length);
			assertTrue("3.1.1", compareContent(getContents(contents[1]), states[0].getContents()));
			assertTrue("3.1.2", compareContent(getContents(contents[0]), states[1].getContents()));
		} catch (CoreException e) {
			fail("3.2", e);
		}

		/* Delete the file. Should add an entry to the store. */
		try {
			file.delete(true, true, getMonitor());
		} catch (CoreException e) {
			fail("4.0", e);
		}

		/* Ensure three entries are available for the file, and that content matches. */
		try {
			IFileState[] states = file.getHistory(getMonitor());
			assertEquals("5.0", 3, states.length);
			assertTrue("5.1.1", compareContent(getContents(contents[2]), states[0].getContents()));
			assertTrue("5.1.2", compareContent(getContents(contents[1]), states[1].getContents()));
			assertTrue("5.1.3", compareContent(getContents(contents[0]), states[2].getContents()));
		} catch (CoreException e) {
			fail("5.2", e);
		}

		/* Roll file back to first version, and ensure that content matches. */
		try {
			IFileState[] states = file.getHistory(getMonitor());
			// Create the file with the contents from one of the states.
			// Won't add another entry to the store.
			file.create(states[0].getContents(), false, getMonitor());

			// Check history store.
			states = file.getHistory(getMonitor());
			assertEquals("6.0", 3, states.length);
			assertTrue("6.1.1", compareContent(getContents(contents[2]), states[0].getContents()));
			assertTrue("6.1.2", compareContent(getContents(contents[1]), states[1].getContents()));
			assertTrue("6.1.3", compareContent(getContents(contents[0]), states[2].getContents()));

			// Check file contents.
			assertTrue("6.2", compareContent(getContents(contents[2]), file.getContents(false)));

		} catch (CoreException e) {
			fail("6.3", e);
		}

		/* Set new contents on the file. Should add an entry to the history store. */
		try {
			file.setContents(getContents(contents[1]), true, true, null);
		} catch (CoreException e) {
			fail("7.0", e);
		}

		/* Ensure four entries are available for the file, and that entries match. */
		try {
			IFileState[] states = file.getHistory(getMonitor());
			assertEquals("8.0", 4, states.length);
			assertTrue("8.1.1", compareContent(getContents(contents[2]), states[0].getContents()));
			assertTrue("8.1.2", compareContent(getContents(contents[2]), states[1].getContents()));
			assertTrue("8.1.3", compareContent(getContents(contents[1]), states[2].getContents()));
			assertTrue("8.1.4", compareContent(getContents(contents[0]), states[3].getContents()));
		} catch (CoreException e) {
			fail("8.2", e);
		}

		/* Roll file back to third version, and ensure that content matches. */
		try {
			IFileState[] states = file.getHistory(getMonitor());
			// Will add another entry to log.
			file.setContents(states[2], true, true, getMonitor());

			// Check history log.
			states = file.getHistory(getMonitor());
			assertEquals("9.0", 5, states.length);
			assertTrue("9.1.1", compareContent(getContents(contents[1]), states[0].getContents()));
			assertTrue("9.1.2", compareContent(getContents(contents[2]), states[1].getContents()));
			assertTrue("9.1.3", compareContent(getContents(contents[2]), states[2].getContents()));
			assertTrue("9.1.4", compareContent(getContents(contents[1]), states[3].getContents()));
			assertTrue("9.1.5", compareContent(getContents(contents[0]), states[4].getContents()));

			// Check file contents.
			assertTrue("9.2", compareContent(getContents(contents[1]), file.getContents(false)));

		} catch (CoreException e) {
			fail("9.3", e);
		}
	}
}
