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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.compareContent;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInputStream;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomContentsStream;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.ensureOutOfSync;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileInfo;
import org.eclipse.core.internal.localstore.IHistoryStore;
import org.eclipse.core.internal.resources.FileState;
import org.eclipse.core.internal.resources.Resource;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.internal.utils.UniversalUniqueIdentifier;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

/**
 * This class defines all tests for the HistoryStore Class.
 */

public class HistoryStoreTest {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

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

	public static void assertFileStateEquals(String tag, IFileState expected, IFileState actual) {
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
		store.remove(IPath.ROOT, monitor);
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

	@After
	public void tearDown() throws Exception {
		wipeHistoryStore(createTestMonitor());
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
	@Test
	public void testAddStateAndPolicies() throws Exception {
		/* Create common objects. */
		IProject project = getWorkspace().getRoot().getProject("Project");
		IFile file = project.getFile("file.txt");
		project.create(createTestMonitor());
		project.open(createTestMonitor());
		file.create(createRandomContentsStream(), true, createTestMonitor());

		/* set local history policies */
		IWorkspaceDescription description = getWorkspace().getDescription();
		// longevity set to 1 day
		description.setFileStateLongevity(1000 * 3600 * 24);
		// keep a max of 5 file states
		description.setMaxFileStates(5);
		// max size of file = 1 Mb
		description.setMaxFileStateSize(1024 * 1024);
		getWorkspace().setDescription(description);

		/* test max file states */
		for (int i = 0; i < 8; i++) {
			ensureOutOfSync(file);
			file.refreshLocal(IResource.DEPTH_ZERO, createTestMonitor());
			file.setContents(createRandomContentsStream(), true, true, createTestMonitor());
		}
		IFileState[] states = file.getHistory(createTestMonitor());
		// Make sure we have 8 states as we haven't trimmed yet.
		assertThat(states).hasSize(8);

		IFileState[] oldStates = states;

		getWorkspace().save(true, null);
		states = file.getHistory(createTestMonitor());
		// We added 8 states.  Make sure we only have 5 (the max).
		assertThat(states).hasSize(description.getMaxFileStates());

		// assert that states are in the correct order (newer ones first)
		long lastModified = states[0].getModificationTime();
		for (int i = 1; i < states.length; i++) {
			assertTrue("1.3." + i, lastModified >= states[i].getModificationTime());
			lastModified = states[i].getModificationTime();
		}

		// assert that the most recent states were preserved
		for (int i = 0; i < states.length; i++) {
			assertFileStateEquals("1.4." + i, oldStates[i], states[i]);
		}

		/* test max file state size */
		description.setMaxFileStates(15);
		// max size of file = 7 bytes
		description.setMaxFileStateSize(7);
		getWorkspace().setDescription(description);
		file = project.getFile("file1.txt");
		file.create(new ByteArrayInputStream(new byte[0]), true, createTestMonitor());
		// Add 10 bytes to exceed the max file state size.
		for (int i = 0; i < 10; i++) {
			file.appendContents(createInputStream("a"), true, true, createTestMonitor());
		}
		getWorkspace().save(true, null);
		states = file.getHistory(createTestMonitor());
		// #states = size + 1 for the 0 byte length file to begin with.
		for (int i = 0; i < states.length; i++) {
			int bytesRead = numBytes(states[i].getContents());
			assertTrue("2.2." + i, bytesRead <= description.getMaxFileStateSize());
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
		getWorkspace().setDescription(description);
		states = file.getHistory(createTestMonitor());
		// Make sure we have 5 states for file file.txt
		assertThat(states).hasSize(description.getMaxFileStates());
		// change policies
		// 10 seconds
		description.setFileStateLongevity(1000 * 10);
		// 1 Mb
		description.setMaxFileStateSize(1024 * 1024);
		getWorkspace().setDescription(description);

		// sleep for more than 10 seconds (the granularity varies on
		// some machines so we will sleep for 12 seconds)
		Thread.sleep(1000 * 12);
		getWorkspace().save(true, null);
		states = file.getHistory(createTestMonitor());
		// The 5 states for file.txt should have exceeded their longevity
		// and been removed. Make sure we have 0 states left.
		assertThat(states).isEmpty();
	}

	@Test
	public void testBug28238() throws Exception {
		// paths to mimic files in the workspace
		IProject project = getWorkspace().getRoot().getProject("myproject28238");
		IFolder folder = project.getFolder("myfolder");
		IFolder destinationFolder = project.getFolder("myfolder2");
		IFile file = folder.getFile("myfile.txt");
		IFile destinationFile = destinationFolder.getFile(file.getName());

		IHistoryStore store = ((Resource) getWorkspace().getRoot()).getLocalManager().getHistoryStore();

		// location of the data on disk
		IFileStore fileStore = workspaceRule.getTempStore();
		createInFileSystem(fileStore);
		assertThat(store.getStates(file.getFullPath(), createTestMonitor())).as("check file has no state").isEmpty();

		// add the data to the history store
		FileInfo fileInfo = new FileInfo(file.getName());
		fileInfo.setLastModified(System.currentTimeMillis());
		store.addState(file.getFullPath(), fileStore, fileInfo, true);
		IFileState[] states = store.getStates(file.getFullPath(), createTestMonitor());
		assertThat(states).hasSize(1);

		// copy the data
		store.copyHistory(folder, destinationFolder, true);

		states = store.getStates(destinationFile.getFullPath(), createTestMonitor());
		assertThat(states).hasSize(1);
	}

	@Test
	public void testBug28603() throws CoreException {
		// paths to mimic files in the workspace
		IProject project = getWorkspace().getRoot().getProject("myproject28603");
		IFolder folder1 = project.getFolder("myfolder1");
		IFolder folder2 = project.getFolder("myfolder2");
		IFile file1 = folder1.getFile("myfile.txt");
		IFile file2 = folder2.getFile(file1.getName());

		// directly deletes history files if project did already existed:
		createInWorkspace(new IResource[] {project, folder1, folder2});
		file1.create(createRandomContentsStream(), IResource.FORCE, createTestMonitor());
		file1.setContents(createRandomContentsStream(), IResource.FORCE | IResource.KEEP_HISTORY, createTestMonitor());
		file1.setContents(createRandomContentsStream(), IResource.FORCE | IResource.KEEP_HISTORY, createTestMonitor());
		file1.setContents(createRandomContentsStream(), IResource.FORCE | IResource.KEEP_HISTORY, createTestMonitor());
		setMaxFileStates(50);

		int maxStates = ResourcesPlugin.getWorkspace().getDescription().getMaxFileStates();

		IFileState[] states = file1.getHistory(createTestMonitor());
		assertThat(states).hasSize(3);
		int currentStates = 3;
		assertThat(file2.getHistory(createTestMonitor())).as("check file2 has no history").isEmpty();
		for (int i = 0; i < maxStates + 10; i++) {
			states = file1.getHistory(createTestMonitor());
			assertThat(states).as(i + " file1 states").hasSize(currentStates);
			file1.move(file2.getFullPath(), true, true, createTestMonitor());
			states = file2.getHistory(createTestMonitor());
			currentStates = currentStates < maxStates ? currentStates + 1 : maxStates;
			assertThat(states).as(i + " file2 states").hasSize(currentStates);
			file2.move(file1.getFullPath(), true, true, createTestMonitor());
			states = file1.getHistory(createTestMonitor());
			currentStates = currentStates < maxStates ? currentStates + 1 : maxStates;
			assertThat(states).as(i + " file1 states").hasSize(currentStates);
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
	 */
	@Test
	public void testClean() throws Exception {
		/* Create common objects. */
		IProject project = getWorkspace().getRoot().getProject("ProjectClean");
		IFile file = project.getFile("file.txt");
		project.create(createTestMonitor());
		project.open(createTestMonitor());
		file.create(createRandomContentsStream(), true, createTestMonitor());
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
		getWorkspace().setDescription(description);

		// Set up 8 file states for this file when 500 are allowed
		for (int i = 0; i < 8; i++) {
			ensureOutOfSync(file);
			file.refreshLocal(IResource.DEPTH_ZERO, createTestMonitor());
			// try {
			// Thread.sleep(5000); // necessary because of lastmodified granularity in some
			// file systems
			// } catch (InterruptedException e) {
			// }
			file.setContents(createRandomContentsStream(), true, true, createTestMonitor());
		}
		// All 8 states should exist.
		long oldLastModTimes[] = new long[8];
		IFileState[] states = file.getHistory(createTestMonitor());
		assertThat(states).hasSize(8);
		for (int i = 0; i < 8; i++) {
			oldLastModTimes[i] = states[i].getModificationTime();
		}

		// Set max. number of file states to be 3
		description.setMaxFileStates(3);
		getWorkspace().setDescription(description);
		// Run 'clean' - should cause 5 of 8 states to be removed
		store.clean(createTestMonitor());
		// Restore max. number of states/file to 500
		description.setMaxFileStates(500);
		getWorkspace().setDescription(description);

		// Check to ensure only 3 states remain.  Make sure these are the 3
		// newer states.
		states = file.getHistory(createTestMonitor());
		assertThat(states).hasSize(3);
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

		/* test max file longevity */
		file = project.getFile("file.txt"); // use the file of the first test
		description.setFileStateLongevity(1000 * 3600 * 24); // 1 day
		description.setMaxFileStates(500);
		description.setMaxFileStateSize(1024 * 1024); // 1 Mb
		// the description should be the same as the first test
		getWorkspace().setDescription(description);
		states = file.getHistory(createTestMonitor());
		assertThat(states).hasSize(3);
		// change policies
		// 10 seconds
		description.setFileStateLongevity(1000 * 10);
		// 1 Mb
		description.setMaxFileStateSize(1024 * 1024);
		getWorkspace().setDescription(description);

		// sleep for 12 seconds (must exceed 10 seconds). This should
		// cause all 3 states for file.txt to be considered stale.
		Thread.sleep(1000 * 12);

		store.clean(createTestMonitor());
		// change policies - restore to original values
		// 1 day
		description.setFileStateLongevity(1000 * 3600 * 24);
		// 1 Mb
		description.setMaxFileStateSize(1024 * 1024);
		getWorkspace().setDescription(description);

		// Ensure we have no state information left.  It should have been
		// considered stale.
		states = file.getHistory(createTestMonitor());
		assertThat(states).isEmpty();
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
	@Test
	public void testCopyFolder() throws Exception {
		String[] contents = {"content1", "content2", "content3", "content4", "content5"};
		// create common objects
		IProject project = getWorkspace().getRoot().getProject("CopyFolderProject");
		project.create(createTestMonitor());
		project.open(createTestMonitor());

		IFile file = project.getFile("file1.txt");

		IFolder folder = project.getFolder("folder1");
		IFolder folder2 = project.getFolder("folder2");
		file = folder.getFile("file1.txt");
		// Setup folder1 and file1.txt with some local history
		folder.create(true, true, createTestMonitor());
		file.create(createInputStream(contents[0]), true, createTestMonitor());
		file.setContents(createInputStream(contents[1]), true, true, createTestMonitor());
		file.setContents(createInputStream(contents[2]), true, true, createTestMonitor());
		IFileState[] states = file.getHistory(createTestMonitor());
		assertThat(states).hasSize(2).satisfiesExactly(
				first -> assertThat(compareContent(createInputStream(contents[1]), first.getContents())).isTrue(),
				second -> assertThat(compareContent(createInputStream(contents[0]), second.getContents())).isTrue());

		// Now do the move
		folder.copy(folder2.getFullPath(), true, createTestMonitor());

		// Check to make sure the file has been copied
		IFile file2 = folder2.getFile("file1.txt");
		assertTrue("1.3", file2.getFullPath().toString().endsWith("folder2/file1.txt"));

		// Give the new (copied file) some new contents
		file2.setContents(createInputStream(contents[3]), true, true, createTestMonitor());
		file2.setContents(createInputStream(contents[4]), true, true, createTestMonitor());

		// Check the local history of both files
		states = file.getHistory(createTestMonitor());
		assertThat(states).hasSize(2).satisfiesExactly(
				first -> assertThat(compareContent(createInputStream(contents[1]), first.getContents())).isTrue(),
				second -> assertThat(compareContent(createInputStream(contents[0]), second.getContents())).isTrue());
		states = file2.getHistory(createTestMonitor());
		assertThat(states).hasSize(4).satisfiesExactly(
				first -> assertThat(compareContent(createInputStream(contents[3]), first.getContents())).isTrue(),
				second -> assertThat(compareContent(createInputStream(contents[2]), second.getContents())).isTrue(),
				third -> assertThat(compareContent(createInputStream(contents[1]), third.getContents())).isTrue(),
				fourth -> assertThat(compareContent(createInputStream(contents[0]), fourth.getContents())).isTrue());

		project.delete(true, createTestMonitor());
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
	@Test
	public void testCopyHistoryFile() throws Exception {
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
		createInWorkspace(project);

		IFolder folder = project.getFolder("folder1");
		IFile file = folder.getFile("file1.txt");
		IFile file2 = folder.getFile("file2.txt");

		// Setup folder1 and file1.txt with some local history
		folder.create(true, true, createTestMonitor());
		file.create(createInputStream(contents[0]), true, createTestMonitor());

		Thread.sleep(1000);
		file.setContents(createInputStream(contents[1]), true, true, createTestMonitor());
		Thread.sleep(1000);
		file.setContents(createInputStream(contents[2]), true, true, createTestMonitor());
		Thread.sleep(1000);

		IFileState[] states = file.getHistory(createTestMonitor());
		assertThat(states).hasSize(2).satisfiesExactly(
				first -> assertThat(compareContent(createInputStream(contents[1]), first.getContents())).isTrue(),
				second -> assertThat(compareContent(createInputStream(contents[0]), second.getContents())).isTrue());
		file2.create(createInputStream(contents[3]), true, createTestMonitor());
		file2.setContents(createInputStream(contents[4]), true, true, createTestMonitor());

		// Run some tests with illegal arguments
		LogListenerVerifier verifier = new LogListenerVerifier();
		ILog log = ResourcesPlugin.getPlugin().getLog();
		log.addLogListener(verifier);

		// Test with null source and/or destination
		IHistoryStore store = ((Resource) file).getLocalManager().getHistoryStore();
		verifier.addExpected(IResourceStatus.INTERNAL_ERROR);
		store.copyHistory(null, null, false);
		verifier.verify();
		verifier.reset();

		verifier.addExpected(IResourceStatus.INTERNAL_ERROR);
		store.copyHistory(null, file2, false);
		verifier.verify();
		verifier.reset();

		verifier.addExpected(IResourceStatus.INTERNAL_ERROR);
		store.copyHistory(file, null, false);
		verifier.verify();
		verifier.reset();

		// Try to copy the history store stuff to the same location
		verifier.addExpected(IResourceStatus.INTERNAL_ERROR);
		store.copyHistory(file, file, false);
		verifier.verify();
		verifier.reset();

		// Remember to remove the log listener now that we are done
		// testing illegal arguments.
		log.removeLogListener(verifier);

		// Test a valid copy of a file
		store.copyHistory(file, file2, false);
		states = file2.getHistory(createTestMonitor());
		assertThat(states).hasSize(3).satisfiesExactly(
				first -> assertThat(compareContent(createInputStream(contents[3]), first.getContents())).isTrue(),
				second -> assertThat(compareContent(createInputStream(contents[1]), second.getContents())).isTrue(),
				third -> assertThat(compareContent(createInputStream(contents[0]), third.getContents())).isTrue());
	}

	@Test
	public void testCopyHistoryFolder() throws Exception {
		String[] contents = {"content0", "content1", "content2", "content3", "content4"};
		// create common objects
		IProject project = getWorkspace().getRoot().getProject("TestCopyHistoryProject");
		createInWorkspace(project);

		IFolder folder = project.getFolder("folder1");
		IFolder folder2 = project.getFolder("folder2");
		IFile file = folder.getFile("file1.txt");
		IFile file2 = folder2.getFile("file1.txt");

		// Setup folder1 and file1.txt with some local history
		folder.create(true, true, createTestMonitor());
		file.create(createInputStream(contents[0]), true, createTestMonitor());

		Thread.sleep(1000);
		file.setContents(createInputStream(contents[1]), true, true, createTestMonitor());
		Thread.sleep(1000);
		file.setContents(createInputStream(contents[2]), true, true, createTestMonitor());
		Thread.sleep(1000);

		IFileState[] states = file.getHistory(createTestMonitor());
		assertThat(states).hasSize(2).satisfiesExactly(
				first -> assertThat(compareContent(createInputStream(contents[1]), first.getContents())).isTrue(),
				second -> assertThat(compareContent(createInputStream(contents[0]), second.getContents())).isTrue());
		folder2.create(true, true, createTestMonitor());
		file2.create(createInputStream(contents[3]), true, createTestMonitor());
		file2.setContents(createInputStream(contents[4]), true, true, createTestMonitor());

		// Test a valid copy of a folder
		IHistoryStore store = ((Resource) file).getLocalManager().getHistoryStore();
		store.copyHistory(folder, folder2, false);
		states = file2.getHistory(createTestMonitor());
		assertThat(states).hasSize(3).satisfiesExactly(
				first -> assertThat(compareContent(createInputStream(contents[3]), first.getContents())).isTrue(),
				second -> assertThat(compareContent(createInputStream(contents[1]), second.getContents())).isTrue(),
				third -> assertThat(compareContent(createInputStream(contents[0]), third.getContents())).isTrue());
	}

	@Test
	public void testCopyHistoryProject() throws Exception {
		String[] contents = {"content0", "content1", "content2", "content3", "content4"};
		// create common objects
		IProject project = getWorkspace().getRoot().getProject("TestCopyHistoryProject");
		IProject project2 = getWorkspace().getRoot().getProject("TestCopyHistoryProject2");
		createInWorkspace(new IResource[] {project, project2});

		IFolder folder = project.getFolder("folder1");
		IFolder folder2 = project2.getFolder("folder1");
		IFile file = folder.getFile("file1.txt");
		IFile file2 = folder2.getFile("file1.txt");
		// Setup folder1 and file1.txt with some local history
		folder.create(true, true, createTestMonitor());
		file.create(createInputStream(contents[0]), true, createTestMonitor());

		Thread.sleep(1000);
		file.setContents(createInputStream(contents[1]), true, true, createTestMonitor());
		Thread.sleep(1000);
		file.setContents(createInputStream(contents[2]), true, true, createTestMonitor());
		Thread.sleep(1000);

		IFileState[] states = file.getHistory(createTestMonitor());
		assertThat(states).hasSize(2).satisfiesExactly(
				first -> assertThat(compareContent(createInputStream(contents[1]), first.getContents())).isTrue(),
				second -> assertThat(compareContent(createInputStream(contents[0]), second.getContents())).isTrue());
		folder2.create(true, true, createTestMonitor());
		file2.create(createInputStream(contents[3]), true, createTestMonitor());
		file2.setContents(createInputStream(contents[4]), true, true, createTestMonitor());

		// Test a valid copy of a folder
		IHistoryStore store = ((Resource) file).getLocalManager().getHistoryStore();
		store.copyHistory(project, project2, false);
		states = file2.getHistory(createTestMonitor());
		assertThat(states).hasSize(3).satisfiesExactly(
				first -> assertThat(compareContent(createInputStream(contents[3]), first.getContents())).isTrue(),
				second -> assertThat(compareContent(createInputStream(contents[1]), second.getContents())).isTrue(),
				third -> assertThat(compareContent(createInputStream(contents[0]), third.getContents())).isTrue());
	}

	@Test
	public void testDelete() throws CoreException {
		// create common objects
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		project.create(createTestMonitor());
		project.open(createTestMonitor());

		// test file
		IFile file = project.getFile("file.txt");
		file.create(createRandomContentsStream(), true, createTestMonitor());
		file.setContents(createRandomContentsStream(), true, true, createTestMonitor());
		file.setContents(createRandomContentsStream(), true, true, createTestMonitor());

		// Check to see that there are only 2 states before the deletion
		IFileState[] states = file.getHistory(createTestMonitor());
		assertThat(states).hasSize(2);

		// Delete the file. This should add a state to the history store.
		file.delete(true, true, createTestMonitor());
		states = file.getHistory(createTestMonitor());
		assertThat(states).hasSize(3);

		// Re-create the file. This should not affect the history store.
		file.create(createRandomContentsStream(), true, createTestMonitor());
		states = file.getHistory(createTestMonitor());
		assertThat(states).hasSize(3);

		// test folder
		IFolder folder = project.getFolder("folder");
		// Make sure this has a different name as the history store information
		// for the first 'file.txt' is likely still around.
		file = folder.getFile("file2.txt");
		folder.create(true, true, createTestMonitor());
		file.create(createRandomContentsStream(), true, createTestMonitor());
		file.setContents(createRandomContentsStream(), true, true, createTestMonitor());
		file.setContents(createRandomContentsStream(), true, true, createTestMonitor());

		// There should only be 2 history store entries.
		states = file.getHistory(createTestMonitor());
		assertThat(states).hasSize(2);

		// Delete the folder. This should cause one more history store entry.
		folder.delete(true, true, createTestMonitor());
		states = file.getHistory(createTestMonitor());
		assertThat(states).hasSize(3);

		// Re-create the folder. There should be no new history store entries.
		folder.create(true, true, createTestMonitor());
		file.create(createRandomContentsStream(), true, createTestMonitor());
		states = file.getHistory(createTestMonitor());
		assertThat(states).hasSize(3);

		project.delete(true, createTestMonitor());
	}

	/**
	 * Test for existence of file states in the HistoryStore.
	 */
	@Test
	public void testExists() throws Throwable {

		/* Create common objects. */
		IProject project = getWorkspace().getRoot().getProject("Project");
		IFile file = project.getFile("removeAllStatesFile.txt");
		project.create(createTestMonitor());
		project.open(createTestMonitor());
		file.create(createRandomContentsStream(), true, createTestMonitor());

		// Constant for the number of states we will create
		final int ITERATIONS = 20;

		/* Add multiple states for one file location. */
		for (int i = 0; i < ITERATIONS; i++) {
			file.setContents(createRandomContentsStream(), true, true, createTestMonitor());
		}

		/* Valid Case: Test retrieved values. */
		IFileState[] states = file.getHistory(createTestMonitor());
		// Make sure we have ITERATIONS number of states
		assertThat(states).hasSize(ITERATIONS);
		// Make sure that each of these states really exists in the filesystem.
		for (IFileState state : states) {
			assertThat(state).matches(IFileState::exists, "exists");
		}
	}

	@Test
	public void testFindDeleted() throws CoreException {
		// create common objects
		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject project = root.getProject("MyProject");
		project.create(createTestMonitor());
		project.open(createTestMonitor());

		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_ONE, createTestMonitor())).isEmpty();

		// test that a deleted file can be found
		IFile pfile = project.getFile("findDeletedFile.txt");
		// create and delete a file
		pfile.create(createRandomContentsStream(), true, createTestMonitor());
		pfile.delete(true, true, createTestMonitor());

		// the deleted file should show up as a deleted member of project
		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_ONE, createTestMonitor()))
				.containsExactly(pfile);
		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, createTestMonitor()))
				.containsExactly(pfile);
		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, createTestMonitor())).isEmpty();

		// the deleted file should show up as a deleted member of workspace root
		assertThat(root.findDeletedMembersWithHistory(IResource.DEPTH_ONE, createTestMonitor())).isEmpty();
		assertThat(root.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, createTestMonitor()))
				.containsExactly(pfile);
		assertThat(root.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, createTestMonitor())).isEmpty();

		// recreate the file
		pfile.create(createRandomContentsStream(), true, createTestMonitor());

		// the deleted file should no longer show up as a deleted member of project
		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_ONE, createTestMonitor())).isEmpty();
		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, createTestMonitor())).isEmpty();
		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, createTestMonitor())).isEmpty();

		// the deleted file should no longer show up as a deleted member of ws root
		assertThat(root.findDeletedMembersWithHistory(IResource.DEPTH_ONE, createTestMonitor())).isEmpty();
		assertThat(root.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, createTestMonitor())).isEmpty();
		assertThat(root.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, createTestMonitor())).isEmpty();

		// scrub the project
		project.delete(true, createTestMonitor());
		project.create(createTestMonitor());
		project.open(createTestMonitor());

		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_ONE, createTestMonitor())).isEmpty();

		// test folder
		IFolder folder = project.getFolder("folder");
		IFile file = folder.getFile("filex.txt");
		IFile folderAsFile = project.getFile(folder.getProjectRelativePath());

		// create and delete a file in a folder
		folder.create(true, true, createTestMonitor());
		file.create(createRandomContentsStream(), true, createTestMonitor());
		file.delete(true, true, createTestMonitor());

		// the deleted file should show up as a deleted member
		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_ONE, createTestMonitor())).isEmpty();
		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, createTestMonitor()))
				.containsExactly(file);
		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, createTestMonitor())).isEmpty();

		// recreate the file
		file.create(createRandomContentsStream(), true, createTestMonitor());

		// the deleted file should no longer show up as a deleted member
		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_ONE, createTestMonitor())).isEmpty();
		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, createTestMonitor())).isEmpty();
		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, createTestMonitor())).isEmpty();

		// deleting the folder should bring it back
		folder.delete(true, true, createTestMonitor());

		// the deleted file should show up as a deleted member of project
		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_ONE, createTestMonitor())).isEmpty();
		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, createTestMonitor()))
				.containsExactly(file);
		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, createTestMonitor())).isEmpty();

		// create and delete a file where the folder was
		folderAsFile.create(createRandomContentsStream(), true, createTestMonitor());
		folderAsFile.delete(true, true, createTestMonitor());
		folder.create(true, true, createTestMonitor());

		// the deleted file should show up as a deleted member of folder
		assertThat(folder.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, createTestMonitor()))
				.containsExactly(folderAsFile);
		assertThat(folder.findDeletedMembersWithHistory(IResource.DEPTH_ONE, createTestMonitor()))
				.containsExactlyInAnyOrder(file, folderAsFile);
		assertThat(folder.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, createTestMonitor()))
				.containsExactlyInAnyOrder(file, folderAsFile);

		// scrub the project
		project.delete(true, createTestMonitor());
		project.create(createTestMonitor());
		project.open(createTestMonitor());

		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_ONE, createTestMonitor())).isEmpty();

		// test a bunch of deletes
		folder = project.getFolder("folder");
		IFile file1 = folder.getFile("file1.txt");
		IFile file2 = folder.getFile("file2.txt");
		IFolder folder2 = folder.getFolder("folder2");
		IFile file3 = folder2.getFile("file3.txt");

		// create and delete a file in a folder
		folder.create(true, true, createTestMonitor());
		folder2.create(true, true, createTestMonitor());
		file1.create(createRandomContentsStream(), true, createTestMonitor());
		file2.create(createRandomContentsStream(), true, createTestMonitor());
		file3.create(createRandomContentsStream(), true, createTestMonitor());
		folder.delete(true, true, createTestMonitor());

		// under root
		assertThat(root.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, createTestMonitor())).isEmpty();
		assertThat(root.findDeletedMembersWithHistory(IResource.DEPTH_ONE, createTestMonitor())).isEmpty();
		assertThat(root.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, createTestMonitor()))
				.containsExactlyInAnyOrder(file1, file2, file3);

		// under project
		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, createTestMonitor())).isEmpty();
		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_ONE, createTestMonitor())).isEmpty();
		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, createTestMonitor()))
				.containsExactlyInAnyOrder(file1, file2, file3);

		// under folder
		assertThat(folder.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, createTestMonitor())).isEmpty();
		assertThat(folder.findDeletedMembersWithHistory(IResource.DEPTH_ONE, createTestMonitor())).hasSize(2);
		assertThat(folder.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, createTestMonitor())).hasSize(3);

		// under folder2
		assertThat(folder2.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, createTestMonitor())).isEmpty();
		assertThat(folder2.findDeletedMembersWithHistory(IResource.DEPTH_ONE, createTestMonitor())).hasSize(1);
		assertThat(folder2.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, createTestMonitor())).hasSize(1);

		project.delete(true, createTestMonitor());

		// once the project is gone, so is all the history for that project
		// under root
		assertThat(root.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, createTestMonitor())).isEmpty();
		assertThat(root.findDeletedMembersWithHistory(IResource.DEPTH_ONE, createTestMonitor())).isEmpty();
		assertThat(root.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, createTestMonitor())).isEmpty();

		// under project
		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, createTestMonitor())).isEmpty();
		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_ONE, createTestMonitor())).isEmpty();
		assertThat(project.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, createTestMonitor())).isEmpty();

		// under folder
		assertThat(folder.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, createTestMonitor())).isEmpty();
		assertThat(folder.findDeletedMembersWithHistory(IResource.DEPTH_ONE, createTestMonitor())).isEmpty();
		assertThat(folder.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, createTestMonitor())).isEmpty();

		// under folder2
		assertThat(folder2.findDeletedMembersWithHistory(IResource.DEPTH_ZERO, createTestMonitor())).isEmpty();
		assertThat(folder2.findDeletedMembersWithHistory(IResource.DEPTH_ONE, createTestMonitor())).isEmpty();
		assertThat(folder2.findDeletedMembersWithHistory(IResource.DEPTH_INFINITE, createTestMonitor())).isEmpty();
	}

	/**
	 * Test for retrieving contents of files with states logged in the HistoryStore.
	 */
	@Test
	public void testGetContents() throws Throwable {
		final int ITERATIONS = 20;

		/* Create common objects. */
		IProject project = getWorkspace().getRoot().getProject("Project");
		project.create(createTestMonitor());
		project.open(createTestMonitor());

		/* Create files. */
		IFile file = project.getFile("getContentsFile.txt");
		String contents = "This file has some contents in testGetContents.";
		createInWorkspace(file, contents);

		IFile secondValidFile = project.getFile("secondGetContentsFile.txt");
		contents = "A file with some other contents in testGetContents.";
		createInWorkspace(secondValidFile, contents);

		IHistoryStore historyStore = ((Workspace) getWorkspace()).getFileSystemManager().getHistoryStore();

		/* Simulated date -- Increment once for each edition added. */
		long myLong = 0L;

		/* Add multiple editions for one file location. */
		for (int i = 0; i < ITERATIONS; i++, myLong++) {
			FileInfo fileInfo = new FileInfo(file.getName());
			fileInfo.setLastModified(myLong);
			historyStore.addState(file.getFullPath(), ((Resource) file).getStore(), fileInfo, true);
			contents = "This file has some contents in testGetContents.";
			try (FileOutputStream output = new FileOutputStream(file.getLocation().toFile())) {
				createInputStream(contents).transferTo(output);
			}
			file.refreshLocal(IResource.DEPTH_INFINITE, null);
		}

		/* Add multiple editions for second file location. */
		for (int i = 0; i < ITERATIONS; i++, myLong++) {
			FileInfo fileInfo = new FileInfo(file.getName());
			fileInfo.setLastModified(myLong);
			historyStore.addState(secondValidFile.getFullPath(), ((Resource) secondValidFile).getStore(), fileInfo, true);
			contents = "A file with some other contents in testGetContents.";
			try (FileOutputStream output = new FileOutputStream(secondValidFile.getLocation().toFile())) {
				createInputStream(contents).transferTo(output);
			}
			secondValidFile.refreshLocal(IResource.DEPTH_INFINITE, null);
		}

		/* Ensure contents of file and retrieved resource are identical.
		 Does not check timestamps. Timestamp checks are performed in a separate test. */
		IFileState[] stateArray = historyStore.getStates(file.getFullPath(), createTestMonitor());
		for (int i = 0; i < stateArray.length; i++, myLong++) {
			try (DataInputStream inFile = new DataInputStream(file.getContents(false))) {
				try (DataInputStream inContents = new DataInputStream(historyStore.getContents(stateArray[i]))) {
					assertTrue(i + " No match, files are not identical.", compareContent(inFile, inContents));
				}
			}
		}

		stateArray = historyStore.getStates(secondValidFile.getFullPath(), createTestMonitor());
		for (int i = 0; i < stateArray.length; i++, myLong++) {
			try (DataInputStream inFile = new DataInputStream(secondValidFile.getContents(false))) {
				try (DataInputStream inContents = new DataInputStream(historyStore.getContents(stateArray[i]))) {
					assertTrue(i + " No match, files are not identical.", compareContent(inFile, inContents));
				}
			}
		}

		/* Test getting an invalid file state. */
		for (int i = 0; i < ITERATIONS; i++) {
			final long immutableValue = myLong;
			// Create bogus FileState using invalid uuid.
			assertThrows(CoreException.class, () -> historyStore.getContents(
					new FileState(historyStore, IPath.ROOT, immutableValue, new UniversalUniqueIdentifier())));
		}

		/* Test verification using null file state. */
		for (int i = 0; i < ITERATIONS; i++) {
			assertThrows(RuntimeException.class, () -> historyStore.getContents(null));
		}
	}

	@Test
	public void testModifiedStamp() throws CoreException {
		/* Initialize common objects. */
		IProject project = getWorkspace().getRoot().getProject("Project");
		project.create(createTestMonitor());
		project.open(createTestMonitor());

		IFile file = project.getFile("file");
		file.create(createRandomContentsStream(), true, createTestMonitor());

		IFileState[] history = file.getHistory(createTestMonitor());
		// no history yet
		assertThat(history).isEmpty();
		// save the file's current time stamp - it will be remembered in the file state
		long fileTimeStamp = file.getLocalTimeStamp();
		file.setContents(createRandomContentsStream(), true, true, createTestMonitor());
		history = file.getHistory(createTestMonitor());
		// one state in the history
		assertThat(history).hasSize(1);
		// the timestamp in the state should match the previous file's timestamp
		assertThat(history[0].getModificationTime()).isEqualByComparingTo(fileTimeStamp);
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
	@Test
	public void testMoveFolder() throws Exception {
		String[] contents = {"content1", "content2", "content3", "content4", "content5"};
		// create common objects
		IProject project = getWorkspace().getRoot().getProject("MyProject");
		project.create(createTestMonitor());
		project.open(createTestMonitor());

		IFile file = project.getFile("file1.txt");

		IFolder folder = project.getFolder("folder1");
		IFolder folder2 = project.getFolder("folder2");
		file = folder.getFile("file1.txt");
		// Setup folder1 and file1.txt with some local history
		folder.create(true, true, createTestMonitor());
		file.create(createInputStream(contents[0]), true, createTestMonitor());
		file.setContents(createInputStream(contents[1]), true, true, createTestMonitor());
		file.setContents(createInputStream(contents[2]), true, true, createTestMonitor());
		IFileState[] states = file.getHistory(createTestMonitor());
		assertThat(states).hasSize(2).satisfiesExactly(
				first -> assertThat(compareContent(createInputStream(contents[1]), first.getContents())).isTrue(),
				second -> assertThat(compareContent(createInputStream(contents[0]), second.getContents())).isTrue());

		// Now do the move
		folder.move(folder2.getFullPath(), true, createTestMonitor());

		// Check to make sure the file has been moved
		IFile file2 = folder2.getFile("file1.txt");
		assertTrue("1.3", file2.getFullPath().toString().endsWith("folder2/file1.txt"));

		// Give the new (moved file) some new contents
		file2.setContents(createInputStream(contents[3]), true, true, createTestMonitor());
		file2.setContents(createInputStream(contents[4]), true, true, createTestMonitor());

		// Check the local history of both files
		states = file.getHistory(createTestMonitor());
		assertThat(states).hasSize(2).satisfiesExactly(
				first -> assertThat(compareContent(createInputStream(contents[1]), first.getContents())).isTrue(),
				second -> assertThat(compareContent(createInputStream(contents[0]), second.getContents())).isTrue());
		states = file2.getHistory(createTestMonitor());
		assertThat(states).hasSize(4).satisfiesExactly(
				first -> assertThat(compareContent(createInputStream(contents[3]), first.getContents())).isTrue(),
				second -> assertThat(compareContent(createInputStream(contents[2]), second.getContents())).isTrue(),
				third -> assertThat(compareContent(createInputStream(contents[1]), third.getContents())).isTrue(),
				fourth -> assertThat(compareContent(createInputStream(contents[0]), fourth.getContents())).isTrue());

		project.delete(true, createTestMonitor());
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
	@Test
	public void testMoveProject() throws Exception {
		String[] contents = {"content1", "content2", "content3", "content4", "content5"};
		// create common objects
		IProject project = getWorkspace().getRoot().getProject("MoveProjectProject");
		IProject project2 = getWorkspace().getRoot().getProject("SecondMoveProjectProject");
		project.create(createTestMonitor());
		project.open(createTestMonitor());

		IFile file = project.getFile("file1.txt");

		IFolder folder = project.getFolder("folder1");
		file = folder.getFile("file1.txt");
		// Setup folder1 and file1.txt with some local history
		folder.create(true, true, createTestMonitor());
		file.create(createInputStream(contents[0]), true, createTestMonitor());
		file.setContents(createInputStream(contents[1]), true, true, createTestMonitor());
		file.setContents(createInputStream(contents[2]), true, true, createTestMonitor());
		IFileState[] states = file.getHistory(createTestMonitor());
		assertThat(states).hasSize(2).satisfiesExactly(
				first -> assertThat(compareContent(createInputStream(contents[1]), first.getContents())).isTrue(),
				second -> assertThat(compareContent(createInputStream(contents[0]), second.getContents())).isTrue());

		// Now do the move
		project.move(IPath.fromOSString("SecondMoveProjectProject"), true, createTestMonitor());

		// Check to make sure the file has been moved
		IFile file2 = project2.getFile("folder1/file1.txt");
		assertTrue("1.3", file2.getFullPath().toString().endsWith("SecondMoveProjectProject/folder1/file1.txt"));

		// Give the new (copied file) some new contents
		file2.setContents(createInputStream(contents[3]), true, true, createTestMonitor());
		file2.setContents(createInputStream(contents[4]), true, true, createTestMonitor());

		// Check the local history of both files
		states = file.getHistory(createTestMonitor());

		// original file should not remember history when project is moved
		assertThat(states).isEmpty();
		states = file2.getHistory(createTestMonitor());
		assertThat(states).hasSize(4).satisfiesExactly(
				first -> assertThat(compareContent(createInputStream(contents[3]), first.getContents())).isTrue(),
				second -> assertThat(compareContent(createInputStream(contents[2]), second.getContents())).isTrue(),
				third -> assertThat(compareContent(createInputStream(contents[1]), third.getContents())).isTrue(),
				fourth -> assertThat(compareContent(createInputStream(contents[0]), fourth.getContents())).isTrue());

		project.delete(true, createTestMonitor());
	}

	@Test
	public void testRemoveAll() throws CoreException {
		/* Create common objects. */
		IProject project = getWorkspace().getRoot().getProject("Project");
		IFile file = project.getFile("removeAllStatesFile.txt");
		project.create(createTestMonitor());
		project.open(createTestMonitor());
		file.create(createRandomContentsStream(), true, createTestMonitor());

		final int ITERATIONS = 20;

		/* test remove in a file */
		for (int i = 0; i < ITERATIONS; i++) {
			file.setContents(createRandomContentsStream(), true, true, createTestMonitor());
		}

		/* Valid Case: Ensure correct number of states available. */
		IFileState[] states = file.getHistory(createTestMonitor());
		assertThat(states).hasSize(ITERATIONS);

		/* Remove all states, and verify that no states remain. */
		file.clearHistory(createTestMonitor());
		states = file.getHistory(createTestMonitor());
		assertThat(states).isEmpty();

		/* test remove in a folder -- make sure it does not affect other resources' states*/
		IFolder folder = project.getFolder("folder");
		IFile anotherOne = folder.getFile("anotherOne");
		folder.create(true, true, createTestMonitor());
		anotherOne.create(createRandomContentsStream(), true, createTestMonitor());
		for (int i = 0; i < ITERATIONS; i++) {
			file.setContents(createRandomContentsStream(), true, true, createTestMonitor());
			anotherOne.setContents(createRandomContentsStream(), true, true, createTestMonitor());
		}

		states = file.getHistory(createTestMonitor());
		assertThat(states).hasSize(ITERATIONS);
		states = anotherOne.getHistory(createTestMonitor());
		assertThat(states).hasSize(ITERATIONS);

		/* Remove all states, and verify that no states remain. */
		project.clearHistory(createTestMonitor());
		states = file.getHistory(createTestMonitor());
		assertThat(states).isEmpty();
		states = anotherOne.getHistory(createTestMonitor());
		assertThat(states).isEmpty();

		/* test remove in a folder -- make sure it does not affect other resources' states*/
		IFile aaa = project.getFile("aaa");
		IFolder bbb = project.getFolder("bbb");
		anotherOne = bbb.getFile("anotherOne");
		IFile ccc = project.getFile("ccc");
		bbb.create(true, true, createTestMonitor());
		anotherOne.create(createRandomContentsStream(), true, createTestMonitor());
		aaa.create(createRandomContentsStream(), true, createTestMonitor());
		ccc.create(createRandomContentsStream(), true, createTestMonitor());

		for (int i = 0; i < ITERATIONS; i++) {
			anotherOne.setContents(createRandomContentsStream(), true, true, createTestMonitor());
			aaa.setContents(createRandomContentsStream(), true, true, createTestMonitor());
			ccc.setContents(createRandomContentsStream(), true, true, createTestMonitor());
		}

		states = anotherOne.getHistory(createTestMonitor());
		assertThat(states).hasSize(ITERATIONS);
		states = aaa.getHistory(createTestMonitor());
		assertThat(states).hasSize(ITERATIONS);
		states = ccc.getHistory(createTestMonitor());
		assertThat(states).hasSize(ITERATIONS);

		/* Remove all states, and verify that no states remain. aaa and ccc should not be affected. */
		bbb.clearHistory(createTestMonitor());
		states = anotherOne.getHistory(createTestMonitor());
		assertThat(states).isEmpty();
		states = aaa.getHistory(createTestMonitor());
		assertThat(states).hasSize(ITERATIONS);
		states = ccc.getHistory(createTestMonitor());
		assertThat(states).hasSize(ITERATIONS);
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
	@Test
	public void testSimpleCopy() throws Exception {
		/* Initialize common objects. */
		IProject project = getWorkspace().getRoot().getProject("SimpleCopyProject");
		project.create(createTestMonitor());
		project.open(createTestMonitor());

		String[] contents = {"content1", "content2", "content3", "content4", "content5"};
		IFile file = project.getFile("simpleCopyFileWithHistoryCopy");
		IFile copyFile = project.getFile("copyOfSimpleCopyFileWithHistoryCopy");

		/* Create first file. */
		file.create(createInputStream(contents[0]), true, null);

		/* Set new contents on first file. Should add two entries to the history store. */
		file.setContents(createInputStream(contents[1]), true, true, null);
		file.setContents(createInputStream(contents[2]), true, true, null);

		/* Copy first file to the second. Second file should have no history. */
		file.copy(copyFile.getFullPath(), true, null);

		/* Check history for both files. */
		IFileState[] states = file.getHistory(null);
		assertThat(states).hasSize(2);
		states = copyFile.getHistory(null);
		assertThat(states).hasSize(2);

		/* Set new contents on second file. Should add two entries to the history store. */
		copyFile.setContents(createInputStream(contents[3]), true, true, null);
		copyFile.setContents(createInputStream(contents[4]), true, true, null);

		/* Check history for both files. */
		// Check log for original file.
		states = file.getHistory(null);
		assertThat(states).hasSize(2).satisfiesExactly(
				first -> assertThat(compareContent(createInputStream(contents[1]), first.getContents())).isTrue(),
				second -> assertThat(compareContent(createInputStream(contents[0]), second.getContents())).isTrue());

		// Check log for copy.
		states = copyFile.getHistory(null);
		assertThat(states).hasSize(4).satisfiesExactly(
				first -> assertThat(compareContent(createInputStream(contents[3]), first.getContents())).isTrue(),
				second -> assertThat(compareContent(createInputStream(contents[2]), second.getContents())).isTrue(),
				third -> assertThat(compareContent(createInputStream(contents[1]), third.getContents())).isTrue(),
				fourth -> assertThat(compareContent(createInputStream(contents[0]), fourth.getContents())).isTrue());
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
	@Test
	public void testSimpleMove() throws Exception {
		/* Initialize common objects. */
		IProject project = getWorkspace().getRoot().getProject("SimpleMoveProject");
		project.create(createTestMonitor());
		project.open(createTestMonitor());

		String[] contents = {"content1", "content2", "content3", "content4", "content5"};
		IFile file = project.getFile("simpleMoveFileWithCopy");
		IFile moveFile = project.getFile("copyOfSimpleMoveFileWithCopy");

		/* Create first file. */
		file.create(createInputStream(contents[0]), true, null);

		/* Set new contents on source file. Should add two entries to the history store. */
		file.setContents(createInputStream(contents[1]), true, true, null);
		file.setContents(createInputStream(contents[2]), true, true, null);

		/* Move source file to second location.
		 * Moved files should have the history of the original file.
		 */
		file.move(moveFile.getFullPath(), true, null);

		/* Check history for both files. */
		IFileState[] states = file.getHistory(null);
		assertThat(states).hasSize(2);
		states = moveFile.getHistory(null);
		assertThat(states).hasSize(2);

		/* Set new contents on moved file. Should add two entries to the history store. */
		moveFile.setContents(createInputStream(contents[3]), true, true, null);
		moveFile.setContents(createInputStream(contents[4]), true, true, null);

		/* Check history for both files. */
		// Check log for original file.
		states = file.getHistory(null);
		assertThat(states).hasSize(2).satisfiesExactly(
				first -> assertThat(compareContent(createInputStream(contents[1]), first.getContents())).isTrue(),
				second -> assertThat(compareContent(createInputStream(contents[0]), second.getContents())).isTrue());

		// Check log for moved file.
		states = moveFile.getHistory(null);
		assertThat(states).hasSize(4).satisfiesExactly(
				first -> assertThat(compareContent(createInputStream(contents[3]), first.getContents())).isTrue(),
				second -> assertThat(compareContent(createInputStream(contents[2]), second.getContents())).isTrue(),
				third -> assertThat(compareContent(createInputStream(contents[1]), third.getContents())).isTrue(),
				fourth -> assertThat(compareContent(createInputStream(contents[0]), fourth.getContents())).isTrue());
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
	@Test
	public void testSimpleUse() throws Exception {
		/* Initialize common objects. */
		IProject project = getWorkspace().getRoot().getProject("Project");
		project.create(createTestMonitor());
		project.open(createTestMonitor());

		String[] contents = {"content1", "content2", "content3"};
		IFile file = project.getFile("file");

		/* Create the file. */
		file.create(createInputStream(contents[0]), true, createTestMonitor());

		/* Set new contents on the file. Should add two entries to the store. */
		for (int i = 0; i < 2; i++) {
			file.setContents(createInputStream(contents[i + 1]), true, true, createTestMonitor());
		}

		/* Ensure two entries are available for the file, and that content matches. */
		IFileState[] states = file.getHistory(createTestMonitor());
		assertThat(states).hasSize(2).satisfiesExactly(
				first -> assertThat(compareContent(createInputStream(contents[1]), first.getContents())).isTrue(),
				second -> assertThat(compareContent(createInputStream(contents[0]), second.getContents())).isTrue());

		/* Delete the file. Should add an entry to the store. */
		file.delete(true, true, createTestMonitor());

		/* Ensure three entries are available for the file, and that content matches. */
		states = file.getHistory(createTestMonitor());
		assertThat(states).hasSize(3).satisfiesExactly(
				first -> assertThat(compareContent(createInputStream(contents[2]), first.getContents())).isTrue(),
				second -> assertThat(compareContent(createInputStream(contents[1]), second.getContents())).isTrue(),
				third -> assertThat(compareContent(createInputStream(contents[0]), third.getContents())).isTrue());
		/* Roll file back to first version, and ensure that content matches. */
		states = file.getHistory(createTestMonitor());
		// Create the file with the contents from one of the states.
		// Won't add another entry to the store.
		file.create(states[0].getContents(), false, createTestMonitor());

		// Check history store.
		states = file.getHistory(createTestMonitor());
		assertThat(states).hasSize(3).satisfiesExactly(
				first -> assertThat(compareContent(createInputStream(contents[2]), first.getContents())).isTrue(),
				second -> assertThat(compareContent(createInputStream(contents[1]), second.getContents())).isTrue(),
				third -> assertThat(compareContent(createInputStream(contents[0]), third.getContents())).isTrue());

		// Check file contents.
		assertThat(compareContent(createInputStream(contents[2]), file.getContents(false))).isTrue();

		/* Set new contents on the file. Should add an entry to the history store. */
		file.setContents(createInputStream(contents[1]), true, true, null);

		/* Ensure four entries are available for the file, and that entries match. */
		states = file.getHistory(createTestMonitor());
		assertThat(states).hasSize(4).satisfiesExactly(
				first -> assertThat(compareContent(createInputStream(contents[2]), first.getContents())).isTrue(),
				second -> assertThat(compareContent(createInputStream(contents[2]), second.getContents())).isTrue(),
				third -> assertThat(compareContent(createInputStream(contents[1]), third.getContents())).isTrue(),
				fourth -> assertThat(compareContent(createInputStream(contents[0]), fourth.getContents())).isTrue());

		/* Roll file back to third version, and ensure that content matches. */
		states = file.getHistory(createTestMonitor());
		// Will add another entry to log.
		file.setContents(states[2], true, true, createTestMonitor());

		// Check history log.
		states = file.getHistory(createTestMonitor());
		assertThat(states).hasSize(5).satisfiesExactly(
				first -> assertThat(compareContent(createInputStream(contents[1]), first.getContents())).isTrue(),
				second -> assertThat(compareContent(createInputStream(contents[2]), second.getContents())).isTrue(),
				third -> assertThat(compareContent(createInputStream(contents[2]), third.getContents())).isTrue(),
				fourth -> assertThat(compareContent(createInputStream(contents[1]), fourth.getContents())).isTrue(),
				fifth -> assertThat(compareContent(createInputStream(contents[0]), fifth.getContents())).isTrue());

		// Check file contents.
		assertThat(compareContent(createInputStream(contents[1]), file.getContents(false))).isTrue();
	}

}
