/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.tests.ccvs.core.provider;

import java.io.*;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.resources.EclipseSynchronizer;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.tests.ccvs.core.CVSTestSetup;
import org.eclipse.team.tests.ccvs.core.EclipseTest;

public class ResourceDeltaTest extends EclipseTest {

	/**
	 * Constructor for ResourceDeltaTest.
	 */
	public ResourceDeltaTest() {
		super();
	}

	/**
	 * Constructor for ResourceDeltaTest.
	 * @param name
	 */
	public ResourceDeltaTest(String name) {
		super(name);
	}

	public static Test suite() {
		String testName = System.getProperty("eclipse.cvs.testName");
		if (testName == null) {
			TestSuite suite = new TestSuite(ResourceDeltaTest.class);
			return new CVSTestSetup(suite);
		} else {
			return new CVSTestSetup(new ResourceDeltaTest(testName));
		}
	}
	
	public void assertNotManaged(ICVSFile cvsFile) throws CVSException {
		assertTrue("File " + cvsFile.getName() + " should not be managed", ! cvsFile.isManaged());
	}
	
	public void assertNotManaged(ICVSFolder cvsFolder) throws CVSException {
		assertNotManaged(cvsFolder, false);
	}
	
	public void assertNotManaged(ICVSFolder cvsFolder, boolean rootManaged) throws CVSException {
		if (!rootManaged)
			assertTrue("Folder " + cvsFolder.getName() + " should not be managed", ! cvsFolder.isManaged());
		assertTrue("Folder " + cvsFolder.getName() + " should not be a cvs folder", ! cvsFolder.isCVSFolder());
		cvsFolder.acceptChildren(new ICVSResourceVisitor() {
			@Override
			public void visitFile(ICVSFile file) throws CVSException {
				assertNotManaged(file);
			}
			@Override
			public void visitFolder(ICVSFolder folder) throws CVSException {
				assertNotManaged(folder, false);
			}
		});
	}
	
	public void assertAdditionMarkerFor(IResource resource, boolean exists) {
		// Addition markers are no longer used
	}
	
	public void assertDeletionMarkerFor(IResource resource, boolean exists) {
	}
	
	public void testOrphanedSubtree() throws TeamException, CoreException {
		IProject project = createProject("testOrphanedSubtree", new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt", "folder1/folder2/b.txt"});
		IFolder folder = project.getFolder(new Path("folder1"));
		folder.move(new Path("moved"), false, false, null);
		folder = project.getFolder(new Path("moved"));
		ICVSFolder cvsFolder = CVSWorkspaceRoot.getCVSFolderFor(folder);
		assertNotManaged(cvsFolder);
		assertAdditionMarkerFor(folder, true);
	}
	
	public void testOrphanedSubsubtree() throws TeamException, CoreException {
		IProject project = createProject("testOrphanedSubsubtree", new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt", "folder1/folder2/b.txt"});
		IFolder folder = project.getFolder(new Path("folder1"));
		IFolder target = project.getFolder("sub");
		target.create(false, true, null);
		folder.move(new Path("sub/moved"), false, false, null);
		folder = project.getFolder(new Path("sub/moved"));
		ICVSFolder cvsFolder = CVSWorkspaceRoot.getCVSFolderFor(folder);
		assertNotManaged(cvsFolder);
		assertAdditionMarkerFor(target, true);
		assertAdditionMarkerFor(folder, false);
	}
	
	public void testDeletionHandling() throws TeamException, CoreException {
		IProject project = createProject("testDeletionHandling", new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt", "folder1/folder2/b.txt"});
		addResources(project, new String[] {"added.txt"}, false);
		assertAdditionMarkerFor(project.getFile("added.txt"), false);
		deleteResources(project, new String[] {"added.txt", "deleted.txt"}, false);
		ICVSFile file = CVSWorkspaceRoot.getCVSFileFor(project.getFile("added.txt"));
		assertNotManaged(file);
		assertDeletionMarkerFor(project.getFile("added.txt"), false);
		file = CVSWorkspaceRoot.getCVSFileFor(project.getFile("deleted.txt"));
		assertTrue("File " + file.getName() + " should be managed", file.isManaged());
		ResourceSyncInfo info = file.getSyncInfo();
		assertTrue("File " + file.getName() + " should be marked as deleted", info.isDeleted());
		assertDeletionMarkerFor(project.getFile("deleted.txt"), true);
	}
	
	public void testFileAdditionHandling() throws TeamException, CoreException {
		IProject project = createProject("testFileAdditionHandling", new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt", "folder1/folder2/b.txt"});
		deleteResources(project, new String[] {"deleted.txt"}, false);
		assertDeletionMarkerFor(project.getFile("deleted.txt"), true);
		addResources(project, new String[] {"deleted.txt"}, false);
		ICVSFile file = CVSWorkspaceRoot.getCVSFileFor(project.getFile("deleted.txt"));
		assertTrue("File " + file.getName() + " should be managed", file.isManaged());
		ResourceSyncInfo info = file.getSyncInfo();
		assertTrue("File " + file.getName() + " should not be marked as deleted", ! info.isDeleted());
		assertTrue("File " + file.getName() + " should not be marked as addition", ! info.isAdded());
		assertDeletionMarkerFor(project.getFile("deleted.txt"), false);
	}
	
	public void testFolderAdditionHandling() throws TeamException, CoreException {
		IProject project = createProject("testFolderAdditionHandling", new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt", "folder1/folder2/b.txt"});
		IFolder folder = project.getFolder("newfolder");
		folder.create(false, true, null);
		assertAdditionMarkerFor(folder, true);
		addResources(new IResource[] {folder});
		assertAdditionMarkerFor(folder, false);
	}
	
	/**
	 * Method setContents is used to set the contents of a java.io.File so we
	 * can test out-of-sync situations
	 * 
	 * @param ioFile
	 */
	private void setContents(File ioFile) throws IOException {
		// wait to ensure the timestamp differs from the one Core has
		waitMsec(1500);
		InputStream in = new BufferedInputStream(getRandomContents());
		try (OutputStream out = new BufferedOutputStream(new FileOutputStream(ioFile))) {
			int next = in.read();
			while (next != -1) {
				out.write(next);
				next = in.read();
			}
		}
	}
	
	/**
	 * This tests maks sure that performing a CVS operation on a file that is
	 * out-of-sync results in the proper exception.
	 */
	public void testOperationOnOutOfSync() throws CoreException, TeamException, IOException {
		IProject project = createProject("testFolderAdditionHandling", new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt", "folder1/folder2/b.txt"});
		IFile file = project.getFile("changed.txt");
		setContentsAndEnsureModified(file);
		File ioFile = file.getLocation().toFile();
		setContents(ioFile);
		try {
			updateProject(project, null, false);
		} catch (CVSException e) {
			// We expect to get an out-of-sync exception
			if (!containsCode(e, IResourceStatus.OUT_OF_SYNC_LOCAL))
				throw e;
		}
	}
	
	private boolean containsCode(CoreException e, int code) {
		return containsCode(e.getStatus(), code);
	}
	
	private boolean containsCode(IStatus status, int code) {
		if (status.getCode() == code)
			return true;
		if (status.isMultiStatus()) {
			IStatus[] children = status.getChildren();
			for (IStatus child : children) {
				if (containsCode(child,code))
					return true;
			}
		}
		Throwable t = status.getException();
		if (t instanceof CoreException) {
			CoreException e = (CoreException) t;
			return containsCode(e, code);
		}
		return false;
	}

	public void testAllCVSFolderRemoval() throws CoreException, TeamException {
		IProject project = createProject("testAllCVSFolderRemoval", new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt", "folder1/folder2/b.txt"});
		// ensure that all th sync info is loaded
		EclipseSynchronizer.getInstance().ensureSyncInfoLoaded(new IResource[] {project}, IResource.DEPTH_INFINITE);
		// delete the CVS folders from the file system and refresh
		String[] cvsFolders = new String[] {"CVS", "folder1/CVS", "folder1/folder2/CVS"};
		deleteIOFiles(project, cvsFolders);
		// The project should no longer be managed
		assertNotManaged(CVSWorkspaceRoot.getCVSFolderFor(project));
	}

	public void testSomeCVSFolderRemoval() throws CoreException, TeamException {
		IProject project = createProject("testAllCVSFolderRemoval", new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt", "folder1/folder2/b.txt"});
		// ensure that all th sync info is loaded
		EclipseSynchronizer.getInstance().ensureSyncInfoLoaded(new IResource[] {project}, IResource.DEPTH_INFINITE);
		// delete the CVS folders from the file system and refresh
		String[] cvsFolders = new String[] {"folder1/CVS", "folder1/folder2/CVS"};
		deleteIOFiles(project, cvsFolders);
		// The project should no longer be managed
		assertNotManaged(CVSWorkspaceRoot.getCVSFolderFor(project.getFolder("folder1")), true);
	}
	
	public void deleteIOFiles(IProject project, String[] cvsFolders)
		throws CoreException {
		IPath rootPath = project.getLocation();
		for (String childPath : cvsFolders) {
			IPath fullPath = rootPath.append(childPath);
			deepDelete(fullPath.toFile());
		}
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
	}

	private static void deepDelete(File resource) {
		if (resource.isDirectory()) {
			File[] fileList = resource.listFiles();
			for (File f : fileList) {
				deepDelete(f);
			}
		}
		resource.delete();
	}
	
	public void testCVSFodlersMarkedTeamPrivate() throws CoreException, TeamException {
		IProject project = createProject("testTeamPrivatefolders", new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt", "folder1/folder2/b.txt"});
		
		project.delete(false /* preserve contents */, true, null);
		project.create(null);
		project.open(null);
		project.accept(new IResourceProxyVisitor() {
			@Override
			public boolean visit(IResourceProxy proxy) throws CoreException {
				if(proxy.getName().equals("CVS")) {
					fail("all folders should be marked as team private. This one was not:" + proxy.requestResource().getFullPath());
				}
				return true;
			}
		}, 0);
	}
	
	public void testExternalDeletion() throws CoreException, TeamException {
		IProject project = createProject("testExternalDeletion", new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt", "folder1/folder2/b.txt"});
		IFile file = project.getFile("folder1/a.txt");
		deepDelete(file.getLocation().toFile());
		file.refreshLocal(IResource.DEPTH_ZERO, DEFAULT_MONITOR);
		assertTrue(!file.exists());
		ICVSFile cvsFile = CVSWorkspaceRoot.getCVSFileFor(file);
		assertTrue(cvsFile.isManaged());
		byte[] syncBytes = cvsFile.getSyncBytes();
		assertTrue(ResourceSyncInfo.isDeletion(syncBytes));
	}
}
