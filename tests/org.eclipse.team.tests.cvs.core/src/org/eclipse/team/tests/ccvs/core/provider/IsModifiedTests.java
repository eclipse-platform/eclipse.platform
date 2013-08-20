/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.tests.ccvs.core.provider;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.core.ICVSResourceVisitor;
import org.eclipse.team.internal.ccvs.core.IResourceStateChangeListener;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.util.ResourceStateChangeListeners;
import org.eclipse.team.tests.ccvs.core.CVSTestSetup;
import org.eclipse.team.tests.ccvs.core.EclipseTest;
import org.eclipse.team.tests.ccvs.core.TeamCVSTestPlugin;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import org.eclipse.jface.util.Util;

/**
 * Test isModified on file, folders and projects.
 */
public class IsModifiedTests extends EclipseTest {

	Set previouslyModified = new HashSet();
	Map changedResources = new HashMap();
	IResourceStateChangeListener listener = new IResourceStateChangeListener() {
		public void resourceSyncInfoChanged(IResource[] changedResources) {
			try {
				for (int i = 0; i < changedResources.length; i++) {
					IResource resource = changedResources[i];
					ICVSResource cvsResource = CVSWorkspaceRoot.getCVSResourceFor(resource);
					recordModificationState(cvsResource);
					recordParents(cvsResource);
					if (cvsResource.isFolder()) {
						recordChildren((ICVSFolder)cvsResource);
					}
				}
			} catch (CVSException e) {
				fail(e.getMessage());
			}
		}
		public void externalSyncInfoChange(IResource[] changedResources) {
			resourceSyncInfoChanged(changedResources);	
		}
		private void recordChildren(ICVSFolder folder) {
			try {
				folder.accept(new ICVSResourceVisitor() {
					public void visitFile(ICVSFile file) throws CVSException {
						recordModificationState(file);
					}
					public void visitFolder(ICVSFolder folder) throws CVSException {
						recordModificationState(folder);
						folder.acceptChildren(this);
					}
				});
			} catch (CVSException e) {
				fail(e.getMessage());
			}
		}
		private void recordParents(ICVSResource cvsResource) throws CVSException {
			if (cvsResource.getIResource().getType() == IResource.ROOT) return;
			recordModificationState(cvsResource);
			recordParents(cvsResource.getParent());
		}
		private void recordModificationState(ICVSResource cvsResource) throws CVSException {
			IsModifiedTests.this.changedResources.put(cvsResource.getIResource(), cvsResource.isModified(null) ? Boolean.TRUE : Boolean.FALSE);
		}
		public void resourceModified(IResource[] changedResources) {
			try {
				for (int i = 0; i < changedResources.length; i++) {
					IResource resource = changedResources[i];
					ICVSResource cvsResource = CVSWorkspaceRoot.getCVSResourceFor(resource);
					IsModifiedTests.this.changedResources.put(resource, cvsResource.isModified(null) ? Boolean.TRUE : Boolean.FALSE);
					recordParents(cvsResource);
					if (cvsResource.isFolder()) {
						recordChildren((ICVSFolder)cvsResource);
					}
				}
			} catch (CVSException e) {
				fail(e.getMessage());
			}
		}
		public void projectConfigured(IProject project) {
		}
		public void projectDeconfigured(IProject project) {
		}
	};
	
	/**
	 * Constructor for CVSProviderTest
	 */
	public IsModifiedTests() {
		super();
	}

	/**
	 * Constructor for CVSProviderTest
	 */
	public IsModifiedTests(String name) {
		super(name);
	}

	public static Test suite() {
		String testName = System.getProperty("eclipse.cvs.testName");
		if (testName == null) {
			TestSuite suite = new TestSuite(IsModifiedTests.class);
			return new CVSTestSetup(suite);
		} else {
			return new CVSTestSetup(new IsModifiedTests(testName));
		}
	}

	/**
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
		previouslyModified.clear();
		changedResources.clear();
		ResourceStateChangeListeners.getListener().addResourceStateChangeListener(listener);
	}

	/**
	 * @see junit.framework.TestCase#tearDown()
	 */
	protected void tearDown() throws Exception {
		previouslyModified.clear();
		changedResources.clear();
		ResourceStateChangeListeners.getListener().removeResourceStateChangeListener(listener);
		super.tearDown();
	}
	
	/*
	 * Assert that the modification state of the provided resources matches the
	 * provided state and that the other are the opposite state.
	 */
	private void assertModificationState(IContainer container, String[] resources, final boolean listedResourcesShouldBeModified) throws CVSException {
		final ICVSFolder rootFolder = CVSWorkspaceRoot.getCVSFolderFor(container);
		final List resourceList = new ArrayList();
		final Set modifiedResources = new HashSet();
		if (resources != null) {
			for (int i = 0; i < resources.length; i++) {
				String string = resources[i];
				resourceList.add(new Path(string));
			}
		}
		waitForIgnoreFileHandling();
		rootFolder.accept(new ICVSResourceVisitor() {
			public void visitFile(ICVSFile file) throws CVSException {
				assertModificationState(file);
			}
			public void visitFolder(ICVSFolder folder) throws CVSException {
				// find the deepest mistake
				folder.acceptChildren(this);
				assertModificationState(folder);	
			}
			public void assertModificationState(ICVSResource resource) throws CVSException {
				IPath relativePath = new Path(resource.getRelativePath(rootFolder));
				boolean resourceModified = resource.isModified(null);
				boolean resourceListed = resourceList.contains(relativePath);
				if (CVSTestSetup.FAIL_ON_BAD_DIFF) {
					assertTrue(resource.getIResource().getFullPath().toString() 
							+ (resourceModified ? " should not be modified but is" : " should be modified but isn't"),
						(listedResourcesShouldBeModified && (resourceModified == resourceListed)) ||
						(!listedResourcesShouldBeModified && (!resourceModified == resourceListed)));
				} else if (!resourceModified){
					// Only fail if a file that should be modified isn't
					assertTrue(resource.getIResource().getFullPath().toString() 
							+ " should be modified but isn't",
							(listedResourcesShouldBeModified && !resourceListed)
								|| (!listedResourcesShouldBeModified && resourceListed));
				}
					
//				Commented because the CVS core doesn't rely on resourceModify to be called.
//				IResource iResource = resource.getIResource();
//				if (resource.isModified()) {
//					modifiedResources.add(iResource);
//					if (!wasPreviouslyModified(iResource)) {
//						// the state has changed, make sure we got a notification
//						Boolean b = (Boolean)changedResources.get(iResource);
//						assertTrue("No notification received for state change of " + iResource.getFullPath().toString(),
//							b == Boolean.TRUE);
//					}	
//				} else {
//					if (wasPreviouslyModified(iResource)) {
//						// the state has changed, make sure we got a notification
//						Boolean b = (Boolean)changedResources.get(iResource);
//						assertTrue("No notification received for state change of " + iResource.getFullPath().toString(),
//							b == Boolean.FALSE);
//					}
//				}
			}
//			public boolean wasPreviouslyModified(IResource iResource) {
//				return previouslyModified.contains(iResource);
//			}
		});
		changedResources.clear();
		previouslyModified.clear();
		previouslyModified.addAll(modifiedResources);
	}
	
	/**
	 * Assert that a project (and all it's children) is clean after it is
	 * created and shared.
	 * 
	 * @see org.eclipse.team.tests.ccvs.core.EclipseTest#createProject(java.lang.String, java.lang.String)
	 */
	protected IProject createProject(String prefix, String[] resources) throws CoreException, TeamException {
		IProject project = super.createProject(prefix, resources);
		assertModificationState(project, null, true);
		return project;
	}


	public void testFileModifications() throws CoreException, TeamException {
		IProject project = createProject("testFileModifications", new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt" });
		// change two files, commit one and revert the other
		setContentsAndEnsureModified(project.getFile("changed.txt"));
		assertModificationState(project, new String[] {".", "changed.txt"}, true);
		setContentsAndEnsureModified(project.getFile(new Path("folder1/a.txt")));
		assertModificationState(project, new String[] {".", "changed.txt", "folder1/", "folder1/a.txt"}, true);
		commitResources(project, new String[] {"folder1/a.txt"});
		assertModificationState(project, new String[] {".", "changed.txt"}, true);
		replace(new IResource[] {project.getFile("changed.txt")}, null, true);
		assertModificationState(project, null, true);
	}

	public void testFileDeletions() throws CoreException, TeamException {
		if (TeamCVSTestPlugin.IS_UNSTABLE_TEST && Util.isMac())
			return;

		IProject project = createProject("testFileDeletions", new String[] { "changed.txt", "folder1/", "folder1/deleted.txt", "folder1/a.txt" });
		// delete and commit a file
		project.getFile("folder1/deleted.txt").delete(false, DEFAULT_MONITOR);
		assertModificationState(project, new String[] {".", "folder1/", "folder1/deleted.txt"}, true);
		commitResources(project, new String[] {"folder1/deleted.txt"});
		assertModificationState(project, null, true);
		// modify, delete and revert a file
		setContentsAndEnsureModified(project.getFile("changed.txt"));
		assertModificationState(project, new String[] {".", "changed.txt"}, true);
		project.getFile("changed.txt").delete(false, DEFAULT_MONITOR);
		assertModificationState(project, new String[] {".", "changed.txt"}, true);
		replace(new IResource[] {project.getFile("changed.txt")}, null, true);
		assertModificationState(project, null, true);
		// modify, delete and commit a file
		setContentsAndEnsureModified(project.getFile("changed.txt"));
		assertModificationState(project, new String[] {".", "changed.txt"}, true);
		project.getFile("changed.txt").delete(false, DEFAULT_MONITOR);
		assertModificationState(project, new String[] {".", "changed.txt"}, true);
		commitResources(project, new String[] {"changed.txt"});
		assertModificationState(project, null, true);
		// delete, recreate and commit
		project.getFile("folder1/a.txt").delete(false, DEFAULT_MONITOR);
		assertModificationState(project, new String[] {".", "folder1/", "folder1/a.txt"}, true);
		buildResources(project, new String[] {"folder1/a.txt"}, false);
		assertModificationState(project, new String[] {".", "folder1/", "folder1/a.txt"}, true);
		commitResources(project, new String[] {"folder1/a.txt"});
		assertModificationState(project, null, true);
		
	}
	
	public void testFileAdditions() throws CoreException, TeamException {
		IProject project = createProject("testFileAdditions", new String[] { "changed.txt", "folder1/", "folder1/deleted.txt", "folder1/a.txt" });
		// create, add and commit a file
		IResource[] addedResources = buildResources(project, new String[] {"folder1/added.txt"}, false);
		assertModificationState(project, new String[] {".", "folder1/", "folder1/added.txt"}, true);
		addResources(addedResources);
		assertModificationState(project, new String[] {".", "folder1/", "folder1/added.txt"}, true);
		commitResources(project, new String[] {"folder1/added.txt"});
		assertModificationState(project, null, true);
		// create, add and delete a file
		addResources(project, new String[] {"added.txt"}, false);
		assertModificationState(project, new String[] {".", "added.txt"}, true);
		project.getFile("added.txt").delete(false, false, DEFAULT_MONITOR);
		assertModificationState(project, null, true);
		// create and delete a file
		addedResources = buildResources(project, new String[] {"folder1/another.txt"}, false);
		assertModificationState(project, new String[] {".", "folder1/", "folder1/another.txt"}, true);
		project.getFile("folder1/another.txt").delete(false, false, DEFAULT_MONITOR);
		assertModificationState(project, null, true);
		// create and ignore a file
		addedResources = buildResources(project, new String[] {"ignored.txt"}, false);
		assertModificationState(project, new String[] {".", "ignored.txt"}, true);
		project.getFile(".cvsignore").create(new ByteArrayInputStream("ignored.txt".getBytes()), false, DEFAULT_MONITOR);
		assertModificationState(project, new String[] {".", ".cvsignore"}, true);
		addResources(new IResource[] {project.getFile(".cvsignore")});
		assertModificationState(project, new String[] {".", ".cvsignore"}, true);
		commitResources(project, new String[] {".cvsignore"});
		assertModificationState(project, null, true);
		// delete the .cvsignore to see the modification come back
		project.getFile(".cvsignore").delete(false, DEFAULT_MONITOR);
		assertModificationState(project, new String[] {".", "ignored.txt", ".cvsignore"}, true);
		commitResources(project, new String[] {".cvsignore"});
		assertModificationState(project, new String[] {".", "ignored.txt"}, true);
		// re-add the ignore and then delete the ignored
		project.getFile(".cvsignore").create(new ByteArrayInputStream("ignored.txt".getBytes()), false, DEFAULT_MONITOR);
		addResources(new IResource[] {project.getFile(".cvsignore")});
		assertModificationState(project, new String[] {".", ".cvsignore"}, true);
		commitResources(project, new String[] {".cvsignore"});
		assertModificationState(project, null, true);
		project.getFile("ignored.txt").delete(false, DEFAULT_MONITOR);
		assertModificationState(project, null, true);
		// add the ignored file to version control
		buildResources(project, new String[] {"ignored.txt"}, false);
		assertModificationState(project, null, true);
		addResources(new IResource[] {project.getFile("ignored.txt")});
		assertModificationState(project, new String[] {".", "ignored.txt"}, true);
		commitProject(project);
		assertModificationState(project, null, true);
	}
	
	public void testFileMoveAndCopy() throws CoreException, TeamException {
		IProject project = createProject("testFileMoveAndCopy", new String[] { "changed.txt", "folder1/", "folder2/", "folder1/a.txt" });
		// move a file
		project.getFile("folder1/a.txt").move(project.getFile("folder2/a.txt").getFullPath(), false, DEFAULT_MONITOR);
		assertModificationState(project, new String[] {".", "folder1/", "folder1/a.txt", "folder2/", "folder2/a.txt"}, true);
		// commit the source
		commitResources(project, new String[] {"folder1/a.txt"});
		assertModificationState(project, new String[] {".", "folder2/", "folder2/a.txt"}, true);
		// copy the destination back to the source
		project.getFolder("folder1").create(false, true, DEFAULT_MONITOR);
		project.getFile("folder2/a.txt").copy(project.getFile("folder1/a.txt").getFullPath(), false, DEFAULT_MONITOR);
		assertModificationState(project, new String[] {".", "folder1/", "folder1/a.txt", "folder2/", "folder2/a.txt"}, true);
		// add the source, delete the destination and commit
		addResources(new IResource[] {project.getFile("folder1/a.txt")});
		project.getFile("folder2/a.txt").delete(false, DEFAULT_MONITOR);
		commitProject(project);
		assertModificationState(project, null, true);
		// Do the above without committing the source
		project.getFile("folder1/a.txt").move(project.getFile("folder2/a.txt").getFullPath(), false, DEFAULT_MONITOR);
		assertModificationState(project, new String[] {".", "folder1/", "folder1/a.txt", "folder2/", "folder2/a.txt"}, true);
		// copy the destination back to the source
		project.getFile("folder2/a.txt").copy(project.getFile("folder1/a.txt").getFullPath(), false, DEFAULT_MONITOR);
		assertModificationState(project, new String[] {".", "folder1/", "folder1/a.txt", "folder2/", "folder2/a.txt"}, true);
		addResources(new IResource[] {project.getFile("folder2/a.txt")});
		try {
			commitProject(project);
		} catch (CVSException e) {
			// Some CVS servers return info about resetting date
			if (e.getStatus().getSeverity() == IStatus.ERROR) {
				throw e;
			}
		}
		assertModificationState(project, null, true);
	}
	
	public void testFolderAdditions() throws CoreException, TeamException {
		IProject project = createProject("testFileAdditions", new String[] { "changed.txt", "folder1/", "folder1/deleted.txt", "folder1/a.txt" });
		// create a folder
		project.getFolder("folder1/folder2").create(false, true, DEFAULT_MONITOR);
		assertModificationState(project, new String[] {".", "folder1/", "folder1/folder2/"}, true);
		addResources(new IResource[] {project.getFolder("folder1/folder2/")});
		assertModificationState(project, null, true);
		
		// create a folder
		project.getFolder("folder1/folder2/folder3").create(false, true, DEFAULT_MONITOR);
		assertModificationState(project, new String[] {".", "folder1/", "folder1/folder2/", "folder1/folder2/folder3"}, true);
		// add some children
		buildResources(project, new String[] {
			"folder1/folder2/folder3/add1.txt", 
			"folder1/folder2/folder3/add2.txt",
			"folder1/folder2/folder3/folder4/",
			"folder1/folder2/folder3/folder5/"}, false);
		assertModificationState(project, new String[] {".", "folder1/", "folder1/folder2/", "folder1/folder2/folder3",
			"folder1/folder2/folder3/add1.txt",
			"folder1/folder2/folder3/add2.txt",
			"folder1/folder2/folder3/folder4/",
			"folder1/folder2/folder3/folder5/"}, true);
		// delete some children
		project.getFile("folder1/folder2/folder3/add2.txt").delete(false, DEFAULT_MONITOR);
		project.getFolder("folder1/folder2/folder3/folder5/").delete(false, DEFAULT_MONITOR);
		assertModificationState(project, new String[] {".", "folder1/", "folder1/folder2/", "folder1/folder2/folder3",
			"folder1/folder2/folder3/add1.txt",
			"folder1/folder2/folder3/folder4/"}, true);
		// add to version control
		addResources(new IResource[] {
			project.getFile("folder1/folder2/folder3/add1.txt"),
			project.getFolder("folder1/folder2/folder3/folder4/")});
		assertModificationState(project, new String[] {".", "folder1/", "folder1/folder2/", "folder1/folder2/folder3",
			"folder1/folder2/folder3/add1.txt"}, true);
		// commit
		commitResources(project, new String[] {"folder1/folder2/folder3/add1.txt"});
		assertModificationState(project, null, true);
		
		// create a folder
		project.getFolder("folder1/ignored").create(false, true, DEFAULT_MONITOR);
		assertModificationState(project, new String[] {".", "folder1/", "folder1/ignored/"}, true);
		// add some files
		buildResources(project, new String[] {"folder1/ignored/file.txt"}, false);
		assertModificationState(project, new String[] {".", "folder1/", "folder1/ignored/", "folder1/ignored/file.txt"}, true);
		// ignore the folder
		project.getFile("folder1/.cvsignore").create(new ByteArrayInputStream("ignored".getBytes()), false, DEFAULT_MONITOR);
		assertModificationState(project, new String[] {".", "folder1/", "folder1/.cvsignore"}, true);
		addResources(new IResource[] {project.getFile("folder1/.cvsignore")});
		assertModificationState(project, new String[] {".", "folder1/", "folder1/.cvsignore"}, true);
		commitResources(project, new String[] {"folder1/.cvsignore"});
		assertModificationState(project, null, true);
		// delete the .cvsignore to see the modification come back
		project.getFile("folder1/.cvsignore").delete(false, DEFAULT_MONITOR);
		assertModificationState(project, new String[] {".", "folder1/", "folder1/.cvsignore", "folder1/ignored/", "folder1/ignored/file.txt"}, true);
		commitResources(project, new String[] {"folder1/.cvsignore"});
		assertModificationState(project, new String[] {".", "folder1/", "folder1/ignored/", "folder1/ignored/file.txt"}, true);
		// re-add the .cvsignore and then delete the ignored
		project.getFile("folder1/.cvsignore").create(new ByteArrayInputStream("ignored".getBytes()), false, DEFAULT_MONITOR);
		assertModificationState(project, new String[] {".", "folder1/", "folder1/.cvsignore"}, true);
		addResources(new IResource[] {project.getFile("folder1/.cvsignore")});
		commitResources(project, new String[] {"folder1/.cvsignore"});
		assertModificationState(project, null, true);
		project.getFolder("folder/ignored").delete(false, DEFAULT_MONITOR);
		assertModificationState(project, null, true);
		// add the ignored file to version control
		buildResources(project, new String[] {"folder1/ignored/file.txt"}, false);
		assertModificationState(project, null, true);
		addResources(new IResource[] {project.getFile("folder1/ignored/file.txt")});
		assertModificationState(project, new String[] {".", "folder1/", "folder1/ignored/", "folder1/ignored/file.txt"}, true);
		commitProject(project);
		assertModificationState(project, null, true);
	}
	
	public void testFolderDeletions() throws CoreException, TeamException {
		IProject project = createProject("testFileAdditions", new String[] { "changed.txt", "folder1/", "folder1/deleted.txt", "folder1/a.txt" });
		// create a folder
		project.getFolder("folder1/folder2").create(false, true, DEFAULT_MONITOR);
		assertModificationState(project, new String[] {".", "folder1/", "folder1/folder2/"}, true);
		// delete the folder
		project.getFolder("folder1/folder2").delete(false, DEFAULT_MONITOR);
		assertModificationState(project, null, true);
		
		// create a folder
		project.getFolder("folder1/folder2").create(false, true, DEFAULT_MONITOR);
		assertModificationState(project, new String[] {".", "folder1/", "folder1/folder2/"}, true);
		// add some children
		buildResources(project, new String[] {"folder1/folder2/file.txt"}, false);
		assertModificationState(project, new String[] {".", "folder1/", "folder1/folder2/", "folder1/folder2/file.txt"}, true);
		// delete the folder
		project.getFolder("folder1/folder2").delete(false, DEFAULT_MONITOR);
		assertModificationState(project, null, true);
		
		// delete a shared folder with files
		project.getFolder("folder1").delete(false, DEFAULT_MONITOR);
		assertModificationState(project, new String[] {".", "folder1/", "folder1/deleted.txt", "folder1/a.txt"}, true);
		// recreate folders and files
		project.getFolder("folder1").create(false, true, DEFAULT_MONITOR);
		assertModificationState(project, new String[] {".", "folder1/", "folder1/deleted.txt", "folder1/a.txt"}, true);
		replace(new IResource[] {project.getFile("folder1/deleted.txt"), project.getFile("folder1/a.txt")}, null, true);
		assertModificationState(project, null, true);
		
		// delete a shared folder with files
		project.getFolder("folder1").delete(false, DEFAULT_MONITOR);
		assertModificationState(project, new String[] {".", "folder1/", "folder1/deleted.txt", "folder1/a.txt"}, true);
		// commit file deletions
		commitProject(project);
		assertModificationState(project, null, true);
	}
	
	public void testFolderMoveAndCopy() throws CoreException, TeamException {
		IProject project = createProject("testFolderMoveAndCopy", new String[] { "changed.txt", "folder1/", "folder2/", "folder1/a.txt" , "folder1/folder3/file.txt"});
		// move a file
		project.getFolder("folder1/folder3").move(project.getFolder("folder2/folder3").getFullPath(), false, DEFAULT_MONITOR);
		assertModificationState(project, new String[] {".", "folder1/", "folder1/folder3", "folder1/folder3/file.txt", "folder2/", "folder2/folder3/", "folder2/folder3/file.txt"}, true);
		// commit the source
		commitResources(project, new String[] {"folder1/folder3"});
		assertModificationState(project, new String[] {".", "folder2/", "folder2/folder3/", "folder2/folder3/file.txt"}, true);
		// copy the destination back to the source
		project.getFolder("folder2/folder3/").copy(project.getFolder("folder1/folder3").getFullPath(), false, DEFAULT_MONITOR);
		assertModificationState(project, new String[] {".", "folder1/", "folder1/folder3", "folder1/folder3/file.txt", "folder2/", "folder2/folder3/", "folder2/folder3/file.txt"}, true);
		// add the source, delete the destination and commit
		addResources(new IResource[] {project.getFile("folder1/folder3/file.txt")});
		project.getFolder("folder2/folder3").delete(false, DEFAULT_MONITOR);
		commitProject(project);
		assertModificationState(project, null, true);
		// Do the above without committing the source
		project.getFolder("folder1/folder3").move(project.getFolder("folder2/folder3").getFullPath(), false, DEFAULT_MONITOR);
		assertModificationState(project, new String[] {".", "folder1/", "folder1/folder3", "folder1/folder3/file.txt", "folder2/", "folder2/folder3/", "folder2/folder3/file.txt"}, true);
		// copy the destination back to the source
		project.getFolder("folder2/folder3/").copy(project.getFolder("folder1/folder3").getFullPath(), false, DEFAULT_MONITOR);
		assertModificationState(project, new String[] {".", "folder1/", "folder1/folder3", "folder1/folder3/file.txt", "folder2/", "folder2/folder3/", "folder2/folder3/file.txt"}, true);
		addResources(new IResource[] {project.getFolder("folder2/folder3/")});
		try {
			commitProject(project);
		} catch (CVSException e) {
			// Some CVS servers return info about resetting date
			if (e.getStatus().getSeverity() == IStatus.ERROR) {
				throw e;
			}
		}
		assertModificationState(project, null, true);
	}
	
	public void testUpdate() throws TeamException, CoreException {
		// Create a test project, import it into cvs and check it out
		IProject project = createProject("testUpdate", new String[] { "changed.txt", "merged.txt", "deleted.txt", "folder1/", "folder1/a.txt" });

		// Check the project out under a different name
		IProject copy = checkoutCopy(project, "-copy");
		assertModificationState(copy, null, true);

		// Perform some operations on the copy and commit
		addResources(copy, new String[] { "added.txt", "folder2/", "folder2/added.txt" }, false);
		setContentsAndEnsureModified(copy.getFile("changed.txt"));
		setContentsAndEnsureModified(copy.getFile("merged.txt"));
		deleteResources(new IResource[] {copy.getFile("deleted.txt")});
		assertModificationState(copy, new String[] {".", "added.txt", "folder2/", "folder2/added.txt", "changed.txt", "merged.txt", "deleted.txt"}, true);
		commitResources(new IResource[] {copy}, IResource.DEPTH_INFINITE);
		assertModificationState(copy, null, true);
		
		// update the project and check status
		setContentsAndEnsureModified(project.getFile("merged.txt"));
		updateProject(project, null, false);
		assertModificationState(project, new String[] {".", "merged.txt"}, true);
		// can't commit because of merge
		// commitProject(project);
		// assertModificationState(project, null, true);
	}
	
	public void testUpdateIgnoreLocal() throws TeamException, CoreException {
		// Create a test project, import it into cvs and check it out
		IProject project = createProject("testUpdateIgnoreLocal", new String[] { "changed.txt", "merged.txt", "deleted.txt", "folder1/", "folder1/a.txt" });

		// modifiy a file
		setContentsAndEnsureModified(project.getFile("changed.txt"));
		assertModificationState(project, new String[] {".", "changed.txt"}, true);
		
		// peform un update -C
		updateProject(project, null, true /* ignore local changes */);
		assertModificationState(project, null, true);
	}
	
	public void testExternalDeletion() throws CoreException, TeamException {
		IProject project = createProject("testExternalDeletion", new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt", "folder1/folder2/b.txt"});
		IFile file = project.getFile("folder1/unmanaged.txt");
		file.create(new ByteArrayInputStream("stuff".getBytes()), false, DEFAULT_MONITOR);
		file.getLocation().toFile().delete();
		file.refreshLocal(IResource.DEPTH_ZERO, DEFAULT_MONITOR);
		assertTrue(!file.exists());
		assertModificationState(project, null, true);
	}
	
	public void testIgnoredAfterCheckout() throws TeamException, CoreException {
		// Bug 43938
		// Create a project and add a .cvsignore to it
		IProject project = createProject("testIgnoredAfterCheckout", new String[] { ".changed.txt", "deleted.txt", "folder1/", "folder1/a.txt", "folder1/folder2/b.txt"});
		project.getFile(".cvsignore").create(new ByteArrayInputStream("ignored".getBytes()), false, DEFAULT_MONITOR);
		addResources(new IResource[] {project.getFile(".cvsignore")});
		commitProject(project);
		assertModificationState(project, null, true);
		project.getFolder("ignored").create(false, true, DEFAULT_MONITOR);
		assertModificationState(project, null, true);
		
		// Checkout a copy and add the file to ensure it is ignored
		// Check the project out under a different name
		IProject copy = checkoutCopy(project, "-copy");
		assertModificationState(copy, null, true);
		copy.getFolder("ignored").create(false, true, DEFAULT_MONITOR);
		assertModificationState(copy, null, true);
	}

	public void testBug62547() throws TeamException, CoreException {
		IProject project = createProject("testBug62547Project", new String[] { "file1.txt", "file2.txt" });
		// ensure files have different content
		setContentsAndEnsureModified(project.getFile("file1.txt"));
		setContentsAndEnsureModified(project.getFile("file2.txt"));
		// ... but the same timestamp
		project.getFile("file2.txt").setLocalTimeStamp(project.getFile("file1.txt").getLocalTimeStamp());
		assertEquals(project.getFile("file1.txt").getLocalTimeStamp(), project.getFile("file2.txt").getLocalTimeStamp());
		// commit both
		commitResources(project, new String[] { "file1.txt", "file2.txt" });

		// delete the first file, and copy the second file over it
		project.getFile("file1.txt").delete(true, getMonitor());
		project.getFile("file2.txt").copy(project.getFile("file1.txt").getFullPath(), true, getMonitor());
		// check the timestamp once again
		assertEquals(project.getFile("file1.txt").getLocalTimeStamp(), project.getFile("file2.txt").getLocalTimeStamp());
		// there should be an outgoing change for the first file
		assertModificationState(project, new String[] { ".", "file1.txt" },	true);
	}
}

