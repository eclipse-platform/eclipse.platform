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
package org.eclipse.team.tests.ccvs.core.subscriber;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.ccvs.core.CVSCompareSubscriber;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.core.client.Command.KSubstOption;
import org.eclipse.team.tests.ccvs.core.CVSTestSetup;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Tests the CVSMergeSubscriber
 */
public class CVSCompareSubscriberTest extends CVSSyncSubscriberTest {

	public static Test suite() {
		String testName = System.getProperty("eclipse.cvs.testName");
		if (testName == null) {
			TestSuite suite = new TestSuite(CVSCompareSubscriberTest.class);
			return new CVSTestSetup(suite);
		} else {
			return new CVSTestSetup(new CVSCompareSubscriberTest(testName));
		}
	}

	public CVSCompareSubscriberTest() {
		super();
	}

	public CVSCompareSubscriberTest(String name) {
		super(name);
	}

	/**
	 * Test the basic changes that can occur when comparing the local workspace to a remote
	 * line-up.
	 */
	public void testStandardChanges() throws CoreException, IOException {
		// Create a test project
		IProject project = createProject("testCompareChanges", new String[]{"file1.txt", "file2.txt", "folder1/", "folder1/a.txt", "folder1/b.txt", "folder2/", "folder2/deleted.txt"});
		// Checkout and branch a copy
		CVSTag tag = new CVSTag("v1", CVSTag.VERSION);
		tagProject(project, tag, true);
		// Modify the workspace
		addResources(project, new String[]{"addition.txt", "folderAddition/", "folderAddition/new.txt"}, true);
		deleteResources(project, new String[]{"folder1/a.txt"}, true);
		deleteResources(project, new String[] {"folder2/"}, true);
		// modify file1 - make two revisions
		appendText(project.getFile("file1.txt"), "Appended text 1", false);
		commitProject(project);
		appendText(project.getFile("file1.txt"), "Appended text 2", false);
		commitProject(project);
		// modify file2 in both branch and head and ensure it's merged properly 
		appendText(project.getFile("file2.txt"), "appended text", false);
		commitProject(project);
		// create a merge subscriber
		CVSCompareSubscriber subscriber = getSyncInfoSource().createCompareSubscriber(project, tag);
		// check the sync states
		assertSyncEquals("testIncomingChanges", subscriber, project, 
				new String[]{
				"file1.txt", 
				"file2.txt", 
				"folder1/",
				"folder1/b.txt",
				"folder1/a.txt", 
				"addition.txt", 
				"folderAddition/", 
				"folderAddition/new.txt", 
				"folder2/",
				"folder2/deleted.txt"}, true, 
				new int[]{
				SyncInfo.CHANGE, 
				SyncInfo.CHANGE, 
				SyncInfo.IN_SYNC,
				SyncInfo.IN_SYNC,
				SyncInfo.ADDITION, 
				SyncInfo.DELETION, 
				SyncInfo.IN_SYNC, 
				SyncInfo.DELETION,
				SyncInfo.IN_SYNC,
				SyncInfo.ADDITION});
	}

	@Override
	protected void tearDown() throws Exception {
		getSyncInfoSource().tearDown();
		super.tearDown();
	}
	
	public void testInvalidTag() throws TeamException, CoreException {
		IProject project = createProject(new String[]{"file1.txt", "file2.txt", "folder1/", "folder1/a.txt", "folder1/b.txt", "folder2/", "folder2/deleted.txt"});
		// Create and compare with a non-existant tag
		CVSTag tag = new CVSTag("non-existant", CVSTag.VERSION);
		CVSCompareSubscriber subscriber = getSyncInfoSource().createCompareSubscriber(project.getFolder("folder1"), tag);
		// All files should be additions
		assertSyncEquals("testInvalidTag", subscriber, project.getFolder("folder1"), 
				new String[]{
				"a.txt",
				"b.txt"}, true, 
				new int[]{
				SyncInfo.DELETION, 
				SyncInfo.DELETION});
	}
	
	public void testBinaryAddition() throws CoreException {
		// See bug 132255
		KSubstOption option = CVSProviderPlugin.getPlugin().getDefaultTextKSubstOption();
		try {
			CVSProviderPlugin.getPlugin().setDefaultTextKSubstOption(Command.KSUBST_TEXT_KEYWORDS_ONLY);
			IProject project = createProject(new String[] { "a.txt"});
			// Checkout and branch a copy
			CVSTag v1 = new CVSTag("v1", CVSTag.VERSION);
			// Add a binary file that contains LFs
			IProject copy = checkoutCopy(project, "-copy");
			create(copy.getFile("binaryFile"), true);
			setContentsAndEnsureModified(copy.getFile("binaryFile"), "/n/n\n\n");
			addResources(new IResource[] { copy.getFile("binaryFile") });
			commitProject(copy);
			// Tag the project
			tagProject(copy, v1, false);
			// Compare with the tag and merge the changes
			CVSCompareSubscriber subscriber = getSyncInfoSource().createCompareSubscriber(project, v1);
			getSyncInfoSource().refresh(subscriber, project);
			getSyncInfoSource().overrideAndUpdateResources(subscriber, false, new IResource[] { project.getFile("binaryFile") });
			assertContentsEqual(copy.getFile("binaryFile"), project.getFile("binaryFile"));
		} finally {
			CVSProviderPlugin.getPlugin().setDefaultTextKSubstOption(option);
		}
	}
	
	public void testBinaryMarkAsMerged() throws CoreException, InvocationTargetException, InterruptedException {
		// See bug 132255
		KSubstOption option = CVSProviderPlugin.getPlugin().getDefaultTextKSubstOption();
		try {
			CVSProviderPlugin.getPlugin().setDefaultTextKSubstOption(Command.KSUBST_TEXT_KEYWORDS_ONLY);
			IProject project = createProject(new String[] { "a.txt"});
			// Checkout and branch a copy
			CVSTag v1 = new CVSTag("v1", CVSTag.VERSION);
			// Add a binary file that contains LFs
			IProject copy = checkoutCopy(project, "-copy");
			create(copy.getFile("binaryFile"), true);
			setContentsAndEnsureModified(copy.getFile("binaryFile"), "/n/n\n\n");
			addResources(new IResource[] { copy.getFile("binaryFile") });
			commitProject(copy);
			// Tag the project
			tagProject(copy, v1, false);
			// Add the same file to the project but don't share it
			create(project.getFile("binaryFile"), true);
			setContentsAndEnsureModified(project.getFile("binaryFile"), "/n/nSome Content\n\n");
			// Compare with the tag and merge the changes
			CVSCompareSubscriber subscriber = getSyncInfoSource().createCompareSubscriber(project, v1);
			getSyncInfoSource().refresh(subscriber, project);
			getSyncInfoSource().markAsMerged(subscriber, new IResource[] { project.getFile("binaryFile") });
			assertIsBinary(project.getFile("binaryFile"));
		} finally {
			CVSProviderPlugin.getPlugin().setDefaultTextKSubstOption(option);
		}
	}
}
