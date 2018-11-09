/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.team.tests.ccvs.core.cvsresources;


import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.Team;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.CVSTeamProvider;
import org.eclipse.team.internal.ccvs.core.resources.EclipseSynchronizer;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.core.syncinfo.MutableResourceSyncInfo;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.core.util.SyncFileWriter;
import org.eclipse.team.tests.ccvs.core.CVSTestSetup;
import org.eclipse.team.tests.ccvs.core.EclipseTest;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests the EclipseSynchronizer.
 * Does not test state change broadcasts.
 */
public class EclipseSynchronizerTest extends EclipseTest {
	private static EclipseSynchronizer sync = EclipseSynchronizer.getInstance();
	
	public EclipseSynchronizerTest() {
		super();
	}
	
	public EclipseSynchronizerTest(String name) {
		super(name);
	}
	
	public static Test suite() {
		TestSuite suite = new TestSuite();

		// Run all tests twice to ensure consistency between batched and non-batched behaviour.
		// 1. First run -- no batching
		suite.addTestSuite(EclipseSynchronizerTest.class);
		// 2. Second run -- with batching
		suite.addTest(new BatchedTestSetup(new TestSuite(EclipseSynchronizerTest.class)));
		return new CVSTestSetup(suite);
	}
	
	public void testFolderSync() throws CoreException, CVSException {
		// Workspace root
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		// Setting should not be an error but sync info should always be null
		FolderSyncInfo info = sync.getFolderSync(root);
		assertNull(info);
		sync.deleteFolderSync(root);
		sync.setFolderSync(root, dummyFolderSync(root));
		info = sync.getFolderSync(root);
		assertNull(info);

		// Non-existant project
		IProject project = root.getProject(getName() + "-" + System.currentTimeMillis());
		assertDoesNotExistInFileSystem(project);
		_testFolderSyncInvalid(project);

		// Project
		project.create(null);
		project.open(null);
		_testFolderSyncValid(project);
		
		// Non-existant folder
		IFolder folder = project.getFolder("folder1");
		assertDoesNotExistInFileSystem(folder);
		_testFolderSyncInvalid(folder);
		
		// Non-existant folder with non-existant parent
		IFolder childFolder = folder.getFolder("folder2");
		assertDoesNotExistInFileSystem(childFolder);
		_testFolderSyncInvalid(childFolder);
		
		// Folder
		folder.create(false /*force*/, true /*local*/, null);
		_testFolderSyncValid(folder);
		
		// Child folder
		childFolder.create(false /*force*/, true /*local*/, null);
		_testFolderSyncValid(childFolder);
		
		// Deleted folder -- sync info should no longer exist
		sync.setFolderSync(folder, dummyFolderSync(folder));
		folder.delete(false /*force*/, null);
		_testFolderSyncInvalid(folder); // verifies sync info was deleted
		
		// Recreated folder -- sync info should not be preserved across deletions
		folder.create(false /*force*/, true /*local*/, null);
		sync.setFolderSync(folder, dummyFolderSync(folder));
		folder.delete(false /*force*/, null);
		folder.create(false /*force*/, true /*local*/, null);
		_testFolderSyncValid(folder); // verifies sync info has not reappeared
		
		// Deleted project
		sync.setFolderSync(project, dummyFolderSync(project));
		project.delete(false /*force*/, null);
		_testFolderSyncInvalid(project);
	}
	
	/*
	 * Test get/set/delete folder sync for things that support it.
	 * Assumes container does not already have sync info.
	 */
	private void _testFolderSyncValid(IContainer container) throws CoreException, CVSException {
		FolderSyncInfo info = sync.getFolderSync(container);
		assertNull(info);
		sync.deleteFolderSync(container);
		FolderSyncInfo newInfo = dummyFolderSync(container);
		sync.setFolderSync(container, newInfo);
		info = sync.getFolderSync(container);
		assertEquals(newInfo, info);
		// verify that deleteFolderSync() does the right thing
		buildResources(container, new String[] { "hassync/", "nosync", "hassync.txt", "nosync.txt" }, true);
		IResource resource = container.getFile(new Path("hassync.txt"));
		sync.setResourceSync(resource, dummyResourceSync(resource));
		resource = container.getFolder(new Path("hassync"));
		sync.setResourceSync(resource, dummyResourceSync(resource));
		assertNotNull(sync.getResourceSync(container.getFile(new Path("hassync.txt"))));
		assertNull(sync.getResourceSync(container.getFile(new Path("nosync.txt"))));
		assertNotNull(sync.getResourceSync(container.getFolder(new Path("hassync"))));
		assertNull(sync.getResourceSync(container.getFolder(new Path("nosync"))));
		if (container.getType() == IResource.FOLDER) {
			sync.setResourceSync(container, dummyResourceSync(container));
			assertNotNull(sync.getResourceSync(container));
		}
		// should delete folder sync for self, and resource sync for children
		sync.deleteFolderSync(container);
		info = sync.getFolderSync(container);
		assertNull(info);
		assertNull(sync.getResourceSync(container.getFile(new Path("hassync.txt"))));
		assertNull(sync.getResourceSync(container.getFile(new Path("nosync.txt"))));
		assertNull(sync.getResourceSync(container.getFolder(new Path("hassync"))));
		assertNull(sync.getResourceSync(container.getFolder(new Path("nosync"))));
		if (container.getType() == IResource.FOLDER) {
			assertNotNull(sync.getResourceSync(container));
		}
	}
	
	/*
	 * Test get/set/delete folder sync for things that should not support it.
	 * Assumes container does not already have sync info.
	 */
	private void _testFolderSyncInvalid(IContainer container) throws CVSException {
		FolderSyncInfo info = sync.getFolderSync(container);
		assertNull(info);
		sync.deleteFolderSync(container);
		try {
			sync.setFolderSync(container, dummyFolderSync(container));
			fail("Expected CVSException");
		} catch (CVSException e) {
		}
		info = sync.getFolderSync(container);
		assertNull(info);
	}
	
	public void testResourceSync() throws CoreException, CVSException {
		// Workspace root
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		_testResourceSyncInvalid(root);

		// Project
		IProject project = getUniqueTestProject(getName());
		assertExistsInFileSystem(project);
		_testResourceSyncInvalid(project);
		
		// Folder
		IFolder folder = project.getFolder("folder1");
		folder.create(false /*force*/, true /*local*/, null);
		_testResourceSyncValid(folder);
		
		// File
		IFile file = folder.getFile("file1");
		file.create(getRandomContents(), false /*force*/, null);
		_testResourceSyncValid(file);
		
		// Deleted/recreated file -- if parent exists, sync info should be preserved across deletions
		ResourceSyncInfo info = dummyResourceSync(file);
		sync.setResourceSync(file, info);
		// Note: deleting a resource will delete it's sync info unless the project has a CVS provider
		file.delete(false /*force*/, null);
		ResourceSyncInfo newInfo = sync.getResourceSync(file);
		//assertEquals(newInfo, info);
		assertEquals(newInfo, null); /* changed for reason noted above */
		file.create(getRandomContents(), false /*force*/, null);
		sync.setResourceSync(file, info); /* added for reason noted above */
		newInfo = sync.getResourceSync(file);
		assertEquals(newInfo, info);
		sync.deleteResourceSync(file);
		file.delete(false /*force*/, null);
		_testResourceSyncValid(file);
		
		// Deleted parent -- sync info of children should also be deleted
		sync.setResourceSync(file, info);
		folder.delete(false /*force*/, null);
		info = sync.getResourceSync(file);
		assertNull(info);
		
		// File in non-existant folder
		IFolder dummyFolder = project.getFolder("folder2");
		assertDoesNotExistInFileSystem(dummyFolder);
		IFile dummyFile = dummyFolder.getFile("file2");
		assertDoesNotExistInFileSystem(dummyFile);
		_testResourceSyncInvalid(dummyFile);
	}
	
	/*
	 * Test get/set/delete resource sync for things that support it.
	 * Assumes resource does not already have sync info.
	 */
	private void _testResourceSyncValid(IResource resource) throws CVSException {
		ResourceSyncInfo info = sync.getResourceSync(resource);
		assertNull(info);
		sync.deleteResourceSync(resource);
		ResourceSyncInfo newInfo = dummyResourceSync(resource);
		sync.setResourceSync(resource, newInfo);
		info = sync.getResourceSync(resource);
		assertEquals(newInfo, info);
		sync.deleteResourceSync(resource);
		info = sync.getResourceSync(resource);
		assertNull(info);
	}
	
	/*
	 * Test get/set/delete resource sync for things that should not support it.
	 * Assumes resource does not already have sync info.
	 */
	private void _testResourceSyncInvalid(IResource resource) throws CVSException {
		ResourceSyncInfo info = sync.getResourceSync(resource);
		assertNull(info);
		sync.deleteResourceSync(resource);
		try {
			sync.setResourceSync(resource, dummyResourceSync(resource));
			fail("Expected CVSException");
		} catch (CVSException e) {
		}
		info = sync.getResourceSync(resource);
		assertNull(info);
	}

	public void testIsIgnored() throws CoreException, TeamException {
		IProject project = getUniqueTestProject("isIgnoredTests");
		CVSTeamProvider.markAsTempShare(project);
		buildResources(project, new String[] {"a.txt", "c.java", "folder1/", "folder1/b.txt", "folder2/"}, true /* include project */);
		
		sync.addIgnored(project, "*.txt");
		
		assertIsIgnored(project.getFile("a.txt"), true);
		assertIsIgnored(project.getFile("c.java"), false);
		assertIsIgnored(project.getFolder("folder1"), false);
		assertIsIgnored(project.getFolder("folder2"), false);
		assertIsIgnored(project.getFile("folder1/b.txt"), false);	
		assertIsIgnored(project.getFile("folder1/not-existing.txt"), false);
		assertIsIgnored(project.getParent(), false);
		assertIsIgnored(project, false);
		
		sync.addIgnored(project, "folder1");
		
		assertIsIgnored(project.getFile("a.txt"), true);
		assertIsIgnored(project.getFile("c.java"), false);
		assertIsIgnored(project.getFolder("folder1"), true);
		assertIsIgnored(project.getFolder("folder2"), false);
		assertIsIgnored(project.getFile("folder1/b.txt"), true);	
		assertIsIgnored(project.getFile("folder1/not-existing.txt"), true);
		assertIsIgnored(project.getParent(), false);
		assertIsIgnored(project, false);
		
		// delete the ignores, the resource delta should clear the cached
		// ignore list
		IResource cvsIgnore = project.getFile(".cvsignore");
		cvsIgnore.delete(true, null);
		waitForIgnoreFileHandling();
		
		assertIsIgnored(project.getFile("a.txt"), false);
		assertIsIgnored(project.getFile("c.java"), false);
		assertIsIgnored(project.getFolder("folder1"), false);
		assertIsIgnored(project.getFolder("folder2"), false);
		assertIsIgnored(project.getFile("folder1/b.txt"), false);	
		assertIsIgnored(project.getFile("folder1/not-existing.txt"), false);
		assertIsIgnored(project.getParent(), false);
		assertIsIgnored(project, false);
		project.delete(true, true, null);
	}
	
	public void testIgnores() throws CoreException, CVSException {
		// Workspace root
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		_testIgnoresInvalid(root);

		// Non-existant project
		IProject project = root.getProject(getName() + "-" + System.currentTimeMillis());
		assertDoesNotExistInFileSystem(project);
		_testIgnoresInvalid(project);

		// Project
		project.create(null);
		project.open(null);
		_testIgnoresValid(project);
		
		// Non-existant folder
		IFolder folder = project.getFolder("folder1");
		assertDoesNotExistInFileSystem(folder);
		_testIgnoresInvalid(folder);
		
		// Non-existant folder with non-existant parent
		IFolder childFolder = folder.getFolder("folder2");
		assertDoesNotExistInFileSystem(childFolder);
		_testIgnoresInvalid(childFolder);
		
		// Folder
		folder.create(false /*force*/, true /*local*/, null);
		_testIgnoresValid(folder);
		
		// Child folder
		childFolder.create(false /*force*/, true /*local*/, null);
		_testIgnoresValid(childFolder);
		
		// Deleted folder -- ignores should no longer exist
		sync.addIgnored(folder, "*.foo");
		folder.delete(false /*force*/, null);
		_testIgnoresInvalid(folder); // verifies sync info was deleted
		
		// Recreated folder -- sync info should not be preserved across deletions
		folder.create(false /*force*/, true /*local*/, null);
		sync.addIgnored(folder, "*.foo");
		folder.delete(false /*force*/, null);
		folder.create(false /*force*/, true /*local*/, null);
		_testIgnoresValid(folder); // verifies sync info has not reappeared
		
		// Deleted project
		sync.addIgnored(project, "*.foo");
		project.delete(false /*force*/, null);
		_testIgnoresInvalid(project);
	}
	
	
	
	/*
	 * Test get/set ignores for things that should not support it.
	 * Assumes resource does not already have ignores.
	 */
	private void _testIgnoresValid(IContainer container) throws CVSException {
		String[] ignored = getIgnored(container);
		assertTrue(ignored.length == 0);
		sync.addIgnored(container, "*.xyz");
		ignored = getIgnored(container);
		assertBijection(ignored, new String[] { "*.xyz" }, null);
		sync.addIgnored(container, "*.abc");
		sync.addIgnored(container, "*.def");
		ignored = getIgnored(container);
		assertBijection(ignored, new String[] { "*.abc", "*.def", "*.xyz" }, null);
	}

	/**
	 * TODO: should use cached ignores somehow
	 * @param container
	 * @return String[]
	 * @throws CVSException
	 */
	private String[] getIgnored(IContainer container) throws CVSException {
		if (container.getType() == IResource.ROOT) return new String[0];
		String[] ignored = SyncFileWriter.readCVSIgnoreEntries(container);
		if (ignored == null) return new String[0];
		return ignored;
	}

	/*
	 * Test get/set ignores for things that should not support it.
	 * Assumes resource does not already have ignores.
	 */
	private void _testIgnoresInvalid(IContainer container) {
		try {
			sync.addIgnored(container, "*.xyz");
			fail("Expected CVSException");
		} catch (CVSException e) {
		}
	}
	
	public void testIgnorePatterns() throws CoreException {
		IProject project = getUniqueTestProject(getName());
		shareProject(project);
		// a
		// a.doc
		// a.txt
		// -b
		// -b.doc
		// -b.txt
		// --c
		// --c.doc
		// --c.txt
		// ---d
		// ---d.doc
		// ---d.txt
		// -c
		// -c.doc
		// -c.txt
		// --d
		// --d.doc
		// --d.txt
		buildResources(project, new String[] { "a/", "a.txt", "a.doc", "a/b/",
				"a/b.txt", "a/b.doc", "a/b/c/", "a/b/c.txt", "a/b/c.doc",
				"a/b/c/d/", "a/b/c/d.txt", "a/b/c/d.doc", "a/c/", "a/c/c.txt",
				"a/c/c.doc", "a/c/d/", "a/c/d.txt", "a/c/d.doc" }, false);

		// don't forget about project name in pattern e.g. '/{unique-project-name}/a/b/c/*.txt'
		
		Team.setAllIgnores(new String[] { "/*/c/*" }, new boolean[] { true });
		assertFalse(getCVSResource(project.getFolder("a/b/c")).isIgnored());
		assertFalse(getCVSResource(project.getFile("a/b/c.txt")).isIgnored());
		assertTrue(getCVSResource(project.getFolder("a/b/c/d")).isIgnored());
		assertTrue(getCVSResource(project.getFile("a/b/c/d.txt")).isIgnored());
		assertTrue(getCVSResource(project.getFolder("a/c/d")).isIgnored());
		assertTrue(getCVSResource(project.getFile("a/c/d.txt")).isIgnored());
		
		Team.setAllIgnores(new String[] { "/" + project.getName() + "/?/c/*" },
				new boolean[] { true });
		assertFalse(getCVSResource(project.getFolder("a/b/c/d")).isIgnored());
		assertFalse(getCVSResource(project.getFile("a/b/c/d.txt")).isIgnored());
		assertTrue(getCVSResource(project.getFolder("a/c/d")).isIgnored());
		assertTrue(getCVSResource(project.getFile("a/c/d.txt")).isIgnored());

		Team.setAllIgnores(new String[] { "/" + project.getName()
				+ "/?/c/*.doc" }, new boolean[] { true });
		assertFalse(getCVSResource(project.getFolder("a/c/d")).isIgnored());
		assertTrue(getCVSResource(project.getFolder("a/c/c.doc")).isIgnored());
		assertFalse(getCVSResource(project.getFolder("a/c/c.txt")).isIgnored());
		assertTrue(getCVSResource(project.getFile("a/c/d.doc")).isIgnored());
		assertFalse(getCVSResource(project.getFile("a/c/d.txt")).isIgnored());
	}
	
	public void testBug279111() throws CoreException {
		IProject project = getUniqueTestProject(getName());
		shareProject(project);
		// folder
		// -aaaa
		// -aaaa.txt
		// --bbbb
		// --bbbb.txt
		// -aaaa1
		// -aaaa1.txt
		// --bbbb1
		// --bbbb1.txt
		buildResources(project, new String[] { "folder/", "folder/aaaa/",
				"folder/aaaa/aaaa.txt", "folder/aaaa/bbbb/",
				"folder/aaaa/bbbb/bbbb.txt", "folder/aaaa1/",
				"folder/aaaa1/aaaa1.txt", "folder/aaaa1/bbbb1/",
				"folder/aaaa1/bbbb1/bbbb1.txt" }, false);

		Team.setAllIgnores(new String[] { "/*/aaaa/*" }, new boolean[] { true });
		assertFalse(getCVSResource(project.getFolder("folder/aaaa")).isIgnored());
		assertTrue(getCVSResource(project.getFile("folder/aaaa/aaaa.txt")).isIgnored());
		assertTrue(getCVSResource(project.getFolder("folder/aaaa/bbbb")).isIgnored());
		assertTrue(getCVSResource(project.getFile("folder/aaaa/bbbb/bbbb.txt")).isIgnored());
		assertFalse(getCVSResource(project.getFolder("folder/aaaa1")).isIgnored());
		assertFalse(getCVSResource(project.getFile("folder/aaaa1/aaaa.txt")).isIgnored());
		assertFalse(getCVSResource(project.getFolder("folder/aaaa1/bbbb1")).isIgnored());
		assertFalse(getCVSResource(project.getFile("folder/aaaa1/bbbb1/bbbb1.txt")).isIgnored());	
		
		Team.setAllIgnores(new String[] { "/*/aaaa*/*" },
				new boolean[] { true });
		assertFalse(getCVSResource(project.getFolder("folder/aaaa")).isIgnored());
		assertTrue(getCVSResource(project.getFile("folder/aaaa/aaaa.txt")).isIgnored());
		assertTrue(getCVSResource(project.getFolder("folder/aaaa/bbbb")).isIgnored());
		assertTrue(getCVSResource(project.getFile("folder/aaaa/bbbb/bbbb.txt")).isIgnored());
		assertFalse(getCVSResource(project.getFolder("folder/aaaa1")).isIgnored());
		assertTrue(getCVSResource(project.getFile("folder/aaaa1/aaaa.txt")).isIgnored());
		assertTrue(getCVSResource(project.getFolder("folder/aaaa1/bbbb1")).isIgnored());
		assertTrue(getCVSResource(project.getFile("folder/aaaa1/bbbb1/bbbb1.txt")).isIgnored());
		
		
		// '?' stands for exactly one character, so 'aaaa?' doesn't match 'aaaa'
		Team.setAllIgnores(new String[] { "/*/aaaa?/*" },
				new boolean[] { true });
		assertFalse(getCVSResource(project.getFolder("folder/aaaa")).isIgnored());
		assertFalse(getCVSResource(project.getFile("folder/aaaa/aaaa.txt")).isIgnored());
		assertFalse(getCVSResource(project.getFolder("folder/aaaa/bbbb")).isIgnored());
		assertFalse(getCVSResource(project.getFile("folder/aaaa/bbbb/bbbb.txt")).isIgnored());
		assertFalse(getCVSResource(project.getFolder("folder/aaaa1")).isIgnored());
		assertTrue(getCVSResource(project.getFile("folder/aaaa1/aaaa.txt")).isIgnored());
		assertTrue(getCVSResource(project.getFolder("folder/aaaa1/bbbb1")).isIgnored());
		assertTrue(getCVSResource(project.getFile("folder/aaaa1/bbbb1/bbbb1.txt")).isIgnored());
	}
	
	public void testBug279111_comment11() throws CoreException {
		IProject project = getUniqueTestProject(getName());
		shareProject(project);
		// folder
		// -aaaa
		// -aaaa.txt
		// --bbbb
		// --bbbb.txt
		// -aaaa1
		// -aaaa1.txt
		// --bbbb1
		// --bbbb1.txt
		buildResources(project, new String[] { "folder/", "folder/aaaa/",
				"folder/aaaa/aaaa.txt", "folder/aaaa/bbbb/",
				"folder/aaaa/bbbb/bbbb.txt", "folder/aaaa1/",
				"folder/aaaa1/aaaa1.txt", "folder/aaaa1/bbbb1/",
				"folder/aaaa1/bbbb1/bbbb1.txt" }, false);
		
		// Setting two patterns works like desired (ignoring a folder, subfolders and files)...
		Team.setAllIgnores(new String[] { "*/aaaa", "*/aaaa/*" }, new boolean[] { true, true });
		assertTrue(getCVSResource(project.getFolder("folder/aaaa")).isIgnored());
		assertTrue(getCVSResource(project.getFile("folder/aaaa/aaaa.txt")).isIgnored());
		assertTrue(getCVSResource(project.getFolder("folder/aaaa/bbbb")).isIgnored());
		assertTrue(getCVSResource(project.getFile("folder/aaaa/bbbb/bbbb.txt")).isIgnored());
		assertFalse(getCVSResource(project.getFolder("folder/aaaa1")).isIgnored());
		assertFalse(getCVSResource(project.getFile("folder/aaaa1/aaaa.txt")).isIgnored());
		assertFalse(getCVSResource(project.getFolder("folder/aaaa1/bbbb1")).isIgnored());
		assertFalse(getCVSResource(project.getFile("folder/aaaa1/bbbb1/bbbb1.txt")).isIgnored());		
		
		Team.setAllIgnores(new String[] { "*/aaaa*", "*/aaaa*/*" }, new boolean[] { true, true });
		assertTrue(getCVSResource(project.getFolder("folder/aaaa")).isIgnored());
		assertTrue(getCVSResource(project.getFile("folder/aaaa/aaaa.txt")).isIgnored());
		assertTrue(getCVSResource(project.getFolder("folder/aaaa/bbbb")).isIgnored());
		assertTrue(getCVSResource(project.getFile("folder/aaaa/bbbb/bbbb.txt")).isIgnored());
		assertTrue(getCVSResource(project.getFolder("folder/aaaa1")).isIgnored());
		assertTrue(getCVSResource(project.getFile("folder/aaaa1/aaaa.txt")).isIgnored());
		assertTrue(getCVSResource(project.getFolder("folder/aaaa1/bbbb1")).isIgnored());
		assertTrue(getCVSResource(project.getFile("folder/aaaa1/bbbb1/bbbb1.txt")).isIgnored());
		
		
		// ... but ignoring only the folder affects all subfolders and files as well
		Team.setAllIgnores(new String[] { "/*/aaaa" }, new boolean[] { true });
		assertTrue(getCVSResource(project.getFolder("folder/aaaa")).isIgnored());
		assertTrue(getCVSResource(project.getFile("folder/aaaa/aaaa.txt")).isIgnored());
		assertTrue(getCVSResource(project.getFolder("folder/aaaa/bbbb")).isIgnored());
		assertTrue(getCVSResource(project.getFile("folder/aaaa/bbbb/bbbb.txt")).isIgnored());
		assertFalse(getCVSResource(project.getFolder("folder/aaaa1")).isIgnored());
		assertFalse(getCVSResource(project.getFile("folder/aaaa1/aaaa.txt")).isIgnored());
		assertFalse(getCVSResource(project.getFolder("folder/aaaa1/bbbb1")).isIgnored());
		assertFalse(getCVSResource(project.getFile("folder/aaaa1/bbbb1/bbbb1.txt")).isIgnored());
		
		Team.setAllIgnores(new String[] { "/*/aaaa*" }, new boolean[] { true });
		assertTrue(getCVSResource(project.getFolder("folder/aaaa")).isIgnored());
		assertTrue(getCVSResource(project.getFile("folder/aaaa/aaaa.txt")).isIgnored());
		assertTrue(getCVSResource(project.getFolder("folder/aaaa/bbbb")).isIgnored());
		assertTrue(getCVSResource(project.getFile("folder/aaaa/bbbb/bbbb.txt")).isIgnored());
		assertTrue(getCVSResource(project.getFolder("folder/aaaa1")).isIgnored());
		assertTrue(getCVSResource(project.getFile("folder/aaaa1/aaaa.txt")).isIgnored());
		assertTrue(getCVSResource(project.getFolder("folder/aaaa1/bbbb1")).isIgnored());
		assertTrue(getCVSResource(project.getFile("folder/aaaa1/bbbb1/bbbb1.txt")).isIgnored());

	}
	
	public void testMembers() throws CoreException, CVSException {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project1 = getUniqueTestProject(getName() + "1");
		IProject project2 = root.getProject(getName() + "2" + System.currentTimeMillis());

		// Workspace root
		IResource[] members = sync.members(ResourcesPlugin.getWorkspace().getRoot());
		assertBijection(members, ResourcesPlugin.getWorkspace().getRoot().getProjects(), null);

		// Non-existant project
		members = sync.members(project2);
		assertEquals("Non-existant project should have no members", 0, members.length);
		
		// Non-existant folder
		IFolder folder = project1.getFolder("folder1");
		assertDoesNotExistInFileSystem(folder);
		members = sync.members(folder);
		assertEquals("Non-existant folder should have no members", 0, members.length);
		
		// Non-existant folder with non-existant parent
		IFolder childFolder = folder.getFolder("folder2");
		assertDoesNotExistInFileSystem(childFolder);
		members = sync.members(childFolder);
		assertEquals("Non-existant folder should have no members", 0, members.length);
		
		// Project
		buildResources(project1, new String[] {
			"hassync.txt", "deleted_nosync.txt", "deleted.txt", "hassync/", "deleted/", "deleted_nosync/" }, true);

		// initially none of the resources have sync info and they all exist
		IResource[] ignores = new IResource[] { project1.getFolder("CVS") };
		Set<IResource> expectedMembers = new HashSet<>(Arrays.asList(project1.members()));
		members = sync.members(project1);
		assertBijection(expectedMembers.toArray(), members, ignores);
		
		// add sync info, resources should still appear exactly once
		IResource resource = project1.getFile("hassync.txt");
		sync.setResourceSync(resource, dummyResourceSync(resource));
		resource = project1.getFolder("hassync");
		sync.setResourceSync(resource, dummyResourceSync(resource));

		resource = project1.getFile("deleted.txt");
		sync.setResourceSync(resource, dummyResourceSync(resource));
		resource = project1.getFolder("deleted");
		sync.setResourceSync(resource, dummyResourceSync(resource));

		members = sync.members(project1);
		assertBijection(expectedMembers.toArray(), members, ignores);

		// delete resources, those with sync info should still appear, those without should not
		// (Note: This is only true for projects that have a CVS provider)
		resource = project1.getFile("deleted.txt");
		resource.delete(false /*force*/, null);
		expectedMembers.remove(resource); /* added for reason noted above */
		resource = project1.getFolder("deleted");
		resource.delete(false /*force*/, null);
		expectedMembers.remove(resource); /* added for reason noted above */

		resource = project1.getFile("deleted_nosync.txt");
		resource.delete(false /*force*/, null);
		expectedMembers.remove(resource);
		resource = project1.getFolder("deleted_nosync");
		resource.delete(false /*force*/, null);
		expectedMembers.remove(resource);

		members = sync.members(project1);
		assertBijection(expectedMembers.toArray(), members, ignores);
		
		// delete sync info, only those that exist should appear
		resource = project1.getFile("hassync.txt");
		sync.deleteResourceSync(resource);
		resource = project1.getFolder("hassync");
		sync.deleteResourceSync(resource);

		resource = project1.getFile("deleted.txt");
		sync.deleteResourceSync(resource);
		expectedMembers.remove(resource);
		resource = project1.getFolder("deleted");
		sync.deleteResourceSync(resource);
		expectedMembers.remove(resource);

		members = sync.members(project1);
		assertBijection(expectedMembers.toArray(), members, ignores);
	}
	
	private FolderSyncInfo dummyFolderSync(IContainer container) {
		return new FolderSyncInfo("repo", ":pserver:user@host:/root", CVSTag.DEFAULT, false);
	}

	private ResourceSyncInfo dummyResourceSync(IResource resource) {
		if (resource.getType() == IResource.FILE) {
			MutableResourceSyncInfo info = new MutableResourceSyncInfo(resource.getName(), "1.1");
			info.setTag(CVSTag.DEFAULT);
			return info;
		} else {
			return new ResourceSyncInfo(resource.getName());
		}
	}
	
	/**
	 * Assert that there exists a bijection between the elements of the arrays.
	 */
	private <T> void assertBijection(T[] a, T[] b, T[] ignores) {
		List<T> listA = new LinkedList<>(Arrays.asList(a));
		List<T> listB = new LinkedList<>(Arrays.asList(b));
		if (ignores != null) {
			for (Object ignore : ignores) {
				listA.remove(ignore);
				listB.remove(ignore);
			}
		}
		assertEquals("Should have same number of elements", listA.size(), listB.size());
		for (T t : listB) {
			assertTrue("Should contain the same elements", listA.contains(t));
			listA.remove(t);
		}
	}
	
	/**
	 * Create a test project whose name is derived from the currently running test case.
	 * The resources are built using the names supplied in the given String array.
	 * Paths ending in / will be folders while others will be files. Intermediate folders
	 * are created as needed. Dummy sync info is applied to all created resources and
	 * the project is mapped to the CVS repository provider.
	 * @param resourcePaths paths of resources to be generated
	 * @return the create project
	 */
	@Override
	protected IProject createProject(String[] resourcePaths) throws CoreException {
		// Create the project and build the resources
		IProject project = getUniqueTestProject(getName());
		buildResources(project, resourcePaths, true);
		
		// Associate dummy sync info with al create resources
		project.accept(resource -> {
			if (resource.getType() != IResource.PROJECT) {
				sync.setResourceSync(resource, dummyResourceSync(resource));
			}
			if (resource.getType() != IResource.FILE) {
				sync.setFolderSync((IContainer)resource, dummyFolderSync((IContainer)resource));
			}
			return true;
		});
		
		// Map the project to CVS so the Move/Delete hook works
		RepositoryProvider.map(project, CVSProviderPlugin.getTypeId());
		return project;
	}
	
	/**
	 * Assert that the resources at the given resource paths have sync info.
	 * Also assert that the ancestors of the resources also have sync info
	 * @param project the project containing the resources
	 * @param resourcePaths the project relative resource paths
	 * @throws CVSException
	 */
	protected void assertHasSyncInfo(IProject project, String[] resourcePaths) throws CVSException {
		for (String path : resourcePaths) {
			IResource resource = findResource(project, path);
			assertHasSyncInfo(resource);
		}
	}

	private IResource findResource(IProject project, String path) {
		IResource resource = project.findMember(path);
		if (resource == null) {
			if (path.charAt(path.length()-1) == Path.SEPARATOR)
				resource = project.getFolder(path);
			else
				resource = project.getFile(path);
		}
		return resource;
	}

	/**
	 * Assert that the resource and its ancestors have sync info
	 * @param resource the resource being queried
	 * @throws CVSException
	 */
	protected void assertHasSyncInfo(IResource resource) throws CVSException {
		if (resource.getType() == IResource.ROOT) return;
		if (resource.getType() != IResource.FILE) {
			assertNotNull("Folder should have folder sync info but does not: " + resource.getProjectRelativePath(), sync.getFolderSync((IContainer)resource));
		}
		if (resource.getType() != IResource.PROJECT) {
			assertNotNull("Resource should have sync bytes but does not: " + resource.getProjectRelativePath(), sync.getSyncBytes(resource));
			assertHasSyncInfo(resource.getParent());
		}
	}
	
	/**
	 * Assert that the resources at the given resource paths do not have sync info.
	 * Also assert that the descendants of the resources also do not have sync info
	 * @param project
	 * @param resourcePaths
	 * @throws CVSException
	 */
	private void assertHasNoSyncInfo(IProject project, String[] resourcePaths) throws CoreException {
		for (String path : resourcePaths) {
			IResource resource = findResource(project, path);
			assertHasNoSyncInfo(resource);
		}
	}
	
	protected void assertHasNoSyncInfo(IResource resource) throws CoreException {
		if (resource.getType() == IResource.ROOT) return;
		if (resource.getType() != IResource.FILE) {
			assertNull("Folder should not have folder sync but does: " + resource.getProjectRelativePath(), sync.getFolderSync((IContainer)resource));
			IResource[] members = ((IContainer)resource).members();
			for (IResource child : members) {
				assertHasNoSyncInfo(child);
			}
		}
		if (resource.getType() != IResource.PROJECT) {
			assertNull("Resource should not have sync bytes but does: " + resource.getProjectRelativePath(), sync.getSyncBytes(resource));
		}
	}

	public void testDeleteFile() throws CoreException {
		// Create a project with dummy sync info
		IProject project =  createProject(new String[] {"folder1/folder2/file1", "folder1/folder2/file2"});
		
		// Delete the file and assert old sync info is still in place and new has no sync info
		IFile file = project.getFile("folder1/folder2/file1");
		file.delete(false, false, null);
		assertHasSyncInfo(project, new String[] {"folder1/folder2/file1"});
	}
	
	public void testDeleteFolder() throws CoreException {
		// Create a project with dummy sync info
		IProject project =  createProject(new String[] {"folder1/folder2/file1", "folder1/folder2/file2"});
		
		// Delete the folder and assert old sync info is still in place and new has no sync info
		IFolder folder = project.getFolder("folder1/folder2/");
		folder.delete(false, false, null);
		assertHasSyncInfo(project, new String[] {"folder1/folder2/file1", "folder1/folder2/file2"});
	}
	
	public void testMoveFile() throws CoreException {
		// Create a project with dummy sync info
		IProject project =  createProject(new String[] {"folder1/folder2/file1", "folder1/folder2/file2"});
		
		// Move the file and assert old sync info is still in place and new has no sync info
		IFile file = project.getFile("folder1/folder2/file1");
		project.getFolder("folder1/folder3/").create(false, true, null);
		file.move(project.getFolder("folder1/folder3/file1").getFullPath(), false, null);
		assertHasSyncInfo(project, new String[] {"folder1/folder2/file1"});
		assertHasNoSyncInfo(project, new String[] {"folder1/folder3"});
	}
	
	public void testMoveFolder() throws CoreException {
		// Create a project with dummy sync info
		IProject project =  createProject(new String[] {"folder1/folder2/file1"});
		
		// Move the folder and assert old sync info is still in place and new has no sync info
		IFolder folder = project.getFolder("folder1/folder2/");
		folder.move(project.getFolder("folder1/folder3").getFullPath(), false, null);
		assertHasSyncInfo(project, new String[] {"folder1/folder2/file1"});
		assertHasNoSyncInfo(project, new String[] {"folder1/folder3/"});
	}
	
	/*
	 * See bug 44446
	 */
	public void testFileRecreation() throws CoreException {
		// Create a project with dummy sync info
		IProject project =  createProject(new String[] {"folder1/file1"});
		
		// Remove the file and assert that it still has sync info
		IFile file = project.getFile("folder1/file1");
		file.delete(false, false, null);
		assertHasSyncInfo(file);
		
		// Recreate the file and assert that it still has sync info
		file.create(getRandomContents(), false /*force*/, null);
		assertHasSyncInfo(file);
		
		// unmanage the file and assert that sync info is gone
		sync.deleteResourceSync(file);
		assertHasNoSyncInfo(file);
	}
	
	/*
	 * This testcase simulates an update that has an incoming deletion and a merge 
	 * (which may do a move).
	 */
	public void testFileMoveAndDelete() throws CoreException {
		// Create a project with dummy sync info
		final IProject project =  createProject(new String[] {"folder1/file1", "folder1/file2"});
		
		sync.run(project, monitor -> {
			try {
				IFile file1 = project.getFile("folder1/file1");
				IFile file2 = project.getFile("folder1/file2");
				// Delete file 1
				file1.delete(false, false, null);
				assertHasSyncInfo(file1);
				assertHasSyncInfo(file2);
				sync.deleteResourceSync(file1);
				assertHasNoSyncInfo(file1);
				assertHasSyncInfo(file2);
				// Move file 2
				file2.move(new Path("file3"), false, false, null);
				assertHasNoSyncInfo(file1);
				assertHasSyncInfo(file2);
			} catch (CoreException e) {
				throw CVSException.wrapException(e);
			}
		}, null);
	}
	
	public void testMoveFolderOverFolder() throws CoreException {
		// Create a project with dummy sync info
		final IProject project =  createProject(new String[] {"folder1/file1", "folder2/file1"});
		
		// Change the sync info of folder1/file1 to be revision 1.9
		String revision = "1.9";
		IFile file11 = project.getFile("folder1/file1");
		ResourceSyncInfo info = sync.getResourceSync(file11);
		MutableResourceSyncInfo muttable = info.cloneMutable();
		muttable.setRevision(revision);
		sync.setResourceSync(file11, muttable);
		
		// Move the folder and verify that the sync info stays
		project.getFolder("folder2").delete(false, false, null);
		project.getFolder("folder1").move(new Path("folder2"), false, false, null);
		assertHasSyncInfo(file11);
		IFile file21 = project.getFile("folder2/file1");
		assertHasSyncInfo(file21);
		assertTrue(sync.getResourceSync(file11).getRevision().equals(revision));
		assertTrue(!sync.getResourceSync(file21).getRevision().equals(revision));
		
	}
}
