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
package org.eclipse.team.tests.ccvs.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFile;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteResource;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFolder;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.ui.operations.RemoteCompareOperation;
import org.eclipse.team.tests.ccvs.core.CVSTestSetup;
import org.eclipse.ui.IWorkbenchPart;

import junit.framework.Test;
import junit.framework.TestSuite;

public class CompareOperationTests extends CVSOperationTest {

	public class TestRemoteCompareOperation extends RemoteCompareOperation {
		private ICVSRemoteFolder leftTree, rightTree;

		public TestRemoteCompareOperation(IWorkbenchPart part, ICVSRemoteResource resource, CVSTag tag) {
			super(part, resource, tag);
		}
		
		/*
		 * Override to prevent compare editor from opening and to capture the results
		 */
		@Override
		protected void openCompareEditor(CompareTreeBuilder builder) {
			this.leftTree = builder.getLeftTree();
			this.rightTree = builder.getRightTree();
		}

		public ICVSRemoteFolder getLeftTree() {
			return leftTree;
		}

		public ICVSRemoteFolder getRightTree() {
			return rightTree;
		}

	}
	
	public CompareOperationTests() {
		super();
	}

	public CompareOperationTests(String name) {
		super(name);
	}
	
	public static Test suite() {
		String testName = System.getProperty("eclipse.cvs.testName");
		if (testName == null) {
			TestSuite suite = new TestSuite(CompareOperationTests.class);
			return new CVSTestSetup(suite);
		} else {
			return new CVSTestSetup(new CompareOperationTests(testName));
		}
	}
	

	/**
	 * Assert that the revisions of any files in the remote tree match the revisions in the local tree
	 */

	private void assertRevisionsMatch(ICVSRemoteFolder folder, IProject project, String[] filePathsWithRevisions, String[] filePathsWithoutRevisions) throws CoreException {
		if (filePathsWithRevisions == null) filePathsWithRevisions = new String[0];
		if (filePathsWithoutRevisions == null) filePathsWithoutRevisions = new String[0];
		IResource[] filesWithRevisions = getResources(project, filePathsWithRevisions);
		IResource[] filesWithoutRevisions = getResources(project, filePathsWithoutRevisions);
		ICVSRemoteFile[] files= getAllFiles(folder);
		assertTrue("The number of remote files with differences does not match the expected number", files.length == (filePathsWithoutRevisions.length + filePathsWithRevisions.length));
		for (ICVSRemoteFile remoteFile : files) {
			for (IResource local : filesWithRevisions) {
				ICVSFile cvsFile = CVSWorkspaceRoot.getCVSFileFor((IFile)local);
				if (cvsFile.getRepositoryRelativePath().equals(remoteFile.getRepositoryRelativePath())) {
					ResourceSyncInfo info = cvsFile.getSyncInfo();
					assertNotNull(info);
					String localRevision = info.getRevision();
					assertNotNull(localRevision);
					String remoteRevision = remoteFile.getRevision();
					assertNotNull(remoteRevision);
					assertEquals("Revisions do not match for " + local.getProjectRelativePath(), localRevision, remoteRevision);
				}
			}
			for (IResource local : filesWithoutRevisions) {
				ICVSFile cvsFile = CVSWorkspaceRoot.getCVSFileFor((IFile)local);
				if (cvsFile.getRepositoryRelativePath().equals(remoteFile.getRepositoryRelativePath())) {
					ResourceSyncInfo info = cvsFile.getSyncInfo();
					assertNotNull(info);
					String localRevision = info.getRevision();
					assertNotNull(localRevision);
					// Cannot assert anything about the remote revision
				}
			}
		}
	}
	
	private ICVSRemoteFile[] getAllFiles(ICVSRemoteFolder folder) {
		List<ICVSRemoteFile> result = new ArrayList<>();
		ICVSRemoteResource[] children = ((RemoteFolder)folder).getChildren();
		if (children != null) {
			for (ICVSRemoteResource resource : children) {
				if (resource.isContainer()) {
					result.addAll(Arrays.asList(getAllFiles((ICVSRemoteFolder)resource)));
				} else {
					result.add((ICVSRemoteFile) resource);
				}
			}
		}
		return result.toArray(new ICVSRemoteFile[result.size()]);
	}

	public void testCompareWithLatest() throws TeamException, CoreException {
		// Create a test project
		IProject project = createProject(new String[] { "file1.txt", "folder1/", "folder1/a.txt", "folder1/b.txt"});
		CVSTag v1 = new CVSTag("v1", CVSTag.VERSION);
		tagProject(project, v1, false);
		
		// Checkout and modify a copy (and commit the changes)
		IProject copy = checkoutCopy(project, "-copy");
		setContentsAndEnsureModified(copy.getFile("folder1/a.txt"));
		addResources(copy, new String[] { "folder1/newFile", "folder2/folder3/add.txt" }, false);
		deleteResources(copy, new String[] {"folder1/b.txt"}, false);
		commitResources(new IResource[] {copy}, IResource.DEPTH_INFINITE);

		// Run the compare operation of the project folder
		ICVSRemoteResource remoteResource = CVSWorkspaceRoot.getRemoteResourceFor(project);
		TestRemoteCompareOperation op = new TestRemoteCompareOperation(null, remoteResource, v1);
		run(op);
		assertRevisionsMatch(op.getRightTree(), project, new String[] {"folder1/a.txt", "folder1/b.txt"}, null);
		assertRevisionsMatch(op.getLeftTree(), copy, new String[] {"folder1/a.txt" }, new String[] {"folder1/newFile", "folder2/folder3/add.txt" } /* files with no revision */);
		
		
		// Run the compare operation of the project folder the other way
		remoteResource = CVSWorkspaceRoot.getRemoteResourceFor(project);
		remoteResource = ((ICVSRemoteFolder)remoteResource).forTag(v1);
		op = new TestRemoteCompareOperation(null, remoteResource, CVSTag.DEFAULT);
		run(op);
		assertRevisionsMatch(op.getLeftTree(), project, new String[] {"folder1/a.txt"}, new String[] {"folder1/b.txt"});
		assertRevisionsMatch(op.getRightTree(), copy, new String[] {"folder1/a.txt", "folder1/newFile", "folder2/folder3/add.txt" }, null /* files with no revision */);
		
		// Run the compare operation of a subfolder
		remoteResource = CVSWorkspaceRoot.getRemoteResourceFor(project.getFolder("folder1"));
		op = new TestRemoteCompareOperation(null, remoteResource, v1);
		run(op);
		assertRevisionsMatch(op.getRightTree(), project, new String[] {"folder1/a.txt", "folder1/b.txt"}, null);
		assertRevisionsMatch(op.getLeftTree(), copy, new String[] {"folder1/a.txt"}, new String[] {"folder1/newFile" } /* files with no revision */);
		
		// Run the operation on a single file
		remoteResource = CVSWorkspaceRoot.getRemoteResourceFor(copy.getFile("folder1/a.txt"));
		op = new TestRemoteCompareOperation(null, remoteResource, v1);
		run(op);
		assertRevisionsMatch(op.getRightTree(), project, new String[] {"folder1/a.txt"}, null);
		assertRevisionsMatch(op.getLeftTree(), copy, new String[] {"folder1/a.txt" }, null /* files with no revision */);
		
		// Run the operation on a single file using RemoteCompareOperation.getTag
		// to determine the tag
		remoteResource = CVSWorkspaceRoot.getRemoteResourceFor(copy.getFile("folder1/a.txt"));
		op = new TestRemoteCompareOperation(null, remoteResource, RemoteCompareOperation.getTag(CVSWorkspaceRoot.getRemoteResourceFor(project.getFile("folder1/a.txt"))));
		run(op);
		assertRevisionsMatch(op.getRightTree(), project, new String[] {"folder1/a.txt"}, null);
		assertRevisionsMatch(op.getLeftTree(), copy, new String[] {"folder1/a.txt" }, null /* files with no revision */);
		
	}

}
